package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Acceptance criteria tests for Milestone 1: First Playable Loop.
 * Each test maps to a specific AC item in GitHub issues #8, #9, or #10.
 */
@RunWith(RobolectricTestRunner::class)
class Milestone1AcceptanceCriteriaTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        stepBankRepository = RoomStepBankRepository(database)
        stepTrackerService = StepTrackerService(stepBankRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Issue #8: Step banking ────────────────────────────────────────────────

    /** #8 AC: PlayerState.bankedSteps is updated in the database on each step batch. */
    @Test
    fun `AC-8-1 bankedSteps updates in DB after recording steps`() = runTest {
        stepTrackerService.recordSensorDelta(count = 500, source = StepSource.Simulation)

        val banked = stepBankRepository.observeStepBank().first()
        assertEquals(500L, banked)

        val playerState = gameRepository.observePlayerState().first()
        assertEquals(500L, playerState.bankedSteps)
    }

    /** #8 AC: Banked step count survives app restart (persisted in DB). */
    @Test
    fun `AC-8-2 bankedSteps persists across DB reads (simulating restart)`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1200, source = StepSource.Simulation)

        // Simulate restart by re-querying directly from the DB (same DB instance, fresh read)
        val freshRead = gameRepository.observePlayerState().first()
        assertEquals(1200L, freshRead.bankedSteps)
    }

    /** #8 AC: Steps increment correctly across multiple batches. */
    @Test
    fun `AC-8-3 multiple step batches accumulate correctly`() = runTest {
        stepTrackerService.recordSensorDelta(count = 300, source = StepSource.Simulation)
        stepTrackerService.recordSensorDelta(count = 700, source = StepSource.Simulation)

        val banked = stepBankRepository.observeStepBank().first()
        assertEquals(1000L, banked)
    }

    // ── Issue #9: Affordability and travel confirmation ───────────────────────

    /**
     * #9 AC: Route is unaffordable when bankedSteps < stepCost (short road costs 1000).
     *
     * Note: JourneyRouteOption lives in feature:journey which is not a dependency of
     * core:data. The affordability check is tested here via TravelResult.NotEnoughSteps,
     * which encodes the same business rule. JourneyRouteOption.canAfford and .shortfall
     * are covered in feature:journey unit tests (JourneyRouteOptionTest).
     */
    @Test
    fun `AC-9-1 route is unaffordable when steps are insufficient`() = runTest {
        stepTrackerService.recordSensorDelta(count = 800, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        assertEquals(1000, shortRoad.stepCost)

        // Confirm that the travel system rejects the journey when bankedSteps < stepCost
        val result = gameRepository.travel(shortRoad.segmentId)
        assertTrue("Route should not be affordable with 800 steps for 1000-step road", result is TravelResult.NotEnoughSteps)
        val blocked = result as TravelResult.NotEnoughSteps
        assertEquals(1000L, blocked.required)
        assertEquals(800L, blocked.available)
        assertEquals(200L, blocked.required - blocked.available) // shortfall
    }

    /** #9 AC: Travel deducts the correct step cost from PlayerState.bankedSteps. */
    @Test
    fun `AC-9-2 travel deducts step cost from bankedSteps`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1500, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        gameRepository.travel(shortRoad.segmentId)

        val player = gameRepository.observePlayerState().first()
        assertEquals(500L, player.bankedSteps) // 1500 - 1000
    }

    /** #9 AC: TravelResult.NotEnoughSteps returned when steps are insufficient. */
    @Test
    fun `AC-9-3 travel blocked when steps insufficient`() = runTest {
        stepTrackerService.recordSensorDelta(count = 500, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        val result = gameRepository.travel(shortRoad.segmentId)

        assertTrue(result is TravelResult.NotEnoughSteps)
        val blocked = result as TravelResult.NotEnoughSteps
        assertEquals(1000L, blocked.required)
        assertEquals(500L, blocked.available)

        // Player state unchanged
        val player = gameRepository.observePlayerState().first()
        assertEquals(1L, player.currentTownId)
        assertEquals(500L, player.bankedSteps)
    }

    // ── Issue #10: Arrival persistence ───────────────────────────────────────

    /** #10 AC: PlayerState.currentTownId updates to destination after travel. */
    @Test
    fun `AC-10-1 currentTownId updates to destination after travel`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        val result = gameRepository.travel(shortRoad.segmentId)

        assertTrue(result is TravelResult.Arrived)
        val player = gameRepository.observePlayerState().first()
        assertEquals(2L, player.currentTownId)
    }

    /** #10 AC: After arrival, a fresh DB read reflects the new town (simulating restart). */
    @Test
    fun `AC-10-2 currentTownId persists to DB (no data loss on restart)`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        gameRepository.travel(roads.first { it.toTownId == 2L }.segmentId)

        // Re-read directly from DB to verify persistence (same DB, simulating cold-start read)
        val freshState = gameRepository.observePlayerState().first()
        assertEquals(2L, freshState.currentTownId)
        assertEquals(0L, freshState.bankedSteps)
    }

    /** #10 AC: No crash or data loss when travel fails (NotEnoughSteps). */
    @Test
    fun `AC-10-3 failed travel leaves DB state intact`() = runTest {
        val before = gameRepository.observePlayerState().first()

        stepTrackerService.recordSensorDelta(count = 100, source = StepSource.Simulation)
        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        gameRepository.travel(roads.first { it.toTownId == 2L }.segmentId)

        val after = gameRepository.observePlayerState().first()
        assertEquals(before.currentTownId, after.currentTownId)
        assertEquals(100L, after.bankedSteps) // steps still banked, not consumed
    }
}

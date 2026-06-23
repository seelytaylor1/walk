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
 * Integration test for User Story 1: Walk & Travel.
 *
 * Independent Test: Simulate step input, spend steps to traverse one road segment,
 * and verify arrival opens the destination town state.
 */
@RunWith(RobolectricTestRunner::class)
class UserStory1TravelFlowTest {
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
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * US1 Scenario 1: Simulate steps → travel → verify arrival.
     */
    @Test
    fun simulatedStepsEnableTravelAndArrivalUpdatesPlayerState() =
        runTest {
            // Seed world: Hearthwick (id=1) → Stoneford (id=2) costs 1000 steps
            gameRepository.initializeNewGame(seed = 1L)

            // Simulate step input via StepTrackerService (as a sensor would)
            stepTrackerService.recordSensorDelta(count = 1200, source = StepSource.Simulation)

            // Verify step bank increased before travel
            val stepsBefore = stepBankRepository.observeStepBank().first()
            assertEquals(1200L, stepsBefore)

            // Retrieve the road segment from Hearthwick to Stoneford
            val roads = gameRepository.observeRoadsFromCurrentTown().first()
            val roadToStoneford = roads.first { it.toTownId == 2L }
            assertEquals(1000, roadToStoneford.stepCost)

            // Travel along the road
            val result = gameRepository.travel(roadToStoneford.segmentId)

            // Verify TravelResult.Arrived with correct destination and remaining steps
            assertTrue("Expected TravelResult.Arrived but got $result", result is TravelResult.Arrived)
            val arrived = result as TravelResult.Arrived
            assertEquals(2L, arrived.townId)
            assertEquals(200L, arrived.remainingSteps)

            // Verify PlayerState.currentTownId updated to destination
            val player = gameRepository.observePlayerState().first()
            assertEquals(2L, player.currentTownId)

            // Verify step bank decreased by road cost (1200 - 1000 = 200)
            val stepsAfter = stepBankRepository.observeStepBank().first()
            assertEquals(200L, stepsAfter)

            // Verify lifetime steps unchanged by travel (only banked steps are spent)
            assertEquals(1200L, player.lifetimeSteps)
        }

    /**
     * US1 Scenario 2: Insufficient steps block travel without mutating player state.
     */
    @Test
    fun insufficientSimulatedStepsBlockTravelAndPreservePlayerState() =
        runTest {
            // Seed world: Hearthwick (id=1) → Stoneford (id=2) costs 1000 steps
            gameRepository.initializeNewGame(seed = 1L)

            // Simulate fewer steps than the road cost
            stepTrackerService.recordSensorDelta(count = 800, source = StepSource.Simulation)

            val roads = gameRepository.observeRoadsFromCurrentTown().first()
            val roadToStoneford = roads.first { it.toTownId == 2L }

            // Attempt travel with insufficient steps
            val result = gameRepository.travel(roadToStoneford.segmentId)

            // Verify NotEnoughSteps result with correct required/available values
            assertTrue(
                "Expected TravelResult.NotEnoughSteps but got $result",
                result is TravelResult.NotEnoughSteps,
            )
            val notEnough = result as TravelResult.NotEnoughSteps
            assertEquals(1000L, notEnough.required)
            assertEquals(800L, notEnough.available)

            // Verify player location unchanged
            val player = gameRepository.observePlayerState().first()
            assertEquals(1L, player.currentTownId)

            // Verify step bank unchanged
            val stepsAfter = stepBankRepository.observeStepBank().first()
            assertEquals(800L, stepsAfter)
        }

    /**
     * US1 Full flow: Simulate steps, travel to intermediate town, simulate more steps,
     * travel again to verify multi-hop travel works correctly.
     */
    @Test
    fun multiHopTravelFlowUpdatesLocationAndStepBankCorrectly() =
        runTest {
            // Seed world: Hearthwick(1) → Stoneford(2) costs 1000, Stoneford(2) → Mistfall(3) costs 2500
            gameRepository.initializeNewGame(seed = 1L)

            // First leg: Hearthwick → Stoneford (cost 1000)
            stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)
            val firstResult = gameRepository.travel(segmentId = 1L) // segment 1: Hearthwick→Stoneford
            assertTrue(firstResult is TravelResult.Arrived)
            assertEquals(2L, (firstResult as TravelResult.Arrived).townId)
            assertEquals(0L, firstResult.remainingSteps)

            // Verify player is now at Stoneford with 0 banked steps
            val playerAtStoneford = gameRepository.observePlayerState().first()
            assertEquals(2L, playerAtStoneford.currentTownId)
            assertEquals(0L, playerAtStoneford.bankedSteps)

            // Second leg: Stoneford → Mistfall (cost 2500)
            stepTrackerService.recordSensorDelta(count = 2700, source = StepSource.Simulation)

            val roadsFromStoneford = gameRepository.observeRoadsFromCurrentTown().first()
            val roadToMistfall = roadsFromStoneford.first { it.toTownId == 3L }

            val secondResult = gameRepository.travel(roadToMistfall.segmentId)
            assertTrue(secondResult is TravelResult.Arrived)
            assertEquals(3L, (secondResult as TravelResult.Arrived).townId)
            assertEquals(200L, secondResult.remainingSteps)

            // Verify final player state
            val playerAtMistfall = gameRepository.observePlayerState().first()
            assertEquals(3L, playerAtMistfall.currentTownId)
            assertEquals(200L, playerAtMistfall.bankedSteps)
            assertEquals(3700L, playerAtMistfall.lifetimeSteps) // 1000 + 2700
        }
}

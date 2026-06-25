package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.CompanionRole
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

@RunWith(RobolectricTestRunner::class)
class ScoutDiscountTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, OrderRepository(database))
        marketRepository = MarketRepository(database)
        stepBankRepository = RoomStepBankRepository(database)
        stepTrackerService = StepTrackerService(stepBankRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun recruitMiraTheScout() {
        // Complete 3 trades to unlock recruitment
        repeat(3) { marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1) }
        companionRepository.recruitCompanion(companionId = 1L) // Mira, Scout
    }

    /** AC-14: With Scout active, travel spends the discounted step cost. */
    @Test
    fun `AC-14 Scout reduces actual step spend by 10 percent`() =
        runTest {
            recruitMiraTheScout()

            val companions = companionRepository.observeActiveCompanions().first()
            assertTrue("Mira should be active", companions.any { it.role == CompanionRole.Scout && it.isActive })

            // Short road costs 1000 steps; with 10% Scout discount = 900
            stepTrackerService.recordSensorDelta(count = 900, source = StepSource.Simulation)

            val roads = gameRepository.observeRoadsFromCurrentTown().first()
            val shortRoad = roads.first { it.toTownId == 2L }

            val result = gameRepository.travel(shortRoad.segmentId)
            assertTrue("Travel should succeed with Scout discount applied, got $result", result is TravelResult.Arrived)

            val player = gameRepository.observePlayerState().first()
            assertEquals(0L, player.bankedSteps) // 900 spent exactly
        }

    /** AC-14: Without Scout, 900 steps is not enough for a 1000-step road. */
    @Test
    fun `AC-14 Without Scout 900 steps is insufficient for 1000-step road`() =
        runTest {
            stepTrackerService.recordSensorDelta(count = 900, source = StepSource.Simulation)

            val roads = gameRepository.observeRoadsFromCurrentTown().first()
            val shortRoad = roads.first { it.toTownId == 2L }

            val result = gameRepository.travel(shortRoad.segmentId)
            assertTrue("Travel should be blocked without Scout, got $result", result is TravelResult.NotEnoughSteps)
        }
}

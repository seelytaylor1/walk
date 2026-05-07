package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.testing.TestDatabaseFactory
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
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
 *
 * Spec acceptance scenarios covered:
 * 1. Given the player has at least the segment cost in banked steps, when they choose to travel,
 *    then their step bank decreases by the segment cost and their location updates to the destination.
 * 2. Given the player has fewer banked steps than the segment cost, when they view the travel option,
 *    then travel is disabled and the result explains the shortfall.
 */
@RunWith(RobolectricTestRunner::class)
class UserStory1TravelFlowTest {

    private lateinit var gameRepository: GameRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = TestDatabaseFactory.createInMemoryDatabase(context)
        gameRepository = GameRepository(database)
        stepBankRepository = RoomStepBankRepository(database)
        stepTrackerService = StepTrackerService(stepBankRepository)
    }

    /**
     * US1 Scenario 1: Simulate steps → travel → verify arrival.
     *
     * Seeds the world, simulates step input via StepTrackerService, calls travel(),
     * and verifies PlayerState.currentTownId updates, step bank decreases by road cost,
     * and TravelResult.Arrived is returned with correct values.
     */
    @Test
    fun simulatedStepsEnableTravelAndArrivalUpdatesPlayerState() = runTest {
        // Seed world: Hearthwick (id=1) → Stoneford (id=2) costs 120 steps
        gameRepository.initializeNewGame(seed = 1L)

        // Simulate step input via StepTrackerService (as a sensor would)
        stepTrackerService.recordSensorDelta(count = 150, source = StepSource.Simulation)

        // Verify step bank increased before travel
        val stepsBefore = stepBankRepository.observeStepBank().first()
        assertEquals(150L, stepsBefore)

        // Retrieve the road segment from Hearthwick to Stoneford
        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val roadToStoneford = roads.first { it.toTownId == 2L }
        assertEquals(120, roadToStoneford.stepCost)

        // Travel along the road
        val result = gameRepository.travel(roadToStoneford.segmentId)

        // Verify TravelResult.Arrived with correct destination and remaining steps
        assertTrue("Expected TravelResult.Arrived but got $result", result is TravelResult.Arrived)
        val arrived = result as TravelResult.Arrived
        assertEquals(2L, arrived.townId)
        assertEquals(30L, arrived.remainingSteps)

        // Verify PlayerState.currentTownId updated to destination
        val player = gameRepository.observePlayerState().first()
        assertEquals(2L, player.currentTownId)

        // Verify step bank decreased by road cost (150 - 120 = 30)
        val stepsAfter = stepBankRepository.observeStepBank().first()
        assertEquals(30L, stepsAfter)

        // Verify lifetime steps unchanged by travel (only banked steps are spent)
        assertEquals(150L, player.lifetimeSteps)
    }

    /**
     * US1 Scenario 2: Insufficient steps block travel without mutating player state.
     *
     * Verifies that when the player has fewer banked steps than the segment cost,
     * travel returns NotEnoughSteps with the correct shortfall and player state is unchanged.
     */
    @Test
    fun insufficientSimulatedStepsBlockTravelAndPreservePlayerState() = runTest {
        // Seed world: Hearthwick (id=1) → Stoneford (id=2) costs 120 steps
        gameRepository.initializeNewGame(seed = 1L)

        // Simulate fewer steps than the road cost
        stepTrackerService.recordSensorDelta(count = 100, source = StepSource.Simulation)

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
        assertEquals(120L, notEnough.required)
        assertEquals(100L, notEnough.available)

        // Verify player location unchanged
        val player = gameRepository.observePlayerState().first()
        assertEquals(1L, player.currentTownId)

        // Verify step bank unchanged
        val stepsAfter = stepBankRepository.observeStepBank().first()
        assertEquals(100L, stepsAfter)
    }

    /**
     * US1 Full flow: Simulate steps, travel to intermediate town, simulate more steps,
     * travel again to verify multi-hop travel works correctly.
     */
    @Test
    fun multiHopTravelFlowUpdatesLocationAndStepBankCorrectly() = runTest {
        // Seed world: Hearthwick(1) → Stoneford(2) costs 120, Stoneford(2) → Mistfall(3) costs 180
        gameRepository.initializeNewGame(seed = 1L)

        // First leg: Hearthwick → Stoneford (cost 120)
        stepTrackerService.recordSensorDelta(count = 120, source = StepSource.Simulation)
        val firstResult = gameRepository.travel(segmentId = 1L) // segment 1: Hearthwick→Stoneford
        assertTrue(firstResult is TravelResult.Arrived)
        assertEquals(2L, (firstResult as TravelResult.Arrived).townId)
        assertEquals(0L, firstResult.remainingSteps)

        // Verify player is now at Stoneford with 0 banked steps
        val playerAtStoneford = gameRepository.observePlayerState().first()
        assertEquals(2L, playerAtStoneford.currentTownId)
        assertEquals(0L, playerAtStoneford.bankedSteps)

        // Second leg: Stoneford → Mistfall (cost 180)
        stepTrackerService.recordSensorDelta(count = 200, source = StepSource.Simulation)

        val roadsFromStoneford = gameRepository.observeRoadsFromCurrentTown().first()
        val roadToMistfall = roadsFromStoneford.first { it.toTownId == 3L }

        val secondResult = gameRepository.travel(roadToMistfall.segmentId)
        assertTrue(secondResult is TravelResult.Arrived)
        assertEquals(3L, (secondResult as TravelResult.Arrived).townId)
        assertEquals(20L, secondResult.remainingSteps)

        // Verify final player state
        val playerAtMistfall = gameRepository.observePlayerState().first()
        assertEquals(3L, playerAtMistfall.currentTownId)
        assertEquals(20L, playerAtMistfall.bankedSteps)
        assertEquals(320L, playerAtMistfall.lifetimeSteps) // 120 + 200
    }
}

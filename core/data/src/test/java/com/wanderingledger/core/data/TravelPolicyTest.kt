package com.wanderingledger.core.data

import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import com.wanderingledger.core.model.PlayerClass
import com.wanderingledger.core.model.PlayerState
import com.wanderingledger.core.model.RoadSegment
import com.wanderingledger.core.model.Town
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for [TravelPolicy]. No database, no Robolectric — every
 * travel rule must be decidable from a [WorldSnapshot] alone.
 */
class TravelPolicyTest {
    private fun player(bankedSteps: Long) =
        PlayerState(
            name = "Ledger Keeper",
            playerClass = PlayerClass.Wanderer,
            gold = 100,
            currentTownId = 1,
            bankedSteps = bankedSteps,
            lifetimeSteps = bankedSteps,
        )

    private fun road(
        eventPool: String = "[]",
        stepCost: Int = 120,
    ) = RoadSegment(
        segmentId = 1,
        fromTownId = 1,
        toTownId = 2,
        stepCost = stepCost,
        narrativeDistance = "a half-day's walk",
        eventPool = eventPool,
    )

    private val destination =
        Town(townId = 2, name = "Stoneford", region = "Heartlands", biome = Biome.Forest)

    private fun snapshot(
        bankedSteps: Long = 200,
        eventPool: String = "[]",
        companions: List<Companion> = emptyList(),
    ) = WorldSnapshot(
        player = player(bankedSteps),
        road = road(eventPool = eventPool),
        destinationTown = destination,
        activeCompanions = companions,
        activeRumors = emptyList(),
        arrivedAt = 1_000L,
    )

    @Test
    fun insufficientStepsReturnsFailureOutcomeWithNoMutations() {
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 50), seed = 1L)

        val result = outcome.result
        assertTrue(result is TravelResult.NotEnoughSteps)
        result as TravelResult.NotEnoughSteps
        assertEquals(120L, result.required)
        assertEquals(50L, result.available)

        assertNull(outcome.playerDelta)
        assertNull(outcome.encounterOutcome)
        assertTrue(outcome.rumorRequests.isEmpty())
        assertTrue(outcome.eventLogs.isEmpty())
        assertEquals(false, outcome.decrementActiveRumors)
    }

    @Test
    fun validTravelProducesCorrectPlayerDelta() {
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200), seed = 1L)

        val result = outcome.result
        assertTrue(result is TravelResult.Arrived)
        result as TravelResult.Arrived
        assertEquals(2L, result.townId)
        assertEquals(80L, result.remainingSteps)

        val delta = outcome.playerDelta!!
        assertEquals(2L, delta.newTownId)
        assertEquals(120L, delta.stepsSpent)
        assertEquals(1_000L, delta.arrivedAt)
        assertTrue(outcome.markDestinationVisited)
        assertTrue(outcome.decrementActiveRumors)
    }

    @Test
    fun validTravelRequestsRoadAndTownRumorGeneration() {
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200), seed = 7L)

        assertEquals(2, outcome.rumorRequests.size)
        assertTrue(outcome.rumorRequests[0] is RumorRequest.RoadEvent)
        assertTrue(outcome.rumorRequests[1] is RumorRequest.TownVisit)
        // Seeds are derived from the travel seed so the whole transaction is
        // reproducible from one seed.
        assertEquals(7L + 1L, outcome.rumorRequests[0].seed)
        assertEquals(7L, outcome.rumorRequests[1].seed)
    }

    @Test
    fun roadWithEventPoolResolvesAnEncounter() {
        val outcome =
            TravelPolicy.compute(
                snapshot(bankedSteps = 200, eventPool = "[\"merchant-cart\"]"),
                seed = 1L,
            )

        assertNotNull("Encounter should be resolved when the pool is non-empty", outcome.encounterOutcome)
        assertEquals("merchant-cart", outcome.encounterOutcome!!.encounterId)
        // Encounter then arrival entries are logged.
        assertEquals(2, outcome.eventLogs.size)
        assertEquals("encounter", outcome.eventLogs[0].type)
        assertEquals("arrival", outcome.eventLogs[1].type)
    }

    @Test
    fun roadWithoutEventPoolHasNoEncounter() {
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200, eventPool = "[]"), seed = 1L)

        assertNull(outcome.encounterOutcome)
        assertEquals(1, outcome.eventLogs.size)
        assertEquals("arrival", outcome.eventLogs.single().type)
    }

    @Test
    fun encounterResolutionIsDeterministicForAGivenSeed() {
        val party = listOf(Companion(1, "Rogue", CompanionRole.Rogue, 2, 0, "active", 1, true))
        val first =
            TravelPolicy.compute(
                snapshot(bankedSteps = 200, eventPool = "[\"merchant-cart\"]", companions = party),
                seed = 99L,
            )
        val second =
            TravelPolicy.compute(
                snapshot(bankedSteps = 200, eventPool = "[\"merchant-cart\"]", companions = party),
                seed = 99L,
            )

        assertEquals(first.encounterOutcome, second.encounterOutcome)
    }
}

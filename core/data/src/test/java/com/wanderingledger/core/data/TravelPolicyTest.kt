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

        assertTrue(outcome is TravelOutcome.Failed)
        val failed = outcome as TravelOutcome.Failed
        assertTrue(failed.result is TravelResult.NotEnoughSteps)
        failed.result as TravelResult.NotEnoughSteps
        assertEquals(120L, failed.result.required)
        assertEquals(50L, failed.result.available)
    }

    @Test
    fun validTravelProducesCorrectPlayerDelta() {
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200), seed = 1L)

        assertTrue(outcome is TravelOutcome.Arrived)
        val arrived = outcome as TravelOutcome.Arrived
        assertEquals(2L, arrived.playerDelta.newTownId)
        assertEquals(120L, arrived.playerDelta.stepsSpent)
        assertEquals(1_000L, arrived.playerDelta.arrivedAt)
        assertTrue(arrived.markDestinationVisited)
        assertTrue(arrived.decrementActiveRumors)
    }

    @Test
    fun validTravelRequestsRoadAndTownRumorGeneration() {
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200), seed = 7L)

        assertTrue(outcome is TravelOutcome.Arrived)
        val arrived = outcome as TravelOutcome.Arrived
        assertEquals(2, arrived.rumorRequests.size)
        assertTrue(arrived.rumorRequests[0] is RumorRequest.RoadEvent)
        assertTrue(arrived.rumorRequests[1] is RumorRequest.TownVisit)
        assertEquals(7L + 1L, arrived.rumorRequests[0].seed)
        assertEquals(7L, arrived.rumorRequests[1].seed)
    }

    @Test
    fun roadWithEventPoolResolvesAnEncounter() {
        val outcome =
            TravelPolicy.compute(
                snapshot(bankedSteps = 200, eventPool = "[\"merchant-cart\"]"),
                seed = 1L,
            )

        assertTrue(outcome is TravelOutcome.Arrived)
        val arrived = outcome as TravelOutcome.Arrived
        assertNotNull("Encounter should be resolved when the pool is non-empty", arrived.encounterOutcome)
        assertEquals("merchant-cart", arrived.encounterOutcome!!.encounterId)
        assertEquals(2, arrived.eventLogs.size)
        assertEquals("encounter", arrived.eventLogs[0].type)
        assertEquals("arrival", arrived.eventLogs[1].type)
    }

    @Test
    fun roadWithoutEventPoolHasNoEncounter() {
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200, eventPool = "[]"), seed = 1L)

        assertTrue(outcome is TravelOutcome.Arrived)
        val arrived = outcome as TravelOutcome.Arrived
        assertNull(arrived.encounterOutcome)
        assertEquals(1, arrived.eventLogs.size)
        assertEquals("arrival", arrived.eventLogs.single().type)
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

        assertTrue(first is TravelOutcome.Arrived && second is TravelOutcome.Arrived)
        assertEquals(
            (first as TravelOutcome.Arrived).encounterOutcome,
            (second as TravelOutcome.Arrived).encounterOutcome,
        )
    }

    @Test
    fun scoutAtBondZeroAppliesTenPercentDiscount() {
        val scout =
            Companion(
                companionId = 1,
                name = "Mira",
                role = CompanionRole.Scout,
                combatPower = 3,
                bondLevel = 0,
                questState = "active",
                locationTownId = 1,
                isActive = true,
            )
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200, companions = listOf(scout)), seed = 1L)

        assertTrue(outcome is TravelOutcome.Arrived)
        val arrived = outcome as TravelOutcome.Arrived
        // stepCost=120, discount=0.10+(0*0.02)=0.10, effective=(120*0.90).toInt()=108
        assertEquals(108L, arrived.playerDelta.stepsSpent)
    }

    @Test
    fun scoutAtMaxBondAppliesTwentyPercentDiscount() {
        val scout =
            Companion(
                companionId = 1,
                name = "Mira",
                role = CompanionRole.Scout,
                combatPower = 3,
                bondLevel = 5,
                questState = "active",
                locationTownId = 1,
                isActive = true,
            )
        val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200, companions = listOf(scout)), seed = 1L)

        assertTrue(outcome is TravelOutcome.Arrived)
        val arrived = outcome as TravelOutcome.Arrived
        // stepCost=120, discount=0.10+(5*0.02)=0.20, effective=(120*0.80).toInt()=96
        assertEquals(96L, arrived.playerDelta.stepsSpent)
    }
}

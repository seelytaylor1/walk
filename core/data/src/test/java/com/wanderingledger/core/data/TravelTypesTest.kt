package com.wanderingledger.core.data

import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.PlayerClass
import com.wanderingledger.core.model.PlayerState
import com.wanderingledger.core.model.RoadSegment
import com.wanderingledger.core.model.Rumor
import com.wanderingledger.core.model.Town
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for the travel data classes. No database, no Robolectric —
 * these types are plain data and must be constructible in isolation.
 */
class TravelTypesTest {
    private val player =
        PlayerState(
            name = "Ledger Keeper",
            playerClass = PlayerClass.Wanderer,
            gold = 100,
            currentTownId = 1,
            bankedSteps = 200,
            lifetimeSteps = 200,
        )
    private val road =
        RoadSegment(
            segmentId = 1,
            fromTownId = 1,
            toTownId = 2,
            stepCost = 120,
            narrativeDistance = "a half-day's walk",
            eventPool = "[\"merchant-cart\"]",
        )
    private val destination =
        Town(townId = 2, name = "Stoneford", region = "Heartlands", biome = Biome.Forest)

    @Test
    fun worldSnapshotHoldsRealisticReadState() {
        val snapshot =
            WorldSnapshot(
                player = player,
                road = road,
                destinationTown = destination,
                activeCompanions = emptyList(),
                activeRumors =
                    listOf(
                        Rumor(
                            text = "Grain is scarce in Hearthwick.",
                            targetGoodId = 3,
                            sourceTownId = 1,
                            createdAt = 0,
                            expiryVisitsLeft = 3,
                        ),
                    ),
                arrivedAt = 1_000L,
            )

        assertEquals(2L, snapshot.road.toTownId)
        assertEquals(1, snapshot.activeRumors.size)
        assertEquals(1_000L, snapshot.arrivedAt)
    }

    @Test
    fun travelOutcomeCoversEveryMutationFieldForASuccessfulTravel() {
        val outcome =
            TravelOutcome(
                result = TravelResult.Arrived(townId = 2, remainingSteps = 80),
                playerDelta = PlayerDelta(newTownId = 2, stepsSpent = 120, arrivedAt = 1_000L),
                markDestinationVisited = true,
                decrementActiveRumors = true,
                rumorRequests =
                    listOf(
                        RumorRequest.RoadEvent(segmentId = 1, seed = 42),
                        RumorRequest.TownVisit(townId = 2, seed = 43),
                    ),
                encounterOutcome =
                    EncounterOutcome(
                        encounterId = "merchant-cart",
                        resultText = "They thanked you with coin.",
                        goldChange = 15,
                    ),
                eventLogs =
                    listOf(
                        EventLogDraft(
                            type = "arrival",
                            meta = "{\"segmentId\":1,\"toTownId\":2}",
                            result = "Arrived after spending 120 steps.",
                            createdAt = 1_000L,
                        ),
                    ),
            )

        assertTrue(outcome.result is TravelResult.Arrived)
        assertEquals(120L, outcome.playerDelta!!.stepsSpent)
        assertTrue(outcome.markDestinationVisited)
        assertTrue(outcome.decrementActiveRumors)
        assertEquals(2, outcome.rumorRequests.size)
        assertEquals(15L, outcome.encounterOutcome!!.goldChange)
        assertEquals("arrival", outcome.eventLogs.single().type)
    }

    @Test
    fun travelOutcomeForFailureCarriesNoMutations() {
        val outcome =
            TravelOutcome(
                result = TravelResult.NotEnoughSteps(required = 120, available = 50),
            )

        assertTrue(outcome.result is TravelResult.NotEnoughSteps)
        assertNull(outcome.playerDelta)
        assertNull(outcome.encounterOutcome)
        assertTrue(outcome.rumorRequests.isEmpty())
        assertTrue(outcome.eventLogs.isEmpty())
        assertEquals(false, outcome.decrementActiveRumors)
    }
}

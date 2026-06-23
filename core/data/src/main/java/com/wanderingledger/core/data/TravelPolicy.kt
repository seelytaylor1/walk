package com.wanderingledger.core.data

import kotlin.random.Random

/**
 * Pure decision logic for a travel action.
 *
 * Every travel *rule* lives here: the step-cost check, road-encounter
 * resolution, rumor generation and the event-log entries that result. It reads
 * a [WorldSnapshot] and a [seed] and returns a [TravelOutcome] describing the
 * mutations to apply — it launches no coroutines, touches no database and has
 * no side effects, so the rules are readable in one place and testable without
 * Room or Robolectric.
 *
 * Encounter resolution is delegated to [EncounterEngine]; rumor generation is
 * delegated to [RumorRepository] via [RumorRequest] descriptors that the write
 * phase fulfils (rumor text depends on town/good data that does not belong in
 * the snapshot).
 */
object TravelPolicy {
    fun compute(
        snapshot: WorldSnapshot,
        seed: Long,
    ): TravelOutcome {
        val player = snapshot.player
        val road = snapshot.road
        val hasActiveScout = snapshot.activeCompanions.any {
            it.role == com.wanderingledger.core.model.CompanionRole.Scout && it.isActive
        }
        val effectiveStepCost = applyScoutDiscount(road.stepCost, hasActiveScout)
        val stepCost = effectiveStepCost.toLong()

        // Rule: travel is blocked when the player cannot afford the road's cost.
        // A blocked travel mutates nothing.
        if (player.bankedSteps < stepCost) {
            return TravelOutcome(
                result =
                    TravelResult.NotEnoughSteps(
                        required = stepCost,
                        available = player.bankedSteps,
                    ),
            )
        }

        val remainingSteps = player.bankedSteps - stepCost
        val arrivedAt = snapshot.arrivedAt

        // Rule: a road with a non-empty event pool resolves one encounter,
        // seeded deterministically from the travel seed and the segment.
        val eventPool = parseEventPool(road.eventPool)
        val encounter: EncounterOutcome? =
            if (eventPool.isNotEmpty()) {
                val encounterSeed = seed + road.segmentId
                val encounterId = eventPool.random(Random(encounterSeed))
                EncounterEngine.resolve(encounterSeed, encounterId, snapshot.activeCompanions)
            } else {
                null
            }

        // Encounter entry is logged before the arrival entry, matching the
        // historical ordering of the inline transaction.
        val eventLogs =
            buildList {
                if (encounter != null) {
                    add(
                        EventLogDraft(
                            type = "encounter",
                            meta =
                                "{\"encounterId\":\"${encounter.encounterId}\",\"success\":${encounter.success}," +
                                    "\"goldChange\":${encounter.goldChange}}",
                            result = encounter.resultText,
                            createdAt = arrivedAt,
                        ),
                    )
                }
                add(
                    EventLogDraft(
                        type = "arrival",
                        meta = "{\"segmentId\":${road.segmentId},\"toTownId\":${road.toTownId}}",
                        result = "Arrived after spending ${road.stepCost} steps.",
                        createdAt = arrivedAt,
                    ),
                )
            }

        return TravelOutcome(
            result = TravelResult.Arrived(townId = road.toTownId, remainingSteps = remainingSteps),
            playerDelta =
                PlayerDelta(
                    newTownId = road.toTownId,
                    stepsSpent = stepCost,
                    arrivedAt = arrivedAt,
                ),
            markDestinationVisited = true,
            decrementActiveRumors = true,
            // Road event first, then town visit — the order the inline
            // transaction generated them in.
            rumorRequests =
                listOf(
                    RumorRequest.RoadEvent(segmentId = road.segmentId, seed = seed + road.segmentId),
                    RumorRequest.TownVisit(townId = road.toTownId, seed = seed),
                ),
            encounterOutcome = encounter,
            eventLogs = eventLogs,
        )
    }

    private fun parseEventPool(raw: String): List<String> =
        try {
            raw
                .trim('[', ']')
                .split(',')
                .map { it.trim(' ', '"') }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
}

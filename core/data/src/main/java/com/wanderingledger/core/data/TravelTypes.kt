package com.wanderingledger.core.data

import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import com.wanderingledger.core.model.PlayerState
import com.wanderingledger.core.model.RoadSegment
import com.wanderingledger.core.model.Rumor
import com.wanderingledger.core.model.Town
import org.json.JSONArray

fun String.parseEventPool(): List<String> =
    try {
        val arr = JSONArray(this)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }

/** 10% step cost reduction applied when a Scout companion is active. */
const val SCOUT_STEP_DISCOUNT = 0.10

/**
 * Returns the effective step cost for a road segment, applying the Scout
 * discount when [hasActiveScout] is true.
 */
fun applyScoutDiscount(stepCost: Int, hasActiveScout: Boolean): Int =
    if (hasActiveScout) (stepCost * (1.0 - SCOUT_STEP_DISCOUNT)).toInt() else stepCost

/**
 * A point-in-time read of everything a travel transaction needs to decide its
 * outcome: the player, the road being travelled, the destination town, the
 * active party and the active rumors.
 *
 * It is plain data — no database handles, no flows — so that travel rules can be
 * computed (and tested) without Room. See [TravelPolicy].
 */
data class WorldSnapshot(
    val player: PlayerState,
    val road: RoadSegment,
    val destinationTown: Town,
    val activeCompanions: List<Companion>,
    val activeRumors: List<Rumor>,
    /** Wall-clock instant the travel arrives, used for timestamps in the outcome. */
    val arrivedAt: Long,
)

/**
 * A request to generate a rumor during the write phase of a travel.
 *
 * Rumor *text* depends on town/good/price data that does not belong in
 * [WorldSnapshot], so [TravelPolicy] emits a request describing which rumor to
 * generate (and the seed to generate it with) and the repository fulfils it.
 */
sealed interface RumorRequest {
    /** Seed forwarded to [RumorRepository] so generation is reproducible. */
    val seed: Long

    data class TownVisit(
        val townId: Long,
        override val seed: Long,
    ) : RumorRequest

    data class RoadEvent(
        val segmentId: Long,
        override val seed: Long,
    ) : RumorRequest
}

/** The change to apply to the player as a result of a successful travel. */
data class PlayerDelta(
    val newTownId: Long,
    val stepsSpent: Long,
    val arrivedAt: Long,
)

/** A pending event-log entry produced by a travel, written verbatim by the repository. */
data class EventLogDraft(
    val type: String,
    val meta: String,
    val result: String,
    val createdAt: Long,
)

/**
 * The complete set of mutations a travel produces.
 *
 * Plain data — no DAO calls. The repository's write phase applies each field:
 * the [playerDelta], whether to mark the destination [markDestinationVisited],
 * whether to [decrementActiveRumors], the [rumorRequests] to fulfil, the
 * [encounterOutcome] (gold/bond deltas) to apply, and the [eventLogs] to insert.
 *
 * For a failed travel (e.g. not enough steps) [result] carries the failure and
 * every mutation field is empty/null.
 */
data class TravelOutcome(
    val result: TravelResult,
    val playerDelta: PlayerDelta? = null,
    val markDestinationVisited: Boolean = false,
    val decrementActiveRumors: Boolean = false,
    val rumorRequests: List<RumorRequest> = emptyList(),
    val encounterOutcome: EncounterOutcome? = null,
    val eventLogs: List<EventLogDraft> = emptyList(),
)

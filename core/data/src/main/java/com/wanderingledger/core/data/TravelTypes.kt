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

const val SCOUT_BASE_DISCOUNT = 0.10
const val SCOUT_BOND_DISCOUNT_PER_LEVEL = 0.02

/**
 * Returns the effective step cost for a road segment, applying the Scout
 * discount scaled by bond level. A null [scoutBondLevel] means no active Scout.
 */
fun applyScoutDiscount(stepCost: Int, scoutBondLevel: Int?): Int {
    if (scoutBondLevel == null) return stepCost
    val discount = SCOUT_BASE_DISCOUNT + (scoutBondLevel * SCOUT_BOND_DISCOUNT_PER_LEVEL)
    return (stepCost * (1.0 - discount)).toInt()
}

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

    /** Called by the write phase to fulfil this request via the rumor repository. */
    suspend fun fulfill(repo: RumorRepository)

    data class TownVisit(
        val townId: Long,
        override val seed: Long,
    ) : RumorRequest {
        override suspend fun fulfill(repo: RumorRepository) =
            repo.generateRumorForTownVisit(townId, seed)
    }

    data class RoadEvent(
        val segmentId: Long,
        override val seed: Long,
    ) : RumorRequest {
        override suspend fun fulfill(repo: RumorRepository) =
            repo.generateRumorFromRoadEvent(segmentId, seed)
    }
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
 * The result of [TravelPolicy.compute].
 *
 * [Arrived] guarantees every mutation field is valid and non-null.
 * [Failed] carries the failure reason; no mutations apply.
 *
 * Callers pattern-match once with Kotlin's exhaustive `when` — no null-checks needed.
 */
sealed interface TravelOutcome {
    data class Arrived(
        val playerDelta: PlayerDelta,
        val markDestinationVisited: Boolean = false,
        val decrementActiveRumors: Boolean = false,
        val rumorRequests: List<RumorRequest> = emptyList(),
        val encounterOutcome: EncounterOutcome? = null,
        val eventLogs: List<EventLogDraft> = emptyList(),
    ) : TravelOutcome

    data class Failed(val result: TravelResult) : TravelOutcome
}

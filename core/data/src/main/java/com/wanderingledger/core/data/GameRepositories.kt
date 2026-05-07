package com.wanderingledger.core.data

import androidx.room.withTransaction
import com.wanderingledger.core.database.EventLogEntity
import com.wanderingledger.core.database.PlayerStateEntity
import com.wanderingledger.core.database.RoadSegmentEntity
import com.wanderingledger.core.database.SeedWorld
import com.wanderingledger.core.database.StepRecordEntity
import com.wanderingledger.core.database.TownEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.PlayerClass
import com.wanderingledger.core.model.PlayerState
import com.wanderingledger.core.model.RoadSegment
import com.wanderingledger.core.model.Town
import com.wanderingledger.core.steptracker.StepBankRepository
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepSpendResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneOffset

class RoomStepBankRepository(
    private val database: WanderingLedgerDatabase,
) : StepBankRepository {
    override fun observeStepBank(): Flow<Long> =
        database.playerDao().getPlayer().filterNotNull().map { it.bankedSteps }

    override suspend fun recordDetectedSteps(count: Int, source: StepSource, recordedAt: Long) {
        if (count <= 0) return
        database.withTransaction {
            val player = database.playerDao().getPlayerSnapshot()
                ?: error("Seed world before recording steps.")
            database.stepRecordDao().insertRecord(
                StepRecordEntity(
                    dateEpoch = recordedAt.toEpochDay(),
                    steps = count,
                    source = source.name,
                ),
            )
            database.playerDao().updatePlayer(
                player.copy(
                    bankedSteps = player.bankedSteps + count,
                    lifetimeSteps = player.lifetimeSteps + count,
                    lastSyncAt = recordedAt,
                ),
            )
        }
    }

    override suspend fun spendSteps(amount: Long, reason: String): StepSpendResult {
        require(amount >= 0) { "Step spend amount must be non-negative." }
        return database.withTransaction {
            val player = database.playerDao().getPlayerSnapshot()
                ?: error("Seed world before spending steps.")
            if (player.bankedSteps < amount) {
                StepSpendResult(spent = false, requested = amount, remaining = player.bankedSteps)
            } else {
                database.playerDao().updatePlayer(
                    player.copy(
                        bankedSteps = player.bankedSteps - amount,
                        lastSyncAt = System.currentTimeMillis(),
                    ),
                )
                StepSpendResult(spent = true, requested = amount, remaining = player.bankedSteps - amount)
            }
        }
    }
}

class GameRepository(
    private val database: WanderingLedgerDatabase,
) {
    fun observePlayerState(): Flow<PlayerState> =
        database.playerDao().getPlayer().filterNotNull().map { it.toModel() }

    fun observeCurrentTown(): Flow<Town> =
        combine(
            database.playerDao().getPlayer().filterNotNull(),
            database.townDao().listTowns(),
        ) { player, towns ->
            towns.first { it.townId == player.currentTownId }.toModel()
        }

    fun observeTown(townId: Long): Flow<Town?> =
        database.townDao().getTown(townId).map { it?.toModel() }

    fun observeRoadsFromCurrentTown(): Flow<List<RoadSegment>> =
        database.playerDao().getPlayer().filterNotNull().map { it.currentTownId }.map { townId ->
            database.roadSegmentDao().listRoadsFrom(townId).first().map { it.toModel() }
        }

    fun observeTravelRoutesFromCurrentTown(): Flow<List<TravelRoute>> =
        database.playerDao().getPlayer().filterNotNull().map { player ->
            val towns = database.townDao().listTowns().first().associateBy { it.townId }
            database.roadSegmentDao().listRoadsFrom(player.currentTownId).first().mapNotNull { road ->
                towns[road.toTownId]?.let { destination ->
                    TravelRoute(
                        segment = road.toModel(),
                        destination = destination.toModel(),
                    )
                }
            }
        }

    suspend fun initializeNewGame(seed: Long = 1L) {
        SeedWorld.ensureSeeded(database, now = seed.coerceAtLeast(1L))
    }

    suspend fun travel(segmentId: Long): TravelResult {
        return database.withTransaction {
            val player = database.playerDao().getPlayerSnapshot()
                ?: error("Seed world before traveling.")
            val road = database.roadSegmentDao().getRoadSnapshot(segmentId)
                ?: return@withTransaction TravelResult.RoadNotFound
            if (road.fromTownId != player.currentTownId) {
                return@withTransaction TravelResult.NotAtRoadOrigin
            }
            if (player.bankedSteps < road.stepCost) {
                return@withTransaction TravelResult.NotEnoughSteps(
                    required = road.stepCost.toLong(),
                    available = player.bankedSteps,
                )
            }

            val arrivedAt = System.currentTimeMillis()
            database.playerDao().updatePlayer(
                player.copy(
                    currentTownId = road.toTownId,
                    bankedSteps = player.bankedSteps - road.stepCost,
                    lastSyncAt = arrivedAt,
                ),
            )
            database.townDao().getTownSnapshot(road.toTownId)?.let { destination ->
                database.townDao().updateTown(destination.copy(storyState = "visited", lastVisitedAt = arrivedAt))
            }
            database.rumorDao().decrementAllActive()
            database.eventLogDao().insertEvent(
                EventLogEntity(
                    type = "arrival",
                    meta = "{\"segmentId\":$segmentId,\"toTownId\":${road.toTownId}}",
                    result = "Arrived after spending ${road.stepCost} steps.",
                    createdAt = arrivedAt,
                ),
            )
            TravelResult.Arrived(road.toTownId, player.bankedSteps - road.stepCost)
        }
    }
}

sealed interface TravelResult {
    data class Arrived(val townId: Long, val remainingSteps: Long) : TravelResult
    data class NotEnoughSteps(val required: Long, val available: Long) : TravelResult
    data object RoadNotFound : TravelResult
    data object NotAtRoadOrigin : TravelResult
}

data class TravelRoute(
    val segment: RoadSegment,
    val destination: Town,
)

private fun PlayerStateEntity.toModel(): PlayerState =
    PlayerState(
        playerId = playerId,
        name = name,
        playerClass = PlayerClass.valueOf(playerClass),
        gold = gold,
        currentTownId = currentTownId,
        inventorySlots = inventorySlots,
        bankedSteps = bankedSteps,
        lifetimeSteps = lifetimeSteps,
        lastSyncAt = lastSyncAt,
    )

private fun TownEntity.toModel(): Town =
    Town(
        townId = townId,
        name = name,
        region = region,
        reputation = reputation,
        storyState = storyState,
        lastVisitedAt = lastVisitedAt,
    )

private fun RoadSegmentEntity.toModel(): RoadSegment =
    RoadSegment(
        segmentId = segmentId,
        fromTownId = fromTownId,
        toTownId = toTownId,
        stepCost = stepCost,
        narrativeDistance = narrativeDistance,
        eventPool = eventPool,
    )

private fun Long.toEpochDay(): Long =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

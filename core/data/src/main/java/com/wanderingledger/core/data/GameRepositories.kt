package com.wanderingledger.core.data

import androidx.room.withTransaction
import com.wanderingledger.core.database.EventLogEntity
import com.wanderingledger.core.database.PlayerStateEntity
import com.wanderingledger.core.database.RoadSegmentEntity
import com.wanderingledger.core.database.SeedWorld
import com.wanderingledger.core.database.StepRecordEntity
import com.wanderingledger.core.database.TownEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.PlayerClass
import com.wanderingledger.core.model.PlayerState
import com.wanderingledger.core.model.RoadSegment
import com.wanderingledger.core.model.Town
import com.wanderingledger.core.steptracker.StepBankRepository
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepSpendResult
import com.wanderingledger.core.telemetry.TelemetryEvent
import com.wanderingledger.core.telemetry.TelemetryService
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
        database
            .playerDao()
            .getPlayer()
            .filterNotNull()
            .map { it.bankedSteps }

    override suspend fun recordDetectedSteps(
        count: Int,
        source: StepSource,
        recordedAt: Long,
    ) {
        if (count <= 0) return
        database.withTransaction {
            val player =
                database.playerDao().getPlayerSnapshot()
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

    override suspend fun spendSteps(
        amount: Long,
        reason: String,
    ): StepSpendResult {
        require(amount >= 0) { "Step spend amount must be non-negative." }
        return database.withTransaction {
            val player =
                database.playerDao().getPlayerSnapshot()
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
    private val rumorRepository: RumorRepository,
    private val companionRepository: CompanionRepository,
) {
    fun observePlayerState(): Flow<PlayerState> =
        database
            .playerDao()
            .getPlayer()
            .filterNotNull()
            .map { it.toModel() }

    fun observeCurrentTown(): Flow<Town> =
        combine(
            database.playerDao().getPlayer().filterNotNull(),
            database.townDao().listTowns(),
        ) { player, towns ->
            towns.first { it.townId == player.currentTownId }.toModel()
        }

    fun observeTown(townId: Long): Flow<Town?> = database.townDao().getTown(townId).map { it?.toModel() }

    fun observeRoadsFromCurrentTown(): Flow<List<RoadSegment>> =
        database.playerDao().getPlayer().filterNotNull().map { it.currentTownId }.map { townId ->
            database
                .roadSegmentDao()
                .listRoadsFrom(townId)
                .first()
                .map { it.toModel() }
        }

    fun observeTravelRoutesFromCurrentTown(): Flow<List<TravelRoute>> =
        database.playerDao().getPlayer().filterNotNull().map { player ->
            val towns =
                database
                    .townDao()
                    .listTowns()
                    .first()
                    .associateBy { it.townId }
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
        val existingRumors = database.rumorDao().listActiveRumors().first()
        if (existingRumors.isEmpty()) {
            rumorRepository.generateRumorForTownVisit(visitedTownId = 1L)
        }
    }

    suspend fun travel(
        segmentId: Long,
        seed: Long = System.currentTimeMillis(),
    ): TravelResult {
        val startedAt = System.currentTimeMillis()
        return database.withTransaction {
            // --- Read: validate the road, then assemble a WorldSnapshot ---
            val player =
                database.playerDao().getPlayerSnapshot()
                    ?: error("Seed world before traveling.")
            val road =
                database.roadSegmentDao().getRoadSnapshot(segmentId)
                    ?: run {
                        recordTravelCompleted(startedAt, segmentId, success = false)
                        return@withTransaction TravelResult.RoadNotFound
                    }
            if (road.fromTownId != player.currentTownId) {
                recordTravelCompleted(startedAt, segmentId, success = false)
                return@withTransaction TravelResult.NotAtRoadOrigin
            }

            TelemetryService.tryRecord(
                TelemetryEvent.TravelStarted(
                    timestamp = startedAt,
                    segmentId = segmentId,
                    fromTownId = player.currentTownId,
                    toTownId = road.toTownId,
                    requiredSteps = road.stepCost,
                    availableSteps = player.bankedSteps,
                ),
            )

            val arrivedAt = System.currentTimeMillis()
            val destination = database.townDao().getTownSnapshot(road.toTownId)
            val snapshot =
                WorldSnapshot(
                    player = player.toModel(),
                    road = road.toModel(),
                    // destinationTown is carried for completeness; TravelPolicy does
                    // not read it, so a synthetic stand-in is harmless if the seed is
                    // missing the row. The visited-update below re-reads the entity.
                    destinationTown =
                        destination?.toModel()
                            ?: Town(townId = road.toTownId, name = "", region = "", biome = Biome.Forest),
                    activeCompanions = companionRepository.observeActiveCompanions().first(),
                    activeRumors = rumorRepository.observeActiveRumors().first(),
                    arrivedAt = arrivedAt,
                )

            // --- Decide: all travel rules live in TravelPolicy ---
            val outcome = TravelPolicy.compute(snapshot, seed)

            when (outcome) {
                is TravelOutcome.Failed -> {
                    recordTravelCompleted(startedAt, segmentId, success = false)
                    return@withTransaction outcome.result
                }
                is TravelOutcome.Arrived -> {
                    val delta = outcome.playerDelta

                    // --- Write: apply the outcome's mutations ---
                    val goldChange = outcome.encounterOutcome?.goldChange ?: 0L
                    database.playerDao().updatePlayer(
                        player.copy(
                            currentTownId = delta.newTownId,
                            bankedSteps = player.bankedSteps - delta.stepsSpent,
                            gold = (player.gold + goldChange).coerceAtLeast(0),
                            lastSyncAt = delta.arrivedAt,
                        ),
                    )

                    if (outcome.markDestinationVisited) {
                        database.townDao().getTownSnapshot(delta.newTownId)?.let { dest ->
                            database.townDao().updateTown(
                                dest.copy(storyState = "visited", lastVisitedAt = delta.arrivedAt),
                            )
                        }
                    }

                    if (outcome.decrementActiveRumors) {
                        database.rumorDao().decrementAllActive()
                    }

                    outcome.rumorRequests.forEach { it.fulfill(rumorRepository) }

                    outcome.encounterOutcome?.let { encounter ->
                        if (encounter.bondChange != 0) {
                            snapshot.activeCompanions.forEach { companion ->
                                companionRepository.updateBond(companion.companionId, encounter.bondChange)
                            }
                        }
                    }

                    outcome.eventLogs.forEach { log ->
                        database.eventLogDao().insertEvent(
                            EventLogEntity(
                                type = log.type,
                                meta = log.meta,
                                result = log.result,
                                createdAt = log.createdAt,
                            ),
                        )
                    }

                    val remainingSteps = player.bankedSteps - delta.stepsSpent
                    recordTravelCompleted(startedAt, segmentId, success = true)
                    return@withTransaction TravelResult.Arrived(
                        townId = delta.newTownId,
                        remainingSteps = remainingSteps,
                    )
                }
            }
        }
    }

    private fun recordTravelCompleted(
        startedAt: Long,
        segmentId: Long,
        success: Boolean,
    ) {
        TelemetryService.tryRecord(
            TelemetryEvent.TravelCompleted(
                timestamp = startedAt,
                segmentId = segmentId,
                latencyMs = System.currentTimeMillis() - startedAt,
                success = success,
            ),
        )
    }
}

sealed interface TravelResult {
    data class Arrived(
        val townId: Long,
        val remainingSteps: Long,
    ) : TravelResult

    data class NotEnoughSteps(
        val required: Long,
        val available: Long,
    ) : TravelResult

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
        completedTradesCount = completedTradesCount,
    )

private fun TownEntity.toModel(): Town =
    Town(
        townId = townId,
        name = name,
        region = region,
        biome = Biome.valueOf(biome),
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
    Instant
        .ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toEpochDay()

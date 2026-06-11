package com.wanderingledger.core.data

import androidx.room.withTransaction
import com.wanderingledger.core.database.EventLogEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import kotlinx.coroutines.flow.first

/**
 * Repository for resolving and logging road encounters.
 */
class EncounterRepository(
    private val database: WanderingLedgerDatabase,
    private val companionRepository: CompanionRepository,
) {
    /**
     * Resolve an encounter on the road.
     */
    suspend fun resolveRoadEncounter(
        seed: Long,
        encounterId: String,
    ): EncounterOutcome {
        val party = companionRepository.observeActiveCompanions().first()
        val outcome = EncounterEngine.resolve(seed, encounterId, party)

        database.withTransaction {
            val player = database.playerDao().getPlayerSnapshot() ?: return@withTransaction

            // Apply gold change
            if (outcome.goldChange != 0L) {
                database.playerDao().updatePlayer(
                    player.copy(gold = (player.gold + outcome.goldChange).coerceAtLeast(0)),
                )
            }

            // Apply bond changes
            if (outcome.bondChange != 0) {
                party.forEach { companion ->
                    companionRepository.updateBond(companion.companionId, outcome.bondChange)
                }
            }

            // Log the event
            database.eventLogDao().insertEvent(
                EventLogEntity(
                    type = "encounter",
                    meta =
                        "{\"encounterId\":\"$encounterId\",\"success\":${outcome.success}," +
                            "\"goldChange\":${outcome.goldChange}}",
                    result = outcome.resultText,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }

        return outcome
    }
}

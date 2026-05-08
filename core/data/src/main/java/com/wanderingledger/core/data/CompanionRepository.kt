package com.wanderingledger.core.data

import com.wanderingledger.core.database.CompanionEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Repository for managing companions and their relationships with the player.
 */
class CompanionRepository(
    private val database: WanderingLedgerDatabase,
) {
    /**
     * Observe the list of companions currently in the player's party.
     */
    fun observeActiveCompanions(): Flow<List<Companion>> =
        database.companionDao().listActiveCompanions().map { entities ->
            entities.map { it.toModel() }
        }

    /**
     * Observe the list of recruitable companions at a specific town.
     */
    fun observeRecruitableCompanionsAtTown(townId: Long): Flow<List<Companion>> =
        database.companionDao().listRecruitableCompanions().map { entities ->
            entities.filter { it.locationTownId == townId }.map { it.toModel() }
        }

    /**
     * Recruit a companion into the party.
     */
    suspend fun recruitCompanion(companionId: Long) {
        val recruitable = database.companionDao().listRecruitableCompanions().map { entities ->
            entities.find { it.companionId == companionId }
        }.firstOrNull() ?: return
        
        database.companionDao().upsertCompanion(
            recruitable.copy(isActive = true, questState = "recruited")
        )
    }

    /**
     * Increase or decrease the bond level with a companion.
     */
    suspend fun updateBond(companionId: Long, delta: Int) {
        val active = database.companionDao().listActiveCompanions().map { entities ->
            entities.find { it.companionId == companionId }
        }.firstOrNull() ?: return
        
        database.companionDao().upsertCompanion(
            active.copy(bondLevel = (active.bondLevel + delta).coerceIn(0, 100))
        )
    }
}

private fun CompanionEntity.toModel(): Companion =
    Companion(
        companionId = companionId,
        name = name,
        role = CompanionRole.valueOf(role),
        combatPower = combatPower,
        bondLevel = bondLevel,
        questState = questState,
        locationTownId = locationTownId,
        isActive = isActive,
    )

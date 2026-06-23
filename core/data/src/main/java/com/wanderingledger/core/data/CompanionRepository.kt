package com.wanderingledger.core.data

import com.wanderingledger.core.database.CompanionEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

const val MAX_ACTIVE_COMPANIONS = 3
const val MAX_BOND_LEVEL = 5

sealed class RecruitmentResult {
    data object Success : RecruitmentResult()

    data object AlreadyActive : RecruitmentResult()

    data object PartyFull : RecruitmentResult()

    data object NotFound : RecruitmentResult()
}

class CompanionRepository(
    private val database: WanderingLedgerDatabase,
) {
    fun observeActiveCompanions(): Flow<List<Companion>> =
        database.companionDao().listActiveCompanions().map { entities ->
            entities.map { it.toModel() }
        }

    fun observeRecruitableCompanionsAtTown(townId: Long): Flow<List<Companion>> =
        database.companionDao().listRecruitableCompanions().map { entities ->
            entities.filter { it.locationTownId == townId }.map { it.toModel() }
        }

    suspend fun recruitCompanion(companionId: Long): RecruitmentResult {
        val recruitable =
            database
                .companionDao()
                .listRecruitableCompanions()
                .map { entities ->
                    entities.find { it.companionId == companionId }
                }.firstOrNull() ?: return RecruitmentResult.NotFound

        val activeCount =
            database
                .companionDao()
                .listActiveCompanions()
                .firstOrNull()
                ?.size ?: 0
        if (activeCount >= MAX_ACTIVE_COMPANIONS) {
            return RecruitmentResult.PartyFull
        }

        database.companionDao().upsertCompanion(
            recruitable.copy(isActive = true, questState = "recruited", bondLevel = 0),
        )
        return RecruitmentResult.Success
    }

    suspend fun updateBond(
        companionId: Long,
        delta: Int,
    ) {
        val active =
            database
                .companionDao()
                .listActiveCompanions()
                .map { entities ->
                    entities.find { it.companionId == companionId }
                }.firstOrNull() ?: return

        database.companionDao().upsertCompanion(
            active.copy(bondLevel = (active.bondLevel + delta).coerceIn(0, MAX_BOND_LEVEL)),
        )
    }

    suspend fun dismissCompanion(companionId: Long) {
        val active =
            database
                .companionDao()
                .listActiveCompanions()
                .map { entities ->
                    entities.find { it.companionId == companionId }
                }.firstOrNull() ?: return

        database.companionDao().upsertCompanion(
            active.copy(isActive = false, questState = "dismissed"),
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

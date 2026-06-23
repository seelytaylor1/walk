package com.wanderingledger.core.steptracker

import kotlinx.coroutines.flow.Flow

interface StepBankRepository {
    fun observeStepBank(): Flow<Long>

    suspend fun recordDetectedSteps(
        count: Int,
        source: StepSource,
        recordedAt: Long = System.currentTimeMillis(),
    )

    suspend fun spendSteps(
        amount: Long,
        reason: String,
    ): StepSpendResult
}

data class StepSpendResult(
    val spent: Boolean,
    val requested: Long,
    val remaining: Long,
) {
    val shortfall: Long get() = (requested - remaining).coerceAtLeast(0)
}

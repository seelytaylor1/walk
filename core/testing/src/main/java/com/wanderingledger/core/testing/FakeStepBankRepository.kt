package com.wanderingledger.core.testing

import com.wanderingledger.core.steptracker.StepBankRepository
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepSpendResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStepBankRepository(initialSteps: Long = 0) : StepBankRepository {
    private val bankedSteps = MutableStateFlow(initialSteps)

    override fun observeStepBank(): Flow<Long> = bankedSteps

    override suspend fun recordDetectedSteps(count: Int, source: StepSource, recordedAt: Long) {
        if (count > 0) {
            bankedSteps.value += count
        }
    }

    override suspend fun spendSteps(amount: Long, reason: String): StepSpendResult {
        val current = bankedSteps.value
        if (current < amount) {
            return StepSpendResult(spent = false, requested = amount, remaining = current)
        }
        bankedSteps.value = current - amount
        return StepSpendResult(spent = true, requested = amount, remaining = bankedSteps.value)
    }
}

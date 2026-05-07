package com.wanderingledger.core.steptracker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StepTrackerServiceTest {
    private val repository = RecordingStepBankRepository()
    private val service = StepTrackerService(repository)

    @Test
    fun simulatedStepsIncreaseBank() = runTest {
        service.recordSensorDelta(75, StepSource.Simulation)

        assertEquals(75L, repository.observeStepBank().first())
        assertEquals(StepSource.Simulation, repository.lastSource)
    }

    @Test
    fun nonPositiveStepDeltaIsIgnored() = runTest {
        service.recordSensorDelta(0, StepSource.Simulation)
        service.recordSensorDelta(-5, StepSource.Simulation)

        assertEquals(0L, repository.observeStepBank().first())
        assertEquals(0, repository.recordCallCount)
    }
}

private class RecordingStepBankRepository : StepBankRepository {
    private val bankedSteps = MutableStateFlow(0L)

    var recordCallCount = 0
        private set
    var lastSource: StepSource? = null
        private set

    override fun observeStepBank(): Flow<Long> = bankedSteps

    override suspend fun recordDetectedSteps(count: Int, source: StepSource, recordedAt: Long) {
        recordCallCount += 1
        lastSource = source
        bankedSteps.value += count
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

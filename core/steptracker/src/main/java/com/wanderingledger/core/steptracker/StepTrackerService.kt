package com.wanderingledger.core.steptracker

class StepTrackerService(
    private val repository: StepBankRepository,
) {
    suspend fun recordSensorDelta(count: Int, source: StepSource = StepSource.Hardware) {
        if (count <= 0) return
        repository.recordDetectedSteps(count = count, source = source)
    }
}

package com.wanderingledger.core.steptracker

import com.wanderingledger.core.telemetry.StepAnomalyType
import com.wanderingledger.core.telemetry.TelemetryEvent
import com.wanderingledger.core.telemetry.TelemetryService
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

class StepTrackerService(
    private val repository: StepBankRepository,
) {
    private val lastTimestamps = ConcurrentHashMap<String, Long>()
    private val burstThreshold = 500

    suspend fun recordSensorDelta(count: Int, source: StepSource = StepSource.Hardware) {
        val now = System.currentTimeMillis()

        when {
            count < 0 -> {
                TelemetryService.tryRecord(
                    TelemetryEvent.StepAnomaly(
                        timestamp = now,
                        anomalyType = StepAnomalyType.NegativeDelta,
                        count = count,
                        details = "Negative step delta recorded from $source",
                    ),
                )
                return
            }
            count == 0 -> {
                TelemetryService.tryRecord(
                    TelemetryEvent.StepAnomaly(
                        timestamp = now,
                        anomalyType = StepAnomalyType.ZeroSteps,
                        count = count,
                        details = "Zero step delta from $source",
                    ),
                )
                return
            }
            count > burstThreshold -> {
                TelemetryService.tryRecord(
                    TelemetryEvent.StepAnomaly(
                        timestamp = now,
                        anomalyType = StepAnomalyType.ExcessiveBurst,
                        count = count,
                        details = "Step burst of $count exceeds threshold $burstThreshold from $source",
                    ),
                )
            }
        }

        val key = "$source-${now / 1000}"
        if (lastTimestamps.containsKey(key)) {
            TelemetryService.tryRecord(
                TelemetryEvent.StepAnomaly(
                    timestamp = now,
                    anomalyType = StepAnomalyType.DuplicateTimestamp,
                    count = count,
                    details = "Duplicate timestamp detected for $source within same second",
                ),
            )
        }
        lastTimestamps[key] = now
        cleanupOldTimestamps()

        repository.recordDetectedSteps(count = count, source = source)

        val bankedAfter = repository.observeStepBank().first()
        TelemetryService.tryRecord(
            TelemetryEvent.StepRecorded(
                timestamp = now,
                count = count,
                source = source.name,
                bankedStepsAfter = bankedAfter,
            ),
        )
    }

    private fun cleanupOldTimestamps() {
        val threshold = System.currentTimeMillis() - 5000
        lastTimestamps.entries.removeIf { it.value < threshold }
    }
}

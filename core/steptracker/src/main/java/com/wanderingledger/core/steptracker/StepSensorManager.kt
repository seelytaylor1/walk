package com.wanderingledger.core.steptracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.wanderingledger.core.telemetry.StepAnomalyType
import com.wanderingledger.core.telemetry.TelemetryEvent
import com.wanderingledger.core.telemetry.TelemetryService

interface StepSensorManager {
    val isHardwareStepCounterAvailable: Boolean

    fun preferredSource(): StepSource

    fun recordSteps(count: Int)
}

class AndroidStepSensorManager(
    context: Context,
) : StepSensorManager {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val isHardwareStepCounterAvailable: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    override fun preferredSource(): StepSource =
        if (isHardwareStepCounterAvailable) StepSource.Hardware else StepSource.MotionFallback

    override fun recordSteps(count: Int) {}
}

sealed class StepSensorState {
    data object HardwareAvailable : StepSensorState()

    data object MotionFallback : StepSensorState()

    data class Anomaly(
        val type: StepAnomalyType,
        val details: String,
    ) : StepSensorState()
}

class FallbackStepSensorManager(
    private val fallbackDetector: AccelStepDetector,
    private val repository: StepBankRepository,
) : StepSensorManager {
    override var isHardwareStepCounterAvailable: Boolean = false

    override fun preferredSource(): StepSource =
        if (isHardwareStepCounterAvailable) StepSource.Hardware else StepSource.MotionFallback

    override fun recordSteps(count: Int) {
        val now = System.currentTimeMillis()
        TelemetryService.tryRecord(
            TelemetryEvent.StepRecorded(
                timestamp = now,
                count = count,
                source = preferredSource().name,
                bankedStepsAfter = 0,
            ),
        )
    }
}

package com.wanderingledger.core.steptracker

import kotlin.math.sqrt

interface AccelStepDetector {
    fun processSample(
        timestampNs: Long,
        ax: Float,
        ay: Float,
        az: Float,
    ): Int

    fun reset()
}

class PeakDetectionStepDetector(
    private val sensitivity: Float = 1.0f,
    private val minStepIntervalMs: Long = 250,
    private val peakThreshold: Float = 1.2f,
) : AccelStepDetector {
    private var lastStepTimeNs: Long = 0L
    private var lastDeviation: Float = 0f
    private var movingAverage: Float = 9.81f
    private var stepsDetected: Int = 0

    private val alpha: Float = 0.05f

    override fun processSample(
        timestampNs: Long,
        ax: Float, ay: Float, az: Float
    ): Int {
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        movingAverage = alpha * magnitude + (1f - alpha) * movingAverage
        val deviation = magnitude - movingAverage

        val threshold = peakThreshold * sensitivity
        if (deviation > threshold && lastDeviation <= threshold) {
            val elapsedMs = (timestampNs - lastStepTimeNs) / 1_000_000
            if (lastStepTimeNs == 0L || elapsedMs >= minStepIntervalMs) {
                stepsDetected++
                lastStepTimeNs = timestampNs
            }
        }

        lastDeviation = deviation
        return stepsDetected
    }

    override fun reset() {
        lastStepTimeNs = 0L
        lastDeviation = 0f
        movingAverage = 9.81f
        stepsDetected = 0
    }
}

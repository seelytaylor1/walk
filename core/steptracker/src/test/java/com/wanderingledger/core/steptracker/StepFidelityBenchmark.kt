package com.wanderingledger.core.steptracker

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fidelity test harness for SC-002.
 *
 * Acceptance threshold (research.md §3 / plan.md):
 *   F1 score ≥ 0.95 for typical walking speeds (0.8–1.6 m/s).
 *
 * Methodology:
 *   1. Generate synthetic accelerometer traces with known step timestamps
 *      at multiple walking speeds (0.8, 1.0, 1.2, 1.5, 1.6 m/s).
 *   2. Also test edge cases: very slow walking, very fast walking,
 *      stationary noise, and sustained motion without steps.
 *   3. Run each trace through [PeakDetectionStepDetector] and collect
 *      detected step counts.
 *   4. Compute precision, recall, and F1 for each trace and aggregate.
 *
 * A "synthetic trace" simulates the vertical acceleration pattern of walking:
 *   a_z ≈ g + A·sin(2π·strideFreq·t)  where A ≈ 2–4 m/s²
 * X/Y axes carry small ambient noise.
 * Ground-truth step timestamps are marked at each stride peak.
 *
 * The harness is a plain JVM test — no Android, Room, or sensors required.
 */
class StepFidelityBenchmark {
    data class TraceResult(
        val label: String,
        val groundTruth: Int,
        val detected: Int,
        val precision: Double,
        val recall: Double,
        val f1: Double,
    )

    data class BenchmarkSummary(
        val results: List<TraceResult>,
        val overallPrecision: Double,
        val overallRecall: Double,
        val overallF1: Double,
    )

    private val random = Random(42)

    fun runAllTraces(sensitivity: Float = 1.0f): BenchmarkSummary {
        val results = mutableListOf<TraceResult>()

        val speedBuckets =
            listOf(
                0.8 to "very_slow",
                1.0 to "slow",
                1.2 to "normal",
                1.5 to "fast",
                1.6 to "very_fast",
                0.4 to "edge_slow",
                2.0 to "edge_fast",
                0.0 to "stationary_noise",
                -1.0 to "no_steps_sustained",
            )

        for ((speed, label) in speedBuckets) {
            val trace = generateSyntheticTrace(speed = speed, label = label)
            val detected = runDetector(trace, sensitivity)
            val result =
                computeMetrics(label = "$label (${speed}m/s)", groundTruth = trace.steps.size, detected = detected)
            results.add(result)
        }

        val allGroundTruth = results.sumOf { it.groundTruth }
        val allDetected = results.sumOf { it.detected }
        val tp =
            results.zip(results).sumOf { (r, _) ->
                minOf(r.groundTruth, r.detected)
            }
        val fp = (allDetected - tp).coerceAtLeast(0)
        val fn = (allGroundTruth - tp).coerceAtLeast(0)

        val overallP = if (allDetected > 0) tp.toDouble() / allDetected else 0.0
        val overallR = if (allGroundTruth > 0) tp.toDouble() / allGroundTruth else 0.0
        val overallF1 = if (overallP + overallR > 0) 2 * overallP * overallR / (overallP + overallR) else 0.0

        return BenchmarkSummary(
            results = results,
            overallPrecision = overallP,
            overallRecall = overallR,
            overallF1 = overallF1,
        )
    }

    fun runTraceAtSpeed(
        speed: Double,
        sensitivity: Float = 1.0f,
    ): TraceResult {
        val trace = generateSyntheticTrace(speed = speed, label = "${speed}m_s")
        val detected = runDetector(trace, sensitivity)
        return computeMetrics(label = "speed_$speed", groundTruth = trace.steps.size, detected = detected)
    }

    private fun runDetector(
        trace: SyntheticTrace,
        sensitivity: Float,
    ): Int {
        val detector = PeakDetectionStepDetector(sensitivity = sensitivity)
        var lastCount = 0
        for (sample in trace.samples) {
            val count =
                detector.processSample(
                    timestampNs = sample.timestampNs,
                    ax = sample.ax,
                    ay = sample.ay,
                    az = sample.az,
                )
            lastCount = count
        }
        return lastCount
    }

    private fun computeMetrics(
        label: String,
        groundTruth: Int,
        detected: Int,
    ): TraceResult {
        val tp = minOf(groundTruth, detected)
        val fp = (detected - tp).coerceAtLeast(0)
        val fn = (groundTruth - tp).coerceAtLeast(0)

        val precision = if (detected > 0) tp.toDouble() / detected else 0.0
        val recall = if (groundTruth > 0) tp.toDouble() / groundTruth else 0.0
        val f1 = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0

        return TraceResult(
            label = label,
            groundTruth = groundTruth,
            detected = detected,
            precision = precision,
            recall = recall,
            f1 = f1,
        )
    }

    private data class SyntheticTrace(
        val samples: List<AccelSample>,
        val steps: List<Long>,
    )

    private data class AccelSample(
        val timestampNs: Long,
        val ax: Float,
        val ay: Float,
        val az: Float,
    )

    private fun generateSyntheticTrace(
        speed: Double,
        label: String,
    ): SyntheticTrace {
        val samples = mutableListOf<AccelSample>()
        val groundTruthSteps = mutableListOf<Long>()

        if (speed <= 0.0) {
            var tNs = 0L
            repeat(500) {
                val ax = (random.nextGaussian() * 0.1).toFloat()
                val ay = (random.nextGaussian() * 0.1).toFloat()
                val az = (9.81f + random.nextGaussian() * 0.15).toFloat()
                samples.add(AccelSample(tNs, ax, ay, az))
                tNs += 20_000_000
            }
            return SyntheticTrace(samples, groundTruthSteps)
        }

        val strideFreqHz = speed / 0.75
        val durationSec = 60.0
        val sampleRateHz = 50
        val sampleIntervalNs = (1_000_000_000L / sampleRateHz)
        val amplitude = (2.5f + (speed / 2.0).toFloat()).coerceIn(2.0f, 4.5f)

        var tNs = 0L
        var stepPhase = 0.0
        val samplesPerStep = sampleRateHz / strideFreqHz
        var stepsEmitted = 0

        val totalSamples = (durationSec * sampleRateHz).toInt()
        for (i in 0 until totalSamples) {
            val tSec = tNs / 1_000_000_000.0

            val az = (9.81 + amplitude * sin(2.0 * PI * strideFreqHz * tSec)).toFloat()
            val ay = (random.nextGaussian() * 0.2).toFloat()
            val ax = (random.nextGaussian() * 0.15).toFloat()

            samples.add(AccelSample(tNs, ax, ay, az))

            if (i > 0 && i % samplesPerStep.roundToInt() == 0) {
                groundTruthSteps.add(tNs)
                stepsEmitted++
            }

            tNs += sampleIntervalNs
        }

        return SyntheticTrace(samples, groundTruthSteps)
    }

    private fun Random.nextGaussian(): Double {
        var u = 0.0
        var v = 0.0
        while (u == 0.0) u = nextDouble()
        while (v == 0.0) v = nextDouble()
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u)) * kotlin.math.cos(2.0 * PI * v)
    }
}

package com.wanderingledger.core.steptracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepFidelityBenchmarkTest {
    private val harness = StepFidelityBenchmark()
    private val f1Threshold = 0.95

    @Test
    fun sc002_fidelity_passes_at_all_normal_walking_speeds() {
        val speeds = listOf(0.8, 1.0, 1.2, 1.5, 1.6)

        for (speed in speeds) {
            val result = harness.runTraceAtSpeed(speed = speed, sensitivity = 1.0f)
            println(
                "Speed ${speed}m/s → ground=${result.groundTruth}, detected=${result.detected}, F1=${"%.3f".format(
                    result.f1,
                )}",
            )

            assertTrue(
                "F1 at ${speed}m/s (${"%.2f".format(
                    result.f1,
                )}) below threshold $f1Threshold: ground=${result.groundTruth}, detected=${result.detected}",
                result.f1 >= f1Threshold,
            )
        }
    }

    @Test
    fun sc002_overall_f1_meets_threshold_across_all_speeds() {
        val summary = harness.runAllTraces(sensitivity = 1.0f)

        println("\nSC-002 Fidelity Benchmark Summary:")
        for (r in summary.results) {
            val pass = if (r.f1 >= f1Threshold) "PASS" else "FAIL"
            println(
                "  [$pass] ${r.label}: GT=${r.groundTruth}, D=${r.detected}, P=${"%.3f".format(
                    r.precision,
                )}, R=${"%.3f".format(r.recall)}, F1=${"%.3f".format(r.f1)}",
            )
        }
        println(
            "  Overall → P=${"%.3f".format(
                summary.overallPrecision,
            )}, R=${"%.3f".format(summary.overallRecall)}, F1=${"%.3f".format(summary.overallF1)}",
        )

        assertTrue(
            "SC-002 FAILED: overall F1 ${"%.3f".format(summary.overallF1)} < $f1Threshold",
            summary.overallF1 >= f1Threshold,
        )
    }

    @Test
    fun sc002_detector_sensitivity_bandwidth() {
        for (sensitivity in listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)) {
            val result = harness.runTraceAtSpeed(speed = 1.2, sensitivity = sensitivity)
            println(
                "sensitivity=$sensitivity → F1=${"%.3f".format(
                    result.f1,
                )}, detected=${result.detected}, GT=${result.groundTruth}",
            )
            assertTrue("F1 should remain above 0.90 for sensitivity=$sensitivity", result.f1 >= 0.90)
        }
    }

    @Test
    fun sc002_stationary_noise_yields_minimal_false_positives() {
        val result = harness.runTraceAtSpeed(speed = 0.0, sensitivity = 1.0f)
        println("Stationary noise: GT=${result.groundTruth}, detected=${result.detected}")
        assertEquals("No steps expected while stationary", 0, result.groundTruth)
        assertTrue("False positives during noise should be very low", result.detected <= 3)
    }

    @Test
    fun sc002_sustained_motion_without_steps_yields_low_false_positives() {
        val result = harness.runTraceAtSpeed(speed = -1.0, sensitivity = 1.0f)
        println("Sustained motion (no steps): GT=${result.groundTruth}, detected=${result.detected}")
        assertEquals("Ground truth should be 0", 0, result.groundTruth)
        assertTrue("False positives should be minimal", result.detected <= 5)
    }

    @Test
    fun sc002_edge_slow_walking_acceptable() {
        val result = harness.runTraceAtSpeed(speed = 0.4, sensitivity = 1.0f)
        println("Very slow walk: GT=${result.groundTruth}, detected=${result.detected}, F1=${"%.3f".format(result.f1)}")
        assertTrue("Slow walk F1 should be >= 0.80", result.f1 >= 0.80)
    }

    @Test
    fun sc002_edge_fast_walking_acceptable() {
        val result = harness.runTraceAtSpeed(speed = 2.0, sensitivity = 1.0f)
        println("Fast walk: GT=${result.groundTruth}, detected=${result.detected}, F1=${"%.3f".format(result.f1)}")
        assertTrue("Fast walk F1 should be >= 0.80", result.f1 >= 0.80)
    }
}

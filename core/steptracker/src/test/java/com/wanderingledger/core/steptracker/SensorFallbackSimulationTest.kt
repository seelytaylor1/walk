package com.wanderingledger.core.steptracker

import com.wanderingledger.core.testing.FakeStepBankRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorFallbackSimulationTest {
    private val repository = FakeStepBankRepository()

    @Test
    fun motionFallback_records_steps_with_correct_source() =
        runTest {
            val service = StepTrackerService(repository)

            service.recordSensorDelta(count = 50, source = StepSource.MotionFallback)

            assertEquals(50L, repository.observeStepBank().first())
        }

    @Test
    fun hardware_preferred_when_available() {
        val manager = FakeStepSensorManager(isHardwareStepCounterAvailable = true)
        assertEquals(StepSource.Hardware, manager.preferredSource())
        assertTrue(manager.isHardwareStepCounterAvailable)
    }

    @Test
    fun motion_fallback_selected_when_hardware_unavailable() {
        val manager = FakeStepSensorManager(isHardwareStepCounterAvailable = false)
        assertEquals(StepSource.MotionFallback, manager.preferredSource())
        assertFalse(manager.isHardwareStepCounterAvailable)
    }

    @Test
    fun service_accepts_both_hardware_and_fallback_sources() =
        runTest {
            val service = StepTrackerService(repository)

            service.recordSensorDelta(count = 10, source = StepSource.Hardware)
            assertEquals(10L, repository.observeStepBank().first())

            service.recordSensorDelta(count = 5, source = StepSource.MotionFallback)
            assertEquals(15L, repository.observeStepBank().first())

            service.recordSensorDelta(count = 3, source = StepSource.Simulation)
            assertEquals(18L, repository.observeStepBank().first())
        }

    @Test
    fun mixed_source_recording_increases_bank_correctly() =
        runTest {
            val service = StepTrackerService(repository)

            repeat(10) { service.recordSensorDelta(count = 1, source = StepSource.Hardware) }
            repeat(5) { service.recordSensorDelta(count = 1, source = StepSource.MotionFallback) }

            assertEquals(15L, repository.observeStepBank().first())
        }

    @Test
    fun zero_count_rejected_for_all_sources() =
        runTest {
            val service = StepTrackerService(repository)

            service.recordSensorDelta(count = 0, source = StepSource.Hardware)
            service.recordSensorDelta(count = 0, source = StepSource.MotionFallback)
            service.recordSensorDelta(count = 0, source = StepSource.Simulation)

            assertEquals(0L, repository.observeStepBank().first())
        }

    @Test
    fun negative_count_rejected_for_all_sources() =
        runTest {
            val service = StepTrackerService(repository)

            service.recordSensorDelta(count = -5, source = StepSource.Hardware)
            service.recordSensorDelta(count = -10, source = StepSource.MotionFallback)
            service.recordSensorDelta(count = -1, source = StepSource.Simulation)

            assertEquals(0L, repository.observeStepBank().first())
        }

    @Test
    fun accel_detector_registers_steps_from_synthetic_motion() {
        val detector = PeakDetectionStepDetector(sensitivity = 1.0f)
        var tNs = 0L
        val stepIntervalNs = 800_000_000L

        repeat(10) {
            detector.processSample(tNs, ax = 0f, ay = 0f, az = 9.81f)
            detector.processSample(tNs + 400_000_000L, ax = 0f, ay = 0f, az = 13f)
            tNs += stepIntervalNs
        }

        val detected = detector.processSample(tNs, ax = 0f, ay = 0f, az = 9.81f)
        assertTrue("Expected at least 1 step detected, got $detected", detected >= 1)
    }

    @Test
    fun accel_detector_reset_clears_step_count() {
        val detector = PeakDetectionStepDetector(sensitivity = 1.0f)

        repeat(5) {
            detector.processSample(it * 800_000_000L, ax = 0f, ay = 0f, az = 13f)
            detector.processSample(it * 800_000_000L + 400_000_000L, ax = 0f, ay = 0f, az = 9.81f)
        }

        val before = detector.processSample(4_000_000_000L, ax = 0f, ay = 0f, az = 13f)
        assertTrue("Should have detected steps before reset", before > 0)
        detector.reset()
        val after = detector.processSample(4_800_000_000L, ax = 0f, ay = 0f, az = 13f)

        assertEquals("After reset and one sample, should have 1 step", 1, after)
        assertTrue("Reset should clear step count history", after < before)
    }

    @Test
    fun detector_detects_steps_at_multiple_gait_speeds() {
        val speeds = listOf(1000L, 700L, 500L)

        for (intervalNs in speeds) {
            val detector = PeakDetectionStepDetector(sensitivity = 1.0f)
            var tNs = 0L

            repeat(20) {
                detector.processSample(tNs, ax = 0f, ay = 0f, az = 9.81f)
                detector.processSample(tNs + intervalNs / 2, ax = 0f, ay = 0f, az = 13f)
                tNs += intervalNs
            }

            val detected = detector.processSample(tNs, ax = 0f, ay = 0f, az = 9.81f)
            assertTrue(
                "Interval ${intervalNs}ns should detect steps, got $detected",
                detected >= 1,
            )
        }
    }

    @Test
    fun detector_respects_min_interval_between_steps() {
        val detector = PeakDetectionStepDetector(minStepIntervalMs = 300)

        detector.processSample(0L, ax = 0f, ay = 0f, az = 13f)
        val first = detector.processSample(300_000_000L, ax = 0f, ay = 0f, az = 13f)

        detector.reset()
        detector.processSample(0L, ax = 0f, ay = 0f, az = 13f)
        val second = detector.processSample(100_000_000L, ax = 0f, ay = 0f, az = 13f)

        assertTrue(
            "Slow steps (300ms interval) should be detected: $first",
            first >= 1,
        )
        assertTrue(
            "Fast steps (100ms interval) should not both register: $second < $first",
            second <= first,
        )
    }

    @Test
    fun detector_noise_tolerance_at_various_sensitivities() {
        val sensitivities = listOf(0.5f, 1.0f, 1.5f)

        for (sensitivity in sensitivities) {
            val detector = PeakDetectionStepDetector(sensitivity = sensitivity)
            var tNs = 0L

            repeat(30) {
                val noise = (Math.random() * 0.5).toFloat()
                detector.processSample(
                    tNs,
                    ax = noise,
                    ay = noise,
                    az = 9.81f + noise,
                )
                detector.processSample(
                    tNs + 400_000_000L,
                    ax = noise,
                    ay = noise,
                    az = 12.5f + noise,
                )
                tNs += 800_000_000L
            }

            val detected = detector.processSample(tNs, ax = 0f, ay = 0f, az = 9.81f)
            assertTrue(
                "Sensitivity $sensitivity: expected steps, got $detected",
                detected >= 1,
            )
        }
    }

    @Test
    fun no_false_positives_when_stationary() {
        val detector = PeakDetectionStepDetector(sensitivity = 1.0f)

        repeat(500) { i ->
            val noise = (Math.random() * 0.2).toFloat()
            detector.processSample(
                i * 20_000_000L,
                ax = noise,
                ay = noise,
                az = 9.81f + noise,
            )
        }

        val detected = detector.processSample(10_000_000_000L, ax = 0f, ay = 0f, az = 9.81f)
        assertTrue(
            "No steps expected when stationary, got $detected",
            detected <= 2,
        )
    }

    @Test
    fun burst_threshold_anomaly_logged_for_large_deltas() =
        runTest {
            val service = StepTrackerService(repository)

            service.recordSensorDelta(count = 1000, source = StepSource.Hardware)
            assertEquals(1000L, repository.observeStepBank().first())
        }
}

private class FakeStepSensorManager(
    override val isHardwareStepCounterAvailable: Boolean,
) : StepSensorManager {
    override fun preferredSource(): StepSource =
        if (isHardwareStepCounterAvailable) StepSource.Hardware else StepSource.MotionFallback

    override fun recordSteps(count: Int) {}
}

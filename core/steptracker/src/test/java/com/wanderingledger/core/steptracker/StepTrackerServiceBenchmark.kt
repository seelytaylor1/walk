package com.wanderingledger.core.steptracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

class StepTrackerServiceBenchmark {
    @Test
    fun benchmarkSingleStepRecordingLatency() {
        val repository = BenchmarkStepBankRepository()
        val service = StepTrackerService(repository)

        val iterations = 1000
        val totalNs =
            measureNanoTime {
                runBlocking {
                    repeat(iterations) {
                        service.recordSensorDelta(10, StepSource.Hardware)
                    }
                }
            }

        val avgLatencyUs = (totalNs / iterations) / 1000.0
        println("Average single-step recording latency: ${"%.2f".format(avgLatencyUs)} μs")
        assertTrue("Latency should be under 5000μs", avgLatencyUs < 5000)
    }

    @Test
    fun benchmarkBurstStepRecording() {
        val repository = BenchmarkStepBankRepository()
        val service = StepTrackerService(repository)

        val burstSize = 500
        val iterations = 100

        val totalNs =
            measureNanoTime {
                runBlocking {
                    repeat(iterations) {
                        service.recordSensorDelta(burstSize, StepSource.Hardware)
                    }
                }
            }

        val avgLatencyUs = (totalNs / iterations) / 1000.0
        println("Average burst ($burstSize steps) recording latency: ${"%.2f".format(avgLatencyUs)} μs")
        assertTrue("Burst latency should be under 10000μs", avgLatencyUs < 10000)
    }

    @Test
    fun benchmarkHighFrequencyStepRecording() {
        val repository = BenchmarkStepBankRepository()
        val service = StepTrackerService(repository)

        val totalSteps = 10000
        val startTime = System.currentTimeMillis()

        runBlocking {
            repeat(totalSteps) { i ->
                service.recordSensorDelta(1, StepSource.Hardware)
            }
        }

        val elapsedMs = System.currentTimeMillis() - startTime
        val throughput = totalSteps.toDouble() / (elapsedMs / 1000.0)
        println("High-frequency throughput: ${"%.0f".format(throughput)} steps/sec")
        println("Total time for $totalSteps steps: ${elapsedMs}ms")
        assertTrue("Should process at least 100 steps/sec", throughput > 100)
    }

    @Test
    fun benchmarkSequentialStepRecording() {
        val repository = BenchmarkStepBankRepository()
        val service = StepTrackerService(repository)

        val sources = listOf(StepSource.Hardware, StepSource.Simulation, StepSource.MotionFallback)
        val totalSteps = 3000

        val totalNs =
            measureNanoTime {
                runBlocking {
                    sources.forEach { source ->
                        repeat(totalSteps / sources.size) {
                            service.recordSensorDelta(1, source)
                        }
                    }
                }
            }

        val avgLatencyNs = totalNs / totalSteps
        println("Sequential multi-source recording avg latency: ${avgLatencyNs / 1000.0} μs")
        assertTrue("Sequential latency should be under 5000μs", avgLatencyNs / 1000.0 < 5000)
    }

    @Test
    fun benchmarkMemoryAllocation() {
        val repository = BenchmarkStepBankRepository()
        val service = StepTrackerService(repository)

        val iterations = 10000
        val beforeMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        runBlocking {
            repeat(iterations) {
                service.recordSensorDelta(10, StepSource.Simulation)
            }
        }

        val afterMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val deltaBytes = afterMem - beforeMem
        val bytesPerCall = deltaBytes.toDouble() / iterations

        println("Memory per step recording: ${"%.2f".format(bytesPerCall)} bytes")
        println("Total memory delta: ${deltaBytes / 1024} KB for $iterations iterations")
    }

    @Test
    fun benchmarkAnomalyDetection() {
        val repository = BenchmarkStepBankRepository()
        val service = StepTrackerService(repository)

        val negativeCount = 1000
        val totalNs =
            measureNanoTime {
                runBlocking {
                    repeat(negativeCount) {
                        service.recordSensorDelta(-1, StepSource.Hardware)
                    }
                }
            }

        val avgLatencyUs = (totalNs / negativeCount) / 1000.0
        println("Anomaly detection latency: ${"%.2f".format(avgLatencyUs)} μs")
        assertTrue("Anomaly detection should be under 2000μs", avgLatencyUs < 2000)
    }

    @Test
    fun benchmarkRepositoryObserveFlow() {
        val repository = BenchmarkStepBankRepository()
        val service = StepTrackerService(repository)

        runBlocking {
            repeat(100) {
                service.recordSensorDelta(10, StepSource.Hardware)
            }
        }

        val observeIterations = 10000
        val totalNs =
            measureNanoTime {
                runBlocking {
                    repeat(observeIterations) {
                        repository.observeStepBank().first()
                    }
                }
            }

        val avgLatencyUs = (totalNs / observeIterations) / 1000.0
        println("Flow observation avg latency: ${"%.2f".format(avgLatencyUs)} μs")
    }
}

private class BenchmarkStepBankRepository : StepBankRepository {
    private val bankedSteps = MutableStateFlow(0L)
    private val stepCount = AtomicLong(0)

    override fun observeStepBank() = bankedSteps

    override suspend fun recordDetectedSteps(
        count: Int,
        source: StepSource,
        recordedAt: Long,
    ) {
        withContext(Dispatchers.IO) {
            stepCount.incrementAndGet()
            bankedSteps.value += count
        }
    }

    override suspend fun spendSteps(
        amount: Long,
        reason: String,
    ): StepSpendResult {
        val current = bankedSteps.value
        if (current < amount) {
            return StepSpendResult(spent = false, requested = amount, remaining = current)
        }
        bankedSteps.value = current - amount
        return StepSpendResult(spent = true, requested = amount, remaining = bankedSteps.value)
    }
}

package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.math.ceil

/**
 * Latency benchmark harness for SC-001.
 *
 * Acceptance criteria (research.md §3 / plan.md):
 *   - median (p50) travel-action completion ≤ 3 s on mid/high devices
 *   - 95th percentile (p95) ≤ 10 s on mid/high devices
 *
 * Methodology:
 *   1. Seed world once; record available road segment IDs and their step costs.
 *   2. For each iteration: record enough simulated steps to afford travel,
 *      invoke `GameRepository.travel(segmentId)`, measure wall-clock time end-to-end.
 *   3. Repeat N times; compute p50 and p95 from the collected latencies.
 *   4. Assert pass/fail against the thresholds above.
 *
 * The harness is intentionally end-to-end (repository + Room + all business
 * logic) so it captures the full stack cost of a travel action.
 * It does NOT require a real device or sensor — all step input is simulated.
 */
@RunWith(RobolectricTestRunner::class)
class TravelLatencyBenchmarkTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    private var segmentId: Long = -1
    private var stepCost: Int = 0

    @Before
    fun setup() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = TestDatabaseFactory.createInMemoryDatabase(context)
            val rumorRepository = RumorRepository(database)
            gameRepository = GameRepository(database, rumorRepository, CompanionRepository(database), OrderRepository(database))
            stepBankRepository = RoomStepBankRepository(database)
            stepTrackerService = StepTrackerService(stepBankRepository)

            gameRepository.initializeNewGame(seed = 1L)

            val roads = gameRepository.observeRoadsFromCurrentTown().first()
            val road = roads.first { it.fromTownId == 1L }
            segmentId = road.segmentId
            stepCost = road.stepCost
        }

    @After
    fun tearDown() {
        database.close()
    }

    private fun runTravelIteration(stepsToRecord: Long): LatencyResult {
        runBlocking {
            stepTrackerService.recordSensorDelta(
                count = stepsToRecord.toInt(),
                source = StepSource.Simulation,
            )
        }

        val startedAt = System.currentTimeMillis()
        val result = runBlocking { gameRepository.travel(segmentId) }
        val endedAt = System.currentTimeMillis()

        return LatencyResult(
            latencyMs = endedAt - startedAt,
            travelResult = result,
        )
    }

    private data class LatencyResult(
        val latencyMs: Long,
        val travelResult: TravelResult,
    )

    private fun computePercentile(
        sorted: LongArray,
        percentile: Double,
    ): Long {
        require(sorted.isNotEmpty())
        val idx = ceil(sorted.size * percentile / 100.0).toInt() - 1
        return sorted[idx.coerceIn(0, sorted.size - 1)]
    }

    @Test
    fun sc001_travel_latency_p50_under_3s_on_warm_setup() =
        runTest {
            val iterations = 100
            val latencies = AtomicLongArray(iterations)

            repeat(iterations) { i ->
                val result = runTravelIteration(stepsToRecord = stepCost.toLong() + 10)
                latencies[i] = result.latencyMs

                val player = gameRepository.observePlayerState().first()
                if (player.currentTownId == 2L) {
                    gameRepository.travel(segmentId = 1L)
                }
            }

            val sorted = LongArray(iterations) { latencies[it] }.sorted().toLongArray()
            val p50 = computePercentile(sorted, 50.0)
            val p95 = computePercentile(sorted, 95.0)

            println("SC-001 Travel Latency Results (n=$iterations):")
            println("  p50 = ${p50}ms (threshold: 3000ms)")
            println("  p95 = ${p95}ms (threshold: 10000ms)")
            println("  min = ${sorted.first()}ms | max = ${sorted.last()}ms")

            assertTrue(
                "SC-001 FAILED: p50 latency ${p50}ms exceeds 3000ms threshold",
                p50 <= 3000,
            )
            assertTrue(
                "SC-001 FAILED: p95 latency ${p95}ms exceeds 10000ms threshold",
                p95 <= 10000,
            )
        }

    @Test
    fun sc001_travel_latency_meets_throughput_floor() =
        runTest {
            val totalTravels = 500
            val stepsPerTrip = stepCost.toLong() + 5

            val startedAt = System.currentTimeMillis()
            repeat(totalTravels) { i ->
                runBlocking {
                    stepTrackerService.recordSensorDelta(
                        count = stepsPerTrip.toInt(),
                        source = StepSource.Simulation,
                    )
                }
                val result = runBlocking { gameRepository.travel(segmentId) }
                val player = gameRepository.observePlayerState().first()
                if (player.currentTownId == 2L) {
                    runBlocking { gameRepository.travel(segmentId = 1L) }
                }
            }
            val totalMs = System.currentTimeMillis() - startedAt

            val avgLatencyMs = totalMs.toDouble() / totalTravels
            val throughputPerSec = totalTravels.toDouble() / (totalMs / 1000.0)

            println("SC-001 Throughput Benchmark:")
            println("  total travels = $totalTravels")
            println("  total wall time = ${totalMs}ms")
            println("  avg latency = ${"%.1f".format(avgLatencyMs)}ms")
            println("  throughput = ${"%.1f".format(throughputPerSec)} travels/sec")

            assertTrue(
                "Throughput too low: avg ${"%.1f".format(avgLatencyMs)}ms per travel",
                avgLatencyMs < 1000,
            )
        }

    @Test
    fun sc001_travel_latency_consistent_across_cold_start() =
        runTest {
            val runs = 10
            val iterationPerRun = 20
            val results = mutableListOf<Long>()

            repeat(runs) {
                val localDatabase =
                    TestDatabaseFactory.createInMemoryDatabase(
                        ApplicationProvider.getApplicationContext(),
                    )
                val localRumorRepo = RumorRepository(localDatabase)
                val localGameRepo =
                    GameRepository(localDatabase, localRumorRepo, CompanionRepository(localDatabase), OrderRepository(localDatabase))
                val localStepBank = RoomStepBankRepository(localDatabase)
                val localService = StepTrackerService(localStepBank)

                localGameRepo.initializeNewGame(seed = System.currentTimeMillis())

                val roads = localGameRepo.observeRoadsFromCurrentTown().first()
                val localSegment = roads.first { it.fromTownId == 1L }.segmentId
                val localCost = roads.first { it.fromTownId == 1L }.stepCost

                repeat(iterationPerRun) {
                    runBlocking {
                        localService.recordSensorDelta(
                            count = localCost + 5,
                            source = StepSource.Simulation,
                        )
                    }
                    val startedAt = System.currentTimeMillis()
                    val result = runBlocking { localGameRepo.travel(localSegment) }
                    val latency = System.currentTimeMillis() - startedAt
                    results.add(latency)

                    val player = localGameRepo.observePlayerState().first()
                    if (player.currentTownId == 2L) {
                        runBlocking { localGameRepo.travel(segmentId = 1L) }
                    }
                }
                localDatabase.close()
            }

            val sorted = results.sorted().toLongArray()
            val p50 = computePercentile(sorted, 50.0)
            val p95 = computePercentile(sorted, 95.0)

            println("SC-001 Cold-Start Consistency ($runs runs × $iterationPerRun = ${results.size} samples):")
            println("  p50 = ${p50}ms | p95 = ${p95}ms")
            println("  min = ${sorted.first()}ms | max = ${sorted.last()}ms")

            assertTrue("p50 cold-start latency ${p50}ms exceeds 3000ms", p50 <= 3000)
            assertTrue("p95 cold-start latency ${p95}ms exceeds 10000ms", p95 <= 10000)
        }
}

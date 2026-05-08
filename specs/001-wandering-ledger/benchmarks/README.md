# Benchmarks: Wandering Ledger

## SC-001: Travel Latency

See `core/data/src/test/java/com/wanderingledger/core/data/TravelLatencyBenchmarkTest.kt`.

**Acceptance thresholds** (from `research.md` §3):
- p50 ≤ 3000 ms on mid/high devices
- p95 ≤ 10000 ms on mid/high devices

**Test classes**:
- `TravelLatencyBenchmarkTest` — end-to-end latency and throughput benchmarks via Robolectric

## SC-002: Step Detection Fidelity

See `core/steptracker/src/test/java/com/wanderingledger/core/steptracker/StepFidelityBenchmarkTest.kt`.

**Acceptance threshold** (from `research.md` §3):
- F1 ≥ 0.95 for typical walking speeds (0.8–1.6 m/s)

**Test classes**:
- `StepFidelityBenchmark` — synthetic accelerometer trace generator + peak-detection detector harness
- `StepFidelityBenchmarkTest` — JUnit test suite asserting F1 thresholds across speed buckets and edge cases

**CI smoke job**: `android-ci.yml` → `smoke` job runs the fidelity suite.
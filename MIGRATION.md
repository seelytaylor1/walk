# Migration Notes

This document covers migration guides for breaking changes between releases.

## Upgrading to 0.2.0

### New Dependency: EncounterRepository

The `GameRepository` constructor signature has changed. It now requires an `EncounterRepository` parameter.

**Before (0.1.0):**
```kotlin
gameRepository = GameRepository(database, rumorRepository)
```

**After (0.2.0):**
```kotlin
encounterRepository = EncounterRepository(database, companionRepository)
gameRepository = GameRepository(database, rumorRepository, encounterRepository)
```

### New Module: core:telemetry

The telemetry module was added. If you have custom implementations of `StepTrackerService` or `MarketRepository`, you may want to integrate telemetry hooks.

**Step Anomalies Detected:**
- Negative step delta
- Excessive burst (>500 steps in single call)
- Zero steps
- Duplicate timestamp within same second

**Market Anomalies Detected:**
- Price spike (>50% increase)
- Price crash (>30% decrease)
- Supply depleted (moved to Scarce)
- Unusual volume (>100% deviation from base value)

### Benchmark Tests

New benchmark tests are available:
```bash
./gradlew :core:steptracker:test
```

These measure:
- Single-step recording latency
- Burst step recording
- High-frequency throughput
- Memory allocation
- Anomaly detection performance
- Flow observation latency

## Upgrading to 0.1.0

This was the initial release with the first playable milestone. No migration notes needed for initial release.
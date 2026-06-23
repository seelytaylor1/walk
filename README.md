# Wandering Ledger

An offline Android walking game where real-world steps become travel currency in a hand-authored trade world.

## Overview

Walk, bank steps, spend steps to travel between towns, buy and sell goods, collect rumors, and recruit companions. All progress persists locally.

## Requirements

- JDK 17
- Android Studio with Android SDK 34
- Android API 26+ emulator or device

## Quick Start

1. Open the repo root in Android Studio
2. Let Gradle sync the multi-module project
3. Run the `app` configuration on an emulator or device

### Gradle Commands

```bash
./gradlew testDebugUnitTest        # Run unit tests
./gradlew :core:steptracker:test   # Run step tracker benchmarks
./gradlew lintDebug                 # Run linting
./gradlew :core:data:test          # Run data layer integration tests
./gradlew connectedDebugAndroidTest # Run Android tests
```

## Project Structure

```
app/                    # Android application module
core/
  model/               # Domain models
  data/                # Data layer abstractions
  database/            # Room database module
  steptracker/         # Step tracking service
  telemetry/           # Telemetry event collection
  designsystem/        # Shared UI components
  ui/                  # Core UI utilities
  testing/             # Test doubles and harnesses
feature/
  worldmap/            # Travel and world map UI
  town/                # Town and market UI
  ledger/              # Rumor ledger UI
  companions/          # Companion roster UI
  character/           # Character state UI
```

## First Playable Milestone

1. Seed a new local game
2. Simulate or record steps
3. Spend steps on a road segment
4. Persist arrival
5. Render the destination town

## Telemetry & Quality

The project includes telemetry hooks for monitoring:
- Step accuracy and anomalies (negative, burst, duplicate timestamps)
- Travel latencies (start, completion, success/failure)
- Market transactions and anomalies (price spikes, crashes, supply depletion)

Run benchmark tests:
```bash
./gradlew :core:steptracker:test
```

See `specs/001-wandering-ledger/tasks.md` for full task list.

## Documentation

- [PRD](prd.md)
- [Quickstart](specs/001-wandering-ledger/quickstart.md)
- [Spec](specs/001-wandering-ledger/spec.md)
- [Data Model](specs/001-wandering-ledger/data-model.md)
- [Tasks](specs/001-wandering-ledger/tasks.md)

## Version

0.2.0 (dev) - Phase 7: Quality Gates
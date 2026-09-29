# Wandering Ledger

An offline Android walking game where real-world steps become travel currency in a hand-authored trade world. Players bank steps, travel between towns, trade goods, collect rumors, and recruit companions. Progress is stored locally.

## Project status

The core game loop and supporting systems are implemented. The project is in release-readiness work: CI is being repaired, the Android build needs a fresh green run, and an end-to-end device playtest is still required. See [Release Readiness](docs/release-readiness.md) for current evidence and remaining checks.

## Requirements

- JDK 17
- Android Studio with Android SDK 34
- Android API 26+ emulator or device

## Quick start

1. Open the repository root in Android Studio.
2. Let Gradle sync the multi-module project.
3. Run the `app` configuration on an API 26+ emulator or device.

### Gradle commands

```powershell
.\gradlew.bat check assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat connectedDebugAndroidTest # Requires a connected device or emulator
```

## Core game loop

1. Start a local game.
2. Bank steps from walking.
3. Spend steps to travel to another town.
4. Trade goods and explore town activities.
5. Restart the app and confirm progress persists.

## Telemetry and benchmarks

Telemetry records step anomalies, travel latency, and market events. The project includes step-fidelity and travel-latency benchmark harnesses; consult the [playtest plan](specs/001-wandering-ledger/playtest-plan.md) before treating benchmark results as real-world device validation.

## Project structure

```
app/                    # Android application module
core/                   # Domain, data, database, step tracking, and shared UI
feature/                # World map, town, ledger, companions, journey, and settings
specs/001-wandering-ledger/ # Product and implementation specifications
docs/                   # Project operations and release-readiness notes
```

## Documentation

- [Product requirements](prd.md)
- [Quickstart](specs/001-wandering-ledger/quickstart.md)
- [Feature specification](specs/001-wandering-ledger/spec.md)
- [Data model](specs/001-wandering-ledger/data-model.md)
- [Playtest plan](specs/001-wandering-ledger/playtest-plan.md)
- [Art prompts](specs/001-wandering-ledger/art-requirements.md)
- [Release readiness](docs/release-readiness.md)

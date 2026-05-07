# Quickstart: Wandering Ledger

## Prerequisites

- JDK 17
- Android Studio with Android SDK 34
- Android API 26+ emulator or device

The Gradle wrapper is checked in. Use `gradlew.bat` on Windows or `./gradlew` on macOS/Linux.

## First Sync

1. Open the repo root in Android Studio.
2. Let Android Studio sync the multi-module Gradle project.
3. Select the `app` run configuration.
4. Run on an API 26+ emulator or device.

## Expected Commands

```powershell
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew connectedDebugAndroidTest
```

CI currently treats coverage parsing as a placeholder until Jacoco/Kover report paths are finalized.

## First Playable Slice

The first implementation milestone is User Story 1:

1. seed a new local game,
2. simulate or record steps,
3. spend steps on a road segment,
4. persist arrival,
5. render the destination town.

## Validation Checklist

- Requirements checklist is fully checked.
- `contracts/`, `data-model.md`, `research.md`, `tasks.md`, and `plan.md` agree on module names and boundaries.
- `feature/ledger` is the only Ledger module name.
- Performance targets are documented but not claimed until benchmark harnesses exist.

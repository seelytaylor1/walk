# Quickstart: Wandering Ledger

## Prerequisites

- JDK 17
- Android Studio with Android SDK 34
- Android API 26+ emulator or device

The Gradle wrapper is checked in. Use `gradlew.bat` on Windows. On macOS/Linux, run `chmod +x gradlew` once, then use `./gradlew`.

## First Sync

1. Open the repo root in Android Studio.
2. Let Android Studio sync the multi-module Gradle project.
3. Select the `app` run configuration.
4. Run on an API 26+ emulator or device.

## Expected Commands

```powershell
.\gradlew.bat ktlintCheck testDebugUnitTest check assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

`ktlintCheck testDebugUnitTest check assembleDebug` is the CI verification command. `connectedDebugAndroidTest` requires a connected Android device or emulator. Coverage reporting is configured in selected modules, but CI does not currently enforce a coverage threshold.

## First Playable Slice

The first implementation milestone is User Story 1:

1. seed a new local game,
2. simulate or record steps,
3. spend steps on a road segment,
4. persist arrival,
5. render the destination town.

## Validation Checklist

- Requirements checklist is reviewed.
- `contracts/`, `data-model.md`, `research.md`, and `plan.md` agree on module names and boundaries.
- `feature/ledger` is the only Ledger module name.
- Performance targets are documented; results are claimed only after running the benchmark harnesses.
- See [release readiness](../../docs/release-readiness.md) for current build and device-playtest evidence.

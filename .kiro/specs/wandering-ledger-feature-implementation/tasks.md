# Implementation Plan: Wandering Ledger — Missing Feature Implementation

## Overview

This plan wires up the five broken areas of the Wandering Ledger app in Kotlin: navigation content switching, World Map travel UI, Android step sensor registration, rumor seeding on first launch, and back-stack content restoration. Each task builds on the previous, ending with full integration and a playable vertical slice.

## Tasks

- [x] 1. Wire NavigationShell content-provider callback
  - [x] 1.1 Add `onNavigateToDestination` callback property to `NavigationShell`
    - Declare `var onNavigateToDestination: ((BottomNavBar.Destination) -> Unit)?` in `NavigationShell.kt`
    - Update `handleDestination` to invoke the callback instead of calling `navigateTo` directly
    - Add `BottomNavBar.Destination.toScreenType()` extension for the fallback path
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6_
  - [x] 1.2 Register the `onNavigateToDestination` callback in `MainActivity.onCreate`
    - After `navigationShell` is constructed, set `navigationShell.onNavigateToDestination` to a lambda that launches the appropriate `show*` / `refresh*` coroutine for each `BottomNavBar.Destination`
    - Cover all four destinations: WORLD_MAP → `refreshWorldMapScreen`, TOWN → `showTownView`, LEDGER → `showLedgerView`, COMPANIONS → `showCompanionsView`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.3, 4.1, 5.1, 6.1_
  - [x]* 1.3 Write unit tests for `NavigationShell.handleDestination` callback dispatch
    - Verify callback is invoked with the correct `Destination` for each tab tap
    - Verify fallback path (no callback set) still calls `navigateTo`
    - _Requirements: 1.1, 1.2_

- [x] 2. Wire NavigationShell back-stack content restoration
  - [x] 2.1 Add `onRestoreScreen` callback property to `NavigationShell`
    - Declare `var onRestoreScreen: ((ScreenType) -> Unit)?` in `NavigationShell.kt`
    - Update `navigateBack` to invoke `onRestoreScreen` after popping the stack and calling `updateNavForScreen`
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  - [x] 2.2 Register the `onRestoreScreen` callback in `MainActivity`
    - Set `navigationShell.onRestoreScreen` to a lambda that launches the appropriate `show*` / `refresh*` coroutine for each `ScreenType`
    - Handle all `ScreenType` values; use no-op for `TOWN_ARRIVAL`
    - _Requirements: 10.1, 10.2, 10.3_
  - [x]* 2.3 Write unit tests for `NavigationShell.navigateBack` content restoration
    - Verify `onRestoreScreen` is called with the correct `ScreenType` after a pop
    - Verify no exception is thrown when `screenStack` is empty
    - Verify duplicate-entry guard: `navigateTo` with same `ScreenType` does not push
    - _Requirements: 10.3, 10.4, 10.5_

- [x] 3. Checkpoint — Ensure navigation wiring compiles and all existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement WorldMapScreen route buttons and simulate-steps button
  - [x] 4.1 Add `RouteButton` composable to `WorldMapScreen.kt`
    - Render destination name, step cost, and narrative distance
    - Enable button when `route.shortfall == 0`; disable and show "Need ${shortfall} more steps" when `shortfall > 0`
    - On click, invoke `actions.onTravel(route.segmentId)`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - [x] 4.2 Add route list and empty-state message to `WorldMapScreen` layout
    - Render `RouteButton` for each entry in `WorldMapScreenState.routes` below the step count and above the map canvas
    - When `routes` is empty, display "No roads lead from here." message
    - _Requirements: 2.1, 2.5, 2.6_
  - [x] 4.3 Add "Simulate Steps" button to `WorldMapScreen`
    - Render an `OutlinedButton` labelled "+75 Steps" that calls `actions.onSimulateSteps`
    - _Requirements: 12.1_
  - [x] 4.4 Wire `onSimulateSteps` in `MainActivity`
    - Implement `WorldMapActions.onSimulateSteps` to call `StepTrackerService.recordSensorDelta(75, StepSource.Simulation)` then `refreshWorldMapScreen()`
    - _Requirements: 12.2, 12.3_
  - [ ]* 4.5 Write unit tests for `RouteButton` rendering states

- [x] 5. Register Android step sensor in MainActivity
  - [x] 5.1 Add step sensor fields and `SensorEventListener` to `MainActivity`
    - Declare `sensorManager`, `stepCounterSensor`, `lastSensorValue`, `stepSensorRegistered` fields
    - Implement `stepSensorListener.onSensorChanged`: compute delta, reject negative/zero deltas, call `StepTrackerService.recordSensorDelta(delta, StepSource.Hardware)`, then call `refreshActiveScreenStepCount()`
    - On first reading (`lastSensorValue < 0`), establish baseline and return without recording
    - _Requirements: 7.1, 7.2, 7.6_
  - [x] 5.2 Initialize sensor in `MainActivity.onCreate`
    - Obtain `SensorManager` via `getSystemService`
    - Obtain `Sensor.TYPE_STEP_COUNTER`; log warning if unavailable
    - _Requirements: 7.1, 7.5_
  - [x] 5.3 Register and unregister sensor in `onResume` / `onPause`
    - `onResume`: register `stepSensorListener` with `SENSOR_DELAY_NORMAL` if sensor is available
    - `onPause`: unregister `stepSensorListener` if registered; set `stepSensorRegistered = false`
    - _Requirements: 7.3, 7.4_
  - [x] 5.4 Implement `refreshActiveScreenStepCount` helper in `MainActivity`
    - Re-run `refreshWorldMapScreen()` or `refreshJourneyScreen()` only when the current screen is WORLD_MAP or JOURNEY; no-op for all other screens
    - _Requirements: 7.6_
  - [ ]* 5.5 Write unit tests for step sensor delta logic

- [x] 6. Checkpoint — Ensure step sensor and World Map UI compile and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Seed rumors on first launch in GameRepository
  - [x] 7.1 Add rumor seeding logic to `GameRepository.initializeNewGame`
    - After `SeedWorld.ensureSeeded`, query `rumorDao().listActiveRumors().first()`
    - If the result is empty, call `rumorRepository.generateRumorForTownVisit(visitedTownId = 1L)`
    - _Requirements: 8.1, 8.2_
  - [x] 7.2 Verify rumor generation on town travel in `GameRepository.travel`
    - Confirm `rumorRepository.generateRumorForTownVisit` is called for the destination town after a successful travel
    - Add the call if it is missing
    - _Requirements: 8.3_
  - [x] 7.3 Verify rumor expiry filter in `RumorRepository.observeActiveRumors`
    - Confirm the DAO query excludes rumors where `expiryVisitsLeft <= 0`
    - Add or fix the filter if it is missing
    - _Requirements: 8.4_
  - [ ]* 7.4 Write unit tests for rumor seeding idempotency

- [x] 8. Wire encounter triggering in GameRepository.travel
  - [x] 8.1 Call `EncounterRepository.resolveRoadEncounter` from `GameRepository.travel`
    - After a successful travel, check if the road segment's `eventPool` is non-empty
    - If non-empty, derive a deterministic seed from arrival timestamp and segment ID, then call `encounterRepository.resolveRoadEncounter(seed, segmentId)`
    - If `eventPool` is empty or blank, skip encounter resolution
    - _Requirements: 9.1, 9.5_
  - [x] 8.2 Apply gold and bond changes in `EncounterRepository.resolveRoadEncounter`
    - If `goldChange != 0`, update player gold in the same DB transaction as the encounter log
    - If `bondChange != 0`, update bond level of all active companions in the same DB transaction
    - Insert an `EventLogEntity` of type `"encounter"` with encounter ID, success flag, and gold change in meta
    - _Requirements: 9.2, 9.3, 9.4_
  - [ ]* 8.3 Write unit tests for encounter triggering conditions

- [x] 9. Manage observation job lifecycle in MainActivity
  - [x] 9.1 Cancel stale observation jobs before replacing screen content in `MainActivity`
    - Track the active observation `Job` for the current screen
    - Cancel the previous job before launching a new `show*` / `refresh*` coroutine
    - _Requirements: 11.5_
  - [x] 9.2 Ensure each `show*` function reloads from the database before rendering
    - Confirm `refreshWorldMapScreen`, `showTownView`, `showLedgerView`, `showCompanionsView` each perform a fresh DB read before calling `replaceContent`
    - Add reload calls where missing
    - _Requirements: 11.1, 11.2, 11.3, 11.4_
  - [ ]* 9.3 Write unit tests for stale job cancellation

- [x] 10. Final checkpoint — Full integration and all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at logical boundaries
- The design uses Kotlin throughout; all code examples follow the patterns in `design.md`
- Unit tests are preferred over property tests here because the design has no Correctness Properties section

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "2.2", "4.1", "5.1", "5.2", "7.1"] },
    { "id": 2, "tasks": ["1.3", "2.3", "4.2", "4.3", "5.3", "5.4", "7.2", "7.3", "8.1"] },
    { "id": 3, "tasks": ["4.4", "4.5", "5.5", "7.4", "8.2", "9.1"] },
    { "id": 4, "tasks": ["8.3", "9.2"] },
    { "id": 5, "tasks": ["9.3"] }
  ]
}
```

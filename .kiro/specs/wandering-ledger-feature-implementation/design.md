# Technical Design Document

## Feature: Wandering Ledger — Missing Feature Implementation

**Spec**: `.kiro/specs/wandering-ledger-feature-implementation/`
**Requirements**: `requirements.md`

---

## Overview

This document describes the technical changes needed to make the Wandering Ledger app fully playable. The work falls into five areas:

1. **Navigation wiring** — `NavigationShell.handleDestination` must call back into `MainActivity` to swap content views.
2. **World Map travel UI** — `WorldMapScreen` must render route buttons with step cost and shortfall.
3. **Android step sensor** — `MainActivity` must register `TYPE_STEP_COUNTER` and feed deltas to `StepTrackerService`.
4. **Rumor seeding** — `GameRepository.initializeNewGame` must seed at least one rumor on first launch.
5. **Back-stack integrity** — `NavigationShell.navigateBack` must restore content as well as nav bar state.

---

## Architecture

The app uses a single-Activity architecture with no Jetpack Navigation. `MainActivity` owns all screen `View` instances and all coroutine-based `show*` / `refresh*` functions. `NavigationShell` is a plain `LinearLayout` that owns the `TopBar`, a content slot, and `BottomNavBar`. It has no knowledge of feature views.

The key insight: `NavigationShell` must **not** hold references to feature views. Instead, `MainActivity` registers a `contentProvider` lambda with `NavigationShell` so that when a tab is tapped, `NavigationShell` calls the lambda and `MainActivity` executes the appropriate `show*` coroutine.

---

## Change 1 — NavigationShell: Content Provider Callback

### Problem
`handleDestination` calls `navigateTo(screenType)` which only updates the nav bar highlight and top bar text. It never calls `replaceContent`.

### Solution
Add a `var onNavigateToDestination: ((BottomNavBar.Destination) -> Unit)?` property to `NavigationShell`. `handleDestination` invokes this callback instead of calling `navigateTo` directly. `MainActivity` sets this callback in `onCreate` after constructing the shell.

```
// NavigationShell.kt — new property
var onNavigateToDestination: ((BottomNavBar.Destination) -> Unit)? = null

// NavigationShell.kt — updated handleDestination
private fun handleDestination(destination: BottomNavBar.Destination) {
    onNavigateToDestination?.invoke(destination)
        ?: run {
            // Fallback: just update nav bar highlight (no content swap)
            val screenType = destination.toScreenType()
            navigateTo(screenType)
        }
}

private fun BottomNavBar.Destination.toScreenType() = when (this) {
    BottomNavBar.Destination.WORLD_MAP  -> ScreenType.WORLD_MAP
    BottomNavBar.Destination.TOWN       -> ScreenType.TOWN
    BottomNavBar.Destination.LEDGER     -> ScreenType.LEDGER
    BottomNavBar.Destination.COMPANIONS -> ScreenType.COMPANIONS
}
```

### MainActivity wiring (in onCreate, after navigationShell is constructed)
```
navigationShell.onNavigateToDestination = { destination ->
    scope.launch {
        when (destination) {
            BottomNavBar.Destination.WORLD_MAP  -> refreshWorldMapScreen()
            BottomNavBar.Destination.TOWN       -> showTownView(currentTownId)
            BottomNavBar.Destination.LEDGER     -> showLedgerView(currentTownId)
            BottomNavBar.Destination.COMPANIONS -> showCompanionsView(currentTownId)
        }
    }
}
```

---

## Change 2 — NavigationShell: Back Navigation Restores Content

### Problem
`navigateBack` updates `currentScreen` and calls `updateNavForScreen` (which updates the top bar and nav highlight) but never calls `replaceContent`. Pressing back leaves the old content visible.

### Solution
`NavigationShell` needs a way to restore content when navigating back. Add a `var onRestoreScreen: ((ScreenType) -> Unit)?` callback. `navigateBack` invokes it after popping the stack.

```
// NavigationShell.kt
var onRestoreScreen: ((ScreenType) -> Unit)? = null

fun navigateBack() {
    if (screenStack.isNotEmpty()) {
        val previous = screenStack.removeLast()
        currentScreen = previous
        updateNavForScreen(previous, null, null)
        onRestoreScreen?.invoke(previous)
    }
}
```

### MainActivity wiring
```
navigationShell.onRestoreScreen = { screenType ->
    scope.launch {
        when (screenType) {
            NavigationShell.ScreenType.WORLD_MAP  -> refreshWorldMapScreen()
            NavigationShell.ScreenType.JOURNEY    -> refreshJourneyScreen()
            NavigationShell.ScreenType.TOWN       -> showTownView(currentTownId)
            NavigationShell.ScreenType.MARKET     -> showMarketView(currentTownId)
            NavigationShell.ScreenType.INVENTORY  -> showInventoryView(currentTownId)
            NavigationShell.ScreenType.LEDGER     -> showLedgerView(currentTownId)
            NavigationShell.ScreenType.CHRONICLE  -> showChronicleView(currentTownId)
            NavigationShell.ScreenType.COMPANIONS -> showCompanionsView(currentTownId)
            NavigationShell.ScreenType.SETTINGS   -> showSettingsView(currentTownId)
            NavigationShell.ScreenType.TOWN_ARRIVAL -> { /* no-op, can't go back to arrival */ }
        }
    }
}
```

---

## Change 3 — WorldMapScreen: Route Buttons + Simulate Steps Button

### Problem
`WorldMapScreen` renders the map canvas and step count but has no UI for the route list or the simulate-steps action.

### Solution
Add a `RoutePanel` composable rendered as a `Column` overlay at the bottom of the `Box`, above the message text. Also add a "Simulate Steps" `Button`.

Layout structure (inside the existing `Box`):
```
Box(fillMaxSize) {
    WorldMapRenderer(fillMaxSize)          // map canvas — bottom layer
    Column(fillMaxSize, padding=16dp) {
        // header: "World Map", town name, steps
        Text("World Map")
        Text("${town}, ${region}")
        Text("Steps: ${bankedSteps}")

        Spacer(weight=1f)                  // pushes route panel to bottom

        // route panel
        if (routes.isEmpty()) {
            Text("No roads lead from here.")
        } else {
            routes.forEach { route ->
                RouteButton(route, onTravel)
            }
        }

        // simulate steps (dev affordance)
        OutlinedButton(onClick = onSimulateSteps) { Text("+75 Steps") }

        // message
        if (message != null) Text(message)
    }
}
```

`RouteButton` composable:
- Shows: destination name, step cost, narrative distance
- Enabled when `route.isEnabled` (shortfall == 0)
- When disabled: shows "Need ${shortfall} more steps" in muted text
- `onClick`: `actions.onTravel(route.segmentId)`

---

## Change 4 — Android Step Sensor Registration

### Problem
`MainActivity` never registers a `SensorEventListener` for `Sensor.TYPE_STEP_COUNTER`. Steps only accumulate via the simulate button.

### Solution
Add sensor fields and lifecycle methods to `MainActivity`.

```kotlin
// New fields in MainActivity
private var sensorManager: SensorManager? = null
private var stepCounterSensor: Sensor? = null
private var lastSensorValue: Float = -1f   // -1 = not yet received first reading
private var stepSensorRegistered: Boolean = false

private val stepSensorListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val current = event.values[0]
        if (lastSensorValue < 0f) {
            // First reading — establish baseline, record 0 steps
            lastSensorValue = current
            return
        }
        val delta = (current - lastSensorValue).toInt()
        lastSensorValue = current
        if (delta <= 0) return   // reject negative or zero deltas
        scope.launch {
            withContext(Dispatchers.IO) {
                stepTrackerService.recordSensorDelta(delta, StepSource.Hardware)
            }
            refreshActiveScreenStepCount()
        }
    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
```

In `onCreate` (after `stepTrackerService` is initialized):
```kotlin
sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
if (stepCounterSensor == null) {
    Log.w("MainActivity", "TYPE_STEP_COUNTER not available; simulation only.")
}
```

In `onResume`:
```kotlin
override fun onResume() {
    super.onResume()
    stepCounterSensor?.let { sensor ->
        sensorManager?.registerListener(stepSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        stepSensorRegistered = true
    }
}
```

In `onPause`:
```kotlin
override fun onPause() {
    super.onPause()
    if (stepSensorRegistered) {
        sensorManager?.unregisterListener(stepSensorListener)
        stepSensorRegistered = false
    }
}
```

`refreshActiveScreenStepCount` is a lightweight helper that re-runs the appropriate `refresh*` function only if the current screen is WORLD_MAP or JOURNEY (the two screens that display step count prominently). For other screens it is a no-op to avoid unnecessary DB reads.

---

## Change 5 — Rumor Seeding on First Launch

### Problem
`GameRepository.initializeNewGame` calls `SeedWorld.ensureSeeded` but does not generate any rumors. The Ledger is empty on first launch.

### Solution
After `SeedWorld.ensureSeeded`, check if any active rumors exist. If none, call `rumorRepository.generateRumorForTownVisit` for the starting town (townId = 1).

```kotlin
// GameRepository.initializeNewGame
suspend fun initializeNewGame(seed: Long = 1L) {
    SeedWorld.ensureSeeded(database, now = seed.coerceAtLeast(1L))
    // Seed at least one rumor on first launch
    val existingRumors = database.rumorDao().listActiveRumors().first()
    if (existingRumors.isEmpty()) {
        rumorRepository.generateRumorForTownVisit(visitedTownId = 1L)
    }
}
```

This is idempotent: subsequent launches find existing rumors and skip generation.

---

## Change 6 — Duplicate Stack Entry Guard (already partially present)

`NavigationShell.navigateTo` already checks `if (currentScreen != screen)` before pushing. No change needed here — the existing guard is correct.

---

## Files Changed

| File | Change |
|---|---|
| `core/ui/.../NavigationShell.kt` | Add `onNavigateToDestination` and `onRestoreScreen` callbacks; update `handleDestination` and `navigateBack` |
| `app/.../MainActivity.kt` | Wire `onNavigateToDestination` and `onRestoreScreen`; add step sensor fields, `onResume`, `onPause`, `refreshActiveScreenStepCount` |
| `feature/worldmap/.../WorldMapScreen.kt` | Add `RouteButton` composable, route list, simulate-steps button |
| `core/data/.../GameRepositories.kt` | Seed one rumor in `initializeNewGame` if none exist |

---

## Data Flow Summary

```
User taps tab
  → BottomNavBar.onNavigate(destination)
  → NavigationShell.handleDestination(destination)
  → NavigationShell.onNavigateToDestination(destination)   ← NEW callback
  → MainActivity.scope.launch { show*(currentTownId) }
  → navigationShell.replaceContent(view)
  → navigationShell.navigateTo(screenType, title, subtitle)
  → updateNavForScreen → topBar.render + bottomNav.setDestination

User presses back
  → NavigationShell.navigateBack()
  → screenStack.removeLast()
  → updateNavForScreen(previous)
  → NavigationShell.onRestoreScreen(previous)              ← NEW callback
  → MainActivity.scope.launch { show*(currentTownId) }

Hardware step fires
  → SensorEventListener.onSensorChanged
  → delta = current - lastSensorValue
  → StepTrackerService.recordSensorDelta(delta, Hardware)
  → RoomStepBankRepository.recordDetectedSteps
  → player.bankedSteps += delta  (Room DB)
  → refreshActiveScreenStepCount()
  → worldMapScreenState.value = updated state  (StateFlow → Compose recompose)
```

---

## Non-Goals (out of scope for this spec)

- Replacing the single-Activity architecture with Jetpack Navigation
- Adding a background `Service` for step counting when the app is backgrounded
- Cloud sync, accounts, or location-based gameplay
- Economy rebalancing or new encounter types

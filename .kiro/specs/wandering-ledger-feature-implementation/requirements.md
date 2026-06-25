# Requirements Document

## Introduction

The Wandering Ledger is an offline Android walking-and-trading game built with Kotlin and Jetpack Compose in a multi-module architecture. The app is semi-implemented: the data layer, individual screens, and audio/haptics infrastructure are largely complete, but the bottom navigation bar does not swap content between screens, the World Map has no travel UI, the Android step sensor is not registered in `MainActivity`, rumors are never seeded at startup, and encounters are wired but never triggered from the map tab. This document specifies the requirements needed to bring those broken and missing pieces to a fully playable vertical slice.

## Glossary

- **NavigationShell**: The root `LinearLayout` that owns the `TopBar`, content area, and `BottomNavBar`. Exposes `replaceContent(view)` and `navigateTo(screenType)`.
- **BottomNavBar**: The four-tab navigation bar with destinations Map, Town, Ledger, and Party.
- **WorldMapScreen**: The Compose screen that renders the map canvas, town nodes, road lines, step count, and travel route options.
- **JourneyScreen**: The Compose screen that shows the biome background, camp state, route cards, and party formation while the player is between towns.
- **TownScreen**: The Android View screen that shows town info and buttons to Market, Inventory, Ledger, Chronicle, Companions, and Settings.
- **LedgerScreen**: The Compose screen that shows rumor scraps with parchment UI.
- **CompanionsScreen**: The Compose screen that shows active and recruitable companions with recruit and interact actions.
- **StepTrackerService**: The service that validates and persists step deltas to the step bank.
- **GameRepository**: The repository that owns `travel()`, `initializeNewGame()`, and player/town observation flows.
- **RumorRepository**: The repository that generates and persists rumor cards.
- **EncounterRepository**: The repository that resolves road encounters and logs outcomes.
- **MainActivity**: The single `ComponentActivity` that owns all screen views, the `NavigationShell`, and all coroutine-based navigation functions.
- **Step_Sensor**: The Android `TYPE_STEP_COUNTER` hardware sensor.
- **Step_Bank**: The persisted count of steps available to spend on travel.
- **Rumor**: A parchment card in the Ledger that hints at market conditions in another town, with an expiry counter.
- **Encounter**: A road event resolved during travel that may change player gold and companion bond levels.
- **SeedWorld**: The Room database seeding utility that populates towns, roads, goods, prices, and companions on first launch.

---

## Requirements

### Requirement 1: Bottom Navigation Content Switching

**User Story:** As a player, I want tapping a bottom nav tab to actually change the screen content, so that I can move between the map, town, ledger, and party views without restarting the app.

#### Acceptance Criteria

1. WHEN the player taps the Map tab, THE NavigationShell SHALL call `replaceContent(worldMapView)` and update the top bar to "World Map".
2. WHEN the player taps the Town tab, THE NavigationShell SHALL call `replaceContent(townView)` and update the top bar to the current town name.
3. WHEN the player taps the Ledger tab, THE NavigationShell SHALL call `replaceContent(ledgerView)` and update the top bar to "Ledger".
4. WHEN the player taps the Party tab, THE NavigationShell SHALL call `replaceContent(companionsView)` and update the top bar to "Party".
5. WHEN any tab is tapped and that tab's destination matches the currently active screen, THE NavigationShell SHALL leave the content unchanged and SHALL NOT push a duplicate entry onto the screen stack.
6. THE NavigationShell SHALL expose a `setContentProvider` callback so that `MainActivity` can supply the correct `View` for each `BottomNavBar.Destination` without `NavigationShell` holding direct references to feature views.

### Requirement 2: World Map Travel UI

**User Story:** As a player, I want to see travel route buttons on the World Map screen, so that I can spend banked steps to move to an adjacent town.

#### Acceptance Criteria

1. WHEN `WorldMapScreenState.routes` is non-empty, THE WorldMapScreen SHALL render one button per route displaying the destination name, step cost, and narrative distance.
2. WHEN a route's `shortfall` is zero, THE WorldMapScreen SHALL render the route button as enabled and tappable.
3. WHEN a route's `shortfall` is greater than zero, THE WorldMapScreen SHALL render the route button as disabled and display the shortfall in steps. WHEN a route's `shortfall` is zero, THE WorldMapScreen SHALL display the shortfall indicator showing zero.
4. WHEN the player taps an enabled route button, THE WorldMapScreen SHALL invoke `WorldMapActions.onTravel` with the route's `segmentId`.
5. WHEN `WorldMapScreenState.routes` is empty, THE WorldMapScreen SHALL display a message indicating no routes are available from the current location.
6. THE WorldMapScreen SHALL render the route list below the step count and above the map canvas so that the map remains visible.

### Requirement 3: Map Tab Shows Correct Screen Based on Travel State

**User Story:** As a player, I want the Map tab to show the World Map when I am in a town and the Journey screen when I am traveling, so that the tab always reflects my current situation.

#### Acceptance Criteria

1. WHEN the player is at a town and taps the Map tab, THE MainActivity SHALL display the WorldMapScreen with routes from the current town.
2. WHEN the player has initiated travel and is between towns, THE MainActivity SHALL display the JourneyScreen when the Map tab is active. WHERE the player is in a town and wishes to review journey progress or plan the next trip, THE MainActivity SHALL also allow the JourneyScreen to be shown from the Map tab.
3. WHEN `refreshWorldMapScreen` completes, THE NavigationShell SHALL show the WorldMapScreen as the active content.
4. WHEN `refreshJourneyScreen` completes, THE NavigationShell SHALL show the JourneyScreen as the active content.

### Requirement 4: Town Tab Navigation

**User Story:** As a player, I want tapping the Town tab to open the current town's screen, so that I can access the market, inventory, ledger, chronicle, companions, and settings from anywhere.

#### Acceptance Criteria

1. WHEN the player taps the Town tab from any screen, THE MainActivity SHALL call `showTownView(currentTownId)` and display the TownScreen.
2. WHEN `showTownView` is called, THE NavigationShell SHALL replace the content with `townView` and set the top bar title to the town name and subtitle to the town region.
3. WHEN the player is already on the TownScreen and taps the Town tab, THE MainActivity SHALL silently refresh the TownScreen state without visual feedback and without duplicating the screen stack entry.

### Requirement 5: Ledger Tab Navigation

**User Story:** As a player, I want tapping the Ledger tab to open the Ledger screen directly, so that I can review my rumor cards without navigating through the town menu.

#### Acceptance Criteria

1. WHEN the player taps the Ledger tab from any screen, THE MainActivity SHALL call `showLedgerView(currentTownId)` and display the LedgerScreen.
2. WHEN `showLedgerView` is called, THE NavigationShell SHALL replace the content with `ledgerView` and set the top bar title to "Ledger".
3. WHEN the LedgerScreen is shown via the Ledger tab from a screen other than the TownScreen, THE LedgerScreen SHALL display a back action that returns to the TownScreen for `currentTownId`.

### Requirement 6: Party Tab Navigation

**User Story:** As a player, I want tapping the Party tab to open the Companions screen directly, so that I can manage my party from anywhere in the app.

#### Acceptance Criteria

1. WHEN the player taps the Party tab from any screen, THE MainActivity SHALL call `showCompanionsView(currentTownId)` and display the CompanionsScreen.
2. WHEN `showCompanionsView` is called, THE NavigationShell SHALL replace the content with `companionsView` and set the top bar title to "Party".
3. WHEN the CompanionsScreen is shown via the Party tab from a screen other than the TownScreen, THE CompanionsScreen SHALL display a back action that returns to the TownScreen for `currentTownId`.

### Requirement 7: Android Step Sensor Registration

**User Story:** As a player, I want the app to count my real walking steps automatically, so that I accumulate steps without needing to tap a simulate button.

#### Acceptance Criteria

1. WHEN `MainActivity.onCreate` completes, THE MainActivity SHALL register a `SensorEventListener` for `Sensor.TYPE_STEP_COUNTER` if the sensor is available on the device.
2. WHEN the Step_Sensor fires a `SensorEvent`, THE MainActivity SHALL compute the delta from the previous sensor reading and call `StepTrackerService.recordSensorDelta(delta, StepSource.Hardware)`. IF the computed delta is negative, THEN THE MainActivity SHALL reject the delta and ignore that sensor reading.
3. WHEN `MainActivity.onResume` is called, THE MainActivity SHALL re-register the Step_Sensor listener if it was previously unregistered.
4. WHEN `MainActivity.onPause` is called, THE MainActivity SHALL unregister the Step_Sensor listener to conserve battery.
5. IF the Step_Sensor is unavailable on the device, THEN THE MainActivity SHALL log a warning and continue operating with simulation-only step input.
6. WHEN the Step_Sensor fires and the delta is recorded, THE MainActivity SHALL refresh the active screen's step count display within 2 seconds. WHILE the display refresh is pending or fails, THE StepTrackerService SHALL continue recording steps so that the Step_Bank remains accurate.

### Requirement 8: Rumor Seeding at Game Start

**User Story:** As a player, I want to see at least one rumor in the Ledger when I first open the app, so that the Ledger does not appear empty and I understand how the rumor system works.

#### Acceptance Criteria

1. WHEN `initializeNewGame` is called and no active rumors exist in the database, THE GameRepository SHALL call `RumorRepository.generateRumorForTownVisit` for the starting town.
2. WHEN the player opens the Ledger after first launch, THE LedgerScreen SHALL display at least one rumor card.
3. WHEN the player travels to a new town, THE GameRepository SHALL call `RumorRepository.generateRumorForTownVisit` for the destination town, producing a new rumor card.
4. WHEN a rumor's `expiryVisitsLeft` reaches zero, THE RumorRepository SHALL exclude that rumor from `observeActiveRumors` results. WHILE a rumor's `expiryVisitsLeft` is greater than zero, THE RumorRepository SHALL include that rumor in `observeActiveRumors` results.
5. THE RumorRepository SHALL NOT generate duplicate rumors for the same town visit within the same travel transaction.

### Requirement 9: Encounter Triggering During Travel

**User Story:** As a player, I want road encounters to fire when I travel between towns, so that the journey feels eventful and my companions and gold can change based on what happens on the road.

#### Acceptance Criteria

1. WHEN `GameRepository.travel` succeeds and the road segment's `eventPool` is non-empty, THE GameRepository SHALL call `EncounterRepository.resolveRoadEncounter` with a deterministic seed derived from the arrival timestamp and segment ID.
2. WHEN an encounter resolves with a non-zero `goldChange`, THE EncounterRepository SHALL update the player's gold in the database within the same transaction as the encounter log entry.
3. WHEN an encounter resolves with a non-zero `bondChange`, THE EncounterRepository SHALL update the bond level of all active companions within the same transaction.
4. WHEN an encounter is resolved, THE EncounterRepository SHALL insert an `EventLogEntity` of type `"encounter"` with the encounter ID, success flag, and gold change in the meta field.
5. WHEN the road segment's `eventPool` is empty or blank, THE GameRepository SHALL skip encounter resolution and complete travel normally. WHEN the road segment's `eventPool` is non-empty but other conditions prevent resolution, THE EncounterRepository SHALL skip the encounter for that specific blocking condition only.
6. WHEN the player opens the Chronicle after a travel that triggered an encounter, THE ChronicleScreen SHALL display the encounter entry with its result text.

### Requirement 10: Navigation Back Stack Integrity

**User Story:** As a player, I want the back button to return me to the previous screen in a predictable order, so that I do not get stuck or lose my place in the app.

#### Acceptance Criteria

1. WHEN the player navigates from the TownScreen to the MarketScreen and presses back, THE NavigationShell SHALL return to the TownScreen.
2. WHEN the player navigates from the TownScreen to the LedgerScreen and presses back, THE NavigationShell SHALL return to the TownScreen.
3. WHEN the player is on the WorldMapScreen and presses back, THE NavigationShell SHALL remain on the WorldMapScreen regardless of internal navigation state.
4. WHEN `navigateTo` is called with the same `ScreenType` as `currentScreen`, THE NavigationShell SHALL NOT push a duplicate entry onto `screenStack`.
5. WHEN `navigateBack` is called and `screenStack` is empty, THE NavigationShell SHALL remain on the current screen without throwing an exception.

### Requirement 11: Screen State Refresh on Tab Re-selection

**User Story:** As a player, I want the screen to show up-to-date data when I switch tabs, so that step counts, rumor lists, and companion states reflect the latest game state.

#### Acceptance Criteria

1. WHEN the player taps the Map tab, THE MainActivity SHALL reload player state and available routes from the database before displaying the WorldMapScreen.
2. WHEN the player taps the Ledger tab, THE MainActivity SHALL reload active rumors from the database before displaying the LedgerScreen.
3. WHEN the player taps the Party tab, THE MainActivity SHALL reload active and recruitable companions from the database before displaying the CompanionsScreen.
4. WHEN the player taps the Town tab, THE MainActivity SHALL reload player gold and town state from the database before displaying the TownScreen.
5. WHEN a tab navigation triggers a database reload, THE MainActivity SHALL cancel any previously active observation jobs for the screen being replaced to prevent memory leaks.

### Requirement 12: Simulate Steps Button Availability on World Map

**User Story:** As a developer and tester, I want a simulate-steps button on the World Map screen, so that I can add steps without a physical device and test travel without walking.

#### Acceptance Criteria

1. THE WorldMapScreen SHALL render a "Simulate Steps" button that invokes `WorldMapActions.onSimulateSteps` when tapped.
2. WHEN `onSimulateSteps` is invoked, THE MainActivity SHALL call `StepTrackerService.recordSensorDelta(75, StepSource.Simulation)` and refresh the WorldMapScreen state.
3. WHEN the WorldMapScreen is refreshed after simulating steps, THE WorldMapScreen SHALL display the updated `bankedSteps` value.

# Repository Contracts: Wandering Ledger

These contracts define domain boundaries. Implementations may use Room, DataStore, sensors, and deterministic engines internally, but callers should depend on these behavior-level interfaces.

## Game State

- `observePlayerState()`: stream current player state, including current town, gold, inventory capacity, lifetime steps, and active companions.
- `initializeNewGame(seed)`: create the v1 seed world with 3 towns, initial player state, 2 recruitable companions, road segments, goods, and starting prices.
- `exportLocalState()`: produce a user-readable recovery payload if migration or startup recovery fails.

## Step Bank

- `observeStepBank()`: stream banked steps available for travel.
- `recordDetectedSteps(count, source, recordedAt)`: persist detected steps and increment banked steps.
- `spendSteps(amount, reason)`: atomically subtract banked steps when enough are available; otherwise return a shortfall result without mutating state.
- `observeCalibrationProfile()`: stream motion fallback sensitivity and last-calibrated timestamp.
- `saveCalibrationProfile(profile)`: persist user calibration preferences.

## Travel

- `listRoadsFrom(townId)`: stream available roads from the player's current town.
- `travel(segmentId)`: atomically validate step cost, spend steps, update current town, log arrival, decrement rumor expiry, and enqueue deterministic road encounter resolution.
- `observeTravelState()`: stream current route options, step shortfalls, and pending/complete travel result.

## Market

- `observeMarket(townId)`: stream town goods, buy prices, sell prices, supply state, inventory availability, and player gold.
- `buyGood(townId, goodId, quantity)`: atomically validate gold and inventory space, update gold, inventory, supply, price history, and relevant reputation.
- `sellGood(townId, goodId, quantity)`: atomically validate inventory, update gold, inventory, demand/supply, price history, and relevant reputation.

## Ledger & Rumors

- `observeActiveRumors()`: stream non-expired rumors sorted by recency.
- `recordRumor(rumor)`: persist a rumor from town visit, road event, or companion insight.
- `expireRumorsForVisit()`: decrement visit-based expiry and hide expired rumors.

## Companions & Encounters

- `observeCompanions()`: stream active and inactive companions with bond and quest state.
- `recruitCompanion(companionId, capacityDecision)`: recruit, replace, switch, or decline while enforcing the 3-active-companion cap.
- `updateCompanionBond(companionId, delta, reason)`: persist deterministic bond changes.
- `resolveRoadEncounter(seed, initialState, encounterId)`: return replayable outcome and persisted event log entry.

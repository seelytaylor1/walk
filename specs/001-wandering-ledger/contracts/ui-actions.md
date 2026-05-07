# UI Action Contracts: Wandering Ledger

UI modules dispatch explicit actions and render immutable screen state. Feature modules should not directly mutate persistence.

## World Map

Actions:
- `SelectRoad(segmentId)`
- `Travel(segmentId)`
- `DismissTravelResult`

State:
- current town name
- banked steps
- route cards with destination, step cost, shortfall, and enabled state
- pending, success, or failure travel result

## Town Market

Actions:
- `Buy(goodId, quantity)`
- `Sell(goodId, quantity)`
- `OpenLedger`
- `OpenWorldMap`

State:
- town summary and reputation
- player gold and inventory slots
- market rows with good name, buy price, sell price, supply/demand badge, and affordability
- transaction result message

## Ledger

Actions:
- `OpenRumor(rumorId)`
- `DismissExpiredRumors`

State:
- active rumor cards sorted by recency
- source town, target good, expiry visits left, and false-rumor flag when known
- empty state when no active rumors exist

## Companions

Actions:
- `Recruit(companionId)`
- `ReplaceActive(existingCompanionId, newCompanionId)`
- `Deactivate(companionId)`
- `DeclineRecruitment(companionId)`

State:
- active companion roster with max 3 slots
- recruitable companions
- replacement prompt when active roster is full
- bond level and quest state

## Calibration

Actions:
- `StartCalibration`
- `StopCalibration`
- `AdjustSensitivity(value)`
- `SaveCalibration`
- `SkipCalibration`

State:
- detected step count for the calibration session
- sensitivity value
- confidence label
- last calibrated timestamp

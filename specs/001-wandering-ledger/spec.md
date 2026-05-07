# Feature Specification: Wandering Ledger

**Feature Branch**: `001-wandering-ledger`
**Created**: 2026-05-06
**Status**: Draft
**Input**: User description: "we're building an application for this prd"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Walk & Travel (Priority: P1)
A casual player walks during their day and opens the app to spend accumulated steps to move the character along roads between towns.

**Why this priority**: Core loop; without reliable step tracking and travel there is no game.

**Independent Test**: Simulate step input and verify steps bank increases; open app and spend steps to traverse a single road segment; verify arrival triggers town view.

**Acceptance Scenarios**:
1. Given player has ≥ segment cost steps, When they choose to travel, Then their step bank decreases by the segment cost and player location updates to destination.
2. Given player has < segment cost, When they open travel UI, Then travel option is disabled and UI explains shortfall.

---

### User Story 2 - Trading Basics (Priority: P1)
Player can buy goods in one town and sell in another, gaining gold and updating inventory.

**Why this priority**: Trading is the primary non-step progression mechanic.

**Independent Test**: In Town A with producing good X, buy X, travel to Town B, sell X at higher price; verify gold increases and inventory slot freed.

**Acceptance Scenarios**:
1. Given Town A produces Good X at low price, When player buys X, Then inventory contains X and gold decreases by buy price.
2. Given Town B demands X, When player sells X, Then gold increases by town's buy price and reputation updates.

---

### User Story 3 - Rumors & Ledger (Priority: P2)
Player receives rumor cards that hint at market shifts; rumors appear in Ledger for later reference.

**Why this priority**: Adds depth and ties into conspiracy; optional for casual players.

**Independent Test**: Trigger a rumor event; verify it appears in the Ledger tab and has expiration metadata.

**Acceptance Scenarios**:
1. Given a rumor tied to Good Y, When the player checks Ledger, Then rumor shows source, expiry, and implicated town.

---

### Edge Cases
- What if step sensor unavailable? App must fall back to accelerometer algorithm and surface calibration UI.
- What if Room DB migration fails? Provide safe migration or clear user-facing error with option to export data.

## Requirements *(mandatory)*

### Functional Requirements
- FR-001: System MUST count and persist steps locally using a foreground service.
- FR-002: System MUST allow players to spend banked steps to traverse a road segment between towns.
- FR-003: System MUST present a town market UI to buy/sell goods and update player gold/inventory.
- FR-004: System MUST show rumor cards and persist them in a Ledger view with expiry metadata.
- FR-005: System MUST support companion recruitment and maintain up to 3 active companions.
- FR-006: System MUST auto-resolve road encounters while the player is away and persist outcomes.
- FR-007: System MUST store all game state in Room and preferences in DataStore.

### Key Entities
- Town: id, name, region, producedGoods, demandedGoods, reputationState, storyState
- Good: id, name, baseValue, currentBuyPrice, currentSellPrice, supplyLevel
- PlayerState: id, name, class, gold, currentTownId, inventory, lifetimeSteps
- Companion: id, name, role, stats, bondLevel, questState
- Rumor: id, text, targetGoodId, sourceTownId, expiryCounter

## Success Criteria *(mandatory)*

### Measurable Outcomes
- SC-001: Player can complete a travel-action end-to-end (steps → travel → arrival) in under 10 seconds in normal device conditions.
- SC-002: Step tracking must record daily steps across 24 hours with ≥95% fidelity in normal walking conditions (measured via test harness).
- SC-003: Trading loop functional: players can perform buy→travel→sell with inventory changes and gold delta verified in tests.
- SC-004: At least 3 towns and 2 companions implemented with working story beats for Act 1–2.

## Assumptions
- All play is offline; no cloud sync required for v1.
- Sensor access: devices expose either `TYPE_STEP_COUNTER` or allow accelerometer-based fallback.
- Target platform: Android; Compose + Room architecture (NowInAndroid patterns available).
- No monetization or accounts required in v1.

---

## Notes & Open Questions
See project PRD for calibration, contraband UX, and economy tuning questions. Marked items requiring playtesting and tuning.

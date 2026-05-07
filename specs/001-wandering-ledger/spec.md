# Feature Specification: Wandering Ledger

**Feature Branch**: `001-wandering-ledger`
**Created**: 2026-05-06
**Status**: Ready for implementation scaffold
**Input**: Build an offline walking-and-trading game based on the Wandering Ledger concept.

## User Scenarios & Testing

### User Story 1 - Walk & Travel (Priority: P1)
A casual player walks during their day and opens the app to spend accumulated steps to move the character along roads between towns.

**Why this priority**: This is the core loop; without reliable step banking and travel there is no game.

**Independent Test**: Simulate step input, verify the step bank increases, spend steps to traverse one road segment, and verify arrival opens the destination town state.

**Acceptance Scenarios**:
1. Given the player has at least the segment cost in banked steps, when they choose to travel, then their step bank decreases by the segment cost and their location updates to the destination.
2. Given the player has fewer banked steps than the segment cost, when they view the travel option, then travel is disabled and the UI explains the shortfall.

### User Story 2 - Trading Basics (Priority: P1)
A player can buy goods in one town and sell them in another, changing gold and inventory.

**Why this priority**: Trading is the primary progression mechanic after walking.

**Independent Test**: In Town A, buy a locally abundant good, travel to Town B where it is demanded, sell it, and verify gold and inventory changes.

**Acceptance Scenarios**:
1. Given Town A sells Good X and the player has enough gold and inventory space, when the player buys Good X, then inventory contains Good X and gold decreases by the buy price.
2. Given Town B demands Good X and the player owns it, when the player sells Good X, then gold increases by the sell price, inventory decreases, and town reputation updates if applicable.

### User Story 3 - Rumors & Ledger (Priority: P2)
A player receives rumor cards that hint at market shifts, and those rumors remain available in a Ledger until they expire.

**Why this priority**: Rumors deepen the trading loop while remaining optional for casual play.

**Independent Test**: Trigger a rumor event, verify it appears in the Ledger, and verify expiration metadata changes after visits.

**Acceptance Scenarios**:
1. Given a rumor tied to a good or town, when the player opens the Ledger, then the rumor shows source, expiry, and implicated target.

## Edge Cases

- If the preferred step sensor is unavailable, the app must fall back to motion-based step detection and surface calibration.
- If local data migration fails, the app must avoid silent data loss and offer a clear recovery or export path.
- If the player attempts to recruit more than 3 active companions, the app must offer replace, switch, or decline without silently dropping companion data.
- If a road encounter resolves while the player is away, the result must be replayable from recorded initial state and seed.

## Functional Requirements

- **FR-001**: System MUST count and persist steps locally.
- **FR-002**: System MUST allow players to spend banked steps to traverse a road segment between towns.
- **FR-003**: System MUST present a town market to buy and sell goods and update player gold and inventory.
- **FR-004**: System MUST show rumor cards and persist them in a Ledger with expiry metadata.
- **FR-005**: System MUST support companion recruitment and maintain up to 3 active companions.
- **FR-006**: System MUST auto-resolve road encounters while the player is away and persist outcomes.
- **FR-007**: System MUST store game state locally and support app restart without losing progress.

## Companion Acceptance Criteria

- Recruitment persists and is restored after app restart.
- Active companion capacity is a hard maximum of 3.
- Duplicate recruitment is handled explicitly and does not create duplicate active companions.
- Removal, replacement, and temporary deactivation persist correctly.
- Bond level changes and companion-specific state are covered by deterministic tests.

## Key Entities

- **Town**: id, name, region, produced goods, demanded goods, reputation state, story state
- **Good**: id, name, base value, contraband flag
- **Player State**: id, name, class, gold, current town, inventory, lifetime steps
- **Companion**: id, name, role, stats, bond level, quest state, active state
- **Road Segment**: id, origin town, destination town, step cost, encounter pool
- **Rumor**: id, text, target good, source town, expiry counter

## Success Criteria

- **SC-001**: Player can complete travel end-to-end from banked steps to arrival. Median travel-action completion is at most 3 seconds and 95th percentile is at most 10 seconds when measured with the canonical latency benchmark harness.
- **SC-002**: Step tracking records daily steps across 24 hours with at least 95% fidelity in normal walking conditions. Fidelity is measured as F1 score against labeled step events on the defined dataset and devices.
- **SC-003**: Trading loop is functional: players can buy, travel, and sell with inventory changes and gold delta verified in tests.
- **SC-004**: Local persistence survives app restart for player state, inventory, companions, rumors, and event outcomes.

## Assumptions

- v1 is offline-only.
- v1 does not include accounts, monetization, cloud sync, or location/GPS gameplay.
- Target platform and implementation technology are defined in `plan.md`, not this stakeholder spec.
- Economy values, step costs, and encounter outcomes are intentionally small and hand-authored for v1 playtesting.

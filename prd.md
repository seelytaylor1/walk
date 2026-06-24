# Wandering Ledger PRD

## Product Intent

Wandering Ledger is an offline Android walking game where real-world steps become travel currency in a small hand-authored trade world. The player walks, banks steps, spends those steps on RoadSegments to travel between Towns, buys and sells Goods, collects Rumors, and gradually recruits Companions. Progress is measured in gold earned and the world gradually opened — not completion, but accumulation.

## v1 Goals

- Make walking feel useful without requiring GPS or cloud accounts.
- Deliver a playable loop: steps → travel → town → buy/sell → travel → profit.
- Keep the world compact for playtesting: 3 Towns, 4–6 Goods, 2 Companions, and a small Rumor pool.
- Persist all progress locally (PlayerState, InventoryItems, Companions, Ledger events) and survive app restarts.

## Non-Goals

- No cloud sync, accounts, monetization, GPS, multiplayer, or live-service economy in v1.
- No large procedural world in the first playable milestone.
- No claim of final step-fidelity performance until the benchmark harness and test traces exist.

## Player Progression Arc

The player begins with a small step bank, no Companions, and limited gold. Over time they:

1. Learn which RoadSegments are affordable and plan routes accordingly.
2. Exploit Rumors to buy low in one Town and sell high in another.
3. Recruit Companions that change travel (Scout reduces costs) or trading (Rogue unlocks contraband) behavior.
4. Build reputation in Towns to unlock better prices.

There is no win state in v1. Success is a player who returns after a walk and immediately knows what to do with their banked steps.

## Milestones

### Milestone 1 — First Playable

**Goal:** Prove the core step-spend-arrive loop works end to end.

**User Story:** As a player, I bank simulated or recorded steps, spend them on one RoadSegment, arrive at the destination Town, and the arrival persists across app restarts.

**Success Criteria:**
- A tester can launch the app, bank ≥ 500 simulated steps, select an affordable JourneyRouteOption, confirm travel, and see the destination Town open on the JourneyScreen.
- PlayerState.currentTownId and PlayerState.bankedSteps update correctly and survive process death.
- No crash or data loss on travel.

### Milestone 2 — Full v1 Loop

**Goal:** Deliver the complete buy/sell/rumor/companion loop with all 3 Towns and 2 Companions in place.

**User Stories:**
1. As a player, I want to buy and sell Goods at a Town so that I can earn gold by exploiting price differences.
2. As a player, I want Rumors to hint at where to find good prices so that walking feels purposeful.
3. As a player, I want to recruit a Companion so that my travel or trading is improved in a way I can feel.
4. As a player, I want to see a Ledger of past events so that I can understand my trading history.
5. As a player, I want CampState to activate when I stop moving so that idle time feels like a rest rather than a pause.

**Success Criteria:**
- A tester completes at least one profitable trade route (buy low, travel, sell high) without consulting the source code.
- At least one Companion is recruitable and their effect is observable.
- Rumors correctly target real price differences at least 50% of the time (false Rumors exist but are not the majority).

## Step Cost Model (Provisional)

Step costs are provisional until the calibration harness produces real traces. These numbers exist so design decisions can be made now.

| RoadSegment | Provisional Step Cost |
|---|---|
| Short (same region) | 1,000 steps |
| Medium (adjacent region) | 2,500 steps |
| Long (cross-region) | 5,000 steps |

A typical 30-minute walk yields approximately 3,000–4,000 steps. The goal is that one meaningful walk = one meaningful journey decision, not one completed journey.

Calibration copy and sensitivity defaults remain in Open Product Tuning until the benchmark harness exists.

## Open Product Tuning

These items require playtesting data before they can be finalized. Each has a provisional placeholder so work can proceed.

| Item | Provisional Value | Finalized By |
|---|---|---|
| Step costs per RoadSegment | See table above | Calibration milestone |
| Initial Goods prices and profit margins | 20–40% margin on cross-region trades | Milestone 2 playtest |
| Calibration copy and sensitivity defaults | Generic ("seems about right") | Calibration milestone |
| Companion recruitment pacing | Available after 3 successful trades | Milestone 2 playtest |
| Rumor frequency and expiry length | 1 Rumor per arrival, expires after 2 visits | Milestone 2 playtest |

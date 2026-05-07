# Wandering Ledger PRD

## Product Intent

Wandering Ledger is an offline Android walking game where real-world steps become travel currency in a small hand-authored trade world. The player walks, banks steps, spends those steps to travel between towns, buys and sells goods, collects rumors, and gradually recruits companions.

## v1 Goals

- Make walking feel useful without requiring GPS or cloud accounts.
- Deliver a playable loop: steps -> travel -> town -> buy/sell -> travel -> profit.
- Keep the world compact for playtesting: 3 towns, 4-6 goods, 2 companions, and a small rumor pool.
- Persist all progress locally and survive app restarts.

## Non-Goals

- No cloud sync, accounts, monetization, GPS, multiplayer, or live-service economy in v1.
- No large procedural world in the first playable milestone.
- No claim of final step-fidelity performance until the benchmark harness and test traces exist.

## First Playable Milestone

The first milestone proves User Story 1: simulated or recorded steps are banked, the player spends steps on one road, arrival persists, and the destination town view opens.

## Open Product Tuning

- Final step costs per road.
- Initial goods/prices and profit margins.
- Calibration copy and sensitivity defaults.
- Companion recruitment pacing.
- Rumor frequency and expiry length.

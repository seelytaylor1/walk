# Testdata Plan: Wandering Ledger

## Seed World

The v1 seed world should contain:

- 3 towns connected by at least 3 road segments.
- 4-6 goods with clear producer/demand relationships.
- 2 recruitable companions, with at most 1 active at game start.
- Starting player state with enough gold to buy one good and enough inventory for a buy-travel-sell loop.
- At least 2 rumor templates with source, target, and expiry metadata.

## Accelerometer Traces

Store recorded motion traces under `testdata/accelerometer-traces/`.

Each trace should include:

- device model and Android version,
- carried position,
- walking speed bucket,
- labeled step count or event timestamps,
- sensor sample timestamp and axis values.

The fidelity harness for SC-002 must compute precision, recall, and F1 from these traces.

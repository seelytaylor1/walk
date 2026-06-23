# Define WorldSnapshot and TravelOutcome data classes

**Type:** AFK  
**Blocked by:** None — can start immediately  
**Label:** enhancement

## What to build

Introduce two data classes in `core:data` that capture everything the travel transaction needs to read and everything it needs to write:

- `WorldSnapshot` — a point-in-time read of player state, road segment, destination town, active companions, and active rumors. No database handles, no flows — plain data.
- `TravelOutcome` — the complete set of mutations that a travel produces: player delta (steps spent, new location), rumors to insert/decrement, encounter outcome (gold/bond deltas), and an event log entry. Again, plain data — no DAO calls.

These types become the seam between "what the world looks like" and "what travel decides to do." They live alongside the existing engine types in `core:data`.

## Acceptance criteria

- [ ] `WorldSnapshot` data class compiles and is visible to other classes in `core:data`
- [ ] `TravelOutcome` data class compiles with fields covering: player delta, new rumors, encounter outcome (nullable), event log entry
- [ ] Both types are covered by at least one unit test that constructs them with realistic values
- [ ] No existing tests broken

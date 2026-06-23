# Inject seeded Random into RumorRepository generation methods

**Type:** AFK  
**Blocked by:** None — can start immediately (runs parallel to issues 02–04)  
**Label:** enhancement

## What to build

`RumorRepository.generateRumorForTownVisit()` and `generateRumorFromRoadEvent()` use an unseeded `Random()`, making their output non-deterministic and their tests unable to assert specific content.

Add a `seed: Long = System.nanoTime()` parameter to both generation methods and construct `Random(seed)` from it — the same pattern already used in `EncounterEngine`. Update callers (currently only `TravelPolicy` / `GameRepository`) to pass a seed derived from the travel's own seed, so the whole transaction is reproducible from a single seed.

## Acceptance criteria

- [ ] Both generation methods accept an optional `seed` parameter
- [ ] `RumorRepositoryTest` has at least one test that pins a seed and asserts specific rumor content
- [ ] Default behaviour (no seed passed) is unchanged — callers that don't pass a seed still get a random result
- [ ] All existing tests pass

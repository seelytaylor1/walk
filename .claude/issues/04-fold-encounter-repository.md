# Fold EncounterRepository into TravelPolicy and delete the class

**Type:** AFK  
**Blocked by:** `03-game-repository-use-travel-policy.md`  
**Label:** enhancement

## What to build

`EncounterRepository` is a one-caller pass-through: it calls `EncounterEngine.resolve()` then applies gold/bond deltas. Now that `TravelPolicy` owns encounter resolution as part of `compute()`, `EncounterRepository` has no remaining purpose.

Delete `EncounterRepository`. Move any logic not already in `EncounterEngine` or `TravelPolicy` to where it belongs. Update `TravelPolicy` to read companion data from `WorldSnapshot` (already there) rather than calling back into `CompanionRepository` inside the transaction.

This eliminates the re-entrancy hazard: `GameRepository → EncounterRepository → CompanionRepository` inside a single transaction.

## Acceptance criteria

- [ ] `EncounterRepository.kt` is deleted
- [ ] No remaining references to `EncounterRepository` anywhere in the codebase
- [ ] Bond and gold deltas from encounters are applied correctly (covered by existing integration test)
- [ ] All tests pass

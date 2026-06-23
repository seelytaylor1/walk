# Extract TravelPolicy as a pure function with unit tests

**Type:** AFK  
**Blocked by:** `01-world-snapshot-travel-outcome.md`  
**Label:** enhancement

## What to build

Extract a `TravelPolicy` object with a single pure function:

```kotlin
fun compute(snapshot: WorldSnapshot, seed: Long): TravelOutcome
```

This function encodes all travel rules: step cost check, rumor generation, encounter resolution, event log construction. It takes no database handles, launches no coroutines, and has no side effects. It delegates to `EncounterEngine` and `RumorRepository`'s generation logic (which should accept injected `Random` by this point — see issue 05).

The goal is that every travel rule is readable in one place, and every travel rule is testable without Room or Robolectric.

## Acceptance criteria

- [ ] `TravelPolicy.compute()` exists and compiles
- [ ] Unit tests cover: insufficient steps returns a failure outcome, valid travel produces correct player delta, rumor generation is called, encounter resolution is called
- [ ] Tests run without a database (no `TestDatabaseFactory`, no Robolectric)
- [ ] `GameRepository.travel()` is not yet changed — policy is additive at this stage
- [ ] No existing tests broken

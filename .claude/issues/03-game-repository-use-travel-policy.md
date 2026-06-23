# Refactor GameRepository.travel() to use TravelPolicy

**Type:** AFK  
**Blocked by:** `02-travel-policy-pure-function.md`  
**Label:** enhancement

## What to build

Refactor `GameRepository.travel()` so its transaction block has three clear phases:

1. **Read** — assemble a `WorldSnapshot` from the DAOs
2. **Decide** — call `TravelPolicy.compute(snapshot, seed)` to get a `TravelOutcome`
3. **Write** — apply the `TravelOutcome` mutations via DAOs

The existing 100+ line transaction block gets replaced by these three phases. All business logic moves to `TravelPolicy`; `GameRepository` becomes pure plumbing.

## Acceptance criteria

- [ ] `GameRepository.travel()` transaction block is three phases: read, compute, write
- [ ] No business logic remains inline in the transaction (step cost check, rumor rules, encounter rules all live in `TravelPolicy`)
- [ ] Existing `GameRepositoryTest` and `UserStory1TravelFlowTest` pass without modification
- [ ] Travel latency benchmark shows no regression

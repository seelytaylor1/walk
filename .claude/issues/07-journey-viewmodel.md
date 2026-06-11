# Extract JourneyViewModel from MainActivity

**Type:** AFK  
**Blocked by:** `06-viewmodel-architecture-decision.md`  
**Label:** enhancement

## What to build

Extract all journey-related state management from `MainActivity` into a `JourneyViewModel` (or mediator, per the decision in issue 06):

- The observe job for game/travel state
- The `buildJourneyScreenState()` call and its resulting `StateFlow`
- The travel action callback (`onTravel`)
- Camp state management

`JourneyViewModel` exposes a single `StateFlow<JourneyScreenState>`. `MainActivity` passes the flow to `JourneyScreen` and nothing else.

## Acceptance criteria

- [ ] `JourneyViewModel` exists in `feature:journey` (or `app`, per architecture decision)
- [ ] `MainActivity` no longer contains journey observe jobs or state builders
- [ ] `JourneyScreen` renders correctly from the ViewModel's `StateFlow`
- [ ] Manual smoke test: travel to a new town works end-to-end
- [ ] No existing tests broken

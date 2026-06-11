# Extract JourneyViewModel from MainActivity

**Type:** AFK  
**Blocked by:** `06-viewmodel-architecture-decision.md`  
**Label:** enhancement

> **Per issue 06: Jetpack ViewModel (Option A).** `JourneyViewModel` extends
> `androidx.lifecycle.ViewModel`, builds its `StateFlow` with `stateIn(viewModelScope, …)`,
> and is obtained in `MainActivity` via the `viewModel { }` factory (lifecycle 2.7.0).

## What to build

Extract all journey-related state management from `MainActivity` into a `JourneyViewModel`:

- The observe job for game/travel state
- The `buildJourneyScreenState()` call and its resulting `StateFlow`
- The travel action callback (`onTravel`)
- Camp state management

`JourneyViewModel` exposes a single `StateFlow<JourneyScreenState>`. `MainActivity` passes the flow to `JourneyScreen` and nothing else.

## Acceptance criteria

- [x] `JourneyViewModel` exists in `app` (per the issue 06 decision; placed in `app`
      to avoid adding a `core:data` dependency to `feature:journey`)
- [x] `MainActivity` no longer contains journey observe jobs or state builders
      (state building lives in `JourneyViewModel.refresh`; the old
      `refreshJourneyScreen` builder is gone, replaced by a thin `showJourney`)
- [x] `JourneyScreen` renders from the ViewModel's `StateFlow` (`journeyViewModel.state`)
- [ ] **Manual smoke test: travel to a new town works end-to-end** — NOT YET RUN
      (no emulator available in the implementing session; needs a human/device run)
- [x] No existing tests broken (`:core:data:testDebugUnitTest` green; `:app:assembleDebug` succeeds)

### Implementation notes

- Added an application-scoped `AppContainer` + `WanderingLedgerApp` so the DB/repositories
  outlive the Activity — required for a retained Jetpack ViewModel to be safe across
  configuration changes (previously `MainActivity` reopened/closed the DB each `onCreate`/`onDestroy`).
- Audio, haptics and navigation stay in `MainActivity`; the ViewModel emits `JourneyEffect`s
  (`TravelBegin`, `TravelBlocked`, `Arrived`, `StartAmbient`, `CommentaryGenerated`) that the
  Activity performs, keeping the ViewModel free of Android UI/sensor/navigation concerns.
- Shared "most recent commentary" preserved: the ViewModel emits `CommentaryGenerated`, which
  `MainActivity` stores in `latestCompanionCommentary` for the (not-yet-extracted) companions screen.

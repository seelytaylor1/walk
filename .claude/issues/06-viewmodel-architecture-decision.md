# HITL: Decide ViewModel approach before extracting feature mediators

**Type:** HITL  
**Blocked by:** None — can start immediately  
**Label:** question

## What to decide

Before extracting per-feature ViewModels from `MainActivity`, agree on the approach. The two main options:

**Option A — Jetpack ViewModel**  
Use `androidx.lifecycle.ViewModel` with `viewModelScope`. Each feature gets a `ViewModel` subclass. Lifecycle management is automatic; ViewModels survive configuration changes. Requires adding the `lifecycle-viewmodel-ktx` dependency if not already present.

**Option B — Plain coroutine scope mediators**  
Keep the current pattern (manual coroutine scope, manual job cancellation) but move it out of `MainActivity` into per-feature classes. No new dependency. Simpler to reason about, but lifecycle management stays manual.

The answer shapes issues 07–10 significantly.

## Acceptance criteria

- [ ] Decision recorded here (edit this issue with the chosen approach and rationale)
- [ ] If Option A: confirm `lifecycle-viewmodel-ktx` version to use
- [ ] Issues 07–09 updated to reflect the decision before work begins

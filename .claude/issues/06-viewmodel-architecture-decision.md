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

## Decision — Option A (Jetpack ViewModel)

**Chosen 2026-06-11.** Each feature gets an `androidx.lifecycle.ViewModel` subclass
that exposes a single `StateFlow<…ScreenState>` built with `stateIn(viewModelScope, …)`.
`MainActivity` obtains them with the `viewModel { … }` composable factory and passes
their flows to the screens.

**Rationale:** ViewModels survive configuration changes (the manual-scope pattern
loses in-flight state on rotation), `viewModelScope` cancels automatically so the
manual job-cancellation bookkeeping in `MainActivity` disappears, and this is the
idiomatic Android shape future contributors expect. The one cost — a new dependency —
is small and the project already uses the AndroidX/Compose stack it slots into.

**Version:** lifecycle `2.7.0` (matches Compose BOM `2024.01.00` / Compose 1.5.x).
Add to the version catalog:

- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0` — `viewModelScope`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0` — the `viewModel { }` factory

## Acceptance criteria

- [x] Decision recorded here (edit this issue with the chosen approach and rationale)
- [x] If Option A: confirm `lifecycle-viewmodel-ktx` version to use → **2.7.0**
- [x] Issues 07–09 updated to reflect the decision before work begins

# Implementation Plan: Wandering Ledger

**Branch**: `001-wandering-ledger` | **Date**: 2026-05-06 | **Spec**: specs/001-wandering-ledger/spec.md
**Input**: Feature specification from `/specs/001-wandering-ledger/spec.md`

## Summary

Deliver a v1 mobile experience implementing the core Wandering Ledger loop: reliable step tracking (accelerometer fallback), step-based travel between hand-crafted towns, a basic trading market (buy/sell), 3 towns and 2 companions, and a Ledger for rumor cards. Use NowInAndroid multi-module patterns (Compose, Room, DataStore). Prioritize offline-first architecture, deterministic tests, and performance budgets for step service and UI responsiveness.

## Technical Context

**Language/Version**: Kotlin (JDK 17), Android SDK 33+
**Primary Dependencies**: AndroidX Compose, Room, DataStore, Kotlin Coroutines & Flow
**Storage**: Room for game state; DataStore for preferences
**Testing**: JUnit, AndroidX Test (instrumentation), Robolectric for JVM UI tests
**Target Platform**: Android (API 26+ recommended, 33+ target)
**Project Type**: Mobile app (multi-module: app + feature modules)
**Performance Goals**: UI 60fps target; step-service battery overhead minimal (foreground service CPU < 2% typical); step counting accuracy ≥95% in normal walking
**Constraints**: Fully offline; no GPS/location permissions; accelerometer-only step detection fallback; limited device memory/support
**Scale/Scope**: v1 local single-player experience; 3 towns, 2 companions, core loop playable end-to-end

## Constitution Check

- Gate: Code Quality — must include linters, formatters, and PR templates. (Plan: enable Kotlin linter, ktfmt/ktlint in CI)
- Gate: Testing Standards — Unit & Integration tests required for step tracker, Room schema, and market logic. (Plan: include unit tests + one instrumentation test for step service)
- Gate: UX Consistency — Use design system module `core/designsystem` for cards and shared components.
- Gate: Performance Requirements — Benchmarks for step service and market simulation must be created in Phase 0 research.

- Detailed performance & test budget (from research):
  - Step accuracy: F1 ≥ 0.95 measured on representative devices (0.8–1.6 m/s walking speeds).
  - Latency (SC-001): median travel-action completion ≤ 3s; 95th percentile ≤ 10s on mid/high devices.
  - Battery/CPU: foreground step service additional CPU ≤ 2% and battery impact ≤ 1–2%/hour on mid-range devices.

All gates satisfied by design provided Phase 0 benchmarks meet the thresholds above; performance validation required before implementation proceeds.

## Project Structure

Selected structure: NowInAndroid-style multi-module Android project.

```
app/
feature/
  worldmap/
  town/
  companions/
  journal/
  character/
core/
  model/
  data/
  database/
  steptracker/
  designsystem/
  ui/
  testing/
```

**Structure Decision**: Use existing NowInAndroid modules (adapted) so feature modules map directly to UI flows. `core/steptracker` implements the foreground service and sensor logic; `core/database` contains Room entities and DAOs.

## Complexity Tracking

No constitution violations requiring formal justification.

## Phase 0 — Research Tasks (Resolve NEEDS CLARIFICATION)

1. Research accelerometer-based pedometer fallback: choose or implement an algorithm with reference implementation (Task: Research & choose pedometer lib/algorithm).
2. Define step calibration UX and acceptable default step costs (Task: Playtest calibration parameters).
3. Benchmark step service battery/CPU impact and produce performance budget (Task: Micro-benchmarks + instrumentation test harness).
4. Define Room schema migration strategy for v1→v2 (Task: Document migration plan).

Deliverable: `research.md` (resolve unknowns above)

## Phase 1 — Design & Contracts

1. Create `data-model.md`: domain entities (`Town`, `Good`, `PlayerState`, `Companion`, `RoadSegment`, `Rumor`) with fields and relationships.
2. Define contracts: local repository interfaces, DAO contracts, and UI events for travel/buy/sell actions. Place under `specs/001-wandering-ledger/contracts/`.
3. Quickstart: minimal developer quickstart with build/run instructions and test commands.
4. Update agent context `.github/copilot-instructions.md` to point to this plan file (if applicable).

Deliverables: `data-model.md`, `contracts/*`, `quickstart.md`

## Phase 2 — Implementation Tasks (high-level)

1. Implement `core:database` Room entities & DAOs (with migrations).
2. Implement `core:steptracker` foreground service with sensors and repository exposing `Flow<Int>` of banked steps.
3. Implement `feature:worldmap` travel UI and travel controller.
4. Implement `feature:town` market UI and market engine (supply/demand + price history).
5. Implement `feature:journal` Ledger UI and rumor persistence.
6. Implement companion system basics and auto-resolve encounters.
7. Add tests: unit tests for market engine, step tracker algorithm, and Room DAOs; instrumentation test for step service.
8. Integrate CI: lint, format, unit tests, instrumentation smoke test on emulator.

## Phase 3 — Release Criteria

- Core loop playable end-to-end with 3 towns and 2 companions.
- Tests passing in CI; performance budgets met for step service and UI responsiveness.
- PR contains migration notes and changelog.

## Next Steps (immediate)

1. Create `research.md` resolving pedometer algorithm and calibration (Phase 0).
2. Create `data-model.md` skeleton (Phase 1).
3. Add `tasks.md` by running `/speckit.tasks` or by generating initial tasks from this plan.


# Implementation Plan: Wandering Ledger

**Branch**: `001-wandering-ledger` | **Date**: 2026-05-06 | **Spec**: specs/001-wandering-ledger/spec.md
**Input**: Feature specification from `/specs/001-wandering-ledger/spec.md`

## Summary

Deliver a v1 Android experience implementing the core Wandering Ledger loop: reliable step tracking with motion fallback, step-based travel between hand-crafted towns, a basic trading market, 3 towns, 2 companions, and a Ledger for rumor cards. Use NowInAndroid-style multi-module patterns with Compose, Room, and DataStore. Prioritize offline-first architecture, deterministic tests, and performance budgets for step service and UI responsiveness.

## Technical Context

**Language/Version**: Kotlin (JDK 17), Android SDK 34 target
**Primary Dependencies**: AndroidX Compose, Room, DataStore, Kotlin Coroutines and Flow
**Storage**: Room for game state; DataStore for preferences and calibration
**Testing**: JUnit, AndroidX Test instrumentation, Robolectric for JVM UI tests
**Target Platform**: Android API 26+
**Project Type**: Mobile app with app, core, and feature modules
**Performance Goals**: UI 60fps target; foreground step-service CPU under 2% typical; step counting F1 >= 0.95 in normal walking
**Constraints**: Fully offline; no accounts; no GPS or location permissions; motion fallback only when preferred step sensor is unavailable or anomalous
**Scale/Scope**: v1 local single-player experience; 3 towns, 2 companions, core loop playable end-to-end

## Constitution Check

- Code Quality: Kotlin formatting/linting must run in CI before merge.
- Testing Standards: steptracker, core/database, and market engine require unit tests and at least 80% module coverage once implemented.
- Migration Safety: Room migrations require deterministic migration tests.
- Performance Requirements: latency and step-fidelity benchmark harnesses are tracked by T038/T039 before claiming SC-001/SC-002.
- UX Consistency: shared UI should use `core/designsystem` components and tokens.
- Offline First: no cloud dependency is allowed in v1.

Detailed performance and test budget from research:

- Step accuracy: F1 >= 0.95 measured on representative devices at 0.8-1.6 m/s walking speeds.
- Latency: median travel-action completion <= 3s and 95th percentile <= 10s on mid/high devices.
- Battery/CPU: foreground step service additional CPU <= 2% and battery impact <= 1-2%/hour on a mid-range device.

## Project Structure

Selected structure: NowInAndroid-style multi-module Android project.

```text
app/
feature/
  worldmap/
  town/
  companions/
  ledger/
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

`core/steptracker` owns sensor and banked-step logic. `core/database` owns Room entities, DAOs, and migrations. Feature modules own UI and UI events. Contracts live under `specs/001-wandering-ledger/contracts/`.

## Design & Contracts

- `data-model.md` defines persisted entities and relationship shape.
- `contracts/repositories.md` defines repository boundaries for game state, market, step bank, companions, rumors, and encounters.
- `contracts/daos.md` defines Room DAO expectations and transaction boundaries.
- `contracts/ui-actions.md` defines UI event inputs and screen state outputs for travel, town market, ledger, companions, and calibration.
- `quickstart.md` documents local setup, build commands, and validation flow.

## Implementation Slices

1. Scaffold Android multi-module project and verify Gradle sync/build.
2. Implement `core/model`, `core/database`, `core/steptracker`, and seed data.
3. Implement User Story 1: simulated steps, banked-step spend, travel persistence, and town arrival.
4. Implement User Story 2: market buy/sell, inventory, price display, and buy-travel-sell integration test.
5. Implement User Story 3 and P2 systems: Ledger rumors, companion basics, deterministic encounters.
6. Add benchmark harnesses, migration tests, coverage gates, release notes, and PR templates.

## Release Criteria

- Core loop playable end-to-end with 3 towns and 2 companions.
- CI runs lint, formatting, unit tests, and smoke tests.
- Critical modules meet coverage gates after implementation.
- Performance budgets are measured before claiming SC-001 and SC-002.
- PR includes migration notes and changelog entry when schema changes are introduced.

# Tasks: Wandering Ledger

**Input**: specs/001-wandering-ledger/
**Prerequisites**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md

## Phase 0: Readiness Hardening

- [x] T000a Switch/create feature branch `001-wandering-ledger`
- [x] T000b Finalize stakeholder spec and clear requirements checklist
- [x] T000c Add contracts for repository, DAO, and UI action boundaries
- [x] T000d Add developer quickstart and seed/testdata plan
- [x] T000e Align Ledger module naming and implementation task order
- [x] T000f Keep existing constitution changes and replace deferred ratification placeholder

---

## Phase 1: Setup

- [x] T001 [P] Initialize Android multi-module project structure (`app/`, `core/`, `feature/`)
- [x] T002 [P] Add CI config for lint, formatting, and unit tests (`.github/workflows/android-ci.yml`)
- [x] T003 [P] Configure final Kotlin linters/formatters and Gradle check tasks
- [x] T004 [P] Add developer quickstart (`specs/001-wandering-ledger/quickstart.md`)
- [x] T004a Add Gradle wrapper files once local Java/Gradle tooling is available

---

## Phase 2: Foundational

- [x] T005 Set up Room database module and migration framework (`core/database/`)
- [x] T006 [P] Define Room entities and DAOs for `Town`, `Good`, `PlayerState`, `Companion`, `RoadSegment`, `Rumor`
- [x] T007 Implement `core:steptracker` service skeleton and repository exposing banked-step state
- [x] T008 [P] Create `core/model` domain classes mapping to database entities
- [x] T009 Add shared design system module with app theme and reusable components
- [x] T010 Add testing harness and test doubles module with in-memory Room support

---

## Phase 3: User Story 1 - Walk & Travel (P1 MVP)

**Goal**: Track steps, bank them, and spend steps to traverse a single road segment; arrival opens town view.

**Independent Test**: Simulate steps, spend to travel, verify `PlayerState.currentTownId` updates and step bank decreases.

- [x] T011 [US1] Verify/refine `specs/001-wandering-ledger/data-model.md`
- [x] T012 [P] [US1] Implement step bank repository and unit tests (`core/steptracker/src/test/...`)
- [x] T013 [US1] Implement travel controller that consumes step bank and updates `PlayerState`
- [x] T014 [US1] Implement `RoadSegment` step cost handling and unit tests
- [x] T015 [US1] Implement `feature/worldmap` UI: travel card and step-spend flow
- [x] T016 [US1] Wire arrival event to open `feature/town` view and persist arrival state
- [x] T017 [US1] Add instrumentation test for simulated step input and travel flow

---

## Phase 4: User Story 2 - Trading Basics (P1)

**Goal**: Buy goods in one town and sell in another; inventory and gold update accordingly.

- [x] T018 [US2] Implement market engine: price calculation, supply/demand updates, price history
- [x] T019 [US2] Implement `feature/town` market UI with buy/sell actions and price display
- [x] T020 [US2] Implement inventory system and UI
- [ ] T021 [US2] Add unit tests for market engine scenarios
- [~] T022 [US2] Hook buy/sell actions into Room persistence and update `PlayerState`
- [~] T023 [US2] Add integration test simulating buy-travel-sell loop

---

## Phase 5: User Story 3 - Rumors & Ledger (P2)

**Goal**: Rumor events surface and are saved to the Ledger UI with expiry metadata.

- [~] T024 [US3] Implement `Rumor` entity and DAO, including expiry counter logic
- [~] T025 [P] [US3] Implement rumor generation hooks for road events and town visits
- [~] T026 [US3] Implement `feature/ledger` UI and rumor list view
- [~] T027 [US3] Add unit tests for rumor expiry, false rumor tagging, and persistence

---

## Phase 6: Companion System & Encounters (P2)

- [~] T028 Implement companion model, bond levels, and recruitment triggers
- [~] T029 Implement deterministic auto-resolve encounter engine for road events
- [~] T030 Add companion UI roster and bond progression interactions
- [~] T031 Add unit tests for auto-resolve outcomes and companion state persistence
- [~] T041 [P] Define deterministic seeds and canonical encounter scenarios, then implement replay tests

---

## Phase 7: Quality Gates & Release

- [~] T032 [P] Add telemetry hooks for step accuracy, travel latencies, and market anomalies
- [~] T033 [P] Add performance benchmark harness for step service battery/CPU
- [~] T034 Update project documentation and README
- [~] T035 Add changelog, migration notes, and PR template updates
- [~] T036 [P] Add CI coverage gate for critical modules after coverage report paths exist
- [~] T037 [P] Add tests to reach module coverage thresholds
- [~] T038 [P][BLOCKER] Implement latency benchmark harness for SC-001 and add CI smoke job
- [~] T039 [P][BLOCKER] Implement fidelity test harness for SC-002
- [~] T040 [P][BLOCKER] Create dataset and device target list for fidelity testing
- [~] T042 [P] Implement calibration UI prototype and onboarding flow
- [~] T043 [P] Create playtest plan and collect representative walking vectors
- [~] T044 [P] Add simulation tests for preferred step sensor absence and motion fallback

---

## Dependencies & Execution Order

- Phase 1 setup must finish before foundational modules.
- Phase 2 foundational modules must finish before user-story implementation.
- User Story 1 is the first playable milestone.
- User Story 2 depends on player state, inventory, roads, and persistence.
- Companion and encounter systems depend on player state, road events, and persistence.
- CI coverage gates become enforceable after Gradle modules and coverage report paths exist.

## Parallel Opportunities

- Tasks marked `[P]` are parallelizable across team members after their phase prerequisites are met.
- Unit tests and model implementation can proceed in parallel with UI work once contracts are stable.

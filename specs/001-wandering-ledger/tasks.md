# Tasks: Wandering Ledger

**Input**: specs/001-wandering-ledger/
**Prerequisites**: plan.md, spec.md

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 [P] Initialize Android multi-module project structure (`app/`, `core/`, `feature/`) in repository
- [ ] T002 [P] Add CI config for lint, formatting, and unit tests (`.github/workflows/android-ci.yml`)
- [ ] T003 [P] Configure Kotlin linters/formatters (`ktlint`/`ktfmt`) and Gradle check tasks
- [ ] T004 [P] Add developer quickstart (`specs/001-wandering-ledger/quickstart.md`) with build and run steps

---

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T005 Setup Room database module and migration framework (`core/database/`)
- [ ] T006 [P] Define Room entities and DAOs for core models: `Town`, `Good`, `PlayerState`, `Companion`, `RoadSegment`, `Rumor` (`core/database/src/main/java/...`)
- [ ] T007 Implement `core:steptracker` step-service skeleton and repository exposing `Flow<Int>` (`core/steptracker/`)
- [ ] T008 [P] Create `core/model` domain classes mapping to database entities (`core/model/`)
- [ ] T009 Add shared design system module with card components and theme tokens (`core/designsystem/`)
- [ ] T010 Add testing harness and test doubles module (`core/testing/`) with in-memory Room for unit tests

---

## Phase 3: User Story 1 - Walk & Travel (Priority: P1) 🎯 MVP

**Goal**: Track steps, bank them, and spend steps to traverse a single road segment; arrival opens town view.

**Independent Test**: Simulate steps, spend to travel, verify `PlayerState.currentTownId` updates and step bank decreased.

- [ ] T011 [US1] Verify/Refine `specs/001-wandering-ledger/data-model.md` with entity fields and relationships (file exists) (FR-003)
- [ ] T012 [P] [US1] Implement step bank repository and unit tests (`core/steptracker/src/test/...`) (FR-001)
- [ ] T013 [US1] Implement travel controller that consumes step bank and updates `PlayerState` (`feature/worldmap/src/main/...`)
- [ ] T014 [US1] Implement `RoadSegment` step cost computation and unit tests (`core/model/` + `core/steptracker` tests)
- [ ] T015 [US1] Implement `feature/worldmap` UI: travel card and step-spend flow (`feature/worldmap/src/main/...`)
- [ ] T016 [US1] Wire arrival event to open `feature/town` view and persist arrival state in Room
- [ ] T017 [US1] Instrumentation test: emulate step input and perform travel flow on emulator/tests (FR-002)

---

## Phase 4: User Story 2 - Trading Basics (Priority: P1)

**Goal**: Buy goods in one town and sell in another; inventory and gold update accordingly.

**Independent Test**: Buy Good X in Town A, travel to Town B, sell X; assert gold/inventory changes.

- [ ] T018 [US2] Implement market engine: price calculation, supply/demand updates, price history (`core/data/market/`)
- [ ] T019 [US2] Implement `feature/town` market UI with buy/sell actions and price display (`feature/town/src/main/...`)
- [ ] T020 [US2] Implement inventory system and UI (`core/model` + `feature/character`)
- [ ] T021 [US2] Add unit tests for market engine scenarios (buy/sell, flooding, recovery)
- [ ] T022 [US2] Hook buy/sell actions into Room persistence and update `PlayerState` (`core/database` DAOs)
- [ ] T023 [US2] Add integration test simulating buy→travel→sell loop (FR-003)

---

## Phase 5: User Story 3 - Rumors & Ledger (Priority: P2)

**Goal**: Rumor events surface and are saved to the Ledger UI with expiry metadata.

**Independent Test**: Trigger rumor generation, view Ledger, confirm expiry behavior.

- [ ] T024 [US3] Implement `Rumor` entity and DAO, including expiry counter logic (`core/database`)
- [ ] T025 [P] [US3] Implement rumor generation hooks for road events and town visits (`core/data/events/`)
- [ ] T026 [US3] Implement Ledger UI and rumor list view (`feature/ledger/src/main/...`)
- [ ] T027 [US3] Add unit tests for rumor expiry, false rumor tagging, and persistence (FR-004)

---

## Phase 6: Companion System & Encounters (Priority: P2)

- [ ] T028 Implement `Companion` model, bond levels, and recruitment triggers (`core/model`, `feature/companions`)
- [ ] T029 Implement auto-resolve encounter engine for road events affecting companions (`core/data/encounters`)
- [ ] T030 Add companion UI roster and bond progression interactions (`feature/companions`)
- [ ] T031 Add unit tests for auto-resolve outcomes and companion state persistence (FR-005, FR-006)

---

## Phase 7: Polish, Metrics & Release

- [ ] T032 [P] Add telemetry hooks for step accuracy, travel latencies, and market anomalies (`core/telemetry/`)
- [ ] T033 [P] Performance tuning: benchmark step service battery and reduce overhead to target budget
- [ ] T034 Documentation: update `specs/001-wandering-ledger/quickstart.md` and `README.md`
- [ ] T035 Release prep: changelog, migration notes, and PR template updates
 - [ ] T036 [P] Add CI coverage gate: configure job to fail when module coverage < 80% (steptracker, market engine, core/database)
 - [ ] T037 [P] Implement unit test coverage targets for critical modules and add tests to reach thresholds (steptracker, market engine, DAOs)

---

## Phase 0b: Test Harnesses & Research (Blocking per constitution)

- [ ] T038 [P][BLOCKER] Implement latency benchmark harness for SC-001 (instrumentation + benchmark tests) and add CI job to run it.
- [ ] T039 [P][BLOCKER] Implement fidelity test harness for SC-002 (define dataset, devices, metrics such as precision/recall/F1) and add reproducible test cases.
- [ ] T040 [P][BLOCKER] Create dataset and device target list for fidelity testing (document devices, walking speeds, and acceptance thresholds) and store under `specs/001-wandering-ledger/research/`.
- [ ] T041 [P] Expand `T029` into deterministic auto-resolve tests: T041a define deterministic seeds and scenarios; T041b implement unit/integration tests to validate outcomes and edge cases.

---

## Playtest & Calibration Tasks (from research.md)

- [ ] T042 [P] Implement calibration UI prototype and quick onboarding flow; persist calibration profile in DataStore.
- [ ] T043 [P] Create playtest plan and collect representative walking vectors (walking speeds 0.8–1.6 m/s) for fidelity dataset.
- [ ] T044 [P] Add simulation tests for `TYPE_STEP_COUNTER` absence and verify accelerometer fallback meets minimum fidelity.

---

## Updates & Blocking Notes

- `T002`, `T036`, and `T037` are elevated to pre-merge blockers to satisfy the constitution's testing and CI requirements. Ensure PRs cannot merge on `001-wandering-ledger` until these tasks are satisfied.

---

## Dependencies & Execution Order

- Complete Phase 1 → Phase 2 (Foundation) must finish before user stories begin
- User stories (Phase 3–5) can proceed in parallel after Foundation
- Companion system depends on PlayerState, inventory, and encounter engine

---

## Parallel Opportunities

- Tasks marked `[P]` are parallelizable across team members
- Unit tests and model implementations can be done in parallel with UI work once DAOs/interfaces are defined

---

## Notes

- Keep tasks granular and include exact file paths in PRs.
- Mark tasks done by adding a single-line PR that references the task ID.

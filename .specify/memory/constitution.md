# Project Constitution

This constitution defines non-negotiable gates and quality requirements for features and PRs.

## Principles

- **Code Quality (MUST)**: All production Kotlin/Java code must pass linters and formatters configured in CI (ktlint/ktfmt or equivalent) before merge.
- **Testing (MUST)**: Critical modules (steptracker, core/database, market engine) MUST include unit tests and maintain at least 80% coverage for the module; CI must fail on missing tests for these modules.
- **Migration Safety (MUST)**: Room schema migrations MUST be deterministic and include automated migration tests; user-visible data loss is disallowed without explicit UX flow for export.
- **Performance Gates (MUST)**: Performance budgets for step tracking and travel latency (as defined in the feature `spec.md`) are enforced; benchmark harnesses referenced in tasks T038/T039 MUST be present for validation.
- **UX Consistency (SHOULD)**: Use `core/designsystem` components for shared UI elements and follow design tokens.
- **Offline First (MUST)**: v1 features must work fully offline with no cloud dependencies.

## Enforcement

- CI must include checks for linters, unit tests, and benchmark harness smoke runs where applicable.
- PRs that introduce schema migrations must include migration tests and documented migration plan.
# Spec Constitution
<!--
Sync Impact Report
- Version change: none (template -> v1.0.0)
- Modified principles: Added Code Quality, Testing Standards, UX Consistency, Performance Requirements
- Added sections: Additional Constraints, Development Workflow
- Removed sections: placeholder tokens replaced
- Templates requiring updates: .specify/templates/plan-template.md (⚠ pending), .specify/templates/spec-template.md (⚠ pending), .specify/templates/tasks-template.md (⚠ pending)
- Follow-up TODOs: none
-->

## Core Principles

### I. Code Quality (NON-NEGOTIABLE)
All code MUST be readable, maintainable, and self-documenting. Enforce deterministic formatting, meaningful naming, and small, single-responsibility modules. Pull requests MUST include clear intent, design rationale, and a minimal changelog entry. Complexity increases MUST be justified with measurable benefits.

### II. Testing Standards (NON-NEGOTIABLE)
Testing is mandatory across levels: unit, integration, and system. Unit tests MUST cover critical logic paths and edge cases with >80% coverage for new modules; integration tests MUST validate contracts between components; end-to-end tests SHOULD cover primary user flows. Tests MUST be fast, deterministic, and included in CI gates. Test-first practices (TDD) are STRONGLY ENCOURAGED for complex features.

### III. User Experience Consistency
Products and libraries MUST present consistent UX patterns: error messaging, input validation, and feedback loops. Design tokens, component behaviors, and accessibility basics (keyboard navigation, semantic markup, color contrast) MUST be followed. UX regressions are considered bugs and must block releases until resolved.

### IV. Performance Requirements
Performance targets MUST be defined for features with measurable SLAs (latency, memory, throughput). New code MUST be evaluated for algorithmic complexity; shipping code MUST meet agreed performance budgets. Performance regressions identified by benchmarks or telemetry MUST be triaged and remediated based on impact.

## Additional Constraints
Security, privacy, and licensing constraints that affect implementation MUST be called out in the spec for any new work. Dependencies MUST be vetted for maintenance and license compatibility. Backwards-incompatible changes MUST follow the versioning and migration guidance in Governance.

## Development Workflow & Quality Gates
- All changes go through PRs with at least one approving reviewer unrelated to the author.
- CI MUST run linters, formatters, tests, and basic security scans on every PR.
- Releases require a changelog entry, migration notes for breaking changes, and automated release artifacts.

## Governance
The constitution defines mandatory engineering practices for the project. Amendments MUST be proposed as a PR against this file and include: rationale, migration plan, and test/automation changes. Non-urgent changes require a simple majority approval from core maintainers; breaking governance changes require a documented migration plan and explicit ratification.

**Version**: 1.0.0 | **Ratified**: 2026-05-06 | **Last Amended**: 2026-05-06

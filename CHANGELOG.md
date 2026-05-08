# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - Phase 7: Quality Gates

### Added
- Telemetry module (`core:telemetry`) with event collection infrastructure
- Step accuracy telemetry: detects negative deltas, excessive bursts (>500 steps), zero steps, duplicate timestamps
- Travel latency telemetry: tracks travel start, completion, and success/failure
- Market anomaly telemetry: price spikes (>50%), crashes (>30%), supply depletion, unusual volume (>100% deviation)
- Performance benchmark harness for step service (`StepTrackerServiceBenchmark`)
  - Single-step recording latency
  - Burst step recording (500 steps)
  - High-frequency throughput (>100 steps/sec)
  - Memory allocation per operation
  - Anomaly detection performance

### Changed
- Updated README with telemetry section and benchmark test commands
- Version updated to 0.2.0 (dev)

### Fixed
- MainActivity.kt: Added missing EncounterRepository to GameRepository constructor
- MainActivity.kt: Added missing onInteract callback for CompanionsActions
- MainActivity.kt: Removed duplicate code block

## [0.1.0] - First Playable Milestone

### Added
- Multi-module Android project structure (app/, core/, feature/)
- Room database with entities: Town, Good, PlayerState, Companion, RoadSegment, Rumor
- Step tracking service with step bank repository
- Travel system: road segments, step cost, arrival events
- Market system: buy/sell goods, price calculation, supply/demand
- Inventory system with capacity limits
- Rumor generation from road events and town visits
- Companion system with bond levels and recruitment
- Encounter engine for deterministic road events
- Design system with reusable UI components
- Unit and integration tests

### Features
- Seed world with initial towns and roads
- Simulate or record steps to bank
- Travel between towns by spending banked steps
- Buy and sell goods in towns
- Collect rumors that appear in ledger
- Recruit companions from towns
- Encounter events on roads

## [0.0.1] - Project Initialization

### Added
- Gradle wrapper and project configuration
- CI workflow for Android builds
- Kotlin linters and formatters configured
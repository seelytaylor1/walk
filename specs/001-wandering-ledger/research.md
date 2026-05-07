# Research: Pedometer Algorithm, Calibration, and Performance Benchmarks

**Feature**: Wandering Ledger
**Path**: specs/001-wandering-ledger/research.md
**Created**: 2026-05-06

## Goal
Resolve technical unknowns for Phase 0:
- Choose accelerometer-based pedometer algorithm (fallback when `TYPE_STEP_COUNTER` unavailable)
- Define step calibration UX and default step-cost parameters
- Define performance benchmarks and test harness for step service (accuracy & battery)

## 1. Options for Step Counting

### 1.1 Use platform `TYPE_STEP_COUNTER` (preferred when available)
- Low-power, hardware-accelerated sensor provided by many devices.
- Pros: High accuracy, low battery impact, persists across reboots on some devices.
- Cons: Not universally available; OEM differences in behavior.

### 1.2 Implement accelerometer-based pedometer algorithm (fallback)
- Candidate algorithms / libraries:
  - Google Android SensorStepDetector examples (reference implementation)
  - Open-source algorithms: `StepDetector` implementations using peak detection + low-pass filtering
  - DSP approach: band-pass filter + peak detection on Z-axis magnitude; heuristics for stride timing
- Pros: Works where `TYPE_STEP_COUNTER` absent; full control for tuning and testing
- Cons: Higher CPU/battery cost; requires careful tuning to avoid false positives/negatives

### 1.3 Hybrid approach (recommended)
- Use `TYPE_STEP_COUNTER` when available.
- Fallback to accelerometer algorithm only when step-counter absent or reporting anomalies.
- Provide calibration UI to map accelerometer sensitivity to user's gait.

## 2. Calibration & UX

### Calibration Goals
- Allow user to validate step detection with a short walk test (30s-60s).
- Offer two calibration modes:
  1. Automatic: app measures while user walks and suggests sensitivity.
  2. Manual: user adjusts sensitivity slider while observing detected steps in real time.
- Provide clear guidance: "Walk naturally for 30 seconds. Detected steps: X. If X is too high/low, adjust sensitivity."

### UX Design Principles
- Calibration optional; default sensible settings applied automatically.
- Do not require permissions beyond sensors; explain why foreground service notification is needed.
- Present calibration as quick, optional onboarding step.
- Show confidence level and last-calibrated timestamp in settings.

## 3. Performance & Accuracy Benchmarks

### Metrics
- Step detection accuracy (precision/recall) vs. a labelled walk dataset or manual counts
- Battery impact (mAh/hour or %/hour) while step service runs in foreground with accelerometer fallback
- CPU usage (average % over time)
- Memory footprint (MB)
- End-to-end travel latency (SC-001: ensure travel action completes <10s in normal conditions)

### Test Harness
- Automated treadmill-style dataset: record accelerometer traces with ground-truth step labels (small set of phone models and walking speeds)
- Emulator smoke tests (instrumentation) that simulate `TYPE_STEP_COUNTER` events and fallbacks
- Micro-benchmarks on actual devices (low-end, mid-tier, high-end) to measure battery and CPU impact

### Acceptance Targets
- Accuracy: >=95% step detection fidelity for typical walking speeds (0.8-1.6 m/s) on tested devices (SC-002)
- Battery: additional battery overhead <=2% CPU typical; target background energy impact <1-2%/hour in foreground service mode (measure empirically)
- Travel UX: end-to-end travel action UI + persistence completes within 10s on target devices (SC-001)

### Test Procedures & Device Classes
- Device classes for verification:
  - Low-end: API 26 device, <=2GB RAM
  - Mid-range: API 29 device, typical consumer phone
  - High-end: API 33+ device
- Test types and acceptance thresholds:
  - Accuracy test: run recorded/tracked walk traces per device class; compute precision/recall; require F1 >= 0.95 across typical walking speeds (0.8-1.6 m/s).
  - Latency test (SC-001): measure time from user pressing "Travel" to arrival state persisted and UI updated; median <= 3s, 95th percentile <= 10s on mid/high devices; allow longer on low-end but document.
  - Battery/CPU: run foreground service for 1 hour with accelerometer fallback enabled; CPU average <= 2% and incremental battery drain <= 1-2%/hour on mid-range device.
  - Robustness: test sensor loss/fallback behavior by simulating `TYPE_STEP_COUNTER` absent and verifying accelerometer fallback and calibration UX.

### Test Harness Details
- Provide an automated test harness that:
  - Replays recorded accelerometer traces against `AccelStepDetector` unit tests to compute accuracy metrics.
  - Runs instrumentation tests that simulate `TYPE_STEP_COUNTER` events and user travel flows to measure latency and persistence.
  - Executes a battery/CPU microbenchmark on real devices using `adb shell dumpsys batterystats` for measurement and reports.

### Reporting
- Store benchmark results under `specs/001-wandering-ledger/benchmarks/` with device metadata, raw traces, and summarized metrics.
- Update `plan.md` Phase 0 with pass/fail criteria referencing these metrics.

## 4. Device & Test Matrix (minimum)
- Low-end Android (API 26, e.g., 2GB RAM device)
- Mid-range Android (API 29)
- High-end Android (API 33)
- Emulator (instrumentation) for repeatable integration tests

## 5. Implementation Recommendations
1. Implement hybrid step service using `TYPE_STEP_COUNTER` primary, accelerometer fallback secondary.
2. Create a small `core/steptracker` module with pluggable detectors: `HardwareStepDetector` and `AccelStepDetector` implementing `StepDetector` interface.
3. Provide calibration UI and store calibration profile in `DataStore`.
4. Add unit tests using recorded accelerometer traces and instrumentation tests simulating sensor events.
5. Create benchmark scripts and document benchmark procedure in `specs/001-wandering-ledger/research.md` (this file).

## 6. Benchmarks: How to run (developer notes)
1. Prepare device (enable developer options, adb over USB).
2. Install debug build with instrumentation and benchmarking flags.
3. For accelerometer traces: run the calibration walk, record raw sensor logs to file.
4. Run step detector against recorded traces in unit tests to compute precision/recall.
5. For battery: run long-duration foreground service test and measure system battery stats (or use `adb shell dumpsys batterystats`).

## 7. Risks & Mitigations
- OEM sensor differences: mitigate by testing across device classes and providing calibration.
- Battery drain on older devices: mitigate by aggressive sensor batching, adaptive sampling when idle, and recommending `TYPE_STEP_COUNTER` use.
- False positives from phone in pocket/transport: mitigate via heuristics (ignore high-frequency patterns, require stride timing consistency) and allow user to disable background counting.

## 8. Deliverables
- `specs/001-wandering-ledger/research.md` (this file)
- `specs/001-wandering-ledger/data-model.md` (next)
- Benchmark scripts under `specs/001-wandering-ledger/benchmarks/`
- Unit test dataset: `specs/001-wandering-ledger/testdata/accelerometer-traces/`

## 9. Next Steps
- Implement `core/steptracker` interface and two detectors (hardware + accel) - add task T007/T012 to track.
- Add calibration UI task (add to UI tasks list).
- Create benchmark dataset and add unit tests to validate accuracy targets.


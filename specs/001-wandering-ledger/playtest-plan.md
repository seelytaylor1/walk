# Playtest Plan: Walking Vector Collection

Feature: Wandering Ledger | T043
Reference: `research.md` §3, `testdata/README.md`, `benchmarks/device-targets.md`

---

## Goal

Collect representative accelerometer traces from real walking sessions across
the target device classes (D01–D09, E01–E03) to validate step detection fidelity
in the wild. Traces feed the SC-002 fidelity harness (`StepFidelityBenchmark`).

---

## Scope

| Item | Count | Notes |
|------|-------|-------|
| Device classes | 3 | Low-end, mid-range, high-end |
| Speed buckets | 5 | 0.8, 1.0, 1.2, 1.5, 1.6 m/s |
| Phone positions | 3 | Hand, pocket, bag |
| Traces per combo | 1 | Minimum; more preferred |
| Total traces | 45+ | 3 classes × 5 speeds × 3 positions |

---

## Prerequisites

- Android device with sensor logging capability
- `adb` access for log extraction
- Reference step counter (manual tally or video) for ground truth
- No GPS/location data in traces
- No personally identifiable information in trace metadata

---

## Trace Format

Each trace is a CSV file: `{model}_{api}_{speed}_{position}.csv`

Schema (see `testdata/accelerometer-traces/README.md`):
```
timestamp_ns,ax_m_s2,ay_m_s2,az_m_s2,step_event
```

Alongside each CSV: `{trace-id}.meta.json`

```json
{
  "traceId": "pixel4a_30_1.2_normal_hand",
  "deviceModel": "Pixel 4a",
  "androidApi": 30,
  "soc": "Snapdragon 730G",
  "ram_gb": 6,
  "position": "hand",
  "speedBucket": 1.2,
  "actualSpeedMps": 1.18,
  "durationSec": 60,
  "sampleRateHz": 50,
  "stepCount": 96,
  "collector": "playtester-01",
  "collectedAt": "2026-05-15T10:00:00Z",
  "stepCounterAvailable": true
}
```

---

## Collection Procedure

### 1. Setup

1. Install a sensor-logging app on the target device.
   - Recommended: SensorLogger or a custom app using `SensorManager` at 50 Hz.
   - Confirm `TYPE_STEP_COUNTER` availability via `SensorManager.getDefaultSensor`.
2. Connect device via `adb` or enable wireless debugging.
3. Start a manual step counter (paper tally or second person counting).
4. Place phone in target position (hand / pocket / bag).

### 2. Recording

1. Begin sensor logging.
2. Start walking at the target speed bucket. Use a metronome or music to
   maintain pace. For speed verification, use a stopwatch over a measured
   30 m track.
3. Walk continuously for 60 seconds.
4. Stop logging.
5. Record the manual step count as ground truth.

### 3. Post-processing

1. Export sensor log from device to host.
2. Convert to CSV with schema above.
3. Align step timestamps with manual tally:
   - Mark the first step timestamp as `step_event=1`, then every stride
     interval thereafter.
4. Verify step count matches expected ±2 steps.
5. Strip any location/identity data.
6. Generate `*.meta.json` metadata file.
7. Copy sanitized CSV + meta to `testdata/accelerometer-traces/`.

### 4. Privacy Checklist

- [ ] No GPS coordinates in trace or metadata
- [ ] No MAC address, IMEI, or device serial
- [ ] No user name or identifier in `collector` field
- [ ] No walking route information
- [ ] Trace duration ≤ 90 seconds

---

## Speed Bucket Guide

| Bucket | Target speed | Stride freq | Steps/60s (est.) | Pacing aid |
|--------|-------------|-------------|-------------------|------------|
| 0.8 m/s | Very slow | ~1.1 Hz | ~64 | Slow stroll |
| 1.0 m/s | Slow | ~1.3 Hz | ~80 | Leisurely walk |
| 1.2 m/s | Normal | ~1.6 Hz | ~96 | Commuter pace |
| 1.5 m/s | Fast | ~2.0 Hz | ~120 | Brisk walk |
| 1.6 m/s | Very fast | ~2.1 Hz | ~128 | Fast march |

To verify speed: walk a measured 30 m track and time it.
`speed = 30 / time_seconds` m/s.

---

## Trace Storage & Reporting

- Store traces in `testdata/accelerometer-traces/` only after sanitization.
- If raw traces contain sensitive data, keep them in a private fork; never push to this repo.
- After collection, update `benchmarks/fidelity-results.md` with pass/fail per device combo.

---

## Validation Criteria

- All 3 phone positions collected for at least 2 speed buckets per device.
- Ground truth step count recorded alongside every trace.
- No PII in any committed file.
- Traces pass the SC-002 fidelity harness with F1 ≥ 0.90 (low-end) or ≥ 0.95 (mid/high).

---

## Schedule

| Week | Task |
|------|------|
| 1 | Collect traces on 1 mid-range device (D04–D06) at all 5 speeds, hand position |
| 2 | Collect pocket and bag positions; expand to second mid-range device |
| 3 | Low-end device (D01–D03) collection; high-end (D07–D09) if available |
| 4 | Analyze results, update fidelity harness, document failures in `benchmarks/fidelity-results.md` |
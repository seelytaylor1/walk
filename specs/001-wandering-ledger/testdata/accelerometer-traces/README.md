# Fidelity Test Dataset: SC-002 Step Detection

Feature: Wandering Ledger | SC-002: Step detection fidelity F1 ≥ 0.95
Reference: `research.md` §3, `testdata/README.md`

---

## Overview

This directory holds accelerometer traces used by `StepFidelityBenchmark` and
`StepFidelityBenchmarkTest` to measure step detection accuracy.

**Two sources of traces:**
1. **Synthetic traces** — generated at runtime by `StepFidelityBenchmark` (no files needed)
2. **Recorded traces** — collected from real devices during playtest (T043)

---

## Synthetic Traces (Runtime-Generated)

No files required. `StepFidelityBenchmark` generates traces in-memory at test time.
Trace metadata:

| Speed bucket | Label | Stride freq (Hz) | Steps (60s) | Intent |
|---|---|---|---|---|
| 0.8 m/s | very_slow | 1.07 | ~64 | SC-002 normal |
| 1.0 m/s | slow | 1.33 | ~80 | SC-002 normal |
| 1.2 m/s | normal | 1.60 | ~96 | SC-002 normal |
| 1.5 m/s | fast | 2.00 | ~120 | SC-002 normal |
| 1.6 m/s | very_fast | 2.13 | ~128 | SC-002 normal |
| 0.4 m/s | edge_slow | 0.53 | ~32 | edge case |
| 2.0 m/s | edge_fast | 2.67 | ~160 | edge case |
| 0.0 | stationary_noise | — | 0 | robustness |
| -1 | no_steps_sustained | — | 0 | robustness |

All synthetic traces are 60 seconds, sampled at 50 Hz.

---

## Recorded Trace Format

When real traces are collected (after T043 playtest), store them here in CSV format.
File naming: `{device-model}_{api-level}_{speed-bucket}_{position}.csv`

Example: `pixel4a_30_1.2_normal_hand.csv`

### CSV Schema

```
timestamp_ns,ax_m_s2,ay_m_s2,az_m_s2,step_event
1699999999000000000,0.12,-0.08,9.83,0
1699999999020000000,0.09,-0.04,9.81,0
...
```

| Column | Type | Description |
|--------|------|-------------|
| `timestamp_ns` | int64 | Unix nanoseconds at sample time |
| `ax_m_s2` | float | X-axis acceleration m/s² |
| `ay_m_s2` | float | Y-axis acceleration m/s² |
| `az_m_s2` | float | Z-axis acceleration m/s² |
| `step_event` | int | 1 if this sample coincides with a step, 0 otherwise |

### Metadata File (Required)

Alongside each CSV, include a JSON metadata file:
`{csv-name}.meta.json`

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
  "collectedAt": "2026-05-01T10:00:00Z",
  "stepCounterAvailable": true
}
```

### Collection Procedure

1. Use a dedicated logging app to record raw sensor data at ≥ 50 Hz.
2. The collector marks step timestamps manually (tap button per step) or via
   ground-truth reference (video count, separate step counter).
3. Post-process: align step markers with sensor timestamps, interpolate if needed.
4. Verify step count matches expected for the walking speed bucket.
5. Strip any location/identity data. Do not commit personal health data.

### Privacy Rules

- No GPS coordinates, MAC addresses, or user identifiers in traces or metadata.
- Traces must be anonymized before commit.
- Store raw traces only in a private fork; only sanitized files enter this repo.

---

## Test Execution

Synthetic traces are run by CI in `smoke` job via `StepFidelityBenchmarkTest`.
Recorded traces (once available) can be replayed by adding a test that reads CSV
files from this directory and feeds samples to `PeakDetectionStepDetector`.

To run fidelity tests locally:
```bash
./gradlew :core:steptracker:testDebugUnitTest --tests "com.wanderingledger.core.steptracker.StepFidelityBenchmarkTest"
```
# Walk Vector Template & Collection Guide

Feature: Wandering Ledger | T043
Reference: `playtest-plan.md`, `testdata/accelerometer-traces/README.md`

---

## Walk Vector Template

Use this template when recording new traces. Fill in metadata before and after
each walking session.

### Naming Convention

```
{device-model}_{android-api}_{speed-bucket}_{position}.csv
```

Examples:
- `pixel4a_30_1.2_normal_hand.csv`
- `motoe13_33_0.8_slow_pocket.csv`
- `galaxy_s24_34_1.5_fast_bag.csv`

### Metadata Template (.meta.json)

```json
{
  "traceId": "",
  "deviceModel": "",
  "androidApi": 0,
  "soc": "",
  "ram_gb": 0,
  "position": "hand|pocket|bag",
  "speedBucket": 0.0,
  "actualSpeedMps": 0.0,
  "durationSec": 60,
  "sampleRateHz": 50,
  "stepCount": 0,
  "collector": "playtester-##",
  "collectedAt": "YYYY-MM-DDTHH:MM:SSZ",
  "stepCounterAvailable": false
}
```

### CSV Template (header row)

```csv
timestamp_ns,ax_m_s2,ay_m_s2,az_m_s2,step_event
0,0.0,0.0,9.81,0
20000000,0.05,-0.03,9.82,0
...
```

- `timestamp_ns`: Unix nanoseconds of the sample
- `ax_m_s2`, `ay_m_s2`, `az_m_s2`: Linear acceleration in m/s² (subtract gravity if using raw)
- `step_event`: 1 if a step occurred at this sample, 0 otherwise

---

## Quick Collection Checklist

Before you start:
- [ ] Confirm device has accelerometer (no GPS needed)
- [ ] Set sensor sampling rate to ≥ 50 Hz
- [ ] Identify the speed bucket you are collecting
- [ ] Decide phone position (hand / pocket / bag)
- [ ] Choose a flat, clear walking area (30 m minimum)
- [ ] Have a second person with a tally counter, or record yourself on video

During recording:
- [ ] Start sensor log
- [ ] Wait 2 seconds before walking (establish baseline)
- [ ] Walk at consistent pace for exactly 60 seconds
- [ ] Count steps mentally or audibly
- [ ] Stop log after 60 seconds + 2 second buffer

After recording:
- [ ] Note actual step count as ground truth
- [ ] Measure 30 m track time for speed verification
- [ ] Calculate actual speed: `30 / time_seconds` m/s
- [ ] Convert sensor log to CSV
- [ ] Align step events with sensor timestamps
- [ ] Fill in `.meta.json`
- [ ] Run privacy checklist from `playtest-plan.md`
- [ ] Copy to `testdata/accelerometer-traces/`

---

## Ground Truth Step Counting

Reliable ground truth is critical for SC-002 validation.

### Methods (by preference)

1. **Two-person tally**: One person walks, another counts steps with a hand tally.
2. **Video review**: Record walking session; count steps frame-by-frame.
3. **Reference device**: Use a separate phone with `TYPE_STEP_COUNTER` as reference.
4. **Metronome**: Use a metronome at the expected stride frequency and count beats.

For each trace, record: `stepCount = X (method: Y)`.

---

## Speed Verification

For each trace, record the actual walking speed:

1. Measure a 30 m track.
2. Start stopwatch when you begin walking.
3. Stop stopwatch when you reach the 30 m mark.
4. `actual_speed_mps = 30 / elapsed_seconds`

Acceptable deviation: ±0.1 m/s from target bucket.

---

## Post-Collection Validation

Before committing a trace:

1. **Step count sanity**: Expected ~`speed * 80` steps per minute for typical gait.
2. **Magnitude sanity**: az should hover around 9.81 m/s² with ±2–4 m/s² oscillation.
3. **Sample rate sanity**: Verify average interval ≈ 20 ms (50 Hz) or 50 ms (20 Hz).
4. **No anomalies**: No sudden jumps > 50 m/s²; no flatlines > 5 seconds.

If a trace fails validation, re-record before committing.
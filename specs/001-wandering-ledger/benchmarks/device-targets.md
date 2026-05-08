# Device Target List: Fidelity Testing (SC-002)

Feature: Wandering Ledger | SC-002: Step detection fidelity F1 ≥ 0.95
Reference: `research.md` §3 (Test Procedures & Device Classes)

---

## Scope

All devices must run Android API 26+.
Fidelity tests run via Robolectric on CI. Physical device testing is optional but
tracked here for future playtest sessions.

---

## Device Classes

### Class A — Low-end (API 26, ≤ 2 GB RAM)

Target: entry-level / older phones
Typical profile: MediaTek quad-core, 1.5 GB RAM, ~$120 retail
Acceptance: F1 ≥ 0.90 (relaxed from core 0.95)

| # | Model | SoC | RAM | Android | Notes |
|---|-------|-----|-----|---------|-------|
| D01 | Moto E13 | MediaTek Helio G24 | 2 GB | API 33 | Budget, no step counter |
| D02 | Samsung Galaxy A03 Core | Spreadtrum SC9863A | 2 GB | API 11 (29 on some) | Low-end market |
| D03 | Nokia G10 | MediaTek Helio G25 | 3 GB | API 30 | Known sensor variance |

### Class B — Mid-range (API 29–31, 4–6 GB RAM)

Target: typical consumer phone
Typical profile: Snapdragon 6xx/7xx, 4 GB RAM, $200–$400
Acceptance: F1 ≥ 0.95

| # | Model | SoC | RAM | Android | Notes |
|---|-------|-----|-----|---------|-------|
| D04 | Pixel 4a | Snapdragon 730G | 6 GB | API 30 | Software step counter |
| D05 | Samsung Galaxy A52 | Snapdragon 720G | 6 GB | API 31 | OEM sensor variance |
| D06 | Redmi Note 10 | Snapdragon 678 | 4 GB | API 30 | Popular mid-tier |

### Class C — High-end (API 33+)

Target: flagship / recent premium
Typical profile: Snapdragon 8xx, 8+ GB RAM, $600+
Acceptance: F1 ≥ 0.95

| # | Model | SoC | RAM | Android | Notes |
|---|-------|-----|-----|---------|-------|
| D07 | Pixel 8 | Tensor G3 | 8 GB | API 34 | Hardware step counter |
| D08 | Samsung Galaxy S24 | Snapdragon 8 Gen 3 | 8 GB | API 34 | High performance |
| D09 | OnePlus 12 | Snapdragon 8 Gen 3 | 12 GB | API 34 | Top specs |

---

## Emulator Coverage (CI / Robolectric)

Robolectric runs fidelity tests without a physical device. These are the reference
ABI / Android version combos used in CI:

| Combo | API | ABI | Purpose |
|-------|-----|-----|---------|
| E01 | 26 | x86 | Low-end API floor |
| E02 | 29 | x86 | Mid-range baseline |
| E03 | 33 | x86_64 | High-end / current |

---

## Test Execution Matrix

| Device | F1 Target | Speed Buckets | Trace Count |
|--------|-----------|---------------|-------------|
| D01–D03 | ≥ 0.90 | 0.8, 1.0, 1.2, 1.5 | 4 each |
| D04–D06 | ≥ 0.95 | 0.8, 1.0, 1.2, 1.5, 1.6 | 5 each |
| D07–D09 | ≥ 0.95 | 0.8, 1.0, 1.2, 1.5, 1.6 | 5 each |
| E01–E03 | ≥ 0.95 | 0.8, 1.0, 1.2, 1.5, 1.6 | 5 each |

---

## Pass / Fail Criteria

- Class B + Class C devices: F1 ≥ 0.95 across all 5 normal speed buckets
- Class A devices: F1 ≥ 0.90 across all 4 normal speed buckets
- Any emulator (E01–E03): F1 ≥ 0.95

Failures must be documented in `benchmarks/fidelity-results.md` with trace file,
device metadata, and detector settings.
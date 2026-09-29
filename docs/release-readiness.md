# Release Readiness

Last reviewed: 2026-09-29

The next project milestone is a verified, polished first-playable slice. This page records what has evidence today and what still needs a real run or device.

## Current evidence

- The `master` branch is the repository default. CI fixes and lint cleanups are on `master` through `34bafaf`.
- The core travel, trade, rumors, companions, camp, reputation, order, and inspection systems are present in the codebase.
- The journey screen uses a painted forest daytime background at `feature/journey/src/main/res/drawable/bg_forest_day.png`. Other biome and time combinations use the procedural fallback.
- Before this workflow repair, GitHub Actions run [36443817342](https://github.com/seelytaylor1/walk/actions/runs/36443817342) failed before creating jobs. GitHub reported an invalid workflow YAML error on line 9; the run has no job logs.
- The first run of the repaired workflow, [36510106822](https://github.com/seelytaylor1/walk/actions/runs/36510106822), reached Gradle and found ktlint violations. `ktlintFormat` was applied, and the aggregate `ktlintCheck` passes locally and in CI.
- Hosted run [36511527363](https://github.com/seelytaylor1/walk/actions/runs/36511527363) passed on `master` at `34bafaf`. It ran `ktlintCheck testDebugUnitTest check assembleDebug`; unit-test tasks, Android Lint, project checks, and debug APK assembly all completed successfully.
- The repository has one open art backlog issue: [#20](https://github.com/seelytaylor1/walk/issues/20). The full backlog is broader than the first-playable slice.

## Release gate

- [x] Make CI run for pushes and pull requests targeting `master`.
- [x] Make CI fail on actual Gradle verification errors and build the debug APK.
- [x] Run `ktlintFormat` and pass the aggregate `ktlintCheck` locally.
- [x] Pass `ktlintCheck testDebugUnitTest check assembleDebug` on `master` in hosted CI.
- [x] See a successful GitHub Actions run on `master` after the formatting fixes.
- [ ] Install the debug app on an Android device and complete the first-playable journey and trade loop.
- [ ] Force-stop and reopen the app; confirm location, step bank, gold, inventory, rumors, and companions persist.
- [ ] Complete a focused forest-route art slice and review it in the running app.

The code verification gate is green. The first-playable release gate remains open until the device checks and focused art slice are complete. A green CI run cannot substitute for those checks.

The current workstation has JDK 17 and the Gradle wrapper, but no usable Android SDK path and no `adb` command. Set up a valid Android SDK before retrying the local build or device playtest.

## Device playtest

Run on an API 26+ Android device with step tracking available:

1. Install a clean debug build and start a new local game.
2. Walk until the step bank covers a route cost; travel to a connected town and confirm the cost is deducted and arrival is recorded.
3. Buy an affordable good, travel to a town that accepts it, and sell it. Confirm gold and inventory update.
4. Visit the Ledger and companion screens, then force-stop and reopen the app.
5. Confirm the player's location, step bank, gold, inventory, rumors, and active companions are unchanged after restart.
6. Record device model, Android version, issues found, and whether each persistence check passed.

The existing [walking-vector playtest plan](../specs/001-wandering-ledger/playtest-plan.md) covers broader sensor-fidelity data collection. That study is separate from this short product-loop playtest.

## Focused art slice

Keep the initial production art scope to one forest route and its first destination rather than producing the full backlog at once:

1. Forest daytime route background — present and integrated.
2. One complementary forest route state, such as dusk or rain, using the same watercolor palette and composition.
3. One destination-town establishing image that carries the route into the trade screen.

Review these assets together in the journey and town flows for crop, palette, contrast, and legibility. Track wider production assets in [art requirements](../specs/001-wandering-ledger/art-requirements.md) and issue [#20](https://github.com/seelytaylor1/walk/issues/20).

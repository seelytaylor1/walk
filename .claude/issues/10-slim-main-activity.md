# Slim MainActivity to wire-up only

**Type:** AFK  
**Blocked by:** `07-journey-viewmodel.md`, `08-town-viewmodel.md`, `09-companions-ledger-viewmodel.md`  
**Label:** enhancement

## What to build

Once all four ViewModels are extracted, `MainActivity` should contain only:

- Dependency construction (database, repositories, ViewModels)
- Navigation: screen transitions in response to callbacks from ViewModels/screens
- Sensor registration (step sensor `onResume`/`onPause`)
- Audio and haptic lifecycle hooks

No state builders, no observe jobs, no `buildChronicleEntries()`, no `JSONObject` parsing. Target: under 150 lines.

## Acceptance criteria

- [ ] `MainActivity` contains no `buildXxxScreenState()` calls
- [ ] `MainActivity` contains no coroutine observe jobs
- [ ] `MainActivity` is under 200 lines
- [ ] Full manual smoke test: travel → market → companions → ledger → map all work correctly
- [ ] All existing tests pass

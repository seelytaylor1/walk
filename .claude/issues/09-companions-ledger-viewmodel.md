# Extract CompanionsViewModel and LedgerViewModel from MainActivity

**Type:** AFK  
**Blocked by:** `06-viewmodel-architecture-decision.md`  
**Label:** enhancement

## What to build

Extract companion and ledger state management from `MainActivity`:

- **`CompanionsViewModel`**: companion observe job, `buildCompanionsScreenState()`, recruit/dismiss/interact callbacks
- **`LedgerViewModel`**: rumor observe job, `buildLedgerScreenState()`

Each exposes a `StateFlow<ScreenState>`. `MainActivity` passes the flows to the respective screens.

## Acceptance criteria

- [ ] `CompanionsViewModel` and `LedgerViewModel` exist
- [ ] `MainActivity` no longer contains companion or ledger observe jobs or state builders
- [ ] Manual smoke test: recruiting a companion works end-to-end
- [ ] Manual smoke test: ledger shows active rumors correctly
- [ ] No existing tests broken

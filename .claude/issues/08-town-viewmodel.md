# Extract TownViewModel from MainActivity

**Type:** AFK  
**Blocked by:** `06-viewmodel-architecture-decision.md`  
**Label:** enhancement

> **Per issue 06: Jetpack ViewModel (Option A).** `TownViewModel` extends
> `androidx.lifecycle.ViewModel` and exposes its state via `stateIn(viewModelScope, …)`,
> obtained in `MainActivity` via the `viewModel { }` factory (lifecycle 2.7.0).

## What to build

Extract all town/market/inventory state management from `MainActivity` into a `TownViewModel`:

- The `marketObserveJob` and `inventoryObserveJob`
- The `buildTownScreenState()`, `buildMarketScreenState()`, and `buildInventoryScreenState()` calls
- Buy and sell action callbacks
- Town arrival state

`TownViewModel` exposes a `StateFlow<TownScreenState>`. `MainActivity` passes the flow to `TownScreen` and nothing else.

## Acceptance criteria

- [ ] `TownViewModel` exists and holds all town/market/inventory observation
- [ ] `MainActivity` no longer contains market or inventory observe jobs or state builders
- [ ] Manual smoke test: buying and selling goods works end-to-end
- [ ] Manual smoke test: arriving at a new town shows correct market prices
- [ ] No existing tests broken

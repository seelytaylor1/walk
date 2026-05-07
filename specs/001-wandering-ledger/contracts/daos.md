# DAO Contracts: Wandering Ledger

Room DAOs should expose reactive reads and transaction-safe writes. Domain repositories own business rules; DAOs own persistence shape.

## Read Contracts

- Towns: get town by id, list towns, list produced/demanded goods, list outgoing roads.
- Goods and prices: list goods, get current town price, list market prices for a town, list recent price history.
- Player: observe the single player row, inventory items, banked step totals, and lifetime steps.
- Companions: list active companions, list inactive/recruitable companions, get companion by id.
- Rumors: list active rumors, list all rumors for export/recovery, get rumor by id.
- Events: list recent event logs, get event by deterministic seed/id for replay.

## Write Contracts

- All writes that affect player gold, inventory, location, step bank, companion state, rumor expiry, or event logs must happen inside Room transactions.
- Travel writes must update player location and step bank together.
- Market writes must update player gold, inventory, town supply/price state, and price history together.
- Companion-cap writes must never delete inactive companions as a side effect of replacing active companions.
- Migration recovery export must read from stable tables without requiring UI-layer models.

## Versioning

- v1 starts with explicit schema version 1.
- Any schema change after v1 must add a deterministic migration test.
- Destructive migration is disallowed unless paired with an explicit user-facing export/recovery flow.

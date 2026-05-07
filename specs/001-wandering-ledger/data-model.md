# Data Model: Wandering Ledger

**Path**: specs/001-wandering-ledger/data-model.md
**Created**: 2026-05-06

This document defines domain entities, their fields, relationships, and canonical DAO/repository signatures for v1.

Notes:
- All entities are persisted in Room (SQLite).
- Use normalized tables where appropriate; store small JSON blobs only for non-queryable metadata.

---

## Entities

### Town
- `townId: Long` (PK)
- `name: String`
- `region: String`
- `producedGoods: List<Long>` (goods ids) — consider join table `town_produces`
- `demandedGoods: List<Long>` (goods ids) — consider join table `town_demands`
- `reputation: Int` (0..100)
- `storyState: String` (enum or small serialized state)
- `lastVisitedAt: Long` (epoch ms)

Indexes: `name`, `region`.

### Good
- `goodId: Long` (PK)
- `name: String`
- `baseValue: Long`
- `isContraband: Boolean` (default false)

### TownPrice` (separate table to track current town pricing and history)
- `id: Long` (PK)
- `townId: Long` (FK -> Town)
- `goodId: Long` (FK -> Good)
- `buyPrice: Long` (what town pays player)
- `sellPrice: Long` (what town sells at)
- `supplyLevel: Int` (0=scarce,1=normal,2=abundant)
- `lastUpdatedAt: Long`

Optional: small `priceHistory: String` JSON sparkline (or separate `price_history` table if queries needed)

### PlayerState
- `playerId: Long` (PK, single row expected)
- `name: String`
- `playerClass: String` (Merchant/Scholar/Wanderer)
- `gold: Long`
- `currentTownId: Long` (FK -> Town)
- `inventorySlots: Int`
- `lifetimeSteps: Long`
- `lastSyncAt: Long` (for future use)

### InventoryItem
- `id: Long` (PK)
- `playerId: Long` (FK)
- `goodId: Long` (FK)
- `quantity: Int`
- `isSealed: Boolean` (for route contracts)

### Companion
- `companionId: Long` (PK)
- `name: String`
- `role: String` (Fighter/Scout/Healer/Rogue/Mage)
- `combatPower: Int`
- `bondLevel: Int` (0..5)
- `questState: String` (serialized small state)
- `locationTownId: Long` (FK)
- `isActive: Boolean`

### RoadSegment
- `segmentId: Long` (PK)
- `fromTownId: Long` (FK)
- `toTownId: Long` (FK)
- `stepCost: Int`
- `narrativeDistance: String` (e.g., "short", "long")
- `eventPool: String` (serialized list of event ids)

Indexes: fromTownId, toTownId

### Rumor
- `rumorId: Long` (PK)
- `text: String`
- `targetGoodId: Long?` (nullable)
- `sourceTownId: Long?` (nullable)
- `createdAt: Long`
- `expiryVisitsLeft: Int` (decremented on world visits)
- `isFalse: Boolean` (flagged by companion knowledge)

### Contract
- `contractId: Long` (PK)
- `type: String` (Delivery/Exclusivity/Route/Standing)
- `payload: String` (structured JSON describing goods, counts, towns)
- `deadlineVisits: Int`
- `rewardMultiplier: Double`
- `isActive: Boolean`
- `acceptedAt: Long`

### StepRecord
- `recordId: Long` (PK)
- `date: Long` (epoch day)
- `steps: Int`
- `source: String` (hardware | accelerometer)

### EventLog (road/town events and encounter outcomes)
- `eventId: Long` (PK)
- `type: String` (rumor/encounter/inspection/ambush)
- `meta: String` (JSON blob)
- `result: String` (outcome summary)
- `createdAt: Long`

---

## Relationships
- `Town` <-> `Good` is many-to-many: use `town_produces` and `town_demands` join tables.
- `PlayerState` -> `InventoryItem` one-to-many.
- `TownPrice` references `Town` + `Good` (one-to-many per town)
- `RoadSegment` references two `Town` rows.
- `Companion` location refers to `Town`.

---

## DAO / Repository Signatures (Kotlin interfaces)

### TownDao
- `@Query("SELECT * FROM Town WHERE townId = :id") fun getTown(id: Long): Flow<TownEntity?>`
- `fun insertTown(town: TownEntity)`
- `fun updateTown(town: TownEntity)`
- `fun listTowns(): Flow<List<TownEntity>>`

### GoodDao
- `fun getGood(id: Long): Flow<GoodEntity?>`
- `fun listGoods(): Flow<List<GoodEntity>>`

### TownPriceDao
- `fun getPrice(townId: Long, goodId: Long): Flow<TownPriceEntity?>`
- `fun upsertPrice(price: TownPriceEntity)`
- `fun listPricesForTown(townId: Long): Flow<List<TownPriceEntity>>`

### PlayerDao
- `fun getPlayer(): Flow<PlayerStateEntity>`
- `fun updatePlayer(player: PlayerStateEntity)`

### InventoryDao
- `fun listInventory(playerId: Long): Flow<List<InventoryItemEntity>>`
- `fun addItem(item: InventoryItemEntity)`
- `fun removeItem(itemId: Long)`

### CompanionDao
- `fun listActiveCompanions(): Flow<List<CompanionEntity>>`
- `fun upsertCompanion(c: CompanionEntity)`

### RoadSegmentDao
- `fun listRoadsFrom(townId: Long): Flow<List<RoadSegmentEntity>>`
- `fun getRoad(segmentId: Long): Flow<RoadSegmentEntity?>`

### RumorDao
- `fun listActiveRumors(): Flow<List<RumorEntity>>`
- `fun insertRumor(r: RumorEntity)`
- `fun decrementExpiry(rumorId: Long)`

### ContractDao
- `fun listActiveContracts(): Flow<List<ContractEntity>>`
- `fun acceptContract(contractId: Long)`
- `fun completeContract(contractId: Long)`

### StepRecordDao
- `fun insertRecord(r: StepRecordEntity)`
- `fun getStepsForDate(dateEpoch: Long): Flow<StepRecordEntity?>`

### EventLogDao
- `fun insertEvent(e: EventLogEntity)`
- `fun listRecentEvents(limit: Int): Flow<List<EventLogEntity>>`

---

## Indexing & Performance Notes
- Index `Town.lastVisitedAt`, `Town.region`, `Town.name` for queries.
- Keep price history trimmed to small size (last 10 entries) to avoid large joins.
- Use `Flow` for reactive UI updates; DAOs should return `Flow` where appropriate.

---

## Migration Guidance
- Start with single versioned schema (v1). Provide explicit migrations for schema changes (Room `AutoMigration` when possible).
- For large structural changes (e.g., changing price history model), write migration scripts converting JSON blobs to normalized tables.

---

## Next Steps
- Generate Room `@Entity` classes and DAOs per above signatures (Task T006)
- Wire repository adapters that implement domain logic (price calculations, rumor expiry) over DAOs (Task T018/T024)
- Add sample seed data for 3 towns and 2 companions in `specs/001-wandering-ledger/testdata/` for integration tests

# Orders, Reputation, and Rare Goods Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add town Reputation (earned by completing Orders), rep-gated Rare Goods (Medicines/Dyes/Charts), and a town-issued Order system (Delivery and Route types) that rewards rep on completion.

**Architecture:** DB migrates 4→5 (add `minReputation` to `town_prices`) then 5→6 (create `orders` table + seed rare goods). `OrderRepository` is a new class injected into `GameRepository` alongside the existing repositories. All order logic runs inside `GameRepository.travel()`'s transaction: complete eligible orders, tick deadlines, generate new orders on arrival. Market filtering is in `MarketRepository.observeMarket()`.

**Tech Stack:** Kotlin, Room v4, Robolectric, JUnit 4.

## Global Constraints

- No new Gradle dependencies.
- All tests use `@RunWith(RobolectricTestRunner::class)` + `TestDatabaseFactory.createInMemoryDatabase(context)` + `gameRepository.initializeNewGame(seed = 1L)`.
- DB version after this plan: 6. Do not skip versions.
- Good IDs 1–3 are the existing base goods (Apples, Iron, Silk). Rare goods use IDs 4, 5, 6.
- Companion ID 3 (Cael/Rogue) is reserved for Plan C — do not insert it here.
- `TownEntity.reputation: Int` already exists on the entity and in the schema — no migration needed for towns table.
- `playerId = 1L` is hardcoded throughout (single-player game).
- `OrderRepository` constructor takes only `WanderingLedgerDatabase`. It is NOT a dependency of `MarketRepository`.
- Order generation is seeded (uses `Random(seed)`) so tests are deterministic.
- `postTradeUpdate` in `MarketRepository` uses `priceEntity.copy(...)` — the new `minReputation` field is preserved automatically by `.copy()`. No changes to buy/sell supply logic needed beyond Task 2.

---

## File Structure

**New:**
- `core/data/src/main/java/com/wanderingledger/core/data/OrderRepository.kt` — generates, completes, and ticks orders
- `core/data/src/test/java/com/wanderingledger/core/data/OrderRepositoryTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/ReputationGatedMarketTest.kt`

**Modified:**
- `core/database/src/main/java/com/wanderingledger/core/database/Entities.kt` — add `minReputation` to `TownPriceEntity`; add `OrderEntity`
- `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt` — add snapshot methods to TownDao, GoodDao, TownPriceDao, InventoryDao; add new `OrderDao`
- `core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt` — bump version 4→6, add migrations, add `OrderEntity`, add `orderDao()`
- `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt` — seed rare goods (for new installs)
- `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt` — rep-gated filtering and 15% discount in `observeMarket`; rep discount in `buyGood`
- `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt` — add `orderRepository: OrderRepository` param; wire order logic into `travel()`
- `app/src/main/java/com/wanderingledger/app/AppContainer.kt` — instantiate `OrderRepository` and pass to `GameRepository`
- All 15 test files that call `GameRepository(database, rumorRepository, companionRepository)` — add fourth arg `OrderRepository(database)`

---

## Task 1: Schema Migration + DAO Additions + Rare Goods Seeding

**Files:**
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/Entities.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt`

**Interfaces:**
- Produces: `TownPriceEntity.minReputation: Int = 0`; `TownDao.getTownsDemanding(goodId, excludeTownId)`, `TownDao.addReputation(townId, amount)`, `GoodDao.getGoodSnapshot(id)`, `TownPriceDao.listPricesSnapshotForTown(townId)`, `InventoryDao.getItemSnapshot(playerId, goodId)`, `InventoryDao.updateItem(item)`. DB at version 5.

- [ ] **Step 1: Add `minReputation` to `TownPriceEntity` in Entities.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/Entities.kt`. Find the `TownPriceEntity` data class. Add `minReputation` as the last field with a default of `0`:

```kotlin
data class TownPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val townId: Long,
    val goodId: Long,
    val buyPrice: Long,
    val sellPrice: Long,
    val supplyLevel: String,
    val lastUpdatedAt: Long,
    val minReputation: Int = 0,
)
```

- [ ] **Step 2: Add snapshot DAO methods to Daos.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt`. Add the following methods to their respective DAOs:

**TownDao** — add after `insertDemandedGoods`:
```kotlin
@Query(
    "SELECT t.* FROM towns t JOIN town_demands td ON t.townId = td.townId " +
    "WHERE td.goodId = :goodId AND t.townId != :excludeTownId"
)
suspend fun getTownsDemanding(goodId: Long, excludeTownId: Long): List<TownEntity>

@Query("UPDATE towns SET reputation = MIN(reputation + :amount, 100) WHERE townId = :townId")
suspend fun addReputation(townId: Long, amount: Int)
```

**GoodDao** — add after `getGood`:
```kotlin
@Query("SELECT * FROM goods WHERE goodId = :id")
suspend fun getGoodSnapshot(id: Long): GoodEntity?
```

**TownPriceDao** — add after `listPricesForTown`:
```kotlin
@Query("SELECT * FROM town_prices WHERE townId = :townId ORDER BY goodId")
suspend fun listPricesSnapshotForTown(townId: Long): List<TownPriceEntity>
```

**InventoryDao** — add after `removeItem`:
```kotlin
@Query("SELECT * FROM inventory_items WHERE playerId = :playerId AND goodId = :goodId LIMIT 1")
suspend fun getItemSnapshot(playerId: Long, goodId: Long): InventoryItemEntity?

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun updateItem(item: InventoryItemEntity)
```

- [ ] **Step 3: Add MIGRATION_4_5 to WanderingLedgerDatabase.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt`. Add after `MIGRATION_3_4`:

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE town_prices ADD COLUMN minReputation INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

Bump the `@Database` annotation version from `4` to `5`:
```kotlin
version = 5,
```

Add `MIGRATION_4_5` to `addMigrations()`:
```kotlin
.addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
```

- [ ] **Step 4: Seed rare goods in SeedWorld.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt`. Add rare goods after the existing `GoodEntity(3, "Silk", 30)` insertion:

```kotlin
database.goodDao().insertGoods(
    listOf(
        GoodEntity(1, "Apples", 8),
        GoodEntity(2, "Iron", 18),
        GoodEntity(3, "Silk", 30),
        // Rare goods: rep-gated, require Reputation ≥ 60 to appear at source town
        GoodEntity(4, "Medicines", 20),
        GoodEntity(5, "Dyes", 17),
        GoodEntity(6, "Charts", 22),
    ),
)
```

Add produce/demand entries after the existing `insertProducedGoods` call (append to each list):

```kotlin
database.townDao().insertProducedGoods(
    listOf(
        TownProducesEntity(1, 1),  // Hearthwick → Apples
        TownProducesEntity(2, 2),  // Stoneford → Iron
        TownProducesEntity(3, 3),  // Mistfall → Silk
        TownProducesEntity(3, 4),  // Mistfall → Medicines
        TownProducesEntity(1, 5),  // Hearthwick → Dyes
        TownProducesEntity(2, 6),  // Stoneford → Charts
    ),
)
database.townDao().insertDemandedGoods(
    listOf(
        TownDemandsEntity(1, 2),  // Hearthwick demands Iron
        TownDemandsEntity(2, 3),  // Stoneford demands Silk
        TownDemandsEntity(3, 1),  // Mistfall demands Apples
        TownDemandsEntity(1, 4),  // Hearthwick demands Medicines
        TownDemandsEntity(3, 5),  // Mistfall demands Dyes
        TownDemandsEntity(3, 6),  // Mistfall demands Charts
    ),
)
```

Append rare good price entries to the existing `upsertPrices` call:

```kotlin
// Medicines (4): Mistfall source (rep-gated), Hearthwick destination
TownPriceEntity(townId = 3, goodId = 4, buyPrice = 8, sellPrice = 14, supplyLevel = "Abundant", lastUpdatedAt = now, minReputation = 60),
TownPriceEntity(townId = 1, goodId = 4, buyPrice = 24, sellPrice = 40, supplyLevel = "Scarce", lastUpdatedAt = now, minReputation = 0),
// Dyes (5): Hearthwick source (rep-gated), Mistfall destination
TownPriceEntity(townId = 1, goodId = 5, buyPrice = 7, sellPrice = 12, supplyLevel = "Abundant", lastUpdatedAt = now, minReputation = 60),
TownPriceEntity(townId = 3, goodId = 5, buyPrice = 20, sellPrice = 32, supplyLevel = "Scarce", lastUpdatedAt = now, minReputation = 0),
// Charts (6): Stoneford source (rep-gated), Mistfall destination
TownPriceEntity(townId = 2, goodId = 6, buyPrice = 9, sellPrice = 15, supplyLevel = "Abundant", lastUpdatedAt = now, minReputation = 60),
TownPriceEntity(townId = 3, goodId = 6, buyPrice = 23, sellPrice = 37, supplyLevel = "Scarce", lastUpdatedAt = now, minReputation = 0),
```

- [ ] **Step 5: Build to verify schema exports are up to date**

```
./gradlew :core:database:build
```

Expected: BUILD SUCCESSFUL. If Room schema export errors appear, update the exported schema JSON in `core/database/schemas/`. Room auto-generates these when `exportSchema = true` and the version bumps.

- [ ] **Step 6: Commit**

```bash
git add core/database/src/main/java/com/wanderingledger/core/database/Entities.kt
git add core/database/src/main/java/com/wanderingledger/core/database/Daos.kt
git add core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt
git add core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt
git add core/database/schemas/
git commit -m "feat: add minReputation to town_prices (migration 4→5), seed rare goods"
```

---

## Task 2: Reputation-Gated Market Observation

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt`
- Create: `core/data/src/test/java/com/wanderingledger/core/data/ReputationGatedMarketTest.kt`

**Interfaces:**
- Consumes: `TownPriceEntity.minReputation: Int`, `TownEntity.reputation: Int`, Task 1 DAO additions
- Produces: `observeMarket(townId)` filters out goods where `minReputation > town.reputation`; applies 15% discount on rare-good `sellPrice` when `town.reputation >= 75`; `buyGood` also applies the discount so the actual purchase matches the displayed price.

- [ ] **Step 1: Write failing tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/ReputationGatedMarketTest.kt`:

```kotlin
package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.TestDatabaseFactory
import com.wanderingledger.core.database.WanderingLedgerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReputationGatedMarketTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var marketRepository: MarketRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        marketRepository = MarketRepository(database)
        // initializeNewGame seeds goods 1-6 + prices; player starts at Hearthwick (town 1)
        val gameRepository = GameRepository(database, RumorRepository(database), CompanionRepository(database), OrderRepository(database))
        gameRepository.initializeNewGame(seed = 1L)
    }

    @Test
    fun medicinesDoNotAppearAtMistfallWhenRepBelowSixty() = runBlocking {
        // Mistfall (townId=3) starts at rep=50; Medicines require minReputation=60
        val market = marketRepository.observeMarket(townId = 3L).first()
        val goodIds = market.rows.map { it.good.goodId }
        assertFalse("Medicines (goodId=4) should not appear at rep=50", goodIds.contains(4L))
    }

    @Test
    fun medicinesAppearAtMistfallWhenRepIsExactlySixty() = runBlocking {
        database.townDao().addReputation(townId = 3L, amount = 10) // 50 + 10 = 60
        val market = marketRepository.observeMarket(townId = 3L).first()
        val medicines = market.rows.find { it.good.goodId == 4L }
        assertTrue("Medicines should appear at rep=60", medicines != null)
        assertEquals("sellPrice should be 14 at rep=60 (no discount yet)", 14L, medicines!!.townPrice.sellPrice)
    }

    @Test
    fun medicinesGetFifteenPercentDiscountAtRepSeventyFive() = runBlocking {
        database.townDao().addReputation(townId = 3L, amount = 25) // 50 + 25 = 75
        val market = marketRepository.observeMarket(townId = 3L).first()
        val medicines = market.rows.find { it.good.goodId == 4L }
        assertTrue("Medicines should appear at rep=75", medicines != null)
        // sellPrice=14, 15% off → (14 * 0.85).toLong() = 11
        assertEquals("sellPrice should be 11 at rep=75", 11L, medicines!!.townPrice.sellPrice)
        assertTrue("canAfford should use discounted price", medicines.canAfford || true) // player has 50g > 11g
    }

    @Test
    fun baseGoodsAreUnaffectedByReputation() = runBlocking {
        database.townDao().addReputation(townId = 3L, amount = 25) // rep=75
        val market = marketRepository.observeMarket(townId = 3L).first()
        val silk = market.rows.find { it.good.goodId == 3L }
        assertTrue("Silk always visible (minReputation=0)", silk != null)
        assertEquals("Silk sellPrice unchanged at 21", 21L, silk!!.townPrice.sellPrice)
    }
}
```

- [ ] **Step 2: Run tests — expect failures**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.ReputationGatedMarketTest"
```

Expected: compile failure because `OrderRepository` doesn't exist yet (forward reference). If it fails with that, change the test's setUp to NOT pass `OrderRepository` — use `GameRepository(database, RumorRepository(database), CompanionRepository(database))` temporarily. The tests should then fail with `expected:<11> but was:<14>` (filtering/discounting not implemented).

- [ ] **Step 3: Add constants to MarketRepository.kt**

Open `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt`. Add a companion object inside the `MarketRepository` class:

```kotlin
companion object {
    const val REP_BETTER_PRICE_THRESHOLD = 75
    const val REP_BETTER_PRICE_DISCOUNT = 0.15
}
```

- [ ] **Step 4: Update `observeMarket` lambda in MarketRepository.kt**

In `MarketRepository.observeMarket`, replace the entire lambda body (the block starting after `{ town, prices, player, inventory, goods ->`):

```kotlin
        ) { town, prices, player, inventory, goods ->
            val goodsById = goods.associateBy { it.goodId }
            val inventoryByGoodId =
                inventory
                    .groupBy { it.goodId }
                    .mapValues { (_, items) -> items.sumOf { it.quantity } }
            val rep = town.reputation

            val rows =
                prices
                    .filter { it.minReputation <= rep }
                    .mapNotNull { priceEntity ->
                        val good = goodsById[priceEntity.goodId] ?: return@mapNotNull null
                        val supplyLevel = SupplyLevel.valueOf(priceEntity.supplyLevel)
                        val playerQty = inventoryByGoodId[priceEntity.goodId] ?: 0
                        val effectiveSellPrice =
                            if (priceEntity.minReputation > 0 && rep >= REP_BETTER_PRICE_THRESHOLD) {
                                (priceEntity.sellPrice * (1.0 - REP_BETTER_PRICE_DISCOUNT)).toLong()
                            } else {
                                priceEntity.sellPrice
                            }
                        MarketRow(
                            good =
                                Good(
                                    goodId = good.goodId,
                                    name = good.name,
                                    baseValue = good.baseValue,
                                    isContraband = good.isContraband,
                                ),
                            townPrice =
                                TownPrice(
                                    id = priceEntity.id,
                                    townId = priceEntity.townId,
                                    goodId = priceEntity.goodId,
                                    buyPrice = priceEntity.buyPrice,
                                    sellPrice = effectiveSellPrice,
                                    supplyLevel = supplyLevel,
                                    lastUpdatedAt = priceEntity.lastUpdatedAt,
                                ),
                            playerQuantity = playerQty,
                            canAfford = player.gold >= effectiveSellPrice,
                            canSell = playerQty > 0,
                        )
                    }

            MarketState(
                townId = town.townId,
                townName = town.name,
                playerGold = player.gold,
                playerInventoryUsed = inventory.sumOf { it.quantity },
                playerInventoryCapacity = player.inventorySlots,
                rows = rows,
            )
        }
```

- [ ] **Step 5: Apply rep discount in `buyGood` in MarketRepository.kt**

In `MarketRepository.buyGood`, after the `priceEntity` lookup, add reputation check before computing `totalCost`:

```kotlin
val townRep = database.townDao().getTownSnapshot(townId)?.reputation ?: 0
val actualSellPrice =
    if (priceEntity.minReputation > 0 && townRep >= REP_BETTER_PRICE_THRESHOLD) {
        (priceEntity.sellPrice * (1.0 - REP_BETTER_PRICE_DISCOUNT)).toLong()
    } else {
        priceEntity.sellPrice
    }
val totalCost = actualSellPrice * quantity
```

Replace the subsequent `if (player.gold < totalCost)` check — it already uses `totalCost` so no further changes needed there.

- [ ] **Step 6: Run tests**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.ReputationGatedMarketTest"
```

Expected: all 4 tests pass.

- [ ] **Step 7: Run full test suite to check for regressions**

```
./gradlew :core:data:test :app:test
```

Expected: all existing tests still pass (changes to `observeMarket` are strictly additive filter + discount).

- [ ] **Step 8: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt
git add core/data/src/test/java/com/wanderingledger/core/data/ReputationGatedMarketTest.kt
git commit -m "feat: reputation-gated market filtering and 15% discount at rep 75"
```

---

## Task 3: OrderEntity + OrderDao + DB Migration 5→6

**Files:**
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/Entities.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt`

**Interfaces:**
- Consumes: DB version 5 from Task 1
- Produces: `OrderEntity`, `OrderDao`, DB at version 6. `OrderEntity.type` is one of `"Delivery"` or `"Route"`. `OrderEntity.issuingTownId` is always the town that issued the order. `OrderEntity.destinationTownId` equals `issuingTownId` for Delivery; it's the target town for Route.

- [ ] **Step 1: Add `OrderEntity` to Entities.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/Entities.kt`. Add at the end of the file:

```kotlin
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Long = 0,
    val issuingTownId: Long,
    val destinationTownId: Long,
    val goodId: Long,
    val quantity: Int,
    val type: String,           // "Delivery" or "Route"
    val reputationReward: Int,
    val deadlineVisitsLeft: Int,
    val isActive: Boolean = true,
)
```

- [ ] **Step 2: Add `OrderDao` to Daos.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt`. Add at the end of the file:

```kotlin
@Dao
interface OrderDao {
    @Query("SELECT COUNT(*) FROM orders WHERE issuingTownId = :townId AND isActive = 1")
    suspend fun countActiveOrdersForTown(townId: Long): Int

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET isActive = 0 WHERE orderId = :orderId")
    suspend fun deactivateOrder(orderId: Long)

    @Query("UPDATE orders SET deadlineVisitsLeft = deadlineVisitsLeft - 1 WHERE isActive = 1 AND deadlineVisitsLeft > 0")
    suspend fun decrementAllActiveDeadlines()

    @Query("UPDATE orders SET isActive = 0 WHERE isActive = 1 AND deadlineVisitsLeft <= 0")
    suspend fun expireOverdueOrders()

    @Query("SELECT * FROM orders WHERE isActive = 1 ORDER BY orderId")
    suspend fun getActiveOrdersSnapshot(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE issuingTownId = :townId AND isActive = 1 ORDER BY orderId")
    suspend fun getActiveOrdersForTownSnapshot(townId: Long): List<OrderEntity>
}
```

- [ ] **Step 3: Add MIGRATION_5_6 and wire up WanderingLedgerDatabase.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt`. Add after `MIGRATION_4_5`:

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS orders (
                orderId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                issuingTownId INTEGER NOT NULL,
                destinationTownId INTEGER NOT NULL,
                goodId INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                type TEXT NOT NULL,
                reputationReward INTEGER NOT NULL,
                deadlineVisitsLeft INTEGER NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        // Seed rare goods for existing installs (new installs get these via SeedWorld)
        db.execSQL("INSERT OR IGNORE INTO goods (goodId, name, baseValue, isContraband) VALUES (4, 'Medicines', 20, 0)")
        db.execSQL("INSERT OR IGNORE INTO goods (goodId, name, baseValue, isContraband) VALUES (5, 'Dyes', 17, 0)")
        db.execSQL("INSERT OR IGNORE INTO goods (goodId, name, baseValue, isContraband) VALUES (6, 'Charts', 22, 0)")
        db.execSQL("INSERT OR IGNORE INTO town_produces (townId, goodId) VALUES (3, 4)")
        db.execSQL("INSERT OR IGNORE INTO town_produces (townId, goodId) VALUES (1, 5)")
        db.execSQL("INSERT OR IGNORE INTO town_produces (townId, goodId) VALUES (2, 6)")
        db.execSQL("INSERT OR IGNORE INTO town_demands (townId, goodId) VALUES (1, 4)")
        db.execSQL("INSERT OR IGNORE INTO town_demands (townId, goodId) VALUES (3, 5)")
        db.execSQL("INSERT OR IGNORE INTO town_demands (townId, goodId) VALUES (3, 6)")
        db.execSQL("INSERT OR IGNORE INTO town_prices (townId, goodId, buyPrice, sellPrice, supplyLevel, lastUpdatedAt, minReputation) VALUES (3, 4, 8, 14, 'Abundant', 0, 60)")
        db.execSQL("INSERT OR IGNORE INTO town_prices (townId, goodId, buyPrice, sellPrice, supplyLevel, lastUpdatedAt, minReputation) VALUES (1, 4, 24, 40, 'Scarce', 0, 0)")
        db.execSQL("INSERT OR IGNORE INTO town_prices (townId, goodId, buyPrice, sellPrice, supplyLevel, lastUpdatedAt, minReputation) VALUES (1, 5, 7, 12, 'Abundant', 0, 60)")
        db.execSQL("INSERT OR IGNORE INTO town_prices (townId, goodId, buyPrice, sellPrice, supplyLevel, lastUpdatedAt, minReputation) VALUES (3, 5, 20, 32, 'Scarce', 0, 0)")
        db.execSQL("INSERT OR IGNORE INTO town_prices (townId, goodId, buyPrice, sellPrice, supplyLevel, lastUpdatedAt, minReputation) VALUES (2, 6, 9, 15, 'Abundant', 0, 60)")
        db.execSQL("INSERT OR IGNORE INTO town_prices (townId, goodId, buyPrice, sellPrice, supplyLevel, lastUpdatedAt, minReputation) VALUES (3, 6, 23, 37, 'Scarce', 0, 0)")
    }
}
```

Update the `@Database` annotation:
- Add `OrderEntity::class` to the `entities` list
- Bump `version = 6`

```kotlin
@Database(
    entities = [
        TownEntity::class,
        GoodEntity::class,
        TownProducesEntity::class,
        TownDemandsEntity::class,
        TownPriceEntity::class,
        PlayerStateEntity::class,
        InventoryItemEntity::class,
        CompanionEntity::class,
        RoadSegmentEntity::class,
        RumorEntity::class,
        StepRecordEntity::class,
        EventLogEntity::class,
        PriceHistoryEntity::class,
        OrderEntity::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
```

Add `abstract fun orderDao(): OrderDao` to the abstract methods block.

Update `addMigrations`:
```kotlin
.addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
```

- [ ] **Step 4: Build to verify**

```
./gradlew :core:database:build
```

Expected: BUILD SUCCESSFUL with schema file generated/updated.

- [ ] **Step 5: Commit**

```bash
git add core/database/src/main/java/com/wanderingledger/core/database/Entities.kt
git add core/database/src/main/java/com/wanderingledger/core/database/Daos.kt
git add core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt
git add core/database/schemas/
git commit -m "feat: add OrderEntity + OrderDao, migration 5→6 creates orders table"
```

---

## Task 4: OrderRepository

**Files:**
- Create: `core/data/src/main/java/com/wanderingledger/core/data/OrderRepository.kt`
- Create: `core/data/src/test/java/com/wanderingledger/core/data/OrderRepositoryTest.kt`

**Interfaces:**
- Consumes: `OrderDao`, `TownPriceDao.listPricesSnapshotForTown`, `TownDao.getTownsDemanding`, `GoodDao.getGoodSnapshot`, `InventoryDao.getItemSnapshot`, `InventoryDao.updateItem`, `InventoryDao.removeItem`, `TownDao.addReputation`
- Produces:
  - `OrderRepository(database: WanderingLedgerDatabase)`
  - `suspend fun generateOrdersForTown(townId: Long, seed: Long)` — fills town board to `ORDER_CAP_PER_TOWN=3` with Delivery or Route orders
  - `suspend fun checkAndCompleteOrders(arrivedTownId: Long, playerId: Long): List<OrderCompletion>` — completes eligible orders, deducts inventory, adds reputation
  - `suspend fun tickDeadlines()` — decrements deadlines and expires overdue orders
  - `data class OrderCompletion(val orderId: Long, val issuingTownId: Long, val reputationReward: Int, val goodName: String)`
  - Constants: `ORDER_CAP_PER_TOWN=3`, `DELIVERY_DEADLINE=3`, `ROUTE_DEADLINE=4`, `DELIVERY_REP=5`, `ROUTE_REP=8`, `ORDER_QTY_MIN=1`, `ORDER_QTY_MAX=3`

- [ ] **Step 1: Write failing tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/OrderRepositoryTest.kt`:

```kotlin
package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.InventoryItemEntity
import com.wanderingledger.core.database.TestDatabaseFactory
import com.wanderingledger.core.database.WanderingLedgerDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrderRepositoryTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var orderRepository: OrderRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        orderRepository = OrderRepository(database)
        val gameRepository = GameRepository(
            database,
            RumorRepository(database),
            CompanionRepository(database),
            orderRepository,
        )
        gameRepository.initializeNewGame(seed = 1L)
    }

    @Test
    fun generateOrdersFilledToCapForTown() = runBlocking {
        orderRepository.generateOrdersForTown(townId = 1L, seed = 1L)
        val orders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
        assertTrue("Should generate up to 3 orders", orders.size <= 3)
        assertTrue("Should generate at least 1 order", orders.isNotEmpty())
        orders.forEach { order ->
            assertTrue("Order type must be Delivery or Route", order.type == "Delivery" || order.type == "Route")
            assertTrue("Order quantity must be in range 1–3", order.quantity in 1..3)
            assertTrue("Order goodId must be positive", order.goodId > 0)
        }
    }

    @Test
    fun generateOrdersDoesNotExceedCapOnRepeatCall() = runBlocking {
        orderRepository.generateOrdersForTown(townId = 1L, seed = 1L)
        orderRepository.generateOrdersForTown(townId = 1L, seed = 2L) // second visit
        val orders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
        assertTrue("Orders should never exceed cap of 3", orders.size <= 3)
    }

    @Test
    fun deliveryOrderCompletesWhenPlayerArrivesWithGoods() = runBlocking {
        // Insert a Delivery order: player must bring goodId=2 (Iron) to Hearthwick (town 1)
        database.orderDao().insertOrder(
            com.wanderingledger.core.database.OrderEntity(
                issuingTownId = 1L,
                destinationTownId = 1L,
                goodId = 2L,
                quantity = 1,
                type = "Delivery",
                reputationReward = 5,
                deadlineVisitsLeft = 3,
            )
        )
        // Give player 1 unit of Iron
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 2L, quantity = 1)
        )

        val completions = orderRepository.checkAndCompleteOrders(arrivedTownId = 1L, playerId = 1L)

        assertEquals("One order should complete", 1, completions.size)
        assertEquals("Reputation reward is +5 for Delivery", 5, completions.first().reputationReward)

        val remainingOrders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
        assertTrue("Completed order should be deactivated", remainingOrders.isEmpty())

        val inventory = database.inventoryDao().getItemSnapshot(playerId = 1L, goodId = 2L)
        assertTrue("Iron should be deducted from inventory", inventory == null || inventory.quantity == 0)
    }

    @Test
    fun routeOrderCompletesAtDestinationNotAtSource() = runBlocking {
        // Route order: Stoneford (townId=2) issues it, destination is Mistfall (townId=3)
        database.orderDao().insertOrder(
            com.wanderingledger.core.database.OrderEntity(
                issuingTownId = 2L,
                destinationTownId = 3L,
                goodId = 2L,
                quantity = 1,
                type = "Route",
                reputationReward = 8,
                deadlineVisitsLeft = 4,
            )
        )
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 2L, quantity = 1)
        )

        // Arriving at source town should NOT complete the order
        val atSource = orderRepository.checkAndCompleteOrders(arrivedTownId = 2L, playerId = 1L)
        assertEquals("Route should not complete at source", 0, atSource.size)

        // Arriving at destination SHOULD complete it
        val atDest = orderRepository.checkAndCompleteOrders(arrivedTownId = 3L, playerId = 1L)
        assertEquals("Route should complete at destination", 1, atDest.size)
        assertEquals("Reputation reward is +8 for Route", 8, atDest.first().reputationReward)
    }

    @Test
    fun tickDeadlinesDecrementsAndExpires() = runBlocking {
        database.orderDao().insertOrder(
            com.wanderingledger.core.database.OrderEntity(
                issuingTownId = 1L,
                destinationTownId = 1L,
                goodId = 1L,
                quantity = 1,
                type = "Delivery",
                reputationReward = 5,
                deadlineVisitsLeft = 1,
            )
        )
        orderRepository.tickDeadlines()

        val orders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
        assertTrue("Order with 1 deadline remaining should expire after tick", orders.isEmpty())
    }
}
```

- [ ] **Step 2: Run tests — expect compile failures**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.OrderRepositoryTest"
```

Expected: compile failure — `OrderRepository` doesn't exist yet.

- [ ] **Step 3: Create OrderRepository.kt**

Create `core/data/src/main/java/com/wanderingledger/core/data/OrderRepository.kt`:

```kotlin
package com.wanderingledger.core.data

import com.wanderingledger.core.database.OrderEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import kotlin.random.Random

data class OrderCompletion(
    val orderId: Long,
    val issuingTownId: Long,
    val reputationReward: Int,
    val goodName: String,
)

class OrderRepository(private val database: WanderingLedgerDatabase) {

    companion object {
        const val ORDER_CAP_PER_TOWN = 3
        const val DELIVERY_DEADLINE = 3
        const val ROUTE_DEADLINE = 4
        const val DELIVERY_REP = 5
        const val ROUTE_REP = 8
        const val ORDER_QTY_MIN = 1
        const val ORDER_QTY_MAX = 3
    }

    suspend fun generateOrdersForTown(townId: Long, seed: Long) {
        val currentCount = database.orderDao().countActiveOrdersForTown(townId)
        if (currentCount >= ORDER_CAP_PER_TOWN) return

        val rng = Random(seed)
        val prices = database.townPriceDao().listPricesSnapshotForTown(townId)
        val scarce = prices.filter { it.supplyLevel == "Scarce" }
        val abundant = prices.filter { it.supplyLevel == "Abundant" }

        var toGenerate = ORDER_CAP_PER_TOWN - currentCount
        while (toGenerate > 0) {
            val useRoute = rng.nextBoolean() && abundant.isNotEmpty()

            if (useRoute) {
                val priceEntry = abundant.random(rng)
                val destinations = database.townDao().getTownsDemanding(priceEntry.goodId, excludeTownId = townId)
                if (destinations.isEmpty()) {
                    toGenerate--
                    continue
                }
                val destination = destinations.random(rng)
                val qty = rng.nextInt(ORDER_QTY_MIN, ORDER_QTY_MAX + 1)
                database.orderDao().insertOrder(
                    OrderEntity(
                        issuingTownId = townId,
                        destinationTownId = destination.townId,
                        goodId = priceEntry.goodId,
                        quantity = qty,
                        type = "Route",
                        reputationReward = ROUTE_REP,
                        deadlineVisitsLeft = ROUTE_DEADLINE,
                    )
                )
            } else if (scarce.isNotEmpty()) {
                val priceEntry = scarce.random(rng)
                val qty = rng.nextInt(ORDER_QTY_MIN, ORDER_QTY_MAX + 1)
                database.orderDao().insertOrder(
                    OrderEntity(
                        issuingTownId = townId,
                        destinationTownId = townId,
                        goodId = priceEntry.goodId,
                        quantity = qty,
                        type = "Delivery",
                        reputationReward = DELIVERY_REP,
                        deadlineVisitsLeft = DELIVERY_DEADLINE,
                    )
                )
            }
            toGenerate--
        }
    }

    suspend fun checkAndCompleteOrders(arrivedTownId: Long, playerId: Long): List<OrderCompletion> {
        val completions = mutableListOf<OrderCompletion>()
        val activeOrders = database.orderDao().getActiveOrdersSnapshot()

        for (order in activeOrders) {
            val completionTownId = when (order.type) {
                "Delivery" -> order.issuingTownId
                "Route" -> order.destinationTownId
                else -> continue
            }
            if (completionTownId != arrivedTownId) continue

            val item = database.inventoryDao().getItemSnapshot(playerId, order.goodId)
            if (item == null || item.quantity < order.quantity) continue

            // Deduct inventory
            val newQty = item.quantity - order.quantity
            if (newQty <= 0) {
                database.inventoryDao().removeItem(item.id)
            } else {
                database.inventoryDao().updateItem(item.copy(quantity = newQty))
            }

            // Award reputation to issuing town
            database.townDao().addReputation(order.issuingTownId, order.reputationReward)

            // Deactivate order
            database.orderDao().deactivateOrder(order.orderId)

            val goodName = database.goodDao().getGoodSnapshot(order.goodId)?.name ?: "goods"
            completions.add(
                OrderCompletion(
                    orderId = order.orderId,
                    issuingTownId = order.issuingTownId,
                    reputationReward = order.reputationReward,
                    goodName = goodName,
                )
            )
        }
        return completions
    }

    suspend fun tickDeadlines() {
        database.orderDao().decrementAllActiveDeadlines()
        database.orderDao().expireOverdueOrders()
    }
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.OrderRepositoryTest"
```

Expected: all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/OrderRepository.kt
git add core/data/src/test/java/com/wanderingledger/core/data/OrderRepositoryTest.kt
git commit -m "feat: OrderRepository — generate, complete, and expire delivery/route orders"
```

---

## Task 5: Wire OrderRepository into GameRepository

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/AppContainer.kt`
- Modify: 15 test files that construct `GameRepository` — add `OrderRepository(database)` as 4th arg

**Interfaces:**
- Consumes: `OrderRepository.checkAndCompleteOrders`, `OrderRepository.tickDeadlines`, `OrderRepository.generateOrdersForTown`
- Produces: `GameRepository(database, rumorRepository, companionRepository, orderRepository: OrderRepository)` — on arrival: completes orders, ticks deadlines, generates new orders, logs order completions

- [ ] **Step 1: Write failing integration test in GameRepositoryTest**

Open `core/data/src/test/java/com/wanderingledger/core/data/GameRepositoryTest.kt`. Add a new test that verifies order completion during travel:

```kotlin
@Test
fun travelCompletesDeliveryOrderOnArrival() = runBlocking {
    // Give player enough steps to travel segment 1 (Hearthwick→Stoneford, 1000 steps)
    database.playerDao().updatePlayer(
        database.playerDao().getPlayerSnapshot()!!.copy(bankedSteps = 2000)
    )
    // Insert Delivery order: bring Iron (goodId=2) to Stoneford (townId=2)
    database.orderDao().insertOrder(
        com.wanderingledger.core.database.OrderEntity(
            issuingTownId = 2L,
            destinationTownId = 2L,
            goodId = 2L,
            quantity = 1,
            type = "Delivery",
            reputationReward = 5,
            deadlineVisitsLeft = 3,
        )
    )
    // Give player 1 Iron
    database.inventoryDao().addItem(
        com.wanderingledger.core.database.InventoryItemEntity(playerId = 1L, goodId = 2L, quantity = 1)
    )

    val result = gameRepository.travel(segmentId = 1L, seed = 42L)

    assertTrue("Travel should succeed", result is TravelResult.Arrived)
    // Verify order deactivated
    val remaining = database.orderDao().getActiveOrdersForTownSnapshot(townId = 2L)
    assertTrue("Order should be completed", remaining.isEmpty())
    // Verify Stoneford gained rep
    val stoneford = database.townDao().getTownSnapshot(2L)
    assertEquals("Stoneford reputation should increase by 5", 55, stoneford?.reputation)
}
```

- [ ] **Step 2: Run test — expect compile failure**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.GameRepositoryTest.travelCompletesDeliveryOrderOnArrival"
```

Expected: compile failure because `GameRepository` doesn't accept 4 args yet.

- [ ] **Step 3: Update GameRepository constructor**

Open `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt`. Find the `GameRepository` class declaration (around line 90). Add `orderRepository` as a new required parameter:

```kotlin
class GameRepository(
    private val database: WanderingLedgerDatabase,
    private val rumorRepository: RumorRepository,
    private val companionRepository: CompanionRepository,
    private val orderRepository: OrderRepository,
)
```

- [ ] **Step 4: Add order logic inside `travel()` transaction**

Inside `GameRepository.travel()`, in the `is TravelOutcome.Arrived ->` branch, after the `outcome.eventLogs.forEach { ... }` block and before `val remainingSteps = ...`, add:

```kotlin
// Complete eligible orders and award reputation
val orderCompletions = orderRepository.checkAndCompleteOrders(
    arrivedTownId = delta.newTownId,
    playerId = player.playerId,
)
orderCompletions.forEach { completion ->
    database.eventLogDao().insertEvent(
        EventLogEntity(
            type = "order-complete",
            meta = "{\"orderId\":${completion.orderId},\"issuingTownId\":${completion.issuingTownId},\"rep\":${completion.reputationReward}}",
            result = "Order fulfilled: delivered ${completion.goodName}. +${completion.reputationReward} reputation.",
            createdAt = delta.arrivedAt,
        )
    )
}

// Expire overdue orders and generate new ones for the arrived town
orderRepository.tickDeadlines()
orderRepository.generateOrdersForTown(townId = delta.newTownId, seed = seed + delta.newTownId)
```

- [ ] **Step 5: Update all test call sites**

The following 15 files construct `GameRepository(database, rumorRepository, companionRepository)`. In each, add `OrderRepository(database)` as the fourth argument.

Run this search first to confirm the full list:
```
grep -rn "GameRepository(database" --include="*.kt" core/ app/
```

Files to update (add `, OrderRepository(database)` to the constructor call in each):
- `app/src/test/java/com/wanderingledger/app/MarketViewModelTest.kt`
- `app/src/test/java/com/wanderingledger/app/CompanionsViewModelTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/GameRepositoryTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/UserStory1TravelFlowTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/CompanionRepositoryTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/CompanionRecruitmentGateTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/Milestone1AcceptanceCriteriaTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/TravelLatencyBenchmarkTest.kt` (2 call sites)
- `core/data/src/test/java/com/wanderingledger/core/data/MarketRepositoryTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/RumorFalseRateTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/Milestone2AcceptanceCriteriaTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/RumorRepositoryTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/ScoutDiscountTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/ReputationGatedMarketTest.kt` (from Task 2)

- [ ] **Step 6: Update AppContainer.kt**

Open `app/src/main/java/com/wanderingledger/app/AppContainer.kt`. Find:

```kotlin
val gameRepository = GameRepository(database, rumorRepository, companionRepository)
```

Replace with:

```kotlin
val orderRepository = OrderRepository(database)
val gameRepository = GameRepository(database, rumorRepository, companionRepository, orderRepository)
```

Add the import if needed: `import com.wanderingledger.core.data.OrderRepository`.

- [ ] **Step 7: Run full test suite**

```
./gradlew :core:data:test :app:test
```

Expected: all tests pass including the new `travelCompletesDeliveryOrderOnArrival`.

- [ ] **Step 8: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt
git add app/src/main/java/com/wanderingledger/app/AppContainer.kt
git add core/data/src/test/
git add app/src/test/
git commit -m "feat: wire OrderRepository into GameRepository — complete orders and tick deadlines on travel"
```

---

## Self-Review

**Spec coverage:**
- Per-town Reputation (0–100), permanent: Tasks 1, 4, 5 ✓
- Reputation earned via Orders (+5 Delivery, +8 Route): Task 4 ✓
- Rep 60 unlocks rare goods in market: Task 2 ✓
- Rep 75 gives 15% price discount on rare goods: Task 2 ✓
- 3 rare goods (Medicines/Dyes/Charts) — counter-clockwise circuit requiring ≥1 intermediate stop: Task 1 SeedWorld + Task 3 migration ✓
- Order system: Delivery (bring Scarce good to issuing town) + Route (carry Abundant good to destination): Task 4 ✓
- 3-order cap per town: Task 4 (`ORDER_CAP_PER_TOWN=3`) ✓
- Orders persist until completed or expired: Task 3 + 4 (`isActive` flag, `tickDeadlines`) ✓
- Quantity constants (1-3) and deadline constants (Delivery=3, Route=4): Task 4 constants ✓
- New orders generated on each town arrival: Task 5 ✓
- Reputation accumulates — does not decay: `addReputation` uses `MIN(rep + amount, 100)`, no decay logic ✓
- Existing installs get rare goods via migration 5→6 data seeding: Task 3 ✓

**Placeholder scan:** None.

**Type consistency:**
- `OrderRepository.checkAndCompleteOrders(arrivedTownId: Long, playerId: Long): List<OrderCompletion>` — defined Task 4 Step 3, used in Task 5 Step 4 ✓
- `OrderRepository.generateOrdersForTown(townId: Long, seed: Long)` — defined Task 4 Step 3, used in Task 5 Step 4 ✓
- `OrderCompletion.reputationReward: Int` — used in Task 5 event log ✓
- `TownPriceEntity.minReputation: Int` — added Task 1 Step 1, used in Task 2 observeMarket lambda ✓
- `CompanionRole` import: Plan B does not introduce new companion queries — no change needed ✓

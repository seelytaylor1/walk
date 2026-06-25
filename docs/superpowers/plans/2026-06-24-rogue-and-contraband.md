# Rogue and Contraband Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Cael (Rogue companion at Mistfall), two contraband goods (Smuggled Spirits, Stolen Relics), contraband market visibility (Rogue-only), and an arrival inspection engine that confiscates contraband at a base 40% chance (reduced to 5–20% with Cael's bond level).

**Architecture:** `InspectionEngine` is a pure object (no Room, no IO) — fully testable with plain JUnit 4. Contraband market filtering adds a 6th reactive source to `observeMarket` (uses nested `combine`). Inspection runs inside `GameRepository.travel()`'s arrival transaction. DB stays at version 6 (no schema changes — `GoodEntity.isContraband` and `CompanionEntity` already support everything needed).

**Prerequisite:** Plan B (`2026-06-24-orders-reputation-rare-goods.md`) must be applied first. This plan assumes DB is at version 6, `OrderRepository` exists, and `GameRepository` takes 4 constructor args.

**Tech Stack:** Kotlin, Room v4, Robolectric + plain JUnit 4.

## Global Constraints

- No new Gradle dependencies.
- Good IDs 1–6 are taken (base goods + rare goods from Plan B). Contraband goods use IDs 7 and 8.
- Companion IDs 1 (Mira/Scout) and 2 (Bram/Fighter) already exist. Cael (Rogue) uses ID 3.
- DB version stays at 6. `GoodEntity.isContraband: Boolean = false` is already in the schema. `CompanionEntity` already exists.
- `playerId = 1L` is hardcoded throughout.
- Inspection is **arrival-triggered** (not a road encounter) — it runs inside `travel()` after encounter resolution.
- Penalty is **confiscation only** — no gold fine, no reputation loss, no game over.
- Inspection applies at **every town** (universal), not just specific ones.
- `InspectionEngine` takes domain model types (`Companion?` from `com.wanderingledger.core.model`) — not database entities.
- `CompanionDao.listActiveCompanions()` returns `Flow<List<CompanionEntity>>`. For the market filter, use it as a reactive source. For inspection inside a transaction, use a snapshot: add `listActiveCompanionsSnapshot()` to `CompanionDao`.
- Rogue encounter bond scaling (merchant-cart bonus): the current `EncounterEngine.resolveMerchantCart` has `val bonus = if (party.any { it.role == CompanionRole.Rogue }) 20 else 0`. This plan upgrades it to scale with bond level.

---

## File Structure

**New:**
- `core/data/src/main/java/com/wanderingledger/core/data/InspectionEngine.kt` — pure inspection logic
- `core/data/src/test/java/com/wanderingledger/core/data/InspectionEngineTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/ContrabandMarketTest.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/InspectionIntegrationTest.kt`

**Modified:**
- `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt` — add `listActiveCompanionsSnapshot()` to `CompanionDao`; add `listContrabandItemsSnapshot(playerId)` to `InventoryDao`
- `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt` — add Cael (Rogue, companionId=3, Mistfall) + contraband goods (7, 8)
- `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt` — add Rogue contraband filter to `observeMarket`
- `core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt` — scale Rogue merchant-cart bonus with bond level
- `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt` — wire inspection into `travel()` arrival branch

---

## Task 1: Seed Rogue Companion and Contraband Goods

**Files:**
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt`

**Interfaces:**
- Produces: `CompanionDao.listActiveCompanionsSnapshot(): List<CompanionEntity>`; `InventoryDao.listContrabandItemsSnapshot(playerId): List<InventoryItemEntity>`; Cael at Mistfall; GoodEntity(7, "Smuggled Spirits", isContraband=true); GoodEntity(8, "Stolen Relics", isContraband=true).

- [ ] **Step 1: Add snapshot DAO methods to Daos.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/Daos.kt`. In `CompanionDao`, add after `listRecruitableCompanions`:

```kotlin
@Query("SELECT * FROM companions WHERE isActive = 1 ORDER BY companionId")
suspend fun listActiveCompanionsSnapshot(): List<CompanionEntity>
```

In `InventoryDao`, add after `updateItem` (from Plan B):

```kotlin
@Query(
    "SELECT i.* FROM inventory_items i " +
    "JOIN goods g ON i.goodId = g.goodId " +
    "WHERE i.playerId = :playerId AND g.isContraband = 1"
)
suspend fun listContrabandItemsSnapshot(playerId: Long): List<InventoryItemEntity>
```

- [ ] **Step 2: Seed Cael and contraband goods in SeedWorld.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt`.

In the `goodDao().insertGoods(...)` call, add the two contraband goods:

```kotlin
GoodEntity(7, "Smuggled Spirits", 25, isContraband = true),
GoodEntity(8, "Stolen Relics", 40, isContraband = true),
```

In `townDao().insertProducedGoods(...)`, add:

```kotlin
TownProducesEntity(1, 7),   // Hearthwick produces Smuggled Spirits
TownProducesEntity(3, 8),   // Mistfall produces Stolen Relics
```

In `townDao().insertDemandedGoods(...)`, add:

```kotlin
TownDemandsEntity(2, 7),   // Stoneford demands Smuggled Spirits
TownDemandsEntity(1, 8),   // Hearthwick demands Stolen Relics
```

In `townPriceDao().upsertPrices(...)`, add:

```kotlin
// Smuggled Spirits (7): Hearthwick → Stoneford
TownPriceEntity(townId = 1, goodId = 7, buyPrice = 10, sellPrice = 15, supplyLevel = "Abundant", lastUpdatedAt = now, minReputation = 0),
TownPriceEntity(townId = 2, goodId = 7, buyPrice = 28, sellPrice = 45, supplyLevel = "Scarce", lastUpdatedAt = now, minReputation = 0),
// Stolen Relics (8): Mistfall → Hearthwick
TownPriceEntity(townId = 3, goodId = 8, buyPrice = 15, sellPrice = 22, supplyLevel = "Abundant", lastUpdatedAt = now, minReputation = 0),
TownPriceEntity(townId = 1, goodId = 8, buyPrice = 38, sellPrice = 60, supplyLevel = "Scarce", lastUpdatedAt = now, minReputation = 0),
```

In `companionDao().upsertCompanions(...)`, add:

```kotlin
CompanionEntity(3, "Cael", "Rogue", 2, 0, "available", 3, false),
```

(CompanionEntity positional args: `companionId, name, role, combatPower, bondLevel, questState, locationTownId, isActive`)

- [ ] **Step 3: Build to verify**

```
./gradlew :core:database:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/database/src/main/java/com/wanderingledger/core/database/Daos.kt
git add core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt
git commit -m "feat: seed Cael (Rogue) at Mistfall + contraband goods Smuggled Spirits and Stolen Relics"
```

---

## Task 2: Contraband Market Filter + Rogue Merchant-Cart Bond Scaling

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt`
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt`
- Create: `core/data/src/test/java/com/wanderingledger/core/data/ContrabandMarketTest.kt`

**Interfaces:**
- Consumes: `CompanionDao.listActiveCompanions(): Flow<List<CompanionEntity>>`, `GoodEntity.isContraband`, `CompanionEntity.role == "Rogue"`, Task 1 seeding
- Produces: `observeMarket(townId)` hides contraband goods unless an active Rogue companion exists; `resolveMerchantCart` uses `20 + (rogue.bondLevel * 4)` bonus

- [ ] **Step 1: Write failing tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/ContrabandMarketTest.kt`:

```kotlin
package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.TestDatabaseFactory
import com.wanderingledger.core.database.WanderingLedgerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContrabandMarketTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var marketRepository: MarketRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        marketRepository = MarketRepository(database)
        val gameRepository = GameRepository(
            database,
            RumorRepository(database),
            CompanionRepository(database),
            OrderRepository(database),
        )
        gameRepository.initializeNewGame(seed = 1L)
    }

    @Test
    fun contrabandGoodsHiddenWithoutRogue() = runBlocking {
        // No Rogue active at Hearthwick (townId=1); Smuggled Spirits (goodId=7) should be hidden
        val market = marketRepository.observeMarket(townId = 1L).first()
        val goodIds = market.rows.map { it.good.goodId }
        assertFalse("Smuggled Spirits should not appear without Rogue", goodIds.contains(7L))
        assertFalse("Stolen Relics should not appear without Rogue", goodIds.contains(8L))
    }

    @Test
    fun contrabandGoodsVisibleWithActiveRogue() = runBlocking {
        // Activate Cael (Rogue, companionId=3)
        val cael = database.companionDao().listRecruitableCompanions().first()
            .find { it.companionId == 3L }!!
        database.companionDao().upsertCompanion(cael.copy(isActive = true))

        val market = marketRepository.observeMarket(townId = 1L).first()
        val goodIds = market.rows.map { it.good.goodId }
        assertTrue("Smuggled Spirits should appear with active Rogue", goodIds.contains(7L))
    }

    @Test
    fun baseGoodsAlwaysVisibleRegardlessOfRogue() = runBlocking {
        val market = marketRepository.observeMarket(townId = 1L).first()
        val goodIds = market.rows.map { it.good.goodId }
        // Apples (1) and Iron (2) are at Hearthwick; both should always be visible
        assertTrue("Apples always visible", goodIds.contains(1L))
        assertTrue("Iron always visible", goodIds.contains(2L))
    }
}
```

- [ ] **Step 2: Run tests — expect failures**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.ContrabandMarketTest"
```

Expected: `contrabandGoodsHiddenWithoutRogue` FAILS (contraband currently visible to everyone since the filter doesn't exist yet). The other tests may pass or fail depending on whether Rogue is active.

- [ ] **Step 3: Restructure `observeMarket` to include companion source**

Open `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt`. The current `observeMarket` uses `combine` with 5 sources. Adding a 6th requires nesting two `combine` calls. Replace the entire `observeMarket` function with:

```kotlin
fun observeMarket(townId: Long): Flow<MarketState> {
    val coreFlow =
        combine(
            database.townDao().getTown(townId).filterNotNull(),
            database.townPriceDao().listPricesForTown(townId),
            database.playerDao().getPlayer().filterNotNull(),
            database.inventoryDao().listInventory(playerId = 1L),
        ) { town, prices, player, inventory ->
            Triple(Pair(town, prices), Pair(player, inventory), Unit)
        }

    return combine(
        coreFlow,
        database.goodDao().listGoods(),
        database.companionDao().listActiveCompanions(),
    ) { (townAndPrices, playerAndInventory, _), goods, companions ->
        val (town, prices) = townAndPrices
        val (player, inventory) = playerAndInventory
        val goodsById = goods.associateBy { it.goodId }
        val inventoryByGoodId =
            inventory
                .groupBy { it.goodId }
                .mapValues { (_, items) -> items.sumOf { it.quantity } }
        val rep = town.reputation
        val hasActiveRogue = companions.any { it.role == "Rogue" && it.isActive }

        val rows =
            prices
                .filter { priceEntity ->
                    val minRep = priceEntity.minReputation
                    if (minRep > rep) return@filter false
                    val good = goodsById[priceEntity.goodId]
                    if (good?.isContraband == true && !hasActiveRogue) return@filter false
                    true
                }
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
}
```

Add `import com.wanderingledger.core.database.CompanionEntity` to the imports if not already present. Note: `companions` here is `List<CompanionEntity>` from the DAO Flow, so `it.role` is a `String` (`"Rogue"`), not `CompanionRole`.

- [ ] **Step 4: Update Rogue merchant-cart bonus in EncounterEngine.kt**

Open `core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt`. Find `resolveMerchantCart`. The current bonus calculation is:

```kotlin
val bonus = if (party.any { it.role == CompanionRole.Rogue }) 20 else 0
```

Replace with:

```kotlin
val rogue = party.firstOrNull { it.role == CompanionRole.Rogue && it.isActive }
val bonus = if (rogue != null) 20 + (rogue.bondLevel * 4) else 0
```

This scales the Rogue merchant-cart advantage: +20 at bond 0, +40 at bond 5.

- [ ] **Step 5: Run tests**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.ContrabandMarketTest"
```

Expected: all 3 tests pass.

- [ ] **Step 6: Run full test suite for regressions**

```
./gradlew :core:data:test :app:test
```

Expected: all tests pass. The `observeMarket` restructure produces identical output for existing goods (no contraband, no rep-gating in base data).

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt
git add core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt
git add core/data/src/test/java/com/wanderingledger/core/data/ContrabandMarketTest.kt
git commit -m "feat: contraband market filter (Rogue-only visibility) + bond-scaled merchant-cart bonus"
```

---

## Task 3: InspectionEngine (Pure Logic)

**Files:**
- Create: `core/data/src/main/java/com/wanderingledger/core/data/InspectionEngine.kt`
- Create: `core/data/src/test/java/com/wanderingledger/core/data/InspectionEngineTest.kt`

**Interfaces:**
- Consumes: `Companion?` from `com.wanderingledger.core.model` (domain model with `role: CompanionRole`, `bondLevel: Int`, `isActive: Boolean`)
- Produces:
  - `InspectionEngine.inspectionChance(activeRogue: Companion?): Double` — returns `0.40` with no Rogue; `(0.20 - bondLevel * 0.03).coerceAtLeast(0.05)` with Rogue
  - `InspectionEngine.rollInspection(chance: Double, seed: Long): Boolean` — deterministic roll

- [ ] **Step 1: Write failing tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/InspectionEngineTest.kt`:

```kotlin
package com.wanderingledger.core.data

import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionEngineTest {

    private fun makeRogue(bondLevel: Int) = Companion(
        companionId = 3,
        name = "Cael",
        role = CompanionRole.Rogue,
        combatPower = 2,
        bondLevel = bondLevel,
        questState = "active",
        locationTownId = 3,
        isActive = true,
    )

    @Test
    fun baseChanceIsFortyPercentWithoutRogue() {
        assertEquals(0.40, InspectionEngine.inspectionChance(activeRogue = null), 0.001)
    }

    @Test
    fun rogueAtBondZeroReducesChanceToTwentyPercent() {
        assertEquals(0.20, InspectionEngine.inspectionChance(makeRogue(bondLevel = 0)), 0.001)
    }

    @Test
    fun rogueAtBondFiveReducesChanceToFivePercent() {
        // 0.20 - (5 * 0.03) = 0.05 → clamped at MIN 0.05
        assertEquals(0.05, InspectionEngine.inspectionChance(makeRogue(bondLevel = 5)), 0.001)
    }

    @Test
    fun chanceNeverFallsBelowFivePercent() {
        // bond=10 would give 0.20 - 0.30 = -0.10, clamped to 0.05
        assertEquals(0.05, InspectionEngine.inspectionChance(makeRogue(bondLevel = 10)), 0.001)
    }

    @Test
    fun rollInspectionIsDeterministicForSameSeed() {
        val result1 = InspectionEngine.rollInspection(chance = 0.40, seed = 42L)
        val result2 = InspectionEngine.rollInspection(chance = 0.40, seed = 42L)
        assertEquals("Same seed should produce same result", result1, result2)
    }

    @Test
    fun inspectionNeverTriggersAtZeroChance() {
        for (seed in 1L..50L) {
            assertFalse("Inspection at 0.0 chance should never trigger", InspectionEngine.rollInspection(0.0, seed))
        }
    }

    @Test
    fun inspectionAlwaysTriggersAtFullChance() {
        for (seed in 1L..50L) {
            assertTrue("Inspection at 1.0 chance should always trigger", InspectionEngine.rollInspection(1.0, seed))
        }
    }

    @Test
    fun higherChanceProducesMoreInspectionsAcrossSeeds() {
        var highCount = 0
        var lowCount = 0
        for (seed in 1L..100L) {
            if (InspectionEngine.rollInspection(0.40, seed)) highCount++
            if (InspectionEngine.rollInspection(0.05, seed)) lowCount++
        }
        assertTrue("40% chance should trigger more than 5% chance over 100 seeds", highCount > lowCount)
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.InspectionEngineTest"
```

Expected: compile failure — `InspectionEngine` doesn't exist yet.

- [ ] **Step 3: Create InspectionEngine.kt**

Create `core/data/src/main/java/com/wanderingledger/core/data/InspectionEngine.kt`:

```kotlin
package com.wanderingledger.core.data

import com.wanderingledger.core.model.Companion
import kotlin.random.Random

object InspectionEngine {
    const val BASE_INSPECTION_CHANCE = 0.40
    const val ROGUE_BASE_INSPECTION_CHANCE = 0.20
    const val ROGUE_BOND_REDUCTION_PER_LEVEL = 0.03
    const val MIN_INSPECTION_CHANCE = 0.05

    fun inspectionChance(activeRogue: Companion?): Double =
        if (activeRogue == null) {
            BASE_INSPECTION_CHANCE
        } else {
            (ROGUE_BASE_INSPECTION_CHANCE - activeRogue.bondLevel * ROGUE_BOND_REDUCTION_PER_LEVEL)
                .coerceAtLeast(MIN_INSPECTION_CHANCE)
        }

    fun rollInspection(chance: Double, seed: Long): Boolean =
        Random(seed).nextDouble() < chance
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.InspectionEngineTest"
```

Expected: all 7 tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/InspectionEngine.kt
git add core/data/src/test/java/com/wanderingledger/core/data/InspectionEngineTest.kt
git commit -m "feat: InspectionEngine — deterministic arrival inspection with Rogue bond scaling"
```

---

## Task 4: Wire Inspection Into GameRepository

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt`
- Create: `core/data/src/test/java/com/wanderingledger/core/data/InspectionIntegrationTest.kt`

**Interfaces:**
- Consumes: `InspectionEngine.inspectionChance(Companion?)`, `InspectionEngine.rollInspection(chance, seed)`, `CompanionDao.listActiveCompanionsSnapshot()`, `InventoryDao.listContrabandItemsSnapshot(playerId)`, domain `Companion` model mapped from `CompanionEntity`
- Produces: On arrival, if player has contraband, roll inspection with `seed + 7919L` (a distinct prime to avoid colliding with encounter seed). On failure: confiscate all contraband items, log `"inspection"` event. On success: no action.

- [ ] **Step 1: Write failing integration tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/InspectionIntegrationTest.kt`:

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
class InspectionIntegrationTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        gameRepository = GameRepository(
            database,
            RumorRepository(database),
            CompanionRepository(database),
            OrderRepository(database),
        )
        gameRepository.initializeNewGame(seed = 1L)
        // Give player enough steps to travel any road
        database.playerDao().updatePlayer(
            database.playerDao().getPlayerSnapshot()!!.copy(bankedSteps = 10000)
        )
    }

    @Test
    fun contrabandConfiscatedWhenInspectedWithoutRogue() = runBlocking {
        // Add Smuggled Spirits (goodId=7, contraband) to player inventory
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 7L, quantity = 2)
        )

        // Find a seed where inspection triggers without Rogue (40% chance)
        // Segment 1: Hearthwick → Stoneford (short road)
        // Try seeds until we find one that triggers inspection
        var inspectionSeed: Long? = null
        for (s in 1L..50L) {
            val chance = InspectionEngine.inspectionChance(activeRogue = null)
            if (InspectionEngine.rollInspection(chance, seed = s + 7919L)) {
                inspectionSeed = s
                break
            }
        }
        requireNotNull(inspectionSeed) { "Could not find a seed that triggers 40% inspection in 50 tries" }

        gameRepository.travel(segmentId = 1L, seed = inspectionSeed)

        val contrabandRemaining = database.inventoryDao().listContrabandItemsSnapshot(playerId = 1L)
        assertTrue("Contraband should be confiscated on inspection", contrabandRemaining.isEmpty())
    }

    @Test
    fun contrabandNotConfiscatedWhenInspectionMisses() = runBlocking {
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 7L, quantity = 1)
        )

        // Find a seed where inspection does NOT trigger (60% of seeds at 40% chance)
        var safeSeed: Long? = null
        for (s in 1L..100L) {
            val chance = InspectionEngine.inspectionChance(activeRogue = null)
            if (!InspectionEngine.rollInspection(chance, seed = s + 7919L)) {
                safeSeed = s
                break
            }
        }
        requireNotNull(safeSeed) { "Could not find a seed that misses 40% inspection in 100 tries" }

        gameRepository.travel(segmentId = 1L, seed = safeSeed)

        val contrabandRemaining = database.inventoryDao().listContrabandItemsSnapshot(playerId = 1L)
        assertEquals("Contraband should survive when inspection misses", 1, contrabandRemaining.sumOf { it.quantity })
    }

    @Test
    fun noInspectionWhenPlayerHasNoContraband() = runBlocking {
        // Player has only Apples (goodId=1, not contraband)
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 1L, quantity = 3)
        )

        val result = gameRepository.travel(segmentId = 1L, seed = 1L)

        assertTrue("Travel should succeed", result is TravelResult.Arrived)
        // No inspection event — nothing to check (just verify travel works cleanly)
    }
}
```

- [ ] **Step 2: Run tests — expect failures**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.InspectionIntegrationTest"
```

Expected: `contrabandConfiscatedWhenInspectedWithoutRogue` FAILS — inspection not yet wired in, so contraband is never confiscated.

- [ ] **Step 3: Wire inspection into GameRepository.travel()**

Open `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt`. Inside `travel()`, in the `is TravelOutcome.Arrived ->` branch, after the order completion block (from Plan B) and before `val remainingSteps = ...`, add:

```kotlin
// Arrival inspection: check for contraband and roll inspection
val contrabandItems = database.inventoryDao().listContrabandItemsSnapshot(player.playerId)
if (contrabandItems.isNotEmpty()) {
    val activeCompanionEntities = database.companionDao().listActiveCompanionsSnapshot()
    val activeRogue = snapshot.activeCompanions.firstOrNull {
        it.role == com.wanderingledger.core.model.CompanionRole.Rogue && it.isActive
    }
    val inspectionChance = InspectionEngine.inspectionChance(activeRogue)
    val inspected = InspectionEngine.rollInspection(inspectionChance, seed = seed + 7919L)
    if (inspected) {
        contrabandItems.forEach { item ->
            database.inventoryDao().removeItem(item.id)
        }
        database.eventLogDao().insertEvent(
            EventLogEntity(
                type = "inspection",
                meta = "{\"arrivedTownId\":${delta.newTownId},\"itemsConfiscated\":${contrabandItems.size}}",
                result = "Your goods were inspected. Contraband was confiscated.",
                createdAt = delta.arrivedAt,
            )
        )
    }
}
```

Note: `snapshot.activeCompanions` is already populated — no extra DB query needed for the domain model lookup. The `listActiveCompanionsSnapshot()` call on the DAO is unused in this version; remove it from the block above (the snapshot contains the domain companions already).

Add the required import at the top of `GameRepositories.kt` if not already present:
```kotlin
import com.wanderingledger.core.data.InspectionEngine
```

- [ ] **Step 4: Run tests**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.InspectionIntegrationTest"
```

Expected: all 3 tests pass.

- [ ] **Step 5: Run full test suite**

```
./gradlew :core:data:test :app:test
```

Expected: all tests pass. Inspection only fires when contraband exists — no impact on base-game travel tests.

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt
git add core/data/src/test/java/com/wanderingledger/core/data/InspectionIntegrationTest.kt
git commit -m "feat: wire arrival inspection into travel() — confiscate contraband on failed inspection"
```

---

## Self-Review

**Spec coverage:**
- Cael (Rogue) recruitable at Mistfall (townId=3): Task 1 seeding ✓
- Contraband visible only when Rogue is active: Task 2 `observeMarket` filter ✓
- Inspection on town arrival (arrival-triggered, not road encounter): Task 4 ✓
- Universal inspection (every town): Task 4 — no town filtering, applies on every arrival ✓
- 40% base inspection rate: `InspectionEngine.BASE_INSPECTION_CHANCE = 0.40` Task 3 ✓
- With Rogue: `0.20 - bondLevel*0.03`, min 5%: `InspectionEngine.inspectionChance()` Task 3 ✓
- Penalty: confiscation only (no gold/rep penalty): Task 4 removes items only ✓
- Smuggled Spirits (Hearthwick→Stoneford): Task 1 seeding ✓
- Stolen Relics (Mistfall→Hearthwick): Task 1 seeding ✓
- Rogue merchant-cart bonus scales with bond: Task 2 `resolveMerchantCart` update ✓
- No schema change (isContraband already in GoodEntity): confirmed — no migration ✓

**Placeholder scan:** None.

**Type consistency:**
- `InspectionEngine.inspectionChance(activeRogue: Companion?)` — defined Task 3, used in Task 4. `Companion` is the domain model from `com.wanderingledger.core.model`. `snapshot.activeCompanions: List<Companion>` carries the right type. ✓
- `InspectionEngine.rollInspection(chance: Double, seed: Long): Boolean` — defined Task 3, called in Task 4 with `seed + 7919L`. ✓
- `CompanionDao.listActiveCompanionsSnapshot(): List<CompanionEntity>` — added Task 1 Step 1. Not needed in Task 4 body (domain companions already in snapshot). The DAO method remains available for future use. ✓
- `InventoryDao.listContrabandItemsSnapshot(playerId: Long): List<InventoryItemEntity>` — added Task 1 Step 1, used in Task 4. ✓
- `observeMarket` `companions` list is `List<CompanionEntity>` (from DAO Flow), so role check uses string `"Rogue"` not `CompanionRole.Rogue`. This matches Task 2 Step 3 code. ✓

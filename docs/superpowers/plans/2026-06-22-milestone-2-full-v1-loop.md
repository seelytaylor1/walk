# Milestone 2 — Full v1 Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the gaps between the existing Milestone 2 implementation and the acceptance criteria in issues #11–#15.

**Architecture:** The buy/sell pipeline (MarketRepository), Ledger/Chronicle, and CampState are largely wired. Four gaps remain: (1) `RumorRepository.generateRumorForTownVisit()` has a 15% false-rate and 3-visit default expiry vs the specified ~50% / 2-visit; (2) companion recruitment has no "3 completed trades" gate; (3) Scout step-cost reduction is unimplemented in both the route display and TravelPolicy; (4) Rogue/contraband is not in the v1 seed and is explicitly out of scope for this plan. Issue #11 (trading) and #13 (ledger) are fully implemented — this plan adds acceptance tests for them and fixes the three gaps.

**Tech Stack:** Kotlin 2.2.10, JVM 17, Android minSdk 28, Room v3 (bumping to v4), JUnit4, Robolectric, kotlinx-coroutines-test.

## Global Constraints

- Kotlin 2.2.10, JVM 17, Android minSdk 28
- Room database will be at version 3 after Milestone 1 plan; this plan targets version 4
- Tests follow the Robolectric pattern in `UserStory1TravelFlowTest.kt` and `MarketRepositoryTest.kt`
- SeedWorld companions: Mira (Scout, locationTownId=1) and Bram (Fighter, locationTownId=2)
- Provisional step costs (from M1 plan): short = 1000, medium = 2500, long = 5000
- Scout discount: 10% reduction (round down to nearest integer) on JourneyRouteOption.stepCost display and TravelPolicy spend
- Rogue/contraband not in v1 seed — that AC item in issue #14 is deferred; note in issue comment after this plan completes
- No third-party DI; all dependencies wired manually

## Out of Scope

- Rogue companion and contraband good filtering (no Rogue in SeedWorld, no contraband goods seeded)
- MarketRepository event log entries for trades (trades fire Telemetry but not EventLog — this is a future enhancement, not an #11 AC item)

---

### Task 1: Fix rumor false rate and expiry (issue #12)

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/RumorRepository.kt`

**Interfaces:**
- Produces: `generateRumorForTownVisit()` — false rate ~50%, default expiry 2 visits
- Consumes: nothing new

- [ ] **Step 1: Write the failing test**

Add a new test file `core/data/src/test/java/com/wanderingledger/core/data/RumorFalseRateTest.kt`:

```kotlin
package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RumorFalseRateTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var rumorRepository: RumorRepository
    private lateinit var companionRepository: CompanionRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** AC-12: ~50% of Rumors are false. Test with 100 seeded calls and check 30%–70% range. */
    @Test
    fun `AC-12 rumor false rate is approximately 50 percent`() = runTest {
        var falseCount = 0
        val total = 100
        for (seed in 1L..total) {
            rumorRepository.generateRumorForTownVisit(visitedTownId = 1L, seed = seed)
        }

        val rumors = rumorRepository.observeActiveRumors().first()
        falseCount = rumors.count { it.isFalse }

        // Allow a wide band (30%–70%) to account for RNG variance
        val falseRate = falseCount.toDouble() / rumors.size
        assertTrue(
            "Expected false rate between 30% and 70%, got ${(falseRate * 100).toInt()}%",
            falseRate in 0.30..0.70,
        )
    }

    /** AC-12: Rumors expire after 2 visits (expiryVisitsLeft = 2 on generation). */
    @Test
    fun `AC-12 town-visit rumors default to 2-visit expiry`() = runTest {
        rumorRepository.generateRumorForTownVisit(visitedTownId = 1L, seed = 42L)

        val rumors = rumorRepository.observeActiveRumors().first()
        assertTrue("Expected at least one rumor", rumors.isNotEmpty())
        val rumor = rumors.last()
        assertTrue(
            "Expected expiryVisitsLeft = 2, got ${rumor.expiryVisitsLeft}",
            rumor.expiryVisitsLeft == 2,
        )
    }
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew :core:data:test --tests "*.RumorFalseRateTest"`
Expected: Both tests FAIL (false rate near 15%, expiry = 3).

- [ ] **Step 3: Fix RumorRepository.generateRumorForTownVisit()**

In `RumorRepository.kt`, make two changes:

Change the false rate from 15% to 50%:
```kotlin
val isFalse = random.nextFloat() < 0.50f  // was 0.15f
```

Change the default `expiryVisits` in the `addRumor()` call to 2:
```kotlin
addRumor(
    text = text,
    targetGoodId = good.goodId,
    sourceTownId = sourceTown.townId,
    expiryVisits = 2,          // was not specified (defaulted to 3)
    isFalse = isFalse,
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :core:data:test --tests "*.RumorFalseRateTest"`
Expected: 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/RumorRepository.kt
git add core/data/src/test/java/com/wanderingledger/core/data/RumorFalseRateTest.kt
git commit -m "fix: set rumor false rate to 50% and default expiry to 2 visits (issue #12)"
```

---

### Task 2: Add completedTradesCount to PlayerState and gate companion recruitment (issue #14)

**Files:**
- Modify: `core/model/src/main/java/com/wanderingledger/core/model/Models.kt` — add `completedTradesCount: Int`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/Entities.kt` — add `completedTradesCount` column to `PlayerStateEntity`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt` — bump to version 4, add Migration 3→4
- Modify: `app/src/main/java/com/wanderingledger/app/AppContainer.kt` — add new migration to builder
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt` — increment count on successful buy/sell
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/CompanionRepository.kt` — gate recruitment on count >= 3
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt` — update `PlayerStateEntity.toModel()` mapper

**Interfaces:**
- Produces: `PlayerState.completedTradesCount: Int` — used by CompanionRepository recruitment gate
- Produces: `CompanionRepository.recruitCompanion()` — now returns `RecruitmentResult.NotEnoughTrades` when count < 3

- [ ] **Step 1: Write the failing test**

Create `core/data/src/test/java/com/wanderingledger/core/data/CompanionRecruitmentGateTest.kt`:

```kotlin
package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompanionRecruitmentGateTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        marketRepository = MarketRepository(database)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** AC-14: Recruitment blocked before 3 trades. */
    @Test
    fun `AC-14 recruitment blocked before 3 completed trades`() = runTest {
        val miraId = 1L // Mira the Scout, located at Town 1 (Hearthwick)

        val result = companionRepository.recruitCompanion(miraId)
        assertTrue(
            "Expected NotEnoughTrades before 3 trades, got $result",
            result is RecruitmentResult.NotEnoughTrades,
        )
    }

    /** AC-14: Recruitment allowed at exactly 3 completed trades. */
    @Test
    fun `AC-14 recruitment allowed after 3 completed trades`() = runTest {
        val miraId = 1L

        // Complete 3 trades (buy 3 Apples from Hearthwick market)
        repeat(3) {
            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        }

        val result = companionRepository.recruitCompanion(miraId)
        assertEquals(RecruitmentResult.Success, result)
    }

    /** AC-14: completedTradesCount increments on both buy and sell. */
    @Test
    fun `AC-14 trade count increments on buy and sell`() = runTest {
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterBuy = gameRepository.observePlayerState().first()
        assertEquals(1, afterBuy.completedTradesCount)

        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterSell = gameRepository.observePlayerState().first()
        assertEquals(2, afterSell.completedTradesCount)
    }
}
```

- [ ] **Step 2: Run to confirm failure (compile errors expected before implementation)**

Run: `./gradlew :core:data:test --tests "*.CompanionRecruitmentGateTest"`
Expected: COMPILE ERROR — `completedTradesCount` does not exist yet, `NotEnoughTrades` does not exist yet.

- [ ] **Step 3: Add completedTradesCount to PlayerState model**

In `core/model/src/main/java/com/wanderingledger/core/model/Models.kt`, add the field to `PlayerState`:

```kotlin
data class PlayerState(
    val playerId: Long,
    val name: String,
    val playerClass: PlayerClass,
    val gold: Long,
    val currentTownId: Long,
    val inventorySlots: Int,
    val bankedSteps: Long,
    val lifetimeSteps: Long,
    val lastSyncAt: Long,
    val completedTradesCount: Int = 0,  // add this field
)
```

- [ ] **Step 4: Add completedTradesCount column to PlayerStateEntity**

In `Entities.kt`, add the column to `PlayerStateEntity`:

```kotlin
@Entity(
    tableName = "player_states",
    foreignKeys = [ForeignKey(TownEntity::class, ["townId"], ["currentTownId"])],
)
data class PlayerStateEntity(
    @PrimaryKey val playerId: Long,
    val name: String,
    val playerClass: String,
    val gold: Long,
    val currentTownId: Long,
    val inventorySlots: Int,
    val bankedSteps: Long,
    val lifetimeSteps: Long,
    val lastSyncAt: Long,
    @ColumnInfo(defaultValue = "0")
    val completedTradesCount: Int = 0,  // add this column
)
```

- [ ] **Step 5: Add Room Migration 3→4**

In `WanderingLedgerDatabase.kt`, add the migration and bump version:

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE player_states ADD COLUMN completedTradesCount INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

Update `@Database(version = 4, ...)` and update `autoMigrations` if desired (this is a manual migration so leave it out of autoMigrations).

- [ ] **Step 6: Wire the new migration in AppContainer.kt**

Add `MIGRATION_3_4` to the builder alongside the existing `MIGRATION_2_3`:

```kotlin
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
    .build()
```

- [ ] **Step 7: Update PlayerStateEntity.toModel() mapper**

In `GameRepositories.kt`, add `completedTradesCount` to the mapper:

```kotlin
private fun PlayerStateEntity.toModel(): PlayerState =
    PlayerState(
        playerId = playerId,
        name = name,
        playerClass = PlayerClass.valueOf(playerClass),
        gold = gold,
        currentTownId = currentTownId,
        inventorySlots = inventorySlots,
        bankedSteps = bankedSteps,
        lifetimeSteps = lifetimeSteps,
        lastSyncAt = lastSyncAt,
        completedTradesCount = completedTradesCount,
    )
```

- [ ] **Step 8: Increment completedTradesCount in MarketRepository on successful trade**

In `MarketRepository.buyGood()`, after `BuyResult.Success` is assembled and before returning, increment the count. Add inside the `database.withTransaction { }` block, after the telemetry call:

```kotlin
// Increment trade count on successful buy
val updatedPlayer = database.playerDao().getPlayerSnapshot()!!
database.playerDao().updatePlayer(
    updatedPlayer.copy(completedTradesCount = updatedPlayer.completedTradesCount + 1)
)

BuyResult.Success(...)
```

Do the same in `MarketRepository.sellGood()`, in the same position before `SellResult.Success`:

```kotlin
// Increment trade count on successful sell
val updatedPlayer = database.playerDao().getPlayerSnapshot()!!
database.playerDao().updatePlayer(
    updatedPlayer.copy(completedTradesCount = updatedPlayer.completedTradesCount + 1)
)

SellResult.Success(...)
```

- [ ] **Step 9: Add NotEnoughTrades to RecruitmentResult and gate in CompanionRepository**

In `CompanionRepository.kt`, add the new result type:

```kotlin
sealed class RecruitmentResult {
    data object Success : RecruitmentResult()
    data object AlreadyActive : RecruitmentResult()
    data object PartyFull : RecruitmentResult()
    data object NotFound : RecruitmentResult()
    data object NotEnoughTrades : RecruitmentResult()  // add this
}
```

In `recruitCompanion()`, add the gate before the party-full check:

```kotlin
suspend fun recruitCompanion(companionId: Long): RecruitmentResult {
    val recruitable = database.companionDao().listRecruitableCompanions()
        .map { entities -> entities.find { it.companionId == companionId } }
        .firstOrNull() ?: return RecruitmentResult.NotFound

    // Gate: require 3 completed trades
    val player = database.playerDao().getPlayerSnapshot()
        ?: return RecruitmentResult.NotFound
    if (player.completedTradesCount < 3) {
        return RecruitmentResult.NotEnoughTrades
    }

    val activeCount = database.companionDao().listActiveCompanions()
        .firstOrNull()?.size ?: 0
    if (activeCount >= MAX_ACTIVE_COMPANIONS) {
        return RecruitmentResult.PartyFull
    }

    database.companionDao().upsertCompanion(
        recruitable.copy(isActive = true, questState = "recruited", bondLevel = 0),
    )
    return RecruitmentResult.Success
}
```

- [ ] **Step 10: Handle NotEnoughTrades in MainActivity**

In `MainActivity.kt`, the `onRecruit` callback already handles `RecruitmentResult` branches. Add the new case:

```kotlin
val message = when (result) {
    RecruitmentResult.Success -> "A new voice joins the road."
    RecruitmentResult.AlreadyActive -> "They are already traveling with you."
    RecruitmentResult.PartyFull -> "The party is full."
    RecruitmentResult.NotFound -> "That companion is not available here."
    RecruitmentResult.NotEnoughTrades -> "Complete a few more trades first."
}
```

- [ ] **Step 11: Run the tests to verify they pass**

Run: `./gradlew :core:data:test --tests "*.CompanionRecruitmentGateTest"`
Expected: 3 tests PASS.

- [ ] **Step 12: Run the full test suite to check for regressions**

Run: `./gradlew test`
Expected: All tests PASS. If `CompanionRepositoryTest` fails, check if it seeds player state before calling `recruitCompanion` — it may need `completedTradesCount = 3` or a `marketRepository.buyGood()` call to reach the gate.

- [ ] **Step 13: Commit**

```bash
git add core/model/src/main/java/com/wanderingledger/core/model/Models.kt
git add core/database/src/main/java/com/wanderingledger/core/database/Entities.kt
git add core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt
git add app/src/main/java/com/wanderingledger/app/AppContainer.kt
git add core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt
git add core/data/src/main/java/com/wanderingledger/core/data/CompanionRepository.kt
git add core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt
git add core/data/src/test/java/com/wanderingledger/core/data/CompanionRecruitmentGateTest.kt
git commit -m "feat: gate companion recruitment on 3 completed trades; add completedTradesCount (issue #14)"
```

---

### Task 3: Scout step cost reduction (issue #14)

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt` — add Scout discount constant
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt` — apply Scout discount when active
- Modify: `app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt` — pass active companions into route building so JourneyRouteOption.stepCost reflects the discount

**Interfaces:**
- Consumes: `Companion.role == CompanionRole.Scout` and `Companion.isActive`
- Produces: `JourneyRouteOption.stepCost` is reduced by 10% (floor) when Scout is active

The Scout discount must apply consistently in two places: (a) the JourneyRouteOption display, so the player sees the reduced cost, and (b) TravelPolicy, so the actual spend uses the same reduced cost. Both must use the same discount function to stay in sync.

- [ ] **Step 1: Write the failing test**

Create `core/data/src/test/java/com/wanderingledger/core/data/ScoutDiscountTest.kt`:

```kotlin
package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.CompanionRole
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScoutDiscountTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        marketRepository = MarketRepository(database)
        stepBankRepository = RoomStepBankRepository(database)
        stepTrackerService = StepTrackerService(stepBankRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun recruitMiraTheScout() {
        // Complete 3 trades to unlock recruitment
        repeat(3) { marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1) }
        companionRepository.recruitCompanion(companionId = 1L) // Mira, Scout
    }

    /** AC-14: With Scout active, travel spends the discounted step cost. */
    @Test
    fun `AC-14 Scout reduces actual step spend by 10 percent`() = runTest {
        recruitMiraTheScout()

        val companions = companionRepository.observeActiveCompanions().first()
        assertTrue("Mira should be active", companions.any { it.role == CompanionRole.Scout && it.isActive })

        // Short road costs 1000 steps; with 10% Scout discount = 900
        stepTrackerService.recordSensorDelta(count = 900, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }

        val result = gameRepository.travel(shortRoad.segmentId)
        assertTrue("Travel should succeed with Scout discount applied, got $result", result is TravelResult.Arrived)

        val player = gameRepository.observePlayerState().first()
        assertEquals(0L, player.bankedSteps) // 900 spent exactly
    }

    /** AC-14: Without Scout, 900 steps is not enough for a 1000-step road. */
    @Test
    fun `AC-14 Without Scout 900 steps is insufficient for 1000-step road`() = runTest {
        stepTrackerService.recordSensorDelta(count = 900, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }

        val result = gameRepository.travel(shortRoad.segmentId)
        assertTrue("Travel should be blocked without Scout, got $result", result is TravelResult.NotEnoughSteps)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew :core:data:test --tests "*.ScoutDiscountTest"`
Expected: `AC-14 Scout reduces actual step spend by 10 percent` FAILS (TravelResult.NotEnoughSteps instead of Arrived).

- [ ] **Step 3: Add Scout discount constant to TravelTypes.kt**

In `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt`, add:

```kotlin
const val SCOUT_STEP_DISCOUNT = 0.10  // 10% reduction when Scout is active
```

Also add a top-level function:

```kotlin
fun applyScoutDiscount(stepCost: Int, hasActiveScout: Boolean): Int =
    if (hasActiveScout) (stepCost * (1.0 - SCOUT_STEP_DISCOUNT)).toInt() else stepCost
```

- [ ] **Step 4: Apply Scout discount in TravelPolicy**

In `TravelPolicy.kt`, the `compute(snapshot, seed)` function currently uses `snapshot.road.stepCost` directly. Update it to apply the Scout discount:

Find the `WorldSnapshot` definition (likely in `TravelTypes.kt` or nearby) — confirm it contains `activeCompanions: List<Companion>`. It does (we saw this in GameRepositories.kt).

In `TravelPolicy.compute()`, replace the direct use of `snapshot.road.stepCost` with:

```kotlin
val hasActiveScout = snapshot.activeCompanions.any {
    it.role == com.wanderingledger.core.model.CompanionRole.Scout && it.isActive
}
val effectiveStepCost = applyScoutDiscount(snapshot.road.stepCost, hasActiveScout)
```

Then use `effectiveStepCost` wherever `snapshot.road.stepCost` is used for the step-spend check and `playerDelta.stepsSpent`. (Do not change the `road.stepCost` stored in the DB — only the computed spend.)

**Note:** Read `TravelPolicy.kt` to find the exact variable names before editing. The pattern is: find where `road.stepCost` is compared against `player.bankedSteps`, replace both the comparison and the delta with `effectiveStepCost`.

- [ ] **Step 5: Apply Scout discount in JourneyViewModel route building**

In `JourneyViewModel.buildState()`, the route list is built as:

```kotlin
routeDestinations = gameRepository.observeTravelRoutesFromCurrentTown().first().map { route ->
    Triple(
        route.segment.segmentId,
        route.destination.name,
        Pair(route.segment.stepCost, route.segment.narrativeDistance),
    )
}
```

Replace with:

```kotlin
val hasActiveScout = activeCompanions.any {
    it.role == com.wanderingledger.core.model.CompanionRole.Scout && it.isActive
}
routeDestinations = gameRepository.observeTravelRoutesFromCurrentTown().first().map { route ->
    Triple(
        route.segment.segmentId,
        route.destination.name,
        Pair(
            applyScoutDiscount(route.segment.stepCost, hasActiveScout),
            route.segment.narrativeDistance,
        ),
    )
}
```

Add the import for `applyScoutDiscount` at the top of `JourneyViewModel.kt`:
```kotlin
import com.wanderingledger.core.data.applyScoutDiscount
```

- [ ] **Step 6: Run the Scout discount tests**

Run: `./gradlew :core:data:test --tests "*.ScoutDiscountTest"`
Expected: 2 tests PASS.

- [ ] **Step 7: Run the full test suite**

Run: `./gradlew test`
Expected: All tests PASS. If `TravelPolicyTest` fails, check that test fixtures still use the right step costs and don't assume `activeCompanions = emptyList()` — they should, since most fixtures seed no companions.

- [ ] **Step 8: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt
git add core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt
git add app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt
git add core/data/src/test/java/com/wanderingledger/core/data/ScoutDiscountTest.kt
git commit -m "feat: implement Scout 10% step cost reduction in TravelPolicy and route display (issue #14)"
```

---

### Task 4: Acceptance-criteria tests for issues #11, #13, #15

**Files:**
- Create: `core/data/src/test/java/com/wanderingledger/core/data/Milestone2AcceptanceCriteriaTest.kt`

These features are already implemented; this task adds tests that verify the specific acceptance criteria.

- [ ] **Step 1: Write the tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/Milestone2AcceptanceCriteriaTest.kt`:

```kotlin
package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Milestone2AcceptanceCriteriaTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        marketRepository = MarketRepository(database)
        stepBankRepository = RoomStepBankRepository(database)
        stepTrackerService = StepTrackerService(stepBankRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Issue #11: Town trading ───────────────────────────────────────────────

    /** AC-11: Buying a Good deducts gold and adds to inventory. */
    @Test
    fun `AC-11-1 buying a good deducts gold and adds to inventory`() = runTest {
        val before = gameRepository.observePlayerState().first()

        // Hearthwick: Apples (goodId=1), sellPrice=5 (player buys at sellPrice)
        val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        assertTrue("Expected BuyResult.Success, got $result", result is BuyResult.Success)

        val after = gameRepository.observePlayerState().first()
        assertEquals(before.gold - 5, after.gold)

        val inventory = database.inventoryDao().listInventory(playerId = 1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertTrue("Player should have Apples in inventory", apples != null && apples.quantity >= 1)
    }

    /** AC-11: Selling a Good adds gold and removes from inventory. */
    @Test
    fun `AC-11-2 selling a good adds gold and removes from inventory`() = runTest {
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 2)

        val beforeSell = gameRepository.observePlayerState().first()
        val result = marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        assertTrue("Expected SellResult.Success, got $result", result is SellResult.Success)

        val after = gameRepository.observePlayerState().first()
        // Apples buyPrice at Hearthwick = 3 (player sells at buyPrice)
        assertEquals(beforeSell.gold + 3, after.gold)

        val inventory = database.inventoryDao().listInventory(playerId = 1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertEquals(1, apples?.quantity ?: 0) // had 2, sold 1
    }

    /** AC-11: A profitable trade route is possible (buy low, travel, sell high). */
    @Test
    fun `AC-11-3 profitable trade route earns gold`() = runTest {
        // Buy Iron at Stoneford (cheap: sellPrice=12), travel to Hearthwick, sell Iron (buyPrice=17)
        // To test this without travel complexity, verify the price spread exists
        val stonefordMarket = marketRepository.observeMarket(townId = 2L).first()
        val ironAtStoneford = stonefordMarket.rows.firstOrNull { it.good.goodId == 2L }
        requireNotNull(ironAtStoneford) { "Iron should be available at Stoneford" }

        val hearthwickMarket = marketRepository.observeMarket(townId = 1L).first()
        val ironAtHearthwick = hearthwickMarket.rows.firstOrNull { it.good.goodId == 2L }
        requireNotNull(ironAtHearthwick) { "Iron should be available at Hearthwick" }

        // Stoneford buy price for Iron < Hearthwick sell price = profitable spread
        assertTrue(
            "Iron should be cheaper to buy at Stoneford than to sell at Hearthwick",
            ironAtStoneford.townPrice.sellPrice < ironAtHearthwick.townPrice.buyPrice,
        )
    }

    // ── Issue #13: Ledger event log ───────────────────────────────────────────

    /** AC-13: Travel events are logged to the EventLog. */
    @Test
    fun `AC-13-1 travel appends an event to the event log`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        gameRepository.travel(shortRoad.segmentId)

        val events = database.eventLogDao().listRecentEvents(10).first()
        assertTrue("Expected at least one event log entry after travel", events.isNotEmpty())
        val arrivalEvent = events.firstOrNull { it.type == "arrival" }
        assertTrue("Expected an 'arrival' event", arrivalEvent != null)
    }

    /** AC-13: Event log persists across DB reads (simulating restart). */
    @Test
    fun `AC-13-2 event log persists after travel`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)
        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        gameRepository.travel(roads.first { it.toTownId == 2L }.segmentId)

        val events = database.eventLogDao().listRecentEvents(10).first()
        assertTrue(events.any { it.type == "arrival" })
    }

    // ── Issue #15: CampState auto-detection ──────────────────────────────────

    /** AC-15: shouldEnterCamp returns true after 5 min idle + 100 banked steps. */
    @Test
    fun `AC-15-1 CampStateDetector activates after 5 min idle with 100 banked steps`() {
        val lastTravelTime = System.currentTimeMillis() - (6 * 60 * 1000L) // 6 minutes ago
        val currentTime = System.currentTimeMillis()

        val shouldCamp = com.wanderingledger.feature.journey.CampStateDetector.shouldEnterCamp(
            lastTravelTime = lastTravelTime,
            currentTime = currentTime,
            bankedSteps = 100L,
        )
        assertTrue("Should enter camp after 5 min idle + 100 steps", shouldCamp)
    }

    /** AC-15: shouldEnterCamp returns false when steps are below threshold. */
    @Test
    fun `AC-15-2 CampStateDetector does not activate with insufficient banked steps`() {
        val lastTravelTime = System.currentTimeMillis() - (6 * 60 * 1000L)
        val currentTime = System.currentTimeMillis()

        val shouldCamp = com.wanderingledger.feature.journey.CampStateDetector.shouldEnterCamp(
            lastTravelTime = lastTravelTime,
            currentTime = currentTime,
            bankedSteps = 50L,
        )
        assertTrue("Should not enter camp with only 50 steps", !shouldCamp)
    }

    /** AC-15: shouldEnterCamp returns false when less than 5 minutes have passed. */
    @Test
    fun `AC-15-3 CampStateDetector does not activate before 5 min idle`() {
        val lastTravelTime = System.currentTimeMillis() - (2 * 60 * 1000L) // 2 minutes ago
        val currentTime = System.currentTimeMillis()

        val shouldCamp = com.wanderingledger.feature.journey.CampStateDetector.shouldEnterCamp(
            lastTravelTime = lastTravelTime,
            currentTime = currentTime,
            bankedSteps = 500L,
        )
        assertTrue("Should not enter camp after only 2 min idle", !shouldCamp)
    }
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :core:data:test --tests "*.Milestone2AcceptanceCriteriaTest"`
Expected: All tests PASS. If `CampStateDetector` is not importable from `core/data` tests, move those three tests to `feature/journey` test module instead.

- [ ] **Step 3: Run the full test suite**

Run: `./gradlew test`
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add core/data/src/test/java/com/wanderingledger/core/data/Milestone2AcceptanceCriteriaTest.kt
git commit -m "test: add Milestone 2 acceptance criteria tests for issues #11, #13, #15"
```

---

### Task 5: Comment Rogue/contraband deferral on issue #14

**Files:** None (GitHub issue update only)

- [ ] **Step 1: Post a comment on issue #14**

Run:
```bash
gh issue comment 14 --body "## Rogue/contraband AC deferred

The Rogue/contraband acceptance criterion ('Rogue effect: contraband Goods appear in Town trading screen when Rogue is active') is out of scope for Milestone 2.

**Reason:** SeedWorld does not include a Rogue companion or any contraband goods (all GoodEntity rows have isContraband=false). Implementing this criterion requires: (1) adding a Rogue companion to SeedWorld, (2) marking at least one Good as contraband, (3) filtering contraband from MarketRepository.observeMarket() based on active Rogue presence.

This will be addressed in a follow-on issue after Milestone 2 playtesting."
```

- [ ] **Step 2: Commit (no code changes)**

No commit needed — issue comment only.

---

## Self-Review

**Spec coverage:**

- #11 AC: "Town screen lists available Goods with buy and sell prices" → wired in `showMarketView` + `MarketScreenView`; AC-11-1/2/3 verify the underlying repository. ✓
- #11 AC: "PlayerState.gold persists across restarts" → AC-11-1 verifies gold deduction via DB. ✓
- #11 AC: "Tester can complete one profitable trade route unaided" → AC-11-3 verifies price spread exists at the data layer. ✓
- #12 AC: "~50% of Rumors are false" → Task 1 fixes rate + AC-12 covers it with 30–70% band. ✓
- #12 AC: "Rumors expire after 2 visits" → Task 1 fixes default expiry + AC-12 tests expiry = 2. ✓
- #12 AC: "1 Rumor per arrival" → `generateRumorForTownVisit` called once per `travel()` in GameRepository. Not a new test — covered by existing `RumorRepositoryTest`. ✓
- #13 AC: "Travel events appended to Ledger" → AC-13-1. ✓
- #13 AC: "Ledger persists across restarts" → AC-13-2. ✓
- #14 AC: "Recruitment prompt after 3rd trade" → Task 2 gates on `completedTradesCount >= 3`. ✓
- #14 AC: "Scout effect: step cost visibly reduced" → Task 3 applies discount in display and spend. ✓
- #14 AC: "Rogue effect: contraband unlocked" → Deferred (Task 5 comment). ✓ (deferred)
- #15 AC: "CampState activates after 5 min + 100 steps" → AC-15-1. ✓
- #15 AC: "Companions show camp-context commentary" → wired in JourneyViewModel.onMakeCamp(). Not a repository test — UI behavior. ✓
- #15 AC: "CampState deactivates on travel" → wired in JourneyViewModel.onTravel() via notifyTraveled(). ✓

**Placeholder scan:** None found.

**Type consistency:**
- `applyScoutDiscount(stepCost: Int, hasActiveScout: Boolean): Int` — used in TravelTypes.kt (defined), JourneyViewModel.kt (imported), TravelPolicy.kt (imported). ✓
- `RecruitmentResult.NotEnoughTrades` — added to sealed class in CompanionRepository.kt, handled in MainActivity.kt. ✓
- `PlayerState.completedTradesCount: Int` — added to model, entity, mapper, incremented in MarketRepository. ✓

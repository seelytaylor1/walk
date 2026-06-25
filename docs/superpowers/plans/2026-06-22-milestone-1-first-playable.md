# Milestone 1 — First Playable Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the step-bank → travel → arrive loop into compliance with the Milestone 1 acceptance criteria in issues #8, #9, and #10.

**Architecture:** The full loop is already implemented in code (StepTrackerService → RoomStepBankRepository, GameRepository.travel → TravelPolicy, Room persistence). The gaps are: (1) SeedWorld step costs are placeholder values (120/180/240) that don't match the PRD provisional values (1000/2500/5000), and (2) the existing `UserStory1TravelFlowTest` needs updating to match, and (3) acceptance-criteria-level tests are missing for issues #8, #9, and #10.

**Tech Stack:** Kotlin 2.2.10, JVM 17, Android minSdk 28, Room v2 (bumping to v3), JUnit4, Robolectric, kotlinx-coroutines-test.

## Global Constraints

- Kotlin 2.2.10, JVM 17, Android minSdk 28
- Room database currently at version 2; all data/schema changes require a new version + Migration object
- Tests use JUnit4 + Robolectric (see `UserStory1TravelFlowTest` as the prior-art pattern)
- `TestDatabaseFactory.createInMemoryDatabase(context)` creates the in-memory test DB — it seeds from SeedWorld, so SeedWorld changes automatically apply in tests
- Provisional step costs from PRD: short road = 1,000 steps, medium road = 2,500 steps, long road = 5,000 steps
- No third-party DI; all dependencies wired manually

---

### Task 1: Update SeedWorld step costs + add Room Migration 2→3

**Files:**
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/AppContainer.kt` (add migration to Room builder)

**Interfaces:**
- Produces: `RoadSegment.stepCost` values — short = 1000, medium = 2500, long = 5000 — used by all downstream tasks

- [ ] **Step 1: Update SeedWorld road step costs**

In `SeedWorld.kt`, replace the `roadSegmentDao().insertRoads(...)` block:

```kotlin
database.roadSegmentDao().insertRoads(
    listOf(
        RoadSegmentEntity(1, 1, 2, 1000, "short", "[\"merchant-cart\"]"),
        RoadSegmentEntity(2, 2, 1, 1000, "short", "[\"merchant-cart\"]"),
        RoadSegmentEntity(3, 2, 3, 2500, "medium", "[\"fog-bank\"]"),
        RoadSegmentEntity(4, 3, 2, 2500, "medium", "[\"fog-bank\"]"),
        RoadSegmentEntity(5, 1, 3, 5000, "long", "[\"old-road\"]"),
        RoadSegmentEntity(6, 3, 1, 5000, "long", "[\"old-road\"]"),
    ),
)
```

- [ ] **Step 2: Add Room Migration 2→3 in WanderingLedgerDatabase.kt**

Add this migration object and wire it into the builder. The column name is `stepCost` (no `@ColumnInfo` rename). The table name is `road_segments`.

In `WanderingLedgerDatabase.kt`, add before the class declaration:

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            UPDATE road_segments SET stepCost = CASE segmentId
                WHEN 1 THEN 1000
                WHEN 2 THEN 1000
                WHEN 3 THEN 2500
                WHEN 4 THEN 2500
                WHEN 5 THEN 5000
                WHEN 6 THEN 5000
                ELSE stepCost
            END
        """.trimIndent())
    }
}
```

Change the `@Database` annotation `version` from 2 to 3:

```kotlin
@Database(
    entities = [...],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
```

- [ ] **Step 3: Wire migration into the Room builder in AppContainer.kt**

Find the `Room.databaseBuilder(...)` call in `AppContainer.kt` and add:

```kotlin
Room.databaseBuilder(
    context.applicationContext,
    WanderingLedgerDatabase::class.java,
    "wandering-ledger.db",
)
.addMigrations(MIGRATION_2_3)
.build()
```

- [ ] **Step 4: Build the project to verify no compile errors**

Run: `./gradlew :core:database:assembleDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 5: Commit**

```bash
git add core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt
git add core/database/src/main/java/com/wanderingledger/core/database/WanderingLedgerDatabase.kt
git add app/src/main/java/com/wanderingledger/app/AppContainer.kt
git commit -m "feat: update provisional step costs to 1000/2500/5000; add Room migration 2→3"
```

---

### Task 2: Update UserStory1TravelFlowTest to match new step costs

**Files:**
- Modify: `core/data/src/test/java/com/wanderingledger/core/data/UserStory1TravelFlowTest.kt`

**Interfaces:**
- Consumes: SeedWorld step costs from Task 1 (short = 1000, medium = 2500, long = 5000)

- [ ] **Step 1: Update step counts and assertions in all three test scenarios**

Replace the hardcoded step values throughout `UserStory1TravelFlowTest.kt`:

**Scenario 1 (`simulatedStepsEnableTravelAndArrivalUpdatesPlayerState`):**
```kotlin
// Record 1200 steps (enough for a short road of 1000)
stepTrackerService.recordSensorDelta(count = 1200, source = StepSource.Simulation)

val stepsBefore = stepBankRepository.observeStepBank().first()
assertEquals(1200L, stepsBefore)

val roads = gameRepository.observeRoadsFromCurrentTown().first()
val roadToStoneford = roads.first { it.toTownId == 2L }
assertEquals(1000, roadToStoneford.stepCost)  // short road

val result = gameRepository.travel(roadToStoneford.segmentId)
assertTrue("Expected TravelResult.Arrived but got $result", result is TravelResult.Arrived)
val arrived = result as TravelResult.Arrived
assertEquals(2L, arrived.townId)
assertEquals(200L, arrived.remainingSteps)

val player = gameRepository.observePlayerState().first()
assertEquals(2L, player.currentTownId)

val stepsAfter = stepBankRepository.observeStepBank().first()
assertEquals(200L, stepsAfter)

assertEquals(1200L, player.lifetimeSteps)
```

**Scenario 2 (`insufficientSimulatedStepsBlockTravelAndPreservePlayerState`):**
```kotlin
// Record 800 steps (not enough for a short road of 1000)
stepTrackerService.recordSensorDelta(count = 800, source = StepSource.Simulation)

val roads = gameRepository.observeRoadsFromCurrentTown().first()
val roadToStoneford = roads.first { it.toTownId == 2L }

val result = gameRepository.travel(roadToStoneford.segmentId)
assertTrue("Expected TravelResult.NotEnoughSteps but got $result", result is TravelResult.NotEnoughSteps)
val notEnough = result as TravelResult.NotEnoughSteps
assertEquals(1000L, notEnough.required)
assertEquals(800L, notEnough.available)

val player = gameRepository.observePlayerState().first()
assertEquals(1L, player.currentTownId)

val stepsAfter = stepBankRepository.observeStepBank().first()
assertEquals(800L, stepsAfter)
```

**Scenario 3 (`multiHopTravelFlowUpdatesLocationAndStepBankCorrectly`):**
```kotlin
// First leg: Hearthwick → Stoneford (cost 1000)
stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)
val firstResult = gameRepository.travel(segmentId = 1L)
assertTrue(firstResult is TravelResult.Arrived)
assertEquals(2L, (firstResult as TravelResult.Arrived).townId)
assertEquals(0L, firstResult.remainingSteps)

val playerAtStoneford = gameRepository.observePlayerState().first()
assertEquals(2L, playerAtStoneford.currentTownId)
assertEquals(0L, playerAtStoneford.bankedSteps)

// Second leg: Stoneford → Mistfall (cost 2500)
stepTrackerService.recordSensorDelta(count = 2700, source = StepSource.Simulation)

val roadsFromStoneford = gameRepository.observeRoadsFromCurrentTown().first()
val roadToMistfall = roadsFromStoneford.first { it.toTownId == 3L }

val secondResult = gameRepository.travel(roadToMistfall.segmentId)
assertTrue(secondResult is TravelResult.Arrived)
assertEquals(3L, (secondResult as TravelResult.Arrived).townId)
assertEquals(200L, secondResult.remainingSteps)

val playerAtMistfall = gameRepository.observePlayerState().first()
assertEquals(3L, playerAtMistfall.currentTownId)
assertEquals(200L, playerAtMistfall.bankedSteps)
assertEquals(3700L, playerAtMistfall.lifetimeSteps) // 1000 + 2700
```

- [ ] **Step 2: Run the updated tests to verify they pass**

Run: `./gradlew :core:data:test --tests "*.UserStory1TravelFlowTest"`
Expected: 3 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add core/data/src/test/java/com/wanderingledger/core/data/UserStory1TravelFlowTest.kt
git commit -m "test: update UserStory1TravelFlowTest to provisional step costs (1000/2500/5000)"
```

---

### Task 3: Add Milestone 1 acceptance-criteria tests (issues #8, #9, #10)

**Files:**
- Create: `core/data/src/test/java/com/wanderingledger/core/data/Milestone1AcceptanceCriteriaTest.kt`

**Interfaces:**
- Consumes: `RoomStepBankRepository.observeStepBank()`, `RoomStepBankRepository.recordDetectedSteps()`, `GameRepository.travel()`, `GameRepository.observePlayerState()`
- Consumes: SeedWorld step costs (short = 1000, medium = 2500, long = 5000)

A good test here covers external behavior only: what the player observes, not internal implementation. Each test maps to a specific numbered acceptance criterion from the issue.

- [ ] **Step 1: Write the failing tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/Milestone1AcceptanceCriteriaTest.kt`:

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

/**
 * Acceptance criteria tests for Milestone 1: First Playable Loop.
 * Each test maps to a specific AC item in GitHub issues #8, #9, or #10.
 */
@RunWith(RobolectricTestRunner::class)
class Milestone1AcceptanceCriteriaTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        stepBankRepository = RoomStepBankRepository(database)
        stepTrackerService = StepTrackerService(stepBankRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Issue #8: Step banking ────────────────────────────────────────────────

    /** #8 AC: PlayerState.bankedSteps is updated in the database on each step batch. */
    @Test
    fun `AC-8-1 bankedSteps updates in DB after recording steps`() = runTest {
        stepTrackerService.recordSensorDelta(count = 500, source = StepSource.Simulation)

        val banked = stepBankRepository.observeStepBank().first()
        assertEquals(500L, banked)

        val playerState = gameRepository.observePlayerState().first()
        assertEquals(500L, playerState.bankedSteps)
    }

    /** #8 AC: Banked step count survives app restart (persisted in DB). */
    @Test
    fun `AC-8-2 bankedSteps persists across DB reads (simulating restart)`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1200, source = StepSource.Simulation)

        // Simulate restart by re-querying directly from the DB (same DB instance, fresh read)
        val freshRead = gameRepository.observePlayerState().first()
        assertEquals(1200L, freshRead.bankedSteps)
    }

    /** #8 AC: Steps increment correctly across multiple batches. */
    @Test
    fun `AC-8-3 multiple step batches accumulate correctly`() = runTest {
        stepTrackerService.recordSensorDelta(count = 300, source = StepSource.Simulation)
        stepTrackerService.recordSensorDelta(count = 700, source = StepSource.Simulation)

        val banked = stepBankRepository.observeStepBank().first()
        assertEquals(1000L, banked)
    }

    // ── Issue #9: Affordability and travel confirmation ───────────────────────

    /** #9 AC: Route is unaffordable when bankedSteps < stepCost (short road costs 1000). */
    @Test
    fun `AC-9-1 route is unaffordable when steps are insufficient`() = runTest {
        stepTrackerService.recordSensorDelta(count = 800, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        assertEquals(1000, shortRoad.stepCost)

        // JourneyRouteOption.canAfford encodes the affordability check
        val option = JourneyRouteOption(
            segmentId = shortRoad.segmentId,
            destinationName = "Stoneford",
            stepCost = shortRoad.stepCost,
            narrativeDistance = shortRoad.narrativeDistance,
            bankedSteps = 800L,
        )
        assertTrue("Route should not be affordable with 800 steps for 1000-step road", !option.canAfford)
        assertEquals(200L, option.shortfall)
    }

    /** #9 AC: Travel deducts the correct step cost from PlayerState.bankedSteps. */
    @Test
    fun `AC-9-2 travel deducts step cost from bankedSteps`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1500, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        gameRepository.travel(shortRoad.segmentId)

        val player = gameRepository.observePlayerState().first()
        assertEquals(500L, player.bankedSteps) // 1500 - 1000
    }

    /** #9 AC: TravelResult.NotEnoughSteps returned when steps are insufficient. */
    @Test
    fun `AC-9-3 travel blocked when steps insufficient`() = runTest {
        stepTrackerService.recordSensorDelta(count = 500, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        val result = gameRepository.travel(shortRoad.segmentId)

        assertTrue(result is TravelResult.NotEnoughSteps)
        val blocked = result as TravelResult.NotEnoughSteps
        assertEquals(1000L, blocked.required)
        assertEquals(500L, blocked.available)

        // Player state unchanged
        val player = gameRepository.observePlayerState().first()
        assertEquals(1L, player.currentTownId)
        assertEquals(500L, player.bankedSteps)
    }

    // ── Issue #10: Arrival persistence ───────────────────────────────────────

    /** #10 AC: PlayerState.currentTownId updates to destination after travel. */
    @Test
    fun `AC-10-1 currentTownId updates to destination after travel`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        val result = gameRepository.travel(shortRoad.segmentId)

        assertTrue(result is TravelResult.Arrived)
        val player = gameRepository.observePlayerState().first()
        assertEquals(2L, player.currentTownId)
    }

    /** #10 AC: After arrival, a fresh DB read reflects the new town (simulating restart). */
    @Test
    fun `AC-10-2 currentTownId persists to DB (no data loss on restart)`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        gameRepository.travel(roads.first { it.toTownId == 2L }.segmentId)

        // Re-read directly from DB to verify persistence (same DB, simulating cold-start read)
        val freshState = gameRepository.observePlayerState().first()
        assertEquals(2L, freshState.currentTownId)
        assertEquals(0L, freshState.bankedSteps)
    }

    /** #10 AC: No crash or data loss when travel fails (NotEnoughSteps). */
    @Test
    fun `AC-10-3 failed travel leaves DB state intact`() = runTest {
        val before = gameRepository.observePlayerState().first()

        stepTrackerService.recordSensorDelta(count = 100, source = StepSource.Simulation)
        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        gameRepository.travel(roads.first { it.toTownId == 2L }.segmentId)

        val after = gameRepository.observePlayerState().first()
        assertEquals(before.currentTownId, after.currentTownId)
        assertEquals(100L, after.bankedSteps) // steps still banked, not consumed
    }
}
```

- [ ] **Step 2: Run the tests to verify they pass**

Run: `./gradlew :core:data:test --tests "*.Milestone1AcceptanceCriteriaTest"`
Expected: 9 tests PASS.

If any fail, inspect output — most likely a step cost mismatch indicating Task 1 isn't complete.

- [ ] **Step 3: Run the full test suite to check for regressions**

Run: `./gradlew test`
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add core/data/src/test/java/com/wanderingledger/core/data/Milestone1AcceptanceCriteriaTest.kt
git commit -m "test: add Milestone 1 acceptance criteria tests for issues #8, #9, #10"
```

---

## Self-Review

**Spec coverage:**
- Issue #8 AC: "Steps increment in real time" → AC-8-1, AC-8-3 cover this at the repository level. The UI display (JourneyStepMeter) is wired in JourneyScreen and covered by existing unit tests for JourneyScreenState — no gap.
- Issue #8 AC: "Survives app restart" → AC-8-2 covers persistence via direct DB re-read.
- Issue #8 AC: "Uses 'banked steps' terminology" → satisfied by existing JourneyStepMeter implementation; not a test item.
- Issue #9 AC: "Unaffordable routes visually distinct" → JourneyRouteCard sets `enabled = route.canAfford` and uses error color — satisfied by existing UI. AC-9-1 tests the underlying `canAfford` property.
- Issue #9 AC: "Provisional step costs used" → satisfied by Task 1 + verified by AC-9-2.
- Issue #10 AC: "PlayerState.currentTownId written before TravelPolicy returns" → GameRepository.travel() uses `database.withTransaction { }`, guaranteeing atomicity. AC-10-1 verifies the outcome.
- Issue #10 AC: "No crash or data loss" → AC-10-3 covers the failure case.

**Placeholder scan:** None found.

**Type consistency:** `JourneyRouteOption` constructor in AC-9-1 matches `JourneyRouteCard.kt:50–58` definition — `segmentId: Long, destinationName: String, stepCost: Int, narrativeDistance: String, bankedSteps: Long`. Confirmed.

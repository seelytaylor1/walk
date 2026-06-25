# Architecture Deepenings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deepen five shallow modules in Wandering Ledger — sealing TravelOutcome, self-fulfilling RumorRequests, extracting MarketAnomalyDetector, consolidating companion commentary state, and decomposing MainActivity into Jetpack ViewModels.

**Architecture:** Pure business logic stays in `core:data` (TravelPolicy, EncounterEngine, MarketEngine). The `app` module holds orchestration (ViewModels, AppContainer, MainActivity as a thin navigation shell). Tasks 1–3 are in `core:data`; Tasks 4–5 span `core:data` and `app`. Each task is independently commitable.

**Tech Stack:** Kotlin, Room v2, Jetpack ViewModel + viewModelScope, Kotlin Coroutines/Flow, Robolectric (for `core:data` integration tests), JUnit 4.

## Global Constraints

- All `core:data` tests use Robolectric (`@RunWith(RobolectricTestRunner::class)`) or plain JUnit — no instrumented tests.
- Integration tests start with `TestDatabaseFactory.createInMemoryDatabase(context)` + `gameRepository.initializeNewGame(seed = 1L)`.
- Known seed data: player starts at townId=1 (Hearthwick) with 50 gold; goodId=1 = Apples, base=10, starts Abundant; segmentId=1: Hearthwick→Stoneford, stepCost=120.
- No new Gradle dependencies.
- Business logic stays in `core:data`; `app` holds orchestration only.
- `GameRepository.travel()` return type (`TravelResult`) is public API — do not change its signature.

---

## File Structure

**Created:**
- `core/data/src/main/java/com/wanderingledger/core/data/MarketAnomalyDetector.kt`
- `core/data/src/test/java/com/wanderingledger/core/data/MarketAnomalyDetectorTest.kt`
- `app/src/main/java/com/wanderingledger/app/CompanionNarrator.kt`
- `app/src/main/java/com/wanderingledger/app/MarketViewModel.kt`
- `app/src/main/java/com/wanderingledger/app/CompanionsViewModel.kt`

**Modified:**
- `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt` — Tasks 1, 2
- `core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt` — Task 1
- `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt` — Tasks 1, 2
- `core/data/src/test/java/com/wanderingledger/core/data/TravelTypesTest.kt` — Task 1
- `core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt` — Task 1
- `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt` — Task 3
- `core/data/src/main/java/com/wanderingledger/core/data/CompanionCommentary.kt` — Task 4
- `app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt` — Tasks 4, 5
- `app/src/main/java/com/wanderingledger/app/AppContainer.kt` — Tasks 4, 5
- `app/src/main/java/com/wanderingledger/app/MainActivity.kt` — Tasks 4, 5

---

## Task 1: Seal TravelOutcome

`TravelOutcome` is currently a flat data class with two redundant success signals (`result: TravelResult` and `playerDelta: PlayerDelta?`). A value of `result = Arrived` with `playerDelta = null` is valid Kotlin but logically impossible. This task converts it to a sealed interface so the type system enforces the contract.

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt`
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt`
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt`
- Modify: `core/data/src/test/java/com/wanderingledger/core/data/TravelTypesTest.kt`
- Modify: `core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt`

**Interfaces:**
- Consumes: existing `TravelPolicy.compute()`, `GameRepository.travel()`
- Produces: `TravelOutcome.Arrived` (all mutation fields non-null), `TravelOutcome.Failed(result: TravelResult)`; `GameRepository.travel()` return type (`TravelResult`) is unchanged

- [ ] **Step 1: Update the failing tests first**

Replace the two `TravelTypesTest` tests that construct `TravelOutcome` directly. Open `core/data/src/test/java/com/wanderingledger/core/data/TravelTypesTest.kt` and replace both test bodies:

```kotlin
@Test
fun travelOutcomeCoversEveryMutationFieldForASuccessfulTravel() {
    val outcome = TravelOutcome.Arrived(
        playerDelta = PlayerDelta(newTownId = 2, stepsSpent = 120, arrivedAt = 1_000L),
        markDestinationVisited = true,
        decrementActiveRumors = true,
        rumorRequests = listOf(
            RumorRequest.RoadEvent(segmentId = 1, seed = 42),
            RumorRequest.TownVisit(townId = 2, seed = 43),
        ),
        encounterOutcome = EncounterOutcome(
            encounterId = "merchant-cart",
            resultText = "They thanked you with coin.",
            goldChange = 15,
        ),
        eventLogs = listOf(
            EventLogDraft(
                type = "arrival",
                meta = "{\"segmentId\":1,\"toTownId\":2}",
                result = "Arrived after spending 120 steps.",
                createdAt = 1_000L,
            ),
        ),
    )

    assertTrue(outcome is TravelOutcome.Arrived)
    assertEquals(120L, outcome.playerDelta.stepsSpent)
    assertTrue(outcome.markDestinationVisited)
    assertTrue(outcome.decrementActiveRumors)
    assertEquals(2, outcome.rumorRequests.size)
    assertEquals(15L, outcome.encounterOutcome!!.goldChange)
    assertEquals("arrival", outcome.eventLogs.single().type)
}

@Test
fun travelOutcomeForFailureCarriesNoMutations() {
    val outcome = TravelOutcome.Failed(
        result = TravelResult.NotEnoughSteps(required = 120, available = 50),
    )

    assertTrue(outcome is TravelOutcome.Failed)
    assertTrue(outcome.result is TravelResult.NotEnoughSteps)
}
```

- [ ] **Step 2: Update TravelPolicyTest to expect the sealed type**

Open `core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt` and replace all test bodies. The tests check `outcome.result is TravelResult.X` — those become `outcome is TravelOutcome.X`. Replace each:

```kotlin
@Test
fun insufficientStepsReturnsFailureOutcomeWithNoMutations() {
    val outcome = TravelPolicy.compute(snapshot(bankedSteps = 50), seed = 1L)

    assertTrue(outcome is TravelOutcome.Failed)
    val failed = outcome as TravelOutcome.Failed
    assertTrue(failed.result is TravelResult.NotEnoughSteps)
    failed.result as TravelResult.NotEnoughSteps
    assertEquals(120L, failed.result.required)
    assertEquals(50L, failed.result.available)
}

@Test
fun validTravelProducesCorrectPlayerDelta() {
    val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200), seed = 1L)

    assertTrue(outcome is TravelOutcome.Arrived)
    val arrived = outcome as TravelOutcome.Arrived
    assertEquals(2L, arrived.playerDelta.newTownId)
    assertEquals(120L, arrived.playerDelta.stepsSpent)
    assertEquals(1_000L, arrived.playerDelta.arrivedAt)
    assertTrue(arrived.markDestinationVisited)
    assertTrue(arrived.decrementActiveRumors)
}

@Test
fun validTravelRequestsRoadAndTownRumorGeneration() {
    val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200), seed = 7L)

    assertTrue(outcome is TravelOutcome.Arrived)
    val arrived = outcome as TravelOutcome.Arrived
    assertEquals(2, arrived.rumorRequests.size)
    assertTrue(arrived.rumorRequests[0] is RumorRequest.RoadEvent)
    assertTrue(arrived.rumorRequests[1] is RumorRequest.TownVisit)
    assertEquals(7L + 1L, arrived.rumorRequests[0].seed)
    assertEquals(7L, arrived.rumorRequests[1].seed)
}

@Test
fun roadWithEventPoolResolvesAnEncounter() {
    val outcome = TravelPolicy.compute(
        snapshot(bankedSteps = 200, eventPool = "[\"merchant-cart\"]"),
        seed = 1L,
    )

    assertTrue(outcome is TravelOutcome.Arrived)
    val arrived = outcome as TravelOutcome.Arrived
    assertNotNull("Encounter should be resolved when the pool is non-empty", arrived.encounterOutcome)
    assertEquals("merchant-cart", arrived.encounterOutcome!!.encounterId)
    assertEquals(2, arrived.eventLogs.size)
    assertEquals("encounter", arrived.eventLogs[0].type)
    assertEquals("arrival", arrived.eventLogs[1].type)
}

@Test
fun roadWithoutEventPoolHasNoEncounter() {
    val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200, eventPool = "[]"), seed = 1L)

    assertTrue(outcome is TravelOutcome.Arrived)
    val arrived = outcome as TravelOutcome.Arrived
    assertNull(arrived.encounterOutcome)
    assertEquals(1, arrived.eventLogs.size)
    assertEquals("arrival", arrived.eventLogs.single().type)
}

@Test
fun encounterResolutionIsDeterministicForAGivenSeed() {
    val party = listOf(Companion(1, "Rogue", CompanionRole.Rogue, 2, 0, "active", 1, true))
    val first = TravelPolicy.compute(
        snapshot(bankedSteps = 200, eventPool = "[\"merchant-cart\"]", companions = party),
        seed = 99L,
    )
    val second = TravelPolicy.compute(
        snapshot(bankedSteps = 200, eventPool = "[\"merchant-cart\"]", companions = party),
        seed = 99L,
    )

    assertTrue(first is TravelOutcome.Arrived && second is TravelOutcome.Arrived)
    assertEquals((first as TravelOutcome.Arrived).encounterOutcome,
                 (second as TravelOutcome.Arrived).encounterOutcome)
}
```

- [ ] **Step 3: Run the tests — expect compile errors (class TravelOutcome.Arrived not found)**

```
./gradlew :core:data:test
```

Expected: compilation fails with "Unresolved reference: Arrived" or similar — the sealed type does not exist yet.

- [ ] **Step 4: Replace TravelOutcome in TravelTypes.kt**

Open `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt`. Delete the entire `TravelOutcome` data class (lines 95–103) and replace it with:

```kotlin
/**
 * The result of [TravelPolicy.compute].
 *
 * [Arrived] guarantees every mutation field is valid and non-null.
 * [Failed] carries the failure reason; no mutations apply.
 *
 * Callers pattern-match once with Kotlin's exhaustive `when` — no null-checks needed.
 */
sealed interface TravelOutcome {
    data class Arrived(
        val playerDelta: PlayerDelta,
        val markDestinationVisited: Boolean = false,
        val decrementActiveRumors: Boolean = false,
        val rumorRequests: List<RumorRequest> = emptyList(),
        val encounterOutcome: EncounterOutcome? = null,
        val eventLogs: List<EventLogDraft> = emptyList(),
    ) : TravelOutcome

    data class Failed(val result: TravelResult) : TravelOutcome
}
```

- [ ] **Step 5: Update TravelPolicy.compute() to return the sealed type**

Open `core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt`. Change the return type of `compute()` from `TravelOutcome` (same, no change) but update the two return sites:

**Failure path** (currently lines 36–42) — replace:
```kotlin
if (player.bankedSteps < stepCost) {
    return TravelOutcome(
        result = TravelResult.NotEnoughSteps(
            required = stepCost,
            available = player.bankedSteps,
        ),
    )
}
```
with:
```kotlin
if (player.bankedSteps < stepCost) {
    return TravelOutcome.Failed(
        TravelResult.NotEnoughSteps(required = stepCost, available = player.bankedSteps),
    )
}
```

**Success path** (currently lines 86–106) — replace:
```kotlin
return TravelOutcome(
    result = TravelResult.Arrived(townId = road.toTownId, remainingSteps = remainingSteps),
    playerDelta = PlayerDelta(
        newTownId = road.toTownId,
        stepsSpent = stepCost,
        arrivedAt = arrivedAt,
    ),
    markDestinationVisited = true,
    decrementActiveRumors = true,
    rumorRequests = listOf(
        RumorRequest.RoadEvent(segmentId = road.segmentId, seed = seed + road.segmentId),
        RumorRequest.TownVisit(townId = road.toTownId, seed = seed),
    ),
    encounterOutcome = encounter,
    eventLogs = eventLogs,
)
```
with:
```kotlin
return TravelOutcome.Arrived(
    playerDelta = PlayerDelta(
        newTownId = road.toTownId,
        stepsSpent = stepCost,
        arrivedAt = arrivedAt,
    ),
    markDestinationVisited = true,
    decrementActiveRumors = true,
    rumorRequests = listOf(
        RumorRequest.RoadEvent(segmentId = road.segmentId, seed = seed + road.segmentId),
        RumorRequest.TownVisit(townId = road.toTownId, seed = seed),
    ),
    encounterOutcome = encounter,
    eventLogs = eventLogs,
)
```

Also remove the `val remainingSteps = ...` line — it is now computed in the repository.

- [ ] **Step 6: Update GameRepository.travel() to pattern-match on the sealed type**

Open `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt`. The `travel()` method currently does:

```kotlin
val outcome = TravelPolicy.compute(snapshot, seed)

val delta = outcome.playerDelta
    ?: run {
        recordTravelCompleted(startedAt, segmentId, success = false)
        return@withTransaction outcome.result
    }

// --- Write: apply the outcome's mutations ---
val goldChange = outcome.encounterOutcome?.goldChange ?: 0L
```

Replace from `val outcome = ...` through to `outcome.result` (the final return at line 259) with:

```kotlin
val outcome = TravelPolicy.compute(snapshot, seed)

when (outcome) {
    is TravelOutcome.Failed -> {
        recordTravelCompleted(startedAt, segmentId, success = false)
        return@withTransaction outcome.result
    }
    is TravelOutcome.Arrived -> {
        val delta = outcome.playerDelta

        // --- Write: apply the outcome's mutations ---
        val goldChange = outcome.encounterOutcome?.goldChange ?: 0L
        database.playerDao().updatePlayer(
            player.copy(
                currentTownId = delta.newTownId,
                bankedSteps = player.bankedSteps - delta.stepsSpent,
                gold = (player.gold + goldChange).coerceAtLeast(0),
                lastSyncAt = delta.arrivedAt,
            ),
        )

        if (outcome.markDestinationVisited) {
            database.townDao().getTownSnapshot(delta.newTownId)?.let { dest ->
                database.townDao().updateTown(
                    dest.copy(storyState = "visited", lastVisitedAt = delta.arrivedAt),
                )
            }
        }

        if (outcome.decrementActiveRumors) {
            database.rumorDao().decrementAllActive()
        }

        outcome.rumorRequests.forEach { request ->
            when (request) {
                is RumorRequest.RoadEvent ->
                    rumorRepository.generateRumorFromRoadEvent(request.segmentId, request.seed)
                is RumorRequest.TownVisit ->
                    rumorRepository.generateRumorForTownVisit(request.townId, request.seed)
            }
        }

        outcome.encounterOutcome?.let { encounter ->
            if (encounter.bondChange != 0) {
                snapshot.activeCompanions.forEach { companion ->
                    companionRepository.updateBond(companion.companionId, encounter.bondChange)
                }
            }
        }

        outcome.eventLogs.forEach { log ->
            database.eventLogDao().insertEvent(
                EventLogEntity(
                    type = log.type,
                    meta = log.meta,
                    result = log.result,
                    createdAt = log.createdAt,
                ),
            )
        }

        val remainingSteps = player.bankedSteps - delta.stepsSpent
        recordTravelCompleted(startedAt, segmentId, success = true)
        return@withTransaction TravelResult.Arrived(
            townId = delta.newTownId,
            remainingSteps = remainingSteps,
        )
    }
}
```

- [ ] **Step 7: Run all core:data tests**

```
./gradlew :core:data:test
```

Expected: all tests pass. If any test still references `outcome.result` or `outcome.playerDelta!!` as flat fields on TravelOutcome, update them to pattern-match on the sealed type.

- [ ] **Step 8: Commit**

```
git add core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt
git add core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt
git add core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt
git add core/data/src/test/java/com/wanderingledger/core/data/TravelTypesTest.kt
git add core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt
git commit -m "refactor: seal TravelOutcome so impossible states are unrepresentable"
```

---

## Task 2: RumorRequest Self-Fulfillment

`GameRepository.travel()` currently `when`-switches on `RumorRequest` subtypes to dispatch to different `RumorRepository` methods. Adding a new rumor type requires changes in three files. This task moves fulfillment into the sealed type itself.

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt`
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt`

**Interfaces:**
- Consumes: `TravelOutcome.Arrived` (from Task 1), `RumorRepository.generateRumorForTownVisit()`, `RumorRepository.generateRumorFromRoadEvent()`
- Produces: `RumorRequest.fulfill(repo: RumorRepository)` — each subtype calls its own repo method

- [ ] **Step 1: Add `fulfill()` to the RumorRequest sealed interface**

Open `TravelTypes.kt`. Add a `suspend fun fulfill(repo: RumorRepository)` abstract method to `RumorRequest`, and implement it in each subtype. `RumorRepository` is in the same package — no import needed. The full updated sealed interface:

```kotlin
sealed interface RumorRequest {
    val seed: Long

    /** Called by the write phase to fulfil this request via the rumor repository. */
    suspend fun fulfill(repo: RumorRepository)

    data class TownVisit(
        val townId: Long,
        override val seed: Long,
    ) : RumorRequest {
        override suspend fun fulfill(repo: RumorRepository) =
            repo.generateRumorForTownVisit(townId, seed)
    }

    data class RoadEvent(
        val segmentId: Long,
        override val seed: Long,
    ) : RumorRequest {
        override suspend fun fulfill(repo: RumorRepository) =
            repo.generateRumorFromRoadEvent(segmentId, seed)
    }
}
```

- [ ] **Step 2: Simplify the write phase in GameRepository.travel()**

Open `GameRepositories.kt`. In the `is TravelOutcome.Arrived ->` branch (from Task 1), find:

```kotlin
outcome.rumorRequests.forEach { request ->
    when (request) {
        is RumorRequest.RoadEvent ->
            rumorRepository.generateRumorFromRoadEvent(request.segmentId, request.seed)
        is RumorRequest.TownVisit ->
            rumorRepository.generateRumorForTownVisit(request.townId, request.seed)
    }
}
```

Replace with:

```kotlin
outcome.rumorRequests.forEach { it.fulfill(rumorRepository) }
```

- [ ] **Step 3: Run all core:data tests**

```
./gradlew :core:data:test
```

Expected: all tests pass. The `GameRepositoryTest` exercises the travel path including rumor generation — if those pass, fulfillment is wired correctly.

- [ ] **Step 4: Commit**

```
git add core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt
git add core/data/src/main/java/com/wanderingledger/core/data/GameRepositories.kt
git commit -m "refactor: give RumorRequest subtypes self-fulfillment, simplify travel write phase"
```

---

## Task 3: Extract MarketAnomalyDetector

Anomaly detection rules (~80 lines) are interleaved in `MarketRepository.postTradeUpdate()`. To test "what triggers a spike?" you must fake a full Room transaction. This task extracts a pure function with its own test file.

**Files:**
- Create: `core/data/src/main/java/com/wanderingledger/core/data/MarketAnomalyDetector.kt`
- Create: `core/data/src/test/java/com/wanderingledger/core/data/MarketAnomalyDetectorTest.kt`
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt`

**Interfaces:**
- Consumes: `SupplyLevel`, `TelemetryEvent.MarketAnomaly`, `MarketAnomalyType` (already imported in `MarketRepository.kt`)
- Produces: `PriceSnapshot(sellPrice: Long, supplyLevel: SupplyLevel)` — caller builds two snapshots; `MarketAnomalyDetector.detect(...): List<TelemetryEvent.MarketAnomaly>` — pure, no side effects

- [ ] **Step 1: Write the failing tests**

Create `core/data/src/test/java/com/wanderingledger/core/data/MarketAnomalyDetectorTest.kt`:

```kotlin
package com.wanderingledger.core.data

import com.wanderingledger.core.model.SupplyLevel
import com.wanderingledger.core.telemetry.MarketAnomalyType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure unit tests — no Room, no Robolectric. */
class MarketAnomalyDetectorTest {

    private fun detect(
        baseSell: Long = 100L,
        newSell: Long = 100L,
        baseValue: Long = 100L,
        oldSupply: SupplyLevel = SupplyLevel.Normal,
        newSupply: SupplyLevel = SupplyLevel.Normal,
    ) = MarketAnomalyDetector.detect(
        townId = 1L,
        goodId = 1L,
        baseValue = baseValue,
        before = PriceSnapshot(sellPrice = baseSell, supplyLevel = oldSupply),
        after = PriceSnapshot(sellPrice = newSell, supplyLevel = newSupply),
        now = 0L,
    )

    @Test
    fun detectsPriceSpikeWhenPriceIncreasesMoreThan50Percent() {
        val events = detect(baseSell = 100L, newSell = 160L)
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.PriceSpike })
        assertFalse(events.any { it.anomalyType == MarketAnomalyType.PriceCrash })
    }

    @Test
    fun noPriceSpikeWhenIncreaseIsExactly50Percent() {
        val events = detect(baseSell = 100L, newSell = 150L)
        assertFalse(events.any { it.anomalyType == MarketAnomalyType.PriceSpike })
    }

    @Test
    fun detectsPriceCrashWhenPriceDecreasesMoreThan30Percent() {
        val events = detect(baseSell = 100L, newSell = 60L)
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.PriceCrash })
        assertFalse(events.any { it.anomalyType == MarketAnomalyType.PriceSpike })
    }

    @Test
    fun noPriceCrashWhenDecreaseIsExactly30Percent() {
        val events = detect(baseSell = 100L, newSell = 70L)
        assertFalse(events.any { it.anomalyType == MarketAnomalyType.PriceCrash })
    }

    @Test
    fun detectsSupplyDepletionWhenTransitioningToScarce() {
        val events = detect(oldSupply = SupplyLevel.Normal, newSupply = SupplyLevel.Scarce)
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.SupplyDepleted })
    }

    @Test
    fun noDepletionWhenAlreadyScarce() {
        val events = detect(oldSupply = SupplyLevel.Scarce, newSupply = SupplyLevel.Scarce)
        assertFalse(events.any { it.anomalyType == MarketAnomalyType.SupplyDepleted })
    }

    @Test
    fun detectsUnusualVolumeWhenPriceExceedsBaseByMoreThan100Percent() {
        val events = detect(newSell = 210L, baseValue = 100L)
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.UnusualVolume })
    }

    @Test
    fun noUnusualVolumeWhenPriceIsExactlyDoubleBase() {
        val events = detect(newSell = 200L, baseValue = 100L)
        assertFalse(events.any { it.anomalyType == MarketAnomalyType.UnusualVolume })
    }

    @Test
    fun returnsEmptyListForQuietTrade() {
        val events = detect(baseSell = 100L, newSell = 105L)
        assertTrue(events.isEmpty())
    }

    @Test
    fun multipleAnomaliesReturnedWhenMultipleThresholdsBroken() {
        // Price spikes AND moves to Scarce in one trade
        val events = detect(
            baseSell = 100L, newSell = 160L,
            oldSupply = SupplyLevel.Normal, newSupply = SupplyLevel.Scarce,
        )
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.PriceSpike })
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.SupplyDepleted })
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```
./gradlew :core:data:test
```

Expected: compilation fails with "Unresolved reference: MarketAnomalyDetector" and "Unresolved reference: PriceSnapshot".

- [ ] **Step 3: Create MarketAnomalyDetector.kt**

Create `core/data/src/main/java/com/wanderingledger/core/data/MarketAnomalyDetector.kt`:

```kotlin
package com.wanderingledger.core.data

import com.wanderingledger.core.model.SupplyLevel
import com.wanderingledger.core.telemetry.MarketAnomalyType
import com.wanderingledger.core.telemetry.TelemetryEvent

/** A point-in-time price reading used as input to [MarketAnomalyDetector.detect]. */
data class PriceSnapshot(
    val sellPrice: Long,
    val supplyLevel: SupplyLevel,
)

/**
 * Pure anomaly detection for market price changes.
 *
 * Takes two [PriceSnapshot]s and returns the anomalies that occurred between
 * them, as [TelemetryEvent.MarketAnomaly] values ready to emit. Has no side
 * effects — callers (e.g. [MarketRepository]) emit via [TelemetryService].
 */
object MarketAnomalyDetector {
    fun detect(
        townId: Long,
        goodId: Long,
        baseValue: Long,
        before: PriceSnapshot,
        after: PriceSnapshot,
        now: Long = System.currentTimeMillis(),
    ): List<TelemetryEvent.MarketAnomaly> {
        val events = mutableListOf<TelemetryEvent.MarketAnomaly>()

        val priceChangePct = if (before.sellPrice > 0) {
            ((after.sellPrice - before.sellPrice).toDouble() / before.sellPrice * 100).toLong()
        } else 0L

        if (priceChangePct > 50) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.PriceSpike,
                townId = townId,
                goodId = goodId.toString(),
                value = after.sellPrice,
                threshold = (before.sellPrice * 1.5).toLong(),
            )
        }

        if (priceChangePct < -30) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.PriceCrash,
                townId = townId,
                goodId = goodId.toString(),
                value = after.sellPrice,
                threshold = (before.sellPrice * 0.7).toLong(),
            )
        }

        if (before.supplyLevel != SupplyLevel.Scarce && after.supplyLevel == SupplyLevel.Scarce) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.SupplyDepleted,
                townId = townId,
                goodId = goodId.toString(),
                value = after.supplyLevel.ordinal.toLong(),
                threshold = SupplyLevel.Scarce.ordinal.toLong(),
            )
        }

        val deviationPct = if (baseValue > 0) {
            ((after.sellPrice - baseValue).toDouble() / baseValue * 100).toLong()
        } else 0L
        if (deviationPct > 100) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.UnusualVolume,
                townId = townId,
                goodId = goodId.toString(),
                value = after.sellPrice,
                threshold = baseValue * 2,
            )
        }

        return events
    }
}
```

- [ ] **Step 4: Run the new unit tests**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.MarketAnomalyDetectorTest"
```

Expected: all 10 tests pass.

- [ ] **Step 5: Replace detectMarketAnomalies() in MarketRepository.kt**

Open `MarketRepository.kt`. In `postTradeUpdate()`, the current call is:

```kotlin
detectMarketAnomalies(townId, goodId, good.baseValue, priceEntity.sellPrice, newSellPrice, currentSupply, newSupply)
```

Replace that line with:

```kotlin
val before = PriceSnapshot(sellPrice = priceEntity.sellPrice, supplyLevel = currentSupply)
val after = PriceSnapshot(sellPrice = newSellPrice, supplyLevel = newSupply)
MarketAnomalyDetector.detect(townId, goodId, good.baseValue, before, after, now).forEach {
    TelemetryService.tryRecord(it)
}
```

Then delete the entire `private fun detectMarketAnomalies(...)` method (lines 415–493).

- [ ] **Step 6: Run all core:data tests**

```
./gradlew :core:data:test
```

Expected: all tests pass including existing `MarketRepositoryTest`.

- [ ] **Step 7: Commit**

```
git add core/data/src/main/java/com/wanderingledger/core/data/MarketAnomalyDetector.kt
git add core/data/src/test/java/com/wanderingledger/core/data/MarketAnomalyDetectorTest.kt
git add core/data/src/main/java/com/wanderingledger/core/data/MarketRepository.kt
git commit -m "refactor: extract MarketAnomalyDetector as pure function, unit-testable without Room"
```

---

## Task 4: CompanionNarrator

Companion commentary state is split: cooldown lives in `CompanionCommentaryEngine` (held by `JourneyViewModel`), the latest spoken line lives in a nullable `latestCompanionCommentary` field in `MainActivity`, and the bridge is an extension function on `CompanionRepository`. A screen rotation loses the latest line. This task consolidates both into a single `CompanionNarrator` that exposes a `StateFlow`.

**Files:**
- Create: `app/src/main/java/com/wanderingledger/app/CompanionNarrator.kt`
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/CompanionCommentary.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/AppContainer.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/MainActivity.kt`

**Interfaces:**
- Consumes: `CompanionRepository`, `CompanionCommentaryEngine`, `CompanionCommentaryContext`, `CompanionCommentaryResult`, `CompanionCommentaryUi`, `CompanionCommentary.toUi()`
- Produces: `CompanionNarrator.latestLine: StateFlow<CompanionCommentaryUi?>`, `CompanionNarrator.requestLine(...): CompanionCommentaryResult`

- [ ] **Step 1: Create CompanionNarrator.kt**

Create `app/src/main/java/com/wanderingledger/app/CompanionNarrator.kt`:

```kotlin
package com.wanderingledger.app

import com.wanderingledger.core.data.CompanionCommentaryContext
import com.wanderingledger.core.data.CompanionCommentaryEngine
import com.wanderingledger.core.data.CompanionCommentaryResult
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.model.Biome
import com.wanderingledger.feature.companions.CompanionCommentaryUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Single source of truth for companion commentary state.
 *
 * Owns the [CompanionCommentaryEngine] (cooldown timer) and the latest
 * spoken line as a [StateFlow]. Both JourneyViewModel and CompanionsViewModel
 * share the same narrator instance via [AppContainer], so commentary from
 * travel and from town interaction appear in the same flow.
 */
class CompanionNarrator(
    private val companionRepository: CompanionRepository,
    private val engine: CompanionCommentaryEngine,
) {
    private val _latestLine = MutableStateFlow<CompanionCommentaryUi?>(null)
    val latestLine: StateFlow<CompanionCommentaryUi?> = _latestLine.asStateFlow()

    suspend fun requestLine(
        companionId: Long,
        context: CompanionCommentaryContext,
        biome: Biome? = null,
        bankedSteps: Long? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): CompanionCommentaryResult {
        val companion = companionRepository.observeActiveCompanions()
            .firstOrNull()
            ?.firstOrNull { it.companionId == companionId }
            ?: return CompanionCommentaryResult.NotActive

        val result = engine.selectLine(companion, context, biome, bankedSteps, nowMs)
        if (result is CompanionCommentaryResult.Spoken) {
            _latestLine.value = result.commentary.toUi()
        }
        return result
    }
}
```

- [ ] **Step 2: Add companionNarrator to AppContainer and remove companionCommentaryEngine**

Open `AppContainer.kt`. Replace:
```kotlin
val companionCommentaryEngine = CompanionCommentaryEngine()
```
with:
```kotlin
val companionNarrator = CompanionNarrator(companionRepository, CompanionCommentaryEngine())
```

- [ ] **Step 3: Update JourneyViewModel to use the narrator**

Open `JourneyViewModel.kt`. Make these changes:

**Constructor:** Replace `private val companionCommentaryEngine: CompanionCommentaryEngine` with `private val narrator: CompanionNarrator`.

**Add StateFlow exposure:**
```kotlin
/** The latest companion line — observed by the companions screen. */
val latestCommentary: StateFlow<CompanionCommentaryUi?> = narrator.latestLine
```

**Update `firstCompanionCommentaryMessage()`:** Replace the body with:
```kotlin
private suspend fun firstCompanionCommentaryMessage(
    context: CompanionCommentaryContext,
    biome: Biome?,
    bankedSteps: Long?,
): String? {
    val companion = withContext(Dispatchers.IO) {
        companionRepository.observeActiveCompanions().first().firstOrNull()
    } ?: return null
    return when (val result = withContext(Dispatchers.IO) {
        narrator.requestLine(
            companionId = companion.companionId,
            context = context,
            biome = biome,
            bankedSteps = bankedSteps,
        )
    }) {
        is CompanionCommentaryResult.Spoken -> "${result.commentary.companionName}: ${result.commentary.line}"
        else -> null
    }
}
```

**Remove `CommentaryGenerated` from `JourneyEffect`:** Delete this data class from the `JourneyEffect` sealed interface:
```kotlin
// DELETE this:
data class CommentaryGenerated(
    val commentary: com.wanderingledger.feature.companions.CompanionCommentaryUi,
) : JourneyEffect
```

**Remove imports:** Remove `import com.wanderingledger.core.data.CompanionCommentaryEngine` and `import com.wanderingledger.core.data.requestCommentary`.

- [ ] **Step 4: Update the JourneyViewModel factory in MainActivity**

Open `MainActivity.kt`. Find the `journeyViewModelFactory` block (around line 222). Replace `companionCommentaryEngine = companionCommentaryEngine` with `narrator = container.companionNarrator`. Also update the field declaration — remove `private lateinit var companionCommentaryEngine: CompanionCommentaryEngine` (line 112) and add `private lateinit var companionNarrator: CompanionNarrator`. Then in `onCreate`, replace `companionCommentaryEngine = container.companionCommentaryEngine` with `companionNarrator = container.companionNarrator`.

The updated factory block:
```kotlin
val journeyViewModelFactory = viewModelFactory {
    initializer {
        JourneyViewModel(
            gameRepository = gameRepository,
            companionRepository = companionRepository,
            narrator = container.companionNarrator,
            stepTrackerService = stepTrackerService,
            accessibilityPreferences = accessibilityPreferences,
        )
    }
}
```

- [ ] **Step 5: Remove `CommentaryGenerated` handling and `latestCompanionCommentary` from MainActivity**

In `MainActivity.kt`:

**Remove the field** (line 174):
```kotlin
// DELETE:
private var latestCompanionCommentary: CompanionCommentaryUi? = null
```

**Remove the handler** in `handleJourneyEffect()`:
```kotlin
// DELETE:
is JourneyEffect.CommentaryGenerated -> {
    latestCompanionCommentary = effect.commentary
}
```

**Update `showCompanionsView()`:** Replace `recentCommentary = latestCompanionCommentary` with `recentCommentary = journeyViewModel.latestCommentary.value` in both the initial render call and the observation collect lambda.

**Update `buildCompanionsActions()` onInteract:** The `requestCommentary` extension function is being deleted. Replace lines 670–682 (the `requestCommentary` call and `latestCompanionCommentary = ...` assignment):
```kotlin
// Replace this block:
val result = withContext(Dispatchers.IO) {
    companionRepository.requestCommentary(
        companionId = companionId,
        context = if (player.bankedSteps < 80L) CompanionCommentaryContext.LowSteps
                  else CompanionCommentaryContext.Town,
        engine = companionCommentaryEngine,
        biome = town?.biome,
        bankedSteps = player.bankedSteps,
    )
}
val message = when (result) {
    is CompanionCommentaryResult.Spoken -> {
        latestCompanionCommentary = result.commentary.toUi()
        ...
    }
    ...
}

// With:
val result = withContext(Dispatchers.IO) {
    companionNarrator.requestLine(
        companionId = companionId,
        context = if (player.bankedSteps < 80L) CompanionCommentaryContext.LowSteps
                  else CompanionCommentaryContext.Town,
        biome = town?.biome,
        bankedSteps = player.bankedSteps,
    )
}
val message = when (result) {
    is CompanionCommentaryResult.Spoken -> {
        // narrator.latestLine already updated — no assignment needed
        withContext(Dispatchers.IO) {
            companionRepository.updateBond(companionId, 1)
        }
        audioManager.play(AudioEvent.BondIncrease)
        hapticManager.perform(HapticEffect.REWARD)
        null
    }
    is CompanionCommentaryResult.OnCooldown ->
        "${result.companionName} is still considering the last thing they said."
    CompanionCommentaryResult.NotActive ->
        "Only active companions can answer from the road."
}
```

Also add `companionNarrator` field reference and update `renderCompanionsView()` similarly.

- [ ] **Step 6: Delete the requestCommentary extension function**

Open `CompanionCommentary.kt`. Delete lines 78–99 (the entire `suspend fun CompanionRepository.requestCommentary(...)` extension function). Remove the now-unused import `import kotlinx.coroutines.flow.firstOrNull` if it's no longer needed elsewhere in that file.

- [ ] **Step 7: Build and verify**

```
./gradlew :app:assembleDebug
```

Expected: clean build. Fix any remaining import errors.

- [ ] **Step 8: Run all tests**

```
./gradlew :core:data:test :app:test
```

Expected: all tests pass.

- [ ] **Step 9: Commit**

```
git add app/src/main/java/com/wanderingledger/app/CompanionNarrator.kt
git add core/data/src/main/java/com/wanderingledger/core/data/CompanionCommentary.kt
git add app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt
git add app/src/main/java/com/wanderingledger/app/AppContainer.kt
git add app/src/main/java/com/wanderingledger/app/MainActivity.kt
git commit -m "refactor: consolidate companion commentary into CompanionNarrator with StateFlow"
```

---

## Task 5: Extract MarketViewModel and CompanionsViewModel

`MainActivity` is 1160 LOC. The market and companions screens drive the most logic: observation Jobs, action callbacks, message state, audio/haptic effects. This task extracts a `MarketViewModel` and a `CompanionsViewModel` using Jetpack ViewModel + `viewModelScope` so lifecycles are managed automatically.

**Context:** After this task, `marketObserveJob` and `companionsObserveJob` are deleted from `MainActivity`. The remaining screens (ledger, chronicle, inventory, settings) still use the manual Job pattern — extending the ViewModel pattern to them follows the same recipe.

**Files:**
- Create: `app/src/main/java/com/wanderingledger/app/MarketViewModel.kt`
- Create: `app/src/main/java/com/wanderingledger/app/CompanionsViewModel.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/AppContainer.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/MainActivity.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt`

**Interfaces:**
- Consumes: `MarketRepository`, `CompanionRepository`, `GameRepository`, `CompanionNarrator` (from Task 4), `AccessibilityPreferences`
- Produces:
  - `MarketViewModel.state: StateFlow<MarketState?>`, `MarketViewModel.message: StateFlow<String?>`, `MarketViewModel.effects: SharedFlow<MarketEffect>`
  - `CompanionsViewModel.state: StateFlow<CompanionsViewState?>`, `CompanionsViewModel.effects: SharedFlow<CompanionsEffect>`, `CompanionsViewModel.latestCommentary: StateFlow<CompanionCommentaryUi?>`

- [ ] **Step 1: Create MarketViewModel.kt**

Create `app/src/main/java/com/wanderingledger/app/MarketViewModel.kt`:

```kotlin
package com.wanderingledger.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderingledger.core.data.BuyResult
import com.wanderingledger.core.data.MarketRepository
import com.wanderingledger.core.data.MarketState
import com.wanderingledger.core.data.SellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MarketEffect {
    data object BuySuccess : MarketEffect
    data object SellSuccess : MarketEffect
    data object TransactionError : MarketEffect
}

class MarketViewModel(
    private val marketRepository: MarketRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<MarketState?>(null)
    val state: StateFlow<MarketState?> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _effects = MutableSharedFlow<MarketEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<MarketEffect> = _effects.asSharedFlow()

    private var observeJob: Job? = null

    /** Start observing market state for [townId]. Call when the market screen becomes active. */
    fun activate(townId: Long) {
        observeJob?.cancel()
        _message.value = null
        observeJob = viewModelScope.launch {
            marketRepository.observeMarket(townId).collect { _state.value = it }
        }
    }

    /** Stop observing. Call when leaving the market screen. */
    fun deactivate() {
        observeJob?.cancel()
        observeJob = null
    }

    fun buy(townId: Long, goodId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                marketRepository.buyGood(townId, goodId, quantity = 1)
            }
            _message.value = result.toBuyMessage()
            when {
                result is BuyResult.Success -> _effects.emit(MarketEffect.BuySuccess)
                result is BuyResult.NotEnoughGold || result == BuyResult.InventoryFull ->
                    _effects.emit(MarketEffect.TransactionError)
            }
        }
    }

    fun sell(townId: Long, goodId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                marketRepository.sellGood(townId, goodId, quantity = 1)
            }
            _message.value = result.toSellMessage()
            when {
                result is SellResult.Success -> _effects.emit(MarketEffect.SellSuccess)
                result is SellResult.NotEnoughInventory ->
                    _effects.emit(MarketEffect.TransactionError)
            }
        }
    }

    private fun BuyResult.toBuyMessage(): String? = when (this) {
        is BuyResult.Success -> "Bought ${quantity}x for ${goldSpent}g. Gold remaining: ${remainingGold}g."
        is BuyResult.NotEnoughGold -> "Not enough gold. Need ${required}g, have ${available}g."
        BuyResult.InventoryFull -> "Inventory is full."
        BuyResult.GoodNotAvailable -> "That good is not available here."
        BuyResult.InvalidQuantity -> null
    }

    private fun SellResult.toSellMessage(): String? = when (this) {
        is SellResult.Success -> "Sold ${quantity}x for ${goldEarned}g. Gold: ${remainingGold}g."
        is SellResult.NotEnoughInventory -> "You only have $available of that good."
        SellResult.GoodNotAvailable -> "That good is not available here."
        SellResult.InvalidQuantity -> null
    }
}
```

- [ ] **Step 2: Create CompanionsViewModel.kt**

Create `app/src/main/java/com/wanderingledger/app/CompanionsViewModel.kt`:

```kotlin
package com.wanderingledger.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderingledger.core.data.CompanionCommentaryContext
import com.wanderingledger.core.data.CompanionCommentaryResult
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.RecruitmentResult
import com.wanderingledger.core.designsystem.accessibility.AccessibilityPreferences
import com.wanderingledger.core.model.Companion
import com.wanderingledger.feature.companions.CompanionCommentaryUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompanionsViewState(
    val active: List<Companion>,
    val recruitable: List<Companion>,
    val reduceMotion: Boolean,
    val message: String?,
    val latestCommentary: CompanionCommentaryUi?,
)

sealed interface CompanionsEffect {
    data object InteractSuccess : CompanionsEffect
    data object CooldownActive : CompanionsEffect
}

class CompanionsViewModel(
    private val companionRepository: CompanionRepository,
    private val gameRepository: GameRepository,
    private val narrator: CompanionNarrator,
    private val accessibilityPreferences: AccessibilityPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<CompanionsViewState?>(null)
    val state: StateFlow<CompanionsViewState?> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CompanionsEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<CompanionsEffect> = _effects.asSharedFlow()

    /** The narrator's StateFlow flows directly — all companion commentary in one place. */
    val latestCommentary: StateFlow<CompanionCommentaryUi?> = narrator.latestLine

    private var observeJob: Job? = null

    fun activate(townId: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                companionRepository.observeActiveCompanions(),
                companionRepository.observeRecruitableCompanionsAtTown(townId),
                accessibilityPreferences.reduceMotion,
                narrator.latestLine,
            ) { active, recruitable, reduceMotion, commentary ->
                CompanionsViewState(
                    active = active,
                    recruitable = recruitable,
                    reduceMotion = reduceMotion,
                    message = _state.value?.message,
                    latestCommentary = commentary,
                )
            }.collect { _state.value = it }
        }
    }

    fun deactivate() {
        observeJob?.cancel()
        observeJob = null
    }

    fun recruit(companionId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                companionRepository.recruitCompanion(companionId)
            }
            val message = when (result) {
                RecruitmentResult.Success -> "A new voice joins the road."
                RecruitmentResult.AlreadyActive -> "They are already traveling with you."
                RecruitmentResult.PartyFull -> "The party is full."
                RecruitmentResult.NotFound -> "That companion is not available here."
                RecruitmentResult.NotEnoughTrades -> "Complete a few more trades first."
            }
            _state.value = _state.value?.copy(message = message)
        }
    }

    fun interact(companionId: Long, townId: Long) {
        viewModelScope.launch {
            val player = withContext(Dispatchers.IO) {
                gameRepository.observePlayerState().first()
            }
            val town = withContext(Dispatchers.IO) {
                gameRepository.observeTown(townId).first()
            }
            val context = if (player.bankedSteps < 80L)
                CompanionCommentaryContext.LowSteps
            else
                CompanionCommentaryContext.Town

            val result = withContext(Dispatchers.IO) {
                narrator.requestLine(
                    companionId = companionId,
                    context = context,
                    biome = town?.biome,
                    bankedSteps = player.bankedSteps,
                )
            }
            when (result) {
                is CompanionCommentaryResult.Spoken -> {
                    withContext(Dispatchers.IO) {
                        companionRepository.updateBond(companionId, 1)
                    }
                    _effects.emit(CompanionsEffect.InteractSuccess)
                }
                is CompanionCommentaryResult.OnCooldown -> {
                    _state.value = _state.value?.copy(
                        message = "${result.companionName} is still considering the last thing they said.",
                    )
                    _effects.emit(CompanionsEffect.CooldownActive)
                }
                CompanionCommentaryResult.NotActive ->
                    _state.value = _state.value?.copy(
                        message = "Only active companions can answer from the road.",
                    )
            }
        }
    }
}
```

- [ ] **Step 3: Wire ViewModels in AppContainer and MainActivity**

In `AppContainer.kt`, add nothing — ViewModels are created via `ViewModelProvider` in the Activity (same pattern as `JourneyViewModel`).

In `MainActivity.kt`, add two new ViewModel fields and factories alongside the existing `journeyViewModel` setup. In `onCreate()`, after the journeyViewModel factory block (around line 234), add:

```kotlin
private lateinit var marketViewModel: MarketViewModel
private lateinit var companionsViewModel: CompanionsViewModel
```

And in `onCreate()`, after creating `journeyViewModel`:

```kotlin
val marketViewModelFactory = viewModelFactory {
    initializer {
        MarketViewModel(marketRepository = container.marketRepository)
    }
}
marketViewModel = ViewModelProvider(this, marketViewModelFactory)[MarketViewModel::class.java]

val companionsViewModelFactory = viewModelFactory {
    initializer {
        CompanionsViewModel(
            companionRepository = container.companionRepository,
            gameRepository = container.gameRepository,
            narrator = container.companionNarrator,
            accessibilityPreferences = container.accessibilityPreferences,
        )
    }
}
companionsViewModel = ViewModelProvider(this, companionsViewModelFactory)[CompanionsViewModel::class.java]

scope.launch {
    marketViewModel.effects.collect { effect ->
        when (effect) {
            MarketEffect.BuySuccess -> {
                audioManager.play(AudioEvent.MarketBuy)
                hapticManager.perform(HapticEffect.CONFIRM)
            }
            MarketEffect.SellSuccess -> {
                audioManager.play(AudioEvent.MarketSell)
                hapticManager.perform(HapticEffect.CONFIRM)
            }
            MarketEffect.TransactionError -> hapticManager.perform(HapticEffect.ERROR)
        }
    }
}

scope.launch {
    companionsViewModel.effects.collect { effect ->
        when (effect) {
            CompanionsEffect.InteractSuccess -> {
                audioManager.play(AudioEvent.BondIncrease)
                hapticManager.perform(HapticEffect.REWARD)
            }
            CompanionsEffect.CooldownActive -> { /* no audio cue */ }
        }
    }
}
```

- [ ] **Step 4: Replace showMarketView() with the ViewModel-backed version**

In `MainActivity.kt`, replace the entire `showMarketView()` method:

```kotlin
private suspend fun showMarketView(
    townId: Long,
    message: String? = null,
) {
    cancelAllObservers()
    currentScreenType = NavigationShell.ScreenType.MARKET
    marketViewModel.activate(townId)

    navigationShell.replaceContent(marketView)
    navigationShell.navigateTo(NavigationShell.ScreenType.MARKET, "Market", null)

    // Render initial state
    val initialMarket = withContext(Dispatchers.IO) {
        marketRepository.observeMarket(townId).first()
    }
    marketView.render(
        buildMarketScreenState(initialMarket, message),
        buildMarketActions(townId),
    )

    marketObserveJob = scope.launch {
        kotlinx.coroutines.flow.combine(
            marketViewModel.state,
            marketViewModel.message,
        ) { state, msg -> Pair(state, msg) }
            .collect { (state, msg) ->
                state ?: return@collect
                marketView.render(buildMarketScreenState(state, msg), buildMarketActions(townId))
            }
    }
}
```

Replace `buildMarketActions()` to delegate buy/sell to the ViewModel instead of calling `marketRepository` directly:

```kotlin
private fun buildMarketActions(townId: Long): MarketActions =
    MarketActions(
        onBuy = BuyActionCallback { goodId ->
            marketViewModel.buy(townId, goodId)
        },
        onSell = SellActionCallback { goodId ->
            marketViewModel.sell(townId, goodId)
        },
        onNavigateBackToTown = MarketNavigationCallback {
            scope.launch {
                marketViewModel.deactivate()
                showTownView(townId)
            }
        },
    )
```

Remove `toBuyMessage()` and `toSellMessage()` extension functions from `MainActivity` — they now live inside `MarketViewModel`.

- [ ] **Step 5: Replace showCompanionsView() with the ViewModel-backed version**

Replace the entire `showCompanionsView()` method and its helpers:

```kotlin
private suspend fun showCompanionsView(
    townId: Long,
    message: String? = null,
) {
    cancelAllObservers()
    currentScreenType = NavigationShell.ScreenType.COMPANIONS
    companionsViewModel.activate(townId)

    navigationShell.replaceContent(companionsView)
    navigationShell.navigateTo(NavigationShell.ScreenType.COMPANIONS, "Party", null)

    // Render initial state from current ViewModel state (may be null on first activate)
    val initialActive = withContext(Dispatchers.IO) {
        companionRepository.observeActiveCompanions().first()
    }
    val initialRecruitable = withContext(Dispatchers.IO) {
        companionRepository.observeRecruitableCompanionsAtTown(townId).first()
    }
    val reduceMotion = accessibilityPreferences.reduceMotion.first()
    companionsView.render(
        buildCompanionsScreenState(
            active = initialActive,
            recruitable = initialRecruitable,
            message = message,
            recentCommentary = companionsViewModel.latestCommentary.value,
            reducedMotion = reduceMotion,
        ),
        buildCompanionsActions(townId),
    )

    companionsObserveJob = scope.launch {
        companionsViewModel.state.collect { state ->
            state ?: return@collect
            companionsView.render(
                buildCompanionsScreenState(
                    active = state.active,
                    recruitable = state.recruitable,
                    message = state.message,
                    recentCommentary = state.latestCommentary,
                    reducedMotion = state.reduceMotion,
                ),
                buildCompanionsActions(townId),
            )
        }
    }
}
```

Replace `buildCompanionsActions()` to delegate to the ViewModel:

```kotlin
private fun buildCompanionsActions(townId: Long): CompanionsActions =
    CompanionsActions(
        onNavigateBack = CompanionNavigationCallback {
            scope.launch {
                companionsViewModel.deactivate()
                showTownView(townId)
            }
        },
        onRecruit = CompanionRecruitCallback { companionId ->
            companionsViewModel.recruit(companionId)
        },
        onInteract = CompanionInteractCallback { companionId ->
            companionsViewModel.interact(companionId, townId)
        },
    )
```

Delete `renderCompanionsView()` — it's no longer needed (ViewModel observation handles re-renders).

- [ ] **Step 6: Clean up MainActivity imports and fields**

Remove from `MainActivity.kt`:
- `private var marketObserveJob: Job? = null` — handled by `marketViewModel.deactivate()`
- `private var companionsObserveJob: Job? = null` — handled by `companionsViewModel.deactivate()`
- All removed imports: `BuyResult`, `SellResult`, direct `marketRepository.buyGood/sellGood` calls
- The inline `requestCommentary` call in `buildCompanionsActions` (already replaced in Task 4 and Step 5 above)
- `companionNarrator` direct usage in `buildCompanionsActions` — now fully inside `CompanionsViewModel`

Update `cancelAllObservers()` — remove the two cancelled jobs:
```kotlin
private fun cancelAllObservers() {
    // marketObserveJob and companionsObserveJob removed; ViewModels manage their own lifecycles
    inventoryObserveJob?.cancel()
    ledgerObserveJob?.cancel()
    chronicleObserveJob?.cancel()
}
```

- [ ] **Step 7: Build**

```
./gradlew :app:assembleDebug
```

Expected: clean build. Fix any remaining import errors or missed references.

- [ ] **Step 8: Run all tests**

```
./gradlew :core:data:test :app:test
```

Expected: all tests pass.

- [ ] **Step 9: Commit**

```
git add app/src/main/java/com/wanderingledger/app/MarketViewModel.kt
git add app/src/main/java/com/wanderingledger/app/CompanionsViewModel.kt
git add app/src/main/java/com/wanderingledger/app/MainActivity.kt
git add app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt
git commit -m "refactor: extract MarketViewModel and CompanionsViewModel, slim MainActivity"
```

---

## Self-Review

**Spec coverage:**
- Candidate 1 (MarketAnomalyDetector): Task 3 ✓
- Candidate 2 (TravelOutcome sealed): Task 1 ✓
- Candidate 3 (RumorRequest self-fulfillment): Task 2 ✓
- Candidate 4 (CompanionNarrator): Task 4 ✓
- Candidate 5 (MainActivity decomposition): Task 5 ✓ (Market + Companions; ledger/chronicle/inventory/settings follow same recipe)

**Type consistency check:**
- `TravelOutcome.Arrived` / `TravelOutcome.Failed` — used consistently in Tasks 1, 2
- `PriceSnapshot(sellPrice, supplyLevel)` — defined in Task 3, used only in Task 3
- `CompanionNarrator.requestLine(companionId, context, biome, bankedSteps, nowMs)` — defined Task 4, called in JourneyViewModel (Task 4) and CompanionsViewModel (Task 5)
- `CompanionNarrator.latestLine: StateFlow<CompanionCommentaryUi?>` — defined Task 4, accessed via `journeyViewModel.latestCommentary` (Task 4) and `companionsViewModel.latestCommentary` (Task 5)
- `MarketViewModel.buy(townId, goodId)` / `sell(townId, goodId)` — defined and called with same signature in Task 5
- `CompanionsViewModel.recruit(companionId)` / `interact(companionId, townId)` — defined and called with same signature in Task 5

**No placeholders detected.**

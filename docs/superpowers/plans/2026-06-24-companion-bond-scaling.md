# Companion Bond Scaling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make companion bond level amplify each companion's core mechanical bonus — Scout step-cost discount grows from 10% to 20% across bond levels 0–5; Fighter effective combat power adds bond level to base combat power, and bandit-ambush encounters appear on long roads.

**Architecture:** All changes are pure-function updates in `core:data` (TravelTypes, TravelPolicy, EncounterEngine) with one caller update in `app` (JourneyViewModel). No schema changes. TDD throughout — all changed code is pure logic, no Robolectric needed.

**Tech Stack:** Kotlin, JUnit 4 (plain — no Robolectric for these tests).

## Global Constraints

- No new Gradle dependencies.
- All tests in this plan are plain JUnit 4 — no Robolectric, no Room.
- Known seed data: segmentId=1 has stepCost=120; Bram (Fighter) has companionId=2, combatPower=5; Mira (Scout) has companionId=1, combatPower=3.
- `CompanionRole` is in `com.wanderingledger.core.model`.
- `applyScoutDiscount` is imported in both `TravelPolicy.kt` and `JourneyViewModel.kt` — both callers must be updated when the signature changes.
- `bondLevel` is already stored on `CompanionEntity` and mapped to the `Companion` domain model. `WorldSnapshot.activeCompanions: List<Companion>` already carries it.

---

## File Structure

**Modified:**
- `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt` — replace `applyScoutDiscount(stepCost, hasActiveScout: Boolean)` with `applyScoutDiscount(stepCost, scoutBondLevel: Int?)`
- `core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt` — switch from `any { Scout }` to `firstOrNull { Scout }?.bondLevel`
- `app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt` — same Scout lookup change in route building
- `core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt` — update `resolveBanditAmbush` with effective combat power formula and tuned threshold
- `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt` — add `"bandit-ambush"` to long road event pools
- `core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt` — add two Scout bond-scaling tests
- `core/data/src/test/java/com/wanderingledger/core/data/EncounterEngineTest.kt` — add three Fighter/bandit tests

---

## Task 1: Scout Discount Scales With Bond Level

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt`
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt`
- Modify: `app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt`
- Modify: `core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt`

**Interfaces:**
- Consumes: `Companion.bondLevel: Int`, `CompanionRole.Scout`, `WorldSnapshot.activeCompanions`
- Produces: `applyScoutDiscount(stepCost: Int, scoutBondLevel: Int?): Int` — null means no active Scout; discount = `SCOUT_BASE_DISCOUNT + (bondLevel * SCOUT_BOND_DISCOUNT_PER_LEVEL)` = 10% at bond 0, 20% at bond 5

- [ ] **Step 1: Add failing tests to TravelPolicyTest**

Open `core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt`. The file has a `snapshot()` helper function — check its signature (it should accept `companions: List<Companion> = emptyList()`). Add these two tests after the existing methods:

```kotlin
@Test
fun scoutAtBondZeroAppliesTenPercentDiscount() {
    val scout = Companion(
        companionId = 1,
        name = "Mira",
        role = CompanionRole.Scout,
        combatPower = 3,
        bondLevel = 0,
        questState = "active",
        locationTownId = 1,
        isActive = true,
    )
    val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200, companions = listOf(scout)), seed = 1L)

    assertTrue(outcome is TravelOutcome.Arrived)
    val arrived = outcome as TravelOutcome.Arrived
    // stepCost=120, discount=0.10+(0*0.02)=0.10, effective=(120*0.90).toInt()=108
    assertEquals(108L, arrived.playerDelta.stepsSpent)
}

@Test
fun scoutAtMaxBondAppliesTwentyPercentDiscount() {
    val scout = Companion(
        companionId = 1,
        name = "Mira",
        role = CompanionRole.Scout,
        combatPower = 3,
        bondLevel = 5,
        questState = "active",
        locationTownId = 1,
        isActive = true,
    )
    val outcome = TravelPolicy.compute(snapshot(bankedSteps = 200, companions = listOf(scout)), seed = 1L)

    assertTrue(outcome is TravelOutcome.Arrived)
    val arrived = outcome as TravelOutcome.Arrived
    // stepCost=120, discount=0.10+(5*0.02)=0.20, effective=(120*0.80).toInt()=96
    assertEquals(96L, arrived.playerDelta.stepsSpent)
}
```

- [ ] **Step 2: Run tests — expect failures**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.TravelPolicyTest"
```

Expected: the two new tests fail with `expected:<108> but was:<120>` — the Boolean signature ignores bond level.

- [ ] **Step 3: Replace applyScoutDiscount in TravelTypes.kt**

Open `core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt`. Find and delete:

```kotlin
const val SCOUT_STEP_DISCOUNT = 0.10
fun applyScoutDiscount(stepCost: Int, hasActiveScout: Boolean): Int =
    if (hasActiveScout) (stepCost * (1.0 - SCOUT_STEP_DISCOUNT)).toInt() else stepCost
```

Replace with:

```kotlin
const val SCOUT_BASE_DISCOUNT = 0.10
const val SCOUT_BOND_DISCOUNT_PER_LEVEL = 0.02

fun applyScoutDiscount(stepCost: Int, scoutBondLevel: Int?): Int {
    if (scoutBondLevel == null) return stepCost
    val discount = SCOUT_BASE_DISCOUNT + (scoutBondLevel * SCOUT_BOND_DISCOUNT_PER_LEVEL)
    return (stepCost * (1.0 - discount)).toInt()
}
```

- [ ] **Step 4: Update TravelPolicy.kt**

Open `core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt`. Find:

```kotlin
val hasActiveScout = snapshot.activeCompanions.any {
    it.role == com.wanderingledger.core.model.CompanionRole.Scout && it.isActive
}
val effectiveStepCost = applyScoutDiscount(snapshot.road.stepCost, hasActiveScout)
```

Replace with:

```kotlin
val activeScout = snapshot.activeCompanions.firstOrNull {
    it.role == CompanionRole.Scout && it.isActive
}
val effectiveStepCost = applyScoutDiscount(snapshot.road.stepCost, activeScout?.bondLevel)
```

Ensure `import com.wanderingledger.core.model.CompanionRole` is present at the top.

- [ ] **Step 5: Update JourneyViewModel.kt**

Open `app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt`. Find the route-building block that calls `applyScoutDiscount`. It currently reads:

```kotlin
val hasActiveScout = activeCompanions.any {
    it.role == com.wanderingledger.core.model.CompanionRole.Scout && it.isActive
}
```

and passes `hasActiveScout` to `applyScoutDiscount`. Replace with:

```kotlin
val activeScout = activeCompanions.firstOrNull {
    it.role == CompanionRole.Scout && it.isActive
}
```

and update the `applyScoutDiscount` call in the route map to pass `activeScout?.bondLevel` instead of `hasActiveScout`.

- [ ] **Step 6: Run all tests**

```
./gradlew :core:data:test :app:test
```

Expected: all tests pass including both new bond-scaling tests.

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/TravelTypes.kt
git add core/data/src/main/java/com/wanderingledger/core/data/TravelPolicy.kt
git add app/src/main/java/com/wanderingledger/app/JourneyViewModel.kt
git add core/data/src/test/java/com/wanderingledger/core/data/TravelPolicyTest.kt
git commit -m "feat: scale Scout step-cost discount with bond level (10%→20% over bond 0–5)"
```

---

## Task 2: Fighter Effective Combat Power + Bandit-Ambush on Long Roads

**Files:**
- Modify: `core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt`
- Modify: `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt`
- Modify: `core/data/src/test/java/com/wanderingledger/core/data/EncounterEngineTest.kt`

**Interfaces:**
- Consumes: `Companion.combatPower: Int`, `Companion.bondLevel: Int` — both already on the `Companion` domain model
- Produces: `resolveBanditAmbush` uses `effectivePower = party.sumOf { it.combatPower + it.bondLevel }`, multiplier 2, threshold 70. Success probability without any party: ~29%. With Bram (bond 0, effectivePower=5): ~39%. With Bram at max bond (effectivePower=10): ~49%.

- [ ] **Step 1: Add failing tests to EncounterEngineTest**

Open `core/data/src/test/java/com/wanderingledger/core/data/EncounterEngineTest.kt`. Add:

```kotlin
@Test
fun banditAmbushWithHighEffectivePowerAlwaysSucceeds() {
    // effectivePower = combatPower(45) + bondLevel(5) = 50 → bonus = 100 → always > 70
    val powerfulFighter = Companion(
        companionId = 1,
        name = "Bram",
        role = CompanionRole.Fighter,
        combatPower = 45,
        bondLevel = 5,
        questState = "active",
        locationTownId = 2,
        isActive = true,
    )
    val outcome = EncounterEngine.resolve(
        seed = 1L,
        encounterId = "bandit-ambush",
        party = listOf(powerfulFighter),
    )
    assertTrue("High-power Fighter should always repel bandits", outcome.success)
    assertEquals("bandit-ambush", outcome.encounterId)
}

@Test
fun banditAmbushWithoutPartyProducesSomeFailures() {
    var failCount = 0
    for (seed in 1L..30L) {
        val outcome = EncounterEngine.resolve(seed = seed, encounterId = "bandit-ambush", party = emptyList())
        if (!outcome.success) failCount++
    }
    // With threshold 70 and no party, ~29% success → expect ~21 failures in 30 rolls
    assertTrue("Expected at least 10 failures in 30 rolls with no party", failCount >= 10)
}

@Test
fun banditAmbushFighterWithMaxBondOutperformsNoParty() {
    val fighter = Companion(
        companionId = 1,
        name = "Bram",
        role = CompanionRole.Fighter,
        combatPower = 5,
        bondLevel = 5,
        questState = "active",
        locationTownId = 2,
        isActive = true,
    )
    var withFighterSuccess = 0
    var withoutFighterSuccess = 0
    for (seed in 1L..30L) {
        if (EncounterEngine.resolve(seed, "bandit-ambush", listOf(fighter)).success) withFighterSuccess++
        if (EncounterEngine.resolve(seed, "bandit-ambush", emptyList()).success) withoutFighterSuccess++
    }
    assertTrue(
        "Max-bond Fighter (effectivePower=10, bonus=20) should succeed more often than no party",
        withFighterSuccess > withoutFighterSuccess,
    )
}
```

- [ ] **Step 2: Run tests — expect failures**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.EncounterEngineTest"
```

Expected: the three new tests fail because the current formula uses `totalPower * 10` and threshold 60, making even an empty party succeed far too often.

- [ ] **Step 3: Update resolveBanditAmbush in EncounterEngine.kt**

Open `core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt`. Replace the entire `resolveBanditAmbush` method:

```kotlin
private fun resolveBanditAmbush(
    random: Random,
    party: List<Companion>,
): EncounterOutcome {
    val effectivePower = party.sumOf { it.combatPower + it.bondLevel }
    val roll = random.nextInt(100) + effectivePower * 2

    return if (roll > 70) {
        EncounterOutcome(
            encounterId = "bandit-ambush",
            resultText = "Bandits tried to ambush you, but your party drove them off!",
            bondChange = 1,
        )
    } else {
        EncounterOutcome(
            encounterId = "bandit-ambush",
            resultText = "You were ambushed by bandits and couldn't hold them off. They took what they could.",
            goldChange = -30,
            success = false,
        )
    }
}
```

- [ ] **Step 4: Run the new tests**

```
./gradlew :core:data:test --tests "com.wanderingledger.core.data.EncounterEngineTest"
```

Expected: all three new tests pass.

- [ ] **Step 5: Add bandit-ambush to long road event pools in SeedWorld.kt**

Open `core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt`. Find:

```kotlin
RoadSegmentEntity(5, 1, 3, 5000, "long", "[\"old-road\"]"),
RoadSegmentEntity(6, 3, 1, 5000, "long", "[\"old-road\"]"),
```

Replace with:

```kotlin
RoadSegmentEntity(5, 1, 3, 5000, "long", "[\"old-road\",\"bandit-ambush\"]"),
RoadSegmentEntity(6, 3, 1, 5000, "long", "[\"old-road\",\"bandit-ambush\"]"),
```

- [ ] **Step 6: Run all core:data tests**

```
./gradlew :core:data:test
```

Expected: all tests pass. SeedWorld changes only affect new game seeding; existing in-memory test databases start fresh.

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/java/com/wanderingledger/core/data/EncounterEngine.kt
git add core/database/src/main/java/com/wanderingledger/core/database/SeedWorld.kt
git add core/data/src/test/java/com/wanderingledger/core/data/EncounterEngineTest.kt
git commit -m "feat: Fighter effective combat power scales with bond; bandit-ambush on long roads"
```

---

## Self-Review

**Spec coverage:**
- Scout discount 10% (bond 0) → 20% (bond 5): Task 1 ✓
- `effectiveCombatPower = baseCombatPower + bondLevel` for Fighter: Task 2 ✓
- `bandit-ambush` on long roads (segments 5 and 6): Task 2 Step 5 ✓
- Threshold tuned so Fighter "shifts meaningfully" not trivializes: multiplier 2, threshold 70 ✓
- Bond scaling is universal (both Scout and Fighter follow the same additive bond pattern): Tasks 1 and 2 ✓

**Placeholder scan:** None found.

**Type consistency:**
- `applyScoutDiscount(stepCost: Int, scoutBondLevel: Int?)` — defined Task 1 Step 3, called in TravelPolicy (Task 1 Step 4) and JourneyViewModel (Task 1 Step 5). ✓
- `party.sumOf { it.combatPower + it.bondLevel }` — defined and used only in EncounterEngine resolveBanditAmbush. ✓

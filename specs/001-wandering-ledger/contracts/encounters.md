# Encounter Contract: Deterministic Seeds & Replay

This contract defines the canonical encounter scenarios, deterministic seed requirements, and replay test expectations for the Wandering Ledger encounter system.

## Encounter Types

| Encounter ID | Description | Party Bonus | Success Threshold |
|--------------|-------------|-------------|-------------------|
| `merchant-cart` | Help merchant with wheel | Rogue: +20 to roll | Roll > 50 |
| `fog-bank` | Navigate foggy area | Scout: always safe | Roll > 40 (or has Scout) |
| `old-road` | Find hidden treasure cache | - | Roll > 70 |
| `bandit-ambush` | Defend against bandits | Fighter power * 10 | Roll + bonus > 60 |

## Canonical Seed Values

These seeds produce deterministic, reproducible outcomes for testing:

| Seed | Encounter | Expected Outcome |
|------|-----------|------------------|
| 1 | merchant-cart | No gold (failure) |
| 50 | merchant-cart | Success (15 gold) |
| 100 | merchant-cart | No gold (failure) |
| 1 | fog-bank | Success (safe) |
| 10 | fog-bank | Success (safe) |
| 50 | fog-bank | Failure (-10 gold) |
| 1 | old-road | Nothing found |
| 75 | old-road | Treasure (50 gold) |
| 10 | bandit-ambush | Victory (+5 bond) |
| 1 | bandit-ambush | Defeat (-30 gold, -2 bond) |

## Party Compositions for Canonical Tests

### Empty Party
```kotlin
val emptyParty = emptyList<Companion>()
```

### Single Fighter Party
```kotlin
val fighterParty = listOf(
    Companion(1, "Warrior", CompanionRole.Fighter, 5, 0, "active", 1, true)
)
```

### Scout Party
```kotlin
val scoutParty = listOf(
    Companion(1, "Scout", CompanionRole.Scout, 2, 0, "active", 1, true)
)
```

### Mixed Party (Rogue + Fighter)
```kotlin
val mixedParty = listOf(
    Companion(1, "Rogue", CompanionRole.Rogue, 2, 0, "active", 1, true),
    Companion(2, "Warrior", CompanionRole.Fighter, 5, 0, "active", 1, true)
)
```

## Replay Requirements

### Initial State Recording

When an encounter is resolved during travel, the following must be persisted for replay:

```kotlin
data class EncounterInitialState(
    val encounterId: String,
    val seed: Long,
    val partySnapshot: List<Companion>,  // Companion IDs, names, roles, bond levels
    val resolvedAt: Long,
)
```

### Replay Test Contract

```kotlin
/**
 * Verify that an encounter can be replayed from recorded initial state.
 * Given the same seed and party composition, the outcome must be identical.
 */
fun verifyEncounterReplay(initialState: EncounterInitialState): Boolean {
    val replayOutcome = EncounterEngine.resolve(
        seed = initialState.seed,
        encounterId = initialState.encounterId,
        party = initialState.partySnapshot
    )
    
    // Compare with originally recorded outcome stored in EventLog
    val originalOutcome = loadFromEventLog(initialState.resolvedAt)
    return replayOutcome == originalOutcome
}
```

## Test Coverage Requirements

- [ ] All 4 encounter types have deterministic seed tests
- [ ] Party composition tests cover empty, single, and mixed parties
- [ ] Replay test verifies same seed + party = same outcome
- [ ] Edge case: unknown encounter returns neutral result
- [ ] Canonical seed table is verified against implementation
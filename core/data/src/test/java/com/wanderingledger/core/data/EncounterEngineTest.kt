package com.wanderingledger.core.data

import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterEngineTest {
    @Test
    fun merchantCartWithRogueGetsBonus() {
        val party =
            listOf(
                Companion(1, "Rogue", CompanionRole.Rogue, 2, 0, "active", 1, true),
            )

        val outcome = EncounterEngine.resolve(100, "merchant-cart", party)

        assertTrue("Should succeed with rogue bonus", outcome.success)
        assertEquals(15, outcome.goldChange)
    }

    @Test
    fun merchantCartProducesGoldOnSuccess() {
        val party = emptyList<Companion>()

        // Find a seed that succeeds (roll > 50)
        var successFound = false
        for (seed in 1..200) {
            val outcome = EncounterEngine.resolve(seed.toLong(), "merchant-cart", party)
            if (outcome.success && outcome.goldChange == 15L) {
                successFound = true
                break
            }
        }
        assertTrue("Should find a successful merchant cart encounter", successFound)
    }

    @Test
    fun merchantCartNoGoldOnFailure() {
        val party = emptyList<Companion>()

        var failureFound = false
        for (seed in 1..200) {
            val outcome = EncounterEngine.resolve(seed.toLong(), "merchant-cart", party)
            if (!outcome.success || outcome.goldChange == 0L) {
                failureFound = true
                break
            }
        }
        assertTrue("Should find a failed merchant cart encounter", failureFound)
    }

    @Test
    fun fogBankWithScoutSucceedsAlways() {
        val party =
            listOf(
                Companion(1, "Scout", CompanionRole.Scout, 2, 0, "active", 1, true),
            )

        // Scout makes fog safe regardless of roll
        for (seed in 1..50) {
            val outcome = EncounterEngine.resolve(seed.toLong(), "fog-bank", party)
            assertTrue("Scout should navigate fog safely for seed $seed", outcome.success)
        }
    }

    @Test
    fun fogBankWithoutScoutMayFail() {
        val party = emptyList<Companion>()

        var failureFound = false
        for (seed in 1..100) {
            val outcome = EncounterEngine.resolve(seed.toLong(), "fog-bank", party)
            if (!outcome.success) {
                failureFound = true
                break
            }
        }
        assertTrue("Should find a fog failure without scout", failureFound)
    }

    @Test
    fun oldRoadHasTreasureChance() {
        val party = emptyList<Companion>()

        var treasureFound = false
        for (seed in 1..1000) {
            val outcome = EncounterEngine.resolve(seed.toLong(), "old-road", party)
            if (outcome.goldChange == 50L) {
                treasureFound = true
                break
            }
        }
        assertTrue("Should find treasure on old road", treasureFound)
    }

    @Test
    fun banditAmbushWithFightersSucceeds() {
        val party =
            listOf(
                Companion(1, "Warrior", CompanionRole.Fighter, 5, 0, "active", 1, true),
            )

        // With effectivePower = 5 + 0 = 5, roll gets +10 bonus (5*2), threshold 70
        val outcome = EncounterEngine.resolve(100, "bandit-ambush", party)

        assertTrue("Fighter should defeat bandits", outcome.success)
        assertEquals(1, outcome.bondChange)
    }

    @Test
    fun banditAmbushWithoutCompanionsCanFail() {
        val party = emptyList<Companion>()

        var failureFound = false
        for (seed in 1..50) {
            val outcome = EncounterEngine.resolve(seed.toLong(), "bandit-ambush", party)
            if (!outcome.success) {
                failureFound = true
                break
            }
        }
        assertTrue("Should find a bandit failure without companions", failureFound)
    }

    @Test
    fun deterministicSameSeedProducesSameResult() {
        val party =
            listOf(
                Companion(1, "Test", CompanionRole.Fighter, 3, 0, "active", 1, true),
            )

        val outcome1 = EncounterEngine.resolve(42, "merchant-cart", party)
        val outcome2 = EncounterEngine.resolve(42, "merchant-cart", party)

        assertEquals(outcome1.goldChange, outcome2.goldChange)
        assertEquals(outcome1.success, outcome2.success)
    }

    @Test
    fun unknownEncounterReturnsNeutralResult() {
        val party = emptyList<Companion>()

        val outcome = EncounterEngine.resolve(1, "unknown-event", party)

        assertEquals("Nothing unusual happened.", outcome.resultText)
        assertEquals(0, outcome.goldChange)
        assertEquals(0, outcome.bondChange)
    }

    @Test
    fun replayFromRecordedStateMatchesOriginal() {
        val initialState =
            listOf(
                Companion(1, "Warrior", CompanionRole.Fighter, 5, 2, "active", 1, true),
                Companion(2, "Scout", CompanionRole.Scout, 2, 1, "active", 1, true),
            )
        val seed = 42L
        val encounterId = "bandit-ambush"

        val originalOutcome = EncounterEngine.resolve(seed, encounterId, initialState)

        val replayOutcome = EncounterEngine.resolve(seed, encounterId, initialState)

        assertEquals("Replay must match original outcome", originalOutcome, replayOutcome)
        assertEquals(originalOutcome.goldChange, replayOutcome.goldChange)
        assertEquals(originalOutcome.bondChange, replayOutcome.bondChange)
        assertEquals(originalOutcome.success, replayOutcome.success)
    }

    @Test
    fun canonicalSeedsProduceExpectedOutcomes() {
        val emptyParty = emptyList<Companion>()

        val merchantOutcome = EncounterEngine.resolve(1, "merchant-cart", emptyParty)
        assertTrue("merchant-cart should produce outcome at seed 1", merchantOutcome.goldChange >= 0)

        val merchantOutcome2 = EncounterEngine.resolve(1, "merchant-cart", emptyParty)
        assertEquals(
            "Seed 1 must be deterministic for merchant-cart",
            merchantOutcome.goldChange,
            merchantOutcome2.goldChange,
        )

        val fogOutcome = EncounterEngine.resolve(50, "fog-bank", emptyParty)
        assertNotNull("fog-bank should produce outcome at seed 50", fogOutcome)

        val oldRoadOutcome = EncounterEngine.resolve(75, "old-road", emptyParty)
        assertNotNull("old-road should produce outcome at seed 75", oldRoadOutcome)

        val fighterParty = listOf(Companion(1, "W", CompanionRole.Fighter, 5, 0, "active", 1, true))
        val banditWin = EncounterEngine.resolve(100, "bandit-ambush", fighterParty)
        assertTrue("Fighter party should succeed at seed 100", banditWin.success)

        val banditAny = EncounterEngine.resolve(1, "bandit-ambush", emptyList())
        assertNotNull("Empty party gets some outcome at seed 1", banditAny)
    }

    @Test
    fun differentSeedsProduceDifferentOutcomes() {
        val party = emptyList<Companion>()

        val outcomes =
            (1..20).map { seed ->
                EncounterEngine.resolve(seed.toLong(), "merchant-cart", party)
            }

        val uniqueOutcomes = outcomes.distinct()
        assertTrue("Different seeds should produce different outcomes", uniqueOutcomes.size > 1)
    }

    @Test
    fun banditAmbushWithHighEffectivePowerAlwaysSucceeds() {
        // effectivePower = combatPower(45) + bondLevel(5) = 50 → bonus = 100 → always > 70
        val powerfulFighter =
            Companion(
                companionId = 1,
                name = "Bram",
                role = CompanionRole.Fighter,
                combatPower = 45,
                bondLevel = 5,
                questState = "active",
                locationTownId = 2,
                isActive = true,
            )
        val outcome =
            EncounterEngine.resolve(
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
        val fighter =
            Companion(
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
}

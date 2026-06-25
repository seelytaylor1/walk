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

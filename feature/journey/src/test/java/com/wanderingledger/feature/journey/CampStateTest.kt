package com.wanderingledger.feature.journey

import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampStateTest {

    // ── CampStateDetector.shouldEnterCamp ────────────────────────────────────

    @Test
    fun `shouldEnterCamp returns false when idle time is under 5 minutes`() {
        val now = 1_000_000L
        val lastTravel = now - 4 * 60 * 1000L // 4 min ago
        assertFalse(CampStateDetector.shouldEnterCamp(lastTravel, now, bankedSteps = 200))
    }

    @Test
    fun `shouldEnterCamp returns false when steps are below threshold`() {
        val now = 1_000_000L
        val lastTravel = now - 6 * 60 * 1000L // 6 min ago
        assertFalse(CampStateDetector.shouldEnterCamp(lastTravel, now, bankedSteps = 99))
    }

    @Test
    fun `shouldEnterCamp returns true at exactly the 5-minute boundary with sufficient steps`() {
        val now = 1_000_000L
        val lastTravel = now - 5 * 60 * 1000L
        assertTrue(CampStateDetector.shouldEnterCamp(lastTravel, now, bankedSteps = 100))
    }

    @Test
    fun `shouldEnterCamp returns true when idle long enough and steps meet threshold`() {
        val now = 1_000_000L
        val lastTravel = now - 30 * 60 * 1000L // 30 min ago
        assertTrue(CampStateDetector.shouldEnterCamp(lastTravel, now, bankedSteps = 500))
    }

    // ── CampStateDetector.determineCampActivity ───────────────────────────────

    @Test
    fun `determineCampActivity assigns KeepingWatch to Scout`() {
        assertEquals(CampActivity.KeepingWatch, CampStateDetector.determineCampActivity(companion(role = CompanionRole.Scout)))
    }

    @Test
    fun `determineCampActivity assigns Cooking to Healer`() {
        assertEquals(CampActivity.Cooking, CampStateDetector.determineCampActivity(companion(role = CompanionRole.Healer)))
    }

    @Test
    fun `determineCampActivity assigns Chatting to high-bond non-special role`() {
        assertEquals(CampActivity.Chatting, CampStateDetector.determineCampActivity(companion(role = CompanionRole.Fighter, bondLevel = 5)))
    }

    @Test
    fun `determineCampActivity assigns Sitting to low-bond non-special role`() {
        assertEquals(CampActivity.Sitting, CampStateDetector.determineCampActivity(companion(role = CompanionRole.Mage, bondLevel = 2)))
    }

    @Test
    fun `determineCampActivity prioritises role over bond level for Scout`() {
        // A Scout with high bond level should still get KeepingWatch, not Chatting.
        assertEquals(CampActivity.KeepingWatch, CampStateDetector.determineCampActivity(companion(role = CompanionRole.Scout, bondLevel = 10)))
    }

    // ── CampState.camping factory ─────────────────────────────────────────────

    @Test
    fun `camping factory sets Camping journey mode`() {
        val state = CampState.camping(biome = Biome.Forest, companions = emptyList())
        assertEquals(JourneyMode.Camping, state.journeyMode)
        assertTrue(state.isCamping)
    }

    @Test
    fun `camping factory assigns role-based activities to companions`() {
        val companions = listOf(
            companion(id = 1L, role = CompanionRole.Scout),
            companion(id = 2L, role = CompanionRole.Healer),
            companion(id = 3L, role = CompanionRole.Fighter),
        )
        val state = CampState.camping(biome = Biome.Forest, companions = companions)

        assertEquals(CampActivity.KeepingWatch, state.campActivities[1L])
        assertEquals(CampActivity.Cooking, state.campActivities[2L])
        assertEquals(CampActivity.Sitting, state.campActivities[3L])
    }

    @Test
    fun `camping factory with no companions produces empty activity map`() {
        val state = CampState.camping(biome = Biome.Mountain, companions = emptyList())
        assertTrue(state.campActivities.isEmpty())
    }

    // ── CampState.withUpdatedDuration ─────────────────────────────────────────

    @Test
    fun `withUpdatedDuration calculates elapsed minutes correctly`() {
        val startTime = 1_000_000L
        val state = CampState(startTime = startTime)
        val updated = state.withUpdatedDuration(startTime + 3 * 60 * 1000L)
        assertEquals(3, updated.durationMinutes)
    }

    @Test
    fun `withUpdatedDuration truncates partial minutes`() {
        val startTime = 1_000_000L
        val state = CampState(startTime = startTime)
        val updated = state.withUpdatedDuration(startTime + 90_000L) // 1.5 minutes
        assertEquals(1, updated.durationMinutes)
    }

    @Test
    fun `withUpdatedDuration returns zero for no elapsed time`() {
        val startTime = 1_000_000L
        val state = CampState(startTime = startTime)
        val updated = state.withUpdatedDuration(startTime)
        assertEquals(0, updated.durationMinutes)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun companion(
        id: Long = 1L,
        role: CompanionRole = CompanionRole.Fighter,
        bondLevel: Int = 0,
    ) = Companion(
        companionId = id,
        name = "Test",
        role = role,
        combatPower = 5,
        bondLevel = bondLevel,
        locationTownId = 1L,
    )
}

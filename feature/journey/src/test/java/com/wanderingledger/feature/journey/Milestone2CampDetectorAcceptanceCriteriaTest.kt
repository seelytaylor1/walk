package com.wanderingledger.feature.journey

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Acceptance criteria tests for Milestone 2, Issue #15: CampState auto-detection.
 *
 * Tests live here (feature:journey) because CampStateDetector is defined in this module
 * and is not a dependency of core:data.
 */
class Milestone2CampDetectorAcceptanceCriteriaTest {

    // ── Issue #15: CampState auto-detection ──────────────────────────────────

    /**
     * AC-15-1: shouldEnterCamp returns true after 5+ min idle with >= 100 banked steps.
     */
    @Test
    fun `AC-15-1 CampStateDetector activates after 5 min idle with 100 banked steps`() {
        val lastTravelTime = System.currentTimeMillis() - (6 * 60 * 1000L) // 6 minutes ago
        val currentTime = System.currentTimeMillis()

        val shouldCamp = CampStateDetector.shouldEnterCamp(
            lastTravelTime = lastTravelTime,
            currentTime = currentTime,
            bankedSteps = 100L,
        )
        assertTrue("Should enter camp after 5+ min idle with 100 banked steps", shouldCamp)
    }

    /**
     * AC-15-2: shouldEnterCamp returns false when banked steps are below threshold (< 100).
     */
    @Test
    fun `AC-15-2 CampStateDetector does not activate with insufficient banked steps`() {
        val lastTravelTime = System.currentTimeMillis() - (6 * 60 * 1000L)
        val currentTime = System.currentTimeMillis()

        val shouldCamp = CampStateDetector.shouldEnterCamp(
            lastTravelTime = lastTravelTime,
            currentTime = currentTime,
            bankedSteps = 50L,
        )
        assertFalse("Should not enter camp with only 50 banked steps", shouldCamp)
    }

    /**
     * AC-15-3: shouldEnterCamp returns false when less than 5 minutes have passed.
     */
    @Test
    fun `AC-15-3 CampStateDetector does not activate before 5 min idle`() {
        val lastTravelTime = System.currentTimeMillis() - (2 * 60 * 1000L) // 2 minutes ago
        val currentTime = System.currentTimeMillis()

        val shouldCamp = CampStateDetector.shouldEnterCamp(
            lastTravelTime = lastTravelTime,
            currentTime = currentTime,
            bankedSteps = 500L,
        )
        assertFalse("Should not enter camp after only 2 min idle", shouldCamp)
    }
}

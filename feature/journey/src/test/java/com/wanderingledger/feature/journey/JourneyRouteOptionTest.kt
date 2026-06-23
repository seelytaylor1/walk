package com.wanderingledger.feature.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyRouteOptionTest {

    @Test
    fun `canAfford is true when banked steps exactly equal step cost`() {
        val route = route(stepCost = 100, bankedSteps = 100)
        assertTrue(route.canAfford)
        assertEquals(0L, route.shortfall)
    }

    @Test
    fun `canAfford is true when banked steps exceed step cost`() {
        val route = route(stepCost = 100, bankedSteps = 250)
        assertTrue(route.canAfford)
        assertEquals(0L, route.shortfall)
    }

    @Test
    fun `canAfford is false when banked steps are below step cost`() {
        val route = route(stepCost = 100, bankedSteps = 60)
        assertFalse(route.canAfford)
        assertEquals(40L, route.shortfall)
    }

    @Test
    fun `canAfford is false with zero banked steps`() {
        val route = route(stepCost = 100, bankedSteps = 0)
        assertFalse(route.canAfford)
        assertEquals(100L, route.shortfall)
    }

    @Test
    fun `shortfall is never negative when over-funded`() {
        val route = route(stepCost = 50, bankedSteps = 1000)
        assertEquals(0L, route.shortfall)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun route(
        stepCost: Int,
        bankedSteps: Long,
    ) = JourneyRouteOption(
        segmentId = 1L,
        destinationName = "Stoneford",
        stepCost = stepCost,
        narrativeDistance = "a half-day's walk",
        bankedSteps = bankedSteps,
    )
}

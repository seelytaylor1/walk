package com.wanderingledger.feature.journey

import com.wanderingledger.core.model.Biome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JourneyScreenStateTest {

    // ── buildJourneyScreenState ───────────────────────────────────────────────

    @Test
    fun `routes are mapped from routeDestinations with correct names and costs`() {
        val state = build(
            bankedSteps = 200,
            routes = listOf(
                Triple(1L, "Stoneford", Pair(120, "a half-day's walk")),
                Triple(2L, "Ashwick", Pair(300, "a full day's journey")),
            ),
        )

        assertEquals(2, state.routes.size)
        assertEquals("Stoneford", state.routes[0].destinationName)
        assertEquals(120, state.routes[0].stepCost)
        assertEquals("Ashwick", state.routes[1].destinationName)
        assertEquals(300, state.routes[1].stepCost)
    }

    @Test
    fun `routes mark affordability based on banked steps`() {
        val state = build(
            bankedSteps = 200,
            routes = listOf(
                Triple(1L, "Near Town", Pair(100, "close")),
                Triple(2L, "Far Town", Pair(500, "far")),
            ),
        )

        assertTrue("Should afford 100-step route with 200 steps", state.routes[0].canAfford)
        assertFalse("Should not afford 500-step route with 200 steps", state.routes[1].canAfford)
    }

    @Test
    fun `routePathData is generated with matching segment IDs and affordability`() {
        val state = build(
            bankedSteps = 150,
            routes = listOf(
                Triple(5L, "High Pass", Pair(100, "a steep climb")),
                Triple(6L, "Deep Ford", Pair(200, "a muddy crossing")),
            ),
        )

        assertEquals(2, state.routePathData.size)
        assertEquals(5L, state.routePathData[0].segmentId)
        assertTrue(state.routePathData[0].isAffordable)
        assertEquals(6L, state.routePathData[1].segmentId)
        assertFalse(state.routePathData[1].isAffordable)
    }

    @Test
    fun `town metadata is passed through unchanged`() {
        val state = build(
            townName = "Millhaven",
            townRegion = "Heartlands",
            biome = Biome.Coast,
            bankedSteps = 0,
            lifetimeSteps = 1000,
            message = "You have arrived.",
        )

        assertEquals("Millhaven", state.currentTownName)
        assertEquals("Heartlands", state.currentTownRegion)
        assertEquals(Biome.Coast, state.currentBiome)
        assertEquals(0L, state.bankedSteps)
        assertEquals(1000L, state.lifetimeSteps)
        assertEquals("You have arrived.", state.message)
    }

    @Test
    fun `empty route list produces empty routes and routePathData`() {
        val state = build(bankedSteps = 100, routes = emptyList())

        assertTrue(state.routes.isEmpty())
        assertTrue(state.routePathData.isEmpty())
    }

    @Test
    fun `message defaults to null when omitted`() {
        val state = build(bankedSteps = 100, routes = emptyList())
        assertNull(state.message)
    }

    // ── parseEventPool ────────────────────────────────────────────────────────

    @Test
    fun `parseEventPool returns list from valid JSON array`() {
        val result = parseEventPool("""["merchant-cart","fog-bank"]""")
        assertEquals(listOf("merchant-cart", "fog-bank"), result)
    }

    @Test
    fun `parseEventPool returns empty list for empty JSON array`() {
        assertTrue(parseEventPool("[]").isEmpty())
    }

    @Test
    fun `parseEventPool returns empty list for invalid JSON`() {
        assertTrue(parseEventPool("not-valid-json").isEmpty())
    }

    @Test
    fun `parseEventPool returns empty list for malformed JSON`() {
        assertTrue(parseEventPool("{\"key\":\"value\"}").isEmpty())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun build(
        townName: String = "Millhaven",
        townRegion: String = "Heartlands",
        biome: Biome = Biome.Forest,
        bankedSteps: Long,
        lifetimeSteps: Long = bankedSteps,
        routes: List<Triple<Long, String, Pair<Int, String>>> = emptyList(),
        message: String? = null,
    ) = buildJourneyScreenState(
        currentTownName = townName,
        currentTownRegion = townRegion,
        currentBiome = biome,
        bankedSteps = bankedSteps,
        lifetimeSteps = lifetimeSteps,
        routeDestinations = routes,
        message = message,
    )
}

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
        val events =
            detect(
                baseSell = 100L,
                newSell = 160L,
                oldSupply = SupplyLevel.Normal,
                newSupply = SupplyLevel.Scarce,
            )
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.PriceSpike })
        assertTrue(events.any { it.anomalyType == MarketAnomalyType.SupplyDepleted })
    }
}

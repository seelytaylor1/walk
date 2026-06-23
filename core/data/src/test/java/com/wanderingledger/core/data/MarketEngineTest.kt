package com.wanderingledger.core.data

import com.wanderingledger.core.model.SupplyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MarketEngine] — pure price calculation and supply/demand logic.
 *
 * No Android or Room dependencies; runs as a plain JVM test.
 */
class MarketEngineTest {
    // ── computeSellPrice ─────────────────────────────────────────────────────

    @Test
    fun sellPriceForNormalSupplyEqualsBaseValue() {
        val price = MarketEngine.computeSellPrice(baseValue = 20L, supplyLevel = SupplyLevel.Normal)
        assertEquals(20L, price)
    }

    @Test
    fun sellPriceForScarceSupplyIsHigherThanBase() {
        val price = MarketEngine.computeSellPrice(baseValue = 20L, supplyLevel = SupplyLevel.Scarce)
        // 20 * 1.5 = 30
        assertEquals(30L, price)
    }

    @Test
    fun sellPriceForAbundantSupplyIsLowerThanBase() {
        val price = MarketEngine.computeSellPrice(baseValue = 20L, supplyLevel = SupplyLevel.Abundant)
        // 20 * 0.7 = 14
        assertEquals(14L, price)
    }

    @Test
    fun sellPriceIsAtLeastOne() {
        // Very small base value with abundant supply should still be at least 1
        val price = MarketEngine.computeSellPrice(baseValue = 1L, supplyLevel = SupplyLevel.Abundant)
        assertTrue(price >= 1L)
    }

    // ── computeBuyPrice ──────────────────────────────────────────────────────

    @Test
    fun buyPriceIsLessThanSellPrice() {
        val sell = MarketEngine.computeSellPrice(20L, SupplyLevel.Normal)
        val buy = MarketEngine.computeBuyPrice(sell)
        assertTrue("Buy price $buy should be less than sell price $sell", buy < sell)
    }

    @Test
    fun buyPriceIsAtLeastOne() {
        val buy = MarketEngine.computeBuyPrice(sellPrice = 1L)
        assertTrue(buy >= 1L)
    }

    @Test
    fun buyPriceNeverExceedsSellPrice() {
        for (sell in 1L..100L) {
            val buy = MarketEngine.computeBuyPrice(sell)
            assertTrue("Buy $buy should be <= sell $sell", buy <= sell)
        }
    }

    // ── computePrices ────────────────────────────────────────────────────────

    @Test
    fun computePricesReturnsSellAndBuyInOrder() {
        val (sell, buy) = MarketEngine.computePrices(baseValue = 30L, supplyLevel = SupplyLevel.Normal)
        assertEquals(30L, sell)
        assertTrue(buy < sell)
    }

    @Test
    fun computePricesForAllSupplyLevels() {
        val base = 18L
        val (scarceSell, scarceBuy) = MarketEngine.computePrices(base, SupplyLevel.Scarce)
        val (normalSell, normalBuy) = MarketEngine.computePrices(base, SupplyLevel.Normal)
        val (abundantSell, abundantBuy) = MarketEngine.computePrices(base, SupplyLevel.Abundant)

        // Sell prices should be ordered: scarce > normal > abundant
        assertTrue(scarceSell > normalSell)
        assertTrue(normalSell > abundantSell)

        // Buy prices should follow the same ordering
        assertTrue(scarceBuy > normalBuy)
        assertTrue(normalBuy > abundantBuy)
    }

    // ── decreaseSupply ───────────────────────────────────────────────────────

    @Test
    fun decreaseSupplyFromAbundantGivesNormal() {
        assertEquals(SupplyLevel.Normal, MarketEngine.decreaseSupply(SupplyLevel.Abundant))
    }

    @Test
    fun decreaseSupplyFromNormalGivesScarce() {
        assertEquals(SupplyLevel.Scarce, MarketEngine.decreaseSupply(SupplyLevel.Normal))
    }

    @Test
    fun decreaseSupplyFromScarceStaysScarce() {
        assertEquals(SupplyLevel.Scarce, MarketEngine.decreaseSupply(SupplyLevel.Scarce))
    }

    // ── increaseSupply ───────────────────────────────────────────────────────

    @Test
    fun increaseSupplyFromScarceGivesNormal() {
        assertEquals(SupplyLevel.Normal, MarketEngine.increaseSupply(SupplyLevel.Scarce))
    }

    @Test
    fun increaseSupplyFromNormalGivesAbundant() {
        assertEquals(SupplyLevel.Abundant, MarketEngine.increaseSupply(SupplyLevel.Normal))
    }

    @Test
    fun increaseSupplyFromAbundantStaysAbundant() {
        assertEquals(SupplyLevel.Abundant, MarketEngine.increaseSupply(SupplyLevel.Abundant))
    }

    // ── clampSupplyLevel ─────────────────────────────────────────────────────

    @Test
    fun clampSupplyLevelZeroIsScarce() {
        assertEquals(SupplyLevel.Scarce, MarketEngine.clampSupplyLevel(0))
    }

    @Test
    fun clampSupplyLevelOneIsNormal() {
        assertEquals(SupplyLevel.Normal, MarketEngine.clampSupplyLevel(1))
    }

    @Test
    fun clampSupplyLevelTwoIsAbundant() {
        assertEquals(SupplyLevel.Abundant, MarketEngine.clampSupplyLevel(2))
    }

    @Test
    fun clampSupplyLevelNegativeIsScarce() {
        assertEquals(SupplyLevel.Scarce, MarketEngine.clampSupplyLevel(-5))
    }

    @Test
    fun clampSupplyLevelAboveMaxIsAbundant() {
        assertEquals(SupplyLevel.Abundant, MarketEngine.clampSupplyLevel(99))
    }

    // ── Price ordering invariants ────────────────────────────────────────────

    @Test
    fun scarcePriceIsHighestAcrossAllBaseValues() {
        for (base in 1L..50L) {
            val scarceSell = MarketEngine.computeSellPrice(base, SupplyLevel.Scarce)
            val normalSell = MarketEngine.computeSellPrice(base, SupplyLevel.Normal)
            val abundantSell = MarketEngine.computeSellPrice(base, SupplyLevel.Abundant)
            assertTrue(
                "For base=$base: scarce=$scarceSell should be >= normal=$normalSell",
                scarceSell >= normalSell,
            )
            assertTrue(
                "For base=$base: normal=$normalSell should be >= abundant=$abundantSell",
                normalSell >= abundantSell,
            )
        }
    }

    @Test
    fun buyingDecreasesSupplyAndRaisesPrices() {
        val base = 20L
        val initialSupply = SupplyLevel.Normal
        val (initialSell, _) = MarketEngine.computePrices(base, initialSupply)

        val newSupply = MarketEngine.decreaseSupply(initialSupply)
        val (newSell, _) = MarketEngine.computePrices(base, newSupply)

        // After buying (supply decreases), sell price should increase
        assertTrue(
            "After buying, sell price should increase: $newSell > $initialSell",
            newSell > initialSell,
        )
    }

    @Test
    fun sellingIncreasesSupplyAndLowersPrices() {
        val base = 20L
        val initialSupply = SupplyLevel.Normal
        val (initialSell, _) = MarketEngine.computePrices(base, initialSupply)

        val newSupply = MarketEngine.increaseSupply(initialSupply)
        val (newSell, _) = MarketEngine.computePrices(base, newSupply)

        // After selling (supply increases), sell price should decrease
        assertTrue(
            "After selling, sell price should decrease: $newSell < $initialSell",
            newSell < initialSell,
        )
    }

    // ── Price boundary scenarios ─────────────────────────────────────────────

    @Test
    fun sellPriceForBaseOneIsAlwaysOneAcrossAllSupplyLevels() {
        // base=1 with any multiplier rounds down to 0 or 1; floor ensures minimum of 1
        assertEquals(1L, MarketEngine.computeSellPrice(1L, SupplyLevel.Scarce))
        assertEquals(1L, MarketEngine.computeSellPrice(1L, SupplyLevel.Normal))
        assertEquals(1L, MarketEngine.computeSellPrice(1L, SupplyLevel.Abundant))
    }

    @Test
    fun buyPriceForBaseOneIsAlwaysOneAcrossAllSupplyLevels() {
        // When sell price is 1 (floor), buy price must also be 1 (floor, clamped to sell)
        for (supply in SupplyLevel.entries) {
            val sell = MarketEngine.computeSellPrice(1L, supply)
            val buy = MarketEngine.computeBuyPrice(sell)
            assertEquals("Buy price should be 1 when sell price is 1 at $supply", 1L, buy)
        }
    }

    @Test
    fun sellPriceForVeryHighBaseValueDoesNotOverflow() {
        // Use a large but safe base value — verify it produces a positive result
        val largeBase = 1_000_000L
        val scarceSell = MarketEngine.computeSellPrice(largeBase, SupplyLevel.Scarce)
        val normalSell = MarketEngine.computeSellPrice(largeBase, SupplyLevel.Normal)
        val abundantSell = MarketEngine.computeSellPrice(largeBase, SupplyLevel.Abundant)

        assertTrue("Scarce sell price should be positive for large base", scarceSell > 0)
        assertTrue("Normal sell price should be positive for large base", normalSell > 0)
        assertTrue("Abundant sell price should be positive for large base", abundantSell > 0)
        // Ordering still holds
        assertTrue(scarceSell > normalSell)
        assertTrue(normalSell > abundantSell)
    }

    @Test
    fun computePricesForSingleUnitBaseValue() {
        // Single-unit transaction: base=1, all supply levels
        for (supply in SupplyLevel.entries) {
            val (sell, buy) = MarketEngine.computePrices(1L, supply)
            assertTrue("Sell price must be >= 1 for base=1 at $supply", sell >= 1L)
            assertTrue("Buy price must be >= 1 for base=1 at $supply", buy >= 1L)
            assertTrue("Buy price must be <= sell price for base=1 at $supply", buy <= sell)
        }
    }

    // ── Supply cycle invariant ───────────────────────────────────────────────

    @Test
    fun buyingToScarceAndSellingBackToAbundantRestoresOriginalPrices() {
        // Start at Abundant, buy twice to reach Scarce, sell twice to return to Abundant
        val base = 20L
        val (originalSell, originalBuy) = MarketEngine.computePrices(base, SupplyLevel.Abundant)

        // Simulate two buys: Abundant → Normal → Scarce
        val afterFirstBuy = MarketEngine.decreaseSupply(SupplyLevel.Abundant)
        val afterSecondBuy = MarketEngine.decreaseSupply(afterFirstBuy)
        assertEquals(SupplyLevel.Scarce, afterSecondBuy)

        // Simulate two sells: Scarce → Normal → Abundant
        val afterFirstSell = MarketEngine.increaseSupply(afterSecondBuy)
        val afterSecondSell = MarketEngine.increaseSupply(afterFirstSell)
        assertEquals(SupplyLevel.Abundant, afterSecondSell)

        // Prices should be back to original
        val (restoredSell, restoredBuy) = MarketEngine.computePrices(base, afterSecondSell)
        assertEquals(
            "Sell price should be restored after full buy/sell cycle",
            originalSell,
            restoredSell,
        )
        assertEquals(
            "Buy price should be restored after full buy/sell cycle",
            originalBuy,
            restoredBuy,
        )
    }

    @Test
    fun supplyClampAtScarceDoesNotChangePriceOnRepeatedDecrease() {
        // Decreasing supply when already Scarce should keep prices the same
        val base = 20L
        val (scarceSell, scarceBuy) = MarketEngine.computePrices(base, SupplyLevel.Scarce)

        val stillScarce = MarketEngine.decreaseSupply(SupplyLevel.Scarce)
        val (clampedSell, clampedBuy) = MarketEngine.computePrices(base, stillScarce)

        assertEquals("Sell price should not change when clamped at Scarce", scarceSell, clampedSell)
        assertEquals("Buy price should not change when clamped at Scarce", scarceBuy, clampedBuy)
    }

    @Test
    fun supplyClampAtAbundantDoesNotChangePriceOnRepeatedIncrease() {
        // Increasing supply when already Abundant should keep prices the same
        val base = 20L
        val (abundantSell, abundantBuy) = MarketEngine.computePrices(base, SupplyLevel.Abundant)

        val stillAbundant = MarketEngine.increaseSupply(SupplyLevel.Abundant)
        val (clampedSell, clampedBuy) = MarketEngine.computePrices(base, stillAbundant)

        assertEquals("Sell price should not change when clamped at Abundant", abundantSell, clampedSell)
        assertEquals("Buy price should not change when clamped at Abundant", abundantBuy, clampedBuy)
    }

    // ── Multi-good independence ──────────────────────────────────────────────

    @Test
    fun changingSupplyForOneGoodDoesNotAffectAnotherGood() {
        // MarketEngine is stateless — supply changes for good A are independent of good B
        val baseB = 50L

        val (sellBBefore, buyBBefore) = MarketEngine.computePrices(baseB, SupplyLevel.Normal)

        // Simulate buying good A (supply decreases for A)
        val newSupplyA = MarketEngine.decreaseSupply(SupplyLevel.Normal)
        assertEquals(SupplyLevel.Scarce, newSupplyA)

        // Good B's prices are computed independently and remain unchanged
        val (sellBAfter, buyBAfter) = MarketEngine.computePrices(baseB, SupplyLevel.Normal)
        assertEquals(
            "Good B sell price should be unaffected by good A supply change",
            sellBBefore,
            sellBAfter,
        )
        assertEquals(
            "Good B buy price should be unaffected by good A supply change",
            buyBBefore,
            buyBAfter,
        )
    }

    // ── Spread invariant ─────────────────────────────────────────────────────

    @Test
    fun buyPriceIsStrictlyLessThanSellPriceForAllSupplyLevelsAndRepresentativeBaseValues() {
        // For base values large enough that sell price > 1, the spread ensures buy < sell.
        // (When sell price hits the floor of 1, buy price also floors to 1 and equals sell.)
        val representativeBases = listOf(5L, 10L, 18L, 20L, 30L, 100L, 1_000L)
        for (base in representativeBases) {
            for (supply in SupplyLevel.entries) {
                val (sell, buy) = MarketEngine.computePrices(base, supply)
                assertTrue(
                    "Buy price $buy should be strictly less than sell price $sell " +
                        "for base=$base supply=$supply",
                    buy < sell,
                )
            }
        }
    }

    @Test
    fun spreadIsAppliedConsistentlyAcrossAllSupplyLevels() {
        // The buy price should always be approximately (1 - SPREAD) * sell price
        val base = 100L
        for (supply in SupplyLevel.entries) {
            val sell = MarketEngine.computeSellPrice(base, supply)
            val buy = MarketEngine.computeBuyPrice(sell)
            val expectedBuy = (sell * (1.0 - MarketEngine.SPREAD)).toLong().coerceAtLeast(1L)
            assertEquals(
                "Buy price should match spread formula for supply=$supply",
                expectedBuy,
                buy,
            )
        }
    }
}

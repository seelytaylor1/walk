package com.wanderingledger.core.data

import com.wanderingledger.core.model.SupplyLevel

/**
 * Pure price-calculation and supply/demand logic for the market engine.
 *
 * All functions are stateless and deterministic — they take values in and return values out,
 * making them easy to unit-test without any Android or Room dependencies.
 */
object MarketEngine {

    // ── Price multipliers ────────────────────────────────────────────────────

    /** Multiplier applied to a good's base value when supply is scarce. */
    const val SCARCE_MULTIPLIER = 1.5

    /** Multiplier applied to a good's base value when supply is normal. */
    const val NORMAL_MULTIPLIER = 1.0

    /** Multiplier applied to a good's base value when supply is abundant. */
    const val ABUNDANT_MULTIPLIER = 0.7

    /**
     * Spread between the town's sell price (what the player pays) and buy price
     * (what the town pays the player). The town always buys at a discount.
     *
     * sell price = computed price
     * buy price  = sell price * (1 - SPREAD)
     */
    const val SPREAD = 0.35

    // ── Price calculation ────────────────────────────────────────────────────

    /**
     * Compute the sell price (what the town charges the player) for a good given its
     * base value and the current supply level.
     *
     * @param baseValue The good's canonical base value in gold.
     * @param supplyLevel Current supply state at this town.
     * @return Sell price in gold, always at least 1.
     */
    fun computeSellPrice(baseValue: Long, supplyLevel: SupplyLevel): Long {
        val multiplier = when (supplyLevel) {
            SupplyLevel.Scarce -> SCARCE_MULTIPLIER
            SupplyLevel.Normal -> NORMAL_MULTIPLIER
            SupplyLevel.Abundant -> ABUNDANT_MULTIPLIER
        }
        return (baseValue * multiplier).toLong().coerceAtLeast(1L)
    }

    /**
     * Compute the buy price (what the town pays the player) for a good.
     * Always lower than the sell price to represent the merchant's margin.
     *
     * @param sellPrice The computed sell price for this good at this town.
     * @return Buy price in gold, always at least 1 and always ≤ sellPrice.
     */
    fun computeBuyPrice(sellPrice: Long): Long =
        (sellPrice * (1.0 - SPREAD)).toLong().coerceAtLeast(1L).coerceAtMost(sellPrice)

    /**
     * Convenience overload that computes both prices from base value and supply level.
     *
     * @return Pair of (sellPrice, buyPrice).
     */
    fun computePrices(baseValue: Long, supplyLevel: SupplyLevel): Pair<Long, Long> {
        val sell = computeSellPrice(baseValue, supplyLevel)
        val buy = computeBuyPrice(sell)
        return sell to buy
    }

    // ── Supply/demand updates ────────────────────────────────────────────────

    /**
     * Decrease supply by one step when the player buys goods from a town.
     * Clamps at [SupplyLevel.Scarce] (the minimum).
     *
     * Abundant → Normal → Scarce
     */
    fun decreaseSupply(current: SupplyLevel): SupplyLevel = when (current) {
        SupplyLevel.Abundant -> SupplyLevel.Normal
        SupplyLevel.Normal -> SupplyLevel.Scarce
        SupplyLevel.Scarce -> SupplyLevel.Scarce // already at minimum
    }

    /**
     * Increase supply by one step when the player sells goods to a town.
     * Clamps at [SupplyLevel.Abundant] (the maximum).
     *
     * Scarce → Normal → Abundant
     */
    fun increaseSupply(current: SupplyLevel): SupplyLevel = when (current) {
        SupplyLevel.Scarce -> SupplyLevel.Normal
        SupplyLevel.Normal -> SupplyLevel.Abundant
        SupplyLevel.Abundant -> SupplyLevel.Abundant // already at maximum
    }

    /**
     * Clamp a raw integer supply ordinal to a valid [SupplyLevel].
     * 0 = Scarce, 1 = Normal, 2 = Abundant; values outside this range are clamped.
     */
    fun clampSupplyLevel(ordinal: Int): SupplyLevel = when {
        ordinal <= 0 -> SupplyLevel.Scarce
        ordinal >= 2 -> SupplyLevel.Abundant
        else -> SupplyLevel.Normal
    }
}

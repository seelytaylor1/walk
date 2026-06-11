package com.wanderingledger.feature.town

/**
 * Rarity levels for goods, affecting their visual emphasis.
 */
enum class GoodRarity {
    Common,
    Uncommon,
    Rare,
    Legendary,
}

/**
 * Profit potential levels for goods, indicating trade opportunities.
 */
enum class ProfitPotential {
    Low,
    Medium,
    High,
}

/**
 * Enhanced state for a good in the market, including presentation-only fields.
 */
data class MarketGoodItemState(
    val goodId: Long,
    val name: String,
    val buyPrice: Long,
    val sellPrice: Long,
    val supplyLevel: com.wanderingledger.core.model.SupplyLevel,
    val rarity: GoodRarity = GoodRarity.Common,
    val isContraband: Boolean = false,
    val playerQuantity: Int = 0,
    val canAfford: Boolean = true,
    val hasSpace: Boolean = true,
    val description: String = "",
    val profitPotential: ProfitPotential = ProfitPotential.Low,
    val hasRumorIndicator: Boolean = false,
    val recommendationReason: String? = null,
)

/**
 * Screen state for the new Compose-based Market Presentation.
 */
data class MarketPresentationState(
    val townName: String,
    val playerGold: Long,
    val inventoryUsed: Int,
    val inventoryCapacity: Int,
    val goods: List<MarketGoodItemState> = emptyList(),
)

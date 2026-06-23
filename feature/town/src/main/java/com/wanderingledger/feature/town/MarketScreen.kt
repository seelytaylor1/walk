package com.wanderingledger.feature.town

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.wanderingledger.core.data.MarketRow
import com.wanderingledger.core.data.MarketState
import com.wanderingledger.core.model.SupplyLevel

// ── Screen state ─────────────────────────────────────────────────────────────

/**
 * A single row in the market UI, derived from [MarketRow].
 */
data class MarketItemState(
    val goodId: Long,
    val goodName: String,
    val buyPrice: Long,
    val sellPrice: Long,
    val supplyLevel: SupplyLevel,
    val isContraband: Boolean,
    val playerQuantity: Int,
    val canAfford: Boolean,
    val canSell: Boolean,
)

/**
 * Full market screen state, derived from [MarketState].
 */
data class MarketScreenState(
    val townId: Long,
    val townName: String,
    val playerGold: Long,
    val playerInventoryUsed: Int,
    val playerInventoryCapacity: Int,
    val items: List<MarketItemState>,
    val message: String? = null,
)

fun buildMarketScreenState(
    marketState: MarketState,
    message: String? = null,
): MarketScreenState =
    MarketScreenState(
        townId = marketState.townId,
        townName = marketState.townName,
        playerGold = marketState.playerGold,
        playerInventoryUsed = marketState.playerInventoryUsed,
        playerInventoryCapacity = marketState.playerInventoryCapacity,
        items =
            marketState.rows.map { row ->
                MarketItemState(
                    goodId = row.good.goodId,
                    goodName = row.good.name,
                    buyPrice = row.townPrice.sellPrice, // "buy price" from player's perspective = town's sell price
                    sellPrice = row.townPrice.buyPrice, // "sell price" from player's perspective = town's buy price
                    supplyLevel = row.townPrice.supplyLevel,
                    isContraband = row.good.isContraband,
                    playerQuantity = row.playerQuantity,
                    canAfford = row.canAfford,
                    canSell = row.canSell,
                )
            },
        message = message,
    )

// ── Actions ───────────────────────────────────────────────────────────────────

fun interface BuyActionCallback {
    fun onBuy(goodId: Long)
}

fun interface SellActionCallback {
    fun onSell(goodId: Long)
}

fun interface MarketNavigationCallback {
    fun onNavigateBackToTown()
}

data class MarketActions(
    val onBuy: BuyActionCallback,
    val onSell: SellActionCallback,
    val onNavigateBackToTown: MarketNavigationCallback,
)

// ── View ──────────────────────────────────────────────────────────────────────

/**
 * View-based market screen for a town.
 *
 * Displays the list of tradeable goods with buy/sell prices, supply level,
 * player gold, and inventory usage. Buy and Sell buttons are disabled when
 * the player cannot afford or has no inventory to sell.
 */
class MarketScreenView(
    context: Context,
) : LinearLayout(context) {
    private val headerText =
        TextView(context).apply {
            textSize = 20f
            setPadding(0, 0, 0, 4)
        }
    private val playerStatusText =
        TextView(context).apply {
            textSize = 15f
            setPadding(0, 0, 0, 8)
        }
    private val messageText =
        TextView(context).apply {
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
    private val backButton =
        Button(context).apply {
            text = "Back to Town"
        }
    private val itemsContainer =
        LinearLayout(context).apply {
            orientation = VERTICAL
        }
    private val scrollView =
        ScrollView(context).apply {
            addView(
                itemsContainer,
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

    init {
        orientation = VERTICAL
        setPadding(48, 48, 48, 48)
        addView(
            headerText,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            playerStatusText,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            messageText,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(backButton)
        addView(
            scrollView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
    }

    fun render(
        state: MarketScreenState,
        actions: MarketActions,
    ) {
        headerText.text = "${state.townName} — Market"
        playerStatusText.text = state.toPlayerStatusText()
        messageText.text = state.message ?: ""
        messageText.visibility = if (state.message != null) VISIBLE else GONE
        backButton.setOnClickListener { actions.onNavigateBackToTown.onNavigateBackToTown() }

        itemsContainer.removeAllViews()

        if (state.items.isEmpty()) {
            itemsContainer.addView(
                TextView(context).apply {
                    text = "No goods available at this market."
                    textSize = 15f
                    setPadding(0, 24, 0, 0)
                },
            )
            return
        }

        state.items.forEach { item ->
            itemsContainer.addView(buildItemRow(item, actions))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildItemRow(
        item: MarketItemState,
        actions: MarketActions,
    ): LinearLayout {
        val row =
            LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(0, 16, 0, 16)
            }

        // Good name + supply + contraband indicator
        val nameText =
            TextView(context).apply {
                text = item.toNameLine()
                textSize = 16f
            }

        // Price info
        val priceText =
            TextView(context).apply {
                text = item.toPriceLine()
                textSize = 14f
                setPadding(0, 4, 0, 4)
            }

        // Inventory info
        val inventoryText =
            TextView(context).apply {
                text = "You own: ${item.playerQuantity}"
                textSize = 13f
                setPadding(0, 0, 0, 8)
            }

        // Buy / Sell buttons side by side
        val buttonRow =
            LinearLayout(context).apply {
                orientation = HORIZONTAL
            }

        val buyButton =
            Button(context).apply {
                text = "Buy (${item.buyPrice}g)"
                isEnabled = item.canAfford
                setOnClickListener { actions.onBuy.onBuy(item.goodId) }
            }

        val sellButton =
            Button(context).apply {
                text = "Sell (${item.sellPrice}g)"
                isEnabled = item.canSell
                setOnClickListener { actions.onSell.onSell(item.goodId) }
            }

        buttonRow.addView(
            buyButton,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            },
        )
        buttonRow.addView(
            sellButton,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )

        // Divider
        val divider =
            TextView(context).apply {
                text = "─────────────────────────────"
                textSize = 10f
                setPadding(0, 8, 0, 0)
            }

        row.addView(nameText)
        row.addView(priceText)
        row.addView(inventoryText)
        row.addView(buttonRow)
        row.addView(divider)

        return row
    }

    private fun MarketScreenState.toPlayerStatusText(): String =
        buildString {
            append("Gold: ${playerGold}g")
            append("  |  ")
            append("Inventory: $playerInventoryUsed / $playerInventoryCapacity")
        }

    private fun MarketItemState.toNameLine(): String =
        buildString {
            append(goodName)
            append("  [${supplyLevel.toDisplayLabel()}]")
            if (isContraband) append("  ⚠ Contraband")
        }

    private fun MarketItemState.toPriceLine(): String = "Buy: ${buyPrice}g  •  Sell: ${sellPrice}g"

    private fun SupplyLevel.toDisplayLabel(): String =
        when (this) {
            SupplyLevel.Scarce -> "Scarce"
            SupplyLevel.Normal -> "Normal"
            SupplyLevel.Abundant -> "Abundant"
        }
}

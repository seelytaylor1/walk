package com.wanderingledger.feature.town

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.wanderingledger.core.data.InventoryRow
import com.wanderingledger.core.data.InventorySummary

// ── Screen state ──────────────────────────────────────────────────────────────

/**
 * A single row in the inventory UI, derived from [InventoryRow].
 */
data class InventoryItemRowState(
    val goodId: Long,
    val goodName: String,
    val quantity: Int,
    val baseValue: Long,
    val isContraband: Boolean,
    val isSealed: Boolean,
)

/**
 * Full inventory screen state, derived from [InventorySummary].
 */
data class InventoryScreenState(
    val playerGold: Long,
    val totalItemsCarried: Int,
    val inventoryCapacity: Int,
    val rows: List<InventoryItemRowState>,
)

fun buildInventoryScreenState(summary: InventorySummary): InventoryScreenState =
    InventoryScreenState(
        playerGold = summary.gold,
        totalItemsCarried = summary.totalItemsCarried,
        inventoryCapacity = summary.inventoryCapacity,
        rows =
            summary.rows.map { row ->
                InventoryItemRowState(
                    goodId = row.good.goodId,
                    goodName = row.good.name,
                    quantity = row.item.quantity,
                    baseValue = row.good.baseValue,
                    isContraband = row.good.isContraband,
                    isSealed = row.item.isSealed,
                )
            },
    )

// ── Actions ───────────────────────────────────────────────────────────────────

fun interface InventoryNavigationCallback {
    fun onNavigateBackToTown()
}

fun interface InventorySellCallback {
    fun onSellItem(goodId: Long)
}

data class InventoryActions(
    val onNavigateBackToTown: InventoryNavigationCallback,
    val onSellItem: InventorySellCallback,
)

// ── View ──────────────────────────────────────────────────────────────────────

/**
 * View-based inventory screen.
 *
 * Displays the player's carried goods with name, quantity, base value, and
 * contraband/sealed indicators. Shows total inventory used / capacity and
 * player gold. Each row has a Sell button that delegates to [InventoryActions].
 */
class InventoryScreenView(
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
            setPadding(0, 0, 0, 16)
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
        state: InventoryScreenState,
        actions: InventoryActions,
    ) {
        headerText.text = "Inventory"
        playerStatusText.text = state.toPlayerStatusText()
        backButton.setOnClickListener { actions.onNavigateBackToTown.onNavigateBackToTown() }

        itemsContainer.removeAllViews()

        if (state.rows.isEmpty()) {
            itemsContainer.addView(
                TextView(context).apply {
                    text = "Your pack is empty."
                    textSize = 15f
                    setPadding(0, 24, 0, 0)
                },
            )
            return
        }

        state.rows.forEach { row ->
            itemsContainer.addView(buildItemRow(row, actions))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildItemRow(
        row: InventoryItemRowState,
        actions: InventoryActions,
    ): LinearLayout {
        val container =
            LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(0, 16, 0, 16)
            }

        // Good name + indicators
        val nameText =
            TextView(context).apply {
                text = row.toNameLine()
                textSize = 16f
            }

        // Quantity and base value
        val detailText =
            TextView(context).apply {
                text = row.toDetailLine()
                textSize = 14f
                setPadding(0, 4, 0, 8)
            }

        // Sell button — disabled for sealed items
        val sellButton =
            Button(context).apply {
                text = "Sell (${row.baseValue}g base)"
                isEnabled = !row.isSealed
                setOnClickListener { actions.onSellItem.onSellItem(row.goodId) }
            }

        // Divider
        val divider =
            TextView(context).apply {
                text = "─────────────────────────────"
                textSize = 10f
                setPadding(0, 8, 0, 0)
            }

        container.addView(nameText)
        container.addView(detailText)
        container.addView(sellButton)
        container.addView(divider)

        return container
    }

    private fun InventoryScreenState.toPlayerStatusText(): String =
        buildString {
            append("Gold: ${playerGold}g")
            append("  |  ")
            append("Inventory: $totalItemsCarried / $inventoryCapacity")
        }

    private fun InventoryItemRowState.toNameLine(): String =
        buildString {
            append(goodName)
            if (isContraband) append("  ⚠ Contraband")
            if (isSealed) append("  🔒 Sealed")
        }

    private fun InventoryItemRowState.toDetailLine(): String = "Qty: $quantity  •  Base value: ${baseValue}g each"
}

package com.wanderingledger.feature.town

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.wanderingledger.core.model.Town

data class TownScreenState(
    val townName: String,
    val townRegion: String,
    val reputation: Int,
    val storyState: String,
    val bankedSteps: Long,
    val gold: Long,
    val message: String? = null,
)

fun interface TownNavigationCallback {
    fun onNavigateToWorldMap()
}

fun interface TownMarketCallback {
    fun onOpenMarket()
}

fun interface TownInventoryCallback {
    fun onOpenInventory()
}

fun interface TownLedgerCallback {
    fun onOpenLedger()
}

fun interface TownChronicleCallback {
    fun onOpenChronicle()
}

fun interface TownCompanionsCallback {
    fun onOpenCompanions()
}

fun interface TownSettingsCallback {
    fun onOpenSettings()
}

data class TownActions(
    val onNavigateToWorldMap: TownNavigationCallback,
    val onOpenMarket: TownMarketCallback,
    val onOpenInventory: TownInventoryCallback,
    val onOpenLedger: TownLedgerCallback,
    val onOpenChronicle: TownChronicleCallback,
    val onOpenCompanions: TownCompanionsCallback,
    val onOpenSettings: TownSettingsCallback,
)

fun buildTownScreenState(
    town: Town,
    bankedSteps: Long,
    gold: Long,
    message: String? = null,
): TownScreenState =
    TownScreenState(
        townName = town.name,
        townRegion = town.region,
        reputation = town.reputation,
        storyState = town.storyState,
        bankedSteps = bankedSteps,
        gold = gold,
        message = message,
    )

class TownScreenView(context: Context) : LinearLayout(context) {
    private val headerText = TextView(context).apply {
        textSize = 22f
        setPadding(0, 0, 0, 8)
    }
    private val detailsText = TextView(context).apply {
        textSize = 16f
        setPadding(0, 0, 0, 24)
    }
    private val messageText = TextView(context).apply {
        textSize = 15f
        setPadding(0, 0, 0, 24)
    }
    private val worldMapButton = Button(context).apply {
        text = "View World Map"
    }
    private val marketButton = Button(context).apply {
        text = "Visit Market"
    }
    private val inventoryButton = Button(context).apply {
        text = "View Inventory"
    }
    private val ledgerButton = Button(context).apply {
        text = "Open Ledger"
    }
    private val chronicleButton = Button(context).apply {
        text = "Open Chronicle"
    }
    private val companionsButton = Button(context).apply {
        text = "Companions"
    }
    private val settingsButton = Button(context).apply {
        text = "Settings"
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
            detailsText,
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
        addView(worldMapButton)
        addView(
            marketButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16
            },
        )
        addView(
            inventoryButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16
            },
        )
        addView(
            ledgerButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16
            },
        )
        addView(
            chronicleButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16
            },
        )
        addView(
            companionsButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16
            },
        )
        addView(
            settingsButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16
            },
        )
    }

    fun render(state: TownScreenState, actions: TownActions) {
        headerText.text = state.toHeaderText()
        detailsText.text = state.toDetailsText()
        messageText.text = state.message ?: ""
        messageText.visibility = if (state.message != null) VISIBLE else GONE
        worldMapButton.setOnClickListener { actions.onNavigateToWorldMap.onNavigateToWorldMap() }
        marketButton.setOnClickListener { actions.onOpenMarket.onOpenMarket() }
        inventoryButton.setOnClickListener { actions.onOpenInventory.onOpenInventory() }
        ledgerButton.setOnClickListener { actions.onOpenLedger.onOpenLedger() }
        chronicleButton.setOnClickListener { actions.onOpenChronicle.onOpenChronicle() }
        companionsButton.setOnClickListener { actions.onOpenCompanions.onOpenCompanions() }
        settingsButton.setOnClickListener { actions.onOpenSettings.onOpenSettings() }
    }

    private fun TownScreenState.toHeaderText(): String =
        "You have arrived in $townName"

    private fun TownScreenState.toDetailsText(): String = buildString {
        appendLine("Region: $townRegion")
        appendLine("Reputation: $reputation / 100")
        appendLine("Status: $storyState")
        appendLine()
        appendLine("Banked steps: $bankedSteps")
        appendLine("Gold: $gold")
    }
}

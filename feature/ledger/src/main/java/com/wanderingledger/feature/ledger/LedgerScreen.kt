package com.wanderingledger.feature.ledger

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.wanderingledger.core.model.Rumor

data class LedgerScreenState(
    val activeRumors: List<Rumor>,
    val message: String? = null,
)

fun interface LedgerNavigationCallback {
    fun onNavigateBack()
}

data class LedgerActions(
    val onNavigateBack: LedgerNavigationCallback,
)

fun buildLedgerScreenState(
    rumors: List<Rumor>,
    message: String? = null,
): LedgerScreenState =
    LedgerScreenState(
        activeRumors = rumors,
        message = message,
    )

class LedgerScreenView(context: Context) : LinearLayout(context) {
    private val headerText = TextView(context).apply {
        textSize = 24f
        setPadding(0, 0, 0, 16)
        text = "The Ledger"
    }
    
    private val rumorsContainer = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    private val backButton = Button(context).apply {
        text = "Back"
    }

    init {
        orientation = VERTICAL
        setPadding(48, 48, 48, 48)
        
        addView(headerText)
        
        // Add a scroll view if needed, but for now just a container
        addView(
            rumorsContainer,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f // Weight 1 to take available space
            )
        )
        
        addView(
            backButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
        )
    }

    fun render(state: LedgerScreenState, actions: LedgerActions) {
        rumorsContainer.removeAllViews()
        
        if (state.activeRumors.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "No active rumors in your ledger."
                textSize = 16f
                setPadding(0, 32, 0, 0)
            }
            rumorsContainer.addView(emptyText)
        } else {
            state.activeRumors.forEach { rumor ->
                val rumorView = TextView(context).apply {
                    text = "• ${rumor.text} (${rumor.expiryVisitsLeft} visits left)"
                    textSize = 16f
                    setPadding(0, 8, 0, 8)
                }
                rumorsContainer.addView(rumorView)
            }
        }
        
        backButton.setOnClickListener { actions.onNavigateBack.onNavigateBack() }
    }
}

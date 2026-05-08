package com.wanderingledger.feature.companions

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.wanderingledger.core.model.Companion

const val MAX_BOND_LEVEL_UI = 5

data class CompanionsScreenState(
    val activeCompanions: List<Companion>,
    val recruitableCompanions: List<Companion>,
    val message: String? = null,
)

fun interface CompanionNavigationCallback {
    fun onNavigateBack()
}

fun interface CompanionRecruitCallback {
    fun onRecruit(companionId: Long)
}

fun interface CompanionInteractCallback {
    fun onInteract(companionId: Long)
}

data class CompanionsActions(
    val onNavigateBack: CompanionNavigationCallback,
    val onRecruit: CompanionRecruitCallback,
    val onInteract: CompanionInteractCallback,
)

fun buildCompanionsScreenState(
    active: List<Companion>,
    recruitable: List<Companion>,
    message: String? = null,
): CompanionsScreenState =
    CompanionsScreenState(
        activeCompanions = active,
        recruitableCompanions = recruitable,
        message = message,
    )

private fun formatBondLevel(level: Int): String {
    val stars = "★".repeat(level) + "☆".repeat(MAX_BOND_LEVEL_UI - level)
    return "$stars ($level/$MAX_BOND_LEVEL_UI)"
}

class CompanionsScreenView(context: Context) : LinearLayout(context) {
    private val headerText = TextView(context).apply {
        textSize = 24f
        setPadding(0, 0, 0, 16)
        text = "Companions"
    }

    private val messageText = TextView(context).apply {
        textSize = 15f
        setPadding(0, 0, 0, 16)
    }

    private val activeHeader = TextView(context).apply {
        textSize = 20f
        setPadding(0, 16, 0, 8)
        text = "Your Party"
    }

    private val activeContainer = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    private val recruitableHeader = TextView(context).apply {
        textSize = 20f
        setPadding(0, 16, 0, 8)
        text = "Available in Town"
    }

    private val recruitableContainer = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    private val backButton = Button(context).apply {
        text = "Back"
    }

    init {
        orientation = VERTICAL
        setPadding(48, 48, 48, 48)
        
        addView(headerText)
        addView(messageText)
        addView(activeHeader)
        addView(activeContainer)
        addView(recruitableHeader)
        addView(recruitableContainer)
        
        addView(
            backButton,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
        )
    }

    fun render(state: CompanionsScreenState, actions: CompanionsActions) {
        messageText.text = state.message ?: ""
        messageText.visibility = if (state.message != null) VISIBLE else GONE

        activeContainer.removeAllViews()
        if (state.activeCompanions.isEmpty()) {
            activeContainer.addView(TextView(context).apply { text = "You are traveling alone." })
        } else {
            state.activeCompanions.forEach { companion ->
                val row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    setPadding(0, 8, 0, 8)
                }
                val info = TextView(context).apply {
                    text = "${companion.name} (${companion.role})\nBond: ${formatBondLevel(companion.bondLevel)}\nPower: ${companion.combatPower}"
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val interactBtn = Button(context).apply {
                    text = "Talk"
                    setOnClickListener { actions.onInteract.onInteract(companion.companionId) }
                }
                row.addView(info)
                row.addView(interactBtn)
                activeContainer.addView(row)
            }
        }

        recruitableContainer.removeAllViews()
        if (state.recruitableCompanions.isEmpty()) {
            recruitableContainer.addView(TextView(context).apply { text = "No one here is looking for a group." })
        } else {
            state.recruitableCompanions.forEach { companion ->
                val row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    setPadding(0, 8, 0, 8)
                }
                val info = TextView(context).apply {
                    text = "${companion.name} (${companion.role})"
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val recruitBtn = Button(context).apply {
                    text = "Recruit"
                    setOnClickListener { actions.onRecruit.onRecruit(companion.companionId) }
                }
                row.addView(info)
                row.addView(recruitBtn)
                recruitableContainer.addView(row)
            }
        }

        backButton.setOnClickListener { actions.onNavigateBack.onNavigateBack() }
    }
}

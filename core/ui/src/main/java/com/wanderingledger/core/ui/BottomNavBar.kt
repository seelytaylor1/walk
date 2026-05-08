package com.wanderingledger.core.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.wanderingledger.core.designsystem.R

class BottomNavBar(
    context: Context,
    private val onNavigate: (Destination) -> Unit,
) : LinearLayout(context) {

    enum class Destination {
        WORLD_MAP,
        TOWN,
        LEDGER,
        COMPANIONS,
    }

    private var currentDestination: Destination = Destination.WORLD_MAP
    private val items = mutableListOf<Pair<String, Destination>>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, 24, 0, 24)
        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_light))
    }

    fun render(activeDestination: Destination) {
        currentDestination = activeDestination
        removeAllViews()

        items.clear()
        items.add(Pair("Map", Destination.WORLD_MAP))
        items.add(Pair("Town", Destination.TOWN))
        items.add(Pair("Ledger", Destination.LEDGER))
        items.add(Pair("Party", Destination.COMPANIONS))

        for ((label, dest) in items) {
            val button = TextView(context)
            button.text = label
            button.textSize = 14f
            button.gravity = Gravity.CENTER
            button.setPadding(32, 16, 32, 16)

            val textColor = if (dest == activeDestination) {
                ContextCompat.getColor(context, R.color.primary_light)
            } else {
                ContextCompat.getColor(context, R.color.on_surface_light)
            }
            button.setTextColor(textColor)

            if (dest == activeDestination) {
                button.typeface = Typeface.DEFAULT_BOLD
            }

            button.setOnClickListener {
                onNavigate(dest)
            }
            addView(button)
        }
    }

    fun setDestination(destination: Destination) {
        render(destination)
    }
}
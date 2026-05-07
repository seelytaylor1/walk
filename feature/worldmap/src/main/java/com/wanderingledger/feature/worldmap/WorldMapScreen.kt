package com.wanderingledger.feature.worldmap

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.wanderingledger.core.data.TravelRoute
import com.wanderingledger.core.model.PlayerState
import com.wanderingledger.core.model.Town

data class WorldMapRouteOption(
    val segmentId: Long,
    val destinationName: String,
    val stepCost: Int,
    val narrativeDistance: String,
    val shortfall: Long,
) {
    val isEnabled: Boolean = shortfall == 0L
}

data class WorldMapScreenState(
    val currentTownName: String,
    val currentTownRegion: String,
    val bankedSteps: Long,
    val lifetimeSteps: Long,
    val routes: List<WorldMapRouteOption>,
    val message: String? = null,
)

fun interface TravelActionCallback {
    fun onTravel(segmentId: Long)
}

data class WorldMapActions(
    val onTravel: TravelActionCallback,
    val onSimulateSteps: () -> Unit,
)

fun buildWorldMapScreenState(
    playerState: PlayerState,
    currentTown: Town,
    routes: List<TravelRoute>,
    message: String? = null,
): WorldMapScreenState =
    WorldMapScreenState(
        currentTownName = currentTown.name,
        currentTownRegion = currentTown.region,
        bankedSteps = playerState.bankedSteps,
        lifetimeSteps = playerState.lifetimeSteps,
        routes = routes.map { route ->
            WorldMapRouteOption(
                segmentId = route.segment.segmentId,
                destinationName = route.destination.name,
                stepCost = route.segment.stepCost,
                narrativeDistance = route.segment.narrativeDistance,
                shortfall = (route.segment.stepCost.toLong() - playerState.bankedSteps).coerceAtLeast(0),
            )
        },
        message = message,
    )

class WorldMapScreenView(context: Context) : LinearLayout(context) {
    private val statusText = TextView(context).apply {
        textSize = 18f
        setPadding(0, 0, 0, 24)
    }
    private val simulateButton = Button(context).apply {
        text = "Simulate 75 steps"
    }
    private val routesContainer = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    init {
        orientation = VERTICAL
        setPadding(48, 48, 48, 48)
        addView(
            statusText,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(simulateButton)
        addView(routesContainer)
    }

    fun render(state: WorldMapScreenState, actions: WorldMapActions) {
        statusText.text = state.toDisplayText()
        simulateButton.setOnClickListener { actions.onSimulateSteps() }
        routesContainer.removeAllViews()

        if (state.routes.isEmpty()) {
            routesContainer.addView(
                TextView(context).apply {
                    text = "No roads leave this town yet."
                    textSize = 16f
                    setPadding(0, 24, 0, 0)
                },
            )
            return
        }

        state.routes.forEach { route ->
            routesContainer.addView(
                Button(context).apply {
                    text = route.toButtonText()
                    isEnabled = route.isEnabled
                    setOnClickListener { actions.onTravel.onTravel(route.segmentId) }
                },
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = 16
                },
            )
        }
    }

    private fun WorldMapScreenState.toDisplayText(): String = buildString {
        appendLine("Wandering Ledger")
        appendLine()
        appendLine("Town: $currentTownName")
        appendLine("Region: $currentTownRegion")
        appendLine("Banked steps: $bankedSteps")
        appendLine("Lifetime steps: $lifetimeSteps")
        message?.let {
            appendLine()
            appendLine(it)
        }
    }

    private fun WorldMapRouteOption.toButtonText(): String =
        if (isEnabled) {
            "Travel to $destinationName ($stepCost steps, $narrativeDistance)"
        } else {
            "Travel to $destinationName ($stepCost steps, need $shortfall more)"
        }
}

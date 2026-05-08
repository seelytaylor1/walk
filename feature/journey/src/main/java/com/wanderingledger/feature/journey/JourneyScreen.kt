package com.wanderingledger.feature.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.component.WLMessageOverlay
import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion

data class JourneyScreenState(
    val currentTownName: String,
    val currentTownRegion: String,
    val currentBiome: Biome,
    val bankedSteps: Long,
    val lifetimeSteps: Long,
    val routes: List<JourneyRouteOption>,
    val activeCompanions: List<Companion> = emptyList(),
    val weather: WeatherCondition = WeatherCondition.Clear,
    val timeOfDay: TimeOfDay = TimeOfDay.Day,
    val message: String? = null,
)

data class JourneyActions(
    val onTravel: (Long) -> Unit,
    val onSimulateSteps: () -> Unit
)

@Composable
fun JourneyScreen(
    state: JourneyScreenState,
    actions: JourneyActions,
    modifier: Modifier = Modifier
) {
    var currentMessage by remember { mutableStateOf(state.message) }

    LaunchedEffect(state.message) {
        currentMessage = state.message
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenHeight = maxHeight

        WLMessageOverlay(
            message = currentMessage,
            onDismiss = { currentMessage = null }
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            EnvironmentBackground(
                biome = state.currentBiome,
                activeCompanions = state.activeCompanions,
                weather = state.weather,
                timeOfDay = state.timeOfDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.35f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = state.currentTownName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = state.currentTownRegion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                JourneyStepMeter(
                    bankedSteps = state.bankedSteps,
                    maxSteps = 49000L,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Destinations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.routes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No roads leave this town yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.routes) { route ->
                            JourneyRouteCard(
                                route = route,
                                onTravel = { actions.onTravel(route.segmentId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun buildJourneyScreenState(
    currentTownName: String,
    currentTownRegion: String,
    currentBiome: Biome,
    bankedSteps: Long,
    lifetimeSteps: Long,
    routeDestinations: List<Triple<Long, String, Pair<Int, String>>>,
    activeCompanions: List<Companion> = emptyList(),
    weather: WeatherCondition = WeatherCondition.Clear,
    timeOfDay: TimeOfDay = TimeOfDay.Day,
    message: String? = null,
): JourneyScreenState = JourneyScreenState(
    currentTownName = currentTownName,
    currentTownRegion = currentTownRegion,
    currentBiome = currentBiome,
    bankedSteps = bankedSteps,
    lifetimeSteps = lifetimeSteps,
    routes = routeDestinations.map { (segmentId, destinationName, stepCostAndDistance) ->
        JourneyRouteOption(
            segmentId = segmentId,
            destinationName = destinationName,
            stepCost = stepCostAndDistance.first,
            narrativeDistance = stepCostAndDistance.second,
            bankedSteps = bankedSteps,
        )
    },
    activeCompanions = activeCompanions,
    weather = weather,
    timeOfDay = timeOfDay,
    message = message,
)
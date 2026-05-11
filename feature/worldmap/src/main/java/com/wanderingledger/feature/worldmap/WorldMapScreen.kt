package com.wanderingledger.feature.worldmap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.PlayerState
import com.wanderingledger.core.model.Town
import com.wanderingledger.core.data.TravelRoute
import androidx.compose.ui.geometry.Offset

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
    val mapLocations: List<MapLocation> = emptyList(),
    val mapRoutes: List<MapRoute> = emptyList(),
    val message: String? = null,
)

data class WorldMapActions(
    val onTravel: (Long) -> Unit,
    val onSimulateSteps: () -> Unit,
)

@Composable
fun WorldMapScreen(
    state: WorldMapScreenState,
    actions: WorldMapActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        WorldMapRenderer(
            locations = state.mapLocations,
            routes = state.mapRoutes,
            onLocationClick = { townId ->
                val route = state.routes.find { 
                    // This is a bit simplified, ideally we'd have a more robust way to link
                    // map locations to travel routes.
                    true 
                }
                // For now, if there's a route to this town, we could trigger it.
                // But usually you'd click a button in the UI or a specific route marker.
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "World Map",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "${state.currentTownName}, ${state.currentTownRegion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Steps: ${state.bankedSteps}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            if (state.message != null) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

// Coordinate mapping for the vertical slice towns
private val TownCoordinates = mapOf(
    1L to Offset(0.3f, 0.4f), // Hearthwick
    2L to Offset(0.7f, 0.2f), // Stoneford
    3L to Offset(0.6f, 0.7f), // Mistfall
)

fun buildWorldMapScreenState(
    playerState: PlayerState,
    currentTown: Town,
    allTowns: List<Town>,
    availableRoutes: List<TravelRoute>,
    allRoads: List<com.wanderingledger.core.model.RoadSegment>,
    message: String? = null,
): WorldMapScreenState {
    val mapLocations = allTowns.map { town ->
        MapLocation(
            townId = town.townId,
            name = town.name,
            offset = TownCoordinates[town.townId] ?: Offset(0.5f, 0.5f),
            isDiscovered = town.lastVisitedAt > 0,
            isCurrentLocation = town.townId == currentTown.townId
        )
    }

    val mapRoutes = allRoads.map { road ->
        MapRoute(
            fromId = road.fromTownId,
            toId = road.toTownId,
            fromOffset = TownCoordinates[road.fromTownId] ?: Offset(0.5f, 0.5f),
            toOffset = TownCoordinates[road.toTownId] ?: Offset(0.5f, 0.5f),
            isDiscovered = allTowns.find { it.townId == road.fromTownId }?.lastVisitedAt ?: 0 > 0 ||
                           allTowns.find { it.townId == road.toTownId }?.lastVisitedAt ?: 0 > 0
        )
    }

    return WorldMapScreenState(
        currentTownName = currentTown.name,
        currentTownRegion = currentTown.region,
        bankedSteps = playerState.bankedSteps,
        lifetimeSteps = playerState.lifetimeSteps,
        routes = availableRoutes.map { route ->
            WorldMapRouteOption(
                segmentId = route.segment.segmentId,
                destinationName = route.destination.name,
                stepCost = route.segment.stepCost,
                narrativeDistance = route.segment.narrativeDistance,
                shortfall = (route.segment.stepCost.toLong() - playerState.bankedSteps).coerceAtLeast(0),
            )
        },
        mapLocations = mapLocations,
        mapRoutes = mapRoutes,
        message = message,
    )
}

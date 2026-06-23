package com.wanderingledger.feature.town

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.component.WLClickableCard
import com.wanderingledger.core.designsystem.theme.WanderingLedgerTheme

/**
 * A screen for navigating between different districts in a town.
 */
@Composable
fun TownNavigationScreen(
    state: TownNavigationState,
    onDistrictClick: (District) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = state.townName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.districts) { district ->
                DistrictCard(
                    district = district,
                    onClick = { onDistrictClick(district) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TownNavigationScreenPreview() {
    WanderingLedgerTheme {
        TownNavigationScreen(
            state =
                TownNavigationState(
                    townName = "Oakhaven",
                    districts =
                        listOf(
                            District(
                                type = DistrictType.Market,
                                name = "Great Market",
                                description = "Trade goods and rumors",
                            ),
                            District(
                                type = DistrictType.Tavern,
                                name = "The Rusty Tankard",
                                description = "Meet companions and rest",
                            ),
                            District(
                                type = DistrictType.Archives,
                                name = "Grand Archives",
                                description = "View your ledger and history",
                            ),
                            District(
                                type = DistrictType.Gates,
                                name = "Town Gates",
                                description = "Return to the world map",
                            ),
                            District(
                                type = DistrictType.Treasury,
                                name = "Local Treasury",
                                description = "Manage your banked steps",
                                isAvailable = false,
                                unlockRequirement = "Requires 1000 steps",
                            ),
                            District(
                                type = DistrictType.Shrine,
                                name = "Whispering Shrine",
                                description = "Ancient secrets await",
                                isAvailable = false,
                                unlockRequirement = "Reputation: Respected",
                            ),
                        ),
                ),
            onDistrictClick = {},
        )
    }
}

/**
 * A card representing a town district.
 */
@Composable
fun DistrictCard(
    district: District,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon =
        district.icon ?: when (district.type) {
            DistrictType.Market -> Icons.Default.ShoppingCart
            DistrictType.Tavern -> Icons.Default.Person
            DistrictType.Archives -> Icons.Default.Menu
            DistrictType.Gates -> Icons.Default.LocationOn
            DistrictType.Treasury -> Icons.Default.Lock
            DistrictType.Shrine -> Icons.Default.Star
        }

    WLClickableCard(
        onClick = onClick,
        enabled = district.isAvailable,
        modifier = modifier.height(180.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint =
                    if (district.isAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = district.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (district.isAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
            )

            if (!district.isAvailable && district.unlockRequirement != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = district.unlockRequirement,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = district.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

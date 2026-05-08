package com.wanderingledger.feature.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.component.WLClickableCard
import com.wanderingledger.core.designsystem.theme.StepBankColor
import com.wanderingledger.core.designsystem.theme.WLTheme

private val AffordabilityRedTint = Color(0xFFFFEBEE)
private val AffordabilityGreenTint = Color(0xFFE8F5E9)

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

data class JourneyRouteOption(
    val segmentId: Long,
    val destinationName: String,
    val stepCost: Int,
    val narrativeDistance: String,
    val bankedSteps: Long,
) {
    val shortfall: Long get() = (stepCost.toLong() - bankedSteps).coerceAtLeast(0)
    val canAfford: Boolean get() = shortfall == 0L
}

@Composable
fun JourneyRouteCard(
    route: JourneyRouteOption,
    onTravel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val biomeColors = WLTheme.current.biomeColors
    val affordability = remember(route.stepCost, route.bankedSteps) {
        (route.bankedSteps.toFloat() / route.stepCost.toFloat()).coerceIn(0f, 1f)
    }
    val affordabilityTint = lerpColor(AffordabilityRedTint, AffordabilityGreenTint, affordability)

    WLClickableCard(
        onClick = onTravel,
        enabled = route.canAfford,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(affordabilityTint, affordabilityTint.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        containerColor = if (route.canAfford) {
            biomeColors.surface.copy(alpha = 0.95f)
        } else {
            biomeColors.surface.copy(alpha = 0.7f)
        },
        contentColor = if (route.canAfford) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (route.canAfford) {
                            biomeColors.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (route.canAfford) {
                        biomeColors.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.destinationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (route.canAfford) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = route.narrativeDistance,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${route.stepCost}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (route.canAfford) {
                        StepBankColor
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                Text(
                    text = "steps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!route.canAfford) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Need ${route.shortfall} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun JourneyRouteCardCompact(
    route: JourneyRouteOption,
    onTravel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val biomeColors = WLTheme.current.biomeColors
    val affordability = remember(route.stepCost, route.bankedSteps) {
        (route.bankedSteps.toFloat() / route.stepCost.toFloat()).coerceIn(0f, 1f)
    }
    val affordabilityTint = lerpColor(AffordabilityRedTint, AffordabilityGreenTint, affordability)

    WLClickableCard(
        onClick = onTravel,
        enabled = route.canAfford,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(affordabilityTint, affordabilityTint.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        containerColor = biomeColors.surface.copy(alpha = 0.9f),
        tonalElevation = 2.dp,
        shadowElevation = if (route.canAfford) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = route.destinationName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${route.stepCost} steps",
                style = MaterialTheme.typography.labelMedium,
                color = if (route.canAfford) {
                    StepBankColor
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
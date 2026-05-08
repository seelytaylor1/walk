package com.wanderingledger.feature.journey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.theme.WLTheme
import com.wanderingledger.core.designsystem.theme.StepBankColor
import androidx.compose.animation.core.Spring

@Composable
fun JourneyStepMeter(
    bankedSteps: Long,
    maxSteps: Long,
    modifier: Modifier = Modifier
) {
    val progress = (bankedSteps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "step_progress"
    )

    val biomeColors = WLTheme.current.biomeColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Travel Energy",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                biomeColors.tertiary.copy(alpha = 0.3f),
                                biomeColors.tertiary.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    StepBankColor,
                                    biomeColors.primary
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            StepPulseWrapper(
                bankedSteps = bankedSteps,
                modifier = Modifier
            ) {
                Text(
                    text = "$bankedSteps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = biomeColors.primary
                )
            }

            Text(
                text = " / $maxSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun JourneyStepMeterCompact(
    bankedSteps: Long,
    stepCost: Int,
    modifier: Modifier = Modifier
) {
    val canAfford = bankedSteps >= stepCost
    val biomeColors = WLTheme.current.biomeColors

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (canAfford) {
                    biomeColors.tertiary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (canAfford) StepBankColor
                    else MaterialTheme.colorScheme.error
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (canAfford) {
                "Ready to travel"
            } else {
                "Need ${stepCost - bankedSteps} more"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (canAfford) biomeColors.primary else MaterialTheme.colorScheme.error
        )
    }
}
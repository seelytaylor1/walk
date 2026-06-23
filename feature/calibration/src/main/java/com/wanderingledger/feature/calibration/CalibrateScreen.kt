package com.wanderingledger.feature.calibration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.component.WLButton
import com.wanderingledger.core.designsystem.component.WLCard
import com.wanderingledger.core.designsystem.component.WLOutlinedButton
import com.wanderingledger.core.designsystem.component.WLTextButton
import com.wanderingledger.core.designsystem.theme.Spacing
import kotlinx.coroutines.delay

enum class CalibrationMode { Auto, Manual }

data class CalibrationScreenState(
    val isRunning: Boolean = false,
    val detectedSteps: Int = 0,
    val sensitivity: Float = 1.0f,
    val confidence: String = "unknown",
    val lastCalibratedAt: Long? = null,
    val stepCounterAvailable: Boolean = false,
)

interface CalibrationActions {
    fun startAuto()

    fun stopAuto()

    fun adjustSensitivity(value: Float)

    fun saveCalibration()

    fun skipCalibration()
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun CalibrateScreen(
    state: CalibrationScreenState,
    onActions: CalibrationActions,
    onBack: () -> Unit,
) {
    var mode by remember { mutableStateOf(CalibrationMode.Auto) }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    var autoComplete by remember { mutableStateOf(false) }

    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            elapsedSeconds = 0f
            autoComplete = false
            while (elapsedSeconds < 30f) {
                delay(1000)
                elapsedSeconds++
            }
            autoComplete = true
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = "Step Calibration",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (state.lastCalibratedAt != null) {
            Text(
                text = "Last calibrated: ${formatTimestamp(state.lastCalibratedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!state.stepCounterAvailable) {
            WLCard {
                Text(
                    text =
                        "Hardware step counter not available. " +
                            "Using motion fallback. Adjust sensitivity to match your gait.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        WLCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(
                    text = "Walk naturally for 30 seconds",
                    style = MaterialTheme.typography.titleMedium,
                )

                AnimatedVisibility(visible = state.isRunning, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "${30 - elapsedSeconds.toInt()} seconds remaining",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Steps detected: ${state.detectedSteps}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                AnimatedVisibility(visible = autoComplete && !state.isRunning, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        Text(
                            text = "Confidence: ${state.confidence}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                AnimatedVisibility(visible = !state.isRunning && !autoComplete, enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        WLButton(
                            text = "Start (Auto)",
                            onClick = { onActions.startAuto() },
                            modifier = Modifier.weight(1f),
                        )
                        WLOutlinedButton(
                            text = "Manual",
                            onClick = { mode = CalibrationMode.Manual },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = mode == CalibrationMode.Manual || state.isRunning) {
            WLCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Text(
                        text = "Sensitivity",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Adjust lower if steps are over-counted; higher if under-counted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Low")
                        Slider(
                            value = state.sensitivity,
                            onValueChange = { onActions.adjustSensitivity(it) },
                            valueRange = 0.5f..1.5f,
                            steps = 9,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Text("High")
                    }
                    Text(
                        text = "Current: ${"%.2f".format(state.sensitivity)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            WLTextButton(text = "Skip", onClick = { onActions.skipCalibration() })
            Spacer(modifier = Modifier.width(Spacing.medium))
            WLButton(
                text = "Save Calibration",
                onClick = { onActions.saveCalibration() },
                enabled = autoComplete || mode == CalibrationMode.Manual,
            )
        }

        WLOutlinedButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val days = diff / (1000 * 60 * 60 * 24)
    val hours = diff / (1000 * 60 * 60)
    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        else -> "just now"
    }
}

package com.wanderingledger.feature.journey

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
fun rememberStepAnimationState(
    bankedSteps: Long,
    modifier: Modifier = Modifier,
): Animatable<Float, *> {
    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(1f) }
    var previousSteps by remember { mutableLongStateOf(bankedSteps) }

    LaunchedEffect(bankedSteps) {
        if (bankedSteps > previousSteps) {
            scope.launch {
                scale.animateTo(
                    targetValue = 1.03f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                )
            }
        }
        previousSteps = bankedSteps
    }

    return scale
}

fun stepSpringAnimationSpec(reducedMotion: Boolean) =
    if (reducedMotion) {
        tween<Float>(durationMillis = 300)
    } else {
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )
    }

@Composable
fun StepPulseWrapper(
    bankedSteps: Long,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (reducedMotion) {
        // Skip the scale animation entirely — just render content at 1:1
        Box(modifier = modifier) { content() }
        return
    }

    val scale = rememberStepAnimationState(bankedSteps)

    Box(
        modifier =
            modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        content()
    }
}

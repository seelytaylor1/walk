package com.wanderingledger.feature.journey

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.wanderingledger.core.model.Biome

@Composable
fun TimeOfDayTint(
    timeOfDay: TimeOfDay,
    modifier: Modifier = Modifier,
) {
    val tintColor by animateColorAsState(
        targetValue = timeOfDay.tintColor,
        animationSpec = tween(durationMillis = 1000),
        label = "time_tint",
    )

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(tintColor),
        )

        if (timeOfDay.vignetteIntensity > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = timeOfDay.vignetteIntensity),
                                    ),
                            ),
                        ),
            )
        }
    }
}

@Composable
fun TransitionOverlay(
    fromBiome: Biome,
    toBiome: Biome,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val fromColors = getBiomeGradientColors(fromBiome)
    val toColors = getBiomeGradientColors(toBiome)

    val blendedTop = lerpColor(fromColors.first, toColors.first, progress)
    val blendedBottom = lerpColor(fromColors.second, toColors.second, progress)

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(blendedTop, blendedBottom),
                        ),
                    ),
        )
    }
}

private fun lerpColor(
    from: Color,
    to: Color,
    fraction: Float,
): Color =
    Color(
        red = from.red + (to.red - from.red) * fraction,
        green = from.green + (to.green - from.green) * fraction,
        blue = from.blue + (to.blue - from.blue) * fraction,
        alpha = from.alpha + (to.alpha - from.alpha) * fraction,
    )

private fun getBiomeGradientColors(biome: Biome): Pair<Color, Color> =
    when (biome) {
        Biome.Forest -> Color(0xFF81C784) to Color(0xFF388E3C)
        Biome.Mountain -> Color(0xFF90A4AE) to Color(0xFF546E7A)
        Biome.Swamp -> Color(0xFF80CBC4) to Color(0xFF00695C)
        Biome.Coast -> Color(0xFF81D4FA) to Color(0xFF0277BD)
        else -> Color(0xFF81C784) to Color(0xFF388E3C)
    }

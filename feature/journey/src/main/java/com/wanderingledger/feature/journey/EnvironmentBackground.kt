package com.wanderingledger.feature.journey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.theme.WLTheme
import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion

private const val BIOME_TRANSITION_DURATION = 500

@Composable
fun EnvironmentBackground(
    biome: Biome,
    activeCompanions: List<Companion> = emptyList(),
    weather: WeatherCondition = WeatherCondition.Clear,
    timeOfDay: TimeOfDay = TimeOfDay.Day,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val biomeColors = WLTheme.current.biomeColors

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            biomeColors.background,
                            biomeColors.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        ParallaxLayer(
            depth = ParallaxDepth.Background,
            biome = biome,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = 20.dp)
        )

        ParallaxLayer(
            depth = ParallaxDepth.Midground,
            biome = biome,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = 40.dp)
        )

        WeatherOverlay(
            weather = weather,
            modifier = Modifier.fillMaxSize()
        )

        TimeOfDayTint(
            timeOfDay = timeOfDay,
            modifier = Modifier.fillMaxSize()
        )

        if (weather == WeatherCondition.Fog) {
            FogOverlay(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            PartyRenderer(
                companions = activeCompanions,
                showPlayer = true,
                modifier = Modifier.offset(y = 60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\uD83D\uDCCD",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Journey",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        ParallaxLayer(
            depth = ParallaxDepth.Foreground,
            biome = biome,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
        )
    }
}

private val MaterialTheme = androidx.compose.material3.MaterialTheme

@Composable
private fun Text(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
fun BiomeTransition(
    fromBiome: Biome,
    toBiome: Biome,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = BIOME_TRANSITION_DURATION),
        label = "biome_transition"
    )

    val fromColors = getBiomeColors(fromBiome)
    val toColors = getBiomeColors(toBiome)

    val blendedTop = lerpColor(fromColors.background, toColors.background, animatedProgress)
    val blendedMid = lerpColor(fromColors.tertiary, toColors.tertiary, animatedProgress)
    val blendedBottom = lerpColor(fromColors.primary, toColors.primary, animatedProgress)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(blendedTop, blendedMid, blendedBottom)
                    )
                )
        )
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    return Color(
        red = from.red + (to.red - from.red) * fraction,
        green = from.green + (to.green - from.green) * fraction,
        blue = from.blue + (to.blue - from.blue) * fraction,
        alpha = from.alpha + (to.alpha - from.alpha) * fraction,
    )
}

private data class BiomeColorSet(
    val primary: Color,
    val tertiary: Color,
    val background: Color
)

private fun getBiomeColors(biome: Biome): BiomeColorSet {
    return when (biome) {
        Biome.Forest -> BiomeColorSet(
            primary = Color(0xFF388E3C),
            tertiary = Color(0xFF81C784),
            background = Color(0xFFF1F8E9)
        )
        Biome.Mountain -> BiomeColorSet(
            primary = Color(0xFF546E7A),
            tertiary = Color(0xFF90A4AE),
            background = Color(0xFFECEFF1)
        )
        Biome.Swamp -> BiomeColorSet(
            primary = Color(0xFF00695C),
            tertiary = Color(0xFF80CBC4),
            background = Color(0xFFE0F2F1)
        )
        Biome.Coast -> BiomeColorSet(
            primary = Color(0xFF0277BD),
            tertiary = Color(0xFF81D4FA),
            background = Color(0xFFE1F5FE)
        )
    }
}
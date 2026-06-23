package com.wanderingledger.feature.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wanderingledger.core.model.Biome

@Composable
fun WeatherOverlay(
    weather: WeatherCondition,
    modifier: Modifier = Modifier,
    biome: Biome = Biome.Forest,
    reducedMotion: Boolean = false,
) {
    if (weather == WeatherCondition.Clear) return

    AtmosphericWeatherOverlay(
        biome = biome,
        weather = weather,
        reducedMotion = reducedMotion,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun FogOverlay(
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val alpha = if (reducedMotion) 0.16f else 0.28f

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFFB0BEC5).copy(alpha = alpha)),
    )
}

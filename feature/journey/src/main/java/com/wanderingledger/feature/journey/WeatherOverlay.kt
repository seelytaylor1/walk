package com.wanderingledger.feature.journey

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private const val PARTICLE_COUNT = 8
private const val ANIMATION_DURATION = 3000

@Composable
fun WeatherOverlay(
    weather: WeatherCondition,
    modifier: Modifier = Modifier
) {
    if (weather == WeatherCondition.Clear) return

    Box(modifier = modifier.fillMaxSize()) {
        repeat(PARTICLE_COUNT) { index ->
            WeatherParticle(
                weather = weather,
                index = index,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun WeatherParticle(
    weather: WeatherCondition,
    index: Int,
    modifier: Modifier = Modifier
) {
    val animationDuration = ANIMATION_DURATION + (index * 200)
    val initialOffset = remember(index) { Random.nextFloat() * 100f }

    val infiniteTransition = rememberInfiniteTransition(label = "weather_particle_$index")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -100f + initialOffset,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration),
            repeatMode = RepeatMode.Restart
        ),
        label = "y_fall"
    )

    val xOffset by infiniteTransition.animateFloat(
        initialValue = (index * 50).toFloat(),
        targetValue = (index * 50).toFloat() + when (weather) {
            WeatherCondition.Rain -> 20f
            WeatherCondition.Snow -> -30f
            WeatherCondition.Fog -> 15f
            WeatherCondition.Storm -> 40f
            else -> 0f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x_drift"
    )

    val particleColor = when (weather) {
        WeatherCondition.Rain -> Color(0x6081D4FA)
        WeatherCondition.Snow -> Color(0x80FFFFFF)
        WeatherCondition.Fog -> Color(0x50B0BEC5)
        WeatherCondition.Storm -> Color(0x7090A4AE)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .offset { IntOffset(xOffset.toInt(), yOffset.toInt()) }
            .background(particleColor)
    ) {
        Text(
            text = weather.emoji,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun FogOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fog_overlay")

    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fog_x"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fog_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x40B0BEC5).copy(alpha = alpha))
    )
}
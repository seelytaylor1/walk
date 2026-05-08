package com.wanderingledger.feature.journey

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private const val FIRE_PARTICLE_COUNT = 12
private const val FIREFLY_COUNT = 6
private const val STAR_COUNT = 20

@Composable
fun CampfireEffect(
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "campfire")

    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 80f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_radius"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF6B35).copy(alpha = 0.6f * flicker * intensity),
                        Color(0xFFFF8C00).copy(alpha = 0.3f * flicker * intensity),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = glowRadius * flicker
                ),
                radius = glowRadius * flicker,
                center = Offset(centerX, centerY)
            )
        }

        Text(
            text = "\uD83E\uDDED",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.offset(y = (-10).dp)
        )

        repeat(FIRE_PARTICLE_COUNT) { index ->
            FireParticle(
                index = index,
                intensity = intensity,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun FireParticle(
    index: Int,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val animationDuration = 1500 + (index * 100)
    val initialOffset = remember(index) { Random.nextFloat() * 30f }

    val infiniteTransition = rememberInfiniteTransition(label = "fire_particle_$index")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -50f - initialOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration),
            repeatMode = RepeatMode.Restart
        ),
        label = "fire_y"
    )

    val xOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration / 2),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_x"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration),
            repeatMode = RepeatMode.Restart
        ),
        label = "fire_alpha"
    )

    Text(
        text = if (index % 3 == 0) "\u2605" else "\u2022",
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFFF6B35).copy(alpha = alpha * intensity),
        modifier = modifier.offset(x = xOffset.dp, y = yOffset.dp)
    )
}

@Composable
fun CampfireGlowOverlay(
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "campfire_glow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x40FF8C00).copy(alpha = glowAlpha * intensity),
                        Color(0x20FF6B35).copy(alpha = glowAlpha * 0.5f * intensity),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun NightStars(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    Box(modifier = modifier.fillMaxSize()) {
        repeat(STAR_COUNT) { index ->
            val xPos = remember(index) { (index * 47) % 400 }
            val yPos = remember(index) { (index * 31) % 200 }

            Text(
                text = "\u2B50",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.offset(
                    x = xPos.dp,
                    y = yPos.dp
                ),
                color = Color.White.copy(alpha = twinkle * 0.8f)
            )
        }
    }
}

@Composable
fun FireflyParticles(
    biome: com.wanderingledger.core.model.Biome,
    modifier: Modifier = Modifier
) {
    if (biome != com.wanderingledger.core.model.Biome.Forest) return

    val infiniteTransition = rememberInfiniteTransition(label = "fireflies")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firefly_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        repeat(FIREFLY_COUNT) { index ->
            val xStart = remember(index) { Random.nextFloat() * 300f }
            val yStart = remember(index) { Random.nextFloat() * 400f }
            val delay = index * 500

            Firefly(
                startX = xStart,
                startY = yStart,
                delay = delay,
                alpha = alpha,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun Firefly(
    startX: Float,
    startY: Float,
    delay: Int,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "firefly_${startX}")

    val xOffset by infiniteTransition.animateFloat(
        initialValue = startX,
        targetValue = startX + Random.nextFloat() * 100f - 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000 + delay),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firefly_x"
    )

    val yOffset by infiniteTransition.animateFloat(
        initialValue = startY,
        targetValue = startY + Random.nextFloat() * 80f - 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000 + delay),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firefly_y"
    )

    Text(
        text = "\u2728",
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.offset(x = xOffset.dp, y = yOffset.dp),
        color = Color(0xFFFFFF00).copy(alpha = alpha * 0.7f)
    )
}

@Composable
fun CampAmbientOverlay(
    timeOfDay: TimeOfDay,
    campfireIntensity: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (timeOfDay) {
            TimeOfDay.Night -> {
                NightStars()
                if (campfireIntensity > 0f) {
                    CampfireGlowOverlay(intensity = campfireIntensity)
                }
                FireflyParticles(
                    biome = com.wanderingledger.core.model.Biome.Forest,
                    modifier = Modifier.fillMaxSize()
                )
            }
            TimeOfDay.Dusk -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x20FF7043))
                )
                if (campfireIntensity > 0.5f) {
                    CampfireGlowOverlay(intensity = campfireIntensity * 0.5f)
                }
            }
            else -> {
                if (campfireIntensity > 0.8f) {
                    CampfireGlowOverlay(intensity = campfireIntensity * 0.3f)
                }
            }
        }
    }
}
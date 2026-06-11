package com.wanderingledger.feature.journey

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.wanderingledger.core.model.Biome
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val MASTER_LOOP_MS = 60_000
private const val REDUCED_MOTION_PARTICLE_SCALE = 0.35f
private const val REDUCED_MOTION_SPEED_SCALE = 0.18f

@Immutable
data class AtmosphericFxConfig(
    val biome: Biome,
    val weather: WeatherCondition = WeatherCondition.Clear,
    val timeOfDay: TimeOfDay = TimeOfDay.Day,
    val campfireIntensity: Float = 0f,
    val includeBiomeAmbient: Boolean = true,
    val includeWeather: Boolean = true,
    val includeNightSky: Boolean = false,
    val reducedMotion: Boolean = false,
)

@Immutable
private data class ParticleEmitterSpec(
    val id: String,
    val kind: ParticleKind,
    val count: Int,
    val color: Color,
    val minSize: Float,
    val maxSize: Float,
    val speed: Float,
    val drift: Float,
    val alpha: Float,
)

private enum class ParticleKind {
    Dust,
    Firefly,
    Rain,
    Snow,
    Fog,
    Storm,
    Ember,
    Star,
    Mist,
}

@Immutable
private data class ParticleSeed(
    val x: Float,
    val y: Float,
    val phase: Float,
    val size: Float,
    val drift: Float,
)

@Composable
fun AtmosphericFxLayer(
    config: AtmosphericFxConfig,
    modifier: Modifier = Modifier,
) {
    val specs = remember(config) { buildEmitterSpecs(config) }
    if (specs.isEmpty()) return

    val lifecycle =
        remember(specs) {
            AtmosphericParticleLifecycle(
                specs.associate { spec ->
                    spec.id to AtmosphericParticleLifecycle.seedsFor(spec.id, spec.count)
                },
            )
        }
    val transition = rememberInfiniteTransition(label = "atmospheric-fx")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = MASTER_LOOP_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "atmospheric-fx-progress",
    )

    Canvas(modifier = modifier) {
        specs.forEach { spec ->
            lifecycle.seeds(spec.id).forEach { seed ->
                drawParticle(spec, seed, progress, config.reducedMotion)
            }
        }
    }
}

@Composable
fun AtmosphericWeatherOverlay(
    biome: Biome,
    weather: WeatherCondition,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    AtmosphericFxLayer(
        config =
            AtmosphericFxConfig(
                biome = biome,
                weather = weather,
                includeBiomeAmbient = false,
                includeWeather = true,
                reducedMotion = reducedMotion,
            ),
        modifier = modifier,
    )
}

@Composable
fun AtmosphericCampfireSparks(
    biome: Biome,
    intensity: Float,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    AtmosphericFxLayer(
        config =
            AtmosphericFxConfig(
                biome = biome,
                campfireIntensity = intensity,
                includeBiomeAmbient = false,
                includeWeather = false,
                reducedMotion = reducedMotion,
            ),
        modifier = modifier,
    )
}

private class AtmosphericParticleLifecycle(
    private val seedsByEmitterId: Map<String, List<ParticleSeed>>,
) {
    fun seeds(emitterId: String): List<ParticleSeed> = seedsByEmitterId[emitterId].orEmpty()

    companion object {
        fun seedsFor(
            emitterId: String,
            count: Int,
        ): List<ParticleSeed> {
            val random = Random(emitterId.hashCode())
            return List(count) {
                ParticleSeed(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    phase = random.nextFloat(),
                    size = random.nextFloat(),
                    drift = random.nextFloat() * 2f - 1f,
                )
            }
        }
    }
}

private fun buildEmitterSpecs(config: AtmosphericFxConfig): List<ParticleEmitterSpec> {
    val motionScale = if (config.reducedMotion) REDUCED_MOTION_PARTICLE_SCALE else 1f
    val speedScale = if (config.reducedMotion) REDUCED_MOTION_SPEED_SCALE else 1f
    val specs = mutableListOf<ParticleEmitterSpec>()

    if (config.includeBiomeAmbient) {
        specs += config.biome.biomeAmbientSpec(motionScale, speedScale)
    }
    if (config.includeWeather && config.weather != WeatherCondition.Clear) {
        specs += config.weather.weatherSpec(motionScale, speedScale)
    }
    if (config.includeNightSky || config.timeOfDay == TimeOfDay.Night) {
        specs +=
            ParticleEmitterSpec(
                id = "night-stars-${config.biome}",
                kind = ParticleKind.Star,
                count = scaledCount(28, motionScale),
                color = Color.White,
                minSize = 1.4f,
                maxSize = 3.2f,
                speed = 0.02f * speedScale,
                drift = 0f,
                alpha = 0.72f,
            )
    }
    if (config.campfireIntensity > 0f) {
        specs +=
            ParticleEmitterSpec(
                id = "campfire-embers-${config.biome}",
                kind = ParticleKind.Ember,
                count = scaledCount((18 * config.campfireIntensity).toInt().coerceAtLeast(4), motionScale),
                color = Color(0xFFFFA040),
                minSize = 2f,
                maxSize = 5f,
                speed = 0.85f * speedScale,
                drift = 34f,
                alpha = 0.78f * config.campfireIntensity,
            )
    }

    return specs
}

private fun Biome.biomeAmbientSpec(
    motionScale: Float,
    speedScale: Float,
): ParticleEmitterSpec =
    when (this) {
        Biome.Forest ->
            ParticleEmitterSpec(
                id = "biome-forest-fireflies",
                kind = ParticleKind.Firefly,
                count = scaledCount(10, motionScale),
                color = Color(0xFFE6E96A),
                minSize = 2f,
                maxSize = 4.8f,
                speed = 0.09f * speedScale,
                drift = 42f,
                alpha = 0.64f,
            )
        Biome.Mountain ->
            ParticleEmitterSpec(
                id = "biome-mountain-dust",
                kind = ParticleKind.Dust,
                count = scaledCount(14, motionScale),
                color = Color(0xFFD7D2C6),
                minSize = 1.2f,
                maxSize = 3.4f,
                speed = 0.14f * speedScale,
                drift = 36f,
                alpha = 0.28f,
            )
        Biome.Swamp ->
            ParticleEmitterSpec(
                id = "biome-swamp-mist",
                kind = ParticleKind.Mist,
                count = scaledCount(12, motionScale),
                color = Color(0xFFB9D7C8),
                minSize = 18f,
                maxSize = 42f,
                speed = 0.05f * speedScale,
                drift = 46f,
                alpha = 0.18f,
            )
        Biome.Coast ->
            ParticleEmitterSpec(
                id = "biome-coast-spray",
                kind = ParticleKind.Dust,
                count = scaledCount(16, motionScale),
                color = Color(0xFFDAF1F5),
                minSize = 1.4f,
                maxSize = 3.8f,
                speed = 0.2f * speedScale,
                drift = 58f,
                alpha = 0.36f,
            )
    }

private fun WeatherCondition.weatherSpec(
    motionScale: Float,
    speedScale: Float,
): ParticleEmitterSpec =
    when (this) {
        WeatherCondition.Rain ->
            ParticleEmitterSpec(
                id = "weather-rain",
                kind = ParticleKind.Rain,
                count = scaledCount(44, motionScale),
                color = Color(0xFF9EDAF3),
                minSize = 8f,
                maxSize = 18f,
                speed = 1.4f * speedScale,
                drift = 54f,
                alpha = 0.42f,
            )
        WeatherCondition.Snow ->
            ParticleEmitterSpec(
                id = "weather-snow",
                kind = ParticleKind.Snow,
                count = scaledCount(32, motionScale),
                color = Color.White,
                minSize = 2f,
                maxSize = 5.5f,
                speed = 0.34f * speedScale,
                drift = 68f,
                alpha = 0.64f,
            )
        WeatherCondition.Fog ->
            ParticleEmitterSpec(
                id = "weather-fog",
                kind = ParticleKind.Fog,
                count = scaledCount(10, motionScale),
                color = Color(0xFFCBD4D6),
                minSize = 56f,
                maxSize = 128f,
                speed = 0.05f * speedScale,
                drift = 86f,
                alpha = 0.16f,
            )
        WeatherCondition.Storm ->
            ParticleEmitterSpec(
                id = "weather-storm",
                kind = ParticleKind.Storm,
                count = scaledCount(54, motionScale),
                color = Color(0xFFB6C3CC),
                minSize = 10f,
                maxSize = 22f,
                speed = 1.85f * speedScale,
                drift = 94f,
                alpha = 0.5f,
            )
        WeatherCondition.Clear -> error("Clear weather has no particle spec.")
    }

private fun DrawScope.drawParticle(
    spec: ParticleEmitterSpec,
    seed: ParticleSeed,
    progress: Float,
    reducedMotion: Boolean,
) {
    val phase = (progress + seed.phase) % 1f
    val radius = spec.minSize + (spec.maxSize - spec.minSize) * seed.size
    val sway = sin((phase * PI.toFloat() * 2f) + seed.phase * 4f) * spec.drift * seed.drift
    val alpha = spec.alpha * alphaFor(spec.kind, phase, reducedMotion)
    val color = spec.color.copy(alpha = alpha.coerceIn(0f, 1f))

    when (spec.kind) {
        ParticleKind.Rain,
        ParticleKind.Storm,
        -> {
            val y = ((seed.y + phase * spec.speed) % 1f) * size.height
            val x = (seed.x * size.width + sway).wrap(size.width)
            drawLine(
                color = color,
                start = Offset(x, y),
                end = Offset(x + spec.drift * 0.18f, y + radius),
                strokeWidth = if (spec.kind == ParticleKind.Storm) 2.2f else 1.4f,
            )
        }
        ParticleKind.Snow,
        ParticleKind.Dust,
        ParticleKind.Firefly,
        -> {
            val y = ((seed.y + phase * spec.speed) % 1f) * size.height
            val x = (seed.x * size.width + sway).wrap(size.width)
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            if (spec.kind == ParticleKind.Firefly) {
                drawCircle(color = color.copy(alpha = alpha * 0.22f), radius = radius * 3.1f, center = Offset(x, y))
            }
        }
        ParticleKind.Fog,
        ParticleKind.Mist,
        -> {
            val x = (seed.x * size.width + sway + phase * spec.speed * size.width).wrap(size.width)
            val y = seed.y * size.height * 0.78f
            drawOval(
                color = color,
                topLeft = Offset(x - radius, y - radius * 0.34f),
                size = Size(radius * 2.8f, radius * 0.72f),
            )
        }
        ParticleKind.Ember -> {
            val centerX = size.width * 0.5f
            val baseY = size.height * 0.54f
            val x = centerX + sway * 0.45f
            val y = baseY - phase * size.height * 0.34f
            drawCircle(color = color, radius = radius * (1f - phase * 0.66f), center = Offset(x, y))
        }
        ParticleKind.Star -> {
            val pulse = 0.54f + 0.46f * sin((phase * PI.toFloat() * 2f) + seed.phase * 8f)
            drawCircle(
                color = color.copy(alpha = alpha * pulse.coerceIn(0.25f, 1f)),
                radius = radius,
                center = Offset(seed.x * size.width, seed.y * size.height * 0.56f),
            )
        }
    }
}

private fun alphaFor(
    kind: ParticleKind,
    phase: Float,
    reducedMotion: Boolean,
): Float {
    val lifecycle =
        when (kind) {
            ParticleKind.Ember -> 1f - phase
            ParticleKind.Firefly,
            ParticleKind.Star,
            -> 0.65f + 0.35f * sin(phase * PI.toFloat() * 2f).coerceAtLeast(0f)
            else -> 1f
        }
    return if (reducedMotion) lifecycle * 0.72f else lifecycle
}

private fun scaledCount(
    count: Int,
    scale: Float,
): Int = (count * scale).toInt().coerceAtLeast(1)

private fun Float.wrap(max: Float): Float {
    if (max <= 0f) return this
    var value = this % max
    if (value < 0f) value += max
    return value
}

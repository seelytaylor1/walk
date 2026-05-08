package com.wanderingledger.feature.journey

import androidx.compose.ui.graphics.Color
import com.wanderingledger.core.model.Biome

enum class WeatherCondition {
    Clear,
    Rain,
    Snow,
    Fog,
    Storm;

    val emoji: String
        get() = when (this) {
            Clear -> ""
            Rain -> "\uD83C\uDF27\uFE0F"
            Snow -> "\u2744\uFE0F"
            Fog -> "\uD83C\uDF2B\uFE0F"
            Storm -> "\u26C8\uFE0F"
        }
}

enum class TimeOfDay {
    Dawn,
    Day,
    Dusk,
    Night;

    val tintColor: Color
        get() = when (this) {
            Dawn -> Color(0x30FFA726)
            Day -> Color.Transparent
            Dusk -> Color(0x30FF7043)
            Night -> Color(0x251565D5)
        }

    val vignetteIntensity: Float
        get() = when (this) {
            Dawn -> 0.3f
            Day -> 0f
            Dusk -> 0.4f
            Night -> 0.6f
        }
}

data class EnvironmentState(
    val biome: Biome,
    val weather: WeatherCondition = WeatherCondition.Clear,
    val timeOfDay: TimeOfDay = TimeOfDay.Day,
    val isTransitioning: Boolean = false,
    val transitionProgress: Float = 0f,
) {
    companion object {
        fun fromBiome(biome: Biome) = EnvironmentState(biome = biome)
    }
}

enum class ParallaxDepth(val speedMultiplier: Float) {
    Background(0.3f),
    Midground(0.6f),
    Foreground(1.0f),
}

data class ParallaxLayerConfig(
    val depth: ParallaxDepth,
    val elements: List<String>,
    val verticalOffset: Float = 0f,
    val horizontalSpacing: Float = 120f,
)
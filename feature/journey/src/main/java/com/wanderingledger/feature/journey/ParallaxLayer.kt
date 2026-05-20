package com.wanderingledger.feature.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.wanderingledger.core.model.Biome

private const val DRIFT_DURATION = 8000

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ParallaxLayer(
    depth: ParallaxDepth,
    biome: Biome,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val elements = getBiomeElements(biome, depth)

    // When reduced motion is active, render at the midpoint static offset instead of animating.
    val xOffsetDp = if (reducedMotion) {
        // Midpoint of the animation range (half of the target value)
        when (depth) {
            ParallaxDepth.Background -> (-20).dp
            ParallaxDepth.Midground  -> (-15).dp
            ParallaxDepth.Foreground -> (-10).dp
        }
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "parallax_$depth")
        val xOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = when (depth) {
                ParallaxDepth.Background -> -40f
                ParallaxDepth.Midground  -> -30f
                ParallaxDepth.Foreground -> -20f
            },
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = DRIFT_DURATION),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "x_drift",
        )
        xOffset.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Decorative — invisible to accessibility tree
            .semantics { invisibleToUser() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = xOffsetDp),
            horizontalArrangement = Arrangement.spacedBy(60.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            elements.forEach { element ->
                Text(
                    text = element,
                    style = when (depth) {
                        ParallaxDepth.Background -> MaterialTheme.typography.displayMedium
                        ParallaxDepth.Midground  -> MaterialTheme.typography.displayLarge
                        ParallaxDepth.Foreground -> MaterialTheme.typography.headlineLarge
                    },
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }
    }
}

private fun getBiomeElements(biome: Biome, depth: ParallaxDepth): List<String> {
    return when (biome) {
        Biome.Forest -> when (depth) {
            ParallaxDepth.Background -> listOf("\uD83C\uDF05", "\uD83C\uDF2B\uFE0F", "\uD83C\uDF05")
            ParallaxDepth.Midground -> listOf("\uD83C\uDF3F", "\uD83C\uDF32", "\uD83C\uDF3F", "\uD83C\uDF32")
            ParallaxDepth.Foreground -> listOf("\uD83C\uDF3F", "\u2618\uFE0F", "\uD83C\uDF3F")
        }
        Biome.Mountain -> when (depth) {
            ParallaxDepth.Background -> listOf("\u26F0\uFE0F", "\u2600\uFE0F", "\u26F0\uFE0F")
            ParallaxDepth.Midground -> listOf("\uD83C\uDF0F", "\uD83C\uDFD4\uFE0F", "\uD83C\uDF0F")
            ParallaxDepth.Foreground -> listOf("\uD83C\uDFD4\uFE0F", "\uD83C\uDF28\uFE0F", "\uD83C\uDFD4\uFE0F")
        }
        Biome.Swamp -> when (depth) {
            ParallaxDepth.Background -> listOf("\uD83C\uDF2B\uFE0F", "\uD83C\uDF2B\uFE0F", "\uD83C\uDF2B\uFE0F")
            ParallaxDepth.Midground -> listOf("\uD83C\uDF3A", "\uD83C\uDF33", "\uD83C\uDF3A", "\uD83C\uDF33")
            ParallaxDepth.Foreground -> listOf("\uD83C\uDF41", "\uD83C\uDF40", "\uD83C\uDF41")
        }
        Biome.Coast -> when (depth) {
            ParallaxDepth.Background -> listOf("\uD83C\uDF1E", "\u2600\uFE0F", "\uD83C\uDF1E")
            ParallaxDepth.Midground -> listOf("\uD83C\uDF0A", "\uD83C\uDFD6\uFE0F", "\uD83C\uDF0A", "\uD83C\uDFD6\uFE0F")
            ParallaxDepth.Foreground -> listOf("\uD83C\uDFD6\uFE0F", "\uD83C\uDFD5\uFE0F", "\uD83C\uDFD6\uFE0F")
        }
    }
}

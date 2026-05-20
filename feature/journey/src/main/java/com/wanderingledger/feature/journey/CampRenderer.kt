package com.wanderingledger.feature.journey

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.theme.WLTheme
import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion

private const val CAMP_TRANSITION_DURATION = 500

@Composable
fun CampRenderer(
    campState: CampState,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val biomeColors = WLTheme.current.biomeColors
    val campfireIntensity = if (campState.campfireLit) campState.ambientIntensity else 0f

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = getCampGradientColors(campState.campsiteBiome)
                    )
                )
        )

        ParallaxLayer(
            depth = ParallaxDepth.Background,
            biome = campState.campsiteBiome,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = 20.dp)
        )

        CampAmbientOverlay(
            timeOfDay = TimeOfDay.Night,
            campfireIntensity = campfireIntensity,
            biome = campState.campsiteBiome,
            reducedMotion = reducedMotion,
            modifier = Modifier.fillMaxSize()
        )

        TentOverlay(
            biome = campState.campsiteBiome,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = 100.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            CampfireEffect(
                intensity = campfireIntensity,
                biome = campState.campsiteBiome,
                reducedMotion = reducedMotion,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.height(20.dp))

            CampCompanionRow(
                companions = campState.currentCompanions,
                activities = campState.campActivities,
                modifier = Modifier.offset(y = 40.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\u26FA",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Making Camp",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            if (campState.durationMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${campState.durationMinutes} min resting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        ParallaxLayer(
            depth = ParallaxDepth.Foreground,
            biome = campState.campsiteBiome,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
        )
    }
}

@Composable
private fun CampCompanionRow(
    companions: List<Companion>,
    activities: Map<Long, CampActivity>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        companions.forEachIndexed { index, companion ->
            val activity = activities[companion.companionId] ?: CampActivity.Sitting

            when (activity) {
                CampActivity.Sleeping -> {
                    SleepingCompanion(
                        companion = companion,
                        modifier = Modifier.offset(x = (index * 50).dp)
                    )
                }
                CampActivity.Cooking -> {
                    CookingCompanion(
                        companion = companion,
                        modifier = Modifier.offset(x = (index * 50).dp)
                    )
                }
                CampActivity.KeepingWatch -> {
                    WatchingCompanion(
                        companion = companion,
                        modifier = Modifier.offset(x = (index * 50).dp)
                    )
                }
                else -> {
                    CampCompanion(
                        companion = companion,
                        activity = activity,
                        modifier = Modifier.offset(x = (index * 50).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TentOverlay(
    biome: Biome,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tent")

    val sway by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tent_sway"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { index ->
            val tentEmoji = when (biome) {
                Biome.Forest -> "\uD83E\uDDF5"
                Biome.Mountain -> "\u26F0\uFE0F"
                Biome.Swamp -> "\uD83C\uDF3F"
                Biome.Coast -> "\uD83C\uDFD6\uFE0F"
            }
            Text(
                text = tentEmoji,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.offset(y = sway.dp),
                color = Color.White.copy(alpha = 0.4f - (index * 0.1f))
            )
        }
    }
}

private fun getCampGradientColors(biome: Biome): List<Color> {
    return when (biome) {
        Biome.Forest -> listOf(
            Color(0xFF1B3A2D),
            Color(0xFF2E5339),
            Color(0xFF1A2F23)
        )
        Biome.Mountain -> listOf(
            Color(0xFF2C3E50),
            Color(0xFF34495E),
            Color(0xFF1A252F)
        )
        Biome.Swamp -> listOf(
            Color(0xFF1D3D3A),
            Color(0xFF2E4A47),
            Color(0xFF142B28)
        )
        Biome.Coast -> listOf(
            Color(0xFF1A3A5C),
            Color(0xFF2E4A6A),
            Color(0xFF0F2A42)
        )
    }
}

@Composable
fun CampTransition(
    fromJourney: Boolean,
    progress: Float,
    campState: CampState,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = CAMP_TRANSITION_DURATION),
        label = "camp_transition"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (animatedProgress < 0.5f) {
            EnvironmentBackground(
                biome = campState.campsiteBiome,
                activeCompanions = campState.currentCompanions,
                reducedMotion = reducedMotion,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CampRenderer(
                campState = campState,
                reducedMotion = reducedMotion,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

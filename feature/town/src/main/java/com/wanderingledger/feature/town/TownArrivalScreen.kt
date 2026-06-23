package com.wanderingledger.feature.town

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanderingledger.core.model.Biome
import kotlinx.coroutines.delay

/**
 * Phases of the arrival presentation.
 */
enum class ArrivalPhase {
    Establishing,
    RevealingTitle,
    Ready,
}

/**
 * UI State for the Town Arrival screen.
 */
data class TownArrivalScreenState(
    val townName: String,
    val townRegion: String,
    val biome: Biome,
)

/**
 * A cinematic screen shown when the player arrives at a new town.
 */
@Composable
fun TownArrivalScreen(
    state: TownArrivalScreenState,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(ArrivalPhase.Establishing) }

    LaunchedEffect(Unit) {
        // Simple state machine for arrival presentation
        delay(800)
        phase = ArrivalPhase.RevealingTitle
        delay(2500)
        phase = ArrivalPhase.Ready
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        // 1. Establishing Artwork Renderer
        EstablishingArtwork(
            biome = state.biome,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Title Overlays
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = phase >= ArrivalPhase.RevealingTitle,
                enter =
                    fadeIn(animationSpec = tween(2000)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(2000),
                        ),
            ) {
                Text(
                    text = state.townName.uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = phase >= ArrivalPhase.RevealingTitle,
                enter = fadeIn(animationSpec = tween(2000, delayMillis = 1000)),
            ) {
                Text(
                    text = state.townRegion,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // 3. Atmospheric Arrival Transitions (The Ready state)
        AnimatedVisibility(
            visible = phase == ArrivalPhase.Ready,
            enter = fadeIn(animationSpec = tween(1000)),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
        ) {
            Button(
                onClick = onFinish,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White,
                    ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            ) {
                Text(
                    text = "CONTINUE",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                )
            }
        }

        // Ambient particles could be added here later (Task 18)
    }
}

@Composable
fun EstablishingArtwork(
    biome: Biome,
    modifier: Modifier = Modifier,
) {
    val baseColor =
        when (biome) {
            Biome.Forest -> Color(0xFF1B3022)
            Biome.Mountain -> Color(0xFF2C3E50)
            Biome.Swamp -> Color(0xFF1A1A1D)
            Biome.Coast -> Color(0xFF011627)
        }

    val accentColor =
        when (biome) {
            Biome.Forest -> Color(0xFF4F772D)
            Biome.Mountain -> Color(0xFF7F8C8D)
            Biome.Swamp -> Color(0xFF4E4E50)
            Biome.Coast -> Color(0xFF20A4F3)
        }

    Box(
        modifier =
            modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(baseColor, accentColor.copy(alpha = 0.3f), baseColor),
                    ),
                ),
    ) {
        // Subtle animated vignette or glow could go here
    }
}

package com.wanderingledger.feature.journey

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.model.Companion

private const val IDLE_BOB_DURATION = 2000
private const val IDLE_BOB_OFFSET = 4f

@Composable
fun PartyMember(
    companion: Companion,
    isLeader: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle_bob")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -IDLE_BOB_OFFSET,
        targetValue = IDLE_BOB_OFFSET,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = IDLE_BOB_DURATION),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "y_bob",
    )

    Text(
        text = companion.role.emoji,
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier.offset(y = yOffset.dp),
    )
}

@Composable
fun PartyPlayer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "player_idle_bob")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -IDLE_BOB_OFFSET,
        targetValue = IDLE_BOB_OFFSET,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = IDLE_BOB_DURATION),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "player_y_bob",
    )

    Text(
        text = "\uD83D\uDEB6",
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier.offset(y = yOffset.dp),
    )
}

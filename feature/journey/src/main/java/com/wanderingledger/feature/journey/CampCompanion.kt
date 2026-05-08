package com.wanderingledger.feature.journey

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.model.Companion as CoreCompanion
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

private const val CAMP_IDLE_DURATION = 3000
private const val CAMP_IDLE_OFFSET = 2f
private const val BREATHING_DURATION = 4000
private const val BREATHING_OFFSET = 3f

@Composable
fun CampCompanion(
    companion: CoreCompanion,
    activity: CampActivity,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "camp_companion_${companion.companionId}")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -CAMP_IDLE_OFFSET,
        targetValue = CAMP_IDLE_OFFSET,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CAMP_IDLE_DURATION),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camp_y_bob"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BREATHING_DURATION),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    Text(
        text = getCampEmoji(companion, activity),
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier.offset(y = yOffset.dp)
    )
}

@Composable
fun SleepingCompanion(
    companion: CoreCompanion,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sleeping_${companion.companionId}")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sleep_alpha"
    )

    Text(
        text = "\uD83D\uDE34",
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    )
}

@Composable
fun CookingCompanion(
    companion: CoreCompanion,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cooking_${companion.companionId}")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "steam_y"
    )

    val xOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "steam_x"
    )

    Text(
        text = "\uD83C\uDF73",
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier
    )

    Text(
        text = "\u2601\uFE0F",
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.offset(x = xOffset.dp, y = yOffset.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
}

@Composable
fun WatchingCompanion(
    companion: CoreCompanion,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "watching_${companion.companionId}")

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "look_around"
    )

    Text(
        text = "\uD83D\uDC41\uFE0F",
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier
    )
}

private fun getCampEmoji(companion: CoreCompanion, activity: CampActivity): String {
    return when (activity) {
        CampActivity.Sleeping -> "\uD83D\uDE34"
        CampActivity.Cooking -> "\uD83C\uDF73"
        CampActivity.KeepingWatch -> "\uD83D\uDC41\uFE0F"
        CampActivity.StokingFire -> "\uD83E\uDDED"
        CampActivity.Chatting -> "\uD83D\uDCAC"
        CampActivity.Sitting -> companion.role.emoji
    }
}
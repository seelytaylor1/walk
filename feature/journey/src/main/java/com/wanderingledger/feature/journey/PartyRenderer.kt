package com.wanderingledger.feature.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.model.Companion

private val RENDERER_HEIGHT = 96.dp

@Composable
fun PartyRenderer(
    companions: List<Companion>,
    modifier: Modifier = Modifier,
    showPlayer: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RENDERER_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        PartyFormation(
            companions = companions,
            showPlayer = showPlayer
        )
    }
}

@Composable
fun PartyRendererCompact(
    companions: List<Companion>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\uD83D\uDEB6",
            style = MaterialTheme.typography.titleLarge
        )
        companions.forEach { companion ->
            Text(
                text = companion.role.emoji,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
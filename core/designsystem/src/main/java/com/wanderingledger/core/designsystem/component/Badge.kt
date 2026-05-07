package com.wanderingledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.theme.ContrabandColor
import com.wanderingledger.core.designsystem.theme.GoldColor
import com.wanderingledger.core.designsystem.theme.InfoLight
import com.wanderingledger.core.designsystem.theme.StepBankColor
import com.wanderingledger.core.designsystem.theme.SuccessLight
import com.wanderingledger.core.designsystem.theme.WarningLight

/**
 * Badge for displaying status, supply level, or other categorical information.
 */
@Composable
fun WLBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .background(
                color = containerColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

/**
 * Supply level badge for market items.
 */
@Composable
fun SupplyBadge(
    supplyLevel: Int,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (supplyLevel) {
        0 -> "Scarce" to WarningLight
        1 -> "Normal" to InfoLight
        2 -> "Abundant" to SuccessLight
        else -> "Unknown" to MaterialTheme.colorScheme.surfaceVariant
    }
    
    WLBadge(
        text = text,
        modifier = modifier,
        containerColor = color.copy(alpha = 0.2f),
        contentColor = color
    )
}

/**
 * Contraband badge for illegal goods.
 */
@Composable
fun ContrabandBadge(
    modifier: Modifier = Modifier
) {
    WLBadge(
        text = "Contraband",
        modifier = modifier,
        containerColor = ContrabandColor.copy(alpha = 0.2f),
        contentColor = ContrabandColor
    )
}

/**
 * Step count badge.
 */
@Composable
fun StepBadge(
    steps: Int,
    modifier: Modifier = Modifier
) {
    WLBadge(
        text = "$steps steps",
        modifier = modifier,
        containerColor = StepBankColor.copy(alpha = 0.2f),
        contentColor = StepBankColor
    )
}

/**
 * Gold amount badge.
 */
@Composable
fun GoldBadge(
    gold: Long,
    modifier: Modifier = Modifier
) {
    WLBadge(
        text = "$gold gold",
        modifier = modifier,
        containerColor = GoldColor.copy(alpha = 0.2f),
        contentColor = Color(0xFF8B6914) // Darker gold for text
    )
}

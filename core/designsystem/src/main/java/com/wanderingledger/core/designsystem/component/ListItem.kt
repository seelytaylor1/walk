package com.wanderingledger.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard list item for displaying information in a list.
 * Used for market items, inventory, companions, etc.
 */
@Composable
fun WLListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val itemModifier =
        if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        }

    Column(modifier = itemModifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (trailing != null) {
                trailing()
            }
        }
        Divider()
    }
}

/**
 * Market item list item with buy/sell prices.
 */
@Composable
fun MarketListItem(
    goodName: String,
    buyPrice: Long,
    sellPrice: Long,
    modifier: Modifier = Modifier,
    supplyLevel: Int? = null,
    isContraband: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    WLListItem(
        title = goodName,
        subtitle = "Buy: $buyPrice • Sell: $sellPrice",
        modifier = modifier,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isContraband) {
                    ContrabandBadge()
                }
                if (supplyLevel != null) {
                    SupplyBadge(supplyLevel = supplyLevel)
                }
            }
        },
        onClick = onClick,
    )
}

/**
 * Inventory item list item with quantity.
 */
@Composable
fun InventoryListItem(
    goodName: String,
    quantity: Int,
    modifier: Modifier = Modifier,
    isSealed: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    WLListItem(
        title = goodName,
        subtitle = if (isSealed) "Quantity: $quantity (Sealed)" else "Quantity: $quantity",
        modifier = modifier,
        onClick = onClick,
    )
}

/**
 * Companion list item with role and bond level.
 */
@Composable
fun CompanionListItem(
    name: String,
    role: String,
    bondLevel: Int,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    WLListItem(
        title = name,
        subtitle = "$role • Bond Level: $bondLevel",
        modifier = modifier,
        trailing = {
            if (isActive) {
                WLBadge(
                    text = "Active",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        onClick = onClick,
    )
}

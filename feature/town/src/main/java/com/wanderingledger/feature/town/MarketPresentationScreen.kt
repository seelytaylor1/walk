package com.wanderingledger.feature.town

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanderingledger.core.designsystem.component.WLCard
import com.wanderingledger.core.designsystem.component.WLOutlinedCard
import com.wanderingledger.core.designsystem.theme.SuccessLight
import com.wanderingledger.core.designsystem.theme.WanderingLedgerTheme
import com.wanderingledger.core.model.SupplyLevel

/**
 * A screen displaying goods available for trade in a market.
 */
@Composable
fun MarketPresentationScreen(
    state: MarketPresentationState,
    onBuy: (MarketGoodItemState) -> Unit,
    onSell: (MarketGoodItemState) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.townName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onBack) {
                        Text("Exit", style = MaterialTheme.typography.labelLarge)
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gold: ${state.playerGold}g",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cargo: ${state.inventoryUsed}/${state.inventoryCapacity}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.inventoryUsed >= state.inventoryCapacity) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.goods) { item ->
                GoodsCard(
                    item = item,
                    onBuy = { onBuy(item) },
                    onSell = { onSell(item) }
                )
            }
        }
    }
}

/**
 * A card representing a tradeable good.
 */
@Composable
fun GoodsCard(
    item: MarketGoodItemState,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rarityColor = when (item.rarity) {
        GoodRarity.Common -> Color.Transparent
        GoodRarity.Uncommon -> Color(0xFF4CAF50)
        GoodRarity.Rare -> Color(0xFF2196F3)
        GoodRarity.Legendary -> Color(0xFFFF9800)
    }

    val supplyColor = when (item.supplyLevel) {
        SupplyLevel.Scarce -> MaterialTheme.colorScheme.error
        SupplyLevel.Normal -> MaterialTheme.colorScheme.onSurfaceVariant
        SupplyLevel.Abundant -> Color(0xFF4CAF50)
    }

    val containerColor = if (item.profitPotential == ProfitPotential.High) {
        SuccessLight.copy(alpha = 0.05f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    WLOutlinedCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = containerColor,
        border = if (item.profitPotential == ProfitPotential.High) {
            BorderStroke(2.dp, SuccessLight)
        } else if (item.rarity != GoodRarity.Common) {
            BorderStroke(2.dp, rarityColor)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.hasRumorIndicator) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Rumor",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (item.isContraband) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Contraband",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.supplyLevel.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = supplyColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.profitPotential == ProfitPotential.High) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "★ HIGH PROFIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessLight,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                
                if (item.playerQuantity > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Held: ${item.playerQuantity}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (!item.recommendationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "“${item.recommendationReason}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBuy,
                    modifier = Modifier.weight(1f),
                    enabled = item.canAfford && item.hasSpace,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.profitPotential == ProfitPotential.High) SuccessLight else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (item.profitPotential == ProfitPotential.High) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUY", style = MaterialTheme.typography.labelSmall)
                        Text("${item.buyPrice}g", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onSell,
                    modifier = Modifier.weight(1f),
                    enabled = item.playerQuantity > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SELL", style = MaterialTheme.typography.labelSmall)
                        Text("${item.sellPrice}g", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarketPresentationPreview() {
    WanderingLedgerTheme {
        MarketPresentationScreen(
            state = MarketPresentationState(
                townName = "Ironport",
                playerGold = 1250,
                inventoryUsed = 4,
                inventoryCapacity = 10,
                goods = listOf(
                    MarketGoodItemState(
                        goodId = 1,
                        name = "Fine Silk",
                        description = "Luxurious fabric from the eastern looms.",
                        buyPrice = 45,
                        sellPrice = 38,
                        supplyLevel = SupplyLevel.Scarce,
                        rarity = GoodRarity.Rare,
                        playerQuantity = 2,
                        profitPotential = ProfitPotential.High,
                        hasRumorIndicator = true,
                        recommendationReason = "High demand in Oakhaven"
                    ),
                    MarketGoodItemState(
                        goodId = 2,
                        name = "Iron Ore",
                        description = "Raw ore for smelting and smithing.",
                        buyPrice = 12,
                        sellPrice = 10,
                        supplyLevel = SupplyLevel.Abundant,
                        rarity = GoodRarity.Common,
                        playerQuantity = 0
                    ),
                    MarketGoodItemState(
                        goodId = 3,
                        name = "Shadow Herb",
                        description = "An illegal herb used in dark alchemy.",
                        buyPrice = 120,
                        sellPrice = 95,
                        supplyLevel = SupplyLevel.Normal,
                        rarity = GoodRarity.Legendary,
                        isContraband = true,
                        playerQuantity = 0,
                        canAfford = false,
                        hasRumorIndicator = true,
                        recommendationReason = "Banned in major cities"
                    )
                )
            ),
            onBuy = {},
            onSell = {},
            onBack = {}
        )
    }
}

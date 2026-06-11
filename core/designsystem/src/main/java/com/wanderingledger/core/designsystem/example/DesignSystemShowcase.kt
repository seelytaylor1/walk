package com.wanderingledger.core.designsystem.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wanderingledger.core.designsystem.component.CompanionListItem
import com.wanderingledger.core.designsystem.component.ContrabandBadge
import com.wanderingledger.core.designsystem.component.GoldBadge
import com.wanderingledger.core.designsystem.component.InventoryListItem
import com.wanderingledger.core.designsystem.component.MarketListItem
import com.wanderingledger.core.designsystem.component.StepBadge
import com.wanderingledger.core.designsystem.component.SupplyBadge
import com.wanderingledger.core.designsystem.component.WLButton
import com.wanderingledger.core.designsystem.component.WLCard
import com.wanderingledger.core.designsystem.component.WLClickableCard
import com.wanderingledger.core.designsystem.component.WLEmptyState
import com.wanderingledger.core.designsystem.component.WLOutlinedButton
import com.wanderingledger.core.designsystem.component.WLOutlinedCard
import com.wanderingledger.core.designsystem.component.WLTextButton
import com.wanderingledger.core.designsystem.component.WLTopAppBar
import com.wanderingledger.core.designsystem.theme.Spacing
import com.wanderingledger.core.designsystem.theme.WanderingLedgerTheme

/**
 * Showcase of design system components.
 * This is for development reference and can be used to preview all components.
 */
@Composable
fun DesignSystemShowcase() {
    WanderingLedgerTheme {
        Scaffold(
            topBar = {
                WLTopAppBar(title = "Design System Showcase")
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                // Typography Section
                Text(
                    text = "Typography",
                    style = MaterialTheme.typography.headlineMedium,
                )

                Text(
                    text = "Display Large",
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = "Headline Medium",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Title Large",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Body Large - This is the main body text style used throughout the app.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Label Medium",
                    style = MaterialTheme.typography.labelMedium,
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Buttons Section
                Text(
                    text = "Buttons",
                    style = MaterialTheme.typography.headlineMedium,
                )

                WLButton(text = "Primary Button", onClick = {})
                WLOutlinedButton(text = "Outlined Button", onClick = {})
                WLTextButton(text = "Text Button", onClick = {})
                WLButton(text = "Disabled Button", onClick = {}, enabled = false)

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Cards Section
                Text(
                    text = "Cards",
                    style = MaterialTheme.typography.headlineMedium,
                )

                WLCard {
                    Text("Standard Card", style = MaterialTheme.typography.titleMedium)
                    Text("This is a standard card for content grouping.")
                }

                WLOutlinedCard {
                    Text("Outlined Card", style = MaterialTheme.typography.titleMedium)
                    Text("This is an outlined card for secondary content.")
                }

                WLClickableCard(onClick = {}) {
                    Text("Clickable Card", style = MaterialTheme.typography.titleMedium)
                    Text("This card can be clicked for interaction.")
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Badges Section
                Text(
                    text = "Badges",
                    style = MaterialTheme.typography.headlineMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    SupplyBadge(supplyLevel = 0)
                    SupplyBadge(supplyLevel = 1)
                    SupplyBadge(supplyLevel = 2)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    ContrabandBadge()
                    StepBadge(steps = 1500)
                    GoldBadge(gold = 250)
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // List Items Section
                Text(
                    text = "List Items",
                    style = MaterialTheme.typography.headlineMedium,
                )

                WLCard {
                    MarketListItem(
                        goodName = "Spices",
                        buyPrice = 100,
                        sellPrice = 150,
                        supplyLevel = 2,
                        isContraband = false,
                    )

                    MarketListItem(
                        goodName = "Silk",
                        buyPrice = 200,
                        sellPrice = 300,
                        supplyLevel = 0,
                        isContraband = true,
                    )
                }

                WLCard {
                    InventoryListItem(
                        goodName = "Wheat",
                        quantity = 5,
                    )

                    InventoryListItem(
                        goodName = "Wine",
                        quantity = 3,
                        isSealed = true,
                    )
                }

                WLCard {
                    CompanionListItem(
                        name = "Elena the Scout",
                        role = "Scout",
                        bondLevel = 3,
                        isActive = true,
                    )

                    CompanionListItem(
                        name = "Marcus the Fighter",
                        role = "Fighter",
                        bondLevel = 2,
                        isActive = false,
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Empty State Section
                Text(
                    text = "Empty State",
                    style = MaterialTheme.typography.headlineMedium,
                )

                WLCard {
                    WLEmptyState(
                        message = "No items found",
                        description = "Your inventory is empty. Visit a market to buy goods.",
                        action = {
                            WLButton(text = "Go to Market", onClick = {})
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DesignSystemShowcasePreview() {
    DesignSystemShowcase()
}

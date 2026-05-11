package com.wanderingledger.feature.ledger

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanderingledger.core.model.Rumor
import com.wanderingledger.core.designsystem.theme.BackgroundLight
import com.wanderingledger.core.designsystem.theme.OnBackgroundLight
import com.wanderingledger.core.designsystem.theme.PrimaryLight
import com.wanderingledger.core.designsystem.theme.SecondaryLight

data class LedgerScreenState(
    val activeRumors: List<Rumor>,
    val message: String? = null,
)

fun interface LedgerNavigationCallback {
    fun onNavigateBack()
}

data class LedgerActions(
    val onNavigateBack: LedgerNavigationCallback,
)

fun buildLedgerScreenState(
    rumors: List<Rumor>,
    message: String? = null,
): LedgerScreenState =
    LedgerScreenState(
        activeRumors = rumors,
        message = message,
    )

@Composable
fun LedgerScreen(
    state: LedgerScreenState,
    actions: LedgerActions,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE5DED0)) // Slightly darker than parchment for contrast
            .padding(16.dp)
    ) {
        // Journal Page
        LedgerPage(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Traveler's Ledger",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Divider(
                    color = Color(0xFF3E2723).copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (state.activeRumors.isEmpty()) {
                    EmptyLedgerState()
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // Space for back button
                    ) {
                        itemsIndexed(state.activeRumors) { index, rumor ->
                            RumorScrap(
                                rumor = rumor,
                                rotation = if (index % 2 == 0) -1.5f else 1.2f
                            )
                        }
                    }
                }
            }
        }

        // Back Button as a bookmark-style or just a physical button
        Button(
            onClick = { actions.onNavigateBack.onNavigateBack() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .shadow(4.dp, MaterialTheme.shapes.medium),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5D4037),
                contentColor = Color.White
            )
        ) {
            Text("Close Ledger")
        }
    }
}

@Composable
fun LedgerPage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(8.dp, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large),
        color = BackgroundLight,
        contentColor = OnBackgroundLight
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated paper texture could be an image, but we use a subtle color
            content()
            
            // Subtle "binding" shadow on the left
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun RumorScrap(
    rumor: Rumor,
    rotation: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(rotation)
            .shadow(2.dp, MaterialTheme.shapes.small),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4) // Yellowish scrap paper
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            // Pin icon simulation
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFD32F2F), androidx.compose.foundation.shape.CircleShape)
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp)
            )

            Column {
                Text(
                    text = rumor.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF212121)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expiry: ${rumor.expiryVisitsLeft} visits",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    
                    if (rumor.targetGoodId != null) {
                        Text(
                            text = "Market Rumor",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLedgerState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your ledger is empty.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black.copy(alpha = 0.4f)
        )
        Text(
            text = "Listen for rumors in towns.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black.copy(alpha = 0.3f)
        )
    }
}

import com.wanderingledger.core.designsystem.theme.WanderingLedgerTheme

class LedgerScreenView(context: Context) : ComposeView(context) {
    fun render(state: LedgerScreenState, actions: LedgerActions) {
        setContent {
            WanderingLedgerTheme {
                LedgerScreen(state, actions)
            }
        }
    }
}

package com.wanderingledger.feature.ledger

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.designsystem.theme.BackgroundLight
import com.wanderingledger.core.designsystem.theme.OnBackgroundLight
import com.wanderingledger.core.designsystem.theme.PrimaryLight
import com.wanderingledger.core.designsystem.theme.SecondaryLight
import com.wanderingledger.core.designsystem.theme.WanderingLedgerTheme
import com.wanderingledger.core.model.Rumor

private val DesktopBrown = Color(0xFF3F3128)
private val PageInk = Color(0xFF35281F)
private val Parchment = Color(0xFFFFF7E2)
private val ParchmentDark = Color(0xFFEAD7B2)
private val ScrapPaper = Color(0xFFFFF2BF)
private val BlueScrap = Color(0xFFE3EAF0)
private val PinRed = Color(0xFF9D342C)

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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DesktopBrown, Color(0xFF6E5746)),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        LedgerDeskTexture()

        LedgerJournal(
            rumorCount = state.activeRumors.size,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp),
        ) {
            AnimatedContent(
                targetState = state.activeRumors,
                transitionSpec = {
                    (slideInHorizontally(
                        animationSpec = tween(360),
                        initialOffsetX = { width -> width / 6 },
                    ) + fadeIn(tween(260))).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(260),
                            targetOffsetX = { width -> -width / 8 },
                        ) + fadeOut(tween(180)),
                    ).using(SizeTransform(clip = false))
                },
                label = "ledger-page-turn",
            ) { rumors ->
                if (rumors.isEmpty()) {
                    EmptyLedgerState(Modifier.fillMaxSize())
                } else {
                    RumorLedgerPage(
                        rumors = rumors,
                        message = state.message,
                    )
                }
            }
        }

        Button(
            onClick = { actions.onNavigateBack.onNavigateBack() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .shadow(8.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5E3F2E),
                contentColor = Color.White,
            ),
        ) {
            Text("Close Ledger")
        }
    }
}

@Composable
private fun LedgerJournal(
    rumorCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp)),
        color = Parchment,
        contentColor = OnBackgroundLight,
    ) {
        Box(Modifier.fillMaxSize()) {
            LedgerPageTexture()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(26.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.24f),
                                Color.Black.copy(alpha = 0.06f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            PageRibbon(
                rumorCount = rumorCount,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 22.dp, end = 20.dp, bottom = 18.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun RumorLedgerPage(
    rumors: List<Rumor>,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LedgerHeader(rumorCount = rumors.size)

        AnimatedVisibility(
            visible = !message.isNullOrBlank(),
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(180)),
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryLight,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 22.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            itemsIndexed(
                items = rumors,
                key = { _, rumor -> rumor.rumorId },
            ) { index, rumor ->
                RumorScrap(
                    rumor = rumor,
                    index = index,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (index % 2 == 0) 2.dp else 14.dp,
                            end = if (index % 2 == 0) 14.dp else 2.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun LedgerHeader(rumorCount: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Traveler's Ledger",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = PageInk,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Rumors, market whispers, and road notes",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = PageInk.copy(alpha = 0.68f),
                    ),
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SecondaryLight.copy(alpha = 0.12f),
                contentColor = SecondaryLight,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(
                    text = "$rumorCount pinned",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Divider(
            color = PageInk.copy(alpha = 0.18f),
            thickness = 1.dp,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
fun RumorScrap(
    rumor: Rumor,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = when (index % 4) {
            0 -> -1.6f
            1 -> 1.2f
            2 -> -0.7f
            else -> 1.7f
        },
        animationSpec = tween(420),
        label = "rumor-scrap-rotation",
    )
    val isMarketRumor = rumor.targetGoodId != null
    val scrapColor = if (isMarketRumor) BlueScrap else ScrapPaper

    Box(modifier = modifier.rotate(rotation)) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp),
        ) {
            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.08f, 0f)
                    lineTo(size.width * 0.96f, size.height * 0.04f)
                    lineTo(size.width * 0.92f, size.height)
                    lineTo(size.width * 0.04f, size.height * 0.94f)
                    close()
                },
                color = Color.Black.copy(alpha = 0.16f),
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ParchmentDark.copy(alpha = 0.82f), RoundedCornerShape(5.dp)),
            shape = RoundedCornerShape(5.dp),
            color = scrapColor,
            contentColor = PageInk,
        ) {
            Box(Modifier.padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 14.dp)) {
                ScrapTexture()
                RumorPin(modifier = Modifier.align(Alignment.TopCenter))
                Column {
                    Text(
                        text = rumor.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            color = PageInk,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (rumor.expiryVisitsLeft == 1) {
                                "Fades after 1 visit"
                            } else {
                                "Fades after ${rumor.expiryVisitsLeft} visits"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = PageInk.copy(alpha = 0.62f),
                        )
                        RumorTag(
                            label = when {
                                rumor.isFalse -> "Untrusted"
                                isMarketRumor -> "Market"
                                else -> "Road"
                            },
                            emphasized = isMarketRumor && !rumor.isFalse,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RumorTag(
    label: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (emphasized) PrimaryLight.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.38f),
        contentColor = if (emphasized) PrimaryLight else PageInk.copy(alpha = 0.72f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun RumorPin(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(y = (-26).dp)
            .size(18.dp)
            .shadow(3.dp, CircleShape)
            .background(PinRed, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
    )
}

@Composable
fun EmptyLedgerState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(width = 180.dp, height = 126.dp)) {
            val ink = PageInk.copy(alpha = 0.36f)
            drawLine(
                color = ink,
                start = Offset(size.width * 0.18f, size.height * 0.74f),
                end = Offset(size.width * 0.82f, size.height * 0.74f),
                strokeWidth = 3f,
            )
            drawCircle(
                color = Color.Transparent,
                radius = size.minDimension * 0.24f,
                center = Offset(size.width * 0.5f, size.height * 0.44f),
                style = Stroke(width = 4f),
            )
            drawLine(
                color = ink,
                start = Offset(size.width * 0.5f, size.height * 0.22f),
                end = Offset(size.width * 0.5f, size.height * 0.66f),
                strokeWidth = 2f,
            )
            drawLine(
                color = ink,
                start = Offset(size.width * 0.28f, size.height * 0.44f),
                end = Offset(size.width * 0.72f, size.height * 0.44f),
                strokeWidth = 2f,
            )
        }
        Text(
            text = "No rumors pinned",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = PageInk,
            ),
        )
        Text(
            text = "Visit towns and listen closely to fill these pages.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Serif,
                color = PageInk.copy(alpha = 0.62f),
            ),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun PageRibbon(
    rumorCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(end = 28.dp)
            .width(28.dp)
            .height(86.dp)
            .background(
                color = if (rumorCount > 0) PinRed else SecondaryLight.copy(alpha = 0.62f),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
            ),
    )
}

@Composable
private fun LedgerDeskTexture() {
    Canvas(Modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.035f)
        repeat(14) { index ->
            val y = size.height * (index + 1) / 15f
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y + (index % 3) * 5f),
                strokeWidth = 1.3f,
            )
        }
    }
}

@Composable
private fun LedgerPageTexture() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, ParchmentDark.copy(alpha = 0.34f)),
                center = Offset(size.width * 0.86f, size.height * 0.12f),
                radius = size.maxDimension * 0.86f,
            ),
        )
        val ruleColor = PageInk.copy(alpha = 0.055f)
        var y = 96f
        while (y < size.height - 28f) {
            drawLine(
                color = ruleColor,
                start = Offset(42f, y),
                end = Offset(size.width - 24f, y),
                strokeWidth = 1f,
            )
            y += 58f
        }
    }
}

@Composable
private fun ScrapTexture() {
    Canvas(Modifier.fillMaxSize()) {
        drawLine(
            color = PageInk.copy(alpha = 0.08f),
            start = Offset(size.width * 0.08f, size.height * 0.18f),
            end = Offset(size.width * 0.92f, size.height * 0.18f),
            strokeWidth = 1f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.42f),
            start = Offset(size.width * 0.04f, size.height * 0.82f),
            end = Offset(size.width * 0.76f, size.height * 0.78f),
            strokeWidth = 2f,
        )
    }
}

class LedgerScreenView(context: Context) : FrameLayout(context) {
    private val composeView = ComposeView(context).also { view ->
        addView(
            view,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    fun render(state: LedgerScreenState, actions: LedgerActions) {
        composeView.setContent {
            WanderingLedgerTheme {
                LedgerScreen(state, actions)
            }
        }
    }
}

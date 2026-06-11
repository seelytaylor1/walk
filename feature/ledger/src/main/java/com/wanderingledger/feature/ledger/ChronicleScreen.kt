package com.wanderingledger.feature.ledger

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
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
import com.wanderingledger.core.designsystem.theme.WanderingLedgerTheme
import com.wanderingledger.feature.ledger.model.ChronicleEntry
import com.wanderingledger.feature.ledger.model.ChronicleIcon
import com.wanderingledger.feature.ledger.model.EntryType

private val DeskBrown = Color(0xFF3F3128)
private val DeepInk = Color(0xFF35281F)
private val MutedInk = Color(0xFF6D4C41)
private val Parchment = Color(0xFFFFF7E2)
private val ParchmentEdge = Color(0xFFE7D0A3)
private val RoadBlue = Color(0xFF496D77)
private val GoldInk = Color(0xFF9B6B14)
private val FieldGreen = Color(0xFF5F7F45)
private val NotePaper = Color(0xFFFFF2BF)

data class ChronicleUiState(
    val entries: List<ChronicleEntry> = emptyList(),
)

fun buildChronicleUiState(entries: List<ChronicleEntry>): ChronicleUiState =
    ChronicleUiState(entries.sortedByDescending { it.timestampMs })

fun interface ChronicleNavigationCallback {
    fun onNavigateBack()
}

data class ChronicleActions(
    val onNavigateBack: ChronicleNavigationCallback,
)

@Composable
fun ChronicleScreen(
    state: ChronicleUiState,
    actions: ChronicleActions,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeskBrown, Color(0xFF6E5746)),
                    ),
                ).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        ChronicleDeskTexture()

        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 76.dp)
                    .shadow(16.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp)),
            color = Parchment,
            contentColor = DeepInk,
        ) {
            Box(Modifier.fillMaxSize()) {
                ChroniclePageTexture()
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            Color.Black.copy(alpha = 0.22f),
                                            Color.Black.copy(alpha = 0.06f),
                                            Color.Transparent,
                                        ),
                                ),
                            ),
                )
                ChronicleRibbon(
                    entryCount = state.entries.size,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(start = 26.dp, top = 24.dp, end = 22.dp, bottom = 18.dp),
                ) {
                    ChronicleHeader(entryCount = state.entries.size)
                    AnimatedContent(
                        targetState = state.entries,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (slideInVertically(tween(300)) { height -> height / 10 } + fadeIn(tween(240)))
                                .togetherWith(
                                    slideOutVertically(tween(220)) { height -> -height / 14 } + fadeOut(tween(180)),
                                ).using(SizeTransform(clip = false))
                        },
                        label = "chronicle-timeline-change",
                    ) { entries ->
                        if (entries.isEmpty()) {
                            EmptyChronicleState(Modifier.fillMaxSize())
                        } else {
                            ChronicleTimeline(
                                entries = entries,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { actions.onNavigateBack.onNavigateBack() },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .shadow(8.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E3F2E),
                    contentColor = Color.White,
                ),
        ) {
            Text("Close Chronicle")
        }
    }
}

@Composable
private fun ChronicleHeader(entryCount: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chronicle",
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = DeepInk,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Routes walked, encounters weathered, companions remembered",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            color = DeepInk.copy(alpha = 0.68f),
                        ),
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GoldInk.copy(alpha = 0.14f),
                contentColor = GoldInk,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(
                    text = "$entryCount entries",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Canvas(
            modifier =
                Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .height(2.dp),
        ) {
            drawLine(
                color = DeepInk.copy(alpha = 0.18f),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 2f,
            )
        }
    }
}

@Composable
private fun ChronicleTimeline(
    entries: List<ChronicleEntry>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 12.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(
            items = entries,
            key = { _, entry -> entry.id },
        ) { index, entry ->
            ChronicleTimelineEntry(
                entry = entry,
                isFirst = index == 0,
                isLast = index == entries.lastIndex,
            )
        }
    }
}

@Composable
private fun ChronicleTimelineEntry(
    entry: ChronicleEntry,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        TimelineRail(
            entry = entry,
            isFirst = isFirst,
            isLast = isLast,
        )
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
                    .border(1.dp, ParchmentEdge.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.42f),
            contentColor = DeepInk,
        ) {
            Box(Modifier.padding(14.dp)) {
                EntryCardTexture(entry.type)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = entry.townName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = entry.type.entryColor(),
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Text(
                            text = entry.timestampFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedInk,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                        )
                    }

                    Text(
                        text = entry.summary,
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                color = DeepInk.copy(alpha = 0.86f),
                            ),
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    if (entry.routeLabel != null || entry.goldDelta != null) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            entry.routeLabel?.let { label ->
                                ChronicleTag(label = label, color = RoadBlue)
                            }
                            entry.goldDelta?.let { delta ->
                                ChronicleTag(
                                    label = "${if (delta > 0) "+" else ""}${delta}g",
                                    color = if (delta >= 0) FieldGreen else Color(0xFF9D342C),
                                )
                            }
                        }
                    }

                    if (entry.companionName != null || entry.companionNote != null) {
                        CompanionNote(
                            name = entry.companionName ?: "Party note",
                            note = entry.companionNote ?: "A quiet mark in the margin.",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRail(
    entry: ChronicleEntry,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(40.dp)
                .height(156.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val lineX = size.width / 2f
            if (!isFirst) {
                drawLine(
                    color = DeepInk.copy(alpha = 0.18f),
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, 18f),
                    strokeWidth = 3f,
                )
            }
            if (!isLast) {
                drawLine(
                    color = DeepInk.copy(alpha = 0.18f),
                    start = Offset(lineX, 34f),
                    end = Offset(lineX, size.height),
                    strokeWidth = 3f,
                )
            }
        }
        Surface(
            modifier =
                Modifier
                    .padding(top = 12.dp)
                    .size(28.dp),
            shape = CircleShape,
            color = entry.type.entryColor(),
            contentColor = Parchment,
            shadowElevation = 2.dp,
        ) {
            ChronicleGlyph(entry.icon, Modifier.padding(6.dp))
        }
    }
}

@Composable
private fun ChronicleGlyph(
    icon: ChronicleIcon,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val ink = Parchment
        when (icon) {
            ChronicleIcon.COMPASS -> {
                drawCircle(ink, radius = size.minDimension * 0.43f, style = Stroke(width = 2.2f))
                drawLine(
                    ink,
                    Offset(size.width * 0.5f, size.height * 0.16f),
                    Offset(
                        size.width * 0.5f,
                        size.height * 0.84f,
                    ),
                    strokeWidth = 2f,
                )
                drawLine(
                    ink,
                    Offset(size.width * 0.16f, size.height * 0.5f),
                    Offset(
                        size.width * 0.84f,
                        size.height * 0.5f,
                    ),
                    strokeWidth = 2f,
                )
            }
            ChronicleIcon.MAP -> {
                val path =
                    Path().apply {
                        moveTo(size.width * 0.1f, size.height * 0.2f)
                        lineTo(size.width * 0.38f, size.height * 0.08f)
                        lineTo(size.width * 0.62f, size.height * 0.2f)
                        lineTo(size.width * 0.9f, size.height * 0.08f)
                        lineTo(size.width * 0.9f, size.height * 0.8f)
                        lineTo(size.width * 0.62f, size.height * 0.92f)
                        lineTo(size.width * 0.38f, size.height * 0.8f)
                        lineTo(size.width * 0.1f, size.height * 0.92f)
                        close()
                    }
                drawPath(path, ink, style = Stroke(width = 2.2f))
            }
            ChronicleIcon.DIALOG -> {
                drawRoundRect(ink, style = Stroke(width = 2.2f), cornerRadius = CornerRadius(5f, 5f))
                drawCircle(ink, radius = 1.8f, center = Offset(size.width * 0.34f, size.height * 0.5f))
                drawCircle(ink, radius = 1.8f, center = Offset(size.width * 0.5f, size.height * 0.5f))
                drawCircle(ink, radius = 1.8f, center = Offset(size.width * 0.66f, size.height * 0.5f))
            }
            ChronicleIcon.FIST -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.34f),
                    size =
                        androidx.compose.ui.geometry.Size(
                            size.width * 0.68f,
                            size.height * 0.34f,
                        ),
                    cornerRadius = CornerRadius(5f, 5f),
                )
                drawLine(
                    DeepInk.copy(alpha = 0.28f),
                    Offset(size.width * 0.32f, size.height * 0.34f),
                    Offset(
                        size.width * 0.32f,
                        size.height * 0.68f,
                    ),
                    strokeWidth = 1.3f,
                )
                drawLine(
                    DeepInk.copy(alpha = 0.28f),
                    Offset(size.width * 0.5f, size.height * 0.34f),
                    Offset(
                        size.width * 0.5f,
                        size.height * 0.68f,
                    ),
                    strokeWidth = 1.3f,
                )
            }
            ChronicleIcon.CIGAR -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.44f),
                    size =
                        androidx.compose.ui.geometry.Size(
                            size.width * 0.68f,
                            size.height * 0.18f,
                        ),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawLine(
                    ink,
                    Offset(size.width * 0.84f, size.height * 0.38f),
                    Offset(
                        size.width * 0.92f,
                        size.height * 0.24f,
                    ),
                    strokeWidth = 1.7f,
                )
            }
        }
    }
}

@Composable
private fun ChronicleTag(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CompanionNote(
    name: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, ParchmentEdge.copy(alpha = 0.72f), RoundedCornerShape(5.dp)),
        shape = RoundedCornerShape(5.dp),
        color = NotePaper.copy(alpha = 0.72f),
        contentColor = DeepInk,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = GoldInk,
            )
            Text(
                text = note,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                    ),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun EmptyChronicleState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier =
                    Modifier
                        .padding(bottom = 14.dp)
                        .size(width = 180.dp, height = 126.dp),
            ) {
                val ink = DeepInk.copy(alpha = 0.34f)
                drawCircle(
                    color = ink,
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.5f, size.height * 0.34f),
                    style = Stroke(width = 4f),
                )
                drawLine(
                    color = ink,
                    start = Offset(size.width * 0.5f, size.height * 0.58f),
                    end = Offset(size.width * 0.5f, size.height * 0.82f),
                    strokeWidth = 4f,
                )
                drawLine(
                    color = ink,
                    start = Offset(size.width * 0.26f, size.height * 0.82f),
                    end = Offset(size.width * 0.74f, size.height * 0.82f),
                    strokeWidth = 4f,
                )
            }
            Text(
                text = "No journey notes yet",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk,
                    ),
            )
            Text(
                text = "The first road will write itself here.",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = DeepInk.copy(alpha = 0.62f),
                    ),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun EntryCardTexture(type: EntryType) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(type.entryColor().copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.18f),
                    radius = size.maxDimension * 0.7f,
                ),
        )
        drawLine(
            color = DeepInk.copy(alpha = 0.05f),
            start = Offset(size.width * 0.08f, size.height * 0.88f),
            end = Offset(size.width * 0.92f, size.height * 0.84f),
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun ChronicleRibbon(
    entryCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(end = 28.dp)
                .width(28.dp)
                .height(92.dp)
                .background(
                    color = if (entryCount > 0) RoadBlue else MutedInk.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                ),
    )
}

@Composable
private fun ChronicleDeskTexture() {
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
private fun ChroniclePageTexture() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, ParchmentEdge.copy(alpha = 0.34f)),
                    center = Offset(size.width * 0.86f, size.height * 0.12f),
                    radius = size.maxDimension * 0.86f,
                ),
        )
        val ruleColor = DeepInk.copy(alpha = 0.045f)
        var y = 108f
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

private fun EntryType.entryColor(): Color =
    when (this) {
        EntryType.TRAVEL -> RoadBlue
        EntryType.ENCOUNTER -> Color(0xFF8B4C33)
        EntryType.MARKET -> GoldInk
        EntryType.COMPANION -> FieldGreen
        EntryType.RUMOR -> Color(0xFF7D5D8C)
    }

class ChronicleScreenView(
    context: Context,
) : FrameLayout(context) {
    private val composeView =
        ComposeView(context).also { view ->
            addView(
                view,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
            )
        }

    fun render(
        state: ChronicleUiState,
        actions: ChronicleActions,
    ) {
        composeView.setContent {
            WanderingLedgerTheme {
                ChronicleScreen(state, actions)
            }
        }
    }
}

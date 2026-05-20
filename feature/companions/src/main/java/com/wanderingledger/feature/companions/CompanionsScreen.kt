package com.wanderingledger.feature.companions

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole

const val MAX_BOND_LEVEL_UI = 5

private val NightDesk = Color(0xFF2F2925)
private val WarmPanel = Color(0xFFFFF6DF)
private val WarmPanelDeep = Color(0xFFE8D2A7)
private val Ink = Color(0xFF35281F)
private val SoftInk = Color(0xFF6D4C41)
private val Moss = Color(0xFF667B4F)
private val Brass = Color(0xFF9B6B14)
private val Rosewood = Color(0xFF8A4E42)

data class CompanionsScreenState(
    val activeCompanions: List<Companion>,
    val recruitableCompanions: List<Companion>,
    val message: String? = null,
    val recentCommentary: CompanionCommentaryUi? = null,
    val reducedMotion: Boolean = false,
)

data class CompanionCommentaryUi(
    val companionId: Long,
    val companionName: String,
    val line: String,
    val tone: String,
)

fun interface CompanionNavigationCallback {
    fun onNavigateBack()
}

fun interface CompanionRecruitCallback {
    fun onRecruit(companionId: Long)
}

fun interface CompanionInteractCallback {
    fun onInteract(companionId: Long)
}

data class CompanionsActions(
    val onNavigateBack: CompanionNavigationCallback,
    val onRecruit: CompanionRecruitCallback,
    val onInteract: CompanionInteractCallback,
)

fun buildCompanionsScreenState(
    active: List<Companion>,
    recruitable: List<Companion>,
    message: String? = null,
    recentCommentary: CompanionCommentaryUi? = null,
    reducedMotion: Boolean = false,
): CompanionsScreenState =
    CompanionsScreenState(
        activeCompanions = active,
        recruitableCompanions = recruitable,
        message = message,
        recentCommentary = recentCommentary,
        reducedMotion = reducedMotion,
    )

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompanionsScreen(
    state: CompanionsScreenState,
    actions: CompanionsActions,
    modifier: Modifier = Modifier,
) {
    val allCompanions = remember(state.activeCompanions, state.recruitableCompanions) {
        state.activeCompanions + state.recruitableCompanions
    }
    var selectedCompanionId by remember(allCompanions) {
        mutableStateOf(allCompanions.firstOrNull()?.companionId ?: -1L)
    }
    val selected = allCompanions.firstOrNull { it.companionId == selectedCompanionId }
        ?: allCompanions.firstOrNull()
    val selectedIsActive = selected?.isActive == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NightDesk, Color(0xFF5E4C40)),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        CompanionDeskTexture()

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)),
            color = WarmPanel,
            contentColor = Ink,
        ) {
            Box(Modifier.fillMaxSize()) {
                CompanionPageTexture()
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                ) {
                    if (maxWidth < 620.dp) {
                        CompanionPortraitStack(
                            state = state,
                            selected = selected,
                            selectedIsActive = selectedIsActive,
                            onSelect = { selectedCompanionId = it },
                            actions = actions,
                            reducedMotion = state.reducedMotion,
                        )
                    } else {
                        CompanionPortraitWide(
                            state = state,
                            selected = selected,
                            selectedIsActive = selectedIsActive,
                            onSelect = { selectedCompanionId = it },
                            actions = actions,
                            reducedMotion = state.reducedMotion,
                        )
                    }
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
            Text("Return to Town")
        }
    }
}

@Composable
private fun CompanionPortraitWide(
    state: CompanionsScreenState,
    selected: Companion?,
    selectedIsActive: Boolean,
    onSelect: (Long) -> Unit,
    actions: CompanionsActions,
    reducedMotion: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        SelectedCompanionPanel(
            companion = selected,
            isActive = selectedIsActive,
            message = state.message,
            recentCommentary = state.recentCommentary?.takeIf { it.companionId == selected?.companionId },
            actions = actions,
            reducedMotion = reducedMotion,
            modifier = Modifier
                .weight(1.18f)
                .fillMaxHeight(),
        )
        CompanionRosterPanel(
            state = state,
            selectedId = selected?.companionId,
            onSelect = onSelect,
            actions = actions,
            modifier = Modifier
                .weight(0.82f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun CompanionPortraitStack(
    state: CompanionsScreenState,
    selected: Companion?,
    selectedIsActive: Boolean,
    onSelect: (Long) -> Unit,
    actions: CompanionsActions,
    reducedMotion: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SelectedCompanionPanel(
            companion = selected,
            isActive = selectedIsActive,
            message = state.message,
            recentCommentary = state.recentCommentary?.takeIf { it.companionId == selected?.companionId },
            actions = actions,
            reducedMotion = reducedMotion,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        CompactRosterRail(
            companions = state.activeCompanions + state.recruitableCompanions,
            selectedId = selected?.companionId,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun SelectedCompanionPanel(
    companion: Companion?,
    isActive: Boolean,
    message: String?,
    recentCommentary: CompanionCommentaryUi?,
    actions: CompanionsActions,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Party",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Ink,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Companions, bonds, and road temper",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = Ink.copy(alpha = 0.66f),
            ),
            modifier = Modifier.padding(top = 3.dp, bottom = 14.dp),
        )

        AnimatedContent(
            targetState = companion,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (reducedMotion) {
                    // Fade only — no slide
                    fadeIn(tween(220)).togetherWith(fadeOut(tween(160)))
                        .using(SizeTransform(clip = false))
                } else {
                    (slideInHorizontally(tween(320)) { width -> width / 8 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(220)) { width -> -width / 10 } + fadeOut(tween(160)))
                        .using(SizeTransform(clip = false))
                }
            },
            label = "selected-companion",
        ) { selected ->
            if (selected == null) {
                EmptyCompanionAttachment(Modifier.fillMaxSize())
            } else {
                CompanionDetail(
                    companion = selected,
                    isActive = isActive,
                    message = message,
                    recentCommentary = recentCommentary,
                    actions = actions,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CompanionDetail(
    companion: Companion,
    isActive: Boolean,
    message: String?,
    recentCommentary: CompanionCommentaryUi?,
    actions: CompanionsActions,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val mood = companion.moodLabel()
    val moodColor = companion.moodColor()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            CompanionPortrait(
                companion = companion,
                moodColor = moodColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .semantics {
                        contentDescription = "${companion.name}, ${companion.role.displayName()}. " +
                            "Bond level ${companion.bondLevel} of $MAX_BOND_LEVEL_UI."
                    },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = companion.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${companion.role.displayName()} · ${if (isActive) "Traveling" else "Available"}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = moodColor,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                MoodBadge(label = mood, color = moodColor)
            }

            Text(
                text = companion.attachmentLine(isActive),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    color = Ink.copy(alpha = 0.84f),
                ),
                modifier = Modifier.padding(top = 14.dp),
            )

            if (recentCommentary != null) {
                CompanionCommentaryScrap(
                    commentary = recentCommentary,
                    color = moodColor,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            RelationshipMeter(
                bondLevel = companion.bondLevel,
                modifier = Modifier.padding(top = 16.dp),
            )

            FlowRow(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompanionTrait("Power ${companion.combatPower}", Brass)
                CompanionTrait(companion.questState.replaceFirstChar { it.titlecase() }, SoftInk)
                CompanionTrait(companion.roleStrength(), moodColor)
            }

            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = SoftInk,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }

        Button(
            onClick = {
                if (isActive) {
                    actions.onInteract.onInteract(companion.companionId)
                } else {
                    actions.onRecruit.onRecruit(companion.companionId)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Moss else Rosewood,
                contentColor = Color.White,
            ),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(if (isActive) "Talk" else "Recruit")
        }
    }
}

@Composable
private fun CompanionCommentaryScrap(
    commentary: CompanionCommentaryUi,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.46f),
        contentColor = Ink,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = commentary.companionName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = commentary.tone,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SoftInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = commentary.line,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = Ink.copy(alpha = 0.86f),
                ),
            )
        }
    }
}

@Composable
private fun CompanionRosterPanel(
    state: CompanionsScreenState,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    actions: CompanionsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RosterSectionLabel(
                title = "Traveling",
                count = state.activeCompanions.size,
            )
        }
        if (state.activeCompanions.isEmpty()) {
            item {
                RosterEmptyLine("You are traveling alone.")
            }
        } else {
            items(state.activeCompanions, key = { it.companionId }) { companion ->
                CompanionRosterRow(
                    companion = companion,
                    selected = companion.companionId == selectedId,
                    actionLabel = "Talk",
                    onSelect = { onSelect(companion.companionId) },
                    onAction = { actions.onInteract.onInteract(companion.companionId) },
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            RosterSectionLabel(
                title = "In Town",
                count = state.recruitableCompanions.size,
            )
        }
        if (state.recruitableCompanions.isEmpty()) {
            item {
                RosterEmptyLine("No one here is looking for a group.")
            }
        } else {
            items(state.recruitableCompanions, key = { it.companionId }) { companion ->
                CompanionRosterRow(
                    companion = companion,
                    selected = companion.companionId == selectedId,
                    actionLabel = "Recruit",
                    onSelect = { onSelect(companion.companionId) },
                    onAction = { actions.onRecruit.onRecruit(companion.companionId) },
                )
            }
        }
    }
}

@Composable
private fun CompactRosterRail(
    companions: List<Companion>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    if (companions.isEmpty()) {
        RosterEmptyLine("No companions nearby.")
        return
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(companions, key = { it.companionId }) { companion ->
            val selected = companion.companionId == selectedId
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.94f,
                animationSpec = tween(180),
                label = "compact-companion-scale",
            )
            Surface(
                modifier = Modifier
                    .width(136.dp)
                    .scale(scale)
                    .clickable { onSelect(companion.companionId) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) companion.moodColor().copy(alpha = 0.16f) else Color.White.copy(alpha = 0.32f),
                contentColor = Ink,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiniPortrait(companion)
                    Column {
                        Text(
                            text = companion.name,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = companion.role.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionRosterRow(
    companion: Companion,
    selected: Boolean,
    actionLabel: String,
    onSelect: () -> Unit,
    onAction: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        animationSpec = tween(180),
        label = "companion-row-scale",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) companion.moodColor().copy(alpha = 0.14f) else Color.White.copy(alpha = 0.36f),
        contentColor = Ink,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MiniPortrait(companion)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = companion.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${companion.role.displayName()} · ${companion.moodLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MiniBondDots(
                    bondLevel = companion.bondLevel,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = companion.moodColor(),
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun CompanionPortrait(
    companion: Companion,
    moodColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .border(1.dp, WarmPanelDeep.copy(alpha = 0.9f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1E1BF),
        contentColor = Ink,
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(moodColor.copy(alpha = 0.24f), Color.Transparent),
                    ),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.28f),
                    radius = size.minDimension * 0.34f,
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                )
                drawOval(
                    color = moodColor.copy(alpha = 0.28f),
                    topLeft = Offset(size.width * 0.24f, size.height * 0.34f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.56f),
                )
                drawCircle(
                    color = companion.skinTone(),
                    radius = size.minDimension * 0.16f,
                    center = Offset(size.width * 0.5f, size.height * 0.31f),
                )
                drawPath(
                    path = Path().apply {
                        moveTo(size.width * 0.38f, size.height * 0.25f)
                        quadraticBezierTo(size.width * 0.5f, size.height * 0.12f, size.width * 0.63f, size.height * 0.25f)
                        quadraticBezierTo(size.width * 0.57f, size.height * 0.2f, size.width * 0.5f, size.height * 0.2f)
                        quadraticBezierTo(size.width * 0.43f, size.height * 0.2f, size.width * 0.38f, size.height * 0.25f)
                        close()
                    },
                    color = companion.hairColor(),
                )
                drawLine(
                    color = Ink.copy(alpha = 0.42f),
                    start = Offset(size.width * 0.43f, size.height * 0.32f),
                    end = Offset(size.width * 0.57f, size.height * 0.32f),
                    strokeWidth = 2f,
                )
                drawPath(
                    path = companion.roleMark(size.width, size.height),
                    color = Color.White.copy(alpha = 0.78f),
                    style = Stroke(width = 4f),
                )
            }
            Text(
                text = companion.role.displayName(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Ink.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            )
        }
    }
}

@Composable
private fun MiniPortrait(companion: Companion) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = companion.moodColor().copy(alpha = 0.2f),
        contentColor = Ink,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = companion.skinTone(),
                radius = size.minDimension * 0.24f,
                center = Offset(size.width * 0.5f, size.height * 0.38f),
            )
            drawOval(
                color = companion.moodColor().copy(alpha = 0.55f),
                topLeft = Offset(size.width * 0.26f, size.height * 0.48f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.36f),
            )
            drawCircle(
                color = companion.hairColor(),
                radius = size.minDimension * 0.16f,
                center = Offset(size.width * 0.5f, size.height * 0.28f),
            )
        }
    }
}

@Composable
private fun RelationshipMeter(
    bondLevel: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Relationship: bond level $bondLevel of $MAX_BOND_LEVEL_UI."
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Relationship",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Ink,
            )
            Text(
                text = "$bondLevel/$MAX_BOND_LEVEL_UI",
                style = MaterialTheme.typography.labelMedium,
                color = SoftInk,
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(MAX_BOND_LEVEL_UI) { index ->
                val filled = index < bondLevel
                val alpha by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.28f,
                    animationSpec = tween(260),
                    label = "bond-alpha",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brass.copy(alpha = alpha)),
                )
            }
        }
    }
}

@Composable
private fun MiniBondDots(
    bondLevel: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(MAX_BOND_LEVEL_UI) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (index < bondLevel) Brass else SoftInk.copy(alpha = 0.22f),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun MoodBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun CompanionTrait(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun RosterSectionLabel(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Ink,
            ),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = SoftInk,
        )
    }
}

@Composable
private fun RosterEmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
        ),
        color = SoftInk,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun EmptyCompanionAttachment(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .size(width = 170.dp, height = 126.dp)
                    .alpha(0.62f),
            ) {
                drawCircle(
                    color = Ink.copy(alpha = 0.26f),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.5f, size.height * 0.34f),
                    style = Stroke(width = 4f),
                )
                drawLine(
                    color = Ink.copy(alpha = 0.26f),
                    start = Offset(size.width * 0.24f, size.height * 0.78f),
                    end = Offset(size.width * 0.76f, size.height * 0.78f),
                    strokeWidth = 4f,
                )
            }
            Text(
                text = "No companions nearby",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                ),
            )
            Text(
                text = "New bonds begin in town.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    color = SoftInk,
                ),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun CompanionDeskTexture() {
    Canvas(Modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.034f)
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
private fun CompanionPageTexture() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, WarmPanelDeep.copy(alpha = 0.34f)),
                center = Offset(size.width * 0.18f, size.height * 0.12f),
                radius = size.maxDimension * 0.9f,
            ),
        )
        val ruleColor = Ink.copy(alpha = 0.045f)
        var y = 86f
        while (y < size.height - 28f) {
            drawLine(
                color = ruleColor,
                start = Offset(24f, y),
                end = Offset(size.width - 24f, y),
                strokeWidth = 1f,
            )
            y += 58f
        }
    }
}

private fun CompanionRole.displayName(): String =
    name.replaceFirstChar { it.titlecase() }

private fun Companion.moodLabel(): String =
    when {
        bondLevel >= 4 -> "Devoted"
        bondLevel >= 2 -> "Steady"
        isActive -> "New bond"
        else -> "Curious"
    }

private fun Companion.moodColor(): Color =
    when (role) {
        CompanionRole.Fighter -> Rosewood
        CompanionRole.Scout -> Moss
        CompanionRole.Healer -> Color(0xFF4D8A6A)
        CompanionRole.Rogue -> Color(0xFF6A5B86)
        CompanionRole.Mage -> Color(0xFF4F6F90)
    }

private fun Companion.skinTone(): Color =
    when ((companionId % 4).toInt()) {
        0 -> Color(0xFFC98F67)
        1 -> Color(0xFFD8AA7A)
        2 -> Color(0xFFB97858)
        else -> Color(0xFFE2B98C)
    }

private fun Companion.hairColor(): Color =
    when ((companionId % 4).toInt()) {
        0 -> Color(0xFF34251F)
        1 -> Color(0xFF6A4934)
        2 -> Color(0xFF2E2A26)
        else -> Color(0xFF805F36)
    }

private fun Companion.roleStrength(): String =
    when (role) {
        CompanionRole.Fighter -> "Guards the line"
        CompanionRole.Scout -> "Reads the road"
        CompanionRole.Healer -> "Keeps watch"
        CompanionRole.Rogue -> "Finds angles"
        CompanionRole.Mage -> "Studies signs"
    }

private fun Companion.attachmentLine(isActive: Boolean): String {
    val bond = when {
        bondLevel >= 4 -> "trusts your pace without looking back"
        bondLevel >= 2 -> "has learned the shape of your road"
        bondLevel == 1 -> "is beginning to settle into the party"
        else -> "is still deciding what kind of traveler you are"
    }
    val place = if (isActive) "beside you" else "near the town gate"
    return "${name} waits $place and $bond."
}

private fun Companion.roleMark(width: Float, height: Float): Path =
    Path().apply {
        when (role) {
            CompanionRole.Fighter -> {
                moveTo(width * 0.42f, height * 0.7f)
                lineTo(width * 0.58f, height * 0.5f)
                moveTo(width * 0.58f, height * 0.7f)
                lineTo(width * 0.42f, height * 0.5f)
            }
            CompanionRole.Scout -> {
                moveTo(width * 0.36f, height * 0.62f)
                quadraticBezierTo(width * 0.5f, height * 0.46f, width * 0.64f, height * 0.62f)
                quadraticBezierTo(width * 0.5f, height * 0.7f, width * 0.36f, height * 0.62f)
            }
            CompanionRole.Healer -> {
                moveTo(width * 0.5f, height * 0.48f)
                lineTo(width * 0.5f, height * 0.72f)
                moveTo(width * 0.38f, height * 0.6f)
                lineTo(width * 0.62f, height * 0.6f)
            }
            CompanionRole.Rogue -> {
                moveTo(width * 0.42f, height * 0.72f)
                lineTo(width * 0.58f, height * 0.48f)
                lineTo(width * 0.55f, height * 0.72f)
            }
            CompanionRole.Mage -> {
                moveTo(width * 0.5f, height * 0.46f)
                lineTo(width * 0.55f, height * 0.58f)
                lineTo(width * 0.68f, height * 0.58f)
                lineTo(width * 0.58f, height * 0.66f)
                lineTo(width * 0.62f, height * 0.78f)
                lineTo(width * 0.5f, height * 0.7f)
                lineTo(width * 0.38f, height * 0.78f)
                lineTo(width * 0.42f, height * 0.66f)
                lineTo(width * 0.32f, height * 0.58f)
                lineTo(width * 0.45f, height * 0.58f)
                close()
            }
        }
    }

class CompanionsScreenView(context: Context) : FrameLayout(context) {
    private val composeView = ComposeView(context).also { view ->
        addView(
            view,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    fun render(state: CompanionsScreenState, actions: CompanionsActions) {
        composeView.setContent {
            WanderingLedgerTheme {
                CompanionsScreen(state, actions)
            }
        }
    }
}

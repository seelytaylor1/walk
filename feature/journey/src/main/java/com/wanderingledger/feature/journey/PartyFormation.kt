package com.wanderingledger.feature.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wanderingledger.core.model.Companion

private val MEMBER_SPACING = 48.dp

@Composable
fun LineFormation(
    companions: List<Companion>,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MEMBER_SPACING),
        content = content
    )
}

@Composable
fun PartyFormation(
    companions: List<Companion>,
    modifier: Modifier = Modifier,
    showPlayer: Boolean = true
) {
    val allMembers = buildList {
        if (showPlayer) add(0 to null)
        companions.forEachIndexed { index, companion ->
            add((index + 1) to companion)
        }
    }

    if (allMembers.isEmpty()) return

    val leaderIndex = allMembers.size / 2

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MEMBER_SPACING)
    ) {
        allMembers.forEachIndexed { index, member ->
            when {
                index == leaderIndex -> {
                    if (member.second != null) {
                        PartyMember(
                            companion = member.second!!,
                            isLeader = true
                        )
                    } else {
                        PartyPlayer()
                    }
                }
                member.second != null -> {
                    PartyMember(companion = member.second!!)
                }
                else -> {
                    PartyPlayer()
                }
            }
        }
    }
}
package com.wanderingledger.feature.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import com.wanderingledger.feature.ledger.models.ChronicleEntry
import com.wanderingledger.feature.ledger.models.EntryType
import com.wanderingledger.feature.ledger.models.ChronicleIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronicleScreen(
    state: ChronicleUiState
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "Chronicle",
                subtitle = "Your journey's history"
            )
        }
    ) { paddingValues ->
        if (state.entries.isEmpty()) {
            EmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            ChronicleList(
                entries = state.entries,
                modifier = Modifier
                    .verticalScroll(state.entries.isNotEmpty().toStatefulEnabled())
            )
        }
    }
}

@Composable
fun ChronicleList(
    entries: List<ChronicleEntry>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = paddingValues(16.dp),
        modifier = modifier
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            ChronicleEntryCard(
                entry = entry,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun TopBar(
    title: String,
    subtitle: String
) {
    AppTopBar(title = title, subtitle = subtitle)
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            InkCompassIllustration()
            Text(
                text = "Your journey hasn't begun yet",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
fun ChronicleEntryCard(
    entry: ChronicleEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Icon(
                icon = entry.icon,
                contentDescription = entry.icon.name,
                tint = getBiomeAccentColor(entry.id),
                modifier = Modifier.startOfRow(
                    endOf = 24.dp
                ).padding(end = 32.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = -12.dp)
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text(
                        text = entry.summary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        maxLines = 3
                    )
                }
                entry.goldDelta?.let { delta ->
                    GoldDeltaBadge(
                        delta = delta,
                        modifier = Modifier.padding(end = -12.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = entry.timestampFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif
                    ),
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}

@Composable
fun AppTopBar(
    title: String,
    subtitle: String
) {
    ScaffoldTopBar(
        title = title,
        subtitle = subtitle
    )
}

@Composable
fun ChronicleList(
    items: List<ChronicleEntry>,
    modifier: Modifier = Modifier
) {
    val pagingState = rememberPagingState(
        PagingConfig(
            pageSize = 30,
            enablePlaceholders = true
        )
    )

    LazyColumn(
        contentPadding = paddingValues(16.dp),
        modifier = modifier
    ) {
        val groupedItems = items.groupBy {
            LocalDate.ofEpochDay(it.timestampMs / 86_400_000)
        }.map { (key, value) ->
            ChronicleEntry(
                id = 0,
                type = EntryType.TRAVEL,
                title = "",
                summary = "",
                townName = "",
                timestampMs = key.toEpochDay() * 86_400_000L,
                companionName = "",
                goldDelta = null,
                icon = ChronicleIcon.COMPASS
            )
        }

        items(
            items = groupedItems,
            key = { it.timestampMs }
        ) { item ->
            StickyHeader(
                title = item.title,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            items(
                items = item.entries,
                key = { it.id }
            ) { entry ->
                ChronicleEntryCard(
                    entry = entry,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun GoldDeltaBadge(
    delta: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeOf(24.dp, 24.dp)
            .background(
                if (delta > 0) MaterialTheme.colorScheme.amber else
                MaterialTheme.colorScheme.rust
            )
    ) {
        Text(
            text = "${if (delta > 0) "+" else ""}${Math.abs(delta)}g",
            color = MaterialTheme.colorScheme.onPrimary,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp
        )
    }
}

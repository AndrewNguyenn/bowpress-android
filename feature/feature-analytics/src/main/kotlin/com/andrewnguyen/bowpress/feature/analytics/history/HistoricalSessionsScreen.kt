package com.andrewnguyen.bowpress.feature.analytics.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.feature.analytics.ui.AnalyticsCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalSessionsScreen(
    onBack: () -> Unit,
    viewModel: HistoricalSessionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoricalSessionsContent(
        state = state,
        onBack = onBack,
        onBowFilter = viewModel::setBowFilter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoricalSessionsContent(
    state: HistoricalSessionsUiState,
    onBack: () -> Unit,
    onBowFilter: (String?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FilterRow(
                bows = state.bows,
                activeBowId = state.activeBowFilter,
                onSelect = onBowFilter,
            )
            if (state.groups.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.groups.forEach { group ->
                        item(key = "header-${group.header}") {
                            Text(
                                text = group.header,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                        items(items = group.rows, key = { it.id }) { row ->
                            SessionRowCard(row = row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    bows: List<Bow>,
    activeBowId: String?,
    onSelect: (String?) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = activeBowId == null,
            onClick = { onSelect(null) },
            label = { Text("All bows") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        bows.forEach { bow ->
            FilterChip(
                selected = activeBowId == bow.id,
                onClick = { onSelect(bow.id) },
                label = { Text(bow.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun SessionRowCard(row: SessionRow) {
    AnalyticsCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.bowName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDate(row.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = row.arrowConfigLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Text(
                    text = "${row.arrowCount} arrows",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (row.feelTags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.feelTags.take(4).forEach { tag ->
                        TagChip(text = tag)
                    }
                    if (row.feelTags.size > 4) {
                        TagChip(text = "+${row.feelTags.size - 4}", muted = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, muted: Boolean = false) {
    val bg = if (muted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val fg = if (muted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    else MaterialTheme.colorScheme.primary
    Text(
        text = text,
        modifier = Modifier
            .background(bg, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No sessions yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Log a session to see it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private fun formatDate(instant: Instant): String {
    val fmt = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
        .withZone(ZoneId.systemDefault())
    return fmt.format(instant)
}

@Preview(showBackground = true)
@Composable
private fun HistoricalSessionsPreview() {
    BowPressTheme {
        HistoricalSessionsContent(
            state = HistoricalSessionsUiState(
                bows = listOf(
                    Bow(
                        id = "b1", userId = "u1", name = "Hoyt RX7",
                        bowType = BowType.COMPOUND, brand = "Hoyt", model = "RX7",
                        createdAt = Instant.now(),
                    ),
                ),
                groups = listOf(
                    SessionGroup(
                        header = "April 2026",
                        rows = listOf(
                            SessionRow(
                                id = "s1",
                                startedAt = Instant.now(),
                                bowId = "b1",
                                bowName = "Hoyt RX7",
                                arrowConfigLabel = "Easton X10",
                                arrowCount = 36,
                                feelTags = listOf("locked-in", "focused"),
                            ),
                        ),
                    ),
                ),
                isLoading = false,
            ),
            onBack = {},
            onBowFilter = {},
        )
    }
}

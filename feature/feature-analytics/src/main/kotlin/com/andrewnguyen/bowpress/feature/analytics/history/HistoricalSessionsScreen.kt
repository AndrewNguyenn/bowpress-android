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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
        onDeleteSession = viewModel::deleteSession,
        onUpdateSession = viewModel::updateSession,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoricalSessionsContent(
    state: HistoricalSessionsUiState,
    onBack: () -> Unit,
    onBowFilter: (String?) -> Unit,
    onDeleteSession: (String) -> Unit = {},
    onUpdateSession: (String, String, List<String>) -> Unit = { _, _, _ -> },
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingEdit by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingRow = remember(pendingDelete, state.groups) {
        pendingDelete?.let { id -> state.groups.flatMap { it.rows }.firstOrNull { it.id == id } }
    }
    val editingRow = remember(pendingEdit, state.groups) {
        pendingEdit?.let { id -> state.groups.flatMap { it.rows }.firstOrNull { it.id == id } }
    }
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
                            SessionRowCard(
                                row = row,
                                onDelete = { pendingDelete = row.id },
                                onEdit = { pendingEdit = row.id },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingRow != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete session?") },
            text = {
                Text(
                    "This permanently removes this session and its arrows, ends, and " +
                        "analytics — locally and from the cloud. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(pendingRow.id)
                    pendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (editingRow != null) {
        EditSessionDialog(
            row = editingRow,
            onDismiss = { pendingEdit = null },
            onSave = { notes, tags ->
                onUpdateSession(editingRow.id, notes, tags)
                pendingEdit = null
            },
        )
    }
}

@Composable
private fun EditSessionDialog(
    row: SessionRow,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
) {
    var notes by rememberSaveable(row.id) { mutableStateOf(row.notes) }
    var tagsText by rememberSaveable(row.id) {
        mutableStateOf(row.feelTags.joinToString(", "))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Feel tags (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("e.g. locked-in, back-tension") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedTags = tagsText
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                onSave(notes, parsedTags)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
private fun SessionRowCard(
    row: SessionRow,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
) {
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
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit session",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete session",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
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
                                notes = "",
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

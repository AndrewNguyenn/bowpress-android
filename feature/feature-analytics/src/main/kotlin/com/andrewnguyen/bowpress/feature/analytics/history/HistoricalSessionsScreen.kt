package com.andrewnguyen.bowpress.feature.analytics.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMoss
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppPondLt
import com.andrewnguyen.bowpress.core.designsystem.AppWABlackFill
import com.andrewnguyen.bowpress.core.designsystem.AppWABlueFill
import com.andrewnguyen.bowpress.core.designsystem.AppWAGoldFill
import com.andrewnguyen.bowpress.core.designsystem.AppWARedFill
import com.andrewnguyen.bowpress.core.designsystem.AppWAWhiteFill
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.bp.BPBigScore
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPFilterSummary
import com.andrewnguyen.bowpress.core.designsystem.bp.BPHairlineButton
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStamp
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStampTone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalSessionsScreen(
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit = {},
    viewModel: HistoricalSessionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoricalSessionsContent(
        state = state,
        onBack = onBack,
        onOpenSession = onOpenSession,
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
    onOpenSession: (String) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    onUpdateSession: (String, String, List<String>) -> Unit = { _, _, _ -> },
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingEdit by rememberSaveable { mutableStateOf<String?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val pendingRow = remember(pendingDelete, state.groups) {
        pendingDelete?.let { id -> state.groups.flatMap { it.rows }.firstOrNull { it.id == id } }
    }
    val editingRow = remember(pendingEdit, state.groups) {
        pendingEdit?.let { id -> state.groups.flatMap { it.rows }.firstOrNull { it.id == id } }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // 1. Nav header with session counts meta.
            item {
                BPNavHeader(
                    eyebrow = "Bowpress",
                    title = "Session log",
                    meta = {
                        SessionCountsSlot(
                            sessionCount = state.totalSessions,
                            arrowCount = state.totalArrows,
                            sinceLabel = state.sinceLabel,
                        )
                    },
                )
            }
            // 2. Filter summary.
            item {
                val activeBow = state.activeBowFilter?.let { id -> state.bows.firstOrNull { it.id == id } }
                val bowLabel = activeBow?.name ?: "All bows"
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    BPFilterSummary(
                        summary = "$bowLabel · All distances · 1 Week",
                        subtitle = "tap to change filters",
                        onEdit = { filtersOpen = true },
                    )
                }
            }

            // 3. Empty state handling.
            if (state.groups.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        EmptyLog()
                    }
                }
            } else {
                // 4. Render groups with optional month-rollup insertion.
                val insertIdx = state.monthRollupInsertIndex
                state.groups.forEachIndexed { idx, group ->
                    if (insertIdx == idx && state.monthRollup != null) {
                        item(key = "month-rollup-${state.monthRollup.monthLabel}") {
                            MonthBox(rollup = state.monthRollup)
                        }
                    }
                    item(key = "header-${group.header}") {
                        GroupHeader(group)
                    }
                    items(group.rows, keyPrefix = "row") { row, isLast ->
                        SessionLogRow(
                            row = row,
                            isLast = isLast,
                            onTap = { onOpenSession(row.id) },
                            onEdit = { pendingEdit = row.id },
                            onDelete = { pendingDelete = row.id },
                        )
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
                    Text("Delete", color = com.andrewnguyen.bowpress.core.designsystem.AppMaple)
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

    if (filtersOpen) {
        ModalBottomSheet(
            onDismissRequest = { filtersOpen = false },
            sheetState = sheetState,
            containerColor = AppPaper,
            tonalElevation = 0.dp,
        ) {
            FiltersSheetBody(
                state = state,
                onBowFilter = onBowFilter,
                onDone = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { filtersOpen = false }
                },
            )
        }
    }
}

// Helper that wraps forEach with a trailing `isLast` flag.
private inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    rows: List<SessionRow>,
    keyPrefix: String,
    crossinline content: @Composable (row: SessionRow, isLast: Boolean) -> Unit,
) {
    val total = rows.size
    rows.forEachIndexed { idx, row ->
        item(key = "$keyPrefix-${row.id}") {
            content(row, idx == total - 1)
        }
    }
}

@Composable
private fun SessionCountsSlot(sessionCount: Int, arrowCount: Int, sinceLabel: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = monoCountLine(value = "$sessionCount", unit = " sessions"),
            style = jetbrainsMono(10.sp),
        )
        Text(
            text = monoCountLine(value = "$arrowCount", unit = " arrows"),
            style = jetbrainsMono(10.sp),
        )
        Text(
            text = "since $sinceLabel",
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
        )
    }
}

private fun monoCountLine(value: String, unit: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) { append(value) }
    withStyle(SpanStyle(color = AppInk3)) { append(unit) }
}

@Composable
private fun GroupHeader(group: SessionGroup) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.header,
                style = interUI(9.5.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.24.em,
                    color = AppInk3,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = group.rangeLabel,
                style = jetbrainsMono(10.sp).copy(
                    letterSpacing = 0.04.em,
                    color = AppInk3,
                ),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))
    }
}

@Composable
private fun SessionLogRow(
    row: SessionRow,
    isLast: Boolean,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val date = remember(row.startedAt) { row.startedAt.atZone(zoneId).toLocalDate() }
    val dayNumber = "%02d".format(date.dayOfMonth)
    val weekdayAbbr = date.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).lowercase(Locale.US)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .background(AppPaper)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .drawBottomHairline(if (isLast) Color.Transparent else AppLine2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Col 1: Day tile (38dp).
        Column(
            modifier = Modifier.width(38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = dayNumber,
                style = frauncesDisplay(22.sp, italic = true).copy(
                    color = if (row.isBest) AppPine else AppPondDk,
                ),
            )
            Text(
                text = weekdayAbbr.uppercase(Locale.US),
                style = interUI(8.5.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.18.em,
                    color = AppInk3,
                ),
            )
        }

        // Col 2: Main.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val title = row.title ?: "Range"
                Text(
                    text = title,
                    style = frauncesDisplay(15.sp, italic = true).copy(color = AppInk),
                )
                val distanceTag = buildString {
                    append(" · ")
                    if (row.distance != null) {
                        append(row.distance.label)
                        append(" · ")
                    }
                    append("${row.arrowCount} arrows")
                }
                Text(
                    text = distanceTag,
                    style = jetbrainsMono(10.sp).copy(
                        letterSpacing = 0.04.em,
                        color = AppInk3,
                    ),
                )
                if (row.isBest) {
                    Spacer(Modifier.weight(1f))
                    BPStamp(text = "BEST", tone = BPStampTone.Pine)
                }
            }

            // Per-arrow bar strip.
            ArrowBars(rings = row.rings, arrowCount = row.arrowCount)

            // Meta line — avg, X%, bow.
            MetaLine(row = row)

            // Optional note excerpt.
            val excerpt = noteExcerpt(row.notes)
            if (excerpt != null) {
                Text(
                    text = excerpt,
                    style = frauncesDisplay(11.5.sp, italic = true, weight = FontWeight.Normal)
                        .copy(color = AppInk2),
                    maxLines = 2,
                )
            }
        }

        // Col 3: Right rail. iOS SessionLogRow shows total score "180 /180" +
        // X count + BEST tag (SessionLogRow.swift). Earlier Android rendered
        // avgRing here; switching to total to match the iOS oracle.
        Column(
            modifier = Modifier.padding(top = 2.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val totalScore = row.rings.sumOf { minOf(it, 10) }
            val maxScore = row.arrowCount * 10
            val hasArrows = row.arrowCount > 0 && row.rings.isNotEmpty()
            BPBigScore(value = if (hasArrows) totalScore.toString() else "—", size = 22.sp)
            if (hasArrows && maxScore > 0) {
                Text(
                    text = "/$maxScore",
                    style = jetbrainsMono(10.sp).copy(color = AppInk3),
                )
                Text(
                    text = "${row.xCount}X",
                    style = interUI(10.sp, weight = FontWeight.SemiBold).copy(
                        letterSpacing = 0.18.em,
                        color = AppInk3,
                    ),
                )
            }
            if (row.isBest) {
                Text(
                    text = "BEST",
                    style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                        letterSpacing = 0.18.em,
                        color = AppPine,
                    ),
                )
            }
            Row {
                Text(
                    text = "›",
                    modifier = Modifier.clickable { onEdit() },
                    style = frauncesDisplay(14.sp, italic = true).copy(color = AppPond),
                )
            }
        }
    }

    // Swipe/context affordance — long-press to delete via the alert below.
    // We keep edit/delete on the chevron + accessible row level, mirroring
    // the iOS `.contextMenu` delete action.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.dp)
            .clickable(
                enabled = false,
                onClick = onDelete,
            ),
    )
}

@Composable
private fun ArrowBars(rings: List<Int>, arrowCount: Int) {
    // Render up to `arrowCount` segments — fill with AppLine2 slots for
    // arrows we haven't hydrated plots for.
    val slots = if (rings.isNotEmpty()) rings else List(arrowCount.coerceAtMost(24)) { -1 }
    if (slots.isEmpty()) {
        Spacer(Modifier.height(6.dp))
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        val segWeight = 1f
        slots.forEach { ring ->
            // Mirror iOS HistoricalSessionsView.barFill — WA target palette
            // so the bar reads gold/red/blue/black/white without a legend.
            val color = when {
                ring < 0 -> AppLine2 // unfilled / hydrating
                ring in 9..11 -> AppWAGoldFill // 11 = X
                ring in 7..8 -> AppWARedFill
                ring in 5..6 -> AppWABlueFill
                ring in 3..4 -> AppWABlackFill
                else -> AppWAWhiteFill // 1, 2, miss
            }
            Box(
                modifier = Modifier
                    .weight(segWeight)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(color),
            )
        }
    }
}

@Composable
private fun MetaLine(row: SessionRow) {
    val anno = buildAnnotatedString {
        if (row.avgRing > 0.0) {
            withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) {
                append("%.1f".format(row.avgRing))
            }
            withStyle(SpanStyle(color = AppInk3)) { append(" avg · ") }
            withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) {
                append("${row.xPct}%")
            }
            withStyle(SpanStyle(color = AppInk3)) { append(" X · ") }
        }
        withStyle(SpanStyle(color = AppInk3)) { append(row.bowName) }
    }
    Text(
        text = anno,
        style = jetbrainsMono(10.sp).copy(letterSpacing = 0.04.em),
    )
}

private fun noteExcerpt(notes: String): String? {
    if (notes.isBlank()) return null
    val first = notes.split(". ", "! ", "? ").firstOrNull()?.trim() ?: notes
    val trimmed = first.take(60).trim()
    return if (trimmed.isEmpty()) null else "“$trimmed”"
}

@Composable
private fun MonthBox(rollup: MonthRollup) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPaper2)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val title = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = AppInk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    ),
                ) {
                    append(rollup.monthLabel)
                }
                withStyle(SpanStyle(color = AppInk3, fontSize = 10.5.sp)) {
                    append("  · ${rollup.sessionCount} sessions · avg ${"%.1f".format(rollup.avgRing)}")
                }
            }
            Text(
                text = title,
                style = frauncesDisplay(14.sp, italic = true),
            )
            Row {
                Text(
                    text = "range days",
                    style = interUI(10.5.sp).copy(color = AppInk2),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "shot days",
                    style = interUI(10.5.sp).copy(color = AppInk2),
                )
            }

            // 30-cell heatmap grid.
            val cellCount = 30
            val maxArrows = rollup.arrowsByDay.values.maxOrNull() ?: 1
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (dayNum in 1..cellCount) {
                        val arrows = rollup.arrowsByDay[dayNum] ?: 0
                        val isBestDay = arrows > 0 && arrows == maxArrows
                        val isInMonth = dayNum <= rollup.daysInMonth
                        val isInRange = dayNum <= rollup.todayDay
                        val fill = when {
                            !isInMonth || !isInRange -> AppLine2
                            else -> heatmapColor(arrows, maxArrows)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(fill)
                                .then(
                                    if (isBestDay) Modifier.drawBehind {
                                        val strokeWidth = 1.5.dp.toPx()
                                        drawRect(
                                            color = AppInk,
                                            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                                            size = androidx.compose.ui.geometry.Size(
                                                size.width - strokeWidth,
                                                size.height - strokeWidth,
                                            ),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth),
                                        )
                                    } else Modifier,
                                ),
                        )
                    }
                }
            }

            // Legend.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendSwatch(color = AppLine2, label = "none")
                LegendSwatch(color = Color(red = 0.75f, green = 0.83f, blue = 0.74f), label = "short")
                LegendSwatch(color = AppMoss, label = "full")
                LegendSwatch(color = AppPine, label = "peak")
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(9.dp).background(color))
        Text(
            text = label,
            style = interUI(9.sp).copy(letterSpacing = 0.04.em, color = AppInk3),
        )
    }
}

private fun heatmapColor(arrows: Int, max: Int): Color {
    if (arrows == 0) return AppLine2
    val ratio = if (max > 0) arrows.toDouble() / max else 0.0
    if (ratio < 0.33) return Color(red = 0.75f, green = 0.83f, blue = 0.74f)
    if (ratio < 0.66) return AppMoss
    return AppPine
}

@Composable
private fun EmptyLog() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No sessions yet",
            style = frauncesDisplay(22.sp, italic = true).copy(color = AppInk),
        )
        Text(
            text = "Log a session to see your history here.",
            style = interUI(14.sp).copy(color = AppInk3),
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
private fun FiltersSheetBody(
    state: HistoricalSessionsUiState,
    onBowFilter: (String?) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Text(
            text = "Filters",
            style = frauncesDisplay(22.sp, italic = true).copy(color = AppInk),
        )
        if (state.bows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BPEyebrow(text = "Bow")
                val scroll = rememberScrollState()
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterPill(label = "All bows", selected = state.activeBowFilter == null) {
                        onBowFilter(null)
                    }
                    state.bows.forEach { bow ->
                        FilterPill(label = bow.name, selected = state.activeBowFilter == bow.id) {
                            onBowFilter(bow.id)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BPHairlineButton(
            label = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            borderTone = AppPondDk,
            labelTone = AppPondDk,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AppPondDk else AppPaper2
    val fg = if (selected) AppPaper else AppPondDk
    Box(
        modifier = Modifier
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = interUI(11.5.sp, weight = FontWeight.SemiBold).copy(color = fg),
        )
    }
}

private fun Modifier.drawBottomHairline(color: Color): Modifier = this.drawBehind {
    if (color.alpha > 0f) {
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f,
        )
    }
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
                totalArrows = 128,
                totalSessions = 8,
                sinceLabel = "2026 · 03",
                groups = listOf(
                    SessionGroup(
                        header = "THIS WEEK",
                        bucket = GroupBucket.THIS_WEEK,
                        rangeLabel = "apr 21 — apr 24 · 3 sessions",
                        rows = listOf(
                            SessionRow(
                                id = "s1",
                                startedAt = Instant.now(),
                                bowId = "b1",
                                bowName = "Hoyt RX7",
                                arrowConfigLabel = "Easton X10",
                                arrowCount = 36,
                                feelTags = listOf("locked-in", "focused"),
                                notes = "Strong finish on the last 12.",
                                distance = null,
                                title = "Range · 20yd",
                                rings = listOf(10, 11, 10, 9, 11, 10, 10, 9, 10, 11, 10, 10),
                                avgRing = 9.8,
                                xCount = 3,
                                xPct = 25,
                                previousAvg = 9.2,
                                isBest = true,
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

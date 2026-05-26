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
    onOpenSession: (sessionId: String, isThreeDCourse: Boolean) -> Unit = { _, _ -> },
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
    onOpenSession: (sessionId: String, isThreeDCourse: Boolean) -> Unit = { _, _ -> },
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
                    items(group.rows, keyPrefix = "row") { row ->
                        SessionLogRow(
                            row = row,
                            onTap = { onOpenSession(row.id, row.isThreeDCourse) },
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

// Keyed-row helper — wraps forEach in LazyListScope.item with a stable
// "<keyPrefix>-<id>" key. The legacy `isLast` flag was dropped along
// with the bespoke SessionLogRow chrome; the ActivityCard wrapper draws
// its own border.
private inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    rows: List<SessionRow>,
    keyPrefix: String,
    crossinline content: @Composable (row: SessionRow) -> Unit,
) {
    rows.forEach { row ->
        item(key = "$keyPrefix-${row.id}") { content(row) }
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

/**
 * iOS parity (A5) — render a Log-tab session row as a thin wrapper around
 * [ActivityCard]. Mirrors iOS commit 5fe1ba7 where `SessionLogRow` started
 * wrapping the shared `ActivityCardHeader` + `ActivityCardRangeBody`
 * composables instead of carrying bespoke 3-column ArrowBars chrome.
 *
 * `reactions = null` suppresses the like/comment bar (Log rows are local
 * sessions, not shared posts). The card-level tap routes to the
 * session-detail screen via [onTap]; the 3-dot overflow on the header
 * surfaces [onEdit] / [onDelete] so a mis-logged session can still be
 * fixed or removed after the row migrated to ActivityCard chrome.
 */
@Composable
private fun SessionLogRow(
    row: SessionRow,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val item = remember(row) { row.toActivityItem() }
    com.andrewnguyen.bowpress.feature.social.ui.feed.ActivityCard(
        item = item,
        onClick = onTap,
        // No location popup needed for a local Log row — sessions
        // don't carry a tagged location until they're shared.
        onLocationTap = {},
        // iOS parity (A5) — local Log rows aren't likeable/commentable;
        // passing null suppresses the reactions bar entirely.
        reactions = null,
        // Edit / Delete surface through the ActivityCard header's
        // 3-dot overflow — restores the affordances the legacy
        // SessionLogRow exposed inline before the refactor.
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

/**
 * iOS parity (A5) — synthesises a feed-style [ActivityItem] from a local
 * [SessionRow] so ActivityCard can render it. Mirrors the iOS pattern
 * where `SessionLogRow` builds an in-memory `ActivityItem` for its child
 * `ActivityCardHeader` + `ActivityCardRangeBody`. The actor is the
 * archer themselves ("You") because a Log row is by definition own
 * activity.
 */
private fun SessionRow.toActivityItem(): com.andrewnguyen.bowpress.core.model.ActivityItem {
    val rings = this.rings
    val score = rings.sumOf { minOf(it, 10) }
    val totalArrows = this.arrowCount
    val faceLabel = if (this.isThreeDCourse) "3D Course" else (this.title ?: "Range")
    val session = com.andrewnguyen.bowpress.core.model.ActivitySession(
        // sharedSessionId is unused for local rows (no comments/likes
        // attach) — reuse the session id so the ActivityCard's subject
        // lookup still produces a stable key.
        sharedSessionId = this.id,
        sessionId = this.id,
        score = score,
        xCount = this.xCount,
        arrowCount = totalArrows,
        distance = this.distance?.label,
        face = faceLabel,
        discipline = if (this.isThreeDCourse) "3d_course" else "range",
        // Chunk the per-arrow rings into 3-arrow ends — the standard
        // WA shoot end size and what the feed-row scorecard expects.
        // Empty list → ActivityCard falls back to a synthesised
        // ledger.
        endRings = rings.takeIf { it.isNotEmpty() }
            ?.chunked(3)
            ?.take(10),
    )
    return com.andrewnguyen.bowpress.core.model.ActivityItem(
        id = this.id,
        // iOS parity (A5) — kind tracks PR-ness so any future code that
        // branches on it (filters, deep-links, a11y) behaves correctly.
        // PR stamp surfaces from `stamp` below, not from kind, but the
        // semantic typing has to match.
        kind = if (this.isBest) {
            com.andrewnguyen.bowpress.core.model.ActivityKind.friend_pr
        } else {
            com.andrewnguyen.bowpress.core.model.ActivityKind.friend_session
        },
        sourceKind = com.andrewnguyen.bowpress.core.model.ActivitySourceKind.friend,
        actorHandle = "you",
        actorDisplayName = "You",
        title = this.title ?: faceLabel,
        // Use the bow name + arrow config as the meta line so the
        // header still surfaces the equipment context.
        meta = this.bowName,
        stamp = if (this.isBest) "PR" else null,
        createdAt = this.startedAt,
        session = session,
        // Local rows have no shared-session id; mark as own activity so
        // any future affordances (edit, delete) gate correctly.
        isOwn = true,
        titleIsCustom = this.title != null,
        actorUserId = "",
        subjectId = this.id,
    )
}

// iOS parity (A5) — the legacy 3-column ArrowBars / MetaLine / noteExcerpt
// helpers have been removed alongside the bespoke SessionLogRow chrome.
// The replacement is the [SessionLogRow] composable above which delegates
// to feature-social's `ActivityCard` (`reactions = null`).

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

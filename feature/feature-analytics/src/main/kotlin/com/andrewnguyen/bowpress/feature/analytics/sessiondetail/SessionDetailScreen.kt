package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
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
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppPondLt
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintBlack
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintBlue
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintBlueLt
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintGold
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintMiss
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintRed
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintRedLt
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintX
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintYellow
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPlottedTarget
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ArrowPlot

/**
 * Session detail. Mirrors iOS `SessionDetailSheet` after the Kenrokuen
 * redesign (HistoricalSessionsView.swift): the page reads as one continuous
 * stack — title header → scorecard → shot distribution → notes → feel —
 * separated by manual hairlines rather than grouped cards.
 *
 *  - Title header: the session's own name in italic Fraunces, distance in
 *    ALL-CAPS Inter underneath, replacing a generic nav title.
 *  - Scorecard: a ruled per-end table (END · shots · SUM · RUN · XS) with
 *    ring-tonal cell tints and a reversed TOTAL row.
 *  - Tapping a scorecard row scopes the shot distribution to that end;
 *    tapping a shot cell opens [ArrowEditSheet] to re-score / delete it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingArrow by remember { mutableStateOf<EditingArrow?>(null) }
    // Selected end scopes the shot distribution; null shows the whole session.
    var selectedEndId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            // Empty title — the in-content `TitleHeader` is the page anchor,
            // matching iOS dropping the generic "Session Detail" nav label.
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { /* future: edit metadata */ }) {
                        Text("Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPaper,
                    navigationIconContentColor = AppInk,
                    actionIconContentColor = AppPondDk,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppPaper),
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = BowPressColors.Accent) }
            } else {
                SessionDetailContent(
                    state = state,
                    selectedEndId = selectedEndId,
                    onToggleEnd = { id ->
                        selectedEndId = if (selectedEndId == id) null else id
                    },
                    onArrowTap = { number, arrow ->
                        editingArrow = EditingArrow(number = number, arrow = arrow)
                    },
                )
            }
        }
    }

    editingArrow?.let { editing ->
        ArrowEditSheet(
            arrow = editing.arrow,
            arrowNumber = editing.number,
            faceType = state.faceType,
            onReplotRing = { ring, zone ->
                // Keypad re-score: pass null plot coords so the VM snaps the
                // dot to the new ring's midline along the existing bearing.
                viewModel.replotArrow(editing.arrow.id, ring, zone, null, null)
            },
            onDelete = { viewModel.deleteArrow(editing.arrow.id) },
            onDismiss = { editingArrow = null },
        )
    }
}

private data class EditingArrow(val number: Int, val arrow: ArrowPlot)

@Composable
private fun SessionDetailContent(
    state: SessionDetailUiState,
    selectedEndId: String?,
    onToggleEnd: (String) -> Unit,
    onArrowTap: (number: Int, arrow: ArrowPlot) -> Unit,
) {
    // Derived once per data change rather than per recomposition — `build`
    // sorts + groups all arrows, and an end-selection tap recomposes.
    val scorecard = remember(state.arrows, state.ends) { state.scorecard }
    // Global 1-based chronological index for every arrow — the number the
    // ArrowEditSheet caption shows. Stable regardless of how ends group.
    val arrowNumbers = remember(state.arrows) {
        state.arrows.sortedBy { it.shotAt }
            .mapIndexed { i, p -> p.id to (i + 1) }
            .toMap()
    }
    val selectedLine = scorecard.lines.firstOrNull { it.end.id == selectedEndId }
    val displayedArrows = selectedLine?.arrows ?: state.arrows

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TitleHeader(title = state.displayTitle, distance = state.distance?.label)
        SectionDivider()

        // Scorecard
        if (scorecard.lines.isNotEmpty()) {
            Section {
                SectionEyebrow(
                    "SCORECARD · ${scorecard.endCount} ENDS · " +
                        "${scorecard.maxShotsPerEnd} ARROWS/END",
                )
                ScorecardTable(
                    scorecard = scorecard,
                    selectedEndId = selectedEndId,
                    arrowNumbers = arrowNumbers,
                    onTapEnd = onToggleEnd,
                    onTapArrow = onArrowTap,
                )
            }
            SectionDivider()
        }

        // Shot distribution — scoped to the selected end when one is tapped
        // in the scorecard above. A multi-spot session draws the 3-spot Vegas
        // card via BPPlottedTarget (each arrow on its actual spot, scored
        // per-spot); a single-face session keeps the 10-ring ShotDistributionTarget.
        if (state.arrows.isNotEmpty()) {
            Section {
                SectionEyebrow(
                    if (selectedLine == null) "SHOT DISTRIBUTION"
                    else "END ${selectedLine.end.endNumber}",
                )
                if (state.targetLayout.isMultiSpot) {
                    BPPlottedTarget(
                        arrows = displayedArrows,
                        faceType = state.faceType,
                        layout = state.targetLayout,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                } else {
                    ShotDistributionTarget(
                        arrows = displayedArrows,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (selectedLine != null) {
                    EndScopeCaption(line = selectedLine)
                }
            }
            SectionDivider()
        }

        // Notes
        if (state.notes.isNotBlank()) {
            Section {
                SectionEyebrow("NOTES")
                Text(
                    text = state.notes,
                    style = interUI(14.sp).copy(color = AppInk),
                )
            }
            SectionDivider()
        }

        // Feel tags
        if (state.feelTags.isNotEmpty()) {
            Section {
                SectionEyebrow("FEEL")
                FeelTagRow(tags = state.feelTags)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Page chrome
// ---------------------------------------------------------------------------

/** Italic Fraunces session name with the distance in ALL-CAPS Inter under it. */
@Composable
private fun TitleHeader(title: String, distance: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = frauncesDisplay(28.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = AppInk),
        )
        if (distance != null) {
            Text(
                text = distance.uppercase(),
                style = interUI(10.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.22.em,
                    color = AppPondDk,
                ),
            )
        }
    }
}

/** Hairline between detail sections — keeps the page one continuous stack. */
@Composable
private fun SectionDivider() {
    HorizontalDivider(thickness = 1.dp, color = AppLine)
}

/** Section block: 16dp side padding, 14dp vertical, 10dp inner spacing. */
@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** ALL-CAPS pond eyebrow heading a section. */
@Composable
private fun SectionEyebrow(label: String) {
    Text(
        text = label,
        style = interUI(10.sp, weight = FontWeight.SemiBold).copy(
            letterSpacing = 0.22.em,
            color = AppPondDk,
        ),
    )
}

/** "X arrows · Y.Y avg" caption shown when the scorecard scopes to one end. */
@Composable
private fun EndScopeCaption(line: Scorecard.Line) {
    val arrows = line.arrows
    val avg = if (arrows.isEmpty()) 0.0 else line.sum.toDouble() / arrows.size
    Text(
        text = "${arrows.size} arrows · ${"%.1f".format(avg)} avg · " +
            "tap the row again to show all",
        style = interUI(11.sp).copy(color = AppInk3),
    )
}

/** Square bordered feel-tag chips, wrapping across lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeelTagRow(tags: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .border(1.dp, AppLine)
                    .background(AppPaper2)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = tag,
                    style = interUI(12.sp, weight = FontWeight.Medium).copy(color = AppInk2),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scorecard table
// ---------------------------------------------------------------------------

// Fixed column widths — single source of truth so the TOTAL row's merged
// score cell stays aligned with the per-row SUM+RUN pair (mirrors iOS `Col`).
private val ColEnd = 52.dp
private val ColSum = 48.dp
private val ColRun = 54.dp
private val ColXs = 36.dp
private val ColSumPlusRun = ColSum + ColRun

private val RowHeight = 32.dp
private val HeaderHeight = 24.dp
private val TotalHeight = 38.dp

/**
 * Per-end scoring table: END column, N shot cells per end, SUM, running
 * total (RUN), and Xs. Shot cells carry a ring-tonal tint; the TOTAL row is
 * reversed (ink ground, paper text) and renders score / max-possible.
 * Mirrors iOS `ScorecardTable` (Analytics/SessionDetailComponents.swift).
 */
@Composable
private fun ScorecardTable(
    scorecard: Scorecard,
    selectedEndId: String?,
    arrowNumbers: Map<String, Int>,
    onTapEnd: (String) -> Unit,
    onTapArrow: (number: Int, arrow: ArrowPlot) -> Unit,
) {
    val shotCols = scorecard.maxShotsPerEnd
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .border(1.dp, AppLine),
    ) {
        ScorecardHeader(shotCols = shotCols)
        // Running total accumulates down the lines.
        var running = 0
        scorecard.lines.forEachIndexed { idx, line ->
            running += line.sum
            ScorecardRow(
                line = line,
                running = running,
                shotCols = shotCols,
                isSelected = line.end.id == selectedEndId,
                arrowNumbers = arrowNumbers,
                onTapEnd = { onTapEnd(line.end.id) },
                onTapArrow = onTapArrow,
            )
            if (idx < scorecard.lines.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = AppLine2)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = AppLine)
        ScorecardTotalRow(scorecard = scorecard, shotCols = shotCols)
    }
}

@Composable
private fun ScorecardHeader(shotCols: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .background(AppPaper2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("END", Modifier.width(ColEnd))
        repeat(shotCols) { HeaderCell("·", Modifier.weight(1f)) }
        HeaderCell("SUM", Modifier.width(ColSum))
        HeaderCell("RUN", Modifier.width(ColRun))
        HeaderCell("XS", Modifier.width(ColXs))
    }
    HorizontalDivider(thickness = 0.5.dp, color = AppLine)
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = interUI(8.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.22.em,
                color = AppInk3,
            ),
        )
    }
}

@Composable
private fun ScorecardRow(
    line: Scorecard.Line,
    running: Int,
    shotCols: Int,
    isSelected: Boolean,
    arrowNumbers: Map<String, Int>,
    onTapEnd: () -> Unit,
    onTapArrow: (number: Int, arrow: ArrowPlot) -> Unit,
) {
    // `line.arrows` is already in shotAt order (see `Scorecard.build`); pad
    // with nulls so the row always renders `shotCols` cells.
    val shots: List<ArrowPlot?> =
        line.arrows + List((shotCols - line.arrows.size).coerceAtLeast(0)) { null }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(if (isSelected) AppPaper2 else AppPaper)
            .then(if (isSelected) Modifier.border(1.dp, AppPondDk) else Modifier)
            .clickable(onClick = onTapEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // End number
        Box(
            modifier = Modifier
                .width(ColEnd)
                .fillMaxHeight()
                .background(AppPaper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${line.end.endNumber}",
                style = frauncesDisplay(13.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppPondDk),
            )
        }
        // Shot cells
        shots.forEach { arrow ->
            ShotCell(
                arrow = arrow,
                modifier = Modifier.weight(1f),
                onTap = arrow?.let { a -> { onTapArrow(arrowNumbers[a.id] ?: 0, a) } },
            )
        }
        // SUM / RUN / XS
        NumberCell("${line.sum}", ColSum, jetbrainsMono(11.sp), AppInk2)
        NumberCell("$running", ColRun, jetbrainsMono(11.sp), AppInk)
        NumberCell("${line.xCount}", ColXs, jetbrainsMono(11.sp), AppPondDk)
    }
}

@Composable
private fun NumberCell(text: String, width: Dp, style: TextStyle, color: Color) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(AppPaper2),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = style.copy(color = color))
    }
}

@Composable
private fun ShotCell(arrow: ArrowPlot?, modifier: Modifier, onTap: (() -> Unit)?) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(ringTint(arrow?.ring))
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (arrow != null) {
            val (text, color) = when {
                arrow.ring == 11 -> "X" to AppPondDk
                arrow.ring <= 0 -> "M" to AppMaple
                else -> "${arrow.ring}" to AppInk
            }
            Text(
                text = text,
                style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = color),
            )
        } else {
            Text(
                text = "·",
                style = interUI(11.sp).copy(color = AppInk3),
            )
        }
    }
}

@Composable
private fun ScorecardTotalRow(scorecard: Scorecard, shotCols: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TotalHeight)
            .background(AppInk),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(ColEnd), contentAlignment = Alignment.Center) {
            Text(
                text = "TOTAL",
                maxLines = 1,
                style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.18.em,
                    color = AppPaper.copy(alpha = 0.6f),
                ),
            )
        }
        repeat(shotCols) { Box(Modifier.weight(1f)) }
        // Score / max-possible spans the merged SUM+RUN cells.
        Box(modifier = Modifier.width(ColSumPlusRun), contentAlignment = Alignment.Center) {
            Text(text = totalScoreText(scorecard))
        }
        Box(modifier = Modifier.width(ColXs), contentAlignment = Alignment.Center) {
            Text(
                text = "${scorecard.totalXCount}X",
                style = jetbrainsMono(11.sp).copy(color = AppPondLt),
            )
        }
    }
}

/** "715 / 720" — score in bold italic, the max trailing it, muted. */
private fun totalScoreText(scorecard: Scorecard) = buildAnnotatedString {
    withStyle(
        frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium)
            .copy(color = AppPaper).toSpanStyle(),
    ) { append("${scorecard.totalScore}") }
    if (scorecard.maxPossibleScore > 0) {
        withStyle(
            frauncesDisplay(16.sp, italic = true, weight = FontWeight.Normal)
                .copy(color = AppPaper.copy(alpha = 0.5f)).toSpanStyle(),
        ) { append(" / ") }
        withStyle(
            frauncesDisplay(14.sp, italic = true, weight = FontWeight.Normal)
                .copy(color = AppPaper.copy(alpha = 0.65f)).toSpanStyle(),
        ) { append("${scorecard.maxPossibleScore}") }
    }
}

/**
 * Tonal cell tint for a ring score. X is the deepest gold, 10 gold, 9 a pale
 * yellow; 8/7 drift into the red band, 6/5 into blue. Mirrors iOS
 * `ScorecardTable.ringBackground`.
 */
private fun ringTint(ring: Int?): Color = when (ring) {
    null -> Color.Transparent
    11 -> AppRingTintX
    10 -> AppRingTintGold
    9 -> AppRingTintYellow
    8 -> AppRingTintRed
    7 -> AppRingTintRedLt
    6 -> AppRingTintBlue
    5 -> AppRingTintBlueLt
    in 1..4 -> AppRingTintBlack
    else -> AppRingTintMiss // miss / 0
}

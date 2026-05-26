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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPlottedTarget
import com.andrewnguyen.bowpress.core.designsystem.bp.ScorecardTable
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Scorecard

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
            distance = state.distance,
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
                        // §B3 — 6-ring at 50/70m → Outdoor80 7-zone face.
                        sixRingVariant = sixRingVariantForDistance(state.distance),
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

/**
 * §B3 — pick the SixRing visual variant by session distance: 50m / 70m
 * outdoor → 7-zone Outdoor80, everything else → Vegas. Mirrors iOS
 * `sixRingStyleForCurrentDistance`. Only consulted on sixRing sessions;
 * tenRing rendering is distance-invariant.
 */
private fun sixRingVariantForDistance(
    distance: com.andrewnguyen.bowpress.core.model.ShootingDistance?,
): com.andrewnguyen.bowpress.core.designsystem.bp.SixRingVariant = when (distance) {
    com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_50,
    com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_70,
    -> com.andrewnguyen.bowpress.core.designsystem.bp.SixRingVariant.Outdoor80
    com.andrewnguyen.bowpress.core.model.ShootingDistance.YARDS_20, null,
    -> com.andrewnguyen.bowpress.core.designsystem.bp.SixRingVariant.Vegas
}

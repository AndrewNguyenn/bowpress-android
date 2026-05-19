package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppWABlackFill
import com.andrewnguyen.bowpress.core.designsystem.AppWABlueFill
import com.andrewnguyen.bowpress.core.designsystem.AppWAGoldFill
import com.andrewnguyen.bowpress.core.designsystem.AppWARedFill
import com.andrewnguyen.bowpress.core.designsystem.AppWAWhiteFill
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ArrowPlot

/**
 * Session detail. Mirrors iOS `SessionDetailSheet`:
 *
 *  - "Shot distribution" header + arrow/end caption
 *  - Per-arrow chip tap → ArrowEditSheet (replot ring / delete)
 *  - Group precision stats (mean dist, group σ)
 *  - End-by-end breakdown
 *
 * Tap-to-replot positional editing is deferred to the pen-magnifier port
 * (task #28); for now the keypad in ArrowEditSheet can re-score, which is
 * the most-requested flow.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Detail") },
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 14.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Shot distribution",
                                style = frauncesDisplay(22.sp, italic = true).copy(color = AppInk),
                            )
                            val arrowLabel = if (state.arrowCount == 1) "1 arrow" else "${state.arrowCount} arrows"
                            val endLabel = if (state.endCount == 1) "1 end" else "${state.endCount} ends"
                            Text(
                                text = "All $arrowLabel · $endLabel",
                                style = jetbrainsMono(11.sp).copy(
                                    letterSpacing = 0.04.em,
                                    color = AppInk3,
                                ),
                            )
                            ShotDistributionTarget(
                                arrows = state.arrows,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            PrecisionStatsRow(stats = state.precision)
                        }
                    }

                    // Per-end breakdown — iOS HistoricalSessionsView ends-rows.
                    // Each row lists the arrows for that end as tappable chips.
                    val endsByNumber = endRows(state.arrows, state.ends)
                    items(
                        count = endsByNumber.size,
                        key = { idx -> endsByNumber[idx].endLabel },
                    ) { idx ->
                        EndRow(
                            row = endsByNumber[idx],
                            onArrowTap = { number, arrow ->
                                editingArrow = EditingArrow(number = number, arrow = arrow)
                            },
                        )
                    }
                }
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
                // Otherwise precision stats would compute from a position
                // that doesn't match the displayed ring.
                viewModel.replotArrow(editing.arrow.id, ring, zone, null, null)
            },
            onDelete = { viewModel.deleteArrow(editing.arrow.id) },
            onDismiss = { editingArrow = null },
        )
    }
}

private data class EditingArrow(val number: Int, val arrow: ArrowPlot)

private data class EndRowData(
    val endLabel: String,
    val arrows: List<Pair<Int, ArrowPlot>>, // (arrowNumber, plot)
    val sumScore: Int,
)

/**
 * Group arrows by endId, fallback to "Unassigned" for plots with no end ref.
 * Arrow numbers are 1-indexed globally (matches iOS — the index reflects the
 * arrow's position across the whole session, not within its end, so the
 * ArrowEditSheet caption is stable when ends get reshuffled).
 */
private fun endRows(arrows: List<ArrowPlot>, ends: List<com.andrewnguyen.bowpress.core.model.SessionEnd>): List<EndRowData> {
    val endByNumber = ends.associate { it.id to it.endNumber }
    val grouped = arrows
        .mapIndexed { i, p -> (i + 1) to p }
        .groupBy { it.second.endId }
    return grouped.entries
        .sortedBy { (endId, _) ->
            endId?.let { endByNumber[it] } ?: Int.MAX_VALUE
        }
        .map { (endId, pairs) ->
            val endNumber = endId?.let { endByNumber[it] }
            val label = endNumber?.let { "END $it" } ?: "UNASSIGNED"
            EndRowData(
                endLabel = label,
                arrows = pairs,
                sumScore = pairs.sumOf { it.second.ring.let { r -> if (r == 11) 10 else r } },
            )
        }
}

@Composable
private fun EndRow(
    row: EndRowData,
    onArrowTap: (number: Int, arrow: ArrowPlot) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppCream)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.endLabel,
                style = interUI(11.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.22.em,
                    color = AppInk3,
                ),
            )
            Text(
                text = "${row.sumScore}",
                style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.arrows.forEach { (number, arrow) ->
                ArrowChip(
                    number = number,
                    arrow = arrow,
                    onTap = { onArrowTap(number, arrow) },
                )
            }
        }
    }
}

@Composable
private fun ArrowChip(number: Int, arrow: ArrowPlot, onTap: () -> Unit) {
    val ring = arrow.ring
    val fill = ringColor(ring)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(fill)
            .border(1.dp, AppLine, RoundedCornerShape(6.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("session_detail_arrow_$number"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "#$number",
            style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.18.em,
                color = onColorFor(ring),
            ),
        )
        Text(
            text = when (ring) {
                11 -> "X"
                0 -> "M"
                else -> ring.toString()
            },
            style = frauncesDisplay(16.sp, italic = true, weight = FontWeight.Medium).copy(
                color = onColorFor(ring),
            ),
        )
    }
}

/** Mirrors HistoricalSessionsScreen.ArrowBars barFill. */
private fun ringColor(ring: Int): Color = when (ring) {
    11, 10, 9 -> AppWAGoldFill
    8, 7 -> AppWARedFill
    6, 5 -> AppWABlueFill
    4, 3 -> AppWABlackFill
    else -> AppWAWhiteFill
}

/** Contrast ink for chips: dark on gold/red/white, light on black/blue. */
private fun onColorFor(ring: Int): Color = when (ring) {
    4, 3 -> AppPaper
    6, 5 -> AppPaper
    else -> AppInk
}

@Composable
private fun PrecisionStatsRow(stats: PrecisionStats?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val centerChip = if (stats != null) {
            "${stats.directionArrow} ${"%.1f".format(stats.meanDistMm)}MM FROM CENTER"
        } else {
            "— MM FROM CENTER"
        }
        val sigmaChip = if (stats != null) {
            "± ${"%.1f".format(stats.groupSigmaMm)}MM GROUP σ"
        } else {
            "± — MM GROUP σ"
        }
        StatChip(text = centerChip)
        StatChip(text = sigmaChip)
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, AppPondDk)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = interUI(11.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.18.em,
                color = AppPondDk,
            ),
        )
    }
}

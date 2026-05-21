package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintBlack
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintBlue
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintBlueLt
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintGold
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintMiss
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintRed
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintRedLt
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintX
import com.andrewnguyen.bowpress.core.designsystem.AppRingTintYellow
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ArrowPlot

/**
 * Live ends-history scorecard — the running end-by-end table the archer sees
 * while a session is in progress. Mirrors the iOS `SessionView.endsHistory`
 * scorecard (Sources/BowPress/Session/SessionView.swift): a ruled table with
 * an END column, N shot cells, SUM, running total (RUN), and Xs, plus a
 * reversed TOTAL row.
 *
 * Tapping an end row opens its actions (add a missed arrow / delete the end);
 * tapping a shot cell deletes that arrow (the live-session quick-fix path).
 */

private val ColEnd = 48.dp
private val ColSum = 44.dp
private val ColRun = 50.dp
private val ColXs = 34.dp
private val RowHeight = 32.dp
private val HeaderHeight = 24.dp
private val TotalHeight = 36.dp

@Composable
fun EndsScorecard(
    breakdown: SessionEndsBreakdown,
    onTapEnd: (endId: String) -> Unit,
    onTapArrow: (ArrowPlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = breakdown.completedEndLines
    val shotCols = breakdown.maxShotsPerEnd
    val totalArrows = lines.sumOf { it.arrows.size }
    val totalScore = lines.sumOf { it.sum }
    val totalXs = lines.sumOf { it.xCount }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppPaper)
            .border(1.dp, AppLine),
    ) {
        ScorecardHeader(shotCols = shotCols)
        var running = 0
        lines.forEachIndexed { idx, line ->
            running += line.sum
            EndRow(
                line = line,
                running = running,
                shotCols = shotCols,
                onTapEnd = { onTapEnd(line.end.id) },
                onTapArrow = onTapArrow,
            )
            if (idx < lines.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = AppLine2)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = AppLine)
        TotalRow(
            shotCols = shotCols,
            totalScore = totalScore,
            maxPossibleScore = totalArrows * 10,
            totalXs = totalXs,
        )
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
private fun EndRow(
    line: SessionEndsBreakdown.EndLine,
    running: Int,
    shotCols: Int,
    onTapEnd: () -> Unit,
    onTapArrow: (ArrowPlot) -> Unit,
) {
    // `line.arrows` is in shotAt order; pad with nulls so every row renders
    // `shotCols` cells.
    val shots: List<ArrowPlot?> =
        line.arrows + List((shotCols - line.arrows.size).coerceAtLeast(0)) { null }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(AppPaper)
            .clickable(onClick = onTapEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(ColEnd).fillMaxHeight().background(AppPaper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${line.end.endNumber}",
                style = frauncesDisplay(13.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppPondDk),
            )
        }
        shots.forEach { arrow ->
            ShotCell(
                arrow = arrow,
                modifier = Modifier.weight(1f),
                onTap = arrow?.let { a -> { onTapArrow(a) } },
            )
        }
        NumberCell("${line.sum}", ColSum, AppInk2)
        NumberCell("$running", ColRun, AppInk)
        NumberCell("${line.xCount}", ColXs, AppPondDk)
    }
}

@Composable
private fun NumberCell(text: String, width: Dp, color: Color) {
    Box(
        modifier = Modifier.width(width).fillMaxHeight().background(AppPaper2),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = jetbrainsMono(11.sp).copy(color = color))
    }
}

/**
 * Tonal cell tint for a ring score — the band hue at a low alpha so the cell
 * reads as a score cue. Mirrors iOS `ScorecardRow.ringBackground`
 * (SessionDetailComponents.swift). A padding (null) cell stays untinted.
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
    else -> AppRingTintMiss // 0 / miss
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
            Text(text = "·", style = interUI(11.sp).copy(color = AppInk3))
        }
    }
}

@Composable
private fun TotalRow(
    shotCols: Int,
    totalScore: Int,
    maxPossibleScore: Int,
    totalXs: Int,
) {
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
        Box(
            modifier = Modifier.width(ColSum + ColRun),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (maxPossibleScore > 0) "$totalScore / $maxPossibleScore" else "$totalScore",
                style = frauncesDisplay(15.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppPaper),
            )
        }
        Box(modifier = Modifier.width(ColXs), contentAlignment = Alignment.Center) {
            Text(
                text = "${totalXs}X",
                style = jetbrainsMono(11.sp).copy(color = AppPaper.copy(alpha = 0.7f)),
            )
        }
    }
}

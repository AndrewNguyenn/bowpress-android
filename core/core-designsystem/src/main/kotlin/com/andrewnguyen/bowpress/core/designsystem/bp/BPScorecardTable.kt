package com.andrewnguyen.bowpress.core.designsystem.bp

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Scorecard

// ---------------------------------------------------------------------------
// BPScorecardTable — the canonical per-end scorecard.
//
// One ruled table used everywhere a scorecard appears: the session-detail
// screen, the friend-session-detail screen, and (via `ringTint`) the feed-row
// preview. Mirrors iOS `ScorecardTable` (Analytics/SessionDetailComponents).
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
 *
 * Callbacks are optional — pass none for a read-only scorecard (a friend's
 * shared session). When [onTapEnd] is supplied, tapping a row reports its
 * end id (the session-detail screen uses this to scope its heatmap); when
 * [onTapArrow] is supplied, tapping a shot cell reports the arrow and its
 * 1-based chronological number from [arrowNumbers].
 */
@Composable
fun ScorecardTable(
    scorecard: Scorecard,
    modifier: Modifier = Modifier,
    selectedEndId: String? = null,
    arrowNumbers: Map<String, Int> = emptyMap(),
    onTapEnd: ((String) -> Unit)? = null,
    onTapArrow: ((number: Int, arrow: ArrowPlot) -> Unit)? = null,
) {
    val shotCols = scorecard.maxShotsPerEnd
    Column(
        modifier = modifier
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
                onTapEnd = onTapEnd?.let { cb -> { cb(line.end.id) } },
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
    onTapEnd: (() -> Unit)?,
    onTapArrow: ((number: Int, arrow: ArrowPlot) -> Unit)?,
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
            .then(if (onTapEnd != null) Modifier.clickable(onClick = onTapEnd) else Modifier),
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
                onTap = if (arrow != null && onTapArrow != null) {
                    { onTapArrow(arrowNumbers[arrow.id] ?: 0, arrow) }
                } else {
                    null
                },
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
            Text(
                text = ringLabel(arrow.ring),
                style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = ringInk(arrow.ring)),
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

/** X for the inner ring (11), M for a miss (≤0), the ring number otherwise. */
fun ringLabel(ring: Int): String = when {
    ring >= 11 -> "X"
    ring <= 0 -> "M"
    else -> ring.toString()
}

/**
 * Ink colour for a ring glyph drawn on its tinted cell — X reads pond, a
 * miss reads maple, everything else ink.
 */
fun ringInk(ring: Int): Color = when {
    ring >= 11 -> AppPondDk
    ring <= 0 -> AppMaple
    else -> AppInk
}

/**
 * Tonal cell tint for a ring score. X is the deepest gold, 10 gold, 9 a pale
 * yellow; 8/7 drift into the red band, 6/5 into blue. Mirrors iOS
 * `ScorecardTable.ringBackground`. A null ring (an empty padding cell) is
 * transparent.
 */
fun ringTint(ring: Int?): Color = when (ring) {
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

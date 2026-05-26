package com.andrewnguyen.bowpress.feature.analytics.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEditLink
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSectionTitle
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSparkline
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun ScoreTimelineScreen(
    onBack: () -> Unit,
    viewModel: ScoreTimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScoreTimelineContent(state = state, onBack = onBack)
}

@Composable
internal fun ScoreTimelineContent(
    state: ScoreTimelineUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        BPNavHeader(
            title = state.bow?.name?.let { "$it · timeline" } ?: "Score timeline",
            meta = { BPEditLink(label = "Close", onClick = onBack) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Loading timeline…",
                            style = interUI(11.sp).copy(color = AppInk3),
                        )
                    }
                }
                state.points.isEmpty() -> EmptyTimeline()
                else -> TimelineBody(points = state.points)
            }
        }
    }
}

@Composable
private fun TimelineBody(points: List<TimelinePoint>) {
    val scores = points.map { it.score }
    val hi = scores.max()
    val lo = scores.min()
    val sigma = computedSigma(scores)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            BPSectionTitle(title = "Avg arrow score over time")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${points.size} configurations",
                style = jetbrainsMono(9.5.sp).copy(color = AppInk3),
            )
            Text(
                text = "range ${formatScore(lo)}—${formatScore(hi)} · σ ${"%.2f".format(sigma)}",
                style = jetbrainsMono(9.5.sp).copy(color = AppInk3),
            )
        }

        BPSparkline(points = scores, height = 140.dp)

        // X-axis: first, mid, two-thirds, right-anchored "now".
        val labels: List<String> = run {
            val count = points.size
            val first = points.first()
            val mid = points[count / 2]
            val twoThirds = points[(count * 2) / 3]
            val dateLabel = { d: Instant ->
                val local = d.atZone(ZoneId.systemDefault()).toLocalDate()
                local.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)).lowercase(Locale.US)
            }
            listOf(dateLabel(first.createdAt), dateLabel(mid.createdAt), dateLabel(twoThirds.createdAt), "now")
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = jetbrainsMono(9.sp).copy(color = AppInk3),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))

        ConfigurationLedger(points = points)
    }
}

@Composable
private fun ConfigurationLedger(points: List<TimelinePoint>) {
    val best = points.maxByOrNull { it.score }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Text(
                text = "CONFIG",
                style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.22.em,
                    color = AppInk3,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "CREATED",
                style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.22.em,
                    color = AppInk3,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "AVG",
                style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.22.em,
                    color = AppInk3,
                ),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))
        points.reversed().forEachIndexed { idx, point ->
            LedgerRow(point = point, isBest = point.configId == best?.configId)
            if (idx < points.size - 1) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine2))
            }
        }
    }
}

@Composable
private fun LedgerRow(point: TimelinePoint, isBest: Boolean) {
    val fmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(ZoneId.systemDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = point.label,
            style = frauncesDisplay(14.sp, italic = true).copy(color = AppInk),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = fmt.format(point.createdAt),
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatScore(point.score),
            style = frauncesDisplay(14.sp, italic = true).copy(
                color = if (isBest) AppPondDk else AppInk,
            ),
        )
    }
}

@Composable
private fun EmptyTimeline() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BPEyebrow(text = "Not enough data")
        Text(
            text = "Log sessions to plot score over time.",
            style = frauncesDisplay(18.sp, italic = true).copy(color = AppInk),
        )
        Text(
            text = "Each bow config's avg arrow score shows up here once you log at least one session against it.",
            style = interUI(11.5.sp).copy(color = AppInk2),
        )
    }
}

private fun formatScore(v: Double): String =
    if (v == 0.0) "—" else "%.1f".format(v)

private fun computedSigma(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    val variance = values.map { (it - mean) * (it - mean) }.average()
    return sqrt(variance)
}

// ---------- Preview ----------

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ScoreTimelinePreview() {
    BowPressTheme {
        val now = Instant.now()
        val pts = (0 until 6).map { idx ->
            TimelinePoint(
                configId = "c$idx",
                label = "C$idx",
                createdAt = now.minusSeconds((6 - idx).toLong() * 86_400),
                score = 8.5 + (idx % 3) * 0.5,
            )
        }
        ScoreTimelineContent(
            state = ScoreTimelineUiState(
                bow = null,
                points = pts,
                isLoading = false,
            ),
            onBack = {},
        )
    }
}

package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
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
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppMoss
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppPondLt
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.bp.BPBigScore
import com.andrewnguyen.bowpress.core.designsystem.bp.BPDelta
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPFilterSummary
import com.andrewnguyen.bowpress.core.designsystem.bp.BPHairlineButton
import com.andrewnguyen.bowpress.core.designsystem.bp.BPLedgerRow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSectionTitle
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSparkline
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStamp
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStampTone
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStatGridCell
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFace
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetStyle
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.Centroid
import com.andrewnguyen.bowpress.core.model.DeltaTone
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.DriftResponse
import com.andrewnguyen.bowpress.core.model.DriftRow
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import com.andrewnguyen.bowpress.core.model.ShiftVector
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.SigmaEllipse
import com.andrewnguyen.bowpress.core.model.SparklinePoint
import com.andrewnguyen.bowpress.core.model.SuggestionStatusStamp
import com.andrewnguyen.bowpress.core.model.TimelinePoint
import com.andrewnguyen.bowpress.core.model.TimelineRange
import com.andrewnguyen.bowpress.core.model.TimelineResponse
import com.andrewnguyen.bowpress.core.model.TrendBadge
import com.andrewnguyen.bowpress.core.model.TrendFinding
import com.andrewnguyen.bowpress.core.model.TrendMetric
import com.andrewnguyen.bowpress.core.model.TrendTone
import com.andrewnguyen.bowpress.core.model.TrendsResponse
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Stable test tags the Compose UI test depends on. */
object AnalyticsDashboardTestTags {
    const val DashboardRoot: String = "analytics_dashboard_root"
}

/**
 * Dashboard screen entry point. Resolves the Hilt view-model and drives the state-first
 * composable. Callers pass navigation lambdas so this screen never touches NavController.
 */
@Composable
fun AnalyticsDashboardScreen(
    onOpenSuggestion: (bowId: String, suggestionId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTimeline: (bowId: String) -> Unit,
    viewModel: AnalyticsDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsDashboardContent(
        state = state,
        onPeriodChange = viewModel::selectPeriod,
        onBowTypeChange = viewModel::selectBowType,
        onDistanceChange = viewModel::selectDistance,
        onRetry = viewModel::refresh,
        onOpenSuggestion = onOpenSuggestion,
        onOpenHistory = onOpenHistory,
        onOpenTimeline = onOpenTimeline,
        onDismissSuggestion = viewModel::dismissSuggestion,
        onMarkRead = viewModel::markSuggestionRead,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsDashboardContent(
    state: DashboardUiState,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    onBowTypeChange: (BowType?) -> Unit,
    onDistanceChange: (ShootingDistance?) -> Unit,
    onRetry: () -> Unit,
    onOpenSuggestion: (bowId: String, suggestionId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTimeline: (bowId: String) -> Unit,
    onDismissSuggestion: (String) -> Unit = {},
    onMarkRead: (String) -> Unit = {},
) {
    var filtersOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val bowLabel = state.selectedBowType?.label ?: "All bows"
    val distanceLabel = state.selectedDistance?.label ?: "All distances"
    val summary = "$bowLabel · $distanceLabel · ${state.period.label}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(AnalyticsDashboardTestTags.DashboardRoot),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // 1. Nav header — owns its own 18dp h-padding; the 1dp hairline runs edge-to-edge.
            item {
                BPNavHeader(
                    eyebrow = "Bowpress",
                    title = "Analytics",
                    meta = { DateMetaSlot(sessionCount = state.overview?.sessionCount ?: 0) },
                )
            }

            // 2. Filter summary pill.
            item {
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    BPFilterSummary(
                        summary = summary,
                        subtitle = "tap to change filters",
                        onEdit = { filtersOpen = true },
                    )
                }
            }

            // Error banner.
            state.error?.let { message ->
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                        ErrorBanner(message = message, onRetry = onRetry)
                    }
                }
            }

            val overview = state.overview
            if (overview != null && overview.sessionCount > 0) {
                // 3. Three-col stat grid.
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        StatGrid(
                            overview = overview,
                            comparison = state.comparison,
                            distanceLabel = state.selectedDistance?.label ?: "mixed",
                        )
                    }
                }

                // 4. Prev→Now compare strip.
                state.comparison?.takeIf { it.previous.sessionCount > 0 }?.let { comparison ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                            CompareStrip(comparison = comparison, period = state.period)
                        }
                    }
                }

                // 5. Score timeline.
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        ScoreTimelineSection(timeline = state.timeline, sparkline = overview.sparkline)
                    }
                }

                // 6. Impact map.
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        ImpactMapSection(
                            comparison = state.comparison,
                            distanceLabel = state.selectedDistance?.label ?: "all distances",
                            bowTypeLabel = state.selectedBowType?.label?.lowercase() ?: "all bows",
                        )
                    }
                }

                // 7. Trend analysis.
                state.trends?.let { trends ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                            TrendAnalysisSection(
                                findings = trends.findings,
                                totalArrows = overview.datasetSummary?.arrows
                                    ?: estimatedArrows(overview.sessionCount),
                            )
                        }
                    }
                }

                // 8. Parameter drift.
                state.drift?.let { drift ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                            ParameterDriftSection(drift = drift)
                        }
                    }
                }

                // 9. Suggestions ledger.
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        SuggestionsLedgerSection(
                            suggestions = state.allSuggestions,
                            onOpen = { suggestion ->
                                onMarkRead(suggestion.id)
                                onOpenSuggestion(suggestion.bowId, suggestion.id)
                            },
                            onDismiss = onDismissSuggestion,
                        )
                    }
                }

                // 10. Footnotes.
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        FootnotesGrid(overview = overview)
                    }
                }

                // 11. Colophon.
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Colophon()
                    }
                }
            } else if (!state.isLoading) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                        EmptyState()
                    }
                }
            }

            if (state.isLoading && state.overview == null) {
                item {
                    Box(modifier = Modifier.padding(24.dp)) {
                        LoadingBlock()
                    }
                }
            }
        }
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
                onPeriodChange = onPeriodChange,
                onBowTypeChange = onBowTypeChange,
                onDistanceChange = onDistanceChange,
                onDone = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { filtersOpen = false }
                },
            )
        }
    }
}

// ---------- Nav header meta ----------

@Composable
private fun DateMetaSlot(sessionCount: Int) {
    val today = LocalDate.now()
    val ymd = today.format(DateTimeFormatter.ofPattern("yyyy · MM · dd", Locale.US))
    val weekday = today.format(DateTimeFormatter.ofPattern("EEEE", Locale.US)).lowercase(Locale.US)
    val count = maxOf(sessionCount, 0)
    Column(horizontalAlignment = Alignment.End) {
        Text(text = ymd, style = jetbrainsMono(10.sp).copy(color = AppInk3))
        Text(
            text = weekday,
            style = jetbrainsMono(10.sp, weight = FontWeight.Medium).copy(color = AppInk),
        )
        val anno = buildAnnotatedString {
            withStyle(SpanStyle(color = AppInk3)) { append("session no. ") }
            withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) {
                append("%03d".format(count))
            }
        }
        Text(text = anno, style = jetbrainsMono(10.sp))
    }
}

// ---------- 3-col stat grid ----------

@Composable
private fun StatGrid(
    overview: AnalyticsOverview,
    comparison: PeriodComparison?,
    distanceLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBottomHairline(color = AppLine),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BPStatGridCell(
                label = "Average",
                sub = "per arrow · out of 11",
                modifier = Modifier.weight(1f),
                ticks = { SparkTicks(points = overview.sparkline?.map { it.avg } ?: emptyList()) },
                mainContent = {
                    BPBigScore(value = formatAvg(overview.avgArrowScore), size = 56.sp)
                },
            )
            VerticalRule()
            BPStatGridCell(
                label = "X rate",
                modifier = Modifier.weight(1f),
                mainContent = {
                    val pct = overview.xPercentage.roundToInt()
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$pct",
                            style = frauncesDisplay(28.sp, italic = true).copy(color = AppPondDk),
                        )
                        Text(
                            text = "%",
                            style = frauncesDisplay(17.sp, italic = true).copy(color = AppPondDk),
                        )
                    }
                    val prev = comparison?.previous?.xPercentage
                    Text(
                        text = prev?.let { "prev · ${it.roundToInt()}%" } ?: "prev · —",
                        style = interUI(10.sp).copy(color = AppInk3),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${overview.sessionCount}",
                        style = frauncesDisplay(17.sp, italic = true).copy(color = AppInk),
                    )
                    Text(
                        text = "sessions",
                        style = interUI(10.sp).copy(color = AppInk3),
                    )
                },
            )
            VerticalRule()
            BPStatGridCell(
                label = "Group ⌀",
                modifier = Modifier.weight(1f),
                mainContent = {
                    val sigma = overview.groupSigma
                    val sigmaText = when {
                        sigma == null || sigma == 0.0 -> "—"
                        else -> "%.1f".format(sigma)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = sigmaText,
                            style = frauncesDisplay(28.sp, italic = true).copy(color = AppInk),
                        )
                        Text(
                            text = "″",
                            style = frauncesDisplay(15.sp, italic = true).copy(color = AppInk),
                        )
                    }
                    Text(
                        text = "at $distanceLabel",
                        style = interUI(10.sp).copy(color = AppInk3),
                    )
                    Spacer(Modifier.height(4.dp))
                    val arrows = overview.datasetSummary?.arrows ?: estimatedArrows(overview.sessionCount)
                    Text(
                        text = "$arrows",
                        style = frauncesDisplay(17.sp, italic = true).copy(color = AppInk),
                    )
                    Text(
                        text = "arrows logged",
                        style = interUI(10.sp).copy(color = AppInk3),
                    )
                },
            )
        }
    }
}

@Composable
private fun VerticalRule() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(80.dp)
            .background(AppLine2),
    )
}

/** 9-bar sparkline strip — tallest bar gets pond-dk accent. */
@Composable
private fun SparkTicks(points: List<Double>) {
    if (points.isEmpty()) {
        Text(
            text = "no session data yet",
            style = interUI(9.5.sp).copy(color = AppInk3),
        )
        return
    }
    val trimmed = points.takeLast(9)
    val hi = trimmed.max()
    val lo = trimmed.min()
    val span = (hi - lo).coerceAtLeast(0.5)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp),
    ) {
        val lastMaxIndex = trimmed.indexOfLast { it == hi }
        trimmed.forEachIndexed { idx, v ->
            val norm = (v - lo) / span
            val fraction = (0.35 + norm * 0.6).toFloat()
            val barHeight = (18f * fraction).dp
            val color = if (idx == lastMaxIndex) AppPondDk else AppPondLt
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .background(color),
            )
        }
    }
}

// ---------- Compare strip ----------

@Composable
private fun CompareStrip(comparison: PeriodComparison, period: AnalyticsPeriod) {
    val delta = comparison.current.avgArrowScore - comparison.previous.avgArrowScore
    val periodLabel = prettyPeriod(period)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .drawBottomHairline(color = AppLine),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BPEyebrow(text = "Prev $periodLabel")
            Text(
                text = formatAvg(comparison.previous.avgArrowScore),
                style = frauncesDisplay(20.sp, italic = true).copy(color = AppInk),
            )
            Text(
                text = "${comparison.previous.xPercentage.roundToInt()}% X · ${comparison.previous.sessionCount} sessions",
                style = jetbrainsMono(9.5.sp).copy(color = AppInk3),
            )
        }
        Text(
            text = "→",
            style = frauncesDisplay(22.sp, italic = true).copy(color = AppMoss),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BPEyebrow(text = "This $periodLabel")
            Text(
                text = formatAvg(comparison.current.avgArrowScore),
                style = frauncesDisplay(20.sp, italic = true).copy(color = AppPondDk),
            )
            val deltaStr = if (abs(delta) >= 0.05) {
                (if (delta > 0) "+" else "") + "%.1f".format(delta)
            } else null
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${comparison.current.xPercentage.roundToInt()}% X · ${comparison.current.sessionCount} sessions",
                    style = jetbrainsMono(9.5.sp).copy(color = AppInk3),
                )
                if (deltaStr != null) {
                    Text(
                        text = deltaStr,
                        style = jetbrainsMono(9.5.sp, weight = FontWeight.Medium).copy(
                            color = if (delta > 0) AppPine else AppMaple,
                        ),
                    )
                }
            }
        }
    }
}

private fun prettyPeriod(period: AnalyticsPeriod): String = when (period) {
    AnalyticsPeriod.THREE_DAYS -> "3 days"
    AnalyticsPeriod.WEEK -> "week"
    AnalyticsPeriod.TWO_WEEKS -> "2 weeks"
    AnalyticsPeriod.MONTH -> "month"
    AnalyticsPeriod.THREE_MONTHS -> "3 months"
    AnalyticsPeriod.SIX_MONTHS -> "6 months"
    AnalyticsPeriod.YEAR -> "year"
}

// ---------- Score timeline ----------

@Composable
private fun ScoreTimelineSection(
    timeline: TimelineResponse?,
    sparkline: List<SparklinePoint>?,
) {
    val points: List<TimelinePoint> = timeline?.points ?: emptyList()
    val avgs: List<Double> = when {
        points.isNotEmpty() -> points.map { it.avg }
        sparkline != null -> sparkline.map { it.avg }
        else -> emptyList()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .drawBottomHairline(color = AppLine),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            BPSectionTitle(title = "Score timeline")
            if (avgs.isNotEmpty()) {
                val lo = timeline?.range?.min ?: avgs.min()
                val hi = timeline?.range?.max ?: avgs.max()
                val sigma = timeline?.range?.sigma ?: computedSigma(avgs)
                Text(
                    text = "range ${formatAvg(lo)}—${formatAvg(hi)} · σ ${"%.2f".format(sigma)}",
                    style = jetbrainsMono(9.5.sp).copy(color = AppInk3),
                )
            }
        }
        if (avgs.isEmpty()) {
            Text(
                text = "no session data yet",
                style = interUI(11.sp).copy(color = AppInk3),
                modifier = Modifier.padding(vertical = 14.dp),
            )
        } else {
            BPSparkline(points = avgs, height = 86.dp)
            TimelineXAxis(
                points = points,
                fallbackSparkline = sparkline ?: emptyList(),
            )
        }
    }
}

@Composable
private fun TimelineXAxis(points: List<TimelinePoint>, fallbackSparkline: List<SparklinePoint>) {
    // Prefer TimelinePoint; fall back to sparkline ISO `at`.
    val dates: List<String> = when {
        points.isNotEmpty() -> points.map { it.at }
        else -> fallbackSparkline.map { it.at }
    }
    if (dates.isEmpty()) return
    val count = dates.size
    val first = dates.first()
    val mid = dates[count / 2]
    val twoThirds = dates[(count * 2) / 3]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        Text(
            text = shortDate(first),
            style = jetbrainsMono(9.sp).copy(color = AppInk3),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = shortDate(mid),
            style = jetbrainsMono(9.sp).copy(color = AppInk3),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = shortDate(twoThirds),
            style = jetbrainsMono(9.sp).copy(color = AppInk3),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "now",
            style = jetbrainsMono(9.sp).copy(color = AppInk3),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun shortDate(iso: String): String {
    return runCatching {
        val instant = Instant.parse(iso)
        val local = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        local.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)).lowercase(Locale.US)
    }.getOrDefault("—")
}

private fun computedSigma(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    val variance = values.map { (it - mean) * (it - mean) }.average()
    return sqrt(variance)
}

// ---------- Impact map ----------

@Composable
private fun ImpactMapSection(
    comparison: PeriodComparison?,
    distanceLabel: String,
    bowTypeLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp)
            .drawBottomHairline(color = AppLine),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BPSectionTitle(title = "Impact map", aside = "$distanceLabel · $bowTypeLabel")
        Text(
            text = "centroid of grouping · this week vs. previous · 1 ring = 1 point",
            style = interUI(10.5.sp).copy(color = AppInk3),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(200.dp),
            ) {
                BPTargetFace(size = 200.dp, style = BPTargetStyle.ImpactMap)
                ImpactMapOverlay(
                    sizeDp = 200,
                    previous = comparison?.previous,
                    current = comparison?.current,
                )
            }
            ImpactLegend(
                previous = comparison?.previous,
                current = comparison?.current,
                shift = comparison?.shift,
                modifier = Modifier.width(130.dp),
            )
        }
    }
}

@Composable
private fun ImpactMapOverlay(
    sizeDp: Int,
    previous: PeriodSlice?,
    current: PeriodSlice?,
) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val wpx = size.width
        val half = wpx / 2f

        fun nx(x: Double): Float = half + x.toFloat() * half
        fun ny(y: Double): Float = half - y.toFloat() * half

        // Previous ellipse — dashed cream outline.
        val prevSigma = previous?.sigma
        val prevCentroid = previous?.centroid
        if (prevSigma != null && prevCentroid != null) {
            drawRotatedEllipse(
                center = Offset(nx(prevCentroid.x), ny(prevCentroid.y)),
                widthPx = (wpx * prevSigma.major.toFloat()).coerceAtLeast(2f),
                heightPx = (wpx * prevSigma.minor.toFloat()).coerceAtLeast(2f),
                rotationDeg = prevSigma.rotationDeg.toFloat(),
                color = AppCream,
                strokeWidthPx = 1f,
                dashed = true,
            )
        }

        // Previous arrow dots (cream translucent).
        previous?.plots?.take(18)?.forEach { plot ->
            val x = plot.plotX ?: return@forEach
            val y = plot.plotY ?: return@forEach
            drawCircle(
                color = AppCream.copy(alpha = 0.7f),
                radius = 1.8.dp.toPx(),
                center = Offset(nx(x), ny(y)),
            )
        }

        // Previous centroid — hollow stone ring.
        if (prevCentroid != null) {
            val c = Offset(nx(prevCentroid.x), ny(prevCentroid.y))
            drawCircle(color = AppPaper, radius = 3.4.dp.toPx(), center = c)
            drawCircle(
                color = AppInk2,
                radius = 3.4.dp.toPx(),
                center = c,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }

        val curSigma = current?.sigma
        val curCentroid = current?.centroid
        if (curSigma != null && curCentroid != null) {
            drawRotatedEllipse(
                center = Offset(nx(curCentroid.x), ny(curCentroid.y)),
                widthPx = (wpx * curSigma.major.toFloat()).coerceAtLeast(2f),
                heightPx = (wpx * curSigma.minor.toFloat()).coerceAtLeast(2f),
                rotationDeg = curSigma.rotationDeg.toFloat(),
                color = AppMaple,
                strokeWidthPx = 1f,
                dashed = false,
            )
        }

        current?.plots?.take(18)?.forEach { plot ->
            val x = plot.plotX ?: return@forEach
            val y = plot.plotY ?: return@forEach
            drawCircle(
                color = AppMaple.copy(alpha = 0.95f),
                radius = 1.8.dp.toPx(),
                center = Offset(nx(x), ny(y)),
            )
        }

        if (curCentroid != null) {
            val c = Offset(nx(curCentroid.x), ny(curCentroid.y))
            drawCircle(color = AppMaple, radius = 3.6.dp.toPx(), center = c)
            drawCircle(
                color = AppPaper,
                radius = 3.6.dp.toPx(),
                center = c,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }

        // Moss shift arrow prev → current.
        if (prevCentroid != null && curCentroid != null) {
            val p1 = Offset(nx(prevCentroid.x), ny(prevCentroid.y))
            val p2 = Offset(nx(curCentroid.x), ny(curCentroid.y))
            drawLine(
                color = AppMoss,
                start = p1,
                end = p2,
                strokeWidth = 1.3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            // Arrowhead.
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            val ux = dx / len
            val uy = dy / len
            val arrowSize = 5.dp.toPx()
            val px = -uy
            val py = ux
            val base = Offset(p2.x - ux * arrowSize, p2.y - uy * arrowSize)
            val left = Offset(base.x + px * arrowSize * 0.55f, base.y + py * arrowSize * 0.55f)
            val right = Offset(base.x - px * arrowSize * 0.55f, base.y - py * arrowSize * 0.55f)
            val arrow = Path().apply {
                moveTo(p2.x, p2.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            drawPath(path = arrow, color = AppMoss)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRotatedEllipse(
    center: Offset,
    widthPx: Float,
    heightPx: Float,
    rotationDeg: Float,
    color: Color,
    strokeWidthPx: Float,
    dashed: Boolean,
) {
    val rx = widthPx / 2f
    val ry = heightPx / 2f
    // Sample 48 points around an ellipse rotated by rotationDeg, then stroke.
    val steps = 48
    val path = Path()
    val rad = Math.toRadians(rotationDeg.toDouble())
    val cosR = cos(rad).toFloat()
    val sinR = sin(rad).toFloat()
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * (2.0 * Math.PI).toFloat()
        val xLocal = rx * cos(t)
        val yLocal = ry * sin(t)
        val xR = xLocal * cosR - yLocal * sinR
        val yR = xLocal * sinR + yLocal * cosR
        val x = center.x + xR
        val y = center.y + yR
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(3f, 3f)) else null,
        ),
    )
}

@Composable
private fun ImpactLegend(
    previous: PeriodSlice?,
    current: PeriodSlice?,
    shift: ShiftVector?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LegendRow(
            dotFill = null,
            dotBorder = AppInk2,
            name = "Previous",
            stat = previous?.let { "${it.sessionCount} sess · ${it.plots.size} arr" } ?: "—",
        )
        LegendRow(
            dotFill = AppMaple,
            dotBorder = null,
            name = "This week",
            stat = current?.let { "${it.sessionCount} sess · ${it.plots.size} arr" } ?: "—",
        )
        Spacer(Modifier.height(4.dp))
        BPEyebrow(text = "Shift")
        Text(
            text = shift?.let { shiftLine(it) } ?: "not enough data",
            style = frauncesDisplay(13.sp, italic = true).copy(color = AppPine),
        )
        if (shift != null && shift.description.isNotEmpty()) {
            Text(
                text = shift.description,
                style = jetbrainsMono(9.sp).copy(color = AppInk3),
            )
        }
    }
}

private fun shiftLine(shift: ShiftVector): String {
    val dx = if (shift.dxMm == 0.0) "0" else "%+.0f".format(shift.dxMm)
    val dy = if (shift.dyMm == 0.0) "0" else "%+.0f".format(shift.dyMm)
    return "$dx, $dy mm"
}

@Composable
private fun LegendRow(
    dotFill: Color?,
    dotBorder: Color?,
    name: String,
    stat: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .drawBottomHairline(color = AppLine2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(9.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                if (dotFill != null) {
                    drawCircle(color = dotFill, radius = r, center = center)
                }
                if (dotBorder != null) {
                    drawCircle(
                        color = dotBorder,
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx()),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = frauncesDisplay(12.5.sp, italic = true).copy(color = AppInk),
            )
            Text(
                text = stat,
                style = jetbrainsMono(9.5.sp).copy(color = AppInk3),
            )
        }
    }
}

// ---------- Trend analysis ----------

@Composable
private fun TrendAnalysisSection(findings: List<TrendFinding>, totalArrows: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BPSectionTitle(
            title = "Trend analysis",
            aside = if (findings.isEmpty()) null else "${findings.size} findings",
        )
        Text(
            text = "insight from the last $totalArrows arrows · ranked by actionability",
            style = interUI(10.5.sp).copy(color = AppInk3),
        )
        if (findings.isEmpty()) {
            Text(
                text = "not enough data yet",
                style = interUI(11.sp).copy(color = AppInk3),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            Column {
                findings.forEachIndexed { idx, finding ->
                    TrendLedgerRow(finding = finding)
                    if (idx < findings.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppLine2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendLedgerRow(finding: TrendFinding) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "${roman(finding.index)}.",
            style = frauncesDisplay(14.sp, italic = true).copy(color = AppPond),
            modifier = Modifier.width(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val metricColor = when (finding.metric.tone) {
                TrendTone.POSITIVE -> AppPine
                TrendTone.NEGATIVE -> AppMaple
                TrendTone.NEUTRAL -> AppPondDk
            }
            val title = buildAnnotatedString {
                withStyle(SpanStyle(color = AppInk)) {
                    append(finding.title)
                    append(" ")
                }
                withStyle(
                    SpanStyle(
                        color = metricColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                ) {
                    append(finding.metric.text)
                }
            }
            Text(
                text = title,
                style = frauncesDisplay(15.sp, italic = true),
            )
            Text(
                text = finding.body,
                style = interUI(11.5.sp).copy(color = AppInk2),
            )
            if (!finding.cues.isNullOrEmpty()) {
                // Minimal `**bold**` parser. We render with a single jetbrains
                // mono style and flip color for bold spans.
                val anno = parseCues(finding.cues!!)
                Text(
                    text = anno,
                    style = jetbrainsMono(9.5.sp).copy(
                        letterSpacing = 0.04.em,
                        color = AppInk3,
                    ),
                )
            }
        }
        BPStamp(
            text = finding.badge.stampLabel(),
            tone = finding.badge.stampTone(),
        )
    }
}

private fun TrendBadge.stampLabel(): String = when (this) {
    TrendBadge.GAIN -> "GAIN"
    TrendBadge.WATCH -> "WATCH"
    TrendBadge.HOLD -> "HOLD"
}

private fun TrendBadge.stampTone(): BPStampTone = when (this) {
    TrendBadge.GAIN -> BPStampTone.Pine
    TrendBadge.WATCH -> BPStampTone.Maple
    TrendBadge.HOLD -> BPStampTone.Stone
}

private fun parseCues(raw: String) = buildAnnotatedString {
    var remaining = raw
    while (true) {
        val open = remaining.indexOf("**")
        if (open < 0) {
            withStyle(SpanStyle(color = AppInk3)) { append(remaining) }
            break
        }
        if (open > 0) {
            withStyle(SpanStyle(color = AppInk3)) { append(remaining.substring(0, open)) }
        }
        val afterOpen = remaining.substring(open + 2)
        val close = afterOpen.indexOf("**")
        if (close < 0) {
            withStyle(SpanStyle(color = AppInk3)) { append(afterOpen) }
            break
        }
        withStyle(SpanStyle(color = AppInk2, fontWeight = FontWeight.Medium)) {
            append(afterOpen.substring(0, close))
        }
        remaining = afterOpen.substring(close + 2)
    }
}

private fun roman(n: Int): String {
    val table = listOf("", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii")
    return if (n in table.indices) table[n] else n.toString()
}

// ---------- Parameter drift ----------

@Composable
private fun ParameterDriftSection(drift: DriftResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BPSectionTitle(
            title = "Parameter drift",
            aside = if (drift.rows.isEmpty()) null else "${drift.rows.size} tracked",
        )
        Text(
            text = "setup values across the period · tap for history",
            style = interUI(10.5.sp).copy(color = AppInk3),
        )
        if (drift.rows.isEmpty()) {
            Text(
                text = "not enough data yet",
                style = interUI(11.sp).copy(color = AppInk3),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            DriftTable(rows = drift.rows)
        }
    }
}

@Composable
private fun DriftTable(rows: List<DriftRow>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            DriftHeaderCell(
                text = "Parameter",
                modifier = Modifier.weight(1f),
                alignEnd = false,
            )
            DriftHeaderCell(text = "Before", modifier = Modifier.width(64.dp), alignEnd = true)
            DriftHeaderCell(text = "Now", modifier = Modifier.width(64.dp), alignEnd = true)
            DriftHeaderCell(text = "Δ", modifier = Modifier.width(64.dp), alignEnd = true)
            DriftHeaderCell(text = "n", modifier = Modifier.width(28.dp), alignEnd = true)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))
        rows.forEachIndexed { idx, row ->
            DriftBodyRow(row = row)
            if (idx < rows.size - 1) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine2))
            }
        }
    }
}

@Composable
private fun DriftHeaderCell(text: String, modifier: Modifier = Modifier, alignEnd: Boolean) {
    Text(
        text = text.uppercase(),
        style = interUI(8.5.sp, weight = FontWeight.SemiBold).copy(
            letterSpacing = 0.20.em,
            color = AppInk3,
        ),
        modifier = modifier,
        textAlign = if (alignEnd) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start,
    )
}

@Composable
private fun DriftBodyRow(row: DriftRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.label,
            style = frauncesDisplay(13.5.sp, italic = true).copy(color = AppInk),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.before ?: "—",
            style = frauncesDisplay(12.5.sp, italic = true).copy(color = AppInk),
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Text(
            text = row.now ?: "—",
            style = frauncesDisplay(12.5.sp, italic = true).copy(color = AppInk),
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Box(
            modifier = Modifier.width(64.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            DriftDeltaCell(row = row)
        }
        Text(
            text = "${row.n}",
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun DriftDeltaCell(row: DriftRow) {
    val text = row.delta ?: "—"
    val fg = when (row.deltaTone) {
        DeltaTone.UP -> AppPine
        DeltaTone.DOWN -> AppMaple
        DeltaTone.FLAT -> AppInk3
    }
    val bg = when (row.deltaTone) {
        DeltaTone.UP -> AppPine.copy(alpha = 0.16f)
        DeltaTone.DOWN -> AppMaple.copy(alpha = 0.12f)
        DeltaTone.FLAT -> Color.Transparent
    }
    Text(
        text = text,
        style = jetbrainsMono(10.sp).copy(color = fg),
        modifier = Modifier
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

// ---------- Suggestions ledger ----------

@Composable
private fun SuggestionsLedgerSection(
    suggestions: List<AnalyticsSuggestion>,
    onOpen: (AnalyticsSuggestion) -> Unit,
    onDismiss: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BPSectionTitle(
            title = "Suggested adjustments",
            aside = if (suggestions.isEmpty()) null else "${suggestions.size} ranked",
        )
        Text(
            text = "by confidence · tap for detail",
            style = interUI(10.5.sp).copy(color = AppInk3),
        )
        if (suggestions.isEmpty()) {
            Text(
                text = "no suggestions yet",
                style = interUI(11.sp).copy(color = AppInk3),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            Column {
                suggestions.forEachIndexed { idx, suggestion ->
                    SuggestionLedgerRow(
                        suggestion = suggestion,
                        index = idx + 1,
                        onTap = { onOpen(suggestion) },
                    )
                    if (idx < suggestions.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppLine2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionLedgerRow(
    suggestion: AnalyticsSuggestion,
    index: Int,
    onTap: () -> Unit,
) {
    val confPct = (suggestion.confidence * 100).roundToInt()
    val relative = relativeTime(suggestion.createdAt)
    val detail = resolvedInlineSummary(suggestion)
    val stampText = resolvedStatusStamp(suggestion)
    val stampTone = stampTone(stampText)
    Box(modifier = Modifier.fillMaxWidth().clickable { onTap() }) {
        BPLedgerRow(
            index = index,
            title = bowParameterDisplayName(suggestion.parameter),
            detail = detail,
            monoLine = "$confPct% confidence · $relative",
            stamp = { BPStamp(text = stampText.uppercase(), tone = stampTone) },
            accessory = { ConfidenceBar(confidence = suggestion.confidence) },
        )
    }
}

@Composable
private fun ConfidenceBar(confidence: Double) {
    val pct = confidence.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(2.dp)
            .background(AppLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .height(2.dp)
                .background(AppPond),
        )
    }
}

private fun resolvedInlineSummary(suggestion: AnalyticsSuggestion): String {
    val override = suggestion.inlineSummary?.trim().orEmpty()
    if (override.isNotEmpty()) return override
    val cur = suggestion.currentValue.trim()
    val next = suggestion.suggestedValue.trim()
    return when {
        cur.isEmpty() -> next
        next.isEmpty() -> cur
        else -> "$cur → $next"
    }
}

private fun resolvedStatusStamp(suggestion: AnalyticsSuggestion): String {
    suggestion.statusStamp?.let {
        return when (it) {
            SuggestionStatusStamp.NEW -> "New"
            SuggestionStatusStamp.PROPOSED -> "Proposed"
            SuggestionStatusStamp.GOOD -> "Good"
            SuggestionStatusStamp.REVIEW -> "Review"
        }
    }
    return when {
        suggestion.wasApplied -> "Applied"
        suggestion.confidence >= 0.85 -> "Good"
        suggestion.confidence < 0.6 -> "Review"
        else -> "Proposed"
    }
}

private fun stampTone(stamp: String): BPStampTone = when (stamp.lowercase(Locale.US)) {
    "new", "proposed", "applied" -> BPStampTone.Pond
    "good" -> BPStampTone.Pine
    "review", "dismissed" -> BPStampTone.Maple
    else -> BPStampTone.Pond
}

private fun relativeTime(t: Instant): String {
    val now = Instant.now()
    val secs = (now.epochSecond - t.epochSecond).coerceAtLeast(0L)
    return when {
        secs < 60 -> "just now"
        secs < 3600 -> "${secs / 60} min ago"
        secs < 86_400 -> "${secs / 3600} hr ago"
        secs < 86_400 * 30 -> "${secs / 86_400} d ago"
        else -> "${secs / (86_400 * 30)} mo ago"
    }
}

private fun bowParameterDisplayName(raw: String): String {
    val map = mapOf(
        "drawLength" to "Draw Length",
        "letOffPct" to "Let-Off %",
        "peepHeight" to "Peep Height",
        "dLoopLength" to "D-Loop Length",
        "topCableTwists" to "Top Cable Twists",
        "bottomCableTwists" to "Bottom Cable Twists",
        "mainStringTopTwists" to "Main String Top Twists",
        "mainStringBottomTwists" to "Main String Bottom Twists",
        "topLimbTurns" to "Top Limb Turns",
        "bottomLimbTurns" to "Bottom Limb Turns",
        "restVertical" to "Rest Vertical",
        "restHorizontal" to "Rest Horizontal",
        "restDepth" to "Rest Depth",
        "sightPosition" to "Sight Position",
        "gripAngle" to "Grip Angle",
        "nockingHeight" to "Nocking Height",
    )
    map[raw]?.let { return it }
    val sb = StringBuilder()
    raw.forEachIndexed { idx, c ->
        if (idx > 0 && c.isUpperCase()) sb.append(' ')
        sb.append(c)
    }
    return sb.toString().replaceFirstChar { it.uppercase() }
}

// ---------- Footnotes grid ----------

@Composable
private fun FootnotesGrid(overview: AnalyticsOverview) {
    val ds = overview.datasetSummary
    val arrows = ds?.arrows ?: estimatedArrows(overview.sessionCount)
    val arrowLabel = ds?.arrowLabel ?: "—"
    val bowLabel = ds?.bowLabel ?: "—"
    val sinceLabel = ds?.sinceDate ?: formatYmd(LocalDate.now().minusDays(3))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppLine))
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FootnoteCell(key = "arrow set", value = arrowLabel, modifier = Modifier.weight(1f))
                FootnoteCell(key = "updated", value = "just now", modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FootnoteCell(key = "bow", value = bowLabel, modifier = Modifier.weight(1f))
                FootnoteCell(key = "sync", value = "✓ cloud", modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FootnoteCell(key = "dataset", value = "$arrows arrows", modifier = Modifier.weight(1f))
                FootnoteCell(key = "since", value = sinceLabel, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FootnoteCell(key: String, value: String, modifier: Modifier = Modifier) {
    val anno = buildAnnotatedString {
        withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) { append(key) }
        withStyle(SpanStyle(color = AppInk3)) { append(" · $value") }
    }
    Text(text = anno, style = jetbrainsMono(9.5.sp), modifier = modifier)
}

private fun formatYmd(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("yyyy · MM · dd", Locale.US))

// ---------- Colophon ----------

@Composable
private fun Colophon() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "tune smarter",
            style = frauncesDisplay(11.sp, italic = true).copy(color = AppInk3),
        )
        Spacer(Modifier.width(7.dp))
        Box(modifier = Modifier.size(5.dp).background(AppPond))
        Spacer(Modifier.width(7.dp))
        Text(
            text = "shoot better",
            style = frauncesDisplay(11.sp, italic = true).copy(color = AppInk3),
        )
    }
}

// ---------- Empty / loading / error ----------

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BPEyebrow(text = "Not enough data")
        Text(
            text = "Log at least one session to unlock analytics.",
            style = frauncesDisplay(18.sp, italic = true).copy(color = AppInk),
        )
        Text(
            text = "Arrows inform centroids, sigmas, and tuning suggestions — six or more is usually enough for the first picture to form.",
            style = interUI(11.5.sp).copy(color = AppInk2),
        )
    }
}

@Composable
private fun LoadingBlock() {
    Text(
        text = "Loading analytics…",
        style = interUI(11.sp).copy(color = AppInk3),
    )
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.size(width = 3.dp, height = 40.dp).background(AppMaple))
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BPEyebrow(text = "Analytics error", tone = AppMaple)
            Text(
                text = message,
                style = interUI(11.5.sp).copy(color = AppInk2),
                maxLines = 3,
            )
            Text(
                text = "RETRY",
                style = interUI(10.5.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.18.em,
                    color = AppPondDk,
                ),
                modifier = Modifier
                    .clickable { onRetry() }
                    .padding(top = 2.dp),
            )
        }
    }
}

// ---------- Filters sheet ----------

@Composable
private fun FiltersSheetBody(
    state: DashboardUiState,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    onBowTypeChange: (BowType?) -> Unit,
    onDistanceChange: (ShootingDistance?) -> Unit,
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
        if (state.availableBowTypes.size >= 2) {
            FilterSection(label = "Bow type") {
                PillRow {
                    FilterPill(label = "All", selected = state.selectedBowType == null) {
                        onBowTypeChange(null)
                    }
                    BowType.entries.forEach { type ->
                        if (type in state.availableBowTypes) {
                            FilterPill(label = type.label, selected = state.selectedBowType == type) {
                                onBowTypeChange(type)
                            }
                        }
                    }
                }
            }
        }
        if (state.availableDistances.size >= 2) {
            FilterSection(label = "Distance") {
                PillRow {
                    FilterPill(label = "All", selected = state.selectedDistance == null) {
                        onDistanceChange(null)
                    }
                    ShootingDistance.entries.forEach { distance ->
                        if (distance in state.availableDistances) {
                            FilterPill(label = distance.label, selected = state.selectedDistance == distance) {
                                onDistanceChange(distance)
                            }
                        }
                    }
                }
            }
        }
        FilterSection(label = "Time range") {
            PillRow {
                AnalyticsPeriod.entries.forEach { period ->
                    FilterPill(label = period.label, selected = state.period == period) {
                        onPeriodChange(period)
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
private fun FilterSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BPEyebrow(text = label)
        content()
    }
}

@Composable
private fun PillRow(content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
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

// ---------- Utils ----------

private fun Modifier.drawBottomHairline(color: Color): Modifier = this.drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1f,
    )
}

private fun formatAvg(v: Double): String =
    if (v == 0.0) "—" else "%.1f".format(v)

private fun estimatedArrows(sessionCount: Int): Int = (sessionCount * 18).coerceAtLeast(0)

// ---------- Previews ----------

@Preview(showBackground = true, name = "Dashboard — loaded")
@Composable
private fun DashboardPreview_Loaded() {
    BowPressTheme {
        AnalyticsDashboardContent(
            state = previewLoadedState,
            onPeriodChange = {},
            onBowTypeChange = {},
            onDistanceChange = {},
            onRetry = {},
            onOpenSuggestion = { _, _ -> },
            onOpenHistory = {},
            onOpenTimeline = {},
        )
    }
}

@Preview(showBackground = true, name = "Dashboard — empty")
@Composable
private fun DashboardPreview_Empty() {
    BowPressTheme {
        AnalyticsDashboardContent(
            state = DashboardUiState(isLoading = false),
            onPeriodChange = {},
            onBowTypeChange = {},
            onDistanceChange = {},
            onRetry = {},
            onOpenSuggestion = { _, _ -> },
            onOpenHistory = {},
            onOpenTimeline = {},
        )
    }
}

internal val previewOverview: AnalyticsOverview = AnalyticsOverview(
    period = AnalyticsPeriod.WEEK,
    sessionCount = 5,
    avgArrowScore = 9.6,
    xPercentage = 28.0,
    groupSigma = 1.4,
    sparkline = listOf(
        SparklinePoint(at = "2026-04-18T18:00:00Z", avg = 9.4),
        SparklinePoint(at = "2026-04-19T18:00:00Z", avg = 9.5),
        SparklinePoint(at = "2026-04-20T18:00:00Z", avg = 9.7),
        SparklinePoint(at = "2026-04-21T18:00:00Z", avg = 9.8),
        SparklinePoint(at = "2026-04-22T18:00:00Z", avg = 9.5),
        SparklinePoint(at = "2026-04-23T18:00:00Z", avg = 9.6),
        SparklinePoint(at = "2026-04-24T18:00:00Z", avg = 9.9),
    ),
)

internal val previewComparison: PeriodComparison = PeriodComparison(
    period = AnalyticsPeriod.WEEK,
    current = PeriodSlice(
        label = "Last 1 Week",
        avgArrowScore = 9.6,
        xPercentage = 28.0,
        sessionCount = 5,
        centroid = Centroid(0.05, 0.1),
        sigma = SigmaEllipse(0.25, 0.18, 30.0),
    ),
    previous = PeriodSlice(
        label = "Previous 1 Week",
        avgArrowScore = 9.2,
        xPercentage = 22.0,
        sessionCount = 4,
        centroid = Centroid(0.15, -0.05),
        sigma = SigmaEllipse(0.3, 0.22, 20.0),
    ),
    shift = ShiftVector(dxMm = -8.0, dyMm = 6.0, direction = "upper-left", description = "group tightened · shifted upper-left"),
)

internal val previewSuggestions: List<AnalyticsSuggestion> = listOf(
    AnalyticsSuggestion(
        id = "s1",
        bowId = "b1",
        createdAt = Instant.now(),
        parameter = "restVertical",
        suggestedValue = "+3/16\"",
        currentValue = "+2/16\"",
        reasoning = "Vertical impact bias detected across last 3 sessions.",
        confidence = 0.82,
        qualifier = null,
        wasRead = false,
        wasDismissed = false,
        deliveryType = DeliveryType.PUSH,
        evidence = null,
    ),
    AnalyticsSuggestion(
        id = "s2",
        bowId = "b1",
        createdAt = Instant.now(),
        parameter = "peepHeight",
        suggestedValue = "9.5\"",
        currentValue = "9.25\"",
        reasoning = "Anchor inconsistency mitigated at new height.",
        confidence = 0.71,
        qualifier = null,
        wasRead = false,
        wasDismissed = false,
        deliveryType = DeliveryType.IN_APP,
        evidence = null,
    ),
)

private val previewTimeline = TimelineResponse(
    period = AnalyticsPeriod.WEEK,
    range = TimelineRange(min = 9.2, max = 9.9, sigma = 0.25),
    points = listOf(
        TimelinePoint("s1", "2026-04-18T18:00:00Z", 9.4, false),
        TimelinePoint("s2", "2026-04-20T18:00:00Z", 9.5, false),
        TimelinePoint("s3", "2026-04-22T18:00:00Z", 9.7, false),
        TimelinePoint("s4", "2026-04-24T18:00:00Z", 9.9, true),
    ),
)

private val previewTrends = TrendsResponse(
    period = AnalyticsPeriod.WEEK,
    findings = listOf(
        TrendFinding(
            id = "t1",
            index = 1,
            title = "X rate trending up",
            metric = TrendMetric(text = "+6pp", tone = TrendTone.POSITIVE),
            body = "Your X rate climbed over the last three sessions — keep the anchor consistent.",
            cues = "**anchor** steady · follow through",
            badge = TrendBadge.GAIN,
        ),
        TrendFinding(
            id = "t2",
            index = 2,
            title = "Vertical bias",
            metric = TrendMetric(text = "−4mm", tone = TrendTone.NEGATIVE),
            body = "Centroid drifting low. Consider nocking point review.",
            cues = null,
            badge = TrendBadge.WATCH,
        ),
    ),
)

private val previewDrift = DriftResponse(
    period = AnalyticsPeriod.WEEK,
    rows = listOf(
        DriftRow(
            parameter = "restVertical",
            label = "Rest Vertical",
            unit = "\"",
            before = "+1/16\"",
            now = "+3/16\"",
            delta = "+2/16\"",
            deltaTone = DeltaTone.UP,
            n = 48,
        ),
    ),
)

private val previewLoadedState = DashboardUiState(
    period = AnalyticsPeriod.WEEK,
    isLoading = false,
    overview = previewOverview,
    comparison = previewComparison,
    topSuggestions = previewSuggestions,
    allSuggestions = previewSuggestions,
    timeline = previewTimeline,
    trends = previewTrends,
    drift = previewDrift,
)

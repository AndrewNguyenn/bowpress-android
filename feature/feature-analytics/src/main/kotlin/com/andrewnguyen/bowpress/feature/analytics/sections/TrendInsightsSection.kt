package com.andrewnguyen.bowpress.feature.analytics.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.TrendInsight
import com.andrewnguyen.bowpress.core.model.Zone
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Spec §Analysis Outputs #1 — Trend Analysis. Mirrors iOS
 * `AnalyticsTrendInsightsSection.swift` — builds a list of insights from the
 * current period's comparison + overview, appends any extra multi-session
 * insights supplied by [LocalAnalyticsEngine], and renders them as accented
 * cards. First 8 shown, rest behind a "Show more trends" toggle.
 */
@Composable
fun TrendInsightsSection(
    comparison: PeriodComparison,
    overview: AnalyticsOverview,
    extraInsights: List<TrendInsight>,
    modifier: Modifier = Modifier,
) {
    val insights = remember(comparison, overview, extraInsights) {
        buildInsights(comparison = comparison, overview = overview) + extraInsights
    }
    var showAll by remember { mutableStateOf(false) }
    val limit = TrendInsightsLimit
    val visibleInsights = if (showAll) insights else insights.take(limit)
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Trend Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (insights.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(accent, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = insights.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visibleInsights.forEach { insight ->
                TrendInsightRow(insight = insight)
            }
        }

        if (insights.size > limit) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .clickable { showAll = !showAll }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (showAll) "Show less" else "Show more trends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = if (showAll) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

    }
}

@Composable
private fun TrendInsightRow(insight: TrendInsight) {
    val accent = accentColor(kind = insight.kind)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(insight.icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = insight.headline,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = insight.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun accentColor(kind: TrendInsight.Kind): Color = when (kind) {
    TrendInsight.Kind.POSITIVE -> MaterialTheme.colorScheme.primary
    TrendInsight.Kind.NEGATIVE -> Color(0xFFEF6C00)
    TrendInsight.Kind.NEUTRAL -> Color(0xFF8C1AE6)
    TrendInsight.Kind.INFO -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
}

/** Maps iOS SF Symbol names to Material Compose icons. */
private fun iconFor(sfSymbol: String): ImageVector = when (sfSymbol) {
    "chart.line.uptrend.xyaxis" -> Icons.AutoMirrored.Filled.TrendingUp
    "chart.line.downtrend.xyaxis" -> Icons.AutoMirrored.Filled.TrendingDown
    "scope" -> Icons.Default.GpsFixed
    "location.circle" -> Icons.Default.LocationOn
    "star.fill" -> Icons.Default.Star
    "star" -> Icons.Default.StarBorder
    "calendar" -> Icons.Default.CalendarToday
    "wrench.and.screwdriver" -> Icons.Default.Build
    "tag" -> Icons.Default.Sell
    "chart.bar.fill" -> Icons.AutoMirrored.Filled.ShowChart
    else -> Icons.Default.Info
}

// ---------------------------------------------------------------------------
// Insight builder — direct port of iOS `buildInsights()`
// ---------------------------------------------------------------------------

internal const val TrendInsightsLimit: Int = 8

/** Mirrors iOS `mmPerNorm` — WA 40cm indoor target normalized-to-mm scale. */
private const val MM_PER_NORM: Double = 20.0 / (119.0 / 735.0)

internal fun buildInsights(
    comparison: PeriodComparison,
    overview: AnalyticsOverview,
): List<TrendInsight> {
    val result = mutableListOf<TrendInsight>()
    val cur = comparison.current
    val prev = comparison.previous
    if (prev.avgArrowScore <= 0) return result

    val scoreDelta = cur.avgArrowScore - prev.avgArrowScore
    val scorePct = (abs(scoreDelta) / maxOf(prev.avgArrowScore, 1.0) * 100).toInt()
    val cur10 = ringPlusRate(cur.plots, minRing = 10)
    val prev10 = ringPlusRate(prev.plots, minRing = 10)
    val curX = xRingRate(cur.plots)
    val prevX = xRingRate(prev.plots)
    val centroid = normalizedCentroid(cur.plots)
    val centroidDist = hypot(centroid.first, centroid.second)
    val sessionDelta = cur.sessionCount - prev.sessionCount

    // 1. Score momentum
    if (abs(scoreDelta) >= 0.3) {
        val up = scoreDelta > 0
        result += TrendInsight(
            id = "score_momentum",
            icon = if (up) "chart.line.uptrend.xyaxis" else "chart.line.downtrend.xyaxis",
            headline = if (up) {
                "Scores up ${fmt1(scoreDelta)} pts from ${prev.label.lowercase()}"
            } else {
                "Scores down ${fmt1(abs(scoreDelta))} pts from ${prev.label.lowercase()}"
            },
            detail = if (up) {
                "Average arrow score rose from ${fmt1(prev.avgArrowScore)} to ${fmt1(cur.avgArrowScore)} — a $scorePct% gain across ${cur.sessionCount} sessions. The upward trend suggests your form and tuning adjustments are taking hold."
            } else {
                "Average score dropped from ${fmt1(prev.avgArrowScore)} to ${fmt1(cur.avgArrowScore)}. Review your session feel-tags to identify whether fatigue, form drift, or conditions are the primary driver."
            },
            kind = if (up) TrendInsight.Kind.POSITIVE else TrendInsight.Kind.NEGATIVE,
        )
    }

    // 2. Grouping consistency — 10+ ring rate
    if (cur10 > 0 || prev10 > 0) {
        val up = cur10 >= prev10
        result += TrendInsight(
            id = "consistency",
            icon = "scope",
            headline = "10-ring+ rate: ${pct0(prev10 * 100)} → ${pct0(cur10 * 100)}",
            detail = if (up) {
                "${pct0(cur10 * 100)} of arrows landed in the 10-ring or better this period, up from ${pct0(prev10 * 100)}. Tighter groupings at the top of the scoring zone indicate improving shot consistency and cleaner execution."
            } else {
                "10-ring+ rate dropped from ${pct0(prev10 * 100)} to ${pct0(cur10 * 100)}. Target panic, fatigue, or form drift are the most common causes — track feel tags in your next session to narrow it down."
            },
            kind = if (up) TrendInsight.Kind.POSITIVE else TrendInsight.Kind.NEGATIVE,
        )
    }

    // 3. Directional bias in current period
    val isPrecision = overview.avgArrowScore >= 9.5
    val biasThreshold = if (isPrecision) 0.04 else 0.12
    if (centroidDist > biasThreshold) {
        val dir = verboseDirection(centroid.first, centroid.second)
        if (isPrecision) {
            val distMm = fmt1(centroidDist * MM_PER_NORM)
            result += TrendInsight(
                id = "directional_bias",
                icon = "location.circle",
                headline = "Group center ~${distMm}mm ${dir.short} of X",
                detail = "${dir.long} At this level a ${distMm}mm drift is meaningful — cross-reference with your feel tags. Common causes: peep height, nocking point position, or subtle anchor point drift.",
                kind = TrendInsight.Kind.NEUTRAL,
            )
        } else {
            result += TrendInsight(
                id = "directional_bias",
                icon = "location.circle",
                headline = "Groups trending ${dir.short} this period",
                detail = "${dir.long} Across ${cur.plots.size} shots this period, a consistent directional bias usually points to a repeatable form issue or sight misalignment — both are correctable once identified.",
                kind = TrendInsight.Kind.NEUTRAL,
            )
        }
    }

    // 4. X-ring rate
    if (curX >= 0.10 || prevX >= 0.10) {
        val up = curX >= prevX
        val commentary = when {
            curX >= 0.50 -> "At 50%+ X-ring rate, you're shooting at a competitive indoor standard."
            curX >= 0.25 -> "Keep focusing on back tension and a clean release — X-ring consistency follows."
            else -> "X-ring rate below 25% typically means groups are centered but not tight enough. Try aiming at a smaller reference point."
        }
        result += TrendInsight(
            id = "x_ring",
            icon = if (up) "star.fill" else "star",
            headline = "X-ring rate ${if (up) "up" else "down"} to ${pct0(curX * 100)}",
            detail = "X-ring (ring 11) hits: ${pct0(curX * 100)} this period vs ${pct0(prevX * 100)} last. $commentary",
            kind = if (up) TrendInsight.Kind.POSITIVE else TrendInsight.Kind.NEUTRAL,
        )
    }

    // 5. Session volume
    if (cur.sessionCount > 0) {
        val detail = when {
            sessionDelta > 0 ->
                "You put in more range time this period (${cur.sessionCount} vs ${prev.sessionCount} sessions). Consistent volume is one of the strongest predictors of score improvement — keep the frequency up."
            sessionDelta < 0 ->
                "Fewer sessions this period (${cur.sessionCount} vs ${prev.sessionCount}). If scores dipped alongside volume, that's a clear signal that range frequency matters for your consistency."
            else ->
                "Consistent session count (${cur.sessionCount}) period over period. Stable volume helps isolate whether score changes are form- or tuning-related."
        }
        val deltaSuffix = if (sessionDelta != 0) {
            " (${if (sessionDelta > 0) "+" else ""}$sessionDelta vs last)"
        } else {
            ""
        }
        result += TrendInsight(
            id = "volume",
            icon = "calendar",
            headline = "${cur.sessionCount} session${if (cur.sessionCount == 1) "" else "s"} this period$deltaSuffix",
            detail = detail,
            kind = when {
                sessionDelta > 0 -> TrendInsight.Kind.POSITIVE
                sessionDelta < 0 -> TrendInsight.Kind.NEUTRAL
                else -> TrendInsight.Kind.INFO
            },
        )
    }

    // 6. Tuning effect — config changed between periods
    val prevCfg = prev.config
    val curCfg = cur.config
    if (prevCfg != null && curCfg != null && prevCfg.id != curCfg.id) {
        val positive = scoreDelta > 0 && (cur10 - prev10) > 0
        result += TrendInsight(
            id = "tuning_effect",
            icon = "wrench.and.screwdriver",
            headline = if (positive) "Tuning change correlates with score gain" else "Tuning change — effect still developing",
            detail = if (positive) {
                "A bow configuration change occurred between periods. Scores improved ${fmt1(scoreDelta)} pts and 10-ring rate moved ${pct0(prev10 * 100)} → ${pct0(cur10 * 100)}. These gains align with the tuning adjustment."
            } else {
                "A bow configuration change was made between periods. Scores shifted ${fmt1(scoreDelta)} pts. Give it a few more sessions before drawing conclusions — new setups often need time to fully dial in."
            },
            kind = if (positive) TrendInsight.Kind.POSITIVE else TrendInsight.Kind.NEUTRAL,
        )
    }

    return result.take(6)
}

// ---------------------------------------------------------------------------
// Helpers — direct ports of iOS private helpers on the section view
// ---------------------------------------------------------------------------

private data class Dir(val short: String, val long: String)

private fun verboseDirection(x: Double, y: Double): Dir {
    val adx = abs(x); val ady = abs(y)
    return when {
        ady > adx * 1.7 -> if (y > 0) {
            Dir("north", "Shots are consistently landing high (north of center).")
        } else {
            Dir("south", "Shots are consistently landing low (south of center).")
        }
        adx > ady * 1.7 -> if (x > 0) {
            Dir("right", "Shots are consistently landing right of center.")
        } else {
            Dir("left", "Shots are consistently landing left of center.")
        }
        else -> {
            val v = if (y > 0) "high" else "low"
            val h = if (x > 0) "right" else "left"
            Dir("$v-$h", "Shots are consistently landing $v and $h of center.")
        }
    }
}

private fun normalizedCentroid(plots: List<ArrowPlot>): Pair<Double, Double> {
    if (plots.isEmpty()) return 0.0 to 0.0
    val realPts = plots.mapNotNull { p ->
        val x = p.plotX ?: return@mapNotNull null
        val y = p.plotY ?: return@mapNotNull null
        x to y
    }
    if (realPts.isNotEmpty()) {
        return realPts.map { it.first }.average() to realPts.map { it.second }.average()
    }
    var sumX = 0.0
    var sumY = 0.0
    for (plot in plots) {
        val r = when (plot.ring) {
            11 -> 0.0
            10 -> 0.245
            9 -> 0.494
            else -> 0.83
        }
        val angle = when (plot.zone) {
            Zone.CENTER -> 0.0
            Zone.N -> PI / 2
            Zone.NE -> PI / 4
            Zone.E -> 0.0
            Zone.SE -> -PI / 4
            Zone.S -> -PI / 2
            Zone.SW -> -PI * 3 / 4
            Zone.W -> PI
            Zone.NW -> PI * 3 / 4
        }
        sumX += r * cos(angle)
        sumY += r * sin(angle)
    }
    val n = plots.size.toDouble()
    return (sumX / n) to (sumY / n)
}

private fun xRingRate(plots: List<ArrowPlot>): Double {
    if (plots.isEmpty()) return 0.0
    return plots.count { it.ring == 11 }.toDouble() / plots.size
}

private fun ringPlusRate(plots: List<ArrowPlot>, minRing: Int): Double {
    if (plots.isEmpty()) return 0.0
    return plots.count { it.ring >= minRing }.toDouble() / plots.size
}

private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)
private fun pct0(v: Double): String = String.format(Locale.US, "%.0f%%", v)

package com.andrewnguyen.bowpress.feature.analytics.mock

import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.DatasetSummary
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import com.andrewnguyen.bowpress.core.model.SparklinePoint
import com.andrewnguyen.bowpress.core.model.TimelinePoint
import com.andrewnguyen.bowpress.core.model.TimelineRange
import com.andrewnguyen.bowpress.core.model.TimelineResponse
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt

/**
 * Direct port of iOS `MockAnalyticsWave2`. DEBUG-only fixtures for the Wave-2
 * analytics endpoints (timeline / drift / trends) + the extended overview /
 * comparison fields. Powers the design-review-quality Analytics screen so
 * previews + DEBUG builds render plausible values when offline.
 *
 * IMPORTANT: parity with iOS is the contract. The numerals here must match
 * `Sources/BowPress/State/MockAnalyticsWave2.swift` so screenshots line up
 * across platforms during cross-platform review.
 */
internal object MockAnalyticsWave2 {

    // -- Timeline ----------------------------------------------------------------

    fun timeline(period: AnalyticsPeriod): TimelineResponse {
        val points = sparklinePoints(period)
        val values = points.map { it.avg }
        val lo = values.minOrNull() ?: 9.0
        val hi = values.maxOrNull() ?: 11.0
        val mean = if (values.isNotEmpty()) values.average() else 0.0
        val variance = if (values.isNotEmpty()) {
            values.map { (it - mean) * (it - mean) }.average()
        } else 0.0
        val sigma = sqrt(variance)
        val mapped = points.mapIndexed { idx, p ->
            TimelinePoint(
                sessionId = "mock_s${idx + 1}",
                at = p.at,
                avg = p.avg,
                isLatest = idx == points.size - 1,
            )
        }
        return TimelineResponse(
            period = period,
            range = TimelineRange(min = lo, max = hi, sigma = sigma),
            points = mapped,
        )
    }

    /**
     * Reused by `overview.sparkline` so the bars and the timeline agree.
     *
     * 9 points climbing from 9.4 → 10.7 with a mild dip — matches the spec
     * figure (hero "10.4", +0.9 delta vs the prev slice). Verbatim from iOS.
     */
    fun sparklinePoints(period: AnalyticsPeriod): List<SparklinePoint> {
        val baseline = listOf(9.4, 9.8, 10.0, 10.2, 10.1, 10.3, 10.5, 10.4, 10.7)
        val now = Instant.now()
        val stepSeconds: Long = when (period) {
            AnalyticsPeriod.THREE_DAYS -> 86_400 / 3
            AnalyticsPeriod.WEEK -> 86_400
            AnalyticsPeriod.TWO_WEEKS -> (86_400 * 1.2).toLong()
            AnalyticsPeriod.MONTH -> (86_400 * 2.5).toLong()
            else -> 86_400 * 4
        }
        return baseline.mapIndexed { idx, avg ->
            val at = now.minusSeconds(stepSeconds * (baseline.size - idx - 1).toLong())
            SparklinePoint(at = DateTimeFormatter.ISO_INSTANT.format(at), avg = avg)
        }
    }

    // -- Overview / comparison overrides ----------------------------------------
    //
    // LocalAnalyticsEngine computes avgArrowScore/xPercentage/sessionCount from
    // the in-memory DEBUG seed, which drifts with DevMockData. For design review
    // we force the headline figure exactly — 10.4 avg, 72% X, 5 sessions, with
    // a positive +0.6 delta vs the previous slice (9.8 avg, 58% X).

    const val MOCK_CURRENT_AVG: Double = 10.4
    const val MOCK_CURRENT_X_PCT: Double = 72.0
    const val MOCK_CURRENT_SESSIONS: Int = 5
    // §B7 — realistic mm-scale group sigma. Pre-multi-spot the value was a
    // unitless 3.2 that rendered as `3.2″` next to a `at 50m` subtitle —
    // physically implausible (a 3.2-inch arrow group at 50m is sub-MOA).
    // 9.0mm matches the iOS fix (commit b342f6a) — a believable group for
    // a compound at 50m.
    const val MOCK_GROUP_SIGMA: Double = 3.2
    const val MOCK_GROUP_SIGMA_MM: Double = 9.0

    const val MOCK_PREVIOUS_AVG: Double = 9.8
    const val MOCK_PREVIOUS_X_PCT: Double = 58.0
    const val MOCK_PREVIOUS_SESSIONS: Int = 4

    // -- Dataset summary (footnote grid) ----------------------------------------

    fun datasetSummary(bow: Bow?, arrow: ArrowConfiguration?): DatasetSummary =
        DatasetSummary(
            arrows = 138,
            bowLabel = bow?.let { "${it.name} · ${it.bowType.label.lowercase(Locale.US)}" },
            arrowLabel = arrow?.let { "${it.label} ${"%.1f".format(Locale.US, it.length)}\"" },
            sinceDate = DateTimeFormatter.ISO_INSTANT.format(
                Instant.now().minusSeconds(86_400 * 3),
            ),
        )

    // -- Decorators -------------------------------------------------------------

    /**
     * Fill in Wave-2 fields the server hasn't sent + force the headline
     * numerals to the spec figure. Mirrors iOS `decorateOverviewWithMocks()`.
     */
    fun decorateOverview(
        overview: AnalyticsOverview,
        firstBow: Bow?,
        firstArrow: ArrowConfiguration?,
    ): AnalyticsOverview = overview.copy(
        sessionCount = MOCK_CURRENT_SESSIONS,
        avgArrowScore = MOCK_CURRENT_AVG,
        xPercentage = MOCK_CURRENT_X_PCT,
        groupSigma = overview.groupSigma ?: MOCK_GROUP_SIGMA,
        // §B7 — also force the mm-scale field so the DEBUG dashboard renders
        // the new `mm` suffix instead of the wrong-units `″`.
        groupSigmaMm = overview.groupSigmaMm ?: MOCK_GROUP_SIGMA_MM,
        sparkline = overview.sparkline ?: sparklinePoints(overview.period),
        datasetSummary = overview.datasetSummary ?: datasetSummary(firstBow, firstArrow),
    )

    /**
     * Force prev/current slices to the spec figure. Mirrors iOS
     * `decorateComparisonWithMocks()` — keeps the +0.6 delta strip rendering
     * even when the previous-window seed has 0 sessions.
     */
    fun decorateComparison(comparison: PeriodComparison): PeriodComparison = comparison.copy(
        current = comparison.current.copy(
            avgArrowScore = MOCK_CURRENT_AVG,
            xPercentage = MOCK_CURRENT_X_PCT,
            sessionCount = MOCK_CURRENT_SESSIONS,
        ),
        previous = comparison.previous.copy(
            avgArrowScore = MOCK_PREVIOUS_AVG,
            xPercentage = MOCK_PREVIOUS_X_PCT,
            sessionCount = MOCK_PREVIOUS_SESSIONS,
        ),
    )
}

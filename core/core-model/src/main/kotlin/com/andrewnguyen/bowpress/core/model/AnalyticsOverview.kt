package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `AnalyticsOverview`.
 *
 * Wave 2 extended the shape with [groupSigma], [sparkline] and [datasetSummary].
 * All three are nullable so pre-Wave-2 server responses still decode cleanly.
 *
 * Multi-spot wave (parity B7, iOS b342f6a) added [groupSigmaMm] — the real
 * physical millimetres at the dominant-bucket spot scale (90mm Vegas, 610mm
 * single-spot face) — and [DatasetSummary.droppedArrows] — arrows that
 * couldn't share a reference frame in a mixed-layout window.
 */
@Serializable
data class AnalyticsOverview(
    val period: AnalyticsPeriod,
    val sessionCount: Int,
    val avgArrowScore: Double,
    val xPercentage: Double,
    val suggestions: List<AnalyticsSuggestion> = emptyList(),
    // Wave 2 additions — all nullable for back-compat.
    val groupSigma: Double? = null,
    val sparkline: List<SparklinePoint>? = null,
    val datasetSummary: DatasetSummary? = null,
    /**
     * Real physical group sigma in millimetres at the dominant-bucket spot
     * scale. Preferred over the legacy unitless [groupSigma] when present
     * (the UI flips the suffix from the wrong-units `″` to `mm`). Null on a
     * pre-multi-spot API response.
     */
    val groupSigmaMm: Double? = null,
)

/**
 * One point in the overview sparkline. `at` is ISO-8601 on the wire — parse to
 * [java.time.Instant] at the use-site if a typed timestamp is required.
 */
@Serializable
data class SparklinePoint(
    val at: String,
    val avg: Double,
)

/** Meta describing the rows analytics worked from. Used by the footnote grid. */
@Serializable
data class DatasetSummary(
    val arrows: Int,
    val bowLabel: String? = null,
    val arrowLabel: String? = null,
    val sinceDate: String? = null,
    /**
     * Arrows dropped from the grouping computation in a mixed single-spot +
     * multi-spot window (the two layouts can't share a reference frame).
     * When > 0 the UI renders `<used> / of <total> logged` on the Group ∅
     * cell so the discrepancy with the headline tally is explained. Null on
     * a pre-multi-spot API response. (parity B7, iOS b342f6a)
     */
    val droppedArrows: Int? = null,
)

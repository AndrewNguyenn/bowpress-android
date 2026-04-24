package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `AnalyticsOverview`.
 *
 * Wave 2 extended the shape with [groupSigma], [sparkline] and [datasetSummary].
 * All three are nullable so pre-Wave-2 server responses still decode cleanly.
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
)

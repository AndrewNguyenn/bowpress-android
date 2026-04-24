package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Trend-analysis payload returned by `GET /analytics/trends`. Mirrors iOS
 * `TrendsResponse`. Drives the "Trend analysis" ledger rendered above the
 * suggestions section on the Analytics screen.
 */
@Serializable
data class TrendsResponse(
    val period: AnalyticsPeriod,
    val findings: List<TrendFinding> = emptyList(),
)

/** Mirrors iOS `TrendFinding`. */
@Serializable
data class TrendFinding(
    val id: String,
    val index: Int,
    val title: String,
    val metric: TrendMetric,
    val body: String,
    val cues: String? = null,
    val badge: TrendBadge,
)

/**
 * Colored inline metric tag embedded in the title. Rendered in monospace and
 * colored per [tone].
 */
@Serializable
data class TrendMetric(
    val text: String,
    val tone: TrendTone,
)

/** Wire: lowercase — `positive` / `negative` / `neutral`. */
@Serializable
enum class TrendTone {
    @SerialName("positive") POSITIVE,
    @SerialName("negative") NEGATIVE,
    @SerialName("neutral") NEUTRAL,
}

/**
 * Right-aligned stamp on each trend row. Wire: lowercase — maps to the pine /
 * maple / stone stamp tones in the design system.
 */
@Serializable
enum class TrendBadge {
    @SerialName("gain") GAIN,
    @SerialName("watch") WATCH,
    @SerialName("hold") HOLD,
}

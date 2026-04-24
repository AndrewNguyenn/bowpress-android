package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/**
 * Score-timeline payload returned by `GET /analytics/timeline`. Mirrors iOS
 * `TimelineResponse`. Powers the "Score timeline" sparkline + the range/σ
 * aside rendered above it on the Analytics screen.
 */
@Serializable
data class TimelineResponse(
    val period: AnalyticsPeriod,
    val range: TimelineRange,
    val points: List<TimelinePoint> = emptyList(),
)

/**
 * Axis metadata. The sparkline uses `max`, the midpoint between min and max,
 * and `min` for the top / middle / bottom axis labels. `sigma` is the
 * session-to-session score standard deviation shown in the aside.
 */
@Serializable
data class TimelineRange(
    val min: Double,
    val max: Double,
    val sigma: Double,
)

/**
 * One session's rolled-up avg arrow score. `at` is an ISO-8601 string on the
 * wire — parse to [java.time.Instant] in the repository / view-model when a
 * typed timestamp is needed. Mirrors iOS `TimelinePoint`.
 */
@Serializable
data class TimelinePoint(
    val sessionId: String,
    val at: String,
    val avg: Double,
    val isLatest: Boolean,
)

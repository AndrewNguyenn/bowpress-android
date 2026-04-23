package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/** Mirrors iOS `AnalyticsOverview`. */
@Serializable
data class AnalyticsOverview(
    val period: AnalyticsPeriod,
    val sessionCount: Int,
    val avgArrowScore: Double,
    val xPercentage: Double,
    val suggestions: List<AnalyticsSuggestion> = emptyList(),
)

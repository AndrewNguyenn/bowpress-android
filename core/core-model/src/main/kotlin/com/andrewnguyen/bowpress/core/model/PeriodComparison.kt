package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/** Mirrors iOS `PeriodSlice`. */
@Serializable
data class PeriodSlice(
    val label: String,
    val plots: List<ArrowPlot> = emptyList(),
    val avgArrowScore: Double,
    val xPercentage: Double,
    val sessionCount: Int,
    val config: BowConfiguration? = null,
)

/** Mirrors iOS `PeriodComparison`. */
@Serializable
data class PeriodComparison(
    val period: AnalyticsPeriod,
    val current: PeriodSlice,
    val previous: PeriodSlice,
)

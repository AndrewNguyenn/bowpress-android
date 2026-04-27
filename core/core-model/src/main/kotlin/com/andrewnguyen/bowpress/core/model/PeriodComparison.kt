package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `PeriodSlice`.
 *
 * Wave 2 added the optional [centroid] + [sigma] ellipse so the Analytics
 * impact-map overlay can plot "previous vs now" without a separate endpoint.
 * Both are nullable so pre-Wave-2 servers decode cleanly.
 */
@Serializable
data class PeriodSlice(
    val label: String,
    val plots: List<ArrowPlot> = emptyList(),
    val avgArrowScore: Double,
    val xPercentage: Double,
    val sessionCount: Int,
    val config: BowConfiguration? = null,
    // Wave 2 — optional centroid / sigma ellipse.
    val centroid: Centroid? = null,
    val sigma: SigmaEllipse? = null,
)

/**
 * Mirrors iOS `PeriodComparison`.
 *
 * Wave 2 added the optional [shift] vector describing how the group centroid
 * moved from the previous to the current period.
 */
@Serializable
data class PeriodComparison(
    val period: AnalyticsPeriod,
    val current: PeriodSlice,
    val previous: PeriodSlice,
    // Wave 2 — nullable when either centroid is missing.
    val shift: ShiftVector? = null,
)

/**
 * Normalized centroid on the target face. Values share the same coordinate
 * space as `ArrowPlot.plotX` / `plotY` (roughly -1…1 across the face).
 */
@Serializable
data class Centroid(
    val x: Double,
    val y: Double,
)

/**
 * 1σ dispersion ellipse. [major] / [minor] are in the same normalized units as
 * [Centroid]; [rotationDeg] is the ellipse angle in degrees.
 */
@Serializable
data class SigmaEllipse(
    val major: Double,
    val minor: Double,
    val rotationDeg: Double,
)

/**
 * Shift vector summarising the move from prev → current centroid, in mm on
 * the physical target face, plus a pre-rendered human-readable description.
 */
@Serializable
data class ShiftVector(
    val dxMm: Double,
    val dyMm: Double,
    val direction: String,
    val description: String,
)

package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.Zone
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pure-logic ring/zone computation. Mirrors iOS `TargetGeometry` verbatim so cross-platform
 * scoring stays in lockstep.
 *
 * iOS source of truth — bowpress-ios/Sources/BowPress/Session/TargetPlotView.swift:
 *   - Ring thresholds: lines 10–15 (`xRadius` ... `r6Radius`)
 *   - `centerZoneRadius`: line 21
 *   - `ring(for:)`: lines 23–33
 *   - `zone(for:angle:)`: lines 35–55 (math-angle → compass bearing conversion)
 *   - Hit handling / atan2 call: lines 148–174 (specifically line 160)
 *   - Normalized (plotX, plotY) storage: lines 163–165
 *
 * Coordinate convention (identical to iOS storage on lines 164–165):
 *   - `plotX` ∈ [-1, 1]: positive = east (right on the screen / target face).
 *   - `plotY` ∈ [-1, 1]: positive = south (down on the screen). iOS flips it back when
 *     storing: `normY = -dy / radius`, where `dy = center.y - point.y` (math-y), so the
 *     persisted value is screen-y-positive-down.
 *   - Normalized radius `r = sqrt(plotX² + plotY²)`, where 1.0 is the outer edge of the
 *     visible target face (blue/white boundary at ~594px in the 1470² art).
 *   - To recover the iOS math angle for the zone computation we flip plotY once more:
 *     `atan2(-plotY, plotX)` gives degrees CCW from east, matching line 160.
 */
object TargetGeometry {
    // Pixel-measured boundaries on the 1470×1470 `target_face.png` (centre 735, 735).
    // Must match bowpress-ios TargetPlotView.swift lines 10–15 exactly.
    const val X_RADIUS: Double = 60.0 / 735.0        // 0.0816… — X / 10 divider
    const val R10_RADIUS: Double = 119.0 / 735.0     // 0.1619… — 10 / 9 divider
    const val R9_RADIUS: Double = 238.0 / 735.0      // 0.3238… — 9 / 8 divider (yellow→red)
    const val R8_RADIUS: Double = 357.0 / 735.0      // 0.4857… — 8 / 7 divider (mid red)
    const val R7_RADIUS: Double = 475.0 / 735.0      // 0.6462… — 7 / 6 divider (red→blue)
    const val R6_RADIUS: Double = 594.0 / 735.0      // 0.8081… — outer edge of ring 6

    /** Real millimetres per 1.0 normalised unit — matches iOS line 18. */
    const val MM_PER_NORM_UNIT: Double = 20.0 / R10_RADIUS  // ≈ 123.5 mm

    /** Within the X ring, this distance from absolute centre is the CENTER zone. (iOS line 21) */
    const val CENTER_ZONE_RADIUS: Double = 0.04

    /** Zone boundaries (compass bearings, degrees) — iOS lines 45–52. */
    private const val OCTANT_HALF = 22.5

    /**
     * Map a normalized distance from the centre to a ring (11=X, 10..6). Returns
     * `null` when the plot is outside ring 6 (a miss). Mirrors iOS `ring(for:)`.
     */
    fun ring(normalizedDist: Double): Int? = when {
        normalizedDist < X_RADIUS -> 11   // X (displayed as "X", scores 10)
        normalizedDist < R10_RADIUS -> 10
        normalizedDist < R9_RADIUS -> 9
        normalizedDist < R8_RADIUS -> 8
        normalizedDist < R7_RADIUS -> 7
        normalizedDist < R6_RADIUS -> 6
        else -> null
    }

    /**
     * Compute the [Zone] for a plot. `angleDegrees` is the math angle (CCW from east),
     * exactly what iOS passes on line 161. Mirrors iOS `zone(for:angle:)`.
     */
    fun zone(normalizedDist: Double, angleDegrees: Double): Zone {
        if (normalizedDist < CENTER_ZONE_RADIUS) return Zone.CENTER

        // Convert math angle (CCW from east) to compass bearing (CW from north). iOS lines 41–42.
        val raw = (90.0 - angleDegrees) % 360.0
        val compass = if (raw < 0) raw + 360.0 else raw

        return when {
            compass >= 360.0 - OCTANT_HALF || compass < OCTANT_HALF -> Zone.N
            compass < OCTANT_HALF + 45.0 -> Zone.NE       //  22.5 ..  67.5
            compass < OCTANT_HALF + 90.0 -> Zone.E        //  67.5 .. 112.5
            compass < OCTANT_HALF + 135.0 -> Zone.SE      // 112.5 .. 157.5
            compass < OCTANT_HALF + 180.0 -> Zone.S       // 157.5 .. 202.5
            compass < OCTANT_HALF + 225.0 -> Zone.SW      // 202.5 .. 247.5
            compass < OCTANT_HALF + 270.0 -> Zone.W       // 247.5 .. 292.5
            else -> Zone.NW                               // 292.5 .. 337.5
        }
    }

    /** Result of classifying a plotted point. */
    data class Classification(
        val ring: Int?,
        val zone: Zone,
        val normalizedDist: Double,
    )

    /**
     * Classify a point stored in iOS (plotX, plotY) convention (plotY positive = south).
     * The radius is rotation-invariant, and the angle is recovered by flipping plotY
     * back to math-y so `atan2(math-y, math-x)` matches iOS line 160.
     */
    fun classify(plotX: Double, plotY: Double): Classification {
        val radius = sqrt(plotX * plotX + plotY * plotY)
        val angleRad = atan2(-plotY, plotX)
        val angleDeg = Math.toDegrees(angleRad)
        return Classification(ring(radius), zone(radius, angleDeg), radius)
    }

    /**
     * Scoring variant — shrinks the plot radius by the arrow-dot radius so a dot that
     * visibly touches a higher ring scores it. Mirrors iOS lines 155–158. The zone is
     * always computed from the full radius (iOS line 161).
     */
    fun classifyWithDotRadius(
        plotX: Double,
        plotY: Double,
        dotNormRadius: Double,
    ): Classification {
        val radius = sqrt(plotX * plotX + plotY * plotY)
        val angleRad = atan2(-plotY, plotX)
        val angleDeg = Math.toDegrees(angleRad)
        val scoringDist = (radius - dotNormRadius).coerceAtLeast(0.0)
        return Classification(ring(scoringDist), zone(radius, angleDeg), radius)
    }
}

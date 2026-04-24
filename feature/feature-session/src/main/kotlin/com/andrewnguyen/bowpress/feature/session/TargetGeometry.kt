package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.Zone
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pure-logic ring/zone computation. Mirrors iOS `TargetGeometry` verbatim so cross-platform
 * scoring stays in lockstep.
 *
 * iOS source of truth — bowpress-ios/Sources/BowPress/Session/TargetPlotView.swift:
 *   - SixRing ring thresholds: lines 10–15 (`xRadius` ... `r6Radius`)
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
 *     visible target face.
 *   - To recover the iOS math angle for the zone computation we flip plotY once more:
 *     `atan2(-plotY, plotX)` gives degrees CCW from east, matching line 160.
 *
 * Two face variants are supported:
 *   - [SixRing]: inner-6 indoor face (compound default). Scores 6..11 (11=X), null outside.
 *   - [TenRing]: WA full 10-ring face (recurve / barebow default). Scores 1..11, null outside.
 */
sealed class TargetGeometry {

    /** Normalized threshold radii, outermost (largest) first, innermost (smallest X) last. */
    abstract val thresholds: DoubleArray

    /** Ring value associated with the innermost (X-minus-one) threshold — 10 on tenRing, 10 on sixRing. */
    abstract val innermostNumericRing: Int

    /** Real millimetres per 1.0 normalised unit — used to size the arrow dot. */
    abstract val mmPerNormUnit: Double

    /**
     * Map a normalized distance from centre to a ring. Returns 11 (X) when inside the
     * innermost threshold, the corresponding ring when inside one of the subsequent
     * thresholds, and null when outside the outermost threshold (a miss). Mirrors iOS
     * `ring(for:)`.
     */
    fun ring(normalizedDist: Double): Int? {
        // thresholds is ordered innermost (smallest) first — X, then the innermost numeric ring's outer edge, etc.
        // The innermost numeric ring equals thresholds.size - 1 "steps" below innermostNumericRing when going outward.
        if (normalizedDist < thresholds[0]) return 11 // X
        // Walk outward. Ring numbers decrement as we move outward.
        var ring = innermostNumericRing
        for (i in 1 until thresholds.size) {
            if (normalizedDist < thresholds[i]) return ring
            ring -= 1
        }
        return null
    }

    /**
     * Compute the [Zone] for a plot. `angleDegrees` is the math angle (CCW from east),
     * exactly what iOS passes on line 161. Mirrors iOS `zone(for:angle:)`.
     */
    fun zone(normalizedDist: Double, angleDegrees: Double): Zone {
        if (normalizedDist < CENTER_ZONE_RADIUS) return Zone.CENTER
        return zoneFromAngle(angleDegrees)
    }

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

    /** Result of classifying a plotted point. */
    data class Classification(
        val ring: Int?,
        val zone: Zone,
        val normalizedDist: Double,
    )

    // ------------------------------------------------------------------------
    // Face presets
    // ------------------------------------------------------------------------

    /**
     * Inner-6 indoor face (historical default). Ring numbers 6..10 plus X. Pixel-measured
     * boundaries on the 1470×1470 target_face.png (centre 735, 735). Must match
     * bowpress-ios TargetPlotView.swift lines 10–15 exactly.
     */
    object SixRing : TargetGeometry() {
        const val X_RADIUS: Double = 60.0 / 735.0        // 0.0816… — X / 10 divider
        const val R10_RADIUS: Double = 119.0 / 735.0     // 0.1619… — 10 / 9 divider
        const val R9_RADIUS: Double = 238.0 / 735.0      // 0.3238… — 9 / 8 divider (yellow→red)
        const val R8_RADIUS: Double = 357.0 / 735.0      // 0.4857… — 8 / 7 divider (mid red)
        const val R7_RADIUS: Double = 475.0 / 735.0      // 0.6462… — 7 / 6 divider (red→blue)
        const val R6_RADIUS: Double = 594.0 / 735.0      // 0.8081… — outer edge of ring 6

        // Ordered innermost-first. ring() walks outward: X, 10, 9, 8, 7, 6, miss.
        override val thresholds: DoubleArray = doubleArrayOf(
            X_RADIUS,
            R10_RADIUS,
            R9_RADIUS,
            R8_RADIUS,
            R7_RADIUS,
            R6_RADIUS,
        )
        override val innermostNumericRing: Int = 10
        override val mmPerNormUnit: Double = 20.0 / R10_RADIUS  // ≈ 123.5 mm
    }

    /**
     * WA full 10-ring face. X at 0.05, then 10 equal-width rings out to 1.0.
     * Thresholds: X=0.05, r10=0.10, r9=0.20, r8=0.30, r7=0.40, r6=0.50, r5=0.60,
     * r4=0.70, r3=0.80, r2=0.90, r1=1.00.
     */
    object TenRing : TargetGeometry() {
        const val X_RADIUS: Double = 0.05
        const val R10_RADIUS: Double = 0.10
        const val R9_RADIUS: Double = 0.20
        const val R8_RADIUS: Double = 0.30
        const val R7_RADIUS: Double = 0.40
        const val R6_RADIUS: Double = 0.50
        const val R5_RADIUS: Double = 0.60
        const val R4_RADIUS: Double = 0.70
        const val R3_RADIUS: Double = 0.80
        const val R2_RADIUS: Double = 0.90
        const val R1_RADIUS: Double = 1.00

        override val thresholds: DoubleArray = doubleArrayOf(
            X_RADIUS,
            R10_RADIUS,
            R9_RADIUS,
            R8_RADIUS,
            R7_RADIUS,
            R6_RADIUS,
            R5_RADIUS,
            R4_RADIUS,
            R3_RADIUS,
            R2_RADIUS,
            R1_RADIUS,
        )
        override val innermostNumericRing: Int = 10
        // Keep the arrow-dot sizing consistent with the 6-ring face so the dot radius
        // in normalised units is independent of which face is rendered. The 10-ring
        // face has a smaller 10-ring (0.10 vs 0.162) so a 5 mm arrow dot looks
        // relatively larger on the 10-ring face — this mirrors reality.
        override val mmPerNormUnit: Double = SixRing.mmPerNormUnit
    }

    companion object {
        /** Within the X ring, this distance from absolute centre is the CENTER zone. (iOS line 21) */
        const val CENTER_ZONE_RADIUS: Double = 0.04

        /** Zone boundaries (compass bearings, degrees) — iOS lines 45–52. */
        private const val OCTANT_HALF = 22.5

        /** Return the geometry preset that matches [TargetFaceType]. */
        fun forFace(faceType: TargetFaceType): TargetGeometry = when (faceType) {
            TargetFaceType.SIX_RING -> SixRing
            TargetFaceType.TEN_RING -> TenRing
        }

        private fun zoneFromAngle(angleDegrees: Double): Zone {
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

        // -------- Back-compat shims (pre face-type refactor) ---------------
        //
        // The earlier API exposed SixRing constants and static `classify*` helpers via a
        // top-level `object TargetGeometry`. These shims keep existing callers and tests
        // compiling by delegating to [SixRing]. Prefer calling into the face-specific
        // instance (or [forFace]) in new code.

        @Deprecated("Use TargetGeometry.SixRing.X_RADIUS.", ReplaceWith("TargetGeometry.SixRing.X_RADIUS"))
        const val X_RADIUS: Double = 60.0 / 735.0

        @Deprecated("Use TargetGeometry.SixRing.R10_RADIUS.", ReplaceWith("TargetGeometry.SixRing.R10_RADIUS"))
        const val R10_RADIUS: Double = 119.0 / 735.0

        @Deprecated("Use TargetGeometry.SixRing.R9_RADIUS.", ReplaceWith("TargetGeometry.SixRing.R9_RADIUS"))
        const val R9_RADIUS: Double = 238.0 / 735.0

        @Deprecated("Use TargetGeometry.SixRing.R8_RADIUS.", ReplaceWith("TargetGeometry.SixRing.R8_RADIUS"))
        const val R8_RADIUS: Double = 357.0 / 735.0

        @Deprecated("Use TargetGeometry.SixRing.R7_RADIUS.", ReplaceWith("TargetGeometry.SixRing.R7_RADIUS"))
        const val R7_RADIUS: Double = 475.0 / 735.0

        @Deprecated("Use TargetGeometry.SixRing.R6_RADIUS.", ReplaceWith("TargetGeometry.SixRing.R6_RADIUS"))
        const val R6_RADIUS: Double = 594.0 / 735.0

        @Deprecated("Use TargetGeometry.SixRing.mmPerNormUnit.", ReplaceWith("TargetGeometry.SixRing.mmPerNormUnit"))
        const val MM_PER_NORM_UNIT: Double = 20.0 / (119.0 / 735.0)

        @Deprecated(
            "Use TargetGeometry.SixRing.classify(plotX, plotY).",
            ReplaceWith("TargetGeometry.SixRing.classify(plotX, plotY)"),
        )
        fun classify(plotX: Double, plotY: Double): Classification =
            SixRing.classify(plotX, plotY)

        @Deprecated(
            "Use TargetGeometry.SixRing.classifyWithDotRadius(plotX, plotY, dotNormRadius).",
            ReplaceWith("TargetGeometry.SixRing.classifyWithDotRadius(plotX, plotY, dotNormRadius)"),
        )
        fun classifyWithDotRadius(
            plotX: Double,
            plotY: Double,
            dotNormRadius: Double,
        ): Classification = SixRing.classifyWithDotRadius(plotX, plotY, dotNormRadius)

        @Deprecated("Use TargetGeometry.SixRing.ring(normalizedDist).", ReplaceWith("TargetGeometry.SixRing.ring(normalizedDist)"))
        fun ring(normalizedDist: Double): Int? = SixRing.ring(normalizedDist)
    }
}

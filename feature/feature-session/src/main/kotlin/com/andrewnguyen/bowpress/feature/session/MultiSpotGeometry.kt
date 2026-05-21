package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.TargetLayout
import kotlin.math.sqrt

/**
 * Layout description of a multi-spot Vegas card: three 6-ring spots on a
 * single 40cm paper, plus the helpers needed to render and score it. Mirrors
 * iOS `MultiSpotGeometry` verbatim so cross-platform scoring stays in lockstep.
 *
 * iOS source of truth — bowpress-ios/Sources/BowPress/Session/MultiSpotGeometry.swift.
 *
 * [centers] are normalised to the *face square* (0…1 in both axes); the face
 * square is the bounded area the target view paints into. [radiusNorm] is the
 * spot's outer radius in the same normalised space — outer = ring 6 outer =
 * "1.0" of the spot. The design ships triangle and vertical layouts.
 */
data class MultiSpotGeometry(
    /**
     * (centerX, centerY) per spot, normalised 0..1 of the face square. Order
     * matches the Vegas labelling: 1 = bottom-left, 2 = top, 3 = bottom-right
     * (for triangle). Vertical is top → middle → bottom.
     */
    val centers: List<NormPoint>,
    /** Spot outer radius in normalised face units. */
    val radiusNorm: Double,
    /** Real-world spot diameter in mm (180mm Vegas). */
    val spotDiameterMm: Double,
) {
    /** A point normalised to the face square (0..1 in both axes). */
    data class NormPoint(val x: Double, val y: Double)

    /**
     * Returns the index of the spot whose centre is closest to [pointNorm]
     * (normalised 0..1 face coords). Always returns a valid index — the
     * nearest-wins rule covers a touch between spots or off the paper.
     */
    fun nearestSpotIndex(pointNorm: NormPoint): Int {
        var bestIdx = 0
        var bestDist = Double.POSITIVE_INFINITY
        centers.forEachIndexed { idx, c ->
            val dx = pointNorm.x - c.x
            val dy = pointNorm.y - c.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < bestDist) {
                bestDist = d
                bestIdx = idx
            }
        }
        return bestIdx
    }

    /** Result of [nearestSpotLocalRadius] — the chosen spot + the touch's
     *  distance from its centre in units of that spot's radius. */
    data class SpotLocal(val spot: Int, val local: Double)

    /**
     * Returns the touch's distance from the nearest spot's centre, in units of
     * *that spot's radius* (so `0` = bullseye, `1` = on the ring-6 line, `> 1`
     * = outside the paper for that spot).
     */
    fun nearestSpotLocalRadius(pointNorm: NormPoint): SpotLocal {
        val idx = nearestSpotIndex(pointNorm)
        val c = centers[idx]
        val dx = pointNorm.x - c.x
        val dy = pointNorm.y - c.y
        val d = sqrt(dx * dx + dy * dy)
        return SpotLocal(spot = idx, local = d / radiusNorm)
    }

    /**
     * Maps a spot-local distance (in units of spot radius) plus the arrow's
     * radius fraction ([arrowRadiusFrac] = shaftMm / spotDiameterMm) to a ring
     * number. Bands per the `3-Spot Triangle` design. Returns null for a miss.
     * Mirrors iOS `ring(forLocal:arrowRadiusFrac:)`.
     */
    fun ring(local: Double, arrowRadiusFrac: Double): Int? {
        val e = local - arrowRadiusFrac
        return when {
            e < 0.075 -> 11   // X
            e < 0.20 -> 10
            e < 0.40 -> 9
            e < 0.60 -> 8
            e < 0.80 -> 7
            e < 1.00 -> 6
            else -> null
        }
    }

    /**
     * Buckets every arrow into its nearest spot and computes the arrow's
     * position relative to that spot. [PerSpotArrow.localX] / `localY` are in
     * −1..1 of the spot's outer radius. Mirrors iOS `assignArrows`.
     */
    fun assignArrows(arrows: List<ArrowPlot>): List<PerSpotArrow> = arrows.mapNotNull { arrow ->
        val px = arrow.plotX ?: return@mapNotNull null
        val py = arrow.plotY ?: return@mapNotNull null
        // ArrowPlot stores plotX/plotY in -1..1 of the face square. Convert
        // to 0..1 face coords to match spot centres.
        val faceX = 0.5 + px / 2.0
        val faceY = 0.5 + py / 2.0
        val pointNorm = NormPoint(faceX, faceY)
        val near = nearestSpotLocalRadius(pointNorm)
        val center = centers[near.spot]
        PerSpotArrow(
            arrow = arrow,
            spotIndex = near.spot,
            localX = (faceX - center.x) / radiusNorm,
            localY = (faceY - center.y) / radiusNorm,
        )
    }

    /** Vegas labelling — "Spot 1/2/3". Matches the design's per-spot card labels. */
    fun label(spotIndex: Int): String = "Spot ${spotIndex + 1}"

    companion object {
        /**
         * The geometry preset for a layout, or null for [TargetLayout.SINGLE].
         * Mirrors iOS `MultiSpotGeometry.preset(for:)`.
         */
        fun preset(layout: TargetLayout): MultiSpotGeometry? = when (layout) {
            TargetLayout.SINGLE -> null
            // Design viewBox (200×200): BL=(55,145), TOP=(100,58), BR=(145,145).
            TargetLayout.TRIANGLE -> MultiSpotGeometry(
                centers = listOf(
                    NormPoint(55.0 / 200.0, 145.0 / 200.0),    // 1 · BL
                    NormPoint(100.0 / 200.0, 58.0 / 200.0),    // 2 · TOP
                    NormPoint(145.0 / 200.0, 145.0 / 200.0),   // 3 · BR
                ),
                radiusNorm = 40.0 / 200.0,                     // 0.20
                spotDiameterMm = TargetLayout.SPOT_DIAMETER_MM,
            )
            TargetLayout.VERTICAL -> MultiSpotGeometry(
                centers = listOf(
                    NormPoint(0.5, 0.183),    // top
                    NormPoint(0.5, 0.500),    // middle
                    NormPoint(0.5, 0.817),    // bottom
                ),
                radiusNorm = 0.135,
                spotDiameterMm = TargetLayout.SPOT_DIAMETER_MM,
            )
        }
    }
}

/**
 * Per-arrow assignment to a spot, with the arrow's position recentered onto
 * that spot's local coord system. [localX] / [localY] are in −1..1 of the
 * spot's outer radius. Mirrors iOS `PerSpotArrow`.
 */
data class PerSpotArrow(
    val arrow: ArrowPlot,
    val spotIndex: Int,
    val localX: Double,
    val localY: Double,
)

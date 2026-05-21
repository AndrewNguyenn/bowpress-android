package com.andrewnguyen.bowpress.feature.session.threed

import kotlin.math.acos

/** A single walked point — a lightweight value-type coordinate. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/**
 * Mirrors iOS `ElevationGrid`. A square-ish grid of terrain elevation samples
 * covering a geographic box — the raw material the course map's topographic
 * contour lines are drawn from. Held in memory for the course's lifetime.
 */
data class ElevationGrid(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val rows: Int,
    val cols: Int,
    /** Row-major elevation samples in metres — `rows * cols` entries. */
    val samples: List<Double>,
) {
    fun sample(row: Int, col: Int): Double = samples[row * cols + col]

    fun coordinate(row: Int, col: Int): GeoPoint {
        val latT = if (rows > 1) row.toDouble() / (rows - 1) else 0.0
        val lonT = if (cols > 1) col.toDouble() / (cols - 1) else 0.0
        return GeoPoint(
            latitude = minLat + latT * (maxLat - minLat),
            longitude = minLon + lonT * (maxLon - minLon),
        )
    }

    val minElevation: Double get() = samples.minOrNull() ?: 0.0
    val maxElevation: Double get() = samples.maxOrNull() ?: 0.0

    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in minLat..maxLat && longitude in minLon..maxLon
}

/**
 * One traced topographic contour at a fixed elevation — the raw
 * marching-squares output, a list of segment endpoints in geographic space.
 */
data class ContourLine(
    val elevation: Double,
    /** 0 = lowest contour level, 1 = highest — drives the ink tone. */
    val depth: Double,
    /** Segment endpoints — each consecutive pair is one line segment. */
    val segments: List<GeoPoint>,
)

/**
 * Mirrors iOS `ContourGenerator`. Generates topographic contour lines from an
 * elevation grid via the marching-squares algorithm.
 */
object ContourGenerator {

    fun levels(grid: ElevationGrid, count: Int = 5): List<Double> {
        val lo = grid.minElevation
        val hi = grid.maxElevation
        if (hi - lo <= 0.5) return emptyList()   // flat terrain — no contours
        val step = (hi - lo) / (count + 1)
        return (1..count).map { lo + step * it }
    }

    fun contours(grid: ElevationGrid, count: Int = 5): List<ContourLine> {
        val levels = levels(grid, count)
        if (levels.isEmpty()) return emptyList()
        return levels.mapIndexed { index, level ->
            ContourLine(
                elevation = level,
                depth = if (levels.size > 1) index.toDouble() / (levels.size - 1) else 0.5,
                segments = segments(grid, level),
            )
        }
    }

    /** Marching squares — emit the segment(s) where `level` crosses each cell. */
    private fun segments(grid: ElevationGrid, level: Double): List<GeoPoint> {
        val out = ArrayList<GeoPoint>()
        if (grid.rows <= 1 || grid.cols <= 1) return out

        for (r in 0 until grid.rows - 1) {
            for (c in 0 until grid.cols - 1) {
                // Cell corners, counter-clockwise from bottom-left.
                val e = doubleArrayOf(
                    grid.sample(r, c),
                    grid.sample(r, c + 1),
                    grid.sample(r + 1, c + 1),
                    grid.sample(r + 1, c),
                )
                val p = arrayOf(
                    grid.coordinate(r, c),
                    grid.coordinate(r, c + 1),
                    grid.coordinate(r + 1, c + 1),
                    grid.coordinate(r + 1, c),
                )
                var caseIndex = 0
                for (i in 0..3) if (e[i] >= level) caseIndex = caseIndex or (1 shl i)
                if (caseIndex == 0 || caseIndex == 15) continue

                fun crossing(edge: Int): GeoPoint {
                    val a = edge
                    val b = (edge + 1) % 4
                    val denom = e[b] - e[a]
                    val t = if (denom == 0.0) 0.5 else (level - e[a]) / denom
                    val tc = t.coerceIn(0.0, 1.0)
                    return GeoPoint(
                        latitude = p[a].latitude + tc * (p[b].latitude - p[a].latitude),
                        longitude = p[a].longitude + tc * (p[b].longitude - p[a].longitude),
                    )
                }

                // Edge pairs per the 16-case table. Cases 5/10 are saddles —
                // resolved one consistent way (invisible at this density).
                val connections: List<Pair<Int, Int>> = when (caseIndex) {
                    1, 14 -> listOf(3 to 0)
                    2, 13 -> listOf(0 to 1)
                    3, 12 -> listOf(3 to 1)
                    4, 11 -> listOf(1 to 2)
                    5 -> listOf(3 to 0, 1 to 2)
                    6, 9 -> listOf(0 to 2)
                    7, 8 -> listOf(3 to 2)
                    10 -> listOf(0 to 1, 2 to 3)
                    else -> emptyList()
                }
                for ((from, to) in connections) {
                    out.add(crossing(from))
                    out.add(crossing(to))
                }
            }
        }
        return out
    }
}

/** Shared 3D geometry helpers — mirrors iOS `AngleFormatting` math. */
object ThreeDGeometry {
    const val METERS_PER_DEGREE_LAT = 111_320.0
    private const val METERS_PER_YARD = 0.9144

    /** Convert a station's ranged distance to metres. */
    fun distanceMeters(value: Double, unit: String?): Double =
        if (unit == "m") value else value * METERS_PER_YARD

    /** The "shoots-like" horizontal cut distance for an inclined shot. */
    fun cutDistance(distance: Double, angleDegrees: Double): Double =
        distance * kotlin.math.cos(kotlin.math.abs(angleDegrees) * Math.PI / 180.0)

    /** Camera-axis elevation from a gravity vector — mirrors MotionAngleReader. */
    fun elevationFromGravity(gx: Float, gy: Float, gz: Float): Double {
        val magnitude = kotlin.math.sqrt((gx * gx + gy * gy + gz * gz).toDouble())
        if (magnitude == 0.0) return 0.0
        // cameraAxis ≈ device -z; angle between gravity and that axis is
        // acos(-g.z). 90° at the horizon, 0° straight down — centre on 0.
        val nz = (-gz / magnitude).coerceIn(-1.0, 1.0)
        return acos(nz) * 180.0 / Math.PI - 90.0
    }
}

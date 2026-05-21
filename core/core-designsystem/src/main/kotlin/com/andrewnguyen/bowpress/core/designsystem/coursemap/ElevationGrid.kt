package com.andrewnguyen.bowpress.core.designsystem.coursemap

import com.andrewnguyen.bowpress.core.model.CourseStation
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow

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

/**
 * Mirrors iOS `DevMockData.make3DElevationGrid`. Synthesizes a rolling-terrain
 * elevation grid — a gentle slope plus two hills — so a course map shows real
 * contour lines on the emulator without a live elevation fetch.
 */
object MockTerrain {

    private const val GRID_SIZE = 12
    private const val METERS_PER_DEGREE_LAT = 111_320.0

    /** A synthesized terrain grid filling the given geographic box. */
    fun gridForBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): ElevationGrid {
        val samples = ArrayList<Double>(GRID_SIZE * GRID_SIZE)
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val nx = c.toDouble() / (GRID_SIZE - 1)
                val ny = r.toDouble() / (GRID_SIZE - 1)
                val slope = 120 + 58 * ny
                val hill1 = 36 * exp(-(((nx - 0.32).pow(2) + (ny - 0.62).pow(2)) / 0.03))
                val hill2 = 27 * exp(-(((nx - 0.74).pow(2) + (ny - 0.30).pow(2)) / 0.045))
                samples.add(slope + hill1 + hill2)
            }
        }
        return ElevationGrid(
            minLat = minLat, maxLat = maxLat, minLon = minLon, maxLon = maxLon,
            rows = GRID_SIZE, cols = GRID_SIZE, samples = samples,
        )
    }

    /** An 800 m terrain box centred on a point — the live/Log-detail variant. */
    fun gridAroundPoint(centerLat: Double, centerLon: Double): ElevationGrid {
        val latDelta = 800.0 / METERS_PER_DEGREE_LAT
        val lonDelta = 800.0 /
            (METERS_PER_DEGREE_LAT * maxOf(cos(centerLat * Math.PI / 180.0), 0.01))
        return gridForBox(
            minLat = centerLat - latDelta, maxLat = centerLat + latDelta,
            minLon = centerLon - lonDelta, maxLon = centerLon + lonDelta,
        )
    }

    /**
     * A terrain grid sized to cover a course's stations *and* inferred targets
     * — so the feed/detail map's contours fill the same frame the course is
     * projected into. A fixed 800 m box would leave the course small inside it
     * and break the feed map's edge-to-edge fill.
     *
     * Returns null when no station has GPS coordinates — there is no area to
     * cover, and callers (`ElevationGridCache`) already handle a null grid.
     */
    fun gridCoveringCourse(coveringStations: List<CourseStation>): ElevationGrid? {
        val lats = ArrayList<Double>()
        val lons = ArrayList<Double>()
        for (s in coveringStations) {
            val lat = s.latitude
            val lon = s.longitude
            if (lat != null && lon != null) {
                lats.add(lat); lons.add(lon)
            }
            CourseMapLayout.inferredTargetGeo(s)?.let { t ->
                lats.add(t.latitude); lons.add(t.longitude)
            }
        }
        val minLat = lats.minOrNull()
        val maxLat = lats.maxOrNull()
        val minLon = lons.minOrNull()
        val maxLon = lons.maxOrNull()
        if (minLat == null || maxLat == null || minLon == null || maxLon == null) {
            return null
        }
        // A small margin so contours aren't cut hard at the course edge.
        val latM = maxOf((maxLat - minLat) * 0.12, 0.0002)
        val lonM = maxOf((maxLon - minLon) * 0.12, 0.0002)
        return gridForBox(
            minLat = minLat - latM, maxLat = maxLat + latM,
            minLon = minLon - lonM, maxLon = maxLon + lonM,
        )
    }
}

/**
 * A process-wide in-memory cache of fetched/seeded elevation grids, keyed by
 * each grid's own bounding box.
 *
 * iOS `ElevationService` caches grids to disk; the Android port has always
 * held the fetched grid only in the view model. This cache adds the missing
 * cross-screen lookup so the social feed's mock 3D courses (whose grids are
 * seeded at startup) can draw contours — the same role iOS's disk cache plays.
 * It is in-memory rather than on-disk: seeding and reads happen in one process
 * session, so persistence isn't needed for the mock-data use case.
 */
object ElevationGridCache {

    /** Cap on cached grids — terrain doesn't change; oldest-first eviction. */
    private const val MAX_GRIDS = 50

    /**
     * Box-keyed grids; access-order is irrelevant, insertion order drives
     * oldest-first eviction via `removeEldestEntry`. All access is guarded by
     * `@Synchronized` — `LinkedHashMap` is not thread-safe on its own.
     */
    private val grids = object : LinkedHashMap<ElevationGrid, ElevationGrid>() {
        override fun removeEldestEntry(eldest: Map.Entry<ElevationGrid, ElevationGrid>): Boolean =
            size > MAX_GRIDS
    }

    /** Cache a grid. Two distinct course areas keep distinct boxes. */
    @Synchronized
    fun store(grid: ElevationGrid) {
        grids.putIfAbsent(grid, grid)
    }

    /** A cached grid whose box contains the point, or null. */
    @Synchronized
    fun covering(latitude: Double, longitude: Double): ElevationGrid? =
        grids.keys.firstOrNull { it.contains(latitude, longitude) }

    /**
     * The cached grid covering a course — looked up by the centroid of its
     * stations. Lets the feed map draw contours, the same way the live and
     * Log-detail maps do. Mirrors iOS `ElevationService.cachedGrid(coveringStations:)`.
     */
    fun covering(stations: List<CourseStation>): ElevationGrid? {
        val geo = stations.mapNotNull { s ->
            val lat = s.latitude ?: return@mapNotNull null
            val lon = s.longitude ?: return@mapNotNull null
            lat to lon
        }
        if (geo.isEmpty()) return null
        val lat = geo.sumOf { it.first } / geo.size
        val lon = geo.sumOf { it.second } / geo.size
        return covering(lat, lon)
    }

    /** Test seam — drop all cached grids. */
    @Synchronized
    fun clear() {
        grids.clear()
    }
}

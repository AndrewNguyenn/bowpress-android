package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.designsystem.coursemap.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.cos
import kotlin.math.max

/**
 * Mirrors iOS `ElevationService`. Fetches the terrain-elevation grid the 3D
 * course map draws its topographic contours from, from Open-Meteo's free
 * elevation API (no key required).
 *
 * The Android port holds the fetched grid in the view model for the course's
 * lifetime rather than caching it to disk (an offline-cache is a follow-up).
 */
object ElevationService {

    /** Grid resolution per side — 10 × 10 = 100, Open-Meteo's per-request ceiling. */
    private const val GRID_SIZE = 10
    /** Half the side of the sampled box, metres (≈ 1.6 km box around the start). */
    private const val HALF_SPAN_METERS = 800.0

    /** Fetch the elevation grid around a point. Returns null on any failure. */
    suspend fun fetchGrid(centerLat: Double, centerLon: Double): ElevationGrid? =
        withContext(Dispatchers.IO) {
            runCatching {
                val latDelta = HALF_SPAN_METERS / ThreeDGeometry.METERS_PER_DEGREE_LAT
                val lonDelta = HALF_SPAN_METERS /
                    (ThreeDGeometry.METERS_PER_DEGREE_LAT * max(cos(centerLat * Math.PI / 180.0), 0.01))
                val minLat = centerLat - latDelta
                val maxLat = centerLat + latDelta
                val minLon = centerLon - lonDelta
                val maxLon = centerLon + lonDelta

                val lats = ArrayList<String>(GRID_SIZE * GRID_SIZE)
                val lons = ArrayList<String>(GRID_SIZE * GRID_SIZE)
                for (r in 0 until GRID_SIZE) {
                    for (c in 0 until GRID_SIZE) {
                        val latT = r.toDouble() / (GRID_SIZE - 1)
                        val lonT = c.toDouble() / (GRID_SIZE - 1)
                        lats.add("%.5f".format(minLat + latT * (maxLat - minLat)))
                        lons.add("%.5f".format(minLon + lonT * (maxLon - minLon)))
                    }
                }
                val url = URL(
                    "https://api.open-meteo.com/v1/elevation" +
                        "?latitude=${lats.joinToString(",")}" +
                        "&longitude=${lons.joinToString(",")}",
                )
                val text = url.openConnection().apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }.getInputStream().bufferedReader().use { it.readText() }

                val array = JSONObject(text).getJSONArray("elevation")
                if (array.length() != GRID_SIZE * GRID_SIZE) return@runCatching null
                val samples = (0 until array.length()).map { array.getDouble(it) }
                ElevationGrid(
                    minLat = minLat, maxLat = maxLat, minLon = minLon, maxLon = maxLon,
                    rows = GRID_SIZE, cols = GRID_SIZE, samples = samples,
                )
            }.getOrNull()
        }
}

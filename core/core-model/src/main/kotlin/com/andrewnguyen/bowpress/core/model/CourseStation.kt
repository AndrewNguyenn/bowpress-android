package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors iOS `CourseStation`. One foam-target shot on a 3D course — a station
 * is to a 3D session what an arrow plot + end is to a range session.
 *
 * The archer ranges the target, captures the shot's incline angle and the
 * compass bearing, plots a single arrow on the circular target, and the
 * station records where it was shot from (GPS) so the course can be traced
 * on a map. `bearingDegrees` + the GPS fix + distance + angle drive the map's
 * inferred-target projection.
 *
 * Photos (the composed scene, and an optional close-up of the arrow in the
 * foam) are stored locally keyed by station id — only the `hasScenePhoto` /
 * `hasArrowPhoto` flags travel in this DTO and to the server.
 */
@Serializable
data class CourseStation(
    val id: String,
    val sessionId: String,
    /** 1-based shot order along the course. */
    val stationNumber: Int,
    /** Archer-estimated (or rangefinder) distance. Null when ranging was skipped. */
    val estimatedDistance: Double? = null,
    /** Unit `estimatedDistance` was entered in ("yd" / "m"). */
    val distanceUnit: String? = null,
    /** Shot incline in degrees: positive uphill, negative downhill. 0 when uncaptured. */
    val angleDegrees: Double = 0.0,
    /** Compass heading at the shutter, 0…360 clockwise from true north. */
    val bearingDegrees: Double? = null,
    /** Capture location — traces the route on the course map. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Station score on the session's scoring system. 0 = miss. */
    val ring: Int = 0,
    /** Arrow position on the circular target, normalized -1…1 from centre. */
    val plotX: Double? = null,
    val plotY: Double? = null,
    val hasScenePhoto: Boolean = false,
    val hasArrowPhoto: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val shotAt: Instant,
    val notes: String? = null,
)

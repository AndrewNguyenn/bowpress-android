package com.andrewnguyen.bowpress.feature.session.threed

import kotlin.math.acos

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

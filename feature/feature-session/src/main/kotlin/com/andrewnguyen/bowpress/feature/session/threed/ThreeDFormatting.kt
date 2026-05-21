package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.model.UnitSystem
import kotlin.math.abs
import kotlin.math.roundToInt

/** Mirrors iOS `AngleFormatting`. */
object AngleFormatting {
    /** Signed whole-degree string, e.g. "+7°", "−12°", "0°". */
    fun signed(degrees: Double): String {
        val rounded = degrees.roundToInt()
        return when {
            rounded > 0 -> "+$rounded°"
            rounded < 0 -> "−${abs(rounded)}°"
            else -> "0°"
        }
    }
}

/** Mirrors iOS `BearingFormatting`. */
object BearingFormatting {
    private val shortPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    private val longPoints = listOf(
        "North", "North-East", "East", "South-East",
        "South", "South-West", "West", "North-West",
    )

    fun cardinal(bearing: Double, long: Boolean = false): String {
        val index = (((bearing % 360) + 360) % 360 / 45.0).roundToInt() % 8
        return if (long) longPoints[index] else shortPoints[index]
    }

    /** e.g. "115° SE". */
    fun compass(bearing: Double): String =
        "${bearing.roundToInt()}° ${cardinal(bearing)}"
}

/** Mirrors iOS `DistanceFormatting`. */
object DistanceFormatting {
    /**
     * Compact distance for a station — "—" when unranged. Shows a tenth when
     * the ranged distance has one ("28.4yd") and drops it for a whole number.
     */
    fun short(value: Double?, unit: String?, system: UnitSystem): String {
        if (value == null) return "—"
        val u = unit ?: if (system == UnitSystem.METRIC) "m" else "yd"
        val tenths = (value * 10).roundToInt() / 10.0
        val num = if (tenths == tenths.roundToInt().toDouble()) {
            tenths.roundToInt().toString()
        } else {
            "%.1f".format(tenths)
        }
        return "$num$u"
    }
}

package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors iOS `BowType` — wire values are lower-case (`compound`, `recurve`, `barebow`). */
@Serializable
enum class BowType {
    @SerialName("compound") COMPOUND,
    @SerialName("recurve") RECURVE,
    @SerialName("barebow") BAREBOW;

    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** Mirrors iOS `RearStabSide` — lower-case strings on the wire. */
@Serializable
enum class RearStabSide {
    @SerialName("none") NONE,
    @SerialName("left") LEFT,
    @SerialName("right") RIGHT,
    @SerialName("both") BOTH;

    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** Mirrors iOS `ArrowPlot.Zone` — upper-case strings (`CENTER`, `N`, `NE`, ...). */
@Serializable
enum class Zone {
    @SerialName("CENTER") CENTER,
    @SerialName("N") N,
    @SerialName("NE") NE,
    @SerialName("E") E,
    @SerialName("SE") SE,
    @SerialName("S") S,
    @SerialName("SW") SW,
    @SerialName("W") W,
    @SerialName("NW") NW
}

/** Mirrors iOS `ArrowConfiguration.FletchingType` — `vane` / `feather`. */
@Serializable
enum class FletchingType {
    @SerialName("vane") VANE,
    @SerialName("feather") FEATHER
}

/**
 * Shaft diameter — mirrors iOS `ArrowConfiguration.ShaftDiameter`. Wire is a Double
 * representing millimetres; fractional-inch sizes are encoded as their mm value
 * (e.g. 19/64" ≈ 7.540625). Use [rawValue] when serialising to JSON.
 */
@Serializable
enum class ShaftDiameter(val rawValue: Double, val displayName: String) {
    MM3_2(3.2, "3.2 mm"),
    MM4_0(4.0, "4.0 mm"),
    MM5_0(5.0, "5.0 mm"),
    IN19_64(7.540625, "19/64\""),
    IN21_64(8.334375, "21/64\""),
    IN22_64(8.731250, "22/64\""),
    IN23_64(9.128125, "23/64\""),
    IN24_64(9.525000, "24/64\""),
    IN25_64(9.921875, "25/64\""),
    IN26_64(10.318750, "26/64\""),
    IN27_64(10.715625, "27/64\"");

    companion object {
        fun fromRaw(raw: Double?): ShaftDiameter? =
            if (raw == null) null else entries.firstOrNull { it.rawValue == raw }
    }
}

/**
 * Period selector for analytics endpoints. Wire values are short codes
 * (`3d`, `7d`, `14d`, `30d`, `90d`, `180d`, `365d`) — mirrors iOS.
 */
@Serializable
enum class AnalyticsPeriod(val wire: String, val label: String, val days: Int) {
    @SerialName("3d") THREE_DAYS("3d", "3 Days", 3),
    @SerialName("7d") WEEK("7d", "1 Week", 7),
    @SerialName("14d") TWO_WEEKS("14d", "2 Weeks", 14),
    @SerialName("30d") MONTH("30d", "1 Month", 30),
    @SerialName("90d") THREE_MONTHS("90d", "3 Months", 90),
    @SerialName("180d") SIX_MONTHS("180d", "6 Months", 180),
    @SerialName("365d") YEAR("365d", "1 Year", 365);

    /** Duration of one period window, in seconds — mirrors the iOS `TimeInterval` extension. */
    val durationSeconds: Long get() = days.toLong() * 86_400L

    companion object {
        fun fromWire(wire: String): AnalyticsPeriod? = entries.firstOrNull { it.wire == wire }
    }
}

/** Mirrors iOS `AnalyticsSuggestion.DeliveryType`. */
@Serializable
enum class DeliveryType {
    @SerialName("push") PUSH,
    @SerialName("inApp") IN_APP,
    @SerialName("reinforcement") REINFORCEMENT
}

/** Mirrors iOS `AuthProvider`. */
@Serializable
enum class AuthProvider {
    @SerialName("email") EMAIL,
    @SerialName("apple") APPLE,
    @SerialName("google") GOOGLE
}

/** Mirrors iOS `ChangeImpactCard.Classification` — `clean` or `compound`. */
@Serializable
enum class ChangeClassification {
    @SerialName("clean") CLEAN,
    @SerialName("compound") COMPOUND
}

/** Mirrors iOS `TagCorrelation.Strength`. */
@Serializable
enum class TagStrength {
    @SerialName("weak") WEAK,
    @SerialName("moderate") MODERATE,
    @SerialName("strong") STRONG
}

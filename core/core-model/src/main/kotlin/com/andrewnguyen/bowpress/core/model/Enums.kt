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

/**
 * Mirrors iOS `TargetFaceType`. The target face the archer is shooting at.
 *  - [SIX_RING]: inner-6 indoor face (compound default). Scored 6..X.
 *  - [TEN_RING]: full WA 10-ring face (recurve / barebow default). Scored 1..X.
 *
 * Wire values: `six_ring` / `ten_ring`.
 */
@Serializable
enum class TargetFaceType {
    @SerialName("six_ring") SIX_RING,
    @SerialName("ten_ring") TEN_RING;

    val label: String get() = when (this) {
        SIX_RING -> "6-Ring"
        TEN_RING -> "10-Ring"
    }

    companion object {
        /** Smart default for a bow style: compound -> 6-ring; everything else -> 10-ring. */
        fun defaultFor(bowType: BowType): TargetFaceType = when (bowType) {
            BowType.COMPOUND -> SIX_RING
            BowType.RECURVE, BowType.BAREBOW -> TEN_RING
        }

        /**
         * Recover a [TargetFaceType] from a free-text face label as it lives
         * on a shared-session row (`shared_sessions.face`).
         *
         * Two passes:
         *  1. Literal wire-value parse — `"six_ring"` / `"ten_ring"`
         *     (case-insensitive). 2 pre-migration-0038 sessions in prod
         *     still carry the snake_case enum value as a label rather
         *     than the human form.
         *  2. Free-text heuristic — `"10"` → TEN_RING, `"6-Ring"` /
         *     `"Vegas"` / `"Spot"` → SIX_RING. The 10 check runs first so
         *     `"60cm"` / `"WA 60"` don't misfire as sixRing.
         *
         * Returns null when the label carries no clear signal; the caller
         * then falls back to the bow-default or to [TEN_RING].
         *
         * Mirrors iOS commit 3c1a305 (parity B2).
         */
        fun matching(label: String?): TargetFaceType? {
            val raw = label?.takeIf { it.isNotEmpty() } ?: return null
            // Literal wire-value parse first.
            when (raw.lowercase()) {
                "six_ring" -> return SIX_RING
                "ten_ring" -> return TEN_RING
            }
            // Free-text heuristic.
            val f = raw.lowercase()
            if (f.contains("10")) return TEN_RING
            if (f.contains("6-ring") || f.contains("spot") || f.contains("vegas")) return SIX_RING
            return null
        }
    }
}

/**
 * Mirrors iOS `TargetLayout` — how the target faces are arranged on the boss.
 *  - [SINGLE]: one face.
 *  - [TRIANGLE]: a 3-spot Vegas triangle (two faces on top, one below).
 *  - [VERTICAL]: a 3-spot vertical strip (three faces stacked).
 *
 * Wire values: `single` / `triangle` / `vertical`. Defaulted to [SINGLE] so
 * sessions that predate the field decode unchanged.
 */
@Serializable
enum class TargetLayout {
    @SerialName("single") SINGLE,
    @SerialName("triangle") TRIANGLE,
    @SerialName("vertical") VERTICAL;

    val label: String get() = when (this) {
        SINGLE -> "Single"
        TRIANGLE -> "3-spot triangle"
        VERTICAL -> "3-spot vertical"
    }

    /** True for the Vegas multi-spot layouts ([TRIANGLE] / [VERTICAL]). */
    val isMultiSpot: Boolean get() = this != SINGLE

    companion object {
        /**
         * Vegas spot face diameter in mm. Each of the three 6-ring spots on a
         * 40cm Vegas 3-spot card prints at 180mm. Drives both the visible
         * rendering scale and the WA edge-rule scoring math (arrow shaft /
         * spot diameter, not / face diameter). Mirrors iOS
         * `TargetLayout.spotDiameterMm`.
         */
        const val SPOT_DIAMETER_MM: Double = 180.0
    }
}

/**
 * Mirrors iOS `ShootingDistance`. Distance from the shooter to the target. Optional
 * on `ShootingSession` — sessions without a chosen distance stay null and the
 * analytics filter only includes a session in a specific-distance view when the
 * value matches exactly. Wire values: `20yd` / `50m` / `70m`.
 */
@Serializable
enum class ShootingDistance {
    @SerialName("20yd") YARDS_20,
    @SerialName("50m")  METERS_50,
    @SerialName("70m")  METERS_70;

    val label: String get() = when (this) {
        YARDS_20 -> "20yd"
        METERS_50 -> "50m"
        METERS_70 -> "70m"
    }

    /** Server-side spelling — what the API expects in the `?distance=` query param. */
    val wire: String get() = label
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

package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * A calibrated sight reading at a specific distance for a specific bow.
 * Per-archer (`userId`) and per-bow (`bowId`). Marks live with the bow
 * because that's how archers think about their sight tape.
 *
 * Mirrors iOS `SightMark` (Models/SightMark.swift). `mark` is numeric so
 * the suggester can interpolate. Multi-pin sights (discrete pin labels)
 * are out of scope for v1.
 *
 * `isSuggestion = true` flags a synthesized mark from
 * [com.andrewnguyen.bowpress.core.analytics.SightMarkSuggester] — these
 * are filtered out before refitting so suggestions don't compound on
 * suggestions.
 */
@Serializable
data class SightMark(
    val id: String,
    val userId: String,
    val bowId: String,
    val distance: Double,
    val distanceUnit: DistanceUnit,
    val mark: Double,
    val note: String? = null,
    val isSuggestion: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
) {
    /**
     * Distance expressed in meters, used for unit-normalized math
     * (sorting, spread checks, fitting). Never shown to the user — that
     * always renders in the archer's preferred unit.
     */
    val distanceInMeters: Double
        get() = distance * distanceUnit.metersPerUnit
}

@Serializable
enum class DistanceUnit {
    YARDS,
    METERS;

    /** Conversion to meters for unit-normalized comparisons. */
    val metersPerUnit: Double
        get() = when (this) {
            YARDS -> 0.9144
            METERS -> 1.0
        }

    /** Short suffix used in display (`"yd"` / `"m"`). */
    val shortLabel: String
        get() = when (this) {
            YARDS -> "yd"
            METERS -> "m"
        }

    companion object {
        /** The unit a given [UnitSystem] prefers for distance. */
        fun preferred(for_: UnitSystem): DistanceUnit =
            if (for_ == UnitSystem.IMPERIAL) YARDS else METERS
    }
}

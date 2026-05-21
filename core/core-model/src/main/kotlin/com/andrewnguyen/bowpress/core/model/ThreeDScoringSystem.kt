package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One concentric scoring band on a 3D target — a value and the outer edge of
 * the band as a fraction (0…1) of the scoring circle's radius.
 */
data class ThreeDRingBand(
    val value: Int,
    /** A plotted arrow at normalized radius `d` scores this band when `d < outerFraction`. */
    val outerFraction: Double,
)

/**
 * Mirrors iOS `ThreeDScoringSystem`. A 3D-archery governing-body scoring
 * system — 3D targets score on animal kill-zone rings, and the values + band
 * sizes differ per body. Band geometry matches the iOS circular target so the
 * plot surface and the score it computes agree exactly.
 */
@Serializable
enum class ThreeDScoringSystem {
    @SerialName("ASA") ASA,
    @SerialName("IBO") IBO,
    @SerialName("WA3D") WA3D;

    val label: String
        get() = when (this) {
            ASA -> "ASA"
            IBO -> "IBO"
            WA3D -> "WA 3D"
        }

    /** Scoring bands, innermost first (highest value first). */
    val bands: List<ThreeDRingBand>
        get() = when (this) {
            ASA -> listOf(
                ThreeDRingBand(14, 0.18),
                ThreeDRingBand(12, 0.34),
                ThreeDRingBand(10, 0.56),
                ThreeDRingBand(8, 0.78),
                ThreeDRingBand(5, 1.00),
            )
            IBO -> listOf(
                ThreeDRingBand(11, 0.22),
                ThreeDRingBand(10, 0.48),
                ThreeDRingBand(8, 0.74),
                ThreeDRingBand(5, 1.00),
            )
            WA3D -> listOf(
                ThreeDRingBand(10, 0.28),
                ThreeDRingBand(9, 0.54),
                ThreeDRingBand(8, 0.78),
                ThreeDRingBand(5, 1.00),
            )
        }

    /** Ring values, innermost first. */
    val rings: List<Int> get() = bands.map { it.value }

    /** Eyebrow string for the setup tile, e.g. "14·12·10·8·5". */
    val ringSummary: String get() = rings.joinToString("·")

    /** The highest ring value — a perfect station. */
    val maxRing: Int get() = bands.firstOrNull()?.value ?: 10

    /**
     * The ring scored for a normalized radial distance `d` (0 = dead centre,
     * 1 = edge of the scoring body). Anything beyond the outermost band is a
     * miss (0).
     */
    fun ringForNormalizedRadius(d: Double): Int {
        for (band in bands) {
            if (d < band.outerFraction) return band.value
        }
        return 0
    }
}

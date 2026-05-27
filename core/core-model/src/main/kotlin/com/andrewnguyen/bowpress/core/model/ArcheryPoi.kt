package com.andrewnguyen.bowpress.core.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure (JVM-testable, no Android imports) ranker for archery-POI candidates.
 *
 * Both the session-end auto-tag (`feature.session.NearestRangeFinder`) and
 * the manual location picker (`feature.social.ui.location.LocationTagPicker`)
 * need to pick the nearest archery range from a list of geocoder candidates.
 * The I/O wrappers around `Geocoder.getFromLocationName("archery", ...)`
 * stay platform-side; this object owns the name filter, the radius cut,
 * and the proximity rank so the contract can't drift between the two
 * call sites and so it can be unit-tested without Robolectric.
 *
 * The 0.25 mi radius is intentionally tight — "auto" should only fire
 * when the archer is plausibly on-site at a known range; anything farther
 * is better handled by an explicit search-bar pick.
 */
object ArcheryPoi {

    /** 0.25 mi in metres — strict cutoff for the "on-site" decision. */
    const val RADIUS_METERS: Double = 402.336

    /**
     * A candidate POI distilled from a Geocoder result. The wrapper code
     * is responsible for extracting [name] (typically `featureName ?:
     * thoroughfare ?: locality`), [latitude], and [longitude] before
     * passing the list to [pickNearest].
     */
    data class Candidate(
        val name: String,
        val latitude: Double,
        val longitude: Double,
    )

    /**
     * Return the closest candidate whose name reads like an archery range
     * AND that sits within [radiusMeters] of (`anchorLat`, `anchorLng`),
     * or null when no candidate qualifies.
     *
     * Name filter: case-insensitive substring match on "archery" or
     * "range" — same heuristic the auto-tag finder uses to reject e.g.
     * "Queens Plaza" while accepting "Queens Archery" or "Tactical Range".
     */
    fun pickNearest(
        candidates: List<Candidate>,
        anchorLat: Double,
        anchorLng: Double,
        radiusMeters: Double = RADIUS_METERS,
    ): String? {
        return candidates
            .asSequence()
            .filter { matchesArcheryName(it.name) }
            .map { it to haversineMeters(anchorLat, anchorLng, it.latitude, it.longitude) }
            .filter { (_, d) -> d <= radiusMeters }
            .minByOrNull { (_, d) -> d }
            ?.first
            ?.name
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * True when [name] looks like an archery range. Lowercased against
     * `Locale.ROOT` so a Turkish-locale device doesn't transform the "I"
     * in "Archery" / "Range" into a dotless ı and miss the match.
     */
    internal fun matchesArcheryName(name: String): Boolean {
        if (name.isBlank()) return false
        val lc = name.lowercase(java.util.Locale.ROOT)
        return lc.contains("archery") || lc.contains("range")
    }

    /**
     * Great-circle distance in metres between two lat/lng pairs. Accurate
     * to ~0.5% over the sub-kilometre distances this ranker cares about —
     * well under the precision of a consumer GPS fix or geocoder result.
     */
    private fun haversineMeters(
        lat1: Double, lng1: Double, lat2: Double, lng2: Double,
    ): Double {
        val earthRadiusM = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).let { it * it }
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }
}

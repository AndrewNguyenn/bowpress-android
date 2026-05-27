package com.andrewnguyen.bowpress.feature.session

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.andrewnguyen.bowpress.core.model.ArcheryPoi
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Mirrors iOS `Sources/BowPress/Session/NearestRangeFinder.swift`.
 *
 * At session-end the share path auto-attaches the nearest archery range to
 * the post so it lands on the feed with a location chip (§18 auto-tag). The
 * lookup is silent-skip in every failure mode:
 *
 *   - Location permission was never granted → return null. Do NOT prompt at
 *     session end; a permission dialog is hostile in a "tap End → save"
 *     moment, and the archer can still tag manually from the finish sheet.
 *   - The GPS fix doesn't arrive within [TIMEOUT_MS] → return null.
 *   - No archery POI within [RADIUS_METERS] → fall back to the reverse-
 *     geocoded city/area label; null only when even that fails.
 *
 * The strict radius is intentionally tight (¼ mile). "Auto" only fires when
 * the archer is plausibly on-site; anything farther would surface the
 * wrong range and is better handled by the manual picker.
 *
 * **Places Autocomplete deviation:** iOS uses `MKLocalSearch` with the
 * "archery" query; Android equivalent would be Places SDK Autocomplete +
 * an API key. The app already wires Google Places lazily for the location
 * tag picker but doesn't ship an API key, so the strict-POI tier is
 * implemented through the platform `Geocoder` with name-substring matching
 * — gracefully degrades when offline (silent-skip → reverse-geocode
 * fallback), and keeps the function key-free. A future change can swap in
 * Places SDK behind the same suspend signature.
 */
object NearestRangeFinder {

    /** Hard cap on the one-shot fix + geocode chain. */
    private const val TIMEOUT_MS = 10_000L

    /**
     * Look up the nearest archery POI to the archer's current GPS fix.
     * Returns a `SessionLocation` ready for `socialRepository.shareSession()`
     * or `null` if anything along the chain didn't pan out.
     *
     * Tiered: first the strict 0.25-mile archery POI search (clean tag when
     * on-site at a known range), then a reverse-geocoded city/locality label
     * when nothing archery-specific is nearby. A session at home or a
     * backyard target ends up tagged "Cupertino, CA" instead of nothing —
     * informative beats empty, and the archer can clear it from the finish
     * sheet if they don't want a tag.
     */
    suspend fun nearestArcheryRangeOrFallback(context: Context): SessionLocation? {
        if (!hasLocationPermission(context)) return null
        val fix = withTimeoutOrNull(TIMEOUT_MS) { currentLocation(context) } ?: return null
        return nearestArcheryRangeOrFallback(context, fix)
    }

    /**
     * Same as the suspend overload above but with a caller-supplied [fix] —
     * used by the finish sheet's `LaunchedEffect` so the location lookup can
     * reuse a location the picker just grabbed without a second fused-client
     * call. Mirrors iOS `find(loc:)`.
     */
    suspend fun nearestArcheryRangeOrFallback(
        context: Context,
        fix: Location,
    ): SessionLocation? {
        nearestArcheryPoi(context, fix)?.let { return it }
        return reverseGeocodedArea(context, fix)
    }

    // ── Strict POI tier ─────────────────────────────────────────────────────

    /**
     * Strict archery-POI search around [fix]. Returns a `SessionLocation`
     * whose `name` is the matched POI's primary line, or null when none of
     * the candidates within `ArcheryPoi.RADIUS_METERS` looks like an archery
     * range.
     *
     * The Geocoder query + lat/lng extraction stays here (platform I/O);
     * the name filter + radius cut + proximity rank live in
     * `ArcheryPoi.pickNearest` so they can't drift between this auto-tag
     * path and the manual `LocationTagPicker.nearestArcheryPoiName`. Both
     * call sites are covered by `ArcheryPoiTest`.
     */
    private suspend fun nearestArcheryPoi(
        context: Context,
        fix: Location,
    ): SessionLocation? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        runCatching {
            // ~2km bias box around the fix — the geocoder won't strictly
            // clip to it but ranks results by proximity. The ranker
            // applies the strict radius cut.
            val degBias = 0.02
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(
                "archery",
                /* maxResults = */ 8,
                /* lowerLeftLatitude  = */ fix.latitude - degBias,
                /* lowerLeftLongitude = */ fix.longitude - degBias,
                /* upperRightLatitude = */ fix.latitude + degBias,
                /* upperRightLongitude= */ fix.longitude + degBias,
            ).orEmpty()
            val candidates = addresses.mapNotNull { addr ->
                if (!addr.hasLatitude() || !addr.hasLongitude()) return@mapNotNull null
                val name = (addr.featureName ?: addr.thoroughfare ?: addr.locality)
                    ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ArcheryPoi.Candidate(name = name, latitude = addr.latitude, longitude = addr.longitude)
            }
            val pickedName = ArcheryPoi.pickNearest(
                candidates = candidates,
                anchorLat = fix.latitude,
                anchorLng = fix.longitude,
            ) ?: return@runCatching null
            // The ranker hands back the winning name; recover its
            // coordinates from the matched candidate so the SessionLocation
            // carries the POI's lat/lng (not the archer's fix).
            val winner = candidates.first { it.name.trim() == pickedName }
            SessionLocation(name = pickedName, latitude = winner.latitude, longitude = winner.longitude)
        }.getOrNull()
    }

    // ── Reverse-geocode fallback ────────────────────────────────────────────

    /**
     * Reverse-geocode [fix] to a `"Name, City"` / `"City, State"` label so
     * the share still lands with a recognizable place tag. Silent-skip on
     * any failure. Mirrors iOS `areaLabel(from:)` priority order.
     */
    private suspend fun reverseGeocodedArea(
        context: Context,
        fix: Location,
    ): SessionLocation? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        val placemark = runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(fix.latitude, fix.longitude, 1)?.firstOrNull()
        }.getOrNull() ?: return@withContext null
        val name = areaLabel(
            featureName = placemark.featureName,
            locality = placemark.locality,
            subLocality = placemark.subLocality,
            adminArea = placemark.adminArea,
        ) ?: return@withContext null
        SessionLocation(name = name, latitude = fix.latitude, longitude = fix.longitude)
    }

    /**
     * Priority order, kept testable: `"name, locality"`, then
     * `"locality, adminArea"`, then `"subLocality, locality"`, then plain
     * name. Anything less is dropped — a bare country code or null name
     * would be worse than no tag.
     */
    internal fun areaLabel(
        featureName: String?,
        locality: String?,
        subLocality: String?,
        adminArea: String?,
    ): String? {
        if (!featureName.isNullOrBlank() && !locality.isNullOrBlank() && featureName != locality) {
            return "$featureName, $locality"
        }
        if (!locality.isNullOrBlank() && !adminArea.isNullOrBlank()) {
            return "$locality, $adminArea"
        }
        if (!subLocality.isNullOrBlank() && !locality.isNullOrBlank()) {
            return "$subLocality, $locality"
        }
        return featureName?.takeIf { it.isNotBlank() } ?: locality?.takeIf { it.isNotBlank() }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(context: Context): Location? {
        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .await()
        }.getOrNull()
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

package com.andrewnguyen.bowpress.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the contract shared by `NearestRangeFinder.nearestArcheryPoi` and
 * `LocationTagPicker.nearestArcheryPoiName`: name filter, radius cut, and
 * proximity rank. Both call sites delegate to `ArcheryPoi.pickNearest`, so
 * these tests are the single source of truth for the "should this POI win"
 * decision.
 */
class ArcheryPoiTest {

    // Anchor: Queens Archery (40.7615, -73.7956). Used in every test so
    // distances are real-ish and easy to reason about against street-view.
    private val anchorLat = 40.7615
    private val anchorLng = -73.7956

    private fun candidate(
        name: String, lat: Double = anchorLat, lng: Double = anchorLng,
    ) = ArcheryPoi.Candidate(name, lat, lng)

    @Test
    fun `picks the only archery-named candidate within radius`() {
        val picked = ArcheryPoi.pickNearest(
            listOf(candidate("Queens Archery")),
            anchorLat, anchorLng,
        )
        assertThat(picked).isEqualTo("Queens Archery")
    }

    @Test
    fun `rejects candidates that don't look like an archery range`() {
        val picked = ArcheryPoi.pickNearest(
            listOf(
                candidate("Queens Plaza"),
                candidate("39th Avenue Diner"),
            ),
            anchorLat, anchorLng,
        )
        assertThat(picked).isNull()
    }

    @Test
    fun `accepts 'range' as well as 'archery'`() {
        val picked = ArcheryPoi.pickNearest(
            listOf(candidate("Tactical Range")),
            anchorLat, anchorLng,
        )
        assertThat(picked).isEqualTo("Tactical Range")
    }

    @Test
    fun `name match is case-insensitive`() {
        val picked = ArcheryPoi.pickNearest(
            listOf(candidate("QUEENS ARCHERY")),
            anchorLat, anchorLng,
        )
        assertThat(picked).isEqualTo("QUEENS ARCHERY")
    }

    @Test
    fun `drops candidates outside the 0_25 mi radius`() {
        // ~600m east of anchor (well past 402m cutoff). 1 deg lng ≈ 84km
        // at lat 40, so 0.0072 ≈ 600m east.
        val farLng = anchorLng + 0.0072
        val picked = ArcheryPoi.pickNearest(
            listOf(candidate("Far Archery", lat = anchorLat, lng = farLng)),
            anchorLat, anchorLng,
        )
        assertThat(picked).isNull()
    }

    @Test
    fun `picks closest when multiple candidates are within radius`() {
        // ~100m north and ~300m north of anchor. 1 deg lat ≈ 111km, so
        // 0.0009 ≈ 100m, 0.0027 ≈ 300m.
        val near = candidate("Near Archery", lat = anchorLat + 0.0009, lng = anchorLng)
        val farther = candidate("Farther Archery", lat = anchorLat + 0.0027, lng = anchorLng)
        val picked = ArcheryPoi.pickNearest(listOf(farther, near), anchorLat, anchorLng)
        assertThat(picked).isEqualTo("Near Archery")
    }

    @Test
    fun `blank name is skipped, not crashed on`() {
        val picked = ArcheryPoi.pickNearest(
            listOf(candidate("   "), candidate("Queens Archery")),
            anchorLat, anchorLng,
        )
        assertThat(picked).isEqualTo("Queens Archery")
    }

    @Test
    fun `trims whitespace off the winning name`() {
        val picked = ArcheryPoi.pickNearest(
            listOf(candidate("  Queens Archery  ")),
            anchorLat, anchorLng,
        )
        assertThat(picked).isEqualTo("Queens Archery")
    }

    @Test
    fun `empty candidate list returns null`() {
        val picked = ArcheryPoi.pickNearest(emptyList(), anchorLat, anchorLng)
        assertThat(picked).isNull()
    }

    @Test
    fun `respects an overridden radius`() {
        // A candidate ~600m away is outside the default 402m, but
        // inside an explicit 1000m radius.
        val far = candidate("Far Archery", lat = anchorLat, lng = anchorLng + 0.0072)
        val withDefault = ArcheryPoi.pickNearest(listOf(far), anchorLat, anchorLng)
        val withWider = ArcheryPoi.pickNearest(listOf(far), anchorLat, anchorLng, radiusMeters = 1_000.0)
        assertThat(withDefault).isNull()
        assertThat(withWider).isEqualTo("Far Archery")
    }
}

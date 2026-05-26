package com.andrewnguyen.bowpress.feature.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-Kotlin tests for the reverse-geocode label priority — Geocoder /
 * fused-client paths can't run on the JVM, but the label-shape logic is the
 * subtle part. Mirrors iOS coverage of `areaLabel(from:)`.
 */
class NearestRangeFinderTest {

    @Test
    fun `prefers feature name plus locality when different`() {
        val label = NearestRangeFinder.areaLabel(
            featureName = "Easton Foundation Center",
            locality = "Newberry",
            subLocality = null,
            adminArea = "FL",
        )
        assertThat(label).isEqualTo("Easton Foundation Center, Newberry")
    }

    @Test
    fun `falls back to city plus state when feature name absent`() {
        val label = NearestRangeFinder.areaLabel(
            featureName = null,
            locality = "Cupertino",
            subLocality = null,
            adminArea = "CA",
        )
        assertThat(label).isEqualTo("Cupertino, CA")
    }

    @Test
    fun `falls back to sub-locality plus city when only sub and city present`() {
        val label = NearestRangeFinder.areaLabel(
            featureName = null,
            locality = "San Francisco",
            subLocality = "Mission",
            adminArea = null,
        )
        assertThat(label).isEqualTo("Mission, San Francisco")
    }

    @Test
    fun `skips feature name when it equals the locality`() {
        // A Geocoder result for the city centre returns featureName == locality;
        // the iOS version's "n != city" guard explicitly skips the trivial
        // "Cupertino, Cupertino" duplicate.
        val label = NearestRangeFinder.areaLabel(
            featureName = "Cupertino",
            locality = "Cupertino",
            subLocality = null,
            adminArea = "CA",
        )
        assertThat(label).isEqualTo("Cupertino, CA")
    }

    @Test
    fun `falls back to featureName alone when nothing else qualifies`() {
        val label = NearestRangeFinder.areaLabel(
            featureName = "Big Bend",
            locality = null,
            subLocality = null,
            adminArea = null,
        )
        assertThat(label).isEqualTo("Big Bend")
    }

    @Test
    fun `returns null when nothing useful is available`() {
        val label = NearestRangeFinder.areaLabel(
            featureName = null,
            locality = null,
            subLocality = null,
            adminArea = "CA",
        )
        assertThat(label).isNull()
    }

    @Test
    fun `treats blank strings as absent`() {
        val label = NearestRangeFinder.areaLabel(
            featureName = " ",
            locality = "  ",
            subLocality = null,
            adminArea = null,
        )
        assertThat(label).isNull()
    }
}

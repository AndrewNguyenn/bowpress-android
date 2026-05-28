package com.andrewnguyen.bowpress.feature.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pure-Kotlin coverage for the C1 [FinishExtras] / [ShareOutcome] /
 * [FinishAudience] types — mirrors iOS `SessionViewModelTests`' ShareOutcome
 * partial-failure assertions and the FinishAudience copy rules.
 */
class FinishExtrasTest {

    @Test
    fun `audience public posts to feed and private does not`() {
        assertThat(FinishAudience.Public.shouldShare).isTrue()
        assertThat(FinishAudience.Private.shouldShare).isFalse()
    }

    @Test
    fun `audience labels and detail mirror iOS strings`() {
        assertThat(FinishAudience.Public.label).isEqualTo("Public")
        assertThat(FinishAudience.Public.detail).isEqualTo("Shared to feed")
        assertThat(FinishAudience.Public.primaryTitle).isEqualTo("Post to feed")
        assertThat(FinishAudience.Private.label).isEqualTo("Private")
        assertThat(FinishAudience.Private.detail).isEqualTo("Only in your log")
        assertThat(FinishAudience.Private.primaryTitle).isEqualTo("Finish")
    }

    // Note: partial-failure message tests for the share path live with the
    // owner of the message string — see core-data
    // SocialSessionSharerPartialFailureTest. The earlier ShareOutcome wrapper
    // was deleted along with the dead VM-side path.

    @Test
    fun `finish extras equality is by value not reference for photo bytes`() {
        val a = FinishExtras(
            title = "Morning",
            description = "shooting in the rain",
            audience = FinishAudience.Public,
            location = null,
            photoData = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5)),
        )
        val b = FinishExtras(
            title = "Morning",
            description = "shooting in the rain",
            audience = FinishAudience.Public,
            location = null,
            photoData = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5)),
        )
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `finish extras inequality flips when audience differs`() {
        val a = FinishExtras(
            title = "x",
            description = "",
            audience = FinishAudience.Public,
            location = null,
            photoData = emptyList(),
        )
        val b = a.copy(audience = FinishAudience.Private)
        assertThat(a).isNotEqualTo(b)
    }

    // Builds an Instant that lands on `hour` in the system default zone, so the
    // round-trip through `timeOfDaySuggestion` (which reads the local hour) is
    // deterministic regardless of where the test runs.
    private fun suggestionAtHour(hour: Int): String {
        val instant = LocalDateTime.of(2026, 5, 28, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        return timeOfDaySuggestion(instant)
    }

    @Test
    fun `time of day suggestion buckets mirror iOS`() {
        // 5..10 Morning, 11..13 Midday, 14..17 Afternoon, 18..21 Evening,
        // else Night — same boundaries as iOS FinishSheet.timeOfDaySuggestion.
        assertThat(suggestionAtHour(5)).isEqualTo("Morning shooting session")
        assertThat(suggestionAtHour(10)).isEqualTo("Morning shooting session")
        assertThat(suggestionAtHour(11)).isEqualTo("Midday shooting session")
        assertThat(suggestionAtHour(13)).isEqualTo("Midday shooting session")
        assertThat(suggestionAtHour(14)).isEqualTo("Afternoon shooting session")
        assertThat(suggestionAtHour(17)).isEqualTo("Afternoon shooting session")
        assertThat(suggestionAtHour(18)).isEqualTo("Evening shooting session")
        assertThat(suggestionAtHour(21)).isEqualTo("Evening shooting session")
        assertThat(suggestionAtHour(22)).isEqualTo("Night shooting session")
        assertThat(suggestionAtHour(4)).isEqualTo("Night shooting session")
        assertThat(suggestionAtHour(0)).isEqualTo("Night shooting session")
    }
}

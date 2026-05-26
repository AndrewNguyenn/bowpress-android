package com.andrewnguyen.bowpress.feature.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}

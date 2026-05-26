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

    @Test
    fun `clean share outcome reports no partial failure`() {
        val outcome = ShareOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = true,
            photosUploaded = 3,
            photosAttempted = 3,
        )
        assertThat(outcome.hasPartialFailure).isFalse()
        assertThat(outcome.partialFailureMessage).isNull()
    }

    @Test
    fun `description-only failure surfaces a description hint`() {
        val outcome = ShareOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = false,
            photosUploaded = 0,
            photosAttempted = 0,
        )
        assertThat(outcome.hasPartialFailure).isTrue()
        assertThat(outcome.partialFailureMessage)
            .isEqualTo("Posted, but your description didn't attach. Tap the post to add it.")
    }

    @Test
    fun `single photo failure with succeeding description surfaces a photo hint`() {
        val outcome = ShareOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = true,
            photosUploaded = 0,
            photosAttempted = 1,
        )
        assertThat(outcome.partialFailureMessage)
            .isEqualTo("Posted, but 1 photo didn't upload. Tap the post to retry.")
    }

    @Test
    fun `multi-photo failure with succeeding description pluralises`() {
        val outcome = ShareOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = true,
            photosUploaded = 1,
            photosAttempted = 3,
        )
        assertThat(outcome.partialFailureMessage)
            .isEqualTo("Posted, but 2 photos didn't upload. Tap the post to retry.")
    }

    @Test
    fun `combined description and photo failure surfaces a combined hint`() {
        val outcome = ShareOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = false,
            photosUploaded = 0,
            photosAttempted = 2,
        )
        assertThat(outcome.partialFailureMessage)
            .isEqualTo("Posted, but your description and 2 photos didn't attach. Tap the post to retry.")
    }

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

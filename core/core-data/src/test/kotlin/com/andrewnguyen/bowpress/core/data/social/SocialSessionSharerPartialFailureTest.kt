package com.andrewnguyen.bowpress.core.data.social

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pin the C1 partial-failure message strings — the snackbar surface the
 * archer sees in the MainScaffold consumes whichever string this helper
 * returns, so the wording is the contract.
 *
 * These assertions used to live next to a dead VM-side `ShareOutcome`
 * wrapper in feature-session; they were moved here when the partial-failure
 * path was collapsed onto the [AppSnackbarBus] route — same hint, one owner.
 */
class SocialSessionSharerPartialFailureTest {

    @Test
    fun `clean outcome reports no hint`() {
        val outcome = ShareWithExtrasOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = true,
            photosUploaded = 3,
            photosAttempted = 3,
        )
        assertThat(outcome.hasPartialFailure).isFalse()
        assertThat(SocialSessionSharer.partialFailureHint(outcome)).isNull()
    }

    @Test
    fun `description-only failure surfaces a description hint`() {
        val outcome = ShareWithExtrasOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = false,
            photosUploaded = 0,
            photosAttempted = 0,
        )
        assertThat(outcome.hasPartialFailure).isTrue()
        assertThat(SocialSessionSharer.partialFailureHint(outcome))
            .isEqualTo("Posted, but your description didn't attach. Tap the post to add it.")
    }

    @Test
    fun `single photo failure with succeeding description surfaces a photo hint`() {
        val outcome = ShareWithExtrasOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = true,
            photosUploaded = 0,
            photosAttempted = 1,
        )
        assertThat(SocialSessionSharer.partialFailureHint(outcome))
            .isEqualTo("Posted, but 1 photo didn't upload. Tap the post to retry.")
    }

    @Test
    fun `multi-photo failure with succeeding description pluralises`() {
        val outcome = ShareWithExtrasOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = true,
            photosUploaded = 1,
            photosAttempted = 3,
        )
        assertThat(SocialSessionSharer.partialFailureHint(outcome))
            .isEqualTo("Posted, but 2 photos didn't upload. Tap the post to retry.")
    }

    @Test
    fun `combined description and photo failure surfaces a combined hint`() {
        val outcome = ShareWithExtrasOutcome(
            sharedSessionId = "ss-1",
            descriptionSucceeded = false,
            photosUploaded = 0,
            photosAttempted = 2,
        )
        assertThat(SocialSessionSharer.partialFailureHint(outcome))
            .isEqualTo("Posted, but your description and 2 photos didn't attach. Tap the post to retry.")
    }
}

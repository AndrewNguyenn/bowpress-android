package com.andrewnguyen.bowpress.push

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-JVM tests for the deep-link URI produced by
 * [NotificationIntentBuilder.buildDeepLinkUriString]. We deliberately test the
 * string form (not the `android.net.Uri` wrapper) so these run without
 * Robolectric.
 */
class NotificationIntentBuilderTest {

    @Test
    fun `suggestion payload with bowId builds expected deep link`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf(
                "type" to "suggestion",
                "id" to "sug_42",
                "bowId" to "bow_7",
            ),
        )
        assertThat(uri).isEqualTo("bowpress://suggestion/sug_42?bowId=bow_7")
    }

    @Test
    fun `suggestion payload without bowId still builds a valid deep link`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "suggestion", "id" to "abc123"),
        )
        assertThat(uri).isEqualTo("bowpress://suggestion/abc123")
    }

    @Test
    fun `bowId is URL-encoded`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "suggestion", "id" to "sug_1", "bowId" to "a b"),
        )
        // java.net.URLEncoder encodes space as `+` (form-URL-encoded). That's
        // acceptable for a query param — the receiver URI-decodes it back.
        assertThat(uri).isEqualTo("bowpress://suggestion/sug_1?bowId=a+b")
    }

    @Test
    fun `missing id returns null so no notification tap target is built`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "suggestion"),
        )
        assertThat(uri).isNull()
    }

    @Test
    fun `unknown type returns null`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "marketing", "id" to "1"),
        )
        assertThat(uri).isNull()
    }

    @Test
    fun `missing type returns null`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(emptyMap())
        assertThat(uri).isNull()
    }
}

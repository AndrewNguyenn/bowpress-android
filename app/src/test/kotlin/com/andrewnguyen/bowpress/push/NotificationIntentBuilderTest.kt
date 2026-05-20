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

    // ── Social push types (§9) ───────────────────────────────────────────────

    @Test
    fun `friend_request deep links to social friends screen`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "friend_request"),
        )
        assertThat(uri).isEqualTo("bowpress://social/friends")
    }

    @Test
    fun `friend_pr deep links to social friends screen`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "friend_pr"),
        )
        assertThat(uri).isEqualTo("bowpress://social/friends")
    }

    @Test
    fun `league_deadline with leagueId deep links to that league`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "league_deadline", "leagueId" to "lg_42"),
        )
        assertThat(uri).isEqualTo("bowpress://social/leagues/lg_42")
    }

    @Test
    fun `league_deadline without leagueId deep links to leagues list`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "league_deadline"),
        )
        assertThat(uri).isEqualTo("bowpress://social/leagues")
    }

    @Test
    fun `club_activity with clubId deep links to that club`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "club_activity", "clubId" to "club_7"),
        )
        assertThat(uri).isEqualTo("bowpress://social/clubs/club_7")
    }

    @Test
    fun `club_activity without clubId deep links to social root`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "club_activity"),
        )
        assertThat(uri).isEqualTo("bowpress://social")
    }
}

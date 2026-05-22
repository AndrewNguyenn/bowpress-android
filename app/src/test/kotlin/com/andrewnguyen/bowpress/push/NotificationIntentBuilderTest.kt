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

    // ── Invitation / accepted push types (§13) ───────────────────────────────

    @Test
    fun `friend_accepted deep links to social friends screen`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "friend_accepted"),
        )
        assertThat(uri).isEqualTo("bowpress://social/friends")
    }

    @Test
    fun `club_invite deep links to the clubs screen`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "club_invite"),
        )
        assertThat(uri).isEqualTo("bowpress://social/clubs")
    }

    @Test
    fun `league_invite deep links to the leagues screen`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "league_invite"),
        )
        assertThat(uri).isEqualTo("bowpress://social/leagues")
    }

    @Test
    fun `invite push types are routed to the social channel`() {
        assertThat(NotificationIntentBuilder.SOCIAL_PUSH_TYPES)
            .containsAtLeast("club_invite", "league_invite", "friend_accepted")
    }

    @Test
    fun `friend_request club_invite and league_invite affect the badge`() {
        assertThat(NotificationIntentBuilder.BADGE_AFFECTING_PUSH_TYPES)
            .containsExactly("friend_request", "club_invite", "league_invite")
    }

    @Test
    fun `friend_accepted does not affect the badge count`() {
        // Accepting a request you sent doesn't add a pending item for you.
        assertThat(NotificationIntentBuilder.BADGE_AFFECTING_PUSH_TYPES)
            .doesNotContain("friend_accepted")
    }

    // ── Mention / reply push types (mentions contract §3.3) ──────────────────

    @Test
    fun `mention_post honours an explicit deepLink`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf(
                "type" to "mention_post",
                "deepLink" to "bowpress://social/sessions/ss_7",
            ),
        )
        assertThat(uri).isEqualTo("bowpress://social/sessions/ss_7")
    }

    @Test
    fun `mention_comment falls back to the subject session when no deepLink`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "mention_comment", "subjectId" to "ss_42"),
        )
        assertThat(uri).isEqualTo("bowpress://social/sessions/ss_42")
    }

    @Test
    fun `comment_reply with neither deepLink nor subjectId routes to the feed`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "comment_reply"),
        )
        assertThat(uri).isEqualTo("bowpress://social")
    }

    @Test
    fun `a blank deepLink is ignored in favour of the subjectId fallback`() {
        val uri = NotificationIntentBuilder.buildDeepLinkUriString(
            mapOf("type" to "mention_post", "deepLink" to "", "subjectId" to "ss_9"),
        )
        assertThat(uri).isEqualTo("bowpress://social/sessions/ss_9")
    }

    @Test
    fun `mention push types are routed to the social channel`() {
        assertThat(NotificationIntentBuilder.SOCIAL_PUSH_TYPES)
            .containsAtLeast("mention_post", "mention_comment", "comment_reply")
    }

    @Test
    fun `mention pushes do not affect the badge count`() {
        // Mentions fire push notifications only; there is no in-app inbox
        // badge in this scope (mentions contract §3.3).
        assertThat(NotificationIntentBuilder.BADGE_AFFECTING_PUSH_TYPES)
            .containsNoneOf("mention_post", "mention_comment", "comment_reply")
    }
}

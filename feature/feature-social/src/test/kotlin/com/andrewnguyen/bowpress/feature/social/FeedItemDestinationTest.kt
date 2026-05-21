package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySession
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedItemDestination
import com.andrewnguyen.bowpress.feature.social.ui.feed.feedItemDestination
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * The feed-row tap routing rule (precedence: session → league → club → actor).
 */
class FeedItemDestinationTest {

    private fun item(
        kind: ActivityKind = ActivityKind.friend_setup,
        sourceKind: ActivitySourceKind = ActivitySourceKind.friend,
        session: ActivitySession? = null,
        actorUserId: String = "u-actor",
        clubId: String? = null,
        leagueId: String? = null,
        isOwn: Boolean = false,
    ) = ActivityItem(
        id = "act-1",
        kind = kind,
        sourceKind = sourceKind,
        actorHandle = "marcus.t",
        actorDisplayName = "Marcus T",
        title = "Did a thing",
        createdAt = Instant.now(),
        session = session,
        actorUserId = actorUserId,
        clubId = clubId,
        leagueId = leagueId,
        isOwn = isOwn,
    )

    private fun sessionPayload() = ActivitySession(
        sharedSessionId = "ss-1",
        sessionId = "sess-1",
        score = 196,
        xCount = 9,
        arrowCount = 18,
    )

    @Test
    fun `a session row routes to the session detail`() {
        val dest = feedItemDestination(
            // session wins even when club + league are also set.
            item(session = sessionPayload(), clubId = "club-1", leagueId = "lg-1"),
        )
        assertThat(dest).isEqualTo(FeedItemDestination.Session("ss-1"))
    }

    @Test
    fun `a league row routes to the league home`() {
        val dest = feedItemDestination(
            // league wins over club when there's no session.
            item(leagueId = "lg-7", clubId = "club-1"),
        )
        assertThat(dest).isEqualTo(FeedItemDestination.League("lg-7"))
    }

    @Test
    fun `a club row routes to the club home`() {
        val dest = feedItemDestination(item(clubId = "club-9"))
        assertThat(dest).isEqualTo(FeedItemDestination.Club("club-9"))
    }

    @Test
    fun `a plain actor row routes to the actor profile`() {
        val dest = feedItemDestination(item(actorUserId = "u-42"))
        assertThat(dest).isEqualTo(FeedItemDestination.Actor("u-42"))
    }

    @Test
    fun `an older row with no routing fields still resolves to an actor destination`() {
        // actorUserId defaults to "" for a pre-routing-fields payload.
        val dest = feedItemDestination(item(actorUserId = ""))
        assertThat(dest).isEqualTo(FeedItemDestination.Actor(""))
    }

    // Social Feed V2 §2 — an own session row routes into owner-editable mode.

    @Test
    fun `an own session row carries isOwn to the session destination`() {
        val dest = feedItemDestination(item(session = sessionPayload(), isOwn = true))
        assertThat(dest).isEqualTo(FeedItemDestination.Session("ss-1", isOwn = true))
    }

    @Test
    fun `a friend's session row routes read-only`() {
        val dest = feedItemDestination(item(session = sessionPayload(), isOwn = false))
        assertThat(dest).isEqualTo(FeedItemDestination.Session("ss-1", isOwn = false))
    }
}

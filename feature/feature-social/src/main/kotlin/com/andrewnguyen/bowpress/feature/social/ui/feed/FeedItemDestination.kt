package com.andrewnguyen.bowpress.feature.social.ui.feed

import com.andrewnguyen.bowpress.core.model.ActivityItem

/**
 * Where a tapped feed row drills to. Every row routes somewhere.
 */
sealed interface FeedItemDestination {
    /**
     * A shared-session detail. [isOwn] is true when the signed-in caller owns
     * the session — the detail screen then opens in owner-editable mode
     * (Social Feed V2 §3/§4).
     */
    data class Session(
        val sharedSessionId: String,
        val isOwn: Boolean = false,
    ) : FeedItemDestination

    /** A league home screen. */
    data class League(val leagueId: String) : FeedItemDestination

    /** A club home screen. */
    data class Club(val clubId: String) : FeedItemDestination

    /** The acting archer's friend profile. */
    data class Actor(val actorUserId: String) : FeedItemDestination
}

/**
 * Resolve the tap destination for [item] by precedence:
 *  1. a shared session  → [FeedItemDestination.Session]
 *  2. else a league     → [FeedItemDestination.League]
 *  3. else a club       → [FeedItemDestination.Club]
 *  4. else the actor    → [FeedItemDestination.Actor]
 *
 * Extracted as a pure function so the routing is unit-testable without a
 * Compose harness — `FeedScreen` and the tests share this one rule.
 */
fun feedItemDestination(item: ActivityItem): FeedItemDestination {
    val session = item.session
    val leagueId = item.leagueId
    val clubId = item.clubId
    return when {
        session != null -> FeedItemDestination.Session(
            sharedSessionId = session.sharedSessionId,
            // §2 — an own row drills into the owner-editable detail.
            isOwn = item.isOwn,
        )
        leagueId != null -> FeedItemDestination.League(leagueId)
        clubId != null -> FeedItemDestination.Club(clubId)
        else -> FeedItemDestination.Actor(item.actorUserId)
    }
}

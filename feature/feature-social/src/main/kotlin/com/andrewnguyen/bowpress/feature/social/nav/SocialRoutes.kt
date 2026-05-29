package com.andrewnguyen.bowpress.feature.social.nav

/**
 * Route constants for the social feature graph. Nested under the top-level
 * `social` tab — `MainScaffold` maps the tab to [SocialRoutes.FEED].
 */
object SocialRoutes {
    // Landing
    const val FEED = "social/feed"

    // Notification center — opened from the bell in the feed top-nav.
    const val NOTIFICATION_CENTER = "social/notifications"

    // Monthly streak calendar — opened from the This-week card's "See more".
    const val STREAK_CALENDAR = "social/streak-calendar"

    // You (settings/profile)
    const val YOU = "social/you"
    const val YOU_PRIVACY = "social/you/privacy"
    const val YOU_BLOCKS = "social/you/blocks"

    // Friends
    const val FRIENDS = "social/friends"
    const val FRIEND_PROFILE = "social/friends/{otherUserId}/profile"
    const val FRIEND_COMPARE = "social/friends/{otherUserId}/compare"

    fun friendProfile(otherUserId: String) = "social/friends/$otherUserId/profile"
    fun friendCompare(otherUserId: String) = "social/friends/$otherUserId/compare"

    // Shared session detail (§16) — drilled into from a feed session row.
    // The `isOwn` query arg flags an own row → the screen opens in
    // owner-editable mode (Social Feed V2 §3/§4). Defaults false so an
    // external deep link without the arg still resolves to read-only.
    const val SESSION_DETAIL = "social/sessions/{sharedSessionId}?isOwn={isOwn}"

    fun sessionDetail(sharedSessionId: String, isOwn: Boolean = false) =
        "social/sessions/$sharedSessionId?isOwn=$isOwn"

    // Comments thread (Social Feed V2 §5) — opened from a feed row's or the
    // session detail's comment button. `subjectId` is the like/comment subject
    // (§5.1); `ownerUserId` is the subject owner, passed so the screen can gate
    // comment deletion (author OR post owner). `ownerUserId` defaults blank for
    // a deep link that lacks it — only the author delete check then applies.
    const val COMMENTS = "social/activity/{subjectId}/comments?ownerUserId={ownerUserId}"

    fun comments(subjectId: String, ownerUserId: String = "") =
        "social/activity/$subjectId/comments?ownerUserId=$ownerUserId"

    // Clubs
    const val CLUBS = "social/clubs"
    const val CLUB_HOME = "social/clubs/{clubId}"

    fun clubHome(clubId: String) = "social/clubs/$clubId"

    // Leagues
    const val LEAGUES = "social/leagues"
    const val LEAGUE_HOME = "social/leagues/{leagueId}"
    const val LEAGUE_CREATE = "social/leagues/create"
    const val LEAGUE_ADMIN = "social/leagues/{leagueId}/admin"

    fun leagueHome(leagueId: String) = "social/leagues/$leagueId"
    fun leagueAdmin(leagueId: String) = "social/leagues/$leagueId/admin"
}

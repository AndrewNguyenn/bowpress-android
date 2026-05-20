package com.andrewnguyen.bowpress.feature.social.nav

/**
 * Route constants for the social feature graph. Nested under the top-level
 * `social` tab — `MainScaffold` maps the tab to [SocialRoutes.FEED].
 */
object SocialRoutes {
    // Landing
    const val FEED = "social/feed"

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

    // Friend session detail (§16) — drilled into from a feed session row.
    const val SESSION_DETAIL = "social/sessions/{sharedSessionId}"

    fun sessionDetail(sharedSessionId: String) = "social/sessions/$sharedSessionId"

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

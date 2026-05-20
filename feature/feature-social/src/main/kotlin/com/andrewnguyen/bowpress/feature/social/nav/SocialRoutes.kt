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

    // Friends
    const val FRIENDS = "social/friends"
    const val FRIEND_SEARCH = "social/friends/search"
    const val FRIEND_PROFILE = "social/friends/{otherUserId}/profile"
    const val FRIEND_COMPARE = "social/friends/{otherUserId}/compare"

    fun friendProfile(otherUserId: String) = "social/friends/$otherUserId/profile"
    fun friendCompare(otherUserId: String) = "social/friends/$otherUserId/compare"

    // Clubs
    const val CLUBS = "social/clubs"
    const val CLUB_HOME = "social/clubs/{clubId}"
    const val CLUB_CREATE = "social/clubs/create"

    fun clubHome(clubId: String) = "social/clubs/$clubId"

    // Leagues
    const val LEAGUES = "social/leagues"
    const val LEAGUE_HOME = "social/leagues/{leagueId}"
    const val LEAGUE_CREATE = "social/leagues/create"
    const val LEAGUE_ADMIN = "social/leagues/{leagueId}/admin"

    fun leagueHome(leagueId: String) = "social/leagues/$leagueId"
    fun leagueAdmin(leagueId: String) = "social/leagues/$leagueId/admin"
}

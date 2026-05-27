package com.andrewnguyen.bowpress.feature.social.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.andrewnguyen.bowpress.core.model.SocialNotification
import com.andrewnguyen.bowpress.feature.social.ui.blocks.BlocksScreen
import com.andrewnguyen.bowpress.feature.social.ui.clubs.ClubHomeScreen
import com.andrewnguyen.bowpress.feature.social.ui.clubs.ClubsScreen
import com.andrewnguyen.bowpress.feature.social.ui.comments.CommentsScreen
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedScreen
import com.andrewnguyen.bowpress.feature.social.ui.friends.CompareScreen
import com.andrewnguyen.bowpress.feature.social.ui.friends.FriendProfileScreen
import com.andrewnguyen.bowpress.feature.social.ui.friends.FriendsScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeagueAdminScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeagueComposerScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeagueHomeScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeaguesScreen
import com.andrewnguyen.bowpress.feature.social.ui.notifications.NotificationCenterScreen
import com.andrewnguyen.bowpress.feature.social.ui.privacy.PrivacyScreen
import com.andrewnguyen.bowpress.feature.social.ui.session.FriendSessionDetailScreen
import com.andrewnguyen.bowpress.feature.social.ui.you.YouScreen

/**
 * Install the social feature's full route set into the root [NavGraphBuilder].
 *
 * [onSignedOut] is forwarded from the host — the You screen offers a sign-out
 * action that the app layer translates into routing back to auth.
 *
 * [onAccountClick], [onSubscriptionClick], [onEquipmentClick] let the You
 * screen deep-link into other feature tabs without the social feature knowing
 * their routes directly.
 */
fun NavGraphBuilder.socialNavGraph(
    navController: NavController,
    onSignedOut: () -> Unit,
    onAccountClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onEquipmentClick: () -> Unit,
    onEditProfileClick: () -> Unit,
) {
    // ── Feed (landing) ────────────────────────────────────────────────────────

    composable(
        route = SocialRoutes.FEED,
        deepLinks = listOf(navDeepLink { uriPattern = "bowpress://social" }),
    ) {
        FeedScreen(
            onAvatarClick = { navController.navigate(SocialRoutes.YOU) },
            onFriendsClick = { navController.navigate(SocialRoutes.FRIENDS) },
            onClubsIndexClick = { navController.navigate(SocialRoutes.CLUBS) },
            onLeaguesIndexClick = { navController.navigate(SocialRoutes.LEAGUES) },
            onClubClick = { clubId -> navController.navigate(SocialRoutes.clubHome(clubId)) },
            onLeagueClick = { leagueId -> navController.navigate(SocialRoutes.leagueHome(leagueId)) },
            onSessionClick = { sharedSessionId, isOwn ->
                navController.navigate(SocialRoutes.sessionDetail(sharedSessionId, isOwn))
            },
            onActorClick = { actorUserId ->
                // A blank actorUserId (older API) has nowhere to drill — skip.
                if (actorUserId.isNotBlank()) {
                    navController.navigate(SocialRoutes.friendProfile(actorUserId))
                }
            },
            onCommentsClick = { subjectId, ownerUserId ->
                navController.navigate(SocialRoutes.comments(subjectId, ownerUserId))
            },
            onBellClick = { navController.navigate(SocialRoutes.NOTIFICATION_CENTER) },
        )
    }

    // ── Notification center (§13) ──────────────────────────────────────────────

    composable(SocialRoutes.NOTIFICATION_CENTER) {
        NotificationCenterScreen(
            onBack = { navController.popBackStack() },
            onItemClick = { notification ->
                val route = notificationRoute(notification)
                if (route != null) navController.navigate(route)
            },
        )
    }

    // ── You / Privacy ─────────────────────────────────────────────────────────

    composable(SocialRoutes.YOU) {
        YouScreen(
            onBack = { navController.popBackStack() },
            onPrivacyClick = { navController.navigate(SocialRoutes.YOU_PRIVACY) },
            onSignOut = onSignedOut,
            onAccountClick = onAccountClick,
            onSubscriptionClick = onSubscriptionClick,
            onEquipmentClick = onEquipmentClick,
            onEditProfileClick = onEditProfileClick,
        )
    }

    composable(SocialRoutes.YOU_PRIVACY) {
        PrivacyScreen(
            onBack = { navController.popBackStack() },
            onManageBlocksClick = { navController.navigate(SocialRoutes.YOU_BLOCKS) },
        )
    }

    composable(SocialRoutes.YOU_BLOCKS) {
        BlocksScreen(
            onBack = { navController.popBackStack() },
        )
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    composable(
        route = SocialRoutes.FRIENDS,
        deepLinks = listOf(navDeepLink { uriPattern = "bowpress://social/friends" }),
    ) {
        FriendsScreen(
            onBack = { navController.popBackStack() },
            onFriendClick = { userId ->
                navController.navigate(SocialRoutes.friendProfile(userId))
            },
        )
    }

    composable(
        route = SocialRoutes.FRIEND_PROFILE,
        arguments = listOf(navArgument("otherUserId") { type = NavType.StringType }),
    ) { entry ->
        val otherUserId = entry.arguments?.getString("otherUserId").orEmpty()
        FriendProfileScreen(
            otherUserId = otherUserId,
            onBack = { navController.popBackStack() },
            onCompare = { userId -> navController.navigate(SocialRoutes.friendCompare(userId)) },
        )
    }

    composable(
        route = SocialRoutes.FRIEND_COMPARE,
        arguments = listOf(navArgument("otherUserId") { type = NavType.StringType }),
    ) { entry ->
        val otherUserId = entry.arguments?.getString("otherUserId").orEmpty()
        CompareScreen(
            otherUserId = otherUserId,
            onBack = { navController.popBackStack() },
        )
    }

    // ── Shared session detail (§16 + Social Feed V2 §3/§4) ──────────────────────

    composable(
        route = SocialRoutes.SESSION_DETAIL,
        arguments = listOf(
            navArgument("sharedSessionId") { type = NavType.StringType },
            navArgument("isOwn") {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
        // Mentions contract §3.3 — a `mention_post` / `mention_comment` /
        // `comment_reply` push deep-links to the shared session here.
        deepLinks = listOf(
            navDeepLink { uriPattern = "bowpress://social/sessions/{sharedSessionId}" },
        ),
    ) { entry ->
        val sharedSessionId = entry.arguments?.getString("sharedSessionId").orEmpty()
        val isOwn = entry.arguments?.getBoolean("isOwn") ?: false
        FriendSessionDetailScreen(
            sharedSessionId = sharedSessionId,
            isOwn = isOwn,
            onBack = { navController.popBackStack() },
            onCommentsClick = { subjectId, ownerUserId ->
                navController.navigate(SocialRoutes.comments(subjectId, ownerUserId))
            },
            // Mentions §3.2 — a tapped `@handle` in the description resolves
            // to that archer's profile.
            onOpenArcher = { userId ->
                if (userId.isNotBlank()) {
                    navController.navigate(SocialRoutes.friendProfile(userId))
                }
            },
        )
    }

    // ── Comments thread (Social Feed V2 §5) ─────────────────────────────────────

    composable(
        route = SocialRoutes.COMMENTS,
        arguments = listOf(
            navArgument("subjectId") { type = NavType.StringType },
            navArgument("ownerUserId") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { entry ->
        val subjectId = entry.arguments?.getString("subjectId").orEmpty()
        val ownerUserId = entry.arguments?.getString("ownerUserId").orEmpty()
        CommentsScreen(
            subjectId = subjectId,
            subjectOwnerUserId = ownerUserId.takeIf { it.isNotBlank() },
            onBack = { navController.popBackStack() },
            // Mentions §3.2 — a tapped `@handle` resolves to an archer profile.
            onOpenArcher = { userId ->
                if (userId.isNotBlank()) {
                    navController.navigate(SocialRoutes.friendProfile(userId))
                }
            },
        )
    }

    // ── Clubs ─────────────────────────────────────────────────────────────────

    composable(
        route = SocialRoutes.CLUBS,
        deepLinks = listOf(navDeepLink { uriPattern = "bowpress://social/clubs" }),
    ) {
        ClubsScreen(
            onBack = { navController.popBackStack() },
            onClubClick = { clubId -> navController.navigate(SocialRoutes.clubHome(clubId)) },
        )
    }

    composable(
        route = SocialRoutes.CLUB_HOME,
        arguments = listOf(navArgument("clubId") { type = NavType.StringType }),
        deepLinks = listOf(navDeepLink { uriPattern = "bowpress://social/clubs/{clubId}" }),
    ) { entry ->
        val clubId = entry.arguments?.getString("clubId").orEmpty()
        ClubHomeScreen(
            clubId = clubId,
            onBack = { navController.popBackStack() },
            // Parity E2 — tap a leaderboard row to drill into the archer.
            onOpenArcher = { userId ->
                navController.navigate(SocialRoutes.friendProfile(userId))
            },
            // Parity E10 — tap a member-activity row to drill into the session.
            onOpenSession = { sharedSessionId ->
                navController.navigate(SocialRoutes.sessionDetail(sharedSessionId))
            },
        )
    }

    // ── Leagues ───────────────────────────────────────────────────────────────

    composable(
        route = SocialRoutes.LEAGUES,
        deepLinks = listOf(navDeepLink { uriPattern = "bowpress://social/leagues" }),
    ) {
        LeaguesScreen(
            onBack = { navController.popBackStack() },
            onLeagueClick = { leagueId -> navController.navigate(SocialRoutes.leagueHome(leagueId)) },
            onCreateClick = { navController.navigate(SocialRoutes.LEAGUE_CREATE) },
        )
    }

    composable(
        route = SocialRoutes.LEAGUE_HOME,
        arguments = listOf(navArgument("leagueId") { type = NavType.StringType }),
        deepLinks = listOf(navDeepLink { uriPattern = "bowpress://social/leagues/{leagueId}" }),
    ) { entry ->
        val leagueId = entry.arguments?.getString("leagueId").orEmpty()
        LeagueHomeScreen(
            leagueId = leagueId,
            onBack = { navController.popBackStack() },
            onAdminClick = { id -> navController.navigate(SocialRoutes.leagueAdmin(id)) },
            // Parity E2 — tap a standings row to drill into the archer.
            onOpenArcher = { userId ->
                navController.navigate(SocialRoutes.friendProfile(userId))
            },
        )
    }

    composable(SocialRoutes.LEAGUE_CREATE) {
        LeagueComposerScreen(
            onBack = { navController.popBackStack() },
            onCreated = { leagueId ->
                navController.navigate(SocialRoutes.leagueHome(leagueId)) {
                    popUpTo(SocialRoutes.LEAGUE_CREATE) { inclusive = true }
                }
            },
        )
    }

    composable(
        route = SocialRoutes.LEAGUE_ADMIN,
        arguments = listOf(navArgument("leagueId") { type = NavType.StringType }),
    ) { entry ->
        val leagueId = entry.arguments?.getString("leagueId").orEmpty()
        LeagueAdminScreen(
            leagueId = leagueId,
            onBack = { navController.popBackStack() },
        )
    }
}

/**
 * The destination a tapped notification drills into, or null when it carries
 * no usable target (e.g. a friend_request, whose row is its own affordance).
 */
private fun notificationRoute(n: SocialNotification): String? = when (n.type) {
    "like", "comment", "comment_reply", "mention_post", "mention_comment", "friend_pr" ->
        n.subjectId?.let { SocialRoutes.sessionDetail(it) }
    "friend_request", "friend_accepted" -> SocialRoutes.FRIENDS
    "club_invite", "club_announcement" ->
        n.subjectId?.let { SocialRoutes.clubHome(it) } ?: SocialRoutes.CLUBS
    "league_invite", "league_deadline", "league_event" ->
        n.subjectId?.let { SocialRoutes.leagueHome(it) } ?: SocialRoutes.LEAGUES
    else -> null
}

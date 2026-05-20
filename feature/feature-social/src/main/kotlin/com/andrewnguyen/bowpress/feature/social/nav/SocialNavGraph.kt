package com.andrewnguyen.bowpress.feature.social.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.andrewnguyen.bowpress.feature.social.ui.blocks.BlocksScreen
import com.andrewnguyen.bowpress.feature.social.ui.clubs.ClubHomeScreen
import com.andrewnguyen.bowpress.feature.social.ui.clubs.ClubsScreen
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedScreen
import com.andrewnguyen.bowpress.feature.social.ui.friends.CompareScreen
import com.andrewnguyen.bowpress.feature.social.ui.friends.FriendProfileScreen
import com.andrewnguyen.bowpress.feature.social.ui.friends.FriendsScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeagueAdminScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeagueComposerScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeagueHomeScreen
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeaguesScreen
import com.andrewnguyen.bowpress.feature.social.ui.privacy.PrivacyScreen
import com.andrewnguyen.bowpress.feature.social.ui.session.FriendSessionDetailScreen
import com.andrewnguyen.bowpress.feature.social.ui.you.YouScreen

/**
 * Install the social feature's full route set into the root [NavGraphBuilder].
 *
 * [onSignedOut] is forwarded from the host — the You screen offers a sign-out
 * action that the app layer translates into routing back to auth.
 *
 * [onAccountClick], [onSubscriptionClick], [onEquipmentClick],
 * [onNotificationsClick] let the You screen deep-link into other feature tabs
 * without the social feature knowing their routes directly.
 */
fun NavGraphBuilder.socialNavGraph(
    navController: NavController,
    onSignedOut: () -> Unit,
    onAccountClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onEquipmentClick: () -> Unit,
    onNotificationsClick: () -> Unit,
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
            onSessionClick = { sharedSessionId ->
                navController.navigate(SocialRoutes.sessionDetail(sharedSessionId))
            },
            onActorClick = { actorUserId ->
                // A blank actorUserId (older API) has nowhere to drill — skip.
                if (actorUserId.isNotBlank()) {
                    navController.navigate(SocialRoutes.friendProfile(actorUserId))
                }
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
            onNotificationsClick = onNotificationsClick,
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

    // ── Friend session detail (§16) ─────────────────────────────────────────────

    composable(
        route = SocialRoutes.SESSION_DETAIL,
        arguments = listOf(navArgument("sharedSessionId") { type = NavType.StringType }),
    ) { entry ->
        val sharedSessionId = entry.arguments?.getString("sharedSessionId").orEmpty()
        FriendSessionDetailScreen(
            sharedSessionId = sharedSessionId,
            onBack = { navController.popBackStack() },
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

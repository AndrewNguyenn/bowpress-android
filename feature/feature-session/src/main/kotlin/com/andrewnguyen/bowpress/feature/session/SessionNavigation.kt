package com.andrewnguyen.bowpress.feature.session

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Adds the session flow to the parent nav graph. Routes:
 *   - `session/home` — start screen (bow + arrow picker).
 *   - `session/active/{sessionId}` — active session with target plot.
 *
 * Designed to slot into the top-level `session` destination defined in
 * `core:core-navigation` / `TopLevelDestination.Session`.
 */
object SessionRoutes {
    const val HOME = "session/home"
    const val ACTIVE = "session/active"
    const val ACTIVE_ROUTE = "$ACTIVE/{sessionId}"
    fun active(sessionId: String) = "$ACTIVE/$sessionId"
}

fun NavGraphBuilder.sessionNavGraph(navController: NavController) {
    composable(SessionRoutes.HOME) {
        SessionHomeScreen(
            onSessionStarted = { sessionId ->
                navController.navigate(SessionRoutes.active(sessionId)) {
                    popUpTo(SessionRoutes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
    composable(
        route = SessionRoutes.ACTIVE_ROUTE,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
    ) { backStack ->
        val sessionId = backStack.arguments?.getString("sessionId").orEmpty()
        ActiveSessionScreen(
            sessionId = sessionId,
            onSessionEnded = {
                navController.navigate(SessionRoutes.HOME) {
                    popUpTo(SessionRoutes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
}

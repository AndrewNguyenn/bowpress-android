package com.andrewnguyen.bowpress.feature.session

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.feature.session.threed.ThreeDCourseScreen

/**
 * Adds the session flow to the parent nav graph. Routes:
 *   - `session/home` — start screen (bow + arrow picker).
 *   - `session/active/{sessionId}` — active range session with target plot.
 *   - `session/course/{sessionId}` — active 3D-course session.
 *
 * Designed to slot into the top-level `session` destination defined in
 * `core:core-navigation` / `TopLevelDestination.Session`.
 */
object SessionRoutes {
    const val HOME = "session/home"
    const val ACTIVE = "session/active"
    const val ACTIVE_ROUTE = "$ACTIVE/{sessionId}"
    const val COURSE = "session/course"
    const val COURSE_ROUTE = "$COURSE/{sessionId}"
    fun active(sessionId: String) = "$ACTIVE/$sessionId"
    fun course(sessionId: String) = "$COURSE/$sessionId"
}

fun NavGraphBuilder.sessionNavGraph(navController: NavController) {
    composable(SessionRoutes.HOME) {
        SessionHomeScreen(
            onSessionStarted = { sessionId, type ->
                val route = if (type == SessionType.THREE_D_COURSE) {
                    SessionRoutes.course(sessionId)
                } else {
                    SessionRoutes.active(sessionId)
                }
                navController.navigate(route) {
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
    composable(
        route = SessionRoutes.COURSE_ROUTE,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
    ) {
        ThreeDCourseScreen(
            onCourseEnded = {
                navController.navigate(SessionRoutes.HOME) {
                    popUpTo(SessionRoutes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
}

package com.andrewnguyen.bowpress.feature.session

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
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

/**
 * Top-level Social tab graph route. Duplicated as a string constant rather
 * than imported from `MainScaffold.TopTab.Social` — that enum is `private`
 * and lives in the `app` module, which the session feature cannot depend
 * on without inverting the module graph. Kept in lockstep with MainScaffold
 * via the iOS-parity convention (the deep-link contract `bowpress://social/...`
 * also resolves here, so this route is effectively part of the public surface).
 */
private const val TAB_SOCIAL = "tab/social"

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
            onSessionEnded = { wasShared ->
                navigateAfterFinish(navController, wasShared)
            },
        )
    }
    composable(
        route = SessionRoutes.COURSE_ROUTE,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
    ) {
        ThreeDCourseScreen(
            onCourseEnded = { wasShared ->
                navigateAfterFinish(navController, wasShared)
            },
        )
    }
}

/**
 * After a session is finalized, route Public finishes to the Social feed so
 * the archer can see their post; everything else (Private, discard, legacy
 * nil-extras path, external null transitions) falls through to the Session
 * tab's home — same destination the screen used before this branch existed.
 *
 * The Social-tab navigate uses the same `popUpTo(startDestination, saveState)
 * / launchSingleTop / restoreState` triple as the bottom-nav handler in
 * `MainScaffold` — keeps the back stack from accumulating one Active /
 * Course entry per finish, and restores the Social tab's nested state when
 * the archer eventually taps back into the Session tab.
 */
private fun navigateAfterFinish(navController: NavController, wasShared: Boolean) {
    if (wasShared) {
        navController.navigate(TAB_SOCIAL) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navController.navigate(SessionRoutes.HOME) {
            popUpTo(SessionRoutes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }
}

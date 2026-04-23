package com.andrewnguyen.bowpress.feature.analytics.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.andrewnguyen.bowpress.feature.analytics.dashboard.AnalyticsDashboardScreen
import com.andrewnguyen.bowpress.feature.analytics.history.HistoricalSessionsScreen
import com.andrewnguyen.bowpress.feature.analytics.suggestion.SuggestionDetailScreen
import com.andrewnguyen.bowpress.feature.analytics.timeline.ScoreTimelineScreen

/**
 * Route constants for the analytics feature graph. Any external module (the host app)
 * should navigate using these rather than hand-typing strings.
 */
object AnalyticsRoutes {
    /** Entry point of the analytics graph (parent route for nested graph). */
    const val Graph: String = "analytics_graph"

    const val Dashboard: String = "analytics/dashboard"
    const val History: String = "analytics/history"

    /** Pattern: `analytics/suggestion/{bowId}/{suggestionId}`. */
    const val SuggestionDetailPattern: String = "analytics/suggestion/{bowId}/{suggestionId}"

    /** Pattern: `analytics/timeline/{bowId}`. */
    const val TimelinePattern: String = "analytics/timeline/{bowId}"

    fun suggestionDetail(bowId: String, suggestionId: String): String =
        "analytics/suggestion/$bowId/$suggestionId"

    fun timeline(bowId: String): String = "analytics/timeline/$bowId"

    /** Nav-arg keys — used by ViewModels to pluck values from `SavedStateHandle`. */
    object Args {
        const val BowId: String = "bowId"
        const val SuggestionId: String = "suggestionId"
    }
}

/**
 * Registers the analytics feature graph on [navController]'s NavHost.
 *
 * Also wires the deep-link contract the platform layer relies on:
 *   `bowpress://suggestion/{suggestionId}?bowId={bowId}` → [SuggestionDetailScreen].
 */
fun NavGraphBuilder.analyticsNavGraph(navController: NavController) {
    navigation(
        route = AnalyticsRoutes.Graph,
        startDestination = AnalyticsRoutes.Dashboard,
    ) {
        composable(AnalyticsRoutes.Dashboard) {
            AnalyticsDashboardScreen(
                onOpenSuggestion = { bowId, suggestionId ->
                    navController.navigate(
                        AnalyticsRoutes.suggestionDetail(bowId = bowId, suggestionId = suggestionId),
                    )
                },
                onOpenHistory = {
                    navController.navigate(AnalyticsRoutes.History)
                },
                onOpenTimeline = { bowId ->
                    navController.navigate(AnalyticsRoutes.timeline(bowId))
                },
            )
        }

        composable(
            route = AnalyticsRoutes.SuggestionDetailPattern,
            arguments = listOf(
                navArgument(AnalyticsRoutes.Args.BowId) { type = NavType.StringType },
                navArgument(AnalyticsRoutes.Args.SuggestionId) { type = NavType.StringType },
            ),
            deepLinks = listOf(
                // Spec: bowpress://suggestion/{id}?bowId={bowId}
                navDeepLink {
                    uriPattern = "bowpress://suggestion/{suggestionId}?bowId={bowId}"
                },
            ),
        ) {
            SuggestionDetailScreen(
                onBack = { navController.popBackStack() },
                onAppliedConfig = { bowId ->
                    // After a successful apply we surface the newly-created config in the
                    // timeline view; the host app can override this by providing a
                    // different callback wrapper.
                    navController.navigate(AnalyticsRoutes.timeline(bowId)) {
                        popUpTo(AnalyticsRoutes.Dashboard)
                    }
                },
            )
        }

        composable(AnalyticsRoutes.History) {
            HistoricalSessionsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = AnalyticsRoutes.TimelinePattern,
            arguments = listOf(
                navArgument(AnalyticsRoutes.Args.BowId) { type = NavType.StringType },
            ),
        ) {
            ScoreTimelineScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

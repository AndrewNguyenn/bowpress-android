package com.andrewnguyen.bowpress.feature.analytics.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.andrewnguyen.bowpress.core.model.TrendFinding
import com.andrewnguyen.bowpress.feature.analytics.dashboard.AnalyticsDashboardScreen
import com.andrewnguyen.bowpress.feature.analytics.history.HistoricalSessionsScreen
import com.andrewnguyen.bowpress.feature.analytics.sessiondetail.SessionDetailScreen
import com.andrewnguyen.bowpress.feature.analytics.suggestion.SuggestionDetailScreen
import com.andrewnguyen.bowpress.feature.analytics.timeline.ScoreTimelineScreen
import com.andrewnguyen.bowpress.feature.analytics.trend.TrendFindingDetailScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json

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

    /** Pattern: `analytics/session/{sessionId}`. */
    const val SessionDetailPattern: String = "analytics/session/{sessionId}"

    fun sessionDetail(sessionId: String): String = "analytics/session/$sessionId"

    /** Pattern: `analytics/trend/{findingJson}`. */
    const val TrendDetailPattern: String = "analytics/trend/{findingJson}"

    fun suggestionDetail(bowId: String, suggestionId: String): String =
        "analytics/suggestion/$bowId/$suggestionId"

    fun timeline(bowId: String): String = "analytics/timeline/$bowId"

    fun trendDetail(finding: TrendFinding): String {
        val json = Json.encodeToString(TrendFinding.serializer(), finding)
        val encoded = URLEncoder.encode(json, StandardCharsets.UTF_8.name())
        return "analytics/trend/$encoded"
    }

    /** Nav-arg keys — used by ViewModels to pluck values from `SavedStateHandle`. */
    object Args {
        const val BowId: String = "bowId"
        const val SuggestionId: String = "suggestionId"
        const val FindingJson: String = "findingJson"
        const val SessionId: String = "sessionId"
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
                onOpenTrendFinding = { finding ->
                    navController.navigate(AnalyticsRoutes.trendDetail(finding))
                },
            )
        }

        composable(
            route = AnalyticsRoutes.TrendDetailPattern,
            arguments = listOf(
                navArgument(AnalyticsRoutes.Args.FindingJson) { type = NavType.StringType },
            ),
        ) { entry ->
            val raw = entry.arguments?.getString(AnalyticsRoutes.Args.FindingJson).orEmpty()
            val decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
            val finding = Json.decodeFromString(TrendFinding.serializer(), decoded)
            TrendFindingDetailScreen(
                finding = finding,
                onBack = { navController.popBackStack() },
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
                onOpenSession = { sessionId ->
                    navController.navigate(AnalyticsRoutes.sessionDetail(sessionId))
                },
            )
        }

        composable(
            route = AnalyticsRoutes.SessionDetailPattern,
            arguments = listOf(
                navArgument(AnalyticsRoutes.Args.SessionId) { type = NavType.StringType },
            ),
        ) { entry ->
            val sessionId = entry.arguments?.getString(AnalyticsRoutes.Args.SessionId).orEmpty()
            SessionDetailScreen(
                sessionId = sessionId,
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

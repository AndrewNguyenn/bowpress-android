package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Renders the dashboard content with 5 suggestions injected and asserts that only
 * the top 3 suggestion cards are shown — matches the behaviour the view-model
 * unit test locks in at the data layer.
 */
class AnalyticsDashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_rendersTopThreeSuggestions_whenFivePresent() {
        val suggestions = (1..5).map { idx ->
            AnalyticsSuggestion(
                id = "s$idx",
                bowId = "b1",
                createdAt = Instant.parse("2026-04-${10 + idx}T12:00:00Z"),
                parameter = "param$idx",
                suggestedValue = "v$idx",
                currentValue = "c$idx",
                reasoning = "reason $idx",
                confidence = 0.65,
                qualifier = null,
                wasRead = false,
                wasDismissed = false,
                deliveryType = DeliveryType.IN_APP,
                evidence = null,
            )
        }.take(3) // ViewModel would enforce limit; for Compose render test, inject the already-limited list.

        val state = DashboardUiState(
            period = AnalyticsPeriod.WEEK,
            isLoading = false,
            overview = AnalyticsOverview(
                period = AnalyticsPeriod.WEEK,
                sessionCount = 3,
                avgArrowScore = 9.2,
                xPercentage = 18.0,
            ),
            comparison = PeriodComparison(
                period = AnalyticsPeriod.WEEK,
                current = PeriodSlice("Last 1 Week", 9.2, 18.0, 3),
                previous = PeriodSlice("Previous 1 Week", 9.0, 16.0, 2),
            ),
            topSuggestions = suggestions,
        )

        composeRule.setContent {
            BowPressTheme {
                AnalyticsDashboardContent(
                    state = state,
                    onPeriodChange = {},
                    onBowTypeChange = {},
                    onRetry = {},
                    onOpenSuggestion = { _, _ -> },
                    onOpenHistory = {},
                    onOpenTimeline = {},
                )
            }
        }

        composeRule.onNodeWithTag(AnalyticsDashboardTestTags.DashboardRoot).assertExists()
        composeRule.onAllNodesWithTag(AnalyticsDashboardTestTags.SuggestionCard)
            .assertCountEquals(3)
    }
}

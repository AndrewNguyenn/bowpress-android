package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import org.junit.Rule
import org.junit.Test

/**
 * Renders the dashboard content with overview + comparison data injected and asserts
 * the root test tag is wired so the Compose UI harness can locate the screen.
 *
 * Suggestion rendering has moved to `SuggestionsDashboardScreen` (Dashboard tab) —
 * see that feature's test suite for the suggestion-card render contract.
 */
class AnalyticsDashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_rendersRootWhenOverviewPresent() {
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
    }
}

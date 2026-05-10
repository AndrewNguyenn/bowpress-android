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
            period = AnalyticsPeriod.THREE_DAYS,
            isLoading = false,
            overview = AnalyticsOverview(
                period = AnalyticsPeriod.THREE_DAYS,
                sessionCount = 3,
                avgArrowScore = 9.2,
                xPercentage = 18.0,
            ),
            comparison = PeriodComparison(
                period = AnalyticsPeriod.THREE_DAYS,
                current = PeriodSlice(
                    label = "Last 3 Days",
                    avgArrowScore = 9.2,
                    xPercentage = 18.0,
                    sessionCount = 3,
                ),
                previous = PeriodSlice(
                    label = "Previous 3 Days",
                    avgArrowScore = 9.0,
                    xPercentage = 16.0,
                    sessionCount = 2,
                ),
            ),
        )

        composeRule.setContent {
            BowPressTheme {
                AnalyticsDashboardContent(
                    state = state,
                    onPeriodChange = {},
                    onBowTypeChange = {},
                    onDistanceChange = {},
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

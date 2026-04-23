package com.andrewnguyen.bowpress.feature.analytics.dashboard

import app.cash.turbine.test
import com.andrewnguyen.bowpress.core.data.repository.AnalyticsRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [AnalyticsDashboardViewModel].
 *
 * Verifies the core promise of the view model: given mock repositories emitting fixed
 * data, the first non-loading [DashboardUiState] contains the expected overview,
 * comparison, and *top 3* suggestions — regardless of how many the repository emits.
 */
class AnalyticsDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state exposes overview, comparison, and top 3 suggestions`() = runTest {
        val overview = AnalyticsOverview(
            period = AnalyticsPeriod.WEEK,
            sessionCount = 7,
            avgArrowScore = 9.4,
            xPercentage = 25.0,
        )
        val comparison = PeriodComparison(
            period = AnalyticsPeriod.WEEK,
            current = PeriodSlice("Last 1 Week", avgArrowScore = 9.4, xPercentage = 25.0, sessionCount = 7),
            previous = PeriodSlice("Previous 1 Week", avgArrowScore = 9.0, xPercentage = 20.0, sessionCount = 6),
        )
        val analyticsRepository = mockk<AnalyticsRepository>()
        coEvery { analyticsRepository.overview(AnalyticsPeriod.WEEK) } returns overview
        coEvery { analyticsRepository.comparison(AnalyticsPeriod.WEEK) } returns comparison

        // Five suggestions — ViewModel should surface the top 3 (undismissed, unread first).
        val suggestions = (1..5).map { idx ->
            AnalyticsSuggestion(
                id = "s$idx",
                bowId = "b1",
                createdAt = Instant.parse("2026-04-${10 + idx}T10:00:00Z"),
                parameter = "param$idx",
                suggestedValue = "v$idx-suggested",
                currentValue = "v$idx-current",
                reasoning = "reason $idx",
                confidence = 0.7,
                qualifier = null,
                wasRead = false,
                wasDismissed = false,
                deliveryType = DeliveryType.IN_APP,
                evidence = null,
            )
        }

        val suggestionRepository = mockk<SuggestionRepository>()
        every { suggestionRepository.observeAll() } returns MutableStateFlow(suggestions)

        val vm = AnalyticsDashboardViewModel(
            analyticsRepository = analyticsRepository,
            suggestionRepository = suggestionRepository,
        )

        vm.uiState.test {
            // Drain loading emissions until we see a populated, non-loading state.
            var state = awaitItem()
            while (state.isLoading || state.overview == null) {
                advanceUntilIdle()
                state = awaitItem()
            }

            assertThat(state.overview).isEqualTo(overview)
            assertThat(state.comparison).isEqualTo(comparison)
            assertThat(state.topSuggestions).hasSize(AnalyticsDashboardViewModel.TopSuggestionLimit)
            // Most-recent-first when all unread — s5 (14th) → s4 → s3.
            assertThat(state.topSuggestions.map { it.id }).containsExactly("s5", "s4", "s3").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissed suggestions are filtered out before the top-3 take`() = runTest {
        val analyticsRepository = mockk<AnalyticsRepository>()
        coEvery { analyticsRepository.overview(any()) } returns AnalyticsOverview(
            period = AnalyticsPeriod.WEEK,
            sessionCount = 1,
            avgArrowScore = 9.0,
            xPercentage = 10.0,
        )
        coEvery { analyticsRepository.comparison(any()) } returns PeriodComparison(
            period = AnalyticsPeriod.WEEK,
            current = PeriodSlice("a", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
            previous = PeriodSlice("b", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
        )

        val suggestions = listOf(
            sample("kept1", dismissed = false, read = false, createdAt = "2026-04-12T00:00:00Z"),
            sample("dismissed", dismissed = true, read = false, createdAt = "2026-04-13T00:00:00Z"),
            sample("kept2", dismissed = false, read = false, createdAt = "2026-04-14T00:00:00Z"),
        )
        val suggestionRepository = mockk<SuggestionRepository>()
        every { suggestionRepository.observeAll() } returns MutableStateFlow(suggestions)

        val vm = AnalyticsDashboardViewModel(analyticsRepository, suggestionRepository)

        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.overview == null) {
                advanceUntilIdle()
                state = awaitItem()
            }
            assertThat(state.topSuggestions.map { it.id })
                .containsExactly("kept2", "kept1").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sample(
        id: String,
        dismissed: Boolean,
        read: Boolean,
        createdAt: String,
    ): AnalyticsSuggestion = AnalyticsSuggestion(
        id = id,
        bowId = "b1",
        createdAt = Instant.parse(createdAt),
        parameter = "p",
        suggestedValue = "s",
        currentValue = "c",
        reasoning = "r",
        confidence = 0.5,
        qualifier = null,
        wasRead = read,
        wasDismissed = dismissed,
        deliveryType = DeliveryType.IN_APP,
        evidence = null,
    )
}

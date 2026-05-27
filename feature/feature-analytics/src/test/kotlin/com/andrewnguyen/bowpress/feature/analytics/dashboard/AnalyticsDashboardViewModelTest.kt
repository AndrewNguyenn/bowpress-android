package com.andrewnguyen.bowpress.feature.analytics.dashboard

import app.cash.turbine.test
import com.andrewnguyen.bowpress.core.data.analytics.LocalAnalyticsEngine
import com.andrewnguyen.bowpress.core.data.repository.AnalyticsRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
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
            period = AnalyticsPeriod.THREE_DAYS,
            sessionCount = 7,
            avgArrowScore = 9.4,
            xPercentage = 25.0,
        )
        val comparison = PeriodComparison(
            period = AnalyticsPeriod.THREE_DAYS,
            current = PeriodSlice("Last 3 Days", avgArrowScore = 9.4, xPercentage = 25.0, sessionCount = 7),
            previous = PeriodSlice("Previous 3 Days", avgArrowScore = 9.0, xPercentage = 20.0, sessionCount = 6),
        )
        val analyticsRepository = mockk<AnalyticsRepository>()
        coEvery { analyticsRepository.overview(AnalyticsPeriod.THREE_DAYS, null, null) } returns overview
        coEvery { analyticsRepository.comparison(AnalyticsPeriod.THREE_DAYS, null, null) } returns comparison

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

        val bowRepository = mockk<BowRepository>()
        every { bowRepository.observeBows() } returns MutableStateFlow(emptyList<Bow>())

        val sessionRepository = mockk<SessionRepository>()
        every { sessionRepository.observeCompleted() } returns MutableStateFlow(emptyList<ShootingSession>())

        val vm = AnalyticsDashboardViewModel(
            analyticsRepository = analyticsRepository,
            suggestionRepository = suggestionRepository,
            bowRepository = bowRepository,
            sessionRepository = sessionRepository,
            localEngine = stubLocalEngine(),
            analyticsRefreshBus = stubRefreshBus(),
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
        coEvery { analyticsRepository.overview(any(), any(), any()) } returns AnalyticsOverview(
            period = AnalyticsPeriod.THREE_DAYS,
            sessionCount = 1,
            avgArrowScore = 9.0,
            xPercentage = 10.0,
        )
        coEvery { analyticsRepository.comparison(any(), any(), any()) } returns PeriodComparison(
            period = AnalyticsPeriod.THREE_DAYS,
            current = PeriodSlice(label = "a", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
            previous = PeriodSlice(label = "b", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
        )

        val suggestions = listOf(
            sample("kept1", dismissed = false, read = false, createdAt = "2026-04-12T00:00:00Z"),
            sample("dismissed", dismissed = true, read = false, createdAt = "2026-04-13T00:00:00Z"),
            sample("kept2", dismissed = false, read = false, createdAt = "2026-04-14T00:00:00Z"),
        )
        val suggestionRepository = mockk<SuggestionRepository>()
        every { suggestionRepository.observeAll() } returns MutableStateFlow(suggestions)

        val bowRepository = mockk<BowRepository>()
        every { bowRepository.observeBows() } returns MutableStateFlow(emptyList<Bow>())

        val sessionRepository = mockk<SessionRepository>()
        every { sessionRepository.observeCompleted() } returns MutableStateFlow(emptyList<ShootingSession>())

        val vm = AnalyticsDashboardViewModel(
            analyticsRepository = analyticsRepository,
            suggestionRepository = suggestionRepository,
            bowRepository = bowRepository,
            sessionRepository = sessionRepository,
            localEngine = stubLocalEngine(),
            analyticsRefreshBus = stubRefreshBus(),
        )

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

    @Test
    fun `selectBowType triggers a re-fetch with the chosen style`() = runTest {
        val analyticsRepository = mockk<AnalyticsRepository>()
        // Two stubs — one for the initial null call, one for the BAREBOW call.
        coEvery { analyticsRepository.overview(AnalyticsPeriod.THREE_DAYS, null, null) } returns AnalyticsOverview(
            period = AnalyticsPeriod.THREE_DAYS, sessionCount = 9, avgArrowScore = 9.0, xPercentage = 0.0,
        )
        coEvery { analyticsRepository.comparison(AnalyticsPeriod.THREE_DAYS, null, null) } returns PeriodComparison(
            period = AnalyticsPeriod.THREE_DAYS,
            current = PeriodSlice(label = "a", avgArrowScore = 9.0, xPercentage = 0.0, sessionCount = 9),
            previous = PeriodSlice(label = "b", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
        )
        coEvery { analyticsRepository.overview(AnalyticsPeriod.THREE_DAYS, BowType.BAREBOW, null) } returns AnalyticsOverview(
            period = AnalyticsPeriod.THREE_DAYS, sessionCount = 3, avgArrowScore = 7.5, xPercentage = 5.0,
        )
        coEvery { analyticsRepository.comparison(AnalyticsPeriod.THREE_DAYS, BowType.BAREBOW, null) } returns PeriodComparison(
            period = AnalyticsPeriod.THREE_DAYS,
            current = PeriodSlice(label = "a", avgArrowScore = 7.5, xPercentage = 5.0, sessionCount = 3),
            previous = PeriodSlice(label = "b", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
        )

        val suggestionRepository = mockk<SuggestionRepository> {
            every { observeAll() } returns MutableStateFlow(emptyList())
        }
        val bowRepository = mockk<BowRepository> {
            every { observeBows() } returns MutableStateFlow(emptyList<Bow>())
        }
        val sessionRepository = mockk<SessionRepository> {
            every { observeCompleted() } returns MutableStateFlow(emptyList<ShootingSession>())
        }

        val vm = AnalyticsDashboardViewModel(
            analyticsRepository = analyticsRepository,
            suggestionRepository = suggestionRepository,
            bowRepository = bowRepository,
            sessionRepository = sessionRepository,
            localEngine = stubLocalEngine(),
            analyticsRefreshBus = stubRefreshBus(),
        )

        vm.uiState.test {
            // Wait for the initial unfiltered load.
            var state = awaitItem()
            while (state.isLoading || state.overview?.sessionCount != 9) {
                advanceUntilIdle()
                state = awaitItem()
            }
            assertThat(state.selectedBowType).isNull()

            // Switch to barebow → the VM should re-fetch and report sessionCount=3.
            vm.selectBowType(BowType.BAREBOW)
            advanceUntilIdle()
            state = awaitItem()
            while (state.isLoading || state.overview?.sessionCount != 3) {
                advanceUntilIdle()
                state = awaitItem()
            }
            assertThat(state.selectedBowType).isEqualTo(BowType.BAREBOW)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Returns a [LocalAnalyticsEngine] that reports 0 sessions — this forces the view-model
     * to fall through to the network repository, preserving the tests' pre-refactor
     * assertion that overview/comparison come from [AnalyticsRepository] stubs.
     */
    private fun stubLocalEngine(): LocalAnalyticsEngine {
        val engine = mockk<LocalAnalyticsEngine>()
        coEvery { engine.overview(any(), any()) } returns AnalyticsOverview(
            period = AnalyticsPeriod.THREE_DAYS,
            sessionCount = 0,
            avgArrowScore = 0.0,
            xPercentage = 0.0,
        )
        coEvery { engine.comparison(any(), any()) } returns PeriodComparison(
            period = AnalyticsPeriod.THREE_DAYS,
            current = PeriodSlice(label = "a", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
            previous = PeriodSlice(label = "b", avgArrowScore = 0.0, xPercentage = 0.0, sessionCount = 0),
        )
        coEvery { engine.multiSessionInsights() } returns emptyList()
        return engine
    }

    private fun stubRefreshBus(): AnalyticsRefreshBus = mockk {
        every { events } returns MutableSharedFlow()
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

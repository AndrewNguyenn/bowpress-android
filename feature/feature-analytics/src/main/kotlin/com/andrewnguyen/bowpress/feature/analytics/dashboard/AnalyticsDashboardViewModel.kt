package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.AnalyticsRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Number of undismissed/unread suggestions surfaced on the dashboard.
 * Matches the iOS dashboard's "top 3" contract.
 */
private const val DASHBOARD_SUGGESTION_LIMIT: Int = 3

/** UI state owned by [AnalyticsDashboardViewModel]. */
data class DashboardUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.WEEK,
    val isLoading: Boolean = true,
    val overview: AnalyticsOverview? = null,
    val comparison: PeriodComparison? = null,
    val topSuggestions: List<AnalyticsSuggestion> = emptyList(),
    val error: String? = null,
)

/**
 * Dashboard view model. Combines the network-backed [AnalyticsRepository] with the
 * Room-backed [SuggestionRepository] flow so the UI reactively shows updates as the
 * background sync mutates the suggestion cache.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val suggestionRepository: SuggestionRepository,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(AnalyticsPeriod.WEEK)

    /** Raising this counter triggers a re-fetch of overview + comparison. */
    private val refreshTrigger = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
    }

    fun selectPeriod(period: AnalyticsPeriod) {
        if (selectedPeriod.value == period) return
        selectedPeriod.value = period
    }

    fun refresh() {
        refreshTrigger.value = refreshTrigger.value + 1
    }

    private fun observeDashboard() {
        val analyticsFlow = combine(selectedPeriod, refreshTrigger) { period, _ -> period }
            .flatMapLatest { period ->
                flow {
                    emit(AnalyticsFetch.Loading(period))
                    try {
                        val overview = analyticsRepository.overview(period)
                        val comparison = analyticsRepository.comparison(period)
                        emit(AnalyticsFetch.Success(period, overview, comparison))
                    } catch (t: Throwable) {
                        emit(AnalyticsFetch.Failure(period, t.message ?: "Failed to load analytics"))
                    }
                }.flowOn(Dispatchers.IO)
            }

        val suggestionsFlow = suggestionRepository.observeAll()

        combine(analyticsFlow, suggestionsFlow) { fetch, suggestions ->
            val top = suggestions
                .asSequence()
                .filter { !it.wasDismissed }
                .sortedWith(
                    compareBy<AnalyticsSuggestion> { it.wasRead }
                        .thenByDescending { it.createdAt },
                )
                .take(DASHBOARD_SUGGESTION_LIMIT)
                .toList()
            when (fetch) {
                is AnalyticsFetch.Loading -> DashboardUiState(
                    period = fetch.period,
                    isLoading = true,
                    overview = _uiState.value.overview,
                    comparison = _uiState.value.comparison,
                    topSuggestions = top,
                    error = null,
                )
                is AnalyticsFetch.Success -> DashboardUiState(
                    period = fetch.period,
                    isLoading = false,
                    overview = fetch.overview,
                    comparison = fetch.comparison,
                    topSuggestions = top,
                    error = null,
                )
                is AnalyticsFetch.Failure -> DashboardUiState(
                    period = fetch.period,
                    isLoading = false,
                    overview = _uiState.value.overview,
                    comparison = _uiState.value.comparison,
                    topSuggestions = top,
                    error = fetch.message,
                )
            }
        }
            .onEach { _uiState.value = it }
            .catch { t -> _uiState.value = _uiState.value.copy(isLoading = false, error = t.message) }
            .launchIn(viewModelScope)
    }

    private sealed interface AnalyticsFetch {
        val period: AnalyticsPeriod

        data class Loading(override val period: AnalyticsPeriod) : AnalyticsFetch
        data class Success(
            override val period: AnalyticsPeriod,
            val overview: AnalyticsOverview,
            val comparison: PeriodComparison,
        ) : AnalyticsFetch
        data class Failure(override val period: AnalyticsPeriod, val message: String) : AnalyticsFetch
    }

    companion object {
        /** Exposed for tests. */
        const val TopSuggestionLimit: Int = DASHBOARD_SUGGESTION_LIMIT
    }
}

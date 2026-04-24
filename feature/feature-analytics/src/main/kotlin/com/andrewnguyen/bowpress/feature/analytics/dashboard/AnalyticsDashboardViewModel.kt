package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.AnalyticsRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Number of undismissed/unread suggestions surfaced on the dashboard.
 * Matches the iOS dashboard's "top 3" contract.
 */
private const val DASHBOARD_SUGGESTION_LIMIT: Int = 3

/** UI state owned by [AnalyticsDashboardViewModel]. */
data class DashboardUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.WEEK,
    /** `null` = "All bows". */
    val selectedBowType: BowType? = null,
    /** Bow styles the user actually owns; drives which filter chips are rendered. */
    val availableBowTypes: Set<BowType> = emptySet(),
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
    private val bowRepository: BowRepository,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(AnalyticsPeriod.WEEK)

    /** `null` = "All bows" — no `bowType` query param sent. */
    private val selectedBowType = MutableStateFlow<BowType?>(null)

    /** Raising this counter triggers a re-fetch of overview + comparison. */
    private val refreshTrigger = MutableStateFlow(0)

    private val availableBowTypes: StateFlow<Set<BowType>> =
        bowRepository.observeBows()
            .map { bows -> bows.map { it.bowType }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
    }

    fun selectPeriod(period: AnalyticsPeriod) {
        if (selectedPeriod.value == period) return
        selectedPeriod.value = period
    }

    fun selectBowType(type: BowType?) {
        if (selectedBowType.value == type) return
        selectedBowType.value = type
    }

    fun refresh() {
        refreshTrigger.value = refreshTrigger.value + 1
    }

    private fun observeDashboard() {
        val analyticsFlow = combine(selectedPeriod, selectedBowType, refreshTrigger) { period, bowType, _ ->
            period to bowType
        }
            .flatMapLatest { (period, bowType) ->
                flow {
                    emit(AnalyticsFetch.Loading(period, bowType))
                    try {
                        val overview = analyticsRepository.overview(period, bowType)
                        val comparison = analyticsRepository.comparison(period, bowType)
                        emit(AnalyticsFetch.Success(period, bowType, overview, comparison))
                    } catch (t: Throwable) {
                        emit(AnalyticsFetch.Failure(period, bowType, t.message ?: "Failed to load analytics"))
                    }
                }.flowOn(Dispatchers.IO)
            }

        val suggestionsFlow = suggestionRepository.observeAll()

        combine(analyticsFlow, suggestionsFlow, availableBowTypes) { fetch, suggestions, available ->
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
                    selectedBowType = fetch.bowType,
                    availableBowTypes = available,
                    isLoading = true,
                    overview = _uiState.value.overview,
                    comparison = _uiState.value.comparison,
                    topSuggestions = top,
                    error = null,
                )
                is AnalyticsFetch.Success -> DashboardUiState(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = available,
                    isLoading = false,
                    overview = fetch.overview,
                    comparison = fetch.comparison,
                    topSuggestions = top,
                    error = null,
                )
                is AnalyticsFetch.Failure -> DashboardUiState(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = available,
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
        val bowType: BowType?

        data class Loading(override val period: AnalyticsPeriod, override val bowType: BowType?) : AnalyticsFetch
        data class Success(
            override val period: AnalyticsPeriod,
            override val bowType: BowType?,
            val overview: AnalyticsOverview,
            val comparison: PeriodComparison,
        ) : AnalyticsFetch
        data class Failure(
            override val period: AnalyticsPeriod,
            override val bowType: BowType?,
            val message: String,
        ) : AnalyticsFetch
    }

    companion object {
        /** Exposed for tests. */
        const val TopSuggestionLimit: Int = DASHBOARD_SUGGESTION_LIMIT
    }
}

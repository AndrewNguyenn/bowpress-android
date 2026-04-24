package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.analytics.LocalAnalyticsEngine
import com.andrewnguyen.bowpress.core.data.repository.AnalyticsRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.ConfigurationChange
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.TagCorrelation
import com.andrewnguyen.bowpress.core.model.TrendInsight
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
import kotlinx.coroutines.launch
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
    val trendInsights: List<TrendInsight> = emptyList(),
    val configurationChanges: List<ConfigurationChange> = emptyList(),
    val tagCorrelations: List<TagCorrelation> = emptyList(),
    val isLoadingChanges: Boolean = false,
    val isLoadingCorrelations: Boolean = false,
    val error: String? = null,
)

/**
 * Dashboard view model. Computes overview/comparison/trend insights locally via
 * [LocalAnalyticsEngine], falling back to the network [AnalyticsRepository] when
 * the local datastore is empty (first-run, signed-in but not yet hydrated).
 * Per-bow change-impact and tag-correlation data still come from the server.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val suggestionRepository: SuggestionRepository,
    private val bowRepository: BowRepository,
    private val localEngine: LocalAnalyticsEngine,
    private val analyticsRefreshBus: AnalyticsRefreshBus,
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
        // External refresh signal — e.g. applied suggestion, FCM ping, completed sync.
        // Agent 5 (app-state plumbing) wires the `AppStateViewModel.analyticsRefreshNonce`
        // bump source into the same `AnalyticsRefreshBus`; we just observe the bus here.
        analyticsRefreshBus.events
            .onEach { refresh() }
            .launchIn(viewModelScope)
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
                        val overview = localOrRemoteOverview(period, bowType)
                        val comparison = localOrRemoteComparison(period, bowType)
                        val insights = runCatching { localEngine.multiSessionInsights() }.getOrDefault(emptyList())
                        emit(AnalyticsFetch.Success(period, bowType, overview, comparison, insights))
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
                is AnalyticsFetch.Loading -> _uiState.value.copy(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = available,
                    isLoading = true,
                    topSuggestions = top,
                    error = null,
                )
                is AnalyticsFetch.Success -> _uiState.value.copy(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = available,
                    isLoading = false,
                    overview = fetch.overview,
                    comparison = fetch.comparison,
                    trendInsights = fetch.trendInsights,
                    topSuggestions = top,
                    error = null,
                )
                is AnalyticsFetch.Failure -> _uiState.value.copy(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = available,
                    isLoading = false,
                    topSuggestions = top,
                    error = fetch.message,
                )
            }
        }
            .onEach {
                _uiState.value = it
                loadPerBowSections()
            }
            .catch { t -> _uiState.value = _uiState.value.copy(isLoading = false, error = t.message) }
            .launchIn(viewModelScope)
    }

    /** Try local first; fall back to remote when the local engine yields an empty window. */
    private suspend fun localOrRemoteOverview(
        period: AnalyticsPeriod,
        bowType: BowType?,
    ): AnalyticsOverview {
        val local = runCatching { localEngine.overview(period, bowType) }.getOrNull()
        return when {
            local != null && local.sessionCount > 0 -> local
            else -> runCatching { analyticsRepository.overview(period, bowType) }
                .getOrElse { local ?: throw it }
        }
    }

    private suspend fun localOrRemoteComparison(
        period: AnalyticsPeriod,
        bowType: BowType?,
    ): PeriodComparison {
        val local = runCatching { localEngine.comparison(period, bowType) }.getOrNull()
        return when {
            local != null && local.current.sessionCount + local.previous.sessionCount > 0 -> local
            else -> runCatching { analyticsRepository.comparison(period, bowType) }
                .getOrElse { local ?: throw it }
        }
    }

    /**
     * Kick off per-bow fetches (change impact, tag correlations) for the user's first bow.
     * Matches iOS behavior in `AnalyticsView` — if the user owns multiple bows the primary
     * (first) bow's data is surfaced; users pick per-bow views elsewhere.
     */
    private fun loadPerBowSections() {
        viewModelScope.launch {
            val firstBowId = runCatching { bowRepository.getBows().firstOrNull()?.id }.getOrNull()
                ?: return@launch
            _uiState.value = _uiState.value.copy(
                isLoadingChanges = true,
                isLoadingCorrelations = true,
            )
            val changes = runCatching { analyticsRepository.fetchConfigurationChanges(firstBowId) }
                .getOrDefault(emptyList())
            val correlations = runCatching { analyticsRepository.fetchTagCorrelations(firstBowId) }
                .getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                configurationChanges = changes,
                tagCorrelations = correlations,
                isLoadingChanges = false,
                isLoadingCorrelations = false,
            )
        }
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
            val trendInsights: List<TrendInsight>,
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

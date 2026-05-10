package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.analytics.LocalAnalyticsEngine
import com.andrewnguyen.bowpress.core.data.repository.AnalyticsRepository
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
import com.andrewnguyen.bowpress.feature.analytics.mock.AnalyticsFixtureDecorator
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.ConfigurationChange
import com.andrewnguyen.bowpress.core.model.DriftResponse
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TagCorrelation
import com.andrewnguyen.bowpress.core.model.TimelineResponse
import com.andrewnguyen.bowpress.core.model.TrendInsight
import com.andrewnguyen.bowpress.core.model.TrendsResponse
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
    /** `null` = "All distances". */
    val selectedDistance: ShootingDistance? = null,
    /** Distances that appear in the user's session history; hides the chip row otherwise. */
    val availableDistances: Set<ShootingDistance> = emptySet(),
    val isLoading: Boolean = true,
    val overview: AnalyticsOverview? = null,
    val comparison: PeriodComparison? = null,
    val topSuggestions: List<AnalyticsSuggestion> = emptyList(),
    /** All non-dismissed suggestions for the primary bow — feeds the full ledger section. */
    val allSuggestions: List<AnalyticsSuggestion> = emptyList(),
    val trendInsights: List<TrendInsight> = emptyList(),
    val configurationChanges: List<ConfigurationChange> = emptyList(),
    val tagCorrelations: List<TagCorrelation> = emptyList(),
    /** Wave 2 B — network-sourced sections. Nullable when a pre-Wave-2 server 404s. */
    val timeline: TimelineResponse? = null,
    val drift: DriftResponse? = null,
    val trends: TrendsResponse? = null,
    val isLoadingChanges: Boolean = false,
    val isLoadingCorrelations: Boolean = false,
    val error: String? = null,
)

/**
 * Dashboard view model. Computes overview/comparison/trend insights locally via
 * [LocalAnalyticsEngine], falling back to the network [AnalyticsRepository] when
 * the local datastore is empty (first-run, signed-in but not yet hydrated).
 * Per-bow change-impact and tag-correlation data still come from the server.
 *
 * Wave 2 B also fetches the three new Kenrokuen analytics endpoints — timeline,
 * trends, drift — on every refresh. Each fetch is wrapped in `runCatching` so a
 * 404 on an older backend degrades gracefully (the view hides that section when
 * the value is null).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val suggestionRepository: SuggestionRepository,
    private val bowRepository: BowRepository,
    private val sessionRepository: SessionRepository,
    private val arrowConfigRepository: ArrowConfigRepository,
    private val localEngine: LocalAnalyticsEngine,
    private val analyticsRefreshBus: AnalyticsRefreshBus,
    private val fixtureDecorator: AnalyticsFixtureDecorator,
) : ViewModel() {

    // Default period matches iOS AnalyticsView (`.threeDays`).
    private val selectedPeriod = MutableStateFlow(AnalyticsPeriod.THREE_DAYS)

    /** `null` = "All bows" — no `bowType` query param sent. */
    private val selectedBowType = MutableStateFlow<BowType?>(null)

    /** `null` = "All distances" — no `distance` query param sent. */
    private val selectedDistance = MutableStateFlow<ShootingDistance?>(null)

    /** Raising this counter triggers a re-fetch of overview + comparison. */
    private val refreshTrigger = MutableStateFlow(0)

    private val availableBowTypes: StateFlow<Set<BowType>> =
        bowRepository.observeBows()
            .map { bows -> bows.map { it.bowType }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val availableDistances: StateFlow<Set<ShootingDistance>> =
        sessionRepository.observeCompleted()
            .map { sessions -> sessions.mapNotNull { it.distance }.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // First bow / arrow drive the DEBUG dataset-summary fixture (MockAnalyticsWave2)
    // and could feed other "primary equipment" UI in the future. Eagerly cached so the
    // dashboard flow doesn't re-query Room on every period/bow/distance change.
    private val firstBow: StateFlow<com.andrewnguyen.bowpress.core.model.Bow?> =
        bowRepository.observeBows()
            .map { it.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val firstArrow: StateFlow<com.andrewnguyen.bowpress.core.model.ArrowConfiguration?> =
        arrowConfigRepository.observeAll()
            .map { it.firstOrNull() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    fun selectDistance(distance: ShootingDistance?) {
        if (selectedDistance.value == distance) return
        selectedDistance.value = distance
    }

    fun refresh() {
        refreshTrigger.value = refreshTrigger.value + 1
    }

    /** Called from the Kenrokuen ledger — optimistic mark-read when a row appears. */
    fun markSuggestionRead(id: String) {
        viewModelScope.launch { runCatching { suggestionRepository.markRead(id) } }
    }

    /** Called when the user swipes a suggestion off the ledger. */
    fun dismissSuggestion(id: String) {
        viewModelScope.launch { runCatching { suggestionRepository.dismiss(id) } }
    }

    private fun observeDashboard() {
        val analyticsFlow = combine(
            selectedPeriod,
            selectedBowType,
            selectedDistance,
            refreshTrigger,
        ) { period, bowType, distance, _ ->
            Triple(period, bowType, distance)
        }
            .flatMapLatest { (period, bowType, distance) ->
                flow {
                    emit(AnalyticsFetch.Loading(period, bowType, distance))
                    try {
                        val overview = if (distance == null) {
                            localOrRemoteOverview(period, bowType)
                        } else {
                            analyticsRepository.overview(period, bowType, distance)
                        }
                        val comparison = if (distance == null) {
                            localOrRemoteComparison(period, bowType)
                        } else {
                            analyticsRepository.comparison(period, bowType, distance)
                        }
                        val insights = runCatching { localEngine.multiSessionInsights() }.getOrDefault(emptyList())
                        // Wave 2 B endpoints — degrade gracefully when the server
                        // hasn't been rolled out yet.
                        val timeline = runCatching {
                            analyticsRepository.fetchTimeline(period, bowType, distance)
                        }.getOrNull()
                        val trends = runCatching {
                            analyticsRepository.fetchTrends(period, bowType, distance)
                        }.getOrNull()
                        // Wave-2 decoration: in DEBUG, FORCES headline numerals
                        // to the spec figure (10.4 / 72% / 5 sess), fills
                        // overview.sparkline so the Score timeline renders
                        // without a backend, and gives the CompareStrip
                        // non-zero "previous" values. Mirrors iOS
                        // `decorateOverviewWithMocks` /
                        // `decorateComparisonWithMocks`. Release builds get a
                        // NoOp decorator from AnalyticsFixtureModule and
                        // render whatever the server sent.
                        //
                        // WARNING: in DEBUG, this OVERRIDES real backend
                        // values — manual QA against a real backend will see
                        // 10.4 / 72% regardless of actual data. Same trap
                        // exists on iOS, by design for design-review parity.
                        // Build a release variant when QA-ing real data.
                        val decoratedOverview = fixtureDecorator.decorateOverview(
                            overview = overview,
                            firstBow = firstBow.value,
                            firstArrow = firstArrow.value,
                        )
                        val decoratedComparison = fixtureDecorator.decorateComparison(comparison)
                        val decoratedTimeline = timeline ?: fixtureDecorator.timelineFallback(period)
                        emit(
                            AnalyticsFetch.Success(
                                period = period,
                                bowType = bowType,
                                distance = distance,
                                overview = decoratedOverview,
                                comparison = decoratedComparison,
                                trendInsights = insights,
                                timeline = decoratedTimeline,
                                trends = trends,
                            ),
                        )
                    } catch (t: Throwable) {
                        emit(AnalyticsFetch.Failure(period, bowType, distance, t.message ?: "Failed to load analytics"))
                    }
                }.flowOn(Dispatchers.IO)
            }

        val suggestionsFlow = suggestionRepository.observeAll()

        combine(
            analyticsFlow,
            suggestionsFlow,
            availableBowTypes,
            availableDistances,
        ) { fetch, suggestions, availBows, availDistances ->
            val undismissed = suggestions.filter { !it.wasDismissed }
            val top = undismissed
                .asSequence()
                .sortedWith(
                    compareBy<AnalyticsSuggestion> { it.wasRead }
                        .thenByDescending { it.createdAt },
                )
                .take(DASHBOARD_SUGGESTION_LIMIT)
                .toList()
            val allRanked = undismissed
                .sortedWith(
                    compareBy<AnalyticsSuggestion> { it.wasApplied }
                        .thenByDescending { it.confidence },
                )
            when (fetch) {
                is AnalyticsFetch.Loading -> _uiState.value.copy(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = availBows,
                    selectedDistance = fetch.distance,
                    availableDistances = availDistances,
                    isLoading = true,
                    topSuggestions = top,
                    allSuggestions = allRanked,
                    error = null,
                )
                is AnalyticsFetch.Success -> _uiState.value.copy(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = availBows,
                    selectedDistance = fetch.distance,
                    availableDistances = availDistances,
                    isLoading = false,
                    overview = fetch.overview,
                    comparison = fetch.comparison,
                    trendInsights = fetch.trendInsights,
                    topSuggestions = top,
                    allSuggestions = allRanked,
                    timeline = fetch.timeline,
                    trends = fetch.trends,
                    error = null,
                )
                is AnalyticsFetch.Failure -> _uiState.value.copy(
                    period = fetch.period,
                    selectedBowType = fetch.bowType,
                    availableBowTypes = availBows,
                    selectedDistance = fetch.distance,
                    availableDistances = availDistances,
                    isLoading = false,
                    topSuggestions = top,
                    allSuggestions = allRanked,
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
     * Kick off per-bow fetches (change impact, tag correlations, drift) for the
     * user's first bow. Matches iOS behavior in `AnalyticsView` — if the user
     * owns multiple bows the primary (first) bow's data is surfaced; users pick
     * per-bow views elsewhere.
     */
    private fun loadPerBowSections() {
        viewModelScope.launch {
            val firstBowId = runCatching { bowRepository.getBows().firstOrNull()?.id }.getOrNull()
                ?: return@launch
            val period = selectedPeriod.value
            _uiState.value = _uiState.value.copy(
                isLoadingChanges = true,
                isLoadingCorrelations = true,
            )
            val changes = runCatching { analyticsRepository.fetchConfigurationChanges(firstBowId) }
                .getOrDefault(emptyList())
            val correlations = runCatching { analyticsRepository.fetchTagCorrelations(firstBowId) }
                .getOrDefault(emptyList())
            val drift = runCatching { analyticsRepository.fetchDrift(firstBowId, period) }
                .getOrNull()
            _uiState.value = _uiState.value.copy(
                configurationChanges = changes,
                tagCorrelations = correlations,
                drift = drift,
                isLoadingChanges = false,
                isLoadingCorrelations = false,
            )
        }
    }

    private sealed interface AnalyticsFetch {
        val period: AnalyticsPeriod
        val bowType: BowType?
        val distance: ShootingDistance?

        data class Loading(
            override val period: AnalyticsPeriod,
            override val bowType: BowType?,
            override val distance: ShootingDistance?,
        ) : AnalyticsFetch
        data class Success(
            override val period: AnalyticsPeriod,
            override val bowType: BowType?,
            override val distance: ShootingDistance?,
            val overview: AnalyticsOverview,
            val comparison: PeriodComparison,
            val trendInsights: List<TrendInsight>,
            val timeline: TimelineResponse?,
            val trends: TrendsResponse?,
        ) : AnalyticsFetch
        data class Failure(
            override val period: AnalyticsPeriod,
            override val bowType: BowType?,
            override val distance: ShootingDistance?,
            val message: String,
        ) : AnalyticsFetch
    }

    companion object {
        /** Exposed for tests. */
        const val TopSuggestionLimit: Int = DASHBOARD_SUGGESTION_LIMIT
    }
}

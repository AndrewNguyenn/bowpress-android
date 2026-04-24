package com.andrewnguyen.bowpress.feature.analytics.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.Bow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A single row-block on the suggestions dashboard.
 *
 * Groups all suggestions for one bow together. Within the group, unread
 * suggestions come first, then read ones; both sub-groups are sorted
 * newest-first. Mirrors iOS `DashboardViewModel.groupedSuggestions`.
 */
data class SuggestionGroup(
    val bowId: String,
    val bowName: String,
    val suggestions: List<AnalyticsSuggestion>,
) {
    val unreadCount: Int = suggestions.count { !it.wasRead }
}

/** UI state for the suggestions Dashboard tab (iOS `DashboardView`). */
data class SuggestionsDashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val groups: List<SuggestionGroup> = emptyList(),
    /** Total number of unread, non-dismissed suggestions across all groups. */
    val unreadCount: Int = 0,
    val error: String? = null,
)

/**
 * View model for the Dashboard tab. Reactively surfaces all non-dismissed
 * suggestions grouped by bow, drives pull-to-refresh, and exposes `markRead`
 * so the screen can optimistically update a suggestion when the user taps it.
 *
 * Mirrors iOS `DashboardViewModel` in `bowpress-ios/Sources/BowPress/Dashboard/DashboardViewModel.swift`.
 */
@HiltViewModel
class SuggestionsDashboardViewModel @Inject constructor(
    private val suggestionRepository: SuggestionRepository,
    private val bowRepository: BowRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionsDashboardUiState())
    val uiState: StateFlow<SuggestionsDashboardUiState> = _uiState.asStateFlow()

    init {
        observeSuggestions()
    }

    private fun observeSuggestions() {
        combine(
            suggestionRepository.observeAll(),
            bowRepository.observeBows(),
        ) { suggestions, bows ->
            val bowsById: Map<String, Bow> = bows.associateBy { it.id }
            val filtered = suggestions.filter { !it.wasDismissed }
            val unreadTotal = filtered.count { !it.wasRead }
            val grouped = filtered
                .groupBy { it.bowId }
                .map { (bowId, list) ->
                    val sorted = list.sortedWith(
                        // Unread first (wasRead=false before wasRead=true), then newest-first.
                        compareBy<AnalyticsSuggestion> { it.wasRead }
                            .thenByDescending { it.createdAt },
                    )
                    SuggestionGroup(
                        bowId = bowId,
                        bowName = bowsById[bowId]?.name ?: bowId,
                        suggestions = sorted,
                    )
                }
                // Stable ordering: bows with more unread come first, then alphabetical by bowId.
                .sortedWith(
                    compareByDescending<SuggestionGroup> { it.unreadCount }
                        .thenBy { it.bowId },
                )
            SuggestionsDashboardUiState(
                isLoading = false,
                isRefreshing = _uiState.value.isRefreshing,
                groups = grouped,
                unreadCount = unreadTotal,
                error = null,
            )
        }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    /**
     * Pull-to-refresh entry point. Fans out to `refreshForBow` for each bow the
     * user owns. Errors are swallowed per-bow so a single failure doesn't drop
     * the others from the UI; callers see a combined error banner only if every
     * bow failed.
     */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        viewModelScope.launch {
            val bows = runCatching { bowRepository.getBows() }.getOrDefault(emptyList())
            var failures = 0
            for (bow in bows) {
                runCatching { suggestionRepository.refreshForBow(bow.id) }
                    .onFailure { failures += 1 }
            }
            val errorMsg: String? = if (bows.isNotEmpty() && failures == bows.size) {
                "Failed to refresh suggestions"
            } else null
            _uiState.value = _uiState.value.copy(isRefreshing = false, error = errorMsg)
        }
    }

    /**
     * Marks a suggestion as read. Called when the user taps a row — matches iOS
     * `DashboardViewModel.markRead`. The Room update is optimistic; the network
     * call is fire-and-forget inside [SuggestionRepository.markRead].
     */
    fun markRead(suggestionId: String) {
        viewModelScope.launch {
            runCatching { suggestionRepository.markRead(suggestionId) }
        }
    }
}

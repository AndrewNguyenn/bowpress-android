package com.andrewnguyen.bowpress.feature.analytics.suggestion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.feature.analytics.navigation.AnalyticsRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestionDetailUiState(
    val isLoading: Boolean = true,
    val suggestion: AnalyticsSuggestion? = null,
    val isApplying: Boolean = false,
    val error: String? = null,
)

sealed interface SuggestionDetailEvent {
    /** Apply succeeded; host should navigate to the newly-created config. */
    data class Applied(val bowId: String, val newConfig: BowConfiguration) : SuggestionDetailEvent
}

@HiltViewModel
class SuggestionDetailViewModel @Inject constructor(
    private val repository: SuggestionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Nav-argument keys were registered by AnalyticsNavigation — plucking them here
    // avoids plumbing them through the composable.
    private val bowId: String = requireNotNull(savedStateHandle[AnalyticsRoutes.Args.BowId]) {
        "bowId argument is required for SuggestionDetail"
    }
    private val suggestionId: String = requireNotNull(savedStateHandle[AnalyticsRoutes.Args.SuggestionId]) {
        "suggestionId argument is required for SuggestionDetail"
    }

    private val _uiState = MutableStateFlow(SuggestionDetailUiState())
    val uiState: StateFlow<SuggestionDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<SuggestionDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val existing = repository.getById(suggestionId)
            if (existing == null) {
                _uiState.value = SuggestionDetailUiState(
                    isLoading = false,
                    error = "Suggestion not found",
                )
                return@launch
            }
            _uiState.value = SuggestionDetailUiState(
                isLoading = false,
                suggestion = existing,
            )
        }
    }

    fun markRead() {
        val current = _uiState.value.suggestion ?: return
        if (current.wasRead) return
        viewModelScope.launch {
            runCatching { repository.markRead(suggestionId) }
            _uiState.value = _uiState.value.copy(
                suggestion = current.copy(wasRead = true),
            )
        }
    }

    fun dismiss() {
        val current = _uiState.value.suggestion ?: return
        viewModelScope.launch {
            runCatching { repository.dismiss(suggestionId) }
            _uiState.value = _uiState.value.copy(
                suggestion = current.copy(wasDismissed = true),
            )
        }
    }

    fun apply() {
        val current = _uiState.value.suggestion ?: return
        if (_uiState.value.isApplying || current.wasApplied) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, error = null)
            val result = runCatching { repository.apply(bowId = bowId, id = suggestionId) }
            result
                .onSuccess { applied ->
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        suggestion = applied.suggestion,
                    )
                    _events.trySend(
                        SuggestionDetailEvent.Applied(
                            bowId = bowId,
                            newConfig = applied.newConfig,
                        ),
                    )
                }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        error = t.message ?: "Failed to apply suggestion",
                    )
                }
        }
    }
}

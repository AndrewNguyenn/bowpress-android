package com.andrewnguyen.bowpress.feature.social.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the friend session detail screen (§16) — a friend's full shared
 * session: scorecard ends + plotted arrows for the target face.
 */
data class FriendSessionDetailUiState(
    val detail: SharedSessionDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FriendSessionDetailViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendSessionDetailUiState(isLoading = true))
    val uiState: StateFlow<FriendSessionDetailUiState> = _uiState.asStateFlow()

    /** Resolve the detail for [sharedSessionId]. */
    fun load(sharedSessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getSharedSessionDetail(sharedSessionId) }
                .onSuccess { detail ->
                    _uiState.update { it.copy(detail = detail, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

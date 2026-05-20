package com.andrewnguyen.bowpress.feature.social.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Achievement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Trophy-case state (contract §15) — the achievement list for either the
 * signed-in user or a friend.
 *
 * Shared by the You screen ([loadMine]) and the Friend profile
 * ([loadForFriend]); a screen calls exactly one loader.
 */
data class AchievementsUiState(
    val achievements: List<Achievement> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState(isLoading = true))
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    /** Load the signed-in user's trophy case (You screen). */
    fun loadMine() {
        load { socialRepository.getMyAchievements() }
    }

    /** Load a friend's trophy case (Friend profile). Visibility-gated server-side. */
    fun loadForFriend(otherUserId: String) {
        load { socialRepository.getFriendAchievements(otherUserId) }
    }

    private fun load(fetch: suspend () -> List<Achievement>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { fetch() }
                .onSuccess { list ->
                    _uiState.update { it.copy(achievements = list, isLoading = false) }
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

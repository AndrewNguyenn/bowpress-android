package com.andrewnguyen.bowpress.feature.social.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.TrophyDef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Trophy-case state (contract §15 / §18) — the earned achievement list for
 * either the signed-in user or a friend, together with the full server-side
 * trophy catalogue (12 kinds) so [TrophyCaseSection] can render every slot as
 * either earned or locked.
 *
 * Shared by the You screen ([loadMine]) and the Friend profile
 * ([loadForFriend]); a screen calls exactly one loader.
 */
data class AchievementsUiState(
    val achievements: List<Achievement> = emptyList(),
    val catalog: List<TrophyDef> = emptyList(),
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

    /**
     * Fetch earned achievements then the trophy catalogue sequentially.
     * If the earned fetch throws the error is surfaced; the catalogue fetch is
     * best-effort — on failure it silently falls back to an empty list so the
     * section degrades gracefully rather than blocking on a secondary call.
     *
     * Sequential order keeps the coroutine structure simple and avoids async
     * child-exception propagation edge-cases in the test dispatcher.
     */
    private fun load(fetchEarned: suspend () -> List<Achievement>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { fetchEarned() }
                .onSuccess { earned ->
                    // Catalogue is best-effort — never block the trophy case on it.
                    val catalog = runCatching { socialRepository.getTrophyCatalog() }
                        .getOrDefault(emptyList())
                    _uiState.update {
                        it.copy(achievements = earned, catalog = catalog, isLoading = false)
                    }
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

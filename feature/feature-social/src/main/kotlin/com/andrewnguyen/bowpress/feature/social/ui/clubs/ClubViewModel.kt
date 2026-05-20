package com.andrewnguyen.bowpress.feature.social.ui.clubs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubFeedItem
import com.andrewnguyen.bowpress.core.model.ClubMember
import com.andrewnguyen.bowpress.core.model.LeaderboardRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClubsUiState(
    val clubs: List<Club> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class ClubHomeUiState(
    val club: Club? = null,
    val members: List<ClubMember> = emptyList(),
    val feed: List<ClubFeedItem> = emptyList(),
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val leaderboardScope: String = "30d",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ClubViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _clubsState = MutableStateFlow(ClubsUiState(isLoading = true))
    val clubsState: StateFlow<ClubsUiState> = _clubsState.asStateFlow()

    private val _clubHomeState = MutableStateFlow(ClubHomeUiState(isLoading = true))
    val clubHomeState: StateFlow<ClubHomeUiState> = _clubHomeState.asStateFlow()

    init {
        loadClubs()
    }

    fun loadClubs() {
        viewModelScope.launch {
            _clubsState.update { it.copy(isLoading = true) }
            runCatching { socialRepository.refreshClubs() }
                .onFailure { e -> _clubsState.update { it.copy(error = e.message, isLoading = false) } }
            socialRepository.observeClubs().collect { clubs ->
                _clubsState.update { it.copy(clubs = clubs, isLoading = false) }
            }
        }
    }

    fun loadClubHome(clubId: String) {
        viewModelScope.launch {
            _clubHomeState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val club = socialRepository.getClub(clubId)
                val members = socialRepository.getClubMembers(clubId)
                val feed = socialRepository.getClubFeed(clubId)
                val leaderboard = socialRepository.getClubLeaderboard(clubId, _clubHomeState.value.leaderboardScope)
                _clubHomeState.update {
                    it.copy(
                        club = club,
                        members = members,
                        feed = feed,
                        leaderboard = leaderboard,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _clubHomeState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun setLeaderboardScope(clubId: String, scope: String) {
        _clubHomeState.update { it.copy(leaderboardScope = scope) }
        viewModelScope.launch {
            runCatching { socialRepository.getClubLeaderboard(clubId, scope) }
                .onSuccess { lb -> _clubHomeState.update { it.copy(leaderboard = lb) } }
        }
    }

    fun createClub(name: String, description: String? = null, onSuccess: (Club) -> Unit) {
        viewModelScope.launch {
            runCatching { socialRepository.createClub(name, description) }
                .onSuccess { club ->
                    _clubsState.update { it.copy(clubs = it.clubs + club) }
                    onSuccess(club)
                }
                .onFailure { e -> _clubsState.update { it.copy(error = e.message) } }
        }
    }

    fun joinClub(inviteCode: String, onSuccess: (Club) -> Unit) {
        viewModelScope.launch {
            runCatching { socialRepository.joinClub(inviteCode) }
                .onSuccess { club ->
                    _clubsState.update { it.copy(clubs = it.clubs + club) }
                    onSuccess(club)
                }
                .onFailure { e -> _clubsState.update { it.copy(error = e.message) } }
        }
    }

    fun leaveClub(clubId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.leaveClub(clubId) }
                .onSuccess {
                    _clubsState.update { s -> s.copy(clubs = s.clubs.filter { it.id != clubId }) }
                }
                .onFailure { e -> _clubsState.update { it.copy(error = e.message) } }
        }
    }

    fun updateClub(id: String, name: String? = null, description: String? = null, notes: String? = null) {
        viewModelScope.launch {
            runCatching { socialRepository.updateClub(id, name, description, notes) }
                .onSuccess { updated ->
                    _clubHomeState.update { it.copy(club = updated) }
                    _clubsState.update { s ->
                        s.copy(clubs = s.clubs.map { if (it.id == id) updated else it })
                    }
                }
                .onFailure { e -> _clubHomeState.update { it.copy(error = e.message) } }
        }
    }

    fun dismissError() {
        _clubsState.update { it.copy(error = null) }
        _clubHomeState.update { it.copy(error = null) }
    }
}

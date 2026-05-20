package com.andrewnguyen.bowpress.feature.social.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.CompareView
import com.andrewnguyen.bowpress.core.model.FriendProfile
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.SocialProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val friends: List<Friendship> = emptyList(),
    val pendingRequests: List<Friendship> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class FriendProfileUiState(
    val friendProfile: FriendProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class FriendSearchUiState(
    val query: String = "",
    val result: SocialProfile? = null,
    val isSearching: Boolean = false,
    val requestSent: Boolean = false,
    val error: String? = null,
)

data class CompareUiState(
    val compareView: CompareView? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    // ── Friends list ─────────────────────────────────────────────────────────

    private val _friendsState = MutableStateFlow(FriendsUiState(isLoading = true))
    val friendsState: StateFlow<FriendsUiState> = _friendsState.asStateFlow()

    // ── Friend search ─────────────────────────────────────────────────────────

    private val _searchState = MutableStateFlow(FriendSearchUiState())
    val searchState: StateFlow<FriendSearchUiState> = _searchState.asStateFlow()

    // ── Friend profile ────────────────────────────────────────────────────────

    private val _profileState = MutableStateFlow(FriendProfileUiState())
    val profileState: StateFlow<FriendProfileUiState> = _profileState.asStateFlow()

    // ── Compare ────────────────────────────────────────────────────────────────

    private val _compareState = MutableStateFlow(CompareUiState())
    val compareState: StateFlow<CompareUiState> = _compareState.asStateFlow()

    init {
        loadFriends()
    }

    // ── Friends list ──────────────────────────────────────────────────────────

    fun loadFriends() {
        viewModelScope.launch {
            _friendsState.update { it.copy(isLoading = true) }
            runCatching { socialRepository.refreshFriends() }
                .onFailure { e -> _friendsState.update { it.copy(error = e.message, isLoading = false) } }
            // Observe local once refreshed
            socialRepository.observeFriends().collect { friends ->
                _friendsState.update { it.copy(friends = friends, isLoading = false) }
            }
        }
        viewModelScope.launch {
            socialRepository.observePendingRequests().collect { pending ->
                _friendsState.update { it.copy(pendingRequests = pending) }
            }
        }
    }

    fun acceptRequest(id: String) {
        viewModelScope.launch {
            runCatching { socialRepository.acceptFriendRequest(id) }
                .onFailure { e -> _friendsState.update { it.copy(error = e.message) } }
        }
    }

    fun declineRequest(id: String) {
        viewModelScope.launch {
            runCatching { socialRepository.declineOrCancelRequest(id) }
                .onFailure { e -> _friendsState.update { it.copy(error = e.message) } }
        }
    }

    fun unfriend(otherUserId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.unfriend(otherUserId) }
                .onFailure { e -> _friendsState.update { it.copy(error = e.message) } }
        }
    }

    // ── Friend search ─────────────────────────────────────────────────────────

    fun setQuery(query: String) {
        _searchState.update { it.copy(query = query, result = null, error = null, requestSent = false) }
    }

    fun searchArcher(handle: String) {
        viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true, error = null) }
            runCatching { socialRepository.searchArcher(handle) }
                .onSuccess { p -> _searchState.update { it.copy(result = p, isSearching = false) } }
                .onFailure { e -> _searchState.update { it.copy(error = e.message, isSearching = false) } }
        }
    }

    fun sendFriendRequest(handle: String) {
        viewModelScope.launch {
            runCatching { socialRepository.sendFriendRequest(handle) }
                .onSuccess { _searchState.update { it.copy(requestSent = true, error = null) } }
                .onFailure { e -> _searchState.update { it.copy(error = e.message) } }
        }
    }

    // ── Friend profile ────────────────────────────────────────────────────────

    fun loadFriendProfile(otherUserId: String) {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getFriendProfile(otherUserId) }
                .onSuccess { fp -> _profileState.update { it.copy(friendProfile = fp, isLoading = false) } }
                .onFailure { e -> _profileState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    // ── Compare ────────────────────────────────────────────────────────────────

    fun loadCompare(otherUserId: String, distance: String? = null, face: String? = null) {
        viewModelScope.launch {
            _compareState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getCompareView(otherUserId, distance, face) }
                .onSuccess { cv -> _compareState.update { it.copy(compareView = cv, isLoading = false) } }
                .onFailure { e -> _compareState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun dismissError() {
        _friendsState.update { it.copy(error = null) }
        _searchState.update { it.copy(error = null) }
        _profileState.update { it.copy(error = null) }
        _compareState.update { it.copy(error = null) }
    }
}

package com.andrewnguyen.bowpress.feature.social.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.CompareView
import com.andrewnguyen.bowpress.core.model.FriendProfile
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipDirection
import com.andrewnguyen.bowpress.core.model.FriendshipSource
import com.andrewnguyen.bowpress.core.model.FriendshipStatus
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
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
    /**
     * Parity E1 — friendship row between the signed-in user and the friend
     * being viewed. Drives the follow-button state machine: null = stranger,
     * pending+outgoing = "Requested", pending+incoming = "Respond", accepted
     * = "Following".
     */
    val friendship: Friendship? = null,
    /** Parity E1 — true while the follow PATCH is in flight. */
    val followBusy: Boolean = false,
    /** Parity E1 — inline error for a failed follow/unfriend round-trip. */
    val followError: String? = null,
)

data class FriendSearchUiState(
    val query: String = "",
    val result: SocialProfile? = null,
    val isSearching: Boolean = false,
    val requestSent: Boolean = false,
    val error: String? = null,
    /**
     * Parity E9 — live substring fuzzy results. The screen's debounced
     * effect kicks `searchSuggestions(query)`; an empty query clears the
     * list. Distinct from [result] which still drives the exact-handle
     * single-row resolution.
     */
    val suggestions: List<HandleSuggestion> = emptyList(),
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

    /**
     * Parity E9 — live, substring fuzzy search backing the new add-friend
     * picker. Matches on handle OR display name. The screen debounces; this
     * is a plain pass-through that swallows network errors so an offline
     * blip doesn't blow away the typed query.
     */
    fun searchSuggestions(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                _searchState.update { it.copy(suggestions = emptyList()) }
                return@launch
            }
            runCatching { socialRepository.searchHandlesSubstring(trimmed) }
                .onSuccess { hits ->
                    _searchState.update { it.copy(suggestions = hits) }
                }
                .onFailure {
                    _searchState.update { it.copy(suggestions = emptyList()) }
                }
        }
    }

    // ── Friend profile ────────────────────────────────────────────────────────

    fun loadFriendProfile(otherUserId: String) {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getFriendProfile(otherUserId) }
                .onSuccess { fp ->
                    _profileState.update { it.copy(friendProfile = fp, isLoading = false) }
                }
                .onFailure { e -> _profileState.update { it.copy(error = e.message, isLoading = false) } }
            // Best-effort refresh so the observe-flows below get a current
            // friends + pending list. Soft-fail.
            runCatching { socialRepository.refreshFriends() }
        }
        // Parity E1 — observe the friends + pending-requests flows so the
        // follow button reflects accept / cancel / unfriend mutations as
        // they land in Room.
        viewModelScope.launch {
            socialRepository.observeFriends().collect { friends ->
                val match = friends.firstOrNull { it.otherUserId == otherUserId }
                if (match != null) {
                    _profileState.update { it.copy(friendship = match) }
                }
            }
        }
        viewModelScope.launch {
            socialRepository.observePendingRequests().collect { pending ->
                val match = pending.firstOrNull { it.otherUserId == otherUserId }
                _profileState.update { state ->
                    // Don't overwrite an accepted friendship with a stale
                    // pending row (a pending row can linger in Room briefly
                    // after the accept call updates the friends list).
                    if (state.friendship?.status == FriendshipStatus.accepted) state
                    else state.copy(friendship = match)
                }
            }
        }
    }

    /**
     * Parity E1 — Strava-style follow cycling on the friend-profile screen.
     *
     * Maps the button state onto our friendship model:
     *   no friendship → send friend request
     *   outgoing pending → cancel
     *   incoming pending → accept
     *   accepted → unfriend
     *
     * Optimistically ticks the visible friendCount up on accept / down on
     * unfriend so the stat row stays in sync with the button label.
     */
    fun cycleFollow(otherUserHandle: String, otherUserId: String) {
        val state = _profileState.value
        if (state.followBusy) return
        _profileState.update { it.copy(followBusy = true, followError = null) }
        viewModelScope.launch {
            val f = state.friendship
            val result = runCatching {
                when {
                    f == null -> {
                        val sent = socialRepository.sendFriendRequest(
                            handle = otherUserHandle,
                            source = FriendshipSource.handle,
                        )
                        _profileState.update { it.copy(friendship = sent) }
                    }
                    f.status == FriendshipStatus.pending &&
                        f.direction == FriendshipDirection.outgoing -> {
                        socialRepository.declineOrCancelRequest(f.id)
                        _profileState.update { it.copy(friendship = null) }
                    }
                    f.status == FriendshipStatus.pending &&
                        f.direction == FriendshipDirection.incoming -> {
                        val updated = socialRepository.acceptFriendRequest(f.id)
                        _profileState.update { s ->
                            val fp = s.friendProfile?.let { it.copy(friendCount = it.friendCount + 1) }
                            s.copy(friendship = updated, friendProfile = fp)
                        }
                    }
                    f.status == FriendshipStatus.accepted -> {
                        socialRepository.unfriend(otherUserId)
                        _profileState.update { s ->
                            val fp = s.friendProfile?.let {
                                it.copy(friendCount = (it.friendCount - 1).coerceAtLeast(0))
                            }
                            s.copy(friendship = null, friendProfile = fp)
                        }
                    }
                    else -> Unit
                }
            }
            result.onFailure { e ->
                _profileState.update {
                    it.copy(followError = "Couldn't update friend status. Try again.")
                }
            }
            _profileState.update { it.copy(followBusy = false) }
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

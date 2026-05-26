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

/**
 * Parity E9 — backing state for the live fuzzy add-friend search. The
 * legacy single-handle `searchArcher` path is gone (commit 5bcf33a on iOS
 * deleted it too); every keystroke now drives [suggestions] and each row
 * carries its own SENT chip via [sentHandles].
 */
data class FriendSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val error: String? = null,
    /** Live substring suggestions. Cleared synchronously when the query goes blank. */
    val suggestions: List<HandleSuggestion> = emptyList(),
    /**
     * Handles successfully sent a friend request this session. Mirrors the
     * per-row state pattern in `InviteArcherSheet` so the row chip flips to
     * SENT without leaving the search. Defensively a Set so duplicate adds
     * are a no-op.
     */
    val sentHandles: Set<String> = emptySet(),
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

    /**
     * Parity E1 — the friend-profile screen is rendered inside this VM's
     * scope and reads [_profileState.friendship] off these flows. Collecting
     * them once at init time (instead of inside [loadFriendProfile]) avoids
     * stacking collectors per nav-entry recomposition. The match filter is
     * keyed by [profileTargetUserId] so the right friendship row surfaces
     * even though the flows fire on every Room mutation.
     */
    private var profileTargetUserId: String = ""

    init {
        loadFriends()
        // Single-collector lifetime — keyed by [profileTargetUserId] so the
        // flow can re-target without spawning new collectors.
        viewModelScope.launch {
            socialRepository.observeFriends().collect { friends ->
                val target = profileTargetUserId
                if (target.isEmpty()) return@collect
                val match = friends.firstOrNull { it.otherUserId == target }
                if (match != null) {
                    _profileState.update { it.copy(friendship = match) }
                }
            }
        }
        viewModelScope.launch {
            socialRepository.observePendingRequests().collect { pending ->
                val target = profileTargetUserId
                if (target.isEmpty()) return@collect
                val match = pending.firstOrNull { it.otherUserId == target }
                // Only set when there's a matching pending row; an unrelated
                // mutation shouldn't blow away an accepted friendship we
                // already resolved on the friends-flow side.
                if (match != null) {
                    _profileState.update { it.copy(friendship = match) }
                }
            }
        }
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

    // ── Friend search (parity E9) ────────────────────────────────────────────

    /**
     * Update the typed query. Clears [suggestions] synchronously when the
     * trimmed input goes empty so the row list disappears immediately on
     * backspace-to-blank instead of waiting for the screen's 250ms debounce
     * to land.
     */
    fun setQuery(query: String) {
        val trimmed = query.trim()
        _searchState.update {
            it.copy(
                query = query,
                error = null,
                suggestions = if (trimmed.isEmpty()) emptyList() else it.suggestions,
            )
        }
    }

    /**
     * Parity E9 — fire-and-forget friend request from a suggestion-row tap.
     * Adds the handle to [sentHandles] on success so the row chip flips to
     * SENT; surfaces errors via [error] without dropping the typed query.
     */
    fun sendFriendRequest(handle: String) {
        viewModelScope.launch {
            runCatching { socialRepository.sendFriendRequest(handle) }
                .onSuccess {
                    _searchState.update {
                        it.copy(error = null, sentHandles = it.sentHandles + handle)
                    }
                }
                .onFailure { e -> _searchState.update { it.copy(error = e.message) } }
        }
    }

    /**
     * Parity E9 — live, substring fuzzy search backing the add-friend
     * picker. Matches on handle OR display name. The screen debounces; this
     * is a plain pass-through that swallows network errors so an offline
     * blip doesn't blow away the typed query.
     */
    fun searchSuggestions(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                _searchState.update { it.copy(suggestions = emptyList(), isSearching = false) }
                return@launch
            }
            _searchState.update { it.copy(isSearching = true) }
            runCatching { socialRepository.searchHandlesSubstring(trimmed) }
                .onSuccess { hits ->
                    _searchState.update { it.copy(suggestions = hits, isSearching = false) }
                }
                .onFailure {
                    _searchState.update { it.copy(suggestions = emptyList(), isSearching = false) }
                }
        }
    }

    // ── Friend profile ────────────────────────────────────────────────────────

    fun loadFriendProfile(otherUserId: String) {
        // Re-target the lifetime-long observers in [init]. They'll surface
        // any matching friendship row from the next emission of either flow.
        profileTargetUserId = otherUserId
        // Clear any leftover friendship from the previously-viewed profile
        // before the observers settle on the new target.
        _profileState.update { it.copy(friendship = null) }
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getFriendProfile(otherUserId) }
                .onSuccess { fp ->
                    _profileState.update { it.copy(friendProfile = fp, isLoading = false) }
                }
                .onFailure { e -> _profileState.update { it.copy(error = e.message, isLoading = false) } }
            // Best-effort refresh so the init-time observers see a current
            // friends + pending list. Soft-fail.
            runCatching { socialRepository.refreshFriends() }
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

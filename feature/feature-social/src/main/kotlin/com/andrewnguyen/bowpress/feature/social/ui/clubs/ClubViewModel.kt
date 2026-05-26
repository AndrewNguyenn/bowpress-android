package com.andrewnguyen.bowpress.feature.social.ui.clubs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubAnnouncement
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
    val announcements: List<ClubAnnouncement> = emptyList(),
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val leaderboardScope: String = "30d",
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Host-only invite-by-handle dialog state. */
    val inviteError: String? = null,
    val inviteSent: Boolean = false,
    /** Host-only announcement composer error. */
    val announcementError: String? = null,
    /**
     * Parity E2 / E3 — the signed-in caller's user id, so the Club screen can
     * gate the "Edit description" affordance to the host (compare to
     * [Club.createdBy]) and pass a null actor-click callback for the caller's
     * own leaderboard row (the you-row stays inert).
     */
    val currentUserId: String = "",
    /** Parity E3 — error message when the description PATCH fails. */
    val descriptionError: String? = null,
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
                // Announcements are member-gated and best-effort — a fetch
                // failure leaves the board empty without sinking the screen.
                val announcements = runCatching {
                    socialRepository.getClubAnnouncements(clubId)
                }.getOrDefault(emptyList())
                // Parity E2 / E3 — current user id drives host gating + the
                // you-row inert affordance in the leaderboard.
                val me = runCatching { socialRepository.getMyProfile() }.getOrNull()
                _clubHomeState.update {
                    it.copy(
                        club = club,
                        members = members,
                        feed = feed,
                        announcements = announcements,
                        leaderboard = leaderboard,
                        currentUserId = me?.userId ?: it.currentUserId,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _clubHomeState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    /**
     * Parity E3 — host-only description update. The dialog is hidden until
     * the host taps Edit and the trimmed text is non-blank — empty trimmed
     * text would clear an existing description, which the caller should opt
     * into deliberately (an explicit empty save still passes through, but
     * the screen's `canSave` gate prevents it). Surfaces failure inline via
     * [ClubHomeUiState.descriptionError].
     */
    fun updateClubDescription(id: String, description: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            _clubHomeState.update { it.copy(descriptionError = null) }
            runCatching {
                socialRepository.updateClub(id, description = description)
            }.onSuccess { updated ->
                _clubHomeState.update { it.copy(club = updated) }
                _clubsState.update { s ->
                    s.copy(clubs = s.clubs.map { if (it.id == id) updated else it })
                }
                onSaved()
            }.onFailure { e ->
                _clubHomeState.update { it.copy(descriptionError = e.message) }
            }
        }
    }

    /**
     * Parity E8 — host-only visibility / joinPolicy update. Either argument
     * may be null to leave the field untouched (matches [UpdateClubBody]).
     */
    fun updateClubAccess(
        id: String,
        visibility: com.andrewnguyen.bowpress.core.model.ClubVisibility? = null,
        joinPolicy: com.andrewnguyen.bowpress.core.model.ClubJoinPolicy? = null,
    ) {
        viewModelScope.launch {
            runCatching {
                socialRepository.updateClubAccess(id, visibility, joinPolicy)
            }.onSuccess { updated ->
                _clubHomeState.update { it.copy(club = updated) }
                _clubsState.update { s ->
                    s.copy(clubs = s.clubs.map { if (it.id == id) updated else it })
                }
            }.onFailure { e ->
                _clubHomeState.update { it.copy(error = e.message) }
            }
        }
    }

    fun resetDescriptionError() {
        _clubHomeState.update { it.copy(descriptionError = null) }
    }

    /** Reload just the announcement board (after a post / pin / delete). */
    private fun refreshAnnouncements(clubId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.getClubAnnouncements(clubId) }
                .onSuccess { list -> _clubHomeState.update { it.copy(announcements = list) } }
        }
    }

    /** Host-only: post a new announcement to the club board. */
    fun postAnnouncement(clubId: String, body: String, pinned: Boolean, onPosted: () -> Unit) {
        viewModelScope.launch {
            _clubHomeState.update { it.copy(announcementError = null) }
            runCatching { socialRepository.postClubAnnouncement(clubId, body, pinned) }
                .onSuccess {
                    refreshAnnouncements(clubId)
                    onPosted()
                }
                .onFailure { e -> _clubHomeState.update { it.copy(announcementError = e.message) } }
        }
    }

    /** Host-only: pin or unpin an announcement. */
    fun setAnnouncementPinned(clubId: String, announcementId: String, pinned: Boolean) {
        viewModelScope.launch {
            runCatching { socialRepository.setClubAnnouncementPinned(clubId, announcementId, pinned) }
                .onSuccess { refreshAnnouncements(clubId) }
                .onFailure { e -> _clubHomeState.update { it.copy(error = e.message) } }
        }
    }

    /** Host-only: delete an announcement. */
    fun deleteAnnouncement(clubId: String, announcementId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.deleteClubAnnouncement(clubId, announcementId) }
                .onSuccess {
                    _clubHomeState.update { s ->
                        s.copy(announcements = s.announcements.filterNot { it.id == announcementId })
                    }
                }
                .onFailure { e -> _clubHomeState.update { it.copy(error = e.message) } }
        }
    }

    fun resetAnnouncementError() {
        _clubHomeState.update { it.copy(announcementError = null) }
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

    /**
     * Host-only: invite an archer to [clubId] by `@handle` (§11). On success
     * flips [ClubHomeUiState.inviteSent]; on failure surfaces [inviteError].
     */
    fun inviteToClub(clubId: String, handle: String) {
        viewModelScope.launch {
            _clubHomeState.update { it.copy(inviteError = null) }
            runCatching { socialRepository.inviteToClub(clubId, handle) }
                .onSuccess { _clubHomeState.update { it.copy(inviteSent = true) } }
                .onFailure { e -> _clubHomeState.update { it.copy(inviteError = e.message) } }
        }
    }

    /** Reset the invite dialog state when it's dismissed. */
    fun resetInviteState() {
        _clubHomeState.update { it.copy(inviteError = null, inviteSent = false) }
    }

    /**
     * Parity E8 — fuzzy substring search for the InviteArcherSheet result
     * rows. Returns up to 8 [HandleSuggestion]s; the sheet handles its own
     * debounce so this is a plain pass-through.
     */
    suspend fun searchInviteCandidates(query: String) =
        runCatching { socialRepository.searchHandlesSubstring(query) }
            .getOrDefault(emptyList())

    /**
     * Parity E8 — single-row invite from the InviteArcherSheet. Returns
     * `Result<Unit>` so the sheet can flip the row to SENT / show the error
     * without going through view-state. Does not flip the legacy
     * `inviteSent` flag (the sheet has its own per-row state).
     */
    suspend fun inviteHandleToClub(clubId: String, handle: String): Result<Unit> =
        runCatching { socialRepository.inviteToClub(clubId, handle) }.map { }

    fun dismissError() {
        _clubsState.update { it.copy(error = null) }
        _clubHomeState.update { it.copy(error = null) }
    }
}

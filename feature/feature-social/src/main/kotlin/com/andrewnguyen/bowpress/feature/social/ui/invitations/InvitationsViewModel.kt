package com.andrewnguyen.bowpress.feature.social.ui.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.sync.SocialBadgeRefreshBus
import com.andrewnguyen.bowpress.core.model.InvitationKind
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pending club + league invitations for the signed-in user (contract §11).
 *
 * Both the Clubs "Invites" section and the Leagues "Invites" section observe
 * this one VM — the API returns club + league invites in a single list, so a
 * shared VM avoids duplicating the fetch/accept/decline logic. Each surface
 * filters [InvitationsUiState.invitations] by [SocialInvitation.kind].
 */
data class InvitationsUiState(
    val invitations: List<SocialInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val clubInvites: List<SocialInvitation>
        get() = invitations.filter { it.kind == InvitationKind.club }

    val leagueInvites: List<SocialInvitation>
        get() = invitations.filter { it.kind == InvitationKind.league }
}

@HiltViewModel
class InvitationsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val socialBadgeRefreshBus: SocialBadgeRefreshBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationsUiState(isLoading = true))
    val uiState: StateFlow<InvitationsUiState> = _uiState.asStateFlow()

    init {
        loadInvitations()
    }

    /** Re-fetch the pending invitation list (online-first). */
    fun loadInvitations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getInvitations() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            invitations = list.filter { inv -> inv.status == InvitationStatus.pending },
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    /**
     * Accept a club/league invitation. Removes it from the list optimistically
     * on success, bumps the Social-tab badge, and invokes [onAccepted] so the
     * host screen can reload its club/league list (the just-joined entity is
     * not in Room until that refresh runs).
     */
    fun acceptInvitation(id: String, onAccepted: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { socialRepository.acceptInvitation(id) }
                .onSuccess {
                    _uiState.update { s ->
                        s.copy(invitations = s.invitations.filterNot { it.id == id })
                    }
                    socialBadgeRefreshBus.bump()
                    onAccepted()
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    /** Decline an invitation, drop it from the list, and refresh the badge. */
    fun declineInvitation(id: String) {
        viewModelScope.launch {
            runCatching { socialRepository.declineInvitation(id) }
                .onSuccess {
                    _uiState.update { s ->
                        s.copy(invitations = s.invitations.filterNot { it.id == id })
                    }
                    socialBadgeRefreshBus.bump()
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

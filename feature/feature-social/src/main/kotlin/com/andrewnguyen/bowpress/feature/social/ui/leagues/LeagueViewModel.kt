package com.andrewnguyen.bowpress.feature.social.ui.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.AdminMatrix
import com.andrewnguyen.bowpress.core.model.AttachmentKind
import com.andrewnguyen.bowpress.core.model.CreateLeagueBody
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.model.HandicapConfig
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueAttachment
import com.andrewnguyen.bowpress.core.model.LeagueEntryRule
import com.andrewnguyen.bowpress.core.model.LeagueSchedule
import com.andrewnguyen.bowpress.core.model.LeagueScheduleKind
import com.andrewnguyen.bowpress.core.model.LeagueStandingRow
import com.andrewnguyen.bowpress.core.model.LeagueSubmission
import com.andrewnguyen.bowpress.core.model.LeagueType
import com.andrewnguyen.bowpress.core.model.RoundDef
import com.andrewnguyen.bowpress.core.model.SubmitScoreBody
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaguesUiState(
    val leagues: List<League> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class LeagueHomeUiState(
    val league: League? = null,
    val standings: List<LeagueStandingRow> = emptyList(),
    val mySubmissions: List<LeagueSubmission> = emptyList(),
    val attachments: List<LeagueAttachment> = emptyList(),
    val adminMatrix: AdminMatrix? = null,
    val selectedDivision: Division? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Host-only invite-by-handle dialog state. */
    val inviteError: String? = null,
    val inviteSent: Boolean = false,
    /** Host-only add-attachment dialog error. */
    val attachmentError: String? = null,
)

/** Form state for the league composer. */
data class LeagueComposerState(
    val name: String = "",
    val leagueType: LeagueType = LeagueType.individual,
    val divisions: List<Division> = listOf(Division.CMP),
    val endCount: Int = 10,
    val arrowsPerEnd: Int = 6,
    val scheduleKind: LeagueScheduleKind = LeagueScheduleKind.weekly,
    val totalWeeks: Int = 8,
    val entryRule: LeagueEntryRule = LeagueEntryRule.open,
    val handicapConfig: HandicapConfig = HandicapConfig(),
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _leaguesState = MutableStateFlow(LeaguesUiState(isLoading = true))
    val leaguesState: StateFlow<LeaguesUiState> = _leaguesState.asStateFlow()

    private val _leagueHomeState = MutableStateFlow(LeagueHomeUiState(isLoading = true))
    val leagueHomeState: StateFlow<LeagueHomeUiState> = _leagueHomeState.asStateFlow()

    private val _composerState = MutableStateFlow(LeagueComposerState())
    val composerState: StateFlow<LeagueComposerState> = _composerState.asStateFlow()

    init {
        loadLeagues()
    }

    fun loadLeagues() {
        viewModelScope.launch {
            _leaguesState.update { it.copy(isLoading = true) }
            runCatching { socialRepository.refreshLeagues() }
                .onFailure { e -> _leaguesState.update { it.copy(error = e.message, isLoading = false) } }
            socialRepository.observeLeagues().collect { leagues ->
                _leaguesState.update { it.copy(leagues = leagues, isLoading = false) }
            }
        }
    }

    fun loadLeagueHome(leagueId: String) {
        viewModelScope.launch {
            _leagueHomeState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val league = socialRepository.getLeague(leagueId)
                val standings = socialRepository.getLeagueStandings(leagueId)
                val submissions = socialRepository.getLeagueSubmissions(leagueId)
                // Attachments are host+entrant-gated and best-effort — a fetch
                // failure leaves the section empty without sinking the screen.
                val attachments = runCatching {
                    socialRepository.getLeagueAttachments(leagueId)
                }.getOrDefault(emptyList())
                _leagueHomeState.update {
                    it.copy(
                        league = league,
                        standings = standings,
                        mySubmissions = submissions,
                        attachments = attachments,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _leagueHomeState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    /** Reload just the attachments (after an add / delete). */
    private fun refreshAttachments(leagueId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.getLeagueAttachments(leagueId) }
                .onSuccess { list -> _leagueHomeState.update { it.copy(attachments = list) } }
        }
    }

    /**
     * Host-only: add an attachment. The repository validates the url/note
     * required for the chosen [kind]; on a validation or API failure the error
     * surfaces in [LeagueHomeUiState.attachmentError].
     */
    fun addAttachment(
        leagueId: String,
        kind: AttachmentKind,
        title: String,
        url: String?,
        note: String?,
        onAdded: () -> Unit,
    ) {
        viewModelScope.launch {
            _leagueHomeState.update { it.copy(attachmentError = null) }
            runCatching { socialRepository.addLeagueAttachment(leagueId, kind, title, url, note) }
                .onSuccess {
                    refreshAttachments(leagueId)
                    onAdded()
                }
                .onFailure { e ->
                    _leagueHomeState.update { it.copy(attachmentError = e.message) }
                }
        }
    }

    /** Host-only: remove an attachment. */
    fun deleteAttachment(leagueId: String, attachmentId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.deleteLeagueAttachment(leagueId, attachmentId) }
                .onSuccess {
                    _leagueHomeState.update { s ->
                        s.copy(attachments = s.attachments.filterNot { it.id == attachmentId })
                    }
                }
                .onFailure { e -> _leagueHomeState.update { it.copy(error = e.message) } }
        }
    }

    fun resetAttachmentError() {
        _leagueHomeState.update { it.copy(attachmentError = null) }
    }

    fun loadAdminMatrix(leagueId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.getLeagueAdminMatrix(leagueId) }
                .onSuccess { matrix -> _leagueHomeState.update { it.copy(adminMatrix = matrix) } }
                .onFailure { e -> _leagueHomeState.update { it.copy(error = e.message) } }
        }
    }

    fun setDivisionFilter(division: Division?) {
        _leagueHomeState.update { it.copy(selectedDivision = division) }
    }

    fun submitScore(leagueId: String, week: Int, rawScore: Int, xCount: Int, sessionId: String? = null) {
        viewModelScope.launch {
            runCatching {
                socialRepository.submitScore(leagueId, SubmitScoreBody(week, sessionId, rawScore, xCount))
            }.onSuccess { submission ->
                _leagueHomeState.update { s ->
                    s.copy(mySubmissions = s.mySubmissions + submission)
                }
            }.onFailure { e ->
                _leagueHomeState.update { it.copy(error = e.message) }
            }
        }
    }

    fun joinLeague(leagueId: String, division: Division) {
        viewModelScope.launch {
            runCatching { socialRepository.joinLeague(leagueId, division) }
                .onSuccess { l -> _leagueHomeState.update { it.copy(league = l) } }
                .onFailure { e -> _leagueHomeState.update { it.copy(error = e.message) } }
        }
    }

    fun leaveLeague(leagueId: String) {
        viewModelScope.launch {
            runCatching { socialRepository.leaveLeague(leagueId) }
                .onSuccess {
                    _leaguesState.update { s -> s.copy(leagues = s.leagues.filter { it.id != leagueId }) }
                }
                .onFailure { e -> _leaguesState.update { it.copy(error = e.message) } }
        }
    }

    // ── Composer ──────────────────────────────────────────────────────────────

    fun updateComposerName(name: String) { _composerState.update { it.copy(name = name) } }
    fun updateComposerType(type: LeagueType) { _composerState.update { it.copy(leagueType = type) } }
    fun updateComposerDivisions(divisions: List<Division>) { _composerState.update { it.copy(divisions = divisions) } }
    fun updateComposerEndCount(count: Int) { _composerState.update { it.copy(endCount = count) } }
    fun updateComposerArrowsPerEnd(count: Int) { _composerState.update { it.copy(arrowsPerEnd = count) } }
    fun updateComposerScheduleKind(kind: LeagueScheduleKind) { _composerState.update { it.copy(scheduleKind = kind) } }
    fun updateComposerTotalWeeks(weeks: Int) { _composerState.update { it.copy(totalWeeks = weeks) } }
    fun updateComposerEntryRule(rule: LeagueEntryRule) { _composerState.update { it.copy(entryRule = rule) } }
    fun updateComposerHandicap(config: HandicapConfig) { _composerState.update { it.copy(handicapConfig = config) } }

    fun createLeague(schedule: LeagueSchedule, onSuccess: (League) -> Unit) {
        val s = _composerState.value
        viewModelScope.launch {
            _composerState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                socialRepository.createLeague(
                    CreateLeagueBody(
                        name = s.name,
                        type = s.leagueType,
                        divisions = s.divisions,
                        round = RoundDef(s.endCount, s.arrowsPerEnd),
                        schedule = schedule,
                        handicap = s.handicapConfig,
                        entryRule = s.entryRule,
                    ),
                )
            }.onSuccess { league ->
                _composerState.update { it.copy(isSaving = false) }
                _leaguesState.update { st -> st.copy(leagues = st.leagues + league) }
                onSuccess(league)
            }.onFailure { e ->
                _composerState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    /**
     * Host-only: invite an archer to [leagueId] by `@handle` (§11). On success
     * flips [LeagueHomeUiState.inviteSent]; on failure surfaces [inviteError].
     */
    fun inviteToLeague(leagueId: String, handle: String) {
        viewModelScope.launch {
            _leagueHomeState.update { it.copy(inviteError = null) }
            runCatching { socialRepository.inviteToLeague(leagueId, handle) }
                .onSuccess { _leagueHomeState.update { it.copy(inviteSent = true) } }
                .onFailure { e -> _leagueHomeState.update { it.copy(inviteError = e.message) } }
        }
    }

    /** Reset the invite dialog state when it's dismissed. */
    fun resetInviteState() {
        _leagueHomeState.update { it.copy(inviteError = null, inviteSent = false) }
    }

    fun dismissError() {
        _leaguesState.update { it.copy(error = null) }
        _leagueHomeState.update { it.copy(error = null) }
        _composerState.update { it.copy(error = null) }
    }
}

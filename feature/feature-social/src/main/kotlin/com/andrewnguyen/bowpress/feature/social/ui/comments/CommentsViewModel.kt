package com.andrewnguyen.bowpress.feature.social.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.canDeleteComment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the comments thread (Social Feed V2 §5.7).
 *
 * [comments] is rendered oldest→newest. [callerUserId] is the signed-in user;
 * [subjectOwnerUserId] the §5.1 subject owner — together they decide, per
 * comment, whether the delete affordance shows (author OR post owner).
 *
 * [loadFailed] is true only when the *initial* thread fetch failed and there
 * is nothing to show — the screen then renders a retry state rather than the
 * "no comments yet" empty state, which would mislabel an unreachable thread as
 * empty.
 */
data class CommentsUiState(
    val comments: List<ActivityComment> = emptyList(),
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val callerUserId: String = "",
    val subjectOwnerUserId: String? = null,
) {
    /** Whether the signed-in caller may delete [comment] (author OR post owner). */
    fun canDelete(comment: ActivityComment): Boolean =
        canDeleteComment(comment, callerUserId, subjectOwnerUserId)
}

/** Body length bounds — mirrors the API contract §5.3 (1–1000 chars, trimmed). */
const val MAX_COMMENT_LEN = 1000

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState(isLoading = true))
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    private var subjectId: String = ""
    // Held so retry() can re-run the load without the screen re-passing args.
    private var subjectOwnerUserId: String? = null

    /**
     * Load the thread for [subjectId]. [subjectOwnerUserId] is forwarded from
     * the opening row so the delete gate works without an extra fetch; the
     * caller's own id is resolved from the cached social profile.
     */
    fun load(subjectId: String, subjectOwnerUserId: String?) {
        this.subjectId = subjectId
        this.subjectOwnerUserId = subjectOwnerUserId?.takeIf { it.isNotBlank() }
        runLoad()
    }

    /** Re-run the thread fetch — wired to the load-failure retry affordance. */
    fun retry() {
        if (subjectId.isBlank()) return
        runLoad()
    }

    private fun runLoad() {
        val ssId = subjectId
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadFailed = false,
                    error = null,
                    subjectOwnerUserId = subjectOwnerUserId,
                )
            }
            val callerId = runCatching { socialRepository.getMyProfile().userId }.getOrNull().orEmpty()
            runCatching { socialRepository.getActivityComments(ssId) }
                .onSuccess { serverComments ->
                    _uiState.update { state ->
                        // C1 — a slow load() whose server snapshot predates a
                        // post()/delete() must not blunt-overwrite the thread:
                        // merge the server list with whatever the user has
                        // already posted locally, de-dupe by id, re-sort.
                        val merged = (state.comments + serverComments)
                            .distinctBy { it.id }
                            .sortedBy { it.createdAt }
                        state.copy(
                            comments = merged,
                            isLoading = false,
                            loadFailed = false,
                            callerUserId = callerId,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            error = e.message,
                            isLoading = false,
                            // Only flag a hard failure when there is nothing to
                            // show — a refresh failure that still has comments
                            // keeps the thread visible with an inline error.
                            loadFailed = state.comments.isEmpty(),
                            callerUserId = callerId,
                        )
                    }
                }
        }
    }

    /**
     * Post [body] as a new comment. A blank trimmed body is ignored locally; an
     * over-long body is clipped to the contract bound before sending purely
     * defensively (the compose field already caps input). On success the new
     * comment is appended (the thread stays oldest→newest).
     */
    fun post(body: String) {
        val ssId = subjectId.ifBlank { return }
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        // Defensive only — CommentsScreen's field caps at MAX_COMMENT_LEN.
        val clipped = trimmed.take(MAX_COMMENT_LEN)
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }
            runCatching { socialRepository.postComment(ssId, clipped) }
                .onSuccess { created ->
                    _uiState.update { state ->
                        // De-dupe in case a concurrent load() already merged
                        // this comment in from the server.
                        val merged = (state.comments + created)
                            .distinctBy { it.id }
                            .sortedBy { it.createdAt }
                        state.copy(comments = merged, isPosting = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isPosting = false) }
                }
        }
    }

    /**
     * Delete [comment] — only attempted when [CommentsUiState.canDelete] is
     * true. On success the comment leaves the thread.
     */
    fun delete(comment: ActivityComment) {
        val ssId = subjectId.ifBlank { return }
        val canDelete = _uiState.value.canDelete(comment)
        if (!canDelete) {
            _uiState.update { it.copy(error = "You can't delete this comment.") }
            return
        }
        viewModelScope.launch {
            runCatching { socialRepository.deleteComment(ssId, comment.id, canDelete = true) }
                .onSuccess {
                    _uiState.update { s -> s.copy(comments = s.comments.filterNot { it.id == comment.id }) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

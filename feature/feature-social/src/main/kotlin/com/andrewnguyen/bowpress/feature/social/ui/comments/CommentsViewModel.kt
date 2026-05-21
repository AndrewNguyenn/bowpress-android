package com.andrewnguyen.bowpress.feature.social.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ActivityActor
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.CommentSort
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import com.andrewnguyen.bowpress.core.model.ToggleLikeResponse
import com.andrewnguyen.bowpress.core.model.canDeleteComment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The reply target the composer is addressed to — set when the user taps
 * **Reply** on a comment row, cleared once the reply posts or is dismissed.
 * [parentCommentId] is the *top-level* comment id the new reply hangs under
 * (the API normalises a reply-to-a-reply up to its top-level parent, §6.3);
 * [addresseeHandle] is the handle shown in the inline "Replying to @x" cue and
 * the `@mention` the body opens with.
 */
data class ReplyTarget(
    val parentCommentId: String,
    val addresseeHandle: String,
)

/**
 * The session-recap context strip shown above the thread (§6.7) — a
 * compressed re-cap of the card. Present only when the subject is a session
 * post whose detail resolved; a club/league subject (or a failed detail
 * fetch) renders no strip.
 */
data class CommentsContext(
    val authorDisplayName: String,
    val recapLine: String,        // "Sara Lin · 50m PR" — name + range/kind
    val telemetry: String,        // mono — "18 arr · 12 X · 2h ago"
    val score: Int,
    val maxScore: Int,
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val likers: List<ActivityActor> = emptyList(),
)

/**
 * State for the comments thread (Social Feed V2 §6.7).
 *
 * [comments] is the list of **top-level** comments in the active [sort] order,
 * each carrying its nested `replies`. [callerUserId] is the signed-in user;
 * [subjectOwnerUserId] the §5.1 subject owner — together they decide, per
 * comment (top-level or reply), whether the delete affordance shows (author
 * OR post owner). [subjectOwnerUserId] also stamps the maple AUTHOR badge on
 * any reply whose author is the session owner.
 *
 * [loadFailed] is true only when the *initial* thread fetch failed and there
 * is nothing to show — the screen then renders a retry state rather than the
 * empty state, which would mislabel an unreachable thread as empty.
 */
data class CommentsUiState(
    val comments: List<ActivityComment> = emptyList(),
    val sort: CommentSort = CommentSort.recent,
    val context: CommentsContext? = null,
    val replyTarget: ReplyTarget? = null,
    // The composer draft is owned here, not screen-local, so a failed reply
    // post keeps the typed text alongside its still-active reply context
    // (M2 — onFailure can't restore a screen-scoped draft).
    val draft: String = "",
    // Top-level comment ids whose full reply chain is expanded — a chain over
    // the collapse threshold otherwise shows only its first reply. Lifted here
    // so `post`'s onSuccess can auto-expand a parent it just replied into
    // (C2 — a reply into a collapsed thread must not vanish behind the stub).
    val expandedThreadIds: Set<String> = emptySet(),
    // Comment ids with a like-toggle in flight — a per-comment re-entry guard
    // so a double-tap on a heart can't fire racing like/unlike (C1).
    val likingCommentIds: Set<String> = emptySet(),
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

    /** Whether [comment]'s author is the session owner — drives the AUTHOR stamp. */
    fun isAuthorComment(comment: ActivityComment): Boolean =
        subjectOwnerUserId != null && comment.userId == subjectOwnerUserId

    /** Total thread size — top-level comments plus every nested reply. */
    val totalCount: Int
        get() = comments.sumOf { 1 + it.replies.size }

    /** Whether [topLevelId]'s reply chain is shown in full. */
    fun isThreadExpanded(topLevelId: String): Boolean = topLevelId in expandedThreadIds
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
    // Held so retry()/setSort() can re-run the load without re-passing args.
    private var subjectOwnerUserId: String? = null

    /**
     * Load the thread for [subjectId]. [subjectOwnerUserId] is forwarded from
     * the opening row so the delete gate + AUTHOR stamp work without an extra
     * fetch; the caller's own id is resolved from the cached social profile.
     */
    fun load(subjectId: String, subjectOwnerUserId: String?) {
        this.subjectId = subjectId
        this.subjectOwnerUserId = subjectOwnerUserId?.takeIf { it.isNotBlank() }
        runLoad()
        loadContext()
    }

    /** Re-run the thread fetch — wired to the load-failure retry affordance. */
    fun retry() {
        if (subjectId.isBlank()) return
        runLoad()
        loadContext()
    }

    /** Switch the thread sort (§6.3) — re-fetches in the new order. */
    fun setSort(sort: CommentSort) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        // A sort change replaces the thread wholesale — the ordering changed,
        // so there is nothing to merge against.
        runLoad(replaceThread = true)
    }

    /**
     * Run the thread fetch in the active sort.
     *
     * [replaceThread] true → the server snapshot replaces the thread wholesale
     * (a sort switch). False → the snapshot is merged with what is already
     * shown, de-duped by id, so a slow load can't blunt-overwrite a
     * just-posted comment (C1).
     */
    private fun runLoad(replaceThread: Boolean = false) {
        val ssId = subjectId
        val sort = _uiState.value.sort
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
            runCatching { socialRepository.getActivityComments(ssId, sort) }
                .onSuccess { serverComments ->
                    _uiState.update { state ->
                        val merged = if (replaceThread) {
                            serverComments
                        } else {
                            mergeThreads(state.comments, serverComments)
                        }
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
                            loadFailed = state.comments.isEmpty(),
                            callerUserId = callerId,
                        )
                    }
                }
        }
    }

    /**
     * Fetch the session-recap context strip. Best-effort — a club/league
     * subject or a failed detail fetch simply leaves the strip absent; the
     * thread still renders.
     */
    private fun loadContext() {
        val ssId = subjectId.ifBlank { return }
        viewModelScope.launch {
            runCatching { socialRepository.getSharedSessionDetail(ssId) }
                .onSuccess { detail -> _uiState.update { it.copy(context = detail.toContext()) } }
            // A failure (non-session subject, offline) is swallowed — the
            // context strip is decorative; the thread is the screen.
        }
    }

    /** Update the composer draft — owned here so a failed post can keep it. */
    fun updateDraft(text: String) {
        // Enforce the contract bound (§5.3) at the single source of truth.
        _uiState.update { it.copy(draft = text.take(MAX_COMMENT_LEN)) }
    }

    /**
     * Begin a reply addressed to [comment]. [comment] may be a top-level
     * comment or a reply — either way the new reply's parent is normalised to
     * the top-level comment id. When replying to a reply the parent is its
     * own `parentCommentId`; when replying to a top-level comment the parent
     * is the comment itself.
     *
     * The draft is rewritten so it opens with the addressee's `@mention` (the
     * contract carries the addressee in the body, §6.1) — a stale leading
     * `@oldhandle ` token from a previous target is replaced, otherwise the
     * mention is prepended to whatever the user has already typed (M1).
     */
    fun startReply(comment: ActivityComment) {
        val parentId = comment.parentCommentId?.takeIf { it.isNotBlank() } ?: comment.id
        val handle = comment.authorHandle
        _uiState.update { state ->
            state.copy(
                replyTarget = ReplyTarget(parentId, handle),
                draft = withMentionPrefix(state.draft, handle),
            )
        }
    }

    /**
     * Dismiss the active reply target — the next post is a top-level comment.
     * A stale leading `@mention ` token is stripped from the draft so it does
     * not address a comment the user is no longer replying to.
     */
    fun cancelReply() {
        _uiState.update { state ->
            state.copy(
                replyTarget = null,
                draft = stripLeadingMention(state.draft),
            )
        }
    }

    /** Expand [topLevelId]'s reply chain — wired to the "view N more" stub. */
    fun expandThread(topLevelId: String) {
        _uiState.update { it.copy(expandedThreadIds = it.expandedThreadIds + topLevelId) }
    }

    /**
     * Post the current [CommentsUiState.draft] — a top-level comment, or a
     * reply when a [ReplyTarget] is active. A blank trimmed draft is ignored.
     *
     * The draft is owned by the ViewModel: it is cleared (and the reply target
     * dropped) ONLY on success, so a failed post keeps the typed text together
     * with its still-active "Replying to @x" context for an immediate retry
     * (M2). On a reply success the parent's reply chain is auto-expanded so
     * the new reply is never hidden behind a "view N more replies" stub (C2).
     */
    fun post() {
        val ssId = subjectId.ifBlank { return }
        val trimmed = _uiState.value.draft.trim()
        if (trimmed.isEmpty()) return
        val target = _uiState.value.replyTarget
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }
            runCatching {
                socialRepository.postComment(ssId, trimmed, parentCommentId = target?.parentCommentId)
            }
                .onSuccess { created ->
                    _uiState.update { state ->
                        val parentId = created.parentCommentId?.takeIf { it.isNotBlank() }
                        val next = if (parentId == null) {
                            // A top-level comment — splice it in directly.
                            mergeThreads(state.comments, listOf(created))
                        } else {
                            // A reply — attach it under its top-level parent.
                            spliceReply(state.comments, created)
                        }
                        state.copy(
                            comments = next,
                            isPosting = false,
                            replyTarget = null,
                            // Cleared only on success.
                            draft = "",
                            // C2 — a reply into a (possibly collapsed) thread
                            // auto-expands that thread so it stays visible.
                            expandedThreadIds = if (parentId != null) {
                                state.expandedThreadIds + parentId
                            } else {
                                state.expandedThreadIds
                            },
                        )
                    }
                }
                .onFailure { e ->
                    // The draft + reply target survive — the user can retry.
                    _uiState.update { it.copy(error = e.message, isPosting = false) }
                }
        }
    }

    /**
     * Delete [comment] — a top-level comment or a reply. Only attempted when
     * [CommentsUiState.canDelete] is true. Deleting a top-level comment drops
     * its whole reply chain (the server cascades, §6.3).
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
                    _uiState.update { s -> s.copy(comments = removeComment(s.comments, comment.id)) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    /**
     * Toggle the caller's like on [comment] (§6.2) — a comment is a likeable
     * subject. Optimistically flips the heart + count, then reconciles against
     * the server; a failed toggle reverts.
     *
     * A per-comment in-flight guard ([CommentsUiState.likingCommentIds]) drops
     * a re-tap while a toggle is already running, so a double-tap can't fire
     * racing like/unlike requests and drift the count (C1).
     */
    fun toggleCommentLike(comment: ActivityComment) {
        // C1 — re-entry guard: ignore the tap if this comment is already
        // mid-toggle.
        if (comment.id in _uiState.value.likingCommentIds) return
        viewModelScope.launch {
            val wasLiked = comment.likedByMe
            // Optimistic flip + mark in flight.
            _uiState.update { s ->
                s.copy(
                    likingCommentIds = s.likingCommentIds + comment.id,
                    comments = updateComment(s.comments, comment.id) { c ->
                        c.copy(
                            likedByMe = !wasLiked,
                            likeCount = (c.likeCount + if (wasLiked) -1 else 1).coerceAtLeast(0),
                        )
                    },
                )
            }
            try {
                runCatching { socialRepository.toggleCommentLike(comment.id, wasLiked) }
                    .onSuccess { result: ToggleLikeResponse ->
                        _uiState.update { s ->
                            s.copy(
                                comments = updateComment(s.comments, comment.id) { c ->
                                    c.copy(likedByMe = result.likedByMe, likeCount = result.likeCount)
                                },
                            )
                        }
                    }
                    .onFailure {
                        // Revert the optimistic flip.
                        _uiState.update { s ->
                            s.copy(
                                comments = updateComment(s.comments, comment.id) { c ->
                                    c.copy(likedByMe = wasLiked, likeCount = comment.likeCount)
                                },
                            )
                        }
                    }
            } finally {
                // Clear the in-flight guard whatever the outcome.
                _uiState.update { s ->
                    s.copy(likingCommentIds = s.likingCommentIds - comment.id)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

// ── Composer mention helpers ─────────────────────────────────────────────────
//
// A reply carries its addressee in the body as a leading `@handle ` token
// (contract §6.1) — the API normalises a reply-to-a-reply up to its top-level
// parent and the mention is the only thing that preserves who was addressed.
// These pure helpers keep the draft's leading mention in sync with the active
// reply target (M1).

/** A leading `@handle ` token at the start of [draft], if present. */
private val LEADING_MENTION = Regex("""^@\S+\s+""")

/**
 * Rewrite [draft] so it opens with `@[handle] `. A stale leading mention from
 * a previous reply target is replaced; otherwise the mention is prepended to
 * whatever the user has already typed (so tapping Reply with text in flight
 * keeps that text and still addresses the new target).
 */
internal fun withMentionPrefix(draft: String, handle: String): String {
    val mention = "@$handle "
    val body = draft.replaceFirst(LEADING_MENTION, "")
    return mention + body
}

/** Strip a leading `@handle ` token from [draft] — used when a reply is cancelled. */
internal fun stripLeadingMention(draft: String): String =
    draft.replaceFirst(LEADING_MENTION, "")

// ── Thread merge helpers ─────────────────────────────────────────────────────
//
// The thread is a list of top-level comments, each with a nested `replies`
// list. These pure helpers keep the merge / splice / mutate logic testable.

/**
 * Merge a [fresh] server snapshot into the [current] thread, de-duping by id.
 * The server is authoritative on top-level ordering and reply contents — a
 * fresh row replaces the current one — but a comment present only locally
 * (a just-posted one a slow load predates) is preserved (C1).
 */
internal fun mergeThreads(
    current: List<ActivityComment>,
    fresh: List<ActivityComment>,
): List<ActivityComment> {
    val freshById = fresh.associateBy { it.id }
    val localOnly = current.filter { it.id !in freshById }
    // Server order first, then any local-only comments appended.
    return fresh + localOnly
}

/** Attach [reply] under its top-level parent, keeping replies oldest→newest. */
internal fun spliceReply(
    comments: List<ActivityComment>,
    reply: ActivityComment,
): List<ActivityComment> {
    val parentId = reply.parentCommentId ?: return comments
    return comments.map { top ->
        if (top.id != parentId) {
            top
        } else {
            val nextReplies = (top.replies.filterNot { it.id == reply.id } + reply)
                .sortedBy { it.createdAt }
            top.copy(replies = nextReplies, replyCount = nextReplies.size)
        }
    }
}

/**
 * Remove the comment with [commentId] — a top-level comment (drops its whole
 * reply chain) or a single reply.
 */
internal fun removeComment(
    comments: List<ActivityComment>,
    commentId: String,
): List<ActivityComment> =
    comments
        .filterNot { it.id == commentId }
        .map { top ->
            val keptReplies = top.replies.filterNot { it.id == commentId }
            if (keptReplies.size == top.replies.size) {
                top
            } else {
                top.copy(replies = keptReplies, replyCount = keptReplies.size)
            }
        }

/** Apply [transform] to the comment with [commentId], top-level or reply. */
internal fun updateComment(
    comments: List<ActivityComment>,
    commentId: String,
    transform: (ActivityComment) -> ActivityComment,
): List<ActivityComment> =
    comments.map { top ->
        when {
            top.id == commentId -> transform(top)
            top.replies.any { it.id == commentId } ->
                top.copy(replies = top.replies.map { if (it.id == commentId) transform(it) else it })
            else -> top
        }
    }

// ── Context strip ────────────────────────────────────────────────────────────

/** Build the §6.7 session-recap context strip from a [SharedSessionDetail]. */
private fun SharedSessionDetail.toContext(): CommentsContext {
    val s = sharedSession
    val range = s.distance?.trim()?.takeIf { it.isNotEmpty() }
    // "Sara Lin · 50m" — name then the range. PR detection lives server-side
    // and is not on the detail payload, so the recap stays name · range.
    val recap = buildString {
        append(ownerDisplayName)
        if (range != null) {
            append(" · ")
            append(range)
        }
    }
    val telemetry = buildString {
        append("${s.arrowCount} arr")
        if (s.xCount > 0) append(" · ${s.xCount} X")
        append(" · ${com.andrewnguyen.bowpress.feature.social.ui.socialRelativeTime(s.shotAt)}")
    }
    return CommentsContext(
        authorDisplayName = ownerDisplayName,
        recapLine = recap,
        telemetry = telemetry,
        score = s.score,
        maxScore = s.arrowCount * 10,
        likeCount = likeCount,
        likedByMe = likedByMe,
        likers = likers,
    )
}

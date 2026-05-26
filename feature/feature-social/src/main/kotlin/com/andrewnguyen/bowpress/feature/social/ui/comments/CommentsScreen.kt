package com.andrewnguyen.bowpress.feature.social.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppDeep
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.CommentSort
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatarImage
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.feed.KudosRow
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionBodyText
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionListPlacement
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionResolverViewModel
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionTextField
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The comment thread for a feed subject — the Section 03 "Thread states"
 * design (Social Feed V2 §6.7).
 *
 * Back bar → session-recap context strip → read-only kudos summary → thread
 * eyebrow with a recent/top sort toggle → comment rows (each with a comment
 * like + Reply action, and a hairline-gutter reply chain) → composer. A
 * comment is deletable by its author **or** the post owner.
 */
@Composable
fun CommentsScreen(
    subjectId: String,
    subjectOwnerUserId: String?,
    onBack: () -> Unit,
    // Mentions contract §3.2 — tapping an `@handle` in a comment body opens
    // that archer's profile. Defaulted to a no-op so existing callers / tests
    // that don't wire mention navigation still compile.
    onOpenArcher: (userId: String) -> Unit = {},
    viewModel: CommentsViewModel = hiltViewModel(),
    mentionResolver: MentionResolverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(subjectId, subjectOwnerUserId) {
        viewModel.load(subjectId, subjectOwnerUserId)
    }

    // A tapped mention resolves its handle → archer profile (§3.2).
    val onMentionTap: (String) -> Unit = { handle ->
        mentionResolver.openMention(handle, onOpenArcher)
    }

    // The composer draft is ViewModel-owned (M2): a failed post keeps the
    // typed text together with its still-active reply context. The mention
    // prefill is handled in the ViewModel's startReply/cancelReply (M1).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.CommentsRoot),
    ) {
        // ── Back bar ──
        // The session author's initials pin to the right of the bar — only
        // when the context strip resolved, which is the only place the
        // author's display name is known (the route carries an id, not a
        // name). A non-session subject simply shows the count alone.
        BackBar(
            count = state.totalCount,
            authorInitials = state.context?.authorDisplayName
                ?.let { avatarInitials(it) }
                .orEmpty(),
            onBack = onBack,
        )
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // ── Context strip — the session recap, when resolved ──
                state.context?.let { ctx ->
                    item {
                        ContextStrip(ctx)
                        HorizontalDivider(color = AppLine, thickness = 1.dp)
                        // Read-only kudos summary.
                        KudosSummary(ctx)
                    }
                }

                // ── Thread eyebrow + sort toggle ──
                item {
                    ThreadEyebrow(
                        count = state.totalCount,
                        sort = state.sort,
                        onSort = viewModel::setSort,
                    )
                }

                // ── Thread body ──
                when {
                    state.isLoading && state.comments.isEmpty() -> item {
                        Text(
                            "Loading…",
                            style = frauncesDisplay(14.sp),
                            color = AppInk3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }

                    // A failed initial fetch with nothing to show — a distinct
                    // retry state, NOT the empty "be the first" state.
                    state.loadFailed -> item {
                        LoadFailedState(onRetry = viewModel::retry)
                    }

                    state.comments.isEmpty() -> item {
                        EmptyThreadState(authorName = state.context?.authorDisplayName)
                    }

                    else -> items(state.comments, key = { it.id }) { comment ->
                        CommentThread(
                            comment = comment,
                            state = state,
                            onLike = viewModel::toggleCommentLike,
                            onReply = viewModel::startReply,
                            onDelete = viewModel::delete,
                            onExpandReplies = viewModel::expandThread,
                            onMentionTap = onMentionTap,
                        )
                    }
                }
            }
        }

        // Inline error.
        state.error?.let { error ->
            Text(
                text = error,
                style = jetbrainsMono(9.5.sp),
                color = AppMaple,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
                    .clickable { viewModel.dismissError() },
            )
        }

        // ── Composer ──
        Composer(
            draft = state.draft,
            onDraftChange = viewModel::updateDraft,
            replyAddressee = state.replyTarget?.addresseeHandle,
            onCancelReply = viewModel::cancelReply,
            canSend = state.draft.trim().isNotEmpty() && !state.isPosting && !state.isLoading,
            onSend = viewModel::post,
            onSearchHandles = viewModel::searchHandles,
        )
    }
}

// ── Back bar ─────────────────────────────────────────────────────────────────

/** ‹ · "Comments" · count · author initials — the §6.7 back bar. */
@Composable
private fun BackBar(count: Int, authorInitials: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹",
            style = frauncesDisplay(24.sp, italic = true),
            color = AppPondDk,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 10.dp),
        )
        Text(
            "Comments",
            style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium),
            color = AppInk,
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$count",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
            )
            if (authorInitials.isNotBlank()) {
                Text(
                    "  ·  ",
                    style = interUI(9.sp, FontWeight.SemiBold),
                    color = AppInk3,
                )
                Text(
                    text = authorInitials.uppercase(),
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPondDk,
                )
            }
        }
    }
}

// ── Context strip ────────────────────────────────────────────────────────────

/** The session-recap strip — 36dp avatar, name·range, mono telemetry, 24dp score. */
@Composable
private fun ContextStrip(ctx: CommentsContext) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatarImage(
            displayName = ctx.authorDisplayName,
            avatarUrl = ctx.authorAvatarUrl,
            avatarVersion = ctx.authorAvatarVersion,
            size = 36,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = ctx.recapLine,
                style = frauncesDisplay(14.5.sp, italic = true, weight = FontWeight.Medium),
                color = AppInk,
            )
            Text(
                text = ctx.telemetry,
                style = jetbrainsMono(9.sp),
                color = AppInk3,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        // Score — upright Fraunces numeral in --deep, quiet "/ max" under it.
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${ctx.score}",
                style = frauncesDisplay(24.sp, italic = false, weight = FontWeight.Medium),
                color = AppDeep,
            )
            if (ctx.maxScore >= ctx.score && ctx.maxScore > 0) {
                Text(
                    text = "/ ${ctx.maxScore}",
                    style = frauncesDisplay(11.sp, italic = false, weight = FontWeight.Medium),
                    color = AppInk3,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** Read-only kudos summary — the kudos stack with no tappable actions (§6.7). */
@Composable
private fun KudosSummary(ctx: CommentsContext) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KudosRow(
            likers = ctx.likers,
            likeCount = ctx.likeCount,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(10.dp))
        // The like state shown read-only — the thread is reached from a card
        // that already owns the like toggle.
        Icon(
            imageVector = if (ctx.likedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = null,
            tint = if (ctx.likedByMe) AppPine else AppInk3,
            modifier = Modifier.size(15.dp),
        )
    }
}

// ── Thread eyebrow + sort ────────────────────────────────────────────────────

/** "THREAD · N comments" with the recent/top sort toggle (§6.3). */
@Composable
private fun ThreadEyebrow(count: Int, sort: CommentSort, onSort: (CommentSort) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row {
            Text(
                "THREAD",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
            )
            Text(
                text = "  ·  ${commentCountLabel(count)}",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
        }
        Spacer(Modifier.weight(1f))
        SortOption("recent", CommentSort.recent, sort, onSort, TestTags.CommentsSortRecent)
        Spacer(Modifier.width(14.dp))
        SortOption("top", CommentSort.top, sort, onSort, TestTags.CommentsSortTop)
    }
}

@Composable
private fun SortOption(
    label: String,
    value: CommentSort,
    selected: CommentSort,
    onSort: (CommentSort) -> Unit,
    testTag: String,
) {
    val on = value == selected
    Text(
        text = if (on) "$label ›" else label,
        style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.18.em),
        color = if (on) AppPondDk else AppInk3,
        modifier = Modifier
            .clickable { onSort(value) }
            .testTag(testTag),
    )
}

// ── Comment thread (a top-level comment + its reply chain) ───────────────────

@Composable
private fun CommentThread(
    comment: ActivityComment,
    state: CommentsUiState,
    onLike: (ActivityComment) -> Unit,
    onReply: (ActivityComment) -> Unit,
    onDelete: (ActivityComment) -> Unit,
    onExpandReplies: (String) -> Unit,
    onMentionTap: (String) -> Unit,
) {
    // A long reply chain collapses to the first reply behind a "view N more
    // replies" expander (§6.7). The expanded set lives in the ViewModel so a
    // reply posted into a collapsed thread auto-expands it (C2).
    val replies = comment.replies
    val collapsed = replies.size > REPLY_COLLAPSE_THRESHOLD &&
        !state.isThreadExpanded(comment.id)
    val shownReplies = if (collapsed) replies.take(1) else replies

    Column(modifier = Modifier.fillMaxWidth()) {
        CommentRow(
            comment = comment,
            isReply = false,
            isAuthor = state.isAuthorComment(comment),
            canDelete = state.canDelete(comment),
            onLike = { onLike(comment) },
            onReply = { onReply(comment) },
            onDelete = { onDelete(comment) },
            onMentionTap = onMentionTap,
        )
        // Reply chain — indented 36dp with a leading hairline gutter. The
        // gutter is painted with drawBehind so it spans exactly the measured
        // height of the replies Column, not a per-reply guess (m3).
        if (shownReplies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val x = 30.dp.toPx()
                        drawLine(
                            color = replyGutterColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(start = 36.dp),
            ) {
                shownReplies.forEach { reply ->
                    CommentRow(
                        comment = reply,
                        isReply = true,
                        isAuthor = state.isAuthorComment(reply),
                        canDelete = state.canDelete(reply),
                        onLike = { onLike(reply) },
                        onReply = { onReply(reply) },
                        onDelete = { onDelete(reply) },
                        onMentionTap = onMentionTap,
                    )
                }
            }
        }
        // "view N more replies" expander.
        if (collapsed) {
            Row(
                modifier = Modifier
                    .padding(start = 36.dp, top = 6.dp, bottom = 10.dp)
                    .clickable { onExpandReplies(comment.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(1.dp)
                        .background(AppPondDk),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "view ${replies.size - 1} more replies",
                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.18.em),
                    color = AppPondDk,
                )
            }
        }
        HorizontalDivider(color = AppLine2, thickness = 1.dp)
    }
}

/** Replies beyond this count collapse behind a "view N more" expander. */
private const val REPLY_COLLAPSE_THRESHOLD = 2

/** The reply-chain gutter hairline colour — captured as a plain value so it
 * can be read inside a `drawBehind` DrawScope (no Composable context there). */
private val replyGutterColor = AppLine

/**
 * One comment row — avatar, italic name, mono handle + time, italic body, a
 * comment LIKE action with its count, and a REPLY action. A [isReply] row
 * uses the smaller 22dp avatar and 13sp body; an [isAuthor] row carries the
 * maple AUTHOR stamp.
 */
@Composable
private fun CommentRow(
    comment: ActivityComment,
    isReply: Boolean,
    isAuthor: Boolean,
    canDelete: Boolean,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onMentionTap: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 0.dp else 18.dp,
                end = 18.dp,
                top = if (isReply) 11.dp else 13.dp,
                bottom = if (isReply) 11.dp else 13.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        SocialAvatarImage(
            displayName = comment.authorDisplayName,
            avatarUrl = comment.authorAvatarUrl,
            avatarVersion = comment.authorAvatarVersion,
            size = if (isReply) 22 else 26,
        )
        Spacer(Modifier.width(if (isReply) 8.dp else 10.dp))
        Column(Modifier.weight(1f)) {
            // Head — name, AUTHOR/handle, time.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorDisplayName,
                    style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Medium),
                    color = AppInk,
                )
                Spacer(Modifier.width(8.dp))
                if (isAuthor) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, AppMaple)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            "AUTHOR",
                            style = interUI(7.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                            color = AppMaple,
                        )
                    }
                } else {
                    Text(
                        text = "@${comment.authorHandle}",
                        style = jetbrainsMono(9.sp),
                        color = AppInk3,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = relativeStamp(comment.createdAt),
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
            }
            // Body — `@handle` mentions render as pond-toned, tappable spans
            // (mentions contract §3.2).
            MentionBodyText(
                text = comment.body,
                style = frauncesDisplay(if (isReply) 13.sp else 14.sp, italic = true, weight = FontWeight.Normal)
                    .copy(color = AppInk),
                onMentionTap = onMentionTap,
                modifier = Modifier.padding(top = 5.dp),
            )
            // Actions — LIKE (count) · REPLY · (Delete).
            Row(
                modifier = Modifier.padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommentAction(
                    label = comment.likeCount.takeIf { it > 0 }?.toString(),
                    icon = if (comment.likedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = if (comment.likedByMe) "Unlike comment" else "Like comment",
                    tint = if (comment.likedByMe) AppPine else AppInk3,
                    testTag = TestTags.CommentRowLikeButton,
                    onClick = onLike,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "REPLY",
                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppInk3,
                    modifier = Modifier
                        .clickable(onClick = onReply)
                        .testTag(TestTags.CommentRowReplyButton),
                )
                if (canDelete) {
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "DELETE",
                        style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppMaple,
                        modifier = Modifier.clickable(onClick = onDelete),
                    )
                }
            }
        }
    }
}

/** One comment action — an icon, optional mono count, tinted by state. */
@Composable
private fun CommentAction(
    label: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(10.dp),
        )
        if (label != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = jetbrainsMono(9.sp),
                color = tint,
            )
        }
    }
}

// ── Empty / failed states ────────────────────────────────────────────────────

/** "Nothing said yet." — the §6.7 empty state. */
@Composable
private fun EmptyThreadState(authorName: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 38.dp, bottom = 38.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, AppInk3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = AppInk3,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Nothing said yet.",
            style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium),
            color = AppInk,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = authorName
                ?.let { "A perfect end deserves a witness. Be the first to congratulate ${it.substringBefore(' ')}." }
                ?: "Be the first to comment.",
            style = frauncesDisplay(13.sp, italic = true, weight = FontWeight.Normal),
            color = AppInk2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** The initial-load-failure retry panel. */
@Composable
private fun LoadFailedState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 32.dp, end = 32.dp)
            .clickable(onClick = onRetry),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⟳", style = frauncesDisplay(30.sp), color = AppMaple)
        Spacer(Modifier.height(10.dp))
        Text(
            "Couldn't load comments.",
            style = frauncesDisplay(15.sp),
            color = AppInk,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap to retry.",
            style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.16.em),
            color = AppPondDk,
        )
    }
}

// ── Composer ─────────────────────────────────────────────────────────────────

/**
 * The compose row — pond-dk avatar, the input, and a POST stamp that activates
 * from ink-3 to pond-dk when typing. When a reply target is set an inline
 * "Replying to @x" cue shows above the input.
 */
@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    replyAddressee: String?,
    onCancelReply: () -> Unit,
    canSend: Boolean,
    onSend: () -> Unit,
    onSearchHandles: suspend (String) -> List<com.andrewnguyen.bowpress.core.model.HandleSuggestion>,
) {
    Column {
        HorizontalDivider(color = AppLine, thickness = 1.dp)
        // Inline reply context.
        if (replyAddressee != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppPaper2)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Replying to ",
                    style = frauncesDisplay(11.5.sp, italic = true, weight = FontWeight.Normal),
                    color = AppInk3,
                )
                Text(
                    text = "@$replyAddressee",
                    style = jetbrainsMono(10.sp, FontWeight.Medium),
                    color = AppPondDk,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "✕",
                    style = interUI(11.sp, FontWeight.SemiBold),
                    color = AppInk3,
                    modifier = Modifier.clickable(onClick = onCancelReply),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPaper2)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Your pond-dk avatar.
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(AppPondDk),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "—",
                    style = frauncesDisplay(10.5.sp, italic = true, weight = FontWeight.Medium),
                    color = AppPaper,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                // The input + `@`-autocomplete (mentions contract §3.1). The
                // suggestion list anchors *above* the bordered input box —
                // the composer sits at the bottom of the screen.
                MentionTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    onSearch = onSearchHandles,
                    textStyle = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Normal)
                        .copy(color = AppInk),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    listPlacement = MentionListPlacement.Above,
                    fieldModifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.CommentsComposeField),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppLine)
                                .background(AppPaper)
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                        ) {
                            if (draft.isEmpty()) {
                                Text(
                                    if (replyAddressee != null) "Write a reply…" else "Say something kind…",
                                    style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Normal),
                                    color = AppInk3,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                // M3 — a visible character counter so the MAX_COMMENT_LEN cap
                // is not a silent truncation; it tints maple as the draft
                // nears (≥90%) or hits the cap.
                if (draft.isNotEmpty()) {
                    val nearCap = draft.length >= MAX_COMMENT_LEN * 9 / 10
                    Text(
                        text = "${draft.length}/$MAX_COMMENT_LEN",
                        style = jetbrainsMono(8.5.sp),
                        color = if (nearCap) AppMaple else AppInk3,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "POST",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = if (canSend) AppPondDk else AppInk3,
                modifier = Modifier
                    .border(1.dp, if (canSend) AppPondDk else AppLine)
                    .clickable(enabled = canSend, onClick = onSend)
                    .padding(horizontal = 11.dp, vertical = 9.dp)
                    .testTag(TestTags.CommentsSendButton),
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** "no comments yet" / "1 comment" / "N comments". */
internal fun commentCountLabel(count: Int): String = when (count) {
    0 -> "no comments yet"
    1 -> "1 comment"
    else -> "$count comments"
}

/** Compact relative time — "just now", "2h", "3d", "2w". */
private fun relativeStamp(from: Instant): String {
    val mins = ChronoUnit.MINUTES.between(from, Instant.now()).coerceAtLeast(0)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m"
        mins < 60 * 24 -> "${mins / 60}h"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)}d"
        else -> "${mins / (60 * 24 * 7)}w"
    }
}

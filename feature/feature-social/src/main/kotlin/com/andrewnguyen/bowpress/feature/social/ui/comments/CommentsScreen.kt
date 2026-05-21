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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The comments thread for a feed subject (Social Feed V2 §5.7).
 *
 * Renders the thread oldest→newest with a sticky compose field. A comment is
 * deletable by its author **or** the post owner — the trash affordance shows
 * only on rows [CommentsViewModel] cleared.
 */
@Composable
fun CommentsScreen(
    subjectId: String,
    subjectOwnerUserId: String?,
    onBack: () -> Unit,
    viewModel: CommentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(subjectId, subjectOwnerUserId) {
        viewModel.load(subjectId, subjectOwnerUserId)
    }

    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.CommentsRoot),
    ) {
        // Top nav
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
        ) {
            Text(
                "‹  Back",
                style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                color = AppPondDk,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Text(
                "Comments",
                style = frauncesDisplay(28.sp),
                color = AppInk,
            )
            Text(
                text = when (val n = state.comments.size) {
                    0 -> "no comments yet"
                    1 -> "1 comment"
                    else -> "$n comments"
                },
                style = jetbrainsMono(10.sp),
                color = AppInk3,
            )
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading && state.comments.isEmpty() -> {
                    Text(
                        "Loading…",
                        style = frauncesDisplay(14.sp),
                        color = AppInk3,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp),
                    )
                }

                // C2 — the initial fetch failed with nothing to show: a
                // distinct retry state, NOT the "no comments yet" empty
                // state (which would mislabel an unreachable thread as empty).
                state.loadFailed -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp, start = 32.dp, end = 32.dp)
                            .clickable { viewModel.retry() },
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
                            style = interUI(10.5.sp, FontWeight.SemiBold)
                                .copy(letterSpacing = 0.16.em),
                            color = AppPondDk,
                        )
                    }
                }

                state.comments.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("○", style = frauncesDisplay(30.sp), color = AppInk3)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Be the first to comment.",
                            style = frauncesDisplay(15.sp),
                            color = AppInk,
                        )
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.comments, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                canDelete = state.canDelete(comment),
                                onDelete = { viewModel.delete(comment) },
                            )
                            HorizontalDivider(color = AppLine2, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Inline error
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

        // Compose field
        HorizontalDivider(color = AppLine, thickness = 1.dp)
        // The compose field stays out of the way until the thread is
        // resolved or has failed loading (C1 — Send is also gated below).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPaper2)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppLine)
                        .background(AppPaper)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                ) {
                    if (draft.isEmpty()) {
                        Text(
                            "Add a comment…",
                            style = frauncesDisplay(14.sp),
                            color = AppInk3,
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it.take(MAX_COMMENT_LEN) },
                        textStyle = frauncesDisplay(14.sp).copy(color = AppInk),
                        cursorBrush = SolidColor(AppPondDk),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.CommentsComposeField),
                    )
                }
                // M2 — a character counter so the MAX_COMMENT_LEN cap is
                // visible rather than a silent truncation. It tints maple as
                // the draft nears (≥90%) or hits the cap.
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
            // C1 — Send is gated on !isLoading too, so a post can't race an
            // in-flight initial load (whose merge would otherwise be the only
            // thing keeping the just-posted comment).
            val canSend = draft.trim().isNotEmpty() &&
                !state.isPosting &&
                !state.isLoading
            Text(
                text = "SEND",
                style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = if (canSend) AppPondDk else AppInk3,
                modifier = Modifier
                    .border(1.dp, if (canSend) AppPondDk else AppLine)
                    .clickable(enabled = canSend) {
                        viewModel.post(draft)
                        draft = ""
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag(TestTags.CommentsSendButton),
            )
        }
    }
}

/** One comment — author, relative time, body, and (when permitted) a delete affordance. */
@Composable
private fun CommentRow(
    comment: ActivityComment,
    canDelete: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SocialAvatar(initials = avatarInitials(comment.authorDisplayName), size = 30)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorDisplayName,
                    style = frauncesDisplay(14.sp),
                    color = AppInk,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "@${comment.authorHandle}",
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "· ${relativeStamp(comment.createdAt)}",
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = comment.body,
                style = frauncesDisplay(13.5.sp),
                color = AppInk,
            )
        }
        if (canDelete) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Delete",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.16.em),
                color = AppMaple,
                modifier = Modifier.clickable(onClick = onDelete),
            )
        }
    }
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

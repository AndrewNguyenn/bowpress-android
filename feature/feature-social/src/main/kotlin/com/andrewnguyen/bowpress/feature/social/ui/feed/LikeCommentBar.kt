package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ToggleLikeResponse
import kotlinx.coroutines.launch

/**
 * The like + comment action bar shown on a feed row (Social Feed V2 §5.7) —
 * an [ActivityItem] overload that resolves the §5.1 subject id and seeds the
 * counts from the row.
 */
@Composable
fun LikeCommentBar(
    item: ActivityItem,
    onToggleLike: suspend (subjectId: String, currentlyLiked: Boolean) -> ToggleLikeResponse,
    onOpenComments: (subjectId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LikeCommentBar(
        subjectId = item.resolvedSubjectId,
        likeCount = item.likeCount,
        likedByMe = item.likedByMe,
        commentCount = item.commentCount,
        // Re-seed key — a feed refresh carries fresh server counts.
        seedKey = "${item.id}:${item.likeCount}:${item.likedByMe}:${item.commentCount}",
        onToggleLike = onToggleLike,
        onOpenComments = onOpenComments,
        modifier = modifier,
    )
}

/**
 * The like + comment action bar (Social Feed V2 §5.7) — shown on a feed row
 * and the session-detail screen.
 *
 * The like button toggles **optimistically**: the heart fills and the count
 * adjusts the instant it is tapped, then [onToggleLike] (the repository call)
 * reconciles against the server's authoritative `{ likeCount, likedByMe }`. A
 * failed toggle reverts. The comment button shows [commentCount] and opens the
 * thread via [onOpenComments].
 *
 * Optimistic state is re-seeded whenever [seedKey] changes — pass a key that
 * folds in the server-authoritative counts so a refresh re-seeds the row.
 */
@Composable
fun LikeCommentBar(
    subjectId: String,
    likeCount: Int,
    likedByMe: Boolean,
    commentCount: Int,
    seedKey: Any,
    onToggleLike: suspend (subjectId: String, currentlyLiked: Boolean) -> ToggleLikeResponse,
    onOpenComments: (subjectId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Optimistic local state — re-seeded whenever the underlying row changes.
    var liked by remember(seedKey) { mutableStateOf(likedByMe) }
    var likeCountState by remember(seedKey) { mutableIntStateOf(likeCount) }
    var inFlight by remember(seedKey) { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Like ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    if (inFlight) return@clickable
                    val wasLiked = liked
                    val priorCount = likeCountState
                    // Optimistic flip.
                    liked = !wasLiked
                    likeCountState = (priorCount + if (wasLiked) -1 else 1).coerceAtLeast(0)
                    inFlight = true
                    scope.launch {
                        runCatching { onToggleLike(subjectId, wasLiked) }
                            .onSuccess { result ->
                                likeCountState = result.likeCount
                                liked = result.likedByMe
                            }
                            .onFailure {
                                // Revert the optimistic flip.
                                liked = wasLiked
                                likeCountState = priorCount
                            }
                        inFlight = false
                    }
                }
                .testTag(TestTags.FeedRowLikeButton)
                .padding(vertical = 4.dp, horizontal = 2.dp),
        ) {
            Icon(
                imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (liked) "Unlike" else "Like",
                tint = if (liked) AppMaple else AppInk3,
                modifier = Modifier.size(15.dp),
            )
            if (likeCountState > 0) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$likeCountState",
                    style = interUI(10.5.sp, FontWeight.SemiBold),
                    color = if (liked) AppMaple else AppInk3,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // ── Comment ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onOpenComments(subjectId) }
                .testTag(TestTags.FeedRowCommentButton)
                .padding(vertical = 4.dp, horizontal = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Comments",
                tint = AppInk3,
                modifier = Modifier.size(15.dp),
            )
            if (commentCount > 0) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$commentCount",
                    style = interUI(10.5.sp, FontWeight.SemiBold),
                    color = AppPondDk,
                )
            }
        }
    }
}

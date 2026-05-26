package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ActivityActor
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatarImage

// ── KudosRow ─────────────────────────────────────────────────────────────────
//
// The Strava-style kudos stack — Section 03's `.act-react .kudos`. A left
// stack of up to 3 overlapping 22dp initial-avatars, then a `+N` chip when the
// total runs ahead of the stack, then a single-line "Name & N others" summary
// (a quiet "Be the first" when nothing has landed yet). Used on the feed card
// and as the read-only reaction summary on the Comments screen.

/**
 * A single 22dp overlapping avatar in the kudos stack. Parity E5 — renders
 * the actor's uploaded photo via Coil when [actor].avatarUrl is present,
 * else falls back to the monogram initials.
 */
@Composable
private fun KudosAvatar(actor: ActivityActor) {
    SocialAvatarImage(
        displayName = actor.displayName,
        avatarUrl = actor.avatarUrl,
        avatarVersion = actor.avatarVersion,
        size = 22,
        borderTint = AppPondDk,
    )
}

/** The `+N` chip terminating the stack — mono, ink-3 outline, paper ground. */
@Composable
private fun KudosMoreChip(extra: Int) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(1.dp, AppInk3)
            .background(AppPaper),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$extra",
            style = jetbrainsMono(8.5.sp, weight = FontWeight.Medium),
            color = AppInk3,
        )
    }
}

/**
 * The kudos stack + summary line — the left half of the reactions row.
 *
 * [likers] is the ≤3 most-recent likers for the avatar stack; [likeCount] is
 * the true total. The stack shows [likers] avatars, then a `+N` chip when
 * `likeCount` exceeds the stack, then a one-line "Name & N others" summary.
 * When [likeCount] is 0 a quiet "Be the first" placeholder shows instead.
 */
@Composable
fun KudosRow(
    likers: List<ActivityActor>,
    likeCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "Be the first" shows ONLY when nothing has landed — a positive
        // `likeCount` never renders the empty invitation (M4), even if no
        // liker avatars are known (a count-only fallback then shows).
        if (likeCount <= 0) {
            Text(
                text = "Be the first",
                style = frauncesDisplay(12.5.sp, italic = true, weight = FontWeight.Normal),
                color = AppInk3,
            )
            return@Row
        }
        // A positive count with no known liker avatars — show the count alone
        // (e.g. an optimistic self-like with no self actor passed in).
        if (likers.isEmpty()) {
            Text(
                text = if (likeCount == 1) "1 like" else "$likeCount likes",
                style = frauncesDisplay(12.5.sp, italic = true, weight = FontWeight.Normal),
                color = AppInk2,
            )
            return@Row
        }

        // Up to 3 overlapping avatars, then the +N chip when the count runs
        // ahead. Negative offset gives the −5dp overlap of the design's
        // `.k-av { margin-right:-5px }`.
        val stack = likers.take(3)
        val extra = (likeCount - stack.size).coerceAtLeast(0)
        Row(verticalAlignment = Alignment.CenterVertically) {
            stack.forEachIndexed { index, actor ->
                Box(modifier = if (index == 0) Modifier else Modifier.offset(x = (-5 * index).dp)) {
                    KudosAvatar(actor)
                }
            }
            if (extra > 0) {
                Box(modifier = Modifier.offset(x = (-5 * stack.size).dp)) {
                    KudosMoreChip(extra)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = kudosSummary(stack, likeCount),
            style = frauncesDisplay(12.5.sp, italic = true, weight = FontWeight.Normal),
            color = AppInk2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The liker list to render given an optimistic like state (M4).
 *
 * [serverLikers] is the API-hydrated ≤3 most-recent likers and
 * [serverLikedByMe] the like state they were fetched with. [likedNow] is the
 * caller's *current* (optimistic) like state. When the caller has liked but
 * the server snapshot did not yet include them, the caller's own [selfActor]
 * is prepended so their avatar shows; when the caller has unliked a like the
 * server still has, the caller is removed. Pure, so it is unit-testable.
 */
internal fun optimisticLikers(
    serverLikers: List<ActivityActor>,
    serverLikedByMe: Boolean,
    likedNow: Boolean,
    selfActor: ActivityActor?,
): List<ActivityActor> = when {
    // No change from the server snapshot — render it as-is.
    likedNow == serverLikedByMe -> serverLikers
    // No self actor known — leave the stack to the count-only fallback.
    selfActor == null -> serverLikers
    // Caller just liked — splice their avatar to the front (most-recent).
    likedNow -> listOf(selfActor) + serverLikers.filterNot { it.userId == selfActor.userId }
    // Caller just unliked — drop their avatar from the stack.
    else -> serverLikers.filterNot { it.userId == selfActor.userId }
}

/**
 * The "Name & N others" summary line — the first liker's name in bold ink, the
 * remainder ("& N others" / "& 1 other") in muted ink. A lone liker is just
 * the name.
 */
internal fun kudosSummary(stack: List<ActivityActor>, likeCount: Int) = buildAnnotatedString {
    val lead = stack.firstOrNull()?.displayName ?: return@buildAnnotatedString
    withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) { append(lead) }
    val others = (likeCount - 1).coerceAtLeast(0)
    when {
        others == 0 -> Unit
        others == 1 -> withStyle(SpanStyle(color = AppInk2)) { append(" & 1 other") }
        else -> withStyle(SpanStyle(color = AppInk2)) { append(" & $others others") }
    }
}

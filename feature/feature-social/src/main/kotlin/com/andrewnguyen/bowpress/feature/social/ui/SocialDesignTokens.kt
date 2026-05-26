package com.andrewnguyen.bowpress.feature.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppStone

// Convenience display helpers for enum types

fun Division.label(): String = when (this) {
    Division.CMP -> "CMP"
    Division.REC -> "REC"
    Division.BAR -> "BAR"
}

fun ActivityKind.isClubKind(): Boolean =
    this == ActivityKind.club_session ||
        this == ActivityKind.club_member_joined ||
        this == ActivityKind.club_created

fun ActivityKind.isLeagueKind(): Boolean =
    this == ActivityKind.league_event ||
        this == ActivityKind.league_created ||
        this == ActivityKind.league_podium

fun ActivitySourceKind.label(): String = when (this) {
    ActivitySourceKind.friend -> "friend"
    ActivitySourceKind.club -> "club"
    ActivitySourceKind.league -> "league"
}

/** Avatar initials from display name (up to 2 chars). */
fun avatarInitials(displayName: String): String {
    val parts = displayName.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

/** Simple square avatar composable matching the Kenrokuen design system. */
@Composable
fun SocialAvatar(
    initials: String,
    size: Int = 32,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .border(1.dp, AppPondDk)
            .background(AppPaper2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = frauncesDisplay(
                size = (size * 0.38f).sp,
                weight = FontWeight.Medium,
                italic = true,
            ),
            color = AppPondDk,
        )
    }
}

/**
 * Parity E5 — avatar that renders an uploaded profile picture when one is
 * available, falling back to initials otherwise. The square chrome (border +
 * paper-2 ground) is preserved across both render paths so the kudos / feed
 * / comments avatars stay visually consistent.
 *
 * Two URL sources, checked in order:
 *  1. [avatarUrl] — an absolute URL the API already provided (rare).
 *  2. [userId] + [avatarVersion] — the API only ever sends `avatarVersion`,
 *     so we reconstruct `{baseUrl}/social/avatars/{userId}?v={n}` via the
 *     `LocalAvatarUrl` resolver. iOS does the same.
 *
 * `?v=<avatarVersion>` is appended in either case so a fresh upload busts
 * Coil's cache.
 *
 * Optional [borderTint] overrides the default pond-dk frame (used by the
 * feed card's pine border for milestone posts).
 */
@Composable
fun SocialAvatarImage(
    displayName: String,
    userId: String?,
    avatarUrl: String?,
    avatarVersion: Int?,
    size: Int = 32,
    borderTint: androidx.compose.ui.graphics.Color = AppPondDk,
    modifier: Modifier = Modifier,
) {
    val resolver = com.andrewnguyen.bowpress.core.designsystem.LocalAvatarUrl.current
    Box(
        modifier = modifier
            .size(size.dp)
            .border(1.dp, borderTint)
            .background(AppPaper2),
        contentAlignment = Alignment.Center,
    ) {
        val cacheBustedUrl = remember(avatarUrl, avatarVersion, userId, resolver) {
            val absolute = avatarUrl?.let { url ->
                if (avatarVersion != null) {
                    val sep = if (url.contains('?')) '&' else '?'
                    "$url${sep}v=$avatarVersion"
                } else {
                    url
                }
            }
            absolute ?: resolver(userId, avatarVersion)
        }
        if (cacheBustedUrl != null) {
            coil.compose.AsyncImage(
                model = cacheBustedUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(size.dp)
                    .padding(0.dp),
            )
        } else {
            Text(
                text = avatarInitials(displayName),
                style = frauncesDisplay(
                    size = (size * 0.38f).sp,
                    weight = FontWeight.Medium,
                    italic = true,
                ),
                color = borderTint,
            )
        }
    }
}

/**
 * Quiet, framed "this couldn't load" panel — used when a member/visibility-
 * gated destination (club / league / friend profile) fails or 403s after a
 * feed drill-in. Keeps the screen from rendering blank.
 */
@Composable
fun SocialUnavailableNotice(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(18.dp)
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = frauncesDisplay(18.sp), color = AppInk)
        Spacer(Modifier.height(8.dp))
        Text(
            detail,
            style = frauncesDisplay(13.sp, italic = true),
            color = AppInk3,
        )
    }
}

/** Compact relative timestamp for social rows — "just now" / "3h ago" / "5d ago". */
fun socialRelativeTime(at: Instant, now: Instant = Instant.now()): String {
    val minutes = ChronoUnit.MINUTES.between(at, now)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d ago"
        else -> "${minutes / (60 * 24 * 7)}w ago"
    }
}

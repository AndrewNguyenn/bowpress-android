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

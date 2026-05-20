package com.andrewnguyen.bowpress.feature.social.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppStone

// Convenience display helpers for enum types

fun Division.label(): String = when (this) {
    Division.CMP -> "CMP"
    Division.REC -> "REC"
    Division.BAR -> "BAR"
}

fun ActivityKind.isClubKind(): Boolean =
    this == ActivityKind.club_session || this == ActivityKind.club_member_joined

fun ActivityKind.isLeagueKind(): Boolean =
    this == ActivityKind.league_event

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

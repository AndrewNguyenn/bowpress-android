package com.andrewnguyen.bowpress.feature.social.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementBadge
import com.andrewnguyen.bowpress.core.model.AchievementKind

/**
 * Short uppercase stamp text for an [AchievementKind] — the maple-stamp
 * vocabulary the design uses for standout sessions.
 */
fun AchievementKind.stamp(): String = when (this) {
    AchievementKind.score_pr -> "SCORE PR"
    AchievementKind.x_pr -> "X PR"
    AchievementKind.flawless -> "FLAWLESS"
    AchievementKind.sharpshooter -> "SHARP"
    AchievementKind.xs_milestone -> "X COUNT"
    AchievementKind.arrows_milestone -> "MILESTONE"
    AchievementKind.sessions_milestone -> "MILESTONE"
    AchievementKind.marathon -> "MARATHON"
    AchievementKind.streak -> "STREAK"
    AchievementKind.weeks_active -> "STREAK"
    AchievementKind.comeback -> "COMEBACK"
    AchievementKind.first_distance -> "FIRST"
    AchievementKind.distance_explorer -> "EXPLORER"
    AchievementKind.course_first -> "3D COURSE"
    AchievementKind.course_milestone -> "3D COURSE"
    AchievementKind.course_explorer -> "3D COURSE"
    AchievementKind.course_marathon -> "3D COURSE"
    AchievementKind.course_pr -> "COURSE PR"
    AchievementKind.league_first_finish -> "LEAGUE"
    AchievementKind.league_champion -> "CHAMPION"
    AchievementKind.league_podium -> "PODIUM"
    AchievementKind.club_founder -> "CLUB"
    AchievementKind.club_host_growth -> "CLUB"
    AchievementKind.club_member -> "CLUB"
    AchievementKind.unknown -> "TROPHY"
}

/** A single glyph that reads as the achievement's flavour. */
fun AchievementKind.glyph(): String = when (this) {
    AchievementKind.score_pr -> "◎"
    AchievementKind.x_pr -> "✕"
    AchievementKind.flawless -> "◈"
    AchievementKind.sharpshooter -> "⊕"
    AchievementKind.xs_milestone -> "⊗"
    AchievementKind.arrows_milestone -> "↟"
    AchievementKind.sessions_milestone -> "❘❘"
    AchievementKind.marathon -> "∞"
    AchievementKind.streak -> "▲"
    AchievementKind.weeks_active -> "⬟"
    AchievementKind.comeback -> "↺"
    AchievementKind.first_distance -> "✦"
    AchievementKind.distance_explorer -> "⌖"
    AchievementKind.course_first -> "⛰"
    AchievementKind.course_milestone -> "❖"
    AchievementKind.course_explorer -> "✸"
    AchievementKind.course_marathon -> "⤧"
    AchievementKind.course_pr -> "⊙"
    AchievementKind.league_first_finish -> "⚑"
    AchievementKind.league_champion -> "♔"
    AchievementKind.league_podium -> "▰"
    AchievementKind.club_founder -> "⌂"
    AchievementKind.club_host_growth -> "⌑"
    AchievementKind.club_member -> "❡"
    AchievementKind.unknown -> "○"
}

/**
 * Maple-bordered achievement badge — the highlighted-row marker in the feed.
 * Compact: a stamp + the headline label.
 */
@Composable
fun AchievementBadgeChip(
    badge: AchievementBadge,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(1.dp, AppMaple)
            .background(AppPaper)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = badge.kind.glyph(),
            style = frauncesDisplay(11.sp),
            color = AppMaple,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = badge.label.uppercase(),
            style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
            color = AppMaple,
        )
    }
}

/**
 * Full-width trophy-case row for one [Achievement] — glyph, label, sublabel.
 * Used by the profile / You-screen trophy case.
 */
@Composable
fun AchievementRow(
    achievement: Achievement,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(1.dp, AppMaple)
            .background(AppPaper)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = achievement.kind.glyph(),
            style = frauncesDisplay(20.sp),
            color = AppMaple,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = achievement.label,
                style = frauncesDisplay(14.sp),
                color = AppMaple,
            )
            achievement.sublabel?.let { sub ->
                Text(
                    text = sub,
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
            }
        }
        Text(
            text = achievement.kind.stamp(),
            style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
            color = AppMaple,
        )
    }
}

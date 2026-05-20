package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementBadgeChip
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun FeedScreen(
    onAvatarClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onClubClick: (String) -> Unit,
    onLeagueClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onActorClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialFeedRoot),
    ) {
        // Top nav
        FeedTopNav(
            friendCount = state.friends.size,
            clubCount = state.clubs.size,
            leagueCount = state.leagues.size,
            myInitials = state.myProfile?.let { avatarInitials(it.displayName) } ?: "?",
            onAvatarClick = onAvatarClick,
        )
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Eyebrow
            item {
                FeedEyebrow()
            }

            if (state.feed.isEmpty() && !state.isLoading) {
                item { FeedEmptyState(onFriendsClick = onFriendsClick) }
            }

            items(state.feed, key = { it.id }) { item ->
                FeedItemRow(
                    item = item,
                    // Routing precedence lives in feedItemDestination().
                    onItemClick = { row ->
                        when (val dest = feedItemDestination(row)) {
                            is FeedItemDestination.Session -> onSessionClick(dest.sharedSessionId)
                            is FeedItemDestination.League -> onLeagueClick(dest.leagueId)
                            is FeedItemDestination.Club -> onClubClick(dest.clubId)
                            is FeedItemDestination.Actor -> onActorClick(dest.actorUserId)
                        }
                    },
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp, modifier = Modifier.padding(horizontal = 0.dp))
            }

            // Pending requests section if any
            if (state.pendingRequests.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 18.dp)) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = "FRIEND REQUESTS",
                                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                                color = AppInk3,
                            )
                            Text(
                                text = "${state.pendingRequests.size}",
                                style = jetbrainsMono(10.sp),
                                color = AppPondDk,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = AppLine, thickness = 1.dp)
                        Spacer(Modifier.height(8.dp))
                        state.pendingRequests.forEach { req ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SocialAvatar(initials = avatarInitials(req.otherDisplayName), size = 30)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        req.otherDisplayName,
                                        style = frauncesDisplay(14.sp),
                                        color = AppInk,
                                    )
                                    Text(
                                        "@${req.otherHandle}",
                                        style = jetbrainsMono(9.5.sp),
                                        color = AppInk3,
                                    )
                                }
                                Text(
                                    text = if (req.direction?.name == "incoming") "incoming" else "sent",
                                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                                    color = AppStone,
                                )
                            }
                            HorizontalDivider(color = AppLine2, thickness = 1.dp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FeedTopNav(
    friendCount: Int,
    clubCount: Int,
    leagueCount: Int,
    myInitials: String,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = "BOWPRESS",
                style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                color = AppPondDk,
            )
            Text(
                text = "Social",
                style = frauncesDisplay(28.sp),
                color = AppInk,
            )
            Text(
                text = "$friendCount friends · $clubCount clubs · $leagueCount leagues",
                style = jetbrainsMono(10.sp),
                color = AppInk3,
            )
        }
        // Avatar button → You screen
        Box(
            modifier = Modifier
                .size(34.dp)
                .border(1.dp, AppPondDk)
                .background(AppPaper2)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = myInitials,
                style = frauncesDisplay(13.sp),
                color = AppPondDk,
            )
        }
    }
}

@Composable
private fun FeedEyebrow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row {
            Text(
                text = "ACTIVITY",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                color = AppPondDk,
            )
            Text(
                text = " · friends + clubs + leagues",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                color = AppInk3,
            )
        }
        Text(
            text = "last 72h",
            style = jetbrainsMono(10.sp),
            color = AppInk3,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedItemRow(
    item: ActivityItem,
    onItemClick: (ActivityItem) -> Unit,
) {
    val avatarInitials = when (item.sourceKind) {
        ActivitySourceKind.club -> avatarInitials(item.actorDisplayName)
        ActivitySourceKind.league -> "◎"
        ActivitySourceKind.friend -> avatarInitials(item.actorDisplayName)
    }
    val avatarColor = when (item.sourceKind) {
        ActivitySourceKind.friend -> AppPine
        ActivitySourceKind.club -> AppStone
        ActivitySourceKind.league -> AppMaple
    }
    val stampColor = when (item.stamp) {
        "PR" -> AppPine
        "2d", "3d" -> AppMaple
        else -> AppPondDk
    }

    // §15 — a highlighted row (achievements present) gets the maple Strava
    // treatment: a left maple rule + a tinted paper ground.
    val baseModifier = if (item.highlighted) {
        Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .border(width = 1.dp, color = AppMaple)
    } else {
        Modifier.fillMaxWidth()
    }
    // Every feed row drills somewhere — the screen routes by precedence
    // (session → league → club → actor profile).
    val rowModifier = baseModifier.clickable { onItemClick(item) }

    Row(
        modifier = rowModifier.padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(30.dp)
                .border(1.dp, if (item.highlighted) AppMaple else avatarColor)
                .background(AppPaper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitials,
                style = frauncesDisplay(11.5.sp),
                color = if (item.highlighted) AppMaple else avatarColor,
            )
        }
        Spacer(Modifier.width(10.dp))

        // Body
        Column(Modifier.weight(1f)) {
            // Title row: actor name in UI + italic body
            Row {
                Text(
                    text = item.actorDisplayName.uppercase() + " ",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPondDk,
                )
            }
            Text(
                text = item.title,
                style = frauncesDisplay(14.sp),
                color = AppInk,
            )
            item.meta?.let { meta ->
                Spacer(Modifier.height(5.dp))
                Text(
                    text = meta,
                    style = jetbrainsMono(9.5.sp),
                    color = AppInk3,
                )
            }
            // §15 — shared-session stat line.
            item.session?.let { s ->
                Spacer(Modifier.height(5.dp))
                Text(
                    text = sessionStatLine(s),
                    style = jetbrainsMono(9.5.sp),
                    color = AppMaple,
                )
            }
            // §15 — achievement badges on a highlighted row.
            if (item.achievements.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    item.achievements.forEach { badge ->
                        AchievementBadgeChip(badge = badge)
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))

        // Right: stamp + time
        Column(horizontalAlignment = Alignment.End) {
            item.stamp?.let {
                val effectiveStampColor = if (item.highlighted) AppMaple else stampColor
                Box(
                    modifier = Modifier
                        .border(1.dp, effectiveStampColor)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = it,
                        style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = effectiveStampColor,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = item.createdAt.relativeTime(),
                style = jetbrainsMono(9.5.sp),
                color = AppInk3,
            )
        }
    }
}

/** Mono stat line for a shared session, e.g. "548 · 12X · 60 arrows · 50m". */
private fun sessionStatLine(s: com.andrewnguyen.bowpress.core.model.ActivitySession): String =
    buildList {
        add("${s.score}")
        add("${s.xCount}X")
        add("${s.arrowCount} arrows")
        s.distance?.let { add(it) }
        s.face?.let { add(it) }
    }.joinToString(" · ")

@Composable
private fun FeedEmptyState(onFriendsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nothing yet.",
            style = frauncesDisplay(18.sp),
            color = AppInk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add friends or join a club to see activity here.",
            style = frauncesDisplay(13.sp, italic = true),
            color = AppInk2,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .border(1.dp, AppPondDk)
                .clickable(onClick = onFriendsClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = "ADD FRIENDS",
                style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
            )
        }
    }
}

/** Relative human-readable time: "2h", "yesterday", etc. */
private fun Instant.relativeTime(): String {
    val now = Instant.now()
    val hours = ChronoUnit.HOURS.between(this, now)
    val days = ChronoUnit.DAYS.between(this, now)
    return when {
        hours < 1 -> "just now"
        hours < 24 -> "${hours}h"
        days == 1L -> "yesterday"
        else -> "${days}d"
    }
}

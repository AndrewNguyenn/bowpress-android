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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.feature.social.ui.EmptyAction
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.SocialEmptyState
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementBadgeChip
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationMapSheet
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun FeedScreen(
    onAvatarClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onClubsIndexClick: () -> Unit,
    onLeaguesIndexClick: () -> Unit,
    onClubClick: (String) -> Unit,
    onLeagueClick: (String) -> Unit,
    onSessionClick: (sharedSessionId: String, isOwn: Boolean) -> Unit,
    onActorClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // §18 — the location whose map popup is open, or null when none. Tapping a
    // feed row's location tag opens it.
    var mapLocation by remember { mutableStateOf<SessionLocation?>(null) }

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
            // Option-A top strip — Friends / Clubs / Leagues nav cards.
            item {
                FeedNavStrip(
                    friendCount = state.friends.size,
                    clubs = state.clubs,
                    leagueCount = state.leagues.size,
                    leagueUrgent = state.leagueDeadlineNear,
                    onFriendsClick = onFriendsClick,
                    onClubsClick = onClubsIndexClick,
                    onLeaguesClick = onLeaguesIndexClick,
                )
            }
            // Eyebrow
            item {
                FeedEyebrow()
            }

            when (state.emptyVariant) {
                FeedEmptyVariant.NewUser -> item {
                    SocialEmptyState(
                        icon = "◎",
                        title = "Your range, connected.",
                        message = "Add friends, join a club, or enter a league — their sessions, PRs, and league standings all land in this feed.",
                        actions = listOf(
                            EmptyAction(
                                label = "Add a friend",
                                sublabel = "by @handle or invite link",
                                onClick = onFriendsClick,
                                testTag = TestTags.SocialEmptyAddFriend,
                            ),
                            EmptyAction(
                                label = "Find a club",
                                sublabel = "join with an 8-character code",
                                onClick = onClubsIndexClick,
                                testTag = TestTags.SocialEmptyFindClub,
                            ),
                        ),
                        modifier = Modifier.testTag(TestTags.SocialFeedNewUserEmpty),
                    )
                }
                FeedEmptyVariant.QuietWeek -> item {
                    SocialEmptyState(
                        icon = "○",
                        title = "Quiet week.",
                        message = "No sessions from your friends or clubs in the last 72 hours. New activity shows up here as it happens.",
                        actions = emptyList(),
                        modifier = Modifier.testTag(TestTags.SocialFeedQuietEmpty),
                    )
                }
                null -> Unit
            }

            items(state.feed, key = { it.id }) { item ->
                // Routing precedence lives in feedItemDestination().
                val openItem: (ActivityItem) -> Unit = { row ->
                    when (val dest = feedItemDestination(row)) {
                        is FeedItemDestination.Session ->
                            onSessionClick(dest.sharedSessionId, dest.isOwn)
                        is FeedItemDestination.League -> onLeagueClick(dest.leagueId)
                        is FeedItemDestination.Club -> onClubClick(dest.clubId)
                        is FeedItemDestination.Actor -> onActorClick(dest.actorUserId)
                    }
                }
                if (activityPreview(item) is ActivityPreview.Target) {
                    // Social Activity Card · 50/50 — the rich card for a
                    // shared range session.
                    ActivityCard(
                        item = item,
                        onClick = { openItem(item) },
                        onLocationTap = { location -> mapLocation = location },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                } else {
                    FeedItemRow(
                        item = item,
                        photoLoader = viewModel.photoLoader,
                        onItemClick = openItem,
                        // §18 — tapping the location tag opens the map popup.
                        onLocationTap = { location -> mapLocation = location },
                    )
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
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

    // §18 — the location map popup behind a feed post's location tag.
    mapLocation?.let { location ->
        LocationMapSheet(
            location = location,
            onDismiss = { mapLocation = null },
        )
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

/**
 * Option-A 3-card nav strip above the activity feed — Friends · Clubs ·
 * Leagues. Mirrors `.nav-strip`/`.nc` in `SOCIAL_DESIGN_OPTION_A.html`: each
 * card is a large Fraunces numeral, a label, and a mono sub-line. The League
 * card takes the maple `urgent` tint/border when a deadline is near.
 */
@Composable
private fun FeedNavStrip(
    friendCount: Int,
    clubs: List<Club>,
    leagueCount: Int,
    leagueUrgent: Boolean,
    onFriendsClick: () -> Unit,
    onClubsClick: () -> Unit,
    onLeaguesClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NavCard(
            count = friendCount,
            label = "Friends",
            sub = "your circle",
            onClick = onFriendsClick,
            modifier = Modifier.weight(1f),
        )
        NavCard(
            count = clubs.size,
            label = "Clubs",
            // First word of each club name, matching the prototype sub-line.
            sub = clubs.takeIf { it.isNotEmpty() }
                ?.joinToString(" · ") { it.name.substringBefore(' ') }
                ?: "none yet",
            onClick = onClubsClick,
            modifier = Modifier.weight(1f),
        )
        NavCard(
            count = leagueCount,
            label = if (leagueCount == 1) "League" else "Leagues",
            sub = if (leagueUrgent) "deadline near" else "standings",
            urgent = leagueUrgent,
            onClick = onLeaguesClick,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One nav-strip card — `.nc` in the design CSS. */
@Composable
private fun NavCard(
    count: Int,
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    urgent: Boolean = false,
) {
    val accent = if (urgent) AppMaple else AppPondDk
    Column(
        modifier = modifier
            .border(1.dp, if (urgent) AppMaple else AppLine)
            // Faint maple wash for the urgent card; plain paper-2 otherwise.
            .background(if (urgent) AppMaple.copy(alpha = 0.08f) else AppPaper2)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
    ) {
        Text(
            text = "$count",
            style = frauncesDisplay(22.sp),
            color = accent,
        )
        Text(
            text = label,
            style = frauncesDisplay(13.sp),
            color = AppInk,
            modifier = Modifier.padding(top = 3.dp),
        )
        Text(
            text = sub,
            style = jetbrainsMono(8.5.sp, if (urgent) FontWeight.Medium else FontWeight.Normal)
                .copy(letterSpacing = 0.04.em),
            color = if (urgent) AppMaple else AppInk3,
            modifier = Modifier.padding(top = 2.dp),
        )
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
                // Social Feed V2 §2 — the caller's own activity now interleaves
                // into the feed, so the eyebrow names the caller first.
                text = " · you + friends + clubs + leagues",
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
    photoLoader: com.andrewnguyen.bowpress.feature.social.ui.session.SessionPhotoLoader,
    onItemClick: (ActivityItem) -> Unit,
    onLocationTap: (SessionLocation) -> Unit,
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

    // The feed's own horizontal content inset — a full-bleed preview (the 3D
    // course map) negates it so it can sit edge-to-edge (mirrors iOS
    // `FeedRowView.contentInset` = 16; Android's feed inset is 18).
    val contentInset = 18.dp
    val preview = activityPreview(item)

    // Social Feed V2 §4 — a photographed session shows its photo gallery in
    // place of the typed §18 preview band. Resolved once so the body knows
    // whether to render the gallery or the discipline preview below the row.
    val photos = item.session?.photos.orEmpty()

    // The preview drops below the avatar/timestamp row so it spans the full
    // row width — and a 3D-course map bleeds past the feed's own horizontal
    // inset to sit truly edge-to-edge. A highlighted card keeps the preview
    // inside its border, matching iOS `HighlightedFeedRow`.
    Column(modifier = rowModifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = contentInset),
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
                // §18 — Instagram-style location tag above the headline. A
                // nested clickable consumes the tap so it opens the map
                // instead of drilling into the row's destination.
                item.session?.location?.let { location ->
                    LocationTag(
                        name = location.name,
                        onTap = { onLocationTap(location) },
                    )
                    Spacer(Modifier.height(3.dp))
                }
                // Actor eyebrow — the acting archer's name (or "YOU" on an own
                // row). Formatting rule lives in FeedHeadline so it stays
                // testable. (Social Feed V2 §2.)
                Row {
                    Text(
                        text = FeedHeadline.actorEyebrow(item) + " ",
                        style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPondDk,
                    )
                }
                // Headline. §1 — a custom title is the archer's own session
                // name, rendered as a quoted caption; a generic title renders
                // verbatim.
                Text(
                    text = FeedHeadline.headline(item),
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

        // The session preview is dropped below the avatar/timestamp row so it
        // spans the full row width. Social Feed V2 §4 — a photographed session
        // shows its photo gallery; otherwise §18's typed discipline band
        // (target face / 3D-course map). A 3D-course map bleeds past the
        // feed's horizontal inset to sit edge-to-edge; the photo gallery and
        // every other preview stay within the inset, and a highlighted card
        // keeps everything inside its border (no bleed).
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            com.andrewnguyen.bowpress.feature.social.ui.session.FeedPhotoGallery(
                sharedSessionId = item.session!!.sharedSessionId,
                photos = photos,
                loader = photoLoader,
                modifier = Modifier.padding(horizontal = contentInset),
            )
        } else if (!preview.isEmpty) {
            val bleed = preview.wantsFullBleed && !item.highlighted
            Spacer(Modifier.height(8.dp))
            ActivityPreviewBand(
                preview = preview,
                modifier = Modifier.padding(
                    horizontal = if (bleed) 0.dp else contentInset,
                ),
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

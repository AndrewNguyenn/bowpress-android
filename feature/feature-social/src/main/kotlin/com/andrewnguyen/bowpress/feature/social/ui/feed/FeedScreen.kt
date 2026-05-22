package com.andrewnguyen.bowpress.feature.social.ui.feed

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
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.feature.social.ui.EmptyAction
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.SocialEmptyState
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationMapSheet

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
    onCommentsClick: (subjectId: String, ownerUserId: String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
    mentionResolver: com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionResolverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Mentions contract §3.2 — a tapped `@handle` in a post title resolves to
    // an archer profile, reusing the actor-profile navigation.
    val onMentionTap: (String) -> Unit = { handle ->
        mentionResolver.openMention(handle, onActorClick)
    }

    // §18 — the location whose map popup is open, or null when none. Tapping a
    // feed row's location tag opens it.
    var mapLocation by remember { mutableStateOf<SessionLocation?>(null) }

    // §4 — the open photo-strip viewer request, or null when closed. Owned at
    // screen scope (not inside the `LazyColumn` card) so scrolling the source
    // card off-screen does not tear down the viewer mid-look.
    var photoViewer by remember { mutableStateOf<PhotoViewerRequest?>(null) }

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
                // Social Feed V2 §5 — opening the comment thread carries the
                // §5.1 subject owner so the screen can gate comment deletion
                // (author OR post owner).
                //
                // LOAD-BEARING INVARIANT: for a session post the §5.1 subject
                // owner is the shared session's `userId`, and `actorUserId` is
                // used here as a stand-in. That is correct ONLY because the
                // API's share endpoint sets a session-post activity row's
                // `actorUserId` to the sharer (= the session owner). For a
                // club/league event the subject owner is likewise the actor.
                // If the API ever decouples actor from subject owner, this
                // must switch to a real owner id. The server is authoritative
                // regardless (a forbidden delete is a 403); this only governs
                // whether the client shows the delete affordance.
                val openComments: (String) -> Unit = { subjectId ->
                    onCommentsClick(subjectId, item.actorUserId)
                }
                // Social Activity Card · 50/50 — every feed row renders inside
                // the one card chrome; ActivityCard picks the body (the 50/50
                // target+ledger for a range session, the course map for a 3D
                // course, a short text body otherwise).
                ActivityCard(
                    item = item,
                    onClick = { openItem(item) },
                    onLocationTap = { location -> mapLocation = location },
                    onToggleLike = viewModel::toggleLike,
                    onOpenComments = openComments,
                    photoLoader = viewModel.photoLoader,
                    // §4 — a photo-strip cell tap raises the screen-level
                    // viewer; the card itself never hosts it.
                    onOpenPhotoViewer = { sharedSessionId, photos, startIndex ->
                        photoViewer = PhotoViewerRequest(
                            sharedSessionId = sharedSessionId,
                            photos = photos,
                            startIndex = startIndex,
                        )
                    },
                    // The signed-in caller, so an optimistic self-like puts
                    // the caller's own avatar into the kudos stack (M4).
                    selfActor = state.myProfile?.let { p ->
                        com.andrewnguyen.bowpress.core.model.ActivityActor(
                            userId = p.userId,
                            handle = p.handle,
                            displayName = p.displayName,
                        )
                    },
                    onMentionTap = onMentionTap,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
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

    // §4 — the full-screen photo viewer. Owned here at screen scope so it
    // survives the source card scrolling out of the `LazyColumn`.
    photoViewer?.let { req ->
        PhotoStripViewer(
            sharedSessionId = req.sharedSessionId,
            photos = req.photos,
            startIndex = req.startIndex,
            loader = viewModel.photoLoader,
            onDismiss = { photoViewer = null },
        )
    }
}

/**
 * §4 — an open photo-strip viewer: which shared session's photos to show and
 * the page to open on. [photos] is the already `ready`-filtered, position-
 * sorted list the source card passed up, so [startIndex] indexes it directly.
 */
private data class PhotoViewerRequest(
    val sharedSessionId: String,
    val photos: List<com.andrewnguyen.bowpress.core.model.ActivityPhoto>,
    val startIndex: Int,
)

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

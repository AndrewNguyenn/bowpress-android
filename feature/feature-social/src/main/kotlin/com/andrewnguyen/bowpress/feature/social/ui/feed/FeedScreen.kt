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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
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
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.feature.social.ui.EmptyAction
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.SocialEmptyState
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationMapSheet

@OptIn(ExperimentalMaterial3Api::class)
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
    onBellClick: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
    mentionResolver: com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionResolverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    // Phase B — in-flight export jobs keyed by sessionId, for the per-card
    // optimistic upload chip.
    val exportJobs by viewModel.exportJobsBySession.collectAsState()
    // iOS parity (A3) — swipeable hero carousel under the top nav.
    val feedSummary by viewModel.feedSummary.collectAsState()

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
        // iOS parity (A2) — Top nav now carries the F/C/L drill-in pills
        // inline with the bell + avatar. The old `FeedNavStrip` 3-card
        // block under the header is gone.
        FeedTopNav(
            friendCount = state.friends.size,
            clubCount = state.clubs.size,
            leagueCount = state.leagues.size,
            // Avatar pill in the top nav — fall back to em-dash (not "?") so
            // the cluster looks calm before the profile hydrates. iOS hard-
            // codes "AN" here; an em-dash is the closest no-info equivalent
            // we can show without inventing initials we don't know.
            myInitials = state.myProfile
                ?.displayName
                ?.takeIf { it.isNotBlank() }
                ?.let { avatarInitials(it) }
                ?: "—",
            notificationCount = pendingCount,
            pendingFriendRequests = state.pendingIncomingFriendRequests,
            pendingClubInvites = state.pendingClubInvites,
            pendingLeagueInvites = state.pendingLeagueInvites,
            leagueUrgent = state.leagueDeadlineNear,
            onAvatarClick = onAvatarClick,
            onBellClick = onBellClick,
            onFriendsPillClick = onFriendsClick,
            onClubsPillClick = onClubsIndexClick,
            onLeaguesPillClick = onLeaguesIndexClick,
        )
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        // iOS parity — `SocialTabView` attaches `.refreshable { load(force:
        // true) }` to its ScrollView. PullToRefreshBox is the Compose
        // equivalent; on swipe-down it drives a fresh feed reload via the
        // existing `viewModel.refresh()` path (resets cursor, replaces the
        // first page — same as the iOS forced reload). `state.isLoading`
        // doubles as the cold-start indicator, which is fine: the same
        // spinner that lands during a user-initiated refresh also lands on
        // the first paint, so the screen never appears empty mid-fetch.
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
            // iOS parity (A3) — swipeable hero carousel above the
            // activity list. Hidden entirely when there's no summary
            // data (a brand-new account); the per-card empty-handling
            // collapses individual cards too.
            feedSummary?.takeIf { it.cards.isNotEmpty() }?.let { summary ->
                item {
                    FeedCarousel(
                        summary = summary,
                        // Best-session "Open" wired to the existing
                        // session-detail navigation when the best
                        // session is shared. The viewmodel preview
                        // currently leaves sharedSessionId = null so
                        // the link is hidden.
                        onOpenBest = summary.bestSession
                            ?.sharedSessionId
                            ?.let { sid -> { onSessionClick(sid, /* isOwn */ false) } },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
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
                        message = "No sessions from your friends or clubs yet. New activity shows up here as it happens.",
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
                // Note: the pagination trigger lives in a separate `item { }`
                // sentinel below — NOT inside this row's body — so it remains
                // a direct LazyColumn child (mirrors iOS commit b849ed5, which
                // moved the SwiftUI sentinel out of an eager VStack so the
                // scroll visibility callback fires on viewport entry).
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
                    // iOS parity (A5) — reactions bundle. Feed rows are
                    // likeable + commentable; Log rows pass null instead.
                    reactions = Reactions(
                        onToggleLike = viewModel::toggleLike,
                        onOpenComments = openComments,
                        // The signed-in caller, so an optimistic self-like puts
                        // the caller's own avatar into the kudos stack (M4).
                        selfActor = state.myProfile?.let { p ->
                            com.andrewnguyen.bowpress.core.model.ActivityActor(
                                userId = p.userId,
                                handle = p.handle,
                                displayName = p.displayName,
                            )
                        },
                    ),
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
                    onMentionTap = onMentionTap,
                    // Parity E2 — tappable actor avatar + name in the header.
                    // Own posts (currentUserId == row actor) pass null so the
                    // self-row stays inert; everyone else opens the actor's
                    // profile.
                    onActorClick = if (item.isOwn) null else { actorUserId: String ->
                        if (actorUserId.isNotBlank()) onActorClick(actorUserId)
                    },
                    // Phase B — the optimistic upload chip. Keyed by the client
                    // sessionId; only an own in-flight share has a job, so a
                    // friend's row resolves to null and shows no chip.
                    uploadJob = item.session?.sessionId?.let { exportJobs[it] },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            // Pagination sentinel — a direct LazyColumn `item { }` child so the
            // lazy-layout composition signal fires reliably when the user
            // scrolls to the end of the feed. Keyed on `state.nextCursor` so
            // each new cursor re-triggers the effect once and only once on
            // entry; null cursor (last page) no-ops. Mirrors iOS
            // `Color.clear.id(nextCursor).onScrollVisibilityChange { … }`
            // (commit b849ed5).
            if (state.feed.isNotEmpty() && state.nextCursor != null) {
                item(key = "feed-sentinel-${state.nextCursor}") {
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth())
                    LaunchedEffect(state.nextCursor) {
                        viewModel.loadMore()
                    }
                }
            }

            // Loading footer — shown while the next page is in flight.
            if (state.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
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

/**
 * iOS parity (A2) — Feed top-nav row.
 *
 * Mirrors `SocialTopNav` + the inline `navPill` row in
 * `SocialTabView.swift`. The F/C/L drill-in icons now live inline with
 * the bell + avatar instead of in a 3-card strip below. Each pill carries
 * a corner badge that counts **actionables only** — pending incoming
 * friend requests, pending club/league invites — so an archer with 14
 * friends and zero inbound activity gets a clean Friends pill. The
 * Leagues pill keeps the maple urgency cue (border + tint) when a
 * deadline is within ~3 days.
 *
 * Test tags are stable on `TestTags.SocialNavPill*` so Maestro flows can
 * tap "navCard.Friends" / etc. without caring about the strip → pill
 * restyle.
 */
@Composable
private fun FeedTopNav(
    friendCount: Int,
    clubCount: Int,
    leagueCount: Int,
    myInitials: String,
    notificationCount: Int,
    pendingFriendRequests: Int,
    pendingClubInvites: Int,
    pendingLeagueInvites: Int,
    leagueUrgent: Boolean,
    onAvatarClick: () -> Unit,
    onBellClick: () -> Unit,
    onFriendsPillClick: () -> Unit,
    onClubsPillClick: () -> Unit,
    onLeaguesPillClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // iOS parity (A1) — the screen-title text is "Feed". The graph
            // route string and class names stay `Social*` for back-compat.
            Text(
                text = "Feed",
                style = frauncesDisplay(28.sp),
                color = AppInk,
            )
            Text(
                text = "$friendCount friends · $clubCount clubs · $leagueCount leagues",
                style = jetbrainsMono(10.sp),
                color = AppInk3,
            )
        }
        // The pill / bell / avatar cluster — bottom-aligned to the title
        // so the bottoms line up with the mono subline.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            NavPill(
                icon = Icons.Filled.People,
                count = pendingFriendRequests,
                onClick = onFriendsPillClick,
                idTag = "Friends",
            )
            NavPill(
                icon = Icons.Filled.Groups,
                count = pendingClubInvites,
                onClick = onClubsPillClick,
                idTag = "Clubs",
            )
            NavPill(
                icon = Icons.Filled.EmojiEvents,
                count = pendingLeagueInvites,
                urgent = leagueUrgent,
                onClick = onLeaguesPillClick,
                idTag = "Leagues",
            )
            // Bell → notification center, with the unread count badge.
            // iOS parity (A2) — border + icon tint activates pond when there
            // is a pending count; the badge itself is always maple (matches
            // bellButton in SocialTabView.swift).
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, if (notificationCount > 0) AppPondDk else AppInk3)
                    .background(AppPaper2)
                    .clickable(onClick = onBellClick),
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = if (notificationCount > 0) AppPondDk else AppInk2,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(0.dp)
                        .align(Alignment.Center),
                )
                if (notificationCount > 0) {
                    PillCornerBadge(
                        text = if (notificationCount > 99) "99+" else "$notificationCount",
                        tint = AppMaple,
                    )
                }
            }
            // Avatar button → You screen
            Box(
                modifier = Modifier
                    .size(32.dp)
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
}

/**
 * iOS parity (A2) — one F/C/L drill-in pill in the FeedScreen top-nav.
 * Mirrors iOS `navPill` in `SocialTabView.swift`:
 *  - 32dp box, 1px stroke, paper-2 ground.
 *  - Stroke + glyph tint maple when [urgent] is true, otherwise ink2.
 *  - Top-right corner badge shows the actionable [count]; hidden when 0
 *    (an empty F/C/L count shouldn't light up the UI).
 *  - Stable Maestro identifier `navCard.<idTag>` kept on the prior nav
 *    card name so existing flows keep working through the restyle.
 */
@Composable
private fun NavPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    onClick: () -> Unit,
    idTag: String,
    urgent: Boolean = false,
) {
    val tint = if (urgent) AppMaple else AppInk2
    val borderColor = if (urgent) AppMaple else AppLine
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, borderColor)
            .background(AppPaper2)
            .clickable(onClick = onClick)
            .testTag("navCard.$idTag"),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = idTag,
            tint = tint,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center),
        )
        if (count > 0) {
            PillCornerBadge(
                text = if (count > 99) "99+" else "$count",
                tint = if (urgent) AppMaple else AppPondDk,
            )
        }
    }
}

/**
 * iOS parity (A2) — the small badge that rides the top-right corner of a
 * NavPill / bell icon. Mirrors iOS `badgeView` in `SocialTabView.swift`:
 * paper-tinted text on the badge fill, paper outline so the chip reads
 * cleanly on top of the pill's own outline.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.PillCornerBadge(
    text: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 6.dp, y = (-5).dp)
            .background(tint)
            .border(1.dp, AppPaper)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            style = jetbrainsMono(9.sp, FontWeight.SemiBold),
            color = AppPaper,
        )
    }
}


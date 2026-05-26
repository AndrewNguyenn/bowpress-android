package com.andrewnguyen.bowpress.feature.social.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPlottedTarget
import com.andrewnguyen.bowpress.core.designsystem.bp.ScorecardTable
import com.andrewnguyen.bowpress.core.designsystem.coursemap.CourseInkMapView
import com.andrewnguyen.bowpress.core.designsystem.coursemap.CourseStationBottomSheet
import com.andrewnguyen.bowpress.core.designsystem.coursemap.CourseStationRow
import com.andrewnguyen.bowpress.core.designsystem.coursemap.runningTotalThrough
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.Scorecard
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.core.model.SharedSession
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatarImage
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionBodyText
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionResolverViewModel

/**
 * Shared-session detail — drilled into from a tapped feed session row.
 *
 * §16 read-only mode (a friend's session): the target face with the friend's
 * arrows plotted, a read-only scorecard, a stat header.
 *
 * Social Feed V2 owner-editable mode (`isOwn = true`): the same detail plus an
 * Edit button that opens [MySessionEditSheet] — title, location, and a
 * tap-to-remove / system-picker-to-add multi-photo gallery (§3, §4).
 */
@Composable
fun FriendSessionDetailScreen(
    sharedSessionId: String,
    isOwn: Boolean,
    onBack: () -> Unit,
    onCommentsClick: (subjectId: String, ownerUserId: String) -> Unit,
    // Mentions §3.2 — a tapped `@handle` in the description resolves to an
    // archer profile. Defaulted so a caller that doesn't wire it still
    // compiles (the tap is then a no-op).
    onOpenArcher: (userId: String) -> Unit = {},
    viewModel: FriendSessionDetailViewModel = hiltViewModel(),
    mentionResolver: MentionResolverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sharedSessionId, isOwn) { viewModel.load(sharedSessionId, isOwn) }

    // The owner deleted the post — the server cascaded the fanout, so pop
    // back to the feed (where the row is already gone via refreshFeed).
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    // A tapped mention in the description resolves its handle → archer (§3.2).
    val onMentionTap: (String) -> Unit = { handle ->
        mentionResolver.openMention(handle, onOpenArcher)
    }

    // Owner-only edit sheet visibility.
    var editing by remember { mutableStateOf(false) }
    // Focused station on a friend's 3D-course map (mirrors ThreeDLogDetailScreen).
    var focusedStation by remember { mutableStateOf<Int?>(null) }
    val unitSystem = LocalUnitSystem.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialSessionDetailRoot),
    ) {
        // Top nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "‹  Feed",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text(
                    if (state.isOwn) "Your session" else "Session",
                    style = frauncesDisplay(28.sp),
                    color = AppInk,
                )
                state.detail?.let { d ->
                    // Parity E2 + E5 — owner avatar + name (tappable when the
                    // viewer is not the owner), with uploaded photo via Coil
                    // when present.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val ownerTap: (() -> Unit)? = if (!state.isOwn) {
                            { onOpenArcher(d.sharedSession.userId) }
                        } else null
                        SocialAvatarImage(
                            displayName = d.ownerDisplayName,
                            avatarUrl = d.ownerAvatarUrl,
                            avatarVersion = d.ownerAvatarVersion,
                            size = 22,
                            modifier = if (ownerTap != null) Modifier.clickable(onClick = ownerTap) else Modifier,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "@${d.ownerHandle} · ${d.ownerDisplayName}",
                            style = jetbrainsMono(10.sp),
                            color = AppInk3,
                            modifier = if (ownerTap != null) Modifier.clickable(onClick = ownerTap) else Modifier,
                        )
                    }
                }
            }
            // Social Feed V2 §3 — the owner edit affordance. Only on an own row.
            if (state.isOwn && state.detail != null) {
                Text(
                    text = "EDIT",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.26.em),
                    color = AppPondDk,
                    modifier = Modifier
                        .border(1.dp, AppPondDk)
                        .clickable { editing = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag(TestTags.MySessionEditButton),
                )
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        when {
            state.isLoading -> {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Loading…",
                    style = frauncesDisplay(14.sp),
                    color = AppInk3,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            state.error != null && state.detail == null -> {
                Spacer(Modifier.height(20.dp))
                Text(
                    state.error.orEmpty(),
                    style = jetbrainsMono(10.sp),
                    color = AppMaple,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }

            state.detail != null -> {
                val detail = state.detail!!
                val courseSystem = detail.session?.scoringSystem ?: ThreeDScoringSystem.ASA
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val areaHeight = maxHeight
                    val focusedCourseStation =
                        focusedStation?.let { detail.stations.getOrNull(it) }
                    val courseRunningTotal = remember(detail.stations, focusedCourseStation) {
                        runningTotalThrough(detail.stations, focusedCourseStation)
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                        // Parity E10 — dedicated location strip ABOVE the
                        // stat header, showing the tagged range / place.
                        // iOS commit 9c85a30. Guards both null AND blank so
                        // a stub `SessionLocation("", lat, lng)` doesn't
                        // render an empty strip.
                        detail.sharedSession.location
                            ?.takeIf { it.name.isNotBlank() }
                            ?.let { loc ->
                                item {
                                    Spacer(Modifier.height(14.dp))
                                    LocationStrip(name = loc.name)
                                }
                            }

                        // Header — stat summary always shown (survives a deleted session).
                        item {
                            Spacer(Modifier.height(14.dp))
                            SessionStatHeader(shared = detail.sharedSession)
                        }

                        // Migration 0039 — the archer's caption, with tappable
                        // `@mention` spans. Omitted when the post has none.
                        detail.sharedSession.description
                            ?.takeIf { it.isNotBlank() }
                            ?.let { description ->
                                item {
                                    Spacer(Modifier.height(12.dp))
                                    MentionBodyText(
                                        text = description,
                                        style = frauncesDisplay(14.sp).copy(color = AppInk2),
                                        onMentionTap = onMentionTap,
                                        modifier = Modifier.testTag(TestTags.SessionDetailDescription),
                                    )
                                }
                            }

                        // Social Feed V2 §5 — the like + comment action bar. The
                        // subject id falls back to the shared-session id for a
                        // pre-§5 detail payload; the subject owner is the session
                        // owner.
                        item {
                            val subjectId = detail.subjectId.ifBlank { detail.sharedSession.id }
                            Spacer(Modifier.height(10.dp))
                            com.andrewnguyen.bowpress.feature.social.ui.feed.LikeCommentBar(
                                subjectId = subjectId,
                                likeCount = detail.likeCount,
                                likedByMe = detail.likedByMe,
                                commentCount = detail.commentCount,
                                seedKey = "$subjectId:${detail.likeCount}:${detail.likedByMe}:${detail.commentCount}",
                                onToggleLike = viewModel::toggleLike,
                                onOpenComments = {
                                    onCommentsClick(subjectId, detail.sharedSession.userId)
                                },
                            )
                        }

                        // Social Feed V2 §4 — the photo gallery, when present.
                        if (detail.photos.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(18.dp))
                                SectionEyebrow("PHOTOS")
                                Spacer(Modifier.height(10.dp))
                                DetailPhotoGallery(
                                    sharedSessionId = detail.sharedSession.id,
                                    photos = detail.photos,
                                    loader = viewModel.photoLoader,
                                )
                            }
                        }

                        val shotSession = detail.session
                        val isCourse = detail.stations.isNotEmpty() ||
                            shotSession?.sessionType == SessionType.THREE_D_COURSE
                        if (isCourse) {
                            // 3D course — the walked map + the station list, the
                            // same content as the 3D Log detail. Mirrors iOS
                            // FriendSessionDetailView's course path.
                            if (detail.stations.isEmpty()) {
                                item { DeletedSessionNotice() }
                            } else {
                                item {
                                    Spacer(Modifier.height(18.dp))
                                    SectionEyebrow("COURSE · ${detail.stations.size} STATIONS")
                                    Spacer(Modifier.height(10.dp))
                                    CourseInkMapView(
                                        stations = detail.stations,
                                        elevationGrid = null,
                                        selectedStation = focusedStation,
                                        // A pin tap selects the station (no
                                        // toggle-to-close) — consistent with a
                                        // row tap; CLOSE ✕ dismisses the sheet.
                                        onTapStation = { focusedStation = it },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                item {
                                    Spacer(Modifier.height(18.dp))
                                    SectionEyebrow("STATIONS")
                                    Spacer(Modifier.height(10.dp))
                                }
                                items(detail.stations, key = { it.id }) { station ->
                                    val idx = station.stationNumber - 1
                                    // Tapping a row focuses the station; the
                                    // bottom sheet rises over the map below.
                                    CourseStationRow(
                                        station = station,
                                        system = courseSystem,
                                        unitSystem = unitSystem,
                                        focused = focusedStation == idx,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, AppLine)
                                            .clickable { focusedStation = idx },
                                    )
                                }
                            }
                        } else if (shotSession == null) {
                            // Owner deleted the underlying session — stat summary only.
                            item { DeletedSessionNotice() }
                        } else {
                            // Target face with the friend's arrows plotted — renders
                            // the real face type + 3-spot layout the friend shot.
                            item {
                                Spacer(Modifier.height(18.dp))
                                SectionEyebrow("SHOT DISTRIBUTION · ${shotSession.targetLayout.label}")
                                Spacer(Modifier.height(10.dp))
                                BPPlottedTarget(
                                    arrows = detail.arrows,
                                    faceType = shotSession.targetFaceType,
                                    layout = shotSession.targetLayout,
                                    // §B3 — outdoor sixRing at 50/70m shows
                                    // the 7-zone face; everything else stays
                                    // on the Vegas 6-zone default.
                                    sixRingStyle = com.andrewnguyen.bowpress.core.designsystem.bp.BPSixRingStyle
                                        .forDistance(shotSession.distance),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(TestTags.SocialSessionTarget),
                                )
                            }

                            // Scorecard — the canonical ruled table, identical to
                            // the session-detail screen (read-only here).
                            item {
                                Spacer(Modifier.height(18.dp))
                                SectionEyebrow("SCORECARD")
                                Spacer(Modifier.height(10.dp))
                                val scorecard = Scorecard.build(
                                    detail.arrows, detail.ends, shotSession.id,
                                )
                                if (scorecard.lines.isEmpty()) {
                                    Text(
                                        "No arrows recorded for this session.",
                                        style = frauncesDisplay(13.sp, italic = true),
                                        color = AppInk3,
                                    )
                                } else {
                                    ScorecardTable(scorecard = scorecard)
                                }
                            }
                        }

                        item { Spacer(Modifier.height(24.dp)) }
                    }

                    // Read-only station detail sheet over the 3D-course map.
                    CourseStationBottomSheet(
                        station = focusedCourseStation,
                        containerHeight = areaHeight,
                        stationCount = detail.stations.size,
                        system = courseSystem,
                        unitSystem = unitSystem,
                        runningTotal = courseRunningTotal,
                        onClose = { focusedStation = null },
                        editable = false,
                    )
                }
            }
        }
    }

    // Social Feed V2 §3/§4 — owner edit sheet.
    if (editing && state.detail != null) {
        val detail = state.detail!!
        LaunchedEffect(Unit) { viewModel.beginEdit() }
        MySessionEditSheet(
            sharedSessionId = detail.sharedSession.id,
            initialTitle = detail.sharedSession.title.orEmpty(),
            initialDescription = detail.sharedSession.description.orEmpty(),
            initialLocation = detail.sharedSession.location,
            photos = detail.photos,
            photoLoader = viewModel.photoLoader,
            isSaving = state.isSaving,
            isDeleting = state.isDeleting,
            onSearchHandles = mentionResolver::searchHandles,
            onSave = { title, description, location ->
                viewModel.saveEdit(title, description, location)
                editing = false
            },
            onAddPhotos = { uris -> viewModel.addPhotos(uris) },
            onRemovePhoto = { photo -> viewModel.removePhoto(photo) },
            onDelete = {
                editing = false
                viewModel.deletePost()
            },
            onDismiss = {
                viewModel.rollbackUncommittedPhotos()
                editing = false
            },
        )
    }
}

/** Score / X / arrows / distance / face summary off the shared session. */
@Composable
private fun SessionStatHeader(shared: SharedSession) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(14.dp),
    ) {
        shared.title?.takeIf { it.isNotBlank() }?.let { title ->
            Text(title, style = frauncesDisplay(16.sp), color = AppInk)
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            // Parity E10 — hero score stays on one line on long values
            // (iOS commits 6aa63fa + 734c6f6). maxLines/softWrap=false
            // keeps the score itself from breaking; the surrounding row
            // already gives the X·arrows label enough room to wrap.
            Text(
                "${shared.score}",
                style = frauncesDisplay(34.sp),
                color = AppPondDk,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${shared.xCount}X · ${shared.arrowCount} arrows",
                style = jetbrainsMono(10.sp),
                color = AppInk3,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        val context = listOfNotNull(shared.distance, shared.face)
        if (context.isNotEmpty()) {
            Text(
                context.joinToString(" · "),
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppMaple,
            )
        }
        // Location moved to the dedicated [LocationStrip] above this header
        // (parity E10). Intentionally not rendered here to avoid duplication.
    }
}

/**
 * Parity E10 — slim location strip rendered ABOVE the session stat header
 * (iOS commit 9c85a30). Shows the tagged range / place at the top of the
 * friend-session detail.
 */
@Composable
private fun LocationStrip(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "◇",
            style = frauncesDisplay(14.sp, italic = true),
            color = AppPondDk,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "LOCATION",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
            Text(
                name,
                style = frauncesDisplay(14.sp, italic = true),
                color = AppInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DeletedSessionNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Session no longer available.", style = frauncesDisplay(15.sp), color = AppInk)
        Spacer(Modifier.height(6.dp))
        Text(
            "The owner deleted this session — only the summary above remains.",
            style = frauncesDisplay(12.sp, italic = true),
            color = AppInk3,
        )
    }
}

@Composable
private fun SectionEyebrow(label: String) {
    Text(
        label,
        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
        color = AppInk3,
    )
}

// §B3 SixRing visual variant by distance lives on
// `BPSixRingStyle.Companion.forDistance(distance)` — the canonical helper.

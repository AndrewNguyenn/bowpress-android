package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppDeep
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPlottedTarget
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSixRingStyle
import com.andrewnguyen.bowpress.core.designsystem.bp.FEED_MIN_DOT_RADIUS
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFace
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFaceType
import com.andrewnguyen.bowpress.core.designsystem.bp.ringTint
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityActor
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySession
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.ToggleLikeResponse
import com.andrewnguyen.bowpress.core.model.Zone
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatarImage
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementBadgeChip
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionBodyText
import com.andrewnguyen.bowpress.feature.social.ui.session.FeedPhotoGallery
import com.andrewnguyen.bowpress.feature.social.ui.session.SessionPhotoLoader
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

// ── ActivityCard ─────────────────────────────────────────────────────────────
//
// The Social Activity Card · 50/50 — explorations/Social Activity Card - 50-50.
// Every feed row renders inside one chrome: a hairline-bordered card with a
// header (avatar · location eyebrow · name · verb · stamp · when), a typed body,
// and a reactions bar footer. The body is what varies:
//   - a range session → the 50/50 body: the WA target with the friend's arrows
//     plotted on the left, the range eyebrow + hero score + per-end ledger on
//     the right;
//   - a 3D course     → the course-map body;
//   - anything else   → a short italic text body.
// Mirrors iOS `ActivityCard`.

/**
 * The rendered side length of the WA target face in the 50/50 body. The
 * arrow-plot overlay is pinned to exactly this square so its Canvas coordinate
 * space always matches the face — independent of how `BPTargetFace`'s content
 * slot is laid out.
 */
private val TARGET_FACE_SIZE = 168.dp

/**
 * The universal feed card. Caller passes the feed [item]; the card picks its
 * body from [activityPreview]. [photoLoader] backs the photo body for a
 * photographed session — null when the caller has none (e.g. previews).
 */
/**
 * iOS parity (A5) — the dependencies the reactions footer needs (toggle
 * like, open comments, who-am-I for the optimistic kudos stack). Passed
 * to [ActivityCard] as a bundle so the bar can be suppressed wholesale
 * by passing `reactions = null`.
 *
 * Mirrors iOS `Reactions` (5fe1ba7) — the Log tab passes nil so its rows
 * render without the like/comment bar (local sessions aren't likeable);
 * the Feed tab passes a real `Reactions` value.
 */
data class Reactions(
    val onToggleLike: suspend (String, Boolean) -> ToggleLikeResponse,
    val onOpenComments: (String) -> Unit,
    /**
     * The signed-in caller as an actor — used to put the caller's own
     * avatar into the kudos stack on an optimistic self-like. Null when
     * unknown (e.g. previews); the stack then just bumps the count.
     */
    val selfActor: ActivityActor? = null,
)

@Composable
fun ActivityCard(
    item: ActivityItem,
    onClick: () -> Unit,
    onLocationTap: (SessionLocation) -> Unit,
    modifier: Modifier = Modifier,
    // iOS parity (A5) — when non-null, the like/comment bar renders;
    // when null, it's suppressed entirely (the Log tab reuses the
    // header + body chrome but doesn't carry reactions on local rows).
    reactions: Reactions? = null,
    photoLoader: SessionPhotoLoader? = null,
    // Tapping a §4 photo-strip cell — the card emits which session's photos to
    // open and at which index. The full-screen viewer is owned by the feed
    // screen (a single screen-level instance), NOT by the card: a viewer kept
    // in this `LazyColumn` item's composition would be torn down the moment
    // the card scrolls off-screen, dismissing it mid-look. No-op default so
    // previews / non-photo callers need not wire it.
    onOpenPhotoViewer: (sharedSessionId: String, photos: List<com.andrewnguyen.bowpress.core.model.ActivityPhoto>, startIndex: Int) -> Unit = { _, _, _ -> },
    // Mentions contract §3.2 — tapping an `@handle` in the post title opens
    // that archer's profile. No-op default so previews / non-mention callers
    // need not wire it.
    onMentionTap: (handle: String) -> Unit = {},
    // Parity E2 — tapping the actor avatar OR name in the card header opens
    // that archer's profile. Null when the row is own / inert (e.g. self
    // posts in the feed); the header then renders avatar + name un-tapped.
    // Default null so previews / older callers compile unchanged.
    onActorClick: ((userId: String) -> Unit)? = null,
    // iOS parity (A5) — overflow affordances for own rows. When at least
    // one of [onEdit] / [onDelete] is non-null the header renders a 3-dot
    // overflow that opens a DropdownMenu with the corresponding items.
    // The Log tab wires these so a mis-logged local session can still be
    // edited / deleted after the row migrated to ActivityCard chrome.
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val preview = activityPreview(item)
    Column(
        modifier = modifier
            // D3 — clamp the card's max width so on tablets / split-screen
            // the photo strip can't drive an oversized layout. Mirrors iOS
            // commit c68e27f. Phones (<720dp) keep fillMaxWidth behaviour.
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .background(AppPaper)
            .border(1.dp, AppLine)
            .clickable(onClick = onClick)
            .testTag(TestTags.FeedRowPreview),
    ) {
        // Parity E4 — small-caps "VIA @{handle}" eyebrow above the header row,
        // shown when the row appeared in the feed because someone you follow
        // was @-mentioned in the post (not because the poster is your friend).
        // The handle tap opens that mentioning archer's profile.
        item.session?.viaMentionHandle?.takeIf { it.isNotBlank() }?.let { viaHandle ->
            ViaMentionEyebrow(handle = viaHandle, onMentionTap = onMentionTap)
            HorizontalDivider(color = AppLine2, thickness = 1.dp)
        }
        ActivityCardHeader(
            item = item,
            onLocationTap = onLocationTap,
            onMentionTap = onMentionTap,
            onActorClick = onActorClick,
            onEdit = onEdit,
            onDelete = onDelete,
        )
        // Header bottom hairline.
        HorizontalDivider(color = AppLine2, thickness = 1.dp)
        ActivityCardBody(item, preview, photoLoader)
        // Section 4 — the photo strip. When the shared session has attached
        // photos it sits between the score body and the kudos / reactions row,
        // hairline-separated by a top border. No photos → no strip, no extra
        // hairline. The strip is additive: a photographed range session still
        // shows its 50/50 score body above.
        val session = item.session
        // The `ready`-filtered, position-sorted photo list — computed once and
        // shared with the strip and (via the open-viewer event) the screen's
        // viewer, so cell-tap indices line up with the viewer's pages.
        val readyPhotos = remember(session?.photos) {
            session?.photos
                ?.filter { it.status == com.andrewnguyen.bowpress.core.model.PhotoStatus.ready }
                ?.sortedBy { it.position }
                .orEmpty()
        }
        if (session != null && photoLoader != null && readyPhotos.isNotEmpty()) {
            HorizontalDivider(color = AppLine, thickness = 1.dp)
            PhotoStrip(
                sharedSessionId = session.sharedSessionId,
                readyPhotos = readyPhotos,
                loader = photoLoader,
                onOpenViewer = { index ->
                    onOpenPhotoViewer(session.sharedSessionId, readyPhotos, index)
                },
            )
        }
        // The reactions bar shows ONLY on a session card AND only when a
        // caller supplies a [Reactions] bundle. iOS parity (A5): the Log
        // tab reuses ActivityCard but never carries reactions on its local
        // rows (`reactions = null`). The Feed tab passes a real Reactions.
        // Club / league / config-change / milestone rows still skip the
        // bar regardless because they aren't likeable / commentable.
        if (item.session != null && reactions != null) {
            // Reactions bar — top hairline, then the borderless action row.
            HorizontalDivider(color = AppLine2, thickness = 1.dp)
            ReactionsBar(
                item = item,
                onToggleLike = reactions.onToggleLike,
                onOpenComments = reactions.onOpenComments,
                selfActor = reactions.selfActor,
            )
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

/**
 * Parity E4 — the "VIA @{handle}" eyebrow row, shown above the card header on
 * a row that surfaced through @-mention fanout. The handle text is tappable;
 * the rest of the row is inert. Tap → opens the mentioning archer's profile.
 */
@Composable
private fun ViaMentionEyebrow(
    handle: String,
    onMentionTap: (handle: String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "VIA ",
            style = interUI(11.sp, FontWeight.Medium).copy(letterSpacing = 0.24.em),
            color = AppInk3,
        )
        Text(
            text = "@$handle",
            style = interUI(11.sp, FontWeight.Medium).copy(letterSpacing = 0.24.em),
            color = AppPondDk,
            modifier = Modifier.clickable { onMentionTap(handle) },
        )
    }
}

@Composable
private fun ActivityCardHeader(
    item: ActivityItem,
    onLocationTap: (SessionLocation) -> Unit,
    onMentionTap: (handle: String) -> Unit,
    onActorClick: ((userId: String) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    // The PR/milestone/social stamp shows only on a row that carries one; the
    // relative time always pins to the right.
    val stamp = item.stamp?.takeIf { it.isNotBlank() }
    val stampColor = stampColorFor(item)
    // The avatar reads pine only when the card carries a pine (milestone)
    // stamp; otherwise it stays the neutral pond-dk. Mirrors iOS `avatarTone`.
    val avatarColor = if (stampColor == AppPine) AppPine else AppPondDk
    // Parity E2 — actor avatar + name navigate to the actor's profile. Wire
    // the click only when a callback was supplied AND the row carries a real
    // actor id (older API rows have a blank actorUserId → nowhere to drill).
    val actorTap: (() -> Unit)? = onActorClick?.takeIf { item.actorUserId.isNotBlank() }
        ?.let { cb -> { cb(item.actorUserId) } }
    Row(
        modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 32dp avatar — pond/pine border, paper-2 ground, italic Fraunces initials.
        // Parity E5 — when the API ships an avatarUrl, render the URL with
        // ?v=<avatarVersion> as a cache-buster; fall back to initials otherwise.
        SocialAvatarImage(
            displayName = item.actorDisplayName,
            userId = item.actorUserId.takeIf { it.isNotBlank() },
            avatarUrl = item.actorAvatarUrl,
            avatarVersion = item.actorAvatarVersion,
            size = 32,
            borderTint = avatarColor,
            modifier = if (actorTap != null) Modifier.clickable(onClick = actorTap) else Modifier,
        )
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            item.session?.location?.let { loc ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLocationTap(loc) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = AppPondDk,
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = loc.name.uppercase(),
                        style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.18.em),
                        color = AppPondDk,
                        maxLines = 2,
                    )
                }
            }
            // Italic Fraunces name. Parity E2 — tappable when an actor click
            // callback is wired (own posts pass null → name stays inert).
            Text(
                text = item.actorDisplayName,
                style = frauncesDisplay(16.sp, italic = true, weight = FontWeight.Medium),
                color = AppInk,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .let { if (actorTap != null) it.clickable(onClick = actorTap) else it },
            )
            // Italic Fraunces verb / session-name headline. Only an
            // archer-authored custom title can carry `@handle` mentions
            // (mentions contract §3.2) — it renders through MentionBodyText so
            // the spans are pond-toned + tappable. A generic server verb
            // phrase ("shot a new PR") can never contain a mention, so it gets
            // a plain Text — no needless ClickableText wrapper / parse.
            val titleStyle = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Normal)
                .copy(color = AppInk2)
            if (item.titleIsCustom) {
                MentionBodyText(
                    text = item.title,
                    style = titleStyle,
                    onMentionTap = onMentionTap,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Text(
                    text = item.title,
                    style = titleStyle,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Migration 0039 — the post caption, a 2-line truncated line under
            // the headline; `@mentions` render as tappable pond-toned spans.
            val description = item.session?.description
            if (!description.isNullOrBlank()) {
                MentionBodyText(
                    text = description,
                    style = frauncesDisplay(12.5.sp, italic = true, weight = FontWeight.Normal)
                        .copy(color = AppInk3),
                    onMentionTap = onMentionTap,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            // Equipment-inline strip — bow · type · arrow, one truncated
            // mono row beneath the description. The composable renders
            // nothing when no equipment metadata ships on the session.
            EquipmentInlineLine(
                bowName = item.session?.bowName,
                bowType = item.session?.bowType?.label,
                arrowName = item.session?.arrowName,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.width(6.dp))

        if (stamp != null) {
            Box(
                modifier = Modifier
                    .border(1.dp, stampColor)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stamp.uppercase(),
                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = stampColor,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = relativeStamp(item.createdAt),
            style = jetbrainsMono(9.5.sp),
            color = AppInk3,
        )
        // iOS parity (A5) — overflow affordance for own rows. Renders only
        // when the caller wired Edit and/or Delete (the Log tab does; the
        // Feed leaves both null today). Click-region stays inside the
        // header so it doesn't fight the card-level tap.
        if (onEdit != null || onDelete != null) {
            Spacer(Modifier.width(4.dp))
            ActivityCardOverflow(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

/**
 * iOS parity (A5) — 3-dot overflow exposing Edit / Delete for own Log
 * rows. Wraps Material3 [DropdownMenu] so the anchor stays inside the
 * header row, and gates each item on a non-null callback. Tapping the
 * dot opens the menu without bubbling up to the card-level click.
 */
@Composable
private fun ActivityCardOverflow(
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "More",
            tint = AppInk3,
            modifier = Modifier
                .size(20.dp)
                .clickable { expanded = true }
                .testTag(TestTags.FeedRowOverflow),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        expanded = false
                        onEdit()
                    },
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text("Delete", color = AppMaple) },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/**
 * Stamp colour for a feed row, decided purely by the activity *kind* — maple
 * for a PR / flier (the alert colour), pond for a social / club / league
 * event, pine reserved for a milestone. `highlighted` deliberately does NOT
 * influence the colour: a league podium / milestone is `highlighted` but must
 * not borrow the maple alert tone. The tone is always resolved, but the header
 * only paints a stamp when the row actually carries a stamp string.
 */
private fun stampColorFor(item: ActivityItem): androidx.compose.ui.graphics.Color =
    when (item.kind) {
        // A PR / flier — the alert colour, reserved for things to notice.
        ActivityKind.friend_pr -> AppMaple
        // Social / club / league events — a join, a podium, a club or league
        // created, a setup change. All read the neutral pond.
        ActivityKind.club_member_joined,
        ActivityKind.club_created,
        ActivityKind.league_created,
        ActivityKind.league_podium,
        ActivityKind.league_event,
        ActivityKind.friend_setup,
        -> AppPondDk
        // Everything else — a plain shared session / club session / unknown —
        // reads pond too. Pine stays reserved for an explicit milestone kind.
        else -> AppPondDk
    }

// ── Body ─────────────────────────────────────────────────────────────────────

@Composable
private fun ActivityCardBody(
    item: ActivityItem,
    preview: ActivityPreview,
    photoLoader: SessionPhotoLoader?,
) {
    when (preview) {
        is ActivityPreview.Target -> RangeSessionBody(
            distance = preview.distance,
            score = preview.score,
            // The hero "/ {max}" uses the session's full arrow count — the
            // ledger may carry only the first 10 ends. Mirrors iOS.
            maxScore = (item.session?.arrowCount ?: 0) * 10,
            endRings = preview.endRings,
            plotPoints = item.session?.plotPoints.orEmpty(),
            // §B2 — prefer the structured TargetFaceType the session was
            // scored against; fall back to the free-text label heuristic
            // only when the structured field is missing (pre-migration
            // 0038 sessions). Mirrors iOS commit a588a30.
            structuredFaceType = item.session?.targetFaceType,
            face = item.session?.face,
            arrowDiameterMm = item.session?.arrowDiameterMm,
            // Migration 0038 — a triangle/vertical layout swaps the single
            // face for the real 3-spot Vegas card. Null → single-face path.
            targetLayout = item.session?.targetLayout,
        )
        is ActivityPreview.Course -> CourseCardBody(
            score = preview.score,
            stations = preview.stations,
        )
        is ActivityPreview.Photo -> {
            val session = item.session
            val photos = session?.photos.orEmpty()
            if (session != null && photoLoader != null && photos.isNotEmpty()) {
                PhotoCardBody(
                    sharedSessionId = session.sharedSessionId,
                    photos = photos,
                    loader = photoLoader,
                )
            } else {
                TextCardBody(item)
            }
        }
        // None — a short italic text body. The headline carried the verb; the
        // body restates the activity's own `meta` line (or the session's mono
        // stat line) plus any achievement badges.
        is ActivityPreview.None -> TextCardBody(item)
    }
}

/**
 * The 50/50 body for a range session — two equal columns on a paper-2 ground
 * split by a 1dp vertical hairline. Left: the WA target with the friend's
 * arrows plotted. Right: range eyebrow → hero score → per-end ledger.
 *
 * [structuredFaceType] is the authoritative `shooting_sessions.target_face_type`
 * (§B2): we render the face from this value when present, falling back to the
 * free-text [face] heuristic only for pre-migration-0038 sessions. [distance]
 * routes the §B3 6-ring → Vegas-or-outdoor80 visual variant.
 * [arrowDiameterMm] sizes the plot dots from a 6mm reference (§B1).
 * [targetLayout] (parity, migration 0038) — when triangle/vertical, the body
 * renders the real 3-spot Vegas card via [BPPlottedTarget] instead of a
 * single bullseye with the arrows fake-clustered in a triangle. Null /
 * `SINGLE` keep the existing single-face card.
 */
@Composable
private fun RangeSessionBody(
    distance: String?,
    score: Int,
    maxScore: Int,
    endRings: List<List<Int>>?,
    plotPoints: List<List<Double>>,
    structuredFaceType: TargetFaceType?,
    face: String?,
    arrowDiameterMm: Double?,
    targetLayout: TargetLayout?,
) {
    val ends = endRings.orEmpty()
    // §B2 — structured field is authoritative; fall back to the legacy
    // face-label heuristic for pre-migration sessions.
    val resolvedFaceType = resolvedBPFaceType(structuredFaceType, face)
    // §B3 — 6-ring at 50/70m → outdoor80 7-zone variant; everything else
    // (20yd or unknown) → Vegas 6-zone. tenRing is distance-invariant.
    val sixRingStyle = sixRingVisualStyleForFeed(distance)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .height(IntrinsicSize.Min),
    ) {
        // Left — the WA target face with arrows plotted as ink dots.
        // fillMaxHeight makes the cell fill the (taller) scorecard column's
        // height so the target sits vertically centred, not toward the top.
        // Multi-spot skips the 12/18dp gutter so the 3-spot card can reach
        // the column edges, matching iOS where `.padding(.vertical, 18)
        // .padding(.horizontal, 12)` only wraps the single-face ZStack.
        val isMulti = targetLayout != null && targetLayout.isMultiSpot
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .heightIn(min = 230.dp)
                .padding(
                    vertical = if (isMulti) 0.dp else 18.dp,
                    horizontal = if (isMulti) 0.dp else 12.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isMulti && targetLayout != null) {
                // 3-spot card — same renderer the detail screen uses, so
                // a Vegas triangle/vertical session looks the same in the
                // feed as it does when opened. Arrows come from the real
                // plotPoints (each bucketed to its nearest spot by
                // BPPlottedTarget → MultiSpotGeometry.assignArrows); an
                // empty plotPoints just renders the empty 3-spot face,
                // which still beats fake-clustering arrows on one bullseye.
                BPPlottedTarget(
                    arrows = multiSpotArrows(ends, plotPoints),
                    // BPPlottedTarget wants the core-model enum, not the
                    // design-system BPTargetFaceType the single-face path
                    // uses. The structured field is authoritative; fall
                    // back to the legacy label heuristic (mirrors iOS
                    // `targetFaceType ?? TargetFaceType.matching(label:)
                    // ?? .tenRing`).
                    faceType = structuredFaceType
                        ?: TargetFaceType.matching(face)
                        ?: TargetFaceType.TEN_RING,
                    layout = targetLayout,
                    sixRingStyle = sixRingStyle,
                    // §B1 — shaft diameter from the feed payload sizes the
                    // dot against the 180mm Vegas spot. The dot formula in
                    // BPPlottedTarget reads the Canvas pixel size at draw
                    // time (shaftMm / 180 * spotRadiusPx), so it scales
                    // proportionally as the face grows from 168 → 320dp —
                    // no math change needed here. FEED_MIN_DOT_RADIUS
                    // mirrors iOS `minDotSize: 3` (diameter).
                    arrowDiameterMm = arrowDiameterMm,
                    minDotRadius = FEED_MIN_DOT_RADIUS,
                    // Match iOS `SessionHeatMapView ... .scaleEffect(1.12)
                    // .frame(maxWidth: 320, maxHeight: 320)` — fill the
                    // column width up to a 320dp cap, then overflow 12%
                    // so the spots reach the column edges. Drops the
                    // previous fixed 168dp pin that left the 3-spot card
                    // visibly smaller than its iOS counterpart.
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp)
                        .scale(1.12f),
                )
            } else {
                // The design ships a ~188dp face; the 50/50 column at phone
                // width can't fit that, so the face renders at
                // TARGET_FACE_SIZE — the size iOS `ActivityCardRangeBody`
                // settled on. `BPTargetFace` is fixed-size.
                BPTargetFace(
                    size = TARGET_FACE_SIZE,
                    face = resolvedFaceType,
                    sixRingStyle = sixRingStyle,
                ) {
                    // The friend's arrows — the real plotted positions from the
                    // feed payload when present, else a deterministic scatter
                    // within each scoring ring's band.
                    ArrowPlotOverlay(
                        ends = ends,
                        plotPoints = plotPoints,
                        arrowDiameterMm = arrowDiameterMm,
                        mmPerNormUnit = feedCardMmPerNormUnit(resolvedFaceType, distance),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(AppLine),
        )
        // Right — range eyebrow, hero score, per-end scorecard.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
        ) {
            Text(
                text = rangeEyebrow(distance),
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            )
            // Hero score on one line: the score as an upright Fraunces numeral
            // in --deep, then a quiet italic " / " and the upright max.
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "$score",
                    style = frauncesDisplay(48.sp, italic = false, weight = FontWeight.Medium),
                    color = AppDeep,
                )
                // The "/ max" tail shows only when the max is coherent — a
                // stale arrowCount could otherwise produce an impossible
                // fraction (e.g. 285/270), so fall back to the bare score.
                if (maxScore >= score) {
                    Text(
                        text = " / ",
                        style = frauncesDisplay(22.sp, italic = true, weight = FontWeight.Normal),
                        color = AppInk3,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                    Text(
                        text = "$maxScore",
                        style = frauncesDisplay(22.sp, italic = false, weight = FontWeight.Medium),
                        color = AppInk3,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
            if (ends.isNotEmpty()) {
                EndsTable(ends, modifier = Modifier.padding(top = 14.dp))
            }
        }
    }
}

/**
 * A 3D-course body — the course-map preview on the paper-2 ground, adopting
 * the card chrome. Reuses [CourseBand].
 */
@Composable
private fun CourseCardBody(
    score: Int,
    stations: List<com.andrewnguyen.bowpress.core.model.CourseStation>,
) {
    Box(modifier = Modifier.fillMaxWidth().background(AppPaper2)) {
        CourseBand(score = score, stations = stations)
    }
}

/**
 * A short italic text body for a non-session / milestone row — the design's
 * stampless or pine-stamped variant keeps a brief text body. Renders the
 * activity's `meta` line (or a session stat line) plus any achievement badges.
 *
 * The body ALWAYS renders at least one line: the `meta` line, else a session
 * stat line, else the activity's own verb `title`. A zero-height body would
 * collapse the card's two body hairlines into a 2dp double rule.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextCardBody(item: ActivityItem) {
    val body = item.meta?.takeIf { it.isNotBlank() }
        ?: item.session?.let { s -> "${s.score} · ${s.xCount}X · ${s.arrowCount} arrows" }
        ?: item.title.takeIf { it.isNotBlank() }
        ?: " " // last-ditch — keeps the body a non-zero height.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(
            text = body,
            style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Normal),
            color = AppInk2,
        )
        // §15 — achievement badges on a milestone row.
        if (item.achievements.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
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
}

/**
 * The photo body — a photographed session's gallery on the paper-2 ground,
 * inside the card chrome. Mirrors the old `FeedPhotoGallery` placement.
 */
@Composable
private fun PhotoCardBody(
    sharedSessionId: String,
    photos: List<com.andrewnguyen.bowpress.core.model.ActivityPhoto>,
    loader: SessionPhotoLoader,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .padding(14.dp),
    ) {
        FeedPhotoGallery(
            sharedSessionId = sharedSessionId,
            photos = photos,
            loader = loader,
        )
    }
}

/** "RANGE · 50M" — the label muted, the distance in pond. */
private fun rangeEyebrow(distance: String?) = buildAnnotatedString {
    withStyle(SpanStyle(color = AppInk3)) { append("RANGE") }
    val d = distance?.trim()?.takeIf { it.isNotEmpty() }
    if (d != null) {
        withStyle(SpanStyle(color = AppInk3)) { append(" · ") }
        withStyle(SpanStyle(color = AppPondDk)) { append(d.uppercase()) }
    }
}

// ── Arrow plotting ───────────────────────────────────────────────────────────

/**
 * Plots the friend's arrows on top of [BPTargetFace] as small ink dots with a
 * cream stroke. The feed payload's [ActivitySession.endRings] gives per-end
 * ring values only — no x/y — so each arrow is scattered **deterministically**
 * within its scoring ring's radius band, seeded by `(endIndex, arrowIndex)`.
 * The same session always plots the same picture; mirrors iOS
 * `ActivityCard.ArrowScatter`.
 *
 * Dot radius (§B1, post-ef77dfb): geometric `shaftMm / mmPerNormUnit *
 * faceRadius` with a 2dp floor — the same dots the feed had before the
 * detail-view fix. The 6mm-reference formula lives on the (yet-unported)
 * larger detail overlay, not the thumbnail feed card.
 */
@Composable
private fun ArrowPlotOverlay(
    ends: List<List<Int>>,
    plotPoints: List<List<Double>>,
    arrowDiameterMm: Double?,
    mmPerNormUnit: Double,
) {
    // Prefer the real plotted positions from the feed; fall back to the
    // deterministic synthesised scatter when the payload carries no x/y.
    val arrows = remember(ends, plotPoints) {
        if (plotPoints.isNotEmpty()) plottedArrows(plotPoints, ends) else scatterArrows(ends)
    }
    if (arrows.isEmpty()) return
    // Pin the Canvas to exactly the face square so the dot coordinate space
    // matches the WA face — never assume `BPTargetFace`'s content slot is
    // itself face-sized.
    Canvas(modifier = Modifier.size(TARGET_FACE_SIZE)) {
        val faceRadius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val dotRadius = feedCardArrowDotRadiusPx(
            faceRadiusPx = faceRadius,
            arrowDiameterMm = arrowDiameterMm,
            mmPerNormUnit = mmPerNormUnit,
            density = density,
        )
        arrows.forEach { arrow ->
            val at = Offset(
                center.x + arrow.x * faceRadius,
                center.y + arrow.y * faceRadius,
            )
            // An X (the standout shot) and a miss (the alert) both read maple;
            // a plain scoring arrow reads ink. Mirrors iOS `TargetArrowDotsOverlay`.
            val fill = if (arrow.ring >= 11 || arrow.ring <= 0) AppMaple else AppInk
            drawCircle(color = fill, radius = dotRadius, center = at)
            drawCircle(
                color = AppCream,
                radius = dotRadius,
                center = at,
                style = Stroke(width = 0.9.dp.toPx()),
            )
        }
    }
}

// ── Per-end ledger ───────────────────────────────────────────────────────────

/**
 * The ledger arrow-glyph size for an end of [arrowsPerEnd] arrows. A wide end
 * (an indoor 5- or 6-arrow end) crushes the equal-weight cells in the narrow
 * 50/50 column, so the glyph scales down a touch so a two-digit "10" still
 * renders single-line in its cell. Pure so it is unit-testable.
 */
internal fun ledgerGlyphSizeSp(arrowsPerEnd: Int): Float = when {
    arrowsPerEnd >= 6 -> 10.5f
    arrowsPerEnd == 5 -> 12f
    else -> 13.5f
}

/** Per-end ledger — end number + arrow ring values, hairline-separated. */
@Composable
private fun EndsTable(ends: List<List<Int>>, modifier: Modifier = Modifier) {
    val rows = ends.take(10)
    // Scale the glyph for the widest end so a 6-arrow indoor end stays legible.
    val widestEnd = rows.maxOfOrNull { it.size } ?: 0
    val glyphSize = ledgerGlyphSizeSp(widestEnd).sp
    Column(modifier = modifier.fillMaxWidth().testTag(TestTags.FeedRowScorecard)) {
        HorizontalDivider(color = AppLine, thickness = 1.dp)
        rows.forEachIndexed { idx, rings ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${idx + 1}",
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(18.dp),
                )
                // Each ring sits in its ring-tonal cell — the same band cue
                // the session-detail scorecard uses.
                rings.forEach { ring ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 16.dp)
                            .heightIn(min = 24.dp)
                            .background(ringTint(ring)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ringGlyph(ring),
                            style = frauncesDisplay(glyphSize, italic = true, weight = FontWeight.Medium)
                                .copy(
                                    textDecoration = if (ring <= 0) TextDecoration.LineThrough else null,
                                ),
                            color = endRingColor(ring),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
            if (idx < rows.lastIndex) {
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
            }
        }
    }
}

/** X for the inner ring, M (struck through) for a miss, the ring number otherwise. */
private fun ringGlyph(ring: Int): String = when {
    ring >= 11 -> "X"
    ring <= 0 -> "M"
    else -> ring.toString()
}

/** Ledger ink — X reads maple, a struck-through miss reads ink-3. */
private fun endRingColor(ring: Int) = when {
    ring >= 11 -> AppMaple
    ring <= 0 -> AppInk3
    else -> AppInk
}

// ── Reactions bar ────────────────────────────────────────────────────────────

/**
 * The card-footer reactions row — Section 03's reworked Strava-style kudos
 * row. A left kudos stack (≤3 overlapping 22dp avatars + a `+N` chip + a
 * "Name & N others" summary, or a quiet "Be the first" when empty), with the
 * like + comment icons pinned right. The "OPEN" affordance is gone — the whole
 * card carries the tap into the thread.
 *
 * The like button toggles optimistically and reconciles against the server,
 * the same wiring as the old `LikeCommentBar`, folded into the card's footer.
 */
@Composable
private fun ReactionsBar(
    item: ActivityItem,
    onToggleLike: suspend (String, Boolean) -> ToggleLikeResponse,
    onOpenComments: (String) -> Unit,
    selfActor: ActivityActor?,
) {
    val scope = rememberCoroutineScope()
    val subjectId = item.resolvedSubjectId
    // Optimistic local state — re-seeded whenever the underlying row changes.
    val seedKey = "${item.id}:${item.likeCount}:${item.likedByMe}:${item.commentCount}"
    var liked by remember(seedKey) { mutableStateOf(item.likedByMe) }
    var likeCountState by remember(seedKey) { mutableIntStateOf(item.likeCount) }
    var inFlight by remember(seedKey) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left — the kudos stack. It tracks the optimistic like count, AND on
        // a self-like the caller's own avatar is spliced into the liker list
        // (M4) so a first-like on an empty post shows the caller's avatar
        // instead of "Be the first" next to a filled heart.
        KudosRow(
            likers = optimisticLikers(
                serverLikers = item.likers,
                serverLikedByMe = item.likedByMe,
                likedNow = liked,
                selfActor = selfActor,
            ),
            likeCount = likeCountState,
            modifier = Modifier
                .weight(1f, fill = false)
                .testTag(TestTags.FeedRowKudos),
        )
        Spacer(Modifier.width(10.dp))
        // Right — like + comment icons, pinned.
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ── Like ──
            ReactionAction(
                icon = { tint ->
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (liked) "Unlike" else "Like",
                        tint = tint,
                        modifier = Modifier.size(15.dp),
                    )
                },
                label = null,
                tint = if (liked) AppPine else AppInk3,
                testTag = TestTags.FeedRowLikeButton,
                onClick = {
                    if (inFlight) return@ReactionAction
                    val wasLiked = liked
                    val priorCount = likeCountState
                    liked = !wasLiked
                    likeCountState = (priorCount + if (wasLiked) -1 else 1).coerceAtLeast(0)
                    inFlight = true
                    scope.launch {
                        runCatching { onToggleLike(subjectId, wasLiked) }
                            .onSuccess { result ->
                                likeCountState = result.likeCount
                                liked = result.likedByMe
                            }
                            .onFailure {
                                liked = wasLiked
                                likeCountState = priorCount
                            }
                        inFlight = false
                    }
                },
            )
            Spacer(Modifier.width(14.dp))
            // ── Comment ──
            ReactionAction(
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = tint,
                        modifier = Modifier.size(15.dp),
                    )
                },
                label = item.commentCount.takeIf { it > 0 }?.toString(),
                tint = AppInk3,
                testTag = TestTags.FeedRowCommentButton,
                onClick = { onOpenComments(subjectId) },
            )
        }
    }
}

/**
 * One borderless reaction action — an icon plus an optional mono count. A null
 * [label] renders the icon alone (a count of 0 is dropped, matching the
 * design's bare like icon).
 */
@Composable
private fun ReactionAction(
    icon: @Composable (tint: androidx.compose.ui.graphics.Color) -> Unit,
    label: String?,
    tint: androidx.compose.ui.graphics.Color,
    testTag: String?,
    onClick: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        icon(tint)
        if (label != null) {
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = jetbrainsMono(10.sp),
                color = AppInk2,
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Picks a target-face shape from a shared session's free-text `face` label —
 * a 6-ring / Vegas / 3-spot face reads as [BPTargetFaceType.SixRing],
 * everything else [BPTargetFaceType.TenRing]. Mirrors iOS
 * `ActivityPreviewBand.faceType(for:)`. Kept exported for tests + any
 * legacy caller that has no access to the structured field.
 */
internal fun faceTypeFor(face: String?): BPTargetFaceType {
    val f = (face ?: "").lowercase()
    // Check "10" before "6-ring" so "60cm" / "WA 60" stays tenRing — the
    // outer "10" hint wins. The 6-side check is intentionally narrow:
    // "6-ring" / "6-spot" / "vegas" / "spot" — NOT a bare "6" — so a
    // "60cm" / "WA 60" label doesn't misfire as sixRing. Mirrors iOS
    // `TargetFaceType.inferring(from:)` precedence.
    return when {
        f.contains("10") -> BPTargetFaceType.TenRing
        f.contains("6-ring") ||
            f.contains("6-spot") ||
            f.contains("vegas") ||
            f.contains("spot") ->
            BPTargetFaceType.SixRing
        else -> BPTargetFaceType.TenRing
    }
}

/**
 * §B2 — resolve the [BPTargetFaceType] for a feed-card render, preferring
 * the structured `targetFaceType` enum field over the free-text `face`
 * label. Mirrors iOS commits a588a30 / 2ec95c2: the authoritative
 * `shooting_sessions.target_face_type` value wins; the heuristic only
 * fires when the structured field is missing (pre-migration 0038).
 */
internal fun resolvedBPFaceType(
    structured: TargetFaceType?,
    face: String?,
): BPTargetFaceType = when (structured) {
    TargetFaceType.SIX_RING -> BPTargetFaceType.SixRing
    TargetFaceType.TEN_RING -> BPTargetFaceType.TenRing
    null -> faceTypeFor(face)
}

/**
 * §B3 — feed-card String → SixRing variant. The feed payload carries
 * `distance` as free text ("50m", "20yd", null), so we parse to the
 * structured [ShootingDistance] enum first, then route through the single
 * canonical [BPSixRingStyle.forDistance] helper. An unrecognised distance
 * string falls through to Vegas — the safe default that matches every
 * other surface.
 *
 * Mirrors iOS `socialSixRingStyle(forDistance:)` (commit d5884e3). Only
 * consulted when the resolved face is sixRing; tenRing is invariant.
 */
internal fun sixRingVisualStyleForFeed(distance: String?): BPSixRingStyle {
    val parsed = parseFeedDistance(distance)
    return BPSixRingStyle.forDistance(parsed)
}

/**
 * Map a free-text feed `distance` label to a [ShootingDistance] enum.
 * Wire values: `"20yd"`, `"50m"`, `"70m"` (matching
 * [ShootingDistance.label] / `.wire`). Anything else → null.
 */
private fun parseFeedDistance(distance: String?): com.andrewnguyen.bowpress.core.model.ShootingDistance? {
    val d = distance?.trim()?.lowercase() ?: return null
    return when (d) {
        "20yd" -> com.andrewnguyen.bowpress.core.model.ShootingDistance.YARDS_20
        "50m" -> com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_50
        "70m" -> com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_70
        else -> null
    }
}

/**
 * §B1 — feed-card arrow-plot dot radius. The feed card uses the
 * geometric `arrowDiameterMm / mmPerNormUnit * faceRadius` formula with a
 * 2dp floor — the same dots the feed had before the detail-view fix.
 * Mirrors iOS `TargetRingScatter.swift:140-145` (commit ef77dfb, which
 * reverted the feed-card overlay from the 6mm-reference back to
 * geometric mm-on-mm + 2pt floor; the 6mm-reference belongs on the
 * larger DETAIL overlay, not the thumbnail-size feed card).
 *
 * [mmPerNormUnit] is the geometry's real mm at radius 1.0 — varies by
 * face (Vegas ≈123.5, Outdoor80 = 400, TenRing ≈123.5) so the dots
 * scale physically with the printed face the archer actually shot.
 * Resolved at the call site via the same face/distance pair the WA
 * face renderer receives. Kept agnostic of `TargetGeometry` itself so
 * `feature-social` stays decoupled from `feature-session`.
 */
internal fun feedCardArrowDotRadiusPx(
    faceRadiusPx: Float,
    arrowDiameterMm: Double?,
    mmPerNormUnit: Double,
    density: Float,
): Float {
    // iOS TargetRingScatter.swift:140-145 — `arrowDiameterMm /
    // mmPerNormUnit * faceRadius`, floor 2pt. The earlier 6mm-reference
    // formula (`faceRadius * 0.04 * shaftMm / 6`) ignored `mmPerNormUnit`,
    // which made every dot the same fraction of the face regardless of
    // whether the printed face was an 80cm outdoor (mmPerNormUnit = 400)
    // or a 40cm indoor (≈123.5). Result: Outdoor80 dots came out ~2.7x
    // too big and indoor dots ~0.8x too small. Restoring the geometric
    // formula matches iOS dot-on-mm exactly across both faces.
    //
    // The 5.0mm fallback matches iOS `TargetRingScatter.arrowDiameterMm`
    // default — the feed payload doesn't carry shaft diameter (only the
    // friend-detail endpoint does), so without an explicit value we
    // assume a mid-range competition shaft. Was 6.0mm here, which made
    // single-spot feed-card dots ~20% bigger than iOS.
    val shaftMm = arrowDiameterMm ?: 5.0
    val computed = (shaftMm / mmPerNormUnit).toFloat() * faceRadiusPx
    val floorPx = 2f * density
    return computed.coerceAtLeast(floorPx)
}

/**
 * §B3 — feed-card mmPerNormUnit for a (resolved face, distance string)
 * pair. Values mirror iOS `TargetGeometry.mmPerNormUnit` so the visual
 * dot scales physically with the printed face the archer shot:
 *  - SixRing, 50m/70m → Outdoor80 → 400mm
 *  - SixRing, anything else → Vegas 40cm → 20 / R10_RADIUS ≈ 123.53mm
 *  - TenRing, 20yd → 40cm WA face → 200mm
 *  - TenRing, 50m → 80cm WA face → 400mm
 *  - TenRing, 70m / null → 122cm WA full face → 610mm
 *
 * TenRing was previously distance-agnostic (always 610mm), but that's
 * wrong for the common 20yd indoor 10-ring face — a 5mm shaft on a
 * 40cm face is 5/200 of the face radius, not 5/610. Mirrors iOS commit
 * adding `tenRingIndoor` (40cm) / `tenRingOutdoor80` (80cm) presets to
 * `TargetGeometry.preset(.tenRing, distance:)`.
 */
internal fun feedCardMmPerNormUnit(
    face: BPTargetFaceType,
    distance: String?,
): Double {
    val parsed = parseFeedDistance(distance)
    return when (face) {
        BPTargetFaceType.SixRing -> when (parsed) {
            com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_50,
            com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_70,
            -> 400.0
            // Vegas indoor / null / 20yd → 20 / (119/735) ≈ 123.53mm.
            com.andrewnguyen.bowpress.core.model.ShootingDistance.YARDS_20, null,
            -> 20.0 / (119.0 / 735.0)
        }
        BPTargetFaceType.TenRing -> when (parsed) {
            com.andrewnguyen.bowpress.core.model.ShootingDistance.YARDS_20 -> 200.0
            com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_50 -> 400.0
            com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_70, null -> 610.0
        }
    }
}

/**
 * Build the [ArrowPlot] list `BPPlottedTarget` expects from the feed
 * payload's flat `[x, y]` plot points and per-end ring values. Each plot
 * is paired with its ring in shot order (`endRings.flatten()[i]`); ring
 * is carried only so a future renderer can colour X / miss — the
 * 3-spot renderer itself bucketises by `plotX` / `plotY`, so the
 * pairing's correctness depends on the API emitting `plotPoints` in
 * one-to-one shot order with the flattened scorecard. Plot points
 * with fewer than two coords (a missing arrow) are skipped. Dummy
 * session / bow / arrow ids are fine — the renderer ignores them.
 * Mirrors the synthesised `multiSpotPlots` iOS `ActivityCardRangeBody`
 * builds.
 */
internal fun multiSpotArrows(
    endRings: List<List<Int>>,
    plotPoints: List<List<Double>>,
): List<ArrowPlot> {
    if (plotPoints.isEmpty()) return emptyList()
    val flatRings = endRings.flatten()
    return plotPoints.mapIndexedNotNull { i, p ->
        if (p.size < 2) return@mapIndexedNotNull null
        ArrowPlot(
            id = "feed-$i",
            sessionId = "feed-row",
            bowConfigId = "",
            arrowConfigId = "",
            ring = flatRings.getOrElse(i) { 10 },
            zone = Zone.CENTER,
            plotX = p[0],
            plotY = p[1],
            shotAt = Instant.EPOCH,
        )
    }
}

/** Compact relative time — "2h", "3d", "2w". Mirrors iOS `relativeStamp`. */
private fun relativeStamp(from: Instant): String {
    val mins = ChronoUnit.MINUTES.between(from, Instant.now()).coerceAtLeast(0)
    return when {
        mins < 60 -> "${mins}m"
        mins < 60 * 24 -> "${mins / 60}h"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)}d"
        else -> "${mins / (60 * 24 * 7)}w"
    }
}

package com.andrewnguyen.bowpress.feature.social.ui.friends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSixRingStyle
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFace
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFaceType
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipDirection
import com.andrewnguyen.bowpress.core.model.FriendshipStatus
import com.andrewnguyen.bowpress.core.model.MutualArcher
import com.andrewnguyen.bowpress.core.model.SessionSummary
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.ThisWeekStat
import com.andrewnguyen.bowpress.feature.social.ui.SocialUnavailableNotice
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementsViewModel
import com.andrewnguyen.bowpress.feature.social.ui.achievements.TrophyCaseSection
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.blocks.BlockViewModel
import com.andrewnguyen.bowpress.feature.social.ui.blocks.MuteBlockAction
import com.andrewnguyen.bowpress.feature.social.ui.label
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Parity E1 — Kenrokuen Strava-style rebuild of the friend-profile screen
 * (iOS commit 82b38fd, ~1037 LOC). Mirrors the iOS structure section-by-
 * section so the two screens read like ports of one another:
 *
 *  - Top nav (back · share)
 *  - Hero (square avatar + faint target inset, italic name + @handle + joined)
 *  - 4-cell stat row (Friends / Mutuals / Sessions / Arrows)
 *  - Strava-style follow + message action row
 *  - Mutuals strip (when present)
 *  - Plot strip of recent sessions with real target faces + arrow dots
 *  - Trophy case (existing component, wired in)
 *  - "This week" block (stats row + 7-day chart)
 *  - Compare CTA (Android-specific, preserved from the prior screen)
 *  - Mute / block
 */
@Composable
fun FriendProfileScreen(
    otherUserId: String,
    onBack: () -> Unit,
    onCompare: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
    blockViewModel: BlockViewModel = hiltViewModel(),
    achievementsViewModel: AchievementsViewModel = hiltViewModel(),
) {
    val state by viewModel.profileState.collectAsState()
    val blocksState by blockViewModel.uiState.collectAsState()
    val trophyState by achievementsViewModel.uiState.collectAsState()

    LaunchedEffect(otherUserId) {
        viewModel.loadFriendProfile(otherUserId)
        achievementsViewModel.loadForFriend(otherUserId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        // ── Top nav ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹ BACK",
                style = interUI(11.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "SHARE ›",
                style = interUI(11.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
            )
        }

        if (state.isLoading) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Loading…",
                style = frauncesDisplay(14.sp, italic = true),
                color = AppInk3,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            return
        }

        if (state.error != null && state.friendProfile == null) {
            SocialUnavailableNotice(
                title = "Profile unavailable",
                detail = "This archer's profile isn't visible to you, or it's no longer reachable.",
            )
            return
        }

        state.friendProfile?.let { fp ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Hero(profile = fp.profile)
                StatsRow(
                    profile = fp.profile,
                    friendCount = fp.friendCount,
                    mutualCount = fp.mutuals.size,
                )
                ActionsRow(
                    friendship = state.friendship,
                    busy = state.followBusy,
                    error = state.followError,
                    onFollowTap = { viewModel.cycleFollow(fp.profile.handle, otherUserId) },
                )

                if (fp.mutuals.isNotEmpty()) {
                    MutualsStrip(fp.mutuals)
                }
                if (fp.recentSessions.isNotEmpty()) {
                    PlotStrip(fp.recentSessions)
                }

                // Trophy case (existing component).
                if (trophyState.achievements.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        TrophyCaseSection(
                            achievements = trophyState.achievements,
                            catalog = trophyState.catalog,
                            ownerLabel = "${firstName(fp.profile.displayName)}'s",
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                ThisWeekBlock(stat = fp.thisWeek)

                // Compare CTA — preserved from the prior screen so the
                // friend-compare flow still works while iOS's rowlist isn't
                // ported.
                fp.recentSessions
                    .firstOrNull { it.distance != null && it.targetFaceType != null }
                    ?.let { item ->
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .background(AppPondDk)
                                .clickable { onCompare(otherUserId) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Compare",
                                    style = frauncesDisplay(18.sp, italic = true),
                                    color = AppPaper,
                                )
                                Text(
                                    "${item.distance} · ${item.targetFaceType} · last 30 days",
                                    style = interUI(9.sp, FontWeight.SemiBold)
                                        .copy(letterSpacing = 0.2.em),
                                    color = AppPaper2.copy(alpha = 0.7f),
                                )
                            }
                            Text(
                                "›",
                                style = frauncesDisplay(30.sp, italic = true),
                                color = AppPaper,
                            )
                        }
                    }

                // Mute / block.
                Spacer(Modifier.height(18.dp))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "MANAGE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(6.dp))
                    MuteBlockAction(
                        kind = BlockKind.archer,
                        targetName = "@${fp.profile.handle}",
                        block = blocksState.blockFor(otherUserId),
                        onSetMode = { mode ->
                            blockViewModel.setBlock(
                                BlockKind.archer,
                                otherUserId,
                                fp.profile.handle,
                                mode,
                            )
                        },
                        onRemove = { blockViewModel.removeBlock(it) },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun Hero(profile: SocialProfile) {
    val joinedLabel = remember(profile.joinedAt) {
        val ld = profile.joinedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        val month = ld.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "joined $month ${ld.year} · ${profile.division?.label() ?: "Archer"}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        HeroAvatar(profile = profile)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            profile.bowSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                Text(
                    summary.uppercase(),
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPondDk,
                )
            }
            Text(
                profile.displayName,
                style = frauncesDisplay(30.sp, italic = true),
                color = AppInk,
            )
            Text(
                "@${profile.handle}",
                style = jetbrainsMono(10.5.sp),
                color = AppInk3,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(joinedLabel, style = jetbrainsMono(10.sp), color = AppInk3)
        }
    }
}

/**
 * Parity E1 — 72dp square avatar with a faint target-face inset underneath.
 * Falls back to the archer's monogram when no avatar is set (initial port —
 * the avatarUrl render is parity E5's surface).
 */
@Composable
private fun HeroAvatar(profile: SocialProfile) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(AppPond),
        contentAlignment = Alignment.Center,
    ) {
        // Faint target-face inset — five concentric paper-coloured rings on
        // top of the pond background. Mirrors iOS TargetFaceInset.
        Canvas(Modifier.size(72.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.width / 2f - 2f
            listOf(1.0f, 0.78f, 0.57f, 0.36f, 0.15f).forEach { ratio ->
                drawCircle(
                    color = AppPaper.copy(alpha = 0.32f),
                    radius = maxR * ratio,
                    center = center,
                    style = Stroke(width = 0.6f),
                )
            }
        }
        Text(
            avatarInitials(profile.displayName),
            style = frauncesDisplay(28.sp, italic = true),
            color = AppPaper,
        )
    }
}

// ── Stats row ────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(profile: SocialProfile, friendCount: Int, mutualCount: Int) {
    Column {
        HorizontalDivider(color = AppLine, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
        ) {
            StatCell("Friends", "$friendCount", accent = true, modifier = Modifier.weight(1f))
            VDivider()
            StatCell("Mutuals", "$mutualCount", modifier = Modifier.weight(1f))
            VDivider()
            StatCell("Sessions", paddedCount(profile.sessionCount), modifier = Modifier.weight(1f))
            VDivider()
            StatCell("Arrows", thousands(profile.arrowCount), modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
        Text(
            value,
            style = frauncesDisplay(24.sp, italic = true),
            color = if (accent) AppPondDk else AppInk,
        )
    }
}

@Composable
private fun VDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(AppLine2),
    )
}

private fun paddedCount(n: Int): String = if (n < 100) "%03d".format(n) else "$n"

private fun thousands(n: Int): String {
    if (n < 1000) return "$n"
    val sb = StringBuilder()
    val s = n.toString()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return sb.toString()
}

private fun firstName(name: String): String = name.split(" ").firstOrNull() ?: name

// ── Actions row (Strava-style follow + message) ─────────────────────────────

private enum class FollowButtonState { NotFriends, Friends, Requested, Respond }

private fun followState(f: Friendship?): FollowButtonState = when {
    f == null -> FollowButtonState.NotFriends
    f.status == FriendshipStatus.accepted -> FollowButtonState.Friends
    f.status == FriendshipStatus.pending &&
        f.direction == FriendshipDirection.outgoing -> FollowButtonState.Requested
    f.status == FriendshipStatus.pending &&
        f.direction == FriendshipDirection.incoming -> FollowButtonState.Respond
    else -> FollowButtonState.Requested
}

@Composable
private fun ActionsRow(
    friendship: Friendship?,
    busy: Boolean,
    error: String?,
    onFollowTap: () -> Unit,
) {
    val s = followState(friendship)
    val label = when (s) {
        FollowButtonState.NotFriends -> "FOLLOW"
        FollowButtonState.Friends -> "FOLLOWING"
        FollowButtonState.Requested -> "REQUESTED"
        FollowButtonState.Respond -> "RESPOND"
    }
    val glyph = when (s) {
        FollowButtonState.NotFriends -> "+"
        FollowButtonState.Friends -> "✓"
        FollowButtonState.Requested -> "·"
        FollowButtonState.Respond -> "›"
    }
    val fillColor = if (s == FollowButtonState.NotFriends) AppPondDk else Color.Transparent
    val borderColor = if (s == FollowButtonState.Requested) AppStone else AppPondDk
    val textColor = when (s) {
        FollowButtonState.NotFriends -> AppPaper
        FollowButtonState.Requested -> AppStone
        else -> AppPondDk
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderColor)
                    .background(fillColor)
                    .clickable(enabled = !busy, onClick = onFollowTap)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (busy) {
                    Text(
                        "…",
                        style = interUI(11.sp, FontWeight.SemiBold)
                            .copy(letterSpacing = 0.22.em, color = textColor),
                    )
                } else {
                    Text(
                        label,
                        style = interUI(11.sp, FontWeight.SemiBold)
                            .copy(letterSpacing = 0.22.em, color = textColor),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        glyph,
                        style = frauncesDisplay(14.sp, italic = true).copy(color = textColor),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, AppPondDk)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "MESSAGE",
                    style = interUI(11.sp, FontWeight.SemiBold)
                        .copy(letterSpacing = 0.22.em, color = AppPondDk.copy(alpha = 0.55f)),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "›",
                    style = frauncesDisplay(14.sp, italic = true)
                        .copy(color = AppPondDk.copy(alpha = 0.55f)),
                )
            }
        }
        error?.let {
            Text(it, style = jetbrainsMono(10.sp), color = AppMaple)
        }
    }
    HorizontalDivider(color = AppLine, thickness = 1.dp)
}

// ── Mutuals strip ────────────────────────────────────────────────────────────

@Composable
private fun MutualsStrip(mutuals: List<MutualArcher>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Overlapping initials avatars, up to 4.
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            mutuals.take(4).forEach { m ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppPond)
                        .border(1.5.dp, AppPaper),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        avatarInitials(m.displayName),
                        style = frauncesDisplay(13.sp, italic = true),
                        color = AppPaper,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Archers you both follow",
                style = frauncesDisplay(13.sp, italic = true),
                color = AppInk,
            )
            Text("${mutuals.size} mutual", style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
    }
    HorizontalDivider(color = AppLine2, thickness = 1.dp)
}

// ── Plot strip ───────────────────────────────────────────────────────────────

@Composable
private fun PlotStrip(sessions: List<SessionSummary>) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "RECENT SESSIONS · PLOT STRIP",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "ALL MEDIA · ${sessions.size} ›",
                style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            sessions.take(8).forEach { s ->
                PlotCard(session = s)
            }
        }
    }
    HorizontalDivider(color = AppLine2, thickness = 1.dp)
}

private val plotDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@Composable
private fun PlotCard(session: SessionSummary) {
    val faceType = when (session.targetFaceType) {
        "six_ring", "sixRing", "vegas" -> BPTargetFaceType.SixRing
        else -> BPTargetFaceType.TenRing
    }
    val sixRingStyle = when (session.distance) {
        "50m", "70m" -> BPSixRingStyle.Outdoor80
        else -> BPSixRingStyle.Vegas
    }
    Column(
        modifier = Modifier
            .width(124.dp)
            .border(1.dp, AppLine),
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .background(AppCream),
            contentAlignment = Alignment.Center,
        ) {
            BPTargetFace(
                size = 124.dp,
                face = faceType,
                sixRingStyle = sixRingStyle,
            )
            // Arrow plots overlay — uses real coords from `session.plots`,
            // scaled with a 6mm-reference dot size (parity B1). Falls back
            // to no overlay when the payload doesn't carry plots.
            if (session.plots.isNotEmpty()) {
                Canvas(Modifier.size(124.dp)) {
                    val radius = min(this.size.width, this.size.height) / 2f
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val shaftMm = session.arrowDiameterMm ?: 6.0
                    val referenceRadius = radius * 0.04f
                    val scale = (shaftMm / 6.0).toFloat()
                    val dot = max(1f, referenceRadius * scale)
                    session.plots.forEach { p ->
                        val cx = center.x + p.x.toFloat() * radius
                        val cy = center.y + p.y.toFloat() * radius
                        drawCircle(
                            color = AppInk,
                            radius = dot,
                            center = Offset(cx, cy),
                        )
                        drawCircle(
                            color = AppCream,
                            radius = dot,
                            center = Offset(cx, cy),
                            style = Stroke(width = 0.6f),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPaper2)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${session.arrowCount}",
                    style = frauncesDisplay(13.sp, italic = true),
                    color = AppInk,
                )
                Spacer(Modifier.width(4.dp))
                Text("ARROWS", style = jetbrainsMono(8.5.sp), color = AppInk3)
            }
            // Caption: distance · date — drops nullable score/xCount per
            // iOS oracle (those legitimately come back null on profile rows).
            val caption = buildString {
                session.distance?.takeIf { it.isNotBlank() }?.let { append(it) }
                session.endedAt?.let {
                    if (isNotEmpty()) append(" · ")
                    append(plotDateFormatter.format(it.atZone(ZoneId.systemDefault())))
                }
            }
            if (caption.isNotEmpty()) {
                Text(caption, style = jetbrainsMono(8.5.sp), color = AppInk3, maxLines = 1)
            }
        }
    }
}

// ── This week ───────────────────────────────────────────────────────────────

@Composable
private fun ThisWeekBlock(stat: ThisWeekStat) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("This week", style = frauncesDisplay(22.sp, italic = true), color = AppInk)
            Spacer(Modifier.weight(1f))
            Text(
                "LAST 7 DAYS",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
        }
        // Stats row — arrows · time · avg
        Row(modifier = Modifier.fillMaxWidth()) {
            ThisWeekStatCell(
                "Arrows",
                "${stat.arrowCount}",
                unit = null,
                accent = true,
                modifier = Modifier.weight(1f),
            )
            VDivider()
            ThisWeekStatCell(
                "Time",
                stat.timeSeconds?.let { (it / 60).toString() } ?: "—",
                unit = "min",
                modifier = Modifier.weight(1f),
            )
            VDivider()
            ThisWeekStatCell(
                "Avg",
                stat.avgArrowScore?.let { "%.1f".format(it) } ?: "—",
                unit = "per arrow",
                modifier = Modifier.weight(1f),
            )
        }
        // 7-day mini chart — Canvas-drawn line. Falls back to an empty-state
        // tile when the dailyAvg array is empty.
        SevenDayChart(daily = stat.dailyAvg, peak = stat.peak)
    }
    HorizontalDivider(color = AppLine, thickness = 1.dp)
}

@Composable
private fun ThisWeekStatCell(
    label: String,
    value: String,
    unit: String?,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = frauncesDisplay(22.sp, italic = true),
                color = if (accent) AppPondDk else AppInk,
            )
            unit?.let {
                Spacer(Modifier.width(4.dp))
                Text(it, style = jetbrainsMono(9.sp), color = AppInk3)
            }
        }
    }
}

/**
 * Inline 7-day line chart. Canvas-drawn so we don't pull a charting library
 * just for the friend-profile mini-chart. Renders the dailyAvg series with
 * a dashed peak marker; nulls in the series become breaks in the polyline so
 * a zero-arrow day reads as "no data" not a dip to 0.
 */
@Composable
private fun SevenDayChart(daily: List<Double?>, peak: Double?) {
    if (daily.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(AppPaper2)
                .border(1.dp, AppLine2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No arrows logged this week",
                style = frauncesDisplay(12.sp, italic = true),
                color = AppInk3,
            )
        }
        return
    }
    val effectivePeak = peak ?: daily.filterNotNull().maxOrNull() ?: 10.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
    ) {
        val w = size.width
        val h = size.height
        val paddingX = 8f
        val paddingY = 14f
        val plotW = w - paddingX * 2
        val plotH = h - paddingY * 2

        // Grid baseline
        drawLine(
            color = AppLine2,
            start = Offset(paddingX, h - paddingY),
            end = Offset(w - paddingX, h - paddingY),
            strokeWidth = 0.8f,
        )
        // Peak marker
        if (effectivePeak > 0) {
            val yPeak = paddingY
            drawLine(
                color = AppLine2,
                start = Offset(paddingX, yPeak),
                end = Offset(w - paddingX, yPeak),
                strokeWidth = 0.6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)),
            )
        }
        val n = daily.size
        val stepX = plotW / max(1, n - 1)

        // Polyline — break on nulls so missing days don't dip to zero.
        var prev: Offset? = null
        daily.forEachIndexed { i, value ->
            if (value == null) {
                prev = null
                return@forEachIndexed
            }
            val frac = (value / effectivePeak).coerceIn(0.0, 1.0).toFloat()
            val cx = paddingX + stepX * i
            val cy = paddingY + plotH * (1f - frac)
            val current = Offset(cx, cy)
            prev?.let {
                drawLine(
                    color = AppPondDk,
                    start = it,
                    end = current,
                    strokeWidth = 1.6f,
                )
            }
            drawCircle(color = AppPondDk, radius = 2.5f, center = current)
            prev = current
        }
    }
}

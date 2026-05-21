package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppDeep
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFace
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFaceType
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivitySession
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import java.time.Instant
import java.time.temporal.ChronoUnit

// ── ActivityCard ─────────────────────────────────────────────────────────────
//
// The shared-session feed card — explorations/Social Activity Card - 50-50.
// A hairline-bordered card: a header (avatar · location · name · verb · stamp ·
// when) over a 50/50 body — the WA target on the left, the range eyebrow + hero
// score + per-end scorecard on the right. Mirrors iOS `ActivityCard`.

/**
 * The 50/50 activity card for a shared range session. Caller passes the feed
 * [item]; the card derives everything from `item.session`.
 */
@Composable
fun ActivityCard(
    item: ActivityItem,
    onClick: () -> Unit,
    onLocationTap: (SessionLocation) -> Unit,
    onToggleLike: suspend (String, Boolean) -> com.andrewnguyen.bowpress.core.model.ToggleLikeResponse,
    onOpenComments: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = item.session ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppPaper)
            .border(1.dp, AppLine)
            .clickable(onClick = onClick)
            .testTag(TestTags.FeedRowPreview),
    ) {
        ActivityCardHeader(item, session, onLocationTap)
        HorizontalDivider(color = AppLine2, thickness = 1.dp)
        ActivityCardBody(session)
        // Social Feed V2 §5 — the like + comment action bar, footer of the card.
        HorizontalDivider(color = AppLine2, thickness = 1.dp)
        LikeCommentBar(
            item = item,
            onToggleLike = onToggleLike,
            onOpenComments = onOpenComments,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ActivityCardHeader(
    item: ActivityItem,
    session: ActivitySession,
    onLocationTap: (SessionLocation) -> Unit,
) {
    // The PR/milestone stamp shows only on a highlighted (achievement) row;
    // the relative time always pins to the right.
    val stamp = if (item.highlighted) item.stamp?.takeIf { it.isNotBlank() } else null
    Row(
        modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, AppPondDk)
                .background(AppPaper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitials(item.actorDisplayName),
                style = frauncesDisplay(12.5.sp, italic = true, weight = FontWeight.Medium),
                color = AppPondDk,
            )
        }
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            session.location?.let { loc ->
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
            Text(
                text = item.actorDisplayName,
                style = frauncesDisplay(16.sp, italic = true, weight = FontWeight.Medium),
                color = AppInk,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = item.title,
                style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Normal),
                color = AppInk2,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(6.dp))

        if (stamp != null) {
            Box(
                modifier = Modifier
                    .border(1.dp, AppMaple)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stamp.uppercase(),
                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppMaple,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = relativeStamp(item.createdAt),
            style = jetbrainsMono(9.5.sp),
            color = AppInk3,
        )
    }
}

@Composable
private fun ActivityCardBody(session: ActivitySession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .height(IntrinsicSize.Min),
    ) {
        // Left — the WA target face.
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 18.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            BPTargetFace(size = 160.dp, face = faceTypeFor(session.face))
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
                text = rangeEyebrow(session.distance),
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "${session.score}",
                    style = frauncesDisplay(48.sp, italic = false, weight = FontWeight.Medium),
                    color = AppDeep,
                )
                val maxScore = session.arrowCount * 10
                if (maxScore > 0) {
                    Text(
                        text = "/",
                        style = frauncesDisplay(22.sp, italic = true, weight = FontWeight.Normal),
                        color = AppInk3,
                    )
                    Text(
                        text = "$maxScore",
                        style = frauncesDisplay(22.sp, italic = false, weight = FontWeight.Medium),
                        color = AppInk3,
                    )
                }
            }
            val ends = session.endRings.orEmpty()
            if (ends.isNotEmpty()) {
                EndsTable(ends, modifier = Modifier.padding(top = 14.dp))
            }
        }
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

/** Per-end ledger — end number + arrow ring values, hairline-separated. */
@Composable
private fun EndsTable(ends: List<List<Int>>, modifier: Modifier = Modifier) {
    val rows = ends.take(10)
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = AppLine, thickness = 1.dp)
        rows.forEachIndexed { idx, rings ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${idx + 1}",
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                rings.forEach { ring ->
                    Text(
                        text = ringLabel(ring),
                        style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Medium),
                        color = ringColor(ring),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (idx < rows.lastIndex) {
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
            }
        }
    }
}

private fun ringLabel(ring: Int): String = when {
    ring >= 11 -> "X"
    ring <= 0 -> "M"
    else -> ring.toString()
}

private fun ringColor(ring: Int) = when {
    ring >= 11 -> AppMaple
    ring <= 0 -> AppInk3
    else -> AppInk
}

/** Compact relative time — "2h", "3d", "2w". Mirrors iOS `relativeStamp`. */
/**
 * Picks a target-face shape from a shared session's free-text `face` label —
 * a 6-ring / Vegas / 3-spot face reads as [BPTargetFaceType.SixRing],
 * everything else [BPTargetFaceType.TenRing]. Mirrors iOS
 * `ActivityPreviewBand.faceType(for:)`.
 */
internal fun faceTypeFor(face: String?): BPTargetFaceType {
    val f = (face ?: "").lowercase()
    return if (f.contains("6") || f.contains("spot") || f.contains("vegas")) {
        BPTargetFaceType.SixRing
    } else {
        BPTargetFaceType.TenRing
    }
}

private fun relativeStamp(from: Instant): String {
    val mins = ChronoUnit.MINUTES.between(from, Instant.now()).coerceAtLeast(0)
    return when {
        mins < 60 -> "${mins}m"
        mins < 60 * 24 -> "${mins / 60}h"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)}d"
        else -> "${mins / (60 * 24 * 7)}w"
    }
}

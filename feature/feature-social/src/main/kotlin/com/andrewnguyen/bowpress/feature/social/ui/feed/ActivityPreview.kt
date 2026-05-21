package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.data.social.TargetPhotoCatalog
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFace
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityItem

// ── ActivityPreview ──────────────────────────────────────────────────────────
//
// The typed preview band a shared-session feed row renders (§18). The feed
// picks one per row: a target-paper photo if the actor attached one, else a
// target face for a range session, else a course block for a 3D course.
// Mirrors iOS SocialComponents.ActivityPreview + ActivityPreviewBand.

/** Fixed band height so the preview kinds line up in the feed. */
private val BAND_HEIGHT = 116.dp

/**
 * The visual preview a shared-session feed row renders. [None] renders
 * nothing.
 *
 * Android has no photo-capture feature yet (iOS issue #23), so [Photo] is
 * currently only produced for the DEBUG mock session tagged in
 * `TargetPhotoCatalog` — enough to demonstrate the variant in the emulator,
 * matching the iOS DEBUG build. When photo capture is ported, the catalog
 * becomes a real lookup and nothing else here changes.
 */
sealed interface ActivityPreview {
    val isEmpty: Boolean get() = this is None

    data object None : ActivityPreview

    /** A target-paper photo the actor attached to the session. */
    data object Photo : ActivityPreview

    /** A range session — a World Archery target face + the score. */
    data class Target(val face: String?, val score: Int) : ActivityPreview

    /** A 3D course — a walked-trail schematic + the score. */
    data class Course(val score: Int, val stations: Int) : ActivityPreview
}

/**
 * Picks the §18 preview band for a feed row: a target-paper photo if the
 * session has one, else a course block for a 3D course, else a target face
 * for a range session. A non-session row gets [None].
 */
fun activityPreview(item: ActivityItem): ActivityPreview {
    val session = item.session ?: return ActivityPreview.None
    // Photo precedence — a photographed session shows the photo over the
    // discipline preview, matching iOS.
    if (TargetPhotoCatalog.hasPhoto(session.sessionId)) return ActivityPreview.Photo
    return if (session.isCourse) {
        ActivityPreview.Course(score = session.score, stations = session.arrowCount)
    } else {
        ActivityPreview.Target(face = session.face, score = session.score)
    }
}

/** The preview band rendered under a shared-session feed row's headline. */
@Composable
fun ActivityPreviewBand(
    preview: ActivityPreview,
    modifier: Modifier = Modifier,
) {
    when (preview) {
        is ActivityPreview.None -> Unit
        is ActivityPreview.Photo -> PhotoBand(modifier = modifier)
        is ActivityPreview.Target -> TargetBand(
            face = preview.face,
            score = preview.score,
            modifier = modifier,
        )
        is ActivityPreview.Course -> CourseBand(
            score = preview.score,
            stations = preview.stations,
            modifier = modifier,
        )
    }
}

/**
 * A target-paper photo the actor attached — a full-bleed banner. Android has
 * no real photo capture yet (iOS issue #23), so this renders a synthetic
 * target-paper image (a WA face on cream paper), the same stand-in the iOS
 * DEBUG build seeds. When real photos land, swap the synthetic face for the
 * decoded image and the rest of the feed is unchanged.
 */
@Composable
private fun PhotoBand(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BAND_HEIGHT)
            .background(AppCream)
            .border(1.dp, AppLine)
            .clipToBounds()
            .testTag(TestTags.FeedRowPreview),
        contentAlignment = Alignment.Center,
    ) {
        // Oversized so it reads as a full-bleed close-up photo of the paper.
        BPTargetFace(size = 168.dp)
    }
}

/** Range session — a WA target face thumbnail beside the score. */
@Composable
private fun TargetBand(face: String?, score: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BAND_HEIGHT)
            .background(AppPaper2)
            .border(1.dp, AppLine)
            .padding(horizontal = 14.dp)
            .testTag(TestTags.FeedRowPreview),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BPTargetFace(size = 78.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "RANGE",
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$score",
                style = frauncesDisplay(26.sp),
                color = AppInk,
            )
            if (!face.isNullOrEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = face,
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
            }
        }
    }
}

/** 3D course — a walked-trail schematic beside the score. */
@Composable
private fun CourseBand(score: Int, stations: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BAND_HEIGHT)
            .background(AppCream)
            .border(1.dp, AppPine.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp)
            .testTag(TestTags.FeedRowPreview),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CourseBlockPreview(
            modifier = Modifier
                .width(96.dp)
                .height(BAND_HEIGHT - 36.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "3D COURSE",
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPine,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$score",
                style = frauncesDisplay(26.sp),
                color = AppInk,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (stations == 1) "1 station" else "$stations stations",
                style = jetbrainsMono(9.sp),
                color = AppInk3,
            )
        }
    }
}

/**
 * The walked-trail schematic for a 3D-course preview — a dashed meander with
 * station pins, evoking a 3D course map without needing its data. Mirrors
 * iOS CourseBlockPreview.
 */
@Composable
private fun CourseBlockPreview(modifier: Modifier = Modifier) {
    val norm = listOf(
        0.10f to 0.74f, 0.27f to 0.32f, 0.43f to 0.64f,
        0.59f to 0.24f, 0.75f to 0.58f, 0.90f to 0.34f,
    )
    Canvas(modifier = modifier) {
        val pts = norm.map { Offset(it.first * size.width, it.second * size.height) }
        val trail = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = trail,
            color = AppPine.copy(alpha = 0.6f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
                ),
            ),
        )
        pts.forEachIndexed { i, p ->
            val last = i == pts.lastIndex
            drawCircle(
                color = if (last) AppMaple else AppPine,
                radius = (if (last) 4f else 3f).dp.toPx(),
                center = p,
            )
        }
    }
}

package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityPhoto
import com.andrewnguyen.bowpress.core.model.PhotoStatus
import com.andrewnguyen.bowpress.feature.social.ui.session.RemoteSessionPhoto
import com.andrewnguyen.bowpress.feature.social.ui.session.SessionPhotoLoader

// ── Photo strip ──────────────────────────────────────────────────────────────
//
// Section 4 of the Social Activity Card — "Photos". When a shared session has
// attached photos, a count-flexed strip sits between the score body and the
// kudos / reactions row, hairline-separated by a top border. Mirrors the
// design handoff's `.photos` / `.ph` / `.more-cnt` markup.
//
// The strip reuses the Part-1 photo pipeline wholesale: each cell is a
// `RemoteSessionPhoto`, which fetches the Bearer-gated display JPEG through
// the `SessionPhotoLoader` and decodes it. The fetch itself is memoised in the
// process-scoped `SessionPhotoCache` inside `SocialRepository` — no API call
// or cache is added here.

/** The 1dp hairline gutter between photo cells — the design's `gap:1px`. */
private val GUTTER = 1.dp

/**
 * The count-flexed layout of a photo strip. Decided purely by [readyCount] —
 * the number of `ready` photos — so it is unit-testable without Compose.
 *
 * Mirrors the design's `.photos.n-1 … .n-4` rules:
 *  - [Single]  — one photo, full card width at 4:3.
 *  - [Pair]    — two photos, a 50/50 split, each 1:1.
 *  - [Trio]    — a big left cell spanning 2 rows + two stacked 1:1 cells.
 *  - [Grid]    — a 2×2 grid of 1:1 cells; when more than 4 photos exist the
 *                4th visible cell carries a "+N more" overlay, [overflow] = N.
 */
internal sealed interface PhotoStripLayout {
    /** How many cells the strip actually renders. */
    val visibleCells: Int

    data object Single : PhotoStripLayout {
        override val visibleCells: Int get() = 1
    }

    data object Pair : PhotoStripLayout {
        override val visibleCells: Int get() = 2
    }

    data object Trio : PhotoStripLayout {
        override val visibleCells: Int get() = 3
    }

    /** [overflow] = total ready photos − 4, the "+N" on the last cell. 0 = exactly 4. */
    data class Grid(val overflow: Int) : PhotoStripLayout {
        override val visibleCells: Int get() = 4
    }
}

/**
 * Maps a count of `ready` photos to its strip layout. Returns null for a count
 * of 0 — no photos means no strip and no extra hairline.
 *
 * 4 or more photos all collapse to the 2×2 [PhotoStripLayout.Grid]. Exactly 4
 * photos fill the grid with no overlay ([PhotoStripLayout.Grid.overflow] = 0).
 * *Past* 4, the 4th cell carries a "+N more" overlay where N = [readyCount] − 4
 * — all four cells show a photo, so the count is only the photos *not* shown.
 * Matches the design's State C (7 photos → "+3 more").
 */
internal fun photoStripLayout(readyCount: Int): PhotoStripLayout? = when {
    readyCount <= 0 -> null
    readyCount == 1 -> PhotoStripLayout.Single
    readyCount == 2 -> PhotoStripLayout.Pair
    readyCount == 3 -> PhotoStripLayout.Trio
    else -> PhotoStripLayout.Grid(overflow = readyCount - 4)
}

/**
 * The photo strip for a session card. Renders nothing when [readyPhotos] is
 * empty — the caller then draws no hairline either.
 *
 * [readyPhotos] must be the *already* `ready`-filtered, position-sorted list
 * (the caller computes it once in a `remember` and shares the same list with
 * [PhotoStripViewer], so cell-tap indices line up with the viewer's pages).
 * pending / failed photos are excluded upstream — the design only shows
 * finished uploads; a `ready` photo whose bytes are still loading shows a
 * paper-2 letterbox, handled inside [RemoteSessionPhoto].
 *
 * Tapping any cell calls [onOpenViewer] with the index of the tapped photo
 * within [readyPhotos].
 */
@Composable
internal fun PhotoStrip(
    sharedSessionId: String,
    readyPhotos: List<ActivityPhoto>,
    loader: SessionPhotoLoader,
    onOpenViewer: (startIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = photoStripLayout(readyPhotos.size) ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppLine) // shows through the 1dp gutters as a hairline
            .testTag(TestTags.FeedRowPhotoStrip),
    ) {
        when (layout) {
            is PhotoStripLayout.Single -> Cell(
                sharedSessionId, readyPhotos[0], loader,
                onClick = { onOpenViewer(0) },
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )

            is PhotoStripLayout.Pair -> Row(
                horizontalArrangement = Arrangement.spacedBy(GUTTER),
            ) {
                readyPhotos.take(2).forEachIndexed { i, photo ->
                    Cell(
                        sharedSessionId, photo, loader,
                        onClick = { onOpenViewer(i) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                    )
                }
            }

            is PhotoStripLayout.Trio -> Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(GUTTER),
            ) {
                // Big left cell — the design's `grid-row:span 2`. The Row is
                // measured at IntrinsicSize.Min, so the right column's two
                // stacked squares + gutter set the row height; the big cell
                // then `fillMaxHeight()` stretches to exactly that height —
                // truly flush, no rounding sliver.
                Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                    Cell(
                        sharedSessionId, readyPhotos[0], loader,
                        onClick = { onOpenViewer(0) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GUTTER),
                ) {
                    readyPhotos.subList(1, 3).forEachIndexed { i, photo ->
                        Cell(
                            sharedSessionId, photo, loader,
                            onClick = { onOpenViewer(i + 1) },
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                    }
                }
            }

            is PhotoStripLayout.Grid -> Column(
                verticalArrangement = Arrangement.spacedBy(GUTTER),
            ) {
                // 2×2 — rows of two 1:1 cells. The 4th visible cell carries the
                // "+N more" overlay when there are more than 4 photos.
                for (rowIndex in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(GUTTER)) {
                        for (colIndex in 0..1) {
                            val cellIndex = rowIndex * 2 + colIndex
                            val isLast = cellIndex == 3
                            val showOverflow = isLast && layout.overflow > 0
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                            ) {
                                Cell(
                                    sharedSessionId, readyPhotos[cellIndex], loader,
                                    onClick = { onOpenViewer(cellIndex) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (showOverflow) {
                                    MoreOverlay(count = layout.overflow)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One photo cell — a cover-fit [RemoteSessionPhoto] on a paper-2 letterbox
 * ground (the design's loading state), tappable into the viewer.
 *
 * `border = false`: the strip's 1dp grid seams come from the strip's own
 * `AppLine` background showing through the `GUTTER` gaps. A per-cell border
 * would double every internal seam and frame the strip on all four sides —
 * the design's `.photos` has only a `border-top`.
 */
@Composable
private fun Cell(
    sharedSessionId: String,
    photo: ActivityPhoto,
    loader: SessionPhotoLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RemoteSessionPhoto(
        sharedSessionId = sharedSessionId,
        photo = photo,
        loader = loader,
        background = AppPaper2,
        border = false,
        modifier = modifier.clickable(onClick = onClick),
    )
}

/**
 * The "+N more" overlay on the 4th cell of an overflowing grid — a 62%-opacity
 * ink scrim with a big italic Fraunces number over a small uppercase "MORE".
 * No maple: this is a neutral count, not an alert.
 */
@Composable
private fun MoreOverlay(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppInk.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+$count",
                style = frauncesDisplay(28.sp, italic = true, weight = FontWeight.Medium),
                color = AppPaper,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "MORE",
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPaper.copy(alpha = 0.7f),
            )
        }
    }
}

// ── Full-screen viewer ───────────────────────────────────────────────────────
//
// Tapping any strip cell opens this pager. The Part-1 photo gallery has no
// full-screen viewer (`FeedPhotoGallery` / `DetailPhotoGallery` are inline
// strips only), so the strip ships a simple swipe pager — still reusing the
// same `RemoteSessionPhoto` fetch + cache for each page.

/**
 * A full-screen, swipeable viewer over a session's `ready` photos, opened from
 * the [PhotoStrip]. Renders inside a full-screen [Dialog]; tapping the scrim or
 * the close affordance dismisses it via [onDismiss].
 *
 * [photos] is the already-filtered `ready` list in display order; [startIndex]
 * is the page to open on. Each page is a contain-fit [RemoteSessionPhoto] —
 * the same authenticated fetch the strip uses, so an already-loaded photo is a
 * cache hit.
 */
@Composable
internal fun PhotoStripViewer(
    sharedSessionId: String,
    photos: List<ActivityPhoto>,
    startIndex: Int,
    loader: SessionPhotoLoader,
    onDismiss: () -> Unit,
) {
    if (photos.isEmpty()) return
    val start = startIndex.coerceIn(0, photos.lastIndex)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
        ),
    ) {
        val pagerState = rememberPagerState(initialPage = start) { photos.size }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppInk)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                // The page fills the screen; `ContentScale.Fit` then sizes the
                // photo to its natural aspect within that — no nested
                // square-letterbox shrinking the image.
                RemoteSessionPhoto(
                    sharedSessionId = sharedSessionId,
                    photo = photos[page],
                    loader = loader,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    background = AppInk,
                    border = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Page counter — "2 / 7" in mono, pinned bottom.
            if (photos.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                    style = jetbrainsMono(11.sp),
                    color = AppPaper,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                )
            }
            // Close affordance — top-end. D2: the whole chip is tappable, not
            // just the glyph. We wrap the Text in a Box that owns the
            // `clickable` so the hit area covers the padded chip rather than
            // just the (~40dp tall) text run. Mirrors iOS commit 4e96ab6.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onDismiss)
                    .padding(20.dp),
            ) {
                Text(
                    text = "CLOSE",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPaper,
                )
            }
        }
    }
}

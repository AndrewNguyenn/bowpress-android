package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

// ── Balanced feed card ─────────────────────────────────────────────────────
//
// Pieces of the balanced feed-card stage (design: explorations/finish/
// BowPress Feed.html). The stage is a fixed 300dp row sitting between the
// equipment metadata strip and the description. Two shapes:
//
//   no media → static split:     [ targets | rule | scorecard ]
//   has media → dual carousel:   [ scorecard ↔ targets | rule | photos ]
//
// Each half-pane swipes independently with `HorizontalPager`'s snap; the dot
// row tracks the active slide. Mirrors iOS `BalancedFeedCard.swift`.

/**
 * Sizing constants for the balanced feed card. Scoped to an object so the
 * constants don't leak into the package namespace.
 */
internal object BalancedFeedLayout {
    /**
     * Fixed stage height for the balanced feed-card body. The design ships
     * a 300dp row; we keep both static-split and dual-carousel shapes on
     * the same constant so every card in the feed reads as one consistent
     * block.
     */
    val stageHeight: Dp = 300.dp
}

/**
 * One slide on a balanced feed-card pager. The [id] is the slide's stable
 * identity — drives Compose's `pageKey` so slides keep their state when
 * peers reshape around them (photo strip layout, etc.). The caller picks
 * the id — `"scorecard"`, `"targets"`, `"photos"` are the three the feed
 * uses today.
 */
data class BalancedFeedPagerSlide(
    val id: String,
    /** Optional kind label rendered in the top-right chip when active. */
    val kind: String?,
    val content: @Composable () -> Unit,
)

/**
 * A horizontally swipeable pane with snap behaviour + dot indicator.
 *
 * Used twice per balanced feed card — once for the [scorecard ↔ targets]
 * pair on the left, once for the [photos] slide on the right. Slides carry
 * stable string ids so Compose's recycle bucket keys off slide identity,
 * not its ordinal in the list. When a slide is removed (photo readiness
 * flips), the pager re-pins onto a surviving slide via [LaunchedEffect].
 *
 * [dotsLight] = true is for a pane sitting on paper (left pane, photo pane);
 * false is for a pane sitting on dark media.
 */
@Composable
internal fun BalancedFeedPager(
    slides: List<BalancedFeedPagerSlide>,
    modifier: Modifier = Modifier,
    dotsLight: Boolean = true,
) {
    if (slides.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = 0) { slides.size }
    // Re-pin onto a surviving slide id when the slide set reshapes. Compose
    // doesn't move pages around when the page count changes — it just
    // truncates / extends — so if the active page index now points off the
    // end of the surviving list, snap to the last surviving slide. Mirrors
    // iOS `reconcileActiveId()`.
    LaunchedEffect(slides.map { it.id }) {
        val maxIndex = slides.lastIndex
        if (pagerState.currentPage > maxIndex) {
            pagerState.scrollToPage(maxIndex.coerceAtLeast(0))
        }
    }
    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            pageSize = androidx.compose.foundation.pager.PageSize.Fill,
            // Stable per-slide key so the page-recycler buckets each
            // kind separately — `FeedVideoTile` (when added) won't lose
            // its prebuffer on a swipe between video and photos.
            key = { index -> slides[index].id },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            slides[page].content()
        }
        ChipOverlay(slides = slides, pagerState = pagerState, dotsLight = dotsLight)
        DotsOverlay(slides = slides, pagerState = pagerState, dotsLight = dotsLight)
    }
}

@Composable
private fun ChipOverlay(
    slides: List<BalancedFeedPagerSlide>,
    pagerState: PagerState,
    dotsLight: Boolean,
) {
    val active = slides.getOrNull(pagerState.currentPage) ?: slides.firstOrNull()
    val kind = active?.kind?.takeIf { it.isNotEmpty() } ?: return
    val backgroundColor = if (dotsLight) AppPaper.copy(alpha = 0.9f) else AppInk.copy(alpha = 0.62f)
    val borderColor = if (dotsLight) AppLine else AppPaper.copy(alpha = 0.22f)
    val textColor = if (dotsLight) AppInk2 else AppPaper
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, RectangleShape)
                .border(1.dp, borderColor)
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                text = kind.uppercase(),
                style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.20.em),
                color = textColor,
            )
        }
    }
}

@Composable
private fun DotsOverlay(
    slides: List<BalancedFeedPagerSlide>,
    pagerState: PagerState,
    dotsLight: Boolean,
) {
    if (slides.size < 2) return
    val backgroundColor = if (dotsLight) AppPaper.copy(alpha = 0.85f) else AppInk.copy(alpha = 0.5f)
    val borderColor = if (dotsLight) AppLine else AppPaper.copy(alpha = 0.16f)
    val activeColor = if (dotsLight) AppPondDk else AppPaper
    val inactiveColor = if (dotsLight) AppInk3 else AppPaper.copy(alpha = 0.45f)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 7.dp)
                .background(backgroundColor, RectangleShape)
                .border(1.dp, borderColor)
                .padding(horizontal = 7.dp, vertical = 4.dp),
        ) {
            slides.forEachIndexed { index, _ ->
                val on = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .width(if (on) 13.dp else 5.dp)
                        .height(5.dp)
                        .clip(RectangleShape)
                        .background(if (on) activeColor else inactiveColor),
                )
                if (index < slides.lastIndex) Spacer(Modifier.size(4.dp))
            }
        }
    }
}


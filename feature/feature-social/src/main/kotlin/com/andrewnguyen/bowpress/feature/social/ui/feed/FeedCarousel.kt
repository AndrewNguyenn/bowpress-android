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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

// =============================================================================
// Feed-tab swipeable hero carousel.
//
// iOS parity (A3) — mirrors `SocialFeedCarousel.swift` + the four card
// bodies in `SocialFeedCarouselCards.swift`. A horizontally-paged row of up
// to four cards (this week / weekly snapshot / best session / tuning
// insight) renders between the FeedScreen top-nav and the activity list.
// Cards with nil data drop out; when every card is nil the carousel
// collapses to a single welcome hero (we just hide the carousel entirely
// on Android — FeedScreen already has a "new user" empty-state below).
//
// Data: the live FeedSummary endpoint (iOS GET /social/feed-summary)
// doesn't exist on Android yet — this composable accepts a [FeedSummaryUi]
// argument that callers stub with [FeedSummaryUi.preview] until a port
// lands. Wiring lives in FeedScreen.
// =============================================================================

/**
 * iOS parity (A3) — UI-side mirror of `FeedSummary`. Lives in the feature
 * module on purpose; once a real `/social/feed-summary` endpoint is wired
 * through `core-data` + `core-model`, this can be replaced with a domain
 * DTO. Fields are nullable so a card with no data drops out cleanly.
 */
data class FeedSummaryUi(
    val thisWeek: ThisWeek? = null,
    val snapshot: Snapshot? = null,
    val bestSession: BestSession? = null,
    val insight: Insight? = null,
    val openingCard: OpeningCard = OpeningCard.ThisWeek,
) {
    enum class OpeningCard { ThisWeek, Snapshot, BestSession, Insight }

    data class Day(val label: String, val arrows: Int, val isToday: Boolean = false)

    data class ThisWeek(
        val weekStreak: Int,
        val days: List<Day>,
        val totalArrows: Int,
        val sessionCount: Int,
    )

    data class Snapshot(
        val sessionsThis: Int,
        val sessionsLast: Int,
        val arrowsThis: Int,
        val arrowsLast: Int,
        val avgRingThis: Double?,
        val avgRingLast: Double?,
        val rangeLabel: String,
    )

    data class ArrowPoint(val x: Double, val y: Double)

    data class BestSession(
        val sessionName: String,
        val avgRing: Double,
        val xCount: Int,
        val totalArrows: Int,
        val bowName: String,
        val arrows: List<ArrowPoint>,
        val prDeltaAvgRing: Double?,
        /** Open-detail callback target — null when the session isn't shared. */
        val sharedSessionId: String?,
    )

    data class InsightMetric(val label: String, val value: String, val maple: Boolean = false)

    data class Insight(
        val headline: String,
        val metrics: List<InsightMetric>,
        val sampleSize: Int,
    )

    /** Ordered, non-null card payload list — empty cards drop out. */
    val cards: List<FeedSummaryCardUi>
        get() = buildList {
            thisWeek?.let { add(FeedSummaryCardUi.ThisWeekCard(it)) }
            snapshot?.let { add(FeedSummaryCardUi.SnapshotCard(it)) }
            bestSession?.let { add(FeedSummaryCardUi.BestSessionCard(it)) }
            insight?.let { add(FeedSummaryCardUi.InsightCard(it)) }
        }

    val openingIndex: Int
        get() = cards.indexOfFirst {
            when (it) {
                is FeedSummaryCardUi.ThisWeekCard -> openingCard == OpeningCard.ThisWeek
                is FeedSummaryCardUi.SnapshotCard -> openingCard == OpeningCard.Snapshot
                is FeedSummaryCardUi.BestSessionCard -> openingCard == OpeningCard.BestSession
                is FeedSummaryCardUi.InsightCard -> openingCard == OpeningCard.Insight
            }
        }.coerceAtLeast(0)

    companion object {
        /**
         * iOS parity (A3) — preview data for previews + the FeedViewModel
         * stub flow. Replace by a live API mapping once `/social/feed-summary`
         * is wired through `core-data`. The shape matches the iOS
         * `DevSocialMockData.weeklySnapshotPreview` block close enough that
         * Maestro screenshots of the carousel layout will line up across
         * platforms.
         */
        val preview: FeedSummaryUi = FeedSummaryUi(
            thisWeek = ThisWeek(
                weekStreak = 3,
                totalArrows = 184,
                sessionCount = 4,
                days = listOf(
                    Day("M", 36),
                    Day("T", 0),
                    Day("W", 48),
                    Day("T", 0),
                    Day("F", 42),
                    Day("S", 58, isToday = true),
                    Day("S", 0),
                ),
            ),
            snapshot = Snapshot(
                sessionsThis = 4,
                sessionsLast = 2,
                arrowsThis = 184,
                arrowsLast = 96,
                avgRingThis = 8.4,
                avgRingLast = 7.9,
                rangeLabel = "this week vs last",
            ),
            bestSession = BestSession(
                sessionName = "Tuesday Vegas",
                avgRing = 8.7,
                xCount = 4,
                totalArrows = 30,
                bowName = "Hoyt RX-7",
                arrows = listOf(
                    ArrowPoint(0.05, -0.08),
                    ArrowPoint(-0.10, 0.05),
                    ArrowPoint(0.12, 0.14),
                    ArrowPoint(-0.02, -0.18),
                    ArrowPoint(0.20, -0.04),
                    ArrowPoint(-0.06, 0.22),
                ),
                prDeltaAvgRing = 0.4,
                sharedSessionId = null,
            ),
            insight = Insight(
                headline = "Group centred right of POA — try a small left sight adjustment.",
                metrics = listOf(
                    InsightMetric("Drift", "3/16″ R", maple = true),
                    InsightMetric("Group ∅", "9mm"),
                    InsightMetric("Distance", "50m"),
                    InsightMetric("Confidence", "high"),
                ),
                sampleSize = 60,
            ),
            openingCard = OpeningCard.BestSession,
        )
    }
}

/** One card payload — non-null by construction. */
sealed class FeedSummaryCardUi {
    data class ThisWeekCard(val data: FeedSummaryUi.ThisWeek) : FeedSummaryCardUi()
    data class SnapshotCard(val data: FeedSummaryUi.Snapshot) : FeedSummaryCardUi()
    data class BestSessionCard(val data: FeedSummaryUi.BestSession) : FeedSummaryCardUi()
    data class InsightCard(val data: FeedSummaryUi.Insight) : FeedSummaryCardUi()
}

/**
 * iOS parity (A3) — feed-tab hero carousel. Mirrors `SocialFeedCarousel`
 * in iOS: a HorizontalPager of up to four cards plus a pill-dot
 * pagination row underneath. Empty payload collapses entirely.
 *
 * @param summary the per-card data; nil card fields drop out.
 * @param onOpenBest opens the friend-session detail behind the
 *   Best-session "Open ›" link. The link only renders when the card has
 *   a non-null `sharedSessionId` AND this callback is non-null — matches
 *   iOS commit 52eaec5 ("hide carousel link buttons that have no
 *   destination").
 */
@Composable
fun FeedCarousel(
    summary: FeedSummaryUi,
    onOpenBest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cards = summary.cards
    if (cards.isEmpty()) return

    Column(modifier = modifier.testTag("socialFeedCarousel")) {
        val pagerState = rememberPagerState(
            initialPage = summary.openingIndex.coerceAtMost(maxOf(0, cards.size - 1)),
        ) { cards.size }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight),
        ) { idx ->
            val card = cards[idx]
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                when (card) {
                    is FeedSummaryCardUi.ThisWeekCard -> ThisWeekCard(card.data)
                    is FeedSummaryCardUi.SnapshotCard -> WeeklySnapshotCard(card.data)
                    is FeedSummaryCardUi.BestSessionCard -> BestSessionCard(
                        data = card.data,
                        // iOS parity — only render the "Open ›" link when
                        // the card has a destination AND an outer handler.
                        onOpen = if (card.data.sharedSessionId != null) onOpenBest else null,
                    )
                    is FeedSummaryCardUi.InsightCard -> TuningInsightCard(card.data)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Pill-dot pagination row. Selected dot stretches to a small
        // capsule per the iOS design.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cards.indices.forEach { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .width(if (selected) 18.dp else 5.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (selected) AppPondDk else AppLine)
                        .testTag("socialCarousel.dot.$i"),
                )
            }
        }
    }
}

/** Card slot height — matches the iOS standard size-class default. */
private val CardHeight = 200.dp

// ── Shared chrome ────────────────────────────────────────────────────────────

/**
 * Header eyebrow + optional "Open ›" / "See more ›" / "Review ›" link.
 * iOS parity — the trailing link only renders when [onLink] is non-null,
 * so a card with no destination doesn't show a dead button.
 */
@Composable
private fun CardHeader(
    eyebrow: String,
    detail: String,
    linkLabel: String = "Open",
    maple: Boolean = false,
    onLink: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = eyebrow.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
            color = if (maple) AppMaple else AppPondDk,
        )
        Text(
            text = " · $detail".uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
            color = AppInk3,
        )
        Spacer(Modifier.width(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (onLink != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = onLink),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$linkLabel ".uppercase(),
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPondDk,
                    )
                    Text(
                        text = "›",
                        style = frauncesDisplay(11.sp, italic = true, weight = FontWeight.Medium),
                        color = AppPondDk,
                    )
                }
            }
        }
    }
}

/** Card frame — paper-2 ground, 1px line border, no radius. */
@Composable
private fun CardFrame(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper2)
            .border(1.dp, AppLine)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

// ── 01 · This week ───────────────────────────────────────────────────────────

@Composable
private fun ThisWeekCard(data: FeedSummaryUi.ThisWeek) {
    CardFrame {
        CardHeader(
            eyebrow = "This week",
            detail = "${data.sessionCount} sessions · ${data.totalArrows} arrows",
        )
        // 7-day bar row. Max bar maps to the tallest bar in the window so
        // a 12-arrow day in a quiet week still reads.
        val maxArrows = (data.days.maxOfOrNull { it.arrows } ?: 0).coerceAtLeast(1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            data.days.forEach { day ->
                val ratio = day.arrows.toFloat() / maxArrows
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height((ratio * 48f).dp.coerceAtLeast(if (day.arrows > 0) 4.dp else 1.dp))
                            .background(
                                when {
                                    day.arrows == 0 -> AppLine
                                    day.isToday -> AppPondDk
                                    else -> AppInk2
                                },
                            ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = day.label,
                        style = jetbrainsMono(8.5.sp),
                        color = if (day.isToday) AppPondDk else AppInk3,
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${data.weekStreak}",
                style = frauncesDisplay(22.sp, weight = FontWeight.Medium),
                color = AppPondDk,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (data.weekStreak == 1) "week streak" else "weeks streak",
                style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppInk3,
            )
        }
    }
}

// ── 02 · Weekly snapshot ─────────────────────────────────────────────────────

@Composable
private fun WeeklySnapshotCard(data: FeedSummaryUi.Snapshot) {
    CardFrame {
        CardHeader(eyebrow = "Weekly snapshot", detail = data.rangeLabel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SnapshotCell(
                label = "Sessions",
                value = "${data.sessionsThis}",
                delta = data.sessionsThis - data.sessionsLast,
                modifier = Modifier.width(96.dp),
            )
            SnapshotCell(
                label = "Arrows",
                value = "${data.arrowsThis}",
                delta = data.arrowsThis - data.arrowsLast,
                modifier = Modifier.width(96.dp),
            )
            SnapshotCell(
                label = "Avg ring",
                value = data.avgRingThis?.let { "%.1f".format(it) } ?: "—",
                delta = run {
                    val a = data.avgRingThis ?: return@run null
                    val b = data.avgRingLast ?: return@run null
                    (((a - b) * 10).toInt())
                },
                deltaIsAvgRing = true,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}

@Composable
private fun SnapshotCell(
    label: String,
    value: String,
    delta: Int?,
    modifier: Modifier = Modifier,
    deltaIsAvgRing: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
        Text(
            text = value,
            style = frauncesDisplay(24.sp, italic = true, weight = FontWeight.Medium),
            color = AppInk,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (delta != null) {
            val (txt, color) = when {
                delta > 0 -> {
                    val pretty = if (deltaIsAvgRing) "+%.1f".format(delta / 10.0) else "+$delta"
                    pretty to AppPondDk
                }
                delta < 0 -> {
                    val pretty = if (deltaIsAvgRing) "%.1f".format(delta / 10.0) else "$delta"
                    pretty to AppMaple
                }
                else -> "no change" to AppInk3
            }
            Text(
                text = txt,
                style = jetbrainsMono(10.sp),
                color = color,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ── 03 · Best session ────────────────────────────────────────────────────────

@Composable
private fun BestSessionCard(
    data: FeedSummaryUi.BestSession,
    onOpen: (() -> Unit)?,
) {
    CardFrame {
        CardHeader(
            eyebrow = "Best session",
            detail = data.bowName,
            linkLabel = "Open",
            onLink = onOpen,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.sessionName,
                    style = frauncesDisplay(15.sp, italic = true, weight = FontWeight.Medium),
                    color = AppInk,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%.1f".format(data.avgRing),
                    style = frauncesDisplay(36.sp, italic = true, weight = FontWeight.Medium),
                    color = AppPondDk,
                )
                Text(
                    text = "${data.xCount}X · ${data.totalArrows} arrows",
                    style = jetbrainsMono(10.sp),
                    color = AppInk3,
                )
                if (data.prDeltaAvgRing != null) {
                    Text(
                        text = "PR +%.1f vs prior".format(data.prDeltaAvgRing),
                        style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPondDk,
                    )
                }
            }
            // Mini target face thumbnail with arrow plot.
            MiniTargetFace(
                arrows = data.arrows,
                modifier = Modifier.size(96.dp),
            )
        }
    }
}

@Composable
private fun MiniTargetFace(
    arrows: List<FeedSummaryUi.ArrowPoint>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)
        // Concentric rings — simple monoline rendering (the porter-target-face
        // porter will replace this with the shared `BPTargetFace` Canvas once
        // it lands).
        val rings = listOf(1.0f, 0.8f, 0.6f, 0.4f, 0.2f)
        rings.forEach { ratio ->
            drawCircle(
                color = AppLine,
                radius = r * ratio,
                center = centre,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        // Centre dot
        drawCircle(color = AppPondDk, radius = r * 0.06f, center = centre)
        // Arrow dots — clamp -1..1 to the face radius.
        arrows.forEach { p ->
            val px = centre.x + (p.x.toFloat() * r)
            val py = centre.y + (p.y.toFloat() * r)
            drawCircle(
                color = AppInk,
                radius = (r * 0.04f).coerceAtLeast(2.dp.toPx()),
                center = Offset(px, py),
            )
        }
    }
}

// ── 04 · Tuning insight ──────────────────────────────────────────────────────

@Composable
private fun TuningInsightCard(data: FeedSummaryUi.Insight) {
    CardFrame {
        CardHeader(
            eyebrow = "Tuning insight",
            detail = "${data.sampleSize} arrows",
            linkLabel = "Review",
            maple = true,
            // Currently no insight-detail destination is wired on Android;
            // leave onLink null until the SuggestionDetail route is exposed.
            onLink = null,
        )
        Text(
            text = data.headline,
            style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Medium),
            color = AppInk,
        )
        // 2x2 metric grid.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data.metrics.take(2).forEach { cell ->
                InsightCell(cell, modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data.metrics.drop(2).take(2).forEach { cell ->
                InsightCell(cell, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InsightCell(metric: FeedSummaryUi.InsightMetric, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = metric.label.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
        Text(
            text = metric.value,
            style = frauncesDisplay(15.sp, italic = true, weight = FontWeight.Medium),
            color = if (metric.maple) AppMaple else AppInk,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// Suppress an unused-warning for an internal symbol that the porter has
// kept for parity with the iOS reference but doesn't currently need.
@Suppress("unused")
private val _accentReference: Color = AppInk

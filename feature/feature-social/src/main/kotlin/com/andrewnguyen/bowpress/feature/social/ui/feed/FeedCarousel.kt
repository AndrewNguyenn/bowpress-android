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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import com.andrewnguyen.bowpress.core.designsystem.AppCream
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
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPlottedTarget
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout

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
// Data: populated from `GET /social/feed-summary` via FeedViewModel
// (see FeedSummaryMapper for the DTO → UI translation). [FeedSummaryUi.preview]
// remains for Compose @Preview composables only.
// =============================================================================

/**
 * iOS parity (A3) — UI-side mirror of the `FeedSummary` core-model DTO.
 * Lives in the feature module on purpose so the carousel's per-card
 * presentation choices (range-label formatting, day-letter derivation,
 * card-collapse rules) stay out of the shared model. Fields are nullable
 * so a card with no data drops out cleanly.
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
        /** Pre-formatted "20yd" / "50m" / "70m" — drops out of the spec line when null. */
        val distanceLabel: String? = null,
        /** Pre-formatted "110gr X10" — drops out of the spec line when null/blank. */
        val arrowLabel: String? = null,
        /** Face type for the single-spot mini-face renderer. */
        val targetFaceType: TargetFaceType? = null,
        /** Multi-spot layout. SINGLE / null = bullseye; TRIANGLE / VERTICAL = 3-spot. */
        val targetLayout: TargetLayout? = null,
        /** Pre-formatted "tue 12:23pm" — shown in the card footer. */
        val startedAtRelative: String? = null,
    )

    data class InsightMetric(val label: String, val value: String, val maple: Boolean = false)

    data class Insight(
        val headline: String,
        val metrics: List<InsightMetric>,
        val sampleSize: Int,
        /** Suggestion the insight derived from — drives a future "Review" deep-link. */
        val suggestionId: String? = null,
        /** Bow the suggestion is scoped to — same purpose as [suggestionId]. */
        val bowId: String? = null,
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
                distanceLabel = "20yd",
                arrowLabel = "140gr",
                targetFaceType = TargetFaceType.TEN_RING,
                targetLayout = TargetLayout.SINGLE,
                startedAtRelative = "tue 12:23pm",
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
            eyebrow = "Best this week",
            detail = "top session",
            linkLabel = "Open",
            onLink = onOpen,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Hero: "9.9 · 14X / 30" — avg in italic Fraunces, stat line in
                // mono. The "14X" run is medium-weight AppPondDk so the X-count
                // reads as the highlight (iOS parity).
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f".format(data.avgRing),
                        style = frauncesDisplay(38.sp, italic = true, weight = FontWeight.Medium),
                        color = AppPondDk,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = AppInk3)) { append("· ") }
                            withStyle(SpanStyle(color = AppPondDk, fontWeight = FontWeight.Medium)) {
                                append("${data.xCount}X")
                            }
                            withStyle(SpanStyle(color = AppInk3)) { append(" / ${data.totalArrows}") }
                        },
                        style = jetbrainsMono(11.sp),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = data.sessionName,
                    style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Medium),
                    color = AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val spec = bestSessionSpecLine(data)
                if (spec.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = spec,
                        style = jetbrainsMono(10.sp),
                        color = AppInk3,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            MiniTargetFace(
                arrows = data.arrows,
                layout = data.targetLayout ?: TargetLayout.SINGLE,
                faceType = data.targetFaceType ?: TargetFaceType.TEN_RING,
                modifier = Modifier.size(70.dp),
            )
        }
        BestSessionFooter(data)
    }
}

private fun bestSessionSpecLine(data: FeedSummaryUi.BestSession): String =
    listOfNotNull(
        data.distanceLabel?.takeIf { it.isNotBlank() },
        data.bowName.takeIf { it.isNotBlank() },
        data.arrowLabel?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

@Composable
private fun BestSessionFooter(data: FeedSummaryUi.BestSession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // iOS draws a 0.5pt hairline above the footer to separate it from
            // the hero block. drawBehind lets us paint it without nesting a
            // wrapper Box.
            .drawBehind {
                drawLine(
                    color = AppLine,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (data.prDeltaAvgRing != null) {
            val delta = data.prDeltaAvgRing
            val sign = if (delta >= 0) "+" else "-"
            val tint = if (delta >= 0) AppPondDk else AppMaple
            Text(
                text = "PR · ",
                style = jetbrainsMono(10.sp),
                color = AppInk3,
            )
            Text(
                text = "$sign%.1f".format(kotlin.math.abs(delta)),
                style = jetbrainsMono(10.sp, FontWeight.Medium),
                color = tint,
            )
            Text(
                text = " over previous best",
                style = jetbrainsMono(10.sp),
                color = AppInk3,
            )
        } else {
            Text(
                text = "New baseline",
                style = jetbrainsMono(10.sp),
                color = AppInk3,
            )
        }
        Spacer(Modifier.weight(1f))
        if (!data.startedAtRelative.isNullOrBlank()) {
            Text(
                text = data.startedAtRelative,
                style = jetbrainsMono(10.sp),
                color = AppInk3,
            )
        }
    }
}

// Design-fixed perceptual ramp — iOS parity. Capped at 6 arrows so the
// face stays readable even when the server happens to return more.
private val MINI_ARROW_FADE_RAMP: List<Float> = listOf(1.0f, 0.78f, 0.62f, 0.5f, 0.38f, 0.28f)

/**
 * `modifier` must size the face to a square — `BPPlottedTarget` always
 * self-squares via `.fillMaxWidth().aspectRatio(1f)`, and a non-square
 * outer would let the overlay arrow dots drift off the painted face.
 */
@Composable
private fun MiniTargetFace(
    arrows: List<FeedSummaryUi.ArrowPoint>,
    layout: TargetLayout,
    faceType: TargetFaceType,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Face only — pass empty arrows so BPPlottedTarget paints the
        // bullseye(s) but doesn't plot anything. The carousel layers its
        // own fade-ramp arrows on top to match iOS's hand-drawn dots.
        BPPlottedTarget(
            arrows = emptyList(),
            layout = layout,
            faceType = faceType,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val centre = Offset(size.width / 2f, size.height / 2f)
            val dotRadius = 2.dp.toPx()
            val haloStroke = 0.75.dp.toPx()
            val limit = arrows.take(MINI_ARROW_FADE_RAMP.size)
            limit.forEachIndexed { idx, p ->
                val px = centre.x + (p.x.toFloat() * r)
                val py = centre.y + (p.y.toFloat() * r)
                val center = Offset(px, py)
                val alpha = MINI_ARROW_FADE_RAMP[idx]
                drawCircle(color = AppInk.copy(alpha = alpha), radius = dotRadius, center = center)
                // Cream halo so the dot reads on yellow X/9 rings and the
                // white outer band — matches iOS's Color.appCream 0.75pt
                // stroke on the same dot.
                drawCircle(
                    color = AppCream.copy(alpha = alpha),
                    radius = dotRadius,
                    center = center,
                    style = Stroke(width = haloStroke),
                )
            }
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

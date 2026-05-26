package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.model.FeedSummary
import com.andrewnguyen.bowpress.core.model.FeedSummaryBestSession
import com.andrewnguyen.bowpress.core.model.FeedSummaryDay
import com.andrewnguyen.bowpress.core.model.FeedSummaryInsight
import com.andrewnguyen.bowpress.core.model.FeedSummaryInsightMetric
import com.andrewnguyen.bowpress.core.model.FeedSummaryOpeningCard
import com.andrewnguyen.bowpress.core.model.FeedSummarySnapshot
import com.andrewnguyen.bowpress.core.model.FeedSummaryThisWeek
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedSummaryUi
import com.andrewnguyen.bowpress.feature.social.ui.feed.toUi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FeedSummaryMapperTest {

    private val utc = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 5, 26)

    @Test
    fun `snapshot range label formats inclusive iso bounds`() {
        val summary = baseSummary(
            snapshot = FeedSummarySnapshot(
                sessionsThis = 4,
                sessionsLast = 2,
                arrowsThis = 184,
                arrowsLast = 96,
                avgRingThis = 8.4,
                avgRingLast = 7.9,
                rangeStart = "2026-05-18",
                rangeEnd = "2026-05-24",
            ),
        )
        val ui = summary.toUi(now = today, zone = utc)
        assertThat(ui.snapshot?.rangeLabel).isEqualTo("mon may 18 → sun may 24")
    }

    @Test
    fun `this week days map Mon-anchored letters in UTC zone (no-shift baseline)`() {
        val summary = baseSummary(
            thisWeek = FeedSummaryThisWeek(
                weekStreak = 3,
                totalArrows = 0,
                sessionCount = 0,
                days = listOf(
                    FeedSummaryDay("2026-05-18", 0),  // Mon
                    FeedSummaryDay("2026-05-19", 0),  // Tue
                    FeedSummaryDay("2026-05-20", 0),  // Wed
                    FeedSummaryDay("2026-05-21", 0),  // Thu
                    FeedSummaryDay("2026-05-22", 0),  // Fri
                    FeedSummaryDay("2026-05-23", 0),  // Sat
                    FeedSummaryDay("2026-05-24", 0),  // Sun
                ),
            ),
        )
        val labels = summary.toUi(now = today, zone = utc).thisWeek!!.days.map { it.label }
        assertThat(labels).containsExactly("M", "T", "W", "T", "F", "S", "S").inOrder()
    }

    @Test
    fun `pacific zone shifts UTC-midnight dayKey back to the prior local day`() {
        // The whole point of the local-TZ shift: a UTC-Monday dayKey
        // should label as "S" (Sunday) for a West-Coast archer, and the
        // "today" highlight should respect the archer's local calendar,
        // not server-UTC. Mirrors the iOS comment on FeedSummaryDay.
        val pacific = ZoneId.of("America/Los_Angeles")
        val pacificToday = LocalDate.of(2026, 5, 24)  // a Sunday in LA
        val summary = baseSummary(
            thisWeek = FeedSummaryThisWeek(
                weekStreak = 1,
                totalArrows = 0,
                sessionCount = 0,
                days = listOf(
                    FeedSummaryDay("2026-05-25", 0),  // UTC Mon midnight = Sun 5pm LA
                    FeedSummaryDay("2026-05-26", 0),  // UTC Tue midnight = Mon 5pm LA
                ),
            ),
        )
        val days = summary.toUi(now = pacificToday, zone = pacific).thisWeek!!.days
        assertThat(days.map { it.label }).containsExactly("S", "M").inOrder()
        assertThat(days.map { it.isToday }).containsExactly(true, false).inOrder()
    }

    @Test
    fun `isToday highlights the local-tz day matching now`() {
        val summary = baseSummary(
            thisWeek = FeedSummaryThisWeek(
                weekStreak = 1,
                totalArrows = 0,
                sessionCount = 0,
                days = listOf(
                    FeedSummaryDay("2026-05-25", 0),
                    FeedSummaryDay("2026-05-26", 0),
                    FeedSummaryDay("2026-05-27", 0),
                ),
            ),
        )
        val ui = summary.toUi(now = today, zone = utc).thisWeek!!.days
        assertThat(ui.map { it.isToday }).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun `opening card enum maps each variant`() {
        fun openerFor(card: FeedSummaryOpeningCard) =
            baseSummary().copy(openingCard = card).toUi(now = today, zone = utc).openingCard
        assertThat(openerFor(FeedSummaryOpeningCard.ThisWeek)).isEqualTo(FeedSummaryUi.OpeningCard.ThisWeek)
        assertThat(openerFor(FeedSummaryOpeningCard.Snapshot)).isEqualTo(FeedSummaryUi.OpeningCard.Snapshot)
        assertThat(openerFor(FeedSummaryOpeningCard.BestSession)).isEqualTo(FeedSummaryUi.OpeningCard.BestSession)
        assertThat(openerFor(FeedSummaryOpeningCard.Insight)).isEqualTo(FeedSummaryUi.OpeningCard.Insight)
    }

    @Test
    fun `best session maps spec fields and formats startedAt for the local zone`() {
        val pacific = ZoneId.of("America/Los_Angeles")
        // 2026-05-26T19:23:00Z = 12:23pm PDT on a Tuesday.
        val summary = baseSummary(
            bestSession = FeedSummaryBestSession(
                sessionId = "s1",
                sharedSessionId = null,
                sessionName = "Midday shooting session",
                avgRing = 9.9,
                xCount = 14,
                totalArrows = 30,
                bowName = "Hoyt RX-7",
                arrows = emptyList(),
                prDeltaAvgRing = -0.1,
                distance = com.andrewnguyen.bowpress.core.model.ShootingDistance.YARDS_20,
                arrowLabel = "140gr",
                targetFaceType = com.andrewnguyen.bowpress.core.model.TargetFaceType.TEN_RING,
                targetLayout = com.andrewnguyen.bowpress.core.model.TargetLayout.TRIANGLE,
                startedAt = "2026-05-26T19:23:00Z",
            ),
        )
        val bs = summary.toUi(now = today, zone = pacific).bestSession!!
        assertThat(bs.distanceLabel).isEqualTo("20yd")
        assertThat(bs.arrowLabel).isEqualTo("140gr")
        assertThat(bs.targetLayout).isEqualTo(com.andrewnguyen.bowpress.core.model.TargetLayout.TRIANGLE)
        assertThat(bs.startedAtRelative).isEqualTo("tue 12:23pm")
    }

    @Test
    fun `insight forwards suggestionId and bowId for future deep-link`() {
        val summary = baseSummary(
            insight = FeedSummaryInsight(
                headline = "Try a small left adjustment.",
                metrics = emptyList(),
                sampleSize = 42,
                suggestionId = "sug-123",
                bowId = "bow-7",
            ),
        )
        val insight = summary.toUi(now = today, zone = utc).insight!!
        assertThat(insight.suggestionId).isEqualTo("sug-123")
        assertThat(insight.bowId).isEqualTo("bow-7")
    }

    @Test
    fun `all four cards copy across with non-null payloads`() {
        val summary = FeedSummary(
            thisWeek = FeedSummaryThisWeek(weekStreak = 2, days = emptyList(), totalArrows = 10, sessionCount = 1),
            snapshot = FeedSummarySnapshot(
                sessionsThis = 1, sessionsLast = 0, arrowsThis = 10, arrowsLast = 0,
                avgRingThis = null, avgRingLast = null,
                rangeStart = "2026-05-18", rangeEnd = "2026-05-24",
            ),
            bestSession = FeedSummaryBestSession(
                sessionId = "s1", sharedSessionId = "ss1", sessionName = "Tuesday Vegas",
                avgRing = 8.7, xCount = 4, totalArrows = 30, bowName = "Hoyt RX-7",
                arrows = emptyList(), prDeltaAvgRing = 0.4,
            ),
            insight = FeedSummaryInsight(
                headline = "Try a small left adjustment.",
                metrics = listOf(FeedSummaryInsightMetric("Drift", "3/16″ R", maple = true)),
                sampleSize = 60,
            ),
            openingCard = FeedSummaryOpeningCard.BestSession,
        )
        val ui = summary.toUi(now = today, zone = utc)
        assertThat(ui.cards).hasSize(4)
        assertThat(ui.bestSession?.sharedSessionId).isEqualTo("ss1")
        assertThat(ui.insight?.metrics?.first()?.maple).isTrue()
    }

    private fun baseSummary(
        thisWeek: FeedSummaryThisWeek? = null,
        snapshot: FeedSummarySnapshot? = null,
        bestSession: FeedSummaryBestSession? = null,
        insight: FeedSummaryInsight? = null,
    ) = FeedSummary(
        thisWeek = thisWeek,
        snapshot = snapshot,
        bestSession = bestSession,
        insight = insight,
        openingCard = FeedSummaryOpeningCard.ThisWeek,
    )
}

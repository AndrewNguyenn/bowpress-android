package com.andrewnguyen.bowpress.core.analytics

import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TrendInsight
import com.andrewnguyen.bowpress.core.model.Zone
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Golden-value coverage for [LocalAnalyticsEngine]. Each test feeds a tiny hand-
 * built dataset and asserts the resulting scores with pre-computed expectations.
 * These numbers must stay identical to what the iOS engine produces — drift
 * surfaces in the user-visible "avg score" / "X%" widgets.
 */
class LocalAnalyticsEngineTest {

    /** Fixed "now" so period windows are deterministic. */
    private val now: Instant = Instant.parse("2026-04-22T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val engine = LocalAnalyticsEngine(clock)

    /** Arrow helper — keeps the tests readable. */
    private fun arrow(
        sessionId: String,
        ring: Int,
        shotAt: Instant,
        excluded: Boolean = false,
        id: String = "p_${sessionId}_$ring-$shotAt",
    ) = ArrowPlot(
        id = id,
        sessionId = sessionId,
        bowConfigId = "bc",
        arrowConfigId = "ac",
        ring = ring,
        zone = Zone.CENTER,
        shotAt = shotAt,
        excluded = excluded,
    )

    private fun session(
        id: String,
        startedAt: Instant,
        bowConfigId: String = "bc",
        feelTags: List<String> = emptyList(),
    ) = ShootingSession(
        id = id,
        bowId = "b1",
        bowConfigId = bowConfigId,
        arrowConfigId = "ac",
        startedAt = startedAt,
        feelTags = feelTags,
    )

    // -------------------------------------------------------------------------
    // Overview
    // -------------------------------------------------------------------------

    /**
     * Input: session "a" (2 days ago), arrows [10, 11, 11, 9]. One X, one excluded.
     * 7-day window → includes everything.
     *
     * Hand computation:
     *   avgScore excludes `excluded=true` arrows; none here → (10 + 11 + 11 + 9) / 4 = 10.25
     *   xRate   does NOT exclude excluded arrows (matches iOS); 2 Xs / 4 arrows = 0.5 → 50%
     */
    @Test
    fun `overview computes avg score and X percentage across window`() {
        val shot = now.minusSeconds(2 * 86400)
        val s = session("a", shot)
        val arrows = listOf(
            arrow("a", ring = 10, shotAt = shot, id = "a1"),
            arrow("a", ring = 11, shotAt = shot, id = "a2"),
            arrow("a", ring = 11, shotAt = shot, id = "a3"),
            arrow("a", ring = 9, shotAt = shot, id = "a4"),
        )
        val o = engine.overview(AnalyticsPeriod.WEEK, listOf(s), arrows)

        assertThat(o.sessionCount).isEqualTo(1)
        assertThat(o.avgArrowScore).isEqualTo(10.25)
        assertThat(o.xPercentage).isEqualTo(50.0)
        assertThat(o.period).isEqualTo(AnalyticsPeriod.WEEK)
    }

    /**
     * Input: one arrow inside the 3d window, one arrow 5 days old.
     *   Window-filtered arrows = [ring 10]
     *   avg = 10.0, xRate = 0 (no 11s)
     */
    @Test
    fun `overview clips arrows outside the period window`() {
        val inWindow = now.minusSeconds(1 * 86400)
        val outsideWindow = now.minusSeconds(5 * 86400)

        val sessions = listOf(session("a", inWindow))
        val arrows = listOf(
            arrow("a", ring = 10, shotAt = inWindow, id = "in"),
            arrow("a", ring = 11, shotAt = outsideWindow, id = "out"),
        )
        val o = engine.overview(AnalyticsPeriod.THREE_DAYS, sessions, arrows)

        assertThat(o.sessionCount).isEqualTo(1)
        assertThat(o.avgArrowScore).isEqualTo(10.0)
        assertThat(o.xPercentage).isEqualTo(0.0)
    }

    /**
     * Mirrors iOS `LocalAnalyticsEngine.overview` — arrows are prefiltered with
     * `!excluded` before reaching either avgScore or xRate, so excluded arrows
     * drop out of BOTH calculations (unlike the raw helpers below).
     *
     * Input: [11, 10, 10-excluded]. After prefilter: [11, 10].
     *   avgScore     = (11 + 10) / 2 = 10.5
     *   xRate        = 1 / 2 = 0.5 → xPercentage 50.0
     */
    @Test
    fun `overview drops excluded arrows before scoring`() {
        val at = now.minusSeconds(86_400)
        val sessions = listOf(session("a", at))
        val arrows = listOf(
            arrow("a", ring = 11, shotAt = at, id = "x1"),
            arrow("a", ring = 10, shotAt = at, id = "x2"),
            arrow("a", ring = 10, shotAt = at, excluded = true, id = "x3"),
        )
        val o = engine.overview(AnalyticsPeriod.WEEK, sessions, arrows)

        assertThat(o.avgArrowScore).isEqualTo(10.5)
        assertThat(o.xPercentage).isEqualTo(50.0)
    }

    // -------------------------------------------------------------------------
    // Comparison
    // -------------------------------------------------------------------------

    /**
     * Period = 3 days → window 1 covers (now - 3d, now], window 2 covers (now - 6d, now - 3d].
     * Session A 1d ago, arrows ring = [10, 11]  (mean 10.5)
     * Session B 4d ago, arrows ring = [9, 9]    (mean 9.0)
     * Session C 8d ago → outside both windows, should not surface anywhere.
     */
    @Test
    fun `comparison splits sessions into current and previous windows`() {
        val a = session("A", now.minusSeconds(86_400))
        val b = session("B", now.minusSeconds(4 * 86_400))
        val c = session("C", now.minusSeconds(8 * 86_400))

        val arrows = listOf(
            arrow("A", 10, a.startedAt, id = "a1"),
            arrow("A", 11, a.startedAt, id = "a2"),
            arrow("B", 9, b.startedAt, id = "b1"),
            arrow("B", 9, b.startedAt, id = "b2"),
            arrow("C", 6, c.startedAt, id = "c1"),
        )

        val cmp = engine.comparison(AnalyticsPeriod.THREE_DAYS, listOf(a, b, c), arrows)

        assertThat(cmp.current.sessionCount).isEqualTo(1)
        assertThat(cmp.current.avgArrowScore).isEqualTo(10.5)
        assertThat(cmp.current.xPercentage).isEqualTo(50.0)

        assertThat(cmp.previous.sessionCount).isEqualTo(1)
        assertThat(cmp.previous.avgArrowScore).isEqualTo(9.0)
        assertThat(cmp.previous.xPercentage).isEqualTo(0.0)

        assertThat(cmp.current.label).isEqualTo("Last 3 Days")
        assertThat(cmp.previous.label).isEqualTo("Previous 3 Days")
    }

    // -------------------------------------------------------------------------
    // Insights
    // -------------------------------------------------------------------------

    /** Fewer than 2 sessions → no insights at all. */
    @Test
    fun `multiSessionInsights returns empty when fewer than two sessions`() {
        val s = session("only", now.minusSeconds(86_400))
        val result = engine.multiSessionInsights(listOf(s), emptyList())
        assertThat(result).isEmpty()
    }

    /**
     * Plateau heuristic — 10 sessions with ≥8 scoring, stdDev < 0.15.
     * We seed each session with 5 arrows averaging 10.0; stdDev is zero.
     */
    @Test
    fun `multiSessionInsights surfaces plateau when variance is tight`() {
        val sessions = (0 until 10).map { i ->
            session("s$i", now.minusSeconds((10L - i) * 86_400))
        }
        val arrows = sessions.flatMap { s ->
            (0 until 5).map { j ->
                arrow(s.id, ring = 10, shotAt = s.startedAt, id = "${s.id}_$j")
            }
        }

        val insights = engine.multiSessionInsights(sessions, arrows)
        val plateau = insights.singleOrNull { it.id == "plateau" }
        assertThat(plateau).isNotNull()
        assertThat(plateau!!.kind).isEqualTo(TrendInsight.Kind.INFO)
    }

    /**
     * Post-tuning (recovery branch):
     *
     *   pre  (cfg-A): avgs 10.0, 10.0, 10.0  → preMean = 10.0
     *   post (cfg-B): avgs  9.0, 10.0, 10.0  → postFirst = 9.5, postLast = 10.0
     *
     *   postFirst < preMean - 0.2   →  9.5  < 9.8    ✓
     *   postLast  ≥ preMean - 0.1   → 10.0 ≥ 9.9     ✓   → POSITIVE
     */
    @Test
    fun `post tuning insight fires with recovery pattern`() {
        val bowA = "cfg-A"
        val bowB = "cfg-B"
        val starts = (0 until 6).map { i -> now.minusSeconds((6L - i) * 86_400) }
        val sessions = listOf(
            session("p0", starts[0], bowConfigId = bowA),
            session("p1", starts[1], bowConfigId = bowA),
            session("p2", starts[2], bowConfigId = bowA),
            session("p3", starts[3], bowConfigId = bowB),
            session("p4", starts[4], bowConfigId = bowB),
            session("p5", starts[5], bowConfigId = bowB),
        )
        // Two arrows per session — their average equals `desired`.
        val desired = listOf(10, 10, 10, 9, 10, 10)
        val arrows = sessions.zip(desired).flatMap { (s, ring) ->
            listOf(
                arrow(s.id, ring = ring, shotAt = s.startedAt, id = "${s.id}_1"),
                arrow(s.id, ring = ring, shotAt = s.startedAt, id = "${s.id}_2"),
            )
        }

        val insights = engine.multiSessionInsights(sessions, arrows)
        val tuning = insights.singleOrNull { it.id == "post_tuning_effect" }
        assertThat(tuning).isNotNull()
        assertThat(tuning!!.kind).isEqualTo(TrendInsight.Kind.POSITIVE)
    }

    /**
     * Condition correlation — needs ≥6 sessions and a tag that appears in ≥3 and is
     * absent from ≥3. We give tag "wind" to 3 sessions with avg 9, untagged 3 with avg 11
     * → |delta| = 2.0 > 0.4 → fires.
     */
    @Test
    fun `condition correlation insight fires when tag delta is meaningful`() {
        val sessions = (0 until 6).map { i ->
            session(
                id = "s$i",
                startedAt = now.minusSeconds((6L - i) * 86_400),
                feelTags = if (i < 3) listOf("wind") else emptyList(),
            )
        }
        val arrows = sessions.flatMap { s ->
            val ring = if ("wind" in s.feelTags) 9 else 11
            (0 until 3).map { j ->
                arrow(s.id, ring = ring, shotAt = s.startedAt, id = "${s.id}_$j")
            }
        }

        val insights = engine.multiSessionInsights(sessions, arrows)
        val corr = insights.singleOrNull { it.id.startsWith("condition_correlation_") }
        assertThat(corr).isNotNull()
        assertThat(corr!!.id).isEqualTo("condition_correlation_wind")
    }

    // -------------------------------------------------------------------------
    // Pure helpers
    // -------------------------------------------------------------------------

    @Test
    fun `avgScore excludes excluded arrows and returns zero on empty`() {
        assertThat(engine.avgScore(emptyList())).isEqualTo(0.0)
        val at = now.minusSeconds(100)
        val arrows = listOf(
            arrow("s", 10, at, id = "a"),
            arrow("s", 11, at, id = "b"),
            arrow("s", 8, at, excluded = true, id = "c"),
        )
        // (10 + 11) / 2 = 10.5 — `c` is excluded.
        assertThat(engine.avgScore(arrows)).isEqualTo(10.5)
    }

    @Test
    fun `xRate counts ring eleven against the full denominator`() {
        val at = now
        val arrows = listOf(
            arrow("s", 11, at, id = "x1"),
            arrow("s", 10, at, id = "x2"),
            arrow("s", 9, at, id = "x3"),
        )
        // 1 / 3 = 0.333…
        assertThat(engine.xRate(arrows)).isWithin(1e-9).of(1.0 / 3.0)
    }
}

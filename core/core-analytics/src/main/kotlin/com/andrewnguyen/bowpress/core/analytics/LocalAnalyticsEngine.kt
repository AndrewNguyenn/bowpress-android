package com.andrewnguyen.bowpress.core.analytics

import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import com.andrewnguyen.bowpress.core.model.ShootingSession
import java.time.Clock
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Direct port of `Sources/BowPress/Services/LocalAnalyticsEngine.swift`.
 *
 * Pure functions — no platform deps, no network, no storage. Callers feed in the raw
 * sessions + plots (and optionally [BowConfiguration]s) they've already gathered, and
 * the engine computes the aggregates iOS renders in the Analytics tab.
 *
 * `Clock` is injected so tests can exercise period slicing deterministically.
 *
 * IMPORTANT: behavior must match the iOS engine line-by-line. Arithmetic, filtering,
 * and threshold constants are all reproduced verbatim — changes here quietly break
 * cross-platform parity of the score numbers.
 */
class LocalAnalyticsEngine(
    private val clock: Clock = Clock.systemUTC(),
) {

    // -------------------------------------------------------------------------
    // Overview
    // -------------------------------------------------------------------------

    /**
     * Mirrors `LocalAnalyticsEngine.overview(period:)` — sessions and plots are filtered
     * to the given window; X% is `ring == 11` divided by total, times 100.
     */
    fun overview(
        period: AnalyticsPeriod,
        sessions: List<ShootingSession>,
        plots: List<ArrowPlot>,
    ): AnalyticsOverview {
        val periodStart = startOfPeriod(period)
        val windowSessions = sessions.filter { !it.startedAt.isBefore(periodStart) }
        val windowPlots = plots
            .filter { !it.excluded }
            .filter { !it.shotAt.isBefore(periodStart) }
        val sessionIds = windowSessions.map { it.id }.toHashSet()
        val periodArrows = windowPlots.filter { it.sessionId in sessionIds }
        return AnalyticsOverview(
            period = period,
            sessionCount = windowSessions.size,
            avgArrowScore = avgScore(periodArrows),
            xPercentage = xRate(periodArrows) * 100.0,
            suggestions = emptyList(),
        )
    }

    // -------------------------------------------------------------------------
    // Comparison
    // -------------------------------------------------------------------------

    /**
     * Mirrors `comparison(period:)` — splits sessions into "current" and "previous"
     * windows of equal length, each measuring back from `now`.
     */
    fun comparison(
        period: AnalyticsPeriod,
        sessions: List<ShootingSession>,
        plots: List<ArrowPlot>,
        configs: List<BowConfiguration> = emptyList(),
    ): PeriodComparison {
        val now = clock.instant()
        val duration = period.durationSeconds
        val currentStart = now.minusSeconds(duration)
        val previousStart = now.minusSeconds(duration * 2)

        val activeArrows = plots.filter { !it.excluded }

        val currentSessions = sessions.filter { !it.startedAt.isBefore(currentStart) }
        val previousSessions = sessions.filter {
            !it.startedAt.isBefore(previousStart) && it.startedAt.isBefore(currentStart)
        }

        val currentSlice = slice(
            label = "Last ${period.label}",
            sessions = currentSessions,
            arrows = activeArrows,
            configs = configs,
        )
        val previousSlice = slice(
            label = "Previous ${period.label}",
            sessions = previousSessions,
            arrows = activeArrows,
            configs = configs,
        )
        return PeriodComparison(period = period, current = currentSlice, previous = previousSlice)
    }

    private fun slice(
        label: String,
        sessions: List<ShootingSession>,
        arrows: List<ArrowPlot>,
        configs: List<BowConfiguration>,
    ): PeriodSlice {
        val ids = sessions.map { it.id }.toHashSet()
        val plots = arrows.filter { it.sessionId in ids }
        val activeConfigId = sessions.maxByOrNull { it.startedAt }?.bowConfigId
        val activeConfig: BowConfiguration? = activeConfigId?.let { cid ->
            configs.firstOrNull { it.id == cid }
        }
        return PeriodSlice(
            label = label,
            plots = plots,
            avgArrowScore = avgScore(plots),
            xPercentage = xRate(plots) * 100.0,
            sessionCount = sessions.size,
            config = activeConfig,
        )
    }

    // -------------------------------------------------------------------------
    // Multi-session insights — these are the four heuristic trend detectors iOS
    // renders in the Analytics tab. Each returns nil if the input doesn't meet
    // the minimum-sample bar.
    // -------------------------------------------------------------------------

    fun multiSessionInsights(
        sessions: List<ShootingSession>,
        plots: List<ArrowPlot>,
    ): List<TrendInsight> {
        val sorted = sessions.sortedBy { it.startedAt }
        if (sorted.size < 2) return emptyList()
        val activeArrows = plots.filter { !it.excluded }

        return listOfNotNull(
            driftInsight(sorted, activeArrows),
            postTuningInsight(sorted, activeArrows),
            conditionCorrelationInsight(sorted, activeArrows),
            plateauInsight(sorted, activeArrows),
        )
    }

    // ---- Drift ---------------------------------------------------------------

    private fun driftInsight(
        sessions: List<ShootingSession>,
        arrows: List<ArrowPlot>,
    ): TrendInsight? {
        val recent = sessions.takeLast(6)
        if (recent.size < 4) return null

        val arrowMap = arrows.groupBy { it.sessionId }
        val centroids: List<DoubleArray> = recent.mapNotNull { session ->
            val plotsForSession = arrowMap[session.id].orEmpty()
            val real = plotsForSession.mapNotNull { p ->
                val x = p.plotX ?: return@mapNotNull null
                val y = p.plotY ?: return@mapNotNull null
                doubleArrayOf(x, y)
            }
            if (real.size < 3) return@mapNotNull null
            val meanX = real.sumOf { it[0] } / real.size
            val meanY = real.sumOf { it[1] } / real.size
            doubleArrayOf(meanX, meanY)
        }
        if (centroids.size < 4) return null

        val meanX = centroids.sumOf { it[0] } / centroids.size
        val meanY = centroids.sumOf { it[1] } / centroids.size
        val meanDist = hypot(meanX, meanY)
        if (meanDist <= 0.03) return null

        val dominantXPositive = meanX >= 0
        val dominantYPositive = meanY >= 0
        val agreeing = centroids.filter {
            (it[0] == 0.0 || (it[0] > 0) == dominantXPositive) &&
                (it[1] == 0.0 || (it[1] > 0) == dominantYPositive)
        }
        if (agreeing.size < 4) return null

        val distMm = String.format(Locale.US, "%.1f", meanDist * MM_PER_NORM)
        val dir = driftDirection(meanX, meanY)
        val param = driftParameterHint(meanX, meanY)
        return TrendInsight(
            id = "multi_session_drift",
            icon = "scope",
            headline = "Group center ~${distMm}mm $dir across ${agreeing.size} sessions",
            detail = "Your group center has been consistently $dir of target center. $param Check whether this drift coincides with a recent config change or form pattern.",
            kind = TrendInsight.Kind.NEUTRAL,
        )
    }

    private fun driftDirection(x: Double, y: Double): String {
        val adx = abs(x); val ady = abs(y)
        if (ady > adx * 1.7) return if (y > 0) "north (high)" else "south (low)"
        if (adx > ady * 1.7) return if (x > 0) "right" else "left"
        val v = if (y > 0) "high" else "low"
        val h = if (x > 0) "right" else "left"
        return "$v-$h"
    }

    private fun driftParameterHint(x: Double, y: Double): String {
        val adx = abs(x); val ady = abs(y)
        if (ady > adx * 1.7) {
            return if (y > 0) {
                "Persistent high drift often points to nocking point too low or peep height too high."
            } else {
                "Persistent low drift often points to nocking point too high."
            }
        }
        return "Persistent horizontal drift often points to rest horizontal position or cant."
    }

    // ---- Post-tuning effect --------------------------------------------------

    private fun postTuningInsight(
        sessions: List<ShootingSession>,
        arrows: List<ArrowPlot>,
    ): TrendInsight? {
        if (sessions.size < 4) return null
        val arrowMap = arrows.groupBy { it.sessionId }

        var changeIdx: Int? = null
        for (i in 1 until sessions.size) {
            if (sessions[i].bowConfigId != sessions[i - 1].bowConfigId) changeIdx = i
        }
        val idx = changeIdx ?: return null

        val pre = sessions.subList(maxOf(0, idx - 2), idx).toList()
        val post = sessions.subList(idx, sessions.size).toList()
        if (pre.size < 1 || post.size < 2) return null

        val preScores = pre.map { avgScore(arrowMap[it.id].orEmpty()) }.filter { it > 0 }
        val postScores = post.map { avgScore(arrowMap[it.id].orEmpty()) }.filter { it > 0 }
        if (preScores.isEmpty() || postScores.isEmpty()) return null

        val preMean = preScores.average()
        val prefixCount = minOf(2, postScores.size)
        val postFirst = postScores.take(prefixCount).sum() / prefixCount
        val postLast = postScores.lastOrNull() ?: postFirst

        return when {
            postFirst < preMean - 0.2 && postLast >= preMean - 0.1 -> {
                val n = postScores.size
                TrendInsight(
                    id = "post_tuning_effect",
                    icon = "wrench.and.screwdriver",
                    headline = "Setup dialing in after recent config change",
                    detail = "Scores dipped for ${if (n > 2) "the first few" else "1–2"} sessions after your last tuning change, then recovered to ${fmt1(postLast)} — above your pre-change average of ${fmt1(preMean)}. The adjustment is holding up.",
                    kind = TrendInsight.Kind.POSITIVE,
                )
            }
            postFirst < preMean - 0.2 && postLast < preMean - 0.2 -> {
                TrendInsight(
                    id = "post_tuning_effect",
                    icon = "wrench.and.screwdriver",
                    headline = "Scores haven't recovered since last config change",
                    detail = "Average score since your last tuning change (${fmt1(postFirst)}) remains below your pre-change baseline (${fmt1(preMean)}). Consider reverting or logging more sessions before drawing conclusions.",
                    kind = TrendInsight.Kind.NEGATIVE,
                )
            }
            else -> null
        }
    }

    // ---- Condition correlation ----------------------------------------------

    private fun conditionCorrelationInsight(
        sessions: List<ShootingSession>,
        arrows: List<ArrowPlot>,
    ): TrendInsight? {
        if (sessions.size < 6) return null
        val arrowMap = arrows.groupBy { it.sessionId }
        val allTags = sessions.flatMap { it.feelTags }.toHashSet()

        data class Best(val tag: String, val delta: Double, val taggedMean: Double, val n: Int)
        var best: Best? = null

        for (tag in allTags) {
            val tagged = sessions.filter { tag in it.feelTags }
            val untagged = sessions.filter { tag !in it.feelTags }
            if (tagged.size < 3 || untagged.size < 3) continue

            val taggedScores = tagged.map { avgScore(arrowMap[it.id].orEmpty()) }.filter { it > 0 }
            val untaggedScores = untagged.map { avgScore(arrowMap[it.id].orEmpty()) }.filter { it > 0 }
            if (taggedScores.isEmpty() || untaggedScores.isEmpty()) continue

            val tm = taggedScores.average()
            val um = untaggedScores.average()
            val delta = abs(tm - um)
            if (delta > 0.4 && (best == null || delta > best.delta)) {
                best = Best(tag = tag, delta = delta, taggedMean = tm, n = tagged.size)
            }
        }
        val b = best ?: return null
        // The Swift source writes `b.taggedMean + b.delta`, comparing tagged mean to itself.
        // The ternary collapses to the first branch whenever delta > 0 (which is guaranteed
        // by the `delta > 0.4` gate above). Preserve that behaviour verbatim.
        val lower = if (b.taggedMean < (b.taggedMean + b.delta)) "lower" else "higher"
        return TrendInsight(
            id = "condition_correlation_${b.tag}",
            icon = "tag",
            headline = "Sessions tagged '${b.tag}' average ${fmt1(b.delta)}pts $lower",
            detail = "Across ${b.n} sessions tagged '${b.tag}', average arrow score is ${fmt1(b.delta)}pts $lower than untagged sessions. If '${b.tag}' reflects a form or equipment issue, addressing it directly may have more impact than further tuning.",
            kind = TrendInsight.Kind.NEUTRAL,
        )
    }

    // ---- Plateau detection ---------------------------------------------------

    private fun plateauInsight(
        sessions: List<ShootingSession>,
        arrows: List<ArrowPlot>,
    ): TrendInsight? {
        if (sessions.size < 8) return null
        val arrowMap = arrows.groupBy { it.sessionId }
        val recent = sessions.takeLast(10)
        val scores = recent.map { avgScore(arrowMap[it.id].orEmpty()) }.filter { it > 0 }
        if (scores.size < 8) return null

        val mean = scores.average()
        val variance = scores.map { (it - mean) * (it - mean) }.sum() / scores.size
        val stdDev = sqrt(variance)
        if (stdDev >= 0.15) return null

        return TrendInsight(
            id = "plateau",
            icon = "chart.bar.fill",
            headline = "Score variance tight at ±${fmt2(stdDev)}pts over ${scores.size} sessions",
            detail = "Your scores have been stable around ${fmt1(mean)} with very little variation. This is a local ceiling — consistent form is good, but if you want to improve, a deliberate equipment change or targeted drill is more likely to move the needle than continued repetition at the current setup.",
            kind = TrendInsight.Kind.INFO,
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Average ring score, excluding excluded arrows. 0 when empty. Mirrors iOS. */
    fun avgScore(plots: List<ArrowPlot>): Double {
        val active = plots.filter { !it.excluded }
        if (active.isEmpty()) return 0.0
        return active.sumOf { it.ring.toDouble() } / active.size
    }

    /**
     * X rate (ring == 11). Returns 0..1 (caller multiplies by 100). Matches iOS
     * behavior exactly — the iOS version does NOT exclude `excluded` arrows from the
     * X rate denominator, so we don't either.
     */
    fun xRate(plots: List<ArrowPlot>): Double {
        if (plots.isEmpty()) return 0.0
        return plots.count { it.ring == 11 }.toDouble() / plots.size
    }

    private fun startOfPeriod(period: AnalyticsPeriod): Instant =
        clock.instant().minusSeconds(period.durationSeconds)

    private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
    private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

    private companion object {
        /** Same scaling constant as iOS — normalized ↔ mm conversion. */
        const val MM_PER_NORM: Double = 20.0 / (119.0 / 735.0)
    }
}

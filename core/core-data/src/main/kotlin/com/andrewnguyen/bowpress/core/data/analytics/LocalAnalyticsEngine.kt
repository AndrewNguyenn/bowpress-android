package com.andrewnguyen.bowpress.core.data.analytics

import com.andrewnguyen.bowpress.core.analytics.LocalAnalyticsEngine as PureLocalAnalyticsEngine
import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BowDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.TrendInsight
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DAO-backed local analytics engine. Direct port of the iOS `LocalAnalyticsEngine`
 * facade that reads from `LocalStore`; delegates the pure computations to the
 * shared [PureLocalAnalyticsEngine] in `core-analytics` so iOS/Android parity
 * tests can exercise the same math.
 */
@Singleton
class LocalAnalyticsEngine @Inject constructor(
    private val sessionDao: SessionDao,
    private val arrowPlotDao: ArrowPlotDao,
    private val bowDao: BowDao,
) {
    private val clock: Clock = Clock.systemUTC()
    private val pure = PureLocalAnalyticsEngine(clock)

    /** Mirrors iOS `LocalAnalyticsEngine.overview(period:bowType:)`. */
    suspend fun overview(period: AnalyticsPeriod, bowType: BowType? = null): AnalyticsOverview {
        val periodStart = clock.instant().minusSeconds(period.durationSeconds)
        var sessions = sessionDao.findSince(periodStart).map { it.toDto() }
        if (bowType != null) {
            val ids = bowIdsForStyle(bowType)
            sessions = sessions.filter { it.bowId in ids }
        }
        val arrows = arrowPlotDao.findSince(periodStart)
            .map { it.toDto() }
            .filter { !it.excluded }
        return pure.overview(period = period, sessions = sessions, plots = arrows)
    }

    /** Mirrors iOS `LocalAnalyticsEngine.comparison(period:bowType:)`. */
    suspend fun comparison(period: AnalyticsPeriod, bowType: BowType? = null): PeriodComparison {
        var allSessions = sessionDao.getAll().map { it.toDto() }
        val allArrows = arrowPlotDao.getAll().map { it.toDto() }.filter { !it.excluded }
        if (bowType != null) {
            val ids = bowIdsForStyle(bowType)
            allSessions = allSessions.filter { it.bowId in ids }
        }
        return pure.comparison(period = period, sessions = allSessions, plots = allArrows)
    }

    /**
     * Mirrors iOS `LocalAnalyticsEngine.multiSessionInsights()`. Returns the four
     * heuristic insights (drift, post-tuning, condition-correlation, plateau).
     */
    suspend fun multiSessionInsights(): List<TrendInsight> {
        val sessions = sessionDao.getAll().map { it.toDto() }
        val arrows = arrowPlotDao.getAll().map { it.toDto() }.filter { !it.excluded }
        return pure.multiSessionInsights(sessions = sessions, plots = arrows)
    }

    private suspend fun bowIdsForStyle(bowType: BowType): Set<String> =
        bowDao.getAll().filter { it.bowType == bowType }.map { it.id }.toHashSet()

    /** Exposed for tests that want to drive the engine with a fixed clock. */
    internal fun nowForTesting(): Instant = clock.instant()
}

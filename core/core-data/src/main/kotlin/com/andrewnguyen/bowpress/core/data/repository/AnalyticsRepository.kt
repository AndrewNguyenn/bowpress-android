package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.TagCorrelation
import com.andrewnguyen.bowpress.core.network.BowPressApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-only repository for analytics endpoints. Analytics responses are aggregates
 * recomputed on demand, so we intentionally skip Room caching here — feature view-models
 * that want "last known values" can hold them in a StateFlow.
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val api: BowPressApi,
) {
    suspend fun overview(period: AnalyticsPeriod): AnalyticsOverview =
        api.fetchAnalyticsOverview(period.wire)

    suspend fun comparison(period: AnalyticsPeriod): PeriodComparison =
        api.fetchAnalyticsComparison(period.wire)

    suspend fun tagCorrelations(bowId: String): List<TagCorrelation> =
        api.fetchTagCorrelations(bowId)
}

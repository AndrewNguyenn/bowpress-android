package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.ConfigurationChange
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
    suspend fun overview(period: AnalyticsPeriod, bowType: BowType? = null): AnalyticsOverview =
        api.fetchAnalyticsOverview(period.wire, bowType?.wire)

    suspend fun comparison(period: AnalyticsPeriod, bowType: BowType? = null): PeriodComparison =
        api.fetchAnalyticsComparison(period.wire, bowType?.wire)

    suspend fun tagCorrelations(bowId: String): List<TagCorrelation> =
        api.fetchTagCorrelations(bowId)

    /**
     * Returns the full history of bow-config changes, most recent first. Mirrors iOS
     * `APIClient.fetchConfigurationChanges(bowId:)`.
     */
    suspend fun fetchConfigurationChanges(bowId: String): List<ConfigurationChange> =
        api.fetchConfigChanges(bowId).sortedByDescending { it.createdAt }

    /** Alias provided so feature-layer code reads symmetrically with [fetchConfigurationChanges]. */
    suspend fun fetchTagCorrelations(bowId: String): List<TagCorrelation> =
        tagCorrelations(bowId)
}

/** Server-side spelling: lowercase enum name matches `@SerialName` on [BowType]. */
private val BowType.wire: String get() = name.lowercase()

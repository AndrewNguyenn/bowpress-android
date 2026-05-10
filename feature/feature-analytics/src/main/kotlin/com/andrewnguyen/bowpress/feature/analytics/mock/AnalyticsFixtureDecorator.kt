package com.andrewnguyen.bowpress.feature.analytics.mock

import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.TimelineResponse
import com.andrewnguyen.bowpress.feature.analytics.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Strategy for filling in / overriding analytics fields the server hasn't sent.
 *
 * Two implementations:
 * - [NoOpAnalyticsFixtureDecorator] — pass-through. Used in release builds and unit tests
 *   so the view-model's data wiring can be exercised without the design-review fixture
 *   forcing values to 10.4 / 72% / 5.
 * - [DebugAnalyticsFixtureDecorator] — fills sparkline + datasetSummary and FORCES headline
 *   numerals to the spec figure (mirrors iOS `decorateOverviewWithMocks`).
 *
 * Hilt picks the right impl at app-build time via [AnalyticsFixtureModule].
 */
interface AnalyticsFixtureDecorator {
    fun decorateOverview(
        overview: AnalyticsOverview,
        firstBow: Bow?,
        firstArrow: ArrowConfiguration?,
    ): AnalyticsOverview

    fun decorateComparison(comparison: PeriodComparison): PeriodComparison

    /** Fallback returned when the server timeline call threw — release builds get null. */
    fun timelineFallback(period: AnalyticsPeriod): TimelineResponse?
}

internal object NoOpAnalyticsFixtureDecorator : AnalyticsFixtureDecorator {
    override fun decorateOverview(
        overview: AnalyticsOverview,
        firstBow: Bow?,
        firstArrow: ArrowConfiguration?,
    ): AnalyticsOverview = overview

    override fun decorateComparison(comparison: PeriodComparison): PeriodComparison = comparison

    override fun timelineFallback(period: AnalyticsPeriod): TimelineResponse? = null
}

internal object DebugAnalyticsFixtureDecorator : AnalyticsFixtureDecorator {
    override fun decorateOverview(
        overview: AnalyticsOverview,
        firstBow: Bow?,
        firstArrow: ArrowConfiguration?,
    ): AnalyticsOverview = MockAnalyticsWave2.decorateOverview(overview, firstBow, firstArrow)

    override fun decorateComparison(comparison: PeriodComparison): PeriodComparison =
        MockAnalyticsWave2.decorateComparison(comparison)

    override fun timelineFallback(period: AnalyticsPeriod): TimelineResponse? =
        MockAnalyticsWave2.timeline(period)
}

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsFixtureModule {
    @Provides
    @Singleton
    fun provideFixtureDecorator(): AnalyticsFixtureDecorator =
        if (BuildConfig.DEBUG) DebugAnalyticsFixtureDecorator else NoOpAnalyticsFixtureDecorator
}

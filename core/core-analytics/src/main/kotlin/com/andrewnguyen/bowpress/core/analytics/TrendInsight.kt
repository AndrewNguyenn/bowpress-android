package com.andrewnguyen.bowpress.core.analytics

/**
 * Mirrors iOS `TrendInsight` (declared inline in `AnalyticsTrendInsightsSection.swift`).
 *
 * `icon` stays a string — iOS names SF Symbols; on Android we pick a Material icon
 * at render time in `feature-analytics`.
 */
data class TrendInsight(
    val id: String,
    val icon: String,
    val headline: String,
    val detail: String,
    val kind: Kind,
) {
    enum class Kind { POSITIVE, NEGATIVE, NEUTRAL, INFO }
}

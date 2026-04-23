package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors iOS `SuggestionEvidence`. Frozen snapshot of the pipeline inputs at synthesis
 * time. Nil when the suggestion pre-dates migration 0015 — callers must degrade gracefully.
 */
@Serializable
data class SuggestionEvidence(
    val sampleSize: Int,
    val sessionIds: List<String> = emptyList(),
    @Serializable(with = InstantSerializer::class)
    val windowStart: Instant,
    @Serializable(with = InstantSerializer::class)
    val windowEnd: Instant,
    val metrics: List<Metric> = emptyList(),
    val relatedConfigChangeIds: List<String>? = null,
    val patternType: String,
) {
    @Serializable
    data class Metric(
        val label: String,
        val value: String,
        val deltaFromBaseline: String? = null,
    )
}

/** Mirrors iOS `AnalyticsSuggestion`. All migration-0015 fields default-tolerantly. */
@Serializable
data class AnalyticsSuggestion(
    val id: String,
    val bowId: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val parameter: String,
    val suggestedValue: String,
    val currentValue: String,
    val reasoning: String,
    val confidence: Double,
    val qualifier: String? = null,
    val wasRead: Boolean,
    val wasDismissed: Boolean = false,
    val deliveryType: DeliveryType,
    val evidence: SuggestionEvidence? = null,
    val wasApplied: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val appliedAt: Instant? = null,
    val appliedConfigId: String? = null,
)

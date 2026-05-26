package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the iOS `SocialFeedSummary.swift` / API `GET /social/feed-summary`
// contract — four optional hero-card payloads plus an `openingCard` hint
// for which slide the carousel should land on. Defaults on every nullable
// card field so a brand-new account (no sessions, no suggestions) still
// decodes to an empty `FeedSummary` rather than a parse failure.

@Serializable
data class FeedSummaryDay(
    /** "YYYY-MM-DD" UTC bucket key. Client derives local day-of-week label + today flag. */
    val dayKey: String,
    val arrows: Int,
)

@Serializable
data class FeedSummaryThisWeek(
    val weekStreak: Int,
    /** Always 7 entries, Mon→Sun UTC. */
    val days: List<FeedSummaryDay>,
    val totalArrows: Int,
    val sessionCount: Int,
)

@Serializable
data class FeedSummarySnapshot(
    val sessionsThis: Int,
    val sessionsLast: Int,
    val arrowsThis: Int,
    val arrowsLast: Int,
    /** Null when there were no scoreable arrows in the window. */
    val avgRingThis: Double? = null,
    val avgRingLast: Double? = null,
    /** "YYYY-MM-DD" inclusive bounds of the current UTC-week. */
    val rangeStart: String,
    val rangeEnd: String,
)

@Serializable
data class FeedSummaryArrowPoint(
    val x: Double,
    val y: Double,
)

@Serializable
data class FeedSummaryBestSession(
    val sessionId: String,
    /** Set when the session is already shared — lets the card "Open" link drill into friend-session detail. */
    val sharedSessionId: String? = null,
    val sessionName: String,
    val avgRing: Double,
    /**
     * Sum of ring values for non-excluded arrows (X scores 10). Max
     * possible is `totalArrows * 10`. Defaulted to null for backward
     * compat with an older API that didn't emit it; the carousel card
     * falls back to "14X / 30" alone in that case.
     */
    val totalScore: Int? = null,
    val xCount: Int,
    val totalArrows: Int,
    val bowName: String,
    /** Up to 6 most-recent non-excluded arrow positions, normalized -1…1 from face centre. */
    val arrows: List<FeedSummaryArrowPoint> = emptyList(),
    /** Delta vs the prior best on the same bow in the last 30d. */
    val prDeltaAvgRing: Double? = null,
    // The following fields are on the wire but not yet rendered by
    // FeedSummaryUi.BestSession — keeping them in the DTO so the
    // contract stays faithful and a card-content parity pass can use
    // them without another core-model migration.
    val distance: ShootingDistance? = null,
    val arrowLabel: String? = null,
    val targetFaceType: TargetFaceType? = null,
    val targetLayout: TargetLayout? = null,
    val startedAt: String? = null,
)

@Serializable
data class FeedSummaryInsightMetric(
    val label: String,
    val value: String,
    /** True when the value should render in the maple alert tint (drift cell). */
    val maple: Boolean = false,
)

@Serializable
data class FeedSummaryInsight(
    val headline: String,
    /** Exactly 4 cells in display order: drift · group size · distance · confidence. */
    val metrics: List<FeedSummaryInsightMetric>,
    val sampleSize: Int,
    // On the wire but not yet routed through FeedSummaryUi.Insight — kept
    // so a future "Review suggestion" deep-link can reuse the same payload.
    val suggestionId: String? = null,
    val bowId: String? = null,
    val surfacedAt: String? = null,
)

/**
 * Which card the carousel should open on. Server-picked per the design's
 * "opens on the most relevant card" rule. Defaulted to `this_week` so a
 * payload from an older API still decodes; `coerceInputValues` on the
 * shared `Json` falls back here for any unknown opener too.
 */
@Serializable
enum class FeedSummaryOpeningCard {
    @SerialName("this_week") ThisWeek,
    @SerialName("snapshot") Snapshot,
    @SerialName("best_session") BestSession,
    @SerialName("insight") Insight,
}

@Serializable
data class FeedSummary(
    val thisWeek: FeedSummaryThisWeek? = null,
    val snapshot: FeedSummarySnapshot? = null,
    val bestSession: FeedSummaryBestSession? = null,
    val insight: FeedSummaryInsight? = null,
    val openingCard: FeedSummaryOpeningCard = FeedSummaryOpeningCard.ThisWeek,
)

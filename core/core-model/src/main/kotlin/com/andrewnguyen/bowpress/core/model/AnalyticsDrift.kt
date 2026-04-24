package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parameter-drift payload returned by `GET /analytics/drift`. Mirrors iOS
 * `DriftResponse`. Powers the "Parameter drift" table — one row per tuning
 * parameter with before/now values, a pre-computed delta string, and a sample
 * size `n`.
 */
@Serializable
data class DriftResponse(
    val period: AnalyticsPeriod,
    val rows: List<DriftRow> = emptyList(),
)

/** Mirrors iOS `DriftRow`. */
@Serializable
data class DriftRow(
    /** Canonical parameter key — matches `AnalyticsSuggestion.parameter`. */
    val parameter: String,
    /** Display name the server emits. */
    val label: String,
    /** Unit suffix appended if the server didn't bake it into before/now. */
    val unit: String,
    /** Pre-formatted before/now/delta strings — fractions like "+3⁄16″" survive the wire. */
    val before: String? = null,
    val now: String? = null,
    val delta: String? = null,
    val deltaTone: DeltaTone,
    /** Sample size — arrows (not sessions) that informed this row. */
    val n: Int,
)

/** Direction token controlling the delta cell's color. Wire: lowercase. */
@Serializable
enum class DeltaTone {
    @SerialName("up") UP,
    @SerialName("down") DOWN,
    @SerialName("flat") FLAT,
}

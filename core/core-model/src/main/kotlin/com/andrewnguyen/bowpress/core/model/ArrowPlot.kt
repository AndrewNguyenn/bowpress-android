package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors iOS `ArrowPlot`. `ring`: 8, 9, 10, 11 — where 11 = X.
 * `plotX`/`plotY` are normalized positions from target centre in (-1..1); may be
 * missing on legacy data.
 */
@Serializable
data class ArrowPlot(
    val id: String,
    val sessionId: String,
    val bowConfigId: String,
    val arrowConfigId: String,
    val ring: Int,
    val zone: Zone,
    val plotX: Double? = null,
    val plotY: Double? = null,
    val endId: String? = null,
    @Serializable(with = InstantSerializer::class)
    val shotAt: Instant,
    val excluded: Boolean = false,
    val notes: String? = null,
)

package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/** Mirrors iOS `SessionConditions`. Included inline on `ShootingSession`. */
@Serializable
data class SessionConditions(
    val windSpeed: Double? = null,
    val tempF: Double? = null,
    val lighting: String? = null,
)

/**
 * Mirrors iOS `ShootingSession`. The nested `ends` / `arrows` arrays are populated on
 * some responses (e.g. session detail) but usually nil on list endpoints — treat them
 * as optional.
 */
@Serializable
data class ShootingSession(
    val id: String,
    val bowId: String,
    val bowConfigId: String,
    val arrowConfigId: String,
    @Serializable(with = InstantSerializer::class)
    val startedAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val endedAt: Instant? = null,
    val notes: String = "",
    val feelTags: List<String> = emptyList(),
    val conditions: SessionConditions? = null,
    val arrowCount: Int = 0,
    val ends: List<SessionEnd>? = null,
    val arrows: List<ArrowPlot>? = null,
)

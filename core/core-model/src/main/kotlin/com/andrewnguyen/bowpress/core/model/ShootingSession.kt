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
    // Default to TEN_RING (the WA full face), not SIX_RING — legacy / synced
    // rows that omit targetFaceType used to force-decode as SIX_RING, which
    // rendered every recurve / barebow archer's session as a compound 6-ring
    // face. TEN_RING is the more conservative renderer. Mirrors iOS commit
    // faf1113. (parity B4)
    val targetFaceType: TargetFaceType = TargetFaceType.TEN_RING,
    /** How the faces are arranged on the boss. Defaulted for legacy rows. */
    val targetLayout: TargetLayout = TargetLayout.SINGLE,
    /** Optional shooting distance — null for sessions that predate the field. */
    val distance: ShootingDistance? = null,
    /** Optional human-supplied title (e.g. "Long-distance work"). Null for legacy rows. */
    val title: String? = null,
    /** Which discipline this session records. Legacy rows decode as `RANGE`. */
    val sessionType: SessionType = SessionType.RANGE,
    /** 3D scoring system — set only for 3D-course sessions; null for range. */
    val scoringSystem: ThreeDScoringSystem? = null,
    val ends: List<SessionEnd>? = null,
    val arrows: List<ArrowPlot>? = null,
)

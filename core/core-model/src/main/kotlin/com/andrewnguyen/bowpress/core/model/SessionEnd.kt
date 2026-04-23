package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/** Mirrors iOS `SessionEnd`. */
@Serializable
data class SessionEnd(
    val id: String,
    val sessionId: String,
    val endNumber: Int,
    val notes: String? = null,
    @Serializable(with = InstantSerializer::class)
    val completedAt: Instant,
)

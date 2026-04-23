package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors server `DeviceToken` (see `deviceTokenController.ts`). On Android, `environment`
 * must be `"development"` or `"production"` — the wire contract is a free-form string to
 * leave room for future values.
 */
@Serializable
data class DeviceToken(
    val id: String,
    val userId: String,
    val token: String,
    val environment: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val lastSeenAt: Instant,
)

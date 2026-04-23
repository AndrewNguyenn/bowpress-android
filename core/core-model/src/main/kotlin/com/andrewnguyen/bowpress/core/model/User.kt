package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors iOS `User`. `emailVerified` and `authProvider` are optional so we
 * tolerate older cached responses that pre-date those fields.
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val emailVerified: Boolean? = null,
    val authProvider: AuthProvider? = null,
) {
    /** Email/password accounts own a password; social accounts don't. Missing → treat as email. */
    val canChangePassword: Boolean
        get() = authProvider == null || authProvider == AuthProvider.EMAIL
}

package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/** Mirrors iOS `Entitlement`. */
@Serializable
data class Entitlement(
    val isActive: Boolean,
    val inTrial: Boolean,
    /** `"apple"` or `"google"` — left as a raw String to preserve forward-compat with new providers. */
    val provider: String? = null,
    val productId: String? = null,
    @Serializable(with = InstantSerializer::class)
    val expiresAt: Instant? = null,
    val autoRenew: Boolean,
) {
    companion object {
        val Inactive = Entitlement(
            isActive = false,
            inTrial = false,
            provider = null,
            productId = null,
            expiresAt = null,
            autoRenew = false,
        )
    }
}

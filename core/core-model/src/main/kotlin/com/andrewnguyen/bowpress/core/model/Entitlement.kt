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

        /**
         * Fixture used by `PlayBillingManager` in DEBUG builds so the
         * `ReadOnlyGate` upgrade banner doesn't overlay the whole app during
         * development. Mirrors iOS `SubscriptionManager.isSubscribed=true`
         * DEBUG shortcut (overridable with `REAL_ENTITLEMENT=1` env var or
         * system property). Never returned in release builds — release uses
         * [ActiveFree] (no real purchase) or [Inactive] depending on
         * [FeatureFlags.MONETIZATION_ENABLED]. The hardcoded `productId =
         * com.andrewnguyen.bowpress.monthly` here is for rendering the
         * subscriber paywall path in dev; don't treat that string as a
         * "this is a dev build" probe — it isn't unique.
         */
        val ActiveDevDebug = Entitlement(
            isActive = true,
            inTrial = false,
            provider = "google",
            productId = "com.andrewnguyen.bowpress.monthly",
            expiresAt = null,
            autoRenew = true,
        )

        /**
         * Active entitlement fixture used when [FeatureFlags.MONETIZATION_ENABLED]
         * is false (app is free for all users). Distinct from [ActiveDevDebug]
         * because it ships in release builds and intentionally carries no
         * provider/product — there is no real purchase backing it.
         */
        val ActiveFree = Entitlement(
            isActive = true,
            inTrial = false,
            provider = null,
            productId = null,
            expiresAt = null,
            autoRenew = false,
        )
    }
}

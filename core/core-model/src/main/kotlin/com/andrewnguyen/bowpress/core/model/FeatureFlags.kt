package com.andrewnguyen.bowpress.core.model

/**
 * Top-level compile-time feature flags. Mirrors `FeatureFlags.swift` on iOS.
 *
 * [MONETIZATION_ENABLED] is the master switch for paywall + entitlement
 * gating. While `false` the app is free for all users: [Entitlement] is
 * seeded Active in [com.andrewnguyen.bowpress.feature.subscription.PlayBillingManager],
 * the upgrade banner stays hidden because nothing is read-only, and the
 * Settings / You "Subscription" rows are removed from the UI. The Play
 * Billing client, `PaywallScreen`, and `SubscriptionVerifier` code stays
 * linked so premium features can be re-gated by flipping this back to `true`
 * (and gating new features on a finer-grained check than blanket
 * `entitlement.isActive`).
 */
object FeatureFlags {
    const val MONETIZATION_ENABLED: Boolean = false
}

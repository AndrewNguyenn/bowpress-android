package com.andrewnguyen.bowpress.feature.subscription

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink

/**
 * Routes owned by `feature-subscription`.
 *
 * `subscription/paywall` — full-screen paywall. Also reachable via deep link
 * `bowpress://paywall` (mirrors iOS `bowpress://paywall` URL scheme).
 */
object SubscriptionRoutes {
    const val PAYWALL = "subscription/paywall"
    const val DEEP_LINK_PAYWALL = "bowpress://paywall"
}

/**
 * Install the subscription graph into the root [NavGraphBuilder].
 *
 * App integration (task #8) calls this from `BowPressNavHost`:
 * ```
 * subscriptionNavGraph(navController)
 * ```
 */
fun NavGraphBuilder.subscriptionNavGraph(navController: NavController) {
    composable(
        route = SubscriptionRoutes.PAYWALL,
        deepLinks = listOf(navDeepLink { uriPattern = SubscriptionRoutes.DEEP_LINK_PAYWALL }),
    ) {
        PaywallScreen(
            onClose = { navController.popBackStack() },
            onPurchaseComplete = { navController.popBackStack() },
        )
    }
}

/**
 * Convenience helper to navigate to the paywall from a feature module without
 * depending on [SubscriptionRoutes] directly.
 */
fun NavController.navigateToPaywall() {
    navigate(SubscriptionRoutes.PAYWALL)
}

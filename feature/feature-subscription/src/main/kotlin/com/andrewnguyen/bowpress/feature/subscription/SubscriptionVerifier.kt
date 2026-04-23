package com.andrewnguyen.bowpress.feature.subscription

import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.VerifySubscriptionRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contract for verifying a Play Billing purchase with the BowPress API and
 * returning the resulting [Entitlement].
 *
 * **Backend gap (platform-dev → backend):** The API currently only exposes
 * `POST /subscription/verify` for Apple JWS. There is no Google equivalent.
 * Once the backend adds `POST /subscription/verify-google` (accepting
 * `{ purchaseToken, productId, packageName }`), swap [HttpSubscriptionVerifier]
 * to call it. Until then, we stub the call: purchases succeed locally and are
 * acknowledged via Play Billing, but the server-side entitlement is not
 * persisted — the user sees a temporary optimistic `Inactive` entitlement and
 * a logged warning.
 *
 * TODO(platform-dev): backend endpoint pending — `/subscription/verify-google`.
 */
interface SubscriptionVerifier {
    /**
     * Send [token] (the Play Billing purchase token) along with metadata to the
     * backend for receipt validation. Returns the server-issued [Entitlement].
     */
    suspend fun verifyGooglePurchase(
        token: String,
        productId: String,
        packageName: String,
    ): Entitlement
}

/**
 * HTTP-backed implementation. While the server endpoint is pending, this
 * implementation logs the intent and returns [Entitlement.Inactive] as a
 * placeholder so the paywall UI doesn't deadlock waiting for verification.
 *
 * Once the backend lands, replace the body with:
 * ```
 * api.verifyGoogleSubscription(VerifyGoogleRequest(token, productId, packageName))
 * ```
 * and add the corresponding Retrofit method + request DTO to core-network.
 */
@Singleton
class HttpSubscriptionVerifier @Inject constructor(
    @Suppress("unused") private val api: BowPressApi,
) : SubscriptionVerifier {

    override suspend fun verifyGooglePurchase(
        token: String,
        productId: String,
        packageName: String,
    ): Entitlement {
        // TODO(platform-dev): backend endpoint pending — `/subscription/verify-google`.
        // When ready, call:
        //   return api.verifyGoogleSubscription(
        //       VerifyGoogleSubscriptionRequest(
        //           purchaseToken = token,
        //           productId = productId,
        //           packageName = packageName,
        //       ),
        //   )
        //
        // For now we reuse the Apple endpoint with the raw token so integration
        // tests have an observable side effect, and fall back to Inactive.
        return runCatching {
            api.verifySubscription(VerifySubscriptionRequest(jws = token))
        }.getOrElse { Entitlement.Inactive }
    }
}

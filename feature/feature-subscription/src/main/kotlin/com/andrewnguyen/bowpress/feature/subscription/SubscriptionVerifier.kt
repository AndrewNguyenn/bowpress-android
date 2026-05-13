package com.andrewnguyen.bowpress.feature.subscription

import android.util.Log
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.VerifyGoogleSubscriptionRequest
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contract for verifying a Play Billing purchase with the BowPress API and
 * returning the resulting [Entitlement]. Mirrors the iOS Apple JWS verify
 * path via the Apple-specific `POST /subscription/verify`.
 *
 * The Android counterpart hits `POST /subscription/verify-google` with
 * `{ purchaseToken, productId, packageName }`. The endpoint currently returns
 * 501 in production because the Play Console developer account hasn't been
 * provisioned yet (see BLOCKERS.md #3). The client still POSTs so the moment
 * the backend goes live — no client release is required.
 */
interface SubscriptionVerifier {
    /**
     * Send [token] (the Play Billing purchase token) along with metadata to the
     * backend for receipt validation. Returns the server-issued [Entitlement],
     * or [Entitlement.Inactive] if the server can't validate (e.g. still 501).
     */
    suspend fun verifyGooglePurchase(
        token: String,
        productId: String,
        packageName: String,
    ): Entitlement
}

/**
 * HTTP-backed implementation. Posts to `/subscription/verify-google`. If the
 * server returns an error (network, 501 while the endpoint is unprovisioned,
 * 4xx for malformed payloads), the caller sees `Entitlement.Inactive` and a
 * logged warning — the local Play Billing ack/acknowledge flow still
 * completes, so the user's purchase isn't lost; subsequent app launches will
 * re-fetch via `GET /subscription` once the server-side flow exists.
 */
@Singleton
class HttpSubscriptionVerifier @Inject constructor(
    private val api: BowPressApi,
) : SubscriptionVerifier {

    override suspend fun verifyGooglePurchase(
        token: String,
        productId: String,
        packageName: String,
    ): Entitlement = runCatching {
        api.verifyGoogleSubscription(
            VerifyGoogleSubscriptionRequest(
                purchaseToken = token,
                productId = productId,
                packageName = packageName,
            ),
        )
    }.onFailure {
        // Never swallow CancellationException — that breaks structured
        // cancellation when the caller scope (paywall VM) is cancelled
        // mid-flight, which would otherwise flip an active entitlement
        // to Inactive on a background backgrounding.
        if (it is CancellationException) throw it
        Log.w(TAG, "verify-google failed; client will retry on next launch", it)
    }.getOrElse { Entitlement.Inactive }

    private companion object {
        const val TAG = "SubscriptionVerifier"
    }
}

package com.andrewnguyen.bowpress.feature.subscription

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import android.util.Log
import com.andrewnguyen.bowpress.core.model.Entitlement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps `com.android.billingclient:billing-ktx:7.1.1`. Mirrors iOS
 * `SubscriptionManager` — connects a single long-lived [BillingClient], queries
 * the two subscription products, runs a purchase flow, acknowledges the
 * resulting purchase, and POSTs the token to the backend via
 * [SubscriptionVerifier].
 *
 * Product IDs (configured in Play Console) must match iOS App Store IDs:
 * - [BowPressProducts.MONTHLY] — `com.andrewnguyen.bowpress.monthly`
 * - [BowPressProducts.ANNUAL]  — `com.andrewnguyen.bowpress.annual`
 *
 * The listener-driven purchase flow (`onPurchasesUpdated`) can't suspend, so
 * we finish the purchase (ack + verify) on an internal [IO][Dispatchers.IO]
 * scope and resolve a [CompletableDeferred] to hand the outcome back to the
 * caller that invoked [launchPurchaseFlow].
 */
object BowPressProducts {
    const val MONTHLY = "com.andrewnguyen.bowpress.monthly"
    const val ANNUAL = "com.andrewnguyen.bowpress.annual"
    val All: List<String> = listOf(MONTHLY, ANNUAL)
}

@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verifier: SubscriptionVerifier,
) : PurchasesUpdatedListener {

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    /**
     * Initial entitlement: `Active` in DEBUG (so ReadOnlyGate doesn't overlay
     * the whole app during dev) unless `REAL_ENTITLEMENT=1` system property is
     * set. Production builds always start `Inactive` and are updated by the
     * Play Billing connect → verifier round-trip. Mirrors iOS
     * `SubscriptionManager` DEBUG shortcut.
     */
    private val _entitlement = MutableStateFlow(
        if (isDebugBuild() && !isRealEntitlementRequested()) {
            Entitlement.ActiveDevDebug
        } else {
            Entitlement.Inactive
        },
    )
    val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    private fun isDebugBuild(): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun isRealEntitlementRequested(): Boolean =
        System.getProperty("REAL_ENTITLEMENT") == "1" ||
            System.getenv("REAL_ENTITLEMENT") == "1"

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Long-running scope used for listener-driven ack/verify work. Includes
     * a CoroutineExceptionHandler backstop so a network failure inside
     * onPurchasesUpdated's launched coroutine can't crash the process —
     * the pending CompletableDeferred is resolved with an Error outcome
     * instead so the paywall UI surfaces the failure cleanly. Reads
     * pendingPurchase via the same atomic read-then-null dance as the
     * Billing listener so writes from the caller thread, the listener,
     * and this handler don't lose each other.
     */
    private val scope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.IO +
            CoroutineExceptionHandler { _, t ->
                Log.w("PlayBilling", "Uncaught error in billing scope", t)
                val p = pendingPurchase
                pendingPurchase = null
                p?.complete(PurchaseOutcome.Error(t.message))
            },
    )

    /**
     * `CompletableDeferred` resolved by [onPurchasesUpdated] so the VM can
     * `await()` the purchase flow result. Reset each time [launchPurchaseFlow]
     * fires.
     */
    // @Volatile because three threads write here: caller (launchPurchaseFlow),
    // Play Billing listener (onPurchasesUpdated), and the scope's exception
    // handler. Play Billing serialises its own callbacks so a Mutex would be
    // overkill, but the JVM needs the explicit happens-before to forbid a
    // stale read from a different thread's cache.
    @Volatile
    private var pendingPurchase: CompletableDeferred<PurchaseOutcome>? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /**
     * Idempotent connect. Safe to call on every paywall entry — if already
     * connected, returns immediately.
     */
    suspend fun connect(): Boolean {
        if (billingClient.isReady) return true
        val deferred = CompletableDeferred<Boolean>()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // Don't auto-reconnect here — the next call to connect() will.
            }

            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    deferred.complete(true)
                } else {
                    _lastError.value = result.debugMessage
                    deferred.complete(false)
                }
            }
        })
        return deferred.await()
    }

    /** Query the two subscription products from Play. */
    suspend fun queryProducts(): List<ProductDetails> {
        if (!connect()) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BowPressProducts.All.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                },
            )
            .build()

        val deferred = CompletableDeferred<List<ProductDetails>>()
        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                deferred.complete(details)
            } else {
                _lastError.value = result.debugMessage
                deferred.complete(emptyList())
            }
        }
        val list = deferred.await().sortedBy { pd ->
            pd.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.priceAmountMicros ?: Long.MAX_VALUE
        }
        _products.value = list
        return list
    }

    /**
     * Launch the Play Billing purchase UI. Must be called from an [Activity]
     * context — not an application context. Suspends until Play returns a
     * result; the entitlement is updated (and [PurchaseOutcome] returned) after
     * the backend verification round-trip.
     */
    suspend fun launchPurchaseFlow(
        activity: Activity,
        product: ProductDetails,
    ): PurchaseOutcome {
        if (!connect()) return PurchaseOutcome.Error("Billing service not ready")
        val offerToken = product.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return PurchaseOutcome.Error("No subscription offer available")

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()

        val deferred = CompletableDeferred<PurchaseOutcome>()
        pendingPurchase = deferred

        val launchResult = billingClient.launchBillingFlow(activity, params)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return PurchaseOutcome.Error(launchResult.debugMessage)
        }
        return deferred.await()
    }

    /**
     * Restore purchases — queries `currentEntitlements` equivalent and posts
     * any active subscription to the backend. Mirrors iOS `restorePurchases`.
     */
    suspend fun restorePurchases(): Entitlement {
        if (!connect()) return Entitlement.Inactive
        val deferred = CompletableDeferred<List<Purchase>>()
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { _, purchases -> deferred.complete(purchases) }

        val purchases = deferred.await()
        var latest: Entitlement = Entitlement.Inactive
        for (p in purchases) {
            if (p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            val productId = p.products.firstOrNull() ?: continue
            val entitlement = verifier.verifyGooglePurchase(
                token = p.purchaseToken,
                productId = productId,
                packageName = context.packageName,
            )
            if (entitlement.isActive) latest = entitlement
            acknowledgeIfNeeded(p)
        }
        _entitlement.value = latest
        return latest
    }

    // region PurchasesUpdatedListener

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val deferred = pendingPurchase
        pendingPurchase = null
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val first = purchases?.firstOrNull()
                if (first == null) {
                    deferred?.complete(PurchaseOutcome.Error("No purchase returned"))
                    return
                }
                // Ack + verify runs on a background coroutine — can't suspend
                // inside the listener callback itself.
                scope.launch {
                    acknowledgeIfNeeded(first)
                    val productId = first.products.firstOrNull() ?: ""
                    val ent = verifier.verifyGooglePurchase(
                        token = first.purchaseToken,
                        productId = productId,
                        packageName = context.packageName,
                    )
                    _entitlement.value = ent
                    deferred?.complete(PurchaseOutcome.Success(ent))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                deferred?.complete(PurchaseOutcome.Cancelled)
            }
            else -> {
                _lastError.value = result.debugMessage
                deferred?.complete(PurchaseOutcome.Error(result.debugMessage))
            }
        }
    }

    // endregion

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val deferred = CompletableDeferred<Unit>()
        billingClient.acknowledgePurchase(params) { _ -> deferred.complete(Unit) }
        deferred.await()
    }

    fun disconnect() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}

/** Outcome of a single purchase-flow attempt. */
sealed class PurchaseOutcome {
    data class Success(val entitlement: Entitlement) : PurchaseOutcome()
    data object Cancelled : PurchaseOutcome()
    data class Error(val message: String?) : PurchaseOutcome()
}

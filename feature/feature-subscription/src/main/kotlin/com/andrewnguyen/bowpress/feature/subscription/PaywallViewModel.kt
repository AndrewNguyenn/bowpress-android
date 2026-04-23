package com.andrewnguyen.bowpress.feature.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.andrewnguyen.bowpress.core.model.Entitlement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the paywall. Mirrors the iOS `SubscriptionManager` observable —
 * products list, entitlement, in-flight flag, and last error.
 */
data class PaywallUiState(
    val products: List<ProductDetails> = emptyList(),
    val entitlement: Entitlement = Entitlement.Inactive,
    val purchaseInFlight: Boolean = false,
    val loading: Boolean = true,
    val lastError: String? = null,
    val purchaseSucceeded: Boolean = false,
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billing: PlayBillingManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        _state.value = _state.value.copy(loading = true, lastError = null)
        viewModelScope.launch {
            val products = billing.queryProducts()
            _state.value = _state.value.copy(
                products = products,
                loading = false,
                lastError = if (products.isEmpty()) "Plans are unavailable right now. Try again shortly." else null,
            )
        }
    }

    /** Called by the Composable with the hosting Activity (required by Play Billing). */
    fun purchase(activity: Activity, product: ProductDetails) {
        if (_state.value.purchaseInFlight) return
        _state.value = _state.value.copy(purchaseInFlight = true, lastError = null)
        viewModelScope.launch {
            when (val outcome = billing.launchPurchaseFlow(activity, product)) {
                is PurchaseOutcome.Success -> {
                    _state.value = _state.value.copy(
                        purchaseInFlight = false,
                        entitlement = outcome.entitlement,
                        purchaseSucceeded = outcome.entitlement.isActive,
                    )
                }
                is PurchaseOutcome.Cancelled -> {
                    _state.value = _state.value.copy(purchaseInFlight = false)
                }
                is PurchaseOutcome.Error -> {
                    _state.value = _state.value.copy(
                        purchaseInFlight = false,
                        lastError = outcome.message ?: "Purchase failed",
                    )
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            val ent = billing.restorePurchases()
            _state.value = _state.value.copy(entitlement = ent)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(lastError = null)
    }
}

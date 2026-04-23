package com.andrewnguyen.bowpress.feature.subscription

import app.cash.turbine.test
import com.android.billingclient.api.ProductDetails
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies the purchase-flow state transitions. The [PlayBillingManager] is
 * mocked so these cases never hit the real Google Play Billing client — we
 * only care that the VM reacts to each [PurchaseOutcome].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates products and clears loading`() = runTest(dispatcher) {
        val monthly = mockProduct(BowPressProducts.MONTHLY)
        val annual = mockProduct(BowPressProducts.ANNUAL)
        val billing = mockk<PlayBillingManager>(relaxed = true).also {
            coEvery { it.queryProducts() } returns listOf(monthly, annual)
        }

        val vm = PaywallViewModel(billing)

        vm.state.test {
            // Skip initial emission(s) until products settle.
            val loaded = awaitStateMatching { !it.loading }
            assertThat(loaded.products).hasSize(2)
            assertThat(loaded.lastError).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `purchase success sets purchaseSucceeded and clears inFlight`() = runTest(dispatcher) {
        val product = mockProduct(BowPressProducts.MONTHLY)
        val activeEntitlement = Entitlement.Inactive.copy(isActive = true, provider = "google")
        val billing = mockk<PlayBillingManager>(relaxed = true).also {
            coEvery { it.queryProducts() } returns listOf(product)
            coEvery { it.launchPurchaseFlow(any(), any()) } returns
                PurchaseOutcome.Success(activeEntitlement)
        }
        val vm = PaywallViewModel(billing)
        // Wait for initial products to land.
        vm.state.test {
            awaitStateMatching { !it.loading }
            cancelAndIgnoreRemainingEvents()
        }

        val activity = mockk<android.app.Activity>(relaxed = true)
        vm.purchase(activity, product)

        vm.state.test {
            val final = awaitStateMatching { !it.purchaseInFlight && it.entitlement.isActive }
            assertThat(final.purchaseSucceeded).isTrue()
            assertThat(final.lastError).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `purchase cancelled clears inFlight without error`() = runTest(dispatcher) {
        val product = mockProduct(BowPressProducts.ANNUAL)
        val billing = mockk<PlayBillingManager>(relaxed = true).also {
            coEvery { it.queryProducts() } returns listOf(product)
            coEvery { it.launchPurchaseFlow(any(), any()) } returns PurchaseOutcome.Cancelled
        }
        val vm = PaywallViewModel(billing)
        vm.state.test {
            awaitStateMatching { !it.loading }
            cancelAndIgnoreRemainingEvents()
        }

        val activity = mockk<android.app.Activity>(relaxed = true)
        vm.purchase(activity, product)

        vm.state.test {
            val final = awaitStateMatching { !it.purchaseInFlight }
            assertThat(final.purchaseSucceeded).isFalse()
            assertThat(final.lastError).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `purchase error surfaces message`() = runTest(dispatcher) {
        val product = mockProduct(BowPressProducts.MONTHLY)
        val billing = mockk<PlayBillingManager>(relaxed = true).also {
            coEvery { it.queryProducts() } returns listOf(product)
            coEvery { it.launchPurchaseFlow(any(), any()) } returns
                PurchaseOutcome.Error("Billing unavailable")
        }
        val vm = PaywallViewModel(billing)
        vm.state.test {
            awaitStateMatching { !it.loading }
            cancelAndIgnoreRemainingEvents()
        }

        val activity = mockk<android.app.Activity>(relaxed = true)
        vm.purchase(activity, product)

        vm.state.test {
            val final = awaitStateMatching { !it.purchaseInFlight }
            assertThat(final.lastError).isEqualTo("Billing unavailable")
            assertThat(final.purchaseSucceeded).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- Helpers ----------------------------------------------------------

    /**
     * Minimal stub that satisfies [ProductDetails] for state tests — we don't
     * assert on any of its properties here, so `relaxed = true` is enough.
     */
    private fun mockProduct(id: String): ProductDetails = mockk(relaxed = true) {
        every { productId } returns id
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<PaywallUiState>.awaitStateMatching(
        predicate: (PaywallUiState) -> Boolean,
    ): PaywallUiState {
        while (true) {
            val s = awaitItem()
            if (predicate(s)) return s
        }
    }
}

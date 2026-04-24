package com.andrewnguyen.bowpress.feature.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPBigScore
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEditLink
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

private const val PLAY_REDEEM_URL = "https://play.google.com/redeem"
private const val TERMS_URL = "https://andrewnguyenn.github.io/bowpress-web/terms.html"
private const val PRIVACY_URL = "https://andrewnguyenn.github.io/bowpress-web/privacy.html"

private val FEATURES = listOf(
    "Unlimited sessions",
    "Advanced arrow flight analytics",
    "Personalised tuning suggestions",
    "Multi-bow profile management",
    "Export & session history",
)

@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    onPurchaseComplete: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(state.purchaseSucceeded) {
        if (state.purchaseSucceeded) onPurchaseComplete()
    }

    Scaffold(containerColor = AppPaper) { innerPadding ->
        val context = LocalContext.current
        PaywallBody(
            padding = innerPadding,
            state = state,
            onClose = onClose,
            onPurchase = { product ->
                activity?.let { viewModel.purchase(it, product) }
            },
            onRestore = { viewModel.restore() },
            onRetry = { viewModel.loadProducts() },
            onRedeemCode = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_REDEEM_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
            onOpenTerms = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
            onOpenPrivacy = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
        )
    }
}

@Composable
private fun PaywallBody(
    padding: PaddingValues,
    state: PaywallUiState,
    onClose: () -> Unit,
    onPurchase: (ProductDetails) -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onRedeemCode: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    // Track selected tier — default to first product when the list loads
    var selectedProductId by remember(state.products) {
        mutableStateOf(
            state.products.find { it.productId == BowPressProducts.ANNUAL }?.productId
                ?: state.products.firstOrNull()?.productId,
        )
    }
    val selectedProduct = state.products.find { it.productId == selectedProductId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(AppPaper),
    ) {
        // Header
        BPNavHeader(
            title = "BowPress Pro",
            eyebrow = "BOWPRESS · SUBSCRIPTION",
            meta = { BPEditLink(label = "CLOSE", onClick = onClose) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Intro copy
            Text(
                text = "Unlock the full tuning engine.",
                style = frauncesDisplay(20.sp, italic = true)
                    .copy(color = AppInk),
            )

            // Product tier cards
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AppPond)
                }
                state.products.isEmpty() -> ErrorCard(state.lastError, onRetry)
                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.products.forEach { product ->
                        TierCard(
                            product = product,
                            isSelected = product.productId == selectedProductId,
                            onTap = { selectedProductId = product.productId },
                        )
                    }
                }
            }

            // Feature list
            if (state.products.isNotEmpty()) {
                FeatureList()
            }

            // Secondary actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.testTag("paywall_restore_button"),
                ) {
                    Text(
                        "Restore Purchases",
                        style = interUI(12.sp).copy(color = AppPond),
                    )
                }
                TextButton(
                    onClick = onRedeemCode,
                    modifier = Modifier.testTag("paywall_redeem_button"),
                ) {
                    Text(
                        "Redeem Code",
                        style = interUI(12.sp).copy(color = AppPond),
                    )
                }
            }

            LegalFooter(onOpenTerms = onOpenTerms, onOpenPrivacy = onOpenPrivacy)
        }

        // Primary CTA pinned to bottom
        if (state.products.isNotEmpty()) {
            val offer = selectedProduct?.subscriptionOfferDetails?.firstOrNull()
            val lastPhase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()
            val price = lastPhase?.formattedPrice ?: ""
            val period = lastPhase?.billingPeriod?.let(::periodLabel) ?: "mo"

            BPPrimaryButton(
                title = "Start free trial",
                subtitle = "$price/$period · cancel anytime",
                onClick = { selectedProduct?.let { onPurchase(it) } },
                enabled = !state.purchaseInFlight && selectedProduct != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tier card
// ---------------------------------------------------------------------------

@Composable
private fun TierCard(
    product: ProductDetails,
    isSelected: Boolean,
    onTap: () -> Unit,
) {
    val offer = product.subscriptionOfferDetails?.firstOrNull()
    val phases = offer?.pricingPhases?.pricingPhaseList.orEmpty()
    val paidPhase = phases.lastOrNull()
    val price = paidPhase?.formattedPrice ?: product.name
    val period = paidPhase?.billingPeriod?.let { periodLabelLong(it) } ?: ""
    val hasFreeTrial = phases.any { it.priceAmountMicros == 0L }

    val accId = when (product.productId) {
        BowPressProducts.MONTHLY -> "paywall_monthly_button"
        BowPressProducts.ANNUAL -> "paywall_annual_button"
        else -> "paywall_product_${product.productId}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .border(1.dp, if (isSelected) AppPondDk else AppLine)
            .clickable(onClick = onTap)
            .padding(14.dp)
            .testTag(accId),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = product.name,
                style = frauncesDisplay(17.sp, italic = true)
                    .copy(color = AppInk),
            )
            // Price in BPBigScore style — split on decimal for maple dot
            BPBigScore(value = price, size = 28.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (hasFreeTrial) {
                Text(
                    text = "1-month free trial",
                    style = interUI(11.sp).copy(color = AppPond),
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Text(
                text = period,
                style = interUI(10.sp).copy(color = AppInk3),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Feature list
// ---------------------------------------------------------------------------

@Composable
private fun FeatureList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FEATURES.forEach { feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 5dp AppPond square bullet
                Box(Modifier.size(5.dp).background(AppPond))
                Text(
                    text = feature,
                    style = interUI(12.sp).copy(color = AppInk),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Error + legal
// ---------------------------------------------------------------------------

@Composable
private fun ErrorCard(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .border(1.dp, AppLine)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            message ?: "Plans are unavailable right now.",
            style = interUI(14.sp).copy(color = AppInk),
        )
        BPPrimaryButton(
            title = "Try again",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LegalFooter(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(
            "Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play.",
            style = interUI(11.sp).copy(color = AppInk3),
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            TextButton(
                onClick = onOpenTerms,
                modifier = Modifier.testTag("paywall_terms_button"),
            ) {
                Text("Terms of Use", style = interUI(11.sp).copy(color = AppPond))
            }
            Text("·", style = interUI(11.sp).copy(color = AppInk3))
            TextButton(
                onClick = onOpenPrivacy,
                modifier = Modifier.testTag("paywall_privacy_button"),
            ) {
                Text("Privacy Policy", style = interUI(11.sp).copy(color = AppPond))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Period label helpers
// ---------------------------------------------------------------------------

/** Short label for the BPPrimaryButton subtitle ("mo", "yr", etc.). */
private fun periodLabel(iso8601: String): String = when (iso8601) {
    "P1W" -> "wk"
    "P1M" -> "mo"
    "P3M" -> "3 mo"
    "P6M" -> "6 mo"
    "P1Y" -> "yr"
    else -> iso8601
}

/** Long label for the tier card "per month" / "per year" sub. */
private fun periodLabelLong(iso8601: String): String = when (iso8601) {
    "P1W" -> "per week"
    "P1M" -> "per month"
    "P3M" -> "per 3 months"
    "P6M" -> "per 6 months"
    "P1Y" -> "per year"
    else -> iso8601
}

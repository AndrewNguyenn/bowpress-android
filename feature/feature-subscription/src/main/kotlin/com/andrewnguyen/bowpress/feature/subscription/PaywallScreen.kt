package com.andrewnguyen.bowpress.feature.subscription

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BowPress Pro") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close")
                    }
                },
            )
        },
    ) { innerPadding ->
        PaywallBody(
            padding = innerPadding,
            state = state,
            onPurchase = { product ->
                activity?.let { viewModel.purchase(it, product) }
            },
            onRestore = { viewModel.restore() },
            onRetry = { viewModel.loadProducts() },
        )
    }
}

@Composable
private fun PaywallBody(
    padding: PaddingValues,
    state: PaywallUiState,
    onPurchase: (ProductDetails) -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Hero()

        Spacer(Modifier.height(24.dp))

        when {
            state.loading -> LoadingCard()
            state.products.isEmpty() -> ErrorCard(state.lastError, onRetry)
            else -> ProductList(state, onPurchase)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = onRestore,
            modifier = Modifier.testTag("paywall_restore_button"),
        ) {
            Text("Restore Purchases", color = BowPressColors.Accent)
        }

        Spacer(Modifier.height(24.dp))

        LegalFooter()
    }
}

@Composable
private fun Hero() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = BowPressColors.Accent,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlock the full tuning engine",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlimited sessions, advanced analytics, and personalised tuning suggestions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = BowPressColors.Accent)
            Spacer(Modifier.height(12.dp))
            Text("Loading plans…", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ErrorCard(message: String?, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                message ?: "Plans are unavailable right now.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
private fun ProductList(state: PaywallUiState, onPurchase: (ProductDetails) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        state.products.forEach { product ->
            ProductRow(
                product = product,
                disabled = state.purchaseInFlight,
                onTap = { onPurchase(product) },
            )
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductDetails,
    disabled: Boolean,
    onTap: () -> Unit,
) {
    val offer = product.subscriptionOfferDetails?.firstOrNull()
    val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val price = phase?.formattedPrice ?: product.name
    val period = phase?.billingPeriod?.let(::periodLabel) ?: ""
    val accId = when (product.productId) {
        BowPressProducts.MONTHLY -> "paywall_monthly_button"
        BowPressProducts.ANNUAL -> "paywall_annual_button"
        else -> "paywall_product_${product.productId}"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(accId),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Billed $period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Button(
            enabled = !disabled,
            onClick = onTap,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(if (disabled) "Processing…" else "Subscribe")
        }
    }
}

@Composable
private fun LegalFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(
            "Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
    }
}

/** Map ISO-8601 duration strings from Play Billing to human-readable labels. */
private fun periodLabel(iso8601: String): String = when (iso8601) {
    "P1W" -> "weekly"
    "P1M" -> "monthly"
    "P3M" -> "every 3 months"
    "P6M" -> "every 6 months"
    "P1Y" -> "yearly"
    else -> iso8601
}

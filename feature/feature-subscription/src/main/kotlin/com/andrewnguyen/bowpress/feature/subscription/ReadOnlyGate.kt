package com.andrewnguyen.bowpress.feature.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import kotlinx.coroutines.launch

/**
 * Mirrors iOS `EnvironmentValues.isReadOnly`. Child screens can read this to
 * disable destructive actions (add bow, start session, edit equipment, etc.).
 *
 * Defaults to `false` so composables outside a [ReadOnlyGate] behave normally.
 */
val LocalIsReadOnly = compositionLocalOf { false }

/**
 * Wraps [content] with a persistent "Read-only mode" banner at the top when
 * [isReadOnly] is true, and exposes [LocalIsReadOnly] so descendants can react
 * to the gated state. Tapping the banner opens a [PaywallScreen] in a modal
 * bottom sheet — the Android equivalent of the iOS `.sheet(isPresented:)`
 * paywall overlay.
 *
 * Parity with iOS `ReadOnlyGateModifier` in
 * `bowpress-ios/Sources/BowPress/Subscriptions/ReadOnlyGate.swift`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadOnlyGate(
    isReadOnly: Boolean,
    content: @Composable () -> Unit,
) {
    var showPaywall by remember { mutableStateOf(false) }
    // Per-session dismiss — iOS 1025e1e. Once the user taps the X in the
    // banner, hide it for the rest of the session (process lifetime). Reset
    // on app restart so users still see it after re-entering. NOT
    // rememberSaveable on purpose: rotation should NOT bring it back; an
    // explicit relaunch should.
    var bannerDismissed by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalIsReadOnly provides isReadOnly) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isReadOnly && !bannerDismissed) {
                UpgradeBanner(
                    onTap = { showPaywall = true },
                    onDismiss = { bannerDismissed = true },
                )
            }
            content()
        }
    }

    if (showPaywall) {
        ModalBottomSheet(
            onDismissRequest = { showPaywall = false },
            sheetState = sheetState,
        ) {
            PaywallScreen(
                onClose = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { showPaywall = false }
                },
                onPurchaseComplete = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { showPaywall = false }
                },
            )
        }
    }
}

@Composable
private fun UpgradeBanner(onTap: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("upgrade_banner"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                // The banner body tap opens paywall; the X has its own hit
                // target inside the row so a misfire doesn't dismiss the
                // banner unintentionally.
                .clickable(onClick = onTap)
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    "Read-only mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Subscribe to log new sessions and edit equipment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.size(4.dp))
            UpgradePill()
            // Per-session dismiss — iOS 1025e1e. Separate IconButton with its
            // own hit area so banner-tap (paywall) vs X-tap (dismiss) don't
            // misfire into each other.
            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp).testTag("upgrade_banner_dismiss"),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss read-only banner",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun UpgradePill() {
    Text(
        text = "Upgrade",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .background(BowPressColors.Accent)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

package com.andrewnguyen.bowpress.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.bp.BPCard
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEditLink
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.model.UnitSystem
import com.andrewnguyen.bowpress.core.model.User

private const val TERMS_URL = "https://andrewnguyenn.github.io/bowpress-web/terms.html"
private const val PRIVACY_URL = "https://andrewnguyenn.github.io/bowpress-web/privacy.html"

@Composable
fun SettingsScreen(
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onManageSubscription: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    SettingsBody(
        user = state.user,
        entitlement = state.entitlement,
        notificationsEnabled = state.notificationsEnabled,
        onEditProfile = onEditProfile,
        onManageSubscription = onManageSubscription,
        onRestorePurchases = viewModel::restorePurchases,
        onSignOut = viewModel::signOut,
        onNotificationsToggle = viewModel::setNotificationsEnabled,
    )
}

@Composable
private fun SettingsBody(
    user: User?,
    entitlement: Entitlement,
    notificationsEnabled: Boolean,
    onEditProfile: () -> Unit,
    onManageSubscription: () -> Unit,
    onRestorePurchases: () -> Unit,
    onSignOut: () -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val unitSystem = LocalUnitSystem.current
    val setUnitSystem = LocalUnitSystemSetter.current

    val planLabel = when {
        !entitlement.isActive -> "FREE"
        entitlement.productId?.contains("annual") == true -> "PRO ANNUAL"
        entitlement.productId?.contains("monthly") == true -> "PRO MONTHLY"
        else -> "BOWPRESS PRO"
    }
    val subscriptionCtaLabel =
        if (entitlement.isActive) "View subscription plans" else "Upgrade to Pro"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        BPNavHeader(eyebrow = "BOWPRESS · ACCOUNT", title = "Settings")

        // Profile block
        ProfileBlock(user = user, onEdit = onEditProfile)

        Spacer(Modifier.height(20.dp))

        // Group 1: Subscription + Notifications + Units
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingsValueRow(
                    label = "Subscription",
                    value = planLabel,
                    onClick = onManageSubscription,
                    testTag = "settings_subscription",
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                NotificationsRow(
                    enabled = notificationsEnabled,
                    onToggle = onNotificationsToggle,
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                UnitsRow(
                    unitSystem = unitSystem,
                    onToggle = { metric ->
                        setUnitSystem(if (metric) UnitSystem.METRIC else UnitSystem.IMPERIAL)
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Group 2: Privacy + Terms + Sign out
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                SettingsLinkRow(
                    label = "Privacy policy",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsLinkRow(
                    label = "Terms of service",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsLinkRow(
                    label = "Sign out",
                    labelColor = AppMaple,
                    onClick = onSignOut,
                    testTag = "settings_sign_out",
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Group 3: Subscription / restore — matches iOS subscriptionAccessRow
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                SubscriptionAccessRow(
                    title = subscriptionCtaLabel,
                    onClick = onManageSubscription,
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsLinkRow(
                    label = "Restore purchases",
                    onClick = onRestorePurchases,
                    testTag = "settings_restore_purchases",
                )
            }
        }

        // Colophon
        Colophon()
    }
}

// ---------------------------------------------------------------------------
// Profile block
// ---------------------------------------------------------------------------

@Composable
private fun ProfileBlock(user: User?, onEdit: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Square avatar — no corner radius
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(AppPondDk),
                contentAlignment = Alignment.Center,
            ) {
                val initials = initials(user?.name.orEmpty())
                Text(
                    text = initials,
                    style = frauncesDisplay(22.sp, italic = true).copy(color = AppPaper),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = user?.name ?: "—",
                    style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = AppInk),
                )
                Text(
                    text = (user?.email ?: "Not signed in").uppercase(),
                    style = jetbrainsMono(10.sp).copy(
                        color = AppInk3,
                        letterSpacing = 0.04.em,
                    ),
                    maxLines = 1,
                )
            }

            if (user != null) {
                BPEditLink(label = "EDIT", onClick = onEdit)
            }
        }

        // 1dp hairline below profile block
        HorizontalDivider(
            modifier = Modifier.padding(bottom = 18.dp),
            color = AppLine,
            thickness = 1.dp,
        )
    }
}

private fun initials(name: String): String {
    val words = name.trim().split("\\s+".toRegex()).filter(String::isNotBlank)
    return words.take(2).map { it.first().uppercaseChar() }.joinToString("").ifEmpty { "?" }
}

// ---------------------------------------------------------------------------
// Group rows
// ---------------------------------------------------------------------------

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = interUI(14.sp).copy(color = AppInk),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = jetbrainsMono(10.sp).copy(color = AppInk3, letterSpacing = 0.04.em),
            )
            Text(
                text = "›",
                style = frauncesDisplay(16.sp, italic = true).copy(color = AppPond),
            )
        }
    }
}

@Composable
private fun NotificationsRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("settings_notifications_toggle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Push notifications",
            style = interUI(14.sp).copy(color = AppInk),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (enabled) "ON" else "OFF",
                style = jetbrainsMono(10.sp).copy(color = AppInk3, letterSpacing = 0.04.em),
            )
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun UnitsRow(
    unitSystem: UnitSystem,
    onToggle: (Boolean) -> Unit,
) {
    val metric = unitSystem == UnitSystem.METRIC
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!metric) }
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("settings_units_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Units",
            style = interUI(14.sp).copy(color = AppInk),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = unitSystem.label.uppercase(),
                style = jetbrainsMono(10.sp).copy(color = AppInk3, letterSpacing = 0.04.em),
            )
            Text(
                text = "›",
                style = frauncesDisplay(16.sp, italic = true).copy(color = AppPond),
            )
        }
    }
}

@Composable
private fun SubscriptionAccessRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("settings_subscription_cta"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = interUI(14.sp).copy(color = AppInk),
            )
            Text(
                text = "unlock the full tuning engine",
                style = jetbrainsMono(10.sp).copy(color = AppInk3, letterSpacing = 0.04.em),
            )
        }
        Text(
            text = "›",
            style = frauncesDisplay(16.sp, italic = true).copy(color = AppPond),
        )
    }
}

@Composable
private fun SettingsLinkRow(
    label: String,
    onClick: () -> Unit,
    labelColor: Color = AppInk,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = interUI(14.sp).copy(color = labelColor),
        )
        Text(
            text = "›",
            style = frauncesDisplay(16.sp, italic = true).copy(color = AppPond),
        )
    }
}

// ---------------------------------------------------------------------------
// Colophon
// ---------------------------------------------------------------------------

@Composable
private fun Colophon() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "est. arch",
            style = frauncesDisplay(11.sp, italic = true).copy(
                color = AppInk3,
                letterSpacing = 0.08.em,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(5.dp).background(AppPond))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "kanazawa",
            style = frauncesDisplay(11.sp, italic = true).copy(
                color = AppInk3,
                letterSpacing = 0.08.em,
            ),
        )
    }
}

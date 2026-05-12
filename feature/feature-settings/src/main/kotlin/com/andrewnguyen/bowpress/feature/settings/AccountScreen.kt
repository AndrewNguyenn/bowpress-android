package com.andrewnguyen.bowpress.feature.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.bp.BPCard
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    var showSignOutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        AccountHeader(onBack = onBack)

        Spacer(Modifier.height(8.dp))

        // Identity: Name + Email (read-only)
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                IdentityRow(label = "Name", value = state.user?.name ?: "—")
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                IdentityRow(label = "Email", value = state.user?.email ?: "—")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Actions: Edit Profile + Delete Account
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                AccountActionRow(
                    label = "Edit Profile",
                    onClick = onEditProfile,
                    testTag = "account_edit_profile",
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                AccountActionRow(
                    label = "Delete Account",
                    labelColor = AppMaple,
                    onClick = onDeleteAccount,
                    testTag = "account_delete_account",
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sign Out
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutConfirm = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .testTag("account_sign_out"),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sign Out",
                        style = interUI(14.sp).copy(color = AppMaple),
                    )
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign out of BowPress?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.signOut()
                    },
                ) { Text("Sign Out", color = AppMaple) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AccountHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppInk)
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Account",
                style = frauncesDisplay(20.sp, italic = true).copy(color = AppInk),
                modifier = Modifier.padding(end = 48.dp), // offset for back-arrow column
            )
        }
    }
}

@Composable
private fun IdentityRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = interUI(14.sp).copy(color = AppInk))
        Text(
            text = value,
            style = jetbrainsMono(10.sp).copy(color = AppInk3, letterSpacing = 0.04.em),
        )
    }
}

@Composable
private fun AccountActionRow(
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
        Text(text = label, style = interUI(14.sp).copy(color = labelColor))
        Text(
            text = "›",
            style = frauncesDisplay(16.sp, italic = true).copy(color = AppPond),
        )
    }
}

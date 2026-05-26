package com.andrewnguyen.bowpress.feature.social.ui.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

/**
 * Parity E7 — value-prop "soft prompt" sheet shown once per device the
 * first time the app has a chance to ask for location (iOS commit 32b5a31,
 * `LocationSoftPromptSheet.swift`, 173 LOC).
 *
 * Previews a feed-card header tagged with a range so the archer can see
 * what enabling location buys them, then fires the system permission
 * dialog on "Use my location". A "Not now" path simply dismisses; either
 * way the caller should flip `LocationPreferencesRepository.markSoftPromptShown`
 * so the sheet never re-appears.
 *
 * @param archerName drives the preview card's actor label. Empty → "You".
 * @param onAllow caller-supplied action that fires the system permission
 *   request. This sheet doesn't reach into ActivityResultContracts itself
 *   because the launcher must be remembered on the call site that has
 *   permission to host it. The sheet supplies a default below that creates
 *   the launcher inline using [rememberLauncherForActivityResult].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSoftPromptSheet(
    archerName: String,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppPaper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header — pin glyph + Fraunces display title + value-prop blurb.
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = AppPondDk,
                modifier = Modifier.size(28.dp),
            )
            Text(
                "Show where you shot",
                style = frauncesDisplay(24.sp, italic = true),
                color = AppInk,
            )
            Text(
                "Tag your sessions with the archery range you're at — automatically. " +
                    "Your friends see the spot at the top of every post on the feed.",
                style = frauncesDisplay(14.sp, italic = true),
                color = AppInk2,
            )

            Spacer(Modifier.height(12.dp))

            // Preview block — a faux feed-card header so the archer can see
            // what enabling location buys them.
            Text(
                "PREVIEW",
                style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
            PreviewCardHeader(archerName = archerName)

            Spacer(Modifier.height(8.dp))

            // CTA stack
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppPondDk)
                    .clickable(onClick = onAllow)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "USE MY LOCATION",
                    style = interUI(13.sp, FontWeight.SemiBold)
                        .copy(letterSpacing = 0.12.em, color = AppPaper),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "NOT NOW",
                    style = interUI(11.sp, FontWeight.SemiBold)
                        .copy(letterSpacing = 0.16.em, color = AppInk3),
                )
            }
        }
    }
}

/**
 * Convenience overload — creates the system-permission launcher itself so a
 * call site only has to wire `archerName` + dismiss. Fires
 * `ACCESS_FINE_LOCATION` on Allow. The launcher callback is the only path
 * that flips `onResolved()` on the allow side so we don't dismiss the
 * sheet (and re-arm the prefs flag race) before the OS dialog returns.
 */
@Composable
fun LocationSoftPromptSheet(
    archerName: String,
    onResolved: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> onResolved() }
    LocationSoftPromptSheet(
        archerName = archerName,
        onAllow = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
        onDismiss = onResolved,
    )
}

@Composable
private fun PreviewCardHeader(archerName: String) {
    val resolved = if (archerName.isBlank()) "You" else archerName
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .border(1.dp, AppLine)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "ORANGE COUNTY ARCHERY",
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppPondDk,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SocialAvatar(initials = avatarInitials(resolved), size = 30)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.padding(end = 8.dp)) {
                Text(
                    resolved,
                    style = frauncesDisplay(14.sp, italic = true),
                    color = AppInk,
                )
                Text(
                    "scored 287 / 300 · now",
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
            }
        }
    }
}

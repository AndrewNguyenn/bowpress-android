package com.andrewnguyen.bowpress.feature.social.ui.you

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.FeatureFlags
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementsViewModel
import com.andrewnguyen.bowpress.feature.social.ui.achievements.TrophyCaseSection
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

@Composable
fun YouScreen(
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSignOut: () -> Unit,
    onAccountClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onEquipmentClick: () -> Unit,
    viewModel: YouViewModel = hiltViewModel(),
    achievementsViewModel: AchievementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val trophyState by achievementsViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { achievementsViewModel.loadMine() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialYouRoot),
    ) {
        // Top nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "‹  Back",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("You", style = frauncesDisplay(28.sp), color = AppInk)
                Text(
                    "profile · preferences · account",
                    style = jetbrainsMono(10.sp),
                    color = AppInk3,
                )
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            // Profile header
            state.profile?.let { profile ->
                ProfileHeader(profile = profile)
            }

            // Trophy case (§15 / §18)
            Spacer(Modifier.height(18.dp))
            TrophyCaseSection(
                achievements = trophyState.achievements,
                catalog = trophyState.catalog,
                ownerLabel = "Shoot",
            )

            // Personal section
            SectionHeader(title = "Personal", aside = "your data")
            SectionCard {
                SettingsRow(
                    title = "Account",
                    sub = "email · password · delete account",
                    onClick = onAccountClick,
                )
                // Subscription row hidden while the app is free
                // (FeatureFlags.MONETIZATION_ENABLED = false).
                if (FeatureFlags.MONETIZATION_ENABLED) {
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                    SettingsRow(
                        title = "Subscription",
                        sub = "manage your plan",
                        onClick = onSubscriptionClick,
                    )
                }
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsRow(
                    title = "Equipment",
                    sub = "shortcut · bows, arrows, default setup",
                    onClick = onEquipmentClick,
                )
            }

            // Preferences section
            SectionHeader(title = "Preferences", aside = "how the app behaves")
            SectionCard {
                SettingsRow(
                    title = "Privacy & visibility",
                    sub = "who sees your sessions · ${state.profile?.visibility?.name ?: "friends"}",
                    onClick = onPrivacyClick,
                )
            }

            // Support section
            SectionHeader(title = "Support", aside = "if you need us")
            SectionCard {
                SettingsRow("Help & FAQ", "tuning guides · scoring rules · how-to", onClick = {})
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsRow("Send feedback", "we read every message", onClick = {})
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsRow("Terms of service", "updated 2025·11·02", onClick = {})
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsRow("Privacy policy", "updated 2025·11·02", onClick = {})
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                SettingsRow("About BowPress", "v2.4.1 · build 412", onClick = {})
            }

            // Sign out
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppMaple)
                    .background(AppPaper)
                    .clickable(onClick = onSignOut)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Sign out",
                        style = frauncesDisplay(15.sp),
                        color = AppMaple,
                    )
                    Text(
                        "your sessions stay on this device",
                        style = jetbrainsMono(9.5.sp),
                        color = AppInk3,
                    )
                }
                Text("›", style = frauncesDisplay(18.sp), color = AppMaple)
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "est. arch · kanazawa",
                style = frauncesDisplay(11.sp, italic = true),
                color = AppInk3,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileHeader(profile: SocialProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Large avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(1.dp, AppPondDk)
                .background(AppPaper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitials(profile.displayName),
                style = frauncesDisplay(24.sp),
                color = AppPondDk,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(profile.displayName, style = frauncesDisplay(22.sp), color = AppInk)
            Text(
                "@${profile.handle}",
                style = jetbrainsMono(11.sp),
                color = AppInk3,
            )
            Text(
                "${profile.sessionCount} sessions · ${profile.arrowCount} arrows · ${profile.division?.name ?: ""}",
                style = jetbrainsMono(9.5.sp),
                color = AppInk3,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, aside: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = frauncesDisplay(14.sp), color = AppInk, modifier = Modifier.weight(1f))
        Text(
            aside.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = frauncesDisplay(14.sp), color = AppInk)
            Text(sub, style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
        Text("›", style = frauncesDisplay(18.sp), color = AppPond)
    }
}

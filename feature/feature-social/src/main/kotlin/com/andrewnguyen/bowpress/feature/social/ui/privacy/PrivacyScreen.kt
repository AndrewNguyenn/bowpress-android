package com.andrewnguyen.bowpress.feature.social.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.feature.social.ui.you.YouViewModel

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onManageBlocksClick: () -> Unit,
    viewModel: YouViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val current = state.profile?.visibility

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialPrivacyRoot),
    ) {
        // Top nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "‹  You",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("Privacy", style = frauncesDisplay(28.sp), color = AppInk)
                Text("profile visibility", style = jetbrainsMono(10.sp), color = AppInk3)
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            item {
                Spacer(Modifier.height(14.dp))
                Text(
                    "WHO CAN SEE YOUR SESSIONS",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                    color = AppInk3,
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose who can view your session data, statistics, and activity on the leaderboard and friend profiles.",
                    style = frauncesDisplay(13.sp, italic = true),
                    color = AppInk3,
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                VisibilityOptionRow(
                    option = SocialVisibility.public,
                    title = "Public",
                    description = "Anyone can see your scores and activity.",
                    current = current,
                    onSelect = { viewModel.updateVisibility(SocialVisibility.public) },
                )
            }
            item {
                VisibilityOptionRow(
                    option = SocialVisibility.friends,
                    title = "Friends only",
                    description = "Only mutual friends can see your activity.",
                    current = current,
                    onSelect = { viewModel.updateVisibility(SocialVisibility.friends) },
                )
            }
            item {
                VisibilityOptionRow(
                    option = SocialVisibility.club,
                    title = "Club members",
                    description = "Friends and club members can see your activity.",
                    current = current,
                    onSelect = { viewModel.updateVisibility(SocialVisibility.club) },
                )
            }
            item {
                VisibilityOptionRow(
                    option = SocialVisibility.nobody,
                    title = "Nobody",
                    description = "Your activity is completely private.",
                    current = current,
                    onSelect = { viewModel.updateVisibility(SocialVisibility.nobody) },
                )
            }

            // Muted & blocked (§14)
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "MUTED & BLOCKED",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                    color = AppInk3,
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppLine)
                        .background(AppPaper)
                        .clickable(onClick = onManageBlocksClick)
                        .padding(14.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Muted & blocked", style = frauncesDisplay(15.sp), color = AppInk)
                        Text(
                            "Archers, clubs, and leagues hidden from your feed.",
                            style = jetbrainsMono(9.sp),
                            color = AppInk3,
                        )
                    }
                    Text("›", style = frauncesDisplay(20.sp), color = AppPondDk)
                }
            }

            // Save feedback
            state.error?.let { err ->
                item {
                    Spacer(Modifier.height(12.dp))
                    Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
                }
            }
            if (state.saveSuccess) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Saved.", style = jetbrainsMono(10.sp), color = AppInk3)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun VisibilityOptionRow(
    option: SocialVisibility,
    title: String,
    description: String,
    current: SocialVisibility?,
    onSelect: () -> Unit,
) {
    val selected = current == option
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, if (selected) AppPondDk else AppLine)
            .background(if (selected) AppPaper2 else AppPaper)
            .clickable(onClick = onSelect)
            .padding(14.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = frauncesDisplay(15.sp), color = if (selected) AppPondDk else AppInk)
            Text(description, style = jetbrainsMono(9.sp), color = AppInk3)
        }
        if (selected) {
            Text(
                "ACTIVE",
                style = interUI(7.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppPondDk,
                modifier = Modifier.border(1.dp, AppPondDk).padding(5.dp, 2.dp),
            )
        }
    }
}

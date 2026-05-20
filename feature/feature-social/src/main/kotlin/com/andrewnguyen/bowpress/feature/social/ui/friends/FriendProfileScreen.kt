package com.andrewnguyen.bowpress.feature.social.ui.friends

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
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.FriendProfile
import com.andrewnguyen.bowpress.core.model.SessionSummary
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

@Composable
fun FriendProfileScreen(
    otherUserId: String,
    onBack: () -> Unit,
    onCompare: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.profileState.collectAsState()

    LaunchedEffect(otherUserId) {
        viewModel.loadFriendProfile(otherUserId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
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
                    "‹  Friends",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text(
                    state.friendProfile?.profile?.displayName ?: "…",
                    style = frauncesDisplay(28.sp),
                    color = AppInk,
                )
                state.friendProfile?.profile?.handle?.let {
                    Text("@$it", style = jetbrainsMono(10.sp), color = AppInk3)
                }
            }
            // Friend avatar
            state.friendProfile?.profile?.let { profile ->
                SocialAvatar(
                    initials = avatarInitials(profile.displayName),
                    size = 34,
                )
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        if (state.isLoading) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Loading…",
                style = frauncesDisplay(14.sp),
                color = AppInk3,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            return
        }

        state.friendProfile?.let { fp ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                // Current setup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "CURRENT SETUP",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppPondDk,
                    )
                }
                fp.profile.bowSummary?.let { bow ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppLine)
                            .background(AppPaper2)
                            .padding(12.dp, 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(bow, style = frauncesDisplay(15.sp), color = AppInk, modifier = Modifier.weight(1f))
                        Text(
                            "ACTIVE",
                            style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                            color = AppPine,
                            modifier = Modifier.border(1.dp, AppPine).padding(5.dp, 2.dp),
                        )
                    }
                }

                // 30d stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "LAST 30 DAYS",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Text(
                        "${fp.stat30d.sessionCount} sessions · ${fp.stat30d.arrowCount} arrows",
                        style = jetbrainsMono(9.5.sp),
                        color = AppInk3,
                    )
                }
                HorizontalDivider(color = AppLine, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                ) {
                    StatBlock(label = "Avg", value = String.format("%.1f", fp.stat30d.avgArrowScore), unit = "per arrow", primary = true, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(1.dp).background(AppLine2))
                    StatBlock(label = "X rate", value = "${(fp.stat30d.xRate * 100).toInt()}%", unit = "of arrows", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(1.dp).background(AppLine2))
                    StatBlock(
                        label = "Group ∅",
                        value = fp.stat30d.groupSigmaMm?.let { String.format("%.1f″", it / 25.4) } ?: "—",
                        unit = "at 50m",
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(color = AppLine, thickness = 1.dp)

                // Recent sessions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text("Recent sessions", style = frauncesDisplay(14.sp), color = AppInk)
                    Text(
                        "filtered · mutual face",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppInk3,
                    )
                }
                fp.recentSessions.take(5).forEach { session ->
                    SessionSummaryRow(session = session, initials = avatarInitials(fp.profile.displayName))
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }

                // Compare CTA
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppPondDk)
                        .clickable { onCompare(otherUserId) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Compare", style = frauncesDisplay(18.sp).copy(color = AppPaper))
                        fp.recentSessions.firstOrNull()?.let {
                            Text(
                                "${it.distance} · ${it.targetFaceType} · last 30 days",
                                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                                color = AppPaper2.copy(alpha = 0.7f),
                            )
                        }
                    }
                    Text("›", style = frauncesDisplay(30.sp).copy(color = AppPaper))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatBlock(
    label: String,
    value: String,
    unit: String,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 4.dp)) {
        Text(
            label.uppercase(),
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
        Text(
            value,
            style = frauncesDisplay(if (primary) 26.sp else 22.sp),
            color = if (primary) AppPondDk else AppInk,
        )
        Text(unit, style = interUI(9.sp), color = AppInk3)
    }
}

@Composable
private fun SessionSummaryRow(session: SessionSummary, initials: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = initials, size = 32)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.title ?: "Session",
                style = frauncesDisplay(14.sp),
                color = AppInk,
            )
            Text(
                "${session.distance ?: ""} · ${session.targetFaceType ?: ""}",
                style = jetbrainsMono(9.sp),
                color = AppInk3,
            )
            Text(
                "${session.score} · ${session.xCount}X · ${session.arrowCount} arrows",
                style = jetbrainsMono(9.5.sp),
                color = AppInk2,
            )
        }
    }
}

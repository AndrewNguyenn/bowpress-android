package com.andrewnguyen.bowpress.feature.social.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.CompareStatBlock
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

@Composable
fun CompareScreen(
    otherUserId: String,
    onBack: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.compareState.collectAsState()
    val profileState by viewModel.profileState.collectAsState()

    LaunchedEffect(otherUserId) {
        viewModel.loadCompare(otherUserId)
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
                .padding(top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "‹  ${profileState.friendProfile?.profile?.displayName ?: "Back"}",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("Compare", style = frauncesDisplay(28.sp), color = AppInk)
            }
            // Stamp
            Column(horizontalAlignment = Alignment.End) {
                state.compareView?.let { cv ->
                    Box(
                        modifier = Modifier.border(1.dp, AppPondDk).padding(5.dp, 3.dp),
                    ) {
                        Text(
                            "${cv.distance ?: "50m"} · ${cv.face ?: "122cm"}\nlast 30 days",
                            style = jetbrainsMono(8.5.sp),
                            color = AppPondDk,
                        )
                    }
                }
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

        state.compareView?.let { cv ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                // vs header
                val friendName = profileState.friendProfile?.profile?.displayName ?: "Friend"
                val friendHandle = profileState.friendProfile?.profile?.handle ?: ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Me
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SocialAvatar(initials = "AN", size = 42)
                        Spacer(Modifier.height(6.dp))
                        Text("You", style = frauncesDisplay(14.sp), color = AppInk)
                        Text("@andrew.n", style = jetbrainsMono(9.sp), color = AppInk3)
                        Text("${cv.me.sessionCount} sessions", style = jetbrainsMono(9.sp), color = AppInk3)
                    }
                    Text("vs", style = frauncesDisplay(24.sp), color = AppInk3)
                    // Them
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = androidx.compose.ui.Modifier
                                .padding(0.dp)
                                .border(1.dp, AppPine)
                                .background(AppPaper2)
                                .padding(0.dp)
                                .then(Modifier.height(42.dp).width(42.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = avatarInitials(friendName),
                                style = frauncesDisplay(16.sp),
                                color = AppPine,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(friendName, style = frauncesDisplay(14.sp), color = AppInk)
                        Text("@$friendHandle", style = jetbrainsMono(9.sp), color = AppInk3)
                        Text("${cv.them.sessionCount} sessions", style = jetbrainsMono(9.sp), color = AppInk3)
                    }
                }
                HorizontalDivider(color = AppLine, thickness = 1.dp)

                // Stat comparisons
                CompareRow(
                    label = "Avg",
                    subLabel = "per arrow",
                    myValue = String.format("%.1f", cv.me.avgScore),
                    theirValue = String.format("%.1f", cv.them.avgScore),
                    myWins = cv.me.avgScore >= cv.them.avgScore,
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                CompareRow(
                    label = "X rate",
                    subLabel = "of arrows",
                    myValue = "${(cv.me.xRate * 100).toInt()}%",
                    theirValue = "${(cv.them.xRate * 100).toInt()}%",
                    myWins = cv.me.xRate >= cv.them.xRate,
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                cv.me.groupSigmaMm?.let { myG ->
                    cv.them.groupSigmaMm?.let { theirG ->
                        CompareRow(
                            label = "Group σ",
                            subLabel = "tighter = better",
                            myValue = String.format("%.1f″", myG / 25.4),
                            theirValue = String.format("%.1f″", theirG / 25.4),
                            myWins = myG <= theirG, // lower = tighter = better
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        } ?: run {
            state.error?.let { err ->
                Text(err, style = jetbrainsMono(10.sp), color = AppMaple, modifier = Modifier.padding(18.dp))
            }
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    subLabel: String,
    myValue: String,
    theirValue: String,
    myWins: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Me
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                myValue,
                style = frauncesDisplay(22.sp),
                color = if (myWins) AppPine else AppInk,
            )
        }
        Spacer(Modifier.width(8.dp))
        // Center label
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
            Text(
                label.uppercase(),
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
            Text(subLabel, style = frauncesDisplay(10.5.sp, italic = true), color = AppInk2)
        }
        Spacer(Modifier.width(8.dp))
        // Them
        Column(Modifier.weight(1f)) {
            Text(
                theirValue,
                style = frauncesDisplay(22.sp),
                color = if (!myWins) AppPine else AppInk,
            )
        }
    }
}

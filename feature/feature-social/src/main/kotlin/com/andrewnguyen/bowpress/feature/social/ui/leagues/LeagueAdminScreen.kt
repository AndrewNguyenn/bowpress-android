package com.andrewnguyen.bowpress.feature.social.ui.leagues

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.andrewnguyen.bowpress.core.model.LeagueEntry
import com.andrewnguyen.bowpress.core.model.LeagueSubmission
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.label

@Composable
fun LeagueAdminScreen(
    leagueId: String,
    onBack: () -> Unit,
    viewModel: LeagueViewModel = hiltViewModel(),
) {
    val state by viewModel.leagueHomeState.collectAsState()

    LaunchedEffect(leagueId) {
        viewModel.loadLeagueHome(leagueId)
        viewModel.loadAdminMatrix(leagueId)
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
                    "‹  League",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text(
                    state.league?.name ?: "Admin",
                    style = frauncesDisplay(28.sp),
                    color = AppInk,
                )
                Text(
                    "admin matrix",
                    style = jetbrainsMono(10.sp),
                    color = AppInk3,
                )
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        if (state.isLoading) {
            Spacer(Modifier.height(32.dp))
            return
        }

        state.error?.let { err ->
            Text(
                err,
                style = jetbrainsMono(10.sp),
                color = AppMaple,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }

        val matrix = state.adminMatrix
        val entries = matrix?.entries ?: emptyList()
        val submissions = matrix?.submissions ?: emptyList()
        val totalWeeks = matrix?.totalWeeks ?: 0

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            // Invite code section
            state.league?.inviteCode?.let { code ->
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "INVITE CODE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, AppLine)
                            .background(AppPaper2)
                            .padding(14.dp, 12.dp),
                    ) {
                        Text(
                            code,
                            style = jetbrainsMono(22.sp),
                            color = AppPondDk,
                        )
                    }
                }
            }

            // Matrix header
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "SCORE MATRIX",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                        modifier = Modifier.weight(1f),
                    )
                    if (totalWeeks > 0) {
                        Text(
                            "$totalWeeks weeks",
                            style = jetbrainsMono(9.sp),
                            color = AppInk3,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, AppLine)
                            .background(AppPaper2)
                            .padding(14.dp),
                    ) {
                        Text(
                            "No archers enrolled yet.",
                            style = frauncesDisplay(14.sp, italic = true),
                            color = AppInk3,
                        )
                    }
                }
            } else {
                items(entries, key = { it.userId }) { entry ->
                    val entrySubmissions = submissions.filter { it.userId == entry.userId }
                    AdminEntryRow(
                        entry = entry,
                        submissions = entrySubmissions,
                        totalWeeks = totalWeeks,
                    )
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AdminEntryRow(
    entry: LeagueEntry,
    submissions: List<LeagueSubmission>,
    totalWeeks: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SocialAvatar(initials = avatarInitials(entry.displayName), size = 28)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, style = frauncesDisplay(14.sp), color = AppInk)
                Text(
                    "@${entry.handle} · ${entry.division.label()}",
                    style = jetbrainsMono(9.sp),
                    color = AppInk3,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${submissions.size}/${if (totalWeeks > 0) totalWeeks else "?"}",
                    style = jetbrainsMono(10.sp),
                    color = if (submissions.size >= totalWeeks && totalWeeks > 0) AppPine else AppInk3,
                )
                Text("weeks", style = jetbrainsMono(8.sp), color = AppInk3)
            }
        }

        // Weekly submission chips
        if (submissions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row {
                submissions.sortedBy { it.week }.forEach { sub ->
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .border(1.dp, AppLine)
                            .background(AppPaper2)
                            .padding(6.dp, 3.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "W${sub.week}",
                                style = interUI(7.sp, FontWeight.SemiBold).copy(letterSpacing = 0.16.em),
                                color = AppInk3,
                            )
                            Text(
                                "${sub.rawScore}",
                                style = jetbrainsMono(9.sp),
                                color = AppInk,
                            )
                        }
                    }
                }
            }
        }
    }
}

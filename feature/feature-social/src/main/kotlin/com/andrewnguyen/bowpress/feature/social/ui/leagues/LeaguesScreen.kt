package com.andrewnguyen.bowpress.feature.social.ui.leagues

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueStatus
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InvitationRow
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InvitationsViewModel
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InvitesSectionHeader

@Composable
fun LeaguesScreen(
    onBack: () -> Unit,
    onLeagueClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: LeagueViewModel = hiltViewModel(),
    invitationsViewModel: InvitationsViewModel = hiltViewModel(),
) {
    val state by viewModel.leaguesState.collectAsState()
    val invitesState by invitationsViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialLeaguesRoot),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    // iOS parity (A1) — back label is "Feed", not "Social".
                    "‹  Feed",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("Leagues", style = frauncesDisplay(28.sp), color = AppInk)
                Text("${state.leagues.size} leagues", style = jetbrainsMono(10.sp), color = AppInk3)
            }
            Box(
                modifier = Modifier
                    .border(1.dp, AppPondDk)
                    .background(AppPondDk)
                    .clickable(onClick = onCreateClick)
                    .padding(10.dp, 6.dp),
            ) {
                Text(
                    "NEW LEAGUE",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPaper,
                )
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            state.error?.let { err ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
                }
            }

            // Pending league invitations (§11)
            if (invitesState.leagueInvites.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.testTag(TestTags.SocialLeagueInvitesSection)) {
                        InvitesSectionHeader(count = invitesState.leagueInvites.size)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                // Namespace the key — see ClubsScreen for the full rationale;
                // invite ids and league ids can collide in the same LazyColumn.
                items(invitesState.leagueInvites, key = { "invite-${it.id}" }) { invite ->
                    InvitationRow(
                        invitation = invite,
                        onAccept = {
                            invitationsViewModel.acceptInvitation(invite.id) {
                                viewModel.loadLeagues()
                            }
                        },
                        onDecline = { invitationsViewModel.declineInvitation(invite.id) },
                    )
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }
            invitesState.error?.let { err ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
                }
            }

            if (state.leagues.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .border(1.dp, AppLine)
                            .background(AppPaper2)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No leagues yet.", style = frauncesDisplay(18.sp), color = AppInk)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Create a league or get an invite code from a host.",
                            style = frauncesDisplay(13.sp, italic = true),
                            color = AppInk3,
                        )
                    }
                }
            } else {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text("MY LEAGUES", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em), color = AppInk3)
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                items(state.leagues, key = { "league-${it.id}" }) { league ->
                    LeagueRow(league = league, onClick = { onLeagueClick(league.id) })
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LeagueRow(league: League, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(league.name, style = frauncesDisplay(15.sp), color = AppInk)
            Text(
                "${league.entryCount} archers · ${league.schedule.kind.name} · ${league.status.name}",
                style = jetbrainsMono(9.5.sp),
                color = AppInk3,
            )
        }
        val statusColor = when (league.status) {
            LeagueStatus.active -> AppPine
            LeagueStatus.upcoming -> AppPondDk
            LeagueStatus.ended -> AppStone
        }
        Text(
            league.status.name.uppercase(),
            style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = statusColor,
            modifier = Modifier.border(1.dp, statusColor).padding(5.dp, 2.dp),
        )
    }
}

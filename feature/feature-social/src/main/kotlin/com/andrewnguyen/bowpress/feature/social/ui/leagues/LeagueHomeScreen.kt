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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueStandingRow
import com.andrewnguyen.bowpress.core.model.LeagueStatus
import com.andrewnguyen.bowpress.core.model.LeagueSubmission
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.blocks.BlockViewModel
import com.andrewnguyen.bowpress.feature.social.ui.blocks.MuteBlockAction
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InviteByHandleDialog
import com.andrewnguyen.bowpress.feature.social.ui.label

@Composable
fun LeagueHomeScreen(
    leagueId: String,
    onBack: () -> Unit,
    onAdminClick: (String) -> Unit,
    viewModel: LeagueViewModel = hiltViewModel(),
    blockViewModel: BlockViewModel = hiltViewModel(),
) {
    val state by viewModel.leagueHomeState.collectAsState()
    val blocksState by blockViewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(leagueId) {
        viewModel.loadLeagueHome(leagueId)
    }

    if (showInviteDialog) {
        InviteByHandleDialog(
            title = "Invite to ${state.league?.name ?: "league"}",
            error = state.inviteError,
            sent = state.inviteSent,
            onSubmit = { handle -> viewModel.inviteToLeague(leagueId, handle) },
            onDismiss = {
                showInviteDialog = false
                viewModel.resetInviteState()
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        val league = state.league

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
                    "‹  Leagues",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text(league?.name ?: "League", style = frauncesDisplay(28.sp), color = AppInk)
                league?.let {
                    Text(
                        "${it.entryCount} archers · ${it.status.name}",
                        style = jetbrainsMono(10.sp),
                        color = AppInk3,
                    )
                }
            }
            // Host-only actions: invite an archer, open the admin matrix.
            if (league?.myEntry == null && league?.hostUserId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, AppPondDk)
                            .background(AppPondDk)
                            .clickable { showInviteDialog = true }
                            .padding(8.dp, 5.dp),
                    ) {
                        Text("INVITE", style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em), color = AppPaper)
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, AppPondDk)
                            .clickable { onAdminClick(leagueId) }
                            .padding(8.dp, 5.dp),
                    ) {
                        Text("ADMIN", style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em), color = AppPondDk)
                    }
                }
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        if (state.isLoading) {
            Spacer(Modifier.height(32.dp))
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            // Round spec
            league?.let { lg ->
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "ROUND SPEC",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppLine)
                            .background(AppPaper2)
                            .padding(14.dp),
                    ) {
                        Column {
                            Text(
                                lg.name,
                                style = frauncesDisplay(17.sp),
                                color = AppInk,
                            )
                            Text(
                                "${lg.round.endCount} ends · ${lg.round.arrowsPerEnd} arrows/end · ${lg.schedule.kind.name}",
                                style = jetbrainsMono(10.sp),
                                color = AppInk3,
                            )
                            Text(
                                "${lg.handicap.equation.name} handicap${lg.handicap.allowancePct?.let { " · ${(it * 100).toInt()}%" } ?: ""}",
                                style = jetbrainsMono(10.sp),
                                color = AppInk3,
                            )
                        }
                    }
                }
            }

            // Division filter tabs
            if ((league?.divisions?.size ?: 0) > 1) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppLine)
                            .background(AppPaper2),
                    ) {
                        val divisions = league?.divisions ?: emptyList()
                        // All tab
                        val tabs = listOf<Division?>(null) + divisions
                        tabs.forEach { div ->
                            val selected = state.selectedDivision == div
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (selected) AppPaper else AppPaper2)
                                    .border(0.dp, AppLine)
                                    .clickable { viewModel.setDivisionFilter(div) }
                                    .padding(6.dp, 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = div?.label() ?: "ALL",
                                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                                    color = if (selected) AppPondDk else AppInk3,
                                )
                            }
                        }
                    }
                }
            }

            // Standings
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text("STANDINGS", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em), color = AppInk3)
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
            }

            val filteredStandings = state.standings.let { rows ->
                val div = state.selectedDivision
                if (div == null) rows else rows.filter { it.division == div }
            }

            items(filteredStandings, key = { it.userId }) { row ->
                StandingRow(row = row)
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
            }

            // My submissions
            if (state.mySubmissions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "MY SUBMISSIONS",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                items(state.mySubmissions, key = { it.id }) { sub ->
                    SubmissionRow(sub = sub)
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }

            // Mute / block (§14)
            league?.let { lg ->
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "MANAGE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(6.dp))
                    MuteBlockAction(
                        kind = BlockKind.league,
                        targetId = lg.id,
                        targetName = lg.name,
                        block = blocksState.blockFor(lg.id),
                        onSetMode = { mode ->
                            blockViewModel.setBlock(BlockKind.league, lg.id, lg.name, mode)
                        },
                        onRemove = { blockViewModel.removeBlock(it) },
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StandingRow(row: LeagueStandingRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (row.isYou) Modifier.background(AppPaper2) else Modifier)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${row.rank}",
            style = frauncesDisplay(if (row.rank == 1) 21.sp else 17.sp),
            color = if (row.rank == 1) AppPine else AppInk3,
            modifier = Modifier.width(26.dp),
        )
        Spacer(Modifier.width(10.dp))
        SocialAvatar(initials = avatarInitials(row.displayName), size = 30)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(row.displayName, style = frauncesDisplay(14.sp), color = AppInk)
                if (row.isYou) {
                    Text(
                        " YOU",
                        style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPondDk,
                        modifier = Modifier.border(1.dp, AppPondDk).padding(4.dp, 1.dp),
                    )
                }
            }
            Text(
                "@${row.handle} · ${row.division.label()} · ${row.weeksSubmitted}wk",
                style = jetbrainsMono(9.sp),
                color = AppInk3,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${row.adjustedTotal}", style = frauncesDisplay(18.sp), color = AppInk)
            Text("raw ${row.total}", style = jetbrainsMono(9.sp), color = AppInk3)
        }
    }
}

@Composable
private fun SubmissionRow(sub: LeagueSubmission) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Week ${if (sub.week == 0) "—" else "${sub.week}"}", style = jetbrainsMono(10.sp), color = AppInk3)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${sub.rawScore}", style = frauncesDisplay(14.sp), color = AppInk)
            if (sub.adjustedScore != sub.rawScore) {
                Text("+${sub.adjustedScore - sub.rawScore} adj", style = jetbrainsMono(9.sp), color = AppPondDk)
            }
        }
    }
}

package com.andrewnguyen.bowpress.feature.social.ui.clubs

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
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.ClubFeedItem
import com.andrewnguyen.bowpress.core.model.ClubMember
import com.andrewnguyen.bowpress.core.model.ClubRole
import com.andrewnguyen.bowpress.core.model.LeaderboardRow
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.blocks.BlockViewModel
import com.andrewnguyen.bowpress.feature.social.ui.blocks.MuteBlockAction
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InviteByHandleDialog

@Composable
fun ClubHomeScreen(
    clubId: String,
    onBack: () -> Unit,
    viewModel: ClubViewModel = hiltViewModel(),
    blockViewModel: BlockViewModel = hiltViewModel(),
) {
    val state by viewModel.clubHomeState.collectAsState()
    val blocksState by blockViewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    val isHost = state.club?.myRole == ClubRole.host

    LaunchedEffect(clubId) {
        viewModel.loadClubHome(clubId)
    }

    if (showInviteDialog) {
        InviteByHandleDialog(
            title = "Invite to ${state.club?.name ?: "club"}",
            error = state.inviteError,
            sent = state.inviteSent,
            onSubmit = { handle -> viewModel.inviteToClub(clubId, handle) },
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
                    "‹  Clubs",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text(
                    state.club?.name ?: "Club",
                    style = frauncesDisplay(28.sp),
                    color = AppInk,
                )
                Text(
                    "${state.members.size} members · ${state.club?.myRole?.name ?: "member"}",
                    style = jetbrainsMono(10.sp),
                    color = AppInk3,
                )
            }
            if (isHost) {
                Box(
                    modifier = Modifier
                        .border(1.dp, AppPondDk)
                        .background(AppPondDk)
                        .clickable { showInviteDialog = true }
                        .padding(10.dp, 6.dp),
                ) {
                    Text(
                        "INVITE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPaper,
                    )
                }
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        if (state.isLoading) {
            Spacer(Modifier.height(32.dp))
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            // Notes / pinned info
            state.club?.notes?.let { notes ->
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "RANGE NOTES",
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
                            .padding(12.dp),
                    ) {
                        Text(notes, style = frauncesDisplay(13.sp, italic = true), color = AppInk2)
                    }
                }
            }

            // Leaderboard
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "LEADERBOARD",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    // Scope tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("30d", "season", "all").forEach { scope ->
                            val selected = scope == state.leaderboardScope
                            Box(
                                modifier = Modifier
                                    .border(1.dp, if (selected) AppPondDk else AppLine)
                                    .background(if (selected) AppPondDk else AppPaper)
                                    .clickable { viewModel.setLeaderboardScope(clubId, scope) }
                                    .padding(6.dp, 3.dp),
                            ) {
                                Text(
                                    scope.uppercase(),
                                    style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.18.em),
                                    color = if (selected) AppPaper else AppInk3,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
            }
            items(state.leaderboard, key = { it.userId }) { row ->
                LeaderboardRowItem(row = row)
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
            }

            // Activity feed
            if (state.feed.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "RECENT ACTIVITY",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                items(state.feed, key = { it.id }) { item ->
                    ClubFeedItemRow(item = item)
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }

            // Members
            if (state.members.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "MEMBERS",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                items(state.members, key = { it.userId }) { member ->
                    MemberRow(member = member)
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }

            // Mute / block (§14)
            state.club?.let { club ->
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "MANAGE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(6.dp))
                    MuteBlockAction(
                        kind = BlockKind.club,
                        targetId = club.id,
                        targetName = club.name,
                        block = blocksState.blockFor(club.id),
                        onSetMode = { mode ->
                            blockViewModel.setBlock(BlockKind.club, club.id, club.name, mode)
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
private fun LeaderboardRowItem(row: LeaderboardRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (row.isYou) Modifier.background(AppPaper2) else Modifier)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank
        Text(
            text = "${row.rank}",
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
            Text("@${row.handle}", style = jetbrainsMono(9.sp), color = AppInk3)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${row.score}",
                style = frauncesDisplay(18.sp),
                color = AppInk,
            )
            Text("${row.xCount}X", style = jetbrainsMono(9.sp), color = AppInk3)
        }
    }
}

@Composable
private fun ClubFeedItemRow(item: ClubFeedItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SocialAvatar(initials = avatarInitials(item.actorDisplayName), size = 30)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.actorDisplayName, style = interUI(10.5.sp, FontWeight.SemiBold), color = AppPondDk)
            Text(item.title, style = frauncesDisplay(13.sp, italic = true), color = AppInk)
            item.meta?.let { Text(it, style = jetbrainsMono(9.sp), color = AppInk3) }
        }
    }
}

@Composable
private fun MemberRow(member: ClubMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = avatarInitials(member.displayName), size = 32)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(member.displayName, style = frauncesDisplay(14.sp), color = AppInk)
            Text("@${member.handle}", style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
        if (member.role == ClubRole.host) {
            Text(
                "HOST",
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
                modifier = Modifier.border(1.dp, AppPondDk).padding(5.dp, 2.dp),
            )
        }
    }
}

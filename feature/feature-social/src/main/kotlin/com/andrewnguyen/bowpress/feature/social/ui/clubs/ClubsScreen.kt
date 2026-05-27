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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubRole
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InvitationRow
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InvitationsViewModel
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InvitesSectionHeader

@Composable
fun ClubsScreen(
    onBack: () -> Unit,
    onClubClick: (String) -> Unit,
    viewModel: ClubViewModel = hiltViewModel(),
    invitationsViewModel: InvitationsViewModel = hiltViewModel(),
) {
    val state by viewModel.clubsState.collectAsState()
    val invitesState by invitationsViewModel.uiState.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }
    var newClubName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialClubsRoot),
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
                Text("Clubs", style = frauncesDisplay(28.sp), color = AppInk)
                Text("${state.clubs.size} clubs", style = jetbrainsMono(10.sp), color = AppInk3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, AppPondDk)
                        .clickable { showJoin = !showJoin }
                        .padding(10.dp, 6.dp),
                ) {
                    Text("JOIN", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em), color = AppPondDk)
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, AppPondDk)
                        .background(AppPondDk)
                        .clickable { showCreate = !showCreate }
                        .padding(10.dp, 6.dp),
                ) {
                    Text("CREATE", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em), color = AppPaper)
                }
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            // Create form
            if (showCreate) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text("NEW CLUB", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em), color = AppPondDk)
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = newClubName,
                        onValueChange = { newClubName = it },
                        textStyle = frauncesDisplay(16.sp).copy(color = AppInk),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (newClubName.isEmpty()) Text("Club name", style = frauncesDisplay(16.sp), color = AppInk3)
                            inner()
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, AppPondDk)
                                .background(AppPondDk)
                                .clickable {
                                    if (newClubName.isNotBlank()) {
                                        viewModel.createClub(newClubName) {
                                            showCreate = false
                                            newClubName = ""
                                            onClubClick(it.id)
                                        }
                                    }
                                }
                                .padding(12.dp, 8.dp),
                        ) {
                            Text("CREATE CLUB", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em), color = AppPaper)
                        }
                        Box(
                            modifier = Modifier
                                .border(1.dp, AppLine)
                                .clickable { showCreate = false }
                                .padding(12.dp, 8.dp),
                        ) {
                            Text("CANCEL", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em), color = AppInk3)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
            }

            // Join form
            if (showJoin) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text("JOIN BY CODE", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em), color = AppPondDk)
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase().take(8) },
                            textStyle = jetbrainsMono(16.sp).copy(color = AppInk),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (joinCode.isEmpty()) Text("INVITE CODE", style = jetbrainsMono(14.sp), color = AppInk3)
                                inner()
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .border(1.dp, AppPondDk)
                                .background(AppPondDk)
                                .clickable {
                                    if (joinCode.isNotBlank()) {
                                        viewModel.joinClub(joinCode) {
                                            showJoin = false
                                            joinCode = ""
                                            onClubClick(it.id)
                                        }
                                    }
                                }
                                .padding(12.dp, 8.dp),
                        ) {
                            Text("JOIN", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em), color = AppPaper)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
            }

            // Error
            state.error?.let { err ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Pending club invitations (§11)
            if (invitesState.clubInvites.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Box(modifier = Modifier.testTag(TestTags.SocialClubInvitesSection)) {
                        InvitesSectionHeader(count = invitesState.clubInvites.size)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                // Namespace the key: LazyColumn requires unique keys across
                // ALL items in the same column, and invite ids can collide
                // with club ids (an invitation row sometimes shares a uuid
                // with the club it points to). Prefix to keep them disjoint.
                items(invitesState.clubInvites, key = { "invite-${it.id}" }) { invite ->
                    InvitationRow(
                        invitation = invite,
                        onAccept = {
                            invitationsViewModel.acceptInvitation(invite.id) {
                                viewModel.loadClubs()
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

            if (state.clubs.isEmpty()) {
                item { ClubEmptyState() }
            } else {
                item {
                    Spacer(Modifier.height(14.dp))
                    Text("MY CLUBS", style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em), color = AppInk3)
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                items(state.clubs, key = { "club-${it.id}" }) { club ->
                    ClubRow(club = club, onClick = { onClubClick(club.id) })
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ClubRow(club: Club, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(club.name, style = frauncesDisplay(15.sp), color = AppInk)
            Text(
                "${club.memberCount} members · ${if (club.myRole == ClubRole.host) "host" else "member"}",
                style = jetbrainsMono(9.5.sp),
                color = AppInk3,
            )
        }
        if (club.myRole == ClubRole.host) {
            Text(
                "HOST",
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
                modifier = Modifier.border(1.dp, AppPondDk).padding(5.dp, 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text("›", style = frauncesDisplay(18.sp), color = AppPond)
    }
}

@Composable
private fun ClubEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No clubs yet.", style = frauncesDisplay(18.sp), color = AppInk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create your own or join with an invite code.",
            style = frauncesDisplay(13.sp, italic = true),
            color = AppInk3,
        )
    }
}

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.andrewnguyen.bowpress.core.model.ClubJoinPolicy
import com.andrewnguyen.bowpress.core.model.ClubMember
import com.andrewnguyen.bowpress.core.model.ClubRole
import com.andrewnguyen.bowpress.core.model.ClubVisibility
import com.andrewnguyen.bowpress.core.model.LeaderboardRow
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.SocialUnavailableNotice
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.blocks.BlockViewModel
import com.andrewnguyen.bowpress.feature.social.ui.blocks.MuteBlockAction
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InviteByHandleDialog

@Composable
fun ClubHomeScreen(
    clubId: String,
    onBack: () -> Unit,
    // Parity E2 / E10 — tap a leaderboard or member-activity row to drill in.
    onOpenArcher: (String) -> Unit = {},
    onOpenSession: (sharedSessionId: String) -> Unit = {},
    viewModel: ClubViewModel = hiltViewModel(),
    blockViewModel: BlockViewModel = hiltViewModel(),
) {
    val state by viewModel.clubHomeState.collectAsState()
    val blocksState by blockViewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var showAnnouncementComposer by remember { mutableStateOf(false) }
    var showDescriptionEditor by remember { mutableStateOf(false) }
    val isHost = state.club?.myRole == ClubRole.host
    val currentUserId = state.currentUserId

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

    if (showAnnouncementComposer) {
        AnnouncementComposerDialog(
            error = state.announcementError,
            onSubmit = { body, pinned ->
                viewModel.postAnnouncement(clubId, body, pinned) {
                    showAnnouncementComposer = false
                }
            },
            onDismiss = {
                showAnnouncementComposer = false
                viewModel.resetAnnouncementError()
            },
        )
    }

    // Parity E3 — host-only description editor sheet.
    if (showDescriptionEditor && state.club != null) {
        DescriptionEditorSheet(
            title = "Edit description",
            initial = state.club?.description.orEmpty(),
            error = state.descriptionError,
            onSave = { newText ->
                viewModel.updateClubDescription(clubId, newText) {
                    showDescriptionEditor = false
                }
            },
            onDismiss = {
                showDescriptionEditor = false
                viewModel.resetDescriptionError()
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

        // Graceful failure — the club home is member/visibility-gated, so a
        // fetch can 403 or fail. Show a quiet notice instead of a blank list.
        if (state.error != null && state.club == null) {
            SocialUnavailableNotice(
                title = "Club unavailable",
                detail = "You may not have access to this club, or it's no longer reachable.",
            )
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            // Parity E3 — read-only description visible to everyone; hosts
            // see an Edit affordance that opens the bottom-sheet editor.
            state.club?.let { club ->
                val desc = club.description?.trim().orEmpty()
                if (desc.isNotEmpty() || isHost) {
                    item {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                "DESCRIPTION",
                                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                                color = AppInk3,
                            )
                            if (isHost) {
                                Text(
                                    if (desc.isEmpty()) "ADD" else "EDIT",
                                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                                    color = AppPondDk,
                                    modifier = Modifier
                                        .clickable { showDescriptionEditor = true }
                                        .padding(4.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = AppLine, thickness = 1.dp)
                        Spacer(Modifier.height(8.dp))
                        if (desc.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AppLine)
                                    .background(AppPaper2)
                                    .padding(14.dp),
                            ) {
                                Text(
                                    desc,
                                    style = frauncesDisplay(13.5.sp, italic = true),
                                    color = AppInk2,
                                )
                            }
                        } else {
                            Text(
                                "No description yet.",
                                style = jetbrainsMono(9.5.sp),
                                color = AppInk3,
                            )
                        }
                    }
                }
            }

            // Parity E8 — host-only visibility + join-policy toggles. Visible
            // only to the club host (the row in the club detail in iOS is
            // member-gated; non-members would never see this section).
            if (isHost && state.club != null) {
                val club = state.club!!
                item {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "ACCESS",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))
                    AccessToggleRow(
                        title = "Visibility · ${club.visibility.label}",
                        detail = if (club.visibility == ClubVisibility.PUBLIC)
                            "members + non-members can view"
                        else
                            "members only",
                        cta = if (club.visibility == ClubVisibility.PUBLIC) "MAKE PRIVATE" else "MAKE PUBLIC",
                        onCta = {
                            val next = if (club.visibility == ClubVisibility.PUBLIC)
                                ClubVisibility.PRIVATE
                            else
                                ClubVisibility.PUBLIC
                            viewModel.updateClubAccess(club.id, visibility = next)
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    AccessToggleRow(
                        title = "Join Policy · ${club.joinPolicy.label}",
                        detail = if (club.joinPolicy == ClubJoinPolicy.OPEN)
                            "anyone with invite code can join"
                        else
                            "host must send invitation first",
                        cta = if (club.joinPolicy == ClubJoinPolicy.OPEN) "INVITE-ONLY" else "OPEN",
                        onCta = {
                            val next = if (club.joinPolicy == ClubJoinPolicy.OPEN)
                                ClubJoinPolicy.INVITE_ONLY
                            else
                                ClubJoinPolicy.OPEN
                            viewModel.updateClubAccess(club.id, joinPolicy = next)
                        },
                    )
                }
            }

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

            // ── Announcement board (§17) ──
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "BOARD",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                    Text(
                        "${state.announcements.size} posts · " +
                            if (isHost) "host can post" else "host-only posts",
                        style = jetbrainsMono(9.sp),
                        color = AppInk3,
                    )
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
            }
            if (state.announcements.isEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No posts yet.",
                        style = frauncesDisplay(13.sp, italic = true),
                        color = AppInk3,
                    )
                }
            } else {
                items(state.announcements, key = { it.id }) { ann ->
                    Spacer(Modifier.height(8.dp))
                    AnnouncementCard(
                        announcement = ann,
                        isHost = isHost,
                        onDelete = { viewModel.deleteAnnouncement(clubId, ann.id) },
                        onTogglePin = {
                            viewModel.setAnnouncementPinned(clubId, ann.id, !ann.pinned)
                        },
                    )
                }
            }
            if (isHost) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppLine)
                            .background(AppPaper2)
                            .clickable { showAnnouncementComposer = true }
                            .padding(11.dp, 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .border(1.dp, AppPondDk),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+", style = frauncesDisplay(18.sp), color = AppPondDk)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Post to the board — only members see it.",
                            style = frauncesDisplay(13.5.sp, italic = true),
                            color = AppInk2,
                        )
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
                LeaderboardRowItem(
                    row = row,
                    // Parity E2 / E10 — tap any non-you row to open that
                    // archer's profile. You-row stays inert.
                    onClick = if (row.userId == currentUserId || row.isYou) null
                    else {
                        { onOpenArcher(row.userId) }
                    },
                )
                HorizontalDivider(color = AppLine2, thickness = 1.dp)
            }

            // ── Member activity (§17) — the club-only member feed ──
            if (state.feed.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            "MEMBER ACTIVITY",
                            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                            color = AppInk3,
                        )
                        Text(
                            "members · last 30d",
                            style = jetbrainsMono(9.sp),
                            color = AppInk3,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                items(state.feed, key = { it.id }) { item ->
                    // Parity E10 — tap a member-activity row that points at a
                    // shared session to drill into the session detail.
                    ClubFeedItemRow(
                        item = item,
                        onClick = item.sharedSessionId?.let { sid ->
                            { onOpenSession(sid) }
                        },
                    )
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
private fun LeaderboardRowItem(row: LeaderboardRow, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (row.isYou) Modifier.background(AppPaper2) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
private fun ClubFeedItemRow(item: ClubFeedItem, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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

/**
 * Parity E8 — host-only access toggle row (visibility + joinPolicy on
 * clubs and leagues share this shape). [cta] is the button label that
 * flips the value when tapped (e.g. "MAKE PRIVATE" when currently public).
 */
@Composable
internal fun AccessToggleRow(
    title: String,
    detail: String,
    cta: String,
    onCta: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = frauncesDisplay(14.sp, italic = true), color = AppInk)
            Text(detail, style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            cta,
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppPondDk,
            modifier = Modifier
                .border(1.dp, AppPondDk)
                .clickable(onClick = onCta)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

/**
 * Parity E3 — bottom-sheet description editor used by [ClubHomeScreen].
 *
 * 200-char counter; [canSave] disallows clearing a non-empty description
 * via whitespace-only input — matches iOS commit 9889102.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DescriptionEditorSheet(
    title: String,
    initial: String,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initial) }
    val maxChars = 200
    val trimmed = text.trim()
    val initialTrimmed = initial.trim()
    val canSave = trimmed.length <= maxChars &&
        trimmed != initialTrimmed &&
        (trimmed.isNotEmpty() || initialTrimmed.isEmpty())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppPaper,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // Header row — Cancel · Title · Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "CANCEL",
                    style = interUI(11.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                    color = AppInk2,
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    title,
                    style = frauncesDisplay(14.sp, italic = true),
                    color = AppInk,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "SAVE",
                    style = interUI(11.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                    color = if (canSave) AppPondDk else AppInk3,
                    modifier = Modifier
                        .then(
                            if (canSave) Modifier.clickable { onSave(trimmed) }
                            else Modifier,
                        ),
                )
            }
            HorizontalDivider(color = AppLine, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { new ->
                    text = if (new.length > maxChars) new.take(maxChars) else new
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                placeholder = {
                    Text(
                        "Describe your club in a couple of sentences…",
                        style = frauncesDisplay(13.sp, italic = true),
                        color = AppInk3,
                    )
                },
                textStyle = frauncesDisplay(14.sp, italic = true).copy(color = AppInk),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppPaper,
                    unfocusedContainerColor = AppPaper,
                    focusedIndicatorColor = AppPondDk,
                    unfocusedIndicatorColor = AppLine,
                ),
            )

            // Counter row — turns maple when full.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (error != null) {
                    Text(
                        error,
                        style = jetbrainsMono(9.5.sp),
                        color = AppMaple,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "${text.length}/$maxChars",
                    style = jetbrainsMono(9.5.sp),
                    color = if (text.length >= maxChars) AppMaple else AppInk3,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

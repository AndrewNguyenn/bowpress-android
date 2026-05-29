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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
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
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipDirection
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

@OptIn(FlowPreview::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onFriendClick: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.friendsState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

    // Set of userIds the signed-in archer is already connected to — drives the
    // per-row FRIENDS pill in the suggestion list so a fuzzy hit on an existing
    // friend can't be re-requested. Mirrors iOS AddFriendSheet.existingFriendUserIds.
    val friendUserIds = remember(state.friends) {
        state.friends.mapTo(HashSet()) { it.otherUserId }
    }

    // Parity E9 — live, debounced (250ms) substring fuzzy search. Every
    // keystroke kicks `searchSuggestions`; the view model handles the
    // empty-query short-circuit. Mirrors iOS commit 5bcf33a.
    LaunchedEffect(Unit) {
        snapshotFlow { searchState.query }
            .debounce(250)
            .collect { q -> viewModel.searchSuggestions(q) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialFriendsRoot),
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
                    // iOS parity (A1) — back label is "Feed", not "Social".
                    "‹  Feed",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("Friends", style = frauncesDisplay(28.sp), color = AppInk)
                Text(
                    "${state.friends.size} connected · ${state.pendingRequests.size} pending",
                    style = jetbrainsMono(10.sp),
                    color = AppInk3,
                )
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            // Parity E9 — live, substring fuzzy add-friend search. Replaces
            // the old exact-handle + manual SEARCH-button flow.
            item {
                Spacer(Modifier.height(14.dp))
                Text(
                    "FIND ARCHERS",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                    color = AppInk3,
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                        .border(1.dp, AppLine)
                        .background(AppPaper2)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Q", style = jetbrainsMono(13.sp), color = AppInk3)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchState.query.isEmpty()) {
                            Text(
                                "name or @handle",
                                style = interUI(14.sp),
                                color = AppInk3,
                            )
                        }
                        BasicTextField(
                            value = searchState.query,
                            onValueChange = { viewModel.setQuery(it) },
                            textStyle = interUI(14.sp).copy(color = AppInk),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.SocialFriendSearchField),
                        )
                    }
                }
                searchState.error?.let { err ->
                    Spacer(Modifier.height(6.dp))
                    Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
                }
            }

            // Live substring results. Hidden when the query is blank so the
            // pending + friends sections still take the full page on first
            // open.
            if (searchState.query.isNotBlank()) {
                if (searchState.suggestions.isEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No archers match \"${searchState.query.trim()}\".",
                            style = jetbrainsMono(10.5.sp),
                            color = AppInk3,
                        )
                    }
                } else {
                    items(searchState.suggestions, key = { it.userId }) { hit ->
                        SuggestionRow(
                            suggestion = hit,
                            // Parity E9 — per-row SENT chip, driven off the
                            // sentHandles set the VM updates on a successful
                            // sendFriendRequest.
                            requestSent = hit.handle in searchState.sentHandles,
                            alreadyFriend = hit.userId in friendUserIds,
                            onAdd = { viewModel.sendFriendRequest(hit.handle) },
                            onOpen = { onFriendClick(hit.userId) },
                        )
                        HorizontalDivider(color = AppLine2, thickness = 1.dp)
                    }
                }
            }

            // Pending requests
            if (state.pendingRequests.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            "REQUESTS",
                            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                            color = AppPondDk,
                        )
                        Text(
                            "${state.pendingRequests.size}",
                            style = jetbrainsMono(10.sp),
                            color = AppInk3,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                // Namespace per-section — see ClubHomeScreen for rationale.
                items(state.pendingRequests, key = { "pending-${it.id}" }) { req ->
                    PendingRequestRow(
                        friendship = req,
                        onAccept = { viewModel.acceptRequest(req.id) },
                        onDecline = { viewModel.declineRequest(req.id) },
                    )
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }

            // Friends list
            if (state.friends.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            "FRIENDS",
                            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                            color = AppInk3,
                        )
                        Text(
                            "${state.friends.size}",
                            style = jetbrainsMono(10.sp),
                            color = AppInk3,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = AppLine, thickness = 1.dp)
                }
                items(state.friends, key = { "friend-${it.id}" }) { friend ->
                    FriendRow(
                        friendship = friend,
                        onClick = { onFriendClick(friend.otherUserId) },
                    )
                    HorizontalDivider(color = AppLine2, thickness = 1.dp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * Parity E9 — single row in the live substring add-friend results. The whole
 * row is tappable to open the archer's profile; the CONNECT chip on the
 * right consumes its own taps to send the friend request without leaving the
 * search.
 */
@Composable
private fun SuggestionRow(
    suggestion: HandleSuggestion,
    requestSent: Boolean,
    alreadyFriend: Boolean,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = avatarInitials(suggestion.displayName), size = 32)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(suggestion.displayName, style = frauncesDisplay(14.sp), color = AppInk)
            Text("@${suggestion.handle}", style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
        when {
            alreadyFriend -> Text(
                "FRIENDS",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
                modifier = Modifier
                    .border(1.dp, AppInk3)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            requestSent -> Text(
                "SENT",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppStone,
                modifier = Modifier
                    .border(1.dp, AppStone)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            else -> Text(
                "CONNECT",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
                modifier = Modifier
                    .border(1.dp, AppPondDk)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PendingRequestRow(
    friendship: Friendship,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = avatarInitials(friendship.otherDisplayName), size = 32)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(friendship.otherDisplayName, style = frauncesDisplay(14.sp), color = AppInk)
            Text("@${friendship.otherHandle}", style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
        if (friendship.direction == FriendshipDirection.incoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, AppPine)
                        .clickable(onClick = onAccept)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "ACCEPT",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPine,
                    )
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, AppStone)
                        .clickable(onClick = onDecline)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "DECLINE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppStone,
                    )
                }
            }
        } else {
            Text(
                "SENT",
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppStone,
            )
        }
    }
}

@Composable
private fun FriendRow(friendship: Friendship, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, AppPine)
                .background(AppPaper2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitials(friendship.otherDisplayName),
                style = frauncesDisplay(12.sp),
                color = AppPine,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(friendship.otherDisplayName, style = frauncesDisplay(14.sp), color = AppInk)
            Text("@${friendship.otherHandle}", style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
        Text("›", style = frauncesDisplay(18.sp), color = AppPond)
    }
}

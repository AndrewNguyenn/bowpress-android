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
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onFriendClick: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.friendsState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

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
                    "‹  Social",
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
            // Search section
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "FIND BY HANDLE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                        color = AppInk3,
                    )
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = AppLine, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("@", style = jetbrainsMono(12.sp), color = AppPondDk)
                    Spacer(Modifier.width(4.dp))
                    BasicTextField(
                        value = searchState.query,
                        onValueChange = { viewModel.setQuery(it) },
                        textStyle = jetbrainsMono(12.sp).copy(color = AppInk),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(TestTags.SocialFriendSearchField),
                        decorationBox = { inner ->
                            if (searchState.query.isEmpty()) {
                                Text("handle", style = jetbrainsMono(12.sp), color = AppInk3)
                            }
                            inner()
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, AppPondDk)
                            .clickable { viewModel.searchArcher(searchState.query) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag(TestTags.SocialFriendSearchSubmit),
                    ) {
                        Text(
                            "SEARCH",
                            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                            color = AppPondDk,
                        )
                    }
                }
                // Search result
                searchState.result?.let { profile ->
                    SearchResultCard(
                        profile = profile,
                        requestSent = searchState.requestSent,
                        onAdd = { viewModel.sendFriendRequest(profile.handle) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                searchState.error?.let { err ->
                    Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
                    Spacer(Modifier.height(8.dp))
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
                items(state.pendingRequests, key = { it.id }) { req ->
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
                items(state.friends, key = { it.id }) { friend ->
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

@Composable
private fun SearchResultCard(
    profile: SocialProfile,
    requestSent: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = avatarInitials(profile.displayName), size = 32)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.displayName, style = frauncesDisplay(14.sp), color = AppInk)
            Text("@${profile.handle}", style = jetbrainsMono(9.5.sp), color = AppInk3)
        }
        if (requestSent) {
            Text(
                "SENT",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppStone,
            )
        } else {
            Box(
                modifier = Modifier
                    .border(1.dp, AppPondDk)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "CONNECT",
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppPondDk,
                )
            }
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

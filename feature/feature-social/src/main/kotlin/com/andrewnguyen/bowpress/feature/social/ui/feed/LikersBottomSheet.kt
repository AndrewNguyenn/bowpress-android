package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.ActivityActor
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatarImage

// ── LikersBottomSheet ────────────────────────────────────────────────────────
//
// The "who liked this" sheet (Social Feed V2 §6.4). A feed card's kudos row
// only carries the ≤3 most-recent likers for the avatar stack, so once a post
// runs past three likes the rest are invisible ("annette h & 4 others liked
// this"). Tapping the kudos stack / caption opens this sheet, which fetches the
// FULL liker list (newest-first) via `GET /social/activity/:subjectId/likers`
// (the repository call wired through [onLoad]) and lists every archer with
// their avatar, display name and @handle.
//
// Mirrors iOS `LikersSheet`.

/**
 * @param subjectId the like subject whose likers to show.
 * @param likeCount the row's known like count — seeds the header so the sheet
 *   doesn't read "0 likes" for the instant before the fetch lands.
 * @param onLoad fetches the full liker list (a suspend lambda so the screen-
 *   level view model drives the Room/API call without leaking dispatchers here).
 * @param onDismiss closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikersBottomSheet(
    subjectId: String,
    likeCount: Int,
    onLoad: suspend (String) -> List<ActivityActor>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var likers by remember { mutableStateOf<List<ActivityActor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    // Bumped by the error-state "Try again" tap to re-run the load effect.
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(subjectId, reloadNonce) {
        isLoading = true
        loadFailed = false
        runCatching { onLoad(subjectId) }
            .onSuccess {
                likers = it
                isLoading = false
            }
            .onFailure {
                loadFailed = true
                isLoading = false
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppPaper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .testTag(TestTags.LikersSheet),
        ) {
            // Header — "KUDOS" eyebrow over a "{N} likes" title. The count never
            // ticks DOWN after the fetch: when the caller just optimistically
            // liked and the server hasn't reflected it, the fetched list omits
            // them, so `maxOf` keeps the seeded count (which already counts the
            // caller) authoritative until the server catches up.
            val count = if (isLoading) likeCount else maxOf(likeCount, likers.size)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "KUDOS",
                    style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppInk3,
                )
                Text(
                    text = if (count == 1) "1 like" else "$count likes",
                    style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium),
                    color = AppInk,
                )
            }
            Spacer(Modifier.padding(top = 8.dp))
            HorizontalDivider(color = AppLine)

            when {
                isLoading -> CenteredBox {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = AppPondDk,
                    )
                }
                loadFailed -> CenteredBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Couldn't load who liked this.",
                            style = frauncesDisplay(14.sp, italic = true),
                            color = AppInk3,
                        )
                        Spacer(Modifier.padding(top = 6.dp))
                        Text(
                            text = "Try again",
                            style = interUI(12.sp, FontWeight.SemiBold),
                            color = AppPondDk,
                            modifier = Modifier.clickable { reloadNonce++ },
                        )
                    }
                }
                likers.isEmpty() -> CenteredBox {
                    Text(
                        text = "No likes yet.",
                        style = frauncesDisplay(14.sp, italic = true),
                        color = AppInk3,
                    )
                }
                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(likers, key = { it.userId }) { actor ->
                        LikerRow(actor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** One liker — 36dp avatar, display name over a mono @handle. */
@Composable
private fun LikerRow(actor: ActivityActor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .testTag(TestTags.LikersSheetRow),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        SocialAvatarImage(
            displayName = actor.displayName,
            userId = actor.userId,
            avatarUrl = actor.avatarUrl,
            avatarVersion = actor.avatarVersion,
            size = 36,
            borderTint = AppPondDk,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = actor.displayName,
                style = frauncesDisplay(15.sp, weight = FontWeight.Medium),
                color = AppInk,
                maxLines = 1,
            )
            Text(
                text = "@${actor.handle}",
                style = jetbrainsMono(11.sp),
                color = AppInk3,
                maxLines = 1,
            )
        }
    }
}

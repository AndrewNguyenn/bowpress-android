package com.andrewnguyen.bowpress.feature.social.ui.invitations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Parity E8 — host-only sheet for inviting a specific archer to a club or
 * league (§11). Live, debounced (250ms) substring fuzzy search on handle OR
 * display name (iOS commit c6b7084 / `InviteArcherSheet.swift`).
 *
 * Replaces the exact-handle-only dialog this codebase used to ship for
 * the host-only invite flow (deleted with parity E8). The invite-code
 * path stays for shareable links; this is the targeted, push-able
 * invitation.
 *
 * [onSearch] returns substring matches, [onInvite] sends the invitation
 * for a specific handle. Both are suspend lambdas so the screen-level
 * view model can drive Room/API calls without leaking dispatchers here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteArcherSheet(
    targetLabel: String,
    onSearch: suspend (String) -> List<HandleSuggestion>,
    onInvite: suspend (HandleSuggestion) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<HandleSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    /** The handle currently being invited (drives the row label). */
    var invitingHandle by remember { mutableStateOf<String?>(null) }
    /** Handles successfully invited this session — flips the row to SENT. */
    var sentHandles by remember { mutableStateOf(setOf<String>()) }
    var status by remember { mutableStateOf<String?>(null) }

    // Drive a new debounced search whenever the query changes. LaunchedEffect
    // already cancels the previous coroutine on a key change, so the delay
    // below acts as the debounce window — no manual Job tracking needed.
    // Mirrors iOS's `scheduleSearch(for:)` from InviteArcherSheet.swift.
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            results = emptyList()
            isSearching = false
            status = null
            return@LaunchedEffect
        }
        isSearching = true
        delay(SEARCH_DEBOUNCE_MS)
        runCatching { onSearch(trimmed) }
            .onSuccess { hits ->
                results = hits
                isSearching = false
            }
            .onFailure {
                results = emptyList()
                isSearching = false
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
                .heightIn(min = 360.dp)
                .padding(horizontal = 16.dp),
        ) {
            // Header
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Invite archer",
                        style = frauncesDisplay(15.sp, italic = true),
                        color = AppInk,
                    )
                    Text(
                        "to $targetLabel",
                        style = jetbrainsMono(9.5.sp),
                        color = AppInk3,
                    )
                }
                Spacer(Modifier.weight(1f))
                // Right-side spacer to balance the CANCEL label.
                Box(Modifier.width(48.dp))
            }
            HorizontalDivider(color = AppLine, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppLine)
                    .background(AppPaper2)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Q",
                    style = jetbrainsMono(13.sp, FontWeight.SemiBold),
                    color = AppInk3,
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = interUI(15.sp).copy(color = AppInk),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "name or @handle",
                                style = interUI(15.sp),
                                color = AppInk3,
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(14.dp))

            // Results / hint area
            val trimmed = query.trim()
            when {
                trimmed.isEmpty() -> Text(
                    "Type a handle or name — partial matches are fine.",
                    style = interUI(13.sp),
                    color = AppInk3,
                )
                isSearching && results.isEmpty() -> Text(
                    "Searching…",
                    style = interUI(13.sp),
                    color = AppInk3,
                )
                results.isEmpty() -> Text(
                    "No archers match \"$trimmed\".",
                    style = interUI(13.sp),
                    color = AppInk3,
                )
                else -> Column {
                    results.forEach { suggestion ->
                        ResultRow(
                            suggestion = suggestion,
                            pending = invitingHandle == suggestion.handle,
                            sent = suggestion.handle in sentHandles,
                            onInvite = {
                                if (suggestion.handle in sentHandles) return@ResultRow
                                if (invitingHandle == suggestion.handle) return@ResultRow
                                scope.launch {
                                    invitingHandle = suggestion.handle
                                    status = null
                                    onInvite(suggestion)
                                        .onSuccess {
                                            sentHandles = sentHandles + suggestion.handle
                                        }
                                        .onFailure {
                                            status =
                                                "Could not send the invite to @${suggestion.handle} — they may already be a member."
                                        }
                                    invitingHandle = null
                                }
                            },
                        )
                    }
                }
            }

            status?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = interUI(13.sp), color = AppMaple)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ResultRow(
    suggestion: HandleSuggestion,
    pending: Boolean,
    sent: Boolean,
    onInvite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !pending && !sent, onClick = onInvite)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = avatarInitials(suggestion.displayName), size = 38)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                suggestion.displayName,
                style = frauncesDisplay(15.sp, italic = true),
                color = AppInk,
            )
            Text(
                "@${suggestion.handle}",
                style = jetbrainsMono(11.sp),
                color = AppInk3,
            )
        }
        when {
            pending -> Text(
                "SENDING…",
                style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppInk3,
            )
            sent -> Text(
                "SENT",
                style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppPine,
                modifier = Modifier
                    .border(1.dp, AppPine)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            else -> Text(
                "INVITE",
                style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppPondDk,
                modifier = Modifier
                    .border(1.dp, AppPondDk)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
    HorizontalDivider(color = AppLine2, thickness = 1.dp)
}

private const val SEARCH_DEBOUNCE_MS = 250L

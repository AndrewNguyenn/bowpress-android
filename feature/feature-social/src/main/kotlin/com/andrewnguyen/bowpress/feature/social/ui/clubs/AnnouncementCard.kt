package com.andrewnguyen.bowpress.feature.social.ui.clubs

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ClubAnnouncement
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials
import com.andrewnguyen.bowpress.feature.social.ui.socialRelativeTime

/**
 * One club-board post (§17). Pinned posts get a pond left-rule + a PINNED
 * stamp. The host sees pin/unpin + delete controls; members read only.
 * Mirrors `announcementCard()` in `social-tab-prototype.js`.
 */
@Composable
fun AnnouncementCard(
    announcement: ClubAnnouncement,
    isHost: Boolean,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Pinned posts get a pond border + faint wash; others a plain line.
            .border(1.dp, if (announcement.pinned) AppPondDk else AppLine)
            .background(if (announcement.pinned) AppPaper2 else AppPaper)
            .padding(14.dp, 13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SocialAvatar(initials = avatarInitials(announcement.authorDisplayName), size = 22)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${announcement.authorDisplayName.uppercase()} · " +
                        socialRelativeTime(announcement.createdAt).uppercase(),
                    style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppInk3,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (announcement.pinned) {
                    Text(
                        "PINNED",
                        style = interUI(8.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                        color = AppPondDk,
                        modifier = Modifier.border(1.dp, AppPondDk).padding(4.dp, 2.dp),
                    )
                }
                if (isHost) {
                    Spacer(Modifier.width(6.dp))
                    HostChip(
                        label = if (announcement.pinned) "UNPIN" else "PIN",
                        onClick = onTogglePin,
                    )
                    Spacer(Modifier.width(4.dp))
                    HostChip(label = "DELETE", onClick = onDelete, danger = true)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            announcement.body,
            style = frauncesDisplay(13.5.sp, italic = true),
            color = AppInk,
        )
    }
}

/** A small bordered host-only control chip. */
@Composable
private fun HostChip(label: String, onClick: () -> Unit, danger: Boolean = false) {
    val color = if (danger) AppMaple else AppInk3
    Box(
        modifier = Modifier
            .border(1.dp, color)
            .clickable(onClick = onClick)
            .padding(6.dp, 3.dp),
    ) {
        Text(
            label,
            style = interUI(7.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
            color = color,
        )
    }
}

/**
 * Host-only composer for a new club-board post — a body field + a pin toggle.
 * [error] surfaces a post failure inline.
 */
@Composable
fun AnnouncementComposerDialog(
    error: String?,
    onSubmit: (body: String, pinned: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var body by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppPondDk)
                .background(AppPaper)
                .padding(20.dp),
        ) {
            Text(
                "POST TO THE BOARD",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                color = AppPondDk,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Only club members see this.",
                style = jetbrainsMono(9.sp),
                color = AppInk3,
            )
            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .border(1.dp, AppLine)
                    .background(AppPaper2)
                    .padding(12.dp, 10.dp),
            ) {
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    textStyle = frauncesDisplay(13.5.sp, italic = true).copy(color = AppInk),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (body.isEmpty()) {
                            Text(
                                "Range closed Sunday for the regional shoot — …",
                                style = frauncesDisplay(13.5.sp, italic = true),
                                color = AppInk3,
                            )
                        }
                        inner()
                    },
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (pinned) AppPondDk else AppLine)
                    .background(if (pinned) AppPaper2 else AppPaper)
                    .clickable { pinned = !pinned }
                    .padding(12.dp, 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Pin to the top", style = frauncesDisplay(14.sp), color = AppInk)
                    Text(
                        "Pinned posts sort above the rest.",
                        style = jetbrainsMono(9.sp),
                        color = AppInk3,
                    )
                }
                Text(
                    if (pinned) "ON" else "OFF",
                    style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                    color = if (pinned) AppPondDk else AppInk3,
                    modifier = Modifier
                        .border(1.dp, if (pinned) AppPondDk else AppLine)
                        .padding(6.dp, 3.dp),
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = jetbrainsMono(10.sp), color = AppMaple)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, AppPondDk)
                        .background(AppPondDk)
                        .clickable { if (body.isNotBlank()) onSubmit(body.trim(), pinned) }
                        .padding(12.dp, 8.dp),
                ) {
                    Text(
                        "POST",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPaper,
                    )
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, AppLine)
                        .clickable(onClick = onDismiss)
                        .padding(12.dp, 8.dp),
                ) {
                    Text(
                        "CANCEL",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppInk3,
                    )
                }
            }
        }
    }
}

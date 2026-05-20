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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

/**
 * Host-only dialog to invite an archer to a club/league by `@handle`.
 *
 * [title] names the target ("Invite to Bay Area Compound"). [error] surfaces
 * an API failure inline (unknown handle, already a member, …). [sent] flips
 * the body to a brief confirmation so the host gets feedback before dismissing.
 */
@Composable
fun InviteByHandleDialog(
    title: String,
    error: String?,
    sent: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var handle by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppPondDk)
                .background(AppPaper)
                .padding(20.dp),
        ) {
            Text(
                "INVITE ARCHER",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                color = AppPondDk,
            )
            Spacer(Modifier.height(4.dp))
            Text(title, style = frauncesDisplay(20.sp), color = AppInk)
            Spacer(Modifier.height(14.dp))

            if (sent) {
                Text(
                    "Invitation sent.",
                    style = frauncesDisplay(15.sp, italic = true),
                    color = AppPine,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, AppPondDk)
                        .background(AppPondDk)
                        .clickable(onClick = onDismiss)
                        .padding(12.dp, 8.dp),
                ) {
                    Text(
                        "DONE",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPaper,
                    )
                }
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppLine)
                    .padding(12.dp, 10.dp),
            ) {
                Text("@", style = jetbrainsMono(16.sp), color = AppInk3)
                BasicTextField(
                    value = handle,
                    onValueChange = { handle = it.trimStart('@').lowercase() },
                    textStyle = jetbrainsMono(16.sp).copy(color = AppInk),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (handle.isEmpty()) {
                            Text("handle", style = jetbrainsMono(16.sp), color = AppInk3)
                        }
                        inner()
                    },
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
                        .clickable { if (handle.isNotBlank()) onSubmit(handle) }
                        .padding(12.dp, 8.dp),
                ) {
                    Text(
                        "SEND INVITE",
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

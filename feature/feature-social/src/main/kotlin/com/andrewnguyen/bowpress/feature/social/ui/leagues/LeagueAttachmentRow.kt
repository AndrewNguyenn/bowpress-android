package com.andrewnguyen.bowpress.feature.social.ui.leagues

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
import com.andrewnguyen.bowpress.core.model.AttachmentKind
import com.andrewnguyen.bowpress.core.model.LeagueAttachment

/** A glyph that reads as the attachment kind. */
private fun AttachmentKind.glyph(): String = when (this) {
    AttachmentKind.LINK -> "↗"
    AttachmentKind.FILE -> "▤"
    AttachmentKind.NOTE -> "✎"
}

/** Lowercase label for a kind, for the row sub-line. */
private fun AttachmentKind.label(): String = when (this) {
    AttachmentKind.LINK -> "link"
    AttachmentKind.FILE -> "file"
    AttachmentKind.NOTE -> "note"
}

/**
 * One league attachment (§17) — a kind glyph, title, and url/note. A `link`
 * row taps through to the URL; the host sees a delete control.
 */
@Composable
fun AttachmentRow(
    attachment: LeagueAttachment,
    isHost: Boolean,
    onOpenLink: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val url = attachment.url
    val tappable = attachment.kind == AttachmentKind.LINK && !url.isNullOrBlank()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper)
            .then(if (tappable) Modifier.clickable { onOpenLink(url!!) } else Modifier)
            .padding(13.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, AppPondDk),
            contentAlignment = Alignment.Center,
        ) {
            Text(attachment.kind.glyph(), style = frauncesDisplay(14.sp), color = AppPondDk)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(attachment.title, style = frauncesDisplay(14.sp), color = AppInk)
            val sub = when (attachment.kind) {
                AttachmentKind.NOTE -> attachment.note ?: "note"
                else -> attachment.url ?: attachment.kind.label()
            }
            Text(
                "${attachment.kind.label()} · $sub",
                style = jetbrainsMono(9.sp),
                color = AppInk3,
            )
        }
        if (isHost) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, AppMaple)
                    .clickable(onClick = onDelete)
                    .padding(6.dp, 3.dp),
            ) {
                Text(
                    "DELETE",
                    style = interUI(7.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                    color = AppMaple,
                )
            }
        }
    }
}

/**
 * Host-only composer for a new league attachment — a kind picker, a title,
 * and a url (link/file) or note (note). [error] surfaces the repository's
 * kind-validation failure inline.
 */
@Composable
fun AttachmentComposerDialog(
    error: String?,
    onSubmit: (kind: AttachmentKind, title: String, url: String?, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(AttachmentKind.LINK) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val needsUrl = kind != AttachmentKind.NOTE

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppPondDk)
                .background(AppPaper)
                .padding(20.dp),
        ) {
            Text(
                "ADD ATTACHMENT",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                color = AppPondDk,
            )
            Spacer(Modifier.height(12.dp))

            // Kind picker.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AttachmentKind.entries.forEach { k ->
                    val selected = k == kind
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (selected) AppPondDk else AppLine)
                            .background(if (selected) AppPondDk else AppPaper)
                            .clickable { kind = k }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            k.label().uppercase(),
                            style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.18.em),
                            color = if (selected) AppPaper else AppInk3,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            DialogField(
                value = title,
                onChange = { title = it },
                placeholder = "Title",
            )

            Spacer(Modifier.height(8.dp))
            if (needsUrl) {
                DialogField(
                    value = url,
                    onChange = { url = it },
                    placeholder = "https://…",
                    mono = true,
                )
            } else {
                DialogField(
                    value = note,
                    onChange = { note = it },
                    placeholder = "Note text",
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
                        .clickable {
                            if (title.isNotBlank()) {
                                onSubmit(
                                    kind,
                                    title.trim(),
                                    url.trim().ifBlank { null },
                                    note.trim().ifBlank { null },
                                )
                            }
                        }
                        .padding(12.dp, 8.dp),
                ) {
                    Text(
                        "ADD",
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

@Composable
private fun DialogField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    mono: Boolean = false,
) {
    val textStyle = if (mono) {
        jetbrainsMono(13.sp).copy(color = AppInk)
    } else {
        frauncesDisplay(14.sp, italic = false).copy(color = AppInk)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(12.dp, 10.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = textStyle,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = textStyle.copy(color = AppInk3))
                inner()
            },
        )
    }
}

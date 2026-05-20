package com.andrewnguyen.bowpress.feature.social.ui.blocks

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.SocialBlock

/** Human-readable noun for a [BlockKind] — used in dialog copy. */
fun BlockKind.noun(): String = when (this) {
    BlockKind.archer -> "archer"
    BlockKind.club -> "club"
    BlockKind.league -> "league"
}

/** Short label for a [BlockMode] in chips / rows. */
fun BlockMode.label(): String = when (this) {
    BlockMode.mute -> "MUTED"
    BlockMode.block -> "BLOCKED"
}

/**
 * Styled "Mute / Block" row for the Friend / Club / League screens.
 *
 * When [block] is null the row offers the action; when set it shows the
 * current state (`MUTED` / `BLOCKED`) and tapping opens the manage dialog.
 * Wire it to a [BlockViewModel]:
 *
 *   MuteBlockAction(
 *     kind = BlockKind.archer, targetId = userId, targetName = handle,
 *     block = blocksState.blockFor(userId),
 *     onSetMode = { mode -> blockVm.setBlock(kind, id, name, mode) },
 *     onRemove = { id -> blockVm.removeBlock(id) },
 *   )
 */
@Composable
fun MuteBlockAction(
    kind: BlockKind,
    targetId: String,
    targetName: String,
    block: SocialBlock?,
    onSetMode: (BlockMode) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.SocialMuteBlockAction)
            .border(1.dp, if (block != null) AppMaple else AppLine)
            .background(if (block != null) AppPaper2 else AppPaper)
            .clickable { dialogOpen = true }
            .padding(14.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (block != null) "Manage mute / block" else "Mute or block",
                style = frauncesDisplay(15.sp),
                color = AppInk,
            )
            Text(
                when (block?.mode) {
                    BlockMode.mute -> "Muted — their activity is hidden from your feed"
                    BlockMode.block -> "Blocked — activity hidden, no friend requests"
                    null -> "Hide ${kind.noun()} activity from your feed"
                },
                style = jetbrainsMono(9.sp),
                color = AppInk3,
            )
        }
        block?.let {
            Text(
                it.mode.label(),
                style = interUI(8.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppMaple,
                modifier = Modifier.border(1.dp, AppMaple).padding(5.dp, 2.dp),
            )
        }
    }

    if (dialogOpen) {
        MuteBlockDialog(
            kind = kind,
            targetName = targetName,
            block = block,
            onMute = { onSetMode(BlockMode.mute); dialogOpen = false },
            onBlock = { onSetMode(BlockMode.block); dialogOpen = false },
            onRemove = { block?.let { onRemove(it.id) }; dialogOpen = false },
            onDismiss = { dialogOpen = false },
        )
    }
}

@Composable
private fun MuteBlockDialog(
    kind: BlockKind,
    targetName: String,
    block: SocialBlock?,
    onMute: () -> Unit,
    onBlock: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppPondDk)
                .background(AppPaper)
                .padding(20.dp),
        ) {
            Text(
                "MUTE / BLOCK",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
                color = AppPondDk,
            )
            Spacer(Modifier.height(4.dp))
            Text(targetName, style = frauncesDisplay(20.sp), color = AppInk)
            Spacer(Modifier.height(12.dp))

            val stayCopy = if (kind == BlockKind.archer) "You stay friends." else "You stay a member."
            val blockCopy = if (kind == BlockKind.archer) {
                "Everything mute does, and the friendship is severed — neither of " +
                    "you can send a friend request."
            } else {
                "Everything mute does. A harder, deliberate cut."
            }

            // Mute option
            DialogOptionRow(
                title = if (block?.mode == BlockMode.mute) "Muted" else "Mute",
                body = "Hide their activity from your feed. No pushes. $stayCopy",
                selected = block?.mode == BlockMode.mute,
                onClick = onMute,
            )
            Spacer(Modifier.height(8.dp))
            // Block option
            DialogOptionRow(
                title = if (block?.mode == BlockMode.block) "Blocked" else "Block",
                body = blockCopy,
                selected = block?.mode == BlockMode.block,
                onClick = onBlock,
            )

            if (block != null) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppLine)
                        .clickable(onClick = onRemove)
                        .padding(12.dp, 10.dp),
                ) {
                    Text(
                        if (block.mode == BlockMode.mute) "UNMUTE" else "UNBLOCK",
                        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                        color = AppPondDk,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 4.dp),
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

@Composable
private fun DialogOptionRow(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (selected) AppMaple else AppLine)
            .background(if (selected) AppPaper2 else AppPaper)
            .clickable(onClick = onClick)
            .padding(14.dp, 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = frauncesDisplay(16.sp), color = if (selected) AppMaple else AppInk)
            Text(body, style = jetbrainsMono(9.sp), color = AppInk3)
        }
        if (selected) {
            Text(
                "ACTIVE",
                style = interUI(7.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                color = AppMaple,
            )
        }
    }
}

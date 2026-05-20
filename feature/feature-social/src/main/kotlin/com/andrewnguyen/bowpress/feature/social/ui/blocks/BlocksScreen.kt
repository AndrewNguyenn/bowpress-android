package com.andrewnguyen.bowpress.feature.social.ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.SocialBlock
import com.andrewnguyen.bowpress.feature.social.ui.SocialAvatar
import com.andrewnguyen.bowpress.feature.social.ui.avatarInitials

/**
 * "Muted & blocked" managed list — every mute/block the user has placed,
 * grouped by kind, each with an Unmute/Unblock affordance. Reached from the
 * Privacy screen (and the You screen).
 */
@Composable
fun BlocksScreen(
    onBack: () -> Unit,
    viewModel: BlockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialBlocksRoot),
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
                    "‹  Privacy",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("Muted & blocked", style = frauncesDisplay(28.sp), color = AppInk)
                Text("${state.blocks.size} hidden", style = jetbrainsMono(10.sp), color = AppInk3)
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            state.error?.let { err ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(err, style = jetbrainsMono(10.sp), color = AppMaple)
                }
            }

            if (state.blocks.isEmpty() && !state.isLoading) {
                item { BlocksEmptyState() }
            }

            blockSection("ARCHERS", state.archerBlocks, viewModel)
            blockSection("CLUBS", state.clubBlocks, viewModel)
            blockSection("LEAGUES", state.leagueBlocks, viewModel)

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Emits a labelled section + its rows, or nothing when [blocks] is empty. */
private fun LazyListScope.blockSection(
    label: String,
    blocks: List<SocialBlock>,
    viewModel: BlockViewModel,
) {
    if (blocks.isEmpty()) return
    item {
        Spacer(Modifier.height(14.dp))
        Text(
            label,
            style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
            color = AppInk3,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = AppLine, thickness = 1.dp)
    }
    items(blocks, key = { it.id }) { block ->
        BlockRow(block = block, onRemove = { viewModel.removeBlock(block.id) })
        HorizontalDivider(color = AppLine2, thickness = 1.dp)
    }
}

@Composable
private fun BlockRow(block: SocialBlock, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(initials = avatarInitials(block.targetName), size = 32)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(block.targetName, style = frauncesDisplay(14.sp), color = AppInk)
            Text(
                block.mode.label().lowercase().replaceFirstChar { it.uppercase() },
                style = jetbrainsMono(9.5.sp),
                color = AppMaple,
            )
        }
        Box(
            modifier = Modifier
                .border(1.dp, AppPondDk)
                .clickable(onClick = onRemove)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                if (block.mode == BlockMode.mute) "UNMUTE" else "UNBLOCK",
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppPondDk,
            )
        }
    }
}

@Composable
private fun BlocksEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nothing muted or blocked.", style = frauncesDisplay(18.sp), color = AppInk)
        Spacer(Modifier.height(8.dp))
        Text(
            "Mute an archer, club, or league from their screen to hide their " +
                "activity from your feed.",
            style = frauncesDisplay(13.sp, italic = true),
            color = AppInk3,
        )
    }
}

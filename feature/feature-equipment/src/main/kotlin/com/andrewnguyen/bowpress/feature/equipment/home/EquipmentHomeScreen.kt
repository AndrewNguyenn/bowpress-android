package com.andrewnguyen.bowpress.feature.equipment.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.bp.BPCard
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEditLink
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStamp
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStampTone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.Bow

/**
 * Kenrokuen equipment home — Bows + Arrows in two stacked sections, each with an
 * ADD edit-link and a flat BPCard row list. Mirrors iOS `ConfigurationView`.
 *
 * Long-press a row to delete; tap to open the detail screen. The FAB and tab row
 * from the old design are replaced by section-level ADD affordances.
 */
@Composable
fun EquipmentHomeScreen(
    onAddBow: () -> Unit,
    onOpenBow: (String) -> Unit,
    onAddArrow: () -> Unit,
    onOpenArrow: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EquipmentHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        BPNavHeader(eyebrow = "BOWPRESS · KIT", title = "Equipment")

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppPond)
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                EquipmentSection(
                    eyebrow = "BOWS · ${state.bows.size}",
                    onAdd = onAddBow,
                ) {
                    if (state.bows.isEmpty()) {
                        EmptyRow("No bows yet")
                    } else {
                        state.bows.forEachIndexed { index, bow ->
                            BowRow(
                                bow = bow,
                                onClick = { onOpenBow(bow.id) },
                                onLongClick = { viewModel.deleteBow(bow.id) },
                            )
                            if (index < state.bows.lastIndex) {
                                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                            }
                        }
                    }
                }

                EquipmentSection(
                    eyebrow = "ARROWS · ${state.arrows.size}",
                    onAdd = onAddArrow,
                ) {
                    if (state.arrows.isEmpty()) {
                        EmptyRow("No arrow setups yet")
                    } else {
                        state.arrows.forEachIndexed { index, arrow ->
                            ArrowRow(
                                arrow = arrow,
                                onClick = { onOpenArrow(arrow.id) },
                                onLongClick = { viewModel.deleteArrow(arrow.id) },
                            )
                            if (index < state.arrows.lastIndex) {
                                HorizontalDivider(color = AppLine2, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section scaffold
// ---------------------------------------------------------------------------

@Composable
private fun EquipmentSection(
    eyebrow: String,
    onAdd: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BPEyebrow(eyebrow)
            BPEditLink(label = "ADD", onClick = onAdd)
        }
        BPCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Row composables
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BowRow(
    bow: Bow,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val spec = buildString {
        append(bow.bowType.label.uppercase())
        val brandModel = listOfNotNull(
            bow.brand.takeIf(String::isNotBlank),
            bow.model.takeIf(String::isNotBlank),
        ).joinToString(" ")
        if (brandModel.isNotBlank()) {
            append(" · ")
            append(brandModel.uppercase())
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("bow_row_${bow.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = bow.name,
                style = frauncesDisplay(15.sp, italic = true).copy(color = AppInk),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = spec,
                style = jetbrainsMono(10.sp).copy(
                    color = AppInk3,
                    letterSpacing = 0.04.em,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "›",
            style = frauncesDisplay(18.sp, italic = true).copy(color = AppPond),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArrowRow(
    arrow: ArrowConfiguration,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val unitSystem = LocalUnitSystem.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("arrow_row_${arrow.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // ACTIVE stamp inline next to the label — matches iOS
            // ConfigurationView.swift:308-314. iOS hardcodes the stamp on
            // every ArrowRow ("active = most recently used; simplified"); we
            // do the same so the visual reads the same on both platforms.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = arrow.label,
                    style = frauncesDisplay(15.sp, italic = true).copy(color = AppInk),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                BPStamp(text = "ACTIVE", tone = BPStampTone.Pond)
            }
            Text(
                text = arrow.specSummary(unitSystem).uppercase(),
                style = jetbrainsMono(10.sp).copy(
                    color = AppInk3,
                    letterSpacing = 0.04.em,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "›",
            style = frauncesDisplay(18.sp, italic = true).copy(color = AppPond),
        )
    }
}

@Composable
private fun EmptyRow(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message.uppercase(),
            style = jetbrainsMono(10.sp).copy(
                color = AppInk3,
                letterSpacing = 0.04.em,
            ),
        )
    }
}

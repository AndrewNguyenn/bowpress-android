package com.andrewnguyen.bowpress.feature.equipment.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.UnitFormatting

/**
 * Top-level equipment screen — Bows / Arrows tabs, each with a FAB for `+` to add.
 * Mirrors iOS `ConfigurationView` but using material-style tabs since Compose
 * idioms prefer a tab row over separate list sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val fabAction: () -> Unit = if (selectedTab == 0) onAddBow else onAddArrow
    val fabLabel = if (selectedTab == 0) "Add bow" else "Add arrow"

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Equipment") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = fabAction,
                containerColor = BowPressColors.Accent,
                modifier = Modifier.testTag(if (selectedTab == 0) "add_bow_fab" else "add_arrow_fab"),
            ) { Icon(Icons.Default.Add, contentDescription = fabLabel) }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bows") },
                    modifier = Modifier.testTag("bows_tab"),
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Arrows") },
                    modifier = Modifier.testTag("arrows_tab"),
                )
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BowPressColors.Accent)
                }
                selectedTab == 0 -> BowList(
                    bows = state.bows,
                    onOpen = onOpenBow,
                    onDelete = viewModel::deleteBow,
                )
                else -> ArrowList(
                    arrows = state.arrows,
                    onOpen = onOpenArrow,
                    onDelete = viewModel::deleteArrow,
                )
            }
        }
    }
}

@Composable
private fun BowList(bows: List<Bow>, onOpen: (String) -> Unit, onDelete: (String) -> Unit) {
    if (bows.isEmpty()) {
        EmptyState("No bows yet")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(bows, key = { it.id }) { bow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(bow.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("bow_row_${bow.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(bow.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = bow.bowType.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDelete(bow.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${bow.name}")
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ArrowList(
    arrows: List<ArrowConfiguration>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (arrows.isEmpty()) {
        EmptyState("No arrow setups yet")
        return
    }
    val unitSystem = LocalUnitSystem.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(arrows, key = { it.id }) { arrow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(arrow.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(arrow.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${UnitFormatting.length(arrow.length, unitSystem)} · ${UnitFormatting.arrowMass(arrow.pointWeight, unitSystem)} point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDelete(arrow.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${arrow.label}")
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.Center) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

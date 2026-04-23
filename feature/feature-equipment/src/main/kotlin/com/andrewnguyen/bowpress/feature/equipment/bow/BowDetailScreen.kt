package com.andrewnguyen.bowpress.feature.equipment.bow

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.feature.equipment.components.ScoreBadge
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bow header + chronological list of tuning configurations. Tapping a history
 * row opens [BowConfigDetailScreen]; "Edit latest" opens [BowConfigEditScreen]
 * seeded from the most recent config. Matches iOS `BowDetailView`'s core
 * affordances while being simpler — we don't re-render the entire edit form
 * here because the Compose idiom puts that work in a dedicated sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BowDetailScreen(
    onBack: () -> Unit,
    onOpenConfig: (configId: String) -> Unit,
    onEditLatest: (configId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BowDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.bow?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteBow(); onBack() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete bow")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BowPressColors.Accent)
            }
            state.bow == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Bow not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> BowDetailBody(
                bow = state.bow!!,
                configurations = state.configurations,
                onOpenConfig = onOpenConfig,
                onEditLatest = {
                    viewModel.latestConfigId()?.let(onEditLatest)
                },
                onToggleReference = viewModel::setReference,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun BowDetailBody(
    bow: com.andrewnguyen.bowpress.core.model.Bow,
    configurations: List<BowConfiguration>,
    onOpenConfig: (String) -> Unit,
    onEditLatest: () -> Unit,
    onToggleReference: (configId: String, pinned: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Header panel.
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(bow.name, style = MaterialTheme.typography.headlineSmall)
            Text(bow.bowType.label, style = MaterialTheme.typography.bodyMedium, color = BowPressColors.Accent)
            if (bow.brand.isNotEmpty() || bow.model.isNotEmpty()) {
                val meta = listOf(bow.brand, bow.model).filter { it.isNotEmpty() }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        FilledTonalButton(
            onClick = onEditLatest,
            enabled = configurations.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("edit_latest_button"),
        ) { Text("Edit latest tune") }

        Spacer(Modifier.height(8.dp))
        SectionHeader("History")

        if (configurations.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No configurations yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(configurations, key = { it.id }) { config ->
                HistoryRow(
                    config = config,
                    onOpen = { onOpenConfig(config.id) },
                    onTogglePin = { onToggleReference(config.id, config.isReference != true) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryRow(
    config: BowConfiguration,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val day = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(config.createdAt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 12.dp)
            .testTag("history_row_${config.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onTogglePin) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = if (config.isReference == true) "Unpin reference" else "Pin as reference",
                tint = if (config.isReference == true) BowPressColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.label ?: day,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (config.label != null) {
                Text(day, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ScoreBadge(score = config.avgArrowScore, isReference = config.isReference == true)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

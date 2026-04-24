package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.Bow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHomeScreen(
    onSessionStarted: (sessionId: String) -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // If a session becomes active while this screen is visible, route to it. Keyed
    // on the session id so we don't loop on recomposition.
    val activeId = state.activeSession?.id
    LaunchedEffect(activeId) {
        if (activeId != null) onSessionStarted(activeId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Session") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text("Bow", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.bows.isEmpty()) {
                Text(
                    "No bows configured. Add one in Equipment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.bows, key = { it.id }) { bow ->
                        BowRow(bow = bow, isSelected = state.selectedBow?.id == bow.id) {
                            viewModel.selectBow(bow)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Arrows", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.arrowConfigs.isEmpty()) {
                Text(
                    "No arrow configs. Add one in Equipment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.arrowConfigs, key = { it.id }) { arrow ->
                        ArrowRow(arrow = arrow, isSelected = state.selectedArrow?.id == arrow.id) {
                            viewModel.selectArrow(arrow)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val bow = state.selectedBow ?: return@Button
                    val arrow = state.selectedArrow ?: return@Button
                    scope.launch { viewModel.startSession(bow, arrow) }
                },
                enabled = state.selectedBow != null && state.selectedArrow != null && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isLoading) "Starting…" else "Start Session")
            }

            state.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(err, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun BowRow(bow: Bow, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) BowPressColors.Accent else Color.Transparent
    val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(bow.name, style = MaterialTheme.typography.titleSmall, color = fg)
            Text(bow.bowType.label, style = MaterialTheme.typography.bodySmall, color = fg)
        }
    }
}

@Composable
private fun ArrowRow(arrow: ArrowConfiguration, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) BowPressColors.Accent else Color.Transparent
    val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitle = arrow.specSummary(LocalUnitSystem.current)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(arrow.label, style = MaterialTheme.typography.titleSmall, color = fg)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg)
        }
    }
}

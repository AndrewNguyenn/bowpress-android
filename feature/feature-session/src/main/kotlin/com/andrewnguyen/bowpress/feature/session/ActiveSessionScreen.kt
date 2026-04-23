package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    sessionId: String,
    onSessionEnded: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showConfigSheet by remember { mutableStateOf(false) }
    var showEndSheet by remember { mutableStateOf(false) }
    // Track whether we've ever seen the session as active so a transient null during
    // initial hydration doesn't bounce the user back to the home screen.
    var sawActive by remember { mutableStateOf(false) }
    if (state.activeSession != null) sawActive = true
    val justEnded = sawActive && state.activeSession == null
    LaunchedEffect(justEnded) {
        if (justEnded) onSessionEnded()
    }
    if (state.activeSession == null) {
        // First composition (flow hasn't delivered yet) or session just ended — render
        // nothing while the nav effect above takes us out.
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Session") },
                actions = {
                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Change config")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEndSheet = true },
                icon = { Icon(Icons.Default.Stop, contentDescription = null) },
                text = { Text("End Session") },
                containerColor = BowPressColors.Accent,
                contentColor = Color.White,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConfigHeader(
                bowName = state.selectedBow?.name ?: state.activeSession?.bowId.orEmpty(),
                configLabel = state.activeBowConfig?.label,
                arrowLabel = state.activeArrowConfig?.label,
                hasPending = state.hasPendingConfigChange,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                TargetPlot(
                    arrows = state.currentArrows,
                    onArrowPlotted = { plotX, plotY, ring, zone ->
                        scope.launch { viewModel.plotArrow(plotX, plotY, ring, zone) }
                    },
                    arrowDiameterMm = state.activeArrowConfig?.shaftDiameterEnum?.rawValue ?: 5.0,
                )
            }

            Text(
                "${state.currentArrows.size} arrow${if (state.currentArrows.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.currentArrows, key = { it.id }) { arrow ->
                    ArrowRow(arrow = arrow, onToggleExcluded = {
                        scope.launch { viewModel.togglePlotExcluded(arrow.id) }
                    })
                }
            }
        }
    }

    if (showConfigSheet) {
        SessionConfigSheet(
            state = state,
            onDismiss = { showConfigSheet = false },
            onConfirm = { bowConfigId, arrowConfigId ->
                viewModel.changeConfig(bowConfigId, arrowConfigId)
                showConfigSheet = false
            },
        )
    }

    if (showEndSheet) {
        EndSessionSheet(
            onDismiss = { showEndSheet = false },
            onFinish = { notes, feelTags ->
                scope.launch {
                    viewModel.endSession(notes, feelTags)
                    showEndSheet = false
                    onSessionEnded()
                }
            },
        )
    }
}

@Composable
private fun ConfigHeader(
    bowName: String,
    configLabel: String?,
    arrowLabel: String?,
    hasPending: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.fillMaxWidth().padding(end = 8.dp)) {
                Text(bowName, style = MaterialTheme.typography.titleSmall)
                configLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                arrowLabel?.let {
                    Text("→ $it", style = MaterialTheme.typography.bodySmall)
                }
                if (hasPending) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Config change pending — plot an arrow to confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BowPressColors.Accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrowRow(arrow: ArrowPlot, onToggleExcluded: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (arrow.ring == 11) "X" else arrow.ring.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                arrow.zone.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleExcluded) {
                Text(if (arrow.excluded) "Include" else "Flier")
            }
        }
    }
}

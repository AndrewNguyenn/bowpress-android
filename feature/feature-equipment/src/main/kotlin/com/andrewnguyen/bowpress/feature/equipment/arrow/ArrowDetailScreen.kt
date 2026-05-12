package com.andrewnguyen.bowpress.feature.equipment.arrow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.UnitToggle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrowDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArrowDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }
    LaunchedEffect(state.showSavedBanner) {
        if (state.showSavedBanner) {
            delay(2_000)
            viewModel.dismissSavedBanner()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.label.ifEmpty { "Arrow" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::delete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete arrow")
                    }
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = BowPressColors.Accent,
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                        )
                    } else {
                        TextButton(onClick = viewModel::save, enabled = state.canSave) {
                            Text("Save")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BowPressColors.Accent)
            }
            state.arrow == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Arrow not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                val unitSystem = LocalUnitSystem.current
                val setUnitSystem = LocalUnitSystemSetter.current
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                UnitToggle(
                    system = unitSystem,
                    onSystemChange = setUnitSystem,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                ArrowFormBody(
                    label = state.label, onLabel = viewModel::updateLabel,
                    brand = state.brand, onBrand = viewModel::updateBrand,
                    model = state.model, onModel = viewModel::updateModel,
                    length = state.length, onLength = viewModel::updateLength,
                    pointWeight = state.pointWeight, onPointWeight = viewModel::updatePointWeight,
                    fletchingType = state.fletchingType, onFletchingType = viewModel::updateFletchingType,
                    fletchingLength = state.fletchingLength, onFletchingLength = viewModel::updateFletchingLength,
                    fletchingOffset = state.fletchingOffset, onFletchingOffset = viewModel::updateFletchingOffset,
                    nockType = state.nockType, onNockType = viewModel::updateNockType,
                    shaftDiameter = state.shaftDiameter, onShaftDiameter = viewModel::updateShaftDiameter,
                    unitSystem = unitSystem,
                )
                if (state.showSavedBanner) {
                    Text("Saved", color = BowPressColors.Accent, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                state.errorMessage?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

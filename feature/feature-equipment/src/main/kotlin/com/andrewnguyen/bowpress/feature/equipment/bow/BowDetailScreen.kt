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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.UnitToggle
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.feature.equipment.components.ScoreBadge
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bow detail screen — matches iOS BowDetailView's structure: the editable Bow
 * Setup form is inlined directly here, followed by the tuning history list.
 * Tapping Save in the toolbar creates a new configuration (same semantics as
 * iOS `saveCurrentState`). Tapping a history row opens [BowConfigDetailScreen]
 * for a read-only view of that snapshot's diff against the current config.
 *
 * Two ViewModels back this screen:
 *  - [BowDetailViewModel] owns the bow's reactive config list + delete action.
 *  - [BowConfigEditViewModel] owns the editable form state. When this screen is
 *    composed via the `equipment/bow/{bowId}` route (no `configId` arg), the
 *    edit ViewModel seeds itself from the bow's latest config — see
 *    [BowConfigEditViewModel] init.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BowDetailScreen(
    onBack: () -> Unit,
    onOpenConfig: (configId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BowDetailViewModel = hiltViewModel(),
    editViewModel: BowConfigEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editState by editViewModel.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // After a Save round-trip the edit VM stamps savedConfigId. Acknowledge it so
    // the one-shot signal can't re-fire on later recompositions, then pop back —
    // matches iOS BowDetailView.saveCurrentState() which calls dismiss() after
    // the configuration is persisted.
    LaunchedEffect(editState.savedConfigId) {
        if (editState.savedConfigId != null) {
            editViewModel.acknowledgeSaved()
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.bow?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (editState.isSaving) {
                        CircularProgressIndicator(
                            color = BowPressColors.Accent,
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                        )
                    } else {
                        TextButton(
                            onClick = editViewModel::save,
                            enabled = !editState.isLoading && editState.bow != null,
                            modifier = Modifier.testTag("save_bow_button"),
                        ) { Text("Save") }
                    }
                    // iOS BowDetailView surfaces deletion as a destructive button at
                    // the bottom of the form (BowDetailView.swift:181-191), not a
                    // topbar action — the bottom row is rendered inside BowDetailBody.
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading || editState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = BowPressColors.Accent) }

            state.bow == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("Bow not found", color = MaterialTheme.colorScheme.onSurfaceVariant) }

            else -> BowDetailBody(
                bow = state.bow!!,
                configurations = state.configurations,
                editState = editState,
                editCallbacks = editViewModel.asCallbacks(),
                onOpenConfig = onOpenConfig,
                onToggleReference = viewModel::setReference,
                onDeleteRequested = { showDeleteConfirm = true },
                modifier = Modifier.padding(padding),
            )
        }
    }

    // Destructive confirmation — matches iOS BowDetailView's "Delete <bow>?"
    // alert (BowDetailView.swift:221-232). Without this, a single trash-icon
    // tap immediately purged the bow and its history.
    if (showDeleteConfirm) {
        val bowName = state.bow?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (bowName.isNotEmpty()) "Delete $bowName?" else "Delete bow?") },
            text = {
                Text("This permanently removes this bow along with its tuning history and shooting sessions. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteBow()
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BowDetailBody(
    bow: Bow,
    configurations: List<BowConfiguration>,
    editState: BowConfigEditViewModel.UiState,
    editCallbacks: BowConfigEditCallbacks,
    onOpenConfig: (String) -> Unit,
    onToggleReference: (configId: String, pinned: Boolean) -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // iOS BowDetailView renders bow.name only via .navigationTitle — there's
        // no separate header band repeating the name/type/brand. Android's
        // TopAppBar already shows the name; the form body starts with the unit
        // toggle, matching BowDetailView.swift:115-118.

        // iOS BowDetailView puts the unit toggle at the very top of the form,
        // above Bow Info (BowDetailView.swift:117). Render it here so the body
        // can suppress its own.
        UnitToggle(
            system = LocalUnitSystem.current,
            onSystemChange = LocalUnitSystemSetter.current,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Bow Info section — mirrors iOS BowDetailView (BowDetailView.swift:119-124).
        // Editable Name + readonly Type. iOS surfaces Name as editable but its
        // saveCurrentState() only persists the BowConfiguration and never touches
        // Bow.name — Android matches that surface exactly. If iOS later wires
        // bow persistence, mirror it here.
        BowInfoSection(
            bow = bow,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // Reference section — mirrors iOS BowDetailView.referenceSection
        // (BowDetailView.swift:248-302). Shows the pinned-config card if one
        // exists, and offers a "Pin current config as reference" affordance
        // for scoreable configs that aren't already the reference.
        ReferenceSection(
            configurations = configurations,
            onToggleReference = onToggleReference,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // Inline editable form — mirrors iOS BowDetailView's Setup / String &
        // Cable / Limbs / etc. sections. isSetup=true so the form renders
        // editable rows for Draw Length / Let-off / Peep / D-Loop (vs the
        // read-only Base Setup summary used when logging a subsequent tune).
        BowConfigEditFormBody(
            state = editState,
            bowType = bow.bowType,
            isSetup = true,
            callbacks = editCallbacks,
            unitSystem = LocalUnitSystem.current,
            onUnitSystemChange = LocalUnitSystemSetter.current,
            // iOS BowDetailView doesn't render the unit toggle or a Label section
            // inside the type-specific form — UnitToggle is already drawn above
            // Bow Info, and iOS leaves config labels to BowConfigEditView's
            // dedicated "Log Tuning" flow.
            showUnitToggle = false,
            showLabel = false,
        )

        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // iOS BowDetailView renders a destructive "Delete Bow" button below
            // History (BowDetailView.swift:181-191). Centered, error-tinted text.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDeleteRequested)
                    .padding(vertical = 14.dp)
                    .testTag("delete_bow_button"),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Delete Bow",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            SectionHeader("History")
            if (configurations.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No configurations yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                configurations.forEach { config ->
                    HistoryRow(
                        config = config,
                        onOpen = { onOpenConfig(config.id) },
                        onTogglePin = { onToggleReference(config.id, config.isReference != true) },
                    )
                    HorizontalDivider()
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BowInfoSection(
    bow: Bow,
    modifier: Modifier = Modifier,
) {
    var name by remember(bow.id) { mutableStateOf(bow.name) }
    Column(modifier = modifier) {
        SectionHeader("Bow Info")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("bow_info_name_field"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Type", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(
                bow.bowType.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/**
 * iOS BowDetailView.referenceSection port. Two stacked affordances:
 *  - If any config has `isReference == true`, render a "Reference" card with
 *    a star icon, the config label, the score-plus-source subtitle, and an
 *    Unpin button when the pin was manual (auto-selected pins don't expose
 *    Unpin because the analytics pipeline will replace them anyway).
 *  - If the latest config isn't the reference, render a "Pin current config
 *    as reference" button — guarded by the same scoreable / no-existing-ref
 *    predicate iOS uses, with a footer explaining the analytics anchor.
 */
@Composable
private fun ReferenceSection(
    configurations: List<BowConfiguration>,
    onToggleReference: (configId: String, pinned: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pinned = configurations.firstOrNull { it.isReference == true }
    val current = configurations.maxByOrNull { it.createdAt }
    val canOfferPin = current != null
        && current.isReference != true
        && (current.scoreable == true || pinned == null)

    if (pinned == null && !canOfferPin) return

    Column(modifier = modifier) {
        if (pinned != null) {
            SectionHeader("Reference")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = BowPressColors.Accent,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pinned.label ?: "Pinned configuration",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val source = if (pinned.referenceManuallyPinned == true) {
                        "Manually pinned"
                    } else {
                        "Auto-selected by analytics"
                    }
                    val subtitle = pinned.avgArrowScore?.let { score ->
                        "Score ${score.toInt()} / 100 · $source"
                    } ?: source
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (pinned.referenceManuallyPinned == true) {
                    TextButton(
                        onClick = { onToggleReference(pinned.id, false) },
                        modifier = Modifier.testTag("unpin_reference_button"),
                    ) {
                        Text("Unpin", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (canOfferPin && current != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleReference(current.id, true) }
                    .padding(vertical = 12.dp)
                    .testTag("pin_current_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = BowPressColors.Accent,
                )
                Text(
                    text = "Pin current config as reference",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "Analytics comparisons anchor to your reference. Unpinning lets the pipeline auto-select the highest-scoring config after each session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.designsystem.coursemap.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

/**
 * The live 3D-course screen — the GPS-traced ink map, the editable station
 * list, and Shoot / Finish actions. Mirrors iOS `ThreeDCourseView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDCourseScreen(
    onCourseEnded: () -> Unit,
    viewModel: ThreeDCourseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val breadcrumb by viewModel.locationTracker.breadcrumb.collectAsStateWithLifecycle()
    val current by viewModel.locationTracker.current.collectAsStateWithLifecycle()
    val unitSystem = LocalUnitSystem.current
    val context = LocalContext.current

    // Once the session row is gone (finished or discarded), leave the screen.
    LaunchedEffect(state.session) {
        if (state.session == null) onCourseEnded()
    }

    // Ask for location the first time the course screen appears.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // The tracker is already started by the view model; a late grant just
        // needs to re-arm GPS registration.
        if (granted) viewModel.locationTracker.ensureLocationUpdates()
    }
    LaunchedEffect(Unit) {
        if (!viewModel.locationTracker.hasLocationPermission()) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var shooting by remember { mutableStateOf(false) }
    var shootStationId by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<com.andrewnguyen.bowpress.core.model.CourseStation?>(null) }
    var finishing by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    if (shooting) {
        ThreeDShootFlow(
            stationId = shootStationId,
            viewModel = viewModel,
            onDone = { shooting = false },
        )
        return
    }
    if (finishing) {
        ThreeDFinishReview(
            state = state,
            breadcrumb = breadcrumb,
            onSign = { viewModel.finishCourse() },
            onBack = { finishing = false },
        )
        return
    }
    editing?.let { station ->
        ThreeDStationEditSheet(
            station = station,
            system = state.scoringSystem,
            unitSystem = unitSystem,
            onSave = { ring, px, py, scene, arrow ->
                viewModel.updateStation(station, ring, px, py, scene, arrow)
                editing = null
            },
            onDelete = {
                viewModel.deleteStation(station)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        BPNavHeader(
            eyebrow = "BOWPRESS · 3D COURSE",
            title = state.session?.title?.takeIf { it.isNotBlank() } ?: "On the course",
            meta = {
                Text(
                    "${state.stations.size} STA · ${state.totalScore} PTS",
                    style = jetbrainsMono(10.sp, FontWeight.Medium).copy(color = AppInk),
                )
            },
        )

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CourseInkMapView(
                    stations = state.stations,
                    breadcrumb = breadcrumb,
                    current = current,
                    elevationGrid = state.elevationGrid,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { BPEyebrow("STATIONS") }
            if (state.stations.isEmpty()) {
                item {
                    Text(
                        "No stations yet — range your first target and shoot.",
                        style = frauncesDisplay(13.sp, italic = true).copy(color = AppInk3),
                    )
                }
            }
            items(state.stations, key = { it.id }) { station ->
                Box(Modifier.clickable { editing = station }) {
                    CourseStationRow(
                        station = station,
                        system = state.scoringSystem,
                        unitSystem = unitSystem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppLine),
                    )
                }
            }
        }

        Column(
            Modifier
                .background(AppPaper)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BPPrimaryButton(
                title = "Shoot station ${state.nextStationNumber}",
                subtitle = "RANGE · ANGLE · PLOT",
                onClick = {
                    shootStationId = viewModel.newStationId()
                    shooting = true
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryAction("Discard", AppMaple, Modifier.weight(1f)) { confirmDiscard = true }
                SecondaryAction("Finish course", AppPondDk, Modifier.weight(1f)) { finishing = true }
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard this course?",
            body = "The walk and every station are deleted. This can't be undone.",
            confirm = "Discard",
            onConfirm = { confirmDiscard = false; viewModel.discardCourse() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun SecondaryAction(label: String, tone: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(AppPaper2)
            .border(1.dp, tone)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = interUI(12.sp, FontWeight.SemiBold).copy(color = tone))
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium)) },
        text = { Text(body, style = interUI(13.sp).copy(color = AppInk)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = AppPaper,
    )
}

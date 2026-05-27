package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.designsystem.coursemap.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import com.andrewnguyen.bowpress.core.model.UnitSystem
import java.io.ByteArrayOutputStream
import kotlin.math.hypot

/**
 * The two-step shoot flow for one 3D station — capture (compose the shot,
 * read its incline angle + compass bearing, range the distance) then plot
 * (place the arrow on the circular target and score it). Mirrors iOS
 * `ThreeDShootFlowView`.
 */
@Composable
fun ThreeDShootFlow(
    stationId: String,
    viewModel: ThreeDCourseViewModel,
    onDone: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }            // 0 = capture, 1 = plot
    var angle by remember { mutableStateOf(0.0) }
    var bearing by remember { mutableStateOf<Double?>(null) }
    var distance by remember { mutableStateOf<Double?>(null) }
    var unit by remember { mutableStateOf("yd") }
    var hasScene by remember { mutableStateOf(false) }

    if (step == 0) {
        ThreeDCaptureScreen(
            stationId = stationId,
            viewModel = viewModel,
            onCancel = onDone,
            onCaptured = { capturedAngle, capturedBearing, rangedDistance, distanceUnit, sceneTaken ->
                angle = capturedAngle
                bearing = capturedBearing
                distance = rangedDistance
                unit = distanceUnit
                hasScene = sceneTaken
                step = 1
            },
        )
    } else {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ThreeDPlotScreen(
            stationId = stationId,
            system = state.scoringSystem,
            distance = distance,
            unit = unit,
            angle = angle,
            hasScenePhoto = hasScene,
            onBack = { step = 0 },
            onSave = { ring, plotX, plotY, hasArrow ->
                val here = viewModel.currentLocation.value
                viewModel.commitStation(
                    id = stationId,
                    estimatedDistance = distance,
                    distanceUnit = unit,
                    angleDegrees = angle,
                    bearingDegrees = bearing,
                    latitude = here?.latitude,
                    longitude = here?.longitude,
                    ring = ring,
                    plotX = plotX,
                    plotY = plotY,
                    hasScenePhoto = hasScene,
                    hasArrowPhoto = hasArrow,
                )
                onDone()
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Step 1 — capture
// ---------------------------------------------------------------------------

@Composable
private fun ThreeDCaptureScreen(
    stationId: String,
    viewModel: ThreeDCourseViewModel,
    onCancel: () -> Unit,
    onCaptured: (angle: Double, bearing: Double?, distance: Double?, unit: String, sceneTaken: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveAngle by viewModel.angleDegrees.collectAsStateWithLifecycle()
    val liveHeading by viewModel.heading.collectAsStateWithLifecycle()

    var photoTaken by remember { mutableStateOf(false) }
    var frozenAngle by remember { mutableStateOf(0.0) }
    var frozenBearing by remember { mutableStateOf<Double?>(null) }
    var manualAngle by remember { mutableStateOf(0) }
    var distanceText by remember { mutableStateOf("") }

    // Live in-app viewfinder (CameraX) when the camera permission is granted
    // and the device has a camera; otherwise the system-camera intent.
    val cameraController = remember { BowPressCameraController() }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }
    LaunchedEffect(Unit) {
        if (!cameraGranted) cameraPermLauncher.launch(android.Manifest.permission.CAMERA)
    }
    val useLiveCamera = cameraGranted && BowPressCameraController.deviceHasCamera(context)

    // Store the scene JPEG + freeze the shot telemetry at the shutter (iOS parity).
    fun onSceneCaptured(bytes: ByteArray) {
        CourseStationPhotoStore.save(context, bytes, stationId, CourseStationPhotoStore.Slot.SCENE)
        frozenAngle = if (viewModel.isAngleSensorAvailable) liveAngle else manualAngle.toDouble()
        frozenBearing = liveHeading
        photoTaken = true
    }

    // System-camera fallback — no in-app camera available.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val bytes = ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)
            }.toByteArray()
            onSceneCaptured(bytes)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        ShootHeader("STEP 1 OF 2 — RANGE THE TARGET", onCancel)

        // The live camera feed (or a dark placeholder), with the crosshair +
        // angle HUD overlaid — "just the camera with our circle and dot".
        Box(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(280.dp)
                .background(AppInk)
                .border(1.dp, AppLine),
            contentAlignment = Alignment.Center,
        ) {
            if (useLiveCamera) {
                CameraViewfinder(
                    controller = cameraController,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, AppPaper.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(AppMaple))
            }
            Column(Modifier.align(Alignment.TopStart).padding(10.dp)) {
                val hud = if (viewModel.isAngleSensorAvailable) {
                    "● GYRO ${AngleFormatting.signed(liveAngle)}"
                } else {
                    "○ MANUAL ${AngleFormatting.signed(manualAngle.toDouble())}"
                }
                Text(hud, style = jetbrainsMono(10.sp).copy(color = AppPaper))
                liveHeading?.let {
                    Text(
                        BearingFormatting.compass(it),
                        style = jetbrainsMono(10.sp).copy(color = AppPaper.copy(alpha = 0.7f)),
                    )
                }
                if (photoTaken) {
                    Text("✓ SCENE SAVED", style = jetbrainsMono(10.sp).copy(color = AppMaple))
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!viewModel.isAngleSensorAvailable) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BPEyebrow("ANGLE")
                    Stepper("−") { manualAngle-- }
                    Text(
                        AngleFormatting.signed(manualAngle.toDouble()),
                        style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
                    )
                    Stepper("+") { manualAngle++ }
                }
            }

            if (!photoTaken) {
                BPPrimaryButton(
                    title = "Capture the shot",
                    subtitle = "FREEZES ANGLE + BEARING",
                    onClick = {
                        if (useLiveCamera) {
                            scope.launch {
                                cameraController.capture(context)?.let { onSceneCaptured(it) }
                            }
                        } else {
                            cameraLauncher.launch(null)
                        }
                    },
                )
            } else {
                BPEyebrow("RANGED DISTANCE")
                Text(
                    if (distanceText.isEmpty()) "—" else distanceText,
                    style = frauncesDisplay(34.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
                )
                DistanceKeypad(
                    text = distanceText,
                    onTextChange = { distanceText = it },
                )
                BPPrimaryButton(
                    title = "Save & plot the arrow",
                    subtitle = "STEP 2 — SCORE THE STATION",
                    onClick = {
                        onCaptured(
                            frozenAngle,
                            frozenBearing,
                            distanceText.toDoubleOrNull(),
                            "yd",
                            true,
                        )
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .background(AppPaper2)
            .border(1.dp, AppLine)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk2))
    }
}

/** 3×4 calculator-style keypad — whole digits plus a single decimal tenth. */
@Composable
private fun DistanceKeypad(text: String, onTextChange: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .background(AppPaper2)
                            .border(1.dp, AppLine)
                            .clickable {
                                onTextChange(applyKey(text, key))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            key,
                            style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
                        )
                    }
                }
            }
        }
    }
}

private fun applyKey(current: String, key: String): String = when (key) {
    "⌫" -> current.dropLast(1)
    "." -> if (current.contains(".") || current.isEmpty()) current else "$current."
    else -> {
        val dot = current.indexOf('.')
        when {
            dot >= 0 && current.length - dot > 2 -> current        // already a tenth
            dot < 0 && current.replace(".", "").length >= 3 -> current  // 3 whole digits
            else -> current + key
        }
    }
}

// ---------------------------------------------------------------------------
// Step 2 — plot
// ---------------------------------------------------------------------------

@Composable
private fun ThreeDPlotScreen(
    stationId: String,
    system: ThreeDScoringSystem,
    distance: Double?,
    unit: String,
    angle: Double,
    hasScenePhoto: Boolean,
    onBack: () -> Unit,
    onSave: (ring: Int, plotX: Double?, plotY: Double?, hasArrowPhoto: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lens = remember { CircleLensController() }
    var plotX by remember { mutableStateOf<Double?>(null) }
    var plotY by remember { mutableStateOf<Double?>(null) }
    var ring by remember { mutableStateOf<Int?>(null) }
    var isMiss by remember { mutableStateOf(false) }
    var hasArrowPhoto by remember { mutableStateOf(false) }

    val arrowCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val bytes = ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)
            }.toByteArray()
            CourseStationPhotoStore.save(context, bytes, stationId, CourseStationPhotoStore.Slot.ARROW)
            hasArrowPhoto = true
        }
    }

    Box(Modifier.fillMaxSize().background(AppPaper)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ShootHeader("STEP 2 OF 2 — ${system.label.uppercase()}", onBack)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${DistanceFormatting.short(distance, unit, UnitSystem.IMPERIAL)} · ${AngleFormatting.signed(angle)}",
                    style = jetbrainsMono(11.sp).copy(color = AppInk3),
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircleTargetView(
                        system = system,
                        arrows = arrowList(stationId, plotX, plotY),
                        showLabels = true,
                        onPlot = { x, y ->
                            plotX = x; plotY = y; isMiss = false
                            ring = system.ringForNormalizedRadius(hypot(x, y).coerceAtMost(1.0))
                        },
                        onLensSnapshotChanged = { lens.snapshot = it },
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BPEyebrow("SCORE")
                    Spacer(Modifier.weight(1f))
                    MissToggle(isMiss) {
                        isMiss = !isMiss
                        if (isMiss) { ring = 0; plotX = null; plotY = null }
                    }
                }
                ScoreChipRow(
                    system = system,
                    selected = if (isMiss) 0 else ring,
                    onSelect = { ring = it; isMiss = it == 0; if (it == 0) { plotX = null; plotY = null } },
                )
                SecondaryButton(
                    if (hasArrowPhoto) "Arrow photo saved ✓" else "Add arrow photo",
                ) { arrowCamera.launch(null) }
                BPPrimaryButton(
                    title = "Save & return to map",
                    subtitle = "STATION SCORED",
                    enabled = ring != null,
                    onClick = { onSave(ring ?: 0, plotX, plotY, hasArrowPhoto) },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
        CircleLensOverlay(lens)
    }
}

@Composable
private fun MissToggle(on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (on) AppMaple else AppPaper2)
            .border(1.dp, AppMaple)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            "MISS",
            style = interUI(10.sp, FontWeight.SemiBold).copy(color = if (on) AppPaper else AppMaple),
        )
    }
}

// ---------------------------------------------------------------------------
// Edit sheet — re-score / re-photo / delete an already-logged station
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDStationEditSheet(
    station: CourseStation,
    system: ThreeDScoringSystem,
    unitSystem: UnitSystem,
    onSave: (ring: Int, plotX: Double?, plotY: Double?, hasScene: Boolean, hasArrow: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var plotX by remember { mutableStateOf(station.plotX) }
    var plotY by remember { mutableStateOf(station.plotY) }
    var ring by remember { mutableStateOf(station.ring) }
    var isMiss by remember { mutableStateOf(station.ring == 0) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppPaper) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Edit station ${station.stationNumber}",
                style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
            )
            Text(
                "${DistanceFormatting.short(station.estimatedDistance, station.distanceUnit, unitSystem)} · ${AngleFormatting.signed(station.angleDegrees)}",
                style = jetbrainsMono(11.sp).copy(color = AppInk3),
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircleTargetView(
                    system = system,
                    arrows = arrowList(station.id, plotX, plotY),
                    onPlot = { x, y ->
                        plotX = x; plotY = y; isMiss = false
                        ring = system.ringForNormalizedRadius(hypot(x, y).coerceAtMost(1.0))
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BPEyebrow("SCORE")
                Spacer(Modifier.weight(1f))
                MissToggle(isMiss) {
                    isMiss = !isMiss
                    if (isMiss) { ring = 0; plotX = null; plotY = null }
                }
            }
            ScoreChipRow(
                system = system,
                selected = if (isMiss) 0 else ring,
                onSelect = { ring = it; isMiss = it == 0; if (it == 0) { plotX = null; plotY = null } },
            )
            BPPrimaryButton(
                title = "Save changes",
                onClick = { onSave(ring, plotX, plotY, station.hasScenePhoto, station.hasArrowPhoto) },
            )
            SecondaryButton("Delete station", tone = AppMaple, onClick = onDelete)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Shared
// ---------------------------------------------------------------------------

@Composable
private fun ShootHeader(eyebrow: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppPaper)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(eyebrow, style = interUI(10.sp, FontWeight.SemiBold).copy(color = AppInk3))
        Text(
            "CANCEL",
            style = interUI(10.sp, FontWeight.SemiBold).copy(color = AppMaple),
            modifier = Modifier.clickable(onClick = onBack),
        )
    }
}

@Composable
private fun SecondaryButton(label: String, tone: Color = AppPondDk, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .border(1.dp, tone)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = interUI(12.sp, FontWeight.SemiBold).copy(color = tone))
    }
}

private fun arrowList(id: String, x: Double?, y: Double?): List<CircleArrow> =
    if (x != null && y != null) listOf(CircleArrow(id, x, y)) else emptyList()

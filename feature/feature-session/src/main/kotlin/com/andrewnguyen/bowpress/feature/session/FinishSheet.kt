package com.andrewnguyen.bowpress.feature.session

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.photo.TargetPhotoStore
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationTagPicker
import com.andrewnguyen.bowpress.feature.social.ui.photo.PendingCropImage
import com.andrewnguyen.bowpress.feature.social.ui.photo.PhotoCropMode
import com.andrewnguyen.bowpress.feature.social.ui.photo.PhotoCropperHost
import com.andrewnguyen.bowpress.feature.social.ui.photo.PhotoCropperLaunch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// ── Finish flow types ─────────────────────────────────────────────────────────

/**
 * Hero/meta-strip shape — range vs 3D course. The rest of the sheet
 * chrome is shared. Mirrors iOS `FinishMode`.
 */
sealed interface FinishMode {
    data class Range(
        val score: Int,
        val xCount: Int,
        val arrowCount: Int,
        val endCount: Int,
    ) : FinishMode

    data class Course(
        val totalScore: Int,
        val killCount: Int,
        val stationCount: Int,
        val averagePerTarget: Double,
    ) : FinishMode
}

/** Max length of the description field — matches iOS `FinishSheet.maxDescription`. */
internal const val FINISH_DESCRIPTION_MAX = 1000

/**
 * Max media slots per finish-sheet post — matches iOS `maxMediaSlots = 4`.
 * Bumped from 3 to match the 2×2 strip capacity so a 4-photo grid renders
 * cleanly without a blank tile. Android has no video yet, so the slot pool
 * is photos-only here; iOS shares the pool with one video.
 */
internal const val FINISH_PHOTOS_MAX = 4

// ── FinishSheet (replaces the legacy EndSessionSheet alert) ──────────────────

/**
 * The C1 bottom-sheet finish flow. Captures title / description / range /
 * photos / audience and hands them back to the caller via [onFinish] +
 * [FinishExtras]. Mirrors iOS `FinishSheet.swift` (~600 LOC) — the
 * structural surface is the same; the iOS-only `Mantis` / `MKLocalSearch`
 * pieces are bridged to uCrop (PhotoCropperHost) and the
 * existing Android LocationTagPicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishSheet(
    mode: FinishMode,
    bowName: String,
    arrowSummary: String?,
    isPosting: Boolean,
    initialTitle: String,
    initialDescription: String,
    initialLocation: SessionLocation?,
    onFinish: (FinishExtras) -> Unit,
    onDiscard: () -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Draft state ─────────────────────────────────────────────────────────

    // Single sheet-open timestamp threaded through the Hero, the title seed,
    // and the title placeholder — mirrors iOS, which carries one `endedAt`
    // through all three so the time-of-day bucket can't disagree across them.
    val endedAt = remember { Instant.now() }

    // Pre-fill the title with the time-of-day suggestion when the session is
    // untitled — matches iOS, where `presentFinishSheet` seeds the draft with
    // `timeOfDaySuggestion(for:)` so an unedited finish saves e.g. "Afternoon
    // shooting session" rather than an empty title. The placeholder below
    // still shows the same suggestion if the archer clears the field.
    var title by remember {
        mutableStateOf(initialTitle.ifBlank { timeOfDaySuggestion(endedAt) })
    }
    var description by remember { mutableStateOf(initialDescription) }
    var audience by remember { mutableStateOf(FinishAudience.Public) }
    var location by remember { mutableStateOf(initialLocation) }
    // Per-photo downscaled bytes — same shape as iOS `FinishDraft.photos`.
    val photos = remember { mutableStateOf<List<ByteArray>>(emptyList()) }
    val thumbnails = remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }

    var showLocationPicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    // FIFO of photos waiting for a crop step after a multi-pick.
    // `currentCrop` is what actually drives `PhotoCropperHost` so the
    // launcher rearms on each new pending image (mirrors iOS's
    // `.fullScreenCover(item: $currentCrop)` re-present trick).
    val cropQueue = remember { androidx.compose.runtime.mutableStateListOf<PendingCropImage>() }
    var currentCrop by remember { mutableStateOf<PendingCropImage?>(null) }

    // System multi-pick photo picker. Picked URIs are enqueued into
    // `cropQueue`; the host below opens uCrop one photo at a time
    // with the SessionMedia ratio picker (4:5 default, plus 1:1 /
    // 4:3 / 3:2 / 16:9 — mirrors iOS's CropperView bounds). The
    // cropped result then decodes + downscales onto `photos.value`,
    // and the next photo in the queue becomes `currentCrop`.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(FINISH_PHOTOS_MAX),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val room = (FINISH_PHOTOS_MAX - photos.value.size).coerceAtLeast(0)
        if (room == 0) return@rememberLauncherForActivityResult
        cropQueue.addAll(uris.take(room).map { PendingCropImage(source = it) })
        if (currentCrop == null) {
            currentCrop = cropQueue.firstOrNull()
        }
    }

    // Drains the cropper queue. Each cropped URI decodes +
    // downscales onto `photos.value`; on cancel the photo is
    // skipped. After each crop completes the next pending image
    // promotes to `currentCrop` (via a recomposition tick driven by
    // the state change), and `PhotoCropperHost`'s id-keyed
    // LaunchedEffect re-fires the launcher.
    PhotoCropperHost(
        launch = PhotoCropperLaunch(image = currentCrop, mode = PhotoCropMode.SessionMedia),
    ) { croppedUri ->
        currentCrop = null
        if (cropQueue.isNotEmpty()) cropQueue.removeAt(0)
        if (croppedUri != null && photos.value.size < FINISH_PHOTOS_MAX) {
            scope.launch {
                val (bytes, thumb) = decodeAndDownscale(context, croppedUri)
                if (bytes != null && thumb != null && photos.value.size < FINISH_PHOTOS_MAX) {
                    photos.value = photos.value + bytes
                    thumbnails.value = thumbnails.value + thumb
                }
            }
        }
        // Promote the next pending image on the next composition.
        currentCrop = cropQueue.firstOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = AppPaper,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            HeaderRow(onClose = onClose, isPosting = isPosting)

            Hero(
                mode = mode,
                endedAt = endedAt,
            )

            MetaStrip(mode = mode, bowName = bowName, arrowSummary = arrowSummary)

            SectionField(label = "TITLE") {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    textStyle = frauncesDisplay(22.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = AppInk),
                    cursorBrush = SolidColor(AppPondDk),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text(
                                text = timeOfDaySuggestion(endedAt),
                                style = frauncesDisplay(22.sp, italic = true)
                                    .copy(color = AppInk3),
                            )
                        }
                        inner()
                    },
                )
            }

            SectionField(label = "DESCRIPTION", optional = true) {
                BasicTextField(
                    value = description,
                    onValueChange = {
                        description = if (it.length > FINISH_DESCRIPTION_MAX) {
                            it.substring(0, FINISH_DESCRIPTION_MAX)
                        } else {
                            it
                        }
                    },
                    textStyle = frauncesDisplay(14.5.sp, italic = true)
                        .copy(color = AppInk),
                    cursorBrush = SolidColor(AppPondDk),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (description.isEmpty()) {
                            Text(
                                text = "Add description · @mention friends who shot with you.",
                                style = frauncesDisplay(14.5.sp, italic = true)
                                    .copy(color = AppInk3),
                            )
                        }
                        inner()
                    },
                )
            }

            SectionField(label = "RANGE", optional = true) {
                LocationRow(
                    location = location,
                    onClick = { showLocationPicker = true },
                )
                if (location != null) {
                    Spacer(Modifier.height(6.dp))
                    ClearLocationLink(onClear = { location = null })
                }
            }

            // `used` in the label so the cap is visually obvious as soon
            // as the archer picks one. Keeps the literal "optional" while
            // empty (the capacity meter is hidden in that state anyway,
            // and showing "0/4" alongside "optional" was redundant).
            val used = photos.value.size
            SectionField(
                label = "ADD PHOTO / VIDEO",
                optional = used == 0,
                optionalSuffix = if (used == 0) null else "$used/$FINISH_PHOTOS_MAX",
            ) {
                PhotoGrid(
                    thumbnails = thumbnails.value,
                    canAdd = photos.value.size < FINISH_PHOTOS_MAX,
                    onAddClick = {
                        photoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    onRemoveAt = { idx ->
                        photos.value = photos.value.toMutableList().also { it.removeAt(idx) }
                        thumbnails.value = thumbnails.value.toMutableList().also { it.removeAt(idx) }
                    },
                )
                if (used > 0) PhotoCapacityMeter(used = used)
            }

            SectionField(label = "AUDIENCE", emphasizedSuffix = "WHO SEES THIS") {
                AudienceRow(
                    selected = audience,
                    onSelect = { audience = it },
                )
            }

            Spacer(Modifier.height(14.dp))

            BPPrimaryButton(
                title = audience.primaryTitle,
                subtitle = audience.primarySubtitle.uppercase(),
                enabled = !isPosting,
                onClick = {
                    onFinish(
                        FinishExtras(
                            title = title.trim(),
                            description = description.trim(),
                            audience = audience,
                            location = location,
                            photoData = photos.value,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "DISCARD ›",
                    style = interUI(9.sp, weight = FontWeight.SemiBold)
                        .copy(letterSpacing = 0.22.em, color = AppMaple),
                    modifier = Modifier
                        .clickable { showDiscardConfirm = true }
                        .padding(vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // Cropper host removed — session-finish photos now skip the cropper
    // (iOS parity). The Edit sheet still has its own PhotoCropperHost
    // for users who want to crop after sharing.

    // C3 — auto-tag the nearest archery range (with reverse-geocode fallback)
    // as soon as the sheet mounts with an empty location. The resolver
    // self-races against a manual pick by re-checking `location == null` at
    // assignment time, mirroring iOS `FinishDraft.autoTagNearestRange`.
    AutoTagNearestRange(initialLocation = location) { resolved ->
        if (location == null) location = resolved
    }

    if (showLocationPicker) {
        LocationTagPicker(
            initial = location,
            onSave = { location = it },
            onRemove = { location = null },
            onDismiss = { showLocationPicker = false },
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this session?") },
            text = { Text("All arrows logged will be permanently deleted. This session won't appear in your log or analytics.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onDiscard() }) {
                    Text("Discard", color = AppMaple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Auto-tag effect ──────────────────────────────────────────────────────────

/**
 * One-shot launch effect that populates an empty initial location with the
 * NearestRangeFinder auto-tag. Mirrors iOS `FinishDraft.autoTagNearestRange`
 * — silent-skip on missing permission, missing fix, or no POI inside the
 * 0.25-mile radius. A no-op when the archer has already tagged a range so a
 * slow geocode can't blow away a fresh manual pick.
 */
@Composable
fun AutoTagNearestRange(
    initialLocation: SessionLocation?,
    onResolved: (SessionLocation) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(initialLocation == null) {
        if (initialLocation != null) return@LaunchedEffect
        val found = NearestRangeFinder.nearestArcheryRangeOrFallback(context)
        if (found != null) onResolved(found)
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HeaderRow(onClose: () -> Unit, isPosting: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "SESSION COMPLETE",
            style = interUI(9.5.sp, weight = FontWeight.SemiBold)
                .copy(letterSpacing = 0.26.em, color = AppPondDk),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "· Save to log",
            style = frauncesDisplay(13.sp, italic = true).copy(color = AppInk3),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "CLOSE ›",
            style = interUI(9.5.sp, weight = FontWeight.SemiBold)
                .copy(letterSpacing = 0.22.em, color = AppInk3),
            modifier = Modifier
                .clickable(enabled = !isPosting) { onClose() }
                .padding(4.dp),
        )
    }
}

// ── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun Hero(mode: FinishMode, endedAt: Instant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = heroEyebrow(mode),
                style = interUI(9.sp, weight = FontWeight.SemiBold)
                    .copy(letterSpacing = 0.24.em, color = AppInk3),
            )
            Spacer(Modifier.height(4.dp))
            HeroScore(mode = mode)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = remember(endedAt) { hhmm(endedAt) },
                style = jetbrainsMono(10.sp).copy(color = AppInk3),
            )
            Text(
                text = "just now",
                style = jetbrainsMono(10.sp).copy(color = AppInk3),
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppLine))
}

private fun heroEyebrow(mode: FinishMode): String = when (mode) {
    is FinishMode.Range -> "Range · ${mode.endCount} ends"
    is FinishMode.Course -> "3D Course · ${mode.stationCount} stations"
}

@Composable
private fun HeroScore(mode: FinishMode) {
    Row(verticalAlignment = Alignment.Bottom) {
        when (mode) {
            is FinishMode.Range -> {
                Text(
                    text = "${mode.score}",
                    style = frauncesDisplay(54.sp, italic = false, weight = FontWeight.Medium)
                        .copy(color = AppInk),
                )
                Text(
                    text = " / ${mode.arrowCount * 10}",
                    style = frauncesDisplay(22.sp, italic = true).copy(color = AppInk3),
                )
            }
            is FinishMode.Course -> {
                Text(
                    text = "${mode.totalScore}",
                    style = frauncesDisplay(54.sp, italic = false, weight = FontWeight.Medium)
                        .copy(color = AppInk),
                )
                Text(
                    text = " · %.1f avg".format(mode.averagePerTarget),
                    style = frauncesDisplay(20.sp, italic = true).copy(color = AppInk2),
                )
            }
        }
    }
}

// ── Meta strip ───────────────────────────────────────────────────────────────

@Composable
private fun MetaStrip(mode: FinishMode, bowName: String, arrowSummary: String?) {
    val parts = buildList {
        when (mode) {
            is FinishMode.Range -> {
                add("${mode.endCount} ends")
                add("${mode.arrowCount} arrows")
                add("${mode.xCount} X")
            }
            is FinishMode.Course -> {
                add("${mode.stationCount} stations")
                add("${mode.killCount} kills")
            }
        }
        add(bowName)
        arrowSummary?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        parts.forEachIndexed { idx, part ->
            Text(
                text = part,
                style = jetbrainsMono(10.5.sp).copy(color = AppInk2),
            )
            if (idx < parts.lastIndex) {
                Text(
                    text = "·",
                    style = jetbrainsMono(10.5.sp).copy(color = AppInk3),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppLine))
}

// ── Section helper ───────────────────────────────────────────────────────────

@Composable
private fun SectionField(
    label: String,
    optional: Boolean = false,
    emphasizedSuffix: String? = null,
    /**
     * Replaces the literal "optional" trailing text when set. Used by the
     * media field to surface the running count ("0/4", "3/4") inline with
     * the label so the cap stops being a silent picker rule. Null falls
     * back to the [optional] boolean's plain "optional" affordance.
     */
    optionalSuffix: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = interUI(9.5.sp, weight = FontWeight.SemiBold)
                        .copy(letterSpacing = 0.24.em, color = AppInk3),
                )
                if (emphasizedSuffix != null) {
                    Text(
                        text = " · $emphasizedSuffix",
                        style = interUI(9.5.sp, weight = FontWeight.SemiBold)
                            .copy(letterSpacing = 0.24.em, color = AppPondDk),
                    )
                }
            }
            when {
                optionalSuffix != null -> Text(
                    text = optionalSuffix,
                    style = frauncesDisplay(11.5.sp, italic = true).copy(color = AppInk3),
                )
                optional -> Text(
                    text = "optional",
                    style = frauncesDisplay(11.5.sp, italic = true).copy(color = AppInk3),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        content()
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppLine2))
}

/**
 * 4-square capacity meter — pond-dark squares for filled photo slots,
 * paper-2 outline for empty slots. Surfaces the [FINISH_PHOTOS_MAX] cap
 * without forcing the archer to count tiles. Mirrors iOS' meter; Android
 * has no video slot so the maple square the iOS design uses for the
 * video slot isn't rendered here. Skipped entirely when the grid is
 * empty (no signal yet) and when the section is collapsed.
 */
@Composable
private fun PhotoCapacityMeter(used: Int) {
    val room = (FINISH_PHOTOS_MAX - used).coerceAtLeast(0)
    Row(
        modifier = Modifier.padding(top = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(FINISH_PHOTOS_MAX) { idx ->
                val on = idx < used
                Box(
                    Modifier
                        .size(9.dp)
                        .background(if (on) AppPondDk else AppPaper2)
                        .border(1.dp, if (on) AppPondDk else AppLine),
                )
            }
        }
        Text(
            text = "$used of $FINISH_PHOTOS_MAX slots · ${if (room == 0) "full" else "$room left"}",
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
        )
    }
}

// ── Location row + clear link (C3) ───────────────────────────────────────────

@Composable
private fun LocationRow(
    location: SessionLocation?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(TestTags.SessionNotesLocationRow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (location == null) Icons.Default.LocationOff else Icons.Default.Place,
            contentDescription = null,
            tint = if (location == null) AppInk3 else AppInk2,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location?.name ?: "Tag a range",
                style = frauncesDisplay(15.sp, italic = true)
                    .copy(color = if (location == null) AppInk3 else AppInk),
                maxLines = 1,
            )
            if (location != null) {
                Text(
                    text = "TAP TO CHANGE",
                    style = jetbrainsMono(9.5.sp).copy(color = AppInk3),
                )
            }
        }
        Box(
            modifier = Modifier
                .background(AppPaper2)
                .border(1.dp, AppPondDk)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Text(
                text = if (location == null) "ADD" else "EDIT",
                style = interUI(9.sp, weight = FontWeight.SemiBold)
                    .copy(letterSpacing = 0.22.em, color = AppPondDk),
            )
        }
        Text(
            text = " ›",
            style = frauncesDisplay(16.sp, italic = true).copy(color = AppInk3),
        )
    }
}

@Composable
private fun ClearLocationLink(onClear: () -> Unit) {
    // C3 — explicit opt-out maple link under the location row. Hidden when
    // nothing is tagged. Mirrors iOS `finishSheet.clearLocation`.
    Text(
        text = "DON'T TAG A LOCATION ›",
        style = interUI(9.sp, weight = FontWeight.SemiBold)
            .copy(letterSpacing = 0.22.em, color = AppMaple),
        modifier = Modifier
            .clickable(onClick = onClear)
            .padding(vertical = 4.dp),
    )
}

// ── Photo grid (preview layout) ─────────────────────────────────────────────
//
// Mirrors iOS `PhotoStripLayoutView` — single (4:3), pair (2x 1:1),
// trio (big left + 2 stacked right), grid (2×2 1:1). Same shape the
// post will actually render on the feed, so the user sees a true
// preview instead of a flat horizontal strip that hides the 4th tile.
private val PREVIEW_GUTTER = 1.dp

@Composable
private fun PhotoGrid(
    thumbnails: List<android.graphics.Bitmap>,
    canAdd: Boolean,
    onAddClick: () -> Unit,
    onRemoveAt: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (thumbnails.isEmpty()) {
            // Empty state — single small + tile, sized so it doesn't
            // dominate the form when no photos picked.
            Row(modifier = Modifier.fillMaxWidth()) {
                AddTile(
                    onClick = onAddClick,
                    modifier = Modifier.size(84.dp),
                )
            }
        } else {
            PreviewLayout(thumbnails = thumbnails, onRemoveAt = onRemoveAt)
            if (canAdd) {
                AddMoreRow(onClick = onAddClick)
            }
        }
    }
}

@Composable
private fun PreviewLayout(
    thumbnails: List<android.graphics.Bitmap>,
    onRemoveAt: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppLine),
    ) {
        when (thumbnails.size) {
            1 -> ThumbCell(
                thumbnails[0], 0, onRemoveAt,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
            2 -> Row(horizontalArrangement = Arrangement.spacedBy(PREVIEW_GUTTER)) {
                thumbnails.take(2).forEachIndexed { i, bmp ->
                    ThumbCell(bmp, i, onRemoveAt,
                        modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
            3 -> Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(PREVIEW_GUTTER),
            ) {
                Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                    ThumbCell(thumbnails[0], 0, onRemoveAt,
                        modifier = Modifier.fillMaxSize())
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(PREVIEW_GUTTER),
                ) {
                    thumbnails.subList(1, 3).forEachIndexed { i, bmp ->
                        ThumbCell(bmp, i + 1, onRemoveAt,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                    }
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(PREVIEW_GUTTER)) {
                for (rowIndex in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PREVIEW_GUTTER)) {
                        for (colIndex in 0..1) {
                            val idx = rowIndex * 2 + colIndex
                            if (idx < thumbnails.size) {
                                ThumbCell(thumbnails[idx], idx, onRemoveAt,
                                    modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbCell(
    bmp: android.graphics.Bitmap,
    index: Int,
    onRemoveAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(AppPaper2)) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Picked photo ${index + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .background(AppInk.copy(alpha = 0.78f))
                .clickable { onRemoveAt(index) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = AppPaper,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun AddTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(AppPaper2)
            .border(1.dp, AppLine)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add photo",
            tint = AppInk3,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "ADD",
            style = interUI(8.5.sp, weight = FontWeight.SemiBold)
                .copy(letterSpacing = 0.22.em, color = AppInk3),
        )
    }
}

@Composable
private fun AddMoreRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .border(1.dp, AppLine)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = AppInk2,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "ADD MORE",
            style = interUI(9.5.sp, weight = FontWeight.SemiBold)
                .copy(letterSpacing = 0.18.em, color = AppInk2),
        )
    }
}

// ── Audience picker ──────────────────────────────────────────────────────────

@Composable
private fun AudienceRow(
    selected: FinishAudience,
    onSelect: (FinishAudience) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .border(1.dp, AppLine),
    ) {
        FinishAudience.entries.forEachIndexed { idx, aud ->
            AudienceSegment(
                aud = aud,
                isSelected = aud == selected,
                onClick = { onSelect(aud) },
                modifier = Modifier.weight(1f),
            )
            if (idx < FinishAudience.entries.lastIndex) {
                Box(Modifier.size(1.dp, 64.dp).background(AppLine))
            }
        }
    }
}

@Composable
private fun AudienceSegment(
    aud: FinishAudience,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = if (isSelected) AppPaper else AppInk3
    val bg = if (isSelected) AppInk else AppPaper2
    Column(
        modifier = modifier
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (aud == FinishAudience.Public) Icons.Default.Public else Icons.Default.Lock,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = aud.label.uppercase(),
            style = interUI(9.5.sp, weight = FontWeight.SemiBold)
                .copy(letterSpacing = 0.22.em, color = tone),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = aud.detail,
            style = frauncesDisplay(10.5.sp, italic = true)
                .copy(color = if (isSelected) AppPaper.copy(alpha = 0.72f) else AppInk3),
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun hhmm(instant: Instant): String {
    val t = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return "%02d:%02d".format(t.hour, t.minute)
}

/**
 * Time-of-day default title — pre-fills the title field for an untitled
 * session and backs the placeholder. Mirrors iOS
 * `FinishSheet.timeOfDaySuggestion(for:)`.
 */
internal fun timeOfDaySuggestion(instant: Instant): String {
    val hour = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).hour
    val part = when (hour) {
        in 5..10 -> "Morning"
        in 11..13 -> "Midday"
        in 14..17 -> "Afternoon"
        in 18..21 -> "Evening"
        else -> "Night"
    }
    return "$part shooting session"
}

/**
 * Decode the uCrop result URI off the main thread and produce the upload
 * bytes + a thumbnail bitmap for the grid. The decoded bitmap is reused —
 * we encode `bitmap → JPEG bytes` through the Bitmap overload of
 * [TargetPhotoStore.downscaledForUpload] (parity gap D1) so we don't pay
 * for a `bytes → bitmap → bytes → bitmap` round-trip. Also deletes the
 * uCrop output cache file the moment we're done with it, matching iOS's
 * autoreleased temp URLs.
 */
private suspend fun decodeAndDownscale(
    context: Context,
    uri: Uri,
): Pair<ByteArray?, android.graphics.Bitmap?> = withContext(Dispatchers.IO) {
    val raw = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: return@withContext null to null
    val bmp = runCatching {
        android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.size)
    }.getOrNull() ?: return@withContext null to null
    val bytes = TargetPhotoStore.downscaledForUpload(bmp)
    // Free the uCrop cache file once we have the decoded bitmap + bytes —
    // a session with three picks would otherwise leave three multi-MB
    // jpegs in cache for the OS to GC at its leisure.
    runCatching {
        val path = uri.path
        if (path != null) java.io.File(path).takeIf { it.exists() }?.delete()
    }
    bytes to bmp
}

package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionListPlacement
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionTextField
import com.andrewnguyen.bowpress.core.designsystem.bp.BPBowIcon
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSixRingStyle
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFace
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFaceType
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetStyle
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.SessionType
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/** Test tag for the target-face tile group on the setup screen. */
const val TARGET_FACE_PICKER_TEST_TAG = "target_face_picker"

/** Test tag for the shooting-distance segmented control on the setup screen. */
const val DISTANCE_PICKER_TEST_TAG = "distance_picker"

/** Test tag for the session name text field. */
const val SESSION_NAME_FIELD_TEST_TAG = "session_name_field"

/** Mirrors iOS `sessionNameMaxLength` in SessionView.swift. */
private const val SESSION_NAME_MAX_LENGTH = 60

/**
 * Kenrokuen session-setup screen — BPNavHeader, bow/distance/target-face/intention
 * fields, and the BPPrimaryButton "Begin session" CTA. Mirrors the iOS port at
 * `bowpress-ios/Sources/BowPress/Session/SessionView.swift` `sessionStartView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHomeScreen(
    onSessionStarted: (sessionId: String, type: SessionType) -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
    // Mentions contract §3.1 — backs the session-name field's @-autocomplete.
    mentionResolver: com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionResolverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // If a session becomes active while this screen is visible, route to it
    // (range or 3D course). Keyed on the id so we don't loop on recomposition.
    val activeId = state.activeSession?.id
    LaunchedEffect(activeId) {
        val active = state.activeSession
        if (active != null) onSessionStarted(active.id, active.sessionType)
    }

    // Seed a default bow + arrow selection the first time the catalog arrives so
    // the user doesn't have to open the bow picker to hit "Begin session". Matches
    // iOS `primeSetupState` (SessionView.swift:487–517).
    LaunchedEffect(state.bows, state.arrowConfigs) {
        if (state.selectedBow == null) state.bows.firstOrNull()?.let(viewModel::selectBow)
        if (state.selectedArrow == null) state.arrowConfigs.firstOrNull()?.let(viewModel::selectArrow)
    }

    // Intention note + session name — collected here, persisted by
    // `viewModel.startSession(title=..., intention=...)`. iOS 4fb5c16 /
    // 9b104a2 / 39df3bd: title goes onto ShootingSession.title, intention
    // is seeded into ShootingSession.notes so the Log row + session detail
    // surface what the user typed. Capped at iOS's 60-char limit.
    var sessionName by remember { mutableStateOf("") }
    var intentionNote by remember { mutableStateOf("") }
    var showBowPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        BPNavHeader(
            title = "Set the stage",
            meta = { SetupStamp() },
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            NameField(
                value = sessionName,
                onValueChange = { sessionName = it.take(SESSION_NAME_MAX_LENGTH) },
                onSearchHandles = mentionResolver::searchHandles,
            )
            HairlineDivider()

            SessionTypeField(
                selected = state.selectedSessionType,
                onSelected = { viewModel.selectSessionType(it) },
            )
            HairlineDivider()

            BowField(
                bow = state.selectedBow,
                arrow = state.selectedArrow,
                warning = equipmentWarning(state),
                onClick = { showBowPicker = true },
            )
            HairlineDivider()

            if (state.selectedSessionType == SessionType.RANGE) {
                DistanceField(
                    selected = state.selectedDistance,
                    onSelected = { viewModel.selectDistance(it) },
                )
                HairlineDivider()

                TargetFaceField(
                    selected = state.selectedFaceType,
                    distance = state.selectedDistance,
                    onSelected = { viewModel.selectFaceType(it) },
                )
                HairlineDivider()

                // Multi-spot Vegas layout picker — only at 20yd + 6-ring,
                // per the design (40cm indoor card). Hidden for any other
                // distance/face combination. Mirrors iOS targetLayoutField.
                if (state.selectedDistance == ShootingDistance.YARDS_20 &&
                    state.selectedFaceType == TargetFaceType.SIX_RING
                ) {
                    TargetLayoutField(
                        selected = state.selectedLayout,
                        onSelected = { viewModel.selectLayout(it) },
                    )
                    HairlineDivider()
                }
            } else {
                ScoringSystemField(
                    selected = state.selectedScoringSystem,
                    onSelected = { viewModel.selectScoringSystem(it) },
                )
                HairlineDivider()
            }

            IntentionField(
                value = intentionNote,
                onValueChange = { intentionNote = it },
            )

            Spacer(Modifier.height(8.dp))

            val isThreeD = state.selectedSessionType == SessionType.THREE_D_COURSE
            BPPrimaryButton(
                title = when {
                    state.isLoading -> "Starting…"
                    isThreeD -> "Begin course"
                    else -> "Begin session"
                },
                subtitle = ctaSubtitle(state),
                // A range session needs a distance picked; a 3D course
                // doesn't. (The target face is always auto-defaulted from
                // the bow, so it never needs gating.)
                enabled = state.selectedBow != null
                    && state.selectedArrow != null
                    && (isThreeD || state.selectedDistance != null)
                    && !state.isLoading,
                onClick = {
                    val bow = state.selectedBow ?: return@BPPrimaryButton
                    val arrow = state.selectedArrow ?: return@BPPrimaryButton
                    scope.launch {
                        if (isThreeD) {
                            viewModel.startThreeDCourse(
                                bow = bow,
                                arrow = arrow,
                                system = state.selectedScoringSystem,
                                title = sessionName,
                                intention = intentionNote,
                            )
                        } else {
                            viewModel.startSession(
                                bow = bow,
                                arrow = arrow,
                                title = sessionName,
                                intention = intentionNote,
                            )
                        }
                    }
                },
            )

            state.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = err,
                    style = interUI(12.sp).copy(color = MaterialTheme.colorScheme.error),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showBowPicker) {
        BowPickerSheet(
            state = state,
            onDismiss = { showBowPicker = false },
            onBowSelected = viewModel::selectBow,
            onArrowSelected = viewModel::selectArrow,
        )
    }
}

// ---------------------------------------------------------------------------
// Setup stamp (date + time band + sunrise, upper-right of BPNavHeader)
// ---------------------------------------------------------------------------

@Composable
private fun SetupStamp() {
    val now = remember { LocalDateTime.now() }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = dateLine(now),
            style = jetbrainsMono(10.sp, weight = FontWeight.Medium).copy(color = AppInk),
        )
        Text(
            text = timeLine(now),
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
        )
        Text(
            text = "sunrise 5:58",
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
        )
    }
}

private fun dateLine(d: LocalDateTime): String {
    val dow = d.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).lowercase()
    val mon = d.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).lowercase()
    return "$dow · $mon ${d.dayOfMonth}"
}

private fun timeLine(d: LocalDateTime): String {
    val h = d.hour
    val band = when (h) {
        in 5..10 -> "morning"
        in 11..13 -> "midday"
        in 14..17 -> "afternoon"
        in 18..20 -> "evening"
        else -> "night"
    }
    return "%d:%02d · %s".format(h, d.minute, band)
}

// ---------------------------------------------------------------------------
// Session name field — free-text label for the upcoming session.
// Mirrors iOS `sessionNameField` in SessionView.swift; placeholder copy is
// identical. State is local for now; iOS persists on its view model.
// ---------------------------------------------------------------------------

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearchHandles: suspend (String) -> List<com.andrewnguyen.bowpress.core.model.HandleSuggestion>,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("NAME", hint = "optional")
        Spacer(Modifier.height(10.dp))
        val inputStyle = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium)
            .copy(color = AppInk)
        val placeholderStyle = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Normal)
            .copy(color = AppInk3)
        // The session name supports `@handle` mentions (mentions contract §3) —
        // MentionTextField adds the debounced @-autocomplete; the suggestion
        // list anchors below the field (the setup screen has room beneath it).
        MentionTextField(
            value = value,
            onValueChange = onValueChange,
            onSearch = onSearchHandles,
            textStyle = inputStyle,
            singleLine = true,
            cursorColor = AppPondDk,
            listPlacement = MentionListPlacement.Below,
            fieldModifier = Modifier
                .fillMaxWidth()
                .testTag(SESSION_NAME_FIELD_TEST_TAG),
            // decorationBox places the placeholder *inside* the text field's own
            // semantics node so Maestro sees it as part of the field, not a
            // separate occluded Text overlay (previous version stacked them in a
            // Box and Maestro reported "not visible" for the placeholder copy).
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "Tournament round, drill, or league night",
                        style = placeholderStyle,
                        maxLines = 1,
                    )
                }
                innerTextField()
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Bow field
// ---------------------------------------------------------------------------

@Composable
private fun BowField(
    bow: Bow?,
    arrow: ArrowConfiguration?,
    warning: String?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("BOW", hint = "tap to change")
        if (warning != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = warning,
                style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppMaple),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPaper2)
                .border(1.dp, AppLine)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, AppPond),
                contentAlignment = Alignment.Center,
            ) {
                BPBowIcon(size = 28.dp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bow?.name ?: "No bow selected",
                    style = frauncesDisplay(17.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = AppInk),
                    maxLines = 1,
                )
                Text(
                    text = bowSpecLine(bow, arrow),
                    style = jetbrainsMono(9.5.sp).copy(
                        letterSpacing = 0.04.em,
                        color = AppInk3,
                    ),
                    maxLines = 1,
                )
            }

            Text(
                text = "›",
                style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppPond),
            )
        }
    }
}

private fun bowSpecLine(bow: Bow?, arrow: ArrowConfiguration?): String {
    if (bow == null) return "SELECT A BOW"
    val parts = mutableListOf(bow.bowType.label.uppercase())
    if (arrow != null) parts += arrow.label.uppercase()
    return parts.joinToString(" · ")
}

// Inline reason the Begin CTA is currently gated by bow/arrow state.
// Distinguishes "nothing configured yet" (send to Equipment) from
// "configured but unselected" (just pick one) so the fix path is
// obvious from the setup screen instead of buried in the picker sheet.
// Mirrors iOS `equipmentWarning` in SessionView.swift.
private fun equipmentWarning(state: SessionUiState): String? {
    val noBowsConfigured = state.bows.isEmpty()
    val noArrowsConfigured = state.arrowConfigs.isEmpty()
    if (noBowsConfigured && noArrowsConfigured) {
        return "Add a bow and an arrow in Equipment before you can begin."
    }
    if (noBowsConfigured) return "Add a bow in Equipment before you can begin."
    if (noArrowsConfigured) return "Add an arrow in Equipment before you can begin."
    val missingBow = state.selectedBow == null
    val missingArrow = state.selectedArrow == null
    return when {
        missingBow && missingArrow -> "Pick a bow and an arrow before you can begin."
        missingBow -> "Pick a bow before you can begin."
        missingArrow -> "Pick an arrow before you can begin."
        else -> null
    }
}

// ---------------------------------------------------------------------------
// Distance field — 3-col segmented (20 YD / 50 M / 70 M)
// ---------------------------------------------------------------------------

@Composable
private fun DistanceField(
    selected: ShootingDistance?,
    onSelected: (ShootingDistance?) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("DISTANCE", hint = distanceHint(selected))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .testTag(DISTANCE_PICKER_TEST_TAG)
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, AppLine),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DistanceSegment(
                ShootingDistance.YARDS_20,
                isSelected = selected == ShootingDistance.YARDS_20,
                onClick = { onSelected(if (selected == ShootingDistance.YARDS_20) null else ShootingDistance.YARDS_20) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(AppLine))
            DistanceSegment(
                ShootingDistance.METERS_50,
                isSelected = selected == ShootingDistance.METERS_50,
                onClick = { onSelected(if (selected == ShootingDistance.METERS_50) null else ShootingDistance.METERS_50) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(AppLine))
            DistanceSegment(
                ShootingDistance.METERS_70,
                isSelected = selected == ShootingDistance.METERS_70,
                onClick = { onSelected(if (selected == ShootingDistance.METERS_70) null else ShootingDistance.METERS_70) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun DistanceSegment(
    distance: ShootingDistance,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parts = splitDistance(distance)
    val numTone = if (isSelected) AppPaper else AppInk2
    val unitTone = if (isSelected) AppPaper.copy(alpha = 0.72f) else AppInk3
    Column(
        modifier = modifier
            .background(if (isSelected) AppPondDk else AppPaper)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = parts.first,
            style = frauncesDisplay(19.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = numTone),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = parts.second.uppercase(),
            style = interUI(8.5.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.18.em,
                color = unitTone,
            ),
        )
    }
}

private fun splitDistance(d: ShootingDistance): Pair<String, String> = when (d) {
    ShootingDistance.YARDS_20 -> "20" to "yd"
    ShootingDistance.METERS_50 -> "50" to "m"
    ShootingDistance.METERS_70 -> "70" to "m"
}

// The distance field only shows for a range session, where a distance is
// required to begin — so an unset distance reads "required", not "optional".
private fun distanceHint(selected: ShootingDistance?): String =
    if (selected == null) "required" else "usual · ${selected.label}"

// ---------------------------------------------------------------------------
// Target face field — two tiles (10-ring / 6-ring) without subtitles
// ---------------------------------------------------------------------------

@Composable
private fun TargetFaceField(
    selected: TargetFaceType,
    distance: ShootingDistance?,
    onSelected: (TargetFaceType) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("TARGET FACE", hint = "World Archery")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .testTag(TARGET_FACE_PICKER_TEST_TAG)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FaceTile(
                face = TargetFaceType.TEN_RING,
                distance = distance,
                isSelected = selected == TargetFaceType.TEN_RING,
                onClick = { onSelected(TargetFaceType.TEN_RING) },
                modifier = Modifier.weight(1f),
            )
            FaceTile(
                face = TargetFaceType.SIX_RING,
                distance = distance,
                isSelected = selected == TargetFaceType.SIX_RING,
                onClick = { onSelected(TargetFaceType.SIX_RING) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One target-face tile. The face icon, name, and physical-size sub-label are
 * all distance-aware — at 20yd the 6-ring tile shows the 40cm Vegas face
 * ("6-ring"); at 50/70m it shows the 80cm WA compound face ("7-ring").
 * Mirrors iOS `faceTile` + `faceName` + `faceSize`.
 */
@Composable
private fun FaceTile(
    face: TargetFaceType,
    distance: ShootingDistance?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) AppPondDk else AppLine
    val nameColor = if (isSelected) AppPondDk else AppInk
    val bgColor = if (isSelected) AppPaper2 else AppPaper
    Column(
        modifier = modifier
            .background(bgColor)
            .border(1.dp, borderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BPTargetFace(
            size = 72.dp,
            style = BPTargetStyle.WA,
            face = when (face) {
                TargetFaceType.TEN_RING -> BPTargetFaceType.TenRing
                TargetFaceType.SIX_RING -> BPTargetFaceType.SixRing
            },
            sixRingStyle = sixRingStyleForDistance(distance),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = faceName(face, distance),
            style = frauncesDisplay(11.5.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = nameColor),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = faceSizeLabel(face, distance),
            style = jetbrainsMono(9.sp).copy(color = AppInk3),
        )
    }
}

/**
 * Distance-aware face name. WA inner-face nomenclature depends on the round:
 * at 20yd indoor the inner face is the 40cm Vegas 6-zone (rings 6–X); at
 * 50/70m outdoor it's the 80cm compound 7-zone (rings 5–X). Same
 * [TargetFaceType.SIX_RING] in the model. Mirrors iOS `faceName`.
 */
private fun faceName(face: TargetFaceType, distance: ShootingDistance?): String = when (face) {
    TargetFaceType.TEN_RING -> "10-ring"
    TargetFaceType.SIX_RING ->
        if (distance == ShootingDistance.YARDS_20) "6-ring" else "7-ring"
}

/**
 * Physical face size from the distance + face. Indoor 20yd uses a 40cm card;
 * outdoor 50/70m uses the 122cm full face for 10-ring and the 80cm compound
 * face for the inner option. Mirrors iOS `faceSize`.
 */
private fun faceSizeLabel(face: TargetFaceType, distance: ShootingDistance?): String =
    when (face) {
        TargetFaceType.TEN_RING ->
            if (distance == ShootingDistance.YARDS_20) "40cm" else "122cm"
        TargetFaceType.SIX_RING ->
            if (distance == ShootingDistance.YARDS_20) "40cm" else "80cm"
    }

/**
 * Picks the visual variant of the six-ring icon so it matches the real face
 * at the picked distance — 40cm Vegas at 20yd or unknown (6 zones, single
 * blue band); 80cm compound at 50/70m (7 zones, blue split). Delegates to
 * the canonical [BPSixRingStyle.forDistance] so the four SixRing surfaces
 * (setup icon, feed card, friend detail, session detail) agree on the
 * Vegas null-default. Mirrors iOS `sixRingStyleForCurrentDistance`.
 */
private fun sixRingStyleForDistance(distance: ShootingDistance?): BPSixRingStyle =
    BPSixRingStyle.forDistance(distance)

// ---------------------------------------------------------------------------
// Target layout field — Triangle / Vertical 3-spot Vegas tiles
// ---------------------------------------------------------------------------

/** Test tag for the multi-spot layout tile group on the setup screen. */
const val TARGET_LAYOUT_PICKER_TEST_TAG = "target_layout_picker"

/**
 * Vegas-paper layout picker — only rendered at 20yd + 6-ring. No "Single"
 * tile: collapsing the picker (by picking any other distance/face) is how
 * you get a single bullseye. Mirrors iOS `targetLayoutField`.
 */
@Composable
private fun TargetLayoutField(
    selected: TargetLayout,
    onSelected: (TargetLayout) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("LAYOUT", hint = "6-ring · 20yd · Vegas")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .testTag(TARGET_LAYOUT_PICKER_TEST_TAG)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LayoutTile(
                layout = TargetLayout.TRIANGLE,
                isSelected = selected == TargetLayout.TRIANGLE,
                onClick = { onSelected(TargetLayout.TRIANGLE) },
                modifier = Modifier.weight(1f),
            )
            LayoutTile(
                layout = TargetLayout.VERTICAL,
                isSelected = selected == TargetLayout.VERTICAL,
                onClick = { onSelected(TargetLayout.VERTICAL) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LayoutTile(
    layout: TargetLayout,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) AppPine else AppLine
    val nameColor = if (isSelected) AppPine else AppInk
    val bgColor = if (isSelected) AppPaper2 else AppPaper
    Column(
        modifier = modifier
            .background(bgColor)
            .border(1.dp, borderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LayoutThumb(layout = layout)
        Spacer(Modifier.height(8.dp))
        Text(
            // iOS uses "Triangle" / "Vertical" on the layout tiles.
            text = if (layout == TargetLayout.TRIANGLE) "Triangle" else "Vertical",
            style = frauncesDisplay(11.5.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = nameColor),
        )
    }
}

/**
 * Mini 3-spot schematic for a layout tile — three tiny concentric Vegas
 * spots arranged triangle or vertical. Mirrors iOS `layoutThumb` /
 * `drawMiniSpot`.
 */
@Composable
private fun LayoutThumb(layout: TargetLayout) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(44.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val centers: List<Offset>
        val spotR: Float
        when (layout) {
            TargetLayout.TRIANGLE -> {
                spotR = w * 0.18f
                val dx = w * 0.26f
                val dy = h * 0.22f
                centers = listOf(
                    Offset(cx, cy - dy),         // top
                    Offset(cx - dx, cy + dy),    // bottom-left
                    Offset(cx + dx, cy + dy),    // bottom-right
                )
            }
            TargetLayout.VERTICAL -> {
                spotR = w * 0.16f
                val dy = h * 0.30f
                centers = listOf(
                    Offset(cx, cy - dy),
                    Offset(cx, cy),
                    Offset(cx, cy + dy),
                )
            }
            TargetLayout.SINGLE -> {
                spotR = w * 0.4f
                centers = listOf(Offset(cx, cy))
            }
        }
        for (c in centers) {
            // Outer blue → red → yellow, scaled to a tiny preview tile.
            drawCircle(MINI_BLUE, spotR, c)
            drawCircle(MINI_RED, spotR * 0.80f, c)
            drawCircle(MINI_YELLOW, spotR * 0.50f, c)
            // Hairline outline so spots read as distinct against paper.
            drawCircle(
                color = MINI_INK,
                radius = spotR,
                center = c,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5.dp.toPx()),
            )
        }
    }
}

private val MINI_BLUE = Color(0xFF4EA8C9)
private val MINI_RED = Color(0xFFD94B3B)
private val MINI_YELLOW = Color(0xFFF0D04A)
private val MINI_INK = Color(0xFF1F2A26)

// ---------------------------------------------------------------------------
// Intention note
// ---------------------------------------------------------------------------

@Composable
private fun IntentionField(value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("INTENTION", hint = "optional · one line")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(AppPaper2)
                .drawBehind {
                    drawRect(
                        color = AppLine,
                        style = Stroke(
                            width = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)),
                        ),
                    )
                }
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "“",
                style = frauncesDisplay(22.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppPond),
            )
            Box(modifier = Modifier.weight(1f)) {
                val textStyle = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Normal)
                    .copy(color = AppInk)
                if (value.isEmpty()) {
                    Text(
                        text = "Keep the back tension through the click. No rushing the shot on end two.",
                        style = textStyle.copy(color = AppInk3),
                        maxLines = 3,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(AppPondDk),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun FieldLabel(label: String, hint: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BPEyebrow(label)
        Text(
            text = hint,
            style = frauncesDisplay(10.5.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = AppPond),
        )
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppLine),
    )
}

private fun ctaSubtitle(state: SessionUiState): String {
    val bow = state.selectedBow?.name?.uppercase() ?: "BOW"
    if (state.selectedSessionType == SessionType.THREE_D_COURSE) {
        return "3D COURSE · $bow · ${state.selectedScoringSystem.label.uppercase()}"
    }
    val distance = state.selectedDistance?.label?.uppercase() ?: "DISTANCE"
    val face = if (state.selectedFaceType == TargetFaceType.TEN_RING) "10-RING" else "6-RING"
    return "$distance · $bow · $face"
}

// ---------------------------------------------------------------------------
// Session type field — Range vs 3D Course
// ---------------------------------------------------------------------------

@Composable
private fun SessionTypeField(
    selected: SessionType,
    onSelected: (SessionType) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("DISCIPLINE", hint = "range or 3D")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SessionType.entries.forEach { type ->
                ChoiceTile(
                    title = type.label,
                    subtitle = type.subtitle,
                    isSelected = selected == type,
                    onClick = { onSelected(type) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scoring system field — ASA / IBO / WA 3D (shown for 3D courses)
// ---------------------------------------------------------------------------

@Composable
private fun ScoringSystemField(
    selected: ThreeDScoringSystem,
    onSelected: (ThreeDScoringSystem) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        FieldLabel("SCORING SYSTEM", hint = "3D rings")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThreeDScoringSystem.entries.forEach { system ->
                ChoiceTile(
                    title = system.label,
                    subtitle = system.ringSummary,
                    isSelected = selected == system,
                    onClick = { onSelected(system) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChoiceTile(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(if (isSelected) AppPaper2 else AppPaper)
            .border(1.dp, if (isSelected) AppPondDk else AppLine)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = frauncesDisplay(15.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = if (isSelected) AppPondDk else AppInk),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle.uppercase(),
            style = jetbrainsMono(8.sp).copy(letterSpacing = 0.04.em, color = AppInk3),
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Bow / arrow picker sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BowPickerSheet(
    state: SessionUiState,
    onDismiss: () -> Unit,
    onBowSelected: (Bow) -> Unit,
    onArrowSelected: (ArrowConfiguration) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val unitSystem = LocalUnitSystem.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Change",
                style = frauncesDisplay(22.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppInk),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            BPEyebrow("BOW")
            Spacer(Modifier.height(6.dp))
            if (state.bows.isEmpty()) {
                EmptyRow("No bows configured. Add one in the Equipment tab.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.bows, key = { it.id }) { bow ->
                        PickerRow(
                            title = bow.name,
                            subtitle = bow.bowType.label,
                            isSelected = state.selectedBow?.id == bow.id,
                            onClick = { onBowSelected(bow) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            BPEyebrow("ARROW")
            Spacer(Modifier.height(6.dp))
            if (state.arrowConfigs.isEmpty()) {
                EmptyRow("No arrow configs. Add one in the Equipment tab.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.arrowConfigs, key = { it.id }) { arrow ->
                        PickerRow(
                            title = arrow.label,
                            subtitle = arrow.specSummary(unitSystem),
                            isSelected = state.selectedArrow?.id == arrow.id,
                            onClick = { onArrowSelected(arrow) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) AppPondDk else AppLine
    val bg = if (isSelected) AppPaper2 else AppPaper
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .border(1.dp, borderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = frauncesDisplay(15.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppInk),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle.uppercase(),
                style = jetbrainsMono(9.sp).copy(letterSpacing = 0.04.em, color = AppInk3),
                maxLines = 1,
            )
        }
        if (isSelected) {
            Text(
                text = "✓",
                style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppPondDk),
            )
        }
    }
}

@Composable
private fun EmptyRow(message: String) {
    Text(
        text = message,
        style = frauncesDisplay(13.sp, italic = true).copy(color = AppInk3),
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

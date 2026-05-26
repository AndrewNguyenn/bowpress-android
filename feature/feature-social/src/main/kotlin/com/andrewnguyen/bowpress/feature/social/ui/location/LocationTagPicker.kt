package com.andrewnguyen.bowpress.feature.social.ui.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

// ── LocationTagPicker ────────────────────────────────────────────────────────
//
// The capture half of §18 location tagging — the Instagram-style "tag a
// location" picker, mirroring iOS LocationTagPicker. The archer drags the OSM
// slippy-map under a fixed centre pin, names the place (auto-filled by reverse
// geocoding, fully editable), and saves. SlippyMap's native gestures give a
// smooth finger pan + pinch-zoom with no custom gesture code.

/** Zoom the picker map opens at — a neighbourhood view. */
private const val PICKER_ZOOM = 14.0
/** Fallback centre when there is no existing tag and no fix yet (SF). */
private val FALLBACK_CENTER = GeoPoint(37.7749, -122.4194)

@OptIn(FlowPreview::class)
@Composable
fun LocationTagPicker(
    initial: SessionLocation?,
    onSave: (SessionLocation) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val mapState = rememberSlippyMapState(
            center = initial?.let { GeoPoint(it.latitude, it.longitude) } ?: FALLBACK_CENTER,
            zoom = PICKER_ZOOM,
        )
        var placeName by remember { mutableStateOf(initial?.name ?: "") }
        var isResolving by remember { mutableStateOf(false) }
        var isLocating by remember { mutableStateOf(false) }
        var permissionDenied by remember { mutableStateOf(false) }
        // True once the archer hand-edits the name — suppresses a late
        // reverse-geocode result from clobbering their text. Cleared on the
        // next pan, which is an explicit "pick a new place" and re-arms
        // auto-naming. Mirrors iOS `userEditedName`.
        var userEditedName by remember { mutableStateOf(false) }

        // Reverse-geocode the centre after the map settles from a pan/zoom.
        // Drop the initial value so opening the picker on an existing tag
        // doesn't overwrite the saved name. debounce() means only one
        // geocode is ever in flight — a newer pan cancels the pending one,
        // so a stale result can't land last.
        LaunchedEffect(Unit) {
            snapshotFlow { mapState.center }
                .drop(1)
                .onEach {
                    // A deliberate pan re-arms auto-naming.
                    userEditedName = false
                }
                .debounce(450)
                .collect { center ->
                    isResolving = true
                    val name = reverseGeocode(context, center)
                    isResolving = false
                    // Don't clobber a name the archer typed since this started.
                    if (name != null && !userEditedName) placeName = name
                }
        }

        // "Use my current location" — fused location, gated by a runtime
        // permission request. Moving the camera changes mapState.center, which
        // the debounced snapshotFlow collector above already observes and
        // geocodes — so this does NOT geocode itself (that would race the
        // collector for placeName/isResolving). It only re-arms auto-naming
        // before the move, since a fix is an explicit "pick a new place".
        fun jumpToCurrentLocation() {
            permissionDenied = false
            isLocating = true
            scope.launch {
                val fix = currentLocation(context)
                isLocating = false
                if (fix != null) {
                    userEditedName = false
                    mapState.moveTo(fix, PICKER_ZOOM)
                }
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            if (grants.values.any { it }) {
                jumpToCurrentLocation()
            } else {
                // Denied — surface a message so the locate button is not a
                // silent dead control. Mirrors iOS `permissionDenied`.
                permissionDenied = true
            }
        }

        fun requestCurrentLocation() {
            if (hasLocationPermission(context)) {
                jumpToCurrentLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }

        // Parity E6 — Instagram-style place search (iOS commit aea47c7).
        // Debounced (300ms) Geocoder.getFromLocationName lookup; tap a result
        // to recenter the map + fill the name.
        var searchQuery by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<List<PlaceHit>>(emptyList()) }
        var isSearching by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            snapshotFlow { searchQuery }
                .debounce(300)
                .collect { q ->
                    val trimmed = q.trim()
                    if (trimmed.isEmpty()) {
                        searchResults = emptyList()
                        isSearching = false
                        return@collect
                    }
                    isSearching = true
                    val hits = searchPlaces(context, trimmed, near = mapState.center)
                    isSearching = false
                    searchResults = hits
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppPaper),
        ) {
            PickerHeader(
                canSave = placeName.isNotBlank(),
                onCancel = onDismiss,
                onSave = {
                    onSave(
                        SessionLocation(
                            name = placeName.trim(),
                            latitude = mapState.center.latitude,
                            longitude = mapState.center.longitude,
                        ),
                    )
                },
            )
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                isSearching = isSearching,
            )
            // Map with a fixed centre pin — the map moves under it.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            ) {
                SlippyMap(
                    state = mapState,
                    pinAtCenter = true,
                    pinColor = AppPondDk,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTags.LocationPickerMap),
                )
                // Parity E6 — when the archer is searching, the result list
                // overlays the map. Tap to jump the pin + fill the name.
                if (searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppPaper.copy(alpha = 0.95f)),
                    ) {
                        SearchResults(
                            results = searchResults,
                            isSearching = isSearching,
                            query = searchQuery,
                            onPick = { hit ->
                                userEditedName = true
                                placeName = hit.primary
                                searchQuery = ""
                                searchResults = emptyList()
                                mapState.moveTo(GeoPoint(hit.lat, hit.lng), PICKER_ZOOM)
                            },
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(40.dp)
                        .background(AppPaper)
                        .border(1.dp, AppLine)
                        .clickable { requestCurrentLocation() }
                        .semantics { contentDescription = "Use my current location" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AppPondDk,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = AppPondDk,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            PickerDetail(
                placeName = placeName,
                onNameChange = {
                    placeName = it
                    // A hand-edit suppresses a late geocode overwrite.
                    userEditedName = true
                },
                isResolving = isResolving,
                permissionDenied = permissionDenied,
                canRemove = initial != null,
                onRemove = {
                    onRemove()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun PickerHeader(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cancel",
                style = interUI(13.sp, FontWeight.Medium),
                color = AppInk3,
                modifier = Modifier.clickable(onClick = onCancel),
            )
            Text(
                text = "Tag location",
                style = frauncesDisplay(16.sp),
                color = AppInk,
            )
            Text(
                text = "Save",
                style = interUI(13.sp, FontWeight.SemiBold),
                color = if (canSave) AppPondDk else AppInk3,
                modifier = Modifier
                    .then(if (canSave) Modifier.clickable(onClick = onSave) else Modifier),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppLine))
    }
}

@Composable
private fun PickerDetail(
    placeName: String,
    onNameChange: (String) -> Unit,
    isResolving: Boolean,
    permissionDenied: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Text(
            text = "PLACE NAME",
            style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
            color = AppInk3,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppLine)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (placeName.isEmpty()) {
                    Text(
                        text = "Name this place",
                        style = frauncesDisplay(15.sp),
                        color = AppInk3,
                    )
                }
                BasicTextField(
                    value = placeName,
                    onValueChange = onNameChange,
                    textStyle = frauncesDisplay(15.sp).copy(color = AppInk),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppPondDk),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.LocationPickerNameField),
                )
            }
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AppPondDk,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Drag the map to position the pin, then name the spot.",
            style = frauncesDisplay(12.sp),
            color = AppInk3,
        )
        if (permissionDenied) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Location access is off — turn it on in Settings to use " +
                    "your current location.",
                style = frauncesDisplay(12.sp),
                color = AppMaple,
            )
        }
        if (canRemove) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppMaple)
                    .clickable(onClick = onRemove)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "REMOVE LOCATION",
                    style = interUI(10.sp, FontWeight.SemiBold).copy(letterSpacing = 0.2.em),
                    color = AppMaple,
                )
            }
        }
    }
}

// ── Location + geocoding helpers ─────────────────────────────────────────────

private fun hasLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

/** One-shot fused-location fix. Returns null on failure / no permission. */
@Suppress("MissingPermission")
private suspend fun currentLocation(context: android.content.Context): GeoPoint? {
    if (!hasLocationPermission(context)) return null
    return runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .await()
        loc?.let { GeoPoint(it.latitude, it.longitude) }
    }.getOrNull()
}

/**
 * Parity E6 — one search hit from [Geocoder.getFromLocationName]. iOS uses
 * `MKLocalSearchCompleter`; on Android the geocoder doesn't need a separate
 * API key but is best-effort (Samsung devices sometimes ship a stub
 * implementation that returns nothing).
 */
internal data class PlaceHit(
    val primary: String,
    val secondary: String,
    val lat: Double,
    val lng: Double,
)

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = AppInk3,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search places — range, club, park…",
                    style = frauncesDisplay(14.sp),
                    color = AppInk3,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = frauncesDisplay(14.sp).copy(color = AppInk),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AppPondDk),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isSearching) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 1.5.dp,
                color = AppPondDk,
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: List<PlaceHit>,
    isSearching: Boolean,
    query: String,
    onPick: (PlaceHit) -> Unit,
) {
    if (isSearching && results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                "Searching for \"$query\"…",
                style = frauncesDisplay(13.sp, italic = true),
                color = AppInk3,
            )
        }
        return
    }
    if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                "No places match \"$query\".",
                style = frauncesDisplay(13.sp, italic = true),
                color = AppInk3,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results, key = { "${it.primary}|${it.lat},${it.lng}" }) { hit ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(hit) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(hit.primary, style = frauncesDisplay(14.sp), color = AppInk)
                if (hit.secondary.isNotBlank()) {
                    Text(hit.secondary, style = jetbrainsMono(10.sp), color = AppInk3)
                }
            }
            HorizontalDivider(color = AppLine2, thickness = 1.dp)
        }
    }
}

/**
 * Parity E6 — best-effort place search via Android's [Geocoder]. Skips the
 * Google Places API to avoid a key dependency; the geocoder takes a free-form
 * string and returns ranked address candidates around the given anchor when
 * supported by the device.
 */
private suspend fun searchPlaces(
    context: android.content.Context,
    query: String,
    near: GeoPoint,
): List<PlaceHit> = withContext(Dispatchers.IO) {
    runCatching {
        @Suppress("DEPRECATION")
        val results = Geocoder(context, Locale.getDefault())
            .getFromLocationName(
                query,
                /* maxResults */ 6,
                /* lowerLeftLat */ near.latitude - 1.5,
                /* lowerLeftLng */ near.longitude - 1.5,
                /* upperRightLat */ near.latitude + 1.5,
                /* upperRightLng */ near.longitude + 1.5,
            )
        results.orEmpty().mapNotNull { a ->
            val primary = a.featureName?.takeIf { it.isNotBlank() && it != a.thoroughfare }
                ?: a.subLocality
                ?: a.locality
                ?: a.adminArea
                ?: return@mapNotNull null
            val secondary = listOfNotNull(
                a.locality?.takeIf { it != primary },
                a.adminArea?.takeIf { it != primary },
                a.countryName,
            ).joinToString(", ")
            PlaceHit(
                primary = primary,
                secondary = secondary,
                lat = a.latitude,
                lng = a.longitude,
            )
        }
    }.getOrDefault(emptyList())
}

/** Reverse-geocode a coordinate into a human place name. Null on failure. */
private suspend fun reverseGeocode(
    context: android.content.Context,
    point: GeoPoint,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        @Suppress("DEPRECATION")
        val results = Geocoder(context, Locale.getDefault())
            .getFromLocation(point.latitude, point.longitude, 1)
        results?.firstOrNull()?.let { a ->
            a.featureName?.takeIf { it.isNotBlank() && it != a.thoroughfare }
                ?: a.subLocality
                ?: a.locality
                ?: a.adminArea
        }
    }.getOrNull()
}

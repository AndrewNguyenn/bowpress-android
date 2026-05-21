package com.andrewnguyen.bowpress.feature.social.ui.location

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.SessionLocation
import java.util.Locale

// ── LocationMapSheet ─────────────────────────────────────────────────────────
//
// The map popup behind a feed post's location tag (§18). Tapping the small
// "in {place}" tag opens this dialog — an OSM slippy-map centred on the tagged
// coordinate with a pin. SlippyMap handles pinch-zoom and finger panning
// natively (Compose detectTransformGestures), so the pan is smooth without any
// custom gesture handling; a recenter control snaps back to the pin.

/** The zoom the map opens at and the recenter control returns to. */
private const val POPUP_ZOOM = 15.0

@Composable
fun LocationMapSheet(
    location: SessionLocation,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val mapState = rememberSlippyMapState(
            center = GeoPoint(location.latitude, location.longitude),
            zoom = POPUP_ZOOM,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppPaper),
        ) {
            LocationMapHeader(location = location, onDismiss = onDismiss)
            Box(Modifier.fillMaxSize()) {
                SlippyMap(
                    state = mapState,
                    markers = listOf(GeoPoint(location.latitude, location.longitude)),
                    pinColor = AppPondDk,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTags.LocationMap),
                )
                // Recenter — snaps the camera back to the tagged pin.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(AppPaper)
                        .border(1.dp, AppLine)
                        .clickable {
                            mapState.moveTo(
                                GeoPoint(location.latitude, location.longitude),
                                POPUP_ZOOM,
                            )
                        }
                        .testTag(TestTags.LocationMapRecenter)
                        .semantics {
                            contentDescription = "Recenter the map on the tagged location"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = AppPondDk,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationMapHeader(location: SessionLocation, onDismiss: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "LOCATION",
                    style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                    color = AppInk3,
                )
                Text(
                    text = location.name,
                    style = frauncesDisplay(22.sp),
                    color = AppInk,
                )
                Text(
                    text = location.coordinateLine(),
                    style = jetbrainsMono(10.sp),
                    color = AppInk3,
                )
            }
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .border(1.dp, AppLine)
                    .clickable(onClick = onDismiss)
                    .testTag(TestTags.LocationMapClose)
                    .semantics { contentDescription = "Close the map" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = AppInk2,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppLine))
    }
}

/** "37.3318, -122.0312" — the tagged coordinate, 4 dp. */
internal fun SessionLocation.coordinateLine(): String =
    String.format(Locale.US, "%.4f, %.4f", latitude, longitude)

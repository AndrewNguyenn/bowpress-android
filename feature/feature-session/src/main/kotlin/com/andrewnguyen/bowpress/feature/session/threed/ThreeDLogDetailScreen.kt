package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.designsystem.coursemap.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

/**
 * Log detail of a finished 3D course — the walked map as the hero, the
 * round's stats, and a tappable station-by-station breakdown. Mirrors iOS
 * `ThreeDLogDetailView`.
 */
@Composable
fun ThreeDLogDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    onOpenAnalytics: () -> Unit = {},
    viewModel: ThreeDLogDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem = LocalUnitSystem.current
    var focused by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        BPNavHeader(
            title = state.session?.title?.takeIf { it.isNotBlank() } ?: "Course",
            meta = {
                Text(
                    "BACK",
                    style = interUI(10.sp, FontWeight.SemiBold).copy(color = AppMaple),
                    modifier = Modifier.clickable(onClick = onBack),
                )
            },
        )

        // The content area is a Box so the station bottom sheet can overlay
        // the map. Tapping a pin or a station row focuses that station; the
        // sheet rises over everything and the map below stays pannable.
        BoxWithConstraints(Modifier.weight(1f)) {
            val areaHeight = maxHeight
            val focusedStation = focused?.let { state.stations.getOrNull(it) }
            val runningTotal = remember(state.stations, focusedStation) {
                runningTotalThrough(state.stations, focusedStation)
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    CourseInkMapView(
                        stations = state.stations,
                        elevationGrid = state.elevationGrid,
                        selectedStation = focused,
                        // A pin tap selects the station (no toggle-to-close)
                        // — consistent with a row tap; CLOSE ✕ dismisses.
                        onTapStation = { focused = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatCell("TOTAL", "${state.totalScore}", Modifier.weight(1f))
                        StatCell("AVERAGE", "%.1f".format(state.averageScore), Modifier.weight(1f))
                        StatCell("KILLS", "${state.killCount}", Modifier.weight(1f))
                    }
                }
                if (state.isCleanRound) {
                    item {
                        Text(
                            "● CLEAN ROUND — no fives, no misses",
                            style = interUI(10.sp, FontWeight.SemiBold).copy(color = AppPondDk),
                        )
                    }
                }
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppPondDk)
                            .clickable(onClick = onOpenAnalytics)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "VIEW 3D ANALYTICS — ANGLE & DISTANCE",
                            style = interUI(11.sp, FontWeight.SemiBold).copy(color = AppPondDk),
                        )
                    }
                }
                item { BPEyebrow("STATIONS") }
                items(state.stations, key = { it.id }) { station ->
                    CourseStationRow(
                        station = station,
                        system = state.scoringSystem,
                        unitSystem = unitSystem,
                        focused = focused == station.stationNumber - 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppLine)
                            .clickable { focused = station.stationNumber - 1 },
                    )
                }
                if (state.stations.isEmpty() && !state.isLoading) {
                    item {
                        Text(
                            "No stations recorded for this course.",
                            style = frauncesDisplay(13.sp, italic = true).copy(color = AppInk3),
                        )
                    }
                }
            }

            // Station detail sheet — read-only (no Edit / Discard).
            CourseStationBottomSheet(
                station = focusedStation,
                containerHeight = areaHeight,
                stationCount = state.stations.size,
                system = state.scoringSystem,
                unitSystem = unitSystem,
                runningTotal = runningTotal,
                onClose = { focused = null },
                editable = false,
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(1.dp, AppLine)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = jetbrainsMono(8.5.sp).copy(color = AppInk3))
        Text(
            value,
            style = frauncesDisplay(26.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
        )
    }
}

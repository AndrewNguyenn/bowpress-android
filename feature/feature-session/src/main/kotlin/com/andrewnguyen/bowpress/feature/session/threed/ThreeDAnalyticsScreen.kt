package com.andrewnguyen.bowpress.feature.session.threed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

/**
 * The 3D-archery analytics view — average per station across every completed
 * 3D course, broken down by shot angle and by distance band. Mirrors iOS
 * `ThreeDAnalyticsView`.
 */
@Composable
fun ThreeDAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: ThreeDAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        BPNavHeader(
            title = "Course form",
            meta = {
                Text(
                    "BACK",
                    style = interUI(10.sp, FontWeight.SemiBold).copy(color = AppMaple),
                    modifier = Modifier.clickable(onClick = onBack),
                )
            },
        )

        if (state.stationCount == 0) {
            Text(
                "Walk a 3D course to see your angle and distance breakdowns here.",
                style = frauncesDisplay(13.sp, italic = true).copy(color = AppInk3),
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeadlineStat("COURSES", "${state.courseCount}", Modifier.weight(1f))
                    HeadlineStat("STATIONS", "${state.stationCount}", Modifier.weight(1f))
                    HeadlineStat("AVG", "%.1f".format(state.averagePerStation), Modifier.weight(1f))
                }
            }
            item { BPEyebrow("BY SHOT ANGLE") }
            items(state.byAngle) { BucketBar(it, state.headlineMax()) }
            item { BPEyebrow("BY DISTANCE") }
            items(state.byDistance) { BucketBar(it, state.headlineMax()) }
        }
    }
}

private fun ThreeDAnalyticsUiState.headlineMax(): Double =
    (byAngle + byDistance).maxOfOrNull { it.average }?.coerceAtLeast(1.0) ?: 1.0

@Composable
private fun HeadlineStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.border(1.dp, AppLine).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = jetbrainsMono(8.5.sp).copy(color = AppInk3))
        Text(
            value,
            style = frauncesDisplay(24.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
        )
    }
}

@Composable
private fun BucketBar(bucket: ThreeDBucket, max: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(bucket.label, style = interUI(12.sp, FontWeight.SemiBold).copy(color = AppInk))
            Text(
                "%.1f · ${bucket.count}".format(bucket.average),
                style = jetbrainsMono(10.sp).copy(color = AppInk3),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(AppPaper2)
                .border(1.dp, AppLine),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((bucket.average / max).toFloat().coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .background(AppPondDk),
            )
        }
    }
}

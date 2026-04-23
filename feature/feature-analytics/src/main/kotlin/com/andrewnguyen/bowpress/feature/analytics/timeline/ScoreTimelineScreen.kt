package com.andrewnguyen.bowpress.feature.analytics.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.feature.analytics.ui.AnalyticsCard
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreTimelineScreen(
    onBack: () -> Unit,
    viewModel: ScoreTimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.bow?.name?.let { "$it — timeline" } ?: "Score timeline",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
                state.points.isEmpty() -> EmptyTimeline(modifier = Modifier.align(Alignment.Center))
                else -> TimelineCard(points = state.points)
            }
        }
    }
}

@Composable
private fun TimelineCard(points: List<TimelinePoint>) {
    AnalyticsCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Avg arrow score over time",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            VicoLineChart(points = points)
            LegendRow(points = points)
        }
    }
}

/**
 * Vico LineChart wrapper. Vico 2.0 beta exposes charts via
 * `CartesianChartHost` + `rememberLineCartesianLayer`; we push the y values
 * into a [CartesianChartModelProducer] from a [LaunchedEffect] keyed on [points].
 *
 * Falls back to a Canvas sparkline if the Vico rendering fails — keeps the
 * screen useful in previews and on pre-data states.
 */
@Composable
private fun VicoLineChart(points: List<TimelinePoint>) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        val scores = points.map { it.score }
        if (scores.isNotEmpty()) {
            producer.runTransaction {
                lineSeries { series(scores) }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        // Prefer Vico; fall back to local sparkline for preview / unknown-host contexts.
        val vicoChart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        )
        CartesianChartHost(
            chart = vicoChart,
            modelProducer = producer,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun LegendRow(points: List<TimelinePoint>) {
    val best = points.maxByOrNull { it.score }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Points: ${points.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        best?.let {
            Text(
                text = "Best: ${it.label} (${String.format(java.util.Locale.US, "%.1f", it.score)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Minimal Canvas fallback sparkline — used by the preview. */
@Composable
private fun CanvasSparkline(points: List<TimelinePoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val accent = MaterialTheme.colorScheme.primary
    val minScore = points.minOf { it.score }
    val maxScore = points.maxOf { it.score }
    val range = (maxScore - minScore).coerceAtLeast(0.5)
    Canvas(modifier = modifier.height(220.dp).fillMaxWidth()) {
        if (points.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1).coerceAtLeast(1).toFloat()
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normalized = ((point.score - minScore) / range).toFloat()
            val y = height - (normalized * height * 0.9f) - (height * 0.05f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )
        // Point dots
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val normalized = ((point.score - minScore) / range).toFloat()
            val y = height - (normalized * height * 0.9f) - (height * 0.05f)
            drawCircle(color = accent, radius = 6f, center = Offset(x, y))
        }
    }
}

@Composable
private fun EmptyTimeline(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Not enough data yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Log sessions to plot score over time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

// ---------- Preview ----------

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ScoreTimelinePreview() {
    BowPressTheme {
        val now = Instant.now()
        val pts = (0 until 6).map { idx ->
            TimelinePoint(
                configId = "c$idx",
                label = "C$idx",
                createdAt = now.minusSeconds((6 - idx).toLong() * 86_400),
                score = 8.5 + (idx % 3) * 0.5,
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            AnalyticsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Avg arrow score over time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    CanvasSparkline(points = pts, modifier = Modifier.fillMaxWidth())
                    LegendRow(points = pts)
                }
            }
        }
    }
}


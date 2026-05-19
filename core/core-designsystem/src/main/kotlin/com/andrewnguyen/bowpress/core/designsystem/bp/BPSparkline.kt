package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMoss
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk

/**
 * Thin polyline over top/mid/bottom dashed guides, last datum highlighted as
 * a filled moss dot with an AppInk 1dp stroke.
 */
@Composable
fun BPSparkline(
    points: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 86.dp,
    range: ClosedRange<Double>? = null,
) {
    val lineColor = AppLine
    val pathColor = AppPondDk
    val dotFill = AppMoss
    val dotStroke = AppInk
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val w = size.width
        val h = size.height

        val lo = range?.start ?: points.minOrNull() ?: 0.0
        val hi = range?.endInclusive ?: points.maxOrNull() ?: 1.0
        val span = (hi - lo).takeIf { it > 0.0 } ?: 1.0

        // Dashed horizontal guides.
        val dash = PathEffect.dashPathEffect(floatArrayOf(1f, 4f))
        for (y in listOf(0f, h / 2f, h)) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = dash,
            )
        }

        // Polyline.
        if (points.size >= 2) {
            val path = Path()
            points.forEachIndexed { i, v ->
                val x = w * i / (points.size - 1).toFloat()
                val norm = ((v - lo) / span).toFloat().coerceIn(0f, 1f)
                val y = h - norm * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }

        // Last-point moss dot with AppInk stroke.
        val last = points.lastOrNull()
        if (last != null) {
            val norm = ((last - lo) / span).toFloat().coerceIn(0f, 1f)
            val cx = w
            val cy = h - norm * h
            val r = 3.5.dp.toPx()
            drawCircle(dotFill, radius = r, center = Offset(cx, cy))
            drawCircle(
                color = dotStroke,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

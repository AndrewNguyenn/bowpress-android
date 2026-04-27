package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk

/**
 * Monoline compound-bow glyph — single stroked Path, viewBox 0 0 32 32.
 * Mirrors the iOS BPBowIcon path: a curved compound-bow riser with small
 * cam arcs at the top and bottom and a horizontal rest/shelf line running
 * to the quiver side.
 */
@Composable
fun BPBowIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    stroke: Dp = 1.3.dp,
    tint: Color = AppPondDk,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 32f
        val path = Path()

        // Main riser curve — upper half (8,3) → (14,16).
        path.moveTo(8 * s, 3 * s)
        path.cubicTo(12 * s, 7 * s, 14 * s, 11 * s, 14 * s, 16 * s)
        // Main riser curve — lower half (14,16) → (8,29).
        path.cubicTo(14 * s, 21 * s, 12 * s, 25 * s, 8 * s, 29 * s)

        // Top cam arc (8,3) → (8,10).
        path.moveTo(8 * s, 3 * s)
        path.cubicTo(6 * s, 5 * s, 6 * s, 8 * s, 8 * s, 10 * s)

        // Bottom cam arc (8,29) → (8,22).
        path.moveTo(8 * s, 29 * s)
        path.cubicTo(6 * s, 27 * s, 6 * s, 24 * s, 8 * s, 22 * s)

        // Upper limb (8,10) → (14,16).
        path.moveTo(8 * s, 10 * s)
        path.lineTo(14 * s, 16 * s)

        // Lower limb (8,22) → (14,16).
        path.moveTo(8 * s, 22 * s)
        path.lineTo(14 * s, 16 * s)

        // Rest/shelf (14,16) → (28,16).
        path.moveTo(14 * s, 16 * s)
        path.lineTo(28 * s, 16 * s)

        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = stroke.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

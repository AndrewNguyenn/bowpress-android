package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppMoss
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtWhite
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow

enum class BPTargetStyle { WA, ImpactMap }

private data class Ring(
    val ratio: Float,
    val fill: Color? = null,
    val stroke: Color? = null,
    val strokeWidth: Float = 0.5f,
)

/** World-Archery 10-ring face — real colors, never reskinned. */
private val waRings = listOf(
    Ring(0.96f, fill = AppTgtWhite, stroke = AppInk, strokeWidth = 0.3f),
    Ring(0.86f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.76f, fill = AppTgtBlack),
    Ring(0.66f, stroke = AppTgtWhite, strokeWidth = 0.25f),
    Ring(0.56f, fill = AppTgtBlue),
    Ring(0.46f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.36f, fill = AppTgtRed),
    Ring(0.26f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.16f, fill = AppTgtYellow),
    Ring(0.08f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.014f, fill = AppInk),
)

/** Pond-gradient Impact Map — from analytics-japanese.html lines 476–488. */
private val impactRings = listOf(
    Ring(0.94f, fill = Color(0xFFD9E1D8)),
    Ring(0.84f, fill = Color(0xFFC9D4C9)),
    Ring(0.74f, fill = Color(0xFFB2C3C2)),
    Ring(0.64f, fill = Color(0xFF8FB3BF)),
    Ring(0.54f, fill = Color(0xFF6D9AA8)),
    Ring(0.44f, fill = Color(0xFF4A7989)),
    Ring(0.34f, fill = Color(0xFF3A6878)),
    Ring(0.24f, fill = Color(0xFF2D5A6B)),
    Ring(0.14f, fill = Color(0xFF1E3E4A)),
    Ring(0.07f, fill = Color(0xFF1F2A26)),
    Ring(0.032f, stroke = AppMoss, strokeWidth = 0.6f),
    Ring(0.008f, fill = AppMoss),
)

@Composable
fun BPTargetFace(
    size: Dp,
    modifier: Modifier = Modifier,
    style: BPTargetStyle = BPTargetStyle.WA,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(modifier = modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val rings = if (style == BPTargetStyle.WA) waRings else impactRings
            val px = this.size.width
            val center = Offset(px / 2f, px / 2f)
            rings.forEach { ring ->
                val radius = ring.ratio * px / 2f
                ring.fill?.let { drawCircle(it, radius, center) }
                ring.stroke?.let {
                    drawCircle(
                        color = it,
                        radius = radius,
                        center = center,
                        style = Stroke(width = ring.strokeWidth.coerceAtLeast(0.5f)),
                    )
                }
            }
        }
        overlay?.invoke(this)
    }
}

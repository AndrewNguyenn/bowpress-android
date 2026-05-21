package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppMoss
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtWhite
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow

/**
 * Visual style of [BPTargetFace].
 *  - [WA] (default) — real World-Archery paint (white/black/blue/red/yellow),
 *    used for every face-as-foreground surface.
 *  - [ImpactMap] — pond-gradient quiet-rings variant for the Analytics Impact
 *    Map, so centroids/shift arrows read as data, not decoration.
 */
enum class BPTargetStyle { WA, ImpactMap }

/**
 * Which World-Archery face [BPTargetFace] renders. Mirrors iOS
 * `BPTargetFace.FaceType`.
 *  - [TenRing] — the full WA 10-ring face (rings 1–X, 5 colour bands).
 *  - [SixRing] — the inner-face scoring zone; its exact ring layout depends
 *    on [BPSixRingStyle].
 *
 * Default is [TenRing] so existing call sites that omit `face` are unaffected.
 */
enum class BPTargetFaceType { TenRing, SixRing }

/**
 * Visual variant of the [BPTargetFaceType.SixRing] face. Mirrors iOS
 * `BPTargetFace.SixRingStyle`. WA distinguishes two inner-face variants:
 *  - [Vegas] — 40cm indoor 6-zone (rings 6,7,8,9,10,X). Blue is a single band;
 *    there is no ring 5 on this face.
 *  - [Outdoor80] — 80cm WA compound inner face (rings 5,6,7,8,9,10,X = 7
 *    zones). Blue is split into two bands.
 *
 * Default [Outdoor80] matches the pre-existing icon.
 */
enum class BPSixRingStyle { Vegas, Outdoor80 }

private data class Ring(
    val ratio: Float,
    val fill: Color? = null,
    val stroke: Color? = null,
    val strokeWidth: Float = 0.5f,
)

/**
 * WA 10-ring face. Ratios match the analytics-japanese.html reference (SVG
 * viewBox 0 0 200 200, radii 96…1.4 ÷ 100). Mirrors iOS `tenRing`.
 */
private val tenRingRings = listOf(
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

/**
 * 40cm Vegas inner face — 6 zones (rings 6,7,8,9,10,X), single blue band.
 * Mirrors iOS `sixRing` / `.vegas`.
 */
private val sixRingVegasRings = listOf(
    Ring(0.96f, fill = AppTgtBlue),
    Ring(0.66f, fill = AppTgtRed),
    Ring(0.50f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.34f, fill = AppTgtYellow),
    Ring(0.17f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.014f, fill = AppInk),
)

/**
 * 80cm WA compound inner face — 7 zones (rings 5,6,7,8,9,10,X), blue split
 * into two bands. Mirrors iOS `sixRing` / `.outdoor80`.
 */
private val sixRingOutdoor80Rings = listOf(
    Ring(0.96f, fill = AppTgtBlue),
    Ring(0.80f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.66f, fill = AppTgtRed),
    Ring(0.50f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.34f, fill = AppTgtYellow),
    Ring(0.17f, stroke = AppInk, strokeWidth = 0.25f),
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

/**
 * World-Archery target face — flat, real ring colours, never reskinned.
 * Mirrors iOS `BPTargetFace`.
 *
 * @param face which WA face to draw (default [BPTargetFaceType.TenRing] so
 *   existing callers are unaffected).
 * @param style [BPTargetStyle.WA] real paint, or [BPTargetStyle.ImpactMap]
 *   pond-gradient. ImpactMap ignores [face].
 * @param sixRingStyle visual variant of the six-ring face — only consulted
 *   when `face == SixRing`.
 * @param showCrosshair draws the dashed maple ±11r ring + centre hairlines.
 */
@Composable
fun BPTargetFace(
    size: Dp,
    modifier: Modifier = Modifier,
    face: BPTargetFaceType = BPTargetFaceType.TenRing,
    style: BPTargetStyle = BPTargetStyle.WA,
    sixRingStyle: BPSixRingStyle = BPSixRingStyle.Outdoor80,
    showCrosshair: Boolean = false,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(modifier = modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val rings = ringsFor(face, style, sixRingStyle)
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

            if (showCrosshair) {
                // Dashed ±11r maple circle (r = 11 in the 200-viewBox SVG →
                // ratio 0.11 of size) + centre hairlines. Mirrors iOS.
                drawCircle(
                    color = AppMaple.copy(alpha = 0.7f),
                    radius = px * 0.11f / 2f,
                    center = center,
                    style = Stroke(
                        width = 0.6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f)),
                    ),
                )
                val arm = px * 0.04f
                val gap = px * 0.03f
                val crosshair = AppMaple.copy(alpha = 0.7f)
                drawLine(crosshair, Offset(center.x, center.y - gap - arm), Offset(center.x, center.y - gap), 0.6f)
                drawLine(crosshair, Offset(center.x, center.y + gap), Offset(center.x, center.y + gap + arm), 0.6f)
                drawLine(crosshair, Offset(center.x - gap - arm, center.y), Offset(center.x - gap, center.y), 0.6f)
                drawLine(crosshair, Offset(center.x + gap, center.y), Offset(center.x + gap + arm, center.y), 0.6f)
            }
        }
        overlay?.invoke(this)
    }
}

/** Resolve the ring stack for a face/style combination. */
private fun ringsFor(
    face: BPTargetFaceType,
    style: BPTargetStyle,
    sixRingStyle: BPSixRingStyle,
): List<Ring> = when {
    style == BPTargetStyle.ImpactMap -> impactRings
    face == BPTargetFaceType.TenRing -> tenRingRings
    sixRingStyle == BPSixRingStyle.Vegas -> sixRingVegasRings
    else -> sixRingOutdoor80Rings
}

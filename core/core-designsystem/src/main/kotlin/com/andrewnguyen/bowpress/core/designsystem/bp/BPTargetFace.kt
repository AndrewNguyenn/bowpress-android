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
import com.andrewnguyen.bowpress.core.model.ShootingDistance

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
 * Vegas is the safe default for callers that don't know their distance —
 * matches the printed 40cm indoor face the picker icon shows + aligns
 * with [forDistance(null)] and every iOS feed-card / detail caller.
 */
enum class BPSixRingStyle {
    Vegas, Outdoor80;

    companion object {
        /**
         * Pick the visual variant by the session's distance. 50m / 70m
         * outdoor → [Outdoor80]; everything else (20yd indoor, unknown,
         * null) → [Vegas]. The single canonical helper for the 4-way
         * fan-out (setup picker icon, feed card, friend detail, session
         * detail). Mirrors iOS `socialSixRingStyle(forDistance:)` /
         * `sixRingStyleForCurrentDistance`.
         */
        fun forDistance(distance: ShootingDistance?): BPSixRingStyle = when (distance) {
            ShootingDistance.METERS_50, ShootingDistance.METERS_70 -> Outdoor80
            ShootingDistance.YARDS_20, null -> Vegas
        }
    }
}

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
 * 40cm Vegas inner face — 6 zones (rings 6,7,8,9,10,X), single thin blue
 * band. Implements the iOS commit 65258d8 ratio fix (20/40/40 reference)
 * while preserving the 0.96 outer paper margin every Android icon shares —
 * so the band widths land at ~17% blue / ~42% red / ~41% yellow rather
 * than the bare-canvas 20/40/40 of the SVG reference. The blue band is
 * the thin outermost annulus, not the chunky 30% double band the picker
 * was previously drawing.
 *
 * 200-viewBox SVG reference: r=40 (yellow outer) / 32 (red outer) / 16
 * (blue outer) → 0.20 / 0.40 / 0.80 if rendered to the canvas edge. With
 * our 0.96 paper-margin scale they map to:
 *  - 0.96 → 0.80 outer blue (~17% canvas width)
 *  - 0.80 → 0.40 red (~42%)
 *  - 0.40 → 0.014 yellow + X dot (~41%)
 *
 * NB: scoring math (TargetGeometry.SixRing) is unchanged — this is a purely
 * visual ratio fix on the picker icon / SixRing-decorative variant.
 */
private val sixRingVegasRings = listOf(
    Ring(0.96f, fill = AppTgtBlue),
    Ring(0.80f, fill = AppTgtRed),
    // Single decorative divider at the 7/8 ring boundary inside red.
    // iOS BPTargetFace.swift's `.vegas` path uses this exact position;
    // earlier versions of this list had two strokes at 0.64 + 0.48 which
    // don't correspond to any WA scoring boundary and broke parity.
    Ring(0.60f, stroke = AppInk, strokeWidth = 0.25f),
    Ring(0.40f, fill = AppTgtYellow),
    Ring(0.20f, stroke = AppInk, strokeWidth = 0.25f),
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
    sixRingStyle: BPSixRingStyle = BPSixRingStyle.Vegas,
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

package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtWhite
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.MultiSpotGeometry
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout

/**
 * Read-only World-Archery target with arrow dots overlaid — the static
 * shot-distribution render.
 *
 * Promoted to core-designsystem so both `feature-analytics`
 * (`SessionDetailScreen`) and `feature-social` (`FriendSessionDetailScreen`)
 * share one component. Uses the `AppTgt*` design tokens for the ring fills.
 *
 * Respects the session's [faceType] and [layout]:
 *  - [TargetFaceType.TEN_RING] draws the full 10-ring face (5 colour bands);
 *    [TargetFaceType.SIX_RING] draws the inner-6 indoor face (yellow/red/blue
 *    only), matching how it is scored 6..X.
 *  - [TargetLayout.SINGLE] draws one face; [TargetLayout.TRIANGLE] draws the
 *    3-spot Vegas triangle (two faces top, one below); [TargetLayout.VERTICAL]
 *    draws the 3-spot vertical strip (three faces stacked).
 *
 * For a multi-spot layout the arrows are placed by **position** — each arrow's
 * stored `plotX/plotY` (∈ [-1, 1] of the whole *card*, east/south-positive)
 * is bucketed to its nearest spot via [MultiSpotGeometry.assignArrows] and
 * drawn at its recentered spot-local position. This matches how a live
 * multi-spot session is scored (nearest-spot, not order-within-end) and
 * mirrors iOS `AggregateMultiSpotFace` / `PerSpotBreakdown`. Plots missing
 * coordinates are skipped.
 */
@Composable
fun BPPlottedTarget(
    arrows: List<ArrowPlot>,
    modifier: Modifier = Modifier,
    faceType: TargetFaceType = TargetFaceType.TEN_RING,
    layout: TargetLayout = TargetLayout.SINGLE,
    // §B3 — when the session was a 6-ring at 50/70m the renderer should
    // paint the 80cm WA compound outdoor 7-zone face (split blue + outer
    // ring 5) instead of the Vegas 6-zone card. The variant is picked by
    // the caller (it knows the session distance) — typically via
    // `BPSixRingStyle.forDistance(distance)`. Defaults to Vegas so
    // existing callers without distance context don't change.
    sixRingStyle: BPSixRingStyle = BPSixRingStyle.Vegas,
    // §B1 — archer's shaft outside diameter in mm. Drives the plot-dot
    // size so a 9mm Black Eagle reads visibly fatter than a 4mm X10
    // against the printed face. Null falls back to a 6mm midline so a
    // session without an arrow-config reference still renders sensible
    // dots. Mirrors iOS `arrowDiameterMm` on `TargetArrowDotsOverlay` /
    // `SessionHeatMapView`.
    arrowDiameterMm: Double? = null,
    // Visibility floor for the rendered dot — caps the geometric formula
    // at a readable minimum so a thumbnail-scale face (feed card) doesn't
    // collapse every dot to sub-pixel. iOS uses ~3pt diameter (1.5pt
    // radius) for the feed (see [FEED_MIN_DOT_RADIUS]) and 2pt radius for
    // the detail; both fit inside the 2dp default. Mirrors iOS `minDotSize`.
    minDotRadius: Dp = 2.dp,
) {
    val multiSpot = MultiSpotGeometry.preset(layout)
    val shaftMm = (arrowDiameterMm ?: DEFAULT_SHAFT_MM).toFloat()
    val minDotRadiusPx = with(LocalDensity.current) { minDotRadius.toPx() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            // The Vegas card is square — both triangle and vertical spot
            // presets are normalised to a 1:1 face square.
            .aspectRatio(1f),
    ) {
        if (multiSpot == null) {
            // Single face — one bullseye, plotX/plotY relative to its centre.
            val center = Offset(size.width / 2f, size.height / 2f)
            val faceRadius = minOf(size.width, size.height) / 2f
            val dotRadius = singleFaceDotRadiusPx(
                faceRadiusPx = faceRadius,
                shaftMm = shaftMm,
                faceType = faceType,
                sixRingStyle = sixRingStyle,
                minDotRadiusPx = minDotRadiusPx,
            )
            drawFace(center, faceRadius, faceType, sixRingStyle)
            arrows.forEach { plot ->
                val px = plot.plotX ?: return@forEach
                val py = plot.plotY ?: return@forEach
                drawArrowDot(
                    at = Offset(
                        center.x + px.toFloat() * faceRadius,
                        center.y + py.toFloat() * faceRadius,
                    ),
                    dotRadius = dotRadius,
                )
            }
            return@Canvas
        }

        // Multi-spot Vegas card — three 6-ring spots. Spot centres + radius
        // come from the shared geometry (normalised to the face square).
        val minEdge = minOf(size.width, size.height)
        val spotRadius = multiSpot.radiusNorm.toFloat() * minEdge
        val spotCentres = multiSpot.centers.map { c ->
            Offset(c.x.toFloat() * size.width, c.y.toFloat() * size.height)
        }
        spotCentres.forEach { drawFace(it, spotRadius, TargetFaceType.SIX_RING, sixRingStyle) }
        val dotRadius = multiSpotDotRadiusPx(
            spotRadiusPx = spotRadius,
            shaftMm = shaftMm,
            minDotRadiusPx = minDotRadiusPx,
        )
        // Position-based bucketing — each arrow on its actual spot, drawn at
        // its recentered spot-local offset (localX/localY ∈ −1..1 of the
        // spot radius). Mirrors the live-session scoring path.
        multiSpot.assignArrows(arrows).forEach { perSpot ->
            val centre = spotCentres[perSpot.spotIndex]
            drawArrowDot(
                at = Offset(
                    centre.x + perSpot.localX.toFloat() * spotRadius,
                    centre.y + perSpot.localY.toFloat() * spotRadius,
                ),
                dotRadius = dotRadius,
            )
        }
    }
}

/**
 * Ring bands for a face, outer → inner. A 10-ring face has all five colour
 * bands; a 6-ring face is the inner-6 — yellow/red/blue only — so its bands
 * span the full radius. The 6-ring outdoor variant adds an outermost blue
 * sub-band so ring 5 lives at the canvas edge (`rings.first == 1.0`).
 *
 * §B3 — the [BPSixRingStyle] argument selects between Vegas (40cm indoor,
 * single blue band, rings 6..X) and Outdoor80 (80cm WA compound, split blue
 * band, rings 5..X). Mirrors iOS `BPTargetFace.SixRingStyle`.
 */
private fun ringBands(
    faceType: TargetFaceType,
    sixRingStyle: BPSixRingStyle,
): List<Pair<Float, Color>> = when (faceType) {
    TargetFaceType.TEN_RING -> listOf(
        1.0f to AppTgtWhite, 0.9f to AppTgtWhite,
        0.8f to AppTgtBlack, 0.7f to AppTgtBlack,
        0.6f to AppTgtBlue, 0.5f to AppTgtBlue,
        0.4f to AppTgtRed, 0.3f to AppTgtRed,
        0.2f to AppTgtYellow, 0.1f to AppTgtYellow,
    )
    TargetFaceType.SIX_RING -> when (sixRingStyle) {
        // 40cm Vegas indoor 6-zone — ONE thin blue ring (6), TWO red rings
        // (7,8), THREE yellow zones (9,10,X). Ring outer edges sit at
        // 1.0, 0.833, 0.667, 0.5, 0.333, 0.167 (each 1/6 of the face
        // radius). Bands are layered outside-in: the colour at index i is
        // visible from `edges[i+1]` to `edges[i]`, so the colour change
        // points (not every entry) define the visible bands. The earlier
        // 1/3-each layout (blue 0.667→1.0, red 0.333→0.667, yellow
        // 0→0.333) mis-painted rings 7 and 9 as blue and red.
        BPSixRingStyle.Vegas -> listOf(
            1.0f to AppTgtBlue,     // ring 6 — blue fills 0.833→1.0
            0.833f to AppTgtRed,    // colour flip — red from here in
            0.667f to AppTgtRed,    // hairline between rings 7 and 8
            0.5f to AppTgtYellow,   // colour flip — yellow from here in
            0.333f to AppTgtYellow, // hairline between rings 9 and 10
            0.167f to AppTgtYellow, // hairline at the X ring outer
        )
        // 80cm WA compound 7-zone — equal-width 1/6 bands, ring 5's outer
        // edge at 1.0; blue is split into ring 5 (outer) + ring 6 (inner),
        // each one 1/6 wide. Mirrors iOS `sixRingOutdoor` paint stack.
        BPSixRingStyle.Outdoor80 -> listOf(
            1.0f to AppTgtBlue, 5f / 6f to AppTgtBlue,
            4f / 6f to AppTgtRed, 3f / 6f to AppTgtRed,
            2f / 6f to AppTgtYellow, 1f / 6f to AppTgtYellow,
        )
    }
}

/** Draw one WA face centred at [center] with the given [radius]. */
private fun DrawScope.drawFace(
    center: Offset,
    radius: Float,
    faceType: TargetFaceType,
    sixRingStyle: BPSixRingStyle,
) {
    val bands = ringBands(faceType, sixRingStyle)
    bands.forEach { (edge, color) ->
        drawCircle(color = color, radius = edge * radius, center = center)
    }
    // Outer edge + hair-thin ring dividers.
    drawCircle(color = DIVIDER, radius = radius, center = center, style = Stroke(width = 1.5f))
    bands.drop(1).map { it.first }.distinct().forEach { edge ->
        drawCircle(
            color = DIVIDER,
            radius = edge * radius,
            center = center,
            style = Stroke(width = 0.5f),
        )
    }
    // X tick at centre.
    drawCircle(color = DIVIDER, radius = 0.05f * radius, center = center, style = Stroke(width = 0.8f))
}

/** Draw one arrow dot — ink fill, paper hairline so it reads on every band. */
private fun DrawScope.drawArrowDot(at: Offset, dotRadius: Float) {
    drawCircle(color = AppInk, radius = dotRadius, center = at)
    drawCircle(color = AppPaper, radius = dotRadius, center = at, style = Stroke(width = 1f))
}

/**
 * Plot-dot radius for a multi-spot Vegas card. Each spot is a real-world
 * 180mm-diameter face (per WA Vegas spec — see [TargetLayout.SPOT_DIAMETER_MM]),
 * so a shaft-diameter-mm dot maps to `shaftMm / 180 * spotRadius` in px (the
 * shaft is measured against the spot diameter, then converted to the spot's
 * drawn pixel radius). Floored at [minDotRadiusPx] so a thumbnail-scale spot
 * (e.g. the 168dp feed card) doesn't drop the dot below pixel visibility.
 * Mirrors iOS `arrowDotSize` multi-spot branch (HistoricalSessionsView, the
 * `multiSpotGeometry` block of `arrowDotSize`).
 */
internal fun multiSpotDotRadiusPx(
    spotRadiusPx: Float,
    shaftMm: Float,
    minDotRadiusPx: Float,
): Float = (shaftMm / TargetLayout.SPOT_DIAMETER_MM.toFloat() * spotRadiusPx)
    .coerceAtLeast(minDotRadiusPx)

/**
 * Plot-dot radius for a single-face render. The shaft is measured against
 * the printed face's physical radius (`mmPerNormUnit`), then mapped to the
 * drawn face's pixel radius. Floored at [minDotRadiusPx]. Mirrors iOS
 * `TargetArrowDotsOverlay.dotRadius` (geometric `arrowDiameterMm /
 * geometry.mmPerNormUnit * faceRadius`).
 */
internal fun singleFaceDotRadiusPx(
    faceRadiusPx: Float,
    shaftMm: Float,
    faceType: TargetFaceType,
    sixRingStyle: BPSixRingStyle,
    minDotRadiusPx: Float,
): Float {
    val mmPerNormUnit = singleFaceMmPerNormUnit(faceType, sixRingStyle)
    return (shaftMm / mmPerNormUnit * faceRadiusPx).coerceAtLeast(minDotRadiusPx)
}

/**
 * Real-world millimetres at normalised face-radius 1.0 — the divisor that
 * turns an `arrowDiameterMm` value into a face-radius fraction. Vegas
 * indoor = 20/(119/735) ≈ 123.5mm and Outdoor80 = 400mm both match
 * `TargetGeometry.SixRing.mmPerNormUnit` / `SixRingOutdoor.mmPerNormUnit`
 * exactly. TenRing reuses Vegas's value rather than the printed 122cm
 * face's 610mm — matches Android's existing
 * `TargetGeometry.TenRing.mmPerNormUnit = SixRing.mmPerNormUnit` choice
 * (see TargetGeometry comment: dot radius stays consistent across faces).
 * This is the one place where Android's plot-dot convention diverges from
 * iOS (iOS uses the physical 610mm radius for TenRing), but no caller of
 * `BPPlottedTarget` renders a single TenRing face in production today.
 *
 * Kept here rather than depending on `feature-session/TargetGeometry` so
 * core-designsystem stays free of a feature dep. If a future contributor
 * changes a value in TargetGeometry, the corresponding entry below has
 * to move in lockstep.
 */
private fun singleFaceMmPerNormUnit(
    faceType: TargetFaceType,
    sixRingStyle: BPSixRingStyle,
): Float = when (faceType) {
    TargetFaceType.SIX_RING -> when (sixRingStyle) {
        BPSixRingStyle.Vegas -> VEGAS_MM_PER_NORM_UNIT
        BPSixRingStyle.Outdoor80 -> OUTDOOR_80_MM_PER_NORM_UNIT
    }
    // Intentional: TenRing reuses Vegas's mmPerNormUnit so a given shaft
    // renders the same physical dot on either face — matches the live
    // scoring geometry's choice and keeps own-vs-friend visuals consistent.
    TargetFaceType.TEN_RING -> VEGAS_MM_PER_NORM_UNIT
}

/**
 * Visibility floor for an arrow dot drawn inside a feed-card-thumbnail
 * face — 1.5dp radius ≡ iOS `minDotSize: 3` (which iOS treats as a
 * diameter). Use this when calling [BPPlottedTarget] from the social
 * feed; the 2dp default suits the larger detail-screen surface.
 */
val FEED_MIN_DOT_RADIUS: Dp = 1.5.dp

/** 6mm is the midline target-arrow shaft — `arrowDiameterMm` falls back here. */
private const val DEFAULT_SHAFT_MM: Double = 6.0

/** Vegas 40cm indoor: 20/(119/735) ≈ 123.53mm at radius 1.0. */
private val VEGAS_MM_PER_NORM_UNIT: Float = (20.0 / (119.0 / 735.0)).toFloat()

/** 80cm WA compound outdoor: 400mm at radius 1.0. */
private const val OUTDOOR_80_MM_PER_NORM_UNIT: Float = 400f

private val DIVIDER = Color(0x66000000)

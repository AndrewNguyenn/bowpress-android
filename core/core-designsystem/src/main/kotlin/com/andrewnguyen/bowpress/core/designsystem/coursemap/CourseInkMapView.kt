package com.andrewnguyen.bowpress.core.designsystem.coursemap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.CourseStation
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.text.font.FontWeight

// ---------------------------------------------------------------------------
// Layout model
// ---------------------------------------------------------------------------

/**
 * A topographic contour traced into the map's normalized 0…1 space.
 *
 * @property depth 0 = lowest contour, 1 = highest — drives the ink tone.
 * @property segments Segment endpoints — each consecutive pair is one line.
 */
data class ProjectedContour(
    val depth: Double,
    val segments: List<Offset>,
)

/**
 * Resolves the on-screen layout of a 3D course — station pins, the walked
 * trail, the walker's position, the *inferred target positions*, and real
 * topographic contours — all in a normalized 0…1 space.
 *
 * Mirrors iOS `CourseMapLayout`. Three cases: a real elevation grid (project
 * against its fixed geographic box and trace real contours), GPS-but-no-grid
 * (project route + pins + targets from their own spread), and no GPS at all
 * (a deterministic synthesized meander — the map never fakes terrain it does
 * not have, so no contours in the last two cases).
 *
 * Offsets are normalized: 0..1 across the map's width / height.
 */
data class CourseMapLayout(
    val stations: List<Offset>,
    /** Inferred target for each station, parallel to [stations]; null when
     *  the station lacks the GPS / bearing / distance to project a target. */
    val targets: List<Offset?>,
    val trail: List<Offset>,
    val current: Offset?,
    /** True when the layout came from real GPS rather than the synthesizer. */
    val isGeoreferenced: Boolean,
    /** Real topographic contours — empty unless an elevation grid was given. */
    val contours: List<ProjectedContour>,
    /**
     * Width : height of the course content — 1 unless [resolve] was asked to
     * fill the frame, in which case it carries the course's true aspect so
     * the map view can shape itself to match.
     */
    val contentAspect: Float = 1f,
) {
    /**
     * Turn the whole layout 90° when the course runs taller than wide, so it
     * always lays out horizontally into a landscape frame. Every point — pins,
     * targets, trail, contours — turns together, so the dashed shot arrows and
     * their upright yardage labels stay correct, just rotated. Mirrors iOS
     * `CourseMapLayout.orientedHorizontally()`.
     */
    fun orientedHorizontally(): CourseMapLayout {
        if (contentAspect >= 1f) return this
        // 90° turn within the unit square: (x, y) → (1 - y, x).
        fun turn(p: Offset) = Offset(1f - p.y, p.x)
        return copy(
            stations = stations.map(::turn),
            targets = targets.map { it?.let(::turn) },
            trail = trail.map(::turn),
            current = current?.let(::turn),
            contours = contours.map {
                ProjectedContour(depth = it.depth, segments = it.segments.map(::turn))
            },
            contentAspect = if (contentAspect > 0f) 1f / contentAspect else 1f,
        )
    }

    companion object {
        /** Inset the projected content sits within, leaving room for chrome. */
        const val PAD: Double = 0.10

        /** Metres per yard — `estimatedDistance` is stored in the station's
         *  own `distanceUnit`, normalized to metres for the target maths. */
        private const val METRES_PER_YARD = 0.9144

        fun resolve(
            stations: List<CourseStation>,
            breadcrumb: List<GeoPoint>,
            current: GeoPoint?,
            grid: ElevationGrid?,
            /** When true the projection keeps the course's true aspect
             *  (rather than squaring it) so the map view can shape itself to
             *  match and the course fills the frame edge-to-edge. */
            fillFrame: Boolean = false,
        ): CourseMapLayout {
            val stationGeo: List<GeoPoint?> = stations.map { s ->
                val lat = s.latitude
                val lon = s.longitude
                if (lat != null && lon != null) GeoPoint(lat, lon) else null
            }
            val targetGeo: List<GeoPoint?> = stations.map(::inferredTargetGeo)
            val allGeo = stations.isNotEmpty() && stationGeo.all { it != null }

            // Best case — a terrain grid: project everything against its
            // fixed geographic box and trace real contours.
            if (grid != null) {
                val proj = Projector(
                    grid.minLat, grid.maxLat, grid.minLon, grid.maxLon,
                    squared = !fillFrame,
                )
                val contours = ContourGenerator.contours(grid).map { line ->
                    ProjectedContour(
                        depth = line.depth,
                        segments = line.segments.map(proj::project),
                    )
                }
                return CourseMapLayout(
                    stations = stationGeo.map { it?.let(proj::project) ?: Offset(0.5f, 0.5f) },
                    targets = targetGeo.map { it?.let(proj::project) },
                    trail = breadcrumb.map(proj::project),
                    current = current?.let(proj::project),
                    isGeoreferenced = true,
                    contours = contours,
                    contentAspect = proj.aspect,
                )
            }

            // GPS but no terrain grid — project the route + pins + targets
            // from their own spread (targets included so they fit the
            // frame); no contours.
            val sample = buildList {
                addAll(stationGeo.filterNotNull())
                addAll(targetGeo.filterNotNull())
                addAll(breadcrumb)
                if (current != null) add(current)
            }
            val spreadProj = if (allGeo) {
                Projector.fromPoints(sample, squared = !fillFrame)
            } else {
                null
            }
            if (spreadProj != null) {
                return CourseMapLayout(
                    stations = stationGeo.map { it?.let(spreadProj::project) ?: Offset(0.5f, 0.5f) },
                    targets = targetGeo.map { it?.let(spreadProj::project) },
                    trail = breadcrumb.map(spreadProj::project),
                    current = current?.let(spreadProj::project),
                    isGeoreferenced = true,
                    contours = emptyList(),
                    contentAspect = spreadProj.aspect,
                )
            }

            // No GPS at all — deterministic synthesized meander, no contours.
            // Targets are still offset from each pin by the captured bearing
            // so the feature is verifiable on a Simulator with no location.
            val synthStations = synthesizedPath(stations.size)
            return CourseMapLayout(
                stations = synthStations,
                targets = stations.zip(synthStations) { s, pin -> synthesizedTarget(s, pin) },
                trail = emptyList(),
                current = if (current != null) synthesizedCurrent(stations.size) else null,
                isGeoreferenced = false,
                contours = emptyList(),
                // The synthesized meander spans ~0.6 wide × ~0.66 tall.
                contentAspect = 0.9f,
            )
        }

        /**
         * The inferred target `GeoPoint` for a station — projected from the
         * shooter pin along the captured compass bearing by the cut
         * (horizontal) distance. Null unless the station carries GPS, a
         * bearing and a ranged distance.
         */
        fun inferredTargetGeo(s: CourseStation): GeoPoint? {
            val lat = s.latitude ?: return null
            val lon = s.longitude ?: return null
            val bearing = s.bearingDegrees ?: return null
            val distance = s.estimatedDistance ?: return null
            val metres = distance * (if (s.distanceUnit == "m") 1.0 else METRES_PER_YARD)
            // Cut distance — the horizontal ground range an inclined shot covers.
            val cut = metres * cos(abs(s.angleDegrees) * Math.PI / 180.0)
            val radians = bearing * Math.PI / 180.0
            val dNorth = cut * cos(radians)
            val dEast = cut * sin(radians)
            // Equirectangular offset — exact enough at 3D-course distances.
            val lat2 = lat + dNorth / 111_320.0
            val lon2 = lon + dEast / (111_320.0 * cos(lat * Math.PI / 180.0))
            return GeoPoint(latitude = lat2, longitude = lon2)
        }

        /** Deterministic station positions along a climbing, wandering route. */
        fun synthesizedPath(count: Int): List<Offset> {
            if (count <= 0) return emptyList()
            return (0 until count).map { i -> synthesizedPoint(i, maxOf(count, 2)) }
        }

        private fun synthesizedPoint(index: Int, n: Int): Offset {
            val t = if (n > 1) index.toDouble() / (n - 1) else 0.5
            val x = 0.20 + 0.60 * (0.5 + 0.42 * sin(t * Math.PI * 2.3))
            val y = 0.84 - 0.66 * t
            return Offset(x.toFloat(), y.toFloat())
        }

        private fun synthesizedCurrent(count: Int): Offset =
            synthesizedPoint(count, maxOf(count + 1, 2))

        /**
         * A target offset from a synthesized pin — no geography, so it uses a
         * fixed visual scale (≈ the prototype's 0.6 px-per-yard over a 366 px
         * map). Cosmetic; the real path is always georeferenced.
         */
        private fun synthesizedTarget(s: CourseStation, pin: Offset): Offset? {
            val bearing = s.bearingDegrees ?: return null
            val distance = s.estimatedDistance ?: return null
            val yards = if (s.distanceUnit == "m") distance / METRES_PER_YARD else distance
            val cut = yards * cos(abs(s.angleDegrees) * Math.PI / 180.0)
            val normPerYard = 0.6 / 366.0
            val radians = bearing * Math.PI / 180.0
            return Offset(
                x = (pin.x + sin(radians) * cut * normPerYard).toFloat(),
                y = (pin.y - cos(radians) * cut * normPerYard).toFloat(),
            )
        }
    }
}

/**
 * Equirectangular projector — longitude scaled by latitude. By default the
 * box is squared to its larger span (a centred, undistorted course);
 * `squared = false` keeps the true span per axis so the course can fill a
 * frame shaped to match it. Mirrors iOS `Projector`.
 */
private class Projector private constructor(
    private val minX: Double,
    private val spanX: Double,
    private val minY: Double,
    private val spanY: Double,
    private val lonScale: Double,
) {
    /** Width : height of the projected box — 1 when squared. */
    val aspect: Float get() = (spanX / spanY).toFloat()

    fun project(p: GeoPoint): Offset {
        val usable = 1.0 - 2 * CourseMapLayout.PAD
        val nx = (p.longitude * lonScale - minX) / spanX
        val ny = (p.latitude - minY) / spanY
        return Offset(
            x = (CourseMapLayout.PAD + nx * usable).toFloat(),
            // Flip: north (max latitude) is the top of the map.
            y = (CourseMapLayout.PAD + (1 - ny) * usable).toFloat(),
        )
    }

    companion object {
        operator fun invoke(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double,
            squared: Boolean = true,
        ): Projector {
            val scale = cos((minLat + maxLat) / 2 * Math.PI / 180.0)
            val sx = maxOf((maxLon - minLon) * scale, 1e-9)
            val sy = maxOf(maxLat - minLat, 1e-9)
            val spanX: Double
            val spanY: Double
            if (squared) {
                val span = maxOf(sx, sy)
                spanX = span
                spanY = span
            } else {
                spanX = sx
                spanY = sy
            }
            return Projector(
                minX = minLon * scale,
                spanX = spanX,
                minY = minLat,
                spanY = spanY,
                lonScale = scale,
            )
        }

        /** Build from a point cloud — null on fewer than 2 points or a
         *  degenerate box (everything within a few metres). */
        fun fromPoints(points: List<GeoPoint>, squared: Boolean = true): Projector? {
            if (points.size < 2) return null
            val lats = points.map { it.latitude }
            val lons = points.map { it.longitude }
            val minLat = lats.min()
            val maxLat = lats.max()
            val minLon = lons.min()
            val maxLon = lons.max()
            val scale = cos((minLat + maxLat) / 2 * Math.PI / 180.0)
            if (maxOf((maxLon - minLon) * scale, maxLat - minLat) <= 0.00003) return null
            return invoke(minLat, maxLat, minLon, maxLon, squared)
        }
    }
}

// ---------------------------------------------------------------------------
// Viewport
// ---------------------------------------------------------------------------

/**
 * Pan / pinch-zoom state for the map. Zoom is anchored on the map centre;
 * [offset] is clamped so the scaled map always fills the frame. Mirrors
 * iOS `MapViewport`.
 */
data class MapViewport(
    val zoom: Float = 1f,
    val offset: Offset = Offset.Zero,
) {
    /** Project a normalized 0…1 point into transformed screen space. */
    fun place(normalized: Offset, size: Size): Offset {
        val baseX = normalized.x * size.width
        val baseY = normalized.y * size.height
        val cx = size.width / 2f
        val cy = size.height / 2f
        return Offset(
            x = cx + (baseX - cx) * zoom + offset.x,
            y = cy + (baseY - cy) * zoom + offset.y,
        )
    }

    /** Clamp [offset] so the scaled map never reveals empty paper. */
    fun clampingOffset(size: Size): MapViewport {
        val maxX = maxOf(0f, (zoom - 1f) * size.width / 2f)
        val maxY = maxOf(0f, (zoom - 1f) * size.height / 2f)
        return copy(
            offset = Offset(
                x = offset.x.coerceIn(-maxX, maxX),
                y = offset.y.coerceIn(-maxY, maxY),
            ),
        )
    }

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 5f

        /** Above this zoom the map counts as "zoomed in" — it claims pan
         *  drags and shows the zoom chip. Below it, drags fall through. */
        const val ZOOMED_THRESHOLD = 1.02f
    }
}

// ---------------------------------------------------------------------------
// The map view
// ---------------------------------------------------------------------------

private const val MAP_ASPECT = 366f / 420f

/**
 * The editorial ink-on-paper course map: faint paper grain, real topographic
 * contour lines (when an elevation grid is available), the walked trail,
 * numbered station pins, and — projected from each station's captured compass
 * bearing — the *inferred target positions* with dashed shot arrows. Tap a
 * pin or a target to focus that shot; the map supports pinch-zoom and
 * drag-to-pan.
 *
 * Mirrors iOS `CourseInkMapView`.
 *
 * @param selectedStation index of the focused shot — drawn in maple, rest dimmed.
 * @param onTapStation null = pins are display-only (no focus interaction).
 * @param showChrome draw the compass + scale-bar chrome, the legend and the
 *   zoom controls (all off for compact thumbnails); also gates interaction.
 * @param adaptiveAspect when true the map shapes itself to the course and
 *   always lays it out horizontally — the course fills the frame edge-to-edge
 *   instead of sitting centred in a fixed portrait box. Used by the social
 *   feed preview. Non-adaptive callers (live / detail maps) are unchanged.
 */
@Composable
fun CourseInkMapView(
    stations: List<CourseStation>,
    breadcrumb: List<GeoPoint> = emptyList(),
    current: GeoPoint? = null,
    elevationGrid: ElevationGrid? = null,
    selectedStation: Int? = null,
    onTapStation: ((Int) -> Unit)? = null,
    showChrome: Boolean = true,
    adaptiveAspect: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val layout = remember(stations, breadcrumb, current, elevationGrid, adaptiveAspect) {
        val resolved = CourseMapLayout.resolve(
            stations, breadcrumb, current, elevationGrid, fillFrame = adaptiveAspect,
        )
        // adaptiveAspect always lays the course out horizontally.
        if (adaptiveAspect) resolved.orientedHorizontally() else resolved
    }
    // Aspect ratio for the map frame. With adaptiveAspect the course is always
    // laid out horizontally, so the frame is always landscape — the course's
    // own (turned) aspect, clamped so it never gets too extreme. Otherwise the
    // fixed portrait box. Mirrors iOS `frameAspect`.
    val frameAspect = if (adaptiveAspect) {
        layout.contentAspect.coerceIn(1.0f, 2.2f)
    } else {
        MAP_ASPECT
    }
    val textMeasurer = rememberTextMeasurer()
    var viewport by remember { mutableStateOf(MapViewport()) }
    // Canvas size in px — captured on draw, used by the gesture handlers.
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val interactive = showChrome
    val hasTarget = layout.targets.any { it != null }

    // Pulsing "you are here" ring — independent of pan / zoom.
    val pulse by rememberInfiniteTransition(label = "walker").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .aspectRatio(frameAspect)
            .background(AppCream)
            .border(1.dp, AppLine)
            .clipToBounds(),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Pinch-zoom — always active when interactive. Pan only
                // claims a one-finger drag while zoomed in; at 1× the drag
                // falls through so an enclosing scroller can still scroll.
                .then(
                    if (interactive) {
                        Modifier.pointerInput(canvasSize) {
                            mapTransformGestures(
                                size = { canvasSize },
                                viewport = { viewport },
                                onViewport = { viewport = it },
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                // Tap focus — a discrete tap, hit-tests against pins and
                // target glyphs. Kept on its own pointerInput so it never
                // competes with the pan drag (mirrors the iOS
                // `simultaneousGesture` fix that stopped the deadlock).
                .then(
                    if (interactive && onTapStation != null) {
                        Modifier.pointerInput(layout, canvasSize) {
                            // Density-scaled so the ~26dp pin hit target is a
                            // real touch target on high-density screens.
                            val hitSlopPx = 26.dp.toPx()
                            mapTapGesture { point ->
                                handleTap(point, canvasSize, viewport, layout, hitSlopPx, onTapStation)
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            canvasSize = size
            drawMap(
                layout = layout,
                viewport = viewport,
                selectedStation = selectedStation,
                stations = stations,
                showChrome = showChrome,
                adaptiveAspect = adaptiveAspect,
                textMeasurer = textMeasurer,
            )
            layout.current?.let { cur ->
                drawWalkerDot(viewport.place(cur, size), pulse)
            }
        }

        if (showChrome) {
            if (hasTarget) {
                Legend(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                )
            }
            if (viewport.zoom > MapViewport.ZOOMED_THRESHOLD) {
                ZoomChip(
                    zoom = viewport.zoom,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Gestures
// ---------------------------------------------------------------------------

/**
 * Pinch-to-zoom plus pan. Pan only claims a one-finger drag once the map is
 * zoomed past [MapViewport.ZOOMED_THRESHOLD]; below that the drag is left
 * unconsumed so an enclosing scroll container can still scroll. This mirrors
 * the iOS `simultaneousGesture` arrangement that kept tap and pan from
 * deadlocking.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.mapTransformGestures(
    size: () -> Size,
    viewport: () -> MapViewport,
    onViewport: (MapViewport) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }
            if (pointers.isEmpty()) break

            val canvas = size()
            if (canvas == Size.Zero) continue

            if (pointers.size >= 2) {
                // Two fingers — pinch-zoom about the map centre.
                val zoomChange = event.calculateZoom()
                if (zoomChange != 1f) {
                    val current = viewport()
                    val next = (current.zoom * zoomChange).coerceIn(
                        MapViewport.MIN_ZOOM,
                        MapViewport.MAX_ZOOM,
                    )
                    onViewport(current.copy(zoom = next).clampingOffset(canvas))
                    event.changes.forEach { it.consume() }
                }
            } else {
                // One finger — pan, but only while zoomed in.
                val change = pointers.first()
                val current = viewport()
                if (current.zoom > MapViewport.ZOOMED_THRESHOLD) {
                    val delta = change.positionChange()
                    if (delta != Offset.Zero) {
                        val moved = current.copy(
                            offset = current.offset + delta,
                        ).clampingOffset(canvas)
                        onViewport(moved)
                        change.consume()
                    }
                }
                // At 1× the drag is intentionally left unconsumed.
            }
        } while (event.changes.any { it.pressed })
    }
}

/** Helper — pinch ratio between this and the previous pointer event. */
private fun androidx.compose.ui.input.pointer.PointerEvent.calculateZoom(): Float {
    val pressed = changes.filter { it.pressed }
    if (pressed.size < 2) return 1f
    fun centroidSize(positionSelector: (androidx.compose.ui.input.pointer.PointerInputChange) -> Offset): Float {
        val centroid = pressed.fold(Offset.Zero) { acc, c -> acc + positionSelector(c) } / pressed.size.toFloat()
        var sum = 0f
        pressed.forEach { c -> sum += (positionSelector(c) - centroid).getDistance() }
        return sum / pressed.size
    }
    val current = centroidSize { it.position }
    val previous = centroidSize { it.previousPosition }
    return if (previous == 0f) 1f else current / previous
}

/**
 * A discrete tap detector. Reports the up-position only when the gesture was
 * a genuine tap (no significant movement), so it never competes with pan.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.mapTapGesture(
    onTap: (Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        var moved = false
        var multiTouch = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } > 1) multiTouch = true
            event.changes.forEach { c ->
                if ((c.position - down.position).getDistance() > slop) moved = true
            }
        } while (event.changes.any { it.pressed })
        if (!moved && !multiTouch) onTap(down.position)
    }
}

/**
 * Tap focus — hit-test against pins and target glyphs in transformed screen
 * space, nearest wins, within ~26dp.
 */
private fun handleTap(
    point: Offset,
    size: Size,
    viewport: MapViewport,
    layout: CourseMapLayout,
    hitSlopPx: Float,
    onTapStation: (Int) -> Unit,
) {
    if (size == Size.Zero) return
    var bestIdx = -1
    var bestDist = Float.MAX_VALUE
    fun consider(idx: Int, anchor: Offset) {
        val d = hypot(anchor.x - point.x, anchor.y - point.y)
        if (d < bestDist) {
            bestDist = d
            bestIdx = idx
        }
    }
    layout.stations.indices.forEach { idx ->
        consider(idx, viewport.place(layout.stations[idx], size))
        layout.targets[idx]?.let { consider(idx, viewport.place(it, size)) }
    }
    if (bestIdx >= 0 && bestDist <= hitSlopPx) onTapStation(bestIdx)
}

// ---------------------------------------------------------------------------
// Canvas drawing
// ---------------------------------------------------------------------------

private fun DrawScope.drawMap(
    layout: CourseMapLayout,
    viewport: MapViewport,
    selectedStation: Int?,
    stations: List<CourseStation>,
    showChrome: Boolean,
    adaptiveAspect: Boolean,
    textMeasurer: TextMeasurer,
) {
    val w = size.width
    val h = size.height
    fun pt(p: Offset): Offset = viewport.place(p, size)

    // Paper grain — faint horizontal rules (screen texture, not terrain, so
    // it does not pan or zoom).
    val rows = (h / 16f).toInt()
    if (rows > 0) {
        for (i in 0..rows) {
            val y = i * 16f
            drawLine(
                color = AppLine2.copy(alpha = 0.25f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 0.4f,
            )
        }
    }

    // Real topographic contours, traced from the elevation grid. Higher
    // contours read slightly darker, like an inked survey map.
    layout.contours.forEach { contour ->
        val path = Path()
        var i = 0
        while (i + 1 < contour.segments.size) {
            val a = pt(contour.segments[i])
            val b = pt(contour.segments[i + 1])
            path.moveTo(a.x, a.y)
            path.lineTo(b.x, b.y)
            i += 2
        }
        val tone = (0.30 + 0.45 * contour.depth).toFloat()
        drawPath(
            path = path,
            color = AppLine.copy(alpha = tone),
            style = Stroke(width = (0.6 + 0.5 * contour.depth).toFloat()),
        )
    }

    // Trail — the real GPS trace, or a smooth curve through the pins.
    val trailPoints: List<Offset> = if (layout.trail.isEmpty()) {
        layout.stations + listOfNotNull(layout.current)
    } else {
        layout.trail
    }
    if (trailPoints.size >= 2) {
        val path = smoothCurve(trailPoints.map(::pt))
        drawPath(
            path = path,
            // Deliberately faint (0.4) — the trail recedes so the new shot
            // arrows read as the primary line work.
            color = AppInk.copy(alpha = 0.4f),
            style = Stroke(
                width = 1.6f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
            ),
        )
    }

    drawShotArrows(layout, viewport, selectedStation, stations, adaptiveAspect, textMeasurer)
    drawTargets(layout, viewport, selectedStation)
    drawPins(layout, viewport, selectedStation, adaptiveAspect, textMeasurer)

    if (showChrome) {
        drawCompass(Offset(24f, h - 30f), textMeasurer)
        drawScaleBar(Offset(w - 90f, h - 22f), viewport.zoom, textMeasurer)
    }
}

/**
 * Dashed shot arrows from each shooter pin to its inferred target, with a
 * ranged-distance label at the midpoint.
 */
private fun DrawScope.drawShotArrows(
    layout: CourseMapLayout,
    viewport: MapViewport,
    selectedStation: Int?,
    stations: List<CourseStation>,
    adaptiveAspect: Boolean,
    textMeasurer: TextMeasurer,
) {
    layout.stations.indices.forEach { idx ->
        val targetN = layout.targets[idx] ?: return@forEach
        val focused = selectedStation == idx
        val dimmed = selectedStation != null && !focused
        val shooter = viewport.place(layout.stations[idx], size)
        val target = viewport.place(targetN, size)
        val length = hypot(target.x - shooter.x, target.y - shooter.y)
        if (length <= 1f) return@forEach

        val alpha = if (dimmed) 0.18f else 1f
        val ink = if (focused) AppMaple else AppInk

        drawLine(
            color = ink.copy(alpha = ink.alpha * alpha),
            start = shooter,
            end = target,
            strokeWidth = if (focused) 1.4f else 0.9f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2f)),
        )

        // Arrowhead, set just short of the target glyph.
        val ux = (target.x - shooter.x) / length
        val uy = (target.y - shooter.y) / length
        val tip = Offset(target.x - ux * 9f, target.y - uy * 9f)
        drawArrowhead(tip, ux, uy, ink.copy(alpha = ink.alpha * alpha))

        // Distance label — for the focused shot, or for all when nothing is
        // focused. Skipped on very short arrows, and on the compact feed map
        // where a busy course would drown in labels.
        val distance = stations.getOrNull(idx)?.estimatedDistance
        if (!adaptiveAspect && (focused || selectedStation == null) &&
            length > 26f && distance != null
        ) {
            val unit = stations[idx].distanceUnit ?: "yd"
            val mid = Offset((shooter.x + target.x) / 2f, (shooter.y + target.y) / 2f)
            val nudge = Offset(mid.x + uy * 8f, mid.y - ux * 8f)
            val label = "${distance.roundToInt()}$unit"
            val rect = Rect(nudge.x - 15f, nudge.y - 7f, nudge.x + 15f, nudge.y + 7f)
            drawRect(
                color = AppCream.copy(alpha = AppCream.alpha * alpha),
                topLeft = rect.topLeft,
                size = rect.size,
            )
            drawRect(
                color = AppLine2.copy(alpha = AppLine2.alpha * alpha),
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 0.5f),
            )
            val measured = textMeasurer.measure(
                text = label,
                style = jetbrainsMono(8.sp).copy(
                    color = (if (focused) AppMaple else AppInk2).copy(alpha = alpha),
                ),
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    nudge.x - measured.size.width / 2f,
                    nudge.y - measured.size.height / 2f,
                ),
            )
        }
    }
}

private fun DrawScope.drawArrowhead(tip: Offset, ux: Float, uy: Float, color: Color) {
    val s = 4f
    // Perpendicular unit vector.
    val px = -uy
    val py = ux
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x - ux * s + px * s * 0.6f, tip.y - uy * s + py * s * 0.6f)
        lineTo(tip.x - ux * s - px * s * 0.6f, tip.y - uy * s - py * s * 0.6f)
        close()
    }
    drawPath(head, color)
}

/** Concentric-ring glyphs marking each inferred target position. */
private fun DrawScope.drawTargets(
    layout: CourseMapLayout,
    viewport: MapViewport,
    selectedStation: Int?,
) {
    layout.stations.indices.forEach { idx ->
        val targetN = layout.targets[idx] ?: return@forEach
        val focused = selectedStation == idx
        val dimmed = selectedStation != null && !focused
        val c = viewport.place(targetN, size)
        val r = if (focused) 9f else 7f
        val alpha = if (dimmed) 0.3f else 1f

        fun ringFill(radius: Float, color: Color) =
            drawCircle(color.copy(alpha = color.alpha * alpha), radius, c)
        fun ringStroke(radius: Float, color: Color, width: Float) =
            drawCircle(color.copy(alpha = color.alpha * alpha), radius, c, style = Stroke(width))

        ringFill(r, AppCream)
        ringStroke(r, AppInk, 0.7f)
        ringStroke(r * 0.62f, AppInk, 0.5f)
        ringFill(r * 0.34f, AppPondDk)
        ringFill(1.3f, AppCream)
        if (focused) {
            ringStroke(r + 5f, AppMaple.copy(alpha = 0.7f), 0.6f)
        }
    }
}

private fun DrawScope.drawPins(
    layout: CourseMapLayout,
    viewport: MapViewport,
    selectedStation: Int?,
    adaptiveAspect: Boolean,
    textMeasurer: TextMeasurer,
) {
    if (adaptiveAspect) {
        drawCompactPins(layout, viewport, textMeasurer)
        return
    }
    layout.stations.indices.forEach { idx ->
        val focused = selectedStation == idx
        val dimmed = selectedStation != null && !focused
        val c = viewport.place(layout.stations[idx], size)
        val r = if (focused) 10f else 8f
        val alpha = if (dimmed) 0.4f else 1f

        val fill = if (focused) AppMaple else AppPaper
        val stroke = if (focused) AppMaple else AppInk
        drawCircle(fill.copy(alpha = fill.alpha * alpha), r, c)
        drawCircle(
            stroke.copy(alpha = stroke.alpha * alpha),
            r,
            c,
            style = Stroke(width = if (focused) 1.4f else 1f),
        )
        val label = "${idx + 1}"
        val measured = textMeasurer.measure(
            text = label,
            style = jetbrainsMono(9.sp, FontWeight.SemiBold).copy(
                color = (if (focused) AppPaper else AppInk2).copy(alpha = alpha),
            ),
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                c.x - measured.size.width / 2f,
                c.y - measured.size.height / 2f,
            ),
        )
    }
}

/**
 * Compact station markers for the feed map — tiny dots, much smaller than the
 * target glyphs, so a busy course (10–70 stations) stays legible. Per-station
 * numbers are dropped; only the start (a hollow ring) and the end (a maple
 * dot) are marked, since the feed preview is a glance, not a walk-through.
 * Mirrors iOS `drawCompactPins`.
 */
private fun DrawScope.drawCompactPins(
    layout: CourseMapLayout,
    viewport: MapViewport,
    textMeasurer: TextMeasurer,
) {
    val lastIdx = layout.stations.size - 1
    layout.stations.indices.forEach { idx ->
        val c = viewport.place(layout.stations[idx], size)
        val isStart = idx == 0
        val isEnd = idx == lastIdx && lastIdx > 0
        val r = if (isStart || isEnd) 4.5f else 2.6f
        when {
            isStart -> {
                drawCircle(AppCream, r, c)
                drawCircle(AppPondDk, r, c, style = Stroke(width = 1.6f))
                drawCompactLabel("START", c, AppPondDk, textMeasurer)
            }
            isEnd -> {
                drawCircle(AppMaple, r, c)
                drawCompactLabel("END", c, AppMaple, textMeasurer)
            }
            else -> drawCircle(AppInk, r, c)
        }
    }
}

/** A tiny mono "START" / "END" label centred just above a compact pin. */
private fun DrawScope.drawCompactLabel(
    text: String,
    c: Offset,
    color: Color,
    textMeasurer: TextMeasurer,
) {
    val measured = textMeasurer.measure(
        text = text,
        style = jetbrainsMono(7.sp, FontWeight.SemiBold).copy(color = color),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            c.x - measured.size.width / 2f,
            (c.y - 11f) - measured.size.height / 2f,
        ),
    )
}

/**
 * Quadratic-smoothed polyline — the same midpoint-control curve the
 * prototype's `buildTrail` draws.
 */
private fun smoothCurve(points: List<Offset>): Path {
    val path = Path()
    val first = points.firstOrNull() ?: return path
    path.moveTo(first.x, first.y)
    if (points.size <= 1) return path
    for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        val mid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
        path.quadraticTo(p0.x, p0.y, mid.x, mid.y)
        if (i == points.size - 1) {
            path.lineTo(p1.x, p1.y)
        }
    }
    return path
}

private fun DrawScope.drawCompass(p: Offset, textMeasurer: TextMeasurer) {
    drawLine(
        color = AppInk,
        start = Offset(p.x, p.y + 6f),
        end = Offset(p.x, p.y - 10f),
        strokeWidth = 0.8f,
    )
    val head = Path().apply {
        moveTo(p.x - 4f, p.y - 5f)
        lineTo(p.x, p.y - 12f)
        lineTo(p.x + 4f, p.y - 5f)
        close()
    }
    drawPath(head, AppInk)
    val measured = textMeasurer.measure(
        text = "N",
        style = TextStyleN.copy(color = AppInk2),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            p.x - measured.size.width / 2f,
            (p.y - 18f) - measured.size.height / 2f,
        ),
    )
}

private fun DrawScope.drawScaleBar(p: Offset, zoom: Float, textMeasurer: TextMeasurer) {
    drawLine(AppInk, start = p, end = Offset(p.x + 60f, p.y), strokeWidth = 1f)
    for (x in floatArrayOf(p.x, p.x + 60f)) {
        drawLine(
            AppInk,
            start = Offset(x, p.y - 3f),
            end = Offset(x, p.y + 3f),
            strokeWidth = 1f,
        )
    }
    // Nominal scale — the 60 px bar reads 100 yd at 1×, halving as the
    // archer zooms in (50 yd at 2×, 25 yd at 4×).
    val yards = (100f / zoom).roundToInt()
    val measured = textMeasurer.measure(
        text = "$yards YD",
        style = jetbrainsMono(8.sp).copy(color = AppInk3),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            (p.x + 30f) - measured.size.width / 2f,
            (p.y + 9f) - measured.size.height / 2f,
        ),
    )
}

/** "N" compass label — Inter 8sp semibold (iOS `bpUI(8, .semibold)`). */
private val TextStyleN = androidx.compose.ui.text.TextStyle(
    fontSize = 8.sp,
    fontWeight = FontWeight.SemiBold,
)

private fun DrawScope.drawWalkerDot(centre: Offset, pulse: Float) {
    // Expanding ring — scales 0.5 → 1.0 over a 30px circle, fades out.
    val ringRadius = 15f * (0.5f + 0.5f * pulse)
    val ringAlpha = (1f - pulse) * 0.7f
    drawCircle(
        color = AppMaple.copy(alpha = ringAlpha),
        radius = ringRadius,
        center = centre,
        style = Stroke(width = 1.2f),
    )
    // Solid core dot — 12px with a paper rim.
    drawCircle(AppMaple, radius = 6f, center = centre)
    drawCircle(AppPaper, radius = 6f, center = centre, style = Stroke(width = 1.5f))
}

// ---------------------------------------------------------------------------
// Chrome overlays (fixed — never pan or zoom)
// ---------------------------------------------------------------------------

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AppPaper)
            .border(1.dp, AppLine)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(12.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(AppPaper, r, center)
                    drawCircle(AppInk, r, center, style = Stroke(1f))
                }
                androidx.compose.material3.Text(
                    text = "1",
                    style = jetbrainsMono(7.sp, FontWeight.SemiBold),
                    color = AppInk2,
                )
            }
            androidx.compose.material3.Text(
                text = "YOU SHOT FROM",
                style = jetbrainsMono(8.sp),
                color = AppInk2,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TargetRingGlyph(Modifier.size(12.dp))
            androidx.compose.material3.Text(
                text = "INFERRED TARGET",
                style = jetbrainsMono(8.sp),
                color = AppInk2,
            )
        }
    }
}

@Composable
private fun ZoomChip(zoom: Float, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text = "${"%.1f".format(zoom)}× · DRAG TO PAN",
        style = jetbrainsMono(9.sp),
        color = AppInk3,
        modifier = modifier
            .background(AppPaper)
            .border(1.dp, AppLine)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * A small static concentric-ring glyph — the inferred-target marker, used in
 * the map legend and anywhere a target needs to be referenced inline.
 * Mirrors iOS `TargetRingGlyph`.
 */
@Composable
fun TargetRingGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(AppCream, r, center)
        drawCircle(AppInk, r, center, style = Stroke(0.5f))
        drawCircle(AppInk, r * 0.7f, center, style = Stroke(0.4f))
        drawCircle(AppPondDk, r * 0.4f, center)
    }
}

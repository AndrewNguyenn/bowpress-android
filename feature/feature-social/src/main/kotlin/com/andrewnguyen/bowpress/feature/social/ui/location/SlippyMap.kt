package com.andrewnguyen.bowpress.feature.social.ui.location

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

// ── SlippyMap ────────────────────────────────────────────────────────────────
//
// The repo ships no map stack, so §18's "pannable / pinch-zoomable map" is a
// self-contained OpenStreetMap slippy-map: 256px Web-Mercator tiles drawn on a
// Canvas, panned + pinch-zoomed with Compose's native detectTransformGestures.
// Native gesture handling gives the smooth finger pan + pinch the contract
// asks for, with no Google Maps API key and no extra dependency.

/** Tile pixel size — the OSM standard. */
private const val TILE_SIZE = 256
private const val MIN_ZOOM = 3.0
private const val MAX_ZOOM = 19.0

/**
 * A geographic coordinate. Kept local to the map package so the map math
 * doesn't leak into the rest of the feature.
 */
internal data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * Imperative handle for the map camera — lets a caller (e.g. a recenter
 * button, or the location picker reading back the pin) drive and read the
 * camera. `centerZoom` is a fractional zoom level.
 */
internal class SlippyMapState(
    initialCenter: GeoPoint,
    initialZoom: Double,
) {
    var center by mutableStateOf(initialCenter)
        internal set
    var zoom by mutableStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        internal set

    /** Snap the camera back to a coordinate at a given zoom. */
    fun moveTo(point: GeoPoint, zoom: Double = this.zoom) {
        center = point
        this.zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }
}

@Composable
internal fun rememberSlippyMapState(
    center: GeoPoint,
    zoom: Double,
): SlippyMapState = remember { SlippyMapState(center, zoom) }

/**
 * The slippy-map composable. Renders OSM tiles centred on [state].center,
 * pannable + pinch-zoomable. [pinAtCenter] draws a fixed centre pin (the
 * tag-picker affordance — the map moves under it); pass false for a read-only
 * popup that instead drops [markers].
 */
@Composable
internal fun SlippyMap(
    state: SlippyMapState,
    modifier: Modifier = Modifier,
    pinAtCenter: Boolean = false,
    markers: List<GeoPoint> = emptyList(),
    pinColor: Color = Color(0xFF2D5A6B),
) {
    val scope = rememberCoroutineScope()
    val tileCache = remember { TileCache(scope) }
    // Viewport size — captured via onSizeChanged (a layout callback) rather
    // than written from the DrawScope, which would re-trigger recomposition
    // every frame and leave the size at Zero during the first pinch.
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = modifier
            .onSizeChanged { viewport = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    // Pinch: adjust fractional zoom by log2 of the scale.
                    if (gestureZoom != 1f) {
                        val newZoom = (state.zoom + ln(gestureZoom.toDouble()) / ln(2.0))
                            .coerceIn(MIN_ZOOM, MAX_ZOOM)
                        // Zoom about the pinch centroid so it stays under the
                        // fingers — convert centroid to world px at both zooms.
                        val worldOld = state.center.toWorldPx(state.zoom)
                        val focusOld = worldOld + (centroid.toVec() - viewport.center())
                        val focusGeo = focusOld.toGeo(state.zoom)
                        state.zoom = newZoom
                        val focusNew = focusGeo.toWorldPx(newZoom)
                        val worldNew = focusNew - (centroid.toVec() - viewport.center())
                        state.center = worldNew.toGeo(newZoom)
                    }
                    // Pan: shift the centre by the drag, in world pixels.
                    if (pan != Offset.Zero) {
                        val world = state.center.toWorldPx(state.zoom)
                        state.center = (world - pan.toVec()).toGeo(state.zoom)
                    }
                }
            },
    ) {
        drawTiles(state, tileCache)
        markers.forEach { drawPin(it.screenOffset(state, size), pinColor) }
        if (pinAtCenter) {
            drawPin(Offset(size.width / 2f, size.height / 2f), pinColor)
        }
    }
}

// ── Drawing ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawTiles(state: SlippyMapState, cache: TileCache) {
    // Render at the nearest integer zoom, scaled to the fractional zoom so the
    // pinch feels continuous between tile levels.
    val intZoom = floor(state.zoom).toInt().coerceIn(MIN_ZOOM.toInt(), MAX_ZOOM.toInt())
    val scale = 2.0.pow(state.zoom - intZoom).toFloat()
    val scaledTile = TILE_SIZE * scale

    val n = 1 shl intZoom
    val centerWorld = state.center.toWorldPx(intZoom.toDouble())
    // Top-left of the viewport in world pixels at intZoom (pre-scale).
    val originX = centerWorld.x - (size.width / 2f) / scale
    val originY = centerWorld.y - (size.height / 2f) / scale

    val firstCol = floor(originX / TILE_SIZE).toInt()
    val firstRow = floor(originY / TILE_SIZE).toInt()
    val cols = (size.width / scaledTile).toInt() + 2
    val rows = (size.height / scaledTile).toInt() + 2

    for (dx in 0..cols) {
        for (dy in 0..rows) {
            val col = firstCol + dx
            val row = firstRow + dy
            if (row < 0 || row >= n) continue
            val wrappedCol = ((col % n) + n) % n
            val left = ((col * TILE_SIZE) - originX) * scale
            val top = ((row * TILE_SIZE) - originY) * scale
            val bitmap = cache.tile(intZoom, wrappedCol, row)
            if (bitmap != null) {
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(left.toInt(), top.toInt()),
                    dstSize = IntSize(scaledTile.toInt() + 1, scaledTile.toInt() + 1),
                )
            } else {
                // Tile not loaded yet — a quiet placeholder so the map never
                // flashes blank while tiles stream in.
                drawRect(
                    color = Color(0xFFE4EBE3),
                    topLeft = Offset(left.toFloat(), top.toFloat()),
                    size = Size(scaledTile, scaledTile),
                )
            }
        }
    }
}

/** A simple teardrop pin centred on its point. */
private fun DrawScope.drawPin(point: Offset, color: Color) {
    val r = 7f
    // Stem.
    drawLine(
        color = color,
        start = point,
        end = Offset(point.x, point.y - 2 * r),
        strokeWidth = 3f,
    )
    // Head + white core.
    drawCircle(color = color, radius = r, center = Offset(point.x, point.y - 2 * r))
    drawCircle(color = Color.White, radius = r * 0.4f, center = Offset(point.x, point.y - 2 * r))
}

// ── Web-Mercator projection ──────────────────────────────────────────────────

/** World-pixel position (px from the world origin) at an integer-ish zoom. */
private data class WorldPx(val x: Double, val y: Double) {
    operator fun minus(o: WorldPx) = WorldPx(x - o.x, y - o.y)
    operator fun plus(o: WorldPx) = WorldPx(x + o.x, y + o.y)
}

private fun GeoPoint.toWorldPx(zoom: Double): WorldPx {
    val n = TILE_SIZE * 2.0.pow(zoom)
    val x = (longitude + 180.0) / 360.0 * n
    val latRad = latitude * PI / 180.0
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
    return WorldPx(x, y)
}

private fun WorldPx.toGeo(zoom: Double): GeoPoint {
    val n = TILE_SIZE * 2.0.pow(zoom)
    val lon = x / n * 360.0 - 180.0
    val latRad = atan(sinh(PI * (1.0 - 2.0 * y / n)))
    return GeoPoint(
        latitude = (latRad * 180.0 / PI).coerceIn(-85.05, 85.05),
        longitude = ((lon + 180.0).mod(360.0)) - 180.0,
    )
}

/** Screen offset of a coordinate within a viewport of [size], given the camera. */
private fun GeoPoint.screenOffset(state: SlippyMapState, size: Size): Offset {
    val world = toWorldPx(state.zoom)
    val center = state.center.toWorldPx(state.zoom)
    return Offset(
        x = (world.x - center.x).toFloat() + size.width / 2f,
        y = (world.y - center.y).toFloat() + size.height / 2f,
    )
}

private fun Offset.toVec() = WorldPx(x.toDouble(), y.toDouble())
private fun IntSize.center() = WorldPx(width / 2.0, height / 2.0)

// ── Tile cache ───────────────────────────────────────────────────────────────

/**
 * In-memory OSM tile cache. Misses kick off a background fetch; once decoded
 * the bitmap lands in the [tiles] snapshot map and the Canvas recomposes.
 */
private class TileCache(private val scope: CoroutineScope) {
    /** Most recently a user can pan — cap the cache so it can't grow forever. */
    private companion object {
        const val MAX_TILES = 256
    }

    // Snapshot-observable so the Canvas recomposes when a tile lands. Mutated
    // only on the composition thread (scope.launch defaults to Dispatchers.Main).
    private val tiles = mutableStateMapOf<String, ImageBitmap>()
    // Insertion-ordered key list backing a simple LRU eviction — also touched
    // only on the composition thread, in lockstep with [tiles].
    private val lruOrder = ArrayDeque<String>()
    // Mutated from both the composition thread (add) and IO coroutines
    // (remove) — must be a concurrent set to avoid a lost update wedging a key.
    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()
    // OSM's tile-usage policy asks clients to throttle — at most a few
    // concurrent fetches.
    private val gate = Semaphore(4)

    fun tile(zoom: Int, x: Int, y: Int): ImageBitmap? {
        val key = "$zoom/$x/$y"
        tiles[key]?.let { return it }
        if (inFlight.add(key)) {
            scope.launch { fetch(key, zoom, x, y) }
        }
        return null
    }

    private suspend fun fetch(key: String, zoom: Int, x: Int, y: Int) {
        val bitmap = withContext(Dispatchers.IO) {
            gate.withPermit { download(zoom, x, y) }
        }
        // Back on the composition thread (scope is Main-dispatched): publish
        // the tile and evict the oldest if the cache is over the cap.
        if (bitmap != null) {
            tiles[key] = bitmap.asImageBitmap()
            lruOrder.remove(key)
            lruOrder.addLast(key)
            while (lruOrder.size > MAX_TILES) {
                val evicted = lruOrder.removeFirst()
                tiles.remove(evicted)
            }
        }
        inFlight.remove(key)
    }

    private fun download(zoom: Int, x: Int, y: Int): Bitmap? = runCatching {
        val url = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png")
        (url.openConnection() as HttpURLConnection).run {
            // OSM requires an identifying User-Agent.
            setRequestProperty("User-Agent", "BowPress/1.6 (Android)")
            connectTimeout = 8_000
            readTimeout = 8_000
            inputStream.use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()
}

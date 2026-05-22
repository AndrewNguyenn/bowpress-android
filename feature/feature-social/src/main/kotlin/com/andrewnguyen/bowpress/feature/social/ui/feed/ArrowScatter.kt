package com.andrewnguyen.bowpress.feature.social.ui.feed

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

// ── ArrowScatter ─────────────────────────────────────────────────────────────
//
// The friend-activity feed card (Social Activity Card · 50/50) shows the WA
// target face WITH the friend's arrows plotted on it. The feed payload's
// `ActivitySession.endRings` only carries each arrow's *ring value* (11 = X …
// 0 = miss) — there is no x/y. So the card synthesises a plausible position:
// each arrow is scattered deterministically inside the radius band of the ring
// it scored, seeded by its flat (end, arrow) index so the picture is stable
// across redraws.
//
// Pure (no Compose) so the scatter geometry is unit-testable. Mirrors iOS
// `TargetRingScatter`.

/**
 * One synthesised arrow dot — a normalised offset from the face centre and the
 * ring it scored (so the dot can colour an X / miss distinctly). `(0, 0)` is
 * dead centre, magnitude `1.0` is the face edge; y is positive-down (screen).
 * A miss can sit just past the `1.0` edge.
 */
data class ScatteredArrow(val ring: Int, val x: Float, val y: Float) {
    /** Distance from the face centre, in face-radius units. */
    val radiusNorm: Float get() = hypot(x, y)
}

/**
 * Inclusive radius band (in face-radius units) a [ring] score occupies on a WA
 * face. The X (11) has its own tight centre band `0..0.05` so it plots inside
 * the X ring as the maple standout dot; ring 10 is `0..0.10`, ring 9 is
 * `0.10..0.20`, … ring 1 is `0.90..1.0`; a miss (≤0) lands just past the face
 * edge.
 */
internal fun ringRadiusBand(ring: Int): Pair<Float, Float> = when {
    // The X — its own centre band, inside the ring-10 bullseye.
    ring >= 11 -> 0f to 0.05f
    ring <= 0 -> 1.0f to 1.06f
    ring == 10 -> 0f to 0.10f
    else -> ((10 - ring) * 0.1f) to ((11 - ring) * 0.1f)
}

/**
 * A stable fractional value in `0..<1` from an integer seed — a plain hash of
 * the seed (mixed with a large odd salt). No RNG state, so it is pure and
 * reproducible. Mirrors iOS `TargetRingScatter.fractional`.
 */
internal fun scatterFraction(seed: Int, salt: Int): Float {
    val h = ((seed.toLong() + 1L) * 0x9E3779B97F4A7C15uL.toLong()) xor
        (salt.toLong() * 0xC2B2AE3D27D4EB4FuL.toLong())
    // Take 24 bits → 0..<1.
    return ((h ushr 40) and 0xFFFFFFL).toFloat() / 0x1000000L.toFloat()
}

/**
 * Scatters every arrow in [endRings] to a deterministic position within its
 * scoring ring's radius band. The result is stable for a given [endRings] —
 * the card can plot it on every recomposition without the dots dancing.
 *
 * Within a band the radius is sampled **area-uniformly** (`sqrt` of a uniform
 * fraction) so dots don't bunch toward the centre of a wide band; the band is
 * inset 12% each side so a dot reads inside its scoring colour. A miss is
 * parked just past the face edge rather than dropped.
 */
fun scatterArrows(endRings: List<List<Int>>): List<ScatteredArrow> = buildList {
    var flatIndex = 0
    endRings.forEach { end ->
        end.forEach { ring ->
            val theta = scatterFraction(flatIndex, salt = 53) * 2f * Math.PI.toFloat()
            val cos = cos(theta)
            val sin = sin(theta)
            val (bandInner, bandOuter) = ringRadiusBand(ring)
            val r: Float = if (ring <= 0) {
                // A miss — park it just outside the face.
                bandInner + scatterFraction(flatIndex, salt = 31) * (bandOuter - bandInner)
            } else {
                // Inset the band 12% each side, then sample area-uniformly.
                val span = bandOuter - bandInner
                val inset = span * 0.12f
                val lo = bandInner + inset
                val hi = maxOf(lo, bandOuter - inset)
                val t = scatterFraction(flatIndex, salt = 17)
                if (lo == hi) lo else sqrt(lo * lo + t * (hi * hi - lo * lo))
            }
            add(ScatteredArrow(ring = ring, x = r * cos, y = r * sin))
            flatIndex++
        }
    }
}

/**
 * Real plotted arrows from the feed payload's `plotPoints` — each `[x, y]`
 * is already a normalised −1…1 offset from the face centre, so it maps
 * straight to a [ScatteredArrow]. Each is paired with its ring from the
 * flattened [endRings] (both are in shot order) so an X / miss still
 * colours. Preferred over [scatterArrows] whenever the payload carries
 * real coordinates.
 */
fun plottedArrows(
    plotPoints: List<List<Double>>,
    endRings: List<List<Int>>,
): List<ScatteredArrow> {
    val flatRings = endRings.flatten()
    return plotPoints.mapIndexed { i, p ->
        ScatteredArrow(
            ring = flatRings.getOrElse(i) { 10 },
            x = p.getOrElse(0) { 0.0 }.toFloat(),
            y = p.getOrElse(1) { 0.0 }.toFloat(),
        )
    }
}

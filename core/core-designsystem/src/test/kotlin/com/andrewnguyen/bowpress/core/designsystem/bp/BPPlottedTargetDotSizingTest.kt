package com.andrewnguyen.bowpress.core.designsystem.bp

import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The geometric arrow-dot sizing helpers behind [BPPlottedTarget]. The
 * Compose render itself is locked behind `LocalDensity`, but the math is
 * pure — pin it here so a future refactor can't silently drop the
 * arrow-diameter scaling and crash the rendered dot back to a fixed
 * pixel size.
 */
class BPPlottedTargetDotSizingTest {

    @Test
    fun `multi-spot scales the dot against the 180mm Vegas spot`() {
        // A 6mm shaft on a 90px spot radius (180px spot diameter): the
        // dot diameter should equal the shaft proportion of the spot:
        // 6 / 180 * 180 = 6px diameter, so 3px radius.
        val r = multiSpotDotRadiusPx(spotRadiusPx = 90f, shaftMm = 6f, minDotRadiusPx = 0f)
        assertThat(r).isWithin(0.001f).of(3f)
    }

    @Test
    fun `multi-spot doubles the dot for a 12mm shaft on the same spot`() {
        // Doubling the physical shaft doubles the rendered dot.
        val r6 = multiSpotDotRadiusPx(spotRadiusPx = 90f, shaftMm = 6f, minDotRadiusPx = 0f)
        val r12 = multiSpotDotRadiusPx(spotRadiusPx = 90f, shaftMm = 12f, minDotRadiusPx = 0f)
        assertThat(r12).isWithin(0.001f).of(r6 * 2)
    }

    @Test
    fun `multi-spot floors at the visibility minimum on a tiny spot`() {
        // At a feed-card scale (say, 30px spot radius), a 6mm shaft would
        // compute to 1px radius — the floor saves the dot from collapsing
        // below pixel visibility.
        val r = multiSpotDotRadiusPx(spotRadiusPx = 30f, shaftMm = 6f, minDotRadiusPx = 3f)
        assertThat(r).isEqualTo(3f)
    }

    @Test
    fun `single-face Vegas sizes against the ~123_5mm mmPerNormUnit`() {
        // Vegas mmPerNormUnit ≈ 123.5; a 6mm shaft on an 84px face radius
        // → 6 / 123.5 * 84 ≈ 4.08px radius.
        val r = singleFaceDotRadiusPx(
            faceRadiusPx = 84f,
            shaftMm = 6f,
            faceType = TargetFaceType.SIX_RING,
            sixRingStyle = BPSixRingStyle.Vegas,
            minDotRadiusPx = 0f,
        )
        assertThat(r).isWithin(0.01f).of(6f / (20f / (119f / 735f)) * 84f)
    }

    @Test
    fun `single-face Outdoor80 sizes against the 400mm mmPerNormUnit`() {
        // Outdoor80 mmPerNormUnit = 400; same shaft renders much smaller
        // on the bigger face — the dot represents the same real-world
        // arrow on a 5x bigger printed card.
        val rVegas = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.SIX_RING,
            sixRingStyle = BPSixRingStyle.Vegas,
            minDotRadiusPx = 0f,
        )
        val rOutdoor = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.SIX_RING,
            sixRingStyle = BPSixRingStyle.Outdoor80,
            minDotRadiusPx = 0f,
        )
        // 123.5 vs 400 → Outdoor80 dot is 123.5/400 ≈ 31% the size.
        assertThat(rOutdoor / rVegas).isWithin(0.01f).of(123.529f / 400f)
    }

    @Test
    fun `single-face TenRing at null distance defaults to 122cm WA face`() {
        // The 122cm full face (610mm radius) is the default when distance
        // is unknown — matches iOS `TargetGeometry.tenRing` /
        // `preset(.tenRing, distance: nil)`.
        val r = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.TEN_RING,
            sixRingStyle = BPSixRingStyle.Vegas, // ignored for TEN_RING
            distance = null,
            minDotRadiusPx = 0f,
        )
        assertThat(r).isWithin(0.001f).of(6f / 610f * 84f)
    }

    @Test
    fun `single-face TenRing at 20yd uses the 40cm indoor face`() {
        // 40cm indoor face → 200mm radius; 6mm shaft → 6/200 * 84 = 2.52px.
        // Mirrors iOS commit adding the .tenRing indoor preset.
        val r = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.TEN_RING,
            sixRingStyle = BPSixRingStyle.Vegas,
            distance = ShootingDistance.YARDS_20,
            minDotRadiusPx = 0f,
        )
        assertThat(r).isWithin(0.001f).of(6f / 200f * 84f)
    }

    @Test
    fun `single-face TenRing at 50m uses the 80cm WA face`() {
        // 80cm outdoor face → 400mm radius; same as the SixRing Outdoor80
        // physical face. 6mm shaft → 6/400 * 84 = 1.26px.
        val r = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.TEN_RING,
            sixRingStyle = BPSixRingStyle.Vegas,
            distance = ShootingDistance.METERS_50,
            minDotRadiusPx = 0f,
        )
        assertThat(r).isWithin(0.001f).of(6f / 400f * 84f)
    }

    @Test
    fun `single-face TenRing at 70m stays on the 122cm full face`() {
        val r = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.TEN_RING,
            sixRingStyle = BPSixRingStyle.Vegas,
            distance = ShootingDistance.METERS_70,
            minDotRadiusPx = 0f,
        )
        assertThat(r).isWithin(0.001f).of(6f / 610f * 84f)
    }
}

package com.andrewnguyen.bowpress.core.designsystem.bp

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
    fun `single-face TenRing reuses Vegas mmPerNormUnit`() {
        // Intentional Android-vs-iOS divergence — see
        // `BPPlottedTarget.singleFaceMmPerNormUnit` doc: TenRing shares
        // Vegas's normaliser so the dot reads at the same physical size
        // regardless of face type. Pin so a future refactor doesn't
        // silently swap in the iOS 610mm value.
        val rVegas = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.SIX_RING,
            sixRingStyle = BPSixRingStyle.Vegas,
            minDotRadiusPx = 0f,
        )
        val rTenRing = singleFaceDotRadiusPx(
            faceRadiusPx = 84f, shaftMm = 6f,
            faceType = TargetFaceType.TEN_RING,
            sixRingStyle = BPSixRingStyle.Vegas, // ignored for TEN_RING
            minDotRadiusPx = 0f,
        )
        assertThat(rTenRing).isWithin(0.001f).of(rVegas)
    }
}

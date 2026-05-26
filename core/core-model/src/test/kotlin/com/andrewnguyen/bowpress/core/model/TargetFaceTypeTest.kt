package com.andrewnguyen.bowpress.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression coverage for [TargetFaceType.matching] — guardrails that keep
 * the Android face-label heuristic in lockstep with iOS
 * `bowpress-ios/.../Models/TargetFaceType.swift` (parity B2 / iOS commit
 * 3c1a305).
 */
class TargetFaceTypeTest {

    // ---- Legacy snake_case raw-value labels (parity B2) ----

    @Test fun `matching six_ring snake_case decodes as SIX_RING`() {
        assertThat(TargetFaceType.matching("six_ring")).isEqualTo(TargetFaceType.SIX_RING)
    }

    @Test fun `matching ten_ring snake_case decodes as TEN_RING`() {
        assertThat(TargetFaceType.matching("ten_ring")).isEqualTo(TargetFaceType.TEN_RING)
    }

    @Test fun `matching is case-insensitive on the raw-value path`() {
        assertThat(TargetFaceType.matching("SIX_RING")).isEqualTo(TargetFaceType.SIX_RING)
        assertThat(TargetFaceType.matching("Ten_Ring")).isEqualTo(TargetFaceType.TEN_RING)
    }

    // ---- Free-text heuristic for human-typed labels ----

    @Test fun `matching 6-Ring decodes as SIX_RING`() {
        assertThat(TargetFaceType.matching("6-Ring")).isEqualTo(TargetFaceType.SIX_RING)
    }

    @Test fun `matching 10-Ring decodes as TEN_RING`() {
        assertThat(TargetFaceType.matching("10-Ring")).isEqualTo(TargetFaceType.TEN_RING)
    }

    @Test fun `matching Vegas 3-Spot decodes as SIX_RING`() {
        assertThat(TargetFaceType.matching("Vegas 3-Spot")).isEqualTo(TargetFaceType.SIX_RING)
    }

    @Test fun `matching Spot decodes as SIX_RING`() {
        assertThat(TargetFaceType.matching("Spot")).isEqualTo(TargetFaceType.SIX_RING)
    }

    // ---- "10" runs before "6-ring" so 60cm-style labels don't misfire ----

    @Test fun `matching 60cm does not misfire as SIX_RING`() {
        // "60cm" contains "10"? No, but "WA 60" contains neither "10" nor "6-ring".
        // The real guard: a label like "10-zone 60cm" must read as TEN_RING.
        assertThat(TargetFaceType.matching("10-zone 60cm")).isEqualTo(TargetFaceType.TEN_RING)
    }

    // ---- Null / empty / unknown ----

    @Test fun `matching null returns null`() {
        assertThat(TargetFaceType.matching(null)).isNull()
    }

    @Test fun `matching empty string returns null`() {
        assertThat(TargetFaceType.matching("")).isNull()
    }

    @Test fun `matching unrecognised label returns null`() {
        assertThat(TargetFaceType.matching("Field 80cm")).isNull()
    }
}

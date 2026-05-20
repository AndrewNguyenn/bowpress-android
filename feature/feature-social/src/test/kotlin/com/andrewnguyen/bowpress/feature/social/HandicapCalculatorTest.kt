package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.model.HandicapCalculator
import com.andrewnguyen.bowpress.core.model.HandicapEquation
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-JVM unit tests for [HandicapCalculator] matching the math in API contract §6.
 * All expected values are hand-computed from the spec formulas.
 */
class HandicapCalculatorTest {

    // ── bracketAllowance ─────────────────────────────────────────────────────

    @Test
    fun `bracketAllowance for avg exactly 590 returns 5`() {
        assertThat(HandicapCalculator.bracketAllowance(590.0)).isEqualTo(5)
    }

    @Test
    fun `bracketAllowance for avg above 590 returns 5`() {
        assertThat(HandicapCalculator.bracketAllowance(598.0)).isEqualTo(5)
    }

    @Test
    fun `bracketAllowance for avg exactly 580 returns 15`() {
        assertThat(HandicapCalculator.bracketAllowance(580.0)).isEqualTo(15)
    }

    @Test
    fun `bracketAllowance for avg between 580 and 590 returns 15`() {
        assertThat(HandicapCalculator.bracketAllowance(585.0)).isEqualTo(15)
    }

    @Test
    fun `bracketAllowance for avg exactly 560 returns 35`() {
        assertThat(HandicapCalculator.bracketAllowance(560.0)).isEqualTo(35)
    }

    @Test
    fun `bracketAllowance for avg exactly 540 returns 60`() {
        assertThat(HandicapCalculator.bracketAllowance(540.0)).isEqualTo(60)
    }

    @Test
    fun `bracketAllowance for avg exactly 500 returns 95`() {
        assertThat(HandicapCalculator.bracketAllowance(500.0)).isEqualTo(95)
    }

    @Test
    fun `bracketAllowance for avg below 500 returns 120`() {
        assertThat(HandicapCalculator.bracketAllowance(480.0)).isEqualTo(120)
    }

    @Test
    fun `bracketAllowance for avg of 0 returns 120`() {
        assertThat(HandicapCalculator.bracketAllowance(0.0)).isEqualTo(120)
    }

    // ── perWeekAllowance ─────────────────────────────────────────────────────

    @Test
    fun `perWeekAllowance for none equation always returns 0`() {
        assertThat(HandicapCalculator.perWeekAllowance(HandicapEquation.none, 550.0, 0.8)).isEqualTo(0)
        assertThat(HandicapCalculator.perWeekAllowance(HandicapEquation.none, 400.0, null)).isEqualTo(0)
    }

    @Test
    fun `perWeekAllowance for allowance equation uses allowancePct`() {
        // (600 - 550) * 0.8 = 40.0 -> 40
        assertThat(HandicapCalculator.perWeekAllowance(HandicapEquation.allowance, 550.0, 0.8)).isEqualTo(40)
    }

    @Test
    fun `perWeekAllowance for allowance equation uses 0_8 default when pct is null`() {
        // implementation: allowancePct ?: 0.8 → (600 - 550) * 0.8 = 40
        assertThat(HandicapCalculator.perWeekAllowance(HandicapEquation.allowance, 550.0, null)).isEqualTo(40)
    }

    @Test
    fun `perWeekAllowance for bracket equation calls bracketAllowance`() {
        // avg = 565 -> bracket = 35
        assertThat(HandicapCalculator.perWeekAllowance(HandicapEquation.bracket, 565.0, null)).isEqualTo(35)
    }

    @Test
    fun `perWeekAllowance for rolling equation uses 0_85 factor`() {
        // (600 - 550) * 0.85 = 42.5 -> 43 (rounded, not truncated)
        assertThat(HandicapCalculator.perWeekAllowance(HandicapEquation.rolling, 550.0, null)).isEqualTo(43)
    }

    // ── adjustedScore ────────────────────────────────────────────────────────

    @Test
    fun `adjustedScore equals raw for none equation`() {
        assertThat(
            HandicapCalculator.adjustedScore(500, HandicapEquation.none, 550.0, null),
        ).isEqualTo(500)
    }

    @Test
    fun `adjustedScore for allowance equation adds allowance to raw`() {
        // allowance = (600 - 540) * 0.8 = 48; adjusted = 510 + 48 = 558
        assertThat(
            HandicapCalculator.adjustedScore(510, HandicapEquation.allowance, 540.0, 0.8),
        ).isEqualTo(558)
    }

    @Test
    fun `adjustedScore for bracket equation adds bracket allowance`() {
        // avg = 565 -> bracket = 35; adjusted = 500 + 35 = 535
        assertThat(
            HandicapCalculator.adjustedScore(500, HandicapEquation.bracket, 565.0, null),
        ).isEqualTo(535)
    }

    @Test
    fun `adjustedScore for rolling equation adds rolling allowance`() {
        // allowance (600-550)*0.85 = 42.5; adjusted = round(530 + 42.5) = round(572.5) = 573
        assertThat(
            HandicapCalculator.adjustedScore(530, HandicapEquation.rolling, 550.0, null),
        ).isEqualTo(573)
    }

    @Test
    fun `adjustedScore rounds the raw-plus-allowance sum`() {
        // The cross-platform rule (§6) is sum-then-round, matching iOS/API.
        // (600 - 551.1) * 0.85 = 41.565 -> perWeekAllowance rounds to 42
        val allowance = HandicapCalculator.perWeekAllowance(HandicapEquation.rolling, 551.1, null)
        assertThat(allowance).isEqualTo(42)
        // adjustedScore rounds the sum: round(500 + 41.565) = round(541.565) = 542
        assertThat(HandicapCalculator.adjustedScore(500, HandicapEquation.rolling, 551.1, null)).isEqualTo(542)
    }
}

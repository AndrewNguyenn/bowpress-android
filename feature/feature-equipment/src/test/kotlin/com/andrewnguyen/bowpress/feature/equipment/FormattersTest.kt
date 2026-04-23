package com.andrewnguyen.bowpress.feature.equipment

import com.andrewnguyen.bowpress.feature.equipment.components.clickerLabel
import com.andrewnguyen.bowpress.feature.equipment.components.halfTwistLabel
import com.andrewnguyen.bowpress.feature.equipment.components.limbTurnsLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sightPositionLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sixteenthLabel
import com.andrewnguyen.bowpress.feature.equipment.components.tillerLabel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Sanity checks for the labels shared with iOS `BowConfigEditView.swift`. */
class FormattersTest {

    @Test fun `sixteenthLabel handles zero`() {
        assertThat(sixteenthLabel(0)).isEqualTo("0/16\"")
    }

    @Test fun `sixteenthLabel signs positive and negative`() {
        assertThat(sixteenthLabel(3)).isEqualTo("+3/16\"")
        assertThat(sixteenthLabel(-5)).isEqualTo("-5/16\"")
    }

    @Test fun `halfTwistLabel uses half-twist encoding`() {
        assertThat(halfTwistLabel(0)).isEqualTo("0 twists")
        assertThat(halfTwistLabel(2)).isEqualTo("+1 twist")
        assertThat(halfTwistLabel(-3)).isEqualTo("-1.5 twists")
    }

    @Test fun `limbTurnsLabel names the direction`() {
        assertThat(limbTurnsLabel(0.0)).isEqualTo("0 turns")
        assertThat(limbTurnsLabel(1.0)).isEqualTo("1 turn in")
        assertThat(limbTurnsLabel(-2.5)).isEqualTo("2.5 turns out")
    }

    @Test fun `sight position baselines zero`() {
        assertThat(sightPositionLabel(0)).isEqualTo("0 (baseline)")
        assertThat(sightPositionLabel(4)).isEqualTo("+4")
        assertThat(sightPositionLabel(-2)).isEqualTo("-2")
    }

    @Test fun `tiller and clicker always signed`() {
        assertThat(tillerLabel(0.0)).isEqualTo("+0.0 mm")
        assertThat(tillerLabel(2.5)).isEqualTo("+2.5 mm")
        assertThat(tillerLabel(-1.5)).isEqualTo("-1.5 mm")
        assertThat(clickerLabel(3.0)).isEqualTo("+3 mm")
        assertThat(clickerLabel(-4.0)).isEqualTo("-4 mm")
    }
}

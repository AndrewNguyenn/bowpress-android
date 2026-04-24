package com.andrewnguyen.bowpress.feature.equipment

import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Field
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Section
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins down the iOS `BowConfigEditView.swift` visibility rules as Kotlin. If a
 * future tuning-field rename slips through, these are the canaries.
 */
class EquipmentFieldRulesTest {

    // ---------- Shared fields ----------

    @Test
    fun `draw length grip and nocking height visible on all bow types`() {
        val fields = listOf(Field.DRAW_LENGTH, Field.GRIP_ANGLE, Field.NOCKING_HEIGHT)
        for (type in BowType.entries) {
            for (field in fields) {
                assertThat(EquipmentFieldRules.isVisible(field, type)).isTrue()
            }
        }
    }

    // ---------- Compound ----------

    @Test
    fun `compound setup shows let-off peep and d-loop`() {
        assertThat(EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, BowType.COMPOUND, isSetup = true)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.PEEP_HEIGHT, BowType.COMPOUND, isSetup = true)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.D_LOOP_LENGTH, BowType.COMPOUND, isSetup = true)).isTrue()
    }

    @Test
    fun `compound non-setup hides let-off peep and d-loop in favour of base setup summary`() {
        assertThat(EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, BowType.COMPOUND, isSetup = false)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.PEEP_HEIGHT, BowType.COMPOUND, isSetup = false)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.D_LOOP_LENGTH, BowType.COMPOUND, isSetup = false)).isFalse()
        assertThat(EquipmentFieldRules.sectionVisible(Section.BASE_SETUP, BowType.COMPOUND, isSetup = false)).isTrue()
    }

    @Test
    fun `compound shows string-cable limbs rest and sight`() {
        assertThat(EquipmentFieldRules.sectionVisible(Section.STRING_AND_CABLE, BowType.COMPOUND)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.LIMBS, BowType.COMPOUND)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.REST, BowType.COMPOUND)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.SIGHT_GRIP_NOCK, BowType.COMPOUND)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.SIGHT_POSITION, BowType.COMPOUND)).isTrue()
    }

    @Test
    fun `compound hides brace height`() {
        assertThat(EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, BowType.COMPOUND)).isFalse()
    }

    @Test
    fun `compound rear-stab sub-fields hide when side is NONE`() {
        val hidden = EquipmentFieldRules.isVisible(
            field = Field.REAR_STAB_WEIGHT,
            bowType = BowType.COMPOUND,
            rearStabSide = RearStabSide.NONE,
        )
        assertThat(hidden).isFalse()
    }

    @Test
    fun `compound rear-stab sub-fields appear once a side is chosen`() {
        for (side in listOf(RearStabSide.LEFT, RearStabSide.RIGHT, RearStabSide.BOTH)) {
            assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_VERT_ANGLE, BowType.COMPOUND, rearStabSide = side)).isTrue()
            assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_HORIZ_ANGLE, BowType.COMPOUND, rearStabSide = side)).isTrue()
        }
    }

    @Test
    fun `compound LEFT or RIGHT shows single weight row and hides V-bar weights`() {
        for (side in listOf(RearStabSide.LEFT, RearStabSide.RIGHT)) {
            assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_WEIGHT, BowType.COMPOUND, rearStabSide = side)).isTrue()
            assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_LEFT_WEIGHT, BowType.COMPOUND, rearStabSide = side)).isFalse()
            assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_RIGHT_WEIGHT, BowType.COMPOUND, rearStabSide = side)).isFalse()
        }
    }

    @Test
    fun `compound BOTH shows left and right weight rows and hides single weight`() {
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_WEIGHT, BowType.COMPOUND, rearStabSide = RearStabSide.BOTH)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_LEFT_WEIGHT, BowType.COMPOUND, rearStabSide = RearStabSide.BOTH)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_RIGHT_WEIGHT, BowType.COMPOUND, rearStabSide = RearStabSide.BOTH)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_VERT_ANGLE, BowType.COMPOUND, rearStabSide = RearStabSide.BOTH)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_HORIZ_ANGLE, BowType.COMPOUND, rearStabSide = RearStabSide.BOTH)).isTrue()
    }

    @Test
    fun `compound NONE hides all rear-stab weight rows`() {
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_WEIGHT, BowType.COMPOUND, rearStabSide = RearStabSide.NONE)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_LEFT_WEIGHT, BowType.COMPOUND, rearStabSide = RearStabSide.NONE)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_RIGHT_WEIGHT, BowType.COMPOUND, rearStabSide = RearStabSide.NONE)).isFalse()
    }

    // ---------- Recurve ----------

    @Test
    fun `recurve shows brace height tiller plunger and clicker`() {
        assertThat(EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, BowType.RECURVE)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.TILLER, BowType.RECURVE)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.PLUNGER, BowType.RECURVE)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.CLICKER, BowType.RECURVE)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.CLICKER_POSITION, BowType.RECURVE)).isTrue()
    }

    @Test
    fun `recurve hides let-off peep d-loop string-cable limbs rest and sight position`() {
        assertThat(EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, BowType.RECURVE)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.PEEP_HEIGHT, BowType.RECURVE)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.D_LOOP_LENGTH, BowType.RECURVE)).isFalse()
        assertThat(EquipmentFieldRules.sectionVisible(Section.STRING_AND_CABLE, BowType.RECURVE)).isFalse()
        assertThat(EquipmentFieldRules.sectionVisible(Section.LIMBS, BowType.RECURVE)).isFalse()
        assertThat(EquipmentFieldRules.sectionVisible(Section.REST, BowType.RECURVE)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.SIGHT_POSITION, BowType.RECURVE)).isFalse()
    }

    @Test
    fun `recurve uses V-bar layout for rear stab`() {
        assertThat(EquipmentFieldRules.sectionVisible(Section.V_BAR, BowType.RECURVE)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.REAR_STAB, BowType.RECURVE)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_LEFT_WEIGHT, BowType.RECURVE)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.REAR_STAB_RIGHT_WEIGHT, BowType.RECURVE)).isTrue()
    }

    @Test
    fun `recurve front stab visible even though rear stab layout changes`() {
        assertThat(EquipmentFieldRules.sectionVisible(Section.FRONT_STAB, BowType.RECURVE)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.FRONT_STAB_WEIGHT, BowType.RECURVE)).isTrue()
    }

    // ---------- Barebow ----------

    @Test
    fun `barebow shows brace height tiller plunger grip and nock only`() {
        assertThat(EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, BowType.BAREBOW)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.TILLER, BowType.BAREBOW)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.PLUNGER, BowType.BAREBOW)).isTrue()
        assertThat(EquipmentFieldRules.sectionVisible(Section.GRIP_AND_NOCK, BowType.BAREBOW)).isTrue()
    }

    @Test
    fun `barebow hides all stabilisers and clicker`() {
        assertThat(EquipmentFieldRules.sectionVisible(Section.FRONT_STAB, BowType.BAREBOW)).isFalse()
        assertThat(EquipmentFieldRules.sectionVisible(Section.REAR_STAB, BowType.BAREBOW)).isFalse()
        assertThat(EquipmentFieldRules.sectionVisible(Section.V_BAR, BowType.BAREBOW)).isFalse()
        assertThat(EquipmentFieldRules.sectionVisible(Section.CLICKER, BowType.BAREBOW)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.FRONT_STAB_WEIGHT, BowType.BAREBOW)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.CLICKER_POSITION, BowType.BAREBOW)).isFalse()
    }

    @Test
    fun `compound let-off and brace height are mutually exclusive by type`() {
        // Task-level requirement: compound → shows let-off, hides brace height; recurve inverts.
        assertThat(EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, BowType.COMPOUND, isSetup = true)).isTrue()
        assertThat(EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, BowType.COMPOUND, isSetup = true)).isFalse()

        assertThat(EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, BowType.RECURVE, isSetup = true)).isFalse()
        assertThat(EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, BowType.RECURVE, isSetup = true)).isTrue()
    }
}

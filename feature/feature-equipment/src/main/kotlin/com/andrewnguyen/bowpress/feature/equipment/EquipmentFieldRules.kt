package com.andrewnguyen.bowpress.feature.equipment

import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide

/**
 * Pure, allocation-free visibility predicate mirroring iOS `BowConfigEditView.swift`.
 *
 * Each field on the bow-config edit form is gated by [BowType] — and for the compound
 * rear-stabiliser sub-fields, additionally gated on [RearStabSide]. Lifting the rules
 * out of the Composable layer lets the Compose UI test assert visibility without
 * touching the renderer, and lets both the edit and detail screens share one truth.
 *
 * See the Swift source:
 *   /bowpress-ios/Sources/BowPress/Configuration/BowConfigEditView.swift (authoritative)
 *   /bowpress-ios/Sources/BowPress/Configuration/BowConfigDetailView.swift (read-only mirror)
 */
object EquipmentFieldRules {

    /**
     * Every editable/displayable field on a bow configuration. The ordering here is
     * top-to-bottom on the iOS form, not semantic — screens render sections by asking
     * the predicate whether the fields in a section should appear.
     */
    enum class Field {
        // Always (modulo bow type) — shared core
        DRAW_LENGTH,
        GRIP_ANGLE,
        NOCKING_HEIGHT,

        // Compound bow setup (isSetup=true)
        LET_OFF_PCT,
        PEEP_HEIGHT,
        D_LOOP_LENGTH,

        // Recurve / barebow bow setup
        BRACE_HEIGHT,

        // String & cable (compound only)
        TOP_CABLE_TWISTS,
        BOTTOM_CABLE_TWISTS,
        MAIN_STRING_TOP_TWISTS,
        MAIN_STRING_BOTTOM_TWISTS,

        // Limbs (compound only)
        TOP_LIMB_TURNS,
        BOTTOM_LIMB_TURNS,

        // Rest (compound only)
        REST_VERTICAL,
        REST_HORIZONTAL,
        REST_DEPTH,

        // Sight (compound only)
        SIGHT_POSITION,

        // Front stab (compound + recurve — not barebow)
        FRONT_STAB_WEIGHT,
        FRONT_STAB_ANGLE,

        // Compound rear stab (gated on side)
        REAR_STAB_SIDE,
        REAR_STAB_WEIGHT,
        REAR_STAB_VERT_ANGLE,
        REAR_STAB_HORIZ_ANGLE,

        // Recurve V-bar (replaces single rear stab)
        REAR_STAB_LEFT_WEIGHT,
        REAR_STAB_RIGHT_WEIGHT,

        // Recurve / barebow
        TILLER_TOP,
        TILLER_BOTTOM,
        PLUNGER_TENSION,

        // Recurve only
        CLICKER_POSITION,
    }

    /**
     * Section headers — roughly 1:1 with iOS Form `Section(...)` groupings. A section
     * is visible whenever any of its fields is visible; screens can simply hide the
     * whole section when [sectionVisible] returns false for the given bow type.
     */
    enum class Section {
        LABEL,            // always visible (the optional tuning-label text field)
        BOW_SETUP,        // draw length + type-specific setup fields
        BASE_SETUP,       // read-only summary when editing a tune after initial setup (compound, isSetup=false)
        STRING_AND_CABLE, // compound
        LIMBS,            // compound
        REST,             // compound
        SIGHT_GRIP_NOCK,  // compound
        GRIP_AND_NOCK,    // recurve / barebow
        TILLER,           // recurve / barebow
        PLUNGER,          // recurve / barebow
        CLICKER,          // recurve
        FRONT_STAB,       // compound + recurve
        REAR_STAB,        // compound
        V_BAR,            // recurve
    }

    /**
     * Whether [field] should render for the given bow type. For compound rear-stab
     * sub-fields, pass the currently-selected [rearStabSide] so weight/vert/horiz
     * collapse when "None" is picked — matches iOS `if rearStabSide != .none { … }`.
     *
     * [isSetup] only affects compound: setup shows editable let-off/peep/d-loop rows,
     * whereas the per-tune edit view collapses those into a read-only Base Setup
     * summary and hides the editors.
     */
    fun isVisible(
        field: Field,
        bowType: BowType,
        isSetup: Boolean = true,
        rearStabSide: RearStabSide = RearStabSide.NONE,
    ): Boolean = when (field) {
        // Shared core — present on every bow type
        Field.DRAW_LENGTH,
        Field.GRIP_ANGLE,
        Field.NOCKING_HEIGHT -> true

        // Compound setup fields are editable only during initial setup; after that,
        // they survive only as a read-only summary row (see Section.BASE_SETUP).
        Field.LET_OFF_PCT,
        Field.PEEP_HEIGHT,
        Field.D_LOOP_LENGTH -> bowType == BowType.COMPOUND && isSetup

        // Brace height replaces the compound setup block on recurve / barebow.
        Field.BRACE_HEIGHT -> bowType == BowType.RECURVE || bowType == BowType.BAREBOW

        // Compound-only sub-systems
        Field.TOP_CABLE_TWISTS,
        Field.BOTTOM_CABLE_TWISTS,
        Field.MAIN_STRING_TOP_TWISTS,
        Field.MAIN_STRING_BOTTOM_TWISTS,
        Field.TOP_LIMB_TURNS,
        Field.BOTTOM_LIMB_TURNS,
        Field.REST_VERTICAL,
        Field.REST_HORIZONTAL,
        Field.REST_DEPTH,
        Field.SIGHT_POSITION -> bowType == BowType.COMPOUND

        // Front stab: compound + recurve. Barebow has no stabs at all.
        Field.FRONT_STAB_WEIGHT,
        Field.FRONT_STAB_ANGLE -> bowType == BowType.COMPOUND || bowType == BowType.RECURVE

        // Compound single rear-stab — side is always shown on compound; weight/angle
        // rows hide when side == NONE, matching iOS `if rearStabSide != .none`.
        Field.REAR_STAB_SIDE -> bowType == BowType.COMPOUND
        Field.REAR_STAB_WEIGHT,
        Field.REAR_STAB_VERT_ANGLE,
        Field.REAR_STAB_HORIZ_ANGLE -> when (bowType) {
            BowType.COMPOUND -> rearStabSide != RearStabSide.NONE
            // Recurve uses the V-bar layout which includes its own vert/horiz rows.
            BowType.RECURVE -> field == Field.REAR_STAB_VERT_ANGLE ||
                field == Field.REAR_STAB_HORIZ_ANGLE
            BowType.BAREBOW -> false
        }

        // Recurve V-bar weights
        Field.REAR_STAB_LEFT_WEIGHT,
        Field.REAR_STAB_RIGHT_WEIGHT -> bowType == BowType.RECURVE

        // Tiller + plunger: recurve + barebow
        Field.TILLER_TOP,
        Field.TILLER_BOTTOM,
        Field.PLUNGER_TENSION -> bowType == BowType.RECURVE || bowType == BowType.BAREBOW

        // Clicker: recurve only
        Field.CLICKER_POSITION -> bowType == BowType.RECURVE
    }

    /**
     * Whether [section] should render at all. Computed from the individual field
     * visibility rules so there's a single source of truth.
     */
    fun sectionVisible(
        section: Section,
        bowType: BowType,
        isSetup: Boolean = true,
    ): Boolean = when (section) {
        Section.LABEL -> true

        Section.BOW_SETUP -> true // every type shows *some* setup (draw + maybe brace/letoff)

        Section.BASE_SETUP -> bowType == BowType.COMPOUND && !isSetup

        Section.STRING_AND_CABLE,
        Section.LIMBS,
        Section.REST,
        Section.SIGHT_GRIP_NOCK -> bowType == BowType.COMPOUND

        Section.GRIP_AND_NOCK,
        Section.TILLER,
        Section.PLUNGER -> bowType == BowType.RECURVE || bowType == BowType.BAREBOW

        Section.CLICKER -> bowType == BowType.RECURVE

        Section.FRONT_STAB -> bowType == BowType.COMPOUND || bowType == BowType.RECURVE

        Section.REAR_STAB -> bowType == BowType.COMPOUND
        Section.V_BAR -> bowType == BowType.RECURVE
    }
}

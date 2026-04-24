package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors iOS `BowConfiguration`. Many fields are optional because they only apply to
 * a specific bow type — see `bowConfigController.ts` for the required-field matrix:
 *   compound:  letOffPct, peepHeight, dLoopLength, twist/limb fields, sightPosition, front-stab,
 *              single rear-stab (weight + vert/horiz + side)
 *   recurve:   sightPosition, front-stab, braceHeight, tiller, plunger, clicker, V-bar L/R
 *   barebow:   braceHeight, tiller, plunger
 *
 * `isReference`, `referenceManuallyPinned`, `avgArrowScore`, `scoreable` are server-computed
 * analytics fields — clients only read them.
 */
@Serializable
data class BowConfiguration(
    val id: String,
    val bowId: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val label: String? = null,

    // Draw / letoff
    val drawLength: Double,
    val letOffPct: Double? = null,

    // String / cable
    val peepHeight: Double? = null,
    val dLoopLength: Double? = null,
    val topCableTwists: Int? = null,
    val bottomCableTwists: Int? = null,
    val mainStringTopTwists: Int? = null,
    val mainStringBottomTwists: Int? = null,

    // Limbs
    val topLimbTurns: Double? = null,
    val bottomLimbTurns: Double? = null,

    // Rest (sixteenth-inch increments where applicable)
    val restVertical: Int,
    val restHorizontal: Int,
    val restDepth: Double,

    // Sight / grip / nock
    val sightPosition: Int? = null,
    val gripAngle: Double,
    val nockingHeight: Int,

    // Front stabilizer (compound + recurve; barebow nil)
    val frontStabWeight: Double? = null,
    val frontStabAngle: Double? = null,

    // Compound single rear-stab (recurve/barebow use V-bar L/R below)
    val rearStabSide: RearStabSide? = null,
    val rearStabWeight: Double? = null,
    val rearStabVertAngle: Double? = null,
    val rearStabHorizAngle: Double? = null,

    // Recurve / barebow
    val braceHeight: Double? = null,
    val tillerTop: Double? = null,
    val tillerBottom: Double? = null,
    val plungerTension: Int? = null,
    val clickerPosition: Double? = null,
    val rearStabLeftWeight: Double? = null,
    val rearStabRightWeight: Double? = null,

    // Server-computed analytics fields
    val isReference: Boolean? = null,
    val referenceManuallyPinned: Boolean? = null,
    val avgArrowScore: Double? = null,
    val scoreable: Boolean? = null,
) {
    /** True when all tunable fields match, ignoring id, createdAt, and label. */
    fun hasMatchingValues(other: BowConfiguration): Boolean =
        bowId == other.bowId &&
            drawLength == other.drawLength && letOffPct == other.letOffPct &&
            peepHeight == other.peepHeight && dLoopLength == other.dLoopLength &&
            topCableTwists == other.topCableTwists && bottomCableTwists == other.bottomCableTwists &&
            mainStringTopTwists == other.mainStringTopTwists && mainStringBottomTwists == other.mainStringBottomTwists &&
            topLimbTurns == other.topLimbTurns && bottomLimbTurns == other.bottomLimbTurns &&
            restVertical == other.restVertical && restHorizontal == other.restHorizontal && restDepth == other.restDepth &&
            sightPosition == other.sightPosition && gripAngle == other.gripAngle && nockingHeight == other.nockingHeight &&
            frontStabWeight == other.frontStabWeight && frontStabAngle == other.frontStabAngle &&
            rearStabSide == other.rearStabSide && rearStabWeight == other.rearStabWeight &&
            rearStabVertAngle == other.rearStabVertAngle && rearStabHorizAngle == other.rearStabHorizAngle &&
            braceHeight == other.braceHeight &&
            tillerTop == other.tillerTop && tillerBottom == other.tillerBottom &&
            plungerTension == other.plungerTension && clickerPosition == other.clickerPosition &&
            rearStabLeftWeight == other.rearStabLeftWeight && rearStabRightWeight == other.rearStabRightWeight

    /**
     * Compact one-line summary for the "Base Setup" recap on edit screens.
     * Shape depends on which optional fields the config actually has.
     * e.g. `Draw 28.5" · Let-off 80% · Peep 9.25" · D-loop 2.125"` (compound imperial).
     */
    fun compactSetupLine(system: UnitSystem): String {
        val parts = mutableListOf(
            "Draw ${UnitFormatting.length(drawLength, system)}",
        )
        letOffPct?.let { parts += "Let-off ${UnitFormatting.percent(it)}" }
        peepHeight?.let { parts += "Peep ${UnitFormatting.length(it, system)}" }
        dLoopLength?.let { parts += "D-loop ${UnitFormatting.length(it, system, digits = 3)}" }
        braceHeight?.let { parts += "Brace ${UnitFormatting.length(it, system, digits = 3)}" }
        return parts.joinToString(" · ")
    }
}

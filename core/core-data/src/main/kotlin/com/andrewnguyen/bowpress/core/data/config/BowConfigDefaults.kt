package com.andrewnguyen.bowpress.core.data.config

import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import java.time.Instant
import java.util.UUID

/**
 * Seed a brand-new [BowConfiguration] with sensible defaults for [bow]. Mirrors
 * iOS `BowConfiguration.makeDefault(for:)`. Lives in core-data so both equipment
 * and session features can fall back to it when no persisted config exists yet
 * — matches iOS's "always have a configuration" pattern (BowDetailView.swift
 * seedState, SessionView/SessionConfigSheet `??` fallbacks).
 */
fun makeDefaultConfig(bow: Bow, now: Instant = Instant.now()): BowConfiguration {
    return when (bow.bowType) {
        BowType.COMPOUND -> BowConfiguration(
            id = UUID.randomUUID().toString(),
            bowId = bow.id,
            createdAt = now,
            label = "Initial Setup",
            drawLength = 28.0,
            letOffPct = 80.0,
            peepHeight = 9.0,
            dLoopLength = 2.0,
            restVertical = 0,
            restHorizontal = 0,
            restDepth = 0.0,
            sightPosition = 0,
            gripAngle = 0.0,
            nockingHeight = 0,
            frontStabWeight = 0.0,
            frontStabAngle = 0.0,
            rearStabSide = RearStabSide.NONE,
        )
        BowType.RECURVE -> BowConfiguration(
            id = UUID.randomUUID().toString(),
            bowId = bow.id,
            createdAt = now,
            label = "Initial Setup",
            drawLength = 28.0,
            restVertical = 0,
            restHorizontal = 0,
            restDepth = 0.0,
            gripAngle = 0.0,
            nockingHeight = 0,
            frontStabWeight = 6.0,
            frontStabAngle = 0.0,
            braceHeight = 8.5,
            tillerTop = 0.0,
            tillerBottom = 0.0,
            plungerTension = 12,
            clickerPosition = 0.0,
            rearStabLeftWeight = 6.0,
            rearStabRightWeight = 6.0,
            rearStabVertAngle = 0.0,
            rearStabHorizAngle = 0.0,
        )
        BowType.BAREBOW -> BowConfiguration(
            id = UUID.randomUUID().toString(),
            bowId = bow.id,
            createdAt = now,
            label = "Initial Setup",
            drawLength = 28.0,
            restVertical = 0,
            restHorizontal = 0,
            restDepth = 0.0,
            gripAngle = 0.0,
            nockingHeight = 0,
            braceHeight = 8.5,
            tillerTop = 0.0,
            tillerBottom = 0.0,
            plungerTension = 12,
        )
    }
}

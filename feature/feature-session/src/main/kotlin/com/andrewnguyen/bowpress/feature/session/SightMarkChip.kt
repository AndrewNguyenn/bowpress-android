package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.DistanceUnit
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.SightMark
import com.andrewnguyen.bowpress.core.model.SightMarkSuggester
import kotlin.math.abs

/**
 * In-session chip showing the relevant sight mark for the active bow at
 * the session's target distance. Renders nothing when there's no useful
 * reading to show — better silent than a fabricated number. Mirrors iOS
 * `SightMarkChip` (SightMarks/SightMarkChip.swift).
 *
 * Match logic:
 *   1. If a measured mark exists within 5cm of the session distance
 *      (compared in meters-space so a 54.68yd mark matches 50m), surface
 *      it as MEASURED.
 *   2. Otherwise, ask [SightMarkSuggester] for a quadratic fit. If gates
 *      pass, surface as SUGGESTED.
 *   3. Otherwise render nothing.
 */
@Composable
fun SightMarkChip(
    bowId: String?,
    distance: ShootingDistance?,
    modifier: Modifier = Modifier,
    viewModel: SightMarkChipViewModel = hiltViewModel(),
) {
    if (bowId == null || distance == null) return
    val marks by viewModel.marksFor(bowId).collectAsState(initial = emptyList())
    val outcome = remember(marks, distance) { lookup(marks, distance) } ?: return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppCream)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("sight_mark_chip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (outcome.isSuggested) Icons.Filled.AutoAwesome else Icons.Filled.GpsFixed,
            contentDescription = null,
            tint = if (outcome.isSuggested) AppInk3 else AppPondDk,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = if (outcome.isSuggested) "Suggested" else "Sight mark",
            style = interUI(12.sp, weight = FontWeight.Medium).copy(
                letterSpacing = 0.04.em,
                color = AppInk3,
            ),
        )
        Text(
            text = "%.2f".format(outcome.mark),
            style = jetbrainsMono(14.sp, weight = FontWeight.SemiBold).copy(color = AppInk),
        )
    }
}

private data class ChipOutcome(val mark: Double, val isSuggested: Boolean)

private fun lookup(marks: List<SightMark>, distance: ShootingDistance): ChipOutcome? {
    val (value, unit) = distanceValueAndUnit(distance)
    val targetMeters = value * unit.metersPerUnit
    // Tolerance ~5cm — tight enough to avoid false matches between
    // adjacent shoot distances, loose enough to absorb the
    // imperial/metric rounding boundary (54.68yd ≈ 50m).
    val measured = marks.firstOrNull { m ->
        !m.isSuggestion && abs(m.distanceInMeters - targetMeters) < 0.05
    }
    if (measured != null) return ChipOutcome(measured.mark, isSuggested = false)

    val outcome = SightMarkSuggester.suggest(value, unit, marks)
    return (outcome as? SightMarkSuggester.Outcome.Suggested)
        ?.let { ChipOutcome(it.suggestion.mark, isSuggested = true) }
}

private fun distanceValueAndUnit(d: ShootingDistance): Pair<Double, DistanceUnit> = when (d) {
    ShootingDistance.YARDS_20 -> 20.0 to DistanceUnit.YARDS
    ShootingDistance.METERS_50 -> 50.0 to DistanceUnit.METERS
    ShootingDistance.METERS_70 -> 70.0 to DistanceUnit.METERS
}

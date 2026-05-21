package com.andrewnguyen.bowpress.core.designsystem.coursemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import com.andrewnguyen.bowpress.core.model.UnitSystem
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Square tile showing a station's stored photo, or a labelled placeholder. */
@Composable
fun StationPhotoTile(
    stationId: String,
    slot: CourseStationPhotoStore.Slot,
    present: Boolean,
    side: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, stationId, slot, present) {
        value = if (present) CourseStationPhotoStore.load(context, stationId, slot) else null
    }
    Box(
        modifier
            .size(side)
            .background(AppPaper2)
            .border(1.dp, AppLine),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = slot.key,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("—", style = jetbrainsMono(9.sp).copy(color = AppInk3))
        }
    }
}

/** A small compass dial with a needle pointing along `bearing` (0° = north, up). */
@Composable
fun CompassDial(bearing: Double, modifier: Modifier = Modifier, diameter: Dp = 34.dp) {
    Canvas(modifier.size(diameter)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(AppLine, radius = r, style = Stroke(width = 1f))
        val rad = Math.toRadians(bearing - 90.0)
        val tip = Offset(
            c.x + (r * 0.78f) * cos(rad).toFloat(),
            c.y + (r * 0.78f) * sin(rad).toFloat(),
        )
        drawLine(AppMaple, c, tip, strokeWidth = 2f)
        drawCircle(AppInk, radius = 1.5f, center = c)
    }
}

/**
 * The shared station-detail card — scene + arrow photos, distance / angle /
 * score telemetry, a mini circular target, and the compass bearing.
 */
@Composable
fun ThreeDStationCard(
    station: CourseStation,
    stationCount: Int,
    system: ThreeDScoringSystem,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(AppPaper)
            .border(1.dp, AppMaple)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "● STATION ${station.stationNumber} OF $stationCount",
            style = interUI(9.sp, FontWeight.SemiBold).copy(color = AppMaple),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationPhotoTile(station.id, CourseStationPhotoStore.Slot.SCENE, station.hasScenePhoto, 56.dp)
            StationPhotoTile(station.id, CourseStationPhotoStore.Slot.ARROW, station.hasArrowPhoto, 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StatLine("DIST", DistanceFormatting.short(station.estimatedDistance, station.distanceUnit, unitSystem))
                StatLine("ANGLE", AngleFormatting.signed(station.angleDegrees))
                StatLine("SCORE", if (station.ring == 0) "M" else "${station.ring}", emphasis = true)
            }
            CircleTargetView(
                system = system,
                arrows = arrowsFor(station),
                showLabels = false,
                modifier = Modifier.size(56.dp),
            )
        }
        if (station.bearingDegrees != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompassDial(station.bearingDegrees!!, diameter = 28.dp)
                Text(
                    "BEARING ${BearingFormatting.compass(station.bearingDegrees!!)} · TARGET PLOTTED ON MAP",
                    style = jetbrainsMono(9.sp).copy(color = AppInk3),
                )
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, emphasis: Boolean = false) {
    Column {
        Text(label, style = jetbrainsMono(8.5.sp).copy(color = AppInk3))
        Text(
            value,
            style = frauncesDisplay(16.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = if (emphasis) AppPondDk else AppInk),
        )
    }
}

/** One row of a station table — number, distance, angle, score, mini target. */
@Composable
fun CourseStationRow(
    station: CourseStation,
    system: ThreeDScoringSystem,
    unitSystem: UnitSystem,
    focused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(if (focused) AppPaper2 else AppPaper)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "%02d".format(station.stationNumber),
            style = jetbrainsMono(11.sp).copy(color = if (focused) AppMaple else AppInk2),
        )
        Column(Modifier.weight(1f)) {
            Text(
                DistanceFormatting.short(station.estimatedDistance, station.distanceUnit, unitSystem) +
                    "  ${AngleFormatting.signed(station.angleDegrees)}",
                style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
            )
            Text(
                if (station.hasArrowPhoto) "SCENE · ARROW" else "SCENE",
                style = jetbrainsMono(8.5.sp).copy(color = AppInk3),
            )
        }
        CircleTargetView(
            system = system,
            arrows = arrowsFor(station),
            showLabels = false,
            modifier = Modifier.size(26.dp),
        )
        Text(
            if (station.ring == 0) "M" else "${station.ring}",
            style = frauncesDisplay(16.sp, italic = true, weight = FontWeight.Medium).copy(color = AppPondDk),
        )
    }
}

fun arrowsFor(station: CourseStation): List<CircleArrow> {
    val x = station.plotX ?: return emptyList()
    val y = station.plotY ?: return emptyList()
    return listOf(CircleArrow(station.id, x, y))
}

/** Score chip-row used by the plot + edit screens. */
@Composable
fun ScoreChipRow(
    system: ThreeDScoringSystem,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val chips = system.rings + 0
        chips.forEach { value ->
            val on = selected == value
            Box(
                Modifier
                    .size(30.dp)
                    .background(if (on) AppPaper2 else AppPaper)
                    .border(1.dp, if (on) AppPondDk else AppLine)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (value == 0) "M" else "$value",
                    style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = if (on) AppPondDk else AppInk2),
                )
            }
        }
    }
}

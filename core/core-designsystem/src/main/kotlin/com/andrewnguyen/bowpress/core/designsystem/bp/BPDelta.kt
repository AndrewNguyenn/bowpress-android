package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import kotlin.math.roundToInt

/**
 * Numeric delta chip — positive (pine), negative (maple) or flat (stone
 * em-dash). JetBrains Mono 10sp with 0.04em tracking, translucent tone fill.
 */
@Composable
fun BPDelta(
    value: Double,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    val rounded = (value * 10.0).roundToInt() / 10.0
    val text = when {
        rounded > 0 -> "+" + formatDelta(rounded) + suffix
        rounded < 0 -> formatDelta(rounded) + suffix
        else -> "—" // em-dash
    }
    val fg: Color = when {
        rounded > 0 -> AppPine
        rounded < 0 -> AppMaple
        else -> AppInk3
    }
    val bg: Color = when {
        rounded > 0 -> AppPine.copy(alpha = 0.16f)
        rounded < 0 -> AppMaple.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    Text(
        text = text,
        modifier = modifier
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        style = jetbrainsMono(10.sp).copy(
            letterSpacing = 0.04.em,
            color = fg,
        ),
    )
}

private fun formatDelta(v: Double): String {
    val whole = v.toLong().toDouble()
    return if (v == whole) "%.0f".format(v) else "%.1f".format(v)
}

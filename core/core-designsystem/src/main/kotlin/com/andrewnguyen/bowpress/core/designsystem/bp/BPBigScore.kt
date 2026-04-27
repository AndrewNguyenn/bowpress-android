package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppDeep
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import kotlin.math.max

/**
 * Hero numeric — splits on the decimal point. Integer and fractional parts
 * render in AppDeep; the dot itself paints AppMaple (the lone maple leaf).
 * Hero sizes (≥ 48sp) render upright per spec; smaller callouts keep the
 * house italic.
 */
@Composable
fun BPBigScore(
    value: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 56.sp,
    unit: String? = null,
) {
    val italic = size.value < 48f
    val parts = value.split(".", limit = 2)
    val main = frauncesDisplay(size, italic = italic, weight = FontWeight.Medium)
        .copy(color = AppDeep)
    val dotStyle = main.copy(color = AppMaple)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(parts[0], style = main, maxLines = 1)
        if (parts.size == 2) {
            Text(".", style = dotStyle, maxLines = 1)
            Text(parts[1], style = main, maxLines = 1)
        }
        if (!unit.isNullOrEmpty()) {
            val unitSize = max(10f, size.value * 0.2f).sp
            Text(
                text = unit.uppercase(),
                modifier = Modifier.padding(start = 4.dp),
                style = interUI(unitSize, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.12.em,
                    color = AppInk3,
                ),
                maxLines = 1,
            )
        }
    }
}

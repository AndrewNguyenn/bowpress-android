package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppStone
import com.andrewnguyen.bowpress.core.designsystem.interUI

enum class BPStampTone { Pond, Pine, Maple, Stone, Ink3 }

private fun BPStampTone.color(): Color = when (this) {
    BPStampTone.Pond -> AppPondDk
    BPStampTone.Pine -> AppPine
    BPStampTone.Maple -> AppMaple
    BPStampTone.Stone -> AppStone
    BPStampTone.Ink3 -> AppInk3
}

/**
 * Flat outlined stamp — the "pill" we never round. Outlined by default; set
 * [solid] to invert to a filled chip. 9sp Inter 600, 0.22em letter-spacing,
 * uppercased at render time via the caller's string.
 */
@Composable
fun BPStamp(
    text: String,
    modifier: Modifier = Modifier,
    tone: BPStampTone = BPStampTone.Pond,
    solid: Boolean = false,
) {
    val col = tone.color()
    val fg = if (solid) AppPaper else col
    Text(
        text = text.uppercase(),
        style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
            letterSpacing = 0.22.em,
            color = fg,
        ),
        modifier = modifier
            .background(if (solid) col else Color.Transparent)
            .border(1.dp, col)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.bpEyebrow

/**
 * Small caps "eyebrow" label — section kicker / meta label. Inter 600
 * uppercase with 0.22em tracking. Default tone is AppInk3; callers pass
 * [tone] for section tints.
 */
@Composable
fun BPEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color = AppInk3,
    size: TextUnit = 9.sp,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = bpEyebrow(size).copy(color = tone),
    )
}

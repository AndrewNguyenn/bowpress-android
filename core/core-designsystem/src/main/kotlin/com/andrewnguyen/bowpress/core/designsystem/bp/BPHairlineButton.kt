package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * Secondary button — 1dp hairline border, uppercase Inter label, no fill.
 * Tint'able when a destructive/info variant is needed; label + border share
 * the tint so the chip reads as one tone.
 */
@Composable
fun BPHairlineButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelTone: Color = AppInk,
    borderTone: Color = AppLine,
) {
    Box(
        modifier = modifier
            .border(1.dp, borderTone)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = interUI(11.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.18.em,
                color = labelTone,
            ),
        )
    }
}

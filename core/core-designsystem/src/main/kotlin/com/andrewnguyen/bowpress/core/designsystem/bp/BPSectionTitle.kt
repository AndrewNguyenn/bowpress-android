package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * Section kicker — Fraunces italic 16sp title + optional right-aligned Inter
 * 9.5sp 0.20em aside label (e.g. "LAST 14 DAYS"). Used to lead cards/lists
 * inside a screen.
 */
@Composable
fun BPSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    aside: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = frauncesDisplay(16.sp, italic = true).copy(color = AppInk),
        )
        if (aside != null) {
            Text(
                text = aside.uppercase(),
                style = interUI(9.5.sp).copy(
                    letterSpacing = 0.20.em,
                    color = AppInk3,
                ),
            )
        }
    }
}

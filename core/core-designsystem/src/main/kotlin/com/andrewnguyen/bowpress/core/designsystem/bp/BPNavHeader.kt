package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * Kenrokuen nav header — Fraunces italic title + "BOWPRESS" wide-tracked
 * eyebrow, plus an optional right-aligned meta slot (session meta, filter
 * summary, edit link). Bottom 1dp AppLine hairline.
 */
@Composable
fun BPNavHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String = "BOWPRESS",
    meta: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height
                drawLine(
                    color = AppLine,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = eyebrow.uppercase(),
                style = interUI(10.5.sp).copy(
                    letterSpacing = 0.32.em,
                    color = AppInk3,
                ),
            )
            Text(
                text = title,
                style = frauncesDisplay(28.sp, italic = true).copy(color = AppInk),
            )
        }
        if (meta != null) meta()
    }
}

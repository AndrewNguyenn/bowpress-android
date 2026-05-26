package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay

/**
 * Kenrokuen nav header — Fraunces italic title plus an optional
 * right-aligned meta slot (session meta, filter summary, edit link).
 * Bottom 1dp AppLine hairline.
 */
@Composable
fun BPNavHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: (@Composable () -> Unit)? = null,
) {
    val underlineColor = AppLine
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height
                drawLine(
                    color = underlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = frauncesDisplay(28.sp, italic = true).copy(color = AppInk),
        )
        if (meta != null) meta()
    }
}

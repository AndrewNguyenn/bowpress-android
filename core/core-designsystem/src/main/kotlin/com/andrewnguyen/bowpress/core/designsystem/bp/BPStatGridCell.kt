package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * Single stat grid cell — eyebrow label on top, primary value slot
 * ([mainContent]), optional [sub] secondary line, optional trailing [ticks]
 * (sparkline, delta, etc.).
 */
@Composable
fun BPStatGridCell(
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    ticks: (@Composable () -> Unit)? = null,
    mainContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BPEyebrow(text = label)
        mainContent()
        if (sub != null) {
            Text(
                text = sub,
                style = interUI(11.sp).copy(color = AppInk2),
            )
        }
        ticks?.invoke()
    }
}

package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2

/**
 * Flat 1dp-hairline card — no rounded corners, no shadows, no elevation.
 * Inset variant swaps the paper fill for the darker AppPaper2 to nest
 * surfaces ledger-style.
 */
@Composable
fun BPCard(
    modifier: Modifier = Modifier,
    inset: Boolean = false,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(if (inset) AppPaper2 else AppPaper)
            .border(1.dp, AppLine)
            .padding(padding),
        content = content,
    )
}

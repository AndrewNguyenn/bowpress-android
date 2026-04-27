package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * Pill-card summary — AppPaper2 fill + hairline border, Fraunces italic
 * summary line + Inter monoesque subtitle, trailing "EDIT ›" link. The whole
 * row is tappable; callers open a sheet or navigate from [onEdit].
 */
@Composable
fun BPFilterSummary(
    summary: String,
    subtitle: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppPaper2)
            .border(1.dp, AppLine)
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = summary,
                style = frauncesDisplay(14.sp, italic = true).copy(color = AppInk),
                maxLines = 2,
            )
            Text(
                text = subtitle,
                style = interUI(10.sp).copy(
                    letterSpacing = 0.04.em,
                    color = AppInk3,
                ),
                maxLines = 1,
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        BPEditLink(onClick = onEdit)
    }
}

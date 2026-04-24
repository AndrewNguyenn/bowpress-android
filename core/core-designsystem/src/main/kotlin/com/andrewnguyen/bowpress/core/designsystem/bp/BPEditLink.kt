package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * "EDIT ›" affordance — uppercase Inter label with a trailing Fraunces italic
 * guillemet. Lives in the trailing slot of nav/header/summary rows. The whole
 * row is clickable.
 */
@Composable
fun BPEditLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Edit",
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = interUI(11.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.18.em,
                color = AppPondDk,
            ),
        )
        Text(
            text = " ›",
            style = frauncesDisplay(14.sp, italic = true).copy(color = AppPondDk),
        )
    }
}

package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppPondLt
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * Full-width primary CTA — pond-dark background, Fraunces italic title, a
 * small Inter eyebrow subtitle, and a trailing oversized "›" that carries the
 * hand-drawn brand feel. No rounded corners, no shadow.
 */
@Composable
fun BPPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (enabled) AppPondDk else AppPondLt)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = frauncesDisplay(20.sp, italic = true).copy(color = AppPaper),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = interUI(9.sp, weight = FontWeight.SemiBold).copy(
                        letterSpacing = 0.20.em,
                        color = AppPaper.copy(alpha = 0.72f),
                    ),
                )
            }
        }
        Text(
            text = "›",
            style = frauncesDisplay(32.sp, italic = true).copy(color = AppPaper),
        )
    }
}

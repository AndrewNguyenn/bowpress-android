package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay

/**
 * The bow**press** wordmark — Fraunces italic with "press" rendered in
 * AppPondDk. Used on auth screens and the splash.
 */
@Composable
fun BPWordmark(
    modifier: Modifier = Modifier,
    size: TextUnit = 36.sp,
) {
    val base = frauncesDisplay(size, italic = true)
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = AppInk)) { append("bow") }
        withStyle(SpanStyle(color = AppPondDk)) { append("press") }
    }
    Text(text = text, style = base, modifier = modifier)
}

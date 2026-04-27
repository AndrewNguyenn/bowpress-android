package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

/**
 * Numbered suggestion/trend row — fixed 22dp index column (Fraunces italic
 * roman numeral in AppPond), flex body (italic title + Inter detail + mono
 * footnote + optional accessory slot), trailing stamp slot.
 */
@Composable
fun BPLedgerRow(
    index: Int,
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    monoLine: String? = null,
    stamp: (@Composable () -> Unit)? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = index.toString(),
            modifier = Modifier.width(22.dp),
            style = frauncesDisplay(17.sp, italic = true).copy(color = AppPond),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = frauncesDisplay(15.sp, italic = true).copy(color = AppInk),
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = interUI(11.5.sp).copy(color = AppInk2),
                )
            }
            if (monoLine != null) {
                Text(
                    text = monoLine,
                    style = jetbrainsMono(9.5.sp).copy(
                        letterSpacing = 0.06.em,
                        color = AppInk3,
                    ),
                )
            }
            accessory?.invoke()
        }
        stamp?.invoke()
    }
}

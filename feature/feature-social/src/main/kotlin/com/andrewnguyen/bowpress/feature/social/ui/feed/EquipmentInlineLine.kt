package com.andrewnguyen.bowpress.feature.social.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono

/**
 * One-line mono strip beneath the verb / description on a feed card:
 * `bow name · bow type · arrow set name`. Ports the design exploration
 * `explorations/Feed Tile - Equipment Inline.html` and mirrors iOS
 * `EquipmentInlineLine` (commit 548ee80).
 *
 * * font: JetBrains Mono, 10sp, 0.06em tracking, uppercase
 * * bow name is the only AppInk2 / weight-500 span (the user-authored
 *   proper noun); bow type + arrow name stay AppInk3 / regular
 * * middle-dot separators in [AppLine] tone so the three names read as
 *   siblings, not a sentence
 * * single line, ellipsize-end so a verbose
 *   `Mathews TRX 38 G2 Custom` doesn't push the card open
 * * a nil/empty field is dropped along with the adjacent separator — a
 *   session shared without an arrow shows just bow · type, etc.
 *
 * Renders nothing when every input is null/blank so callers don't have
 * to gate the surrounding padding.
 */
@Composable
fun EquipmentInlineLine(
    bowName: String?,
    bowType: String?,
    arrowName: String?,
    modifier: Modifier = Modifier,
) {
    val spans = orderedSpans(bowName, bowType, arrowName)
    if (spans.isEmpty()) return

    val annotated = buildAnnotatedString {
        spans.forEachIndexed { index, span ->
            if (index > 0) {
                withStyle(SpanStyle(color = AppLine)) {
                    append(" · ")
                }
            }
            withStyle(span.style) {
                append(span.text.uppercase())
            }
        }
    }

    val voLabel = accessibilityLabel(bowName, bowType, arrowName)

    Text(
        text = annotated,
        style = jetbrainsMono(10.sp).copy(letterSpacing = 0.06.em),
        color = AppInk3,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        // `clearAndSetSemantics` replaces the inferred `Text` semantics
        // (which would otherwise speak the literal uppercased+separated
        // glyphs to TalkBack) with the rolled-up "Bow X, Y, Arrow Z"
        // label. Equivalent to iOS's
        // `.accessibilityElement(children: .ignore)
        // + .accessibilityLabel(...)`.
        modifier = modifier.clearAndSetSemantics { contentDescription = voLabel },
    )
}

private data class EquipmentSpan(val text: String, val style: SpanStyle)

private fun orderedSpans(
    bowName: String?,
    bowType: String?,
    arrowName: String?,
): List<EquipmentSpan> {
    val out = mutableListOf<EquipmentSpan>()
    bowName?.takeIf { it.isNotBlank() }?.let {
        // Primary span — the user-authored proper noun. AppInk2 / 500.
        out += EquipmentSpan(it, SpanStyle(color = AppInk2, fontWeight = FontWeight.Medium))
    }
    bowType?.takeIf { it.isNotBlank() }?.let {
        out += EquipmentSpan(it, SpanStyle())
    }
    arrowName?.takeIf { it.isNotBlank() }?.let {
        out += EquipmentSpan(it, SpanStyle())
    }
    return out
}

/** "Bow Hoyt Prevail 37, Compound, Arrow Easton X10 Match." — drops blanks. */
private fun accessibilityLabel(
    bowName: String?,
    bowType: String?,
    arrowName: String?,
): String {
    val parts = mutableListOf<String>()
    bowName?.takeIf { it.isNotBlank() }?.let { parts += "Bow $it" }
    bowType?.takeIf { it.isNotBlank() }?.let { parts += it }
    arrowName?.takeIf { it.isNotBlank() }?.let { parts += "Arrow $it" }
    return parts.joinToString(", ")
}

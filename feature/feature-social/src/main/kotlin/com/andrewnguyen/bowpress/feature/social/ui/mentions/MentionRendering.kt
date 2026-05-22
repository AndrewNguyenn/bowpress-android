package com.andrewnguyen.bowpress.feature.social.ui.mentions

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk

/** Annotation tag carrying a tapped mention's handle out of [mentionAnnotatedString]. */
const val MENTION_ANNOTATION_TAG = "mention"

/**
 * Builds an [AnnotatedString] from [text] where every `@handle` token
 * (mentions contract §3.2) is rendered pond-toned and carries a
 * [MENTION_ANNOTATION_TAG] string annotation holding the bare handle, so a tap
 * can resolve it to a profile. Non-mention text keeps the ambient style.
 *
 * Detection reuses [MentionText.mentionSpans] — the same syntactic rule the
 * server parses with — so a trailing dot stays plain text and a too-short
 * token is not styled. An unresolved handle is intentionally still styled and
 * still tappable; the *tap handler* decides resolution (an unresolved handle
 * resolves to null → no-op), keeping this function free of any network call.
 */
fun mentionAnnotatedString(text: String): AnnotatedString {
    val spans = MentionText.mentionSpans(text)
    if (spans.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        for (span in spans) {
            // Plain text before the mention.
            if (span.start > cursor) append(text.substring(cursor, span.start))
            // The mention itself — pond-toned, medium weight, tap annotation.
            pushStringAnnotation(tag = MENTION_ANNOTATION_TAG, annotation = span.handle)
            withStyle(SpanStyle(color = AppPondDk, fontWeight = FontWeight.Medium)) {
                append(text.substring(span.start, span.end))
            }
            pop()
            cursor = span.end
        }
        // Trailing plain text after the last mention.
        if (cursor < text.length) append(text.substring(cursor))
    }
}

/**
 * Renders [text] with `@handle` mentions as pond-toned, tappable spans
 * (mentions contract §3.2). Tapping a mention invokes [onMentionTap] with the
 * bare handle; the caller resolves it (via the archer-by-handle endpoint) and
 * navigates — an unresolved handle should resolve to a no-op.
 *
 * [style] is the ambient text style for the non-mention runs (the mention runs
 * override only colour + weight). A body with no mentions renders exactly as a
 * plain `Text`.
 */
@Composable
fun MentionBodyText(
    text: String,
    style: TextStyle,
    onMentionTap: (handle: String) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val annotated = mentionAnnotatedString(text)
    @Suppress("DEPRECATION") // ClickableText is the simplest stable per-span tap path.
    ClickableText(
        text = annotated,
        style = style,
        modifier = modifier,
        maxLines = maxLines,
        onClick = { offset ->
            annotated.getStringAnnotations(MENTION_ANNOTATION_TAG, offset, offset)
                .firstOrNull()
                ?.let { onMentionTap(it.item) }
        },
    )
}

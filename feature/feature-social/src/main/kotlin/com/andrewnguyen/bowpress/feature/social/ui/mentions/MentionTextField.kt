package com.andrewnguyen.bowpress.feature.social.ui.mentions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
import kotlinx.coroutines.delay

/** Debounce window before an in-progress `@token` fires a handle search. */
private const val SEARCH_DEBOUNCE_MS = 250L

/**
 * Where the autocomplete suggestion list sits relative to the field. A field
 * near the bottom of the screen (the comment composer) wants the list
 * [Above]; a field with room below (the session-name setup field) wants it
 * [Below].
 */
enum class MentionListPlacement { Above, Below }

/**
 * A `BasicTextField` with `@`-mention autocomplete (mentions contract §3.1) —
 * the single reusable composer shared by the session-name field and the
 * comment / reply composer.
 *
 * The caller owns the text as a plain [String] (matching the existing
 * composers). Internally a [TextFieldValue] is kept so the caret is known: as
 * the user types, an in-progress `@token` at the caret is detected
 * ([MentionText.activeMentionQuery]), debounced ~250ms, and passed to
 * [onSearch]; the returned [HandleSuggestion]s render in a flat,
 * hairline-bordered, paper-ground list anchored directly under the field.
 * Tapping a row replaces the token with `@handle ` and dismisses the list.
 * The list also dismisses when the caret leaves any mention token (a space, a
 * selection, a caret move).
 *
 * Layout is the caller's: [decorationBox] places the placeholder, and the
 * suggestion list is emitted as a sibling *below* the field inside this
 * composable's own [Column], so the caller drops [MentionTextField] in
 * wherever the bare field used to sit.
 */
@Composable
fun MentionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: suspend (String) -> List<HandleSuggestion>,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    cursorColor: androidx.compose.ui.graphics.Color = AppPondDk,
    listPlacement: MentionListPlacement = MentionListPlacement.Below,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() },
) {
    // The TextFieldValue is the source of caret truth. It is re-seeded from
    // [value] only when the *text* diverges (a ViewModel-driven change such as
    // a reply-mention prefill) so a pure caret move doesn't get clobbered.
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    if (fieldValue.text != value) {
        // External text change — adopt it, caret to the end.
        fieldValue = TextFieldValue(value, selection = androidx.compose.ui.text.TextRange(value.length))
    }

    // The active autocomplete query, debounced. Null when no in-progress token.
    var query by remember { mutableStateOf<MentionQuery?>(null) }
    var suggestions by remember { mutableStateOf<List<HandleSuggestion>>(emptyList()) }

    // Re-derive the active query whenever the field changes — only when the
    // selection is a collapsed caret (no token detection mid-selection).
    val caret = fieldValue.selection.takeIf { it.collapsed }?.start
    LaunchedEffect(fieldValue.text, caret) {
        val q = caret?.let { MentionText.activeMentionQuery(fieldValue.text, it) }
        query = q
        if (q == null) {
            suggestions = emptyList()
        } else {
            // Debounce — a fast typist shouldn't fire a request per keystroke.
            delay(SEARCH_DEBOUNCE_MS)
            suggestions = runCatching { onSearch(q.prefix) }.getOrDefault(emptyList())
        }
    }

    val field = @Composable {
        BasicTextField(
            value = fieldValue,
            onValueChange = { next ->
                fieldValue = next
                if (next.text != value) onValueChange(next.text)
            },
            textStyle = textStyle,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(cursorColor),
            modifier = fieldModifier,
            decorationBox = decorationBox,
        )
    }

    val list = @Composable {
        // ── Suggestion list — flat, hairline-bordered, paper ground ──
        val rows = suggestions
        if (query != null && rows.isNotEmpty()) {
            MentionSuggestionList(
                placement = listPlacement,
                suggestions = rows,
                onPick = { picked ->
                    val q = query ?: return@MentionSuggestionList
                    val inserted = MentionText.insertMention(fieldValue.text, q, picked.handle)
                    fieldValue = TextFieldValue(
                        text = inserted.text,
                        selection = androidx.compose.ui.text.TextRange(inserted.caret),
                    )
                    onValueChange(inserted.text)
                    // Insertion ends the token (the trailing space) — dismiss.
                    query = null
                    suggestions = emptyList()
                },
            )
        }
    }

    Column(modifier = modifier) {
        when (listPlacement) {
            MentionListPlacement.Above -> {
                list()
                field()
            }
            MentionListPlacement.Below -> {
                field()
                list()
            }
        }
    }
}

/**
 * The flat suggestion list — a hairline-bordered, paper-ground column of
 * handle + display-name rows. Capped in height so a long list scrolls rather
 * than pushing the composer off-screen.
 */
@Composable
private fun MentionSuggestionList(
    placement: MentionListPlacement,
    suggestions: List<HandleSuggestion>,
    onPick: (HandleSuggestion) -> Unit,
) {
    // A small gap between the list and the field, on whichever side faces it.
    val gap = when (placement) {
        MentionListPlacement.Above -> Modifier.padding(bottom = 6.dp)
        MentionListPlacement.Below -> Modifier.padding(top = 6.dp)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .then(gap)
            .border(1.dp, AppLine)
            .background(AppPaper)
            .heightIn(max = 188.dp)
            .testTag(TestTags.MentionSuggestionList),
    ) {
        items(suggestions, key = { it.userId }) { suggestion ->
            MentionSuggestionRow(suggestion = suggestion, onPick = { onPick(suggestion) })
            if (suggestion != suggestions.last()) {
                HorizontalDivider(color = AppLine, thickness = 1.dp)
            }
        }
    }
}

/** One suggestion row — `@handle` in pond mono over the display name in Fraunces. */
@Composable
private fun MentionSuggestionRow(suggestion: HandleSuggestion, onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .testTag(TestTags.MentionSuggestionRowPrefix + suggestion.handle)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = "@${suggestion.handle}",
            style = jetbrainsMono(10.5.sp, FontWeight.Medium),
            color = AppPondDk,
        )
        Text(
            text = suggestion.displayName,
            style = frauncesDisplay(12.5.sp, italic = true, weight = FontWeight.Normal)
                .copy(color = AppInk),
        )
    }
}

package com.andrewnguyen.bowpress.feature.social.ui.mentions

/**
 * Pure mention-text logic shared by the `@`-autocomplete composer and the
 * mention-rendering spans (mentions contract §3). Kept free of any Compose /
 * Android dependency so the token detection, insertion, and span extraction
 * are unit-testable on a plain JVM classpath.
 *
 * Mention syntax (contract §1): a mention is `@` + a handle, the handle being
 * `[a-z0-9._]`, 3–30 chars, with no leading/trailing dot. Handles are
 * lowercased. The server is authoritative on whether a mention "resolves" —
 * the client only detects the syntactic token.
 */
object MentionText {

    /** The character class a handle is built from — lowercase letters, digits, dot, underscore. */
    private const val HANDLE_CHARS = "a-z0-9._"

    /**
     * A complete, syntactically valid handle: 3–30 chars from [HANDLE_CHARS],
     * no leading or trailing dot. Used for [mentionSpans].
     */
    private val HANDLE = Regex("(?![._])[$HANDLE_CHARS]{3,30}")

    /**
     * Finds every `@handle` token in [text] as a [MentionSpan] (the `@`, the
     * handle, and the text range). A trailing dot on the captured handle is
     * trimmed — `@sara.` mentions `sara` and the dot stays plain text, per the
     * contract's "trim trailing dots" rule. Tokens shorter than the 3-char
     * minimum after trimming are dropped.
     *
     * The matched `@` must be at a **word boundary** — index 0 or preceded by
     * whitespace — mirroring [activeMentionQuery] and, crucially, the server's
     * authoritative parse: an email / mid-word `@` (`andrew@bowpress.app`) is
     * NOT a mention. A consequence: two glued mentions `@sara@marcus` resolve
     * only `@sara` (the second `@` follows a handle char, not a boundary) —
     * intentional, matching the server.
     *
     * Case-insensitive against the source; the [MentionSpan.handle] is
     * lowercased so callers compare against canonical handles.
     */
    fun mentionSpans(text: String): List<MentionSpan> {
        val out = mutableListOf<MentionSpan>()
        // Scan for `@` followed by handle characters; trim trailing dots, then
        // validate the remainder.
        // Case-insensitive — a mention typed with capitals (`@Sara`) still
        // resolves; the captured handle is lowercased to its canonical form.
        val token = Regex("@([$HANDLE_CHARS]{2,30})", RegexOption.IGNORE_CASE)
        for (m in token.findAll(text)) {
            val start = m.range.first
            // Word-boundary gate — the `@` must start the text or follow
            // whitespace, so `a@b` (an email) never renders as a mention.
            if (start > 0 && !text[start - 1].isWhitespace()) continue
            val raw = m.groupValues[1]
            // Trim any trailing dot(s) — they are not part of the mention.
            val handle = raw.trimEnd('.').lowercase()
            if (!HANDLE.matches(handle)) continue
            // The matched span covers `@` + the *retained* handle only, so a
            // trimmed trailing dot is left outside the tappable span.
            val end = start + 1 + handle.length
            out += MentionSpan(start = start, end = end, handle = handle)
        }
        return out
    }

    /**
     * Detects an in-progress `@token` whose end sits exactly at [caret] in
     * [text] — the autocomplete trigger. Returns the [MentionQuery] (the token
     * range and the typed prefix, sans `@`) or null when the caret is not
     * inside a mention token.
     *
     * A token is "in progress" when, scanning left from the caret, an unbroken
     * run of handle characters is immediately preceded by an `@`, and that `@`
     * is itself at a word boundary (start of text or preceded by whitespace) —
     * so a stray `@` mid-word (an email `a@b`) does not trigger. The prefix may
     * be empty (caret right after a bare `@`), so the query fires the moment
     * the user types `@`.
     */
    fun activeMentionQuery(text: String, caret: Int): MentionQuery? {
        if (caret < 0 || caret > text.length) return null
        // Walk left from the caret over handle characters.
        var i = caret
        while (i > 0 && text[i - 1].isHandleChar()) i--
        // The char before the handle run must be `@`.
        if (i == 0 || text[i - 1] != '@') return null
        val atIndex = i - 1
        // The `@` must be at a word boundary — start of text or after whitespace.
        if (atIndex > 0 && !text[atIndex - 1].isWhitespace()) return null
        val prefix = text.substring(i, caret)
        // Reject a prefix containing a `@` (shouldn't happen — `@` is not a
        // handle char — but guards against future edits).
        return MentionQuery(
            atIndex = atIndex,
            caret = caret,
            prefix = prefix.lowercase(),
        )
    }

    /**
     * Replaces the in-progress mention token described by [query] in [text]
     * with `@handle ` (a trailing space, so the next word is not glued to the
     * mention and the autocomplete dismisses). Returns the new text together
     * with the caret position after the inserted token+space.
     */
    fun insertMention(text: String, query: MentionQuery, handle: String): MentionInsertion {
        val canonical = handle.removePrefix("@").lowercase()
        val replacement = "@$canonical "
        val before = text.substring(0, query.atIndex)
        val after = text.substring(query.caret)
        val newText = before + replacement + after
        val newCaret = before.length + replacement.length
        return MentionInsertion(text = newText, caret = newCaret)
    }

    private fun Char.isHandleChar(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '.' || this == '_'
}

/**
 * A `@handle` mention occurrence inside a body of text. [start] is the index
 * of the `@`; [end] is exclusive (one past the last handle char). [handle] is
 * the lowercased handle without the `@`.
 */
data class MentionSpan(
    val start: Int,
    val end: Int,
    val handle: String,
)

/**
 * An in-progress `@token` at the caret — the autocomplete query. [atIndex] is
 * the index of the `@`; [caret] is the caret position (one past the last typed
 * prefix char); [prefix] is the lowercased typed text after the `@` (may be
 * empty when the caret sits right after a bare `@`).
 */
data class MentionQuery(
    val atIndex: Int,
    val caret: Int,
    val prefix: String,
)

/** Result of [MentionText.insertMention] — the rewritten text and new caret. */
data class MentionInsertion(
    val text: String,
    val caret: Int,
)

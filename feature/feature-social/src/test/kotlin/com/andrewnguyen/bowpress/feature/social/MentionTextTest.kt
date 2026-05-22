package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionQuery
import com.andrewnguyen.bowpress.feature.social.ui.mentions.MentionText
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-JVM tests for [MentionText] — the in-progress `@token` detection,
 * token insertion, and mention-span extraction backing the mentions contract
 * §3 client.
 */
class MentionTextTest {

    // ── In-progress @token detection ─────────────────────────────────────────

    @Test
    fun `caret right after a bare @ yields an empty-prefix query`() {
        val text = "hey @"
        val q = MentionText.activeMentionQuery(text, caret = text.length)
        assertThat(q).isEqualTo(MentionQuery(atIndex = 4, caret = 5, prefix = ""))
    }

    @Test
    fun `caret inside a typed token yields the typed prefix`() {
        val text = "hey @sar"
        val q = MentionText.activeMentionQuery(text, caret = text.length)
        assertThat(q?.prefix).isEqualTo("sar")
        assertThat(q?.atIndex).isEqualTo(4)
    }

    @Test
    fun `prefix is lowercased`() {
        val text = "@SaRa"
        val q = MentionText.activeMentionQuery(text, caret = text.length)
        assertThat(q?.prefix).isEqualTo("sara")
    }

    @Test
    fun `a token at the very start of the text triggers`() {
        val q = MentionText.activeMentionQuery("@lina", caret = 5)
        assertThat(q?.prefix).isEqualTo("lina")
        assertThat(q?.atIndex).isEqualTo(0)
    }

    @Test
    fun `caret after a space following the token does not trigger`() {
        // The token ended — a space dismisses the autocomplete.
        val text = "@sara "
        val q = MentionText.activeMentionQuery(text, caret = text.length)
        assertThat(q).isNull()
    }

    @Test
    fun `an @ glued to a preceding word is not a mention trigger`() {
        // An email-style `a@b` — the `@` is mid-word, not at a boundary.
        val text = "mail me a@bc"
        val q = MentionText.activeMentionQuery(text, caret = text.length)
        assertThat(q).isNull()
    }

    @Test
    fun `caret in plain text with no @ yields no query`() {
        val q = MentionText.activeMentionQuery("just some text", caret = 5)
        assertThat(q).isNull()
    }

    @Test
    fun `caret in the middle of a token uses only the text up to the caret`() {
        // "@sara" with the caret after "sa" → prefix "sa", not "sara".
        val q = MentionText.activeMentionQuery("@sara", caret = 3)
        assertThat(q?.prefix).isEqualTo("sa")
    }

    // ── Token insertion ──────────────────────────────────────────────────────

    @Test
    fun `inserting a mention replaces the in-progress token with handle plus space`() {
        val text = "hey @sar"
        val q = MentionText.activeMentionQuery(text, caret = text.length)!!
        val result = MentionText.insertMention(text, q, "sara.lin")
        assertThat(result.text).isEqualTo("hey @sara.lin ")
        // Caret sits right after the inserted token + space.
        assertThat(result.caret).isEqualTo("hey @sara.lin ".length)
    }

    @Test
    fun `inserting a mention preserves text after the caret`() {
        val text = "@sar and others"
        // Caret after "@sar".
        val q = MentionText.activeMentionQuery(text, caret = 4)!!
        val result = MentionText.insertMention(text, q, "sara")
        assertThat(result.text).isEqualTo("@sara  and others")
        assertThat(result.caret).isEqualTo("@sara ".length)
    }

    @Test
    fun `inserting a mention from a bare @ inserts the full handle`() {
        val text = "shout out to @"
        val q = MentionText.activeMentionQuery(text, caret = text.length)!!
        val result = MentionText.insertMention(text, q, "lina.h")
        assertThat(result.text).isEqualTo("shout out to @lina.h ")
    }

    @Test
    fun `insertion lowercases and strips a leading @ on the picked handle`() {
        val text = "@s"
        val q = MentionText.activeMentionQuery(text, caret = 2)!!
        val result = MentionText.insertMention(text, q, "@Sara")
        assertThat(result.text).isEqualTo("@sara ")
    }

    // ── Mention-span extraction ──────────────────────────────────────────────

    @Test
    fun `extracts a single mention span`() {
        val spans = MentionText.mentionSpans("great shooting @sara.lin")
        assertThat(spans).hasSize(1)
        assertThat(spans[0].handle).isEqualTo("sara.lin")
        assertThat(spans[0].start).isEqualTo("great shooting ".length)
        assertThat(spans[0].end).isEqualTo("great shooting @sara.lin".length)
    }

    @Test
    fun `extracts multiple mention spans`() {
        val spans = MentionText.mentionSpans("@sara and @lina.h shot today")
        assertThat(spans.map { it.handle }).containsExactly("sara", "lina.h").inOrder()
    }

    @Test
    fun `a trailing dot is trimmed off the captured handle`() {
        // "@sara." — the contract trims the trailing dot; the span covers only
        // "@sara" and the dot is left as plain text.
        val text = "nice round @sara."
        val spans = MentionText.mentionSpans(text)
        assertThat(spans).hasSize(1)
        assertThat(spans[0].handle).isEqualTo("sara")
        assertThat(text.substring(spans[0].start, spans[0].end)).isEqualTo("@sara")
    }

    @Test
    fun `a too-short token is not a mention`() {
        // "@ab" — 2 chars, below the 3-char handle minimum.
        assertThat(MentionText.mentionSpans("@ab hi")).isEmpty()
    }

    @Test
    fun `handle matching is case-insensitive and the span handle is lowercased`() {
        val spans = MentionText.mentionSpans("welcome @Sara.Lin")
        assertThat(spans).hasSize(1)
        assertThat(spans[0].handle).isEqualTo("sara.lin")
    }

    @Test
    fun `plain text with no mentions yields no spans`() {
        assertThat(MentionText.mentionSpans("no mentions here at all")).isEmpty()
    }

    @Test
    fun `a leading-dot token is rejected`() {
        // ".foo" is not a valid handle (no leading dot).
        assertThat(MentionText.mentionSpans("@.foo bar")).isEmpty()
    }

    @Test
    fun `an over-long token captures only the 30-char handle ceiling`() {
        // The contract token regex caps the capture at 30 chars — the same
        // bound the server applies. 35 a's → a 30-char handle span; the
        // trailing 5 a's stay plain text.
        val spans = MentionText.mentionSpans("@" + "a".repeat(35))
        assertThat(spans).hasSize(1)
        assertThat(spans[0].handle).hasLength(30)
    }

    @Test
    fun `an email address renders no mention span`() {
        // `@bowpress` here follows a handle char (`w`), not a word boundary —
        // the `@` is mid-word, so it is not a mention. Mirrors the server.
        assertThat(MentionText.mentionSpans("write me at andrew@bowpress.app")).isEmpty()
    }

    @Test
    fun `a mid-word at-sign is not a mention`() {
        // `foo@bar` — the `@` is preceded by `o`, not whitespace.
        assertThat(MentionText.mentionSpans("foo@bar baz")).isEmpty()
    }

    @Test
    fun `two glued mentions resolve only the first`() {
        // `@sara@marcus` — the second `@` follows a handle char, not a word
        // boundary, so only `@sara` is a mention. Intentional, matching the
        // server's authoritative parse.
        val spans = MentionText.mentionSpans("@sara@marcus tied")
        assertThat(spans.map { it.handle }).containsExactly("sara")
    }
}

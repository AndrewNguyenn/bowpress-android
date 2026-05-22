package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.feature.social.ui.mentions.MENTION_ANNOTATION_TAG
import com.andrewnguyen.bowpress.feature.social.ui.mentions.mentionAnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for [mentionAnnotatedString] — the `AnnotatedString` builder behind
 * the tappable `@handle` mention spans (mentions contract §3.2).
 */
class MentionRenderingTest {

    @Test
    fun `plain text yields an annotated string with no mention annotations`() {
        val annotated = mentionAnnotatedString("just a normal session name")
        assertThat(annotated.text).isEqualTo("just a normal session name")
        assertThat(
            annotated.getStringAnnotations(MENTION_ANNOTATION_TAG, 0, annotated.text.length),
        ).isEmpty()
    }

    @Test
    fun `a mention carries a tag annotation holding the bare handle`() {
        val text = "great round @sara.lin"
        val annotated = mentionAnnotatedString(text)
        // The visible text is unchanged — only spans/annotations are added.
        assertThat(annotated.text).isEqualTo(text)
        val annotations =
            annotated.getStringAnnotations(MENTION_ANNOTATION_TAG, 0, text.length)
        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].item).isEqualTo("sara.lin")
    }

    @Test
    fun `the annotation spans exactly the at-plus-handle range`() {
        val text = "hi @lina there"
        val annotated = mentionAnnotatedString(text)
        val a = annotated.getStringAnnotations(MENTION_ANNOTATION_TAG, 0, text.length).single()
        assertThat(a.start).isEqualTo(3)            // index of '@'
        assertThat(a.end).isEqualTo("hi @lina".length)
        assertThat(text.substring(a.start, a.end)).isEqualTo("@lina")
    }

    @Test
    fun `multiple mentions each get their own annotation`() {
        val annotated = mentionAnnotatedString("@sara and @lina.h tied")
        val annotations =
            annotated.getStringAnnotations(MENTION_ANNOTATION_TAG, 0, annotated.text.length)
        assertThat(annotations.map { it.item }).containsExactly("sara", "lina.h")
    }

    @Test
    fun `an offset tap inside a mention resolves to that handle`() {
        // Simulates ClickableText.onClick: the offset of a character inside
        // "@sara" must resolve to the `sara` annotation.
        val text = "ping @sara now"
        val annotated = mentionAnnotatedString(text)
        val insideMention = text.indexOf("sara") + 1
        val hit = annotated
            .getStringAnnotations(MENTION_ANNOTATION_TAG, insideMention, insideMention)
            .firstOrNull()
        assertThat(hit?.item).isEqualTo("sara")
    }

    @Test
    fun `an offset tap in plain text resolves to no mention`() {
        val text = "ping @sara now"
        val annotated = mentionAnnotatedString(text)
        val inPlainText = text.indexOf("now") + 1
        val hit = annotated
            .getStringAnnotations(MENTION_ANNOTATION_TAG, inPlainText, inPlainText)
            .firstOrNull()
        assertThat(hit).isNull()
    }

    @Test
    fun `a trailing dot stays outside the tappable mention span`() {
        val text = "nice @sara."
        val annotated = mentionAnnotatedString(text)
        val a = annotated.getStringAnnotations(MENTION_ANNOTATION_TAG, 0, text.length).single()
        assertThat(a.item).isEqualTo("sara")
        // The dot is at the end and is not covered by the annotation.
        assertThat(a.end).isEqualTo(text.length - 1)
    }
}

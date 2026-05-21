package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedHeadline
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Social Feed V2 §1/§2 — feed-row headline formatting: custom session-name
 * titles render as a quoted caption, the actor stays visible, and the caller's
 * own rows read "YOU".
 */
class FeedHeadlineTest {

    private fun item(
        actorDisplayName: String = "Marcus Tan",
        actorHandle: String = "marcus.t",
        title: String = "logged a session",
        titleIsCustom: Boolean = false,
        isOwn: Boolean = false,
    ) = ActivityItem(
        id = "act-1",
        kind = ActivityKind.friend_session,
        sourceKind = ActivitySourceKind.friend,
        actorHandle = actorHandle,
        actorDisplayName = actorDisplayName,
        title = title,
        createdAt = Instant.now(),
        titleIsCustom = titleIsCustom,
        isOwn = isOwn,
    )

    @Test
    fun `a generic-title row renders the title verbatim`() {
        val it = item(title = "shot a new PR", titleIsCustom = false)
        assertThat(FeedHeadline.headline(it)).isEqualTo("shot a new PR")
    }

    @Test
    fun `a custom-title row renders the session name as a quoted caption`() {
        val it = item(title = "Saturday 70m practice", titleIsCustom = true)
        assertThat(FeedHeadline.headline(it)).isEqualTo("“Saturday 70m practice”")
    }

    @Test
    fun `the actor eyebrow shows the actor name on a friend row`() {
        val it = item(actorDisplayName = "Marcus Tan", isOwn = false)
        assertThat(FeedHeadline.actorEyebrow(it)).isEqualTo("MARCUS TAN")
    }

    @Test
    fun `an own row's eyebrow reads YOU`() {
        val it = item(isOwn = true)
        assertThat(FeedHeadline.actorEyebrow(it)).isEqualTo("YOU")
    }

    @Test
    fun `a custom-title row keeps the actor handle visible in the eyebrow`() {
        // §1 — the headline below is the archer's caption, so the eyebrow must
        // still identify the actor by name + handle.
        val it = item(
            actorDisplayName = "Marcus Tan",
            actorHandle = "marcus.t",
            titleIsCustom = true,
        )
        assertThat(FeedHeadline.actorEyebrow(it)).isEqualTo("MARCUS TAN · @marcus.t")
    }

    @Test
    fun `an own custom-title row reads YOU plus the handle`() {
        val it = item(actorHandle = "andrew.n", titleIsCustom = true, isOwn = true)
        assertThat(FeedHeadline.actorEyebrow(it)).isEqualTo("YOU · @andrew.n")
    }
}

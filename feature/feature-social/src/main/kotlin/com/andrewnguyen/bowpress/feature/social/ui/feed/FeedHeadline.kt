package com.andrewnguyen.bowpress.feature.social.ui.feed

import com.andrewnguyen.bowpress.core.model.ActivityItem

/**
 * Pure feed-row headline formatting (Social Feed V2 §1, §2). Extracted from
 * `FeedScreen` so the custom-title / own-row rules are unit-testable without a
 * Compose harness — the screen and the tests share these functions.
 */
object FeedHeadline {

    /**
     * The actor eyebrow line above the headline.
     *  - An `isOwn` row reads "YOU" — the caller's own activity is now
     *    interleaved into the feed, so their posts must be self-evident.
     *  - A `titleIsCustom` row appends " · @handle": the headline below is the
     *    archer's caption, not an actor-verb phrase, so it does not itself
     *    identify the actor.
     */
    fun actorEyebrow(item: ActivityItem): String {
        val actor = if (item.isOwn) "YOU" else item.actorDisplayName.uppercase()
        return if (item.titleIsCustom) "$actor · @${item.actorHandle}" else actor
    }

    /**
     * The headline body. §1 — when `titleIsCustom` the title IS the archer's
     * own session name, so it is rendered as a quoted caption set apart from
     * the actor verb. A generic verb-phrase title renders verbatim.
     */
    fun headline(item: ActivityItem): String =
        if (item.titleIsCustom) "“${item.title}”" else item.title
}

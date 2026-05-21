package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.model.ActivityActor
import com.andrewnguyen.bowpress.feature.social.ui.feed.optimisticLikers
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Social Feed V2 §6.7 / M4 — covers [optimisticLikers], the pure helper that
 * keeps the kudos avatar stack in sync with an optimistic like toggle.
 */
class KudosRowTest {

    private fun actor(id: String) = ActivityActor(
        userId = id,
        handle = "$id.h",
        displayName = "Actor $id",
    )

    private val marcus = actor("u-1")
    private val jamie = actor("u-2")
    private val me = actor("me-id")

    @Test
    fun `no like change returns the server liker list unchanged`() {
        val server = listOf(marcus, jamie)
        val result = optimisticLikers(
            serverLikers = server,
            serverLikedByMe = false,
            likedNow = false,
            selfActor = me,
        )
        assertThat(result).isEqualTo(server)
    }

    @Test
    fun `an optimistic self-like prepends the caller's avatar to the stack`() {
        // The server snapshot does not yet include the caller.
        val result = optimisticLikers(
            serverLikers = listOf(marcus, jamie),
            serverLikedByMe = false,
            likedNow = true,
            selfActor = me,
        )
        // The caller is the most-recent liker — first in the stack.
        assertThat(result).containsExactly(me, marcus, jamie).inOrder()
    }

    @Test
    fun `a self-like on an empty post shows the caller's avatar, not Be the first`() {
        val result = optimisticLikers(
            serverLikers = emptyList(),
            serverLikedByMe = false,
            likedNow = true,
            selfActor = me,
        )
        // M4 — a first-like surfaces the caller's own avatar.
        assertThat(result).containsExactly(me)
    }

    @Test
    fun `an optimistic unlike removes the caller's avatar from the stack`() {
        // The server snapshot still has the caller liking.
        val result = optimisticLikers(
            serverLikers = listOf(me, marcus, jamie),
            serverLikedByMe = true,
            likedNow = false,
            selfActor = me,
        )
        assertThat(result).containsExactly(marcus, jamie).inOrder()
    }

    @Test
    fun `a self-like never duplicates the caller already in the server list`() {
        // A stale server snapshot already includes the caller — splicing must
        // not produce two of them.
        val result = optimisticLikers(
            serverLikers = listOf(me, marcus),
            serverLikedByMe = false,
            likedNow = true,
            selfActor = me,
        )
        assertThat(result).containsExactly(me, marcus).inOrder()
        assertThat(result.count { it.userId == "me-id" }).isEqualTo(1)
    }

    @Test
    fun `with no self actor known the server list is returned as a fallback`() {
        val server = listOf(marcus, jamie)
        val result = optimisticLikers(
            serverLikers = server,
            serverLikedByMe = false,
            likedNow = true,
            selfActor = null,
        )
        // No self avatar to splice — the count-only fallback handles the rest.
        assertThat(result).isEqualTo(server)
    }
}

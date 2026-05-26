package com.andrewnguyen.bowpress.core.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import com.andrewnguyen.bowpress.core.data.seed.DevMockData
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * F3 — notification mutations write through to the DEBUG mock list so the
 * offline-fallback path through `getPendingCount()` reflects the user's
 * action; without this, the tab badge stuck at the seed unread count even
 * after the notification screen rendered everything as read.
 *
 * Mirrors iOS commit 46cce3e (DevSocialMockStore mutators).
 */
class SocialRepositoryNotificationMutationTest {

    private lateinit var api: BowPressApi
    private lateinit var repo: SocialRepository

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        // Mark the context DEBUG so the repository's `isDebugBuild` branch
        // executes — the mock-mutation path is DEBUG-only by design.
        val appInfo = ApplicationInfo().also { it.flags = ApplicationInfo.FLAG_DEBUGGABLE }
        val debugContext = mockk<Context> {
            every { applicationInfo } returns appInfo
        }
        repo = SocialRepository(
            api,
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), // photoCache
            debugContext,
        )
        // Reset the shared mock list so each test starts from the seed.
        DevMockData.resetNotificationsForTests()
    }

    @After
    fun tearDown() {
        DevMockData.resetNotificationsForTests()
    }

    @Test
    fun `markAllNotificationsRead flips DevMockData unread to zero`() = runTest {
        val seedUnread = DevMockData.notificationList.unread
        assertThat(seedUnread).isGreaterThan(0)

        repo.markAllNotificationsRead()

        assertThat(DevMockData.notificationList.unread).isEqualTo(0)
        coVerify { api.markAllNotificationsRead() }
    }

    @Test
    fun `markNotificationRead flips a single mock notification`() = runTest {
        val firstUnreadId = DevMockData.notifications.first { !it.read }.id
        val seedUnread = DevMockData.notificationList.unread

        repo.markNotificationRead(firstUnreadId)

        assertThat(DevMockData.notificationList.unread).isEqualTo(seedUnread - 1)
        coVerify { api.markNotificationRead(firstUnreadId) }
    }

    @Test
    fun `dismissNotification removes the row from the mock list`() = runTest {
        val seedSize = DevMockData.notifications.size
        val target = DevMockData.notifications.first().id

        repo.dismissNotification(target)

        assertThat(DevMockData.notifications).hasSize(seedSize - 1)
        assertThat(DevMockData.notifications.none { it.id == target }).isTrue()
        coVerify { api.dismissNotification(target) }
    }

    @Test
    fun `dismissAllNotifications clears the mock list`() = runTest {
        assertThat(DevMockData.notifications).isNotEmpty()

        repo.dismissAllNotifications()

        assertThat(DevMockData.notifications).isEmpty()
        assertThat(DevMockData.notificationList.unread).isEqualTo(0)
        coVerify { api.dismissAllNotifications() }
    }

    @Test
    fun `getPendingCount fallback reflects post-mutation mock unread when api fails`() = runTest {
        // Force the API path to fail so getPendingCount falls back to the
        // DEBUG mock branch (`DevMockData.notificationList.unread`).
        coEvery { api.getPendingCount() } throws RuntimeException("offline")

        val before = repo.getPendingCount().notifications
        assertThat(before).isGreaterThan(0)

        repo.markAllNotificationsRead()

        val after = repo.getPendingCount().notifications
        assertThat(after).isEqualTo(0)
    }
}

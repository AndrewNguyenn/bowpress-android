package com.andrewnguyen.bowpress.core.data.push

import com.andrewnguyen.bowpress.core.model.DeviceToken
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.RegisterDeviceTokenRequest
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class DeviceTokenRegistrarTest {

    @Test
    fun `register posts once per unique token`() = runTest {
        val api = mockk<BowPressApi>(relaxed = true)
        val captured = slot<RegisterDeviceTokenRequest>()
        coEvery { api.registerDeviceToken(capture(captured)) } returns fakeDeviceToken()

        val registrar = DeviceTokenRegistrar(api).apply {
            environmentOverride = "development"
        }

        registrar.register("tok_1")
        registrar.register("tok_1")
        registrar.register("tok_1")

        coVerify(exactly = 1) { api.registerDeviceToken(any()) }
        assertThat(captured.captured.token).isEqualTo("tok_1")
        assertThat(captured.captured.environment).isEqualTo("development")
    }

    @Test
    fun `new token triggers a new POST`() = runTest {
        val api = mockk<BowPressApi>(relaxed = true)
        coEvery { api.registerDeviceToken(any()) } returns fakeDeviceToken()

        val registrar = DeviceTokenRegistrar(api).apply {
            environmentOverride = "production"
        }
        registrar.register("tok_old")
        registrar.register("tok_new")

        coVerify(exactly = 2) { api.registerDeviceToken(any()) }
    }

    @Test
    fun `blank token is ignored`() = runTest {
        val api = mockk<BowPressApi>(relaxed = true)
        val registrar = DeviceTokenRegistrar(api)
        registrar.register("")
        registrar.register("   ")
        coVerify(exactly = 0) { api.registerDeviceToken(any()) }
    }

    @Test
    fun `network failure does not poison dedupe cache`() = runTest {
        val api = mockk<BowPressApi>(relaxed = true)
        // First attempt fails; second attempt should retry.
        coEvery { api.registerDeviceToken(any()) } throws RuntimeException("offline") andThen fakeDeviceToken()

        val registrar = DeviceTokenRegistrar(api)
        registrar.register("tok_x")
        registrar.register("tok_x")

        coVerify(exactly = 2) { api.registerDeviceToken(any()) }
    }

    @Test
    fun `reset clears dedupe cache so same token re-posts`() = runTest {
        val api = mockk<BowPressApi>(relaxed = true)
        coEvery { api.registerDeviceToken(any()) } returns fakeDeviceToken()

        val registrar = DeviceTokenRegistrar(api)
        registrar.register("tok_a")
        registrar.reset()
        registrar.register("tok_a")

        coVerify(exactly = 2) { api.registerDeviceToken(any()) }
    }

    private fun fakeDeviceToken(): DeviceToken = DeviceToken(
        id = "d1",
        userId = "u1",
        token = "tok_1",
        environment = "development",
        createdAt = Instant.EPOCH,
        lastSeenAt = Instant.EPOCH,
    )
}

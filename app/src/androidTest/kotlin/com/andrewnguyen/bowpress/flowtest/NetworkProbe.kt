package com.andrewnguyen.bowpress.flowtest

import kotlinx.serialization.json.JsonElement

/**
 * Test-only HTTP recorder + responder. The runner calls into one of these
 * for `apiExpect` and `apiRespond` steps. Implementations bind to the
 * concrete network library in use:
 *
 * - `MockWebServerProbe` (planned) — wraps OkHttp's MockWebServer for
 *   Retrofit-based modes
 * - A custom `OkHttp.Interceptor` recorder for in-process flows where
 *   spinning up a server is overkill
 *
 * The probe contract is intentionally narrow so the runner stays library-
 * agnostic; concrete probe choice is per-test-class.
 */
interface NetworkProbe {

    /**
     * Stub the next call matching [Step.ApiRespond.method] + [Step.ApiRespond.path].
     * The probe is responsible for resolving `bodyFile` against the test
     * APK's asset directory if `body` is null and `bodyFile` is set.
     */
    fun respond(step: Step.ApiRespond)

    /**
     * Assert that an outgoing request matching [method] + [path] (and
     * optionally [bodyContains]) has been recorded since the last
     * [reset]. Throws AssertionError on miss.
     */
    fun expect(method: HttpMethod, path: String, bodyContains: JsonElement?)

    /** Drop any recorded requests / pending responses. */
    fun reset()
}

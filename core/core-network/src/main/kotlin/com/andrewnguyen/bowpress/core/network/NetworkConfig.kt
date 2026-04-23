package com.andrewnguyen.bowpress.core.network

/**
 * Holds the base URL used by the Retrofit client. Values come from [NetworkModule]
 * which reads `BuildConfig.DEBUG` (`10.0.2.2` loopback for the Android emulator →
 * host's `localhost:8787`) or the production Workers URL. Kept as a data class so
 * tests can inject an arbitrary value.
 */
data class NetworkConfig(
    val baseUrl: String,
    val debug: Boolean,
) {
    companion object {
        const val DEBUG_BASE_URL = "http://10.0.2.2:8787/"
        const val RELEASE_BASE_URL = "https://bowpress-api.stageandrewnguyen.workers.dev/"
    }
}

plugins {
    alias(libs.plugins.bowpress.android.library)
    alias(libs.plugins.bowpress.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.andrewnguyen.bowpress.core.data"
}

dependencies {
    api(project(":core:core-model"))
    api(project(":core:core-network"))
    api(project(":core:core-database"))
    api(project(":core:core-analytics"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    // Social Feed V2 — building a raw-JPEG RequestBody for the photo upload
    // endpoint. Retrofit exposes okhttp transitively; declared explicitly
    // because SocialRepository references okhttp3 types directly.
    implementation(libs.okhttp)

    // WorkManager + Hilt worker support for BackgroundSyncService
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

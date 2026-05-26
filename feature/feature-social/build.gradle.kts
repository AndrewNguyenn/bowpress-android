plugins {
    alias(libs.plugins.bowpress.android.library)
    alias(libs.plugins.bowpress.android.library.compose)
    alias(libs.plugins.bowpress.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.andrewnguyen.bowpress.feature.social"

    // BuildConfig.DEBUG gates the FeedViewModel hero-carousel preview
    // fixture so release builds don't leak hardcoded fixtures into every
    // user's Feed — same hazard guard as :feature:feature-analytics.
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-navigation"))

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    // §18 location tagging — fused location for "use current location".
    implementation(libs.play.services.location)
    // Parity E5 — profile picture across feed / kudos / comments / detail.
    // Coil AsyncImage with avatarUrl + ?v=avatarVersion cache buster.
    implementation(libs.coil.compose)

    // D1 — uCrop wraps the photo-crop step before avatar / shared-session
    // uploads. PhotoCropperSheet (in this module) bridges its Activity-based
    // API to Compose, exposing a `.free` (free-aspect) and `.square` mode
    // mirroring iOS's Mantis configuration. Mantis = uCrop on Android.
    implementation(libs.ucrop)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

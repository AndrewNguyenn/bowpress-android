plugins {
    alias(libs.plugins.bowpress.android.library)
    alias(libs.plugins.bowpress.android.library.compose)
    alias(libs.plugins.bowpress.android.hilt)
}

android {
    namespace = "com.andrewnguyen.bowpress.feature.session"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-navigation"))
    // §18 — the end-session sheet opens feature-social's LocationTagPicker so
    // a session can be tagged with a place before it is shared to the feed.
    implementation(project(":feature:feature-social"))

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    // C3 — NearestRangeFinder uses FusedLocationProviderClient for the
    // one-shot fix + the platform Geocoder for the reverse-geocode fallback.
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // CameraX — the live viewfinder for both the 3D capture screen and the
    // finish-sheet media picker's Camera tab (photo capture + video record).
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // Video CameraX use case for the picker's Video tab.
    implementation(libs.androidx.camera.video)

    // uCrop — finish-sheet photo crop step (4:5 portrait, mirrors iOS YP's
    // sessionMedia crop ratio). The cropper Compose bridge lives in
    // feature-social as `PhotoCropperSheet`, which is re-exported via the
    // existing inter-module dep above.
    implementation(libs.ucrop)

    // Media3 — ExoPlayer drives the trim sheet's preview; Transformer runs
    // the trim export. Effect + UI artifacts come in for player chrome +
    // composition needs.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

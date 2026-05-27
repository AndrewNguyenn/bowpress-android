plugins {
    alias(libs.plugins.bowpress.android.library)
    alias(libs.plugins.bowpress.android.library.compose)
    alias(libs.plugins.bowpress.android.hilt)
}

android {
    namespace = "com.andrewnguyen.bowpress.feature.settings"
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-navigation"))

    // Settings observes the entitlement StateFlow from PlayBillingManager so
    // the Subscription row renders Pro Monthly / Pro Annual / Free correctly.
    // Acceptable coupling — both modules already live under feature/ and
    // share the Entitlement core-model.
    implementation(project(":feature:feature-subscription"))

    // Edit Profile reuses the avatar tile (SocialAvatarImage) and the uCrop
    // bridge (PhotoCropperHost) from feature-social — both already exist for
    // the feed / session photo flows. iOS uses the same building blocks from
    // its Components folder.
    implementation(project(":feature:feature-social"))

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

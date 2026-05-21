plugins {
    alias(libs.plugins.bowpress.android.library)
    alias(libs.plugins.bowpress.android.library.compose)
    alias(libs.plugins.bowpress.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.andrewnguyen.bowpress.feature.social"
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

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

plugins {
    alias(libs.plugins.bowpress.android.library)
    alias(libs.plugins.bowpress.android.library.compose)
}

android {
    namespace = "com.andrewnguyen.bowpress.core.designsystem"
}

dependencies {
    api(project(":core:core-model"))

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.lottie.compose)
}

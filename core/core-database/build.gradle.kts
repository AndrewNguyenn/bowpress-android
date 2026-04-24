plugins {
    alias(libs.plugins.bowpress.android.library)
    alias(libs.plugins.bowpress.android.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.andrewnguyen.bowpress.core.database"
    // Expose the Room schema JSONs to instrumented tests so MigrationTestHelper can find them.
    sourceSets["androidTest"].assets.srcDirs("$projectDir/schemas")
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

// Room schema export — writes `core-database/schemas/<db-version>.json` on each build.
// Future migrations verify against these.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:core-model"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.room.testing)
}

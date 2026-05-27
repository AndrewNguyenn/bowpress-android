import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.bowpress.android.application)
    alias(libs.plugins.bowpress.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.andrewnguyen.bowpress"

    defaultConfig {
        applicationId = "com.andrewnguyen.bowpress"
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing — credentials are sourced from local.properties (gitignored)
    // so the keystore file path + passwords never enter version control. When
    // RELEASE_KEYSTORE_FILE is missing (CI without secrets, fresh checkouts),
    // the signing config silently falls back to unsigned and release assembly
    // still succeeds for smoke-testing the build pipeline.
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) FileInputStream(f).use { load(it) }
    }
    val releaseKeystorePath = localProps.getProperty("RELEASE_KEYSTORE_FILE")
    val hasReleaseSigning = releaseKeystorePath != null && file(releaseKeystorePath).exists()

    signingConfigs {
        if (releaseKeystorePath != null && hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = localProps.getProperty("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        // The flow-spec runner ships flow JSON files as test assets. The
        // source of truth lives at repo root in `flows/`; we copy them into
        // a build-dir location and register that as an androidTest asset
        // srcDir. See `copyFlowsToTestAssets` task below.
        //
        // The kotlin/ srcDir is added explicitly because the Kotlin Android
        // plugin only auto-registers it for library modules, not application
        // modules — without this AGP silently skips compileDebugAndroidTestKotlin
        // (NO-SOURCE) and the flow-spec tests don't ship in the test APK.
        named("androidTest") {
            assets.srcDir(layout.buildDirectory.dir("generated/flowAssets"))
            java.srcDirs("src/androidTest/kotlin")
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)

    // WorkManager — Application-level Configuration.Provider wiring
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.navigation.compose)

    // Firebase (FCM) — google-services.json drives the BoM config at build time.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // core modules providing DeviceTokenRegistrar + nav graphs
    implementation(project(":core:core-data"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-navigation"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":feature:feature-auth"))
    implementation(project(":feature:feature-equipment"))
    implementation(project(":feature:feature-session"))
    implementation(project(":feature:feature-analytics"))
    implementation(project(":feature:feature-subscription"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-social"))

    // Coil ImageLoader override — registered on BowPressApplication so avatar
    // requests carry the Bearer token via the existing AuthInterceptor.
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Flow-spec test runner (parses flows/*.flow.json + drives Compose UI Test).
    androidTestImplementation(libs.kotlinx.serialization.json)
}

// Copy the repo-root `flows/` directory (the cross-platform behavioral
// spec source of truth) into a build-dir location that's wired as an
// androidTest asset srcDir above. After this task runs, the test APK
// contains `flows/*.flow.json` + `flows/fixtures/*.json` at the asset
// root, addressable via `context.assets.open("flows/<name>.flow.json")`.
val copyFlowsToTestAssets by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.dir("flows"))
    into(layout.buildDirectory.dir("generated/flowAssets/flows"))
    include("**/*.json")
}

afterEvaluate {
    listOf("mergeDebugAndroidTestAssets", "mergeReleaseAndroidTestAssets").forEach { taskName ->
        tasks.findByName(taskName)?.dependsOn(copyFlowsToTestAssets)
    }
}

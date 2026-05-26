pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // uCrop (com.github.yalantis:ucrop) is published on JitPack. Used by
        // PhotoCropperSheet (parity gap D1) — the in-app crop step before a
        // session/avatar photo upload, mirroring iOS's Mantis integration.
        maven("https://jitpack.io")
    }
}

rootProject.name = "bowpress-android"

include(":app")

include(":core:core-model")
include(":core:core-network")
include(":core:core-database")
include(":core:core-data")
include(":core:core-analytics")
include(":core:core-designsystem")
include(":core:core-navigation")

include(":feature:feature-auth")
include(":feature:feature-equipment")
include(":feature:feature-session")
include(":feature:feature-analytics")
include(":feature:feature-settings")
include(":feature:feature-subscription")
include(":feature:feature-social")

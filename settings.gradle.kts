pluginManagement {
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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "LifeOS"
include(":app")
include(":sms")
// :baselineprofile — Baseline Profile generator module.
// To generate: temporarily uncomment, apply AGP classpath plugin manually,
// then run `./gradlew :baselineprofile:generateBaselineProfile`.
// include(":baselineprofile")

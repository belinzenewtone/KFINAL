// Top-level build file. Configuration common to all subprojects goes here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library)     apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.hilt)               apply false
    alias(libs.plugins.ksp)                apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint) apply false
}

// Static analysis (Phase 4): detekt gates CI with maxIssues: 0.
// Per-module config lives in each module's build.gradle.kts; shared YAML in
// config/detekt/detekt.yml.

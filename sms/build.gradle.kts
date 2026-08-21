/**
 * :sms — Android library module containing the SMS parser.
 *
 * All parser sources live in-repo under src/main/java (vendored from
 * RFINAL/modules/lifeos-sms; see PHASE0_DECISIONS.md D1/D2). This module is
 * fully self-contained and must build without any sibling repo present.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace  = "com.lifeos.sms"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("robolectric.logging.enabled", "true")
    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {
    // Room — DbWriter.kt uses SupportSQLiteDatabase, resolved by the app module's Room instance
    implementation(libs.room.runtime)

    // WorkManager — SmsImportWorker, SmsProcessWorker, IngestSweepWorker
    implementation(libs.workmanager)

    // Coroutines
    implementation(libs.coroutines.android)

    // Core Android
    implementation(libs.core.ktx)

    // Hilt — SmsService.kt uses @Inject / @Singleton
    implementation(libs.hilt.android)

    // Unit tests (ported from RFINAL modules/lifeos-sms — see Phase 2)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.work.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
}

// Static analysis (Phase 4) — shared YAML, strict gate
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

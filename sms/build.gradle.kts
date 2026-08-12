/**
 * :sms — Android library module wrapping the existing Kotlin SMS parser.
 *
 * Source files are referenced IN-PLACE from modules/lifeos-sms/android/src/main/java/
 * so the parser files are NEVER copied — edits to the originals are reflected here
 * automatically. Only SmsReceiverModule.kt (the Expo bridge) is excluded; it is
 * replaced by SmsService.kt in this module's own source set.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.lifeos.sms"
    compileSdk = 37

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

    sourceSets {
        named("main") {
            java.srcDirs(
                // Parser source files — reference in-place; never copied
                "../../modules/lifeos-sms/android/src/main/java",
                // SmsService.kt (our bridge replacement) lives here
                "src/main/java"
            )
            java.excludes.add("**/SmsReceiverModule.kt")

            manifest.srcFile("src/main/AndroidManifest.xml")
        }
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
}

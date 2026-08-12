/**
 * :sms — Android library module wrapping the existing Kotlin SMS parser.
 *
 * Parser sources are referenced IN-PLACE from modules/lifeos-sms and staged into
 * a build directory at configuration time, so edits to the originals are
 * reflected here automatically. Only SmsReceiverModule.kt (the Expo bridge) is
 * excluded; it is replaced by SmsService.kt in this module's own source set.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

/**
 * Stage the SMS parser sources from modules/lifeos-sms into a build directory,
 * excluding the Expo bridge stub (SmsReceiverModule.kt) which is replaced by
 * SmsService.kt in this module's own source set.
 */
val parserStagingDir = layout.buildDirectory.dir("generated/sms-parser")

val stageParserSources by tasks.registering(Copy::class) {
    from("../../RFINAL/modules/lifeos-sms/android/src/main/java")
    exclude("**/SmsReceiverModule.kt")
    into(parserStagingDir)
}
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(stageParserSources)
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

    sourceSets {
        named("main") {
            java.srcDir(parserStagingDir)
            java.srcDir("src/main/java")

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

    // Hilt — SmsService.kt uses @Inject / @Singleton
    implementation(libs.hilt.android)
}

import java.util.Properties

// ─────────────────────────────────────────────────────────────────────────────
// Phase 3 CMP: converted from kotlin("android") to kotlin("multiplatform").
// Source files stay in src/main/java/ (androidMain) until Phase 5.
// ─────────────────────────────────────────────────────────────────────────────
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)   // JetBrains CMP Gradle plugin
    alias(libs.plugins.kotlin.compose)           // Compose compiler plugin (all targets)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Release signing (Phase 5): credentials live in keystore.properties (git-ignored).
// CI provides the same file from secrets — see .github/workflows/release.yml.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Kotlin Multiplatform targets + source sets
// ─────────────────────────────────────────────────────────────────────────────
kotlin {
    // ── Android ──────────────────────────────────────────────────────────────
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs +=
                    listOf(
                        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                        "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
                        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                        "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    )
            }
        }
    }

    // ── iOS ───────────────────────────────────────────────────────────────────
    // Requires macOS + Xcode to compile; declared here for CMP structure.
    // Sources live in src/iosMain/kotlin/ (Phase 5+).
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // ── Desktop (JVM) ─────────────────────────────────────────────────────────
    // Sources live in src/desktopMain/kotlin/ (Phase 5+).
    jvm("desktop")

    // ── Source sets ───────────────────────────────────────────────────────────
    sourceSets {
        // Common — shared across ALL targets
        commonMain.dependencies {
            // Compose Multiplatform runtime (JetBrains CMP — maps to AndroidX on Android)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(compose.components.uiToolingPreview)

            // CMP-compatible date/time (replaces java.time.*)
            implementation(libs.kotlinx.datetime)

            // Stable Compose params — no @Stable annotation needed
            implementation(libs.kotlinx.collections.immutable)

            // Koin DI — koin-core is a KMP artifact; versions pinned in version catalog
            implementation(libs.koin.core)

            // Coroutines core (KMP artifact); android adds the Android dispatcher
            implementation(libs.coroutines.core)

            // SQLDelight — KMP database layer (replaces Room, Phase 4)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.runtime)
        }

        // Phase 5: sources moved to src/androidMain/kotlin and src/commonMain/kotlin.
        // No srcDirs override needed — KMP default layout applies.

        // Android — platform-specific sources
        androidMain.dependencies {
            // AndroidX core
            implementation(libs.core.ktx)
            implementation(libs.splashscreen)
            implementation(libs.activity.compose)

            // Extended material icons (CMP accessor — no separate version needed)
            implementation(compose.materialIconsExtended)

            // Navigation (Jetpack — Android-specific; CMP navigation TBD Phase 5+)
            implementation(libs.navigation.compose)

            // Koin Android extensions (androidContext(), viewModel DSL)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // SQLDelight Android driver (Phase 4 — Room removed)
            implementation(libs.sqldelight.android.driver)

            // SQLite framework — provides FrameworkSQLiteOpenHelperFactory for DatabaseDriverFactory
            implementation(libs.androidx.sqlite.framework)

            // DataStore
            implementation(libs.datastore)

            // Lifecycle / ViewModel
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.ktx)
            implementation(libs.lifecycle.runtime.ktx)

            // Coroutines Android dispatcher
            implementation(libs.coroutines.android)

            // WorkManager
            implementation(libs.workmanager)

            // Biometrics
            implementation(libs.biometric)

            // Baseline Profile — ART AOT compilation for cold-start improvement
            implementation(libs.profileinstaller)

            // SMS module (parser — source included, zero parser changes)
            implementation(project(":sms"))
        }

        // iOS — empty until Phase 5; hierarchy auto-wired by KMP default targets
        // iosMain source set is auto-created for iosX64 + iosArm64 + iosSimulatorArm64

        // Desktop — empty until Phase 5
        // desktopMain source set is auto-created for jvm("desktop")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Android application configuration (unchanged from pre-CMP)
// ─────────────────────────────────────────────────────────────────────────────
android {
    namespace    = "com.belinze.lifeos"
    compileSdk   = 35

    defaultConfig {
        applicationId = "com.belinze.lifeos.compose"
        minSdk = 26
        targetSdk = 35
        versionCode = 22
        versionName = "1.5.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField(
            "String",
            "OTA_MANIFEST_URL",
            "\"https://raw.githubusercontent.com/belinzenewtone/KFINAL/master/ota/manifest.json\"",
        )
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (keystorePropsFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                null // unsigned local builds; CI always signs
            }
        }
    }

    // KMP remaps Android source roots; keep existing src/main/ layout until Phase 5 source move
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
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
}

// ─────────────────────────────────────────────────────────────────────────────
// SQLDelight database configuration (Phase 4 — replaces Room)
// ─────────────────────────────────────────────────────────────────────────────
sqldelight {
    databases {
        create("LifeOsDatabase") {
            packageName.set("com.belinze.lifeos.data.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AGP-level configurations that cannot live inside KMP source sets
// ─────────────────────────────────────────────────────────────────────────────
dependencies {
    // Core library desugaring — AGP-level, not a KMP source-set dep
    coreLibraryDesugaring(libs.desugar)

    // Debug-only Compose tooling (build-type variant — not expressible in KMP source sets)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Unit tests (Robolectric — Phase 3 migration test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
}

// Static analysis (Phase 4) — shared YAML, strict gate
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

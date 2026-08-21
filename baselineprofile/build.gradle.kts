plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace    = "com.belinze.lifeos.baselineprofile"
    compileSdk   = 35

    defaultConfig {
        minSdk = 28   // Baseline Profiles require API 28+
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Points to the app module so the generator can start the real app.
    targetProjectPath = ":app"

    // Required for self-instrumented tests on API 29+.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // Re-use the debug build for local iteration; CI uses release.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.test.runner)
    implementation(libs.uiautomator)
}

package com.belinze.lifeos

import androidx.compose.ui.window.ComposeUIViewController
import com.belinze.lifeos.di.iosModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin { modules(iosModule) }
    }
) {
    // App content will be wired here in a later phase when UI is in commonMain.
    // For now this sets up the Koin container for the iOS entry point.
}

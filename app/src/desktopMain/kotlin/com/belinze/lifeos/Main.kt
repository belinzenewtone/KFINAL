package com.belinze.lifeos

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.belinze.lifeos.di.desktopModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin { modules(desktopModule) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "LifeOS",
    ) {
        // App content will be wired here in a later phase when UI is in commonMain
    }
}

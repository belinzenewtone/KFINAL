package com.belinze.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.belinze.lifeos.ui.navigation.LifeOsNavHost
import com.belinze.lifeos.ui.theme.LifeOsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity Compose host.
 *
 * Responsibilities:
 *  - Install the splash screen (hides once the first Compose frame is drawn).
 *  - Enable edge-to-edge display so the Compose UI can draw under system bars.
 *  - Hand off to [LifeOsNavHost], which owns all navigation and auth-guard logic.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LifeOsTheme {
                LifeOsNavHost()
            }
        }
    }
}

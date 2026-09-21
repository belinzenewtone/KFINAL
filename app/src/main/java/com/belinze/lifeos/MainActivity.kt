package com.belinze.lifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 *  - Surface notification tap deep-link routes to the nav graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Pending navigation route from a notification tap; consumed once by the nav graph. */
    var pendingNotifRoute: String? by mutableStateOf(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingNotifRoute = intent?.getStringExtra("nav_route")

        setContent {
            LifeOsTheme {
                LifeOsNavHost(
                    pendingNotifRoute = pendingNotifRoute,
                    onNotifRouteConsumed = { pendingNotifRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotifRoute = intent.getStringExtra("nav_route")
    }
}

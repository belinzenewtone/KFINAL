package com.belinze.lifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.belinze.lifeos.data.datastore.AppPreferenceState
import com.belinze.lifeos.ui.navigation.LifeOsNavHost
import com.belinze.lifeos.ui.theme.LifeOsTheme
import com.belinze.lifeos.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val settingsVm: SettingsViewModel by viewModel()

    var pendingNotifRoute: String? by mutableStateOf(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingNotifRoute = intent?.getStringExtra("nav_route")

        setContent {
            val settings by settingsVm.settings.collectAsState(initial = AppPreferenceState())
            val darkTheme = when (settings.theme) {
                "light" -> false
                "dark"  -> true
                else    -> isSystemInDarkTheme()
            }
            LifeOsTheme(darkTheme = darkTheme) {
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

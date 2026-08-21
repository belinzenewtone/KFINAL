package com.belinze.lifeos.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.core.update.OtaSharedTrigger
import com.belinze.lifeos.core.update.presentation.OtaUpdatePromptHost
import com.belinze.lifeos.ui.components.FloatingTabBar
import com.belinze.lifeos.ui.components.LifeOsTab
import com.belinze.lifeos.ui.screen.assistant.AssistantScreen
import com.belinze.lifeos.ui.screen.calendar.CalendarScreen
import com.belinze.lifeos.ui.screen.finance.FinanceScreen
import com.belinze.lifeos.ui.screen.home.HomeScreen
import com.belinze.lifeos.ui.screen.profile.ProfileScreen
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.AppViewModel
import com.lifeos.sms.SmsEventBus

// ─────────────────────────────────────────────────────────────────────────────
// MainScaffold
//
// 1:1 port of MainTabNavigator.tsx + FloatingTabBar integration.
//
// Layout:
//   ┌─────────────────────────────────────────┐
//   │  Tab content (fills whole screen)       │
//   │  (each tab is edge-to-edge with its own │
//   │   PageScaffold / HeroSurface)           │
//   │                                         │
//   │  ┌───────────────────────────────────┐  │
//   │  │  FloatingTabBar (bottom, overlay) │  │
//   │  └───────────────────────────────────┘  │
//   └─────────────────────────────────────────┘
//
// The FloatingTabBar is absolutely positioned over the content, so each tab
// screen must apply Spacing.bottomNavSafeArea (100dp) as bottom padding on
// any scrollable content to avoid being obscured.
//
// Tab state is preserved with rememberSaveable — matches RN bottom-tab behavior
// where each tab retains its scroll position when switching.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScaffold(
    navController: NavHostController,
    appViewModel:  AppViewModel,
    modifier:      Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(LifeOsTab.Home) }

    // ── Fuliza limit prompt (mirrors AppNavigator.tsx listener) ───────────────
    var showFulizaDialog by rememberSaveable { mutableStateOf(false) }
    var fulizaInput by rememberSaveable { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        SmsEventBus.fulizaLimitNeeded.collect { outstanding ->
            val prefs = appViewModel.uiState.value.prefs
            // Only prompt when the user hasn't configured a limit.
            if (prefs.fulizaLimit <= 0) {
                fulizaInput = outstanding
                showFulizaDialog = true
            }
        }
    }

    if (showFulizaDialog) {
        AlertDialog(
            onDismissRequest = { showFulizaDialog = false },
            title            = { Text("Set your Fuliza limit") },
            text             = {
                Column {
                    Text("A Fuliza charge was detected but you haven't set your credit limit yet. Set it so your balances stay accurate.")
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value         = if (fulizaInput == 0.0) "" else fulizaInput.toInt().toString(),
                        onValueChange = { fulizaInput = it.toDoubleOrNull() ?: 0.0 },
                        label         = { Text("Fuliza limit (KES)") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFulizaDialog = false
                    appViewModel.updateFulizaLimit(fulizaInput)
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFulizaDialog = false }) {
                    Text("Later")
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Tab content (fills the full screen) ───────────────────────────────
        when (selectedTab) {
            LifeOsTab.Home      -> HomeTabContent(navController)
            LifeOsTab.Finance   -> FinanceTabContent(navController)
            LifeOsTab.Calendar  -> CalendarTabContent(navController)
            LifeOsTab.Assistant -> AssistantTabContent(navController)
            LifeOsTab.Profile   -> ProfileTabContent(navController, appViewModel)
        }

        // ── FloatingTabBar — absolutely positioned at bottom ───────────────────
        FloatingTabBar(
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            modifier    = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.xs),  // slight lift for shadow clearance
        )

        // ── OTA update dialog — single instance, overlaid on all content ─────
        // manualTrigger comes from OtaSharedTrigger so Settings (and any other
        // screen) can request a fresh check without a second host composable.
        val otaTrigger by OtaSharedTrigger.manualTrigger.collectAsStateWithLifecycle()
        OtaUpdatePromptHost(shouldCheckForUpdates = true, manualTrigger = otaTrigger)
    }
}

// ─── Tab content slots ────────────────────────────────────────────────────────

@Composable
private fun HomeTabContent(navController: NavHostController) {
    HomeScreen(navController = navController)
}

@Composable
private fun FinanceTabContent(navController: NavHostController) {
    FinanceScreen(navController = navController)
}

@Composable
private fun CalendarTabContent(navController: NavHostController) {
    CalendarScreen(navController = navController)
}

@Composable
private fun AssistantTabContent(navController: NavHostController) {
    AssistantScreen()
}

@Composable
private fun ProfileTabContent(navController: NavHostController, appViewModel: AppViewModel) {
    ProfileScreen(navController = navController)
}

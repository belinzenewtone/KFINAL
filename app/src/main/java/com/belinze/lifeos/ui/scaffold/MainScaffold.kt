package com.belinze.lifeos.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FloatingTabBar
import com.belinze.lifeos.ui.components.LifeOsTab
import com.belinze.lifeos.ui.screen.assistant.AssistantScreen
import com.belinze.lifeos.ui.screen.calendar.CalendarScreen
import com.belinze.lifeos.ui.screen.finance.FinanceScreen
import com.belinze.lifeos.ui.screen.home.HomeScreen
import com.belinze.lifeos.ui.screen.profile.ProfileScreen
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.AppViewModel

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

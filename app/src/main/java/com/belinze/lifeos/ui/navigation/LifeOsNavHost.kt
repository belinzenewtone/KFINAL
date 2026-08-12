package com.belinze.lifeos.ui.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.belinze.lifeos.ui.screen.auth.AppLockScreen
import com.belinze.lifeos.ui.screen.auth.AuthScreen
import com.belinze.lifeos.ui.screen.auth.LoadingScreen
import com.belinze.lifeos.ui.screen.auth.OnboardingScreen
import com.belinze.lifeos.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────────────────────
// LifeOsNavHost — root composable for the whole app.
//
// Mirror of AppNavigator.tsx auth-guard logic:
//
//   1. !hasHydrated                           → LoadingScreen
//   2. screenLockEnabled && isAppLocked       → AppLockScreen
//   3. !hasCompletedOnboarding                → OnboardingScreen
//   4. !isAuthenticated                       → AuthScreen
//   5. else                                   → MainNavHost (authenticated graph)
//
// Lifecycle-based app-lock: mirrors the AppState.addEventListener logic in RN.
//   ‣ ON_STOP  → arm the lock
//   ‣ ON_START → if armed and lockable, call setAppLocked(true) after grace period
//
// The grace period (lockTimeoutMinutes) applies only when fingerprintEnabled.
// PIN-only mode locks immediately (grace = 0).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LifeOsNavHost(
    modifier:     Modifier      = Modifier,
    appViewModel: AppViewModel  = hiltViewModel(),
) {
    val uiState by appViewModel.uiState.collectAsState()

    // ── Lifecycle-based app-lock observer ─────────────────────────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    var armedForLock   by remember { mutableStateOf(false) }
    var backgroundedAt by remember { mutableLongStateOf(0L) }
    var lastForegroundCheck by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    armedForLock   = true
                    backgroundedAt = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (armedForLock) {
                        armedForLock = false
                        val prefs      = appViewModel.uiState.value.prefs
                        val isLockable = prefs.hasCompletedOnboarding &&
                            prefs.isAuthenticated &&
                            prefs.screenLockEnabled &&
                            prefs.pinCode.isNotEmpty()

                        if (isLockable) {
                            val graceMs   = if (prefs.fingerprintEnabled)
                                prefs.lockTimeoutMinutes * 60_000L
                            else 0L
                            val elapsedMs = System.currentTimeMillis() - backgroundedAt
                            if (elapsedMs >= graceMs) {
                                appViewModel.setAppLocked(true)
                            }
                        }
                    }
                    // Foreground re-check — throttled to 60s (mirrors RN AppState
                    // 'active' handler running checkAllBudgetThresholds).
                    val now = System.currentTimeMillis()
                    if (now - lastForegroundCheck > 60_000L) {
                        lastForegroundCheck = now
                        appViewModel.refreshOnForeground()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Forgot-PIN dialog state ───────────────────────────────────────────────
    var showForgotPinDialog by remember { mutableStateOf(false) }

    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title   = { Text("Forgot your PIN?") },
            text    = { Text("This turns off screen lock so you can get back into the app. You can set a new PIN afterward in Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showForgotPinDialog = false
                    appViewModel.forgotPin()
                }) { Text("Turn off screen lock") }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ── Auth guard — declarative, no NavHost needed at this level ─────────────
    when {
        !uiState.hasHydrated -> {
            LoadingScreen(modifier = modifier)
        }

        uiState.prefs.screenLockEnabled && uiState.isAppLocked -> {
            AppLockScreen(
                prefs       = uiState.prefs,
                onUnlock    = { appViewModel.setAppLocked(false) },
                onForgotPin = { _ -> showForgotPinDialog = true },
                modifier    = modifier,
            )
        }

        !uiState.prefs.hasCompletedOnboarding -> {
            OnboardingScreen(
                onComplete = { appViewModel.completeOnboarding() },
                viewModel  = appViewModel,
                modifier   = modifier,
            )
        }

        !uiState.prefs.isAuthenticated -> {
            AuthScreen(
                onAuthenticated = { appViewModel.setAuthenticated(true) },
                viewModel       = appViewModel,
                modifier        = modifier,
            )
        }

        else -> {
            // Authenticated: hand off to the full navigation graph
            MainNavHost(
                appViewModel = appViewModel,
                modifier     = modifier,
            )
        }
    }
}

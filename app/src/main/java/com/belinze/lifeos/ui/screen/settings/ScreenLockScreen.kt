package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SettingsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ScreenLockScreen — Security settings
//
// Tabs: Fingerprint | PIN
//   Fingerprint tab: enable toggle + auto-lock timeout selector
//   PIN tab: enable toggle + PIN change form (new/confirm)
// ─────────────────────────────────────────────────────────────────────────────

private val TIMEOUT_OPTIONS = listOf(1, 5, 15, 30, 60)

@Composable
fun ScreenLockScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings      by viewModel.settings.collectAsState()
    var activeTab     by rememberSaveable { mutableStateOf(0) }  // 0=Fingerprint, 1=PIN
    var newPin        by rememberSaveable { mutableStateOf("") }
    var confirmPin    by rememberSaveable { mutableStateOf("") }
    var pinError      by remember { mutableStateOf<String?>(null) }
    var pinSaving     by remember { mutableStateOf(false) }
    var pinSaved      by remember { mutableStateOf(false) }

    PageScaffold(
        eyebrow = "Settings",
        title   = "Screen Lock",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {

            // ── Tab selector ──────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf("Fingerprint", "PIN").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeTab == idx,
                        onClick  = { activeTab = idx },
                        label    = { Text(label) },
                    )
                }
            }

            if (pinSaved) {
                InlineBanner(
                    message  = "PIN updated successfully",
                    tone     = BannerTone.Success,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
                )
            }

            if (activeTab == 0) {
                // ── Fingerprint tab ───────────────────────────────────────
                SettingsToggleRow(
                    label    = "Fingerprint unlock",
                    desc     = "Use biometrics to unlock the app",
                    checked  = settings.fingerprintEnabled,
                    onToggle = { viewModel.setFingerprintEnabled(it) },
                )
                if (settings.fingerprintEnabled) {
                    SectionHeader(label = "Auto-lock after", modifier = Modifier.padding(top = Spacing.sm))
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        TIMEOUT_OPTIONS.forEach { mins ->
                            val label = if (mins < 60) "${mins}m" else "1h"
                            FilterChip(
                                selected = settings.lockTimeoutMinutes == mins,
                                onClick  = { viewModel.setLockTimeout(mins) },
                                label    = { Text(label) },
                            )
                        }
                    }
                }
            } else {
                // ── PIN tab ───────────────────────────────────────────────
                SettingsToggleRow(
                    label    = "PIN lock",
                    desc     = "Require PIN to unlock the app",
                    checked  = settings.screenLockEnabled,
                    onToggle = { v ->
                        if (v) viewModel.setScreenLockEnabled(true)
                        else viewModel.disableScreenLock()
                    },
                )

                if (settings.screenLockEnabled) {
                    Spacer(Modifier.height(Spacing.md))
                    Text("Change PIN", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.70f),
                        modifier = Modifier.padding(horizontal = Spacing.screenHorizontal))
                    Spacer(Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value            = newPin,
                        onValueChange    = { if (it.length <= 6) newPin = it },
                        label            = { Text("New PIN (4-6 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions  = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier         = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal),
                        singleLine       = true,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value            = confirmPin,
                        onValueChange    = { if (it.length <= 6) confirmPin = it },
                        label            = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions  = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier         = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal),
                        singleLine       = true,
                    )
                    if (pinError != null) {
                        Text(pinError!!, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = 4.dp))
                    }
                    Spacer(Modifier.height(Spacing.md))
                    Button(
                        onClick  = {
                            pinError = null
                            when {
                                newPin.length < 4    -> pinError = "PIN must be at least 4 digits"
                                newPin != confirmPin -> pinError = "PINs do not match"
                                else -> {
                                    pinSaving = true
                                    viewModel.setPinCode(newPin)
                                    newPin = ""; confirmPin = ""
                                    pinSaving = false; pinSaved = true
                                }
                            }
                        },
                        enabled  = !pinSaving && newPin.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal),
                    ) {
                        if (pinSaving) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Save PIN")
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SettingsRow
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SettingsViewModel

private const val PIN_LENGTH = 6
private val TIMEOUT_OPTIONS = listOf(0, 1, 5, 15, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenLockScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var activeTab by remember { mutableStateOf(if (settings.pinCode.isNotEmpty()) "fingerprint" else "pin") }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    PageScaffold(
        title = "Screen Lock",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        TopBanner(
            visible = message != null,
            message = message ?: "",
            tone = BannerTone.Success,
            onDismiss = { message = null },
            autoDismissMs = 2500,
        )

        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf("fingerprint" to "Fingerprint", "pin" to "PIN").forEach { (value, label) ->
                        FilterChip(
                            selected = activeTab == value,
                            onClick = { activeTab = value },
                            label = { Text(label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            if (activeTab == "fingerprint") {
                GlassCard {
                    SettingsRow(
                        icon = Icons.Filled.Fingerprint,
                        label = "Fingerprint",
                        subtitle = if (!settings.screenLockEnabled || settings.pinCode.isEmpty())
                            "Requires a PIN as fallback" else null,
                        toggle = true,
                        toggleValue = settings.fingerprintEnabled,
                        onToggleChange = { v ->
                            viewModel.setFingerprintEnabled(v)
                            message = if (v) "Fingerprint unlock enabled" else null
                        },
                    )
                    if (settings.fingerprintEnabled) {
                        Spacer(Modifier.height(Spacing.lg))
                        Text("Auto-lock", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            TIMEOUT_OPTIONS.forEach { mins ->
                                FilterChip(
                                    selected = settings.lockTimeoutMinutes == mins,
                                    onClick = { viewModel.setLockTimeout(mins) },
                                    label = {
                                        Text(
                                            if (mins == 0) "Immediately"
                                            else if (mins == 60) "After 1 hour"
                                            else "After $mins minute${if (mins == 1) "" else "s"}"
                                        )
                                    },
                                )
                            }
                        }
                        Text(
                            "How long the app can sit in the background before fingerprint (or your PIN) is required again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                GlassCard {
                    SettingsRow(
                        label = "PIN lock",
                        toggle = true,
                        toggleValue = settings.screenLockEnabled,
                        onToggleChange = { v ->
                            if (v) viewModel.setScreenLockEnabled(true)
                            else viewModel.disableScreenLock()
                        },
                        isLast = true,
                    )

                    if (settings.screenLockEnabled) {
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            if (settings.pinCode.isNotEmpty()) "Reset your secure access code"
                            else "Set up your secure access code",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "Use exactly $PIN_LENGTH digits. Your new PIN is stored only on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.lg))

                        if (settings.pinCode.isNotEmpty()) {
                            PinInput("Current PIN", currentPin, { currentPin = it })
                        }
                        PinInput("New PIN", newPin, { newPin = it })
                        PinInput("Confirm new PIN", confirmPin, { confirmPin = it })

                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = Spacing.sm))
                        }

                        Button(
                            onClick = {
                                when {
                                    settings.pinCode.isNotEmpty() && currentPin != settings.pinCode -> {
                                        error = "Incorrect PIN"
                                    }
                                    newPin.length != PIN_LENGTH || confirmPin.length != PIN_LENGTH -> {
                                        error = "PIN must be exactly $PIN_LENGTH digits."
                                    }
                                    newPin != confirmPin -> {
                                        error = "PINs don't match"
                                    }
                                    else -> {
                                        viewModel.setPinCode(newPin)
                                        viewModel.setScreenLockEnabled(true)
                                        currentPin = ""; newPin = ""; confirmPin = ""
                                        message = if (settings.pinCode.isEmpty()) "PIN set up successfully"
                                                  else "PIN changed successfully"
                                    }
                                }
                            },
                            enabled = newPin.length == PIN_LENGTH && confirmPin.length == PIN_LENGTH &&
                                (settings.pinCode.isEmpty() || currentPin.length == PIN_LENGTH),
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.base),
                        ) {
                            Text(if (settings.pinCode.isNotEmpty()) "Update PIN" else "Set PIN")
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun PinInput(label: String, value: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value.length}/$PIN_LENGTH", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(PIN_LENGTH)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            placeholder = { Text("0 0 0 0 0 0") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SettingsRow
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    PageScaffold(
        title = "Notifications",
        onBack = { navController.popBackStack() },
        scrollable = false,
        topBanner = {
            TopBanner(
                visible = infoMessage != null,
                message = infoMessage ?: "",
                tone = BannerTone.Success,
                onDismiss = { infoMessage = null },
                autoDismissMs = 3000,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            SectionLabel("Notifications")
            GlassCard {
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    label = "Enable notifications",
                    toggle = true,
                    toggleValue = settings.notificationsEnabled,
                    onToggleChange = { v ->
                        viewModel.setNotificationsEnabled(v)
                        infoMessage = if (v) "Notifications enabled" else "Notifications disabled"
                    },
                    isLast = true,
                )
            }

            SectionLabel("Budget Alerts")
            GlassCard {
                SettingsRow(
                    icon = Icons.Outlined.Wallet,
                    label = "Budget threshold alerts",
                    subtitle = "Notify when spending exceeds a budget category",
                    toggle = true,
                    toggleValue = settings.budgetThresholdAlerts,
                    onToggleChange = { v ->
                        viewModel.setBudgetAlerts(v)
                        infoMessage = if (v) "Budget alerts enabled" else "Budget alerts disabled"
                    },
                    isLast = true,
                )
            }

            if (settings.budgetThresholdAlerts) {
                SectionLabel("Alert Levels")
                GlassCard {
                    // Local state keeps the slider thumb smooth during drag;
                    // the ViewModel is only notified when the user lifts their finger
                    // (onValueChangeFinished), which prevents the GlassCard from
                    // recomposing on every drag frame and causing background jumps.
                    AlertLevelStepper(
                        label = "High",
                        savedValue = settings.alertThresholdHigh,
                        onCommit = { viewModel.setAlertThresholdHigh(it) },
                    )
                    AlertLevelStepper(
                        label = "Medium",
                        savedValue = settings.alertThresholdMedium,
                        onCommit = { viewModel.setAlertThresholdMedium(it) },
                    )
                    AlertLevelStepper(
                        label = "Low",
                        savedValue = settings.alertThresholdLow,
                        onCommit = { viewModel.setAlertThresholdLow(it) },
                        isLast = true,
                    )
                }
            }

            SectionLabel("Daily Digest")
            GlassCard {
                SettingsRow(
                    icon = Icons.Outlined.WbSunny,
                    label = "Morning summary",
                    subtitle = "Morning summary of tasks, spending and upcoming events",
                    toggle = true,
                    toggleValue = settings.dailyDigestMorningSummary,
                    onToggleChange = { v ->
                        viewModel.setDailyDigestMorning(v)
                        infoMessage = if (v) "Daily digest enabled" else "Daily digest disabled"
                    },
                )
                SettingsRow(
                    label = "Delivery time",
                    value = formatTime(settings.dailyDigestDeliveryTime),
                    showChevron = true,
                    onPress = { showTimePicker = true },
                    disabled = !settings.dailyDigestMorningSummary,
                    isLast = true,
                )
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }

    // ── Delivery time picker dialog ───────────────────────────────────────────
    if (showTimePicker) {
        val parts   = settings.dailyDigestDeliveryTime.split(":")
        val initH   = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val initM   = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val tState  = rememberTimePickerState(
            initialHour   = initH,
            initialMinute = initM,
            is24Hour      = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title            = { Text("Delivery time") },
            text             = { TimePicker(state = tState) },
            confirmButton    = {
                TextButton(onClick = {
                    showTimePicker = false
                    val hStr = tState.hour.toString().padStart(2, '0')
                    val mStr = tState.minute.toString().padStart(2, '0')
                    val time = "$hStr:$mStr"
                    viewModel.setDailyDigestTime(time)
                    infoMessage = "Delivery time: ${formatTime(time)}"
                }) { Text("Save") }
            },
            dismissButton    = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}

// ─── AlertLevelStepper ────────────────────────────────────────────────────────

@Composable
private fun AlertLevelStepper(
    label:      String,
    savedValue: Int,
    onCommit:   (Int) -> Unit,
    isLast:     Boolean = false,
) {
    // Local drag state: keeps the thumb responsive without writing to the
    // ViewModel (and triggering recomposition of the whole card) on every frame.
    var draft by remember(savedValue) { mutableFloatStateOf(savedValue.toFloat()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top    = Spacing.sm,
                bottom = if (isLast) Spacing.xs else Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Slider(
            value                = draft,
            onValueChange        = { draft = it },          // local state only — no recompose above
            onValueChangeFinished = { onCommit(draft.toInt()) },   // write ViewModel when thumb released
            valueRange           = 0f..100f,
            steps                = 9,
            modifier             = Modifier.weight(2f),
        )
        Text(
            "${draft.toInt()}%",
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        style    = MaterialTheme.typography.titleMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.base),
    )
}

private fun formatTime(time: String): String {
    val parts = time.split(":")
    if (parts.size < 2) return time
    val h = parts[0].toIntOrNull() ?: return time
    val m = parts[1].toIntOrNull() ?: return time
    val period      = if (h >= 12) "PM" else "AM"
    val displayHour = if (h % 12 == 0) 12 else h % 12
    return "${displayHour.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} $period"
}

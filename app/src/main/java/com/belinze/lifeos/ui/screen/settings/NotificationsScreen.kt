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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var infoMessage by remember { mutableStateOf<String?>(null) }

    PageScaffold(
        title = "Notifications",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        TopBanner(
            visible = infoMessage != null,
            message = infoMessage ?: "",
            tone = BannerTone.Success,
            onDismiss = { infoMessage = null },
            autoDismissMs = 3000,
        )

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            SectionLabel("Notifications")
            GlassCard {
                SettingsRow(
                    icon = Icons.Filled.Notifications,
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
                    icon = Icons.Filled.Wallet,
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
                    AlertLevelStepper(
                        label = "High",
                        value = settings.alertThresholdHigh,
                        onChange = { viewModel.setAlertThresholdHigh(it) },
                    )
                    AlertLevelStepper(
                        label = "Medium",
                        value = settings.alertThresholdMedium,
                        onChange = { viewModel.setAlertThresholdMedium(it) },
                    )
                    AlertLevelStepper(
                        label = "Low",
                        value = settings.alertThresholdLow,
                        onChange = { viewModel.setAlertThresholdLow(it) },
                    )
                }
            }

            SectionLabel("Daily Digest")
            GlassCard {
                SettingsRow(
                    icon = Icons.Filled.WbSunny,
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
                    onPress = {
                        // simplified time edit via dialog
                        infoMessage = "Delivery time: ${formatTime(settings.dailyDigestDeliveryTime)}"
                    },
                    disabled = !settings.dailyDigestMorningSummary,
                    isLast = true,
                )
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.base),
    )
}

@Composable
private fun AlertLevelStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 9,
            modifier = Modifier.weight(2f),
        )
        Text("$value%", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.sm))
    }
}

private fun formatTime(time: String): String {
    val parts = time.split(":")
    if (parts.size < 2) return time
    val h = parts[0].toIntOrNull() ?: return time
    val m = parts[1].toIntOrNull() ?: return time
    val period = if (h >= 12) "PM" else "AM"
    val displayHour = if (h % 12 == 0) 12 else h % 12
    return "${displayHour.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} $period"
}

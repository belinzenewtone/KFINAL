package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SettingsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// NotificationsScreen — matches NotificationsScreen.tsx
//
// Sections:
//   ‣ Master enable toggle
//   ‣ Budget alerts toggle
//   ‣ Task & event reminders toggle
//   ‣ Daily digest toggle
//   ‣ Recurring rule alerts toggle
//   ‣ Transaction alerts toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()

    PageScaffold(
        eyebrow = "Settings",
        title   = "Notifications",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // ── Master toggle ─────────────────────────────────────────────────
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable notifications", fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                        Text("Receive all in-app and system notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    }
                    Switch(
                        checked         = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    )
                }
            }

            // ── Budget Alerts ─────────────────────────────────────────────────
            SectionHeader(label = "Budget Alerts")
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                NotifToggleRow(
                    label    = "Budget threshold alerts",
                    desc     = "Notify when spending approaches budget limits",
                    checked  = settings.budgetThresholdAlerts && settings.notificationsEnabled,
                    enabled  = settings.notificationsEnabled,
                    onToggle = { viewModel.setBudgetAlerts(it) },
                )
            }

            // ── Reminders ─────────────────────────────────────────────────────
            SectionHeader(label = "Task & Events")
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                NotifToggleRow(
                    label    = "Task & event reminders",
                    desc     = "Alerts for upcoming deadlines and events",
                    checked  = settings.notifReminders && settings.notificationsEnabled,
                    enabled  = settings.notificationsEnabled,
                    onToggle = { viewModel.setNotifReminders(it) },
                )
            }

            // ── Daily Digest ──────────────────────────────────────────────────
            SectionHeader(label = "Daily Digest")
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                NotifToggleRow(
                    label    = "Morning summary",
                    desc     = "Daily spend and task summary each morning",
                    checked  = settings.notifDailyDigest && settings.notificationsEnabled,
                    enabled  = settings.notificationsEnabled,
                    onToggle = { viewModel.setNotifDailyDigest(it) },
                )
            }

            // ── Recurring ─────────────────────────────────────────────────────
            SectionHeader(label = "Recurring Rules")
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                NotifToggleRow(
                    label    = "Recurring rule alerts",
                    desc     = "Notify when recurring amounts are detected",
                    checked  = settings.notifRecurringRules && settings.notificationsEnabled,
                    enabled  = settings.notificationsEnabled,
                    onToggle = { viewModel.setNotifRecurring(it) },
                )
            }

            // ── Transaction Alerts ────────────────────────────────────────────
            SectionHeader(label = "Transactions")
            FrostCard(modifier = Modifier.fillMaxWidth()) {
                NotifToggleRow(
                    label    = "Transaction alerts",
                    desc     = "Notify on incoming SMS transaction detections",
                    checked  = settings.notifTxAlerts && settings.notificationsEnabled,
                    enabled  = settings.notificationsEnabled,
                    onToggle = { viewModel.setNotifTxAlerts(it) },
                )
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun NotifToggleRow(
    label:    String,
    desc:     String,
    checked:  Boolean,
    enabled:  Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground)
            Text(desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
        }
        Switch(checked = checked, onCheckedChange = onToggle, enabled = enabled)
    }
}

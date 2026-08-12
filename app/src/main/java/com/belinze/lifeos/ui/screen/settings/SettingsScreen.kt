package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.SectionHeader
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SettingsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SettingsScreen — hub settings screen
//
// Sections:
//   ‣ Appearance (Light / Auto / Dark theme)
//   ‣ Calendar (swipe toggle)
//   ‣ Security (Screen Lock row)
//   ‣ Finance (Fuliza limit row)
//   ‣ Notifications row
//   ‣ Import SMS toggle + SMS Import Health row
//   ‣ About section (Changelog + danger zone)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    PageScaffold(
        eyebrow = "App",
        title   = "Settings",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {

            // ── Appearance ────────────────────────────────────────────────
            SectionHeader(label = "Appearance")
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf("light", "system", "dark").forEach { theme ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick  = { viewModel.setTheme(theme) },
                        label    = { Text(theme.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // ── Display ───────────────────────────────────────────────────
            SectionHeader(label = "Display")
            SettingsToggleRow(
                label    = "Calendar swipe navigation",
                checked  = settings.calendarSwipe,
                onToggle = { viewModel.setCalendarSwipe(it) },
            )
            SettingsToggleRow(
                label    = "Haptic feedback",
                checked  = settings.hapticFeedback,
                onToggle = { viewModel.setHapticFeedback(it) },
            )

            // ── Security ──────────────────────────────────────────────────
            SectionHeader(label = "Security", modifier = Modifier.padding(top = Spacing.sm))
            SettingsNavRow(
                label   = "Screen Lock",
                desc    = if (settings.screenLockEnabled) "Enabled" else "Disabled",
                onClick = { navController.navigate(Route.SECURITY) },
            )

            // ── Notifications ─────────────────────────────────────────────
            SectionHeader(label = "Notifications", modifier = Modifier.padding(top = Spacing.sm))
            SettingsNavRow(
                label   = "Notification settings",
                desc    = if (settings.notificationsEnabled) "Enabled" else "Disabled",
                onClick = { navController.navigate(Route.NOTIFICATIONS) },
            )

            // ── Finance ───────────────────────────────────────────────────
            SectionHeader(label = "Finance", modifier = Modifier.padding(top = Spacing.sm))
            SettingsNavRow(
                label   = "Budgets & Planner",
                desc    = "Manage budgets, goals, loans",
                onClick = { navController.navigate(Route.BUDGETS) },
            )

            // ── SMS Import ────────────────────────────────────────────────
            SectionHeader(label = "SMS Import", modifier = Modifier.padding(top = Spacing.sm))
            SettingsToggleRow(
                label    = "Background SMS receiver",
                checked  = settings.smsBgReceiver,
                onToggle = { viewModel.setSmsBgReceiver(it) },
            )
            SettingsNavRow(
                label   = "SMS Import Health",
                desc    = "Check recent import activity",
                onClick = { navController.navigate(Route.SMS_IMPORT_HEALTH) },
            )
            SettingsNavRow(
                label   = "Review Queue",
                desc    = "Transactions pending review",
                onClick = { navController.navigate(Route.REVIEW_QUEUE) },
            )

            // ── About ─────────────────────────────────────────────────────
            SectionHeader(label = "About", modifier = Modifier.padding(top = Spacing.sm))
            SettingsNavRow(
                label   = "What's new",
                desc    = "Changelog",
                onClick = { navController.navigate(Route.CHANGELOG) },
            )
            SettingsNavRow(
                label   = "Clear all local data",
                desc    = "⚠ This cannot be undone",
                onClick = { showClearDialog = true },
                isDestructive = true,
            )

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title            = { Text("Clear all data?") },
            text             = { Text("All transactions, tasks, events, and settings will be permanently deleted. This cannot be undone.") },
            confirmButton    = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ─── Settings row helpers ─────────────────────────────────────────────────────

@Composable
fun SettingsToggleRow(
    label:    String,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
    desc:     String?   = null,
    modifier: Modifier  = Modifier,
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (desc != null) {
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            }
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
fun SettingsNavRow(
    label:          String,
    desc:           String?  = null,
    onClick:        () -> Unit,
    isDestructive:  Boolean  = false,
    modifier:       Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val labelColor = if (isDestructive) MaterialTheme.colorScheme.error
                     else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(0.12f)),
                onClick    = onClick)
            .padding(horizontal = Spacing.screenHorizontal, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
            if (desc != null) {
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(0.35f))
    }
}

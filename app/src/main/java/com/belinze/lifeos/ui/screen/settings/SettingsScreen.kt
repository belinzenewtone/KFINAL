package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.FulizaLimitModal
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.SegmentedControl
import com.belinze.lifeos.ui.components.SettingsRow
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SettingsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SettingsScreen — 1:1 port of src/screens/settings/SettingsScreen.tsx.
// ─────────────────────────────────────────────────────────────────────────────

private const val APP_NAME = "LifeOS"
private const val APP_VERSION = "1.0.0"
private val WARNING = Color(0xFFF5CB5C)

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel:     SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var fulizaVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        TopBanner(
            visible = infoMessage != null,
            message = infoMessage ?: "",
            tone = BannerTone.Success,
            onDismiss = { infoMessage = null },
            autoDismissMs = 3000,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = Spacing.sm)
                .padding(bottom = Spacing.bottomNavSafeArea),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text  = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.width(24.dp))
            }

            SectionLabel("Appearance")
            GlassCard {
                SegmentedControl(
                    options = listOf(
                        "light"  to "Light",
                        "system" to "Auto",
                        "dark"   to "Dark",
                    ),
                    value = settings.theme,
                    onChange = { value ->
                        viewModel.setTheme(value)
                        infoMessage = "Theme set to ${value.replaceFirstChar { it.uppercase() }}"
                    },
                )
            }

            SectionLabel("Calendar")
            GlassCard {
                SettingsRow(
                    icon = Icons.Filled.SwapHoriz,
                    label = "Swipe to change month",
                    subtitle = "Swipe left/right on the calendar grid to move between months.",
                    toggle = true,
                    toggleValue = settings.calendarSwipe,
                    onToggleChange = {
                        viewModel.setCalendarSwipe(it)
                        infoMessage = if (it) "Calendar swipe on" else "Calendar swipe off"
                    },
                    isLast = true,
                )
            }

            SectionLabel("Security")
            GlassCard {
                val lockSubtitle = buildString {
                    if (settings.fingerprintEnabled) append("Fingerprint")
                    if (settings.pinCode.isNotEmpty() && settings.screenLockEnabled) {
                        if (isNotEmpty()) append(" · ")
                        append("PIN")
                    }
                    if (isEmpty()) append("No lock configured")
                }
                SettingsRow(
                    icon = Icons.Filled.Security,
                    label = "Screen lock",
                    subtitle = lockSubtitle,
                    showChevron = true,
                    onPress = { navController.navigate(Route.SCREEN_LOCK) },
                )
                SettingsRow(
                    icon = Icons.Filled.TouchApp,
                    label = "Haptic feedback",
                    subtitle = "Vibration on actions like completing tasks or deleting items.",
                    toggle = true,
                    toggleValue = settings.hapticFeedback,
                    onToggleChange = {
                        viewModel.setHapticFeedback(it)
                        infoMessage = if (it) "Haptics enabled" else "Haptics disabled"
                    },
                    isLast = true,
                )
            }

            SectionLabel("Notifications")
            GlassCard {
                val notificationsSubtitle = buildString {
                    if (settings.budgetThresholdAlerts) append("Budget alerts")
                    if (settings.notifDailyDigest) {
                        if (isNotEmpty()) append(" · ")
                        append("Daily digest")
                    }
                    if (isEmpty()) append("All off")
                }
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    label = "Notification settings",
                    subtitle = notificationsSubtitle,
                    showChevron = true,
                    onPress = { navController.navigate(Route.NOTIFICATIONS) },
                    isLast = true,
                )
            }

            SectionLabel("Assistant")
            GlassCard {
                SettingsRow(
                    icon = Icons.Filled.AutoAwesome,
                    label = "Quick suggestions",
                    subtitle = "Allow the assistant to propose actions based on your messages",
                    toggle = true,
                    toggleValue = settings.assistantQuickSuggestions,
                    onToggleChange = {
                        viewModel.setAssistantQuickSuggestions(it)
                        infoMessage = if (it) "AI suggestions enabled" else "AI suggestions disabled"
                    },
                    isLast = true,
                )
            }

            SectionLabel("Finance")
            GlassCard {
                SettingsRow(
                    icon = Icons.Filled.Wallet,
                    label = "Fuliza credit limit",
                    value = if (settings.fulizaLimit > 0) com.belinze.lifeos.util.formatCurrency(settings.fulizaLimit)
                            else "Not set",
                    showChevron = true,
                    onPress = { fulizaVisible = true },
                    isLast = true,
                )
            }

            SectionLabel("Import SMS")
            if (!viewModel.hasSmsPermissions()) {
                PermissionBanner(onClick = { infoMessage = "Grant SMS permissions in device Settings" })
            }
            GlassCard {
                SettingsRow(
                    icon = Icons.Filled.Radio,
                    label = "Background receiver",
                    subtitle = "Automatically capture & analyse M-Pesa messages even when the app is closed.",
                    toggle = true,
                    toggleValue = settings.smsBgReceiver,
                    onToggleChange = {
                        viewModel.setSmsBgReceiver(it)
                        infoMessage = if (it) "Background receiver on" else "Background receiver off"
                    },
                )
                SettingsRow(
                    icon = Icons.Filled.Medication,
                    label = "Import Health",
                    showChevron = true,
                    onPress = { navController.navigate(Route.SMS_IMPORT_HEALTH) },
                )
                SettingsRow(
                    icon = Icons.Filled.List,
                    label = "Review Queue",
                    showChevron = true,
                    onPress = { navController.navigate(Route.REVIEW_QUEUE) },
                    isLast = true,
                )
            }

            SectionLabel("About")
            GlassCard {
                SettingsRow(
                    icon = Icons.Filled.CardGiftcard,
                    label = "What's new",
                    showChevron = true,
                    onPress = { navController.navigate(Route.CHANGELOG) },
                )
                SettingsRow(
                    icon = Icons.Filled.Info,
                    label = "About Version",
                    value = "$APP_NAME $APP_VERSION",
                    showChevron = true,
                    onPress = { },
                )
                SettingsRow(
                    icon = Icons.Filled.Delete,
                    iconColor = MaterialTheme.colorScheme.error,
                    label = "Clear all local data",
                    showChevron = true,
                    destructive = true,
                    onPress = { showClearDialog = true },
                    isLast = true,
                )
            }

            SectionLabel("App Updates")
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                ) {
                    OutlinedButton(
                        onClick = {
                            infoMessage = "You're running $APP_VERSION in a development build — update checks only work in a published EAS build."
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Check", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = { infoMessage = "No update available yet. Check for updates first." },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Download")
                    }
                }
            }
        }
    }

    FulizaLimitModal(
        visible = fulizaVisible,
        currentLimit = settings.fulizaLimit,
        onSave = { limit ->
            viewModel.setFulizaLimit(limit)
            fulizaVisible = false
            infoMessage = if (limit > 0)
                "Fuliza limit set to ${com.belinze.lifeos.util.formatCurrency(limit)}"
            else "Fuliza credit limit cleared"
        },
        onCancel = { fulizaVisible = false },
    )

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all local data?") },
            text = { Text("This will reset the app to its initial state. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAllData {
                        navController.popBackStack(Route.MAIN, inclusive = false)
                    }
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.base),
    )
}

@Composable
private fun PermissionBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm)
            .background(WARNING.copy(alpha = 0x20 / 255f), RoundedCornerShape(20.dp))
            .border(1.dp, WARNING, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = WARNING.copy(alpha = 0.2f)),
                onClick = onClick,
            )
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = WARNING, modifier = Modifier.size(18.dp))
        Text(
            text = "SMS permissions not granted — tap to allow",
            style = MaterialTheme.typography.bodyMedium,
            color = WARNING,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = WARNING,
            modifier = Modifier.size(16.dp),
        )
    }
}

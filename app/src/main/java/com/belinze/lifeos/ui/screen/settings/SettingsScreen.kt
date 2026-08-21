package com.belinze.lifeos.ui.screen.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.core.update.presentation.OtaUpdatePromptHost
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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var fulizaVisible by remember { mutableStateOf(false) }
    var smsGranted by remember { mutableStateOf(viewModel.hasSmsPermissions()) }
    // ST-2: track permission-request-in-flight
    var smsRequesting by remember { mutableStateOf(false) }
    // OTA: manualTrigger increments each time user taps "Check for Updates"
    var otaCheckTrigger by remember { mutableStateOf(0) }

    // ST-4: re-check SMS permission on every lifecycle resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                smsGranted = viewModel.hasSmsPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        smsRequesting = false
        smsGranted = results.values.any { it }
        infoMessage = if (smsGranted) {
            "SMS permissions granted"
        } else {
                // ST-3: denial message
            "Permissions denied — grant them in device Settings"
        }
    }

    // OTA update dialog — triggered manually or auto on app launch via MainScaffold
    OtaUpdatePromptHost(shouldCheckForUpdates = true, manualTrigger = otaCheckTrigger)

    Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
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
                    icon = Icons.Outlined.SwapHoriz,
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
                    icon = Icons.Outlined.Security,
                    label = "Screen lock",
                    subtitle = lockSubtitle,
                    showChevron = true,
                    onPress = { navController.navigate(Route.SCREEN_LOCK) },
                )
                SettingsRow(
                    icon = Icons.Outlined.TouchApp,
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
                    icon = Icons.Outlined.Notifications,
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
                    icon = Icons.Outlined.AutoAwesome,
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
                    icon = Icons.Outlined.Wallet,
                    label = "Fuliza credit limit",
                    value = if (settings.fulizaLimit > 0) {
                        com.belinze.lifeos.util.formatCurrency(settings.fulizaLimit)
                    } else {
                        "Not set"
                    },
                    showChevron = true,
                    onPress = { fulizaVisible = true },
                    isLast = true,
                )
            }

            SectionLabel("Import SMS")
            if (!smsGranted) {
                // ST-2: show "Requesting…" label while permission dialog is open
                PermissionBanner(
                    requesting = smsRequesting,
                    onClick = {
                        smsRequesting = true
                        smsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                        )
                    },
                )
            }
            GlassCard {
                SettingsRow(
                    icon = Icons.Outlined.Radio,
                    label = "Background receiver",
                    subtitle = "Automatically capture & analyse M-Pesa messages even when the app is closed.",
                    toggle = true,
                    toggleValue = settings.smsBgReceiver,
                    onToggleChange = { enabled ->
                        try {
                            viewModel.setSmsBgReceiver(enabled)
                            infoMessage = if (enabled) "Background receiver on" else "Background receiver off"
                            // ST-6: on enable, check battery optimization and prompt
                            if (enabled) {
                                val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
                                if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        } catch (e: Exception) {
                            // ST-7: show error if toggle throws
                            infoMessage = "Could not update background receiver"
                        }
                    },
                )
                SettingsRow(
                    icon = Icons.Outlined.Medication,
                    label = "Import Health",
                    showChevron = true,
                    onPress = { navController.navigate(Route.SMS_IMPORT_HEALTH) },
                )
                SettingsRow(
                    icon = Icons.Outlined.List,
                    label = "Review Queue",
                    showChevron = true,
                    onPress = { navController.navigate(Route.REVIEW_QUEUE) },
                    isLast = true,
                )
            }

            SectionLabel("About")
            GlassCard {
                SettingsRow(
                    icon = Icons.Outlined.CardGiftcard,
                    label = "What's new",
                    showChevron = true,
                    onPress = { navController.navigate(Route.CHANGELOG) },
                )
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    label = "About Version",
                    value = "$APP_NAME $APP_VERSION",
                    showChevron = true,
                    onPress = { showAboutDialog = true },
                )
                SettingsRow(
                    icon = Icons.Outlined.Delete,
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
                SettingsRow(
                    icon        = Icons.Outlined.Refresh,
                    label       = "Check for Updates",
                    value       = "v$APP_VERSION",
                    showChevron = true,
                    isLast      = true,
                    onPress     = { otaCheckTrigger++ },
                )
            }
        }

        // Banner overlaid so it doesn't shift scrollable content
        TopBanner(
            visible = infoMessage != null,
            message = infoMessage ?: "",
            tone = BannerTone.Success,
            onDismiss = { infoMessage = null },
            autoDismissMs = 3000,
        )
    }

    FulizaLimitModal(
        visible = fulizaVisible,
        currentLimit = settings.fulizaLimit,
        onSave = { limit ->
            viewModel.setFulizaLimit(limit)
            fulizaVisible = false
            infoMessage = if (limit > 0) {
                "Fuliza limit set to ${com.belinze.lifeos.util.formatCurrency(limit)}"
            } else {
                "Fuliza credit limit cleared"
            }
        },
        onCancel = { fulizaVisible = false },
    )

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About") },
            text  = { Text("$APP_NAME v$APP_VERSION") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            },
        )
    }

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

// ST-2: requesting param shows "Requesting…" and hides chevron while in-flight
@Composable
private fun PermissionBanner(onClick: () -> Unit, requesting: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm)
            .background(WARNING.copy(alpha = 0x20 / 255f), RoundedCornerShape(20.dp))
            .border(1.dp, WARNING, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = WARNING.copy(alpha = 0.2f)),
                onClick = { if (!requesting) onClick() },
            )
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (requesting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = WARNING)
        } else {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = WARNING, modifier = Modifier.size(18.dp))
        }
        Text(
            text = if (requesting) "Requesting…" else "SMS permissions not granted — tap to allow",
            style = MaterialTheme.typography.bodyMedium,
            color = WARNING,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        if (!requesting) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = WARNING,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

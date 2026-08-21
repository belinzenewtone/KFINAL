package com.belinze.lifeos.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SmsImportHealthViewModel
import com.lifeos.sms.SmsService
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// SmsImportHealthScreen — revamped for clarity and reduced clutter
// ─────────────────────────────────────────────────────────────────────────────

private val WARNING_COLOR = Color(0xFFF5CB5C)
private val FULIZA_COLOR  = Color(0xFFFB923C)

@Composable
fun SmsImportHealthScreen(
    navController: NavHostController,
    viewModel:     SmsImportHealthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var isRepairingDb       by remember { mutableStateOf(false) }
    var showClearLogConfirm by remember { mutableStateOf(false) }
    var showDiagnostics     by remember { mutableStateOf(false) }

    // Auto-refresh every 30 s
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); viewModel.load() }
    }

    // BUG-S7: re-load on every lifecycle resume so battery-exemption status
    // updates immediately after the user returns from the system dialog.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Action result dialog
    var showResultDialog    by remember { mutableStateOf(false) }
    var resultDialogTitle   by remember { mutableStateOf("") }
    var resultDialogMessage by remember { mutableStateOf("") }
    LaunchedEffect(state.resultMessage) {
        val msg = state.resultMessage ?: return@LaunchedEffect
        val (title, body) = when {
            msg.startsWith("Reconcile")         -> "Reconcile complete"  to msg.removePrefix("Reconcile complete — ")
            msg.startsWith("Retry")             -> "Retry complete"      to msg.removePrefix("Retry complete — ")
            msg.startsWith("Database repaired") -> "Database repaired"   to "The integrity check is now passing."
            msg.startsWith("Still corrupted")   -> "Still corrupted"     to msg.removePrefix("Still corrupted — ")
            else                                -> "Done"                to msg
        }
        resultDialogTitle = title; resultDialogMessage = body; showResultDialog = true
    }
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title            = { Text(resultDialogTitle) },
            text             = { Text(resultDialogMessage) },
            confirmButton    = { TextButton(onClick = { showResultDialog = false }) { Text("OK") } },
        )
    }
    if (showClearLogConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogConfirm = false },
            title = { Text("Clear import log?") },
            text  = { Text("All visible log entries will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showClearLogConfirm = false; viewModel.clearImportLog() }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearLogConfirm = false }) { Text("Cancel") } },
        )
    }

    PageScaffold(
        title      = "SMS Import Health",
        onBack     = { navController.popBackStack() },
        scrollable = false,
        actions    = {
            IconButton(onClick = { viewModel.load() }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(20.dp),
                    tint = if (state.isLoading) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
    ) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh    = { viewModel.load() },
            modifier     = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(vertical = Spacing.base),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // ─ 1. Receiver Status + quick actions ─────────────────────────
                item {
                    GlassCard {
                        // Status row
                        val (statusLabel, statusColor) = when (state.receiverStatus) {
                            SmsImportHealthViewModel.ReceiverStatus.Active   -> "Active"   to MaterialTheme.colorScheme.primary
                            SmsImportHealthViewModel.ReceiverStatus.Disabled -> "Disabled" to MaterialTheme.colorScheme.error
                            SmsImportHealthViewModel.ReceiverStatus.Idle     -> "Idle"     to MaterialTheme.colorScheme.outline
                            SmsImportHealthViewModel.ReceiverStatus.Unknown  -> "Waiting"  to WARNING_COLOR
                        }
                        val lastFireLabel = state.lastFireMs
                            ?.let { formatTimestamp(Instant.ofEpochMilli(it).toString()) } ?: "Never"

                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            // Status indicator
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(statusColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(12.dp).clip(CircleShape)
                                        .background(statusColor)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                ) {
                                    Text(
                                        "Realtime Receiver",
                                        style      = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Surface(
                                        color = statusColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(50),
                                    ) {
                                        Text(
                                            statusLabel,
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = statusColor,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                Text(
                                    "Last fired: $lastFireLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Battery warning
                        if (state.isReceiverEnabled && !state.batteryExempt) {
                            Spacer(Modifier.height(Spacing.sm))
                            InlineWarning(
                                icon    = Icons.Outlined.BatteryAlert,
                                text    = "Battery optimization is on — tap to exempt",
                                color   = WARNING_COLOR,
                                onClick = { viewModel.requestBatteryExemption() },
                            )
                        }

                        // DB integrity warning
                        if (state.hasDbIntegrityIssue) {
                            Spacer(Modifier.height(Spacing.sm))
                            InlineWarning(
                                icon    = Icons.Outlined.Warning,
                                text    = "DB integrity check failed" + (state.dbIntegrityMessage?.let { " — $it" } ?: ""),
                                color   = MaterialTheme.colorScheme.error,
                                loading = isRepairingDb,
                                loadingLabel = "Repairing…",
                                onClick = { isRepairingDb = true; viewModel.repairDb() },
                            )
                        }
                        LaunchedEffect(state.resultMessage) {
                            if (state.resultMessage != null) isRepairingDb = false
                        }

                        // Ingest queue warning
                        if (state.pendingQueueCount > 0 || state.failedQueueCount > 0) {
                            Spacer(Modifier.height(Spacing.sm))
                            val queueColor = if (state.failedQueueCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                WARNING_COLOR
                            }
                            val qLabel = buildString {
                                append("${state.pendingQueueCount} queued")
                                if (state.failedQueueCount > 0) append(" · ${state.failedQueueCount} failed")
                            }
                            InlineWarning(
                                icon    = Icons.Outlined.Layers,
                                text    = qLabel,
                                color   = queueColor,
                                onClick = { viewModel.retryIngestQueue() },
                            )
                        }

                        Spacer(Modifier.height(Spacing.sm))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(Spacing.sm))

                        // Actions
                        val anyRunning = state.isReconciling || state.isRetrying
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedButton(
                                onClick  = { viewModel.reconcile() },
                                enabled  = !anyRunning,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Sync, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(Spacing.xs))
                                Text(if (state.isReconciling) "Running…" else "Reconcile")
                            }
                            OutlinedButton(
                                onClick  = { viewModel.retryQuarantined() },
                                enabled  = !anyRunning,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(Spacing.xs))
                                Text(
                                    when {
                                        state.isRetrying             -> "Running…"
                                        state.totalQuarantined > 0  -> "Retry (${state.totalQuarantined})"
                                        else                         -> "Retry Queue"
                                    }
                                )
                            }
                        }
                    }
                }

                // ─ 2. Lifetime Stats ──────────────────────────────────────────
                item {
                    GlassCard {
                        CardHeader("Lifetime Stats", Icons.Outlined.Schedule)
                        Spacer(Modifier.height(Spacing.sm))

                        val statsEmpty = state.totalImported == 0 && state.totalDuplicates == 0 &&
                            state.totalQuarantined == 0 && state.totalFailed == 0
                        if (state.isLoading && statsEmpty) {
                            Box(Modifier.fillMaxWidth().padding(Spacing.base), Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            Row(
                                modifier             = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                StatCell("Imported",    state.totalImported,    MaterialTheme.colorScheme.primary)
                                StatCell("Duplicates",  state.totalDuplicates,  MaterialTheme.colorScheme.onSurfaceVariant)
                                StatCell("Quarantined", state.totalQuarantined, WARNING_COLOR)
                                StatCell("Failed",      state.totalFailed,
                                    if (state.totalFailed > 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    })
                            }
                            val totalProcessed = state.totalImported + state.totalDuplicates +
                                state.totalQuarantined + state.totalFailed
                            if (state.totalImported > 0 && totalProcessed > 0) {
                                val rate = (state.totalImported.toDouble() * 100 / totalProcessed).toInt()
                                Spacer(Modifier.height(Spacing.sm))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(Modifier.height(Spacing.xs))
                                Row(
                                    modifier             = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment    = Alignment.CenterVertically,
                                ) {
                                    Text("Success rate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$rate%",
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = when {
                                            rate >= 90 -> MaterialTheme.colorScheme.primary
                                            rate >= 70 -> WARNING_COLOR
                                            else       -> MaterialTheme.colorScheme.error
                                        })
                                }
                                LinearProgressIndicator(
                                    progress = { rate / 100f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color    = when {
                                        rate >= 90 -> MaterialTheme.colorScheme.primary
                                        rate >= 70 -> WARNING_COLOR
                                        else       -> MaterialTheme.colorScheme.error
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Text(
                                    "${state.txCountInDb} transactions in database",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.xs),
                                )
                            }
                        }
                    }
                }

                // ─ 3. Recent Rejections (only when present) ───────────────────
                if (state.rejections.isNotEmpty()) {
                    item {
                        GlassCard {
                            CardHeader("Recent Rejections", Icons.Outlined.Warning)
                            Spacer(Modifier.height(Spacing.xs))
                            state.rejections.take(3).forEachIndexed { i, r ->
                                if (i > 0) {
                                    HorizontalDivider(
                                    modifier = Modifier.padding(vertical = Spacing.xs),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                )
                                }
                                RejectionRow(r)
                            }
                            if (state.rejections.size > 3) {
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    "+${state.rejections.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // ─ 4. Import Log ──────────────────────────────────────────────
                item {
                    GlassCard {
                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment    = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Icon(Icons.Outlined.Schedule, null,
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Text("Import Log",
                                    style      = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold)
                                if (state.audit.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(50),
                                    ) {
                                        Text(
                                            state.audit.size.toString(),
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            if (state.audit.isNotEmpty()) {
                                TextButton(onClick = { showClearLogConfirm = true }) {
                                    Text("Clear",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Spacing.xs),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )

                        val visibleAudit = state.audit
                            .filter { it.id > state.lastClearedAuditId }
                            .take(6)

                        if (state.isLoading && visibleAudit.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = Spacing.base), Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else if (visibleAudit.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = Spacing.xl), Alignment.Center) {
                                Text("No import activity yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            visibleAudit.forEachIndexed { i, entry ->
                                if (i > 0) {
                                    HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 0.dp),
                                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                )
                                }
                                AuditRow(entry)
                            }
                        }
                    }
                }

                // ─ 5. Diagnostics (collapsible) ───────────────────────────────
                item {
                    GlassCard {
                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment    = Alignment.CenterVertically,
                        ) {
                            Text("Diagnostics",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(
                                onClick  = { showDiagnostics = !showDiagnostics },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = if (showDiagnostics) "Collapse" else "Expand",
                                    modifier = Modifier.rotate(if (showDiagnostics) 90f else 0f),
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showDiagnostics,
                            enter   = expandVertically(),
                            exit    = shrinkVertically(),
                        ) {
                            Column {
                                Spacer(Modifier.height(Spacing.sm))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(Modifier.height(Spacing.sm))
                                DiagRow("Native TX count", (state.nativeTxCount ?: "?").toString())
                                DiagRow("Audit count", (state.nativeAuditCount ?: "?").toString())
                                Spacer(Modifier.height(Spacing.xs))
                                Text("DB path",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer {
                                    Text(state.nativeDbPath ?: "unknown",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }
    }
}

// ─── Reusable composables ─────────────────────────────────────────────────────

@Composable
private fun CardHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(icon, contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint     = MaterialTheme.colorScheme.primary)
        Text(title,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InlineWarning(
    icon:         androidx.compose.ui.graphics.vector.ImageVector,
    text:         String,
    color:        Color,
    loading:      Boolean = false,
    loadingLabel: String  = "Running…",
    onClick:      () -> Unit,
) {
    OutlinedButton(
        onClick  = onClick,
        enabled  = !loading,
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border   = ButtonDefaults.outlinedButtonBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(color)),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = color)
            Spacer(Modifier.width(Spacing.xs))
            Text(loadingLabel, style = MaterialTheme.typography.bodySmall)
        } else {
            Icon(icon, null, Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = color)
        Text(label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp)
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier             = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RejectionRow(r: SmsService.RejectionEntry) {
    Row(
        modifier             = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment    = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier.padding(top = 4.dp)
                .size(8.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(rejectionReasonLabel(r.reason),
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.error,
                maxLines   = 1)
            Text(r.rawSms,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2)
        }
        Text(formatTimestamp(Instant.ofEpochMilli(r.timestampMs).toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun AuditRow(entry: SmsService.AuditEntry) {
    // Capture cross-module properties as local vals for smart-cast compatibility
    val amount        = entry.amount
    val failureReason = entry.failureReason
    val mpesaCode     = entry.mpesaCode
    val merchant      = entry.merchant
    val outcome       = entry.outcome

    val dotColor     = outcomeColor(outcome)
    val badgeLabel   = outcomeLabelShort(outcome)
    val timeLabel    = formatTimestamp(
        auditDisplayTimestamp(entry.createdAt, entry.smsDate, outcome)
    )
    val sub = buildString {
        if (!mpesaCode.isNullOrBlank()) append(mpesaCode)
        if (amount != null) {
            if (isNotEmpty()) append(" · ")
            append("Ksh ${formatAmount(amount)}")
        }
        if (!failureReason.isNullOrBlank()) {
            if (isNotEmpty()) append(" · ")
            append(failureReason.take(40))
        }
    }

    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // ── Row 1: outcome badge (pill) + timestamp ────────────────────────
        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            Surface(
                color = dotColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    badgeLabel,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = dotColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Text(
                timeLabel,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // ── Row 2: merchant name (primary) ────────────────────────────────
        if (!merchant.isNullOrBlank()) {
            Text(
                merchant,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
            )
        }
        // ── Row 3: ref code · amount · failure reason ─────────────────────
        if (sub.isNotBlank()) {
            Text(
                sub,
                style    = MaterialTheme.typography.bodySmall,
                color    = if (!failureReason.isNullOrBlank()) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun auditDisplayTimestamp(createdAt: String?, smsDate: String?, outcome: String): String? {
    val entry = createdAt ?: return null
    if (!smsDate.isNullOrBlank() &&
        (outcome.startsWith("imported_") || outcome.startsWith("retry_imported"))
    ) {
        return smsDate
    }
    return entry
}

private fun formatTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return "Never"
    return try {
        // Try Instant first (has 'Z' or offset).  Fall back to LocalDateTime
        // for timestamps stored without a timezone designator.
        val instant = try {
            Instant.parse(iso)
        } catch (_: Exception) {
            java.time.LocalDateTime.parse(iso.take(19))
                .atZone(ZoneId.systemDefault()).toInstant()
        }
        val dt    = instant.atZone(ZoneId.systemDefault())
        val today = java.time.LocalDate.now(ZoneId.systemDefault())
        if (dt.toLocalDate() == today) {
            dt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
        } else {
            dt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH))
        }
    } catch (_: Exception) {
        // Last resort: trim to the human-readable portion (first 16 chars)
        iso.take(16)
    }
}

private fun formatAmount(amount: Double) =
    String.format(Locale.ENGLISH, "%,.2f", amount)

@Composable
private fun outcomeColor(outcome: String): Color = when {
    outcome.contains("imported") || outcome.contains("realtime") || outcome.contains("retry") ->
        MaterialTheme.colorScheme.primary
    outcome.contains("failed") || outcome.contains("error") ->
        MaterialTheme.colorScheme.error
    outcome.contains("quarantine") -> WARNING_COLOR
    outcome.contains("duplicate") || outcome.contains("skipped") || outcome.contains("ignored") ->
        MaterialTheme.colorScheme.onSurfaceVariant
    outcome.contains("fuliza") -> FULIZA_COLOR
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun outcomeLabelShort(outcome: String): String = when {
    outcome.startsWith("imported_realtime")  -> "realtime"
    outcome.startsWith("imported_scan")      -> "scan"
    outcome.startsWith("imported_review")    -> "review"
    outcome.startsWith("imported_batch")     -> "batch"
    outcome.startsWith("retry_imported")     -> "retried"
    outcome.startsWith("fuliza_balance")     -> "fuliza"
    outcome.startsWith("duplicate_detected") -> "duplicate"
    outcome.startsWith("quarantined")        -> "quarantined"
    outcome.startsWith("parse_failed")       -> "fail:" + outcome.removePrefix("parse_failed:")
    outcome.startsWith("import_failed")      -> "failed"
    outcome.startsWith("ignored")            -> "ignored"
    outcome.startsWith("dismissed")          -> "dismissed"
    outcome.length > 20                      -> outcome.take(20) + "…"
    else                                     -> outcome
}

private fun rejectionReasonLabel(reason: String): String = when {
    reason.startsWith("parse_exception") -> "Parser exception"
    else -> when (reason) {
        "not_mpesa"         -> "Not an M-Pesa SMS"
        "fuliza_notice"     -> "Fuliza service notice"
        "ambiguous_receipt" -> "Ambiguous receipt"
        "no_code"           -> "No M-Pesa code found"
        "no_amount"         -> "No amount extractable"
        else                -> reason
    }
}

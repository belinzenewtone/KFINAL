package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SmsImportHealthViewModel
import com.lifeos.sms.SmsService

// ─────────────────────────────────────────────────────────────────────────────
// SmsImportHealthScreen — 1:1 with SmsImportHealthScreen.tsx
//
// Sections: Receiver Status | Lifetime Counters | Activity |
//           Actions | Recent Rejections | Diagnostics | Import Log
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SmsImportHealthScreen(
    navController: NavHostController,
    viewModel:     SmsImportHealthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title      = "SMS Import Health",
        onBack     = { navController.popBackStack() },
        scrollable = false,
        actions    = {
            IconButton(
                onClick  = { viewModel.load() },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
            }
        },
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@PageScaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {

            // ─ 1. Receiver Status ─
            item {
                GlassCard {
                    Text("Receiver Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.sm))
                    ReceiverStatusRow(state)

                    // Battery optimization warning
                    if (!state.batteryExempt) {
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedButton(
                            onClick  = { /* Intent to battery optimization settings handled by system */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            border = ButtonDefaults.outlinedButtonBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    MaterialTheme.colorScheme.error),
                            ),
                        ) { Text("⚠ Battery optimization active — may delay SMS") }
                    }

                    // DB integrity
                    if (state.hasDbIntegrityIssue) {
                        Spacer(Modifier.height(Spacing.xs))
                        OutlinedButton(
                            onClick  = { viewModel.reconcile() },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("⚠ DB integrity issue detected — reconcile now") }
                    }

                    // Ingest queue warning
                    if (state.failedQueueCount > 0) {
                        Spacer(Modifier.height(Spacing.xs))
                        OutlinedButton(
                            onClick  = { viewModel.retryQuarantined() },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("⚠ ${state.failedQueueCount} messages failed — retry queue") }
                    }
                }
            }

            // ─ 2. Lifetime Counters ─
            item {
                GlassCard {
                    Text("Lifetime Counters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier             = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        CounterCell("Imported",   state.totalImported,   MaterialTheme.colorScheme.primary)
                        CounterCell("Duplicates", state.totalDuplicates, MaterialTheme.colorScheme.outline)
                        CounterCell("Quarantined", state.totalQuarantined, Color(0xFFF5CB5C))
                        CounterCell("Failed",     state.totalFailed,     MaterialTheme.colorScheme.error)
                    }
                    val total = state.totalImported + state.totalFailed
                    if (total > 0) {
                        Spacer(Modifier.height(Spacing.sm))
                        val rate = (state.totalImported.toDouble() / total * 100).toInt()
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(Spacing.xs))
                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Success rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$rate%",
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (rate >= 80) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ─ 3. Activity ─
            item {
                GlassCard {
                    Text("Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.sm))
                    TimestampRow("Transactions in DB",  state.txCountInDb.toString())
                    TimestampRow("Pending queue",        state.pendingQueueCount.toString())
                    TimestampRow("Failed queue",         state.failedQueueCount.toString())
                    state.oldestPendingAt?.let { TimestampRow("Oldest pending at", it) }
                }
            }

            // ─ 4. Actions ─
            item {
                GlassCard {
                    Text("Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(
                            onClick  = { viewModel.reconcile() },
                            modifier = Modifier.weight(1f),
                        ) { Text("Reconcile") }
                        OutlinedButton(
                            onClick  = { viewModel.retryQuarantined() },
                            modifier = Modifier.weight(1f),
                        ) { Text("Retry Queue") }
                    }
                }
            }

            // ─ 5. Recent Rejections (conditional) ─
            if (state.rejections.isNotEmpty()) {
                item {
                    GlassCard {
                        Text("Recent Rejections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(Spacing.sm))
                        state.rejections.forEach { r ->
                            RejectionRow(r)
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }

            // ─ 6. Diagnostics ─
            item {
                GlassCard {
                    Text("Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Spacing.sm))
                    TimestampRow("Receiver enabled", if (state.isReceiverEnabled) "Yes" else "No")
                    TimestampRow("Battery exempt",   if (state.batteryExempt) "Yes" else "No")
                    TimestampRow("Audit entries",    state.audit.size.toString())
                }
            }

            // ─ 7. Import Log ─
            if (state.audit.isNotEmpty()) {
                item {
                    GlassCard {
                        Row(
                            modifier             = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment    = Alignment.CenterVertically,
                        ) {
                            Text("Import Log",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { viewModel.clearImportLog() }) {
                                Text("Clear",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(Spacing.xs))

                        val visibleAudit = state.audit.filter { it.id > state.lastClearedAuditId }
                        visibleAudit.forEach { entry ->
                            AuditRow(entry)
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

// ─── Receiver Status row ──────────────────────────────────────────────────────

@Composable
private fun ReceiverStatusRow(state: SmsImportHealthViewModel.UiState) {
    val (statusLabel, statusColor) = when (state.receiverStatus) {
        SmsImportHealthViewModel.ReceiverStatus.Active   -> "Active"   to Color(0xFF22C55E)
        SmsImportHealthViewModel.ReceiverStatus.Idle     -> "Idle"     to Color(0xFFF59E0B)
        SmsImportHealthViewModel.ReceiverStatus.Disabled -> "Disabled" to MaterialTheme.colorScheme.error
        SmsImportHealthViewModel.ReceiverStatus.Unknown  -> "Unknown"  to MaterialTheme.colorScheme.outline
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(Modifier.width(Spacing.sm))
        Surface(
            color = statusColor.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text     = statusLabel,
                style    = MaterialTheme.typography.labelSmall,
                color    = statusColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text  = if (state.lastFireMs != null) "Last fired: ${timeAgo(state.lastFireMs)}" else "Never fired",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Counter cell ─────────────────────────────────────────────────────────────

@Composable
private fun CounterCell(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value.toString(),
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = color,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Timestamp row ────────────────────────────────────────────────────────────

@Composable
private fun TimestampRow(label: String, value: String) {
    Row(
        modifier             = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold)
    }
}

// ─── Rejection row ────────────────────────────────────────────────────────────

@Composable
private fun RejectionRow(r: SmsService.RejectionEntry) {
    Column {
        Text(r.reason,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.error)
        Text(r.rawSms.take(100),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2)
        Text(timeAgo(r.timestampMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Audit log row ────────────────────────────────────────────────────────────

@Composable
private fun AuditRow(entry: SmsService.AuditEntry) {
    // Capture cross-module nullable properties as local vals so smart-cast works
    val amount        = entry.amount
    val failureReason = entry.failureReason

    val outcomeColor = when (entry.outcome) {
        "imported", "retry_imported" -> MaterialTheme.colorScheme.primary
        "failed"                     -> MaterialTheme.colorScheme.error
        "quarantine"                 -> Color(0xFFF5CB5C)
        "duplicate"                  -> MaterialTheme.colorScheme.outline
        "fuliza"                     -> Color(0xFFFB923C)
        else                         -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(outcomeColor)
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = entry.merchant ?: entry.outcome,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    modifier   = Modifier.weight(1f),
                )
                if (amount != null) {
                    Text(
                        text  = com.belinze.lifeos.util.formatCurrency(amount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (!failureReason.isNullOrBlank()) {
                Text(
                    text  = failureReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text  = entry.createdAt.take(16).replace("T", " "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Time ago ─────────────────────────────────────────────────────────────────

private fun timeAgo(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    val minutes = diff / 60_000L
    val hours   = minutes / 60L
    val days    = hours / 24L
    return when {
        minutes < 1  -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24   -> "${hours}h ago"
        else         -> "${days}d ago"
    }
}

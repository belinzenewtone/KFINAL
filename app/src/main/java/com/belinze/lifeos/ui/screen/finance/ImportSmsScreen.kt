package com.belinze.lifeos.ui.screen.finance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SmsImportViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ImportSmsSheet — ModalBottomSheet matching RFINAL's ImportSmsSheet component.
//
// Flow (mirrors RFINAL):
//   Step 1 (mode): M-Pesa Only / Banks Only / M-Pesa + Banks
//   Step 2 (period): Last 1 Month / Last 3 Months / Last 6 Months + Back
//   On period tap: dismiss sheet + viewModel.runImport(days, mode)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSmsSheet(
    onDismiss: () -> Unit,
    viewModel: SmsImportViewModel,
) {
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED) {
            viewModel.setPermissionGranted(true)
        }
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setPermissionGranted(granted) }

    ModalBottomSheet(
        onDismissRequest = { selectedMode = null; viewModel.cancelDetection(); onDismiss() },
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor       = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when {
                // ── Banks Detected — confirm before importing ──────────────────
                state.showDetectionResult && state.detectedInstitutions != null && !state.isImporting -> {
                    val detected = state.detectedInstitutions!!
                    val total    = detected.sumOf { it.count }
                    Text(
                        "Banks Detected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                    Text(
                        "Found $total financial messages from:",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(detected, key = { it.institutionId }) { inst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    SmsImportViewModel.institutionLabel(inst.institutionId),
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label   = { Text("${inst.count} msgs", fontSize = 11.sp) },
                                    border  = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled    = true,
                                        borderColor = Color.White.copy(alpha = 0.12f),
                                    ),
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    Text(
                        "Missing banks will be supported in future updates.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        OutlinedButton(
                            onClick  = { viewModel.cancelDetection(); selectedMode = null; onDismiss() },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(20.dp),
                        ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
                        Button(
                            onClick  = { viewModel.confirmBankImport(); selectedMode = null; onDismiss() },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(20.dp),
                        ) { Text("Import All") }
                    }
                }

                // ── Scanning / importing ────────────────────────────────────────
                state.isImporting || state.isDetecting -> {
                    Text(
                        if (state.isDetecting) "Scanning Messages" else "Importing",
                        style    = MaterialTheme.typography.titleMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color    = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            if (state.isDetecting) "Detecting banks in your inbox..." else "Processing messages...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // ── Step 2: period selection ─────────────────────────────────────
                selectedMode != null -> {
                    Text(
                        "Select Time Period",
                        style    = MaterialTheme.typography.titleMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                    Text(
                        "How far back should we scan?",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                    listOf(30L to "Last 1 Month", 90L to "Last 3 Months", 180L to "Last 6 Months").forEach { (days, label) ->
                        OutlinedButton(
                            onClick = {
                                val mode = selectedMode!!
                                if (mode == "mpesa_only") selectedMode = null
                                viewModel.startImportFlow(days, mode)
                                if (mode == "mpesa_only") onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                        ) { Text(label) }
                    }
                    TextButton(
                        onClick  = { selectedMode = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                // ── Step 1: mode selection ────────────────────────────────────────
                else -> {
                    Text(
                        "Import SMS",
                        style    = MaterialTheme.typography.titleMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                    Text(
                        "Choose what to import — you'll pick a time period next.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                    if (!state.permissionGranted) {
                        Button(
                            onClick  = { permLauncher.launch(Manifest.permission.READ_SMS) },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.LockOpen, contentDescription = null)
                            Spacer(Modifier.width(Spacing.xs))
                            Text("Allow SMS Access")
                        }
                    } else {
                        listOf(
                            Triple("mpesa_only", Icons.Outlined.Message,        "M-Pesa Only"),
                            Triple("banks_only", Icons.Outlined.AccountBalance,  "Banks Only"),
                            Triple("all",        Icons.Outlined.SwapHoriz,       "M-Pesa + Banks"),
                        ).forEach { (mode, icon, label) ->
                            OutlinedButton(
                                onClick  = { selectedMode = mode },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(12.dp),
                            ) {
                                Icon(icon, contentDescription = null)
                                Spacer(Modifier.width(Spacing.xs))
                                Text(label)
                            }
                        }
                    }
                }
            }
        }
    }
}

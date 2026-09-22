package com.belinze.lifeos.ui.screen.finance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        onDismissRequest = { selectedMode = null; onDismiss() },
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.x2l),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (selectedMode != null) {
                // Step 2: period selection
                Text(
                    "Select Time Period",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "How far back should we scan?",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
                listOf(30L to "Last 1 Month", 90L to "Last 3 Months", 180L to "Last 6 Months").forEach { (days, label) ->
                    OutlinedButton(
                        onClick = {
                            val mode = selectedMode!!
                            selectedMode = null
                            onDismiss()
                            viewModel.runImport(days, mode)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                    ) { Text(label) }
                }
                TextButton(
                    onClick  = { selectedMode = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                // Step 1: mode selection
                Text(
                    "Import SMS",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Choose what to import — you'll pick a time period next.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
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

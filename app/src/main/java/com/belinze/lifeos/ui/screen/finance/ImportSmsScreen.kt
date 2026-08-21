package com.belinze.lifeos.ui.screen.finance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.GlassCardVariant
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SmsImportViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ImportSmsScreen — 1:1 port of the ImportSmsSheet from FinanceScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

private data class PeriodOption(val label: String, val days: Long?) // null = all

private val PERIODS = listOf(
    PeriodOption("1 week",   7),
    PeriodOption("3 months", 90),
    PeriodOption("6 months", 180),
    PeriodOption("All",      null),
)

private val FILTERS = listOf(
    "all"        to "All messages",
    "mpesa_only" to "M-Pesa only",
    "banks_only" to "Banks only",
)

@Composable
fun ImportSmsScreen(
    navController: NavHostController,
    viewModel:     SmsImportViewModel = hiltViewModel(),
) {
    val state   by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Seed the permission state from the real system grant — ViewModel starts
    // with false by default, which would show the "Allow SMS Access" card even
    // when the user already granted permission during onboarding or a prior visit.
    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) viewModel.setPermissionGranted(true)
    }

    var periodDays by remember { mutableStateOf<Long?>(90) }   // default: 3 months
    var filter     by remember { mutableStateOf("all") }

    // Refresh message count whenever scan window, filter, or permission changes
    LaunchedEffect(periodDays, filter, state.permissionGranted) {
        if (state.permissionGranted) viewModel.previewImport(periodDays, filter)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setPermissionGranted(granted)
    }

    PageScaffold(
        eyebrow  = "Finance",
        title    = "Import SMS",
        subtitle = "Scan messages for transactions",
        onBack   = { navController.popBackStack() },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            // ── Status banner ─────────────────────────────────────────────
            if (state.banner != null) {
                InlineBanner(tone = BannerTone.Info, message = state.banner ?: "")
            }

            // ── Import in progress ────────────────────────────────────────
            if (state.isImporting) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    val prog = state.importProgress
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (prog != null) "Scanned ${prog.imported} of ${prog.total} messages…"
                                else "Scanning your messages…",
                                style      = MaterialTheme.typography.bodyMedium,
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                if (prog != null && prog.quarantined > 0)
                                    "${prog.quarantined} flagged for review · this may take a moment"
                                else "This may take a moment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    if (prog != null && prog.total > 0) {
                        val fraction = (prog.imported.toFloat() / prog.total).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // ── Import complete ───────────────────────────────────────────
            state.importResult?.let { result ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "Import complete",
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    // Summary rows
                    listOf(
                        "Transactions imported" to "${result.imported}",
                        "Duplicates skipped"    to "${result.duplicates}",
                        "Flagged for review"    to "${result.quarantined}",
                        "Failed"                to "${result.failed}".takeIf { result.failed > 0 },
                    ).filterNot { it.second == null }.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value ?: "", style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // ── How it works ──────────────────────────────────────────────
            if (!state.isImporting) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "HOW IT WORKS",
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                        modifier      = Modifier.padding(bottom = Spacing.sm),
                    )
                    listOf(
                        Icons.Outlined.PhoneAndroid to "Messages are read directly on your device",
                        Icons.Outlined.Security     to "Nothing is uploaded — 100% private",
                        Icons.Outlined.CheckCircle  to "Detected transactions are added to Finance",
                    ).forEach { (icon, text) ->
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier.padding(vertical = 3.dp),
                        ) {
                            Icon(icon, contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp))
                            Text(text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // ── Scan window ───────────────────────────────────────────────
            GlassCard(variant = GlassCardVariant.Default, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(bottom = Spacing.sm),
                ) {
                    Icon(Icons.Outlined.Message, contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Text(
                        "Scan window",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "How far back to look for messages",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
                Row(
                    modifier              = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PERIODS.forEach { p ->
                        FilterChip(
                            selected = periodDays == p.days,
                            onClick  = { periodDays = p.days },
                            label    = { Text(p.label) },
                        )
                    }
                }
            }

            // ── Institution filter ────────────────────────────────────────
            GlassCard(variant = GlassCardVariant.Default, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(bottom = Spacing.sm),
                ) {
                    Icon(Icons.Outlined.LockOpen, contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Text(
                        "Message source",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "Filter by institution type",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FILTERS.forEach { (key, label) ->
                        FilterChip(
                            selected = filter == key,
                            onClick  = { filter = key },
                            label    = { Text(label) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── Message preview count ─────────────────────────────────────
            if (state.permissionGranted) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (state.isPreviewing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                "Counting messages…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            val count = state.previewCount
                            Icon(
                                Icons.Outlined.Message,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = when {
                                    count == null -> "Select a window to preview"
                                    count == 0    -> "No messages found in this window"
                                    else          -> "$count message${if (count != 1) "s" else ""} found — ready to scan"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (count != null && count > 0) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // ── Permission + import action ────────────────────────────────
            if (!state.permissionGranted) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "SMS permission needed",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "LifeOS needs permission to read your messages. Messages are processed locally — nothing leaves your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.base))
                    Button(
                        onClick  = { permLauncher.launch(Manifest.permission.READ_SMS) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.LockOpen, contentDescription = null,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        Text("Allow SMS Access")
                    }
                }
            } else {
                Button(
                    onClick  = { viewModel.runImport(periodDays, filter) },
                    enabled  = !state.isImporting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (state.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text("Import running…")
                    } else {
                        Icon(Icons.Outlined.Message, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(Spacing.sm))
                        Text("Start Import", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

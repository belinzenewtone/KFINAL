package com.belinze.lifeos.ui.screen.finance

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.GlassCardVariant
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SmsImportViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ImportSmsScreen — 1:1 port of the ImportSmsSheet from FinanceScreen.tsx
//
// Lets the user pick a scan window (7d / 30d / 90d / all) and an institution
// filter (M-Pesa / Banks / All), then runs the historical SMS import via
// SmsService.importHistoricalSms(fromMs, toMs, filter).
// ─────────────────────────────────────────────────────────────────────────────

private data class PeriodOption(val label: String, val days: Long?) // null = all

private val PERIODS = listOf(
    PeriodOption("7 days", 7),
    PeriodOption("30 days", 30),
    PeriodOption("90 days", 90),
    PeriodOption("All", null),
)

private val FILTERS = listOf(
    "all"        to "All",
    "mpesa_only" to "M-Pesa",
    "banks_only" to "Banks",
)

@Composable
fun ImportSmsScreen(
    navController: NavHostController,
    viewModel:     SmsImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    var periodDays by remember { mutableStateOf<Long?>(30) }
    var filter     by remember { mutableStateOf("all") }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setPermissionGranted(granted)
    }

    PageScaffold(
        eyebrow = "Finance",
        title   = "Import SMS",
        onBack  = { navController.popBackStack() },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (state.banner != null) {
                InlineBanner(tone = BannerTone.Info, message = state.banner ?: "")
            }
            if (state.isImporting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Importing messages…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── Scan window ───────────────────────────────────────────────
            GlassCard(variant = GlassCardVariant.Default, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Scan window", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
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
            }

            // ── Institution filter ────────────────────────────────────────
            GlassCard(variant = GlassCardVariant.Default, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Messages to scan", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FILTERS.forEach { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick  = { filter = key },
                                label    = { Text(label) },
                            )
                        }
                    }
                }
            }

            // ── Permission + import ───────────────────────────────────────
            if (!state.permissionGranted) {
                Button(
                    onClick = { permLauncher.launch(Manifest.permission.READ_SMS) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Allow SMS Access")
                }
            } else {
                Button(
                    onClick = { viewModel.runImport(periodDays, filter) },
                    enabled = !state.isImporting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (state.isImporting) "Import running…" else "Import Messages")
                }
            }

            Text(
                text  = "Messages are read and parsed on-device only — nothing is uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

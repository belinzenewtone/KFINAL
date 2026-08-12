package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.CsvImportViewModel

// ─────────────────────────────────────────────────────────────────────────────
// CsvImportScreen — 1:1 port of CsvImportScreen.tsx
//
// Paste CSV content → auto-detect columns → preview → import.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CsvImportScreen(
    navController: NavHostController,
    viewModel:     CsvImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var csvText by remember { mutableStateOf("") }

    PageScaffold(
        eyebrow = "Planner",
        title   = "CSV Import",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.error != null) {
                InlineBanner(tone = BannerTone.Error, message = state.error ?: "")
            }
            if (state.done) {
                InlineBanner(
                    tone    = BannerTone.Success,
                    message = "Imported ${state.imported} transactions",
                )
            }

            Text("Paste CSV content", fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
            OutlinedTextField(
                value         = csvText,
                onValueChange = { csvText = it },
                placeholder   = { Text("amount,merchant,date,category\n1000,Safaricom,2026-08-01,utilities") },
                minLines      = 6,
                modifier      = Modifier.fillMaxWidth(),
            )

            Button(
                onClick  = { viewModel.parse(csvText) },
                enabled  = csvText.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Parse CSV")
            }

            // ── Preview ───────────────────────────────────────────────────
            if (state.preview.isNotEmpty()) {
                Text("Preview (${state.totalRows} rows)", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = Spacing.md))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        state.preview.take(8).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(row.merchant, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f))
                                Text(formatCurrency(row.amount), style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }

                Button(
                    onClick  = { viewModel.importAll() },
                    enabled  = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Import ${state.totalRows} Transactions")
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

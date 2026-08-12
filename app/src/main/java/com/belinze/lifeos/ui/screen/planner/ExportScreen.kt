package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.belinze.lifeos.viewmodel.ExportViewModel

// ─────────────────────────────────────────────────────────────────────────────
// ExportScreen — 1:1 port of ExportScreen.tsx (JSON export + history)
// ─────────────────────────────────────────────────────────────────────────────

private data class DomainOption(val label: String, val default: Boolean)

@Composable
fun ExportScreen(
    navController: NavHostController,
    viewModel:     ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    var tx by remember { mutableStateOf(true) }
    var tasks by remember { mutableStateOf(true) }
    var events by remember { mutableStateOf(true) }
    var budgets by remember { mutableStateOf(true) }
    var incomes by remember { mutableStateOf(true) }
    var recurring by remember { mutableStateOf(true) }
    var goals by remember { mutableStateOf(true) }

    PageScaffold(
        eyebrow = "Planner",
        title   = "Export",
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
            if (state.lastExport != null) {
                InlineBanner(tone = BannerTone.Success, message = state.lastExport ?: "")
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Include", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                    ExportToggle("Transactions", tx) { tx = it }
                    ExportToggle("Tasks", tasks) { tasks = it }
                    ExportToggle("Events", events) { events = it }
                    ExportToggle("Budgets", budgets) { budgets = it }
                    ExportToggle("Incomes", incomes) { incomes = it }
                    ExportToggle("Recurring rules", recurring) { recurring = it }
                    ExportToggle("Goals", goals) { goals = it }
                }
            }

            Button(
                onClick  = {
                    viewModel.exportJson(tx, tasks, events, budgets, incomes, recurring, goals)
                },
                enabled  = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Export as JSON")
                }
            }

            // ── History ───────────────────────────────────────────────────
            if (state.history.isNotEmpty()) {
                Text("Past exports", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = Spacing.md))
                state.history.take(10).forEach { exp ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(exp.createdAt?.take(19)?.replace("T", " ") ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
                        Text("${exp.recordCount ?: 0} records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.45f))
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun ExportToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

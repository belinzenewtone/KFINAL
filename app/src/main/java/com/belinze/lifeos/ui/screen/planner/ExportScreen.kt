package com.belinze.lifeos.ui.screen.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.ExportFormat
import com.belinze.lifeos.viewmodel.ExportViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class PreviewDomain(val key: String, val label: String, val color: Color)

private val PREVIEW_DOMAINS = listOf(
    PreviewDomain("transactions", "Transactions", Color(0xFF4DB8FF)),
    PreviewDomain("tasks", "Tasks", Color(0xFF34D399)),
    PreviewDomain("events", "Events", Color(0xFFF59E0B)),
    PreviewDomain("budgets", "Budgets", Color(0xFFA78BFA)),
    PreviewDomain("incomes", "Incomes", Color(0xFF22C55E)),
    PreviewDomain("recurring", "Recurring", Color(0xFF06B6D4)),
    PreviewDomain("goals", "Goals", Color(0xFFEC4899)),
)

private val DATE_WINDOWS =
    listOf("week" to "This Week", "month" to "This Month", "last30" to "Last 30 Days", "custom" to "Custom Range", "all" to "All Time")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavHostController,
    viewModel:     ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var format by remember { mutableStateOf(ExportFormat.CSV) }
    var dateWindow by remember { mutableStateOf("all") }
    var customStart by remember { mutableStateOf("") }
    var customEnd by remember { mutableStateOf("") }
    var encryptEnabled by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var selectedDomains by remember { mutableStateOf(PREVIEW_DOMAINS.map { it.key }.toSet()) }

    PageScaffold(
        title = "Export",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (state.error != null) {
            InlineBanner(tone = BannerTone.Error, message = state.error ?: "")
        }
        if (state.lastExport != null) {
            InlineBanner(tone = BannerTone.Success, message = state.lastExport ?: "")
        }

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            GlassCard {
                SectionLabel("FORMAT")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ExportFormat.values().forEach { fmt ->
                        FilterChip(
                            selected = format == fmt,
                            onClick = { format = fmt },
                            label = { Text(fmt.name) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    when (format) {
                        ExportFormat.CSV -> "Transactions only — opens in Excel / Google Sheets"
                        ExportFormat.JSON -> "All data: transactions, tasks, events, budgets"
                        ExportFormat.PDF -> "Formatted document — share or save as PDF"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GlassCard {
                SectionLabel("DATE WINDOW")
                var dateWindowExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dateWindowExpanded,
                    onExpandedChange = { dateWindowExpanded = it },
                ) {
                    OutlinedTextField(
                        value = DATE_WINDOWS.firstOrNull { it.first == dateWindow }?.second ?: "All Time",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date window") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dateWindowExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = dateWindowExpanded,
                        onDismissRequest = { dateWindowExpanded = false },
                    ) {
                        DATE_WINDOWS.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    dateWindow = key
                                    dateWindowExpanded = false
                                },
                            )
                        }
                    }
                }
                // EX-1: custom date range fields
                if (dateWindow == "custom") {
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = customStart,
                            onValueChange = { customStart = it },
                            label = { Text("Start date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = customEnd,
                            onValueChange = { customEnd = it },
                            label = { Text("End date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Encrypt file", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("Protect the export with a passphrase",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = encryptEnabled,
                        onCheckedChange = { encryptEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor    = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor    = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor  = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                if (encryptEnabled) {
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase") },
                        placeholder = { Text("Enter passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            GlassCard {
                SectionLabel("EXPORT PREVIEW")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(PREVIEW_DOMAINS, key = { it.key }) { domain ->
                        val active = if (format == ExportFormat.CSV) {
                            domain.key == "transactions"
                        } else {
                            domain.key in selectedDomains
                        }
                        val locked = format == ExportFormat.CSV && domain.key != "transactions"
                        Column(
                            modifier = Modifier
                                .size(width = 92.dp, height = 92.dp)
                                .background(
                                    if (active) {
                                        domain.color.copy(alpha = 0x52 / 255f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    MaterialTheme.shapes.large,
                                )
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) {
                                        domain.color
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    },
                                    shape = MaterialTheme.shapes.large,
                                )
                                .then(
                                    if (locked) {
                                        Modifier
                                    } else {
                                        Modifier.clickable {
                                        selectedDomains = if (domain.key in selectedDomains) {
                                            selectedDomains - domain.key
                                        } else {
                                            selectedDomains + domain.key
                                        }
                                    }
                                    }
                                )
                                .padding(Spacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        if (active) {
                                            domain.color.copy(alpha = 0x3D / 255f)
                                        } else {
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                        },
                                        MaterialTheme.shapes.medium,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                val domainIcon = when (domain.key) {
                                    "transactions" -> Icons.Outlined.Receipt
                                    "tasks"        -> Icons.Outlined.TaskAlt
                                    "events"       -> Icons.Outlined.CalendarMonth
                                    "budgets"      -> Icons.Outlined.AccountBalanceWallet
                                    "incomes"      -> Icons.Outlined.TrendingUp
                                    "recurring"    -> Icons.Outlined.Repeat
                                    "goals"        -> Icons.Outlined.Flag
                                    else           -> Icons.Outlined.Wallet
                                }
                                Icon(domainIcon, contentDescription = null,
                                    tint = if (active) domain.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp))
                            }
                            // EX-2: item count — shown before label (RFINAL: icon → count → label)
                            val count = state.domainCounts[domain.key]
                            Text(
                                count?.toString() ?: "—",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (active) domain.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(domain.label, style = MaterialTheme.typography.bodySmall,
                                color = if (active) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val pass = if (encryptEnabled) passphrase else ""
                    when (format) {
                        ExportFormat.JSON -> viewModel.exportJson(
                            includeTransactions = "transactions" in selectedDomains,
                            includeTasks        = "tasks"        in selectedDomains,
                            includeEvents       = "events"       in selectedDomains,
                            includeBudgets      = "budgets"      in selectedDomains,
                            includeIncomes      = "incomes"      in selectedDomains,
                            includeRecurring    = "recurring"    in selectedDomains,
                            includeGoals        = "goals"        in selectedDomains,
                            dateWindow          = dateWindow,
                            customStart         = customStart,
                            customEnd           = customEnd,
                            passphrase          = pass,
                        )
                        ExportFormat.CSV -> viewModel.exportCsv(
                            includeTransactions = "transactions" in selectedDomains,
                            includeTasks        = "tasks"        in selectedDomains,
                            includeEvents       = "events"       in selectedDomains,
                            includeBudgets      = "budgets"      in selectedDomains,
                            includeIncomes      = "incomes"      in selectedDomains,
                            includeRecurring    = "recurring"    in selectedDomains,
                            includeGoals        = "goals"        in selectedDomains,
                            dateWindow          = dateWindow,
                            customStart         = customStart,
                            customEnd           = customEnd,
                            passphrase          = pass,
                        )
                        ExportFormat.PDF -> viewModel.exportPdf(
                            includeTransactions = "transactions" in selectedDomains,
                            includeTasks        = "tasks"        in selectedDomains,
                            includeEvents       = "events"       in selectedDomains,
                            includeBudgets      = "budgets"      in selectedDomains,
                            dateWindow          = dateWindow,
                            customStart         = customStart,
                            customEnd           = customEnd,
                            passphrase          = pass,
                        )
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Export ${format.name}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Export History", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                if (state.history.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (state.history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.x2l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    Text("No exports yet", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                state.history.take(10).forEach { exp ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            val fmtColor = when (exp.format?.lowercase()) {
                                "json" -> Color(0xFF22C55E)
                                "pdf"  -> Color(0xFFF59E0B)
                                else   -> MaterialTheme.colorScheme.primary
                            }
                            Box(contentAlignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(fmtColor.copy(alpha = 0x20 / 255f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Description, contentDescription = null,
                                        tint = fmtColor, modifier = Modifier.size(18.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF22C55E), CircleShape),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.filePath ?: "", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Text("${exp.format?.uppercase()} · ${exp.recordCount ?: 0} records · ${formatDateTime(exp.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.sm))
}

private fun formatDateTime(iso: String?): String = try {
    LocalDateTime.parse(iso?.take(19)).format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
} catch (_: Exception) {
    iso?.take(16) ?: ""
}

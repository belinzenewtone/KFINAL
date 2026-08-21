package com.belinze.lifeos.ui.screen.planner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.InlineBanner
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.CsvImportViewModel

private val ALL_FIELDS = listOf(
    "amount" to "Amount *",
    "merchant" to "Merchant *",
    "date" to "Date *",
    "category" to "Category",
    "type" to "Type",
    "status" to "Status",
    "description" to "Description",
)

@Composable
fun CsvImportScreen(
    navController: NavHostController,
    viewModel:     CsvImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val read = { u: Uri ->
                context.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() }
            }
            viewModel.loadFromUri(it, read)
        }
    }

    PageScaffold(
        title = "Import CSV",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        if (state.error != null) {
            InlineBanner(tone = BannerTone.Error, message = state.error ?: "")
        }
        if (state.done) {
            InlineBanner(tone = BannerTone.Success, message = "Imported ${state.imported} transactions")
        }

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Button(
                // BUG #30: "text/csv" hides most files on Android (file manager
                // shows only CSV). Use "*/*" so the picker lists all file types;
                // the ViewModel validates the CSV content after reading.
                onClick = { picker.launch("*/*") },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Pick CSV file")
                }
            }

            if (state.headers.isNotEmpty()) {
                Text("Column mapping", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)

                ALL_FIELDS.forEach { (field, label) ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                        Text(label, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.xs))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            item {
                                FilterChip(
                                    selected = when (field) {
                                        "amount" -> state.mapping.amount.isBlank()
                                        "merchant" -> state.mapping.merchant.isBlank()
                                        "date" -> state.mapping.date.isBlank()
                                        "category" -> state.mapping.category.isBlank()
                                        "type" -> state.mapping.type.isBlank()
                                        "status" -> state.mapping.status.isBlank()
                                        else -> state.mapping.description.isBlank()
                                    },
                                    onClick = { viewModel.updateMapping(field, "") },
                                    label = { Text("None") },
                                )
                            }
                            items(state.headers, key = { it }) { header ->
                                FilterChip(
                                    selected = when (field) {
                                        "amount" -> state.mapping.amount == header
                                        "merchant" -> state.mapping.merchant == header
                                        "date" -> state.mapping.date == header
                                        "category" -> state.mapping.category == header
                                        "type" -> state.mapping.type == header
                                        "status" -> state.mapping.status == header
                                        else -> state.mapping.description == header
                                    },
                                    onClick = { viewModel.updateMapping(field, header) },
                                    label = { Text(header) },
                                )
                            }
                        }
                    }
                }

                Text(
                    "Preview (${state.valid.size} valid, ${state.invalid.size} invalid)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Spacing.base),
                )

                state.valid.take(5).forEach { row ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(row.merchant, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f), maxLines = 1)
                            Text(formatCurrency(row.amount), style = MaterialTheme.typography.bodyMedium,
                                color = if (row.type == "income") Color(0xFF34D399) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold)
                        }
                        Text("${row.category} · ${row.date.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }

                state.invalid.take(3).forEach { row ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
                        Text(row.errors.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                        Text("${row.merchant.ifBlank { "(no merchant)" }} · ${row.amount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }

                Button(
                    onClick = { viewModel.importValid() },
                    enabled = state.valid.isNotEmpty() && !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Import ${state.valid.size} transactions")
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

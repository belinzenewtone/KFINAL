package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.SmsImportHealthViewModel

// ─────────────────────────────────────────────────────────────────────────────
// SmsImportHealthScreen — recent SMS import activity
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SmsImportHealthScreen(
    navController: NavHostController,
    viewModel:     SmsImportHealthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    PageScaffold(
        eyebrow = "Settings",
        title   = "SMS Import Health",
        onBack  = { navController.popBackStack() },
        scrollable = false,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Summary ───────────────────────────────────────────────────
            item {
                FrostCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.pendingQueue.toString(),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground)
                            Text("Queued", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.rejections.size.toString(),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444))
                            Text("Rejected", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.auditCount.toString(),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Audited", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                        }
                    }
                }
            }

            // ── Recent audit entries ──────────────────────────────────────
            if (state.audit.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                        contentAlignment = Alignment.Center) {
                        Text("No import activity yet.",
                            color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                    }
                }
            } else {
                item {
                    Text("Recent activity", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = Spacing.sm))
                }
                items(state.audit, key = { it.id }) { entry ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.merchant ?: entry.mpesaCode ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground)
                            Text(entry.createdAt?.take(19)?.replace("T", " ") ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.45f))
                        }
                        Text(
                            text  = entry.outcome ?: "—",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (entry.outcome) {
                                "imported", "completed" -> Color(0xFF10B981)
                                "rejected", "failed"    -> Color(0xFFEF4444)
                                else -> MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }

            // ── Quarantined queue ─────────────────────────────────────────
            if (state.quarantined.isNotEmpty()) {
                item {
                    Text("Quarantined", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm))
                }
                items(state.quarantined, key = { it.id }) { q ->
                    Text(
                        text  = q.body?.take(80) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.70f),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

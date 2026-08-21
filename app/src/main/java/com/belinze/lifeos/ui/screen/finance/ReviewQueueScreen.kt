package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.ReviewQueueViewModel
import com.lifeos.sms.SmsService

// ─────────────────────────────────────────────────────────────────────────────
// ReviewQueueScreen — 1:1 with ReviewQueueScreen.tsx
//
// Data source: SmsService audit log (quarantined / review / pending entries)
// NOT TransactionViewModel.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReviewQueueScreen(
    navController: NavHostController,
    viewModel:     ReviewQueueViewModel = hiltViewModel(),
) {
    val state   by viewModel.uiState.collectAsState()
    val visible  = viewModel.visibleEntries

    // Auto-dismiss banner after 3500ms
    val banner = state.banner
    LaunchedEffect(banner) {
        if (banner != null) {
            kotlinx.coroutines.delay(3_500)
            viewModel.clearBanner()
        }
    }

    val subtitle = when {
        state.isLoading -> null
        visible.isEmpty() -> "All clear"
        else -> "${visible.size} pending"
    }

    PageScaffold(
        title      = "Review Queue",
        subtitle   = subtitle,
        onBack     = { navController.popBackStack() },
        scrollable = false,
        topBanner  = {
            TopBanner(
                visible       = banner != null,
                message       = banner?.message ?: "",
                tone          = if (banner?.isSuccess == true) BannerTone.Success else BannerTone.Error,
                onDismiss     = { viewModel.clearBanner() },
                autoDismissMs = 3500,
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                visible.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(Spacing.base))
                            Text(
                                "Queue clear",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "No transactions waiting for review",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(top = Spacing.xs, bottom = Spacing.bottomNavSafeArea),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        // ─ Bulk actions card ─
                        item {
                            GlassCard {
                                Row(
                                    modifier             = Modifier.fillMaxWidth(),
                                    verticalAlignment    = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                ) {
                                    Icon(
                                        Icons.Outlined.HourglassEmpty,
                                        contentDescription = null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        "${visible.size} transaction${if (visible.size != 1) "s" else ""} waiting for review",
                                        style    = MaterialTheme.typography.bodyMedium,
                                        color    = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(Modifier.height(Spacing.sm))
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    Button(
                                        onClick  = { viewModel.recoverAll() },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Recover all") }
                                    OutlinedButton(
                                        onClick  = { viewModel.dismissAll() },
                                        modifier = Modifier.weight(1f),
                                        colors   = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                        border = ButtonDefaults.outlinedButtonBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                MaterialTheme.colorScheme.error),
                                        ),
                                    ) { Text("Dismiss all") }
                                }
                            }
                        }

                        // ─ Entry cards ─
                        items(visible, key = { it.id }) { entry ->
                            EntryCard(
                                entry    = entry,
                                onRecover = { viewModel.recoverEntry(entry) },
                                onDismiss = { viewModel.dismissEntry(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Entry card ───────────────────────────────────────────────────────────────

@Composable
private fun EntryCard(
    entry:     SmsService.AuditEntry,
    onRecover: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Capture cross-module nullable properties as local vals so smart-cast works
    val amount        = entry.amount
    val merchant      = entry.merchant
    val failureReason = entry.failureReason
    val mpesaCode     = entry.mpesaCode
    val confidence    = entry.confidence

    GlassCard {
        // Outcome chip + amount
        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            OutcomeChip(entry.outcome)
            if (amount != null) {
                Text(
                    text       = formatCurrency(amount),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xs))

        // Merchant
        if (!merchant.isNullOrBlank()) {
            Text(
                text       = merchant,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Raw SMS snippet
        if (entry.rawMessage.isNotBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text     = entry.rawMessage.take(140),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }

        // "Already in your ledger" note for review entries
        if (entry.outcome.contains("review")) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text  = "Already in your ledger",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF5CB5C),
            )
        }

        // Failure reason
        if (!failureReason.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text  = failureReason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // Meta: mpesaCode + confidence
        if (!mpesaCode.isNullOrBlank() || !confidence.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (!mpesaCode.isNullOrBlank()) {
                    Text(
                        mpesaCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!confidence.isNullOrBlank()) {
                    Text("·", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$confidence confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val recoverLabel = if (entry.outcome.contains("review")) "Approve" else "Recover"
            Button(onClick = onRecover, modifier = Modifier.weight(1f)) {
                Text(recoverLabel)
            }
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = ButtonDefaults.outlinedButtonBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error),
                ),
            ) { Text("Dismiss") }
        }
    }
}

// ─── Outcome chip ─────────────────────────────────────────────────────────────

@Composable
private fun OutcomeChip(outcome: String) {
    val (label, color) = when {
        outcome.contains("quarantin") -> "Quarantined" to MaterialTheme.colorScheme.error
        outcome.contains("review")    -> "Review"      to Color(0xFFF5CB5C)
        outcome == "batch_pending"    -> "Pending"     to MaterialTheme.colorScheme.primary
        else                          -> "Pending"     to MaterialTheme.colorScheme.primary
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            color      = color,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

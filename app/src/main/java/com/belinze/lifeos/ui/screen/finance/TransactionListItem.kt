package com.belinze.lifeos.ui.screen.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belinze.lifeos.data.db.entity.TransactionEntity
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// TransactionListItem
//
// Matches the RN TransactionListItem component exactly:
//   ‣ Left: coloured circle icon (arrow up/down/swap based on type)
//   ‣ Middle: merchant name (bold) + category chip + time
//   ‣ Right: signed amount + status label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TransactionListItem(
    tx:       TransactionEntity,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = MaterialTheme.colorScheme.primary.copy(0.12f)),
                onClick           = onClick,
            )
            .padding(horizontal = Spacing.screenHorizontal, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Icon ────────────────────────────────────────────────────────────
        val txType = tx.transactionType ?: "expense"
        Box(
            modifier         = Modifier
                .size(44.dp)
                .background(txIconBg(txType), MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = txIcon(txType),
                contentDescription = txType,
                tint               = txIconTint(txType),
                modifier           = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(Spacing.base))

        // ── Main text (merchant + category + description) ────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = tx.merchant?.ifBlank { null } ?: tx.description?.take(40) ?: "Unknown",
                style    = MaterialTheme.typography.bodyLarge,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tx.category?.isNotBlank() == true) {
                Text(
                    text     = tx.category ?: "",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (!tx.description.isNullOrBlank() && tx.merchant?.isNotBlank() == true) {
                Text(
                    text     = tx.description ?: "",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.width(Spacing.base))

        // ── Amount + date + status ───────────────────────────────────────
        Column(horizontalAlignment = Alignment.End) {
            val isIncome   = txType == "income"
            val isTransfer = txType == "transfer"
            val isFuliza   = txType == "fuliza"
            val prefix = when {
                isIncome             -> "+"
                isTransfer || isFuliza -> ""
                else                 -> "-"
            }
            val amountColor = when {
                isIncome             -> MaterialTheme.colorScheme.primary
                isTransfer || isFuliza -> MaterialTheme.colorScheme.onSurface
                else                 -> MaterialTheme.colorScheme.error
            }
            Text(
                text  = "$prefix${formatCurrency(tx.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
            )
            Text(
                text  = isoToDate(tx.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tx.status != "completed" && tx.status.isNotBlank()) {
                Text(
                    text  = tx.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(tx.status),
                )
            }
        }
    }
}

// ─── Day-group header ─────────────────────────────────────────────────────────

@Composable
fun DayGroupHeader(
    dateLabel: String,
    total:     Double? = null,   // null = hide the right-side total (used with Paging 3)
    modifier:  Modifier = Modifier,
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, top = Spacing.base, bottom = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text      = dateLabel,
            fontSize  = 11.sp,
            fontWeight = FontWeight.Medium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
        )
        if (total != null) {
            Text(
                text       = "-${formatCurrency(total)}",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Category chip ────────────────────────────────────────────────────────────

@Composable
fun CategoryChip(category: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text      = category,
            fontSize  = 10.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines  = 1,
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun txIconBg(type: String): Color {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        "income"   -> Color(0xFF10B981).copy(alpha = 0.15f)
        "transfer" -> scheme.primary.copy(alpha = 0.12f)
        "fuliza"   -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        else       -> scheme.surfaceVariant
    }
}

@Composable
private fun txIconTint(type: String): Color {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        "income"   -> Color(0xFF10B981)
        "transfer" -> scheme.primary
        "fuliza"   -> Color(0xFFF59E0B)
        else       -> scheme.onSurfaceVariant
    }
}

private val DATE_FMT = DateTimeFormatter.ofPattern("dd MMM")

private fun isoToDate(iso: String?): String {
    if (iso == null) return ""
    return try {
        LocalDateTime.parse(iso.take(19)).toLocalDate().format(DATE_FMT)
    } catch (_: Exception) {
        ""
    }
}

private fun txIcon(type: String) = when (type) {
    "income"   -> Icons.Outlined.ArrowDownward
    "transfer" -> Icons.Outlined.SwapHoriz
    else       -> Icons.Outlined.ArrowUpward
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    "pending"   -> Color(0xFFF59E0B)
    "failed"    -> Color(0xFFEF4444)
    "reversed"  -> MaterialTheme.colorScheme.onSurfaceVariant
    else        -> MaterialTheme.colorScheme.onSurfaceVariant
}

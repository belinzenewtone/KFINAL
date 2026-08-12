package com.belinze.lifeos.ui.screen.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.DayBar
import com.belinze.lifeos.viewmodel.WeekReviewViewModel

// ─────────────────────────────────────────────────────────────────────────────
// WeekReviewScreen — 1:1 with WeekReviewScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

private val COLOR_NORMAL = Color(0xFF22C55E)
private val COLOR_HIGH   = Color(0xFFF59E0B)
private val COLOR_PEAK   = Color(0xFFEF4444)

@Composable
fun WeekReviewScreen(
    navController: NavHostController,
    viewModel:     WeekReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val pref  by viewModel.prefState.collectAsState()

    val firstName = pref.profileName.split(" ").firstOrNull()?.ifBlank { null } ?: "there"
    val greeting  = "${state.greeting}, $firstName"

    LaunchedEffect(Unit) { viewModel.load() }

    PageScaffold(
        title      = "Weekly Review",
        onBack     = { navController.popBackStack() },
        scrollable = false,
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(Spacing.base))
                        Text("Building your review…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(Spacing.screenHorizontal),
                    contentAlignment = Alignment.Center) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = Spacing.screenHorizontal,
                        vertical   = Spacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.base),
                ) {
                    // ─ Week label ─
                    item {
                        Text(
                            text  = state.weekLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // ─ Greeting ─
                    item {
                        Text(
                            text  = greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    // ─ Health Score hero ─
                    item { HealthScoreCard(state.healthScore, state.scoreLabel, Color(state.scoreColor)) }

                    // ─ 7-day Spend Pattern ─
                    item { SpendPatternCard(state.dayBars) }

                    // ─ What Changed? ─
                    if (state.changeItems.isNotEmpty()) {
                        item {
                            GlassCard {
                                Text(
                                    text       = "What Changed?",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                state.changeItems.forEachIndexed { i, item ->
                                    if (i > 0) Spacer(Modifier.height(Spacing.sm))
                                    ChangeItemRow(item.icon, item.text, item.sentiment)
                                }
                            }
                        }
                    }

                    // ─ Spending ─
                    item {
                        GlassCard {
                            Text("Spending",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text  = formatCurrency(state.weekSpend),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (state.topCategory.isNotBlank()) {
                                Text(
                                    text  = "Top: ${state.topCategory.replaceFirstChar { it.uppercaseChar() }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // ─ Tasks ─
                    item {
                        GlassCard {
                            Text("Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(Spacing.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text  = state.tasksCompleted.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = COLOR_NORMAL,
                                    )
                                    Text("Done",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                VerticalDivider(
                                    modifier  = Modifier
                                        .height(48.dp)
                                        .align(Alignment.CenterVertically),
                                    thickness = 1.dp,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val pendColor = if (state.tasksPending > 5) COLOR_HIGH else MaterialTheme.colorScheme.onSurface
                                    Text(
                                        text  = state.tasksPending.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = pendColor,
                                    )
                                    Text("Pending",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                }
            }
        }
    }
}

// ─── Health Score Hero ────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(score: Int, label: String, color: Color) {
    GlassCard {
        Column(
            modifier              = Modifier.fillMaxWidth(),
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Text(
                text       = "Financial Health Score",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(Spacing.lg))

            Box(
                modifier        = Modifier
                    .size(100.dp)
                    .border(4.dp, color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = score.toString(),
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color      = color,
                        lineHeight = 40.sp,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            Text(
                text  = label,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text  = when {
                    score >= 80 -> "You're doing great this week!"
                    score >= 60 -> "On track — keep it up"
                    score >= 40 -> "Room to improve"
                    else        -> "Let's turn things around"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── 7-Day Spend Pattern ──────────────────────────────────────────────────────

@Composable
private fun SpendPatternCard(dayBars: List<DayBar>) {
    var selectedBar by remember { mutableStateOf<DayBar?>(null) }
    val maxAmount = dayBars.maxOfOrNull { it.amount }.takeIf { it != null && it > 0.0 } ?: 1.0

    GlassCard {
        Text(
            text       = "7-Day Spend Pattern",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.base))

        // Tooltip
        selectedBar?.let { bar ->
            Text(
                text  = formatCurrency(bar.amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
        }

        // Bars row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.Bottom,
        ) {
            dayBars.forEach { bar ->
                val fraction  = (bar.amount / maxAmount).coerceIn(0.0, 1.0).toFloat()
                val barColor  = when {
                    bar.isFuture || bar.amount == 0.0 ->
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    bar.amount > bar.avg * 1.5         -> COLOR_PEAK
                    bar.amount > bar.avg               -> COLOR_HIGH
                    else                               -> COLOR_NORMAL
                }
                val isSelected = selectedBar == bar

                Column(
                    modifier              = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                        ) { selectedBar = if (isSelected) null else bar },
                    horizontalAlignment   = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier        = Modifier
                            .height(80.dp)
                            .width(18.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // Track
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))
                        )
                        // Fill
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barColor)
                                    .then(
                                        if (isSelected) Modifier.border(1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            RoundedCornerShape(3.dp))
                                        else Modifier
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = dowLabel(bar.dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
            listOf("Normal" to COLOR_NORMAL, "High" to COLOR_HIGH, "Peak" to COLOR_PEAK)
                .forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(3.dp))
                        Text(label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp)
                    }
                }
            Spacer(Modifier.weight(1f))
            Text(
                text  = "Tap bar for details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 9.sp,
            )
        }
    }
}

private fun dowLabel(dow: Int) = when (dow) {
    1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"
    5 -> "Fri"; 6 -> "Sat"; else -> "Sun"
}

// ─── Change Item ──────────────────────────────────────────────────────────────

@Composable
private fun ChangeItemRow(icon: String, text: String, sentiment: String) {
    val color = when (sentiment) {
        "good"    -> COLOR_NORMAL
        "warn"    -> COLOR_HIGH
        else      -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val imageVector: ImageVector = when (icon) {
        "trending-down-outline"    -> Icons.Filled.TrendingDown
        "trending-up-outline"      -> Icons.Filled.TrendingUp
        "warning-outline"          -> Icons.Filled.Warning
        "alert-circle-outline"     -> Icons.Filled.Error
        "checkmark-circle-outline" -> Icons.Filled.CheckCircle
        else                       -> Icons.Filled.BarChart
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

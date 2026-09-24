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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.DayBar
import com.belinze.lifeos.viewmodel.WeekReviewViewModel
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// WeekReviewScreen — 1:1 with WeekReviewScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

private val COLOR_NORMAL = Color(0xFF22C55E)
private val COLOR_HIGH   = Color(0xFFF59E0B)
private val COLOR_PEAK   = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekReviewScreen(
    navController: NavHostController,
    viewModel:     WeekReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pref  by viewModel.prefState.collectAsStateWithLifecycle()

    val firstName = pref.profileName.split(" ").firstOrNull()?.ifBlank { null } ?: "there"
    val greeting  = "${state.greeting}, $firstName"

    var isPullingRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) isPullingRefresh = false
    }

    PageScaffold(
        title      = "Weekly Review",
        onBack     = { navController.popBackStack() },
        scrollable = false,
    ) {
        when {
            state.isLoading && !isPullingRefresh -> {
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
                PullToRefreshBox(
                    isRefreshing = isPullingRefresh,
                    onRefresh    = { isPullingRefresh = true; viewModel.load() },
                    modifier     = Modifier.fillMaxSize(),
                ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start  = Spacing.screenHorizontal,
                        end    = Spacing.screenHorizontal,
                        top    = Spacing.sm,
                        bottom = Spacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // ─ Week label ─
                    item {
                        Text(
                            text  = state.weekLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // ─ Greeting ─
                    item {
                        Text(
                            text  = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
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
                                SectionEyebrow("What Changed?", modifier = Modifier.padding(bottom = Spacing.sm))
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
                            SectionEyebrow("Spending", modifier = Modifier.padding(bottom = Spacing.sm))
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(
                                    "KSh",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                                Text(
                                    text  = String.format(java.util.Locale.US, "%,d", state.weekSpend.roundToInt()),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            if (state.topCategory.isNotBlank()) {
                                Text(
                                    text  = "Top category: ${state.topCategory.replaceFirstChar { it.uppercaseChar() }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.xs),
                                )
                            } else {
                                Text(
                                    text  = "No spend recorded yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = Spacing.xs),
                                )
                            }
                        }
                    }

                    // ─ Tasks ─
                    item {
                        GlassCard {
                            SectionEyebrow("Tasks", modifier = Modifier.padding(bottom = Spacing.base))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text  = state.tasksCompleted.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = COLOR_NORMAL,
                                    )
                                    Text("Done",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                VerticalDivider(
                                    modifier  = Modifier
                                        .height(32.dp)
                                        .align(Alignment.CenterVertically),
                                    thickness = 1.dp,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val pendColor = if (state.tasksPending > 5) COLOR_HIGH else MaterialTheme.colorScheme.onSurface
                                    Text(
                                        text  = state.tasksPending.toString(),
                                        style = MaterialTheme.typography.titleLarge,
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
                } // PullToRefreshBox
            }
        }
    }
}

// ─── Section eyebrow label — matches the uppercase, letter-spaced labelMedium
// section headers used throughout WeekReviewScreen.tsx ("7-DAY SPEND PATTERN",
// "WHAT CHANGED?", "SPENDING", "TASKS"). ─────────────────────────────────────

@Composable
private fun SectionEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        style         = MaterialTheme.typography.labelMedium,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier      = modifier,
    )
}

// ─── Health Score Hero ────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(score: Int, label: String, color: Color) {
    GlassCard {
        Column(
            modifier              = Modifier.fillMaxWidth().padding(vertical = Spacing.base),
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier        = Modifier
                    .size(80.dp)
                    .border(3.dp, color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = score.toString(),
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color,
                    lineHeight = 34.sp,
                )
            }

            Text(
                text       = label,
                style      = MaterialTheme.typography.titleMedium,
                color      = color,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.padding(top = Spacing.sm),
            )
            Text(
                text      = "Financial Health Score · spend, categorization & tasks",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = Spacing.xs),
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
        SectionEyebrow("7-Day Spend Pattern", modifier = Modifier.padding(bottom = Spacing.base))

        // Bars — each column has a fixed pill-tooltip slot above the bar track.
        // The slot height (28dp) is always reserved, so no bar shifts when selected.
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
                    bar.avg == 0.0                     -> COLOR_NORMAL
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
                            enabled           = bar.amount > 0.0,
                        ) { selectedBar = if (isSelected) null else bar },
                    horizontalAlignment   = Alignment.CenterHorizontally,
                ) {
                    // Bar area — 100dp track plus a headroom row so the amount badge
                    // can float immediately above the bar without clipping (mirrors
                    // React's chartRow = BAR_MAX_HEIGHT + 48).
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth(0.85f)
                            .height(148.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // Track background
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))
                        )
                        // Bar fill — matches React's Math.max(pct * 100dp, amount > 0 ? 2dp : 0)
                        // so a very small nonzero spend day is never rendered invisibly thin.
                        if (bar.amount > 0.0) {
                            val barHeight = (100.dp * fraction).coerceAtLeast(2.dp)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barColor)
                            )
                            // Amount badge floats immediately above the bar top
                            // (React: position absolute, bottom = barH + 4).
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(barHeight + 4.dp))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(barColor)
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text       = formatCurrency(bar.amount, decimals = 0),
                                        fontSize   = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White,
                                        maxLines   = 1,
                                        softWrap   = false,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = dowLabel(bar.dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            barColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
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
                color = MaterialTheme.colorScheme.outline,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
        "trending-down-outline"    -> Icons.Outlined.TrendingDown
        "trending-up-outline"      -> Icons.Outlined.TrendingUp
        "warning-outline"          -> Icons.Outlined.Warning
        "alert-circle-outline"     -> Icons.Outlined.Error
        "checkmark-circle-outline" -> Icons.Outlined.CheckCircle
        else                       -> Icons.Outlined.BarChart
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

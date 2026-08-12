package com.belinze.lifeos.ui.screen.home

// (animation imports reserved for Phase 8 shimmer refinement)
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.FrostCardGlow
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.compactCurrency
import com.belinze.lifeos.viewmodel.TaskViewModel
import com.belinze.lifeos.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
//
// 1:1 port of src/screens/home/HomeScreen.tsx.
//
// Layout (top-to-bottom):
//   ‣ Aurora glow (3 radial rings + linear gradient — pointer-events none)
//   ‣ TopBanner for errors
//   ‣ Header row: "Today" + date + profile button
//   ‣ Focus section: "Daily focus" eyebrow, greeting headline, subtitle
//   ‣ Horizontal metrics row: Today / Week / Month (FrostCard)
//   ‣ HomeMenuCard: 4 quick links
//   ‣ WeeklyResetCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    navController:        NavHostController,
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    taskViewModel:        TaskViewModel        = hiltViewModel(),
) {
    val isDark    = isSystemInDarkTheme()
    val txState   by transactionViewModel.uiState.collectAsState()
    val taskState by taskViewModel.uiState.collectAsState()

    // Derived metrics (computed from available state)
    val monthSpend  = txState.monthTotals?.expense ?: 0.0
    val todayStr    = remember { java.time.LocalDate.now().toString() }
    val weekMonday  = remember { java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString() }
    val todaySpend  = remember(txState.transactions) {
        txState.transactions
            .filter { it.date?.take(10) == todayStr && it.transactionType in listOf("expense", "transfer", "fuliza") }
            .sumOf { it.amount }
    }
    val weekSpend   = remember(txState.transactions) {
        txState.transactions
            .filter {
                val d = it.date?.take(10) ?: ""
                d >= weekMonday && it.transactionType in listOf("expense", "transfer", "fuliza")
            }
            .sumOf { it.amount }
    }

    val greeting  = remember { greeting() }
    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH))
    }

    val bgGrad = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0A0A0B), Color(0xFF0D1117), Color(0xFF0A0A0B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE8EDF3), Color(0xFFDDE4EE), Color(0xFFE8EDF3)))
    }

    Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(brush = bgGrad) }) {

        // ── Aurora glow rings (non-interactive, behind content) ───────────────
        AuroraGlow(isDark = isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // Error banner
            TopBanner(
                visible  = txState.error != null,
                message  = txState.error ?: "",
                tone     = BannerTone.Error,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
                    .padding(bottom = Spacing.bottomNavSafeArea),
            ) {

                // ── Header row ────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text  = "Today",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text  = todayLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                        )
                    }
                    FilledIconButton(
                        onClick = { /* navigate to Profile tab handled by tab switch */ },
                        colors  = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint               = MaterialTheme.colorScheme.onBackground,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                }

                // ── Focus section ─────────────────────────────────────────────
                Column(
                    modifier            = Modifier.padding(bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text       = "Daily focus",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text      = greeting,
                        style     = MaterialTheme.typography.headlineLarge,   // 30sp/700
                        color     = MaterialTheme.colorScheme.onBackground,
                        maxLines  = 2,
                    )
                    Text(
                        text  = "Review priorities, schedule, and your spend trend.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                    )
                }

                // ── Metrics row (horizontal scroll) ───────────────────────────
                if (txState.isLoading) {
                    ShimmerLoadingState(rowCount = 1)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = Spacing.xl),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                    ) {
                        HomeMetricCard(label = "Today",  amount = todaySpend, glow = FrostCardGlow.Blue)
                        HomeMetricCard(label = "Week",   amount = weekSpend,  glow = FrostCardGlow.Teal)
                        HomeMetricCard(label = "Month",  amount = monthSpend, glow = FrostCardGlow.Blue)
                    }
                }

                // ── HomeMenuCard ──────────────────────────────────────────────
                HomeMenuCard(
                    pendingTaskCount = taskState.pendingCount,
                    nextEventTitle   = taskState.upcoming.firstOrNull()?.title,
                    onTasks          = { navController.navigate(Route.TASKS) },
                    onEvents         = { navController.navigate(Route.EVENTS) },
                    onInsights       = { navController.navigate(Route.INSIGHTS) },
                    onSearch         = { navController.navigate(Route.SEARCH) },
                    modifier         = Modifier.padding(bottom = Spacing.xl),
                )

                // ── WeeklyResetCard ───────────────────────────────────────────
                WeeklyResetCard(
                    pendingTaskCount = taskState.pendingCount,
                    onPress          = { navController.navigate(Route.WEEK_REVIEW) },
                )
            }
        }
    }
}

// ─── Aurora glow (pointer-events none in RN) ─────────────────────────────────

@Composable
private fun AuroraGlow(isDark: Boolean) {
    val primary = if (isDark) Color(0xFF57B9FF) else Color(0xFF0369A1)
    val teal    = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0D9488)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
    ) {
        val w = size.width
        // Ring 1 (top-right, primary) — top: -140, right: -90, size: 340
        drawCircle(primary.copy(alpha = if (isDark) 0.08f else 0.05f),
            radius = 170.dp.toPx(), center = Offset(w + 90.dp.toPx() - 170.dp.toPx(), -140.dp.toPx() + 170.dp.toPx()))
        // Ring 2 (top-right, primary) — top: -100, right: -50, size: 250
        drawCircle(primary.copy(alpha = if (isDark) 0.09f else 0.06f),
            radius = 125.dp.toPx(), center = Offset(w + 50.dp.toPx() - 125.dp.toPx(), -100.dp.toPx() + 125.dp.toPx()))
        // Ring 3 (left, teal) — top: 60, left: -120, size: 280
        drawCircle(teal.copy(alpha = if (isDark) 0.05f else 0.04f),
            radius = 140.dp.toPx(), center = Offset(-120.dp.toPx() + 140.dp.toPx(), 60.dp.toPx() + 140.dp.toPx()))
    }
}

// ─── HomeMetricCard ───────────────────────────────────────────────────────────

@Composable
private fun HomeMetricCard(
    label:  String,
    amount: Double,
    glow:   FrostCardGlow,
) {
    FrostCard(glow = glow, modifier = Modifier.width(140.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text       = compactCurrency(amount),
            style      = MaterialTheme.typography.headlineMedium,  // 24sp/700
            color      = MaterialTheme.colorScheme.onBackground,
            maxLines   = 1,
        )
    }
}

// ─── HomeMenuCard ─────────────────────────────────────────────────────────────

@Composable
private fun HomeMenuCard(
    pendingTaskCount: Int,
    nextEventTitle:   String?,
    onTasks:          () -> Unit,
    onEvents:         () -> Unit,
    onInsights:       () -> Unit,
    onSearch:         () -> Unit,
    modifier:         Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            MenuButton(
                label    = "Tasks",
                badge    = if (pendingTaskCount > 0) "$pendingTaskCount" else null,
                icon     = Icons.Filled.TaskAlt,
                onClick  = onTasks,
                modifier = Modifier.weight(1f),
            )
            MenuButton(
                label    = "Events",
                badge    = null,
                icon     = Icons.Filled.CalendarMonth,
                onClick  = onEvents,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            MenuButton(
                label    = "Insights",
                badge    = null,
                icon     = Icons.Filled.Analytics,
                onClick  = onInsights,
                modifier = Modifier.weight(1f),
            )
            MenuButton(
                label    = "Search",
                badge    = null,
                icon     = Icons.Filled.Search,
                onClick  = onSearch,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MenuButton(
    label:   String,
    badge:   String?,
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary           = MaterialTheme.colorScheme.primary
    val isDark            = isSystemInDarkTheme()
    val bgColor           = if (isDark) Color(0xFF161618) else Color(0xFFFFFFFF).copy(alpha = 0.65f)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(72.dp)
            .background(bgColor, MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(0.15f)),
                onClick           = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(primary, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// ─── WeeklyResetCard ─────────────────────────────────────────────────────────

@Composable
private fun WeeklyResetCard(
    pendingTaskCount: Int,
    onPress:          () -> Unit,
    modifier:         Modifier = Modifier,
) {
    val isDark  = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isDark) Color(0xFF0C2340) else Color(0xFFEFF6FF),
                MaterialTheme.shapes.medium,
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = primary.copy(0.15f)),
                onClick           = onPress,
            )
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Week Review",
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text  = if (pendingTaskCount > 0) "$pendingTaskCount tasks pending · tap to review"
                        else "Review your week's progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
            )
        }
        Text("→", color = primary, fontWeight = FontWeight.Bold)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
}

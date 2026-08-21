package com.belinze.lifeos.ui.screen.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.BannerTone
import com.belinze.lifeos.ui.components.FrostCard
import com.belinze.lifeos.ui.components.FrostCardGlow
import com.belinze.lifeos.ui.components.ShimmerLoadingState
import com.belinze.lifeos.ui.components.TopBanner
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.util.formatCurrency
import com.belinze.lifeos.viewmodel.EventViewModel
import com.belinze.lifeos.viewmodel.ProfileViewModel
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
    eventViewModel:       EventViewModel       = hiltViewModel(),
    profileViewModel:     ProfileViewModel     = hiltViewModel(),
) {
    val isDark       = isSystemInDarkTheme()
    val txState      by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val taskState    by taskViewModel.uiState.collectAsStateWithLifecycle()
    val eventState   by eventViewModel.uiState.collectAsStateWithLifecycle()
    val prefState    by profileViewModel.prefState.collectAsStateWithLifecycle()

    // Spend metrics for Today and Week — sourced from ViewModel-computed totals
    // (accurate across all data, not just the current paging window).
    val todayCash = txState.todayExpense
    val weekCash  = txState.weekExpense

    val firstName = remember(prefState.profileName, prefState.profileUsername) {
        val raw = prefState.profileName.trim().ifBlank { prefState.profileUsername.trim() }
        raw.split(" ").firstOrNull { it.isNotBlank() } ?: ""
    }
    val greeting  = remember(firstName) { greeting(firstName) }
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
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text  = todayLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledIconButton(
                        onClick = { navController.navigate(Route.PERSONAL_INFORMATION) },
                        colors  = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Person,
                            contentDescription = "Profile",
                            tint               = MaterialTheme.colorScheme.onSurface,
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
                        color     = MaterialTheme.colorScheme.onSurface,
                        maxLines  = 2,
                    )
                    Text(
                        text  = "Review priorities, schedule, and your spend trend.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ── Metrics row (horizontal scroll) ───────────────────────────
                if (txState.monthTotals == null) {
                    ShimmerLoadingState(rowCount = 1)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.xl),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                    ) {
                        HomeMetricCard(label = "Today's Spend",  amount = todayCash,  glow = FrostCardGlow.Blue,  modifier = Modifier.weight(1f))
                        HomeMetricCard(label = "Week's Spend",   amount = weekCash,   glow = FrostCardGlow.Teal,  modifier = Modifier.weight(1f))
                    }
                }

                // ── HomeMenuCard ──────────────────────────────────────────────
                HomeMenuCard(
                    pendingTaskCount = taskState.pendingCount,
                    nextEventTitle   = eventState.nextEvent?.title,
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

        // Error banner — overlaid so it doesn't shift content
        TopBanner(
            visible  = txState.error != null,
            message  = txState.error ?: "",
            tone     = BannerTone.Error,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars),
        )
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
    label:    String,
    amount:   Double,
    glow:     FrostCardGlow,
    modifier: Modifier = Modifier,
) {
    FrostCard(glow = glow, modifier = modifier) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text       = formatCurrency(amount, decimals = 0),
            style      = MaterialTheme.typography.headlineMedium,  // 24sp/700
            color      = MaterialTheme.colorScheme.onSurface,
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
    FrostCard(
        glow = FrostCardGlow.None,
        modifier = modifier,
        content = {
            MenuRow(
                label = "Tasks",
                value = "$pendingTaskCount pending",
                icon = Icons.Outlined.TaskAlt,
                onClick = onTasks,
            )
            Spacer(Modifier.height(Spacing.base))
            MenuRow(
                label = "Next Event",
                value = nextEventTitle ?: "No event",
                icon = Icons.Outlined.CalendarMonth,
                onClick = onEvents,
            )
            Spacer(Modifier.height(Spacing.base))
            MenuRow(
                label = "Analytics",
                value = "Trends",
                icon = Icons.Outlined.Analytics,
                onClick = onInsights,
            )
            Spacer(Modifier.height(Spacing.base))
            MenuRow(
                label = "Search",
                value = "Explore",
                icon = Icons.Outlined.Search,
                onClick = onSearch,
            )
        },
    )
}

@Composable
private fun MenuRow(
    label:   String,
    value:   String,
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = primary.copy(alpha = 0.15f)),
                onClick = onClick,
            )
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(primary.copy(alpha = 0x20 / 255f), MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Spacing.base))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─── WeeklyResetCard ─────────────────────────────────────────────────────────

@Composable
private fun WeeklyResetCard(
    pendingTaskCount: Int,
    onPress:          () -> Unit,
    modifier:         Modifier = Modifier,
) {
    FrostCard(
        glow = FrostCardGlow.Teal,
        modifier = modifier,
        onClick = onPress,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "Weekly reset",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "Open Weekly Review",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text  = "Clear $pendingTaskCount pending task${if (pendingTaskCount == 1) "" else "s"} before the week closes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun greeting(firstName: String = ""): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val base = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
    return if (firstName.isNotBlank()) "$base, $firstName" else base
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

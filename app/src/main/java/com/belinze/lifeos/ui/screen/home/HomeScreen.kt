package com.belinze.lifeos.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    modifier              = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
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
                            modifier           = Modifier.size(22.dp),
                        )
                    }
                }

                // ── Focus section ─────────────────────────────────────────────
                Column(
                    modifier            = Modifier.padding(bottom = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
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
                        style     = MaterialTheme.typography.headlineMedium,
                        color     = MaterialTheme.colorScheme.onSurface,
                        maxLines  = 2,
                    )
                    Text(
                        text  = "Review priorities, schedule, and your spend trend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ── Content (shimmer while loading) ───────────────────────────
                if (txState.monthTotals == null) {
                    ShimmerLoadingState(rowCount = 3, rowHeight = 88.dp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                    ) {
                        HomeMetricCard(label = "Today",     amount = todayCash, glow = FrostCardGlow.Blue, modifier = Modifier.weight(1f))
                        HomeMetricCard(label = "This Week", amount = weekCash,  glow = FrostCardGlow.Teal, modifier = Modifier.weight(1f))
                    }

                    HomeMenuCard(
                        pendingTaskCount = taskState.pendingCount,
                        nextEventTitle   = eventState.nextEvent?.title,
                        nextEventDate    = eventState.nextEvent?.date,
                        onTasks          = { navController.navigate(Route.TASKS) },
                        onEvents         = { navController.navigate(Route.EVENTS) },
                        onInsights       = { navController.navigate(Route.INSIGHTS) },
                        onSearch         = { navController.navigate(Route.SEARCH) },
                        modifier         = Modifier.padding(bottom = Spacing.lg),
                    )

                    WeeklyResetCard(
                        pendingTaskCount = taskState.pendingCount,
                        onPress          = { navController.navigate(Route.WEEK_REVIEW) },
                    )
                }
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
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text       = formatCurrency(amount, decimals = 0),
            style      = MaterialTheme.typography.titleLarge,
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
    nextEventDate:    String?,
    onTasks:          () -> Unit,
    onEvents:         () -> Unit,
    onInsights:       () -> Unit,
    onSearch:         () -> Unit,
    modifier:         Modifier = Modifier,
) {
    val eventDateLabel = nextEventDate?.let {
        try {
            java.time.LocalDateTime.parse(it.take(19))
                .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
        } catch (_: Exception) {
            try {
                java.time.LocalDate.parse(it.take(10))
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
            } catch (_: Exception) {
                null
            }
        }
    }
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
            Spacer(Modifier.height(Spacing.sm))
            MenuRow(
                label    = "Next Event",
                value    = nextEventTitle ?: "No upcoming",
                subValue = if (nextEventTitle != null) eventDateLabel else null,
                icon     = Icons.Outlined.CalendarMonth,
                onClick  = onEvents,
            )
            Spacer(Modifier.height(Spacing.sm))
            MenuRow(
                label = "Analytics",
                value = "Trends",
                icon = Icons.Outlined.Analytics,
                onClick = onInsights,
            )
            Spacer(Modifier.height(Spacing.sm))
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
    label:    String,
    value:    String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    onClick:  () -> Unit,
    subValue: String? = null,
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
                .size(34.dp)
                .background(primary.copy(alpha = 0x20 / 255f), MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (subValue != null) {
                Text(
                    text  = subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }
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

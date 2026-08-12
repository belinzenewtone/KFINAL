package com.belinze.lifeos.ui.screen.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing

private data class LearningSession(
    val title: String,
    val category: String,
    val description: String,
    val durationMinutes: Int,
)

private val CATEGORIES = listOf("All", "Finance", "Technology", "Health", "Leadership", "Mindfulness", "Career")

private val CATEGORY_COLORS = mapOf(
    "Finance" to Color(0xFFF5CB5C),
    "Technology" to Color(0xFF7FC8F8),
    "Health" to Color(0xFF7BC47B),
    "Leadership" to Color(0xFFD0BCFF),
    "Mindfulness" to Color(0xFFF2B8B5),
    "Career" to Color(0xFF67D4E0),
)

private val SESSIONS = listOf(
    LearningSession("Budgeting basics in 10 minutes", "Finance", "Understand your income, fixed costs, and discretionary spend.", 10),
    LearningSession("M-Pesa transactions explained", "Finance", "Read your M-Pesa statements: sends, receives, fees, Fuliza.", 8),
    LearningSession("Build a zero-based budget", "Finance", "Give every shilling a job with a simple spreadsheet method.", 15),
    LearningSession("Getting started with password managers", "Technology", "Store credentials safely and stop reusing passwords.", 12),
    LearningSession("How mobile money encryption works", "Technology", "The TLS + hashing that keeps your SMS balances private.", 9),
    LearningSession("Sleep and financial decisions", "Health", "How rest affects impulse spending and attention.", 7),
    LearningSession("Habit stacking for consistency", "Health", "Tie new routines to existing cues so they stick.", 11),
    LearningSession("Running effective weekly reviews", "Leadership", "A 30-minute ritual to plan, reflect, and reset.", 20),
    LearningSession("Calm planning with deep work", "Mindfulness", "Protect focus blocks and reduce context switching.", 13),
    LearningSession("Negotiating your salary", "Career", "Prepare a case, practice the ask, and handle objections.", 16),
    LearningSession("Writing a personal OKR set", "Career", "Turn annual goals into quarterly, measurable outcomes.", 14),
)

@Composable
fun LearningScreen(navController: NavHostController) {
    var category by remember { mutableStateOf<String?>(null) }
    val filtered = SESSIONS.filter { category == null || it.category == category }
    val completedCount = SESSIONS.size // hardcoded sessions count; real DB integration is out of scope
    val monthlyHours = 2.5f
    val monthlyGoalHours = 10f

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow = "Growth",
            title = "Learn",
            onBack = { navController.popBackStack() },
            scrollable = false,
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text("$completedCount of ${SESSIONS.size} sessions completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.base))

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Monthly Goal", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            Text("$monthlyHours / $monthlyGoalHours hours",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        LearningProgressBar(progress = monthlyHours / monthlyGoalHours)
                    }

                    Spacer(Modifier.height(Spacing.base))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = if (cat == "All") category == null else category == cat,
                                onClick = { category = if (cat == "All") null else cat },
                                label = { Text(cat) },
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.base))
                }

                items(filtered, key = { it.title }) { session ->
                    LearningCard(session)
                }
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { },
            text = { Text("Log Session") },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.lg),
        )
    }
}

@Composable
private fun LearningCard(session: LearningSession) {
    val color = CATEGORY_COLORS[session.category] ?: MaterialTheme.colorScheme.primary
    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.category, style = MaterialTheme.typography.bodySmall, color = color)
                Text(session.title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(Icons.Filled.CheckCircle, contentDescription = null,
                tint = Color(0xFF7BC47B), modifier = Modifier.size(22.dp))
        }
        Text(session.description, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Schedule, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
            Text("${session.durationMinutes} min", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LearningProgressBar(progress: Float) {
    Box(
        modifier = Modifier.fillMaxWidth().height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp)
                .background(Color(0xFF7BC47B), CircleShape),
        )
    }
}

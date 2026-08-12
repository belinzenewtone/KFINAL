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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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

// ─────────────────────────────────────────────────────────────────────────────
// LearningScreen — curated learning sessions (mirrors LearningScreen.tsx)
// ─────────────────────────────────────────────────────────────────────────────

private data class LearningSession(
    val title: String,
    val category: String,
    val description: String,
    val durationMinutes: Int,
)

private val CATEGORIES = listOf("All", "Finance", "Technology", "Health", "Leadership", "Mindfulness", "Career")

private val CATEGORY_COLORS = mapOf(
    "Finance"     to Color(0xFFF5CB5C),
    "Technology"  to Color(0xFF7FC8F8),
    "Health"      to Color(0xFF7BC47B),
    "Leadership"  to Color(0xFFD0BCFF),
    "Mindfulness" to Color(0xFFF2B8B5),
    "Career"      to Color(0xFF67D4E0),
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
    var category by remember { mutableStateOf("All") }

    val filtered = SESSIONS.filter { category == "All" || it.category == category }

    PageScaffold(
        eyebrow = "Learn",
        title   = "Learning",
        onBack  = { navController.popBackStack() },
        scrollable = false,
    ) {
        // Category chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CATEGORIES.take(5).forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick  = { category = cat },
                    label    = { Text(cat) },
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.title }) { session ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            text  = session.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = CATEGORY_COLORS[session.category] ?: MaterialTheme.colorScheme.primary,
                        )
                        Text(session.title, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                        Text(session.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
                        Text("${session.durationMinutes} min", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.45f))
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
        }
    }
}

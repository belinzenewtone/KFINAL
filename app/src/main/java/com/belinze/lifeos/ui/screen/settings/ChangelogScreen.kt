package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing

private data class ChangelogEntry(val version: String, val date: String, val title: String, val highlights: List<String>)

private val CHANGELOG = listOf(
    ChangelogEntry(
        "1.4.6", "2026-05", "Profile & settings redesign", listOf(
            "RN-style profile hero card with avatar, name, workspace, member since",
            "Tool Hub 3×2 grid with Analytics, Review, Search, Recurring, Export, Assistant",
            "Settings grouped cards with icon + title + subtitle + toggle/chevron pattern",
            "Search smooth transitions (no more flashing)",
            "Improved theme picker with pill segmented control",
        ),
    ),
    ChangelogEntry(
        "1.4.2", "2026-05", "Search data fix & profile grid redesign", listOf(
            "Search now queries all data including unclaimed records",
            "Added deleted_at IS NULL filter to all search queries",
            "Profile grid redesigned — icon above title, tap any card to open its page",
        ),
    ),
    ChangelogEntry(
        "1.4.0", "2026-05", "UI overhaul — Nexus Blue, pill shapes, unified shadows", listOf(
            "Nexus Blue #2E6FE8 palette applied across light/dark themes",
            "Bottom nav labels always visible with smooth color animation",
            "Real-time search with source icons and cleaner layout",
            "Unified design tokens for cards, inputs, buttons, dialogs",
        ),
    ),
    ChangelogEntry(
        "1.3.4", "2026-05", "Stable baseline", listOf(
            "Task management with categories, priorities, deadlines",
            "Finance tracking with M-Pesa integration",
            "Calendar events and recurring templates",
            "AI assistant for quick task creation",
            "Offline-first with cloud sync",
        ),
    ),
)

@Composable
fun ChangelogScreen(navController: NavHostController) {
    PageScaffold(
        title = "What's new",
        onBack = { navController.popBackStack() },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Text(
                "PersonalOS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Text("Recent updates and improvements.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.base))

            CHANGELOG.forEachIndexed { index, entry ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "v${entry.version}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (index == 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.size(Spacing.sm))
                            Text(entry.date, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                            if (index == 0) {
                                Text(
                                    "Latest",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0x20 / 255f), RoundedCornerShape(9999.dp))
                                        .padding(horizontal = Spacing.sm, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(entry.title, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(Spacing.sm))
                        entry.highlights.forEach { highlight ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                Spacer(Modifier.size(Spacing.sm))
                                Text(highlight, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

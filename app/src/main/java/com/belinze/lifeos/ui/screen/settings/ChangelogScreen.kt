package com.belinze.lifeos.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing

// ─────────────────────────────────────────────────────────────────────────────
// ChangelogScreen — 1:1 port of ChangelogScreen.tsx
// ─────────────────────────────────────────────────────────────────────────────

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
        eyebrow = "App",
        title   = "What's New",
        onBack  = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CHANGELOG.forEach { entry ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text("${entry.version} · ${entry.date}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                        entry.highlights.forEach { h ->
                            Text("• $h", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.70f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

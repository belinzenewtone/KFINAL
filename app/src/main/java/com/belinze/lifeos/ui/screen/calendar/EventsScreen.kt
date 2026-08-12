package com.belinze.lifeos.ui.screen.calendar

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventViewModel

// ─────────────────────────────────────────────────────────────────────────────
// EventsScreen — 1:1 port of EventsScreen.tsx
//
// Upcoming events (filtered + searchable), tap → detail, + → new event.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EventsScreen(
    navController: NavHostController,
    viewModel:     EventViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }

    val nowIso = java.time.LocalDateTime.now().toString()
    val upcoming = state.events
        .filter { it.date >= nowIso.take(10) }
        .filter { query.isBlank() || it.title.lowercase().contains(query.lowercase()) }
        .sortedBy { it.date }

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow = "Calendar",
            title   = "Events",
            subtitle = "Upcoming",
            onBack  = { navController.popBackStack() },
            scrollable = false,
        ) {
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Search events…") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
            )

            if (upcoming.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l),
                    contentAlignment = Alignment.Center) {
                    Text("No upcoming events. Tap + to schedule one.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(upcoming, key = { it.id }) { event ->
                        GlassCard(
                            onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.shapes.extraSmall,
                                        )
                                )
                                Spacer(Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.title, fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground)
                                    Text(
                                        text  = "${event.date.take(10)}${event.location?.let { " · $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                                    )
                                }
                                Text(
                                    text  = event.type.replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick        = { navController.navigate(NavTo.eventForm()) },
            text           = { Text("Add Event") },
            icon           = { Icon(Icons.Filled.Add, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.bottomNavSafeArea + Spacing.md),
        )
    }
}

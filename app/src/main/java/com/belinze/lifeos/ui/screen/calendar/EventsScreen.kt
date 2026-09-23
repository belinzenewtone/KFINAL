package com.belinze.lifeos.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun EventsScreen(
    navController: NavHostController,
    viewModel:     EventViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val today    = remember { LocalDate.now().toString() }
    val upcoming = remember(state.events, query) {
        state.events
            .filter { it.type != "task" }   // show events, birthdays, anniversaries, countdowns — not tasks
            .filter { it.date.take(10) >= today }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .sortedBy { it.date }
    }

    PageScaffold(
        title = "Events",
        subtitle = "Upcoming",
        onBack = { navController.popBackStack() },
        scrollable = false,
        actions = {
            IconButton(onClick = { navController.navigate(NavTo.eventForm()) }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add event", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search events...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
        )

        if (upcoming.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l)) {
                Text("No upcoming events", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Tap + to schedule your next event.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
            ) {
                items(upcoming, key = { it.id }) { event ->
                    GlassCard(
                        onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.base),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 40.dp)
                                    .background(eventImportanceColor(event.importance), MaterialTheme.shapes.extraSmall),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    event.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    formatEventSubtitle(event.date, event.type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                com.belinze.lifeos.util.formatLocation(event.location)?.let { loc ->
                                    Text(
                                        loc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }
    }
}

private fun formatEventSubtitle(iso: String, type: String): String {
    val typeLabel = when (type) {
        "birthday"    -> "Birthday"
        "anniversary" -> "Anniversary"
        "countdown"   -> "Countdown"
        else          -> "Event"
    }
    val datePart = try {
        val date = LocalDate.parse(iso.take(10))
        date.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy"))
    } catch (_: Exception) {
        iso.take(10)
    }
    val timePart = if (iso.length > 10) {
        try {
            val time = iso.take(16).substring(11)
            val h = time.take(2).toInt()
            val m = time.takeLast(2).toInt()
            val ampm = if (h < 12) "AM" else "PM"
            val dh = if (h == 0) 12 else if (h > 12) h - 12 else h
            " · %d:%02d %s".format(dh, m, ampm)
        } catch (_: Exception) {
            ""
        }
    } else {
        ""
    }
    return "$datePart$timePart · $typeLabel"
}

@Composable
private fun eventImportanceColor(importance: String) = when (importance) {
    "high"   -> androidx.compose.ui.graphics.Color(0xFFF2B8B5)
    "medium" -> androidx.compose.ui.graphics.Color(0xFFFBBF24)
    else     -> androidx.compose.ui.graphics.Color(0xFF7FC8F8)
}

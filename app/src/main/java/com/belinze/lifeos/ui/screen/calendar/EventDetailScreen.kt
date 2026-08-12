package com.belinze.lifeos.ui.screen.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventViewModel

// ─────────────────────────────────────────────────────────────────────────────
// EventDetailScreen — read-only view of a single calendar event.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EventDetailScreen(
    eventId:       String,
    navController: NavHostController,
    viewModel:     EventViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val event   = remember(uiState.events, eventId) { uiState.events.find { it.id == eventId } }

    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { /* events already loaded via init */ }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Event") },
            text  = { Text("This event will be permanently removed. Continue?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.softDelete(eventId)
                    navController.popBackStack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }

    PageScaffold(
        eyebrow = event?.type?.replaceFirstChar { it.uppercase() } ?: "Event",
        title   = event?.title ?: "—",
        onBack  = { navController.popBackStack() },
        actions = {
            if (event != null) {
                IconButton(onClick = { navController.navigate(NavTo.eventForm(eventId)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
        if (event == null) {
            Text("Event not found.", color = MaterialTheme.colorScheme.error)
            return@PageScaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier            = Modifier.verticalScroll(rememberScrollState()),
        ) {
            // ── Detail rows ───────────────────────────────────────────────────
            DetailRow(
                icon  = Icons.Filled.CalendarMonth,
                label = "Date",
                value = event.date.take(10),
            )

            if (event.endDate != null) {
                DetailRow(
                    icon  = Icons.Filled.CalendarMonth,
                    label = "Ends",
                    value = event.endDate!!.take(10),
                )
            }

            if (!event.location.isNullOrBlank()) {
                DetailRow(
                    icon  = Icons.Filled.LocationOn,
                    label = "Location",
                    value = event.location!!,
                )
            }

            if (event.repeatRule != null) {
                DetailRow(
                    icon  = Icons.Filled.Loop,
                    label = "Repeats",
                    value = event.repeatRule!!.replaceFirstChar { it.uppercase() },
                )
            }

            if (!event.description.isNullOrBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                Text("Notes", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.60f))
                Text(event.description!!, color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }
}

@Composable
private fun DetailRow(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            Text(value, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

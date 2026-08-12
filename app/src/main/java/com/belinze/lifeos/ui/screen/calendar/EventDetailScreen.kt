package com.belinze.lifeos.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val PRIORITY_COLORS = mapOf(
    "low" to Color(0xFF7FC8F8),
    "medium" to Color(0xFFF5CB5C),
    "high" to Color(0xFFF2B8B5),
)

@Composable
fun EventDetailScreen(
    eventId:       String,
    navController: NavHostController,
    viewModel:     EventViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val event = remember(uiState.events, eventId) { uiState.events.find { it.id == eventId } }
    var showDelete by remember { mutableStateOf(false) }

    PageScaffold(
        title = "Event",
        onBack = { navController.popBackStack() },
        actions = {
            if (event != null) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        scrollable = false,
    ) {
        if (event == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@PageScaffold
        }

        val priorityColor = PRIORITY_COLORS[event.importance] ?: MaterialTheme.colorScheme.onSurfaceVariant

        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0x20 / 255f), RoundedCornerShape(9999.dp))
                            .padding(horizontal = Spacing.base, vertical = Spacing.xs),
                    ) {
                        Text(event.importance.uppercase(), style = MaterialTheme.typography.labelSmall,
                            color = priorityColor)
                    }
                    Spacer(Modifier.height(Spacing.base))
                    Text(event.title, style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    Text("${event.type} · ${event.kind}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    event.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = Spacing.base))
                    }
                }
            }

            DetailRow("Starts", formatDateTime(event.date))
            event.endDate?.let { DetailRow("Ends", formatDateTime(it)) }
            event.location?.let { DetailRow("Location", it) }
            DetailRow("Status", event.status)
            DetailRow("Repeat", event.repeatRule ?: "none")
            if (event.allDay != 0) DetailRow("All day", "Yes")

            Button(
                onClick = { navController.navigate(NavTo.eventForm(event.id, event.type)) },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
            ) {
                Text("Edit Event")
            }

            Spacer(Modifier.height(Spacing.bottomNavSafeArea))
        }
    }

    if (showDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete event") },
            text = { Text("Are you sure?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.softDelete(eventId)
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.base),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = Spacing.base))
    }
}

private fun formatDateTime(iso: String): String = try {
    LocalDateTime.parse(iso.take(19)).format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
} catch (_: Exception) { iso.take(16) }

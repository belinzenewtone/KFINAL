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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.LearningSessionEntity
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.components.PageScaffold
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.LearningViewModel

private val CATEGORIES = listOf("All", "Finance", "Technology", "Health", "Leadership", "Mindfulness", "Career")

private val CATEGORY_COLORS = mapOf(
    "Finance"     to Color(0xFFF5CB5C),
    "Technology"  to Color(0xFF7FC8F8),
    "Health"      to Color(0xFF7BC47B),
    "Leadership"  to Color(0xFFD0BCFF),
    "Mindfulness" to Color(0xFFF2B8B5),
    "Career"      to Color(0xFF67D4E0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    navController: NavHostController,
    viewModel: LearningViewModel = hiltViewModel(),
) {
    // LE-1 / LE-10: live Room state
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var category by remember { mutableStateOf<String?>(null) }
    val filtered       = remember(state.sessions, category) { state.sessions.filter { category == null || it.category == category } }
    val completedCount = remember(state.sessions)           { state.sessions.count { it.isCompleted == 1 } }

    val monthlyHours    = state.monthlyHours
    val monthlyGoalHours = 10f

    // LE-2: bottom sheet visibility state
    var showLogSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            eyebrow = "Growth",
            title = "Learn",
            onBack = { navController.popBackStack() },
            scrollable = false,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
            ) {
                item {
                    Text("$completedCount of ${state.sessions.size} sessions completed",
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
                            Text("%.1f / %.0f hours".format(monthlyHours, monthlyGoalHours),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        LearningProgressBar(progress = monthlyHours / monthlyGoalHours)
                    }

                    // LE-6: horizontal chip scroll row for category filter
                    Spacer(Modifier.height(Spacing.base))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(CATEGORIES, key = { it }) { cat ->
                            FilterChip(
                                selected = if (cat == "All") category == null else category == cat,
                                onClick  = { category = if (cat == "All") null else cat },
                                label    = { Text(cat) },
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.base))
                }

                // LE-4: empty state when filter yields no results
                if (filtered.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                "No sessions here",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Try a different category or log a new session.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    // LE-3: key by stable id; onTap delegates to ViewModel
                    items(filtered, key = { it.id }) { session ->
                        LearningCard(
                            session = session,
                            onTap   = { viewModel.toggleCompleted(session.id, session.isCompleted == 1) },
                        )
                    }
                }

                item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showLogSheet = true },
            text = { Text("Log Session") },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.lg),
        )

        // LE-2: Log Session bottom sheet
        if (showLogSheet) {
            LogSessionSheet(
                defaultCategory = category ?: "Career",
                onDismiss = { showLogSheet = false },
                onSave = { topic, selectedCategory, durationMinutes, notes ->
                    viewModel.logSession(
                        title       = topic,
                        category    = selectedCategory,
                        duration    = durationMinutes,
                        description = notes,
                    )
                    showLogSheet = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSessionSheet(
    defaultCategory: String,
    onDismiss: () -> Unit,
    onSave: (topic: String, category: String, durationMinutes: Int, notes: String) -> Unit,
) {
    val sessionCategories = CATEGORIES.drop(1) // exclude "All"

    var topic         by remember { mutableStateOf("") }
    var duration      by remember { mutableStateOf("") }
    var notes         by remember { mutableStateOf("") }
    var selectedCat   by remember { mutableStateOf(defaultCategory.takeIf { it in sessionCategories } ?: "Career") }
    var topicError    by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            Text(
                "Log Session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it; topicError = false },
                label = { Text("Topic") },
                isError = topicError,
                supportingText = if (topicError) ({ Text("Topic is required") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it; durationError = false },
                label = { Text("Duration (minutes)") },
                isError = durationError,
                supportingText = if (durationError) ({ Text("Enter a duration greater than 0") }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // Category chip row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(sessionCategories, key = { it }) { cat ->
                    FilterChip(
                        selected = cat == selectedCat,
                        onClick  = { selectedCat = cat },
                        label    = { Text(cat) },
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.size(Spacing.sm))
                Button(
                    onClick = {
                        val durationInt = duration.trim().toIntOrNull() ?: 0
                        topicError    = topic.isBlank()
                        durationError = durationInt <= 0
                        if (!topicError && !durationError) {
                            onSave(topic.trim(), selectedCat, durationInt, notes.trim())
                        }
                    },
                ) {
                    Text("Save")
                }
            }
        }
    }
}

// LE-3 + LE-5: card is now tappable and shows progress/chip for non-completed sessions
@Composable
private fun LearningCard(session: LearningSessionEntity, onTap: () -> Unit) {
    // LE-7: elevated Card surface to match RN session card style
    val completed = session.isCompleted == 1
    val color = CATEGORY_COLORS[session.category] ?: MaterialTheme.colorScheme.primary
    Card(
        onClick   = onTap,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.base),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                if (completed) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                        tint = Color(0xFF7BC47B), modifier = Modifier.size(22.dp))
                }
            }
            if (!session.description.isNullOrBlank()) {
                Text(session.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                Text("${session.durationMinutes} min", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // LE-5: progress bar + chip
            if (!completed) {
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = onTap,
                    label = { Text("Start", style = MaterialTheme.typography.labelSmall) },
                )
            } else {
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = onTap,
                    label = { Text("Mark Incomplete", style = MaterialTheme.typography.labelSmall) },
                )
            }
        } // end Column padding
    }
}

@Composable
private fun LearningProgressBar(progress: Float) {
    // LE-9: dynamic color — green ≥80%, yellow ≥40%, red <40%
    val barColor = when {
        progress >= 0.80f -> Color(0xFF7BC47B)   // green
        progress >= 0.40f -> Color(0xFFF5CB5C)   // yellow
        else              -> Color(0xFFEF4444)    // red
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp)
                .background(barColor, CircleShape),
        )
    }
}

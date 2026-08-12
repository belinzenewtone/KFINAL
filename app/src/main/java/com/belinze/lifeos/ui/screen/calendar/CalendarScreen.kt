package com.belinze.lifeos.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.EventEntity
import com.belinze.lifeos.data.db.entity.TaskEntity
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.CalendarView
import com.belinze.lifeos.viewmodel.EventViewModel
import com.belinze.lifeos.viewmodel.TaskFilter
import com.belinze.lifeos.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// CalendarScreen
//
// 1:1 port of src/screens/calendar/CalendarScreen.tsx.
//
// Three views (segmented control at top):
//   Month  — 7-col grid of day cells with event dots
//   Week   — 7-day strip showing events per day
//   Agenda — flat chronological list of upcoming events
// ─────────────────────────────────────────────────────────────────────────────

/** Outer tab: Calendar grid/agenda, Tasks list, or Events feed */
private enum class CalendarOuterTab { Calendar, Tasks, Events }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    eventViewModel: EventViewModel = hiltViewModel(),
    taskViewModel:  TaskViewModel  = hiltViewModel(),
) {
    val eventState by eventViewModel.uiState.collectAsState()
    val taskState  by taskViewModel.uiState.collectAsState()
    val isDark     = isSystemInDarkTheme()

    // Outer tab state
    var outerTab by rememberSaveable { mutableStateOf(CalendarOuterTab.Calendar) }

    // Calendar inner state
    var selectedDate     by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val activeView       = eventState.calendarView

    // Events on selected day
    val selectedDayEvents = remember(eventState.events, selectedDate) {
        eventState.events.filter { it.date.take(10) == selectedDate }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "Calendar",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // ── Outer tab: Calendar / Tasks / Events ──────────────────────────
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
            ) {
                CalendarOuterTab.values().forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = outerTab == tab,
                        onClick  = { outerTab = tab },
                        shape    = SegmentedButtonDefaults.itemShape(index = idx, count = CalendarOuterTab.values().size),
                        label    = { Text(tab.name) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            when (outerTab) {
                CalendarOuterTab.Calendar -> {
                    // ── Inner view selector (Month / Week / Agenda) ───────────
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        CalendarView.values().forEach { view ->
                            FilterChip(
                                selected = activeView == view,
                                onClick  = { eventViewModel.setCalendarView(view) },
                                label    = { Text(view.name) },
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    when (activeView) {
                        CalendarView.Month  -> MonthView(
                            yearMonth         = currentYearMonth,
                            events            = eventState.events,
                            selectedDate      = selectedDate,
                            onDaySelect       = { selectedDate = it },
                            onPrevMonth       = { currentYearMonth = currentYearMonth.minusMonths(1) },
                            onNextMonth       = { currentYearMonth = currentYearMonth.plusMonths(1) },
                            navController     = navController,
                            selectedDayEvents = selectedDayEvents,
                        )
                        CalendarView.Week   -> WeekView(
                            events        = eventState.events,
                            selectedDate  = selectedDate,
                            onDaySelect   = { selectedDate = it },
                            navController = navController,
                        )
                        CalendarView.Agenda -> AgendaView(
                            events        = eventState.events.filter { it.date.take(10) >= LocalDate.now().toString() },
                            navController = navController,
                        )
                    }
                }

                CalendarOuterTab.Tasks -> {
                    // ── Tasks tab ─────────────────────────────────────────────
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        TaskFilter.values().forEach { filter ->
                            FilterChip(
                                selected = taskState.filter == filter,
                                onClick  = { taskViewModel.setFilter(filter) },
                                label    = { Text(filter.name) },
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    if (taskState.tasks.isEmpty()) {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(Spacing.x2l),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No tasks", color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(taskState.tasks, key = { it.id }) { task ->
                                CalendarTaskItem(
                                    task     = task,
                                    onToggle = { taskViewModel.complete(task.id) },
                                    onClick  = { navController.navigate(NavTo.taskDetail(task.id)) },
                                )
                            }
                            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                        }
                    }
                }

                CalendarOuterTab.Events -> {
                    // ── Events tab — flat chronological list ──────────────────
                    val upcoming = remember(eventState.events) {
                        eventState.events
                            .filter { it.date.take(10) >= LocalDate.now().toString() }
                            .sortedBy { it.date }
                    }
                    if (upcoming.isEmpty()) {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(Spacing.x2l),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No upcoming events", color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(upcoming, key = { it.id }) { event ->
                                EventListItem(event, onClick = { navController.navigate(NavTo.eventDetail(event.id)) })
                            }
                            item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                        }
                    }
                }
            }
        }

        // ── FAB — context-sensitive ───────────────────────────────────────────
        ExtendedFloatingActionButton(
            onClick = {
                when (outerTab) {
                    CalendarOuterTab.Tasks  -> navController.navigate(Route.TASK_FORM)
                    else                   -> navController.navigate(Route.EVENT_FORM)
                }
            },
            text  = { Text(if (outerTab == CalendarOuterTab.Tasks) "Add Task" else "Add Event") },
            icon  = { Icon(Icons.Filled.Add, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.lg, bottom = Spacing.bottomNavSafeArea + Spacing.md),
        )
    }
}

// ─── Calendar task item (lightweight, in-line row) ───────────────────────────

@Composable
private fun CalendarTaskItem(
    task:     TaskEntity,
    onToggle: () -> Unit,
    onClick:  () -> Unit,
) {
    val isDone            = task.status == "completed"
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = ripple(color = primary.copy(0.12f)), onClick = onClick)
            .padding(horizontal = Spacing.screenHorizontal, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector        = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint               = if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground.copy(0.40f),
            modifier           = Modifier
                .size(22.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text           = task.title,
                style          = MaterialTheme.typography.bodyMedium,
                color          = if (isDone) MaterialTheme.colorScheme.onBackground.copy(0.40f) else MaterialTheme.colorScheme.onBackground,
                maxLines       = 1,
            )
            if (task.deadline != null) {
                Text(
                    text  = task.deadline.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                )
            }
        }
    }
}

// ─── Month view ───────────────────────────────────────────────────────────────

@Composable
private fun MonthView(
    yearMonth:         YearMonth,
    events:            List<EventEntity>,
    selectedDate:      String,
    onDaySelect:       (String) -> Unit,
    onPrevMonth:       () -> Unit,
    onNextMonth:       () -> Unit,
    navController:     NavHostController,
    selectedDayEvents: List<EventEntity>,
) {
    val primary = MaterialTheme.colorScheme.primary
    val today   = LocalDate.now()

    Column(modifier = Modifier.fillMaxWidth()) {

        // Month navigation
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text       = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        // Day-of-week header
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.values().map { it.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) }.forEach { label ->
                Text(
                    text      = label,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize  = 11.sp,
                    color     = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Day cells
        val firstDay    = yearMonth.atDay(1)
        val startOffset = (firstDay.dayOfWeek.value % 7)  // Mon=0, Sun=6
        val daysInMonth = yearMonth.lengthOfMonth()
        val rows        = ((startOffset + daysInMonth + 6) / 7)

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - startOffset + 1
                    if (dayIndex < 1 || dayIndex > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date      = yearMonth.atDay(dayIndex)
                        val dateStr   = date.toString()
                        val isToday   = date == today
                        val isSelected = dateStr == selectedDate
                        val hasEvent  = events.any { it.date.take(10) == dateStr }

                        DayCell(
                            day        = dayIndex,
                            isToday    = isToday,
                            isSelected = isSelected,
                            hasEvent   = hasEvent,
                            onClick    = { onDaySelect(dateStr) },
                            modifier   = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Selected day events
        if (selectedDayEvents.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text       = "Events",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                modifier   = Modifier.padding(horizontal = Spacing.screenHorizontal),
                letterSpacing = 0.5.sp,
            )
            selectedDayEvents.forEach { event ->
                EventListItem(event, onClick = { navController.navigate(NavTo.eventDetail(event.id)) })
            }
        }

        Spacer(Modifier.height(Spacing.bottomNavSafeArea))
    }
}

// ─── Week view ────────────────────────────────────────────────────────────────

@Composable
private fun WeekView(
    events:        List<EventEntity>,
    selectedDate:  String,
    onDaySelect:   (String) -> Unit,
    navController: NavHostController,
) {
    val today = LocalDate.now()
    val monday = today.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal)) {
                weekDays.forEach { date ->
                    val isToday    = date == today
                    val isSelected = date.toString() == selectedDate
                    val hasEvent   = events.any { it.date.take(10) == date.toString() }
                    DayCell(
                        day        = date.dayOfMonth,
                        isToday    = isToday,
                        isSelected = isSelected,
                        hasEvent   = hasEvent,
                        onClick    = { onDaySelect(date.toString()) },
                        modifier   = Modifier.weight(1f),
                        dayLabel   = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                    )
                }
            }
        }

        val dayEvents = events.filter { it.date.take(10) == selectedDate }
        if (dayEvents.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                    Text("No events", color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                }
            }
        } else {
            items(dayEvents, key = { it.id }) { event ->
                EventListItem(event, onClick = { navController.navigate(NavTo.eventDetail(event.id)) })
            }
        }
        item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
    }
}

// ─── Agenda view ─────────────────────────────────────────────────────────────

@Composable
private fun AgendaView(
    events:        List<EventEntity>,
    navController: NavHostController,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (events.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                    Text("No upcoming events", color = MaterialTheme.colorScheme.onBackground.copy(0.40f))
                }
            }
        } else {
            items(events.sortedBy { it.date }, key = { it.id }) { event ->
                EventListItem(event, onClick = { navController.navigate(NavTo.eventDetail(event.id)) })
            }
        }
        item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
    }
}

// ─── Day cell ─────────────────────────────────────────────────────────────────

@Composable
private fun DayCell(
    day:        Int,
    isToday:    Boolean,
    isSelected: Boolean,
    hasEvent:   Boolean,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier,
    dayLabel:   String?  = null,
) {
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier          = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> primary
                    isToday    -> primary.copy(0.15f)
                    else       -> Color.Transparent
                },
                CircleShape,
            )
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (dayLabel != null) {
            Text(dayLabel, fontSize = 9.sp, color = if (isSelected) primary.copy(0.85f) else
                MaterialTheme.colorScheme.onBackground.copy(0.45f))
        }
        Text(
            text       = day.toString(),
            fontSize   = 13.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday    -> primary
                else       -> MaterialTheme.colorScheme.onBackground
            },
        )
        if (hasEvent) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else primary, CircleShape),
            )
        }
    }
}

// ─── Event list item ──────────────────────────────────────────────────────────

@Composable
private fun EventListItem(event: EventEntity, onClick: () -> Unit) {
    val primary           = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = ripple(color = primary.copy(0.12f)), onClick = onClick)
            .padding(horizontal = Spacing.screenHorizontal, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(4.dp, 36.dp).background(primary, MaterialTheme.shapes.extraSmall))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(event.title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            if (event.date.length > 10) {
                Text(
                    text  = event.date.take(16).replace("T", " at "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                )
            }
        }
    }
}

package com.belinze.lifeos.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.belinze.lifeos.data.db.entity.EventEntity
import com.belinze.lifeos.data.db.entity.TaskEntity
import com.belinze.lifeos.ui.components.GlassCard
import com.belinze.lifeos.ui.navigation.NavTo
import com.belinze.lifeos.ui.navigation.Route
import com.belinze.lifeos.ui.theme.Spacing
import com.belinze.lifeos.viewmodel.EventViewModel
import com.belinze.lifeos.viewmodel.SettingsViewModel
import com.belinze.lifeos.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// CalendarScreen — 1:1 port of src/screens/calendar/CalendarScreen.tsx.
// Three outer tabs: Calendar / Tasks / Events. The React app has no separate
// Month/Week/Agenda selector, so that Kotlin-only control is removed here.
// ─────────────────────────────────────────────────────────────────────────────

private enum class CalendarTab { Calendar, Tasks, Events }

private val SUCCESS = Color(0xFF7BC47B)
private val WARNING = Color(0xFFF5CB5C)
private val BIRTHDAY = Color(0xFFEC4899)
private val ANNIVERSARY = Color(0xFF22C55E)

// HorizontalPager bounds — ±100 years around today so months can be swiped
// freely in either direction (same paging engine the Material3 date picker uses).
private const val PAGER_CENTER = 1200
private const val PAGER_RANGE = PAGER_CENTER * 2 + 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController:    NavHostController,
    eventViewModel:   EventViewModel   = hiltViewModel(),
    taskViewModel:    TaskViewModel    = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val eventState   by eventViewModel.uiState.collectAsStateWithLifecycle()
    val taskState    by taskViewModel.uiState.collectAsStateWithLifecycle()
    val settings     by settingsViewModel.settings.collectAsState(initial = com.belinze.lifeos.data.datastore.AppPreferenceState())
    val calendarSwipeEnabled = settings.calendarSwipe

    var selectedTab by rememberSaveable { mutableStateOf(CalendarTab.Calendar) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var calendarQuery by remember { mutableStateOf("") }
    var tasksQuery by remember { mutableStateOf("") }
    var eventsQuery by remember { mutableStateOf("") }
    var addMenuOpen by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val todayYearMonth = YearMonth.now()

    // Pager center = current month; swiping left/right moves ±1 month with the
    // same smooth animated paging the Material3 date picker uses.
    val pagerState = rememberPagerState(initialPage = PAGER_CENTER) { PAGER_RANGE }
    val yearMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths(pagerState.currentPage.toLong() - PAGER_CENTER)
    }
    val isCurrentMonth = yearMonth == todayYearMonth
    val scope = rememberCoroutineScope()

    val headerSubtitle = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    val selectedDateLabel = remember(selectedDate) {
        runCatching {
            LocalDate.parse(selectedDate).format(DateTimeFormatter.ofPattern("EEEE, MMM dd", Locale.ENGLISH))
        }.getOrNull() ?: selectedDate
    }

    val dayItems = remember(eventState.events, selectedDate) {
        eventState.events.filter { it.date.take(10) == selectedDate }
    }

    val filteredDayItems = if (calendarQuery.isBlank()) {
        dayItems
    } else {
        dayItems.filter { it.title.contains(calendarQuery, ignoreCase = true) }
    }

    val dayItemGroups = listOf(
        Triple("Tasks", SUCCESS, filteredDayItems.filter { it.type == "task" }),
        Triple("Events", MaterialTheme.colorScheme.primary, filteredDayItems.filter { it.type == "event" }),
        Triple("Birthdays", BIRTHDAY, filteredDayItems.filter { it.type == "birthday" }),
        Triple("Anniversaries", ANNIVERSARY, filteredDayItems.filter { it.type == "anniversary" }),
        Triple("Countdowns", WARNING, filteredDayItems.filter { it.type == "countdown" }),
    ).filter { it.third.isNotEmpty() }

    val allTasks = taskState.tasks
    val pendingCount = allTasks.count { it.status == "active" }
    val doneCount = allTasks.count { it.status == "completed" }
    val filteredTasks = if (tasksQuery.isBlank()) {
        allTasks
    } else {
        allTasks.filter { it.title.contains(tasksQuery, ignoreCase = true) }
    }

    val allEvents = eventState.events.filter {
        it.type == "event"
    }
    val filteredEvents = if (eventsQuery.isBlank()) {
        allEvents
    } else {
        allEvents.filter { it.title.contains(eventsQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Calendar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        headerSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { addMenuOpen = true }) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
            ) {
                CalendarTab.values().forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = CalendarTab.values().size),
                        label = { Text(tab.name) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            // ── Month navigation — always visible, regardless of tab ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // CA-4: Today button — sits tight against the month/year row above
            // with a uniform 4dp gap on both sides, only when not on current month.
            if (!isCurrentMonth) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(PAGER_CENTER) }
                            selectedDate = today.toString()
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("Today", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Weekday header
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // Smooth month paging — userScrollEnabled respects the Settings toggle.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1,
                userScrollEnabled = calendarSwipeEnabled,
            ) { page ->
                val pageMonth = YearMonth.now().plusMonths(page.toLong() - PAGER_CENTER)
                MonthGrid(
                    yearMonth = pageMonth,
                    today = today,
                    selectedDate = selectedDate,
                    events = eventState.events,
                    onDayClick = { selectedDate = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(Spacing.base))
            Text(
                selectedDateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.sm))

            // ── Tab content scrolls below the always-visible calendar ──────────
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    CalendarTab.Calendar -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.screenHorizontal),
                    ) {
                        SearchBar(
                            value = calendarQuery,
                            onChange = { calendarQuery = it },
                            placeholder = "Search across all categories",
                        )
                        Spacer(Modifier.height(Spacing.sm))

                        if (dayItemGroups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.x2l),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                                    Text("Nothing for the day", style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text("Tap + to add an event, birthday, countdown and more.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            dayItemGroups.forEach { group ->
                                DayItemGroup(
                                    label = group.first,
                                    color = group.second,
                                    items = group.third,
                                    onItemClick = { item ->
                                        // Day-view items are all EventEntity rows (tasks with
                                        // type="task" live in the events table, not tasks table).
                                        navController.navigate(NavTo.eventDetail(item.id))
                                    },
                                )
                                Spacer(Modifier.height(Spacing.base))
                            }
                        }

                        Spacer(Modifier.height(Spacing.bottomNavSafeArea))
                    }

                    CalendarTab.Tasks -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.screenHorizontal),
                        contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
                    ) {
                        item {
                            Text(
                                "$pendingCount Pending · 0 Doing · $doneCount Done",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            SearchBar(
                                value = tasksQuery,
                                onChange = { tasksQuery = it },
                                placeholder = "Search tasks...",
                            )
                            Spacer(Modifier.height(Spacing.sm))
                        }

                        if (filteredTasks.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                                    Text("No tasks here", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(filteredTasks, key = { it.id }) { task ->
                                CalendarTaskItem(
                                    task = task,
                                    onToggle = { taskViewModel.complete(task.id) },
                                    onClick = { navController.navigate(NavTo.taskDetail(task.id)) },
                                )
                            }
                        }
                        item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                    }

                    CalendarTab.Events -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.screenHorizontal),
                        contentPadding = PaddingValues(bottom = Spacing.bottomNavSafeArea),
                    ) {
                        item {
                            Text(
                                "${filteredEvents.size} event${if (filteredEvents.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            SearchBar(
                                value = eventsQuery,
                                onChange = { eventsQuery = it },
                                placeholder = "Search events...",
                            )
                            Spacer(Modifier.height(Spacing.sm))
                        }

                        if (filteredEvents.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.x2l), contentAlignment = Alignment.Center) {
                                    Text("No events yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(filteredEvents, key = { it.id }) { event ->
                                EventListItem(
                                    event = event,
                                    onClick = { navController.navigate(NavTo.eventDetail(event.id)) },
                                    onDelete = { eventViewModel.softDelete(event.id) },
                                )
                            }
                        }
                        item { Spacer(Modifier.height(Spacing.bottomNavSafeArea)) }
                    }
                }
            }
        }
    }

    if (addMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = { addMenuOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Add", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { addMenuOpen = false }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                AddMenuOption("Task", { addMenuOpen = false; navController.navigate(NavTo.eventForm(type = "task")) })
                AddMenuOption("Event", { addMenuOpen = false; navController.navigate(NavTo.eventForm(type = "event")) })
                AddMenuOption("Birthday", { addMenuOpen = false; navController.navigate(NavTo.eventForm(type = "birthday")) })
                AddMenuOption("Anniversary", { addMenuOpen = false; navController.navigate(NavTo.eventForm(type = "anniversary")) })
                AddMenuOption("Countdown", { addMenuOpen = false; navController.navigate(NavTo.eventForm(type = "countdown")) })
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DayItemGroup(
    label: String,
    color: Color,
    items: List<EventEntity>,
    onItemClick: (EventEntity) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 16.dp)
                    .background(color, MaterialTheme.shapes.extraSmall),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${items.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items.forEach { item ->
            GlassCard(
                modifier = Modifier.padding(top = Spacing.sm),
                onClick = { onItemClick(item) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color, androidx.compose.foundation.shape.CircleShape),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        if (item.date.length > 10) {
                            Text(
                                item.date.take(16).replace("T", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val isDone = task.status == "completed"
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier.padding(bottom = Spacing.sm),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) SUCCESS else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDone) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                )
                task.deadline?.let {
                    Text(
                        it.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (task.priority) {
                            "high" -> MaterialTheme.colorScheme.error
                            "medium" -> WARNING
                            else -> MaterialTheme.colorScheme.primary
                        },
                        androidx.compose.foundation.shape.CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun EventListItem(
    event: EventEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier.padding(bottom = Spacing.sm),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 40.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(
                    "${event.date.take(10)} · ${event.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth:   YearMonth,
    today:       LocalDate,
    selectedDate: String,
    events:      List<EventEntity>,
    onDayClick:  (String) -> Unit,
    modifier:    Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val firstDay = yearMonth.atDay(1)
        // Monday = 0 offset (Mon-first grid matches "M T W T F S S" header)
        val startOffset = firstDay.dayOfWeek.value - 1
        val daysInMonth = yearMonth.lengthOfMonth()
        val rows = (startOffset + daysInMonth + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - startOffset + 1
                    if (dayIndex < 1 || dayIndex > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = yearMonth.atDay(dayIndex)
                        val dateStr = date.toString()
                        val isToday = date == today
                        val isSelected = dateStr == selectedDate
                        // CA-2: collect one dot per event type present on this day
                        val dotColors = remember(events, dateStr) {
                            buildList {
                                val dayEvents = events.filter { it.date.take(10) == dateStr }
                                if (dayEvents.any { it.type == "task" })        add(SUCCESS)
                                if (dayEvents.any { it.type == "event" })       add(Color(0xFF60A5FA))
                                if (dayEvents.any { it.type == "birthday" })    add(BIRTHDAY)
                                if (dayEvents.any { it.type == "anniversary" }) add(ANNIVERSARY)
                                if (dayEvents.any { it.type == "countdown" })   add(WARNING)
                            }
                        }
                        DayCell(
                            day = dayIndex,
                            isToday = isToday,
                            isSelected = isSelected,
                            dotColors = dotColors,
                            onClick = { onDayClick(dateStr) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    dotColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                when {
                    isSelected -> primary
                    isToday -> primary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                },
                androidx.compose.foundation.shape.CircleShape,
            )
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            day.toString(),
            fontSize = 13.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        // CA-2: colored dots for each event type present on this day
        if (dotColors.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                dotColors.take(3).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary else color,
                                androidx.compose.foundation.shape.CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMenuOption(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                onClick = onClick,
            )
            .padding(vertical = Spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

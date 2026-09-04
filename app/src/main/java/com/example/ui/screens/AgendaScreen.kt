package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaEventEntity
import com.example.data.model.SubjectEntity
import com.example.ui.StudyViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SubjectColorBadge
import com.example.ui.components.formatCountdown
import com.example.ui.components.formatTimestampToDate
import com.example.ui.components.formatTimestampToDateTime
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AgendaScreen(
    viewModel: StudyViewModel,
    onAddEvaluationRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val events by viewModel.agendaEvents.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: À venir, 1: Passées, 2: Toutes
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) } // null = all subjects
    var isCalendarView by remember { mutableStateOf(false) }

    val currentTime = System.currentTimeMillis()

    val filteredEvents = events.filter { event ->
        val matchesTab = when (selectedFilterTab) {
            0 -> event.dateTime >= currentTime && !event.isCompleted
            1 -> event.dateTime < currentTime || event.isCompleted
            else -> true
        }
        val matchesSubject = selectedSubjectFilter == null || event.subjectName == selectedSubjectFilter
        matchesTab && matchesSubject
    }.sortedBy { it.dateTime }

    Box(modifier = modifier.fillMaxSize().testTag("agenda_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Agenda Scolaire",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Évaluations, devoirs & rappels locaux",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // View Toggle (List / Calendar)
                Surface(
                    onClick = { isCalendarView = !isCalendarView },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("agenda_view_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCalendarView) Icons.Default.FormatListBulleted else Icons.Default.CalendarViewMonth,
                            contentDescription = "Basculer la vue",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCalendarView) "Liste" else "Calendrier",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Tabs (À venir / Passées / Toutes)
            TabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedFilterTab == 0,
                    onClick = { selectedFilterTab = 0 },
                    text = { Text("À venir", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_upcoming")
                )
                Tab(
                    selected = selectedFilterTab == 1,
                    onClick = { selectedFilterTab = 1 },
                    text = { Text("Passées", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_past")
                )
                Tab(
                    selected = selectedFilterTab == 2,
                    onClick = { selectedFilterTab = 2 },
                    text = { Text("Toutes", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_all")
                )
            }

            // Subject Filters (Horizontal Chips)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSubjectFilter == null,
                        onClick = { selectedSubjectFilter = null },
                        label = { Text("Toutes matières") }
                    )
                }
                items(subjects) { subject ->
                    FilterChip(
                        selected = selectedSubjectFilter == subject.name,
                        onClick = {
                            selectedSubjectFilter = if (selectedSubjectFilter == subject.name) null else subject.name
                        },
                        label = { Text(subject.name) }
                    )
                }
            }

            // Content
            if (isCalendarView) {
                AgendaCalendarView(events = filteredEvents, onToggleCompleted = { id, comp ->
                    viewModel.toggleEventCompleted(id, comp)
                }, onDelete = { viewModel.deleteAgendaEvent(it) })
            } else {
                if (filteredEvents.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.CalendarMonth,
                        title = "Aucune évaluation trouvée",
                        description = "Planifie tes devoirs, examens et contrôles pour recevoir des rappels automatiques sans Internet.",
                        actionLabel = "Ajouter une évaluation",
                        onActionClick = onAddEvaluationRequested
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredEvents, key = { it.id }) { event ->
                            EvaluationCard(
                                event = event,
                                subjects = subjects,
                                onToggleCompleted = { viewModel.toggleEventCompleted(event.id, !event.isCompleted) },
                                onDelete = { viewModel.deleteAgendaEvent(event) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddEvaluationRequested,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 16.dp)
                .testTag("add_evaluation_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter une évaluation")
        }
    }
}

@Composable
fun EvaluationCard(
    event: AgendaEventEntity,
    subjects: List<SubjectEntity>,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val subject = subjects.firstOrNull { it.name == event.subjectName }
    val colorHex = subject?.colorHex ?: "#4F46E5"
    val subjectColor = parseHexColor(colorHex)

    val priorityColor = when (event.priority) {
        "Urgente" -> AccentRose
        "Haute" -> AccentAmber
        "Basse" -> Color(0xFF64748B)
        else -> PrimaryIndigo
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("evaluation_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject and Evaluation Type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubjectColorBadge(subjectName = event.subjectName, colorHex = colorHex)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = event.evaluationType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Priority Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = event.priority,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title with StrikeThrough if completed
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Terminer",
                        tint = if (event.isCompleted) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 34.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time, Room & Countdown footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 34.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatTimestampToDateTime(event.dateTime),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (event.room.isNotBlank()) {
                        Text(
                            text = "Salle : ${event.room}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!event.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = formatCountdown(event.dateTime),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaCalendarView(
    events: List<AgendaEventEntity>,
    onToggleCompleted: (Long, Boolean) -> Unit,
    onDelete: (AgendaEventEntity) -> Unit
) {
    // Group events by day
    val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
    val sdfDisplayDay = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRANCE)
    val groupedByDay = events.groupBy { sdfDay.format(Date(it.dateTime)) }

    if (groupedByDay.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Default.CalendarMonth,
            title = "Calendrier vide",
            description = "Aucune évaluation ne correspond aux filtres sélectionnés."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedByDay.forEach { (dayKey, dayEvents) ->
                item {
                    val dateSample = Date(dayEvents.first().dateTime)
                    val formattedDay = sdfDisplayDay.format(dateSample).replaceFirstChar { it.uppercase() }

                    Column {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = formattedDay,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dayEvents.forEach { ev ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(ev.dateTime)),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = PrimaryIndigo
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• ${ev.subjectName} (${ev.evaluationType})",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = ev.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                        IconButton(
                                            onClick = { onToggleCompleted(ev.id, !ev.isCompleted) }
                                        ) {
                                            Icon(
                                                imageVector = if (ev.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Terminer",
                                                tint = if (ev.isCompleted) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEvaluationDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        subjectId: Long?,
        subjectName: String,
        title: String,
        type: String,
        dateTime: Long,
        room: String,
        description: String,
        priority: String,
        reminderOption: String,
        reminderHour: Int,
        reminderMinute: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "Mathématiques") }
    var evaluationType by remember { mutableStateOf("Contrôle") }
    var room by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Moyenne") }
    var reminderOption by remember { mutableStateOf("1_DAY_BEFORE") }
    var reminderHour by remember { mutableIntStateOf(8) }

    // Date picker state
    val calendar = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) } }
    var selectedDateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    val evalTypes = listOf("Contrôle", "Devoir", "Interrogation", "Examen", "Exposé", "Concours", "Autre")
    val priorities = listOf("Basse", "Moyenne", "Haute", "Urgente")
    val reminderOptions = listOf(
        "1_DAY_BEFORE" to "1 jour avant (08:00)",
        "2_DAYS_BEFORE" to "2 jours avant (08:00)",
        "SAME_DAY" to "Le jour même (08:00)",
        "NONE" to "Aucun rappel"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une évaluation") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre de l'évaluation *") },
                        placeholder = { Text("ex: Dissertation, Devoir surveillé...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_eval_title_input")
                    )
                }

                // Subject dropdown
                item {
                    Text("Matière :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { subj ->
                            FilterChip(
                                selected = selectedSubject == subj.name,
                                onClick = { selectedSubject = subj.name },
                                label = { Text(subj.name) }
                            )
                        }
                    }
                }

                // Evaluation Type
                item {
                    Text("Type d'évaluation :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(evalTypes) { t ->
                            FilterChip(
                                selected = evaluationType == t,
                                onClick = { evaluationType = t },
                                label = { Text(t) }
                            )
                        }
                    }
                }

                // Date Picker trigger
                item {
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Date de l'évaluation :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatTimestampToDate(selectedDateMillis),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Room (Salle)
                item {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Salle (optionnel)") },
                        placeholder = { Text("ex: B204, Amphithéâtre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Priority
                item {
                    Text("Priorité :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        priorities.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p) }
                            )
                        }
                    }
                }

                // Reminder option
                item {
                    Text("Rappel local hors ligne :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        reminderOptions.forEach { (key, label) ->
                            Surface(
                                onClick = { reminderOption = key },
                                shape = RoundedCornerShape(8.dp),
                                color = if (reminderOption == key) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (reminderOption == key) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (reminderOption == key) FontWeight.Bold else FontWeight.Normal,
                                        color = if (reminderOption == key) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Consignes / Description") },
                        placeholder = { Text("Chapitres 3 et 4, calculatrice autorisée...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val subj = subjects.firstOrNull { it.name == selectedSubject }
                        onConfirm(
                            subj?.id,
                            selectedSubject,
                            title.trim(),
                            evaluationType,
                            selectedDateMillis,
                            room.trim(),
                            description.trim(),
                            priority,
                            reminderOption,
                            reminderHour,
                            0
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_eval_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

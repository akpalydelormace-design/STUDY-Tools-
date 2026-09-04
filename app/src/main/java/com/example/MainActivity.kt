package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AgendaEventEntity
import com.example.data.model.NoteEntity
import com.example.data.model.PdfDocumentEntity
import com.example.ui.StudyViewModel
import com.example.ui.screens.AddEvaluationDialog
import com.example.ui.screens.AddGradeDialog
import com.example.ui.screens.AgendaScreen
import com.example.ui.screens.BulletinScreen
import com.example.ui.screens.GlobalSearchDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PdfScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SubjectManagerDialog
import com.example.ui.dialogs.HistoryDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryIndigo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val studyViewModel: StudyViewModel = viewModel()
            val themeMode by studyViewModel.themeMode.collectAsState()

            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                MainStudyApp(viewModel = studyViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudyApp(viewModel: StudyViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activePdf by viewModel.activePdf.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedTrimestre by viewModel.selectedTrimestre.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSubjectManagerDialog by remember { mutableStateOf(false) }
    var showGlobalSearchDialog by remember { mutableStateOf(false) }
    var showAddEvaluationDialog by remember { mutableStateOf(false) }
    var showAddGradeDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavigationItem("Accueil", Icons.Default.Home, "tab_nav_home"),
        NavigationItem("PDF", Icons.Default.PictureAsPdf, "tab_nav_pdf"),
        NavigationItem("Agenda", Icons.Default.CalendarMonth, "tab_nav_agenda"),
        NavigationItem("Notes", Icons.Default.EditNote, "tab_nav_notes"),
        NavigationItem("Bulletin", Icons.Default.Assessment, "tab_nav_bulletin")
    )

    // In PDF Reader mode, we can hide standard TopAppBar and BottomBar to maximize reading space
    val isReadingPdf = activePdf != null && selectedTab == 1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isReadingPdf) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Study Tools",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showGlobalSearchDialog = true },
                            modifier = Modifier.testTag("global_search_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Recherche globale",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("settings_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Paramètres",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!isReadingPdf) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { viewModel.setSelectedTab(index) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryIndigo,
                                selectedTextColor = PrimaryIndigo,
                                indicatorColor = PrimaryIndigo.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tabIdx -> viewModel.setSelectedTab(tabIdx) },
                    onOpenPdf = { pdf ->
                        viewModel.setSelectedTab(1)
                        viewModel.openPdf(pdf)
                    },
                    onOpenNote = { note ->
                        viewModel.openNote(note)
                    },
                    onAddEvaluationClick = { showAddEvaluationDialog = true },
                    onAddGradeClick = { showAddGradeDialog = true },
                    onAddNoteClick = { viewModel.setSelectedTab(3) },
                    onOpenHistory = { showHistoryDialog = true }
                )
                1 -> PdfScreen(viewModel = viewModel)
                2 -> AgendaScreen(
                    viewModel = viewModel,
                    onAddEvaluationRequested = { showAddEvaluationDialog = true }
                )
                3 -> NotesScreen(viewModel = viewModel)
                4 -> BulletinScreen(
                    viewModel = viewModel,
                    onOpenSubjectManager = { showSubjectManagerDialog = true }
                )
            }
        }
    }

    // Modal dialogs
    if (showGlobalSearchDialog) {
        GlobalSearchDialog(
            viewModel = viewModel,
            onDismiss = { showGlobalSearchDialog = false },
            onOpenPdf = { pdf ->
                viewModel.setSelectedTab(1)
                viewModel.openPdf(pdf)
            },
            onOpenNote = { note ->
                viewModel.openNote(note)
            },
            onOpenAgenda = { event ->
                viewModel.setSelectedTab(2)
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            onOpenHistory = { showHistoryDialog = true }
        )
    }

    if (showSubjectManagerDialog) {
        SubjectManagerDialog(
            viewModel = viewModel,
            onDismiss = { showSubjectManagerDialog = false }
        )
    }

    if (showAddEvaluationDialog) {
        AddEvaluationDialog(
            subjects = subjects,
            onDismiss = { showAddEvaluationDialog = false },
            onConfirm = { subjectId, subjectName, title, type, dateTime, room, description, priority, reminderOption, hour, min ->
                viewModel.addAgendaEvent(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    title = title,
                    evaluationType = type,
                    dateTime = dateTime,
                    room = room,
                    description = description,
                    priority = priority,
                    reminderOption = reminderOption,
                    reminderHour = hour,
                    reminderMinute = min
                )
            }
        )
    }

    if (showAddGradeDialog) {
        AddGradeDialog(
            subjects = subjects,
            presetSubject = null,
            currentTrimestre = selectedTrimestre,
            onDismiss = { showAddGradeDialog = false },
            onConfirm = { subjectId, subjectName, trim, score, outOf, coef, type, comment ->
                viewModel.addGrade(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    trimestre = trim,
                    score = score,
                    outOf = outOf,
                    coefficient = coef,
                    evaluationType = type,
                    date = System.currentTimeMillis(),
                    comment = comment
                )
            }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            viewModel = viewModel,
            onDismiss = { showHistoryDialog = false },
            onOpenPdf = { pdf ->
                viewModel.setSelectedTab(1)
                viewModel.openPdf(pdf)
            },
            onOpenNote = { note ->
                viewModel.openNote(note)
            },
            onOpenAgenda = {
                viewModel.setSelectedTab(2)
            },
            onOpenBulletin = {
                viewModel.setSelectedTab(4)
            }
        )
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)

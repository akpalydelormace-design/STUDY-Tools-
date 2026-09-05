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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import com.example.data.model.GradeEntity
import com.example.data.model.HistoryTypes
import com.example.data.model.SubjectEntity
import com.example.domain.GradeCalculator
import com.example.domain.SubjectGradeSummary
import com.example.ui.StudyViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SubjectColorBadge
import com.example.ui.components.formatTimestampToDate
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan

@Composable
fun BulletinScreen(
    viewModel: StudyViewModel,
    onOpenSubjectManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTrimestre by viewModel.selectedTrimestre.collectAsState()
    val report by viewModel.currentTrimestreReport.collectAsState()
    val allReports by viewModel.allTrimestresReports.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedSchoolYear by viewModel.selectedSchoolYear.collectAsState()

    var showAddGradeDialog by remember { mutableStateOf(false) }
    var presetSubjectForGrade by remember { mutableStateOf<SubjectEntity?>(null) }
    var gradeToDelete by remember { mutableStateOf<GradeEntity?>(null) }
    var gradeToEdit by remember { mutableStateOf<GradeEntity?>(null) }

    // Progression
    val currentAvg = report.generalAverage
    val prevAvg = if (selectedTrimestre > 1) allReports[selectedTrimestre - 1]?.generalAverage else null
    val progression = GradeCalculator.calculateProgression(prevAvg, currentAvg)

    LaunchedEffect(selectedTrimestre) {
        viewModel.recordHistory(
            resourceType = HistoryTypes.BULLETIN,
            resourceId = "trimestre_$selectedTrimestre",
            title = "Bulletin — Trimestre $selectedTrimestre",
            subtitle = if (currentAvg != null) "Moyenne: ${GradeCalculator.formatScore(currentAvg)}/20" else "Trimestre en cours",
            actionType = "CONSULTED"
        )
    }

    Box(modifier = modifier.fillMaxSize().testTag("bulletin_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bulletin Scolaire",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Moyennes pondérées avec tes coefficients configurés",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Subjects management button
                    Surface(
                        onClick = onOpenSubjectManager,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("manage_subjects_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Matières",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Matières", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        }
                    }
                }
            }

            // Trimestre TabRow
            item {
                OutlinedTextField(
                    value = selectedSchoolYear,
                    onValueChange = viewModel::setSelectedSchoolYear,
                    label = { Text("Année scolaire") },
                    placeholder = { Text("ex. 2025-2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("school_year_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTrimestre - 1,
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Tab(
                        selected = selectedTrimestre == 1,
                        onClick = { viewModel.setSelectedTrimestre(1) },
                        text = { Text("Trimestre 1", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_t1")
                    )
                    Tab(
                        selected = selectedTrimestre == 2,
                        onClick = { viewModel.setSelectedTrimestre(2) },
                        text = { Text("Trimestre 2", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_t2")
                    )
                    Tab(
                        selected = selectedTrimestre == 3,
                        onClick = { viewModel.setSelectedTrimestre(3) },
                        text = { Text("Trimestre 3", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_t3")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // High Impact General Average Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryIndigo, SecondaryCyan)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "Moyenne générale — Trimestre $selectedTrimestre",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = GradeCalculator.formatScore(currentAvg),
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = " / 20",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Total coef : ${report.totalCoefficients.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Progression indicator
                            if (progression != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (progression >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${if (progression >= 0) "+" else ""}${GradeCalculator.formatScore(progression)} pts par rapport au T${selectedTrimestre - 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Highlights (Meilleure matière / À consolider)
                            if (report.bestSubject != null || report.worstSubject != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    report.bestSubject?.let { best ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Meilleure matière : ${best.subjectName} (${GradeCalculator.formatScore(best.averageScore)}/20)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    report.worstSubject?.let { worst ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = AccentRose, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "À consolider : ${worst.subjectName} (${GradeCalculator.formatScore(worst.averageScore)}/20)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subjects list & their grades
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notes par matière",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (report.subjects.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Grade,
                        title = "Aucune matière enregistrée",
                        description = "Ajoute tes matières et tes premières notes pour calculer ta moyenne automatiquement.",
                        actionLabel = "Gérer les matières",
                        onActionClick = onOpenSubjectManager
                    )
                }
            } else {
                items(report.subjects, key = { it.subjectId }) { subjSummary ->
                    SubjectBulletinCard(
                        summary = subjSummary,
                        onAddGrade = {
                            presetSubjectForGrade = subjects.firstOrNull { it.id == subjSummary.subjectId }
                            showAddGradeDialog = true
                        },
                        onDeleteGrade = { gradeToDelete = it },
                        onEditGrade = { gradeToEdit = it }
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                presetSubjectForGrade = null
                showAddGradeDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 16.dp)
                .testTag("add_grade_fab"),
            containerColor = AccentEmerald,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter une note")
        }
    }

    // Add Grade Dialog
    if (showAddGradeDialog) {
        AddGradeDialog(
            subjects = subjects,
            presetSubject = presetSubjectForGrade,
            currentTrimestre = selectedTrimestre,
            schoolYear = selectedSchoolYear,
            onDismiss = { showAddGradeDialog = false },
            onConfirm = { subjectId, subjectName, trim, score, outOf, coef, type, comment, schoolYear ->
                viewModel.addGrade(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    trimestre = trim,
                    score = score,
                    outOf = outOf,
                    coefficient = coef,
                    evaluationType = type,
                    date = System.currentTimeMillis(),
                    comment = comment,
                    schoolYear = schoolYear
                )
                showAddGradeDialog = false
            }
        )
    }

    if (gradeToEdit != null) {
        AddGradeDialog(
            subjects = subjects,
            presetSubject = subjects.firstOrNull { it.id == gradeToEdit?.subjectId },
            currentTrimestre = gradeToEdit!!.trimestre,
            schoolYear = gradeToEdit!!.schoolYear,
            initialGrade = gradeToEdit,
            onDismiss = { gradeToEdit = null },
            onConfirm = { subjectId, subjectName, trim, score, outOf, coef, type, comment, year ->
                viewModel.updateGrade(gradeToEdit!!.copy(
                    subjectId = subjectId, subjectName = subjectName, trimestre = trim,
                    score = score, outOf = outOf, coefficient = coef, evaluationType = type,
                    comment = comment, schoolYear = year
                ))
                gradeToEdit = null
            }
        )
    }

    // Delete Grade Confirmation Dialog
    if (gradeToDelete != null) {
        AlertDialog(
            onDismissRequest = { gradeToDelete = null },
            title = { Text("Supprimer cette note ?") },
            text = {
                Text("Voulez-vous supprimer la note de ${gradeToDelete?.score}/${gradeToDelete?.outOf?.toInt()} en ${gradeToDelete?.subjectName} ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        gradeToDelete?.let { viewModel.deleteGrade(it) }
                        gradeToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { gradeToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun SubjectBulletinCard(
    summary: SubjectGradeSummary,
    onAddGrade: () -> Unit,
    onDeleteGrade: (GradeEntity) -> Unit,
    onEditGrade: (GradeEntity) -> Unit
) {
    val colorHex = summary.subjectColor
    val parsedColor = parseHexColor(colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("subject_card_${summary.subjectId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Subject header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = summary.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Coefficient ${summary.subjectCoefficient.toInt()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Average badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (summary.averageScore != null) {
                        if (summary.averageScore >= 12f) AccentEmerald.copy(alpha = 0.15f)
                        else if (summary.averageScore >= 10f) AccentAmber.copy(alpha = 0.15f)
                        else AccentRose.copy(alpha = 0.15f)
                    } else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = GradeCalculator.formatScore(summary.averageScore),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = if (summary.averageScore != null) {
                                if (summary.averageScore >= 12f) AccentEmerald
                                else if (summary.averageScore >= 10f) AccentAmber
                                else AccentRose
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " / 20",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }

            // Grades list for this subject
            if (summary.grades.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.grades.forEach { grade ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${GradeCalculator.formatScore(grade.score)}/${GradeCalculator.formatScore(grade.outOf)} • ${GradeCalculator.formatScore(GradeCalculator.normalizeToTwenty(grade.score, grade.outOf))}/20",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "coef ${grade.coefficient.toInt()}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = grade.evaluationType,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatTimestampToDate(grade.date),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onEditGrade(grade) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(16.dp))
                                    }
                                    if (grade.comment.isNotBlank()) {
                                        Text(
                                            text = grade.comment,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteGrade(grade) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aucune note saisie pour ce trimestre.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick add note button for this subject
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = onAddGrade,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().testTag("add_grade_for_subject_${summary.subjectId}")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ajouter une note en ${summary.subjectName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo
                    )
                }
            }
        }
    }
}

@Composable
fun AddGradeDialog(
    subjects: List<SubjectEntity>,
    presetSubject: SubjectEntity?,
    currentTrimestre: Int,
    schoolYear: String = GradeCalculator.currentSchoolYear(),
    onDismiss: () -> Unit,
    initialGrade: GradeEntity? = null,
    onConfirm: (
        subjectId: Long,
        subjectName: String,
        trimestre: Int,
        score: Float,
        outOf: Float,
        coefficient: Float,
        evaluationType: String,
        comment: String,
        schoolYear: String
    ) -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(presetSubject ?: subjects.firstOrNull())
    }
    var scoreInput by remember { mutableStateOf(initialGrade?.score?.toString() ?: "") }
    var outOfInput by remember { mutableStateOf(initialGrade?.outOf?.toString() ?: "20") }
    var coefInput by remember { mutableStateOf(initialGrade?.coefficient?.toString() ?: "1") }
    var evaluationType by remember { mutableStateOf(initialGrade?.evaluationType ?: "Contrôle") }
    var trimestre by remember { mutableIntStateOf(currentTrimestre) }
    var comment by remember { mutableStateOf(initialGrade?.comment ?: "") }
    var yearInput by remember { mutableStateOf(schoolYear) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val evalTypes = listOf("Contrôle", "Devoir", "Interrogation", "Examen", "Oral", "TP")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGrade == null) "Ajouter une note" else "Modifier la note") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Subject selection
                item {
                    Text("Matière :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = selectedSubject?.id == s.id,
                                onClick = { selectedSubject = s },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }

                // Trimestre
                item {
                    OutlinedTextField(
                        value = yearInput,
                        onValueChange = { yearInput = it },
                        label = { Text("Année scolaire *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Trimestre :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { t ->
                            FilterChip(
                                selected = trimestre == t,
                                onClick = { trimestre = t },
                                label = { Text("T$t") }
                            )
                        }
                    }
                }

                // Score & OutOf row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = scoreInput,
                            onValueChange = { scoreInput = it },
                            label = { Text("Note obtenue *") },
                            placeholder = { Text("ex: 15.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("grade_score_input")
                        )
                        OutlinedTextField(
                            value = outOfInput,
                            onValueChange = { outOfInput = it },
                            label = { Text("Barème") },
                            placeholder = { Text("20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (validationError != null) item { Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                // Coefficient
                item {
                    OutlinedTextField(
                        value = coefInput,
                        onValueChange = { coefInput = it },
                        label = { Text("Coefficient de l'évaluation") },
                        placeholder = { Text("1.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Evaluation Type
                item {
                    Text("Type :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(evalTypes) { t ->
                            FilterChip(
                                selected = evaluationType == t,
                                onClick = { evaluationType = t },
                                label = { Text(t) }
                            )
                        }
                    }
                }

                // Comment
                item {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Commentaire / Appréciation (optionnel)") },
                        placeholder = { Text("Bon travail, soigner la rédaction...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = scoreInput.replace(",", ".").toFloatOrNull()
                    val outOf = outOfInput.replace(",", ".").toFloatOrNull()
                    val coef = coefInput.replace(",", ".").toFloatOrNull()
                    val subj = selectedSubject
                    val validation = if (score == null || outOf == null || coef == null) null else
                        GradeCalculator.validateGrade(score, outOf, coef, trimestre, yearInput.trim())
                    validationError = when {
                        subj == null -> "Choisis une matière."
                        validation == null -> "La note, le barème et le coefficient doivent être des nombres valides."
                        !validation.isValid -> validation.message
                        else -> null
                    }
                    if (subj != null && validation?.isValid == true) {
                        onConfirm(
                            subj.id,
                            subj.name,
                            trimestre,
                            score!!,
                            outOf!!,
                            coef!!,
                            evaluationType,
                            comment.trim(),
                            yearInput.trim()
                        )
                    }
                },
                enabled = scoreInput.isNotBlank() && selectedSubject != null,
                modifier = Modifier.testTag("confirm_add_grade_button")
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
}

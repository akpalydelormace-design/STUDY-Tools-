package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyDatabase
import com.example.data.model.AgendaEventEntity
import com.example.data.model.GradeEntity
import com.example.data.model.HistoryEntity
import com.example.data.model.HistoryTypes
import com.example.data.model.NoteEntity
import com.example.data.model.NotebookEntity
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.CanvasData
import com.example.data.model.CanvasElement
import com.example.data.model.NoteTypes
import com.example.data.pdf.PdfHelper
import com.example.data.pdf.PdfSearchResult
import com.example.data.repository.StudyRepository
import com.example.domain.GradeCalculator
import com.example.domain.TrimestreReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed class GlobalSearchResult(val typeLabel: String, val title: String, val subtitle: String) {
    class PdfResult(val pdf: PdfDocumentEntity, val page: Int = 1) :
        GlobalSearchResult("📖 PDF", "${pdf.title} — page $page", "Document PDF (${pdf.pageCount} pages)")

    class NoteResult(val note: NoteEntity) :
        GlobalSearchResult(
            when (note.noteType) {
                com.example.data.model.NoteTypes.CANVAS -> "🎨 Super Note"
                com.example.data.model.NoteTypes.MINDMAP -> "🧠 Carte mentale"
                else -> "📝 Note"
            },
            note.title,
            if (note.attachedPdfTitle.isNotBlank()) {
                "${note.notebookTitle} • Réf: ${note.attachedPdfTitle} (p.${note.attachedPdfPage ?: 1})"
            } else {
                "${note.notebookTitle} • ${note.content.take(60)}"
            }
        )

    class AgendaResult(val event: AgendaEventEntity) :
        GlobalSearchResult("📅 Agenda", "${event.subjectName} — ${event.title}", "Évaluation: ${event.evaluationType}")
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyDatabase.getInstance(application)
    val repository = StudyRepository(application, db)

    // Navigation & Tab state
    private val _selectedTab = MutableStateFlow(0) // 0: Accueil, 1: PDF, 2: Agenda, 3: Notes, 4: Bulletin
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    // Selected Trimestre (1, 2, 3)
    private val _selectedTrimestre = MutableStateFlow(1)
    val selectedTrimestre: StateFlow<Int> = _selectedTrimestre.asStateFlow()

    fun setSelectedTrimestre(t: Int) {
        if (t in 1..3) _selectedTrimestre.value = t
    }

    private val _selectedSchoolYear = MutableStateFlow(GradeCalculator.currentSchoolYear())
    val selectedSchoolYear: StateFlow<String> = _selectedSchoolYear.asStateFlow()

    fun setSelectedSchoolYear(year: String) {
        if (year.isNotBlank()) _selectedSchoolYear.value = year.trim()
    }

    // Theme mode: "SYSTEM", "LIGHT", "DARK"
    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    // Data streams from repository
    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val grades: StateFlow<List<GradeEntity>> = repository.allGrades.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val agendaEvents: StateFlow<List<AgendaEventEntity>> = repository.allAgendaEvents.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val notebooks: StateFlow<List<NotebookEntity>> = repository.allNotebooks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val pdfs: StateFlow<List<PdfDocumentEntity>> = repository.allPdfs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val recentPdfs: StateFlow<List<PdfDocumentEntity>> = repository.recentPdfs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val recentHistory: StateFlow<List<HistoryEntity>> = repository.recentHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Current Trimestre Report
    val currentTrimestreReport: StateFlow<TrimestreReport> = combine(
        _selectedTrimestre,
        _selectedSchoolYear,
        subjects,
        grades
    ) { trim, schoolYear, subjs, grds ->
        GradeCalculator.buildTrimestreReport(trim, subjs, grds, schoolYear)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TrimestreReport(GradeCalculator.currentSchoolYear(), 1, emptyList(), null, 0f, null, null)
    )

    // Reports for all 3 trimestres to calculate evolution
    val allTrimestresReports: StateFlow<Map<Int, TrimestreReport>> = combine(
        _selectedSchoolYear,
        subjects,
        grades
    ) { schoolYear, subjs, grds ->
        mapOf(
            1 to GradeCalculator.buildTrimestreReport(1, subjs, grds, schoolYear),
            2 to GradeCalculator.buildTrimestreReport(2, subjs, grds, schoolYear),
            3 to GradeCalculator.buildTrimestreReport(3, subjs, grds, schoolYear)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

    // Overall General Average across all available grades
    val generalAverageAcrossTrimestres: StateFlow<Float?> = currentTrimestreReport.combine(allTrimestresReports) { curr, all ->
        val gradedTrimestres = all.values.mapNotNull { it.generalAverage }
        if (gradedTrimestres.isNotEmpty()) {
            val avg = gradedTrimestres.sum() / gradedTrimestres.size
            GradeCalculator.roundToTwoDecimals(avg)
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ==========================================
    // PDF READER STATE
    // ==========================================
    private val _activePdf = MutableStateFlow<PdfDocumentEntity?>(null)
    val activePdf: StateFlow<PdfDocumentEntity?> = _activePdf.asStateFlow()

    private val _currentPdfPage = MutableStateFlow(1)
    val currentPdfPage: StateFlow<Int> = _currentPdfPage.asStateFlow()

    private val _currentPdfBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPdfBitmap: StateFlow<Bitmap?> = _currentPdfBitmap.asStateFlow()

    private val _isPdfLoading = MutableStateFlow(false)
    val isPdfLoading: StateFlow<Boolean> = _isPdfLoading.asStateFlow()

    // PDF Search state
    private val _pdfSearchQuery = MutableStateFlow("")
    val pdfSearchQuery: StateFlow<String> = _pdfSearchQuery.asStateFlow()

    private val _pdfSearchResults = MutableStateFlow<List<PdfSearchResult>>(emptyList())
    val pdfSearchResults: StateFlow<List<PdfSearchResult>> = _pdfSearchResults.asStateFlow()

    private val _currentPdfSearchMatchIndex = MutableStateFlow(0) // 0-based
    val currentPdfSearchMatchIndex: StateFlow<Int> = _currentPdfSearchMatchIndex.asStateFlow()

    private val _pdfErrorMessage = MutableStateFlow<String?>(null)
    val pdfErrorMessage: StateFlow<String?> = _pdfErrorMessage.asStateFlow()

    fun clearPdfErrorMessage() {
        _pdfErrorMessage.value = null
    }

    private var pdfPagesTextCache = mapOf<Int, String>()

    fun openPdf(pdf: PdfDocumentEntity, initialPage: Int? = null) {
        val file = File(pdf.localFilePath)
        if (!file.exists()) {
            _pdfErrorMessage.value = "Document indisponible : le fichier « ${pdf.title} » est introuvable."
            return
        }
        _pdfErrorMessage.value = null
        _activePdf.value = pdf
        val startPage = (initialPage ?: pdf.lastPageRead).coerceIn(1, pdf.pageCount.coerceAtLeast(1))
        _currentPdfPage.value = startPage
        _pdfSearchQuery.value = ""
        _pdfSearchResults.value = emptyList()
        _currentPdfSearchMatchIndex.value = 0

        loadPdfPage(pdf, startPage)

        // Record in intelligent history
        viewModelScope.launch {
            repository.recordHistory(
                resourceType = HistoryTypes.PDF,
                resourceId = pdf.id.toString(),
                title = pdf.title,
                subtitle = "Page $startPage / ${pdf.pageCount}",
                actionType = "OPENED",
                extraData = startPage.toString()
            )
        }

        // Pre-cache text in background for instant search
        viewModelScope.launch {
            if (file.exists()) {
                pdfPagesTextCache = PdfHelper.extractTextByPages(file)
            }
        }
    }

    fun openPdfAtPage(pdfId: Long, page: Int = 1, onUnavailable: () -> Unit = {}) {
        val pdf = pdfs.value.find { it.id == pdfId }
        if (pdf != null) {
            val file = File(pdf.localFilePath)
            if (file.exists()) {
                openPdf(pdf, page)
                setSelectedTab(1) // PDF tab
            } else {
                _pdfErrorMessage.value = "Document indisponible : le fichier « ${pdf.title} » est introuvable."
                onUnavailable()
            }
        } else {
            _pdfErrorMessage.value = "Document indisponible : le fichier a été supprimé de la bibliothèque."
            onUnavailable()
        }
    }

    fun closePdfReader() {
        _activePdf.value?.let { pdf ->
            val page = _currentPdfPage.value
            viewModelScope.launch {
                repository.updatePdfLastPage(pdf.id, page)
            }
        }
        _activePdf.value = null
        _currentPdfBitmap.value = null
        pdfPagesTextCache = emptyMap()
    }

    fun goToPdfPage(page: Int) {
        val pdf = _activePdf.value ?: return
        val validPage = page.coerceIn(1, pdf.pageCount.coerceAtLeast(1))
        _currentPdfPage.value = validPage
        loadPdfPage(pdf, validPage)
        viewModelScope.launch {
            repository.updatePdfLastPage(pdf.id, validPage)
            repository.recordHistory(
                resourceType = HistoryTypes.PDF,
                resourceId = pdf.id.toString(),
                title = pdf.title,
                subtitle = "Page $validPage / ${pdf.pageCount}",
                actionType = "READ",
                extraData = validPage.toString()
            )
        }
    }

    fun nextPdfPage() {
        goToPdfPage(_currentPdfPage.value + 1)
    }

    fun prevPdfPage() {
        goToPdfPage(_currentPdfPage.value - 1)
    }

    private fun loadPdfPage(pdf: PdfDocumentEntity, page: Int) {
        viewModelScope.launch {
            _isPdfLoading.value = true
            val file = File(pdf.localFilePath)
            if (file.exists()) {
                val bmp = PdfHelper.renderPageBitmap(file, page - 1, targetWidth = 1400)
                _currentPdfBitmap.value = bmp
            }
            _isPdfLoading.value = false
        }
    }

    fun performPdfSearch(query: String) {
        _pdfSearchQuery.value = query
        if (query.isBlank()) {
            _pdfSearchResults.value = emptyList()
            _currentPdfSearchMatchIndex.value = 0
            return
        }

        viewModelScope.launch {
            val results = PdfHelper.searchInPages(pdfPagesTextCache, query)
            _pdfSearchResults.value = results
            _currentPdfSearchMatchIndex.value = 0
            if (results.isNotEmpty()) {
                // Jump to first result's page
                val match = results[0]
                goToPdfPage(match.pageIndex + 1)
            }
        }
    }

    fun nextPdfSearchResult() {
        val results = _pdfSearchResults.value
        if (results.isEmpty()) return
        val nextIdx = (_currentPdfSearchMatchIndex.value + 1) % results.size
        _currentPdfSearchMatchIndex.value = nextIdx
        goToPdfPage(results[nextIdx].pageIndex + 1)
    }

    fun prevPdfSearchResult() {
        val results = _pdfSearchResults.value
        if (results.isEmpty()) return
        val prevIdx = if (_currentPdfSearchMatchIndex.value - 1 < 0) results.size - 1 else _currentPdfSearchMatchIndex.value - 1
        _currentPdfSearchMatchIndex.value = prevIdx
        goToPdfPage(results[prevIdx].pageIndex + 1)
    }

    fun importPdfUri(uri: Uri, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.importPdf(uri)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun deletePdf(pdf: PdfDocumentEntity) {
        viewModelScope.launch {
            if (_activePdf.value?.id == pdf.id) {
                closePdfReader()
            }
            repository.deletePdfFromLibrary(pdf)
        }
    }

    // ==========================================
    // GLOBAL SEARCH
    // ==========================================
    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    val globalSearchResults: StateFlow<List<GlobalSearchResult>> = combine(
        _globalSearchQuery,
        pdfs,
        notes,
        agendaEvents
    ) { q, pdfList, noteList, eventList ->
        if (q.isBlank()) return@combine emptyList()
        val query = q.trim().lowercase()
        val matches = mutableListOf<GlobalSearchResult>()

        // 1. PDF matches
        for (pdf in pdfList) {
            if (pdf.title.lowercase().contains(query) || pdf.extractedText.lowercase().contains(query)) {
                matches.add(GlobalSearchResult.PdfResult(pdf))
            }
        }

        // 2. Note matches
        for (note in noteList) {
            if (note.title.lowercase().contains(query) ||
                note.content.lowercase().contains(query) ||
                note.subjectName.lowercase().contains(query) ||
                note.notebookTitle.lowercase().contains(query) ||
                note.canvasDataJson.lowercase().contains(query) ||
                note.mindMapDataJson.lowercase().contains(query) ||
                note.folderName.lowercase().contains(query)
            ) {
                matches.add(GlobalSearchResult.NoteResult(note))
            }
        }

        // 3. Agenda matches
        for (event in eventList) {
            if (event.title.lowercase().contains(query) ||
                event.subjectName.lowercase().contains(query) ||
                event.description.lowercase().contains(query)
            ) {
                matches.add(GlobalSearchResult.AgendaResult(event))
            }
        }

        matches
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGlobalSearchQuery(q: String) {
        _globalSearchQuery.value = q
    }

    // ==========================================
    // SUBJECTS & GRADES ACTIONS
    // ==========================================
    fun addSubject(name: String, coefficient: Float, colorHex: String, iconName: String = "School") {
        viewModelScope.launch {
            repository.addSubject(name, coefficient, colorHex, iconName)
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun addGrade(
        subjectId: Long,
        subjectName: String,
        trimestre: Int,
        score: Float,
        outOf: Float,
        coefficient: Float,
        evaluationType: String,
        date: Long,
        comment: String,
        schoolYear: String = _selectedSchoolYear.value
    ) {
        viewModelScope.launch {
            repository.addGrade(
                subjectId = subjectId,
                subjectName = subjectName,
                trimestre = trimestre,
                score = score,
                outOf = outOf,
                coefficient = coefficient,
                evaluationType = evaluationType,
                date = date,
                comment = comment,
                schoolYear = schoolYear
            )
        }
    }

    fun updateGrade(grade: GradeEntity) {
        viewModelScope.launch { repository.updateGrade(grade) }
    }

    fun deleteGrade(grade: GradeEntity) {
        viewModelScope.launch {
            repository.deleteGrade(grade)
        }
    }

    // ==========================================
    // AGENDA ACTIONS
    // ==========================================
    fun addAgendaEvent(
        subjectId: Long?,
        subjectName: String,
        title: String,
        evaluationType: String,
        dateTime: Long,
        room: String,
        description: String,
        priority: String,
        reminderOption: String,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        viewModelScope.launch {
            val eventId = repository.addAgendaEvent(
                subjectId = subjectId,
                subjectName = subjectName,
                title = title,
                evaluationType = evaluationType,
                dateTime = dateTime,
                room = room,
                description = description,
                priority = priority,
                reminderOption = reminderOption,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            repository.recordHistory(
                resourceType = HistoryTypes.AGENDA,
                resourceId = eventId.toString(),
                title = "$subjectName — $title",
                subtitle = evaluationType,
                actionType = "CREATED"
            )
        }
    }

    fun toggleEventCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleEventCompleted(id, completed)
        }
    }

    fun deleteAgendaEvent(event: AgendaEventEntity) {
        viewModelScope.launch {
            repository.deleteAgendaEvent(event)
        }
    }

    // ==========================================
    // NOTEBOOKS & NOTES ACTIONS
    // ==========================================
    fun addNotebook(title: String, subjectName: String, colorHex: String, iconEmoji: String) {
        viewModelScope.launch {
            repository.addNotebook(title, subjectName, colorHex, iconEmoji)
        }
    }

    fun deleteNotebook(notebook: NotebookEntity) {
        viewModelScope.launch {
            repository.deleteNotebook(notebook)
        }
    }

    // Active note editor state
    private val _activeNoteToEdit = MutableStateFlow<NoteEntity?>(null)
    val activeNoteToEdit: StateFlow<NoteEntity?> = _activeNoteToEdit.asStateFlow()

    fun openNote(note: NoteEntity) {
        _activeNoteToEdit.value = note
        _selectedTab.value = 3
        viewModelScope.launch {
            val type = when (note.noteType) {
                com.example.data.model.NoteTypes.CANVAS -> HistoryTypes.SUPER_NOTE
                com.example.data.model.NoteTypes.MINDMAP -> HistoryTypes.MIND_MAP
                else -> HistoryTypes.NOTE
            }
            repository.recordHistory(
                resourceType = type,
                resourceId = note.id.toString(),
                title = note.title.ifBlank { "Note sans titre" },
                subtitle = "${note.notebookTitle} • ${note.subjectName}".trim().removePrefix("•").removeSuffix("•").trim(),
                actionType = "OPENED"
            )
        }
    }

    fun closeNote() {
        _activeNoteToEdit.value = null
    }

    fun addNote(
        notebookId: Long,
        notebookTitle: String,
        title: String,
        content: String = "",
        subjectName: String = "",
        category: String = "Cours",
        attachmentsJson: String = "[]",
        noteType: String = com.example.data.model.NoteTypes.TEXT,
        canvasDataJson: String = "",
        mindMapDataJson: String = "",
        folderName: String = "Cours",
        attachedPdfId: Long? = null,
        attachedPdfTitle: String = "",
        attachedPdfPage: Int? = null,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val newId = repository.addNote(
                notebookId = notebookId,
                notebookTitle = notebookTitle,
                title = title,
                content = content,
                subjectName = subjectName,
                category = category,
                attachmentsJson = attachmentsJson,
                noteType = noteType,
                canvasDataJson = canvasDataJson,
                mindMapDataJson = mindMapDataJson,
                folderName = folderName,
                attachedPdfId = attachedPdfId,
                attachedPdfTitle = attachedPdfTitle,
                attachedPdfPage = attachedPdfPage
            )
            onComplete(newId)
        }
    }

    fun createNoteFromPdf(
        pdf: PdfDocumentEntity,
        page: Int,
        noteType: String = NoteTypes.TEXT,
        snippet: String = "",
        searchQuery: String = ""
    ) {
        viewModelScope.launch {
            val defaultNb = notebooks.value.firstOrNull()
            val nbId = defaultNb?.id ?: 1L
            val nbTitle = defaultNb?.title ?: "Général"
            val subjectName = defaultNb?.subjectName ?: "Général"

            val baseTitle = if (searchQuery.isNotBlank()) "Extrait : $searchQuery" else "${pdf.title.removeSuffix(".pdf")} — p.$page"

            when (noteType) {
                NoteTypes.CANVAS -> {
                    val canvasData = CanvasData(
                        elements = listOf(
                            CanvasElement.PdfRef(
                                id = UUID.randomUUID().toString(),
                                x = 120f,
                                y = 120f,
                                width = 300f,
                                height = if (snippet.isNotBlank()) 150f else 120f,
                                pdfId = pdf.id,
                                pdfTitle = pdf.title,
                                pageNumber = page,
                                noteSnippet = snippet
                            )
                        )
                    )
                    val newId = repository.addNote(
                        notebookId = nbId,
                        notebookTitle = nbTitle,
                        title = baseTitle,
                        content = "Super Note avec référence PDF : ${pdf.title}",
                        subjectName = subjectName,
                        category = "Cours",
                        noteType = NoteTypes.CANVAS,
                        canvasDataJson = canvasData.toJson(),
                        folderName = "Cours",
                        attachedPdfId = pdf.id,
                        attachedPdfTitle = pdf.title,
                        attachedPdfPage = page
                    )
                    val note = repository.getNoteById(newId)
                    if (note != null) {
                        openNote(note)
                    }
                }
                else -> {
                    val contentBuilder = StringBuilder()
                    contentBuilder.append("Source\n${pdf.title} — page $page\n\n")
                    if (snippet.isNotBlank()) {
                        contentBuilder.append("« $snippet »\n\n")
                    }
                    contentBuilder.append("Notes : \n")

                    val newId = repository.addNote(
                        notebookId = nbId,
                        notebookTitle = nbTitle,
                        title = baseTitle,
                        content = contentBuilder.toString(),
                        subjectName = subjectName,
                        category = "Cours",
                        noteType = NoteTypes.TEXT,
                        folderName = "Cours",
                        attachedPdfId = pdf.id,
                        attachedPdfTitle = pdf.title,
                        attachedPdfPage = page
                    )
                    val note = repository.getNoteById(newId)
                    if (note != null) {
                        openNote(note)
                    }
                }
            }
        }
    }

    fun appendPdfToExistingNote(
        targetNote: NoteEntity,
        pdf: PdfDocumentEntity,
        page: Int,
        snippet: String = ""
    ) {
        viewModelScope.launch {
            when (targetNote.noteType) {
                NoteTypes.CANVAS -> {
                    val currentData = CanvasData.fromJson(targetNote.canvasDataJson)
                    val newPdfRef = CanvasElement.PdfRef(
                        id = UUID.randomUUID().toString(),
                        x = (currentData.elements.maxOfOrNull { it.x + it.width } ?: 120f) + 40f,
                        y = 120f,
                        width = 300f,
                        height = if (snippet.isNotBlank()) 150f else 120f,
                        pdfId = pdf.id,
                        pdfTitle = pdf.title,
                        pageNumber = page,
                        noteSnippet = snippet
                    )
                    val updatedData = currentData.copy(
                        elements = currentData.elements + newPdfRef
                    )
                    val updated = targetNote.copy(
                        canvasDataJson = updatedData.toJson(),
                        attachedPdfId = targetNote.attachedPdfId ?: pdf.id,
                        attachedPdfTitle = if (targetNote.attachedPdfTitle.isBlank()) pdf.title else targetNote.attachedPdfTitle,
                        attachedPdfPage = targetNote.attachedPdfPage ?: page,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateNote(updated)
                    openNote(updated)
                }
                else -> {
                    val addition = buildString {
                        append("\n\n---\n")
                        append("Source\n${pdf.title} — page $page\n")
                        if (snippet.isNotBlank()) {
                            append("« $snippet »\n")
                        }
                    }
                    val updated = targetNote.copy(
                        content = targetNote.content + addition,
                        attachedPdfId = targetNote.attachedPdfId ?: pdf.id,
                        attachedPdfTitle = if (targetNote.attachedPdfTitle.isBlank()) pdf.title else targetNote.attachedPdfTitle,
                        attachedPdfPage = targetNote.attachedPdfPage ?: page,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateNote(updated)
                    openNote(updated)
                }
            }
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
            val type = when (note.noteType) {
                com.example.data.model.NoteTypes.CANVAS -> HistoryTypes.SUPER_NOTE
                com.example.data.model.NoteTypes.MINDMAP -> HistoryTypes.MIND_MAP
                else -> HistoryTypes.NOTE
            }
            repository.recordHistory(
                resourceType = type,
                resourceId = note.id.toString(),
                title = note.title.ifBlank { "Note sans titre" },
                subtitle = "${note.notebookTitle} • ${note.subjectName}".trim().removePrefix("•").removeSuffix("•").trim(),
                actionType = "EDITED"
            )
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun toggleNoteFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleNoteFavorite(id, isFavorite)
        }
    }

    // ==========================================
    // INTELLIGENT HISTORY ACTIONS
    // ==========================================
    fun recordHistory(
        resourceType: String,
        resourceId: String,
        title: String,
        subtitle: String = "",
        actionType: String = "",
        extraData: String = ""
    ) {
        viewModelScope.launch {
            repository.recordHistory(
                resourceType = resourceType,
                resourceId = resourceId,
                title = title,
                subtitle = subtitle,
                actionType = actionType,
                extraData = extraData
            )
        }
    }

    fun deleteHistoryEntry(entry: HistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(entry)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // ==========================================
    // BACKUP & RESET
    // ==========================================
    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataJson()
            onResult(json)
        }
    }

    fun importBackupJson(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.importDataJson(jsonString)
            onResult(ok)
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            onComplete()
        }
    }
}

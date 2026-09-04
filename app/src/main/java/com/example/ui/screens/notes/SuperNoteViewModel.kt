package com.example.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CanvasData
import com.example.data.model.CanvasElement
import com.example.data.model.CanvasTool
import com.example.data.model.DrawingStroke
import com.example.data.model.HistoryTypes
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTypes
import com.example.data.model.StrokePoint
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.hypot

data class CanvasSnapshot(
    val elements: List<CanvasElement>,
    val strokes: List<DrawingStroke>
)

class SuperNoteViewModel(
    private val repository: StudyRepository,
    private val initialNote: NoteEntity?,
    defaultNotebookId: Long,
    defaultNotebookTitle: String,
    defaultSubjectName: String
) : ViewModel() {

    // Note identity & metadata
    private val _noteId = MutableStateFlow<Long?>(initialNote?.id)
    val noteId: StateFlow<Long?> = _noteId.asStateFlow()

    private val _title = MutableStateFlow(initialNote?.title ?: "Super Note")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _selectedSubjectName = MutableStateFlow(initialNote?.subjectName ?: defaultSubjectName)
    val selectedSubjectName: StateFlow<String> = _selectedSubjectName.asStateFlow()

    private val _selectedNotebookId = MutableStateFlow(initialNote?.notebookId ?: defaultNotebookId)
    val selectedNotebookId: StateFlow<Long> = _selectedNotebookId.asStateFlow()

    private val _selectedNotebookTitle = MutableStateFlow(initialNote?.notebookTitle ?: defaultNotebookTitle)
    val selectedNotebookTitle: StateFlow<String> = _selectedNotebookTitle.asStateFlow()

    private val _selectedFolder = MutableStateFlow(initialNote?.folderName ?: "Cours")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _isFavorite = MutableStateFlow(initialNote?.isFavorite ?: false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // Attached PDF metadata
    private val _attachedPdfId = MutableStateFlow(initialNote?.attachedPdfId)
    val attachedPdfId: StateFlow<Long?> = _attachedPdfId.asStateFlow()

    private val _attachedPdfTitle = MutableStateFlow(initialNote?.attachedPdfTitle ?: "")
    val attachedPdfTitle: StateFlow<String> = _attachedPdfTitle.asStateFlow()

    private val _attachedPdfPage = MutableStateFlow(initialNote?.attachedPdfPage)
    val attachedPdfPage: StateFlow<Int?> = _attachedPdfPage.asStateFlow()

    // Canvas Camera & Viewport
    private val parsedInitialData = CanvasData.fromJson(initialNote?.canvasDataJson ?: "")

    private val _panX = MutableStateFlow(parsedInitialData.panX)
    val panX: StateFlow<Float> = _panX.asStateFlow()

    private val _panY = MutableStateFlow(parsedInitialData.panY)
    val panY: StateFlow<Float> = _panY.asStateFlow()

    private val _zoomScale = MutableStateFlow(if (parsedInitialData.zoomScale in 0.2f..5.0f) parsedInitialData.zoomScale else 1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    // Canvas Board Objects
    private val _elements = MutableStateFlow<List<CanvasElement>>(parsedInitialData.elements)
    val elements: StateFlow<List<CanvasElement>> = _elements.asStateFlow()

    private val _strokes = MutableStateFlow<List<DrawingStroke>>(parsedInitialData.strokes)
    val strokes: StateFlow<List<DrawingStroke>> = _strokes.asStateFlow()

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    // Drawing Tools
    private val _activeTool = MutableStateFlow(CanvasTool.PAN_SELECT)
    val activeTool: StateFlow<CanvasTool> = _activeTool.asStateFlow()

    private val _penColorHex = MutableStateFlow("#1E293B")
    val penColorHex: StateFlow<String> = _penColorHex.asStateFlow()

    private val _penStrokeWidth = MutableStateFlow(4f)
    val penStrokeWidth: StateFlow<Float> = _penStrokeWidth.asStateFlow()

    private val _gridType = MutableStateFlow("DOTS") // DOTS, GRID, LINES, BLANK
    val gridType: StateFlow<String> = _gridType.asStateFlow()

    // Undo / Redo History
    private val undoStack = mutableListOf<CanvasSnapshot>()
    private val redoStack = mutableListOf<CanvasSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Auto-Save Engine
    private val _saveStatus = MutableStateFlow("Enregistré")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private var autoSaveJob: Job? = null
    private var isDirty = false

    init {
        recordUndoCheckpoint()
    }

    // =========================================================
    // CAMERA & VIEWPORT OPERATIONS
    // =========================================================
    fun setPan(x: Float, y: Float) {
        _panX.value = x
        _panY.value = y
        scheduleAutoSave()
    }

    fun updatePan(dx: Float, dy: Float) {
        _panX.value += dx
        _panY.value += dy
        scheduleAutoSave()
    }

    fun setZoom(zoom: Float) {
        _zoomScale.value = zoom.coerceIn(0.3f, 4.0f)
        scheduleAutoSave()
    }

    fun updateZoom(factor: Float, focalX: Float, focalY: Float) {
        val oldZoom = _zoomScale.value
        val newZoom = (oldZoom * factor).coerceIn(0.3f, 4.0f)
        if (newZoom != oldZoom) {
            val ratio = newZoom / oldZoom
            val newPanX = focalX - (focalX - _panX.value) * ratio
            val newPanY = focalY - (focalY - _panY.value) * ratio
            _zoomScale.value = newZoom
            _panX.value = newPanX
            _panY.value = newPanY
            scheduleAutoSave()
        }
    }

    fun resetCamera() {
        _panX.value = 0f
        _panY.value = 0f
        _zoomScale.value = 1f
        scheduleAutoSave()
    }

    // =========================================================
    // CANVAS ELEMENTS & STROKES
    // =========================================================
    fun recordUndoCheckpoint() {
        undoStack.add(
            CanvasSnapshot(
                elements = _elements.value.map { deepCopyElement(it) },
                strokes = _strokes.value.toList()
            )
        )
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _canUndo.value = undoStack.size > 1
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.size > 1) {
            val currentState = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(currentState)
            val previousState = undoStack.last()
            _elements.value = previousState.elements.map { deepCopyElement(it) }
            _strokes.value = previousState.strokes.toList()
            _canUndo.value = undoStack.size > 1
            _canRedo.value = true
            scheduleAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(nextState)
            _elements.value = nextState.elements.map { deepCopyElement(it) }
            _strokes.value = nextState.strokes.toList()
            _canUndo.value = undoStack.size > 1
            _canRedo.value = redoStack.isNotEmpty()
            scheduleAutoSave()
        }
    }

    fun addElement(element: CanvasElement) {
        recordUndoCheckpoint()
        _elements.value = _elements.value + element
        _selectedElementId.value = element.id
        scheduleAutoSave()
    }

    fun updateElement(updated: CanvasElement) {
        _elements.value = _elements.value.map { if (it.id == updated.id) updated else it }
        scheduleAutoSave()
    }

    fun removeElement(id: String) {
        recordUndoCheckpoint()
        _elements.value = _elements.value.filterNot { it.id == id }
        if (_selectedElementId.value == id) {
            _selectedElementId.value = null
        }
        scheduleAutoSave()
    }

    fun deleteElement(id: String) {
        removeElement(id)
    }

    fun duplicateElement(id: String) {
        val target = _elements.value.find { it.id == id } ?: return
        recordUndoCheckpoint()
        val newId = UUID.randomUUID().toString()
        val offset = 30f
        val duplicate = when (target) {
            is CanvasElement.Text -> target.copy(id = newId, x = target.x + offset, y = target.y + offset)
            is CanvasElement.Image -> target.copy(id = newId, x = target.x + offset, y = target.y + offset)
            is CanvasElement.Shape -> target.copy(id = newId, x = target.x + offset, y = target.y + offset)
            is CanvasElement.Table -> target.copy(
                id = newId,
                x = target.x + offset,
                y = target.y + offset,
                cells = target.cells.map { it.toMutableList() }.toMutableList()
            )
            is CanvasElement.Sticker -> target.copy(id = newId, x = target.x + offset, y = target.y + offset)
            is CanvasElement.PdfRef -> target.copy(id = newId, x = target.x + offset, y = target.y + offset)
        }
        _elements.value = _elements.value + duplicate
        _selectedElementId.value = newId
        scheduleAutoSave()
    }

    fun selectElement(id: String?) {
        _selectedElementId.value = id
    }

    fun addStroke(stroke: DrawingStroke) {
        recordUndoCheckpoint()
        _strokes.value = _strokes.value + stroke
        scheduleAutoSave()
    }

    fun eraseStrokesNear(worldX: Float, worldY: Float, threshold: Float = 28f) {
        val currentStrokes = _strokes.value
        val remaining = currentStrokes.filterNot { stroke ->
            stroke.points.any { p -> hypot(p.x - worldX, p.y - worldY) < threshold }
        }
        if (remaining.size != currentStrokes.size) {
            recordUndoCheckpoint()
            _strokes.value = remaining
            scheduleAutoSave()
        }
    }

    fun clearCanvas() {
        recordUndoCheckpoint()
        _elements.value = emptyList()
        _strokes.value = emptyList()
        _selectedElementId.value = null
        scheduleAutoSave()
    }

    // =========================================================
    // TOOLS & DRAWING SETTINGS
    // =========================================================
    fun setActiveTool(tool: CanvasTool) {
        _activeTool.value = tool
        if (tool != CanvasTool.PAN_SELECT) {
            _selectedElementId.value = null
        }
    }

    fun setPenColor(colorHex: String) {
        _penColorHex.value = colorHex
    }

    fun setPenStrokeWidth(width: Float) {
        _penStrokeWidth.value = width
    }

    fun setGridType(type: String) {
        _gridType.value = type
    }

    // =========================================================
    // NOTE METADATA OPERATIONS
    // =========================================================
    fun setTitle(newTitle: String) {
        _title.value = newTitle
        scheduleAutoSave()
    }

    fun setSubjectName(name: String) {
        _selectedSubjectName.value = name
        scheduleAutoSave()
    }

    fun setFolder(folder: String) {
        _selectedFolder.value = folder
        scheduleAutoSave()
    }

    fun setNotebook(id: Long, title: String) {
        _selectedNotebookId.value = id
        _selectedNotebookTitle.value = title
        scheduleAutoSave()
    }

    fun toggleFavorite() {
        _isFavorite.value = !_isFavorite.value
        scheduleAutoSave()
    }

    fun setFavorite(fav: Boolean) {
        _isFavorite.value = fav
        scheduleAutoSave()
    }

    fun attachPdf(pdfId: Long?, title: String, page: Int? = null) {
        _attachedPdfId.value = pdfId
        _attachedPdfTitle.value = title
        _attachedPdfPage.value = page
        scheduleAutoSave()
    }

    fun detachPdf() {
        _attachedPdfId.value = null
        _attachedPdfTitle.value = ""
        _attachedPdfPage.value = null
        scheduleAutoSave()
    }

    fun addPdfRefElement(pdfId: Long, pdfTitle: String, pageNumber: Int = 1, noteSnippet: String = "") {
        // Place in current center of viewport
        val centerX = (-_panX.value + 400f) / _zoomScale.value
        val centerY = (-_panY.value + 400f) / _zoomScale.value
        val pdfRef = CanvasElement.PdfRef(
            id = UUID.randomUUID().toString(),
            x = centerX,
            y = centerY,
            width = 280f,
            height = if (noteSnippet.isNotBlank()) 140f else 110f,
            pdfId = pdfId,
            pdfTitle = pdfTitle,
            pageNumber = pageNumber,
            noteSnippet = noteSnippet
        )
        // Also associate with note metadata if none set yet
        if (_attachedPdfId.value == null) {
            _attachedPdfId.value = pdfId
            _attachedPdfTitle.value = pdfTitle
            _attachedPdfPage.value = pageNumber
        }
        addElement(pdfRef)
    }

    // =========================================================
    // ROOM-BASED AUTO-SAVE SERVICE
    // =========================================================
    fun scheduleAutoSave(debounceMs: Long = 1500L) {
        isDirty = true
        _saveStatus.value = "Modifications non enregistrées..."
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(debounceMs)
            persistToRoom()
        }
    }

    suspend fun saveImmediately(): Boolean = withContext(Dispatchers.IO) {
        autoSaveJob?.cancel()
        persistToRoom()
    }

    private suspend fun persistToRoom(): Boolean {
        return try {
            _saveStatus.value = "Enregistrement en cours..."

            val canvasData = CanvasData(
                elements = _elements.value,
                strokes = _strokes.value,
                panX = _panX.value,
                panY = _panY.value,
                zoomScale = _zoomScale.value
            )
            val jsonStr = canvasData.toJson()
            val finalTitle = _title.value.ifBlank { "Super Note" }

            val currentId = _noteId.value
            if (currentId == null) {
                // First insert into Room
                val newId = repository.addNote(
                    notebookId = _selectedNotebookId.value,
                    notebookTitle = _selectedNotebookTitle.value,
                    title = finalTitle,
                    content = "Super Note : ${_elements.value.size} éléments",
                    subjectName = _selectedSubjectName.value,
                    category = _selectedFolder.value,
                    noteType = NoteTypes.CANVAS,
                    canvasDataJson = jsonStr,
                    folderName = _selectedFolder.value,
                    attachedPdfId = _attachedPdfId.value,
                    attachedPdfTitle = _attachedPdfTitle.value,
                    attachedPdfPage = _attachedPdfPage.value
                )
                _noteId.value = newId
                val noteIdToRecord = _noteId.value?.toString() ?: ""
                repository.recordHistory(
                    resourceType = HistoryTypes.SUPER_NOTE,
                    resourceId = noteIdToRecord,
                    title = finalTitle,
                    subtitle = "${_selectedNotebookTitle.value} • ${_selectedSubjectName.value}".trim().removePrefix("•").removeSuffix("•").trim(),
                    actionType = "EDITED"
                )
            } else {
                val updatedNote = NoteEntity(
                    id = currentId,
                    notebookId = _selectedNotebookId.value,
                    notebookTitle = _selectedNotebookTitle.value,
                    title = finalTitle,
                    content = "Super Note : ${_elements.value.size} éléments",
                    subjectName = _selectedSubjectName.value,
                    category = _selectedFolder.value,
                    isFavorite = _isFavorite.value,
                    noteType = NoteTypes.CANVAS,
                    canvasDataJson = jsonStr,
                    folderName = _selectedFolder.value,
                    attachedPdfId = _attachedPdfId.value,
                    attachedPdfTitle = _attachedPdfTitle.value,
                    attachedPdfPage = _attachedPdfPage.value,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(updatedNote)
                repository.recordHistory(
                    resourceType = HistoryTypes.SUPER_NOTE,
                    resourceId = currentId.toString(),
                    title = finalTitle,
                    subtitle = "${_selectedNotebookTitle.value} • ${_selectedSubjectName.value}".trim().removePrefix("•").removeSuffix("•").trim(),
                    actionType = "EDITED"
                )
            }
            isDirty = false
            _saveStatus.value = "Enregistré"
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _saveStatus.value = "Erreur de sauvegarde"
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isDirty) {
            // Auto flush on lifecycle clear
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                persistToRoom()
            }
        }
    }

    private fun deepCopyElement(elem: CanvasElement): CanvasElement {
        return when (elem) {
            is CanvasElement.Text -> elem.copy()
            is CanvasElement.Image -> elem.copy()
            is CanvasElement.Shape -> elem.copy()
            is CanvasElement.Table -> elem.copy(cells = elem.cells.map { it.toMutableList() }.toMutableList())
            is CanvasElement.Sticker -> elem.copy()
            is CanvasElement.PdfRef -> elem.copy()
        }
    }
}

class SuperNoteViewModelFactory(
    private val repository: StudyRepository,
    private val initialNote: NoteEntity?,
    private val defaultNotebookId: Long,
    private val defaultNotebookTitle: String,
    private val defaultSubjectName: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuperNoteViewModel::class.java)) {
            return SuperNoteViewModel(
                repository = repository,
                initialNote = initialNote,
                defaultNotebookId = defaultNotebookId,
                defaultNotebookTitle = defaultNotebookTitle,
                defaultSubjectName = defaultSubjectName
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

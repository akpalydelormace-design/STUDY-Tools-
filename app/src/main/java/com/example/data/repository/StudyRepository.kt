package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.backup.BackupManager
import com.example.data.local.StudyDatabase
import com.example.data.model.AgendaEventEntity
import com.example.data.model.GradeEntity
import com.example.data.model.HistoryEntity
import com.example.data.model.HistoryTypes
import com.example.data.model.NoteEntity
import com.example.data.model.NotebookEntity
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.SubjectEntity
import com.example.domain.GradeCalculator
import com.example.data.pdf.PdfHelper
import com.example.data.receiver.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.io.File

class StudyRepository(private val context: Context, private val db: StudyDatabase) {

    val allSubjects: Flow<List<SubjectEntity>> = db.subjectDao().getAllSubjects()
    val allGrades: Flow<List<GradeEntity>> = db.gradeDao().getAllGrades()
    val allAgendaEvents: Flow<List<AgendaEventEntity>> = db.agendaDao().getAllEvents()
    val allNotebooks: Flow<List<NotebookEntity>> = db.notebookDao().getAllNotebooks()
    val allNotes: Flow<List<NoteEntity>> = db.noteDao().getAllNotes()
    val allPdfs: Flow<List<PdfDocumentEntity>> = db.pdfDao().getAllPdfs()
    val recentPdfs: Flow<List<PdfDocumentEntity>> = db.pdfDao().getRecentPdfs(5)
    val recentHistory: Flow<List<HistoryEntity>> = db.historyDao().getRecentHistory()
    fun getRecentHistoryLimited(limit: Int): Flow<List<HistoryEntity>> = db.historyDao().getRecentHistoryLimited(limit)

    // History operations
    suspend fun recordHistory(
        resourceType: String,
        resourceId: String,
        title: String,
        subtitle: String = "",
        actionType: String = "",
        extraData: String = ""
    ) {
        val existing = db.historyDao().findEntry(resourceType, resourceId)
        val entry = if (existing != null) {
            existing.copy(
                title = title.ifBlank { existing.title },
                subtitle = subtitle.ifBlank { existing.subtitle },
                actionType = actionType.ifBlank { existing.actionType },
                extraData = if (extraData.isNotBlank()) extraData else existing.extraData,
                timestamp = System.currentTimeMillis()
            )
        } else {
            HistoryEntity(
                resourceType = resourceType,
                resourceId = resourceId,
                title = title,
                subtitle = subtitle,
                actionType = actionType,
                extraData = extraData,
                timestamp = System.currentTimeMillis()
            )
        }
        db.historyDao().insertOrUpdate(entry)
        db.historyDao().trimOldEntries()
    }

    suspend fun deleteHistoryEntry(entry: HistoryEntity) {
        db.historyDao().deleteEntry(entry)
    }

    suspend fun deleteHistoryEntryById(id: Long) {
        db.historyDao().deleteEntryById(id)
    }

    suspend fun clearHistory() {
        db.historyDao().clearAllHistory()
    }

    suspend fun getPdfById(id: Long): PdfDocumentEntity? {
        return db.pdfDao().getPdfById(id)
    }

    suspend fun getAgendaEventById(id: Long): AgendaEventEntity? {
        return db.agendaDao().getEventById(id)
    }

    // Subjects
    suspend fun addSubject(name: String, coefficient: Float, colorHex: String, iconName: String): Long {
        return db.subjectDao().insertSubject(
            SubjectEntity(
                name = name.trim(),
                coefficient = coefficient,
                colorHex = colorHex,
                iconName = iconName
            )
        )
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        db.subjectDao().updateSubject(subject)
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        db.gradeDao().deleteGradesBySubject(subject.id)
        db.subjectDao().deleteSubject(subject)
    }

    // Grades
    suspend fun addGrade(
        subjectId: Long,
        subjectName: String,
        trimestre: Int,
        score: Float,
        outOf: Float,
        coefficient: Float,
        evaluationType: String,
        date: Long,
        comment: String,
        schoolYear: String = GradeEntity.UNSPECIFIED_SCHOOL_YEAR
    ): Long {
        require(GradeCalculator.validateGrade(score, outOf, coefficient, trimestre, schoolYear).isValid) {
            GradeCalculator.validateGrade(score, outOf, coefficient, trimestre, schoolYear).message ?: "Note invalide"
        }
        return db.gradeDao().insertGrade(
            GradeEntity(
                subjectId = subjectId,
                subjectName = subjectName,
                trimestre = trimestre,
                score = score,
                outOf = outOf,
                coefficient = coefficient,
                evaluationType = evaluationType,
                date = date,
                comment = comment.trim(),
                schoolYear = schoolYear.trim()
            )
        )
    }

    suspend fun updateGrade(grade: GradeEntity) {
        require(GradeCalculator.validateGrade(grade.score, grade.outOf, grade.coefficient, grade.trimestre, grade.schoolYear).isValid) {
            GradeCalculator.validateGrade(grade.score, grade.outOf, grade.coefficient, grade.trimestre, grade.schoolYear).message ?: "Note invalide"
        }
        db.gradeDao().updateGrade(grade)
    }

    suspend fun deleteGrade(grade: GradeEntity) {
        db.gradeDao().deleteGrade(grade)
    }

    // Agenda
    suspend fun addAgendaEvent(
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
    ): Long {
        val event = AgendaEventEntity(
            subjectId = subjectId,
            subjectName = subjectName,
            title = title.trim(),
            evaluationType = evaluationType,
            dateTime = dateTime,
            room = room.trim(),
            description = description.trim(),
            priority = priority,
            reminderOption = reminderOption,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            isCompleted = false
        )
        val id = db.agendaDao().insertEvent(event)
        NotificationHelper.scheduleReminder(
            context = context,
            eventId = id,
            subjectName = subjectName,
            title = title,
            eventDateTime = dateTime,
            reminderOption = reminderOption,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute
        )
        return id
    }

    suspend fun updateAgendaEvent(event: AgendaEventEntity) {
        db.agendaDao().updateEvent(event)
        NotificationHelper.cancelReminder(context, event.id)
        if (!event.isCompleted) {
            NotificationHelper.scheduleReminder(
                context = context,
                eventId = event.id,
                subjectName = event.subjectName,
                title = event.title,
                eventDateTime = event.dateTime,
                reminderOption = event.reminderOption,
                reminderHour = event.reminderHour,
                reminderMinute = event.reminderMinute
            )
        }
    }

    suspend fun toggleEventCompleted(id: Long, completed: Boolean) {
        db.agendaDao().setCompleted(id, completed)
        if (completed) {
            NotificationHelper.cancelReminder(context, id)
        }
    }

    suspend fun deleteAgendaEvent(event: AgendaEventEntity) {
        NotificationHelper.cancelReminder(context, event.id)
        db.agendaDao().deleteEvent(event)
    }

    // Notebooks & Notes
    suspend fun addNotebook(title: String, subjectName: String, colorHex: String, iconEmoji: String): Long {
        return db.notebookDao().insertNotebook(
            NotebookEntity(
                title = title.trim(),
                subjectName = subjectName,
                colorHex = colorHex,
                iconEmoji = iconEmoji
            )
        )
    }

    suspend fun deleteNotebook(notebook: NotebookEntity) {
        db.notebookDao().deleteNotebook(notebook)
    }

    suspend fun addNote(
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
        attachedPdfPage: Int? = null
    ): Long {
        val now = System.currentTimeMillis()
        return db.noteDao().insertNote(
            NoteEntity(
                notebookId = notebookId,
                notebookTitle = notebookTitle,
                title = title.trim(),
                content = content,
                subjectName = subjectName,
                category = category,
                isFavorite = false,
                isImportant = false,
                attachmentsJson = attachmentsJson,
                createdAt = now,
                updatedAt = now,
                noteType = noteType,
                canvasDataJson = canvasDataJson,
                mindMapDataJson = mindMapDataJson,
                folderName = folderName,
                attachedPdfId = attachedPdfId,
                attachedPdfTitle = attachedPdfTitle,
                attachedPdfPage = attachedPdfPage,
                timestamp = now
            )
        )
    }

    suspend fun updateNote(note: NoteEntity) {
        db.noteDao().updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: NoteEntity) {
        db.noteDao().deleteNote(note)
    }

    suspend fun toggleNoteFavorite(id: Long, isFavorite: Boolean) {
        db.noteDao().toggleFavorite(id, isFavorite)
    }

    suspend fun getNoteById(id: Long): NoteEntity? {
        return db.noteDao().getNoteById(id)
    }

    // PDF Documents
    suspend fun importPdf(uri: Uri): Long {
        val (destFile, originalName) = PdfHelper.importPdfToInternalStorage(context, uri)
        val pageCount = PdfHelper.getPageCount(destFile)
        val pagesText = PdfHelper.extractTextByPages(destFile)
        val snippet = pagesText.values.take(3).joinToString(" ").take(500)

        val entity = PdfDocumentEntity(
            title = originalName,
            uriString = uri.toString(),
            localFilePath = destFile.absolutePath,
            pageCount = pageCount,
            lastPageRead = 1,
            fileSizeBytes = destFile.length(),
            lastOpenedAt = System.currentTimeMillis(),
            addedAt = System.currentTimeMillis(),
            extractedText = snippet
        )
        return db.pdfDao().insertPdf(entity)
    }

    suspend fun updatePdfLastPage(id: Long, page: Int) {
        db.pdfDao().updateLastPage(id, page)
    }

    suspend fun deletePdfFromLibrary(pdf: PdfDocumentEntity) {
        // Delete only from library / app cache if desired, without affecting user original storage
        try {
            val file = File(pdf.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        db.pdfDao().deletePdf(pdf)
    }

    // Export / Import
    suspend fun exportDataJson(): String {
        return BackupManager.exportDataToJson(db)
    }

    suspend fun importDataJson(jsonString: String): Boolean {
        return BackupManager.importDataFromJson(db, jsonString)
    }

    suspend fun clearAllData() {
        db.clearAllTables()
    }
}

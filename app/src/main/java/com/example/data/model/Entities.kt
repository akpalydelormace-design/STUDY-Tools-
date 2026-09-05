package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coefficient: Float = 1.0f,
    val colorHex: String = "#4F46E5",
    val iconName: String = "School"
)

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val trimestre: Int = 1, // 1, 2, 3
    val score: Float,       // Note sur 20
    val outOf: Float = 20.0f,
    val coefficient: Float = 1.0f,
    val evaluationType: String = "Devoir",
    val date: Long = System.currentTimeMillis(),
    val comment: String = "",
    /** School year selected by the learner (for example, 2025-2026). */
    val schoolYear: String = "Non renseignée"
) {
    companion object {
        const val UNSPECIFIED_SCHOOL_YEAR = "Non renseignée"
    }
}

@Entity(tableName = "agenda_events")
data class AgendaEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val subjectName: String,
    val title: String,
    val evaluationType: String = "Devoir", // Devoir, Interrogation, Examen, Exposé, Contrôle, Concours, Autre
    val dateTime: Long,                   // Epoch timestamp
    val room: String = "",                // Salle de classe
    val description: String = "",
    val priority: String = "Moyenne",     // Basse, Moyenne, Haute, Urgente
    val reminderOption: String = "SAME_DAY", // NONE, SAME_DAY, 1_DAY_BEFORE, 2_DAYS_BEFORE, CUSTOM
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectName: String = "Général",
    val colorHex: String = "#4F46E5",
    val iconEmoji: String = "📘",
    val createdAt: Long = System.currentTimeMillis()
)

object NoteTypes {
    const val TEXT = "TEXT"
    const val CANVAS = "CANVAS"
    const val MINDMAP = "MINDMAP"
}

@Entity(tableName = "structured_notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Général"
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notebookId: Long,
    val notebookTitle: String,
    val title: String,
    val content: String,
    val subjectName: String = "",
    val category: String = "Cours",
    val isFavorite: Boolean = false,
    val isImportant: Boolean = false,
    val attachmentsJson: String = "[]", // JSON serialized list of file paths/uris
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val noteType: String = NoteTypes.TEXT,
    val canvasDataJson: String = "",
    val mindMapDataJson: String = "",
    val folderName: String = "Cours",
    val attachedPdfId: Long? = null,
    val attachedPdfTitle: String = "",
    val attachedPdfPage: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uriString: String,
    val localFilePath: String,
    val pageCount: Int = 1,
    val lastPageRead: Int = 1,
    val fileSizeBytes: Long = 0L,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val extractedText: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryTypes {
    const val PDF = "PDF"
    const val NOTE = "NOTE"
    const val SUPER_NOTE = "SUPER_NOTE"
    const val MIND_MAP = "MIND_MAP"
    const val AGENDA = "AGENDA"
    const val BULLETIN = "BULLETIN"
    const val PODCAST = "PODCAST"
}

@Entity(
    tableName = "podcasts",
    indices = [
        androidx.room.Index(value = ["pdfId"], unique = true)
    ]
)
data class PodcastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfId: Long,
    val title: String,
    val durationMs: Long = 0L,
    val localAudioPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED", // GENERATING, COMPLETED, FAILED
    val script: String = "",
    val modelUsed: String = "gemini-2.5-flash-preview-tts",
    val segmentCount: Int = 1,
    val playbackPositionMs: Long = 0L,
    val lastListenedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "history_entries",
    indices = [
        androidx.room.Index(value = ["resourceType", "resourceId"], unique = true)
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resourceType: String,            // PDF, NOTE, AGENDA, BULLETIN
    val resourceId: String,              // ID or key of resource
    val title: String,                   // Display title (e.g., "Philosophie.pdf", "Dissertation")
    val subtitle: String = "",           // Context (e.g. "Page 18", "Modifiée", "Devoir de français")
    val actionType: String = "",         // OPENED, EDITED, SEARCHED, CREATED
    val extraData: String = "",          // Additional metadata like page number, note type, search query
    val timestamp: Long = System.currentTimeMillis()
)

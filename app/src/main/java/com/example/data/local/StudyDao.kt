package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AgendaEventEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.GradeEntity
import com.example.data.model.HistoryEntity
import com.example.data.model.Item
import com.example.data.model.Note
import com.example.data.model.NoteEntity
import com.example.data.model.NotebookEntity
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    suspend fun getAllSubjectsList(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?
}

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY date DESC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades ORDER BY date DESC")
    suspend fun getAllGradesList(): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE trimestre = :trimestre ORDER BY date DESC")
    fun getGradesByTrimestre(trimestre: Int): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getGradesForSubject(subjectId: Long): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Update
    suspend fun updateGrade(grade: GradeEntity)

    @Delete
    suspend fun deleteGrade(grade: GradeEntity)

    @Query("DELETE FROM grades WHERE subjectId = :subjectId")
    suspend fun deleteGradesBySubject(subjectId: Long)
}

@Dao
interface AgendaDao {
    @Query("SELECT * FROM agenda_events ORDER BY dateTime ASC")
    fun getAllEvents(): Flow<List<AgendaEventEntity>>

    @Query("SELECT * FROM agenda_events ORDER BY dateTime ASC")
    suspend fun getAllEventsList(): List<AgendaEventEntity>

    @Query("SELECT * FROM agenda_events WHERE dateTime >= :currentTime ORDER BY dateTime ASC")
    fun getUpcomingEvents(currentTime: Long): Flow<List<AgendaEventEntity>>

    @Query("SELECT * FROM agenda_events WHERE dateTime < :currentTime ORDER BY dateTime DESC")
    fun getPastEvents(currentTime: Long): Flow<List<AgendaEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AgendaEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<AgendaEventEntity>)

    @Update
    suspend fun updateEvent(event: AgendaEventEntity)

    @Delete
    suspend fun deleteEvent(event: AgendaEventEntity)

    @Query("UPDATE agenda_events SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY title ASC")
    fun getAllNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks ORDER BY title ASC")
    suspend fun getAllNotebooksList(): List<NotebookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: NotebookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebooks(notebooks: List<NotebookEntity>)

    @Update
    suspend fun updateNotebook(notebook: NotebookEntity)

    @Delete
    suspend fun deleteNotebook(notebook: NotebookEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE notebookId = :notebookId ORDER BY updatedAt DESC")
    fun getNotesByNotebook(notebookId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR subjectName LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?
}

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_documents ORDER BY lastOpenedAt DESC")
    fun getAllPdfs(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY lastOpenedAt DESC")
    suspend fun getAllPdfsList(): List<PdfDocumentEntity>

    @Query("SELECT * FROM pdf_documents ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun getRecentPdfs(limit: Int = 5): Flow<List<PdfDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdfs(pdfs: List<PdfDocumentEntity>)

    @Update
    suspend fun updatePdf(pdf: PdfDocumentEntity)

    @Delete
    suspend fun deletePdf(pdf: PdfDocumentEntity)

    @Query("UPDATE pdf_documents SET lastPageRead = :page, lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun updateLastPage(id: Long, page: Int, openedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getPdfById(id: Long): PdfDocumentEntity?
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingsEntity>>

    @Query("SELECT value FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearAllSettings()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}

@Dao
interface NoteCrudDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Query("SELECT * FROM structured_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM structured_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC LIMIT 60")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistoryLimited(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    suspend fun getAllHistoryList(): List<HistoryEntity>

    @Query("SELECT * FROM history_entries WHERE resourceType = :type AND resourceId = :resId LIMIT 1")
    suspend fun findEntry(type: String, resId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<HistoryEntity>)

    @Delete
    suspend fun deleteEntry(entry: HistoryEntity)

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM history_entries WHERE resourceType = :type AND resourceId = :resId")
    suspend fun deleteByResource(type: String, resId: String)

    @Query("DELETE FROM history_entries")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM history_entries")
    suspend fun getCount(): Int

    // Clean up oldest items if exceeding limit (e.g. 60)
    @Query("DELETE FROM history_entries WHERE id NOT IN (SELECT id FROM history_entries ORDER BY timestamp DESC LIMIT 60)")
    suspend fun trimOldEntries()
}




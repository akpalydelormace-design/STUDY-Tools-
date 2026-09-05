package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Item::class,
        Note::class,
        SubjectEntity::class,
        GradeEntity::class,
        AgendaEventEntity::class,
        NotebookEntity::class,
        NoteEntity::class,
        PdfDocumentEntity::class,
        AppSettingsEntity::class,
        HistoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun noteCrudDao(): NoteCrudDao
    abstract fun subjectDao(): SubjectDao
    abstract fun gradeDao(): GradeDao
    abstract fun agendaDao(): AgendaDao
    abstract fun notebookDao(): NotebookDao
    abstract fun noteDao(): NoteDao
    abstract fun pdfDao(): PdfDao
    abstract fun settingsDao(): SettingsDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN attachedPdfPage INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS history_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        resourceType TEXT NOT NULL,
                        resourceId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL DEFAULT '',
                        actionType TEXT NOT NULL DEFAULT '',
                        extraData TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_history_entries_resourceType_resourceId ON history_entries(resourceType, resourceId)")
            }
        }

        /**
         * Existing grades are preserved. Their school year is explicitly marked as
         * unknown rather than guessed from an arbitrary official configuration.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE grades ADD COLUMN schoolYear TEXT NOT NULL DEFAULT 'Non renseignée'")
            }
        }

        fun getInstance(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_tools.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default subjects & notebooks in background
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            populateInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: StudyDatabase) {
            val defaultSubjects = listOf(
                SubjectEntity(name = "Mathématiques", coefficient = 4.0f, colorHex = "#3B82F6", iconName = "Calculate"),
                SubjectEntity(name = "Français", coefficient = 4.0f, colorHex = "#10B981", iconName = "MenuBook"),
                SubjectEntity(name = "Philosophie", coefficient = 3.0f, colorHex = "#8B5CF6", iconName = "Psychology"),
                SubjectEntity(name = "Histoire-Géographie", coefficient = 3.0f, colorHex = "#F59E0B", iconName = "Public"),
                SubjectEntity(name = "Anglais", coefficient = 3.0f, colorHex = "#06B6D4", iconName = "Language"),
                SubjectEntity(name = "Physique-Chimie", coefficient = 3.0f, colorHex = "#EF4444", iconName = "Science"),
                SubjectEntity(name = "SVT", coefficient = 2.0f, colorHex = "#14B8A6", iconName = "Biotech")
            )
            db.subjectDao().insertSubjects(defaultSubjects)

            val defaultNotebooks = listOf(
                NotebookEntity(title = "📘 Philosophie", subjectName = "Philosophie", colorHex = "#8B5CF6", iconEmoji = "📘"),
                NotebookEntity(title = "📗 Français", subjectName = "Français", colorHex = "#10B981", iconEmoji = "📗"),
                NotebookEntity(title = "📙 Anglais", subjectName = "Anglais", colorHex = "#06B6D4", iconEmoji = "📙"),
                NotebookEntity(title = "📕 Histoire-Géo", subjectName = "Histoire-Géographie", colorHex = "#F59E0B", iconEmoji = "📕"),
                NotebookEntity(title = "📓 Personnel", subjectName = "Général", colorHex = "#4F46E5", iconEmoji = "📓")
            )
            db.notebookDao().insertNotebooks(defaultNotebooks)
        }
    }
}

typealias AppDatabase = StudyDatabase

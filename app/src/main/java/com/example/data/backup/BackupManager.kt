package com.example.data.backup

import com.example.data.local.StudyDatabase
import com.example.data.model.AgendaEventEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.GradeEntity
import com.example.data.model.HistoryEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NotebookEntity
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    suspend fun exportDataToJson(db: StudyDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Study Tools")
        root.put("version", "1.0.0")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Subjects
        val subjects = db.subjectDao().getAllSubjectsList()
        val subjectsArray = JSONArray()
        for (s in subjects) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("coefficient", s.coefficient.toDouble())
                put("colorHex", s.colorHex)
                put("iconName", s.iconName)
            }
            subjectsArray.put(obj)
        }
        root.put("subjects", subjectsArray)

        // 2. Grades
        val grades = db.gradeDao().getAllGradesList()
        val gradesArray = JSONArray()
        for (g in grades) {
            val obj = JSONObject().apply {
                put("id", g.id)
                put("subjectId", g.subjectId)
                put("subjectName", g.subjectName)
                put("trimestre", g.trimestre)
                put("score", g.score.toDouble())
                put("outOf", g.outOf.toDouble())
                put("coefficient", g.coefficient.toDouble())
                put("evaluationType", g.evaluationType)
                put("date", g.date)
                put("comment", g.comment)
                put("schoolYear", g.schoolYear)
            }
            gradesArray.put(obj)
        }
        root.put("grades", gradesArray)

        // 3. Agenda
        val events = db.agendaDao().getAllEventsList()
        val eventsArray = JSONArray()
        for (e in events) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("subjectId", e.subjectId ?: -1L)
                put("subjectName", e.subjectName)
                put("title", e.title)
                put("evaluationType", e.evaluationType)
                put("dateTime", e.dateTime)
                put("room", e.room)
                put("description", e.description)
                put("priority", e.priority)
                put("reminderOption", e.reminderOption)
                put("reminderHour", e.reminderHour)
                put("reminderMinute", e.reminderMinute)
                put("isCompleted", e.isCompleted)
            }
            eventsArray.put(obj)
        }
        root.put("agendaEvents", eventsArray)

        // 4. Notebooks
        val notebooks = db.notebookDao().getAllNotebooksList()
        val notebooksArray = JSONArray()
        for (nb in notebooks) {
            val obj = JSONObject().apply {
                put("id", nb.id)
                put("title", nb.title)
                put("subjectName", nb.subjectName)
                put("colorHex", nb.colorHex)
                put("iconEmoji", nb.iconEmoji)
                put("createdAt", nb.createdAt)
            }
            notebooksArray.put(obj)
        }
        root.put("notebooks", notebooksArray)

        // 5. Notes
        val notes = db.noteDao().getAllNotesList()
        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("notebookId", n.notebookId)
                put("notebookTitle", n.notebookTitle)
                put("title", n.title)
                put("content", n.content)
                put("subjectName", n.subjectName)
                put("category", n.category)
                put("isFavorite", n.isFavorite)
                put("isImportant", n.isImportant)
                put("attachmentsJson", n.attachmentsJson)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
                put("noteType", n.noteType)
                put("canvasDataJson", n.canvasDataJson)
                put("mindMapDataJson", n.mindMapDataJson)
                put("folderName", n.folderName)
                if (n.attachedPdfId != null) put("attachedPdfId", n.attachedPdfId)
                put("attachedPdfTitle", n.attachedPdfTitle)
                if (n.attachedPdfPage != null) put("attachedPdfPage", n.attachedPdfPage)
                put("timestamp", n.timestamp)
            }
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // 6. PDF history
        val pdfs = db.pdfDao().getAllPdfsList()
        val pdfsArray = JSONArray()
        for (p in pdfs) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("uriString", p.uriString)
                put("localFilePath", p.localFilePath)
                put("pageCount", p.pageCount)
                put("lastPageRead", p.lastPageRead)
                put("fileSizeBytes", p.fileSizeBytes)
                put("lastOpenedAt", p.lastOpenedAt)
                put("addedAt", p.addedAt)
            }
            pdfsArray.put(obj)
        }
        root.put("pdfDocuments", pdfsArray)

        // 7. History
        val history = db.historyDao().getAllHistoryList()
        val historyArray = JSONArray()
        for (h in history) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("resourceType", h.resourceType)
                put("resourceId", h.resourceId)
                put("title", h.title)
                put("subtitle", h.subtitle)
                put("actionType", h.actionType)
                put("extraData", h.extraData)
                put("timestamp", h.timestamp)
            }
            historyArray.put(obj)
        }
        root.put("historyEntries", historyArray)

        // 8. Settings / Coefficients
        val settings = db.settingsDao().getAllSettingsList()
        val settingsArray = JSONArray()
        for (s in settings) {
            val obj = JSONObject().apply {
                put("key", s.key)
                put("value", s.value)
            }
            settingsArray.put(obj)
        }
        root.put("appSettings", settingsArray)

        root.toString(2)
    }

    suspend fun importDataFromJson(db: StudyDatabase, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Subjects
            if (root.has("subjects")) {
                val array = root.getJSONArray("subjects")
                val subjectsList = mutableListOf<SubjectEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    subjectsList.add(
                        SubjectEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            coefficient = obj.optDouble("coefficient", 1.0).toFloat(),
                            colorHex = obj.optString("colorHex", "#4F46E5"),
                            iconName = obj.optString("iconName", "School")
                        )
                    )
                }
                if (subjectsList.isNotEmpty()) {
                    db.subjectDao().insertSubjects(subjectsList)
                }
            }

            // Grades
            if (root.has("grades")) {
                val array = root.getJSONArray("grades")
                val gradesList = mutableListOf<GradeEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    gradesList.add(
                        GradeEntity(
                            id = obj.optLong("id", 0L),
                            subjectId = obj.getLong("subjectId"),
                            subjectName = obj.getString("subjectName"),
                            trimestre = obj.optInt("trimestre", 1),
                            score = obj.getDouble("score").toFloat(),
                            outOf = obj.optDouble("outOf", 20.0).toFloat(),
                            coefficient = obj.optDouble("coefficient", 1.0).toFloat(),
                            evaluationType = obj.optString("evaluationType", "Devoir"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            comment = obj.optString("comment", ""),
                            schoolYear = obj.optString("schoolYear", GradeEntity.UNSPECIFIED_SCHOOL_YEAR)
                        )
                    )
                }
                if (gradesList.isNotEmpty()) {
                    db.gradeDao().insertGrades(gradesList)
                }
            }

            // Agenda
            if (root.has("agendaEvents")) {
                val array = root.getJSONArray("agendaEvents")
                val eventsList = mutableListOf<AgendaEventEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val rawSubId = obj.optLong("subjectId", -1L)
                    eventsList.add(
                        AgendaEventEntity(
                            id = obj.optLong("id", 0L),
                            subjectId = if (rawSubId > 0) rawSubId else null,
                            subjectName = obj.getString("subjectName"),
                            title = obj.getString("title"),
                            evaluationType = obj.optString("evaluationType", "Devoir"),
                            dateTime = obj.getLong("dateTime"),
                            room = obj.optString("room", ""),
                            description = obj.optString("description", ""),
                            priority = obj.optString("priority", "Moyenne"),
                            reminderOption = obj.optString("reminderOption", "SAME_DAY"),
                            reminderHour = obj.optInt("reminderHour", 8),
                            reminderMinute = obj.optInt("reminderMinute", 0),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
                if (eventsList.isNotEmpty()) {
                    db.agendaDao().insertEvents(eventsList)
                }
            }

            // Notebooks
            if (root.has("notebooks")) {
                val array = root.getJSONArray("notebooks")
                val notebooksList = mutableListOf<NotebookEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    notebooksList.add(
                        NotebookEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            subjectName = obj.optString("subjectName", "Général"),
                            colorHex = obj.optString("colorHex", "#4F46E5"),
                            iconEmoji = obj.optString("iconEmoji", "📘"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (notebooksList.isNotEmpty()) {
                    db.notebookDao().insertNotebooks(notebooksList)
                }
            }

            // Notes
            if (root.has("notes")) {
                val array = root.getJSONArray("notes")
                val notesList = mutableListOf<NoteEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    notesList.add(
                        NoteEntity(
                            id = obj.optLong("id", 0L),
                            notebookId = obj.getLong("notebookId"),
                            notebookTitle = obj.optString("notebookTitle", "Général"),
                            title = obj.getString("title"),
                            content = obj.optString("content", ""),
                            subjectName = obj.optString("subjectName", ""),
                            category = obj.optString("category", "Cours"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isImportant = obj.optBoolean("isImportant", false),
                            attachmentsJson = obj.optString("attachmentsJson", "[]"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            noteType = obj.optString("noteType", "TEXT"),
                            canvasDataJson = obj.optString("canvasDataJson", ""),
                            mindMapDataJson = obj.optString("mindMapDataJson", ""),
                            folderName = obj.optString("folderName", "Cours"),
                            attachedPdfId = if (obj.has("attachedPdfId") && !obj.isNull("attachedPdfId")) obj.optLong("attachedPdfId") else null,
                            attachedPdfTitle = obj.optString("attachedPdfTitle", ""),
                            attachedPdfPage = if (obj.has("attachedPdfPage") && !obj.isNull("attachedPdfPage")) obj.optInt("attachedPdfPage") else null,
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (notesList.isNotEmpty()) {
                    db.noteDao().insertNotes(notesList)
                }
            }

            // History (backward compatible: optional)
            if (root.has("historyEntries")) {
                val array = root.getJSONArray("historyEntries")
                val historyList = mutableListOf<HistoryEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    historyList.add(
                        HistoryEntity(
                            id = obj.optLong("id", 0L),
                            resourceType = obj.getString("resourceType"),
                            resourceId = obj.getString("resourceId"),
                            title = obj.getString("title"),
                            subtitle = obj.optString("subtitle", ""),
                            actionType = obj.optString("actionType", ""),
                            extraData = obj.optString("extraData", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (historyList.isNotEmpty()) {
                    db.historyDao().insertAll(historyList)
                }
            }

            // Settings / Coefficients (backward compatible)
            if (root.has("appSettings")) {
                val array = root.getJSONArray("appSettings")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val key = obj.getString("key")
                    val value = obj.getString("value")
                    db.settingsDao().setSetting(AppSettingsEntity(key, value))
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

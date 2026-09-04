package com.example.ui.screens.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTypes
import com.example.data.model.PdfDocumentEntity
import com.example.ui.StudyViewModel
import com.example.ui.components.parseHexColor
import com.example.ui.theme.PrimaryIndigo
import kotlinx.coroutines.delay
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTextNoteEditor(
    viewModel: StudyViewModel,
    initialNote: NoteEntity?,
    defaultNotebookId: Long = 1L,
    defaultNotebookTitle: String = "Général",
    onClose: () -> Unit,
    onDelete: ((NoteEntity) -> Unit)? = null
) {
    val subjects by viewModel.subjects.collectAsState()
    val notebooks by viewModel.notebooks.collectAsState()
    val allPdfs by viewModel.pdfs.collectAsState()

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var contentValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialNote?.content ?: "",
                selection = TextRange(initialNote?.content?.length ?: 0)
            )
        )
    }

    var selectedNotebookId by remember { mutableStateOf(initialNote?.notebookId ?: defaultNotebookId) }
    var selectedNotebookTitle by remember { mutableStateOf(initialNote?.notebookTitle ?: defaultNotebookTitle) }
    var selectedSubjectName by remember { mutableStateOf(initialNote?.subjectName ?: (subjects.firstOrNull()?.name ?: "")) }
    var selectedFolder by remember { mutableStateOf(initialNote?.folderName ?: "Cours") }
    var isFavorite by remember { mutableStateOf(initialNote?.isFavorite ?: false) }
    var isImportant by remember { mutableStateOf(initialNote?.isImportant ?: false) }

    var attachedPdfId by remember { mutableStateOf(initialNote?.attachedPdfId) }
    var attachedPdfTitle by remember { mutableStateOf(initialNote?.attachedPdfTitle ?: "") }

    val attachmentsList = remember {
        mutableStateListOf<String>().apply {
            if (initialNote != null && initialNote.attachmentsJson.isNotBlank()) {
                try {
                    val arr = JSONArray(initialNote.attachmentsJson)
                    for (i in 0 until arr.length()) {
                        add(arr.getString(i))
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    var showPdfPickerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var autoSaveStatus by remember { mutableStateOf("Enregistré") }

    // Dropdown states
    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showNotebookDropdown by remember { mutableStateOf(false) }
    var showFolderDropdown by remember { mutableStateOf(false) }

    val standardFolders = listOf("Cours", "Fiche de révision", "Dissertation", "Citations", "Exercices", "Méthodes")

    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { attachmentsList.add(it.toString()) }
    }

    // Auto-save logic
    fun saveCurrentState() {
        val finalTitle = title.ifBlank { "Note sans titre" }
        val finalJson = JSONArray(attachmentsList).toString()

        if (initialNote == null) {
            viewModel.addNote(
                notebookId = selectedNotebookId,
                notebookTitle = selectedNotebookTitle,
                title = finalTitle,
                content = contentValue.text,
                subjectName = selectedSubjectName,
                category = selectedFolder,
                attachmentsJson = finalJson,
                noteType = NoteTypes.TEXT,
                folderName = selectedFolder,
                attachedPdfId = attachedPdfId,
                attachedPdfTitle = attachedPdfTitle
            )
        } else {
            val updated = initialNote.copy(
                notebookId = selectedNotebookId,
                notebookTitle = selectedNotebookTitle,
                title = finalTitle,
                content = contentValue.text,
                subjectName = selectedSubjectName,
                category = selectedFolder,
                isFavorite = isFavorite,
                isImportant = isImportant,
                attachmentsJson = finalJson,
                folderName = selectedFolder,
                attachedPdfId = attachedPdfId,
                attachedPdfTitle = attachedPdfTitle,
                updatedAt = System.currentTimeMillis()
            )
            viewModel.updateNote(updated)
        }
        autoSaveStatus = "Enregistré"
    }

    // Auto-save debounce effect
    LaunchedEffect(title, contentValue.text, selectedSubjectName, selectedFolder, isFavorite, isImportant, attachmentsList.size, attachedPdfId) {
        autoSaveStatus = "Modification..."
        delay(1200)
        saveCurrentState()
    }

    // Helper formatting insertion
    fun insertFormatting(prefix: String, suffix: String = "") {
        val currentText = contentValue.text
        val selection = contentValue.selection
        val selectedText = currentText.substring(selection.start, selection.end)
        val newText = currentText.substring(0, selection.start) + prefix + selectedText + suffix + currentText.substring(selection.end)
        val newCursor = selection.start + prefix.length + selectedText.length + suffix.length
        contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
    }

    // Word and character count calculation
    val wordCount = remember(contentValue.text) {
        if (contentValue.text.isBlank()) 0
        else contentValue.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = contentValue.text.length

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (title.isBlank()) "Nouvelle note" else title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "$autoSaveStatus • $wordCount mots • $charCount car.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        saveCurrentState()
                        onClose()
                    },
                    modifier = Modifier.testTag("note_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favori",
                        tint = if (isFavorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (initialNote != null && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(
                    onClick = {
                        saveCurrentState()
                        onClose()
                    },
                    modifier = Modifier.testTag("note_save_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Enregistrer", tint = PrimaryIndigo)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Metadata Pills Strip (Subject, Notebook, Folder, PDF)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Subject Pill
            val currentSubjectObj = subjects.find { it.name == selectedSubjectName }
            val subColor = currentSubjectObj?.colorHex?.let { parseHexColor(it) } ?: PrimaryIndigo
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = subColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, subColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { showSubjectDropdown = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(subColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedSubjectName.ifBlank { "Matière" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subColor
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSubjectDropdown,
                    onDismissRequest = { showSubjectDropdown = false }
                ) {
                    subjects.forEach { subj ->
                        DropdownMenuItem(
                            text = { Text(subj.name) },
                            onClick = {
                                selectedSubjectName = subj.name
                                showSubjectDropdown = false
                            }
                        )
                    }
                }
            }

            // Notebook Pill
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { showNotebookDropdown = true }
                ) {
                    Text(
                        text = "📖 $selectedNotebookTitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                DropdownMenu(
                    expanded = showNotebookDropdown,
                    onDismissRequest = { showNotebookDropdown = false }
                ) {
                    notebooks.forEach { nb ->
                        DropdownMenuItem(
                            text = { Text("${nb.iconEmoji} ${nb.title}") },
                            onClick = {
                                selectedNotebookId = nb.id
                                selectedNotebookTitle = nb.title
                                showNotebookDropdown = false
                            }
                        )
                    }
                }
            }

            // Folder Pill
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { showFolderDropdown = true }
                ) {
                    Text(
                        text = "📁 $selectedFolder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                DropdownMenu(
                    expanded = showFolderDropdown,
                    onDismissRequest = { showFolderDropdown = false }
                ) {
                    standardFolders.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f) },
                            onClick = {
                                selectedFolder = f
                                showFolderDropdown = false
                            }
                        )
                    }
                }
            }

            // PDF Link Pill
            if (attachedPdfId != null && attachedPdfTitle.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                        val pdf = allPdfs.find { it.id == attachedPdfId }
                        if (pdf != null) {
                            saveCurrentState()
                            viewModel.openPdf(pdf)
                            viewModel.setSelectedTab(1)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = attachedPdfTitle.take(16),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Ouvrir",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                attachedPdfId = null
                                attachedPdfTitle = ""
                            },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Délier", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { showPdfPickerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Lier PDF", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Title Input Field
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Titre de la note...", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("note_title_input")
        )

        // Advanced Rich Text Formatting Toolbar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    TextToolbarButton(label = "H1", onClick = { insertFormatting("\n# ") })
                }
                item {
                    TextToolbarButton(label = "H2", onClick = { insertFormatting("\n## ") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.FormatBold, label = "Gras", onClick = { insertFormatting("**", "**") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.FormatItalic, label = "Italique", onClick = { insertFormatting("*", "*") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.FormatUnderlined, label = "Souligné", onClick = { insertFormatting("<u>", "</u>") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.Highlight, label = "Surligner", onClick = { insertFormatting("==", "==") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.FormatListBulleted, label = "Puces", onClick = { insertFormatting("\n- ") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.FormatListNumbered, label = "Numéroté", onClick = { insertFormatting("\n1. ") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.CheckBox, label = "Tâche", onClick = { insertFormatting("\n[ ] ") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.FormatQuote, label = "Citation", onClick = { insertFormatting("\n> ") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.HorizontalRule, label = "Séparateur", onClick = { insertFormatting("\n---\n") })
                }
                item {
                    TextToolbarIconButton(icon = Icons.Default.AttachFile, label = "Fichier", onClick = {
                        attachmentPickerLauncher.launch(arrayOf("*/*"))
                    })
                }
            }
        }

        // Attachments preview strip
        if (attachmentsList.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attachmentsList) { uriStr ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uriStr.substringAfterLast("/").take(18),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { attachmentsList.remove(uriStr) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Main Editor Text Area
        TextField(
            value = contentValue,
            onValueChange = { contentValue = it },
            placeholder = {
                Text(
                    "Commence à rédiger ton cours ou ta fiche de révision...\n\n" +
                            "• Rédige en toute liberté hors-ligne\n" +
                            "• Ajoute des titres H1/H2, gras, italique, citations, cases à cocher\n" +
                            "• Lier un PDF de ta bibliothèque pour l'ouvrir à tout moment !",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .testTag("note_content_input")
        )
    }

    // PDF Link Picker Dialog
    if (showPdfPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPdfPickerDialog = false },
            title = { Text("Lier un document PDF", fontWeight = FontWeight.Bold) },
            text = {
                if (allPdfs.isEmpty()) {
                    Text("Aucun document PDF dans la bibliothèque. Importe d'abord un PDF dans l'onglet PDF.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allPdfs.forEach { pdf ->
                            Card(
                                onClick = {
                                    attachedPdfId = pdf.id
                                    attachedPdfTitle = pdf.title
                                    showPdfPickerDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pdf.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                        Text("${pdf.pageCount} pages", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPdfPickerDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && initialNote != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer cette note ?") },
            text = { Text("Cette action est irréversible. La note sera définitivement supprimée.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(initialNote)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun TextToolbarIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TextToolbarButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTypes
import com.example.data.model.NotebookEntity
import com.example.ui.StudyViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatTimestampToDate
import com.example.ui.components.parseHexColor
import com.example.ui.screens.notes.AdvancedTextNoteEditor
import com.example.ui.screens.notes.MindMapEditor
import com.example.ui.screens.notes.SuperNoteCanvasEditor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.PrimaryIndigo
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val notebooks by viewModel.notebooks.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val allPdfs by viewModel.pdfs.collectAsState()
    val globalActiveNote by viewModel.activeNoteToEdit.collectAsState()

    // Local editor navigation state
    var localEditingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var creatingNewType by remember { mutableStateOf<String?>(null) } // TEXT, CANVAS, MINDMAP or null
    var showCreateTypeSheet by remember { mutableStateOf(false) }

    // Filters and Search
    var selectedNotebookId by remember { mutableStateOf<Long?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var selectedSubjectName by remember { mutableStateOf<String?>(null) }
    var selectedNoteTypeFilter by remember { mutableStateOf<String?>(null) } // null = All, or TEXT, CANVAS, MINDMAP
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var sortByAlphabetical by remember { mutableStateOf(false) }

    // Dialogs
    var showCreateNotebookDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val activeNote = globalActiveNote ?: localEditingNote

    // Handle Active Editor Screen
    if (activeNote != null || creatingNewType != null) {
        val targetType = activeNote?.noteType ?: creatingNewType ?: NoteTypes.TEXT
        val selectedNotebookObj = notebooks.find { it.id == selectedNotebookId } ?: notebooks.firstOrNull()
        val defaultNbId = selectedNotebookObj?.id ?: 1L
        val defaultNbTitle = selectedNotebookObj?.title ?: "Général"

        val onEditorClose: () -> Unit = {
            viewModel.closeNote()
            localEditingNote = null
            creatingNewType = null
        }

        val onEditorDelete: (NoteEntity) -> Unit = { noteToDelete ->
            viewModel.deleteNote(noteToDelete)
            onEditorClose()
        }

        when (targetType) {
            NoteTypes.CANVAS -> {
                SuperNoteCanvasEditor(
                    viewModel = viewModel,
                    initialNote = activeNote,
                    defaultNotebookId = defaultNbId,
                    defaultNotebookTitle = defaultNbTitle,
                    onClose = onEditorClose,
                    onDelete = onEditorDelete
                )
            }
            NoteTypes.MINDMAP -> {
                MindMapEditor(
                    viewModel = viewModel,
                    initialNote = activeNote,
                    defaultNotebookId = defaultNbId,
                    defaultNotebookTitle = defaultNbTitle,
                    onClose = onEditorClose,
                    onDelete = onEditorDelete
                )
            }
            else -> {
                AdvancedTextNoteEditor(
                    viewModel = viewModel,
                    initialNote = activeNote,
                    defaultNotebookId = defaultNbId,
                    defaultNotebookTitle = defaultNbTitle,
                    onClose = onEditorClose,
                    onDelete = onEditorDelete
                )
            }
        }
    } else {
        // Main Workspace Notes List
        val filteredNotes = notes.filter { n ->
            val matchesNotebook = selectedNotebookId == null || n.notebookId == selectedNotebookId
            val matchesFolder = selectedFolder == null || n.folderName.equals(selectedFolder, ignoreCase = true)
            val matchesSubject = selectedSubjectName == null || n.subjectName.equals(selectedSubjectName, ignoreCase = true)
            val matchesType = selectedNoteTypeFilter == null || n.noteType.equals(selectedNoteTypeFilter, ignoreCase = true)
            val matchesFav = !showOnlyFavorites || n.isFavorite
            val matchesSearch = searchQuery.isBlank() ||
                    n.title.contains(searchQuery, ignoreCase = true) ||
                    n.content.contains(searchQuery, ignoreCase = true) ||
                    n.subjectName.contains(searchQuery, ignoreCase = true) ||
                    n.notebookTitle.contains(searchQuery, ignoreCase = true) ||
                    n.folderName.contains(searchQuery, ignoreCase = true) ||
                    n.canvasDataJson.contains(searchQuery, ignoreCase = true) ||
                    n.mindMapDataJson.contains(searchQuery, ignoreCase = true)

            matchesNotebook && matchesFolder && matchesSubject && matchesType && matchesFav && matchesSearch
        }.let { list ->
            if (sortByAlphabetical) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.updatedAt }
        }

        val availableFolders = remember(notes, selectedNotebookId) {
            val baseFolders = listOf("Tous", "Cours", "Fiche de révision", "Dissertation", "Citations", "Exercices", "Méthodes")
            val dynamicFolders = notes.filter { selectedNotebookId == null || it.notebookId == selectedNotebookId }
                .map { it.folderName }
                .filter { it.isNotBlank() }
            (baseFolders + dynamicFolders).distinct()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("notes_screen")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Espace Notes",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${notes.size} note${if (notes.size > 1) "s" else ""} • ${notebooks.size} cahier${if (notebooks.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Add Notebook button
                    Surface(
                        onClick = { showCreateNotebookDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("add_notebook_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nouveau carnet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        }
                    }
                }

                // Search Box inside notes
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher dans les notes, cours, schémas...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("notes_search_input")
                )

                // 1. Notebooks Horizontal Selector
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedNotebookId == null,
                            onClick = { selectedNotebookId = null },
                            label = { Text("Tous les carnets") }
                        )
                    }
                    items(notebooks) { nb ->
                        FilterChip(
                            selected = selectedNotebookId == nb.id,
                            onClick = {
                                selectedNotebookId = if (selectedNotebookId == nb.id) null else nb.id
                            },
                            label = { Text("${nb.iconEmoji} ${nb.title}") }
                        )
                    }
                }

                // 2. Folders & Categories Strip
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableFolders) { folder ->
                        val isSelected = (selectedFolder == null && folder == "Tous") || (selectedFolder == folder)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFolder = if (folder == "Tous") null else folder
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text(folder, fontSize = 12.sp) }
                        )
                    }
                }

                // 3. Note Type Filters (Toutes, 📝 Textes, 🎨 Super Notes, 🧠 Cartes mentales)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedNoteTypeFilter == null,
                            onClick = { selectedNoteTypeFilter = null },
                            label = { Text("Toutes", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedNoteTypeFilter == NoteTypes.TEXT,
                            onClick = { selectedNoteTypeFilter = if (selectedNoteTypeFilter == NoteTypes.TEXT) null else NoteTypes.TEXT },
                            leadingIcon = { Text("📝", fontSize = 12.sp) },
                            label = { Text("Textes", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedNoteTypeFilter == NoteTypes.CANVAS,
                            onClick = { selectedNoteTypeFilter = if (selectedNoteTypeFilter == NoteTypes.CANVAS) null else NoteTypes.CANVAS },
                            leadingIcon = { Text("🎨", fontSize = 12.sp) },
                            label = { Text("Super Notes", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedNoteTypeFilter == NoteTypes.MINDMAP,
                            onClick = { selectedNoteTypeFilter = if (selectedNoteTypeFilter == NoteTypes.MINDMAP) null else NoteTypes.MINDMAP },
                            leadingIcon = { Text("🧠", fontSize = 12.sp) },
                            label = { Text("Cartes mentales", fontSize = 11.sp) }
                        )
                    }
                }

                // 4. Filter & Sort Row (Favorites, Alphabetical, Subject Filter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = showOnlyFavorites,
                            onClick = { showOnlyFavorites = !showOnlyFavorites },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showOnlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Favoris", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = sortByAlphabetical,
                            onClick = { sortByAlphabetical = !sortByAlphabetical },
                            leadingIcon = {
                                Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text("A-Z", fontSize = 11.sp) }
                        )
                    }

                    // Subject filter dropdown
                    var showSubjMenu by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { showSubjMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedSubjectName != null) PrimaryIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedSubjectName ?: "Matière",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedSubjectName != null) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DropdownMenu(expanded = showSubjMenu, onDismissRequest = { showSubjMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Toutes les matières") },
                                onClick = { selectedSubjectName = null; showSubjMenu = false }
                            )
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = { selectedSubjectName = s.name; showSubjMenu = false }
                                )
                            }
                        }
                    }
                }

                // Notes List
                if (filteredNotes.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.Description,
                        title = "Aucune note trouvée",
                        description = if (searchQuery.isNotBlank() || showOnlyFavorites)
                            "Aucun résultat pour cette recherche ou filtre."
                        else
                            "Appuie sur le bouton '+' pour créer une note texte, une Super Note ou une carte mentale.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            EnhancedNoteCardItem(
                                note = note,
                                onOpen = { localEditingNote = note },
                                onToggleFavorite = {
                                    viewModel.toggleNoteFavorite(note.id, !note.isFavorite)
                                },
                                onOpenAttachedPdf = { pdfId ->
                                    val pdf = allPdfs.find { it.id == pdfId }
                                    if (pdf != null) {
                                        viewModel.openPdf(pdf)
                                        viewModel.setSelectedTab(1)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Floating Action Button to Add a Note
            FloatingActionButton(
                onClick = { showCreateTypeSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 16.dp)
                    .testTag("add_note_fab"),
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle note")
            }
        }
    }

    // Modal Sheet: Choose Note Type
    if (showCreateTypeSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCreateTypeSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Créer dans l'espace de travail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 1. Text Note
                NoteTypeOptionCard(
                    icon = "📝",
                    title = "Note texte structurée",
                    description = "Cours, fiches de révision, dissertations. Enrichissement H1/H2, gras, listes, surlignage.",
                    badgeColor = PrimaryIndigo,
                    onClick = {
                        showCreateTypeSheet = false
                        creatingNewType = NoteTypes.TEXT
                    }
                )

                // 2. Super Note (Infinite Canvas)
                NoteTypeOptionCard(
                    icon = "🎨",
                    title = "Super Note (Toile infinie)",
                    description = "Zoom & pan infini. Blocs de texte déplaçables, schémas, dessins au stylet, tableaux, autocollants d'étude.",
                    badgeColor = Color(0xFF7C3AED),
                    onClick = {
                        showCreateTypeSheet = false
                        creatingNewType = NoteTypes.CANVAS
                    }
                )

                // 3. Mind Map
                NoteTypeOptionCard(
                    icon = "🧠",
                    title = "Carte mentale dynamique",
                    description = "Arborescence d'idées, nœuds et connexions fluides. Idéal pour structurer des concepts et réviser.",
                    badgeColor = Color(0xFF0D9488),
                    onClick = {
                        showCreateTypeSheet = false
                        creatingNewType = NoteTypes.MINDMAP
                    }
                )
            }
        }
    }

    // Create Notebook Dialog
    if (showCreateNotebookDialog) {
        var nbTitle by remember { mutableStateOf("") }
        var nbEmoji by remember { mutableStateOf("📘") }
        val emojis = listOf("📘", "📗", "📙", "📕", "📓", "📑", "💡", "🔬", "📐", "🌍")

        AlertDialog(
            onDismissRequest = { showCreateNotebookDialog = false },
            title = { Text("Créer un carnet") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nbTitle,
                        onValueChange = { nbTitle = it },
                        label = { Text("Nom du carnet *") },
                        placeholder = { Text("ex: Philosophie, SVT, Personnel...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_notebook_title_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Icône :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp)
                    ) {
                        emojis.forEach { emo ->
                            Surface(
                                onClick = { nbEmoji = emo },
                                shape = RoundedCornerShape(8.dp),
                                color = if (nbEmoji == emo) PrimaryIndigo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emo, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nbTitle.isNotBlank()) {
                            viewModel.addNotebook(
                                title = "$nbEmoji $nbTitle",
                                subjectName = nbTitle,
                                colorHex = "#4F46E5",
                                iconEmoji = nbEmoji
                            )
                            showCreateNotebookDialog = false
                        }
                    },
                    enabled = nbTitle.isNotBlank()
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNotebookDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun NoteTypeOptionCard(
    icon: String,
    title: String,
    description: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 22.sp)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun EnhancedNoteCardItem(
    note: NoteEntity,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAttachedPdf: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_item_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onOpen
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Type badge, Notebook & Subject, Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    val (typeIcon, typeLabel, typeColor) = when (note.noteType) {
                        NoteTypes.CANVAS -> Triple("🎨", "Super Note", Color(0xFF7C3AED))
                        NoteTypes.MINDMAP -> Triple("🧠", "Carte mentale", Color(0xFF0D9488))
                        else -> Triple("📝", "Note texte", PrimaryIndigo)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, typeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "$typeIcon $typeLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Notebook & Folder
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "${note.notebookTitle} • ${note.folderName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favori",
                        tint = if (note.isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Note Title
            Text(
                text = note.title.ifBlank { "Note sans titre" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Content snippet or visual descriptor
            val snippet = when (note.noteType) {
                NoteTypes.CANVAS -> {
                    if (note.content.isNotBlank()) note.content else "Toile infinie"
                }
                NoteTypes.MINDMAP -> {
                    if (note.content.isNotBlank()) note.content else "Arborescence d'idées"
                }
                else -> {
                    note.content.lines().firstOrNull { it.isNotBlank() } ?: "Note vide"
                }
            }

            Text(
                text = snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Attached PDF badge, Subject, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Attached PDF Chip
                    if (note.attachedPdfId != null && note.attachedPdfTitle.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onOpenAttachedPdf(note.attachedPdfId!!) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = note.attachedPdfTitle.take(14),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Ouvrir",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    // Subject Badge
                    if (note.subjectName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = note.subjectName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = formatTimestampToDate(note.updatedAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

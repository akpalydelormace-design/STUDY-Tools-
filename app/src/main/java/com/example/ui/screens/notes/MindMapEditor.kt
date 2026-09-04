package com.example.ui.screens.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MindMapData
import com.example.data.model.MindMapNode
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTypes
import com.example.ui.StudyViewModel
import com.example.ui.components.parseHexColor
import com.example.ui.theme.PrimaryIndigo
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapEditor(
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

    var title by remember { mutableStateOf(initialNote?.title ?: "Carte mentale") }
    var selectedSubjectName by remember { mutableStateOf(initialNote?.subjectName ?: (subjects.firstOrNull()?.name ?: "")) }
    var selectedNotebookId by remember { mutableStateOf(initialNote?.notebookId ?: defaultNotebookId) }
    var selectedNotebookTitle by remember { mutableStateOf(initialNote?.notebookTitle ?: defaultNotebookTitle) }
    var selectedFolder by remember { mutableStateOf(initialNote?.folderName ?: "Cours") }
    var isFavorite by remember { mutableStateOf(initialNote?.isFavorite ?: false) }

    var attachedPdfId by remember { mutableStateOf(initialNote?.attachedPdfId) }
    var attachedPdfTitle by remember { mutableStateOf(initialNote?.attachedPdfTitle ?: "") }

    // Canvas Pan & Zoom
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    // MindMap Data
    val initialData = remember { MindMapData.fromJson(initialNote?.mindMapDataJson ?: "") }
    val nodes = remember { mutableStateListOf<MindMapNode>().apply { addAll(initialData.nodes) } }
    var mapStyle by remember { mutableStateOf(initialData.style) } // MODERN_INDIGO, PASTEL_STUDY, DARK_NEON, MINIMALIST

    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val selectedNode = nodes.find { it.id == selectedNodeId }

    // Undo / Redo history
    val undoHistory = remember { mutableStateListOf<List<MindMapNode>>() }
    val redoHistory = remember { mutableStateListOf<List<MindMapNode>>() }

    fun recordUndoCheckpoint() {
        undoHistory.add(nodes.map { it.copy() })
        redoHistory.clear()
        if (undoHistory.size > 25) {
            undoHistory.removeAt(0)
        }
    }

    fun applyUndo() {
        if (undoHistory.isNotEmpty()) {
            val lastState = undoHistory.removeAt(undoHistory.size - 1)
            redoHistory.add(nodes.map { it.copy() })
            nodes.clear()
            nodes.addAll(lastState)
        }
    }

    fun applyRedo() {
        if (redoHistory.isNotEmpty()) {
            val nextState = redoHistory.removeAt(redoHistory.size - 1)
            undoHistory.add(nodes.map { it.copy() })
            nodes.clear()
            nodes.addAll(nextState)
        }
    }

    // Dialog states
    var editingNodeTarget by remember { mutableStateOf<MindMapNode?>(null) }
    var showPdfPickerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    // Auto-save logic
    var saveStatus by remember { mutableStateOf("Enregistré") }
    fun saveMindMap() {
        val data = MindMapData(nodes = nodes.toList(), style = mapStyle)
        val jsonStr = data.toJson()
        val finalTitle = title.ifBlank { nodes.find { it.parentId == null }?.text ?: "Carte mentale" }

        if (initialNote == null) {
            viewModel.addNote(
                notebookId = selectedNotebookId,
                notebookTitle = selectedNotebookTitle,
                title = finalTitle,
                content = "Carte mentale : ${nodes.size} nœuds",
                subjectName = selectedSubjectName,
                category = selectedFolder,
                noteType = NoteTypes.MINDMAP,
                mindMapDataJson = jsonStr,
                folderName = selectedFolder,
                attachedPdfId = attachedPdfId,
                attachedPdfTitle = attachedPdfTitle
            )
        } else {
            val updated = initialNote.copy(
                notebookId = selectedNotebookId,
                notebookTitle = selectedNotebookTitle,
                title = finalTitle,
                content = "Carte mentale : ${nodes.size} nœuds",
                subjectName = selectedSubjectName,
                category = selectedFolder,
                isFavorite = isFavorite,
                noteType = NoteTypes.MINDMAP,
                mindMapDataJson = jsonStr,
                folderName = selectedFolder,
                attachedPdfId = attachedPdfId,
                attachedPdfTitle = attachedPdfTitle,
                updatedAt = System.currentTimeMillis()
            )
            viewModel.updateNote(updated)
        }
        saveStatus = "Enregistré"
    }

    // Auto-save debounce effect
    LaunchedEffect(nodes.size, title, mapStyle, selectedSubjectName, attachedPdfId) {
        saveStatus = "Modification..."
        delay(1500)
        saveMindMap()
    }

    // Background color based on theme
    val bgColor = when (mapStyle) {
        "DARK_NEON" -> Color(0xFF0F172A)
        "PASTEL_STUDY" -> Color(0xFFFEF9C3).copy(alpha = 0.35f)
        "MINIMALIST" -> Color(0xFFFAFAFA)
        else -> Color(0xFFF8FAFC)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title.ifBlank { "Carte mentale" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${nodes.size} idées • Zoom ${(zoomScale * 100).roundToInt()}% • $saveStatus",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    saveMindMap()
                    onClose()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                IconButton(onClick = { applyUndo() }, enabled = undoHistory.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Annuler",
                        tint = if (undoHistory.isNotEmpty()) PrimaryIndigo else Color.LightGray
                    )
                }
                IconButton(onClick = { applyRedo() }, enabled = redoHistory.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Rétablir",
                        tint = if (redoHistory.isNotEmpty()) PrimaryIndigo else Color.LightGray
                    )
                }
                Box {
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Thème visuel")
                    }
                    DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Moderne Indigo") },
                            onClick = { mapStyle = "MODERN_INDIGO"; showThemeMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Pastel Scolaire") },
                            onClick = { mapStyle = "PASTEL_STUDY"; showThemeMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sombre Néon") },
                            onClick = { mapStyle = "DARK_NEON"; showThemeMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Minimaliste") },
                            onClick = { mapStyle = "MINIMALIST"; showThemeMenu = false }
                        )
                    }
                }
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favori",
                        tint = if (isFavorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (initialNote != null && onDelete != null) {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = {
                    saveMindMap()
                    onClose()
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Enregistrer", tint = PrimaryIndigo)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Sub Toolbar: Title & Zoom Controls
        Surface(
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicInlineTitleField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.3f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom -", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "${(zoomScale * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { zoomScale = 1f }
                    )
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(3.0f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom +", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            panX = 0f
                            panY = 0f
                            zoomScale = 1f
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Recentrer", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // PDF banner if linked
        if (attachedPdfId != null && attachedPdfTitle.isNotBlank()) {
            Surface(
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF lié : $attachedPdfTitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    TextButton(
                        onClick = {
                            val pdf = allPdfs.find { it.id == attachedPdfId }
                            if (pdf != null) {
                                saveMindMap()
                                viewModel.openPdf(pdf)
                                viewModel.setSelectedTab(1)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Ouvrir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Toolbar for Selected Node
        if (selectedNode != null) {
            Surface(
                color = PrimaryIndigo.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "\"${selectedNode.text.take(15)}\"",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )

                    // Add Child Node Button
                    ActionChipButton(icon = Icons.Default.Add, label = "+ Sous-idée") {
                        recordUndoCheckpoint()
                        val newChild = MindMapNode(
                            id = UUID.randomUUID().toString(),
                            text = "Nouvelle idée",
                            x = selectedNode.x + 180f,
                            y = selectedNode.y + 70f,
                            parentId = selectedNode.id,
                            colorHex = selectedNode.colorHex
                        )
                        nodes.add(newChild)
                        selectedNodeId = newChild.id
                    }

                    // Edit Text Button
                    ActionChipButton(icon = Icons.Default.Edit, label = "Modifier") {
                        editingNodeTarget = selectedNode
                    }

                    // Cycle Color Button
                    ActionChipButton(icon = Icons.Default.FormatPaint, label = "Couleur") {
                        recordUndoCheckpoint()
                        val palette = listOf("#4F46E5", "#06B6D4", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899")
                        val currentIdx = palette.indexOf(selectedNode.colorHex)
                        val nextCol = palette[(currentIdx + 1) % palette.size]
                        selectedNode.colorHex = nextCol
                    }

                    // Delete Node (only if not root)
                    if (selectedNode.parentId != null) {
                        ActionChipButton(icon = Icons.Default.Delete, label = "Supprimer") {
                            recordUndoCheckpoint()
                            // Delete this node and its descendants
                            val toDelete = mutableSetOf(selectedNode.id)
                            var added = true
                            while (added) {
                                val currentCount = toDelete.size
                                nodes.forEach { n ->
                                    if (n.parentId in toDelete) {
                                        toDelete.add(n.id)
                                    }
                                }
                                added = (toDelete.size > currentCount)
                            }
                            nodes.removeAll { it.id in toDelete }
                            selectedNodeId = null
                        }
                    }
                }
            }
        }

        // ==========================================
        // MIND MAP CANVAS
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .background(bgColor)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        panX += pan.x
                        panY += pan.y
                        zoomScale = (zoomScale * zoom).coerceIn(0.25f, 3.5f)
                    }
                }
        ) {
            // Connection Curves Layer (Bézier curves from parent to children)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val nodeMap = nodes.associateBy { it.id }

                for (node in nodes) {
                    val parent = node.parentId?.let { nodeMap[it] }
                    if (parent != null) {
                        val parentScreenX = parent.x * zoomScale + panX + 60f * zoomScale
                        val parentScreenY = parent.y * zoomScale + panY + 24f * zoomScale

                        val childScreenX = node.x * zoomScale + panX + 60f * zoomScale
                        val childScreenY = node.y * zoomScale + panY + 24f * zoomScale

                        val midX = (parentScreenX + childScreenX) / 2f
                        val path = Path().apply {
                            moveTo(parentScreenX, parentScreenY)
                            cubicTo(
                                midX, parentScreenY,
                                midX, childScreenY,
                                childScreenX, childScreenY
                            )
                        }

                        val branchColor = parseHexColor(node.colorHex)
                        drawPath(
                            path = path,
                            color = branchColor.copy(alpha = 0.85f),
                            style = Stroke(
                                width = 3.5f * zoomScale.coerceIn(0.7f, 2.5f),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }

            // Render Nodes
            for (node in nodes) {
                val isSelected = (node.id == selectedNodeId)
                val isRoot = (node.parentId == null)
                val nodeColor = parseHexColor(node.colorHex)

                val screenX = node.x * zoomScale + panX
                val screenY = node.y * zoomScale + panY
                val nodeWidth = if (isRoot) 150f * zoomScale else 130f * zoomScale
                val nodeHeight = if (isRoot) 56f * zoomScale else 48f * zoomScale

                Box(
                    modifier = Modifier
                        .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                        .size(nodeWidth.dp, nodeHeight.dp)
                        .pointerInput(node.id) {
                            detectDragGestures(
                                onDragStart = {
                                    selectedNodeId = node.id
                                    recordUndoCheckpoint()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    node.x += dragAmount.x / zoomScale
                                    node.y += dragAmount.y / zoomScale
                                }
                            )
                        }
                        .clickable { selectedNodeId = node.id }
                ) {
                    Surface(
                        shape = if (node.shape == "PILL" || isRoot) RoundedCornerShape(24.dp) else RoundedCornerShape(10.dp),
                        color = if (isRoot) nodeColor else Color.White,
                        border = if (!isRoot) androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.5.dp,
                            color = if (isSelected) PrimaryIndigo else nodeColor
                        ) else null,
                        shadowElevation = if (isSelected) 6.dp else 3.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = node.text,
                                fontSize = if (isRoot) (13 * zoomScale).sp else (11 * zoomScale).sp,
                                fontWeight = if (isRoot) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isRoot) Color.White else Color(0xFF1E293B),
                                maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Node Text Dialog
    if (editingNodeTarget != null) {
        var textInput by remember { mutableStateOf(editingNodeTarget?.text ?: "") }
        AlertDialog(
            onDismissRequest = { editingNodeTarget = null },
            title = { Text("Modifier l'idée") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Texte de l'idée") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    recordUndoCheckpoint()
                    editingNodeTarget?.text = textInput.ifBlank { "Idée" }
                    editingNodeTarget = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNodeTarget = null }) { Text("Annuler") }
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirmDialog && initialNote != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer cette carte mentale ?") },
            text = { Text("Cette action est irréversible.") },
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
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Annuler") }
            }
        )
    }
}

package com.example.ui.screens.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CanvasData
import com.example.data.model.CanvasElement
import com.example.data.model.CanvasTool
import com.example.data.model.DrawingStroke
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTypes
import com.example.data.model.StrokePoint
import com.example.ui.StudyViewModel
import com.example.ui.components.parseHexColor
import com.example.ui.theme.PrimaryIndigo
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperNoteCanvasEditor(
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

    val superNoteViewModel: SuperNoteViewModel = viewModel(
        factory = SuperNoteViewModelFactory(
            repository = viewModel.repository,
            initialNote = initialNote,
            defaultNotebookId = defaultNotebookId,
            defaultNotebookTitle = defaultNotebookTitle,
            defaultSubjectName = initialNote?.subjectName ?: (subjects.firstOrNull()?.name ?: "Général")
        ),
        key = "SuperNote_${initialNote?.id ?: "new"}"
    )

    val coroutineScope = rememberCoroutineScope()

    val title by superNoteViewModel.title.collectAsState()
    val selectedSubjectName by superNoteViewModel.selectedSubjectName.collectAsState()
    val selectedNotebookId by superNoteViewModel.selectedNotebookId.collectAsState()
    val selectedNotebookTitle by superNoteViewModel.selectedNotebookTitle.collectAsState()
    val selectedFolder by superNoteViewModel.selectedFolder.collectAsState()
    val isFavorite by superNoteViewModel.isFavorite.collectAsState()
    val attachedPdfId by superNoteViewModel.attachedPdfId.collectAsState()
    val attachedPdfTitle by superNoteViewModel.attachedPdfTitle.collectAsState()
    val attachedPdfPage by superNoteViewModel.attachedPdfPage.collectAsState()

    // Canvas State: Pan & Zoom from ViewModel
    val panX by superNoteViewModel.panX.collectAsState()
    val panY by superNoteViewModel.panY.collectAsState()
    val zoomScale by superNoteViewModel.zoomScale.collectAsState()

    // Elements & Strokes from ViewModel
    val elements by superNoteViewModel.elements.collectAsState()
    val strokes by superNoteViewModel.strokes.collectAsState()
    val selectedElementId by superNoteViewModel.selectedElementId.collectAsState()
    val selectedElement = elements.find { it.id == selectedElementId }

    // Undo / Redo from ViewModel
    val canUndo by superNoteViewModel.canUndo.collectAsState()
    val canRedo by superNoteViewModel.canRedo.collectAsState()
    val saveStatus by superNoteViewModel.saveStatus.collectAsState()

    // Active Tool & Drawing properties
    val activeTool by superNoteViewModel.activeTool.collectAsState()
    val penColorHex by superNoteViewModel.penColorHex.collectAsState()
    val penStrokeWidth by superNoteViewModel.penStrokeWidth.collectAsState()

    // Temporary active stroke for smooth drawing
    var currentDrawingPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    // Dialog states
    var showAddTextDialog by remember { mutableStateOf(false) }
    var editTextTarget by remember { mutableStateOf<CanvasElement.Text?>(null) }
    var showAddShapeDialog by remember { mutableStateOf(false) }
    var showAddTableDialog by remember { mutableStateOf(false) }
    var showStickerDialog by remember { mutableStateOf(false) }
    var showPdfPickerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingTableCell by remember { mutableStateOf<Triple<CanvasElement.Table, Int, Int>?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val newImg = CanvasElement.Image(
                id = UUID.randomUUID().toString(),
                x = -panX / zoomScale + 150f,
                y = -panY / zoomScale + 150f,
                width = 240f,
                height = 180f,
                uri = it.toString()
            )
            superNoteViewModel.addElement(newImg)
            superNoteViewModel.selectElement(newImg.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Header Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title.ifBlank { "Super Note" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Toile infinie • Zoom ${(zoomScale * 100).roundToInt()}% • $saveStatus",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            superNoteViewModel.saveImmediately()
                            onClose()
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                IconButton(onClick = { superNoteViewModel.undo() }, enabled = canUndo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Annuler",
                        tint = if (canUndo) PrimaryIndigo else Color.LightGray
                    )
                }
                IconButton(onClick = { superNoteViewModel.redo() }, enabled = canRedo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Rétablir",
                        tint = if (canRedo) PrimaryIndigo else Color.LightGray
                    )
                }
                IconButton(onClick = { superNoteViewModel.setFavorite(!isFavorite) }) {
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
                    coroutineScope.launch {
                        superNoteViewModel.saveImmediately()
                        onClose()
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Enregistrer", tint = PrimaryIndigo)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Metadata & Quick Zoom Toolbar
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
                // Title inline edit
                BasicInlineTitleField(
                    value = title,
                    onValueChange = { superNoteViewModel.setTitle(it) },
                    modifier = Modifier.weight(1f)
                )

                // Zoom controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { superNoteViewModel.setZoom(zoomScale - 0.15f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom -", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "${(zoomScale * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { superNoteViewModel.setZoom(1f) }
                    )
                    IconButton(
                        onClick = { superNoteViewModel.setZoom(zoomScale + 0.15f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom +", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { superNoteViewModel.resetCamera() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Recentrer", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Attached PDF strip if present
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
                        text = "PDF lié : $attachedPdfTitle${if ((attachedPdfPage ?: 1) > 1) " (page $attachedPdfPage)" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                superNoteViewModel.saveImmediately()
                                viewModel.openPdfAtPage(attachedPdfId!!, attachedPdfPage ?: 1)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Ouvrir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { superNoteViewModel.detachPdf() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Détacher", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Tool Mode Selection Bar (Pan, Pen, Highlighter, Eraser, + Elements)
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolTabButton(
                    icon = Icons.Default.PanTool,
                    label = "Sélection",
                    isSelected = activeTool == CanvasTool.PAN_SELECT,
                    onClick = { superNoteViewModel.setActiveTool(CanvasTool.PAN_SELECT) }
                )
                ToolTabButton(
                    icon = Icons.Default.BorderColor,
                    label = "Stylet",
                    isSelected = activeTool == CanvasTool.PEN,
                    onClick = { superNoteViewModel.setActiveTool(CanvasTool.PEN) }
                )
                ToolTabButton(
                    icon = Icons.Default.Highlight,
                    label = "Surligneur",
                    isSelected = activeTool == CanvasTool.HIGHLIGHTER,
                    onClick = { superNoteViewModel.setActiveTool(CanvasTool.HIGHLIGHTER) }
                )
                ToolTabButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "Gomme",
                    isSelected = activeTool == CanvasTool.ERASER,
                    onClick = { superNoteViewModel.setActiveTool(CanvasTool.ERASER) }
                )

                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.4f)))
                Spacer(modifier = Modifier.width(6.dp))

                // Insert Object Buttons
                ActionChipButton(icon = Icons.Default.TextFields, label = "+ Texte") {
                    showAddTextDialog = true
                }
                ActionChipButton(icon = Icons.Default.ChangeHistory, label = "+ Forme") {
                    showAddShapeDialog = true
                }
                ActionChipButton(icon = Icons.Default.TableChart, label = "+ Tableau") {
                    showAddTableDialog = true
                }
                ActionChipButton(icon = Icons.Default.Star, label = "+ Autocollant") {
                    showStickerDialog = true
                }
                ActionChipButton(icon = Icons.Default.Image, label = "+ Image") {
                    imagePickerLauncher.launch(arrayOf("image/*"))
                }
                ActionChipButton(icon = Icons.Default.PictureAsPdf, label = "+ Lier PDF") {
                    showPdfPickerDialog = true
                }
            }
        }

        // Secondary Pen / Color Toolbar if Pen or Highlighter is active
        if (activeTool == CanvasTool.PEN || activeTool == CanvasTool.HIGHLIGHTER) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf("#1E293B", "#4F46E5", "#2563EB", "#DC2626", "#16A34A", "#CA8A04", "#EA580C", "#9333EA")
                    colors.forEach { hex ->
                        val col = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (penColorHex == hex) 2.5.dp else 1.dp,
                                    color = if (penColorHex == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { superNoteViewModel.setPenColor(hex) }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Taille :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val sizes = listOf(3f to "Fin", 6f to "Moyen", 12f to "Épais")
                    sizes.forEach { (sz, lbl) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (penStrokeWidth == sz) PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.clickable { superNoteViewModel.setPenStrokeWidth(sz) }
                        ) {
                            Text(
                                text = lbl,
                                fontSize = 11.sp,
                                fontWeight = if (penStrokeWidth == sz) FontWeight.Bold else FontWeight.Normal,
                                color = if (penStrokeWidth == sz) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Contextual action strip for Selected Element
        if (selectedElement != null && activeTool == CanvasTool.PAN_SELECT) {
            Surface(
                color = PrimaryIndigo.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Objet sélectionné : ${selectedElement.javaClass.simpleName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (selectedElement is CanvasElement.Text) {
                            IconButton(onClick = { editTextTarget = selectedElement }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Éditer texte", modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = {
                                superNoteViewModel.duplicateElement(selectedElement.id)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Dupliquer", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                superNoteViewModel.deleteElement(selectedElement.id)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ==========================================
        // MAIN INFINITE CANVAS DRAWING / INTERACTION
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .background(Color(0xFFF8FAFC))
                .pointerInput(activeTool) {
                    when (activeTool) {
                        CanvasTool.PAN_SELECT -> {
                            detectTransformGestures { _, pan, zoom, _ ->
                                superNoteViewModel.updatePan(pan.x, pan.y)
                                superNoteViewModel.setZoom(zoomScale * zoom)
                            }
                        }
                        CanvasTool.PEN, CanvasTool.HIGHLIGHTER -> {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val canvasX = (offset.x - panX) / zoomScale
                                    val canvasY = (offset.y - panY) / zoomScale
                                    currentDrawingPoints = listOf(StrokePoint(canvasX, canvasY))
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val canvasX = (change.position.x - panX) / zoomScale
                                    val canvasY = (change.position.y - panY) / zoomScale
                                    currentDrawingPoints = currentDrawingPoints + StrokePoint(canvasX, canvasY)
                                },
                                onDragEnd = {
                                    if (currentDrawingPoints.size > 1) {
                                        superNoteViewModel.addStroke(
                                            DrawingStroke(
                                                points = currentDrawingPoints,
                                                colorHex = penColorHex,
                                                strokeWidth = penStrokeWidth,
                                                isHighlighter = (activeTool == CanvasTool.HIGHLIGHTER)
                                            )
                                        )
                                    }
                                    currentDrawingPoints = emptyList()
                                },
                                onDragCancel = {
                                    currentDrawingPoints = emptyList()
                                }
                            )
                        }
                        CanvasTool.ERASER -> {
                            detectTapGestures { tapOffset ->
                                val canvasX = (tapOffset.x - panX) / zoomScale
                                val canvasY = (tapOffset.y - panY) / zoomScale
                                superNoteViewModel.eraseStrokesNear(canvasX, canvasY, 30f / zoomScale)
                            }
                        }
                    }
                }
        ) {
            // Background grid pattern (dots)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 32f * zoomScale
                val startX = (panX % step + step) % step
                val startY = (panY % step + step) % step

                var x = startX
                while (x < size.width) {
                    var y = startY
                    while (y < size.height) {
                        drawCircle(
                            color = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                            radius = 1.5f * zoomScale.coerceIn(0.7f, 2f),
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }

                // Render saved drawing strokes
                for (stroke in strokes) {
                    if (stroke.points.size < 2) continue
                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(first.x * zoomScale + panX, first.y * zoomScale + panY)
                    for (i in 1 until stroke.points.size) {
                        val p = stroke.points[i]
                        path.lineTo(p.x * zoomScale + panX, p.y * zoomScale + panY)
                    }
                    val baseCol = parseHexColor(stroke.colorHex)
                    val finalCol = if (stroke.isHighlighter) baseCol.copy(alpha = 0.35f) else baseCol
                    val widthPx = (if (stroke.isHighlighter) stroke.strokeWidth * 2.8f else stroke.strokeWidth) * zoomScale
                    drawPath(
                        path = path,
                        color = finalCol,
                        style = Stroke(
                            width = widthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Render current in-progress stroke
                if (currentDrawingPoints.size > 1) {
                    val path = Path()
                    val first = currentDrawingPoints.first()
                    path.moveTo(first.x * zoomScale + panX, first.y * zoomScale + panY)
                    for (i in 1 until currentDrawingPoints.size) {
                        val p = currentDrawingPoints[i]
                        path.lineTo(p.x * zoomScale + panX, p.y * zoomScale + panY)
                    }
                    val baseCol = parseHexColor(penColorHex)
                    val finalCol = if (activeTool == CanvasTool.HIGHLIGHTER) baseCol.copy(alpha = 0.35f) else baseCol
                    val widthPx = (if (activeTool == CanvasTool.HIGHLIGHTER) penStrokeWidth * 2.8f else penStrokeWidth) * zoomScale
                    drawPath(
                        path = path,
                        color = finalCol,
                        style = Stroke(
                            width = widthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Render interactive Canvas Elements
            for (element in elements) {
                val isSelected = (element.id == selectedElementId)
                val screenX = element.x * zoomScale + panX
                val screenY = element.y * zoomScale + panY
                val screenW = element.width * zoomScale
                val screenH = element.height * zoomScale

                Box(
                    modifier = Modifier
                        .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                        .size(screenW.dp, screenH.dp)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) PrimaryIndigo else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .pointerInput(activeTool, isSelected) {
                            if (activeTool == CanvasTool.PAN_SELECT) {
                                detectDragGestures(
                                    onDragStart = {
                                        superNoteViewModel.selectElement(element.id)
                                        superNoteViewModel.recordUndoCheckpoint()
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        element.x += dragAmount.x / zoomScale
                                        element.y += dragAmount.y / zoomScale
                                    }
                                )
                            }
                        }
                        .clickable {
                            if (activeTool == CanvasTool.PAN_SELECT) {
                                superNoteViewModel.selectElement(element.id)
                            }
                        }
                ) {
                    // Element Content Rendering
                    when (element) {
                        is CanvasElement.Text -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = parseHexColor(element.backgroundColorHex),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = element.text.ifBlank { "Texte..." },
                                        fontSize = (element.fontSize * zoomScale).sp,
                                        fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal,
                                        color = parseHexColor(element.textColorHex)
                                    )
                                }
                            }
                        }
                        is CanvasElement.Image -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 3.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AsyncImage(
                                    model = element.uri,
                                    contentDescription = "Image canvas",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        is CanvasElement.Shape -> {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val col = parseHexColor(element.colorHex)
                                when (element.shapeType) {
                                    "CIRCLE" -> {
                                        if (element.isFilled) {
                                            drawOval(color = col)
                                        } else {
                                            drawOval(color = col, style = Stroke(width = element.strokeWidth * zoomScale))
                                        }
                                    }
                                    "LINE" -> {
                                        drawLine(
                                            color = col,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = element.strokeWidth * zoomScale,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    "ARROW" -> {
                                        drawLine(
                                            color = col,
                                            start = Offset(0f, size.height / 2),
                                            end = Offset(size.width, size.height / 2),
                                            strokeWidth = element.strokeWidth * zoomScale,
                                            cap = StrokeCap.Round
                                        )
                                        // Arrowhead
                                        val arrowSize = 16f * zoomScale
                                        val endX = size.width
                                        val endY = size.height / 2
                                        val arrowPath = Path().apply {
                                            moveTo(endX, endY)
                                            lineTo(endX - arrowSize, endY - arrowSize / 1.5f)
                                            lineTo(endX - arrowSize, endY + arrowSize / 1.5f)
                                            close()
                                        }
                                        drawPath(arrowPath, col)
                                    }
                                    else -> { // RECTANGLE
                                        if (element.isFilled) {
                                            drawRect(color = col)
                                        } else {
                                            drawRect(color = col, style = Stroke(width = element.strokeWidth * zoomScale))
                                        }
                                    }
                                }
                            }
                        }
                        is CanvasElement.Table -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    for (r in 0 until element.rows) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        ) {
                                            for (c in 0 until element.cols) {
                                                val cellText = element.cells.getOrNull(r)?.getOrNull(c) ?: ""
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxSize()
                                                        .border(0.5.dp, Color(0xFFCBD5E1))
                                                        .background(if (r == 0) Color(0xFFF1F5F9) else Color.White)
                                                        .clickable {
                                                            editingTableCell = Triple(element, r, c)
                                                        }
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = cellText.ifBlank { if (r == 0) "Titre" else "-" },
                                                        fontSize = (11 * zoomScale).sp,
                                                        fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is CanvasElement.Sticker -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 3.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = element.emoji, fontSize = (28 * zoomScale).sp)
                                    Text(
                                        text = element.label,
                                        fontSize = (10 * zoomScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        is CanvasElement.PdfRef -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 3.dp,
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF Ref",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size((18 * zoomScale).dp.coerceAtLeast(14.dp))
                                        )
                                        Text(
                                            text = element.pdfTitle.ifBlank { "Document PDF" },
                                            fontSize = (11 * zoomScale).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1
                                        )
                                    }
                                    if (element.noteSnippet.isNotBlank()) {
                                        Text(
                                            text = element.noteSnippet,
                                            fontSize = (9 * zoomScale).sp,
                                            color = Color(0xFF475569),
                                            maxLines = 2
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                        modifier = Modifier.clickable {
                                            coroutineScope.launch {
                                                superNoteViewModel.saveImmediately()
                                                viewModel.openPdfAtPage(element.pdfId, element.pageNumber)
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size((12 * zoomScale).dp.coerceAtLeast(10.dp))
                                            )
                                            Text(
                                                text = "Page ${element.pageNumber} • Ouvrir",
                                                fontSize = (9 * zoomScale).sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Resize handle (bottom-right corner) when selected
                    if (isSelected && activeTool == CanvasTool.PAN_SELECT) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 6.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        element.width = (element.width + dragAmount.x / zoomScale).coerceAtLeast(60f)
                                        element.height = (element.height + dragAmount.y / zoomScale).coerceAtLeast(40f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Add Text Dialog
    if (showAddTextDialog) {
        var textInput by remember { mutableStateOf("Nouveau texte") }
        var isBold by remember { mutableStateOf(false) }
        var textColorHex by remember { mutableStateOf("#1E293B") }

        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text("Ajouter un bloc texte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Texte du bloc") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isBold) PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.clickable { isBold = !isBold }
                        ) {
                            Text("Gras", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newElement = CanvasElement.Text(
                        id = UUID.randomUUID().toString(),
                        x = -panX / zoomScale + 120f,
                        y = -panY / zoomScale + 120f,
                        text = textInput,
                        isBold = isBold,
                        textColorHex = textColorHex
                    )
                    superNoteViewModel.addElement(newElement)
                    showAddTextDialog = false
                }) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) { Text("Annuler") }
            }
        )
    }

    // 2. Edit Text Dialog
    if (editTextTarget != null) {
        var textInput by remember { mutableStateOf(editTextTarget?.text ?: "") }
        var isBold by remember { mutableStateOf(editTextTarget?.isBold ?: false) }
        var fontSize by remember { mutableFloatStateOf(editTextTarget?.fontSize ?: 16f) }

        AlertDialog(
            onDismissRequest = { editTextTarget = null },
            title = { Text("Modifier le texte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isBold) PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.clickable { isBold = !isBold }
                        ) {
                            Text("Gras", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    editTextTarget?.let { target ->
                        val updated = target.copy(
                            text = textInput,
                            isBold = isBold,
                            fontSize = fontSize
                        )
                        superNoteViewModel.updateElement(updated)
                    }
                    editTextTarget = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTextTarget = null }) { Text("Annuler") }
            }
        )
    }

    // 3. Add Shape Dialog
    if (showAddShapeDialog) {
        AlertDialog(
            onDismissRequest = { showAddShapeDialog = false },
            title = { Text("Insérer une forme") },
            text = {
                val shapes = listOf(
                    "RECTANGLE" to "Rectangle",
                    "CIRCLE" to "Cercle",
                    "LINE" to "Ligne",
                    "ARROW" to "Flèche"
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shapes.forEach { (type, label) ->
                        Card(
                            onClick = {
                                val shapeElem = CanvasElement.Shape(
                                    id = UUID.randomUUID().toString(),
                                    x = -panX / zoomScale + 120f,
                                    y = -panY / zoomScale + 120f,
                                    width = if (type == "LINE" || type == "ARROW") 200f else 160f,
                                    height = if (type == "LINE" || type == "ARROW") 40f else 120f,
                                    shapeType = type,
                                    colorHex = "#4F46E5"
                                )
                                superNoteViewModel.addElement(shapeElem)
                                showAddShapeDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddShapeDialog = false }) { Text("Fermer") }
            }
        )
    }

    // 4. Add Table Dialog
    if (showAddTableDialog) {
        var rows by remember { mutableStateOf("3") }
        var cols by remember { mutableStateOf("3") }

        AlertDialog(
            onDismissRequest = { showAddTableDialog = false },
            title = { Text("Ajouter un tableau") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = rows,
                        onValueChange = { rows = it },
                        label = { Text("Nombre de lignes") }
                    )
                    OutlinedTextField(
                        value = cols,
                        onValueChange = { cols = it },
                        label = { Text("Nombre de colonnes") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val r = rows.toIntOrNull()?.coerceIn(2, 6) ?: 3
                    val c = cols.toIntOrNull()?.coerceIn(2, 6) ?: 3
                    val tbl = CanvasElement.Table(
                        id = UUID.randomUUID().toString(),
                        x = -panX / zoomScale + 100f,
                        y = -panY / zoomScale + 100f,
                        rows = r,
                        cols = c,
                        cells = MutableList(r) { MutableList(c) { "" } }
                    )
                    superNoteViewModel.addElement(tbl)
                    showAddTableDialog = false
                }) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTableDialog = false }) { Text("Annuler") }
            }
        )
    }

    // 5. Edit Table Cell Dialog
    if (editingTableCell != null) {
        val (table, row, col) = editingTableCell!!
        var cellContent by remember { mutableStateOf(table.cells.getOrNull(row)?.getOrNull(col) ?: "") }

        AlertDialog(
            onDismissRequest = { editingTableCell = null },
            title = { Text("Cellule [L${row + 1}, C${col + 1}]") },
            text = {
                OutlinedTextField(
                    value = cellContent,
                    onValueChange = { cellContent = it },
                    label = { Text("Contenu de la case") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newCells = table.cells.map { it.toMutableList() }.toMutableList()
                    if (row < newCells.size && col < newCells[row].size) {
                        newCells[row][col] = cellContent
                    }
                    val updatedTable = table.copy(cells = newCells)
                    superNoteViewModel.updateElement(updatedTable)
                    editingTableCell = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTableCell = null }) { Text("Annuler") }
            }
        )
    }

    // 6. Sticker Picker Dialog
    if (showStickerDialog) {
        val studyStickers = listOf(
            "⭐" to "Important",
            "💡" to "Idée",
            "📌" to "À retenir",
            "⚠️" to "Attention",
            "❗" to "Examen",
            "✅" to "Terminé",
            "🧠" to "À mémoriser",
            "📚" to "Définition",
            "🎯" to "Objectif"
        )
        AlertDialog(
            onDismissRequest = { showStickerDialog = false },
            title = { Text("Autocollants d'étude") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studyStickers.forEach { (emoji, label) ->
                        Card(
                            onClick = {
                                val stk = CanvasElement.Sticker(
                                    id = UUID.randomUUID().toString(),
                                    x = -panX / zoomScale + 120f,
                                    y = -panY / zoomScale + 120f,
                                    emoji = emoji,
                                    label = label
                                )
                                superNoteViewModel.addElement(stk)
                                showStickerDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStickerDialog = false }) { Text("Fermer") }
            }
        )
    }

    // 7. PDF Picker Dialog
    if (showPdfPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPdfPickerDialog = false },
            title = { Text("Lier un PDF à la Super Note") },
            text = {
                if (allPdfs.isEmpty()) {
                    Text("Aucun document PDF dans la bibliothèque. Importe d'abord un PDF dans l'onglet PDF.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allPdfs.forEach { pdf ->
                            Card(
                                onClick = {
                                    superNoteViewModel.attachPdf(pdf.id, pdf.title, 1)
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
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPdfPickerDialog = false }) { Text("Fermer") }
            }
        )
    }

    // 8. Delete Confirm Dialog
    if (showDeleteConfirmDialog && initialNote != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer cette Super Note ?") },
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

@Composable
fun ToolTabButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) PrimaryIndigo else Color.Transparent,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionChipButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BasicInlineTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Titre...", fontSize = 14.sp) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
    )
}

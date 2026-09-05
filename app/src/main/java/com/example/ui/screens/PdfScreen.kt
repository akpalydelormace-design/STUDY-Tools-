package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PdfDocumentEntity
import com.example.ui.StudyViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PodcastPlayerView
import com.example.ui.components.formatTimestampToDate
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryIndigo

@Composable
fun PdfScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val activePdf by viewModel.activePdf.collectAsState()

    if (activePdf != null) {
        PdfReaderView(viewModel = viewModel, pdf = activePdf!!)
    } else {
        PdfLibraryView(viewModel = viewModel, modifier = modifier)
    }
}

@Composable
fun PdfLibraryView(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val pdfs by viewModel.pdfs.collectAsState()
    var pdfToDelete by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPdfUri(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdf_library_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lecteur PDF",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${pdfs.size} document${if (pdfs.size > 1) "s" else ""} • Recherche locale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("import_pdf_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Importer",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Importer",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (pdfs.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.PictureAsPdf,
                title = "Aucun document PDF",
                description = "Importe tes cours, fiches de révision et polycopiés depuis ton téléphone. Ils restent disponibles 100% hors ligne.",
                actionLabel = "Importer un fichier PDF",
                onActionClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pdfs, key = { it.id }) { pdf ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_item_${pdf.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        onClick = { viewModel.openPdf(pdf) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pdf.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Page ${pdf.lastPageRead} / ${pdf.pageCount}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatTimestampToDate(pdf.lastOpenedAt),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { pdfToDelete = pdf },
                                modifier = Modifier.testTag("delete_pdf_${pdf.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Supprimer de la bibliothèque",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog to remove PDF from library
    if (pdfToDelete != null) {
        AlertDialog(
            onDismissRequest = { pdfToDelete = null },
            title = { Text("Supprimer de la bibliothèque ?") },
            text = {
                Text(
                    "Le document sera retiré de Study Tools sans supprimer le fichier original sur ton appareil."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pdfToDelete?.let { viewModel.deletePdf(it) }
                        pdfToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun PdfReaderView(
    viewModel: StudyViewModel,
    pdf: PdfDocumentEntity
) {
    val currentPage by viewModel.currentPdfPage.collectAsState()
    val currentBitmap by viewModel.currentPdfBitmap.collectAsState()
    val isLoading by viewModel.isPdfLoading.collectAsState()
    val searchQuery by viewModel.pdfSearchQuery.collectAsState()
    val searchResults by viewModel.pdfSearchResults.collectAsState()
    val currentMatchIdx by viewModel.currentPdfSearchMatchIndex.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showJumpPageDialog by remember { mutableStateOf(false) }
    var targetPageInput by remember { mutableStateOf(currentPage.toString()) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("pdf_reader_screen")
    ) {
        // Top Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.closePdfReader() },
                        modifier = Modifier.testTag("pdf_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = pdf.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page $currentPage sur ${pdf.pageCount}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                targetPageInput = currentPage.toString()
                                showJumpPageDialog = true
                            }
                        )
                    }

                    // Direct Page Jump
                    IconButton(onClick = {
                        targetPageInput = currentPage.toString()
                        showJumpPageDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.FindInPage,
                            contentDescription = "Aller à la page",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Search Toggle
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) {
                                viewModel.performPdfSearch("")
                            }
                        },
                        modifier = Modifier.testTag("pdf_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Highly visible Search Bar (when search is open)
                if (isSearchActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.performPdfSearch(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pdf_search_input"),
                                placeholder = { Text("Rechercher un mot, une expression...") },
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.performPdfSearch("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Prev Match
                            IconButton(
                                onClick = { viewModel.prevPdfSearchResult() },
                                enabled = searchResults.isNotEmpty()
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Résultat précédent")
                            }

                            // Next Match
                            IconButton(
                                onClick = { viewModel.nextPdfSearchResult() },
                                enabled = searchResults.isNotEmpty(),
                                modifier = Modifier.testTag("pdf_next_search_result")
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Résultat suivant")
                            }
                        }

                        // Match Count & Snippet Preview
                        if (searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (searchResults.isNotEmpty()) {
                                    Text(
                                        text = "Résultat ${currentMatchIdx + 1} sur ${searchResults.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Page ${searchResults[currentMatchIdx].pageIndex + 1}",
                                        fontSize = 12.sp,
                                        color = AccentEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Aucun résultat pour cette recherche (les scans d'images sans texte ne contiennent pas de texte indexé)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Context preview
                            if (searchResults.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = searchResults[currentMatchIdx].previewContext,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // PDF Page Viewport (Zoomable / Pannable)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        val maxOffsetX = (1000f * (scale - 1f))
                        val maxOffsetY = (1400f * (scale - 1f))
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PrimaryIndigo)
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "Page $currentPage du PDF",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                Text(
                    text = "Impossible de charger la page $currentPage",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Integrated Podcast IA Player
        PodcastPlayerView(viewModel = viewModel, pdf = pdf)

        // Bottom Navigation Bar for PDF
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        viewModel.prevPdfPage()
                    },
                    enabled = currentPage > 1,
                    modifier = Modifier.testTag("pdf_prev_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Page précédente"
                    )
                }

                // Quick zoom reset / controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            scale = (scale - 0.5f).coerceAtLeast(1f)
                            if (scale == 1f) offset = Offset.Zero
                        }
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Dézoomer")
                    }
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable {
                                scale = 1f
                                offset = Offset.Zero
                            }
                            .padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            scale = (scale + 0.5f).coerceAtMost(4f)
                        }
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoomer")
                    }
                }

                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        viewModel.nextPdfPage()
                    },
                    enabled = currentPage < pdf.pageCount,
                    modifier = Modifier.testTag("pdf_next_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Page suivante"
                    )
                }
            }
        }
    }

    // Direct Jump to Page Dialog
    if (showJumpPageDialog) {
        AlertDialog(
            onDismissRequest = { showJumpPageDialog = false },
            title = { Text("Aller à la page") },
            text = {
                Column {
                    Text("Saisis un numéro de page entre 1 et ${pdf.pageCount} :")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetPageInput,
                        onValueChange = { targetPageInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pageNum = targetPageInput.toIntOrNull()
                    if (pageNum != null && pageNum in 1..pdf.pageCount) {
                        scale = 1f
                        offset = Offset.Zero
                        viewModel.goToPdfPage(pageNum)
                    }
                    showJumpPageDialog = false
                }) {
                    Text("Accéder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpPageDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

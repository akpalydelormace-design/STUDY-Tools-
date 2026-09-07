package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer

data class PdfSearchResult(
    val pageIndex: Int, // 0-based
    val matchText: String,
    val previewContext: String
)

object PdfHelper {

    private const val TAG = "PdfHelper"
    private var isPdfBoxInitialized = false

    /**
     * Initializes PDFBox for Android. Safe to call multiple times.
     */
    fun init(context: Context) {
        if (!isPdfBoxInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isPdfBoxInitialized = true
                Log.d(TAG, "PDFBoxResourceLoader initialized successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize PDFBoxResourceLoader", e)
            }
        }
    }

    /**
     * Normalizes text for case-insensitive and accent-insensitive matching.
     * Handles accents, ligatures, lowercase conversion, and collapses whitespace.
     */
    fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        val sanitized = input
            .replace("œ", "oe")
            .replace("Œ", "oe")
            .replace("æ", "ae")
            .replace("Æ", "ae")

        val unaccented = Normalizer.normalize(sanitized, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")

        return unaccented.lowercase().replace("\\s+".toRegex(), " ")
    }

    /**
     * Copies a PDF from user's storage Uri into internal storage so it is permanently
     * available offline.
     */
    suspend fun importPdfToInternalStorage(context: Context, uri: Uri): Pair<File, String> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var fileName = "document_${System.currentTimeMillis()}.pdf"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val queriedName = cursor.getString(nameIndex)
                if (!queriedName.isNullOrBlank()) {
                    fileName = queriedName
                }
            }
        }

        val pdfDir = File(context.filesDir, "study_pdfs")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        // Sanitize filename
        val safeName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val destinationFile = File(pdfDir, "${System.currentTimeMillis()}_$safeName")

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }

        Pair(destinationFile, fileName)
    }

    /**
     * Gets page count of a PDF using Android native PdfRenderer.
     */
    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page count", e)
            1
        }
    }

    /**
     * Renders a specific page to a high-quality Bitmap using native PdfRenderer.
     */
    suspend fun renderPageBitmap(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val height = (targetWidth * ratio).toInt().coerceAtLeast(100)
                        val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering page bitmap", e)
            null
        }
    }

    /**
     * Checks if extracted text indicates a scanned image PDF (no extractable text).
     */
    fun isScannedPdf(pagesText: Map<Int, String>): Boolean {
        if (pagesText.isEmpty()) return true
        val totalChars = pagesText.values.sumOf { it.trim().length }
        return totalChars < 10
    }

    /**
     * Extracts text per page from PDF using Apache PDFBox for Android.
     * Automatically handles font encodings, CMaps, CID fonts, subsetted fonts,
     * compressed object streams (/ObjStm), and PDF text operators.
     */
    suspend fun extractTextByPages(file: File, context: Context? = null): Map<Int, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Int, String>()
        if (context != null) {
            init(context)
        }

        try {
            PDDocument.load(file).use { document ->
                val totalPages = document.numberOfPages
                val stripper = PDFTextStripper()

                for (p in 0 until totalPages) {
                    val rawPageText = try {
                        stripper.startPage = p + 1
                        stripper.endPage = p + 1
                        stripper.getText(document) ?: ""
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting text on page $p for file ${file.name}", e)
                        ""
                    }

                    val cleanedText = rawPageText
                        .replace("\r", " ")
                        .replace("\n", " ")
                        .replace("\t", " ")
                        .replace("\\s+".toRegex(), " ")
                        .trim()

                    result[p] = cleanedText
                }

                // Log DEBUG [PDF_SEARCH]
                val totalExtractedLength = result.values.sumOf { it.length }
                Log.d("PDF_SEARCH", "pdfPath=${file.name}, totalPages=$totalPages, totalExtractedLength=$totalExtractedLength")
                for ((pageIdx, pageText) in result) {
                    Log.d("PDF_SEARCH", "pdfPath=${file.name}, page=$pageIdx, extractedTextLength=${pageText.length}")
                }

                if (totalExtractedLength == 0) {
                    Log.w("PDF_SEARCH", "WARNING: pdfPath=${file.name} extractedTextLength=0. PDF is likely a scanned image/photo.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF ${file.name} using PDFBox", e)
        }

        result
    }

    /**
     * Saves extracted page text to disk JSON cache.
     */
    fun savePageTextCacheToDisk(context: Context, pdfId: Long, pagesText: Map<Int, String>) {
        try {
            val cacheDir = File(context.filesDir, "pdf_text_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val cacheFile = File(cacheDir, "${pdfId}_cache.json")
            val json = JSONObject()
            for ((page, text) in pagesText) {
                json.put(page.toString(), text)
            }
            cacheFile.writeText(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving page text cache for pdf $pdfId", e)
        }
    }

    /**
     * Loads extracted page text from disk JSON cache if available.
     */
    fun loadPageTextCacheFromDisk(context: Context, pdfId: Long): Map<Int, String>? {
        try {
            val cacheFile = File(context.filesDir, "pdf_text_cache/${pdfId}_cache.json")
            if (!cacheFile.exists()) return null
            val jsonStr = cacheFile.readText()
            if (jsonStr.isBlank()) return null
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<Int, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val pageIdx = key.toIntOrNull() ?: continue
                map[pageIdx] = json.getString(key)
            }
            return map
        } catch (e: Exception) {
            Log.e(TAG, "Error loading page text cache for pdf $pdfId", e)
            return null
        }
    }

    /**
     * Searches a keyword/expression across pages and returns search occurrences with context.
     * Case-insensitive, accent-insensitive, and whitespace-tolerant search.
     */
    fun searchInPages(
        pagesText: Map<Int, String>,
        query: String,
        pdfId: Long = -1
    ): List<PdfSearchResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<PdfSearchResult>()
        val normalizedQuery = normalizeText(query.trim())
        if (normalizedQuery.isEmpty()) return emptyList()

        for ((pageIndex, text) in pagesText) {
            if (text.isBlank()) continue
            val normalizedPageText = normalizeText(text)
            var startIndex = 0
            var matchCountOnPage = 0

            while (startIndex < normalizedPageText.length) {
                val foundIndex = normalizedPageText.indexOf(normalizedQuery, startIndex)
                if (foundIndex < 0) break

                matchCountOnPage++

                val origLength = text.length
                val ratio = if (normalizedPageText.isNotEmpty()) origLength.toDouble() / normalizedPageText.length.toDouble() else 1.0
                val approxStart = (foundIndex * ratio).toInt().coerceIn(0, origLength)
                val approxMatchLen = (query.trim().length * ratio).toInt().coerceAtLeast(1)

                val contextStart = (approxStart - 40).coerceAtLeast(0)
                val contextEnd = (approxStart + approxMatchLen + 40).coerceAtMost(origLength)

                val matchedSegment = text.substring(
                    approxStart,
                    (approxStart + approxMatchLen).coerceAtMost(origLength)
                ).ifBlank { query }

                val snippetText = text.substring(contextStart, contextEnd)
                    .replace("\\s+".toRegex(), " ")
                    .trim()

                val snippet = (if (contextStart > 0) "..." else "") + snippetText + (if (contextEnd < origLength) "..." else "")

                results.add(
                    PdfSearchResult(
                        pageIndex = pageIndex,
                        matchText = matchedSegment,
                        previewContext = snippet
                    )
                )

                startIndex = foundIndex + normalizedQuery.length.coerceAtLeast(1)
            }

            // Log DEBUG per ÉTAPE 2
            Log.d("PDF_SEARCH", "pdfId=$pdfId, page=$pageIndex, extractedTextLength=${text.length}, query=$query, normalizedQuery=$normalizedQuery, matchCount=$matchCountOnPage")
        }

        return results
    }
}

package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.InflaterInputStream

data class PdfSearchResult(
    val pageIndex: Int, // 0-based
    val matchText: String,
    val previewContext: String
)

object PdfHelper {

    private const val TAG = "PdfHelper"

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
                        // Fill white background for pages with transparent backgrounds
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
     * Extracts text per page from PDF bytes using an offline parser.
     * Decodes uncompressed and Flate-compressed text streams.
     */
    suspend fun extractTextByPages(file: File): Map<Int, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Int, String>()
        try {
            val bytes = file.readBytes()
            val textBlocks = extractStreamsText(bytes)
            
            // Map text blocks to pages or distribute evenly if page markers are present
            val pageCount = getPageCount(file)
            if (textBlocks.isNotEmpty()) {
                val chunkSize = (textBlocks.size + pageCount - 1) / pageCount
                for (p in 0 until pageCount) {
                    val start = (p * chunkSize).coerceAtMost(textBlocks.size)
                    val end = ((p + 1) * chunkSize).coerceAtMost(textBlocks.size)
                    val pageText = if (start < end) {
                        textBlocks.subList(start, end).joinToString(" ")
                    } else ""
                    result[p] = pageText
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF", e)
        }
        result
    }

    /**
     * Searches a keyword/expression across pages and returns search occurrences with context.
     */
    fun searchInPages(
        pagesText: Map<Int, String>,
        query: String
    ): List<PdfSearchResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<PdfSearchResult>()
        val lowerQuery = query.trim().lowercase()

        for ((pageIndex, text) in pagesText) {
            val lowerText = text.lowercase()
            var startIndex = 0
            while (true) {
                val foundIndex = lowerText.indexOf(lowerQuery, startIndex)
                if (foundIndex < 0) break

                val contextStart = (foundIndex - 30).coerceAtLeast(0)
                val contextEnd = (foundIndex + query.length + 30).coerceAtMost(text.length)
                val snippet = "..." + text.substring(contextStart, contextEnd).replace("\n", " ") + "..."

                results.add(
                    PdfSearchResult(
                        pageIndex = pageIndex,
                        matchText = text.substring(foundIndex, (foundIndex + query.length).coerceAtMost(text.length)),
                        previewContext = snippet
                    )
                )

                startIndex = foundIndex + query.length.coerceAtLeast(1)
            }
        }
        return results
    }

    /**
     * Scans raw PDF bytes for stream ... endstream blocks and extracts text from Tj / TJ operators.
     */
    private fun extractStreamsText(pdfBytes: ByteArray): List<String> {
        val extractedLines = mutableListOf<String>()
        val streamTag = "stream".toByteArray(Charsets.US_ASCII)
        val endStreamTag = "endstream".toByteArray(Charsets.US_ASCII)

        var idx = 0
        while (idx < pdfBytes.size) {
            val streamStart = indexOfBytes(pdfBytes, streamTag, idx)
            if (streamStart == -1) break

            // Move past "stream\r\n" or "stream\n"
            var dataStart = streamStart + streamTag.size
            if (dataStart < pdfBytes.size && pdfBytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < pdfBytes.size && pdfBytes[dataStart] == '\n'.code.toByte()) dataStart++

            val streamEnd = indexOfBytes(pdfBytes, endStreamTag, dataStart)
            if (streamEnd == -1) break

            val streamLength = streamEnd - dataStart
            if (streamLength > 0) {
                val streamData = ByteArray(streamLength)
                System.arraycopy(pdfBytes, dataStart, streamData, 0, streamLength)

                // Try decompressed (FlateDecode)
                val decompressed = tryDecompress(streamData) ?: streamData
                val textTokens = parsePdfTextOperators(decompressed)
                if (textTokens.isNotBlank()) {
                    extractedLines.add(textTokens)
                }
            }

            idx = streamEnd + endStreamTag.size
        }

        return extractedLines
    }

    private fun tryDecompress(data: ByteArray): ByteArray? {
        return try {
            val bis = ByteArrayInputStream(data)
            val iis = InflaterInputStream(bis)
            val buffer = ByteArray(1024)
            val bos = ByteArrayOutputStream()
            var len: Int
            while (iis.read(buffer).also { len = it } > 0) {
                bos.write(buffer, 0, len)
            }
            bos.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePdfTextOperators(streamBytes: ByteArray): String {
        val content = String(streamBytes, Charsets.ISO_8859_1)
        val sb = StringBuilder()

        // Extract (text) Tj
        val tjRegex = """\((.*?)\)\s*Tj""".toRegex()
        for (match in tjRegex.findAll(content)) {
            val text = cleanPdfString(match.groupValues[1])
            if (text.isNotBlank()) {
                sb.append(text).append(" ")
            }
        }

        // Extract [(text)-10(text)] TJ
        val tjArrayRegex = """\[(.*?)\]\s*TJ""".toRegex(RegexOption.DOT_MATCHES_ALL)
        for (match in tjArrayRegex.findAll(content)) {
            val arrayContent = match.groupValues[1]
            val innerTexts = """\((.*?)\)""".toRegex().findAll(arrayContent)
            for (inner in innerTexts) {
                val clean = cleanPdfString(inner.groupValues[1])
                if (clean.isNotBlank()) {
                    sb.append(clean)
                }
            }
            sb.append(" ")
        }

        return sb.toString().trim()
    }

    private fun cleanPdfString(raw: String): String {
        return raw.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    private fun indexOfBytes(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        outer@ for (i in fromIndex..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }
}

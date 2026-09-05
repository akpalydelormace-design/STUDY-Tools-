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
import java.text.Normalizer
import java.util.zip.InflaterInputStream

data class PdfSearchResult(
    val pageIndex: Int, // 0-based
    val matchText: String,
    val previewContext: String
)

object PdfHelper {

    private const val TAG = "PdfHelper"

    /**
     * Normalizes text for case-insensitive and accent-insensitive matching.
     * Removes diacritics and converts to lowercase.
     */
    fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        val unaccented = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return unaccented.lowercase()
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
     * Extracts text per page from PDF bytes using an offline parser.
     * Maps streams specifically to PDF page objects when possible.
     */
    suspend fun extractTextByPages(file: File): Map<Int, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Int, String>()
        try {
            val bytes = file.readBytes()
            val totalPages = getPageCount(file)

            val pageTexts = extractTextByPdfObjects(bytes, totalPages)
            if (pageTexts.isNotEmpty() && pageTexts.values.any { it.isNotBlank() }) {
                for (p in 0 until totalPages) {
                    result[p] = pageTexts[p] ?: ""
                }
                return@withContext result
            }

            // Fallback: extract streams sequentially
            val textBlocks = extractStreamsText(bytes)
            if (textBlocks.isNotEmpty()) {
                val chunkSize = (textBlocks.size + totalPages - 1) / totalPages
                for (p in 0 until totalPages) {
                    val start = (p * chunkSize).coerceAtMost(textBlocks.size)
                    val end = ((p + 1) * chunkSize).coerceAtMost(textBlocks.size)
                    val pageText = if (start < end) {
                        textBlocks.subList(start, end).joinToString(" ")
                    } else ""
                    result[p] = pageText
                }
            } else {
                for (p in 0 until totalPages) {
                    result[p] = ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF", e)
        }
        result
    }

    /**
     * Searches a keyword/expression across pages and returns search occurrences with context.
     * Case-insensitive and accent-insensitive search.
     */
    fun searchInPages(
        pagesText: Map<Int, String>,
        query: String
    ): List<PdfSearchResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<PdfSearchResult>()
        val normalizedQuery = normalizeText(query.trim())
        if (normalizedQuery.isEmpty()) return emptyList()

        for ((pageIndex, text) in pagesText) {
            if (text.isBlank()) continue
            val normalizedPageText = normalizeText(text)
            var startIndex = 0

            while (startIndex < normalizedPageText.length) {
                val foundIndex = normalizedPageText.indexOf(normalizedQuery, startIndex)
                if (foundIndex < 0) break

                // Snippet calculation in original text
                val origLength = text.length
                val ratio = if (normalizedPageText.length > 0) origLength.toDouble() / normalizedPageText.length.toDouble() else 1.0
                val approxStart = (foundIndex * ratio).toInt().coerceIn(0, origLength)
                val approxMatchLen = (query.trim().length * ratio).toInt().coerceAtLeast(1)

                val contextStart = (approxStart - 35).coerceAtLeast(0)
                val contextEnd = (approxStart + approxMatchLen + 35).coerceAtMost(origLength)

                val matchedSegment = text.substring(
                    approxStart,
                    (approxStart + approxMatchLen).coerceAtMost(origLength)
                ).ifBlank { query }

                val snippetText = text.substring(contextStart, contextEnd)
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .replace("\\s+".toRegex(), " ")
                    .trim()

                val snippet = if (contextStart > 0) "...$snippetText..." else "$snippetText..."

                results.add(
                    PdfSearchResult(
                        pageIndex = pageIndex,
                        matchText = matchedSegment,
                        previewContext = snippet
                    )
                )

                startIndex = foundIndex + normalizedQuery.length.coerceAtLeast(1)
            }
        }
        return results
    }

    /**
     * Parses PDF indirect objects and extracts stream contents mapped directly to page objects.
     */
    private fun extractTextByPdfObjects(pdfBytes: ByteArray, totalPages: Int): Map<Int, String> {
        val pageTexts = mutableMapOf<Int, String>()
        try {
            val contentStr = String(pdfBytes, Charsets.ISO_8859_1)

            // Find stream objects by ID
            val objRegex = """(\d+)\s+0\s+obj\s*(.*?)endobj""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val streamMap = mutableMapOf<Int, ByteArray>()
            val pageContentsMap = mutableListOf<List<Int>>()

            for (match in objRegex.findAll(contentStr)) {
                val objId = match.groupValues[1].toIntOrNull() ?: continue
                val objBody = match.groupValues[2]

                // Check if page object
                if (objBody.contains("/Type") && objBody.contains("/Page") && !objBody.contains("/Pages")) {
                    val contentsMatch = """/Contents\s*(\d+)\s+0\s+R""".toRegex().find(objBody)
                    if (contentsMatch != null) {
                        val streamId = contentsMatch.groupValues[1].toIntOrNull()
                        if (streamId != null) {
                            pageContentsMap.add(listOf(streamId))
                        }
                    } else {
                        val arrayContentsMatch = """/Contents\s*\[\s*(.*?)\s*\]""".toRegex(RegexOption.DOT_MATCHES_ALL).find(objBody)
                        if (arrayContentsMatch != null) {
                            val ids = """(\d+)\s+0\s+R""".toRegex().findAll(arrayContentsMatch.groupValues[1])
                                .mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
                            if (ids.isNotEmpty()) {
                                pageContentsMap.add(ids)
                            }
                        }
                    }
                }

                // Check if stream object
                if (objBody.contains("stream")) {
                    val streamStart = match.range.first + match.value.indexOf("stream") + 6
                    var dataStart = streamStart
                    if (dataStart < pdfBytes.size && pdfBytes[dataStart] == '\r'.code.toByte()) dataStart++
                    if (dataStart < pdfBytes.size && pdfBytes[dataStart] == '\n'.code.toByte()) dataStart++

                    val endStreamIdx = contentStr.indexOf("endstream", dataStart)
                    if (endStreamIdx > dataStart) {
                        val streamData = ByteArray(endStreamIdx - dataStart)
                        System.arraycopy(pdfBytes, dataStart, streamData, 0, streamData.size)
                        streamMap[objId] = streamData
                    }
                }
            }

            if (pageContentsMap.isNotEmpty()) {
                for ((idx, streamIds) in pageContentsMap.withIndex()) {
                    if (idx >= totalPages) break
                    val sb = StringBuilder()
                    for (streamId in streamIds) {
                        val rawData = streamMap[streamId] ?: continue
                        val decompressed = tryDecompress(rawData) ?: rawData
                        val text = parsePdfTextOperators(decompressed)
                        if (text.isNotBlank()) {
                            sb.append(text).append(" ")
                        }
                    }
                    pageTexts[idx] = sb.toString().trim()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractTextByPdfObjects", e)
        }
        return pageTexts
    }

    /**
     * Scans raw PDF bytes for stream ... endstream blocks.
     */
    private fun extractStreamsText(pdfBytes: ByteArray): List<String> {
        val extractedLines = mutableListOf<String>()
        val streamTag = "stream".toByteArray(Charsets.US_ASCII)
        val endStreamTag = "endstream".toByteArray(Charsets.US_ASCII)

        var idx = 0
        while (idx < pdfBytes.size) {
            val streamStart = indexOfBytes(pdfBytes, streamTag, idx)
            if (streamStart == -1) break

            var dataStart = streamStart + streamTag.size
            if (dataStart < pdfBytes.size && pdfBytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < pdfBytes.size && pdfBytes[dataStart] == '\n'.code.toByte()) dataStart++

            val streamEnd = indexOfBytes(pdfBytes, endStreamTag, dataStart)
            if (streamEnd == -1) break

            val streamLength = streamEnd - dataStart
            if (streamLength > 0) {
                val streamData = ByteArray(streamLength)
                System.arraycopy(pdfBytes, dataStart, streamData, 0, streamLength)

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

    fun parsePdfTextOperators(streamBytes: ByteArray): String {
        val content = String(streamBytes, Charsets.ISO_8859_1)
        val sb = StringBuilder()

        // Extract literal strings: (text) Tj
        val tjRegex = """\((.*?)\)\s*Tj""".toRegex()
        for (match in tjRegex.findAll(content)) {
            val text = cleanPdfString(match.groupValues[1])
            if (text.isNotBlank()) {
                sb.append(text).append(" ")
            }
        }

        // Extract hex strings: <48656c6c6f> Tj
        val hexTjRegex = """<([0-9a-fA-F\s]+)>\s*Tj""".toRegex()
        for (match in hexTjRegex.findAll(content)) {
            val text = decodeHexPdfString(match.groupValues[1])
            if (text.isNotBlank()) {
                sb.append(text).append(" ")
            }
        }

        // Extract array text: [(text) -10 <hex>] TJ
        val tjArrayRegex = """\[(.*?)\]\s*TJ""".toRegex(RegexOption.DOT_MATCHES_ALL)
        for (match in tjArrayRegex.findAll(content)) {
            val arrayContent = match.groupValues[1]
            // literal matches
            val innerTexts = """\((.*?)\)""".toRegex().findAll(arrayContent)
            for (inner in innerTexts) {
                val clean = cleanPdfString(inner.groupValues[1])
                if (clean.isNotBlank()) {
                    sb.append(clean)
                }
            }
            // hex matches
            val innerHex = """<([0-9a-fA-F\s]+)>""".toRegex().findAll(arrayContent)
            for (hex in innerHex) {
                val clean = decodeHexPdfString(hex.groupValues[1])
                if (clean.isNotBlank()) {
                    sb.append(clean)
                }
            }
            sb.append(" ")
        }

        return sb.toString().trim()
    }

    private fun decodeHexPdfString(hex: String): String {
        val cleanHex = hex.replace("\\s".toRegex(), "")
        if (cleanHex.isEmpty() || cleanHex.length % 2 != 0) return ""
        return try {
            val bytes = ByteArray(cleanHex.length / 2)
            for (i in bytes.indices) {
                bytes[i] = cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            } else {
                String(bytes, Charsets.ISO_8859_1)
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun cleanPdfString(raw: String): String {
        return raw.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", " ")
            .replace("\\r", " ")
            .replace("\\t", " ")
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

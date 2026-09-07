package com.example.data.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PdfHelper.init(context)
    }

    @Test
    fun `normalizeText removes accents, ligatures, and converts to lowercase`() {
        val normalized = PdfHelper.normalizeText("Éléphant & Maître D'école à l'Université œil")
        assertEquals("elephant & maitre d'ecole a l'universite oeil", normalized)
    }

    @Test
    fun `searchInPages finds exact, case-insensitive, and accent-insensitive occurrences including Sartre`() {
        val pagesCache = mapOf(
            0 to "Philo Tle_L2_Le commentaire de texte selon Jean-Paul Sartre sur la philosophie.",
            1 to "SARTRE affirme que l'existence précède l'essence dans L'Être et le Néant.",
            2 to "Exercices pratiques et dissertations."
        )

        val results = PdfHelper.searchInPages(pagesCache, "Sartre")
        assertEquals(2, results.size)
        assertEquals(0, results[0].pageIndex)
        assertEquals(1, results[1].pageIndex)
        assertTrue(results[0].previewContext.contains("Sartre"))
        assertTrue(results[1].previewContext.contains("SARTRE"))

        val lowercaseResults = PdfHelper.searchInPages(pagesCache, "sartre")
        assertEquals(2, lowercaseResults.size)

        val accentedResults = PdfHelper.searchInPages(pagesCache, "philosophie")
        assertEquals(1, accentedResults.size)
        assertEquals(0, accentedResults[0].pageIndex)
    }

    @Test
    fun `searchInPages returns empty list when query is absent`() {
        val pagesCache = mapOf(
            0 to "Un texte quelconque sans le mot recherché.",
            1 to "Un autre paragraphe de test."
        )

        val results = PdfHelper.searchInPages(pagesCache, "physique")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `searchInPages handles empty or blank queries gracefully`() {
        val pagesCache = mapOf(0 to "Contenu de test")
        assertTrue(PdfHelper.searchInPages(pagesCache, "").isEmpty())
        assertTrue(PdfHelper.searchInPages(pagesCache, "   ").isEmpty())
    }

    @Test
    fun `isScannedPdf correctly identifies empty or near-empty text maps`() {
        assertTrue(PdfHelper.isScannedPdf(emptyMap()))
        assertTrue(PdfHelper.isScannedPdf(mapOf(0 to " ", 1 to "")))
        assertFalse(PdfHelper.isScannedPdf(mapOf(0 to "Ce document contient un paragraphe de texte valide.")))
    }

    @Test
    fun `extractTextByPages extracts text from real PDF created with PDFBox`() {
        val tempPdf = File.createTempFile("test_sartre", ".pdf")
        tempPdf.deleteOnExit()

        PDDocument().use { doc ->
            val page = PDPage()
            doc.addPage(page)
            PDPageContentStream(doc, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 12f)
                stream.newLineAtOffset(50f, 700f)
                stream.showText("Philo Tle L2 Le commentaire selon Jean-Paul Sartre sur la philosophie.")
                stream.endText()
            }
            doc.save(tempPdf)
        }

        val extractedPages = kotlinx.coroutines.runBlocking {
            PdfHelper.extractTextByPages(tempPdf, context)
        }

        assertEquals(1, extractedPages.size)
        val page0Text = extractedPages[0] ?: ""
        assertTrue(page0Text.contains("Sartre"))

        val searchResults = PdfHelper.searchInPages(extractedPages, "sartre")
        assertEquals(1, searchResults.size)
        assertEquals(0, searchResults[0].pageIndex)
    }
}

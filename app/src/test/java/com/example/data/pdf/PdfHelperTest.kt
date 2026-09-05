package com.example.data.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfHelperTest {

    @Test
    fun `normalizeText removes accents and converts to lowercase`() {
        val normalized = PdfHelper.normalizeText("Éléphant & Maître D'école à l'Université")
        assertEquals("elephant & maitre d'ecole a l'universite", normalized)
    }

    @Test
    fun `searchInPages finds exact, case-insensitive, and accent-insensitive occurrences`() {
        val pagesCache = mapOf(
            0 to "Le premier chapitre traite des équations du second degré.",
            1 to "Résolution d'une ÉQUATION par factorisation.",
            2 to "Exercices pratiques."
        )

        val results = PdfHelper.searchInPages(pagesCache, "equation")
        assertEquals(2, results.size)
        assertEquals(0, results[0].pageIndex)
        assertEquals(1, results[1].pageIndex)
        assertTrue(results[0].previewContext.contains("équations"))
        assertTrue(results[1].previewContext.contains("ÉQUATION"))
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
    fun `parsePdfTextOperators decodes literal and hex Tj operators`() {
        val pdfStreamContent = """
            BT
            /F1 12 Tf
            (Bonjour tout le monde) Tj
            <48656c6c6f20576f726c64> Tj
            [(A) -10 (B) -20 (C)] TJ
            ET
        """.trimIndent()

        val extracted = PdfHelper.parsePdfTextOperators(pdfStreamContent.toByteArray(Charsets.ISO_8859_1))
        assertTrue(extracted.contains("Bonjour tout le monde"))
        assertTrue(extracted.contains("Hello World"))
        assertTrue(extracted.contains("ABC"))
    }
}

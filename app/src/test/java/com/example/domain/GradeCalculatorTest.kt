package com.example.domain

import com.example.data.model.GradeEntity
import com.example.data.model.SubjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeCalculatorTest {
    private val year = "2025-2026"
    private val maths = SubjectEntity(id = 1, name = "Maths", coefficient = 4f)
    private val french = SubjectEntity(id = 2, name = "Français", coefficient = 2f)

    private fun grade(id: Long, subject: SubjectEntity, trimestre: Int, score: Float, outOf: Float = 20f, coef: Float = 1f, schoolYear: String = year) =
        GradeEntity(id, subject.id, subject.name, trimestre, score, outOf, coef, "Devoir", 1L, "", schoolYear)

    @Test fun `converts 15 over 30 to 10 over 20`() = assertEquals(10f, GradeCalculator.normalizeToTwenty(15f, 30f))
    @Test fun `converts 45 over 50 to 18 over 20`() = assertEquals(18f, GradeCalculator.normalizeToTwenty(45f, 50f))

    @Test fun `weights evaluations within a subject independently of subject coefficient`() {
        val average = GradeCalculator.calculateSubjectAverage(listOf(grade(1, maths, 1, 12f), grade(2, maths, 1, 16f, coef = 2f)))
        assertEquals(14.67f, average)
    }

    @Test fun `weights general average by subject coefficients`() {
        val report = GradeCalculator.buildTrimestreReport(1, listOf(maths, french), listOf(grade(1, maths, 1, 10f), grade(2, french, 1, 16f)), year)
        assertEquals(12f, report.generalAverage)
        assertEquals(6f, report.totalCoefficients)
    }

    @Test fun `recalculates general average instantly when subject coefficient is modified`() {
        val updatedMaths = maths.copy(coefficient = 1f) // Maths coef drops from 4 to 1
        val report = GradeCalculator.buildTrimestreReport(1, listOf(updatedMaths, french), listOf(grade(1, maths, 1, 10f), grade(2, french, 1, 16f)), year)
        // Math = 10 (coef 1), French = 16 (coef 2) -> (10*1 + 16*2) / 3 = 42/3 = 14.00
        assertEquals(14f, report.generalAverage)
        assertEquals(3f, report.totalCoefficients)
    }

    @Test fun `validates subject coefficient strictly greater than zero`() {
        assertTrue(GradeCalculator.validateSubjectCoefficient(1f).isValid)
        assertTrue(GradeCalculator.validateSubjectCoefficient(0.5f).isValid)
        assertFalse(GradeCalculator.validateSubjectCoefficient(0f).isValid)
        assertFalse(GradeCalculator.validateSubjectCoefficient(-2f).isValid)
        assertFalse(GradeCalculator.validateSubjectCoefficient(Float.NaN).isValid)
    }

    @Test fun `validates trimestre coefficient strictly greater than zero`() {
        assertTrue(GradeCalculator.validateTrimestreCoefficient(1f).isValid)
        assertTrue(GradeCalculator.validateTrimestreCoefficient(2.5f).isValid)
        assertFalse(GradeCalculator.validateTrimestreCoefficient(0f).isValid)
        assertFalse(GradeCalculator.validateTrimestreCoefficient(-1f).isValid)
        assertFalse(GradeCalculator.validateTrimestreCoefficient(Float.NaN).isValid)
    }

    @Test fun `calculates annual average weighted by configurable trimestre coefficients`() {
        val r1 = GradeCalculator.buildTrimestreReport(1, listOf(maths), listOf(grade(1, maths, 1, 10f)), year) // T1 avg = 10
        val r2 = GradeCalculator.buildTrimestreReport(2, listOf(maths), listOf(grade(2, maths, 2, 16f)), year) // T2 avg = 16
        val r3 = GradeCalculator.buildTrimestreReport(3, listOf(maths), listOf(grade(3, maths, 3, 18f)), year) // T3 avg = 18

        val reports = mapOf(1 to r1, 2 to r2, 3 to r3)
        val defaultCoefs = mapOf(1 to 1f, 2 to 1f, 3 to 1f)
        val customCoefs = mapOf(1 to 1f, 2 to 2f, 3 to 3f)

        // Default equal weights: (10 + 16 + 18) / 3 = 14.67
        assertEquals(14.67f, GradeCalculator.calculateAnnualAverage(reports, defaultCoefs))

        // Custom weights: (10*1 + 16*2 + 18*3) / (1+2+3) = (10 + 32 + 54) / 6 = 96 / 6 = 16.00
        assertEquals(16.00f, GradeCalculator.calculateAnnualAverage(reports, customCoefs))
    }

    @Test fun `trimestres and school years stay independent`() {
        val grades = listOf(grade(1, maths, 1, 10f), grade(2, maths, 2, 18f), grade(3, maths, 3, 4f), grade(4, maths, 1, 20f, schoolYear = "2026-2027"))
        assertEquals(10f, GradeCalculator.buildTrimestreReport(1, listOf(maths), grades, year).generalAverage)
        assertEquals(18f, GradeCalculator.buildTrimestreReport(2, listOf(maths), grades, year).generalAverage)
        assertEquals(4f, GradeCalculator.buildTrimestreReport(3, listOf(maths), grades, year).generalAverage)
        assertEquals(20f, GradeCalculator.buildTrimestreReport(1, listOf(maths), grades, "2026-2027").generalAverage)
    }

    @Test fun `add modify and delete are reflected by immutable grade lists`() {
        val added = listOf(grade(1, maths, 1, 12f))
        assertEquals(12f, GradeCalculator.calculateSubjectAverage(added))
        val modified = added.map { it.copy(score = 18f) }
        assertEquals(18f, GradeCalculator.calculateSubjectAverage(modified))
        assertNull(GradeCalculator.calculateSubjectAverage(modified.filterNot { it.id == 1L }))
    }

    @Test fun `rejects invalid grade values and never invents a coefficient`() {
        assertFalse(GradeCalculator.validateGrade(-1f, 20f, 1f, 1, year).isValid)
        assertFalse(GradeCalculator.validateGrade(1f, 0f, 1f, 1, year).isValid)
        assertFalse(GradeCalculator.validateGrade(1f, 20f, 0f, 1, year).isValid)
        assertFalse(GradeCalculator.validateGrade(1f, 20f, 1f, 4, year).isValid)
        assertFalse(GradeCalculator.validateGrade(21f, 20f, 1f, 1, year).isValid)
        assertTrue(GradeCalculator.validateGrade(1f, 20f, 2.5f, 1, year).isValid)
    }

    @Test fun `subject without valid grades has no artificial average`() {
        assertNull(GradeCalculator.calculateSubjectAverage(emptyList()))
        assertNull(GradeCalculator.calculateGeneralAverage(listOf(SubjectGradeSummary(1, "Maths", 4f, "#000", null))))
    }

    @Test fun `calculates progression only when both trimestres exist`() {
        assertEquals(2.5f, GradeCalculator.calculateProgression(11f, 13.5f))
        assertNull(GradeCalculator.calculateProgression(null, 13.5f))
    }

    @Test fun `formatCoefficient correctly formats integers and decimals`() {
        assertEquals("1", GradeCalculator.formatCoefficient(1f))
        assertEquals("2.5", GradeCalculator.formatCoefficient(2.5f))
        assertEquals("3", GradeCalculator.formatCoefficient(3.0f))
    }
}

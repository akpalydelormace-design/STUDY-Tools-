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
}

package com.example.domain

import com.example.data.model.GradeEntity
import com.example.data.model.SubjectEntity
import java.util.Locale

data class SubjectGradeSummary(
    val subjectId: Long,
    val subjectName: String,
    val subjectCoefficient: Float,
    val subjectColor: String,
    val averageScore: Float?, // Note sur 20, null if no grades
    val grades: List<GradeEntity> = emptyList()
)

data class TrimestreReport(
    val trimestre: Int,
    val subjects: List<SubjectGradeSummary>,
    val generalAverage: Float?, // Note sur 20, null if no grades
    val totalCoefficients: Float,
    val bestSubject: SubjectGradeSummary?,
    val worstSubject: SubjectGradeSummary?
)

object GradeCalculator {

    /**
     * Calculates the weighted average for a single subject:
     * M = sum(score_i * coef_i) / sum(coef_i)
     * Scores are normalized to /20.
     */
    fun calculateSubjectAverage(grades: List<GradeEntity>): Float? {
        if (grades.isEmpty()) return null
        var totalWeightedScore = 0.0
        var totalCoefficient = 0.0

        for (grade in grades) {
            val coef = if (grade.coefficient > 0) grade.coefficient else 1.0f
            // Normalize score to /20 if outOf != 20
            val normalizedScore = if (grade.outOf > 0) (grade.score / grade.outOf) * 20.0f else grade.score
            totalWeightedScore += normalizedScore * coef
            totalCoefficient += coef
        }

        return if (totalCoefficient > 0) {
            val avg = (totalWeightedScore / totalCoefficient).toFloat()
            roundToTwoDecimals(avg)
        } else null
    }

    /**
     * Calculates the general average (Moyenne Générale) across subjects:
     * MG = sum(subject_average * subject_coefficient) / sum(subject_coefficient)
     * Only subjects with at least one grade are included in the calculation.
     */
    fun calculateGeneralAverage(
        subjectSummaries: List<SubjectGradeSummary>
    ): Float? {
        val gradedSubjects = subjectSummaries.filter { it.averageScore != null }
        if (gradedSubjects.isEmpty()) return null

        var weightedSum = 0.0
        var totalCoef = 0.0

        for (summary in gradedSubjects) {
            val avg = summary.averageScore ?: continue
            val coef = if (summary.subjectCoefficient > 0) summary.subjectCoefficient else 1.0f
            weightedSum += avg * coef
            totalCoef += coef
        }

        return if (totalCoef > 0) {
            val generalAvg = (weightedSum / totalCoef).toFloat()
            roundToTwoDecimals(generalAvg)
        } else null
    }

    /**
     * Builds the full TrimestreReport for a given trimestre.
     */
    fun buildTrimestreReport(
        trimestre: Int,
        allSubjects: List<SubjectEntity>,
        allGrades: List<GradeEntity>
    ): TrimestreReport {
        val trimestreGrades = allGrades.filter { it.trimestre == trimestre }
        val gradesBySubject = trimestreGrades.groupBy { it.subjectId }

        val subjectSummaries = allSubjects.map { subject ->
            val gradesForSub = gradesBySubject[subject.id] ?: emptyList()
            val avg = calculateSubjectAverage(gradesForSub)
            SubjectGradeSummary(
                subjectId = subject.id,
                subjectName = subject.name,
                subjectCoefficient = subject.coefficient,
                subjectColor = subject.colorHex,
                averageScore = avg,
                grades = gradesForSub
            )
        }

        val gradedSummaries = subjectSummaries.filter { it.averageScore != null }
        val generalAvg = calculateGeneralAverage(subjectSummaries)
        val totalActiveCoefs = gradedSummaries.map { it.subjectCoefficient }.sum()

        val bestSubject = gradedSummaries.maxByOrNull { it.averageScore ?: 0f }
        val worstSubject = if (gradedSummaries.size > 1) gradedSummaries.minByOrNull { it.averageScore ?: 20f } else null

        return TrimestreReport(
            trimestre = trimestre,
            subjects = subjectSummaries,
            generalAverage = generalAvg,
            totalCoefficients = totalActiveCoefs,
            bestSubject = bestSubject,
            worstSubject = worstSubject
        )
    }

    /**
     * Calculates the progression between two trimestres (e.g., T2 - T1).
     */
    fun calculateProgression(prevAverage: Float?, currentAverage: Float?): Float? {
        if (prevAverage == null || currentAverage == null) return null
        return roundToTwoDecimals(currentAverage - prevAverage)
    }

    fun roundToTwoDecimals(value: Float): Float {
        return kotlin.math.round(value * 100) / 100f
    }

    fun formatScore(score: Float?): String {
        return if (score != null) {
            String.format(Locale.FRANCE, "%.2f", score)
        } else {
            "--"
        }
    }
}

package com.example.domain

import com.example.data.model.GradeEntity
import com.example.data.model.SubjectEntity
import java.util.Calendar
import java.util.Locale

data class GradeValidationResult(val isValid: Boolean, val message: String? = null)

data class SubjectGradeSummary(
    val subjectId: Long,
    val subjectName: String,
    val subjectCoefficient: Float,
    val subjectColor: String,
    val averageScore: Float?,
    val grades: List<GradeEntity> = emptyList()
)

data class TrimestreReport(
    val schoolYear: String,
    val trimestre: Int,
    val subjects: List<SubjectGradeSummary>,
    val generalAverage: Float?,
    val totalCoefficients: Float,
    val bestSubject: SubjectGradeSummary?,
    val worstSubject: SubjectGradeSummary?
)

/** Pure, offline business rules for the school report. */
object GradeCalculator {
    fun currentSchoolYear(now: Calendar = Calendar.getInstance()): String {
        val year = now.get(Calendar.YEAR)
        val startYear = if (now.get(Calendar.MONTH) >= Calendar.AUGUST) year else year - 1
        return "$startYear-${startYear + 1}"
    }

    fun validateGrade(
        score: Float,
        outOf: Float,
        evaluationCoefficient: Float,
        trimestre: Int,
        schoolYear: String
    ): GradeValidationResult = when {
        schoolYear.isBlank() -> GradeValidationResult(false, "L'année scolaire est obligatoire.")
        trimestre !in 1..3 -> GradeValidationResult(false, "Le trimestre doit être T1, T2 ou T3.")
        !score.isFinite() || score < 0f -> GradeValidationResult(false, "La note doit être positive ou nulle.")
        !outOf.isFinite() || outOf <= 0f -> GradeValidationResult(false, "Le barème doit être strictement supérieur à 0.")
        score > outOf -> GradeValidationResult(false, "La note ne peut pas dépasser le barème.")
        !evaluationCoefficient.isFinite() || evaluationCoefficient <= 0f -> GradeValidationResult(false, "Le coefficient de l'évaluation doit être strictement supérieur à 0.")
        else -> GradeValidationResult(true)
    }

    fun validateSubjectCoefficient(coefficient: Float): GradeValidationResult = when {
        !coefficient.isFinite() || coefficient <= 0f -> GradeValidationResult(false, "Le coefficient de la matière doit être strictement supérieur à 0.")
        else -> GradeValidationResult(true)
    }

    fun validateTrimestreCoefficient(coefficient: Float): GradeValidationResult = when {
        !coefficient.isFinite() || coefficient <= 0f -> GradeValidationResult(false, "Le coefficient du trimestre doit être strictement supérieur à 0.")
        else -> GradeValidationResult(true)
    }

    fun normalizeToTwenty(score: Float, outOf: Float): Float? {
        if (!score.isFinite() || !outOf.isFinite() || score < 0f || outOf <= 0f || score > outOf) return null
        return roundToTwoDecimals(score / outOf * 20f)
    }

    fun calculateSubjectAverage(grades: List<GradeEntity>): Float? {
        val validGrades = grades.filter {
            validateGrade(it.score, it.outOf, it.coefficient, it.trimestre, it.schoolYear).isValid
        }
        if (validGrades.isEmpty()) return null
        val totalCoefficient = validGrades.sumOf { it.coefficient.toDouble() }
        if (totalCoefficient <= 0.0) return null
        val weighted = validGrades.sumOf { grade ->
            (normalizeToTwenty(grade.score, grade.outOf) ?: return null).toDouble() * grade.coefficient
        }
        return roundToTwoDecimals((weighted / totalCoefficient).toFloat())
    }

    fun calculateGeneralAverage(subjectSummaries: List<SubjectGradeSummary>): Float? {
        val gradedSubjects = subjectSummaries.filter { it.averageScore != null && it.subjectCoefficient > 0f && it.subjectCoefficient.isFinite() }
        if (gradedSubjects.isEmpty()) return null
        val totalCoefficient = gradedSubjects.sumOf { it.subjectCoefficient.toDouble() }
        if (totalCoefficient <= 0.0) return null
        val weighted = gradedSubjects.sumOf { (it.averageScore ?: 0f).toDouble() * it.subjectCoefficient }
        return roundToTwoDecimals((weighted / totalCoefficient).toFloat())
    }

    fun calculateAnnualAverage(
        trimestreReports: Map<Int, TrimestreReport>,
        trimestreCoefficients: Map<Int, Float>
    ): Float? {
        val validTrimestres = trimestreReports.mapNotNull { (trimestre, report) ->
            val avg = report.generalAverage ?: return@mapNotNull null
            val coef = trimestreCoefficients[trimestre] ?: 1.0f
            if (coef > 0f && coef.isFinite()) {
                Pair(avg, coef)
            } else null
        }
        if (validTrimestres.isEmpty()) return null
        val totalCoef = validTrimestres.sumOf { it.second.toDouble() }
        if (totalCoef <= 0.0) return null
        val weightedSum = validTrimestres.sumOf { (avg, coef) -> avg.toDouble() * coef }
        return roundToTwoDecimals((weightedSum / totalCoef).toFloat())
    }

    fun buildTrimestreReport(
        trimestre: Int,
        allSubjects: List<SubjectEntity>,
        allGrades: List<GradeEntity>,
        schoolYear: String = currentSchoolYear()
    ): TrimestreReport {
        val trimestreGrades = allGrades.filter { it.trimestre == trimestre && it.schoolYear == schoolYear }
        val gradesBySubject = trimestreGrades.groupBy { it.subjectId }
        val subjects = allSubjects.map { subject ->
            val grades = gradesBySubject[subject.id].orEmpty()
            SubjectGradeSummary(subject.id, subject.name, subject.coefficient, subject.colorHex, calculateSubjectAverage(grades), grades)
        }
        val graded = subjects.filter { it.averageScore != null && it.subjectCoefficient > 0f && it.subjectCoefficient.isFinite() }
        return TrimestreReport(
            schoolYear = schoolYear,
            trimestre = trimestre,
            subjects = subjects,
            generalAverage = calculateGeneralAverage(subjects),
            totalCoefficients = graded.sumOf { it.subjectCoefficient.toDouble() }.toFloat(),
            bestSubject = graded.maxByOrNull { it.averageScore ?: Float.NEGATIVE_INFINITY },
            worstSubject = if (graded.size > 1) graded.minByOrNull { it.averageScore ?: Float.POSITIVE_INFINITY } else null
        )
    }

    fun calculateProgression(prevAverage: Float?, currentAverage: Float?): Float? =
        if (prevAverage == null || currentAverage == null) null else roundToTwoDecimals(currentAverage - prevAverage)

    fun roundToTwoDecimals(value: Float): Float = kotlin.math.round(value * 100) / 100f

    fun formatScore(score: Float?): String = score?.let { String.format(Locale.FRANCE, "%.2f", it) } ?: "--"

    fun formatCoefficient(coef: Float): String =
        if (coef % 1f == 0f) coef.toInt().toString() else String.format(Locale.US, "%.2f", coef).trimEnd('0').trimEnd('.')
}

package app.studycapture.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.studycapture.R
import app.studycapture.processing.MaterialProcessor
import app.studycapture.understanding.SubjectCatalog

/** Materials start without a title until a heading is found; never show an empty title. */
@Composable
fun materialTitle(title: String): String = title.ifBlank { stringResource(R.string.material_untitled) }

/** Subject names from the classifier are canonical English keys; show them in the UI language. */
@Composable
fun subjectLabel(key: String): String {
    val res = when (key) {
        "Mathematics" -> R.string.subject_mathematics
        "Physics" -> R.string.subject_physics
        "Chemistry" -> R.string.subject_chemistry
        "Biology" -> R.string.subject_biology
        "History" -> R.string.subject_history
        "Geography" -> R.string.subject_geography
        "Literature" -> R.string.subject_literature
        "English" -> R.string.subject_english
        "Russian" -> R.string.subject_russian
        "Computer Science" -> R.string.subject_cs
        "Social Studies" -> R.string.subject_social
        else -> null
    }
    return res?.let { stringResource(it) } ?: key
}

/** Page errors are stored as codes (MaterialProcessor.ERROR_*); show them in the UI language. */
@Composable
fun pageErrorText(code: String?): String = stringResource(
    when (code) {
        MaterialProcessor.ERROR_MEMORY -> R.string.page_error_memory
        MaterialProcessor.ERROR_UNREADABLE -> R.string.page_error_unreadable
        MaterialProcessor.ERROR_NO_CYRILLIC -> R.string.check_no_cyrillic
        else -> R.string.page_error_generic
    },
)

/** Catalog subjects, localized, for the subject picker. */
@Composable
fun catalogSubjectLabels(): List<String> = SubjectCatalog.subjects.map { subjectLabel(it) }

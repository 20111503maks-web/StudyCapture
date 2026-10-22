package app.studycapture.processing

import android.graphics.ImageDecoder
import app.studycapture.ai.StudyAssistant
import app.studycapture.data.db.AppDatabase
import app.studycapture.data.db.MaterialEntity
import app.studycapture.data.db.MaterialStatus
import app.studycapture.data.db.PageEntity
import app.studycapture.data.db.PageStage
import app.studycapture.data.files.PageFileStore
import app.studycapture.data.repo.toEntity
import app.studycapture.data.repo.toModel
import app.studycapture.data.settings.SettingsRepository
import app.studycapture.ocr.OcrRouter
import app.studycapture.understanding.LanguageDetector
import app.studycapture.understanding.LabeledText
import app.studycapture.understanding.StructureAnalyzer
import app.studycapture.understanding.TopicTitle
import kotlinx.coroutines.CancellationException
import java.io.File

/**
 * The page pipeline. Every stage is written to the database before work on it
 * starts, so the progress screen shows real state and a run that was killed
 * resumes with the first unfinished page.
 */
class MaterialProcessor(
    private val db: AppDatabase,
    private val files: PageFileStore,
    private val ocr: OcrRouter,
    private val settings: SettingsRepository,
    private val assistant: StudyAssistant,
    private val preparer: ImagePreparer = ImagePreparer(),
) {
    private val pageDao get() = db.pageDao()
    private val libraryDao get() = db.libraryDao()

    suspend fun process(materialId: String) {
        val material = libraryDao.material(materialId) ?: return
        if (material.status != MaterialStatus.PROCESSING) return
        val cyrillic = settings.current().cyrillicOcr

        for (page in pageDao.pages(materialId)) {
            if (page.stage == PageStage.READY || page.stage == PageStage.FAILED) continue
            try {
                processPage(page, cyrillic)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                pageDao.updatePage(page.copy(stage = PageStage.FAILED, error = describe(e), updatedAt = now()))
            }
        }
        organize(materialId)
    }

    private suspend fun processPage(page: PageEntity, cyrillic: Boolean) {
        val original = page.originalPath?.let(::File) ?: error("missing original")
        if (pageDao.page(page.id) == null) return
        var current = page.copy(stage = PageStage.CLEANING, error = null, updatedAt = now())
        pageDao.updatePage(current)

        val processedFile = files.processedFile(page.materialId, page.id)
        files.ensureParent(processedFile)
        val bitmap = preparer.prepare(original, processedFile)
        try {
            current = current.copy(stage = PageStage.READING, processedPath = processedFile.path, updatedAt = now())
            pageDao.updatePage(current)
            val result = ocr.recognize(bitmap, cyrillic)
            // The user may have deleted the page while it was being read: write nothing back.
            if (pageDao.page(page.id) == null) {
                files.deleteFiles(processedFile.path)
                return
            }

            current = current.copy(
                stage = PageStage.UNDERSTANDING,
                ocrEngine = result.engineId,
                language = LanguageDetector.detect(result.lines.joinToString("\n") { it.text }).code,
                error = if (result.cyrillicUnavailable) ERROR_NO_CYRILLIC else null,
                updatedAt = now(),
            )
            pageDao.updatePage(current)
            val blocks = StructureAnalyzer.analyze(result.lines)
            pageDao.replaceBlocks(page.id, blocks.map { it.toEntity(page.id) })
            pageDao.updatePage(current.copy(stage = PageStage.READY, updatedAt = now()))
        } finally {
            bitmap.recycle()
        }
    }

    /** "Organizing": suggest subject, topic and title from the recognized text. */
    suspend fun organize(materialId: String) {
        val material = libraryDao.material(materialId) ?: return
        val pages = pageDao.pages(materialId)
        if (pages.isNotEmpty() && pages.all { it.stage == PageStage.FAILED }) {
            libraryDao.updateMaterial(material.copy(status = MaterialStatus.FAILED, updatedAt = now()))
            return
        }
        val blocks = pageDao.blocksForMaterial(materialId).map { it.toModel() }
        val text = blocks.joinToString("\n") { it.text }
        val history = libraryDao.labeledHistory().mapNotNull { row ->
            row.text?.let { LabeledText(row.subject, row.topic, it) }
        }
        val suggestion = assistant.suggestSubject(text, history)
        val title = TopicTitle.suggest(blocks) ?: material.title
        libraryDao.updateMaterial(
            material.copy(
                status = MaterialStatus.NEEDS_REVIEW,
                title = if (material.titleIsDefault()) title else material.title,
                suggestedSubject = suggestion?.subject,
                suggestedTopic = suggestion?.topic,
                suggestionKeywords = suggestion?.matchedKeywords?.take(5)?.joinToString(", "),
                updatedAt = now(),
            ),
        )
    }

    private fun MaterialEntity.titleIsDefault() = title.isBlank() || title == DEFAULT_TITLE

    private fun describe(e: Throwable): String = when (e) {
        is OutOfMemoryError -> "Not enough memory to process this page"
        is ImageDecoder.DecodeException -> "This file could not be read as an image"
        is java.io.IOException -> "This file could not be read"
        else -> e.message ?: e.javaClass.simpleName
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        /** Placeholder title until the pipeline finds a heading; the UI shows a localized label. */
        const val DEFAULT_TITLE = ""
        const val ERROR_NO_CYRILLIC = "cyrillic-data-missing"
    }
}

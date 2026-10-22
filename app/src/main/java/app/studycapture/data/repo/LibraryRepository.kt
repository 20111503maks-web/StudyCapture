package app.studycapture.data.repo

import androidx.room.withTransaction
import app.studycapture.data.db.AppDatabase
import app.studycapture.data.db.MaterialStatus
import app.studycapture.data.db.SearchHitRow
import app.studycapture.data.db.SubjectEntity
import app.studycapture.data.db.TopicEntity
import app.studycapture.data.files.PageFileStore
import app.studycapture.understanding.SearchQueryParser
import app.studycapture.processing.ProcessingScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class LibraryRepository(
    private val db: AppDatabase,
    private val files: PageFileStore,
    private val scheduler: ProcessingScheduler,
) {
    private val libraryDao get() = db.libraryDao()
    private val pageDao get() = db.pageDao()

    fun observeSubjects() = libraryDao.observeSubjects()
    fun observeSubject(id: String) = libraryDao.observeSubject(id)
    fun observeTopics(subjectId: String) = libraryDao.observeTopics(subjectId)
    fun observeMaterials(subjectId: String? = null, limit: Int = 200) = libraryDao.observeMaterials(subjectId, limit)
    fun observeUnfinished() = libraryDao.observeUnfinished()
    fun observeMaterial(id: String) = libraryDao.observeMaterial(id)
    fun observeMaterialRow(id: String) = libraryDao.observeMaterialRow(id)
    fun observePages(materialId: String) = pageDao.observePages(materialId)
    fun observeBlocks(materialId: String) = pageDao.observeBlocksForMaterial(materialId)
    fun observeReadyCount() = libraryDao.observeReadyCount()
    suspend fun subjectNames(): List<String> = libraryDao.allSubjects().map { it.name }

    /**
     * Offline search. Natural questions ("где у меня было про митохондрии?") are reduced
     * to stemmed terms; all terms must match, and if nothing does, any term may.
     */
    suspend fun search(query: String): List<SearchHitRow> {
        val strict = SearchQueryParser.ftsAnd(query) ?: return emptyList()
        val hits = pageDao.search(strict)
        if (hits.isNotEmpty()) return hits
        val loose = SearchQueryParser.ftsOr(query) ?: return emptyList()
        return if (loose == strict) hits else pageDao.search(loose)
    }

    /** Files the material under subject/topic (creating them if new) and marks it saved. */
    suspend fun saveMaterial(materialId: String, subjectName: String, topicName: String, title: String) {
        val now = System.currentTimeMillis()
        // Kotlin comparison: SQLite NOCASE folds ASCII only, "биология" ≠ "Биология" there.
        val subject = libraryDao.allSubjects().firstOrNull { it.name.equals(subjectName.trim(), ignoreCase = true) } ?: SubjectEntity(
            id = UUID.randomUUID().toString(),
            name = subjectName.trim(),
            colorIndex = (libraryDao.allSubjects().size % SUBJECT_COLORS),
            createdAt = now,
            updatedAt = now,
        ).also { libraryDao.insertSubject(it) }
        val cleanTopic = topicName.trim().ifEmpty { title.trim() }
        val topic = libraryDao.topicsOf(subject.id).firstOrNull { it.name.equals(cleanTopic, ignoreCase = true) } ?: TopicEntity(
            id = UUID.randomUUID().toString(),
            subjectId = subject.id,
            name = cleanTopic,
            createdAt = now,
            updatedAt = now,
        ).also { libraryDao.insertTopic(it) }
        val material = libraryDao.material(materialId) ?: return
        libraryDao.updateMaterial(
            material.copy(topicId = topic.id, title = title.trim(), status = MaterialStatus.READY, updatedAt = now),
        )
    }

    suspend fun editBlock(pageId: String, blockIndex: Int, text: String) = pageDao.editBlock(pageId, blockIndex, text)

    suspend fun confirmBlock(pageId: String, blockIndex: Int) = pageDao.confirmBlock(pageId, blockIndex)

    /** Stops background processing first so no file is written after the rows are gone. */
    suspend fun deleteMaterial(materialId: String) {
        scheduler.cancel(materialId)
        libraryDao.deleteMaterial(materialId)
        withContext(Dispatchers.IO) { files.deleteMaterial(materialId) }
    }

    /**
     * Deletes the page, its photos, the flashcards quoted from it and the material's summary
     * (which may quote it — the user can make it again). Deletes the material if it was the last page.
     */
    suspend fun deletePage(pageId: String) {
        val page = pageDao.page(pageId) ?: return
        db.withTransaction {
            db.studyDao().deleteCardsForPage(pageId)
            db.studyDao().deleteSummary(page.materialId)
            pageDao.deletePage(pageId)
        }
        withContext(Dispatchers.IO) { files.deleteFiles(page.originalPath, page.processedPath) }
        if (pageDao.pages(page.materialId).isEmpty()) deleteMaterial(page.materialId)
    }

    suspend fun deleteSubject(subjectId: String) {
        // Collect ids first (any status): the cascade removes rows, files must be removed explicitly.
        val ids = libraryDao.materialIdsForSubject(subjectId)
        ids.forEach(scheduler::cancel)
        libraryDao.deleteSubject(subjectId)
        withContext(Dispatchers.IO) { ids.forEach(files::deleteMaterial) }
    }

    companion object {
        const val SUBJECT_COLORS = 6
    }
}

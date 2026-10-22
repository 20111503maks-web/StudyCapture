package app.studycapture.data.repo

import android.net.Uri
import androidx.room.withTransaction
import app.studycapture.data.db.AppDatabase
import app.studycapture.data.db.MaterialEntity
import app.studycapture.data.db.MaterialStatus
import app.studycapture.data.db.PageEntity
import app.studycapture.data.db.PageStage
import app.studycapture.data.db.SourceType
import app.studycapture.data.files.PageFileStore
import app.studycapture.processing.MaterialProcessor
import app.studycapture.processing.ProcessingScheduler
import app.studycapture.understanding.StructureAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class CaptureRepository(
    private val db: AppDatabase,
    private val files: PageFileStore,
    private val scheduler: ProcessingScheduler,
    private val processor: MaterialProcessor,
) {
    /**
     * Copies every picked image into private storage first (picker URIs can expire),
     * then records the material and queues background processing.
     * A file that cannot be copied becomes a FAILED page instead of aborting the batch.
     */
    suspend fun importImages(uris: List<Uri>, source: SourceType): String {
        require(uris.isNotEmpty())
        val now = System.currentTimeMillis()
        val materialId = UUID.randomUUID().toString()
        val pages = uris.mapIndexed { index, uri ->
            val pageId = UUID.randomUUID().toString()
            val target = files.originalFile(materialId, pageId)
            val error = try {
                files.copyIn(uri, target)
                if (source == SourceType.CAMERA) files.deleteScannerOutput(uri)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e // unreadable, revoked, too large, provider error: the page is marked failed
            }
            PageEntity(
                id = pageId,
                materialId = materialId,
                pageIndex = index,
                originalPath = target.path.takeIf { error == null },
                processedPath = null,
                stage = if (error == null) PageStage.CAPTURED else PageStage.FAILED,
                language = null,
                ocrEngine = null,
                error = error?.let { MaterialProcessor.ERROR_UNREADABLE },
                createdAt = now,
                updatedAt = now,
            )
        }
        db.libraryDao().insertMaterial(
            MaterialEntity(
                id = materialId,
                topicId = null,
                title = MaterialProcessor.DEFAULT_TITLE,
                sourceType = source,
                status = MaterialStatus.PROCESSING,
                createdAt = now,
                updatedAt = now,
            ),
        )
        db.pageDao().insertPages(pages)
        scheduler.enqueue(materialId)
        return materialId
    }

    /** Pasted text needs no OCR: structure it right away and go straight to review. */
    suspend fun importText(text: String): String {
        val now = System.currentTimeMillis()
        val materialId = UUID.randomUUID().toString()
        val pageId = UUID.randomUUID().toString()
        val blocks = withContext(Dispatchers.Default) { StructureAnalyzer.analyzeText(text) }
        db.withTransaction {
            db.libraryDao().insertMaterial(
                MaterialEntity(
                    id = materialId,
                    topicId = null,
                    title = MaterialProcessor.DEFAULT_TITLE,
                    sourceType = SourceType.TEXT,
                    status = MaterialStatus.PROCESSING,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            db.pageDao().insertPages(
                listOf(
                    PageEntity(
                        id = pageId, materialId = materialId, pageIndex = 0, originalPath = null,
                        processedPath = null, stage = PageStage.READY, language = null, ocrEngine = "pasted-text",
                        error = null, createdAt = now, updatedAt = now,
                    ),
                ),
            )
            db.pageDao().replaceBlocks(pageId, blocks.map { it.toEntity(pageId) })
        }
        processor.organize(materialId)
        return materialId
    }

    /** Re-queues a page that failed (e.g. after freeing memory). */
    suspend fun retryPage(pageId: String) {
        val page = db.pageDao().page(pageId) ?: return
        if (page.originalPath == null) return
        db.pageDao().updatePage(page.copy(stage = PageStage.CAPTURED, error = null, updatedAt = System.currentTimeMillis()))
        val material = db.libraryDao().material(page.materialId) ?: return
        db.libraryDao().updateMaterial(material.copy(status = MaterialStatus.PROCESSING))
        scheduler.enqueue(page.materialId)
    }
}

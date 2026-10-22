package app.studycapture.data.repo

import android.content.Context
import app.studycapture.data.db.AppDatabase
import app.studycapture.data.files.PageFileStore
import app.studycapture.data.settings.SettingsRepository
import app.studycapture.processing.ProcessingScheduler
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageInfo(val originals: Int, val bytes: Long)

/**
 * Deletion controls. v0.1 has no account and no cloud copy, so "delete everything"
 * is complete: database, page photos, scanner leftovers in cache, image cache,
 * the unpacked OCR language data and settings.
 */
class PrivacyRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val files: PageFileStore,
    private val settings: SettingsRepository,
    private val scheduler: ProcessingScheduler,
) {
    suspend fun storageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        StorageInfo(files.originalCount(), files.usageBytes())
    }

    /** Removes summaries, unapproved card suggestions and subject suggestions; keeps pages and approved cards. */
    suspend fun deleteGeneratedData() {
        db.studyDao().deleteAllSummaries()
        db.studyDao().deleteUnapprovedCards()
        db.libraryDao().clearSuggestions()
    }

    suspend fun deleteEverything() = withContext(Dispatchers.IO) {
        scheduler.cancelAll()
        db.clearAllTables()
        files.deleteAll()
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        File(context.filesDir, "tesseract").deleteRecursively()
        SingletonImageLoader.get(context).memoryCache?.clear()
        settings.clear()
    }
}

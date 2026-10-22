package app.studycapture.data.repo

import app.studycapture.data.db.AppDatabase
import app.studycapture.data.files.PageFileStore
import app.studycapture.data.settings.SettingsRepository
import app.studycapture.processing.ProcessingScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StorageInfo(val originals: Int, val bytes: Long)

/**
 * Deletion controls. v0.1 has no account and no cloud copy, so "delete everything"
 * is complete: database, page photos, language data cache and settings.
 */
class PrivacyRepository(
    private val db: AppDatabase,
    private val files: PageFileStore,
    private val settings: SettingsRepository,
    private val scheduler: ProcessingScheduler,
) {
    suspend fun storageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        StorageInfo(files.originalCount(), files.usageBytes())
    }

    /** Removes generated summaries and unapproved card suggestions; keeps pages and approved cards. */
    suspend fun deleteGeneratedData() {
        db.studyDao().deleteAllSummaries()
        db.studyDao().deleteUnapprovedCards()
    }

    suspend fun deleteEverything() = withContext(Dispatchers.IO) {
        scheduler.cancelAll()
        db.clearAllTables()
        files.deleteAll()
        settings.clear()
    }
}

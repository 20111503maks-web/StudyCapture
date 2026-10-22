package app.studycapture

import android.content.Context
import app.studycapture.ai.OnDeviceStudyAssistant
import app.studycapture.ai.StudyAssistant
import app.studycapture.data.db.AppDatabase
import app.studycapture.data.files.PageFileStore
import app.studycapture.data.repo.CaptureRepository
import app.studycapture.data.repo.LibraryRepository
import app.studycapture.data.repo.PrivacyRepository
import app.studycapture.data.repo.StudyRepository
import app.studycapture.data.settings.SettingsRepository
import app.studycapture.ocr.MlKitLatinOcr
import app.studycapture.ocr.OcrRouter
import app.studycapture.ocr.TesseractOcr
import app.studycapture.processing.MaterialProcessor
import app.studycapture.processing.ProcessingScheduler
import app.studycapture.srs.FsrsScheduler

/**
 * Manual dependency injection (docs/adr/0003). Every provider is created here,
 * so swapping an engine (e.g. a future LLM-backed StudyAssistant) is one line.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val files by lazy { PageFileStore(appContext) }
    val settings by lazy { SettingsRepository(appContext) }
    val scheduler by lazy { ProcessingScheduler(appContext) }

    val assistant: StudyAssistant by lazy { OnDeviceStudyAssistant() }
    private val ocr by lazy { OcrRouter(latin = MlKitLatinOcr(), cyrillic = TesseractOcr(appContext)) }
    private val srs by lazy { FsrsScheduler(enableFuzzing = true) }

    val processor by lazy { MaterialProcessor(database, files, ocr, settings, assistant) }
    val capture by lazy { CaptureRepository(database, files, scheduler, processor) }
    val library by lazy { LibraryRepository(database, files, scheduler) }
    val study by lazy { StudyRepository(database, assistant, srs) }
    val privacy by lazy { PrivacyRepository(appContext, database, files, settings, scheduler) }
}

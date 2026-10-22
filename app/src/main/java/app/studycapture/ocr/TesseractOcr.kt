package app.studycapture.ocr

import android.content.Context
import android.graphics.Bitmap
import app.studycapture.model.OcrLine
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Tesseract (via Tesseract4Android, Apache-2.0) for Cyrillic text, which ML Kit
 * does not support. Language data (tessdata_fast, Apache-2.0) ships in assets
 * and is copied to private storage on first use — no download needed.
 */
class TesseractOcr(
    private val context: Context,
    private val languages: List<String> = listOf("rus", "eng"),
) : OcrEngine {
    override val id = "tesseract-" + languages.joinToString("+")
    private val mutex = Mutex()

    fun isAvailable(): Boolean {
        val bundled = context.assets.list(ASSET_DIR)?.toSet().orEmpty()
        return languages.all { "$it.traineddata" in bundled }
    }

    override suspend fun recognize(bitmap: Bitmap): List<OcrLine> = withContext(Dispatchers.Default) {
        mutex.withLock {
            val dataRoot = ensureData()
            val api = TessBaseAPI()
            try {
                check(api.init(dataRoot.absolutePath, languages.joinToString("+"))) { "Tesseract init failed" }
                api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                api.setImage(bitmap)
                api.getUTF8Text() // runs recognition
                readLines(api)
            } finally {
                api.recycle()
            }
        }
    }

    private fun readLines(api: TessBaseAPI): List<OcrLine> {
        val iterator = api.getResultIterator() ?: return emptyList()
        val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
        val lines = mutableListOf<OcrLine>()
        try {
            iterator.begin()
            do {
                val text = iterator.getUTF8Text(level)?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    val confidence = (iterator.confidence(level) / 100f).coerceIn(0f, 1f)
                    lines += OcrLine(text, confidence, iterator.getBoundingRect(level).toBBox())
                }
            } while (iterator.next(level))
        } finally {
            iterator.delete()
        }
        return lines
    }

    private fun ensureData(): File {
        val root = File(context.filesDir, "tesseract")
        val tessdata = File(root, "tessdata").apply { mkdirs() }
        for (lang in languages) {
            val target = File(tessdata, "$lang.traineddata")
            if (target.length() > 0) continue
            val tmp = File(tessdata, "$lang.traineddata.tmp")
            context.assets.open("$ASSET_DIR/$lang.traineddata").use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            }
            check(tmp.renameTo(target)) { "Cannot install $lang language data" }
        }
        return root
    }

    private companion object {
        const val ASSET_DIR = "tessdata"
    }
}

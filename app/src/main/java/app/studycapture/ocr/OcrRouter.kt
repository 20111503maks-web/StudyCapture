package app.studycapture.ocr

import android.graphics.Bitmap
import app.studycapture.model.OcrLine

data class OcrResult(
    val lines: List<OcrLine>,
    val engineId: String,
    /** Set when Cyrillic recognition was wanted but its language data is missing. */
    val cyrillicUnavailable: Boolean = false,
)

/**
 * Picks the engine per page. ML Kit is the default (best Latin quality).
 * When Cyrillic recognition is enabled, Tesseract also reads the page and wins
 * if its output is mostly Cyrillic — ML Kit turns Cyrillic into Latin look-alikes.
 */
class OcrRouter(
    private val latin: OcrEngine,
    private val cyrillic: TesseractOcr,
) {
    suspend fun recognize(bitmap: Bitmap, cyrillicEnabled: Boolean): OcrResult {
        val latinLines = latin.recognize(bitmap)
        if (!cyrillicEnabled) return OcrResult(latinLines, latin.id)
        if (!cyrillic.isAvailable()) return OcrResult(latinLines, latin.id, cyrillicUnavailable = true)

        val cyrillicLines = cyrillic.recognize(bitmap)
        val ratio = cyrillicRatio(cyrillicLines.joinToString(" ") { it.text })
        return if (ratio >= CYRILLIC_SHARE) OcrResult(cyrillicLines, cyrillic.id) else OcrResult(latinLines, latin.id)
    }

    companion object {
        private const val CYRILLIC_SHARE = 0.3

        fun cyrillicRatio(text: String): Double {
            val letters = text.filter { it.isLetter() }
            if (letters.isEmpty()) return 0.0
            return letters.count { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.CYRILLIC }.toDouble() / letters.length
        }
    }
}

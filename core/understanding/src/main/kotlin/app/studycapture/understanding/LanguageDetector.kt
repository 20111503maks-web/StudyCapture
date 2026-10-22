package app.studycapture.understanding

import app.studycapture.understanding.text.Stopwords
import app.studycapture.understanding.text.TextNorm

enum class Script { LATIN, CYRILLIC, MIXED, OTHER, NONE }

/**
 * @property code ISO 639-1 code: "ru", "uk", "en", "de", "fr", "es", or "und" (undetermined).
 * @property confidence 0..1, a rough evidence measure — not a calibrated probability.
 */
data class DetectedLanguage(val code: String, val script: Script, val confidence: Double)

/**
 * Offline language guess for study material.
 *
 * Heuristics, in order:
 * 1. **Script.** Count Cyrillic vs Latin letters. ≥ 80 % of one → that script; Cyrillic+Latin
 *    < 50 % of all letters → OTHER (Greek, CJK, …; code "und"); otherwise MIXED and the dominant
 *    script decides the language family. Fewer than 3 letters → NONE / "und".
 * 2. **Language inside the script.** Stopword hits per language plus language-unique letters
 *    (uk: і ї є ґ; ru: ы э ъ ё; de: ä ö ü ß; fr: é è ê à ç œ ù; es: ñ ¿ ¡ á í ó ú) weighted ×2.
 *    With no evidence at all the default is "ru" for Cyrillic and "en" for Latin (low confidence).
 * 3. **Confidence** = script purity × margin between the best and second language × a
 *    small-sample factor (texts under ~8 words get a lower value).
 */
object LanguageDetector {
    private const val UK_LETTERS = "іїєґ"
    private const val RU_LETTERS = "ыэъё"
    private val LATIN_MARKERS = mapOf(
        "de" to "äöüß",
        "fr" to "éèêàçœùîôë",
        "es" to "ñ¿¡áíóú",
    )
    private val CYR_LANGS = listOf("ru", "uk")
    private val LAT_LANGS = listOf("en", "de", "fr", "es")

    fun detect(text: String): DetectedLanguage {
        var cyr = 0
        var lat = 0
        var other = 0
        for (c in text) {
            if (!c.isLetter()) continue
            when {
                TextNorm.isCyrillic(c) -> cyr++
                TextNorm.isLatin(c) -> lat++
                else -> other++
            }
        }
        val letters = cyr + lat + other
        if (letters < 3) return DetectedLanguage("und", Script.NONE, 0.0)
        val script = when {
            cyr >= 0.8 * letters -> Script.CYRILLIC
            lat >= 0.8 * letters -> Script.LATIN
            cyr + lat < 0.5 * letters -> Script.OTHER
            else -> Script.MIXED
        }
        if (script == Script.OTHER) return DetectedLanguage("und", Script.OTHER, 0.3)
        val cyrillicFamily = cyr >= lat
        val purity = maxOf(cyr, lat).toDouble() / letters

        val lower = TextNorm.normalize(text)
        val words = TextNorm.words(text).filter { w -> w.any { it.isLetter() } }
        val candidates = if (cyrillicFamily) CYR_LANGS else LAT_LANGS
        val scores = candidates.associateWith { lang ->
            val sw = Stopwords.BY_LANG.getValue(lang)
            var s = words.count { it in sw }.toDouble()
            s += 2.0 * when (lang) {
                "uk" -> lower.count { it in UK_LETTERS }
                // ё was folded to е by normalize(), so count it on the raw text.
                "ru" -> lower.count { it in RU_LETTERS } + text.count { it == 'ё' || it == 'Ё' }
                "en" -> 0
                else -> lower.count { it in LATIN_MARKERS.getValue(lang) }
            }
            s
        }
        val ranked = scores.entries.sortedWith(
            compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { candidates.indexOf(it.key) },
        )
        val best = ranked[0]
        val second = ranked.getOrNull(1)?.value ?: 0.0
        val margin = if (best.value <= 0.0) 0.4 else 0.5 + 0.5 * (best.value - second) / (best.value + second)
        val sample = minOf(1.0, 0.4 + words.size / 12.0)
        val confidence = (purity * margin * sample).coerceIn(0.05, 0.99)
        return DetectedLanguage(best.key, script, confidence)
    }
}

package app.studycapture.understanding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguageDetectorTest {
    @Test
    fun russianAndEnglishMaterial() {
        val ru = LanguageDetector.detect(Samples.RU_ALGEBRA)
        assertEquals("ru", ru.code)
        assertEquals(Script.CYRILLIC, ru.script)
        assertTrue(ru.confidence > 0.6)
        val en = LanguageDetector.detect(Samples.EN_BIOLOGY)
        assertEquals("en", en.code)
        assertEquals(Script.LATIN, en.script)
        assertTrue(en.confidence > 0.6)
    }

    @Test
    fun ukrainianGermanFrenchSpanish() {
        assertEquals("uk", LanguageDetector.detect("Клітина є основною одиницею життя, і вона має ядро та мембрану.").code)
        assertEquals("de", LanguageDetector.detect("Die Zelle ist die kleinste Einheit des Lebens und hat einen Kern.").code)
        assertEquals("fr", LanguageDetector.detect("La cellule est l'unité de base de la vie et elle contient un noyau.").code)
        assertEquals("es", LanguageDetector.detect("La célula es la unidad básica de la vida y tiene un núcleo.").code)
    }

    @Test
    fun mixedTextUsesTheDominantScript() {
        val d = LanguageDetector.detect("Present Simple используется для регулярных действий: I go to school every day.")
        assertEquals(Script.MIXED, d.script)
        assertEquals("ru", d.code)
        val formulaHeavy = LanguageDetector.detect("Дискриминант вычисляется по формуле D = b² − 4ac, где a, b, c — коэффициенты.")
        assertEquals("ru", formulaHeavy.code)
    }

    @Test
    fun emptyOrTinyTextIsUndetermined() {
        assertEquals(DetectedLanguage("und", Script.NONE, 0.0), LanguageDetector.detect(""))
        assertEquals("und", LanguageDetector.detect("  12 + 5 = 17 ").code)
        assertEquals("und", LanguageDetector.detect("x²").code)
        assertEquals("und", LanguageDetector.detect("光合作用是植物的过程").code)
        // A single word still gets a best guess, but with low confidence.
        val one = LanguageDetector.detect("Митоз")
        assertEquals("ru", one.code)
        assertTrue(one.confidence < 0.6)
    }
}

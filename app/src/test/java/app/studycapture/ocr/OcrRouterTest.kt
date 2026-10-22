package app.studycapture.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrRouterTest {
    @Test
    fun cyrillicRatioCountsLettersOnly() {
        assertEquals(1.0, OcrRouter.cyrillicRatio("Митоз — деление ядра. 2 + 2 = 4"), 1e-9)
        assertEquals(0.0, OcrRouter.cyrillicRatio("Mitosis is nuclear division"), 1e-9)
        assertEquals(0.0, OcrRouter.cyrillicRatio("12345 = + -"), 1e-9)
    }

    @Test
    fun mixedTextGivesProportionalRatio() {
        // 4 Cyrillic letters, 4 Latin letters.
        assertEquals(0.5, OcrRouter.cyrillicRatio("мама papa"), 1e-9)
    }
}

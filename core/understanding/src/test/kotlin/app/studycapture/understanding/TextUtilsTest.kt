package app.studycapture.understanding

import app.studycapture.understanding.text.Sentences
import app.studycapture.understanding.text.Stemmer
import app.studycapture.understanding.text.TextNorm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextUtilsTest {
    @Test
    fun russianInflectionsShareAStem() {
        val forms = listOf("митохондрия", "митохондрии", "митохондриями", "митохондрию")
        val stems = forms.map { Stemmer.stem(it) }.toSet()
        assertEquals(setOf("митохондр"), stems)
        assertEquals(Stemmer.stem("уравнение"), Stemmer.stem("уравнения"))
        assertEquals(Stemmer.stem("уравнений"), Stemmer.stem("уравнениями"))
        assertEquals("сил", Stemmer.stem("сила"))
        assertEquals("сил", Stemmer.stem("силы"))
        // Short words are never cut below the minimum stem length.
        assertEquals("кот", Stemmer.stem("кот"))
    }

    @Test
    fun englishStemsArePrefixesOfTheirForms() {
        assertEquals("photosynthes", Stemmer.stem("photosynthesis"))
        assertEquals("cell", Stemmer.stem("cells"))
        assertEquals("process", Stemmer.stem("processes"))
        assertEquals("class", Stemmer.stem("class"))
        assertEquals("nucleus", Stemmer.stem("nucleus"))
        assertEquals("divid", Stemmer.stem("dividing"))
        assertEquals("newton", Stemmer.stem("newton's"))
        assertTrue("studies".startsWith(Stemmer.stem("studies")))
    }

    @Test
    fun sentenceSplitterRespectsAbbreviationsAndInitials() {
        val s = Sentences.split(
            "Главнокомандующим был назначен М. И. Кутузов. Сражение произошло в 1812 г. у деревни Бородино. " +
                "См. рис. 3, т. е. схему. Plants need light, e.g. sunlight. Fig. 2 shows it! Done?",
        )
        assertEquals(
            listOf(
                "Главнокомандующим был назначен М. И. Кутузов.",
                "Сражение произошло в 1812 г. у деревни Бородино.",
                "См. рис. 3, т. е. схему.",
                "Plants need light, e.g. sunlight.",
                "Fig. 2 shows it!",
                "Done?",
            ),
            s,
        )
    }

    @Test
    fun normalizationFoldsCaseAndYo() {
        assertEquals("еж и елка", TextNorm.normalize("Ёж и Ёлка"))
        assertEquals(listOf("newton's", "law", "f", "ma"), TextNorm.words("Newton's law: F = ma"))
        assertEquals("Квадратные…", TextNorm.cut("Квадратные уравнения", 12))
    }
}

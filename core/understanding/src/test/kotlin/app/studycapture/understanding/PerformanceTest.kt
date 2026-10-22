package app.studycapture.understanding

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Scaled stress test: 300 pages × 40 lines of varied, realistic-looking text (target from the
 * product spec). Generous wall-clock limits keep it stable on slow CI machines.
 */
class PerformanceTest {
    private val words = (
        "клетка митоз хромосома ядро мембрана цитоплазма деление фаза профаза метафаза анафаза телофаза " +
            "уравнение корень дискриминант коэффициент функция график парабола степень ускорение сила масса " +
            "энергия скорость импульс война армия сражение империя реформа государство климат материк океан " +
            "рельеф cell nucleus membrane energy force mass equation root function graph river climate empire " +
            "происходит образуется является состоит получают изменяется увеличивается process forms contains"
        ).split(" ")

    private fun sentence(r: Random): String {
        val n = 6 + r.nextInt(10)
        val s = (0 until n).joinToString(" ") { words[r.nextInt(words.size)] + if (r.nextInt(7) == 0) "${r.nextInt(100)}" else "" }
        return s.replaceFirstChar { it.uppercase() } + "."
    }

    @Test
    fun threeHundredPagesOfFortyLines() {
        val r = Random(42)
        val started = System.nanoTime()
        val pages = (0 until 300).map { p ->
            val lines = buildList {
                add("Тема ${p + 1}. ${words[r.nextInt(words.size)].replaceFirstChar { it.uppercase() }}")
                repeat(39) { i ->
                    add(
                        when (i % 10) {
                            3 -> "${words[r.nextInt(words.size)].replaceFirstChar { it.uppercase() }} — это ${sentence(r).lowercase()}"
                            5 -> "F${i} = m${i}a + ${r.nextInt(9)}"
                            7 -> "• ${sentence(r)}"
                            else -> sentence(r)
                        },
                    )
                }
            }
            PageText("p$p", StructureAnalyzer.analyzeText(lines.joinToString("\n")))
        }
        val analyzed = System.nanoTime()
        val summary = ExtractiveSummarizer().summarize(pages)
        val summarized = System.nanoTime()
        val cards = FlashcardGenerator().suggest(pages, max = 50)
        val carded = System.nanoTime()
        SubjectClassifier().suggest(pages.flatMap { it.blocks }.joinToString("\n") { it.text })
        val classified = System.nanoTime()

        val ms = { a: Long, b: Long -> (b - a) / 1_000_000 }
        println("analyze=${ms(started, analyzed)}ms summarize=${ms(analyzed, summarized)}ms cards=${ms(summarized, carded)}ms classify=${ms(carded, classified)}ms")
        assertEquals(300, pages.size)
        assertTrue(!summary.isEmpty)
        assertEquals(50, cards.size)
        assertTrue(ms(analyzed, summarized) < 5_000, "summarize too slow: ${ms(analyzed, summarized)} ms")
        assertTrue(ms(summarized, carded) < 5_000, "cards too slow")
        assertTrue(ms(started, analyzed) < 5_000, "structure analysis too slow")
        assertTrue(ms(carded, classified) < 3_000, "classification too slow")
    }
}

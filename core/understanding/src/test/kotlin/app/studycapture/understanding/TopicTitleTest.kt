package app.studycapture.understanding

import app.studycapture.model.BlockType
import app.studycapture.model.TextBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TopicTitleTest {
    private fun b(i: Int, type: BlockType, text: String, uncertain: Boolean = false) =
        TextBlock(i, type, text, if (uncertain) 0.2f else 0.9f, null, uncertain)

    @Test
    fun prefersHeadings() {
        assertEquals("МИТОЗ", TopicTitle.suggest(StructureAnalyzer.analyze(Samples.RU_BIOLOGY_OCR)))
        assertEquals("Квадратные уравнения", TopicTitle.suggest(StructureAnalyzer.analyzeText(Samples.RU_ALGEBRA)))
        val blocks = listOf(b(0, BlockType.HEADING, "Глава 3", uncertain = true), b(1, BlockType.SUBHEADING, "Фазы митоза:"))
        assertEquals("Фазы митоза", TopicTitle.suggest(blocks))
    }

    @Test
    fun fallsBackToFirstSentenceTrimmed() {
        val long = "Фотосинтез — это процесс образования органических веществ из углекислого газа и воды на свету. Второе."
        val t = TopicTitle.suggest(listOf(b(0, BlockType.DEFINITION, long)))!!
        assertTrue(t.length <= TopicTitle.MAX_LENGTH + 1)
        assertTrue(t.endsWith("…"))
        assertTrue(long.startsWith(t.removeSuffix("…")))
        assertEquals("обмен веществ", TopicTitle.suggest(listOf(b(0, BlockType.LIST_ITEM, "• обмен веществ"))))
    }

    @Test
    fun nothingUsable() {
        assertNull(TopicTitle.suggest(emptyList()))
        assertNull(TopicTitle.suggest(listOf(b(0, BlockType.HEADING, "Митоз", uncertain = true))))
        assertNull(TopicTitle.suggest(listOf(b(0, BlockType.FORMULA, "1 + 2"))))
    }
}

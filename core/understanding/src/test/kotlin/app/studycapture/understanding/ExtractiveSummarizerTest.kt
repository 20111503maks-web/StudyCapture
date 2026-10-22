package app.studycapture.understanding

import app.studycapture.model.BlockType
import app.studycapture.model.TextBlock
import app.studycapture.understanding.text.TextNorm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtractiveSummarizerTest {
    private val summarizer = ExtractiveSummarizer()

    /** Every point must be verbatim (modulo whitespace / leading bullet) text of its source block. */
    private fun assertGrounded(pages: List<PageText>, summary: Summary) {
        val byRef = pages.flatMap { p -> p.blocks.map { (p.pageId to it.index) to it } }.toMap()
        for (s in summary.sections) for (pt in s.points) {
            val block = byRef.getValue(pt.source.pageId to pt.source.blockIndex)
            assertFalse(block.uncertain, "uncertain block used: ${block.text}")
            assertTrue(TextNorm.squeeze(block.text).contains(pt.text), "not verbatim: '${pt.text}' in '${block.text}'")
        }
    }

    @Test
    fun russianBiologySummaryKeepsDefinitionsListsAndSkipsUncertain() {
        val pages = listOf(PageText("p1", StructureAnalyzer.analyze(Samples.RU_BIOLOGY_OCR)))
        val summary = summarizer.summarize(pages)
        assertGrounded(pages, summary)
        assertEquals(listOf("МИТОЗ", "Фазы митоза"), summary.sections.map { it.title })
        val first = summary.sections[0].points.single()
        assertEquals(PointKind.DEFINITION, first.kind)
        assertTrue(first.text.startsWith("Митоз — это способ деления"))
        val phases = summary.sections[1].points.filter { it.kind == PointKind.LIST_ITEM }.map { it.text.substringBefore(" —") }
        assertEquals(listOf("Профаза", "Метафаза", "Анафаза", "Телофаза"), phases)
        val all = summary.sections.flatMap { it.points }.joinToString(" ") { it.text }
        assertFalse(all.contains("Цитокинез"), "uncertain line leaked into the summary")
        assertFalse(all.contains("Рис. 12"), "captions are not summary material")
        assertTrue(all.contains("Важно: перед митозом в интерфазе происходит удвоение ДНК."))
        // Points stay in reading order inside a section.
        val idx = summary.sections[1].points.map { it.source.blockIndex }
        assertEquals(idx.sorted(), idx)
    }

    @Test
    fun algebraAlwaysKeepsDefinitionsAndFormulasButSkipsTasksAndExamples() {
        val pages = listOf(PageText("alg", StructureAnalyzer.analyzeText(Samples.RU_ALGEBRA)))
        val summary = summarizer.summarize(pages)
        assertGrounded(pages, summary)
        val points = summary.sections.flatMap { it.points }
        assertEquals(2, points.count { it.kind == PointKind.DEFINITION })
        assertEquals(listOf("Дискриминант: D = b² − 4ac", "x₁,₂ = (−b ± √D) / 2a"), points.filter { it.kind == PointKind.FORMULA }.map { it.text })
        assertTrue(points.none { it.text.startsWith("Пример") || it.text.startsWith("Задача") })
    }

    @Test
    fun keySentenceBudgetIsRespectedAndOrderKept() {
        val pages = listOf(PageText("h", StructureAnalyzer.analyzeText(Samples.RU_HISTORY)))
        val three = summarizer.summarize(pages, maxKeySentences = 3)
        assertGrounded(pages, three)
        val pts = three.sections.single().points
        assertEquals(3, pts.size)
        assertTrue(pts.all { it.kind == PointKind.KEY_SENTENCE })
        val all = Samples.RU_HISTORY.lines()[1]
        val positions = pts.map { all.indexOf(it.text) }
        assertTrue(positions.all { it >= 0 })
        assertEquals(positions.sorted(), positions)
        assertTrue(summarizer.summarize(pages, maxKeySentences = 0).isEmpty)
    }

    @Test
    fun multiplePagesDuplicatesAndMixedLanguages() {
        val pages = listOf(
            PageText("a", StructureAnalyzer.analyzeText(Samples.EN_BIOLOGY)),
            // The same definition photographed twice must appear once.
            PageText("b", StructureAnalyzer.analyzeText("Photosynthesis is the process by which green plants use sunlight to make glucose from carbon dioxide and water.")),
            PageText("c", StructureAnalyzer.analyzeText("Митохондрии — это органоиды, в которых происходит клеточное дыхание.\nPresent Simple используется для регулярных действий и фактов.")),
        )
        val summary = summarizer.summarize(pages)
        assertGrounded(pages, summary)
        val texts = summary.sections.flatMap { it.points }.map { it.text }
        assertEquals(1, texts.count { it.startsWith("Photosynthesis is the process") })
        assertTrue(texts.any { it.startsWith("Митохондрии — это органоиды") })
        assertEquals(texts.size, texts.toSet().size)
    }

    @Test
    fun emptyAndUncertainOnlyInput() {
        assertTrue(summarizer.summarize(emptyList()).isEmpty)
        assertTrue(summarizer.summarize(listOf(PageText("x", emptyList()))).isEmpty)
        val unsure = TextBlock(0, BlockType.DEFINITION, "Мейоз — это деление с уменьшением числа хромосом.", 0.2f, null, true)
        val s = summarizer.summarize(listOf(PageText("x", listOf(unsure))))
        assertTrue(s.isEmpty)
        assertTrue(s.sections.isEmpty())
    }
}

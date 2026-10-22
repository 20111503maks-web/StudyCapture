package app.studycapture.understanding

import app.studycapture.model.BlockType
import app.studycapture.model.TextBlock
import app.studycapture.understanding.text.TextNorm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlashcardGeneratorTest {
    private val gen = FlashcardGenerator()

    /** Backs are verbatim material of the source block (list backs: each line). Fronts never leak uncertain text. */
    private fun assertGrounded(pages: List<PageText>, cards: List<CardSuggestion>) {
        val byRef = pages.flatMap { p -> p.blocks.map { (p.pageId to it.index) to it } }.toMap()
        val material = pages.flatMap { it.blocks }.filter { !it.uncertain }.joinToString(" ") { TextNorm.squeeze(it.text) }
        for (c in cards) {
            val block = byRef.getValue(c.source.pageId to c.source.blockIndex)
            assertFalse(block.uncertain, "card from uncertain block: $c")
            c.back.split("\n").forEach { line -> assertTrue(material.contains(line), "back not verbatim: '$line'") }
            if (c.kind == CardKind.CLOZE) {
                assertTrue(c.front.contains("_____"))
                c.front.split("_____").forEach { part -> assertTrue(material.contains(part.trim()), "cloze front not verbatim: '$part'") }
            }
        }
    }

    @Test
    fun russianBiologyCards() {
        val pages = listOf(PageText("bio", StructureAnalyzer.analyze(Samples.RU_BIOLOGY_OCR)))
        val cards = gen.suggest(pages)
        assertGrounded(pages, cards)
        val mitosis = cards.first { it.front == "Что такое Митоз?" }
        assertEquals(CardKind.DEFINITION, mitosis.kind)
        assertTrue(mitosis.back.startsWith("способ деления эукариотических клеток"))
        assertEquals(1, mitosis.source.blockIndex)
        val list = cards.single { it.kind == CardKind.LIST }
        assertEquals("Перечислите: Фазы митоза", list.front)
        assertEquals(4, list.back.lines().size)
        assertTrue(list.back.lines()[0].startsWith("Профаза — "))
        assertTrue(cards.any { it.front == "Что такое Телофаза?" })
        assertTrue(cards.none { "Цитокинез" in it.front || "Цитокинез" in it.back }, "uncertain line must not become a card")
        assertTrue(cards.any { it.kind == CardKind.CLOZE })
    }

    @Test
    fun algebraFormulaAndClozeDefinitionCards() {
        val pages = listOf(PageText("alg", StructureAnalyzer.analyzeText(Samples.RU_ALGEBRA)))
        val cards = gen.suggest(pages)
        assertGrounded(pages, cards)
        val def = cards.first { it.source.blockIndex == 1 }
        assertEquals(CardKind.CLOZE, def.kind)
        assertEquals("_____ называется уравнение вида ax² + bx + c = 0, где a ≠ 0.", def.front)
        assertEquals("Квадратным уравнением", def.back)
        val disc = cards.first { it.kind == CardKind.FORMULA && it.back == "D = b² − 4ac" }
        assertEquals("Формула: Дискриминант (D = ?)", disc.front)
        val roots = cards.first { it.kind == CardKind.FORMULA && it.back.startsWith("x₁,₂") }
        assertEquals("Формула корней (x₁,₂ = ?)", roots.front)
        assertTrue(cards.none { it.back.contains("5x + 6") || it.back.contains("3x − 2") }, "examples/tasks are not cards")
    }

    @Test
    fun englishCardsUseEnglishTemplates() {
        val pages = listOf(
            PageText("bio", StructureAnalyzer.analyzeText(Samples.EN_BIOLOGY)),
            PageText("phys", StructureAnalyzer.analyzeText(Samples.EN_PHYSICS)),
        )
        val cards = gen.suggest(pages)
        assertGrounded(pages, cards)
        val fronts = cards.map { it.front }
        assertTrue("What is Photosynthesis?" in fronts)
        assertTrue("What are Mitochondria?" in fronts)
        assertTrue("List: Stages of mitosis" in fronts)
        val newton = cards.single { it.back == "F = ma" }
        assertEquals("Formula: Newton's Second Law (F = ?)", newton.front)
        assertEquals("phys", newton.source.pageId)
        assertEquals(
            "the process by which green plants use sunlight to make glucose from carbon dioxide and water.",
            cards.first { it.front == "What is Photosynthesis?" }.back,
        )
    }

    @Test
    fun capDeduplicationAndOrdering() {
        val page = PageText("bio", StructureAnalyzer.analyze(Samples.RU_BIOLOGY_OCR))
        val twice = listOf(page, page.copy(pageId = "bio2"))
        val cards = gen.suggest(twice)
        assertEquals(cards.size, cards.map { TextNorm.dedupKey(it.front) }.toSet().size)
        assertEquals(cards.size, cards.map { TextNorm.dedupKey(it.back) }.toSet().size)
        assertEquals(3, gen.suggest(twice, max = 3).size)
        assertTrue(gen.suggest(twice, max = 0).isEmpty())
        // Priority: definitions are kept first when the cap is tight.
        assertTrue(gen.suggest(twice, max = 2).all { it.kind == CardKind.DEFINITION })
    }

    @Test
    fun listsWithUncertainItemsAndEmptyInputGiveNoCards() {
        val blocks = listOf(
            TextBlock(0, BlockType.HEADING, "Признаки живого", 0.9f, null, false),
            TextBlock(1, BlockType.LIST_ITEM, "• обмен веществ", 0.9f, null, false),
            TextBlock(2, BlockType.LIST_ITEM, "• разд@жимость", 0.3f, null, true),
            TextBlock(3, BlockType.LIST_ITEM, "• рост и развитие", 0.9f, null, false),
        )
        val cards = gen.suggest(listOf(PageText("p", blocks)))
        assertTrue(cards.none { it.kind == CardKind.LIST })
        assertTrue(gen.suggest(emptyList()).isEmpty())
    }
}

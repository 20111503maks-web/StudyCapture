package app.studycapture.understanding

import app.studycapture.model.BBox
import app.studycapture.model.BlockType
import app.studycapture.model.BlockType.CAPTION
import app.studycapture.model.BlockType.DEFINITION
import app.studycapture.model.BlockType.EXAMPLE
import app.studycapture.model.BlockType.FORMULA
import app.studycapture.model.BlockType.HEADING
import app.studycapture.model.BlockType.HIGHLIGHT
import app.studycapture.model.BlockType.LIST_ITEM
import app.studycapture.model.BlockType.PARAGRAPH
import app.studycapture.model.BlockType.SUBHEADING
import app.studycapture.model.BlockType.TABLE_ROW
import app.studycapture.model.BlockType.TASK
import app.studycapture.model.OcrLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StructureAnalyzerTest {
    private fun types(text: String): List<BlockType> = StructureAnalyzer.analyzeText(text).map { it.type }

    @Test
    fun russianBiologyPhotoIsStructuredAndWrappedLinesAreJoined() {
        val blocks = StructureAnalyzer.analyze(Samples.RU_BIOLOGY_OCR)
        assertEquals(
            listOf(HEADING, DEFINITION, SUBHEADING, LIST_ITEM, LIST_ITEM, LIST_ITEM, LIST_ITEM, PARAGRAPH, HIGHLIGHT, DEFINITION, CAPTION),
            blocks.map { it.type },
        )
        assertEquals((0 until blocks.size).toList(), blocks.map { it.index })
        assertEquals(
            "Митоз — это способ деления эукариотических клеток, при котором из одной материнской клетки " +
                "образуются две дочерние клетки с идентичным набором хромосом.",
            blocks[1].text,
        )
        // Multi-sentence paragraph across four OCR lines (the previous lines reach the right margin).
        assertTrue(blocks[7].text.startsWith("Биологическое значение митоза"))
        assertTrue(blocks[7].text.endsWith("восстановление повреждённых тканей."))
        // Block box is the union of its lines; confidence is the minimum.
        assertEquals(40, blocks[1].box!!.left)
        assertEquals(0.93f, blocks[1].confidence)
        // Low-confidence line: kept verbatim, flagged, not merged with anything.
        val low = blocks[9]
        assertTrue(low.uncertain)
        assertEquals("Цитокинез — разделение цитоплазмы между дочерними клетками.", low.text)
        assertEquals(0.31f, low.confidence)
        assertFalse(blocks.filter { it.index != 9 }.any { it.uncertain })
    }

    @Test
    fun russianAlgebraPastedText() {
        assertEquals(
            listOf(HEADING, DEFINITION, DEFINITION, FORMULA, PARAGRAPH, PARAGRAPH, PARAGRAPH, SUBHEADING, FORMULA, EXAMPLE, TASK),
            types(Samples.RU_ALGEBRA),
        )
    }

    @Test
    fun englishBiologyAndPhysics() {
        assertEquals(
            listOf(HEADING, DEFINITION, PARAGRAPH, FORMULA, PARAGRAPH, SUBHEADING, DEFINITION, DEFINITION, SUBHEADING, LIST_ITEM, LIST_ITEM, LIST_ITEM, LIST_ITEM),
            types(Samples.EN_BIOLOGY),
        )
        assertEquals(listOf(HEADING, FORMULA, PARAGRAPH, PARAGRAPH, PARAGRAPH, PARAGRAPH), types(Samples.EN_PHYSICS))
    }

    @Test
    fun tableRowsAreDetected() {
        assertEquals(listOf(HEADING, TABLE_ROW, TABLE_ROW, TABLE_ROW, TABLE_ROW), types(Samples.RU_TABLE))
        assertEquals(
            listOf(TABLE_ROW, TABLE_ROW, TABLE_ROW),
            types("Year\tEvent\n1812\tBattle of Borodino\n1815\tBattle of Waterloo"),
        )
        assertEquals(
            listOf(TABLE_ROW, TABLE_ROW),
            types("Вещество    Формула\nВода    H₂O"),
        )
        // A single pipe in a formula is not a table.
        assertEquals(listOf(FORMULA), types("|x| = 5"))
    }

    @Test
    fun markersTasksExamplesCaptionsHighlights() {
        assertEquals(TASK, types("Упражнение 5. Вставьте пропущенные буквы.").single())
        assertEquals(TASK, types("№ 245 (а, б)").single())
        assertEquals(TASK, types("Exercise 3: Fill in the gaps.").single())
        assertEquals(EXAMPLE, types("Например, клетки кожи делятся митозом.").single())
        assertEquals(EXAMPLE, types("Example 2. Solve x² = 9.").single())
        assertEquals(CAPTION, types("Fig. 3 The structure of a plant cell").single())
        assertEquals(CAPTION, types("Таблица 2. Сравнение митоза и мейоза").single())
        assertEquals(HIGHLIGHT, types("Запомните: сумма углов треугольника равна 180°.").single())
        assertEquals(HIGHLIGHT, types("Note: the mass of an object never changes.").single())
        assertEquals(HEADING, types("§ 14. Строение клетки").first())
        assertEquals(HEADING, types("Chapter 3 Cell Biology").first())
        assertEquals(LIST_ITEM, types("а) профаза").single())
        assertEquals(LIST_ITEM, types("• chromosomes condense").single())
    }

    @Test
    fun definitionPatternsInSeveralLanguages() {
        assertEquals(DEFINITION, types("Фотосинтез — это процесс образования органических веществ на свету.").single())
        assertEquals(DEFINITION, types("Атом - це найменша частинка хімічного елемента.").single())
        assertEquals(DEFINITION, types("A prime number is a natural number greater than 1 with no divisors other than 1 and itself.").single())
        assertEquals(DEFINITION, types("Определение. Биссектрисой угла называется луч, делящий угол пополам.").single())
        assertEquals(DEFINITION, types("Die Zelle ist die kleinste Einheit des Lebens.").single())
        assertEquals(PARAGRAPH, types("It is a very important process for all living things.").single())
        assertEquals(PARAGRAPH, types("Он — учитель математики в нашей школе.").single())
    }

    @Test
    fun boxesDistinguishHeadingsByHeight() {
        val lines = listOf(
            OcrLine("Строение клетки", 0.95f, BBox(40, 40, 400, 95)),
            OcrLine("Клетка является основной единицей строения всех живых", 0.95f, BBox(40, 110, 1000, 140)),
            OcrLine("организмов.", 0.95f, BBox(40, 150, 220, 180)),
            OcrLine("Ядро", 0.95f, BBox(40, 200, 140, 236)),
            OcrLine("Ядро хранит наследственную информацию клетки.", 0.95f, BBox(40, 250, 900, 280)),
        )
        val blocks = StructureAnalyzer.analyze(lines)
        assertEquals(listOf(HEADING, PARAGRAPH, SUBHEADING, PARAGRAPH), blocks.map { it.type })
        assertEquals("Клетка является основной единицей строения всех живых организмов.", blocks[1].text)
    }

    @Test
    fun hyphenatedWrapIsJoinedWithoutSpaceAndKeepsTheHyphen() {
        val lines = listOf(
            OcrLine("Во время митоза происходит равномерное распре-", 0.9f, BBox(40, 40, 1000, 70)),
            OcrLine("деление хромосом между дочерними клетками.", 0.9f, BBox(40, 80, 800, 110)),
        )
        assertEquals(
            "Во время митоза происходит равномерное распре-деление хромосом между дочерними клетками.",
            StructureAnalyzer.analyze(lines).single().text,
        )
    }

    @Test
    fun uncertainLinesAreNeverMergedAndThresholdIsConfigurable() {
        val lines = listOf(
            OcrLine("Клетка является основной единицей строения всех живых", 0.9f, null),
            OcrLine("организмов и состоит из цитоплазмы и ядра.", 0.5f, null),
            OcrLine("всё остальное", null, null),
        )
        val blocks = StructureAnalyzer.analyze(lines)
        assertEquals(3, blocks.size)
        assertEquals(listOf(false, true, false), blocks.map { it.uncertain })
        assertNull(blocks[2].confidence)
        val lenient = StructureAnalyzer.analyze(lines, uncertainThreshold = 0.4f)
        assertEquals(1, lenient.size)
        assertEquals(0.5f, lenient.single().confidence)
        assertFalse(lenient.single().uncertain)
    }

    @Test
    fun pastedTextKeepsBlankLineBreaksAndWrappedParagraphs() {
        val text = """
            Причины войны

            Главной причиной войны было стремление Наполеона к мировому господству и
            нарушение Россией условий континентальной блокады Англии, которая
            подрывала экономику страны.
        """.trimIndent()
        val blocks = StructureAnalyzer.analyzeText(text)
        assertEquals(listOf(HEADING, PARAGRAPH), blocks.map { it.type })
        assertTrue(blocks[1].text.endsWith("которая подрывала экономику страны."))
        assertTrue(blocks.none { it.uncertain })
    }

    @Test
    fun emptyInput() {
        assertTrue(StructureAnalyzer.analyze(emptyList()).isEmpty())
        assertTrue(StructureAnalyzer.analyzeText("").isEmpty())
        assertTrue(StructureAnalyzer.analyzeText("  \n\n \t ").isEmpty())
    }
}

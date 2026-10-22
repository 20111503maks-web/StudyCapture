package app.studycapture.understanding.structure

import app.studycapture.model.BlockType
import app.studycapture.understanding.StructureAnalyzer

/**
 * Marks table rows (formula lines excluded, so "|x| = 5" stays a formula): ≥ 2 cells separated by
 * `|` (at least two pipes, or one pipe with no relation sign), by tabs, or by runs of ≥ 2 spaces. Space-separated
 * rows need ≥ 3 cells, or 2 cells when a neighbouring line has the same column count (aligned
 * columns). OCR engines that return each cell as a separate line are not reconstructed.
 */
internal object TableDetector {
    private val MULTISPACE = Regex(" {2,}")
    private val PIPE = Regex("\\|")
    private val TABS = Regex("\\t+")

    fun mark(lines: List<StructureAnalyzer.Ln?>) {
        val spaceCells = lines.map { l -> l?.let { cells(it.text, MULTISPACE) } ?: 0 }
        lines.forEachIndexed { i, l ->
            if (l == null || l.marker?.type == BlockType.LIST_ITEM && !l.text.contains('|')) return@forEachIndexed
            val t = l.text
            val pipes = t.count { it == '|' }
            val pipeCells = cells(t, PIPE)
            val table = when {
                l.formula -> false // "|x| = 5", "| a | + | b | ≥ | a + b |"
                pipes >= 2 && pipeCells >= 2 -> true
                pipes == 1 && pipeCells == 2 && !FormulaDetector.hasRelation(t) -> true
                cells(t, TABS) >= 2 -> true
                spaceCells[i] >= 3 -> true
                spaceCells[i] == 2 -> spaceCells.getOrNull(i - 1) == 2 || spaceCells.getOrNull(i + 1) == 2
                else -> false
            }
            if (table) l.marker = LinePatterns.Marker(BlockType.TABLE_ROW, false)
        }
    }

    private fun cells(t: String, sep: Regex): Int = t.split(sep).count { it.isNotBlank() }
}

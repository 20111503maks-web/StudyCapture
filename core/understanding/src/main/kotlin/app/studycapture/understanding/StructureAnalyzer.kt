package app.studycapture.understanding

import app.studycapture.model.BBox
import app.studycapture.model.BlockType
import app.studycapture.model.OcrLine
import app.studycapture.model.TextBlock
import app.studycapture.understanding.structure.DefinitionParser
import app.studycapture.understanding.structure.FormulaDetector
import app.studycapture.understanding.structure.LinePatterns
import app.studycapture.understanding.structure.TableDetector

/**
 * Groups OCR lines (or pasted text lines) into typed [TextBlock]s. Text is never rewritten:
 * a block's text is its lines, trimmed, joined with one space (no space after a line that ends
 * in a letter + hyphen when the next line starts in lowercase — the hyphen itself is kept).
 *
 * Pipeline:
 * 1. **Line markers** ([LinePatterns]): caption, task, example, highlight, definition label,
 *    bullet/numbered list item, heading keyword (§, Глава, Тема, Chapter, …); table rows
 *    ([TableDetector]); formula density ([FormulaDetector]).
 * 2. **Joining wrapped lines** into one block. Never across an uncertain line, a blank line,
 *    a line with its own marker, a font-size jump (> 1.3× height) or a vertical gap
 *    > 1.2× median line height. A line joins the previous one when it starts in lowercase (or
 *    with `,;)`), when the previous line is a bare label ("Определение", "Задача 3."), or when the
 *    previous line has no terminal punctuation, is not a heading candidate or a formula, and reached
 *    the right margin (with boxes: ≥ 82 % of the text column; pasted text: ≥ 40 chars and ≥ 75 %
 *    of the longest line) or ends with "," / "-". With boxes, multi-sentence paragraphs also join
 *    when the previous line reached the right margin and the next one is not indented. A list item
 *    continues only with a lowercase line or (with boxes) a hanging-indented line.
 * 3. **Typing a block**: its first line's marker wins; else a single heading-like line becomes
 *    HEADING (keyword, ALL CAPS, ≥ 1.3× median height) or SUBHEADING (≥ 1.12× height, "1.2 …"
 *    numbering, or a short line without terminal punctuation); else FORMULA; else DEFINITION
 *    ([DefinitionParser]); else "label: formula" ≤ 16 words → FORMULA; else PARAGRAPH.
 * 4. **Post-passes**: a short line ending with ":" right before a list/formula/table becomes a
 *    SUBHEADING ("Фазы митоза:"); if a page has no HEADING, a leading SUBHEADING is promoted.
 *
 * Confidence of a block = minimum of its lines' non-null confidences (null when none reported).
 * `uncertain` = confidence != null && confidence < threshold. Indices are sequential from 0.
 */
object StructureAnalyzer {
    private val NEWLINES = Regex("\\r\\n|\\r|\\n")

    fun analyze(lines: List<OcrLine>, uncertainThreshold: Float = 0.6f): List<TextBlock> {
        val raw: List<Ln?> = lines.map { l ->
            val t = l.text.trim()
            val conf = l.confidence
            if (t.isEmpty()) null else Ln(t, conf, l.box, conf != null && conf < uncertainThreshold)
        }
        val present = raw.filterNotNull()
        if (present.isEmpty()) return emptyList()
        val geo = Geometry.of(present)
        val maxLen = present.maxOf { it.text.length }
        TableDetector.mark(raw)
        for (i in raw.indices) {
            val l = raw[i] ?: continue
            val next = raw.getOrNull(i + 1)
            l.headingish = headingish(l, next, geo)
        }
        val segments = ArrayList<Seg>()
        var cur: Seg? = null
        for (l in raw) {
            if (l == null) {
                cur = null
                continue
            }
            val c = cur
            if (c != null && canJoin(c, l, geo, maxLen)) {
                c.lines += l
            } else {
                cur = Seg(mutableListOf(l)).also { segments += it }
            }
        }
        val types = segments.map { classify(it, geo) }.toMutableList()
        postProcess(segments, types)
        return segments.mapIndexed { i, s -> s.toBlock(i, types[i], uncertainThreshold) }
    }

    fun analyzeText(text: String): List<TextBlock> =
        analyze(text.split(NEWLINES).map { OcrLine(it, null, null) })

    // ---- heuristics -------------------------------------------------------------------------

    private fun headingish(l: Ln, next: Ln?, geo: Geometry?): Boolean {
        val m = l.marker
        if (m != null && m.type != BlockType.HEADING) return false
        val t = l.text
        val words = wordCount(t)
        if (words == 0 || words > (if (m != null) 14 else 10) || t.length > 90) return false
        if (t.last() in ".,;:" && m == null) return false
        val firstAlnum = t.firstOrNull { it.isLetterOrDigit() } ?: return false
        if (firstAlnum.isLetter() && firstAlnum.isLowerCase()) return false
        if (next != null && startsContinuation(next.text)) return false
        if (m != null) return true
        if (l.formula || FormulaDetector.hasRelation(t) || l.isDefinition) return false
        if (geo != null && l.box != null && geo.isFull(l.box) && geo.ratio(l.box) < 1.12) return false
        return true
    }

    private fun canJoin(seg: Seg, n: Ln, geo: Geometry?, maxLen: Int): Boolean {
        if (seg.first.uncertain || n.uncertain || n.marker != null) return false
        val fm = seg.first.marker
        if (fm != null && (fm.type == BlockType.TABLE_ROW || fm.type == BlockType.HEADING)) return false
        val p = seg.last
        val pb = p.box
        val nb = n.box
        if (geo != null && pb != null && nb != null) {
            if (nb.top - pb.bottom > 1.2 * geo.medianH) return false
            val hp = pb.height
            val hn = nb.height
            if (hp > 0 && hn > 0 && maxOf(hp, hn).toDouble() / minOf(hp, hn) > 1.3) return false
        }
        if (seg.lines.size == 1 && fm?.bareLabel == true) return true
        if (n.formula && p.text.last() == ':') return false
        if (startsContinuation(n.text)) return true
        if (seg.lines.size == 1 && fm == null && p.headingish) return false
        val boxes = geo != null && pb != null && nb != null
        if (fm?.type == BlockType.LIST_ITEM) {
            // An item continues only on a hanging indent (text left of the bullet column is a new block).
            val itemLeft = seg.first.box?.left
            return boxes && itemLeft != null && nb!!.left - itemLeft >= 0.5 * geo!!.medianH
        }
        if (p.formula || FormulaDetector.hasRelation(p.text)) return false
        val last = p.text.last()
        if (last == ':' || last == ';') return false
        if (last in ".!?…") {
            if (!boxes || fm != null) return false
            val segLeft = seg.lines.mapNotNull { it.box?.left }.minOrNull() ?: nb!!.left
            val indented = nb!!.left - segLeft > 0.8 * geo!!.medianH
            return geo.isFull(pb!!) && !indented && !n.formula && !n.headingish && !n.isDefinition
        }
        if (n.text.first().isUpperCase() && n.isDefinition) return false
        if (last == ',' || last == '-') return true
        // Wrapped text reaches the right margin; without boxes, "margin" = near the longest line.
        return if (boxes) geo!!.isFull(pb!!) else p.text.length >= 40 && p.text.length >= 0.75 * maxLen
    }

    private fun classify(seg: Seg, geo: Geometry?): BlockType {
        val text = seg.text
        val fm = seg.first.marker
        if (fm != null) {
            return if (fm.type == BlockType.HEADING) {
                if (seg.lines.size == 1 && wordCount(text) <= 14) BlockType.HEADING else BlockType.PARAGRAPH
            } else {
                fm.type
            }
        }
        if (seg.lines.size == 1) headingLevel(seg.first, geo)?.let { return it }
        val single = seg.lines.size == 1
        if (if (single) seg.first.formula else FormulaDetector.isFormula(text)) return BlockType.FORMULA
        if (if (single) seg.first.isDefinition else DefinitionParser.parse(text) != null) return BlockType.DEFINITION
        if (wordCount(text) <= 16 && FormulaDetector.split(text) != null) return BlockType.FORMULA
        return BlockType.PARAGRAPH
    }

    private fun headingLevel(l: Ln, geo: Geometry?): BlockType? {
        if (!l.headingish) return null
        val letters = l.text.filter { it.isLetter() }
        if (letters.length >= 4 && letters.none { it.isLowerCase() }) return BlockType.HEADING
        val ratio = if (geo != null && l.box != null) geo.ratio(l.box) else null
        if (ratio != null && ratio >= 1.3) return BlockType.HEADING
        if (ratio != null && ratio >= 1.12) return BlockType.SUBHEADING
        if (LinePatterns.NUMBERED_SECTION.containsMatchIn(l.text)) return BlockType.SUBHEADING
        val words = wordCount(l.text)
        return if (words <= (if (ratio != null) 7 else 8)) BlockType.SUBHEADING else null
    }

    private fun postProcess(segs: List<Seg>, types: MutableList<BlockType>) {
        for (i in 0 until segs.size - 1) {
            val s = segs[i]
            if (types[i] == BlockType.PARAGRAPH && s.lines.size == 1 && s.text.endsWith(':') &&
                wordCount(s.text) <= 10 && !s.first.uncertain &&
                types[i + 1] in setOf(BlockType.LIST_ITEM, BlockType.FORMULA, BlockType.TABLE_ROW)
            ) {
                types[i] = BlockType.SUBHEADING
            }
        }
        if (BlockType.HEADING !in types) {
            val first = segs.indices.firstOrNull { !segs[it].first.uncertain }
            if (first != null && types[first] == BlockType.SUBHEADING) types[first] = BlockType.HEADING
        }
    }

    private fun startsContinuation(t: String): Boolean = t.first().isLowerCase() || t.first() in ",;)]»"

    internal fun wordCount(t: String): Int =
        t.split(' ', '\t').count { w -> w.any { it.isLetterOrDigit() } }

    // ---- internal model ---------------------------------------------------------------------

    internal class Ln(val text: String, val conf: Float?, val box: BBox?, val uncertain: Boolean) {
        var marker: LinePatterns.Marker? = LinePatterns.marker(text)
        val formula: Boolean = FormulaDetector.isFormula(text)
        var headingish: Boolean = false
        val isDefinition: Boolean by lazy { DefinitionParser.parse(text) != null }
    }

    private class Seg(val lines: MutableList<Ln>) {
        val first: Ln get() = lines[0]
        val last: Ln get() = lines[lines.size - 1]
        val text: String by lazy {
            buildString {
                lines.forEachIndexed { i, l ->
                    if (i > 0) {
                        val prev = lines[i - 1].text
                        val hyphenWrap = prev.length >= 2 && prev.last() in "-‐" && prev[prev.length - 2].isLetter() &&
                            l.text.first().isLowerCase()
                        if (!hyphenWrap) append(' ')
                    }
                    append(l.text)
                }
            }
        }

        fun toBlock(index: Int, type: BlockType, threshold: Float): TextBlock {
            val conf = lines.mapNotNull { it.conf }.minOrNull()
            val boxes = lines.mapNotNull { it.box }
            val box = if (boxes.isEmpty()) null else BBox(
                boxes.minOf { it.left }, boxes.minOf { it.top }, boxes.maxOf { it.right }, boxes.maxOf { it.bottom },
            )
            return TextBlock(index, type, text, conf, box, conf != null && conf < threshold)
        }
    }

    /** Page geometry from line boxes; only used when at least half of the lines have boxes. */
    private class Geometry(val medianH: Double, private val minLeft: Int, private val maxRight: Int) {
        fun ratio(b: BBox): Double = if (medianH <= 0) 1.0 else b.height / medianH

        /** The line reaches (close to) the right margin of the text column. */
        fun isFull(b: BBox): Boolean = b.right >= minLeft + 0.82 * (maxRight - minLeft)

        companion object {
            fun of(lines: List<Ln>): Geometry? {
                val boxes = lines.mapNotNull { it.box }.filter { it.height > 0 && it.width > 0 }
                if (boxes.isEmpty() || boxes.size * 2 < lines.size) return null
                val hs = boxes.map { it.height }.sorted()
                val median = if (hs.size % 2 == 1) hs[hs.size / 2].toDouble() else (hs[hs.size / 2 - 1] + hs[hs.size / 2]) / 2.0
                return Geometry(median, boxes.minOf { it.left }, boxes.maxOf { it.right })
            }
        }
    }
}

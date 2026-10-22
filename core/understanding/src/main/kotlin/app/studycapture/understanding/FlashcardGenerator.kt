package app.studycapture.understanding

import app.studycapture.model.BlockType
import app.studycapture.model.SourceRef
import app.studycapture.model.TextBlock
import app.studycapture.understanding.rank.ClozeMaker
import app.studycapture.understanding.rank.KeySentences
import app.studycapture.understanding.rank.RankUnit
import app.studycapture.understanding.structure.Definition
import app.studycapture.understanding.structure.DefinitionParser
import app.studycapture.understanding.structure.FormulaDetector
import app.studycapture.understanding.structure.LinePatterns
import app.studycapture.understanding.text.TextNorm

/**
 * Suggests flashcards built only from the user's text. Backs are always verbatim material.
 *
 * Priority (the cap [suggest]'s `max` is filled in this order; output is then in reading order):
 * 1. **Definitions** (DEFINITION blocks, and list items like "Профаза — …"): "What is X?" /
 *    "Что такое X?" → body. "X называется Y" style → cloze with the term blanked.
 *    "Определение: …" without a term → "Define: <heading>" when a heading exists.
 * 2. **Formulas**: "Formula: <label or nearest heading> (<lhs> = ?)" → the formula part.
 * 3. **Lists**: 2..8 certain items right under a heading → "List: <heading>" → items, one per line.
 *    Lists with an uncertain item are skipped entirely (the answer would be incomplete).
 * 4. **Cloze** on the top TextRank sentences (6..40 words), at most half of `max`: the most
 *    salient word — letters only, ≥ 4 chars, not a stopword — is replaced by "_____" everywhere
 *    it occurs in that sentence (same stem). Salience = (1 + ln df) · ln((N + 1) / (df + 0.5)),
 *    ×0.6 for verb/adverb-looking words (-ing/-ed/-ly, -ть/-ет/-ил/…), ×1.5 for words of a defined term (heading words get no boost: they are often generic,
 *    like "года" in "Война 1812 года").
 *
 * The template language is detected per block (fallback: the whole material). Uncertain blocks
 * never produce cards. Cards with a duplicate front or back (letters/digits compared) are dropped.
 */
class FlashcardGenerator {
    private class Draft(val card: CardSuggestion, val order: Int)

    fun suggest(pages: List<PageText>, max: Int = 20): List<CardSuggestion> {
        if (max <= 0) return emptyList()
        val corpusLang = materialLanguage(pages)
        val defs = ArrayList<Draft>()
        val formulas = ArrayList<Draft>()
        val lists = ArrayList<Draft>()
        val sentences = ArrayList<RankUnit>()
        val salientStems = HashSet<String>()
        var heading: String? = null
        var order = 0
        for (page in pages) {
            val blocks = page.blocks.sortedBy { it.index }
            var i = 0
            while (i < blocks.size) {
                val b = blocks[i]
                val src = SourceRef(page.pageId, b.index)
                val text = TextNorm.squeeze(b.text)
                val tpl = { context: String -> CardTemplates.forLanguage(lang(context, corpusLang)) }
                when {
                    b.type == BlockType.HEADING || b.type == BlockType.SUBHEADING -> {
                        heading = if (b.uncertain) null else text.trimEnd(':', ' ').ifEmpty { null }
                    }
                    b.uncertain -> Unit
                    b.type == BlockType.DEFINITION -> definitionCard(text, heading, src, tpl(text))?.let {
                        defs += Draft(it, order)
                    }
                    b.type == BlockType.FORMULA -> formulaCard(text, heading, src, corpusLang)?.let { formulas += Draft(it, order) }
                    b.type == BlockType.LIST_ITEM -> {
                        var j = i
                        while (j < blocks.size && blocks[j].type == BlockType.LIST_ITEM) j++
                        val run = blocks.subList(i, j)
                        run.filter { !it.uncertain }.forEach { item ->
                            val itemText = TextNorm.squeeze(LinePatterns.stripBullet(item.text))
                            val d = DefinitionParser.parse(itemText)
                            if (d?.style == Definition.Style.TERM_FIRST) {
                                definitionCard(itemText, heading, SourceRef(page.pageId, item.index), tpl(itemText))
                                    ?.let { defs += Draft(it, order + item.index - b.index) }
                            }
                        }
                        listCard(run, heading, src, tpl(heading ?: text))?.let { lists += Draft(it, order) }
                        order += run.size
                        i = j
                        continue
                    }
                    b.type == BlockType.PARAGRAPH || b.type == BlockType.HIGHLIGHT -> {
                        sentences += KeySentences.sentencesOf(text, src, order * 100)
                    }
                }
                if (b.type == BlockType.DEFINITION && !b.uncertain) {
                    DefinitionParser.parse(text)?.term?.let { salientStems += TextNorm.contentStems(it) }
                }
                order++
                i++
            }
        }
        val chosen = ArrayList<Draft>()
        val seen = HashSet<String>()
        fun take(d: Draft): Boolean {
            if (chosen.size >= max) return false
            val f = "f:" + TextNorm.dedupKey(d.card.front)
            val bk = "b:" + TextNorm.dedupKey(d.card.back)
            if (f in seen || bk in seen) return false
            seen += f
            seen += bk
            chosen += d
            return true
        }
        (defs + formulas + lists).forEach { take(it) }
        val clozeBudget = minOf(max - chosen.size, maxOf(1, max / 2))
        if (clozeBudget > 0 && sentences.isNotEmpty()) {
            var made = 0
            for (d in ClozeMaker.make(sentences, salientStems, clozeBudget * 2)) {
                if (made >= clozeBudget) break
                if (take(Draft(d.first, d.second / 100))) made++
            }
        }
        return chosen.sortedBy { it.order }.map { it.card }
    }

    private fun definitionCard(text: String, heading: String?, src: SourceRef, tpl: CardTemplates): CardSuggestion? {
        val d = DefinitionParser.parse(text) ?: return null
        return when (d.style) {
            Definition.Style.TERM_FIRST -> CardSuggestion(tpl.whatIs(displayTerm(d.term!!), d.plural), d.body, CardKind.DEFINITION, src)
            Definition.Style.CLOZE -> {
                val term = d.term!!
                val at = text.indexOf(term)
                if (at < 0) return null
                val front = text.substring(0, at) + BLANK + text.substring(at + term.length)
                CardSuggestion(front, term, CardKind.CLOZE, src)
            }
            Definition.Style.LABELED -> heading?.let { CardSuggestion(tpl.define(it), d.body, CardKind.DEFINITION, src) }
        }
    }

    private fun formulaCard(text: String, heading: String?, src: SourceRef, corpusLang: String): CardSuggestion? {
        val split = FormulaDetector.split(text)
        val label = split?.first
        val formula = split?.second ?: text
        val context = label ?: heading
        val tpl = CardTemplates.forLanguage(if (context != null) lang(context, corpusLang) else corpusLang)
        val lhs = FormulaDetector.leftSide(formula)
        // "Формула корней" already says it is a formula: don't prefix "Формула:" again.
        val ctx = context?.let { c ->
            val cut = TextNorm.cut(c, 80)
            if (FORMULA_WORDS.any { TextNorm.normalize(c).startsWith(it) }) cut else tpl.formula(cut)
        }
        val front = when {
            ctx != null && lhs != null -> "$ctx ($lhs = ?)"
            ctx != null -> ctx
            lhs != null -> tpl.formulaFor(lhs)
            else -> return null
        }
        return CardSuggestion(front, formula, CardKind.FORMULA, src)
    }

    private fun listCard(run: List<TextBlock>, heading: String?, src: SourceRef, tpl: CardTemplates): CardSuggestion? {
        if (heading == null || run.size !in 2..MAX_LIST_ITEMS || run.any { it.uncertain }) return null
        val items = run.map { TextNorm.squeeze(LinePatterns.stripBullet(it.text)) }.filter { it.isNotEmpty() }
        if (items.size < 2) return null
        return CardSuggestion(tpl.list(heading), items.joinToString("\n"), CardKind.LIST, src)
    }

    /** "The cell" → "the cell" inside "What is …?"; other terms stay exactly as written. */
    private fun displayTerm(term: String): String {
        val first = term.substringBefore(' ')
        return if (first in setOf("The", "A", "An") && term.contains(' ')) first.lowercase() + term.substring(first.length) else term
    }

    private fun lang(text: String, fallback: String): String {
        val d = LanguageDetector.detect(text)
        return if (d.code == "und" || d.confidence < 0.3) fallback else d.code
    }

    private fun materialLanguage(pages: List<PageText>): String {
        val sample = StringBuilder()
        loop@ for (p in pages) for (b in p.blocks) {
            if (!b.uncertain) sample.append(b.text).append(' ')
            if (sample.length > 4000) break@loop
        }
        return LanguageDetector.detect(sample.toString()).code.let { if (it == "und") "en" else it }
    }

    internal companion object {
        const val BLANK = ClozeMaker.BLANK
        const val MAX_LIST_ITEMS = 8
        private val FORMULA_WORDS = listOf("формул", "formul", "formel", "fórmul")
    }
}

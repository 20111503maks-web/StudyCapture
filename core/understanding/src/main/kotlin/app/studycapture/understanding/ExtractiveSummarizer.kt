package app.studycapture.understanding

import app.studycapture.model.BlockType
import app.studycapture.model.SourceRef
import app.studycapture.understanding.rank.KeySentences
import app.studycapture.understanding.rank.RankUnit
import app.studycapture.understanding.structure.LinePatterns
import app.studycapture.understanding.text.TextNorm

/**
 * Extractive (copy-only) summary of a material. Nothing is generated: every point is the
 * verbatim text of one block or one sentence of a block, with whitespace collapsed and a leading
 * bullet/number removed.
 *
 * - **Sections** follow HEADING/SUBHEADING blocks in reading order across pages (a page without a
 *   heading continues the previous section). An uncertain heading opens an untitled section.
 *   Sections that end up empty are dropped.
 * - **Always included**: DEFINITION and FORMULA blocks, HIGHLIGHT blocks ("Важно: …") as key
 *   sentences, and list runs of ≤ 8 items (longer lists compete in the ranking).
 * - **Ranked**: sentences of PARAGRAPH blocks (4..80 words). TextRank over TF-IDF cosine with
 *   RU/EN/UK/DE/FR/ES stopwords and light stemming; the best [maxKeySentences] for the whole
 *   material are kept, skipping near-duplicates (cosine > 0.6) of already kept points.
 * - **Skipped**: uncertain blocks, examples, tasks, captions, table rows, exact duplicates
 *   (compared on letters/digits only, first occurrence wins).
 * - Points keep their original order inside a section.
 */
class ExtractiveSummarizer {
    private class Item(val unit: RankUnit, val kind: PointKind, val fixed: Boolean) {
        var selected = false
    }

    private class Section(val title: String?) {
        val items = ArrayList<Item>()
    }

    fun summarize(pages: List<PageText>, maxKeySentences: Int = 6): Summary {
        val sections = ArrayList<Section>()
        var current = Section(null).also { sections += it }
        var order = 0
        for (page in pages) {
            val blocks = page.blocks.sortedBy { it.index }
            var i = 0
            while (i < blocks.size) {
                val b = blocks[i]
                val src = SourceRef(page.pageId, b.index)
                if (b.type == BlockType.HEADING || b.type == BlockType.SUBHEADING) {
                    current = Section(if (b.uncertain) null else title(b.text)).also { sections += it }
                    i++
                    continue
                }
                if (b.type == BlockType.LIST_ITEM) {
                    var j = i
                    while (j < blocks.size && blocks[j].type == BlockType.LIST_ITEM) j++
                    val run = blocks.subList(i, j)
                    val fixed = run.size <= MAX_FIXED_LIST
                    for (item in run) {
                        if (item.uncertain) continue
                        val text = TextNorm.squeeze(LinePatterns.stripBullet(item.text))
                        if (text.isEmpty()) continue
                        current.items += Item(RankUnit(text, SourceRef(page.pageId, item.index), order++, false), PointKind.LIST_ITEM, fixed)
                    }
                    i = j
                    continue
                }
                if (!b.uncertain) {
                    val text = TextNorm.squeeze(b.text)
                    when (b.type) {
                        BlockType.DEFINITION -> current.items += Item(RankUnit(text, src, order++, true), PointKind.DEFINITION, true)
                        BlockType.FORMULA -> current.items += Item(RankUnit(text, src, order++, true), PointKind.FORMULA, true)
                        BlockType.HIGHLIGHT -> current.items += Item(RankUnit(text, src, order++, true), PointKind.KEY_SENTENCE, true)
                        BlockType.PARAGRAPH -> {
                            val units = KeySentences.sentencesOf(text, src, order)
                            order += units.size + 1
                            units.forEach { current.items += Item(it, PointKind.KEY_SENTENCE, false) }
                        }
                        else -> Unit
                    }
                }
                i++
            }
        }
        dedup(sections)
        val all = sections.flatMap { it.items }
        if (all.isEmpty()) return Summary(emptyList())
        KeySentences.score(all.map { it.unit })
        val fixedUnits = all.filter { it.fixed }.map { it.unit }
        val candidates = all.filter { !it.fixed }
        val avoid = if (fixedUnits.size <= 400) fixedUnits else emptyList()
        val chosen = KeySentences.select(candidates.map { it.unit }, maxKeySentences, avoid).toHashSet()
        candidates.forEach { it.selected = it.unit in chosen }
        return Summary(
            sections.mapNotNull { s ->
                val points = s.items.filter { it.fixed || it.selected }.map { SummaryPoint(it.unit.text, it.kind, it.unit.source) }
                if (points.isEmpty()) null else SummarySection(s.title, points)
            },
        )
    }

    /** Removes exact duplicates (letters/digits only); fixed items claim their keys first. */
    private fun dedup(sections: List<Section>) {
        val seen = HashSet<String>()
        for (pass in listOf(true, false)) {
            for (s in sections) {
                s.items.removeAll { item ->
                    if (item.fixed != pass) return@removeAll false
                    val key = TextNorm.dedupKey(item.unit.text)
                    key.isEmpty() || !seen.add(key)
                }
            }
        }
    }

    private fun title(text: String): String? =
        TextNorm.squeeze(text).trimEnd(':', ' ').takeIf { it.isNotEmpty() }

    private companion object {
        const val MAX_FIXED_LIST = 8
    }
}

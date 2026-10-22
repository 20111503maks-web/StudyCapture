package app.studycapture.understanding

import app.studycapture.model.BlockType
import app.studycapture.model.TextBlock
import app.studycapture.understanding.structure.LinePatterns
import app.studycapture.understanding.text.Sentences
import app.studycapture.understanding.text.TextNorm

/**
 * Suggests a material title from its blocks: the first certain HEADING with ≥ 3 letters, else the
 * first such SUBHEADING, else the first sentence of the first certain text block (paragraph,
 * definition, list item, highlight). Leading bullets and a trailing ":" are dropped and the result
 * is cut on a word boundary to [MAX_LENGTH] chars ("…" appended). Null when nothing qualifies.
 */
object TopicTitle {
    const val MAX_LENGTH = 60

    private val FALLBACK_TYPES = setOf(
        BlockType.PARAGRAPH, BlockType.DEFINITION, BlockType.LIST_ITEM, BlockType.HIGHLIGHT, BlockType.FORMULA,
    )

    fun suggest(blocks: List<TextBlock>): String? {
        val usable = blocks.filter { !it.uncertain && TextNorm.letterCount(it.text) >= 3 }
        val pick = usable.firstOrNull { it.type == BlockType.HEADING }
            ?: usable.firstOrNull { it.type == BlockType.SUBHEADING }
        if (pick != null) return clean(pick.text)
        val fallback = usable.firstOrNull { it.type in FALLBACK_TYPES } ?: return null
        val first = Sentences.split(LinePatterns.stripBullet(fallback.text)).firstOrNull() ?: return null
        return clean(first)
    }

    private fun clean(s: String): String? {
        val t = TextNorm.squeeze(LinePatterns.stripBullet(s)).trimEnd(':', ' ', ';', ',')
        return if (t.isEmpty()) null else TextNorm.cut(t, MAX_LENGTH)
    }
}

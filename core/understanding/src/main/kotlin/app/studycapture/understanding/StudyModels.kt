package app.studycapture.understanding

import app.studycapture.model.SourceRef
import app.studycapture.model.TextBlock

/** Blocks of one page, as produced by [StructureAnalyzer]. */
data class PageText(val pageId: String, val blocks: List<TextBlock>)

enum class PointKind { DEFINITION, FORMULA, KEY_SENTENCE, LIST_ITEM }

/** [text] is verbatim from the block at [source] (only whitespace and a leading bullet trimmed). */
data class SummaryPoint(val text: String, val kind: PointKind, val source: SourceRef)

/** [title] is the verbatim heading the points belong to; null for text before any heading. */
data class SummarySection(val title: String?, val points: List<SummaryPoint>)

data class Summary(val sections: List<SummarySection>) {
    val isEmpty: Boolean get() = sections.all { it.points.isEmpty() }
}

enum class CardKind { DEFINITION, CLOZE, FORMULA, LIST }

/**
 * A suggested flashcard. [back] is always verbatim material text (list items joined by "\n").
 * [front] is either a fixed template around verbatim text ("What is X?", "Формула: X") or the
 * material sentence with one term replaced by "_____".
 */
data class CardSuggestion(val front: String, val back: String, val kind: CardKind, val source: SourceRef)

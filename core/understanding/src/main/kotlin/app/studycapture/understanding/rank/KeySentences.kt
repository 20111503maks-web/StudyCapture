package app.studycapture.understanding.rank

import app.studycapture.model.SourceRef
import app.studycapture.understanding.structure.FormulaDetector
import app.studycapture.understanding.text.Sentences
import app.studycapture.understanding.text.TextNorm

/** One rankable unit (a sentence, definition, formula, list item) with its stems. */
internal class RankUnit(
    val text: String,
    val source: SourceRef,
    val order: Int,
    val firstInBlock: Boolean,
) {
    val stems: List<String> = TextNorm.contentStems(text)
    var vector: SparseVec? = null
    var score: Double = 0.0
}

/** Sentence extraction and TextRank scoring shared by the summarizer and the card generator. */
internal object KeySentences {
    const val MIN_WORDS = 4
    const val MAX_WORDS = 80

    /** Sentences of a block worth ranking: 4..80 words, not formulas, with ≥ 3 content stems. */
    fun sentencesOf(blockText: String, source: SourceRef, orderStart: Int): List<RankUnit> {
        val out = ArrayList<RankUnit>()
        Sentences.split(TextNorm.squeeze(blockText)).forEachIndexed { k, s ->
            val words = s.split(' ').count { w -> w.any { it.isLetter() } }
            if (words in MIN_WORDS..MAX_WORDS && !FormulaDetector.isFormula(s)) {
                val u = RankUnit(s, source, orderStart + k, k == 0)
                if (u.stems.size >= 3) out += u
            }
        }
        return out
    }

    /**
     * Scores every unit with TextRank (all units take part in the graph, so sentences close to
     * definitions/formulas gain centrality). First sentences of a block get a 10 % bonus.
     * Returns the shared [TfIdf] model.
     */
    fun score(units: List<RankUnit>): TfIdf {
        val model = TfIdf(units.map { it.stems })
        units.forEach { it.vector = model.vector(it.stems) }
        val scores = TextRank.rank(units.map { it.vector!! }, model)
        units.forEachIndexed { i, u -> u.score = scores[i] * (if (u.firstInBlock) 1.1 else 1.0) }
        return model
    }

    /**
     * Greedy top-[max] selection by score, skipping a unit whose cosine similarity to an already
     * chosen unit (or to any of [avoid]) exceeds [maxSim] — a light MMR to avoid repeats.
     */
    fun select(candidates: List<RankUnit>, max: Int, avoid: List<RankUnit> = emptyList(), maxSim: Double = 0.6): List<RankUnit> {
        if (max <= 0) return emptyList()
        val chosen = ArrayList<RankUnit>()
        for (c in candidates.sortedWith(compareByDescending<RankUnit> { it.score }.thenBy { it.order })) {
            if (chosen.size >= max) break
            val v = c.vector ?: continue
            if (chosen.any { it.vector!!.dot(v) > maxSim } || avoid.any { a -> a.vector?.let { it.dot(v) > maxSim } == true }) {
                continue
            }
            chosen += c
        }
        return chosen
    }
}

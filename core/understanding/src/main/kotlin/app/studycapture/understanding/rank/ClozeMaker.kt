package app.studycapture.understanding.rank

import app.studycapture.understanding.CardKind
import app.studycapture.understanding.CardSuggestion
import app.studycapture.understanding.text.Stemmer
import app.studycapture.understanding.text.Stopwords
import app.studycapture.understanding.text.TextNorm
import kotlin.math.ln

/** Builds cloze cards from the top-ranked sentences (see [app.studycapture.understanding.FlashcardGenerator]). */
internal object ClozeMaker {
    const val BLANK = "_____"
    private const val MIN_WORDS = 6
    private const val MAX_WORDS = 40

    private val VERBISH_LAT = listOf("ing", "ed", "ly")
    private val VERBISH_CYR = listOf("ть", "ться", "тся", "ся", "сь", "ет", "ит", "ют", "ут", "ят", "ат", "ал", "ил", "ла", "ло", "ли", "ыл")

    /** Hiding a verb/adverb ("acting", "measured", "происходит") makes a weak card: weight 0.6. */
    private fun shape(w: String): Double {
        val verbish = if (TextNorm.isCyrillic(w.last())) VERBISH_CYR.any { w.endsWith(it) } else VERBISH_LAT.any { w.endsWith(it) }
        return if (verbish) 0.6 else 1.0
    }

    /** Returns (card, order of the source sentence), best sentences first. */
    fun make(units: List<RankUnit>, boost: Set<String>, limit: Int): List<Pair<CardSuggestion, Int>> {
        val model = KeySentences.score(units)
        val eligible = units.filter { u -> u.text.split(' ').count { w -> w.any { it.isLetter() } } in MIN_WORDS..MAX_WORDS }
        val ranked = KeySentences.select(eligible, limit)
        val usedStems = HashSet<String>()
        val out = ArrayList<Pair<CardSuggestion, Int>>()
        for (u in ranked) {
            val matches = TextNorm.wordMatches(u.text).toList()
            var bestStem: String? = null
            var bestSurface: String? = null
            var bestScore = 0.0
            for (m in matches) {
                val w = m.value
                if (w.length < 4 || !w.all { it.isLetter() }) continue
                val norm = TextNorm.normalize(w)
                if (norm in Stopwords.ALL) continue
                val stem = Stemmer.stem(norm)
                if (stem in usedStems) continue
                val df = model.df(stem).coerceAtLeast(1).toDouble()
                val s = (1 + ln(df)) * ln((model.size + 1) / (df + 0.5)) * (if (stem in boost) 1.5 else 1.0) * shape(norm)
                if (s > bestScore + 1e-9 || (bestSurface != null && s > bestScore - 1e-9 && w.length > bestSurface.length)) {
                    bestScore = s
                    bestStem = stem
                    bestSurface = w
                }
            }
            if (bestStem == null || bestSurface == null || bestScore <= 0.0) continue
            val front = buildString {
                var last = 0
                for (m in matches) {
                    if (m.value.all { it.isLetter() } && Stemmer.stem(TextNorm.normalize(m.value)) == bestStem) {
                        append(u.text, last, m.range.first)
                        append(BLANK)
                        last = m.range.last + 1
                    }
                }
                append(u.text, last, u.text.length)
            }
            usedStems += bestStem
            out += CardSuggestion(front, bestSurface, CardKind.CLOZE, u.source) to u.order
        }
        return out
    }
}

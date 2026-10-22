package app.studycapture.understanding

import app.studycapture.understanding.subject.NaiveBayes
import app.studycapture.understanding.subject.SubjectLexicon
import app.studycapture.understanding.text.TextNorm
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** A material the user has already filed under [subject] (and optionally [topic]). */
data class LabeledText(val subject: String, val topic: String?, val text: String)

/**
 * @property subject canonical key from [SubjectCatalog] — or a user's own subject name learned
 *   from history.
 * @property score 0..1, higher is more certain (not a calibrated probability).
 * @property matchedKeywords text words that triggered lexicon keywords (or, for history-only
 *   matches, words shared with the user's earlier materials) — shown as "because of …".
 */
data class SubjectSuggestion(val subject: String, val topic: String?, val score: Double, val matchedKeywords: List<String>)

/**
 * Suggests a subject/topic for a material.
 *
 * **Lexicon score** ([SubjectLexicon]): for each subject, raw = Σ weight·(1 + ln count) over the
 * distinct keywords hit. It is length-normalized as `1 − exp(−raw / (1.5 + 0.02·N))` (N = words
 * in the text, so long texts need proportionally more hits) and multiplied by √(share of raw
 * among all subjects), which lowers ambiguous texts. A subject needs raw ≥ 2 (one specific term
 * or two ordinary ones). Topic = the subject's topic with the most keyword weight (≥ 1).
 *
 * **History** ([NaiveBayes] over [history]): posterior × min(1, 2·coverage) per subject. It is
 * blended in with weight `min(1, 2·max coverage) · n/(n + 3)` (n = history size), so it matters
 * only when the user has filed similar material, and grows as more is filed. The topic then
 * comes from the most similar history item of the winning subject (cosine ≥ 0.25) if any.
 *
 * Suggestions with a final score below [MIN_SCORE] are dropped; an empty list means "not sure".
 */
class SubjectClassifier(history: List<LabeledText> = emptyList()) {
    private class HistoryDoc(val subject: String, val topic: String?, val stems: Map<String, Int>)

    private val historyDocs = history.filter { it.subject.isNotBlank() }.map { h ->
        HistoryDoc(h.subject.trim(), h.topic?.trim()?.ifEmpty { null }, TextNorm.contentStems(h.text).groupingBy { it }.eachCount())
    }
    private val bayes = NaiveBayes(historyDocs.map { d -> d.subject to d.stems.flatMap { (k, v) -> List(v) { k } } })

    fun suggest(text: String, topN: Int = 3): List<SubjectSuggestion> {
        if (topN <= 0) return emptyList()
        val tokens = TextNorm.words(text).filter { w -> w.any { it.isLetter() } }
        if (tokens.isEmpty()) return emptyList()
        val lex = lexicon(tokens)
        val totalRaw = lex.values.sumOf { it.raw }
        val lexScore = lex.mapValues { (_, h) ->
            if (h.raw < 2.0) 0.0 else (1 - exp(-h.raw / (1.5 + 0.02 * tokens.size))) * sqrt(h.raw / totalRaw)
        }

        val stems = TextNorm.contentStems(text)
        val nb = bayes.classify(stems)
        val histScore = nb?.posterior?.mapValues { (c, p) -> p * minOf(1.0, 2 * (nb.coverage[c] ?: 0.0)) } ?: emptyMap()
        val strength = if (nb == null) 0.0 else
            minOf(1.0, 2 * (nb.coverage.values.maxOrNull() ?: 0.0)) * bayes.size / (bayes.size + 3.0)

        val subjects = lexScore.keys + histScore.keys
        val queryVec = stems.groupingBy { it }.eachCount()
        return subjects.map { s ->
            val score = (1 - strength) * (lexScore[s] ?: 0.0) + strength * (histScore[s] ?: 0.0)
            val hits = lex[s]
            val topic = historyTopic(s, queryVec) ?: hits?.bestTopic()
            val matched = hits?.matched() ?: emptyList()
            SubjectSuggestion(s, topic, score, matched.ifEmpty { sharedWords(s, queryVec) })
        }
            .filter { it.score >= MIN_SCORE }
            .sortedByDescending { it.score }
            .take(topN)
    }

    private class Hits {
        var raw = 0.0
        val topicRaw = HashMap<String, Double>()
        val words = HashMap<String, Double>()

        fun bestTopic(): String? = topicRaw.filterKeys { it != SubjectLexicon.GENERAL }
            .maxByOrNull { it.value }?.takeIf { it.value >= 1.0 }?.key

        fun matched(): List<String> = words.entries.sortedByDescending { it.value }.take(8).map { it.key }
    }

    private fun lexicon(tokens: List<String>): Map<String, Hits> {
        val tokenCounts = tokens.groupingBy { it }.eachCount()
        val out = HashMap<String, Hits>()
        for (subject in SubjectLexicon.SUBJECTS) {
            val hits = Hits()
            for (topic in subject.topics) for (k in topic.keywords) {
                var count = 0
                var firstWord: String? = null
                for ((tok, c) in tokenCounts) if (k.matches(tok)) {
                    count += c
                    if (firstWord == null) firstWord = tok
                }
                if (count == 0) continue
                val w = k.weight * (1 + ln(count.toDouble()))
                hits.raw += w
                hits.topicRaw[topic.name] = (hits.topicRaw[topic.name] ?: 0.0) + w
                hits.words[firstWord!!] = (hits.words[firstWord] ?: 0.0) + w
            }
            if (hits.raw > 0) out[subject.name] = hits
        }
        return out
    }

    private fun historyTopic(subject: String, query: Map<String, Int>): String? = historyDocs
        .filter { it.subject == subject && it.topic != null }
        .map { it to cosine(query, it.stems) }
        .filter { it.second >= 0.25 }
        .maxByOrNull { it.second }?.first?.topic

    private fun sharedWords(subject: String, query: Map<String, Int>): List<String> {
        val vocab = historyDocs.filter { it.subject == subject }.flatMap { it.stems.keys }.toSet()
        return query.entries.filter { it.key in vocab }.sortedByDescending { it.value }.take(5).map { it.key }
    }

    private fun cosine(a: Map<String, Int>, b: Map<String, Int>): Double {
        var dot = 0.0
        for ((k, v) in a) b[k]?.let { dot += v.toDouble() * it }
        val na = sqrt(a.values.sumOf { it.toDouble() * it })
        val nb = sqrt(b.values.sumOf { it.toDouble() * it })
        return if (na == 0.0 || nb == 0.0) 0.0 else dot / (na * nb)
    }

    companion object {
        const val MIN_SCORE = 0.3
    }
}

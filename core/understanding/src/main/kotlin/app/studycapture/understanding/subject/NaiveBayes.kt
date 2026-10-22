package app.studycapture.understanding.subject

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Multinomial naive Bayes over the user's own labeled materials (stemmed content words), with
 * Laplace smoothing. Only words seen in the history count as evidence (unknown words say
 * nothing about the user's labels). To keep long texts from producing 0/1 certainties, the
 * log-likelihoods are tempered by √(number of known words) before the softmax.
 *
 * Each class also reports *coverage*: the share of the query's words that occur in that class's
 * history. The classifier uses coverage to decide how much to trust history over the lexicon.
 */
internal class NaiveBayes(docs: List<Pair<String, List<String>>>) {
    private val counts = HashMap<String, HashMap<String, Int>>()
    private val totals = HashMap<String, Int>()
    private val docCount = HashMap<String, Int>()
    private val vocab = HashSet<String>()
    val size: Int = docs.size

    init {
        for ((label, tokens) in docs) {
            docCount[label] = (docCount[label] ?: 0) + 1
            val c = counts.getOrPut(label) { HashMap() }
            for (t in tokens) {
                c[t] = (c[t] ?: 0) + 1
                vocab += t
            }
            totals[label] = (totals[label] ?: 0) + tokens.size
        }
    }

    class Result(val posterior: Map<String, Double>, val coverage: Map<String, Double>)

    fun classify(tokens: List<String>): Result? {
        if (size == 0 || tokens.isEmpty()) return null
        val known = tokens.filter { it in vocab }
        if (known.isEmpty()) return null
        val classes = counts.keys.toList()
        val v = vocab.size.toDouble()
        val ll = classes.map { c ->
            val cnt = counts.getValue(c)
            val total = totals.getValue(c).toDouble()
            var s = ln((docCount.getValue(c) + 1.0) / (size + classes.size))
            for (t in known) s += ln(((cnt[t] ?: 0) + 1.0) / (total + v))
            s / sqrt(known.size.toDouble())
        }
        val max = ll.max()
        val e = ll.map { exp(it - max) }
        val z = e.sum()
        val posterior = classes.indices.associate { classes[it] to e[it] / z }
        val distinct = tokens.toSet()
        val coverage = classes.associateWith { c ->
            val cnt = counts.getValue(c)
            distinct.count { it in cnt }.toDouble() / distinct.size
        }
        return Result(posterior, coverage)
    }
}

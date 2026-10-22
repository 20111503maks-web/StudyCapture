package app.studycapture.understanding.rank

import kotlin.math.ln
import kotlin.math.sqrt

/** L2-normalized sparse vector over term ids (ids sorted ascending). */
internal class SparseVec(val ids: IntArray, val weights: DoubleArray) {
    val isEmpty: Boolean get() = ids.isEmpty()

    fun dot(o: SparseVec): Double {
        var i = 0
        var j = 0
        var s = 0.0
        while (i < ids.size && j < o.ids.size) {
            when {
                ids[i] == o.ids[j] -> { s += weights[i] * o.weights[j]; i++; j++ }
                ids[i] < o.ids[j] -> i++
                else -> j++
            }
        }
        return s
    }
}

/**
 * TF-IDF over "documents" (here: sentences) given as stemmed, stopword-free token lists.
 * idf = ln((N + 1) / (df + 1)) + 1 (smoothed, always > 0); tf = 1 + ln(count).
 */
internal class TfIdf(docs: List<List<String>>) {
    private val ids = HashMap<String, Int>()
    private val df: IntArray
    val size: Int = docs.size

    init {
        val counts = ArrayList<Int>()
        for (d in docs) {
            for (t in d.toSet()) {
                val id = ids.getOrPut(t) { counts.add(0); counts.size - 1 }
                counts[id] = counts[id] + 1
            }
        }
        df = counts.toIntArray()
    }

    fun id(term: String): Int? = ids[term]

    fun df(term: String): Int = ids[term]?.let { df[it] } ?: 0

    fun dfById(id: Int): Int = df[id]

    fun idf(term: String): Double = ln((size + 1.0) / (df(term) + 1.0)) + 1.0

    fun vector(tokens: List<String>): SparseVec {
        val tf = HashMap<Int, Int>()
        for (t in tokens) ids[t]?.let { tf[it] = (tf[it] ?: 0) + 1 }
        val sorted = tf.keys.sorted()
        val w = DoubleArray(sorted.size) { k ->
            val id = sorted[k]
            (1.0 + ln(tf.getValue(id).toDouble())) * (ln((size + 1.0) / (df[id] + 1.0)) + 1.0)
        }
        val norm = sqrt(w.sumOf { it * it })
        if (norm > 0) for (k in w.indices) w[k] /= norm
        return SparseVec(sorted.toIntArray(), w)
    }
}

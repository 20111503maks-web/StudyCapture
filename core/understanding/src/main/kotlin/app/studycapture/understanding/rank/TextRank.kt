package app.studycapture.understanding.rank

/**
 * TextRank (Mihalcea & Tarau 2004) over sentence vectors: nodes are sentences, edge weights are
 * TF-IDF cosine similarities (edges below [MIN_SIM] dropped), scores from weighted PageRank with
 * damping 0.85: S(i) = (1 − d) + d · Σⱼ wⱼᵢ / (Σₖ wⱼₖ) · S(j).
 *
 * Scaling: similarities are accumulated through an inverted index, so only sentence pairs that
 * share a term are touched. For large inputs, terms occurring in more than 10 % of sentences
 * (≥ 50) are ignored for the graph — they carry little idf weight but make the graph dense.
 */
internal object TextRank {
    private const val DAMPING = 0.85
    private const val MIN_SIM = 0.05
    private const val MAX_ITER = 60
    private const val EPS = 1e-6

    fun rank(vectors: List<SparseVec>, tfidf: TfIdf): DoubleArray {
        val n = vectors.size
        if (n == 0) return DoubleArray(0)
        if (n == 1) return doubleArrayOf(1.0)
        val dfCap = if (n > 500) maxOf(50, n / 10) else Int.MAX_VALUE
        // inverted index: term id -> (sentence, weight)
        val postings = HashMap<Int, MutableList<Int>>()
        vectors.forEachIndexed { i, v -> v.ids.forEach { t -> if (tfidf.dfById(t) <= dfCap) postings.getOrPut(t) { ArrayList() } += i } }
        val adjIdx = Array(n) { IntList() }
        val adjW = Array(n) { DoubleList() }
        val acc = DoubleArray(n)
        val touched = IntList()
        for (i in 0 until n) {
            val vi = vectors[i]
            for (k in vi.ids.indices) {
                val list = postings[vi.ids[k]] ?: continue
                for (j in list) {
                    if (j <= i) continue
                    if (acc[j] == 0.0) touched.add(j)
                    acc[j] += vi.weights[k] * weightOf(vectors[j], vi.ids[k])
                }
            }
            for (t in 0 until touched.size) {
                val j = touched[t]
                val w = acc[j]
                if (w >= MIN_SIM) {
                    adjIdx[i].add(j); adjW[i].add(w)
                    adjIdx[j].add(i); adjW[j].add(w)
                }
                acc[j] = 0.0
            }
            touched.clear()
        }
        val outSum = DoubleArray(n) { i -> adjW[i].sum() }
        var s = DoubleArray(n) { 1.0 }
        repeat(MAX_ITER) {
            val next = DoubleArray(n) { 1.0 - DAMPING }
            for (j in 0 until n) {
                if (outSum[j] == 0.0) continue
                val share = DAMPING * s[j] / outSum[j]
                val idx = adjIdx[j]
                val w = adjW[j]
                for (k in 0 until idx.size) next[idx[k]] += share * w[k]
            }
            var delta = 0.0
            for (i in 0 until n) delta += kotlin.math.abs(next[i] - s[i])
            s = next
            if (delta < EPS * n) return s
        }
        return s
    }

    private fun weightOf(v: SparseVec, id: Int): Double {
        val k = v.ids.binarySearch(id)
        return if (k >= 0) v.weights[k] else 0.0
    }

    private class IntList {
        private var a = IntArray(8)
        var size = 0
            private set

        fun add(v: Int) {
            if (size == a.size) a = a.copyOf(size * 2)
            a[size++] = v
        }

        operator fun get(i: Int): Int = a[i]

        fun clear() { size = 0 }
    }

    private class DoubleList {
        private var a = DoubleArray(8)
        var size = 0
            private set

        fun add(v: Double) {
            if (size == a.size) a = a.copyOf(size * 2)
            a[size++] = v
        }

        operator fun get(i: Int): Double = a[i]

        fun sum(): Double {
            var s = 0.0
            for (i in 0 until size) s += a[i]
            return s
        }
    }
}

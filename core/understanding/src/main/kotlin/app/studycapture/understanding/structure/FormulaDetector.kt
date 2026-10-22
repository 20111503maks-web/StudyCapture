package app.studycapture.understanding.structure

import app.studycapture.understanding.text.Stopwords
import app.studycapture.understanding.text.TextNorm

/**
 * Recognizes math/physics/chemistry formulas by symbol density.
 *
 * A text is a formula when it contains a relation (= ≈ ≠ ≤ ≥ < > → ⇒ ∈) or a big operator
 * (√ ∑ ∫ ∏) and has at most two "real words" (letter runs of ≥ 3 letters that are not math
 * function names such as sin/log/lim) — or when math symbols and digits make up ≥ 45 % of its
 * non-space characters and it has at most one real word. Single letters, "ma", "4ac", "H₂O"
 * are variables, not words.
 */
internal object FormulaDetector {
    private const val RELATIONS = "=≈≠≤≥<>→⇒⇔↔∈≡∝"
    private const val BIG_OPS = "√∑∫∏∮"
    private const val MATH = "+−-×÷*/^±·∞πΔ∆∂²³⁴⁵⁶⁷⁸⁹⁰¹₀₁₂₃₄₅₆₇₈₉%°′″()[]{}|!,'"
    private val LETTER_RUN = Regex("\\p{L}+")
    private val NON_SPACE = Regex("\\S+")
    private val FUNCS = setOf(
        "sin", "cos", "tan", "tg", "ctg", "cot", "log", "ln", "lg", "lim", "max", "min", "exp", "sqrt",
        "arcsin", "arccos", "arctg", "arctan", "const", "mod", "div", "det", "sgn", "abs", "sinh", "cosh",
    )

    fun realWords(text: String): Int =
        LETTER_RUN.findAll(text).count { it.value.length >= 3 && it.value.lowercase() !in FUNCS }

    fun hasRelation(text: String): Boolean = text.any { it in RELATIONS }

    fun isFormula(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty() || t.length > 160) return false
        val words = realWords(t)
        val strong = hasRelation(t) || t.any { it in BIG_OPS }
        if (strong && words <= 2 && startsMathy(t)) return true
        val nonSpace = t.count { !it.isWhitespace() }
        val mathy = t.count { it.isDigit() || it in RELATIONS || it in BIG_OPS || it in MATH }
        val anyOp = strong || t.any { it in "+−×÷*/^²³" }
        return anyOp && words <= 1 && nonSpace > 0 && mathy.toDouble() / nonSpace >= 0.45
    }

    /**
     * Splits "label: formula" ("Второй закон Ньютона: F = ma", "Discriminant D = b² − 4ac") into
     * (label, formula). Both parts are verbatim substrings. Returns null when there is no
     * short wordy label followed by a formula containing a relation.
     */
    fun split(text: String): Pair<String, String>? {
        val t = text.trim()
        val tokens = NON_SPACE.findAll(t).toList()
        if (tokens.size < 2) return null
        for (k in 1 until minOf(tokens.size, 12)) {
            val start = tokens[k].range.first
            val formula = t.substring(start).trim()
            if (!hasRelation(formula) || !isFormula(formula)) continue
            val label = t.substring(0, start).trim().trimEnd(':', '—', '–', '-', ',', ' ')
            if (realWords(label) == 0 || label.length > 80) return null
            return label to formula
        }
        return null
    }

    /** The formula must start with a variable, number, bracket or operator — not with a word. */
    private fun startsMathy(t: String): Boolean {
        val first = LETTER_RUN.find(t.substringBefore(' '))?.value ?: return true
        val lower = first.lowercase()
        return when {
            first.length == 1 -> true
            first.length == 2 -> TextNorm.normalize(first) !in Stopwords.ALL
            else -> lower in FUNCS
        }
    }

    /** Left-hand side of the first "=" when it is short ("F", "D", "x₁,₂"), for card fronts. */
    fun leftSide(formula: String): String? {
        val i = formula.indexOf('=')
        if (i <= 0) return null
        val lhs = formula.substring(0, i).trim()
        return lhs.takeIf { it.isNotEmpty() && it.length <= 10 && realWords(it) == 0 }
    }
}

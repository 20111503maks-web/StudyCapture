package app.studycapture.understanding.text

/**
 * Splits a block into sentences without altering them. A boundary is `.`/`!`/`?`/`…` followed by
 * whitespace and an uppercase letter, digit, quote, bracket or dash — unless the period ends a
 * known abbreviation ("т.е.", "рис.", "Fig.", "e.g.") or a single-letter initial ("А. С. Пушкин").
 */
internal object Sentences {
    private val ABBREV = setOf(
        "т", "е", "д", "п", "др", "пр", "см", "рис", "табл", "стр", "г", "гг", "в", "вв", "им", "ул", "с",
        "тыс", "млн", "млрд", "руб", "коп", "напр", "ср", "англ", "лат", "греч", "проф", "акад", "св",
        "e", "g", "i", "eg", "ie", "fig", "figs", "mr", "mrs", "ms", "dr", "vs", "st", "p", "pp",
        "ch", "sec", "eq", "approx", "cf", "vol", "jr", "sr", "prof", "abb", "bzw", "z", "b",
    )

    fun split(text: String): List<String> {
        val out = ArrayList<String>()
        var start = 0
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (c == '.' || c == '!' || c == '?' || c == '…') {
                var j = i
                while (j + 1 < n && text[j + 1] in ".!?…»\")") j++
                var k = j + 1
                if (k < n && text[k].isWhitespace()) {
                    while (k < n && text[k].isWhitespace()) k++
                    if (k < n && startsSentence(text[k]) && !(c == '.' && j == i && isAbbreviation(text, i))) {
                        add(out, text.substring(start, j + 1))
                        start = k
                    }
                }
                i = j + 1
            } else {
                i++
            }
        }
        if (start < n) add(out, text.substring(start))
        return out
    }

    private fun add(out: MutableList<String>, s: String) {
        val t = s.trim()
        if (t.isNotEmpty()) out += t
    }

    private fun startsSentence(c: Char): Boolean =
        c.isUpperCase() || c.isDigit() || c in "«\"“(—–-["

    private fun isAbbreviation(text: String, dot: Int): Boolean {
        var b = dot - 1
        while (b >= 0 && text[b].isLetter()) b--
        val word = TextNorm.normalize(text.substring(b + 1, dot))
        if (word.isEmpty()) return false
        if (word.length == 1 && text[dot - 1].isUpperCase()) return true // initial: "А. С. Пушкин"
        return word in ABBREV
    }
}

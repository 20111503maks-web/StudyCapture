package app.studycapture.understanding.text

/**
 * A deliberately light suffix-stripping stemmer for Russian/Ukrainian (Cyrillic) and English-like
 * (Latin) words. It is not a linguistic stemmer: it only removes common inflection endings so that
 * the stem works as a *prefix* key ("митохондрии" → "митохондр", matches "митохондрия").
 *
 * Rules: the input must already be normalized ([TextNorm.normalize]). The longest matching suffix
 * whose removal leaves a long enough stem wins. Cyrillic stems keep ≥ 4 chars (≥ 3 when only a
 * single vowel is removed, so "сила"/"силы" → "сил"); Latin stems keep ≥ 3 chars.
 */
internal object Stemmer {
    private val CYR_SUFFIXES = listOf(
        // nouns/adjectives (RU)
        "остями", "иями", "ями", "ами", "ость", "ости", "остью", "иях", "ях", "ах", "ием", "ией", "иям",
        "ого", "его", "ому", "ему", "ыми", "ими", "ых", "их", "ов", "ев", "ей", "ий", "ый", "ой", "ая", "яя",
        "ое", "ее", "ые", "ие", "ия", "ию", "ии", "ом", "ем", "ам", "ям", "ую", "юю",
        // verbs (RU)
        "ться", "тся", "ать", "ять", "ить", "еть",
        // Ukrainian
        "ів", "їв", "ою", "ею", "ій", "ої", "ім", "ії", "ію", "ія",
        // single letters
        "а", "я", "ы", "и", "е", "у", "ю", "о", "ь", "й", "і", "ї", "є",
    ).filter { s -> s.all { TextNorm.isCyrillic(it) } }.distinct().sortedByDescending { it.length }

    private val LAT_SUFFIXES = listOf(
        "izations", "ization", "ational", "ations", "ation", "nesses", "ness", "ments", "ment",
        "ingly", "ings", "ing", "edly", "ies", "ied", "es", "ed", "ly", "al", "s",
    ).sortedByDescending { it.length }

    private const val CYR_VOWELS = "аяыиеуюоэіїє"

    fun stem(word: String): String {
        if (word.isEmpty() || !word.last().isLetter()) return word
        val last = word.last()
        return when {
            TextNorm.isCyrillic(last) -> cyrillic(word)
            TextNorm.isLatin(last) -> latin(word)
            else -> word
        }
    }

    private fun cyrillic(w: String): String {
        for (suf in CYR_SUFFIXES) {
            if (!w.endsWith(suf)) continue
            val stemLen = w.length - suf.length
            val min = if (suf.length == 1 && suf[0] in CYR_VOWELS) 3 else 4
            if (stemLen >= min) return w.substring(0, stemLen)
        }
        return w
    }

    private fun latin(input: String): String {
        val w = input.removeSuffix("'s").removeSuffix("’s")
        // "photosynthesis" → "photosynthes", "analysis" → "analys": Greek -sis nouns.
        if (w.endsWith("sis") && w.length >= 7) return w.dropLast(2)
        if (w.endsWith("ss") || w.endsWith("us")) return w // class, process, nucleus
        for (suf in LAT_SUFFIXES) {
            if (w.endsWith(suf) && w.length - suf.length >= 3) return w.dropLast(suf.length)
        }
        return w
    }
}

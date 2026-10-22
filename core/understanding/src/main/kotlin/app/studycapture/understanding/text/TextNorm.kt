package app.studycapture.understanding.text

import java.util.Locale

/**
 * Low-level text helpers shared by every analyzer. Nothing here changes the user's text:
 * normalization produces *keys* for matching/ranking; the original strings are what we show.
 */
internal object TextNorm {
    /** A word: letters/digits, optionally with inner apostrophes (Newton's, м'ята). */
    private val WORD = Regex("[\\p{L}\\p{M}\\d]+(?:['’ʼ][\\p{L}\\p{M}]+)*")
    private val SPACES = Regex("\\s+")

    /** Lowercase (locale-independent) and fold ё→е, the way Russian readers treat them. */
    fun normalize(s: String): String = s.lowercase(Locale.ROOT).replace('ё', 'е')

    /** Normalized word tokens in reading order. */
    fun words(text: String): List<String> =
        WORD.findAll(text).map { normalize(it.value) }.toList()

    /** Word matches with their positions in the original (un-normalized) text. */
    fun wordMatches(text: String): Sequence<MatchResult> = WORD.findAll(text)

    /** Normalized, stopword-free, stemmed tokens with at least one letter — the unit for ranking. */
    fun contentStems(text: String): List<String> =
        words(text).filter { w -> w.length >= 2 && w.any { it.isLetter() } && w !in Stopwords.ALL }
            .map { Stemmer.stem(it) }

    /** Trims and collapses whitespace runs (line joins, tabs) into single spaces. */
    fun squeeze(s: String): String = SPACES.replace(s.trim(), " ")

    /** Key for duplicate detection: letters and digits only, normalized. */
    fun dedupKey(s: String): String =
        buildString { normalize(s).forEach { if (it.isLetterOrDigit()) append(it) } }

    fun letterCount(s: String): Int = s.count { it.isLetter() }

    fun isCyrillic(c: Char): Boolean = Character.UnicodeScript.of(c.code) == Character.UnicodeScript.CYRILLIC

    fun isLatin(c: Char): Boolean = Character.UnicodeScript.of(c.code) == Character.UnicodeScript.LATIN

    /**
     * Cuts [s] to at most [max] chars on a word boundary, appending "…" when shortened.
     * Used only for titles; the cut text is still a verbatim prefix.
     */
    fun cut(s: String, max: Int): String {
        if (s.length <= max) return s
        val hard = s.substring(0, max)
        val space = hard.lastIndexOf(' ')
        val base = if (space >= max / 2) hard.substring(0, space) else hard
        return base.trimEnd(' ', ',', ';', ':', '—', '–', '-') + "…"
    }
}

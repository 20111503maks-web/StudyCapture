package app.studycapture.understanding.structure

import app.studycapture.understanding.text.TextNorm

/**
 * A definition found in a block. [term] and [body] are verbatim substrings of the block text.
 *
 * - [Style.TERM_FIRST]: "X — это Y", "X is a Y", "X: Y" → ask "What is X?", answer Y.
 * - [Style.CLOZE]: "Y называется X" / "X называется Y" (term often inflected) → blank the term.
 * - [Style.LABELED]: "Определение. …" with no recognizable term → [term] is null.
 */
internal data class Definition(
    val term: String?,
    val body: String,
    val style: Style,
    val plural: Boolean = false,
) {
    enum class Style { TERM_FIRST, CLOZE, LABELED }
}

/**
 * Pattern-based definition extraction (RU/UK/EN + DE/FR/ES basics). Patterns, tried in order:
 * 1. `X — это Y` / `X - це Y` (also `X это Y` when X ≤ 3 words);
 * 2. `Y называется X` / `X is called Y` (cloze; the shorter side, ≤ 4 words, is the term);
 * 3. `X is/are (a|an|the|defined as|called|…) Y`, `X means/refers to Y`, `X ist ein Y`, `X est un Y`, `X es un Y`;
 * 4. `X — Y` (term ≤ 5 words, body ≥ 2 words, no digits in the term);
 * 5. `X: Y` (term ≤ 4 words, body ≥ 3 words, not a formula, not a label like "Ответ").
 * Terms must contain letters, must not start with a pronoun, and are at most 6 words / 60 chars.
 */
internal object DefinitionParser {
    private val I = RegexOption.IGNORE_CASE
    private const val END = "(?![\\p{L}])"
    private val ETO = Regex("^(.{1,60}?)\\s*[—–-]\\s*(?:это|це)$END\\s*(.{3,})$", I)
    private val ETO_NO_DASH = Regex("^([^,.!?:;—–]{1,40}?)\\s+(?:это|це)\\s+(.{3,})$", I)
    private val CALLED = Regex(
        "^(.+?)\\s+(называется|называются|называют|называем|називається|називаються|is called|are called|" +
            "is known as|are known as|is termed|heißt|s'appelle|se llama|se denomina)$END\\s+(.+?)$",
        I,
    )
    private val IS_A = Regex(
        "^(.{1,60}?)\\s+(is|are)\\s+((?:defined as|called|known as|the|an?|one|any)$END.{3,})$",
        I,
    )
    private val MEANS = Regex("^(.{1,60}?)\\s+(means|refers to|stands for|denotes|is defined as|are defined as)\\s+(.{3,})$", I)
    private val IS_SHORT = Regex("^(\\p{Lu}[\\p{L}\\p{M}'’-]*(?:\\s+[\\p{L}\\p{M}'’-]+){0,2})\\s+(is|are)\\s+(.{3,})$")
    private val EURO = Regex(
        "^(.{1,60}?)\\s+(ist|sind|est|sont|es|son)\\s+((?:eine?|der|die|das|une?|le|la|les|el|los|las|una?)$END.{3,})$",
        I,
    )
    private val DASH = Regex("^([^—–:.!?\\d]{1,50}?)\\s+(?:[—–]|-(?=\\s))\\s+(.{3,})$")
    private val COLON = Regex("^([^:.!?\\d]{1,40}):\\s+(.{3,})$")

    private val SPACES = Regex("\\s+")
    private val INSTRUMENTAL = listOf("ом", "ем", "ой", "ей", "ью", "ами", "ями", "ым", "им", "ою", "єю")
    private val PRONOUNS = setOf(
        "it", "this", "that", "there", "here", "he", "she", "they", "we", "you", "i", "these", "those", "which",
        "what", "who", "one", "each", "all", "some", "many", "most", "such", "his", "her", "its", "their", "our",
        "он", "она", "оно", "они", "это", "мы", "вы", "я", "ты", "тот", "та", "то", "все", "всё", "кто", "что",
        "він", "вона", "воно", "вони", "це", "хто", "що", "es", "er", "sie", "il", "elle", "ce", "ello",
    )
    private val COLON_LABELS = setOf(
        "ответ", "решение", "дано", "найти", "пример", "примеры", "задача", "задание", "вывод", "итог", "примечание",
        "answer", "solution", "given", "find", "example", "examples", "task", "note", "conclusion", "summary",
        "тема", "topic", "дата", "date", "домашнее задание", "дз", "homework", "hw", "урок", "lesson",
    )

    fun parse(raw: String): Definition? {
        var text = TextNorm.squeeze(raw)
        LinePatterns.DEFINITION_LABEL.find(text)?.let { m ->
            val rest = text.substring(m.range.last + 1).trim().trimStart(':', '.', '—', '–', '-', ' ')
            if (rest.isEmpty()) return null
            return parseBody(rest) ?: Definition(null, rest, Definition.Style.LABELED)
        }
        return parseBody(text)
    }

    private fun parseBody(text: String): Definition? {
        ETO.find(text)?.let { m -> termFirst(m.groupValues[1], m.groupValues[2], false)?.let { return it } }
        ETO_NO_DASH.find(text)?.let { m ->
            if (words(m.groupValues[1]) <= 3) termFirst(m.groupValues[1], m.groupValues[2], false)?.let { return it }
        }
        CALLED.find(text)?.let { m -> cloze(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { return it } }
        IS_A.find(text)?.let { m ->
            termFirst(m.groupValues[1], m.groupValues[3], m.groupValues[2].equals("are", true))?.let { return it }
        }
        MEANS.find(text)?.let { m -> termFirst(m.groupValues[1], m.groupValues[3], false)?.let { return it } }
        IS_SHORT.find(text)?.let { m ->
            if (words(m.groupValues[3]) >= 4 && nounLike(m.groupValues[3].substringBefore(' '))) {
                termFirst(m.groupValues[1], m.groupValues[3], m.groupValues[2] == "are")?.let { return it }
            }
        }
        EURO.find(text)?.let { m ->
            val plural = m.groupValues[2].lowercase() in setOf("sind", "sont", "son")
            termFirst(m.groupValues[1], m.groupValues[3], plural)?.let { return it }
        }
        DASH.find(text)?.let { m ->
            val term = m.groupValues[1]
            if (words(term) <= 5 && words(m.groupValues[2]) >= 2 && !FormulaDetector.isFormula(m.groupValues[2])) {
                termFirst(term, m.groupValues[2], false)?.let { return it }
            }
        }
        COLON.find(text)?.let { m ->
            val term = m.groupValues[1].trim()
            val body = m.groupValues[2]
            if (words(term) <= 4 && words(body) >= 3 && TextNorm.normalize(term) !in COLON_LABELS &&
                !FormulaDetector.isFormula(body) && FormulaDetector.split(text) == null
            ) {
                termFirst(term, body, false)?.let { return it }
            }
        }
        return null
    }

    private fun termFirst(rawTerm: String, rawBody: String, plural: Boolean): Definition? {
        val term = cleanTerm(rawTerm) ?: return null
        val body = rawBody.trim()
        if (TextNorm.letterCount(body) < 3) return null
        return Definition(term, body, Definition.Style.TERM_FIRST, plural)
    }

    /**
     * "X называется Y": the term is the side in the instrumental case ("Квадратным уравнением
     * называется …"), else the shorter side; for "is called"/"known as" it is always the right side.
     */
    private fun cloze(left: String, verb: String, right: String): Definition? {
        val l = LinePatterns.stripBullet(left).trim()
        val r = right.trim().trimEnd('.', ';', '!')
        val lastLeft = TextNorm.normalize(l.substringAfterLast(' '))
        val termIsLeft = when {
            verb.first().isLetter() && TextNorm.isLatin(verb.first()) -> false
            INSTRUMENTAL.any { lastLeft.endsWith(it) } && words(l) <= 4 -> true
            else -> words(l) < words(r)
        }
        val (term, rest) = if (termIsLeft) l to r else r to l
        if (words(term) > 4 || words(rest) < 2) return null
        val t = cleanTerm(term) ?: return null
        return Definition(t, rest, Definition.Style.CLOZE)
    }

    private fun cleanTerm(raw: String): String? {
        val t = LinePatterns.stripBullet(raw).trim().trimEnd(',', ':', ' ').trim('«', '»', '"', '“', '”')
        if (t.isEmpty() || t.length > 60 || words(t) > 6 || TextNorm.letterCount(t) < 2) return null
        if (!t.first().isLetter()) return null
        val first = TextNorm.normalize(t.substringBefore(' '))
        if (first in PRONOUNS) return null
        return t
    }

    /** "X is organelles…" yes; "X is measured / released / inversely / in …" no. */
    private fun nounLike(word: String): Boolean {
        val w = TextNorm.normalize(word)
        if (w.length < 3 || w in NOT_NOUN) return false
        return !(w.endsWith("ed") || w.endsWith("ly") || w.endsWith("en") || w.endsWith("ing"))
    }

    private val NOT_NOUN = setOf(
        "not", "also", "very", "too", "often", "usually", "always", "never", "only", "still", "now", "then", "here",
        "there", "about", "into", "from", "with", "for", "important", "essential", "necessary", "possible", "equal",
        "proportional", "responsible", "able", "used", "made", "found", "known", "seen", "shown", "true", "false",
        "different", "similar", "same", "more", "less", "much", "many", "most", "some", "all", "one", "two", "three",
    )

    private fun words(s: String): Int = s.trim().split(SPACES).count { w -> w.any { it.isLetterOrDigit() } }
}

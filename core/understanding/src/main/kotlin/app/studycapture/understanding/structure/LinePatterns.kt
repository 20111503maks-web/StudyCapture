package app.studycapture.understanding.structure

import app.studycapture.model.BlockType

/**
 * Line-start markers that decide a block's type on their own ("strong" markers). Patterns are
 * matched against the trimmed line, case-insensitively, and cover RU/UK/EN plus DE/FR/ES basics.
 * `(?![\p{L}])` is used instead of `\b`, which is ASCII-only on recent JDKs.
 */
internal object LinePatterns {
    private val I = RegexOption.IGNORE_CASE
    private const val END = "(?![\\p{L}])"

    /** •, -, *, "1.", "1)", "a)", "а)". A dash followed by "это"/"це" is a definition, not a bullet. */
    val BULLET = Regex(
        "^(?:[•●○◦▪▫■□‣∙·*►▸➢✓✔→]|[-–—](?!\\s*(?:это|це)$END)|\\d{1,2}[.)](?!\\d)|[a-zа-яё][)])\\s*(?=\\S)",
        I,
    )

    val TASK = Regex(
        "^(?:(?:задача|задание|упражнение|вопрос|exercise|task|problem|question|aufgabe|exercice|ejercicio|" +
            "вправа|завдання|питання)$END\\s*(?:№\\s*)?(?:\\d+[a-zа-я]?[.):]?|[:.)]|$)|№\\s*\\d+|(?:ex|упр|ex\\.)\\.?\\s*\\d+|" +
            "(?:ответ|решение|дано|найти|answer|solution|given|find|відповідь|розв['’]язання)\\s*:)",
        I,
    )

    val EXAMPLE = Regex(
        "^(?:примеры?|например|for example|for instance|examples?|e\\.\\s?g\\.|приклад|наприклад|beispiel|" +
            "exemple|ejemplo|z\\.\\s?b\\.)$END",
        I,
    )

    val CAPTION = Regex(
        "^(?:(?:рис\\.?|рисунок|fig\\.?|figure|таблица|табл\\.|table|схема|diagram|abb\\.|abbildung|tabelle|" +
            "графік|график|chart)\\s*\\d+|(?:рис|fig)\\.\\s)",
        I,
    )

    val HIGHLIGHT = Regex(
        "^(?:!{1,3}(?=\\s*\\S)|(?:важно|запомни(?:те)?|внимание|обрати(?:те)? внимание|правило|note|n\\.?\\s?b\\.?|" +
            "important|remember|key point|rule|wichtig|merke|attention|важливо|запам['’]ятай(?:те)?|увага)" +
            "\\s*(?:[!:.—–-]|$))",
        I,
    )

    val DEFINITION_LABEL = Regex(
        "^(?:определение|definition|def\\.|означення|визначення|définition|definición)\\s*\\d*\\s*(?:[:.—–-]|$)",
        I,
    )

    val HEADING_KEYWORD = Regex(
        "^(?:§\\s*\\d|(?:глава|тема|раздел|урок|лекция|параграф|chapter|section|unit|lesson|topic|lecture|" +
            "розділ|kapitel|chapitre|capítulo|lección)(?:\\s*$|\\s*[\\d:.IVX№]|\\s+\\p{Lu}))",
        I,
    )

    val NUMBERED_SECTION = Regex("^\\d+(?:\\.\\d+)+\\.?\\s+\\S")

    private val LABEL_REMAINDER = Regex("^[\\s\\d.):!—–-]*$")

    /** The strong marker of [text] (trimmed), or null. TABLE_ROW is decided separately. */
    fun marker(text: String): Marker? {
        CAPTION.find(text)?.let { return Marker(BlockType.CAPTION, bare(text, it)) }
        TASK.find(text)?.let { return Marker(BlockType.TASK, bare(text, it)) }
        EXAMPLE.find(text)?.let { return Marker(BlockType.EXAMPLE, bare(text, it)) }
        HIGHLIGHT.find(text)?.let { return Marker(BlockType.HIGHLIGHT, bare(text, it)) }
        DEFINITION_LABEL.find(text)?.let { return Marker(BlockType.DEFINITION, bare(text, it)) }
        if (BULLET.containsMatchIn(text)) return Marker(BlockType.LIST_ITEM, false)
        if (HEADING_KEYWORD.containsMatchIn(text)) return Marker(BlockType.HEADING, false)
        return null
    }

    /** A "bare" label ("Определение", "Задача 3.", "Пример:") owns the following line(s). */
    private fun bare(text: String, m: MatchResult): Boolean = LABEL_REMAINDER.matches(text.substring(m.range.last + 1))

    /** Removes a leading bullet/number ("• ", "1) ", "а) ") — allowed for display. */
    fun stripBullet(text: String): String {
        val m = BULLET.find(text) ?: return text.trim()
        return text.substring(m.range.last + 1).trim()
    }

    data class Marker(val type: BlockType, val bareLabel: Boolean)
}

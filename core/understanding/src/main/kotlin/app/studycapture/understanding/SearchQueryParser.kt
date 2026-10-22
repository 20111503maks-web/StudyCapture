package app.studycapture.understanding

import app.studycapture.understanding.text.Stemmer
import app.studycapture.understanding.text.Stopwords
import app.studycapture.understanding.text.TextNorm

/**
 * Turns a natural-language search ("где у меня было про митохондрии?", "where did I have
 * photosynthesis notes") into SQLite FTS4 prefix queries.
 *
 * 1. Lowercase, fold ё→е; every character that is not a letter or digit becomes a space. This
 *    removes all FTS syntax (`* : ^ ( ) - " '`), so user input can never inject operators.
 * 2. Drop FTS keywords (and/or/not/near), all RU/UK/EN/DE/FR/ES stopwords, and query filler
 *    ("где", "было", "про", "конспект", "notes", "find", …) incl. their inflections.
 * 3. Light stemming ([Stemmer]); keep tokens of ≥ 2 chars; dedupe; at most [MAX_TERMS].
 * 4. Emit `stem*` terms: [ftsAnd] joins them with spaces (FTS4 implicit AND), [ftsOr] with " OR ".
 *
 * Terms contain only lowercase letters/digits, so the index should use the `unicode61` tokenizer
 * (case-folds Cyrillic and folds ё→е).
 */
object SearchQueryParser {
    const val MAX_TERMS = 8

    private val FTS_KEYWORDS = setOf("and", "or", "not", "near")

    private val FILLER = setOf(
        // RU
        "где", "когда", "как", "что", "чем", "какой", "какая", "какие", "какое", "каком", "какую", "у", "меня", "мне",
        "мой", "мои", "моя", "мое", "мою", "моих", "было", "был", "была", "были", "есть", "про", "о", "об", "обо", "по",
        "найди", "найти", "найдите", "покажи", "показать", "покажите", "поиск", "искать", "ищу", "нужно", "надо",
        "хочу", "тема", "теме", "тему", "темы", "фото", "фотка", "нибудь", "то", "либо", "там", "это", "этом",
        "писал", "писала", "записывал", "записывала", "учили", "проходили", "было", "вроде", "кажется",
        // UK
        "мене", "мені", "було", "знайди", "покажи", "де",
        // EN
        "where", "did", "i", "have", "had", "my", "me", "about", "notes", "note", "notebook", "find", "show", "search",
        "something", "anything", "info", "information", "photo", "photos", "picture", "page", "pages", "topic",
        "lesson", "wrote", "written", "write", "learned", "studied", "stuff", "thing", "things", "look", "looking",
        "for", "was", "were", "is", "are", "the", "a", "an", "on", "in", "of", "with", "please",
    )

    /** Stem prefixes of filler words with many inflections ("конспектах", "записях", "заметки"). */
    private val FILLER_PREFIXES = listOf(
        "конспект", "запис", "заметк", "заметок", "тетрад", "фотк", "фотограф", "снимк", "скрин", "материал",
    )

    fun terms(raw: String): List<String> {
        val cleaned = buildString(raw.length) {
            for (c in TextNorm.normalize(raw)) append(if (c.isLetterOrDigit()) c else ' ')
        }
        val out = LinkedHashSet<String>()
        for (tok in cleaned.split(' ')) {
            if (tok.isEmpty() || tok in FTS_KEYWORDS || tok in FILLER || tok in Stopwords.ALL) continue
            if (FILLER_PREFIXES.any { tok.startsWith(it) }) continue
            val stem = Stemmer.stem(tok)
            if (stem.length < 2) continue
            out += stem
            if (out.size >= MAX_TERMS) break
        }
        return out.toList()
    }

    fun ftsAnd(raw: String): String? = terms(raw).takeIf { it.isNotEmpty() }?.joinToString(" ") { "$it*" }

    fun ftsOr(raw: String): String? = terms(raw).takeIf { it.isNotEmpty() }?.joinToString(" OR ") { "$it*" }
}

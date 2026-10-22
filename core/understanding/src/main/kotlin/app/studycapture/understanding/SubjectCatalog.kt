package app.studycapture.understanding

import app.studycapture.understanding.subject.SubjectLexicon

/** Canonical subject keys (English). The UI localizes them with [displayName]. */
object SubjectCatalog {
    val subjects: List<String> = SubjectLexicon.SUBJECTS.map { it.name }

    /** Canonical topic keys per subject, e.g. "Biology" → ["Cell Biology", "Genetics", …]. */
    val topics: Map<String, List<String>> = SubjectLexicon.SUBJECTS.associate { s ->
        s.name to s.topics.map { it.name }.filter { it != SubjectLexicon.GENERAL }
    }

    private val RU = mapOf(
        "Mathematics" to "Математика", "Physics" to "Физика", "Chemistry" to "Химия", "Biology" to "Биология",
        "History" to "История", "Geography" to "География", "Literature" to "Литература",
        "English" to "Английский язык", "Russian" to "Русский язык", "Computer Science" to "Информатика",
        "Social Studies" to "Обществознание",
        "Algebra" to "Алгебра", "Geometry" to "Геометрия", "Calculus" to "Математический анализ",
        "Trigonometry" to "Тригонометрия", "Probability and Statistics" to "Теория вероятностей и статистика",
        "Mechanics" to "Механика", "Electricity and Magnetism" to "Электричество и магнетизм", "Optics" to "Оптика",
        "Thermodynamics" to "Термодинамика", "Atomic Physics" to "Атомная физика",
        "General Chemistry" to "Общая химия", "Organic Chemistry" to "Органическая химия",
        "Cell Biology" to "Цитология", "Genetics" to "Генетика", "Botany" to "Ботаника", "Zoology" to "Зоология",
        "Human Anatomy" to "Анатомия человека", "Ecology" to "Экология", "Evolution" to "Эволюция",
        "Ancient History" to "История Древнего мира", "Medieval History" to "История Средних веков",
        "Modern History" to "Новая история", "20th Century" to "XX век", "Russian History" to "История России",
        "Physical Geography" to "Физическая география", "Human Geography" to "Экономическая география",
        "Poetry" to "Поэзия", "Prose" to "Проза", "Literary Theory" to "Теория литературы",
        "English Grammar" to "Грамматика", "English Vocabulary" to "Лексика",
        "Orthography" to "Орфография", "Morphology" to "Морфология", "Syntax and Punctuation" to "Синтаксис и пунктуация",
        "Algorithms" to "Алгоритмы", "Programming" to "Программирование", "Information and Data" to "Информация и данные",
        "Economics" to "Экономика", "Law" to "Право", "Politics" to "Политика", "Society" to "Общество",
    )

    /** Localized name of a subject/topic key; only "ru" is translated for now, else the key itself. */
    fun displayName(key: String, languageCode: String): String = if (languageCode == "ru") RU[key] ?: key else key
}

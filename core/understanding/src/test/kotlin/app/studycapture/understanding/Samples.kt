package app.studycapture.understanding

import app.studycapture.model.BBox
import app.studycapture.model.OcrLine

/** Realistic school material used across tests. */
object Samples {
    /**
     * Builds OCR lines with plausible boxes: each spec is "text", "#text" (heading, taller) or
     * "?text" (low confidence 0.31). Width grows with text length (16 px/char, max 960).
     */
    fun ocr(vararg specs: String): List<OcrLine> {
        var y = 40
        return specs.map { spec ->
            val heading = spec.startsWith("#")
            val low = spec.startsWith("?")
            val text = if (heading || low) spec.substring(1) else spec
            val h = if (heading) 48 else 30
            val box = BBox(40, y, 40 + minOf(960, text.length * 16), y + h)
            y += h + 10
            OcrLine(text, if (low) 0.31f else 0.93f, box)
        }
    }

    val RU_BIOLOGY_OCR = ocr(
        "#МИТОЗ",
        "Митоз — это способ деления эукариотических клеток, при котором",
        "из одной материнской клетки образуются две дочерние клетки",
        "с идентичным набором хромосом.",
        "Фазы митоза:",
        "1. Профаза — хромосомы спирализуются, ядерная оболочка распадается.",
        "2. Метафаза — хромосомы выстраиваются по экватору клетки.",
        "3. Анафаза — сестринские хроматиды расходятся к полюсам клетки.",
        "4. Телофаза — образуются два новых ядра, делится цитоплазма.",
        "Биологическое значение митоза состоит в том, что дочерние клетки",
        "получают точную копию генетической информации материнской клетки.",
        "Благодаря митозу происходит рост организма и восстановление",
        "повреждённых тканей.",
        "Важно: перед митозом в интерфазе происходит удвоение ДНК.",
        "?Цитокинез — разделение цитоплазмы между дочерними клетками.",
        "Рис. 12. Схема митоза",
    )

    val RU_ALGEBRA = """
        Квадратные уравнения
        Квадратным уравнением называется уравнение вида ax² + bx + c = 0, где a ≠ 0.
        Числа a, b и c называются коэффициентами квадратного уравнения.
        Дискриминант: D = b² − 4ac
        Если D > 0, то уравнение имеет два различных действительных корня.
        Если D = 0, то уравнение имеет один корень.
        Если D < 0, то уравнение не имеет действительных корней.
        Формула корней:
        x₁,₂ = (−b ± √D) / 2a
        Пример 1. Решите уравнение x² − 5x + 6 = 0.
        Задача № 12. Найдите корни уравнения 2x² + 3x − 2 = 0.
    """.trimIndent()

    val EN_BIOLOGY = """
        Photosynthesis
        Photosynthesis is the process by which green plants use sunlight to make glucose from carbon dioxide and water.
        It takes place in the chloroplasts of leaf cells, which contain the green pigment chlorophyll.
        6CO₂ + 6H₂O → C₆H₁₂O₆ + 6O₂
        Oxygen is released as a by-product of photosynthesis.

        Cell division
        Mitochondria are organelles that release energy from glucose during cellular respiration.
        Mitosis is a type of cell division that produces two genetically identical daughter cells.
        Stages of mitosis:
        - Prophase
        - Metaphase
        - Anaphase
        - Telophase
    """.trimIndent()

    val EN_PHYSICS = """
        Newton's Second Law
        F = ma
        Here F is the net force acting on the body, m is its mass and a is its acceleration.
        The acceleration of a body is directly proportional to the net force acting on it.
        The acceleration is inversely proportional to the mass of the body.
        Force is measured in newtons (N).
    """.trimIndent()

    val RU_HISTORY = """
        Отечественная война 1812 года
        В июне 1812 года армия Наполеона перешла реку Неман и вторглась в пределы Российской империи. Русские армии под командованием Барклая-де-Толли и Багратиона отступали вглубь страны. Главнокомандующим русской армией был назначен М. И. Кутузов. 26 августа 1812 года произошло Бородинское сражение, одна из крупнейших битв XIX века. После сражения Кутузов оставил Москву, чтобы сохранить армию. В декабре 1812 года остатки французской армии покинули Россию.
    """.trimIndent()

    val RU_TABLE = """
        Фазы митоза
        Фаза | Что происходит
        Профаза | спирализация хромосом
        Метафаза | хромосомы на экваторе
        Анафаза | расхождение хроматид
    """.trimIndent()
}

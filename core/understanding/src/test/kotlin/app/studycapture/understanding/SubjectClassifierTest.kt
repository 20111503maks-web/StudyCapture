package app.studycapture.understanding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubjectClassifierTest {
    private val clf = SubjectClassifier()

    private fun top(text: String, c: SubjectClassifier = clf) = c.suggest(text).firstOrNull()

    @Test
    fun catalogIsCanonical() {
        assertEquals(
            listOf(
                "Mathematics", "Physics", "Chemistry", "Biology", "History", "Geography", "Literature", "English",
                "Russian", "Computer Science", "Social Studies",
            ),
            SubjectCatalog.subjects,
        )
        assertTrue("Cell Biology" in SubjectCatalog.topics.getValue("Biology"))
        assertEquals("Биология", SubjectCatalog.displayName("Biology", "ru"))
        assertEquals("Biology", SubjectCatalog.displayName("Biology", "en"))
    }

    @Test
    fun schoolMaterialGetsTheRightSubjectAndTopic() {
        val alg = top(Samples.RU_ALGEBRA)!!
        assertEquals("Mathematics", alg.subject)
        assertEquals("Algebra", alg.topic)
        assertTrue("дискриминант" in alg.matchedKeywords)

        val bio = top(StructureAnalyzer.analyze(Samples.RU_BIOLOGY_OCR).joinToString("\n") { it.text })!!
        assertEquals("Biology" to "Cell Biology", bio.subject to bio.topic)

        assertEquals("Biology", top(Samples.EN_BIOLOGY)!!.subject)
        val phys = top(Samples.EN_PHYSICS)!!
        assertEquals("Physics" to "Mechanics", phys.subject to phys.topic)
        assertEquals("History", top(Samples.RU_HISTORY)!!.subject)
        assertEquals("Physics", top("Второй закон Ньютона: ускорение тела прямо пропорционально равнодействующей силе.")!!.subject)
        assertEquals("Chemistry", top("Кислоты реагируют с основаниями, образуя соль и воду. Оксид натрия взаимодействует с водой.")!!.subject)
        assertEquals("Geography", top("Климат Евразии: материк расположен во всех климатических поясах северного полушария, рельеф разнообразен.")!!.subject)
        assertEquals("Literature", top("В стихотворении Пушкина используются эпитеты и метафоры, лирический герой размышляет о свободе.")!!.subject)
        assertEquals("Russian", top("Причастный оборот обособляется запятыми, если стоит после определяемого существительного.")!!.subject)
        assertEquals("English", top("Present Perfect: the auxiliary verb have plus the past participle. Irregular verbs must be learned.")!!.subject)
        assertEquals("Computer Science", top("Алгоритм сортировки пузырьком: в цикле сравниваем соседние элементы массива.")!!.subject)
        assertEquals("Social Studies", top("Рыночная экономика основана на спросе и предложении, конкуренции и свободе цен. Налоги формируют бюджет.")!!.subject)
    }

    @Test
    fun scoresAreSortedBoundedAndTopNRespected() {
        val s = clf.suggest("Клетка, митоз, хромосомы. Ускорение и сила по Ньютону. Дискриминант уравнения.", topN = 3)
        assertTrue(s.size in 2..3)
        assertEquals(s.sortedByDescending { it.score }, s)
        assertTrue(s.all { it.score in 0.0..1.0 })
        assertEquals(1, clf.suggest(Samples.RU_ALGEBRA, topN = 1).size)
    }

    @Test
    fun unrelatedOrEmptyTextGivesNoSuggestion() {
        assertTrue(clf.suggest("").isEmpty())
        assertTrue(clf.suggest("Купить хлеб, молоко и позвонить бабушке вечером.").isEmpty())
        assertTrue(clf.suggest("Meeting with friends on Saturday at the park.").isEmpty())
    }

    @Test
    fun historyTeachesTheClassifierCustomSubjectsAndTopics() {
        val history = listOf(
            LabeledText("Astronomy", "Solar System", "Планеты Солнечной системы: Меркурий, Венера, Земля, Марс обращаются вокруг Солнца."),
            LabeledText("Astronomy", "Solar System", "Юпитер и Сатурн — планеты-гиганты, у Сатурна есть кольца."),
            LabeledText("Astronomy", "Stars", "Звёзды рождаются в туманностях, Солнце — жёлтый карлик."),
            LabeledText("Biology", "Botany", "Фотосинтез происходит в хлоропластах листьев растений."),
        )
        val learned = SubjectClassifier(history)
        val noHistory = clf.suggest("Марс — четвёртая планета Солнечной системы, у него два спутника.")
        assertTrue(noHistory.none { it.subject == "Astronomy" })
        val s = learned.suggest("Марс — четвёртая планета Солнечной системы, у него два спутника.").first()
        assertEquals("Astronomy", s.subject)
        assertEquals("Solar System", s.topic)
        assertTrue(s.matchedKeywords.isNotEmpty())
        // Unrelated text is still classified by the lexicon, not pulled towards history.
        assertEquals("Mathematics", learned.suggest(Samples.RU_ALGEBRA).first().subject)
    }
}

package app.studycapture.understanding.subject

/**
 * Keyword lexicon per school subject and topic (EN + RU, some UK/DE/FR/ES).
 *
 * Keyword syntax (all lowercase, ё written as е): `stem` matches any token starting with it;
 * `=word` matches only that exact token; a leading `!` doubles the weight (very specific terms,
 * e.g. "дискриминант", "mitosis"). Stems are chosen to avoid cross-subject collisions
 * ("хлор" would hit "хлоропласт", so chemistry uses "=хлор"/"хлорид").
 */
internal object SubjectLexicon {
    class Keyword(val text: String, val exact: Boolean, val weight: Double) {
        fun matches(token: String): Boolean = if (exact) token == text else token.startsWith(text)
    }

    class Topic(val name: String, val keywords: List<Keyword>)

    class Subject(val name: String, val topics: List<Topic>)

    private fun kw(spec: String): List<Keyword> = spec.trim().split(Regex("\\s+")).map { raw ->
        var s = raw
        val strong = s.startsWith("!").also { if (it) s = s.substring(1) }
        val exact = s.startsWith("=").also { if (it) s = s.substring(1) }
        Keyword(s, exact, if (strong) 2.0 else 1.0)
    }

    private fun subject(name: String, vararg topics: Pair<String, String>) =
        Subject(name, topics.map { (t, spec) -> Topic(t, kw(spec)) })

    /** Topic named [GENERAL] carries subject-wide words and is never reported as a topic. */
    const val GENERAL = ""

    val SUBJECTS: List<Subject> = listOf(
        subject(
            "Mathematics",
            GENERAL to "!математик !mathemat теорем theorem =число =числа чисел !рівнян",
            "Algebra" to """!уравнен !дискриминант !квадратн корн корен !многочлен одночлен неравенств коэффициент
                !алгебр степен !логарифм !парабол линейн дроб числител знаменател пропорци !виет
                !equation !quadratic !discriminant =root roots !polynomial monomial inequalit coefficient !algebra
                exponent !logarithm !parabola linear fraction numerator denominator proportion !vieta factoriz
                !gleichung !équation !ecuaci""",
            "Geometry" to """!треугольник !окружност !гипотенуз !катет =угол =угла =углы =углов !биссектрис !параллелограмм
                !трапеци !ромб !периметр площад !пифагор !вектор многоугольник =хорда =хорды хордой радиус диаметр перпендикуляр
                !triangle !circle !hypotenuse angle !bisector !parallelogram !trapezoid !rhombus perimeter
                !pythagor polygon !chord radius diameter !congruen perpendicular""",
            "Calculus" to """!производн !интеграл =предел !дифференц !первообразн экстремум
                !derivative !integral =limit limits !differenti !antiderivative extremum""",
            "Trigonometry" to """!синус !косинус !тангенс !котангенс !тригонометр радиан
                !sine !cosine !tangent !trigonometr radian =sin =cos =tg =tan""",
            "Probability and Statistics" to """!вероятност !статистик !комбинатор !перестановк сочетани !дисперси
                !probabilit !statistic combinator permutation variance =mean""",
        ),
        subject(
            "Physics",
            GENERAL to "!физик !physic !фізик энерги energy",
            "Mechanics" to """!ньютон =сила =силы =силу =силой !ускорен скорост !импульс !инерци =масса =массы массой
                !кинематик динамик !кинетическ потенциальн =трение =трения трением !гравитац !тяготен перемещен траектори !механик
                !newton =force forces !accelerat velocity !momentum !inertia =mass !kinematic dynamics kinetic
                friction !gravit displacement trajectory !mechanic""",
            "Electricity and Magnetism" to """!электр =ток =тока =току током !напряжен !сопротивлен !заряд =ом !ампер
                !вольт =цепь =цепи !конденсатор !магнит !electric =current !voltage !resistance charge !ohm
                !ampere !volt circuit !capacitor !magnet""",
            "Optics" to """!оптик !линз !преломлен !отражен свет =луч лучи !фокусн
                !optic !lens !refraction !reflection light""",
            "Thermodynamics" to """!термодинам !теплот температур !теплопровод !энтропи
                !thermodynam heat temperature !entropy""",
            "Atomic Physics" to """!квант !фотон !радиоактив !ядерн !quantum !photon !radioactiv !nuclear""",
        ),
        subject(
            "Chemistry",
            GENERAL to "!хими !chemistr !chemical !хімі !периодическ !periodic !менделеев !mendeleev",
            "General Chemistry" to """!молекул =атом атомы атомов атомн !реакци !валентност !кислот !щелоч !оксид
                =моль молярн раствор =ион =ионы ионн !электролит !окислен восстановител !катализ =соль =соли солей
                !гидроксид !водород кислород !углерод !азот !натри =хлор хлорид !изотоп
                !molecul =atom atoms atomic !reaction !valence !acid !alkali !oxide =mole moles molar solution
                =ion ions ionic !electrolyte !oxidation !catalyst =salt salts !hydroxide !hydrogen oxygen carbon
                !nitrogen !sodium !chlorine !isotope""",
            "Organic Chemistry" to """!органическ !углеводород !алкан !алкен !алкин !спирт !эфир !бензол !полимер
                !organic !hydrocarbon !alkane !alkene !alkyne !alcohol !ester !benzene !polymer""",
        ),
        subject(
            "Biology",
            GENERAL to "!биолог !biolog !организм !organism !species",
            "Cell Biology" to """!клетк !клеток !митоз !мейоз !митохондри !хромосом =ядро ядра !цитоплазм !мембран
                !органоид !органелл !рибосом !хлоропласт !вакуол !эукариот !прокариот !профаз !метафаз !анафаз
                !телофаз !интерфаз !веретен !клітин
                !cell !mitosis !meiosis !mitochondri !chromosom nucleus !cytoplasm !membrane !organelle !ribosom
                !chloroplast !vacuole !eukaryot !prokaryot !prophase !metaphase !anaphase !telophase !interphase
                !spindle !zelle !cellule !célul""",
            "Genetics" to """=ген =гены генов !генетик =днк =рнк !наследствен !аллел !доминантн !рецессивн !мутаци
                !генотип !фенотип =мендель =менделя менделем =mendel mendelian =gene genes !genetic =dna =rna !heredit !allele !dominant !recessive
                !mutation !genotype !phenotype !mendel""",
            "Botany" to """!фотосинтез !растени !хлорофилл =лист =листья листьев !корнев !стебел цветок цветк !семен
                !опылен !photosynthes !plant !chlorophyll leaf leaves !flower !seed !pollinat""",
            "Zoology" to """!животн !млекопитающ !позвоночн !беспозвоночн !насеком !птиц =рыба =рыбы рыб !земноводн
                !пресмыкающ !animal !mammal !vertebrate !invertebrate !insect bird =fish !amphibian !reptile""",
            "Human Anatomy" to """!анатоми =орган =органы органов !кров !сердц !нервн !пищеварен !дыхан !скелет !мышц
                !гормон !anatomy =organ organs !blood !heart !nervous !digest !respirat !skeleton !muscle !hormone""",
            "Ecology" to """!экосистем !экологи популяци !биоценоз !продуцент !консумент !редуцент
                !ecosystem !ecolog population !decomposer habitat !biome""",
            "Evolution" to """!эволюц !дарвин !отбор !evolution !darwin selection""",
        ),
        subject(
            "History",
            GENERAL to """!истори !history !historical !історі =век =века =веке =веков century centuries !династи
                !dynasty !битв !сражен !battle договор !treaty !восстани !uprising !rebellion !корол =king !queen""",
            "Ancient History" to """!древн !античн !фараон египет !римск =полис =полиса полисы !ancient !pharaoh !roman !antiquity""",
            "Medieval History" to """!средневеков !феодал !рыцар !крестов !княз !княжеств !киевск !монгол =орда =орды
                !medieval !feudal !knight !crusade""",
            "Modern History" to """!революци !индустриальн !колони !наполеон !просвещен !revolution !industrial
                !colonial !colonies !colony !napoleon !enlightenment""",
            "20th Century" to """=война =войны =войне войн мировой мировая !ссср !советск !сталин !ленин !большевик
                !фашизм !гитлер =war wars !soviet !stalin !lenin !bolshevik !fascis !nazi !hitler""",
            "Russian History" to """!=царь !=царя !=царем =цари царей =петр !импери !император !крепостн !декабрист реформ !самодержав
                !tsar !czar !emperor !serf !decembrist""",
        ),
        subject(
            "Geography",
            GENERAL to "!географ !geograph =карта =карты map maps",
            "Physical Geography" to """!климат !материк !континент !океан !рельеф !широт !долгот !меридиан !экватор
                !равнин !вулкан !землетрясен атмосфер !гидросфер !литосфер !осадк !муссон !пассат =река =реки
                =рек реками !озер !пустын !тундр !тайг !саванн
                !climate !continent !ocean relief !latitude !longitude !meridian !equator plain !volcano
                !earthquake atmosphere !hydrosphere !lithosphere precipitation !monsoon river lake !desert
                !tundra !taiga !savanna""",
            "Human Geography" to """!населени !демограф !урбанизац !миграци !ископаем !сельскохозяйствен
                population !demograph !urbaniz !migration mineral !agricultur""",
        ),
        subject(
            "Literature",
            GENERAL to "!литератур !literature !literary !пушкин !лермонтов !достоевск !гогол !чехов !тургенев !онегин !shakespeare !dickens !hamlet",
            "Poetry" to """!стихотворен !стих =поэт =поэта =поэты поэтов поэтом !поэтесс !поэтическ !поэм !рифм !строф !ямб !хорей !лирик !лирическ
                !poem !poet !rhyme !stanza !verse !sonnet !lyric""",
            "Prose" to """роман !повест рассказ !персонаж =герой =героя героев =герои !сюжет композици автор
                !novel story !protagonist !plot !narrator author character""",
            "Literary Theory" to """!эпитет !метафор !олицетворен !аллегори !жанр !эпос !драм !комеди !трагеди !ирони
                !epithet !metaphor !personification !allegory !genre !drama !comedy !tragedy !irony""",
        ),
        subject(
            "English",
            GENERAL to "!english !английск",
            "English Grammar" to """!tense !irregular !verb !noun !adjective !adverb !pronoun !preposition article
                !passive !conditional !modal !gerund !infinitive !auxiliary participle =simple continuous
                perfect !grammar""",
            "English Vocabulary" to """!vocabulary !phrasal !idiom !synonym !antonym translation перевод""",
        ),
        subject(
            "Russian",
            GENERAL to "!лингвист !фонетик !лексик !лексическ =русский",
            "Orthography" to """!орфограф !орфограмм !правописан !безударн !ударени !гласн !=согласные !=согласных !=согласный !приставк
                !суффикс окончани""",
            "Morphology" to """!морфолог !существительн !прилагательн !глагол !наречи !местоимени !причасти
                !деепричасти !числительн !падеж !склонени !спряжени частиц предлог !междомети""",
            "Syntax and Punctuation" to """!синтаксис !пунктуац !подлежащ !сказуем дополнени обстоятельств !запят
                !тире !двоеточи !сложносочинен !сложноподчинен !бессоюзн !обособлен !однородн вводн
                !словосочетани предложени""",
        ),
        subject(
            "Computer Science",
            GENERAL to "!информатик !informatics !computer !компьютер",
            "Algorithms" to """!алгоритм !сортировк !рекурси !algorithm sorting !recursion !recursive complexity""",
            "Programming" to """программ переменн цикл !массив !python !java !javascript !kotlin pascal функци
                !оператор !компилятор !programming program variable =loop loops !array function !compiler
                syntax string integer !boolean""",
            "Information and Data" to """информаци =бит =бита битов !байт !двоичн !кодирован !процессор =сеть
                =bit =bits !byte !binary !encoding !processor !database !network !internet""",
        ),
        subject(
            "Social Studies",
            GENERAL to "!обществознан !обществ !социальн !society !social",
            "Economics" to """!экономик !рынок рынк !спрос !инфляци !бюджет !налог !деньг !банк !конкуренц
                !economy !economic !market !demand !supply !inflation !budget =tax =taxes taxation !money !bank !competition""",
            "Law" to """=право =права правов закон !конституц =суд =суда судебн !преступлен граждан
                law !constitution !court !crime !citizen !rights !legal""",
            "Politics" to """государств власт !демократи !выбор !парламент политик !президент
                !government state !democracy !election !parliament !politic !president""",
            "Society" to """!социализац культур !личност =семья =семьи =семье !culture !personality family""",
        ),
    )
}

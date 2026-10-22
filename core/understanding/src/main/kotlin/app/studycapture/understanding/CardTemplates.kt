package app.studycapture.understanding

/** Fixed question templates per UI language. Only the wrapper is ours; %s is material text. */
internal class CardTemplates(
    private val whatIs: String,
    private val whatAre: String,
    private val formula: String,
    private val formulaFor: String,
    private val list: String,
    private val define: String,
) {
    fun whatIs(term: String, plural: Boolean) = (if (plural) whatAre else whatIs).format(term)
    fun formula(context: String) = formula.format(context)
    fun formulaFor(lhs: String) = formulaFor.format(lhs)
    fun list(heading: String) = list.format(heading)
    fun define(heading: String) = define.format(heading)

    companion object {
        private val BY_LANG = mapOf(
            "ru" to CardTemplates("Что такое %s?", "Что такое %s?", "Формула: %s", "Формула для %s", "Перечислите: %s", "Дайте определение: %s"),
            "uk" to CardTemplates("Що таке %s?", "Що таке %s?", "Формула: %s", "Формула для %s", "Перелічіть: %s", "Дайте визначення: %s"),
            "en" to CardTemplates("What is %s?", "What are %s?", "Formula: %s", "Formula for %s", "List: %s", "Define: %s"),
            "de" to CardTemplates("Was ist %s?", "Was sind %s?", "Formel: %s", "Formel für %s", "Nennen Sie: %s", "Definieren Sie: %s"),
            "fr" to CardTemplates("Qu'est-ce que %s ?", "Que sont %s ?", "Formule : %s", "Formule pour %s", "Énumérez : %s", "Définissez : %s"),
            "es" to CardTemplates("¿Qué es %s?", "¿Qué son %s?", "Fórmula: %s", "Fórmula para %s", "Enumere: %s", "Defina: %s"),
        )

        fun forLanguage(code: String): CardTemplates = BY_LANG[code] ?: BY_LANG.getValue("en")
    }
}

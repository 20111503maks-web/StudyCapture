package app.studycapture.understanding.text

/** Function-word lists per language (normalized: lowercase, ё→е). */
internal object Stopwords {
    private fun set(s: String): Set<String> = s.trim().split(Regex("\\s+")).toSet()

    val RU = set(
        """
        и в во не что он на я с со как а то все всё она так его но да ты к у же вы за бы по только ее мне
        было вот от меня еще нет о из ему теперь когда даже ну вдруг ли если уже или ни быть был него до
        вас нибудь опять уж вам ведь там потом себя ничего ей может они тут где есть надо ней для мы тебя
        их чем была сам чтоб без будто чего раз тоже себе под будет ж тогда кто этот того потому этого какой
        совсем ним здесь этом один почти мой тем чтобы нее сейчас были куда зачем всех никогда можно при
        наконец два об другой хоть после над больше тот через эти нас про всего них какая много разве три
        эту моя впрочем хорошо свою этой перед иногда лучше чуть том нельзя такой им более всегда конечно
        всю между это эта является являются также который которая которое которые которых котором которой
        которую которым свой своих своей свои своего такие такая такое также либо ещё обо ко но весь вся
        всем всеми каждый очень будут будем мои мое моё нам наш наша наши ваш ваша
        """,
    )
    val UK = set(
        """
        і й та в у на з із зі до що як це не але або для від по за при про він вона воно вони ми ви я ти є
        був була було були бути цей ця ці цього той яка який які яке також тому якщо вже ще лише його її їх
        так ні чи де коли тут там щоб бо же ж мене мені нас вас усі всі все
        """,
    )
    val EN = set(
        """
        a an the and or but if then else of to in on at by for with from into onto about as is are was were
        be been being am do does did doing have has had having i me my mine we us our you your he him his
        she her it its they them their this that these those there here which who whom whose what when where
        why how all any both each few more most other some such no nor not only own same so than too very
        can will just should would could may might must shall also not up down out over under again further
        once s t don't it's i'm via per e g eg ie etc
        """,
    )
    val DE = set(
        """
        der die das und ist sind ein eine einer eines einem einen zu von mit den dem des nicht auf für im in
        es sich auch als an wie oder aber bei aus nach wird werden war hat haben ich du er sie wir ihr man
        dass so am vom zum zur um noch nur kann
        """,
    )
    val FR = set(
        """
        le la les de des du un une et est sont en dans que qui pour par sur avec au aux ce cette ces il elle
        ils elles nous vous je tu ne pas plus ou mais se son sa ses leur être a ont été comme on l d qu c
        """,
    )
    val ES = set(
        """
        el la los las de del y es son un una unos unas en que por para con se su sus al lo como más pero o
        este esta estos estas ese esa ser fue está están no le les ya muy también e
        """,
    )

    val BY_LANG: Map<String, Set<String>> = mapOf("ru" to RU, "uk" to UK, "en" to EN, "de" to DE, "fr" to FR, "es" to ES)

    val ALL: Set<String> = RU + UK + EN + DE + FR + ES
}

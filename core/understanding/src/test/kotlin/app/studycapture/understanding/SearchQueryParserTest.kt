package app.studycapture.understanding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchQueryParserTest {
    @Test
    fun naturalRussianQuestion() {
        val terms = SearchQueryParser.terms("где у меня было про митохондрии?")
        assertEquals(1, terms.size)
        val t = terms.single()
        assertTrue("митохондрии".startsWith(t) && "митохондрия".startsWith(t) && "митохондриями".startsWith(t), t)
        assertTrue(t.length >= 6)
        assertEquals("$t*", SearchQueryParser.ftsAnd("где у меня было про митохондрии?"))
    }

    @Test
    fun naturalEnglishQuestion() {
        val terms = SearchQueryParser.terms("Where did I have my photosynthesis notes?")
        assertEquals(listOf("photosynthes"), terms)
        assertEquals(listOf("newton", "law"), SearchQueryParser.terms("find notes about Newton's laws"))
    }

    @Test
    fun severalTermsAndOrFallback() {
        val q = "Покажи конспекты про квадратные уравнения и дискриминант"
        val terms = SearchQueryParser.terms(q)
        assertEquals(3, terms.size)
        assertTrue("квадратные".startsWith(terms[0]) && "квадратного".startsWith(terms[0]))
        assertTrue("уравнений".startsWith(terms[1]))
        assertEquals("дискриминант", terms[2])
        assertEquals(terms.joinToString(" ") { "$it*" }, SearchQueryParser.ftsAnd(q))
        assertEquals(terms.joinToString(" OR ") { "$it*" }, SearchQueryParser.ftsOr(q))
        // ё folds to е, case is ignored.
        assertEquals(SearchQueryParser.terms("ЁЛКА"), SearchQueryParser.terms("елка"))
    }

    @Test
    fun ftsSyntaxCannotBeInjected() {
        assertEquals("foo*", SearchQueryParser.ftsAnd("foo\" OR *"))
        assertEquals("foo*", SearchQueryParser.ftsOr("foo\" OR *"))
        for (evil in listOf("\"a\" NEAR/2 b*", "x:y ^z (w) -v 'u'", "митоз\" AND NOT \"мейоз", "col:* OR \"\"")) {
            val m = SearchQueryParser.ftsAnd(evil) ?: continue
            assertTrue(Regex("[\\p{L}\\d]+\\*( [\\p{L}\\d]+\\*)*").matches(m), "unsafe MATCH: $m")
        }
        assertNull(SearchQueryParser.ftsAnd("\" OR * AND NOT"))
    }

    @Test
    fun onlyStopwordsOrEmptyGiveNull() {
        assertNull(SearchQueryParser.ftsAnd(""))
        assertNull(SearchQueryParser.ftsOr("где у меня было?"))
        assertTrue(SearchQueryParser.terms("where did I have it").isEmpty())
    }

    @Test
    fun termCountIsCappedAndNumbersKept() {
        val many = "клетка ядро мембрана рибосома митохондрия хлоропласт вакуоль лизосома центриоль цитоплазма"
        assertEquals(SearchQueryParser.MAX_TERMS, SearchQueryParser.terms(many).size)
        assertEquals(listOf("1812", "войн"), SearchQueryParser.terms("война 1812").sorted())
    }
}

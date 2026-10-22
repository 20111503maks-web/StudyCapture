package app.studycapture.srs

/**
 * Minimal JSON reader for the test vectors (the module deliberately has no JSON dependency).
 * Objects become [Map], arrays [List], numbers [Double] or [Long], plus String/Boolean/null.
 */
internal class MiniJson private constructor(private val s: String) {
    private var i = 0

    companion object {
        fun parse(text: String): Any? = MiniJson(text).run { value().also { ws(); check(i == s.length) } }
    }

    private fun ws() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun value(): Any? {
        ws()
        return when (val c = s[i]) {
            '{' -> obj()
            '[' -> arr()
            '"' -> str()
            't' -> lit("true", true)
            'f' -> lit("false", false)
            'n' -> lit("null", null)
            else -> if (c == '-' || c.isDigit()) num() else error("Unexpected '$c' at $i")
        }
    }

    private fun lit(word: String, v: Any?): Any? {
        check(s.startsWith(word, i)) { "Bad literal at $i" }
        i += word.length
        return v
    }

    private fun obj(): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        i++
        ws()
        if (s[i] == '}') return m.also { i++ }
        while (true) {
            ws()
            val k = str()
            ws()
            check(s[i++] == ':')
            m[k] = value()
            ws()
            if (s[i++] == '}') return m
        }
    }

    private fun arr(): List<Any?> {
        val l = ArrayList<Any?>()
        i++
        ws()
        if (s[i] == ']') return l.also { i++ }
        while (true) {
            l += value()
            ws()
            if (s[i++] == ']') return l
        }
    }

    private fun str(): String {
        check(s[i++] == '"')
        val sb = StringBuilder()
        while (s[i] != '"') {
            val c = s[i++]
            if (c != '\\') { sb.append(c); continue }
            when (val e = s[i++]) {
                'n' -> sb.append('\n')
                't' -> sb.append('\t')
                'u' -> sb.append(s.substring(i, i + 4).toInt(16).toChar()).also { i += 4 }
                else -> sb.append(e)
            }
        }
        i++
        return sb.toString()
    }

    private fun num(): Any {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        val t = s.substring(start, i)
        return if (t.any { it in ".eE" }) t.toDouble() else t.toLong()
    }
}

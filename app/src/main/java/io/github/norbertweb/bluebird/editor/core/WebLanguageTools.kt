package io.github.norbertweb.bluebird.editor.core

/** Lightweight web-language intelligence. Designed to be replaced/augmented by LSP providers. */
object WebLanguageTools {
    data class Match(val start: Int, val end: Int, val label: String)

    private val htmlTag = Regex("</?([A-Za-z][A-Za-z0-9:-]*)\\b[^>]*>")
    private val htmlVoid = setOf("area","base","br","col","embed","hr","img","input","link","meta","param","source","track","wbr")

    fun htmlTagPairs(text: String): List<Match> {
        val stack = ArrayDeque<Pair<String, Int>>()
        val result = mutableListOf<Match>()
        htmlTag.findAll(text).forEach { m ->
            val raw = m.value
            val name = m.groupValues[1].lowercase()
            if (name in htmlVoid || raw.endsWith("/>") || raw.startsWith("<!")) return@forEach
            if (raw.startsWith("</")) {
                val open = stack.removeLastOrNull()
                if (open != null && open.first == name) result += Match(open.second, m.range.last + 1, name)
            } else stack.addLast(name to m.range.first)
        }
        return result
    }

    fun cssDiagnostics(text: String): List<Diagnostic> {
        val out = mutableListOf<Diagnostic>()
        val property = Regex("(?m)(^|[;{}])\\s*([a-zA-Z-]+)\\s*:")
        val known = setOf("display","position","top","right","bottom","left","width","height","margin","padding","color","background","background-color","border","border-radius","font-size","font-weight","line-height","opacity","overflow","z-index","flex","flex-direction","justify-content","align-items","gap","grid","grid-template-columns","grid-template-rows","transform","transition","animation","box-shadow","cursor","content")
        property.findAll(text).forEach { m ->
            val name = m.groupValues[2]
            if (name.lowercase() !in known && !name.startsWith("--")) {
                val before = text.substring(0, m.range.first)
                val line = before.count { it == '\n' } + 1
                val col = m.range.first - (before.lastIndexOf('\n') + 1) + 1
                out += Diagnostic(line, col, DiagnosticSeverity.WARNING, "Unknown or unsupported CSS property '$name'", "CSS")
            }
        }
        return out
    }

    fun javascriptDiagnostics(text: String): List<Diagnostic> {
        val out = mutableListOf<Diagnostic>()
        Regex("(?m)\\bconsole\\.(log|error|warn|info)\\s*\\(").findAll(text).forEach { m ->
            // Intentionally no warning: console calls are useful during development.
        }
        Regex("(?m)\\bvar\\s+[A-Za-z_$][\\w$]*").findAll(text).forEach { m ->
            val before = text.substring(0, m.range.first)
            out += Diagnostic(before.count { it == '\n' } + 1, m.range.first - (before.lastIndexOf('\n') + 1) + 1,
                DiagnosticSeverity.INFO, "Consider const or let instead of var", "JavaScript")
        }
        return out
    }

    fun diagnostics(text: String, fileName: String): List<Diagnostic> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "html", "htm" -> analyzeDiagnostics(text, fileName)
            "css", "scss", "sass", "less" -> (analyzeDiagnostics(text, fileName) + cssDiagnostics(text)).sortedWith(compareBy<Diagnostic>{it.line}.thenBy{it.column}).take(1000)
            "js", "jsx", "mjs", "cjs", "ts", "tsx" -> (analyzeDiagnostics(text, fileName) + javascriptDiagnostics(text)).sortedWith(compareBy<Diagnostic>{it.line}.thenBy{it.column}).take(1000)
            else -> analyzeDiagnostics(text, fileName)
        }
    }
}

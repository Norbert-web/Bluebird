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

    fun diagnostics(text: String, fileName: String): List<Diagnostic> = LanguageServiceRegistry.diagnostics(text, fileName)


    /** Returns the paired HTML tag name range when the cursor is inside a tag pair. */
    fun htmlTagAt(text: String, offset: Int): Pair<IntRange, String>? {
        val cursor = offset.coerceIn(0, text.length)
        return htmlTagPairs(text).firstOrNull { cursor in it.start..it.end }?.let { match ->
            match.start until match.end to match.label
        }
    }

    /** Renames matching HTML tags for editor commands; returns unchanged text when no pair is found. */
    fun renameHtmlTag(text: String, offset: Int, newName: String): String {
        val safeName = newName.trim().takeIf { it.matches(Regex("[A-Za-z][A-Za-z0-9:-]*")) } ?: return text
        val cursor = offset.coerceIn(0, text.length)
        val match = htmlTagPairs(text).firstOrNull { cursor in it.start..it.end } ?: return text
        val rangeText = text.substring(match.start, match.end)
        val oldName = match.label
        val replaced = rangeText
            .replaceFirst(Regex("(<\\s*)$oldName(?=\\s|/?>)", RegexOption.IGNORE_CASE), "$1$safeName")
            .replaceFirst(Regex("(</\\s*)$oldName(?=\\s*>)", RegexOption.IGNORE_CASE), "$1$safeName")
        return text.substring(0, match.start) + replaced + text.substring(match.end)
    }
}

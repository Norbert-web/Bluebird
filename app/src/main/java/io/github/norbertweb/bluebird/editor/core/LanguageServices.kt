package io.github.norbertweb.bluebird.editor.core

/**
 * Provider boundary for language intelligence. A real LSP-backed provider can replace the
 * lightweight providers below without changing editor-facing APIs.
 */
interface LanguageProvider {
    val id: String
    fun supports(fileName: String): Boolean
    fun completions(text: String, cursor: Int, projectWords: List<String>): List<CompletionItem> = emptyList()
    fun diagnostics(text: String, fileName: String): List<Diagnostic> = emptyList()
}


object LanguageServiceRegistry {
    private val providers: List<LanguageIntelligenceProvider> = listOf(
        HtmlLanguageProvider,
        CssLanguageProvider,
        JavaScriptLanguageProvider,
    )

    fun providerFor(fileName: String): LanguageIntelligenceProvider? = providers.firstOrNull { it.supports(fileName) }

    fun completions(text: String, cursor: Int, fileName: String, projectWords: List<String>): List<CompletionItem> {
        val provider = providerFor(fileName)
        val local = provider?.completions(text, cursor, projectWords).orEmpty()
        return (local + projectWords.map { CompletionItem(it, "Workspace symbol") })
            .distinctBy { it.label }
            .take(50)
    }

    fun diagnostics(text: String, fileName: String): List<Diagnostic> {
        val provider = providerFor(fileName)
        return (analyzeDiagnostics(text, fileName) + provider?.diagnostics(text, fileName).orEmpty())
            .sortedWith(compareBy<Diagnostic> { it.line }.thenBy { it.column })
            .take(1000)
    }

    fun symbols(text: String, fileName: String): List<DocumentSymbol> =
        providerFor(fileName)?.symbols(text, fileName).orEmpty().sortedBy { it.offset }

    fun definition(text: String, fileName: String, cursor: Int): DefinitionLocation? =
        providerFor(fileName)?.definition(text, fileName, cursor)

    fun references(text: String, fileName: String, cursor: Int): List<ReferenceLocation> =
        providerFor(fileName)?.references(text, fileName, cursor).orEmpty()

    fun hover(text: String, fileName: String, cursor: Int): HoverInfo? =
        providerFor(fileName)?.hover(text, fileName, cursor)
}

private fun lineColumn(text: String, offset: Int): Pair<Int, Int> {
    val safe = offset.coerceIn(0, text.length)
    val line = text.take(safe).count { it == '\n' } + 1
    val lineStart = text.lastIndexOf('\n', safe - 1).let { if (it < 0) 0 else it + 1 }
    return line to (safe - lineStart + 1)
}

private object HtmlLanguageProvider : LanguageIntelligenceProvider {
    override val id = "html"
    private val tags = listOf(
        "html", "head", "body", "main", "section", "article", "header", "footer", "nav", "aside",
        "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6", "a", "img", "button", "form",
        "input", "label", "textarea", "select", "option", "ul", "ol", "li", "table", "thead", "tbody",
        "tr", "th", "td", "script", "style", "link", "meta", "title", "video", "audio", "canvas", "svg",
    )
    private val attrs = listOf(
        "class", "id", "href", "src", "alt", "title", "style", "type", "name", "value", "placeholder",
        "required", "disabled", "checked", "selected", "target", "rel", "width", "height", "aria-label", "role", "data-testid",
    )
    private val voidTags = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
    private val tagRegex = Regex("</?([A-Za-z][A-Za-z0-9:-]*)\\b[^>]*>")

    override fun supports(fileName: String) = fileName.substringAfterLast('.', "").lowercase() in setOf("html", "htm")

    override fun completions(text: String, cursor: Int, projectWords: List<String>): List<CompletionItem> {
        val p = cursor.coerceIn(0, text.length)
        val prefixStart = CompletionEngine.findPrefixStart(text, p)
        val prefix = text.substring(prefixStart, p)
        val before = text.substring(0, p).takeLast(1000)
        val lastOpen = before.lastIndexOf('<')
        val inTag = lastOpen > before.lastIndexOf('>')
        if (!inTag) return emptyList()
        val inside = before.substring(lastOpen + 1)
        val candidates = when {
            inside.startsWith("/") -> tags.map { CompletionItem(it, "HTML closing tag") }
            inside.contains(' ') -> attrs.map { CompletionItem(it, "HTML attribute") }
            else -> tags.map { CompletionItem(it, if (it in voidTags) "HTML void element" else "HTML element") }
        }
        return candidates.filter { it.label.startsWith(prefix, true) && it.label != prefix }
    }

    override fun symbols(text: String, fileName: String): List<DocumentSymbol> = buildList {
        tagRegex.findAll(text).forEach { match ->
            val raw = match.value
            if (raw.startsWith("</") || raw.startsWith("<!")) return@forEach
            val name = match.groupValues[1]
            val offset = match.range.first + raw.indexOf(name).coerceAtLeast(0)
            val (line, column) = lineColumn(text, offset)
            add(DocumentSymbol(name, SymbolKind.TAG, line, column, offset))
        }
    }

    override fun definition(text: String, fileName: String, cursor: Int): DefinitionLocation? {
        val word = LanguageIntelligence.wordAt(text, cursor)
        return symbols(text, fileName).firstOrNull { it.name.equals(word, true) }?.let {
            DefinitionLocation(fileName, it.line, it.column, it.offset, it.name)
        }
    }

    override fun references(text: String, fileName: String, cursor: Int): List<ReferenceLocation> {
        val word = LanguageIntelligence.wordAt(text, cursor)
        if (word.isEmpty()) return emptyList()
        val regex = Regex("(?<![A-Za-z0-9_:-])" + Regex.escape(word) + "(?![A-Za-z0-9_:-])")
        return regex.findAll(text).map { match ->
            val (line, column) = lineColumn(text, match.range.first)
            ReferenceLocation(fileName, line, column, match.range.first, match.value.length)
        }.toList()
    }

    override fun hover(text: String, fileName: String, cursor: Int): HoverInfo? {
        val word = LanguageIntelligence.wordAt(text, cursor)
        val tag = tags.firstOrNull { it.equals(word, true) } ?: return null
        val (line, column) = lineColumn(text, cursor)
        return HoverInfo(tag, "**HTML element** `<$tag>`", line, column)
    }

    override fun diagnostics(text: String, fileName: String): List<Diagnostic> {
        val result = mutableListOf<Diagnostic>()
        val stack = ArrayDeque<Pair<String, Int>>()
        tagRegex.findAll(text).forEach { match ->
            val raw = match.value
            val name = match.groupValues[1].lowercase()
            val (line, column) = lineColumn(text, match.range.first)
            if (name in voidTags || raw.endsWith("/>") || raw.startsWith("<!")) return@forEach
            if (raw.startsWith("</")) {
                val open = stack.removeLastOrNull()
                if (open == null) result += Diagnostic(line, column, DiagnosticSeverity.ERROR, "Unexpected closing tag </$name>", id)
                else if (open.first != name) result += Diagnostic(line, column, DiagnosticSeverity.ERROR, "Closing tag </$name> does not match <${open.first}>", id)
            } else stack.add(name to match.range.first)
        }
        stack.forEach { (name, offset) ->
            val (line, column) = lineColumn(text, offset)
            result += Diagnostic(line, column, DiagnosticSeverity.ERROR, "Unclosed HTML tag <$name>", id)
        }
        return result
    }
}

private object CssLanguageProvider : LanguageIntelligenceProvider {
    override val id = "css"
    private val props = setOf(
        "display", "position", "top", "right", "bottom", "left", "width", "height", "min-width", "max-width", "min-height", "max-height",
        "margin", "padding", "box-sizing", "color", "background", "background-color", "border", "border-radius", "font-family", "font-size",
        "font-weight", "line-height", "text-align", "text-decoration", "opacity", "overflow", "z-index", "flex", "flex-direction", "flex-wrap",
        "justify-content", "align-items", "align-content", "gap", "grid", "grid-template-columns", "grid-template-rows", "transform", "transition",
        "animation", "box-shadow", "cursor", "content", "visibility", "white-space", "object-fit", "letter-spacing",
    )
    private val values = listOf("block", "inline", "inline-block", "flex", "grid", "none", "auto", "relative", "absolute", "fixed", "sticky", "hidden", "visible", "center", "start", "end", "space-between", "space-around", "space-evenly", "wrap", "nowrap", "solid", "transparent", "inherit", "initial", "unset", "pointer", "ease", "linear")

    override fun supports(fileName: String) = fileName.substringAfterLast('.', "").lowercase() in setOf("css", "scss", "sass", "less")

    override fun completions(text: String, cursor: Int, projectWords: List<String>): List<CompletionItem> {
        val p = cursor.coerceIn(0, text.length)
        val prefixStart = CompletionEngine.findPrefixStart(text, p)
        val prefix = text.substring(prefixStart, p)
        val context = text.substring(0, p).takeLast(1000)
        if (context.lastIndexOf('{') <= context.lastIndexOf('}')) return emptyList()
        val declaration = context.substringAfterLast('{')
        val valueMode = declaration.lastIndexOf(':') > declaration.lastIndexOf(';')
        val source = if (valueMode) values.map { CompletionItem(it, "CSS value") } else props.map { CompletionItem(it, "CSS property") }
        return source.filter { it.label.startsWith(prefix, true) && it.label != prefix }
    }

    override fun symbols(text: String, fileName: String): List<DocumentSymbol> = buildList {
        Regex("(?m)(^|[}])\\s*([^{}]+)\\{").findAll(text).forEach { match ->
            val selector = match.groupValues[2].trim()
            if (selector.isEmpty()) return@forEach
            val relative = match.value.indexOf(selector).coerceAtLeast(0)
            val offset = match.range.first + relative
            val (line, column) = lineColumn(text, offset)
            add(DocumentSymbol(selector, SymbolKind.SELECTOR, line, column, offset))
        }
    }

    override fun definition(text: String, fileName: String, cursor: Int): DefinitionLocation? {
        val word = LanguageIntelligence.wordAt(text, cursor)
        return symbols(text, fileName).firstOrNull { it.name.contains(word, true) }?.let {
            DefinitionLocation(fileName, it.line, it.column, it.offset, it.name)
        }
    }

    override fun references(text: String, fileName: String, cursor: Int): List<ReferenceLocation> {
        val word = LanguageIntelligence.wordAt(text, cursor)
        if (word.isEmpty()) return emptyList()
        return Regex("(?<![A-Za-z0-9_-])" + Regex.escape(word) + "(?![A-Za-z0-9_-])").findAll(text).map { match ->
            val (line, column) = lineColumn(text, match.range.first)
            ReferenceLocation(fileName, line, column, match.range.first, match.value.length)
        }.toList()
    }

    override fun hover(text: String, fileName: String, cursor: Int): HoverInfo? {
        val word = LanguageIntelligence.wordAt(text, cursor)
        val property = props.firstOrNull { it.equals(word, true) } ?: return null
        val (line, column) = lineColumn(text, cursor)
        return HoverInfo(property, "**CSS property** `$property`", line, column)
    }

    override fun diagnostics(text: String, fileName: String): List<Diagnostic> {
        val result = mutableListOf<Diagnostic>()
        Regex("(?m)(^|[;{}])\\s*([a-zA-Z-]+)\\s*:").findAll(text).forEach { match ->
            val name = match.groupValues[2]
            if (name.lowercase() !in props && !name.startsWith("--")) {
                val (line, column) = lineColumn(text, match.range.first)
                result += Diagnostic(line, column, DiagnosticSeverity.WARNING, "Unknown CSS property '$name'", id)
            }
        }
        return result
    }
}

private object JavaScriptLanguageProvider : LanguageIntelligenceProvider {
    override val id = "javascript"
    private val keywords = listOf("const", "let", "var", "function", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "class", "extends", "new", "this", "import", "export", "from", "async", "await", "try", "catch", "finally", "throw", "typeof", "instanceof", "in", "of", "true", "false", "null", "undefined")
    private val apis = listOf("console.log", "console.error", "console.warn", "document.querySelector", "document.querySelectorAll", "document.getElementById", "document.createElement", "addEventListener", "removeEventListener", "fetch", "setTimeout", "setInterval", "JSON.parse", "JSON.stringify", "Math.max", "Math.min", "Array.isArray", "Object.keys", "Promise", "URL", "Date")

    override fun supports(fileName: String) = fileName.substringAfterLast('.', "").lowercase() in setOf("js", "jsx", "mjs", "cjs", "ts", "tsx")

    override fun completions(text: String, cursor: Int, projectWords: List<String>): List<CompletionItem> {
        val p = cursor.coerceIn(0, text.length)
        val prefixStart = CompletionEngine.findPrefixStart(text, p)
        val prefix = text.substring(prefixStart, p)
        return (keywords.map { CompletionItem(it, "JavaScript keyword") } + apis.map { CompletionItem(it, "JavaScript API") })
            .filter { it.label.startsWith(prefix, true) && it.label != prefix }
    }

    override fun symbols(text: String, fileName: String): List<DocumentSymbol> {
        val out = mutableListOf<DocumentSymbol>()
        val patterns = listOf(
            Regex("(?m)\\b(?:function|class)\\s+([A-Za-z_$][\\w$]*)"),
            Regex("(?m)\\b(?:const|let|var)\\s+([A-Za-z_$][\\w$]*)"),
        )
        patterns.forEach { regex -> regex.findAll(text).forEach { match ->
            val name = match.groupValues[1]
            val offset = match.range.first + match.value.lastIndexOf(name)
            val (line, column) = lineColumn(text, offset)
            val kind = when {
                match.value.contains("function") -> SymbolKind.FUNCTION
                match.value.contains("class") -> SymbolKind.CLASS
                else -> SymbolKind.VARIABLE
            }
            out += DocumentSymbol(name, kind, line, column, offset)
        } }
        return out.sortedBy { it.offset }
    }

    override fun definition(text: String, fileName: String, cursor: Int): DefinitionLocation? {
        val word = LanguageIntelligence.wordAt(text, cursor)
        return symbols(text, fileName).firstOrNull { it.name == word }?.let {
            DefinitionLocation(fileName, it.line, it.column, it.offset, it.name)
        }
    }

    override fun references(text: String, fileName: String, cursor: Int): List<ReferenceLocation> {
        val word = LanguageIntelligence.wordAt(text, cursor)
        if (word.isEmpty()) return emptyList()
        return Regex("(?<![A-Za-z0-9_$])" + Regex.escape(word) + "(?![A-Za-z0-9_$])").findAll(text).map { match ->
            val (line, column) = lineColumn(text, match.range.first)
            ReferenceLocation(fileName, line, column, match.range.first, match.value.length)
        }.toList()
    }

    override fun hover(text: String, fileName: String, cursor: Int): HoverInfo? {
        val word = LanguageIntelligence.wordAt(text, cursor)
        if (word.isEmpty()) return null
        val detail = when {
            keywords.contains(word) -> "**JavaScript keyword** `$word`"
            apis.any { it.startsWith(word) } -> "**JavaScript API** `$word`"
            else -> symbols(text, fileName).firstOrNull { it.name == word }?.let { "**${it.kind.name.lowercase().replaceFirstChar { ch -> ch.titlecase() }}** `$word`" }
        } ?: return null
        val (line, column) = lineColumn(text, cursor)
        return HoverInfo(word, detail, line, column)
    }

    override fun diagnostics(text: String, fileName: String): List<Diagnostic> {
        val result = mutableListOf<Diagnostic>()
        Regex("(?m)\\bvar\\s+[A-Za-z_$][\\w$]*").findAll(text).forEach { match ->
            val (line, column) = lineColumn(text, match.range.first)
            result += Diagnostic(line, column, DiagnosticSeverity.INFO, "Prefer const or let over var", id)
        }
        return result
    }
}

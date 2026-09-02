package io.github.norbertweb.bluebird.editor.core

/** Language-intelligence primitives shared by lightweight providers and a future LSP client. */
enum class SymbolKind { CLASS, FUNCTION, VARIABLE, PROPERTY, TAG, SELECTOR, UNKNOWN }

data class DocumentSymbol(
    val name: String,
    val kind: SymbolKind,
    val line: Int,
    val column: Int,
    val offset: Int,
    val containerName: String? = null,
)

data class DefinitionLocation(
    val fileName: String,
    val line: Int,
    val column: Int,
    val offset: Int,
    val symbol: String,
)

data class ReferenceLocation(
    val fileName: String,
    val line: Int,
    val column: Int,
    val offset: Int,
    val length: Int,
)

data class HoverInfo(val symbol: String, val markdown: String, val line: Int, val column: Int)

interface LanguageIntelligenceProvider : LanguageProvider {
    fun symbols(text: String, fileName: String): List<DocumentSymbol> = emptyList()
    fun definition(text: String, fileName: String, cursor: Int): DefinitionLocation? = null
    fun references(text: String, fileName: String, cursor: Int): List<ReferenceLocation> = emptyList()
    fun hover(text: String, fileName: String, cursor: Int): HoverInfo? = null
}

object LanguageIntelligence {
    fun symbols(text: String, fileName: String): List<DocumentSymbol> =
        (LanguageServiceRegistry.providerFor(fileName) as? LanguageIntelligenceProvider)
            ?.symbols(text, fileName).orEmpty()

    fun definition(text: String, fileName: String, cursor: Int): DefinitionLocation? =
        (LanguageServiceRegistry.providerFor(fileName) as? LanguageIntelligenceProvider)
            ?.definition(text, fileName, cursor)

    fun references(text: String, fileName: String, cursor: Int): List<ReferenceLocation> =
        (LanguageServiceRegistry.providerFor(fileName) as? LanguageIntelligenceProvider)
            ?.references(text, fileName, cursor).orEmpty()

    fun hover(text: String, fileName: String, cursor: Int): HoverInfo? =
        (LanguageServiceRegistry.providerFor(fileName) as? LanguageIntelligenceProvider)
            ?.hover(text, fileName, cursor)

    fun wordAt(text: String, cursor: Int): String {
        if (text.isEmpty()) return ""
        val p = cursor.coerceIn(0, text.length)
        var start = p
        var end = p
        fun valid(c: Char) = c.isLetterOrDigit() || c == '_' || c == '$' || c == '-' || c == ':' || c == '.'
        while (start > 0 && valid(text[start - 1])) start--
        while (end < text.length && valid(text[end])) end++
        return text.substring(start, end)
    }
}

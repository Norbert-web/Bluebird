package io.github.norbertweb.bluebird.editor.core

/** Context-aware lightweight completion provider. Designed to be replaced by LSP providers later. */
data class CompletionItem(val label: String, val detail: String, val insertText: String = label)

object CompletionEngine {
    fun suggest(text: String, cursor: Int, fileName: String, projectWords: List<String>): List<CompletionItem> =
        LanguageServiceRegistry.completions(text, cursor, fileName, projectWords)

    fun findPrefixStart(text: String, cursor: Int): Int {
        var i = cursor.coerceIn(0, text.length)
        while (i > 0 && (text[i - 1].isLetterOrDigit() || text[i - 1] == '_' || text[i - 1] == '-' || text[i - 1] == '$')) i--
        return i
    }
}

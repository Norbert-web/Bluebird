package io.github.norbertweb.bluebird.editor.core

/**
 * LSP-first completion bridge. Local completion remains the fallback so the editor stays useful
 * when a server is unavailable, slow, or does not advertise completion capability.
 */
class HybridLanguageIntelligence(
    private val manager: LanguageServerManager,
) {
    fun suggest(
        filePath: String,
        fileName: String,
        text: String,
        offset: Int,
        projectWords: List<String>,
        callback: (List<CompletionItem>) -> Unit,
    ): Int? {
        val useLsp = manager.isConnected() && manager.capabilities().completion
        if (!useLsp) {
            callback(CompletionEngine.suggest(text, offset, fileName, projectWords))
            return null
        }
        return manager.requestCompletionAsync(filePath, text, offset) { lspItems ->
            if (lspItems.isNotEmpty()) {
                callback(lspItems.map { CompletionItem(it.label, it.detail, it.insertText) })
            } else {
                callback(CompletionEngine.suggest(text, offset, fileName, projectWords))
            }
        }
    }
}

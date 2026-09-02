package io.github.norbertweb.bluebird.editor.core

import java.io.File

/** Lifecycle holder for the optional project language server. */
class LanguageServerManager {
    private var active: LanguageServerClient = NoOpLanguageServerClient
    @Volatile private var initialized = false
    private val lock = Any()

    fun attach(client: LanguageServerClient, root: File): Boolean = synchronized(lock) {
        detachLocked()
        val ok = runCatching { client.initialize(root.absolutePath) }.getOrDefault(false)
        if (!ok) {
            runCatching { client.shutdown() }
            return@synchronized false
        }
        active = client
        initialized = true
        true
    }

    fun detach() = synchronized(lock) { detachLocked() }

    private fun detachLocked() {
        if (initialized) runCatching { active.shutdown() }
        active = NoOpLanguageServerClient
        initialized = false
    }

    fun client(): LanguageServerClient = synchronized(lock) { active }
    fun isConnected(): Boolean = initialized
    fun capabilities(): LspServerCapabilities = synchronized(lock) { active.capabilities() }

    fun requestCompletionAsync(filePath: String, text: String, offset: Int, callback: (List<LspCompletionItem>) -> Unit): Int? =
        (active as? StdioLanguageServerClient)?.requestCompletionAsync(filePath, text, offset, callback)

    fun requestSemanticTokensAsync(filePath: String, callback: (List<LspSemanticToken>) -> Unit): Int? =
        (active as? StdioLanguageServerClient)?.requestSemanticTokensAsync(filePath, callback)

    fun codeActions(filePath: String, startOffset: Int, endOffset: Int): List<LspCodeAction> = active.codeActions(filePath, startOffset, endOffset)
    fun formatDocument(filePath: String): List<org.json.JSONObject> = active.formatDocument(filePath)
    fun semanticTokens(filePath: String): List<LspSemanticToken> = active.semanticTokens(filePath)

    fun setDiagnosticsHandler(handler: ((String?, List<LspDiagnostic>) -> Unit)?) {
        (active as? StdioLanguageServerClient)?.diagnosticsHandler = handler?.let { callback -> { pair -> callback(pair.first, pair.second) } }
    }

    fun cancelRequest(id: Int) { (active as? StdioLanguageServerClient)?.cancelRequest(id) }

    fun didOpen(tab: TabData) {
        if (!initialized || tab.filePath.isBlank()) return
        active.didOpen(tab.filePath, tab.content.text, languageId(tab.fileName))
    }

    fun didChange(tab: TabData) {
        if (!initialized || tab.filePath.isBlank()) return
        active.didChange(tab.filePath, tab.content.text)
    }

    fun didClose(tab: TabData) {
        if (!initialized || tab.filePath.isBlank()) return
        active.didClose(tab.filePath)
    }

    private fun languageId(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "html"
        "css", "scss", "sass", "less" -> "css"
        "ts", "tsx" -> "typescript"
        "jsx" -> "javascriptreact"
        else -> "javascript"
    }
}

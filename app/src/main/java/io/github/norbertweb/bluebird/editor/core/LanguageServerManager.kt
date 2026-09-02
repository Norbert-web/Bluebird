package io.github.norbertweb.bluebird.editor.core

import java.io.File

/** Lifecycle holder for the optional project language server. */
class LanguageServerManager {
    private var active: LanguageServerClient = NoOpLanguageServerClient
    private var initialized = false

    fun attach(client: LanguageServerClient, root: File): Boolean {
        detach()
        val ok = client.initialize(root.absolutePath)
        if (!ok) {
            client.shutdown()
            return false
        }
        active = client
        initialized = true
        return true
    }

    fun detach() {
        if (initialized) active.shutdown()
        active = NoOpLanguageServerClient
        initialized = false
    }

    fun client(): LanguageServerClient = active
    fun isConnected(): Boolean = initialized
    fun capabilities(): LspServerCapabilities = active.capabilities()

    fun requestCompletionAsync(filePath: String, text: String, offset: Int, callback: (List<LspCompletionItem>) -> Unit): Int? =
        (active as? StdioLanguageServerClient)?.requestCompletionAsync(filePath, text, offset, callback)

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

package io.github.norbertweb.bluebird.editor.core

/**
 * Small editor-facing abstraction for an eventual Language Server Protocol client.
 * The current workspace index can be used as the local fallback implementation.
 */
interface LanguageServerClient {
    fun initialize(rootPath: String): Boolean
    fun shutdown()
    fun didOpen(filePath: String, text: String, languageId: String)
    fun didChange(filePath: String, text: String)
    fun didClose(filePath: String)
    fun definition(filePath: String, offset: Int): DefinitionLocation?
    fun references(filePath: String, offset: Int): List<ReferenceLocation>
    fun rename(filePath: String, offset: Int, newName: String): Map<String, String>
    fun hover(filePath: String, offset: Int): HoverInfo?

    /** Optional richer LSP operations. Implementations may return null/empty when unsupported. */
    fun completion(filePath: String, offset: Int): List<LspCompletionItem> = emptyList()
    fun capabilities(): LspServerCapabilities = LspServerCapabilities()
}

object NoOpLanguageServerClient : LanguageServerClient {
    override fun initialize(rootPath: String): Boolean = false
    override fun shutdown() = Unit
    override fun didOpen(filePath: String, text: String, languageId: String) = Unit
    override fun didChange(filePath: String, text: String) = Unit
    override fun didClose(filePath: String) = Unit
    override fun definition(filePath: String, offset: Int): DefinitionLocation? = null
    override fun references(filePath: String, offset: Int): List<ReferenceLocation> = emptyList()
    override fun rename(filePath: String, offset: Int, newName: String): Map<String, String> = emptyMap()
    override fun hover(filePath: String, offset: Int): HoverInfo? = null
    override fun completion(filePath: String, offset: Int): List<LspCompletionItem> = emptyList()
    override fun capabilities(): LspServerCapabilities = LspServerCapabilities()
}

package io.github.norbertweb.bluebird.editor.core

import java.io.File

/**
 * Lightweight workspace index for HTML/CSS/JS/TS. It deliberately has no Android/UI dependency,
 * so it can later be replaced or backed by an LSP workspace index without changing editor APIs.
 */
data class WorkspaceFileDocument(val path: String, val text: String, val fileName: String)

data class WorkspaceSearchResult(
    val filePath: String,
    val fileName: String,
    val line: Int,
    val column: Int,
    val lineText: String,
    val matchText: String,
    val offset: Int,
)

class WorkspaceSymbolIndex(
    private val maxFiles: Int = 1500,
    private val maxFileBytes: Long = 2L * 1024L * 1024L,
) {
    private val documents = linkedMapOf<String, WorkspaceFileDocument>()
    private val symbols = mutableListOf<IndexedSymbol>()

    data class IndexedSymbol(val location: DefinitionLocation, val kind: SymbolKind)

    data class WorkspaceSymbolEntry(
        val name: String,
        val kind: SymbolKind,
        val filePath: String,
        val fileName: String,
        val line: Int,
        val column: Int,
        val offset: Int,
    )

    data class WorkspaceFileEntry(
        val path: String,
        val fileName: String,
        val symbolCount: Int,
    )

    val indexedFilePaths: List<String> get() = documents.keys.toList()
    val indexedSymbolCount: Int get() = symbols.size

    val indexedSymbols: List<WorkspaceSymbolEntry>
        get() = symbols.map { item ->
            WorkspaceSymbolEntry(
                name = item.location.symbol,
                kind = item.kind,
                filePath = item.location.fileName,
                fileName = File(item.location.fileName).name,
                line = item.location.line,
                column = item.location.column,
                offset = item.location.offset,
            )
        }

    val indexedFiles: List<WorkspaceFileEntry>
        get() = documents.values.map { doc ->
            WorkspaceFileEntry(
                path = doc.path,
                fileName = doc.fileName,
                symbolCount = symbols.count { it.location.fileName == doc.path },
            )
        }.sortedBy { it.path.lowercase() }

    fun clear() {
        documents.clear()
        symbols.clear()
    }

    fun rebuild(root: File?, openDocuments: Map<String, String> = emptyMap()) {
        clear()
        if (root == null) return
        val rootDir = if (root.isFile) root.parentFile else root
        if (rootDir == null || !rootDir.exists()) return

        val diskFiles = rootDir.walkTopDown()
            .onEnter { dir ->
                val name = dir.name.lowercase()
                name !in setOf(".git", ".gradle", "build", "node_modules", ".idea")
            }
            .filter { it.isFile && isSupported(it.name) && it.length() <= maxFileBytes }
            .take(maxFiles)
            .toList()

        diskFiles.forEach { file ->
            runCatching { documents[file.canonicalPath] = WorkspaceFileDocument(file.canonicalPath, file.readText(), file.name) }
        }
        // Open buffers always win over disk content, including unsaved files.
        openDocuments.forEach { (path, text) ->
            if (path.isNotBlank()) {
                val canonical = runCatching { File(path).canonicalPath }.getOrDefault(path)
                documents[canonical] = WorkspaceFileDocument(canonical, text, File(path).name)
            }
        }
        rebuildSymbols()
    }

    fun updateDocument(path: String, text: String) {
        if (path.isBlank() || !isSupported(path)) return
        val canonical = runCatching { File(path).canonicalPath }.getOrDefault(path)
        documents[canonical] = WorkspaceFileDocument(canonical, text, File(path).name)
        symbols.removeAll { it.location.fileName == canonical }
        addSymbols(canonical, text, File(path).name)
    }

    private fun rebuildSymbols() {
        symbols.clear()
        documents.values.forEach { addSymbols(it.path, it.text, it.fileName) }
    }

    private fun addSymbols(path: String, text: String, fileName: String) {
        LanguageIntelligence.symbols(text, fileName).forEach { symbol ->
            symbols += IndexedSymbol(
                DefinitionLocation(path, symbol.line, symbol.column, symbol.offset, symbol.name),
                symbol.kind,
            )
        }
    }

    fun definition(symbol: String, preferredFile: String? = null): DefinitionLocation? {
        if (symbol.isBlank()) return null
        val matches = symbols.filter { it.location.symbol.equals(symbol, ignoreCase = true) }
        return matches.firstOrNull { preferredFile != null && sameFile(it.location.fileName, preferredFile) }?.location
            ?: matches.firstOrNull()?.location
    }

    fun references(symbol: String): List<ReferenceLocation> {
        if (symbol.isBlank()) return emptyList()
        val regex = Regex("(?<![A-Za-z0-9_$:-])" + Regex.escape(symbol) + "(?![A-Za-z0-9_$:-])")
        return documents.values.flatMap { doc ->
            regex.findAll(doc.text).map { match ->
                val line = doc.text.take(match.range.first).count { it == '\n' } + 1
                val lineStart = doc.text.lastIndexOf('\n', match.range.first - 1).let { if (it < 0) 0 else it + 1 }
                ReferenceLocation(doc.path, line, match.range.first - lineStart + 1, match.range.first, match.value.length)
            }.toList()
        }
    }


    fun renameSymbol(oldName: String, newName: String): Map<String, String> {
        if (oldName.isBlank() || newName.isBlank() || oldName == newName) return emptyMap()
        val regex = Regex("(?<![A-Za-z0-9_$:-])" + Regex.escape(oldName) + "(?![A-Za-z0-9_$:-])")
        val changed = linkedMapOf<String, String>()
        documents.values.forEach { doc ->
            val next = regex.replace(doc.text, newName)
            if (next != doc.text) changed[doc.path] = next
        }
        return changed
    }

    fun search(query: String, caseSensitive: Boolean = false, regexMode: Boolean = false, maxResults: Int = 500): List<WorkspaceSearchResult> {
        if (query.isEmpty()) return emptyList()
        val pattern = runCatching {
            if (regexMode) Regex(query, if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
            else Regex(Regex.escape(query), if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
        }.getOrNull() ?: return emptyList()
        val out = mutableListOf<WorkspaceSearchResult>()
        documents.values.forEach { doc ->
            doc.text.split('\n').forEachIndexed { lineIndex, lineText ->
                pattern.findAll(lineText).forEach { match ->
                    if (out.size >= maxResults) return@forEach
                    out += WorkspaceSearchResult(
                        doc.path, doc.fileName, lineIndex + 1, match.range.first + 1,
                        lineText.trim(), match.value, offsetForLine(doc.text, lineIndex) + match.range.first,
                    )
                }
                if (out.size >= maxResults) return@forEachIndexed
            }
            if (out.size >= maxResults) return@forEach
        }
        return out
    }

    private fun offsetForLine(text: String, line: Int): Int {
        var current = 0
        repeat(line) {
            val next = text.indexOf('\n', current)
            if (next < 0) return current
            current = next + 1
        }
        return current
    }

    private fun sameFile(a: String, b: String): Boolean =
        runCatching { File(a).canonicalFile == File(b).canonicalFile }.getOrDefault(a == b)

    companion object {
        fun isSupported(fileName: String): Boolean =
            fileName.substringAfterLast('.', "").lowercase() in setOf("html", "htm", "css", "js", "mjs", "cjs", "jsx", "ts", "tsx")
    }
}

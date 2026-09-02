package io.github.norbertweb.bluebird.editor.core

import java.io.File

data class WorkspaceFileDocument(val path: String, val text: String, val fileName: String)

data class WorkspaceImport(
    val sourceFile: String,
    val importedName: String,
    val modulePath: String,
    val localName: String = importedName,
    val isTypeOnly: Boolean = false,
)

data class WorkspaceExport(
    val sourceFile: String,
    val exportedName: String,
    val localName: String = exportedName,
    val isDefault: Boolean = false,
)

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
    private val imports = mutableListOf<WorkspaceImport>()
    private val exports = mutableListOf<WorkspaceExport>()

    data class IndexedSymbol(val location: DefinitionLocation, val kind: SymbolKind)
    data class WorkspaceSymbolEntry(val name: String, val kind: SymbolKind, val filePath: String, val fileName: String, val line: Int, val column: Int, val offset: Int, val containerName: String? = null)
    data class WorkspaceFileEntry(val path: String, val fileName: String, val symbolCount: Int)

    val indexedFilePaths: List<String> get() = documents.keys.toList()
    val indexedSymbolCount: Int get() = symbols.size
    val indexedImportCount: Int get() = imports.size
    val indexedExportCount: Int get() = exports.size
    val indexedImports: List<WorkspaceImport> get() = imports.toList()
    val indexedExports: List<WorkspaceExport> get() = exports.toList()

    val indexedSymbols: List<WorkspaceSymbolEntry>
        get() = symbols.map { item ->
            WorkspaceSymbolEntry(item.location.symbol, item.kind, item.location.fileName, File(item.location.fileName).name, item.location.line, item.location.column, item.location.offset)
        }

    val indexedFiles: List<WorkspaceFileEntry>
        get() = documents.values.map { doc -> WorkspaceFileEntry(doc.path, doc.fileName, symbols.count { it.location.fileName == doc.path }) }.sortedBy { it.path.lowercase() }

    fun clear() {
        documents.clear(); symbols.clear(); imports.clear(); exports.clear()
    }

    fun rebuild(root: File?, openDocuments: Map<String, String> = emptyMap()) {
        clear()
        if (root == null) return
        val rootDir = if (root.isFile) root.parentFile else root
        if (rootDir == null || !rootDir.exists()) return
        val ignored = setOf(".git", ".gradle", "build", "node_modules", ".idea", "target", "out")
        rootDir.walkTopDown().onEnter { it.name.lowercase() !in ignored }
            .filter { it.isFile && isSupported(it.name) && it.length() <= maxFileBytes }
            .take(maxFiles)
            .forEach { file -> runCatching { documents[file.canonicalPath] = WorkspaceFileDocument(file.canonicalPath, file.readText(), file.name) } }
        openDocuments.forEach { (path, text) ->
            if (path.isNotBlank()) {
                val canonical = canonical(path)
                documents[canonical] = WorkspaceFileDocument(canonical, text, File(path).name)
            }
        }
        rebuildSymbols()
    }

    fun updateDocument(path: String, text: String) {
        if (path.isBlank() || !isSupported(path)) return
        val canonical = canonical(path)
        documents[canonical] = WorkspaceFileDocument(canonical, text, File(path).name)
        symbols.removeAll { it.location.fileName == canonical }
        imports.removeAll { it.sourceFile == canonical }
        exports.removeAll { it.sourceFile == canonical }
        addSymbols(canonical, text, File(path).name)
    }

    private fun rebuildSymbols() {
        symbols.clear(); imports.clear(); exports.clear()
        documents.values.forEach { addSymbols(it.path, it.text, it.fileName) }
    }

    private fun addSymbols(path: String, text: String, fileName: String) {
        LanguageIntelligence.symbols(text, fileName).forEach { symbol ->
            symbols += IndexedSymbol(DefinitionLocation(path, symbol.line, symbol.column, symbol.offset, symbol.name), symbol.kind)
        }
        indexModuleBindings(path, text)
    }

    private fun indexModuleBindings(path: String, text: String) {
        val importFrom = Regex("(?m)\\bimport\\s+(?:(type)\\s+)?(.+?)\\s+from\\s+[\\\"'](.+?)[\\\"']")
        importFrom.findAll(text).forEach { match ->
            val clause = match.groupValues[2].trim()
            val module = match.groupValues[3]
            val typeOnly = match.groupValues[1].isNotBlank()
            val parts = clause.replace("{", "").replace("}", "").split(',').map { it.trim() }.filter { it.isNotBlank() }
            parts.forEach { part ->
                val pieces = part.split(Regex("\\s+as\\s+"))
                val imported = pieces.firstOrNull()?.trim().orEmpty().ifEmpty { "*" }
                val local = pieces.lastOrNull()?.trim().orEmpty().ifEmpty { imported }
                imports += WorkspaceImport(path, imported, module, local, typeOnly)
            }
        }
        Regex("(?m)\\bimport\\s+[\\\"'](.+?)[\\\"']").findAll(text).forEach { match ->
            imports += WorkspaceImport(path, "*", match.groupValues[1], "*", false)
        }
        Regex("(?m)\\bexport\\s+(?:default\\s+)?(?:async\\s+)?(?:function|class|const|let|var)\\s+([A-Za-z_$][\\w$]*)").findAll(text).forEach { match ->
            val name = match.groupValues[1]
            exports += WorkspaceExport(path, name, name, match.value.contains("default"))
        }
        Regex("(?m)\\bexport\\s+default\\s+([A-Za-z_$][\\w$]*)").findAll(text).forEach { match ->
            val name = match.groupValues[1]
            exports += WorkspaceExport(path, name, name, true)
        }
        Regex("(?m)\\bexport\\s*\\{([^}]+)\\}").findAll(text).forEach { match ->
            match.groupValues[1].split(',').forEach { part ->
                val bits = part.trim().split(Regex("\\s+as\\s+"))
                val local = bits.firstOrNull()?.trim().orEmpty()
                val exported = bits.lastOrNull()?.trim().orEmpty()
                if (local.isNotBlank() && exported.isNotBlank()) exports += WorkspaceExport(path, exported, local, false)
            }
        }
    }

    fun definition(symbol: String, preferredFile: String? = null): DefinitionLocation? {
        if (symbol.isBlank()) return null
        val matches = symbols.filter { it.location.symbol.equals(symbol, ignoreCase = true) }
        return matches.firstOrNull { preferredFile != null && sameFile(it.location.fileName, preferredFile) }?.location ?: matches.firstOrNull()?.location
    }

    fun definitionForImport(fromFile: String, symbol: String): DefinitionLocation? {
        val file = canonical(fromFile)
        val binding = imports.firstOrNull { it.sourceFile == file && (it.localName == symbol || it.importedName == symbol || it.importedName == "*") } ?: return null
        val target = resolveModule(file, binding.modulePath) ?: return null
        val exported = exports.firstOrNull { it.sourceFile == target && (it.exportedName == binding.importedName || it.isDefault) }
        return symbols.firstOrNull { it.location.fileName == target && (it.location.symbol == exported?.localName || it.location.symbol == binding.importedName || binding.importedName == "*") }?.location
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

    fun referencesWithImports(fromFile: String, symbol: String): List<ReferenceLocation> {
        val base = references(symbol)
        val imported = imports.filter { it.sourceFile == canonical(fromFile) && it.localName == symbol }.flatMap { binding ->
            val target = resolveModule(binding.sourceFile, binding.modulePath) ?: return@flatMap emptyList()
            referencesInFile(target, binding.importedName.takeUnless { it == "*" } ?: symbol)
        }
        return (base + imported).distinctBy { "${it.fileName}:${it.offset}" }
    }

    private fun referencesInFile(path: String, symbol: String): List<ReferenceLocation> {
        val doc = documents[canonical(path)] ?: return emptyList()
        val regex = Regex("(?<![A-Za-z0-9_$:-])" + Regex.escape(symbol) + "(?![A-Za-z0-9_$:-])")
        return regex.findAll(doc.text).map { match ->
            val line = doc.text.take(match.range.first).count { it == '\n' } + 1
            val start = doc.text.lastIndexOf('\n', match.range.first - 1).let { if (it < 0) 0 else it + 1 }
            ReferenceLocation(doc.path, line, match.range.first - start + 1, match.range.first, match.value.length)
        }.toList()
    }

    fun resolveModule(fromFile: String, modulePath: String): String? {
        if (!modulePath.startsWith('.')) return null
        val raw = File(File(fromFile).parentFile ?: return null, modulePath)
        val candidates = listOf(raw, File(raw.path + ".js"), File(raw.path + ".jsx"), File(raw.path + ".ts"), File(raw.path + ".tsx"), File(raw.path + ".css"), File(raw.path + ".html"), File(raw, "index.js"), File(raw, "index.ts"), File(raw, "index.jsx"), File(raw, "index.tsx"))
        return candidates.firstOrNull { documents.containsKey(canonical(it.path)) }?.let { canonical(it.path) }
    }

    fun renameSymbol(oldName: String, newName: String): Map<String, String> {
        if (oldName.isBlank() || newName.isBlank() || oldName == newName) return emptyMap()
        val regex = Regex("(?<![A-Za-z0-9_$:-])" + Regex.escape(oldName) + "(?![A-Za-z0-9_$:-])")
        return documents.values.mapNotNull { doc ->
            val next = regex.replace(doc.text, newName)
            if (next == doc.text) null else doc.path to next
        }.toMap(LinkedHashMap())
    }

    fun quickOpenFiles(query: String = "", maxResults: Int = 80): List<WorkspaceFileEntry> {
        val normalized = query.trim().lowercase()
        return indexedFiles.map { it to fuzzyScore(normalized, (it.fileName + " " + it.path).lowercase()) }
            .filter { normalized.isEmpty() || it.second >= 0 }
            .sortedWith(compareByDescending<Pair<WorkspaceFileEntry, Int>> { it.second }.thenBy { it.first.path.lowercase() })
            .take(maxResults).map { it.first }
    }

    fun search(query: String, caseSensitive: Boolean = false, regexMode: Boolean = false, maxResults: Int = 500): List<WorkspaceSearchResult> {
        if (query.isEmpty()) return emptyList()
        val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        val pattern = runCatching { Regex(if (regexMode) query else Regex.escape(query), options) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<WorkspaceSearchResult>()
        outer@ for (doc in documents.values) {
            doc.text.split('\n').forEachIndexed { lineIndex, lineText ->
                pattern.findAll(lineText).forEach { match ->
                    if (out.size >= maxResults) return@forEach
                    out += WorkspaceSearchResult(doc.path, doc.fileName, lineIndex + 1, match.range.first + 1, lineText.trim(), match.value, offsetForLine(doc.text, lineIndex) + match.range.first)
                }
                if (out.size >= maxResults) return@forEachIndexed
            }
            if (out.size >= maxResults) break@outer
        }
        return out
    }

    private fun fuzzyScore(query: String, candidate: String): Int {
        if (query.isEmpty()) return 0
        if (candidate.contains(query)) return 1000 - candidate.indexOf(query)
        var qi = 0; var score = 0; var previous = -2
        for (i in candidate.indices) {
            if (qi >= query.length) break
            if (candidate[i] == query[qi]) { score += if (i == previous + 1) 12 else 5; if (i == 0 || candidate[i - 1] in "/\\\\._- ") score += 8; previous = i; qi++ }
        }
        return if (qi == query.length) score else -1
    }

    private fun offsetForLine(text: String, line: Int): Int {
        var current = 0
        repeat(line) { val next = text.indexOf('\n', current); if (next < 0) return current; current = next + 1 }
        return current
    }
    private fun canonical(path: String): String = runCatching { File(path).canonicalPath }.getOrDefault(path)
    private fun sameFile(a: String, b: String): Boolean = canonical(a) == canonical(b)

    companion object {
        fun isSupported(fileName: String): Boolean = fileName.substringAfterLast('.', "").lowercase() in setOf("html", "htm", "css", "scss", "sass", "less", "js", "mjs", "cjs", "jsx", "ts", "tsx")
    }
}

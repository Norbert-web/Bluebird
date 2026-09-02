package io.github.norbertweb.bluebird.editor.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Optional adapter that turns a generic stdio LSP server into Bluebird's editor-facing
 * language intelligence API. The local providers remain the fallback when no server is
 * attached or when a server does not return a usable result.
 */
class LspLanguageServerAdapter(
    private val client: StdioLanguageServerClient,
) : LanguageServerClient {
    override fun initialize(rootPath: String): Boolean = client.initialize(rootPath)
    override fun shutdown() = client.shutdown()
    override fun didOpen(filePath: String, text: String, languageId: String) = client.didOpen(filePath, text, languageId)
    override fun didChange(filePath: String, text: String) = client.didChange(filePath, text)
    override fun didClose(filePath: String) = client.didClose(filePath)

    override fun definition(filePath: String, offset: Int): DefinitionLocation? {
        val result = client.rawRequest("textDocument/definition", textPositionParams(filePath, offset)) ?: return null
        val item = when {
            result.has("uri") -> result
            result.optJSONArray("result")?.length()?.let { it > 0 } == true -> result.optJSONArray("result")?.optJSONObject(0)
            else -> null
        } ?: return null
        return locationFromJson(item)
    }

    override fun references(filePath: String, offset: Int): List<ReferenceLocation> {
        val result = client.rawRequest("textDocument/references", textPositionParams(filePath, offset).put("context", JSONObject().put("includeDeclaration", true))) ?: return emptyList()
        val array = result.optJSONArray("result") ?: if (result.length() > 0 && result.optString("uri").isNotBlank()) JSONArray().put(result) else return emptyList()
        return (0 until array.length()).mapNotNull { locationFromReferenceJson(array.optJSONObject(it)) }
    }

    override fun rename(filePath: String, offset: Int, newName: String): Map<String, String> {
        val params = textPositionParams(filePath, offset).put("newName", newName)
        val result = client.rawRequest("textDocument/rename", params) ?: return emptyMap()
        val changes = result.optJSONObject("changes") ?: return emptyMap()
        val output = linkedMapOf<String, String>()
        val names = changes.keys()
        while (names.hasNext()) {
            val uri = names.next()
            val edits = changes.optJSONArray(uri) ?: continue
            val path = uriToPath(uri) ?: continue
            val original = runCatching { File(path).readText() }.getOrNull() ?: continue
            output[path] = applyTextEdits(original, edits)
        }
        return output
    }

    override fun completion(filePath: String, offset: Int): List<LspCompletionItem> = client.completion(filePath, offset)

    override fun capabilities(): LspServerCapabilities = client.capabilities()

    override fun hover(filePath: String, offset: Int): HoverInfo? {
        val result = client.rawRequest("textDocument/hover", textPositionParams(filePath, offset)) ?: return null
        val value = result.optJSONObject("contents")?.let { it.optString("value").ifBlank { it.toString() } }
            ?: result.optString("contents").ifBlank { return null }
        val word = LanguageIntelligence.wordAt(runCatching { File(filePath).readText() }.getOrDefault(""), offset)
        return HoverInfo(word, value, 0, 0)
    }

    private fun textPositionParams(filePath: String, offset: Int): JSONObject {
        val text = runCatching { File(filePath).readText() }.getOrDefault("")
        val safe = offset.coerceIn(0, text.length)
        val line = text.take(safe).count { it == '\n' }
        val lastNewline = text.lastIndexOf('\n', safe - 1)
        val character = safe - (lastNewline + 1)
        return JSONObject().put("textDocument", JSONObject().put("uri", fileToUri(filePath))).put("position", JSONObject().put("line", line).put("character", character))
    }

    private fun locationFromJson(json: JSONObject?): DefinitionLocation? {
        if (json == null) return null
        val uri = json.optString("uri")
        val range = json.optJSONObject("range") ?: return null
        val start = range.optJSONObject("start") ?: return null
        val path = uriToPath(uri) ?: return null
        val line = start.optInt("line", 0) + 1
        val column = start.optInt("character", 0) + 1
        val text = runCatching { File(path).readText() }.getOrDefault("")
        val offset = lineColumnToOffset(text, line, column)
        val symbol = LanguageIntelligence.wordAt(text, offset)
        return DefinitionLocation(path, line, column, offset, symbol)
    }

    private fun locationFromReferenceJson(json: JSONObject?): ReferenceLocation? {
        if (json == null) return null
        val uri = json.optString("uri")
        val range = json.optJSONObject("range") ?: return null
        val start = range.optJSONObject("start") ?: return null
        val end = range.optJSONObject("end") ?: start
        val path = uriToPath(uri) ?: return null
        val line = start.optInt("line", 0) + 1
        val column = start.optInt("character", 0) + 1
        val endColumn = end.optInt("character", start.optInt("character", 0)) + 1
        val text = runCatching { File(path).readText() }.getOrDefault("")
        val offset = lineColumnToOffset(text, line, column)
        return ReferenceLocation(path, line, column, offset, (endColumn - column).coerceAtLeast(1))
    }

    private fun applyTextEdits(original: String, edits: JSONArray): String {
        data class Edit(val start: Int, val end: Int, val replacement: String)
        val parsed = mutableListOf<Edit>()
        for (i in 0 until edits.length()) {
            val edit = edits.optJSONObject(i) ?: continue
            val range = edit.optJSONObject("range") ?: continue
            val start = range.optJSONObject("start") ?: continue
            val end = range.optJSONObject("end") ?: continue
            parsed += Edit(
                lineColumnToOffset(original, start.optInt("line", 0) + 1, start.optInt("character", 0) + 1),
                lineColumnToOffset(original, end.optInt("line", 0) + 1, end.optInt("character", 0) + 1),
                edit.optString("newText"),
            )
        }
        var output = original
        parsed.sortedByDescending { it.start }.forEach { edit ->
            val start = edit.start.coerceIn(0, output.length)
            val end = edit.end.coerceIn(start, output.length)
            output = output.substring(0, start) + edit.replacement + output.substring(end)
        }
        return output
    }

    private fun lineColumnToOffset(text: String, line: Int, column: Int): Int {
        if (line <= 1) return (column - 1).coerceAtLeast(0).coerceAtMost(text.length)
        var offset = 0
        repeat(line - 1) {
            val next = text.indexOf('\n', offset)
            if (next < 0) return text.length
            offset = next + 1
        }
        return (offset + column - 1).coerceIn(offset, text.length)
    }

    private fun fileToUri(path: String): String = java.net.URI.create("file://${File(path).absolutePath.replace("\\", "/")}").toString()
    private fun uriToPath(uri: String): String? = runCatching { java.net.URI(uri).path }.getOrNull()
}

package io.github.norbertweb.bluebird.editor.core

import org.json.JSONArray
import org.json.JSONObject

/** LSP completion item normalized for Bluebird's editor UI. */
data class LspCompletionItem(
    val label: String,
    val detail: String = "",
    val insertText: String = label,
    val kind: Int = 0,
)

data class LspServerCapabilities(
    val completion: Boolean = false,
    val diagnostics: Boolean = false,
    val hover: Boolean = false,
    val definition: Boolean = false,
    val references: Boolean = false,
    val rename: Boolean = false,
    val codeAction: Boolean = false,
    val formatting: Boolean = false,
    val semanticTokens: Boolean = false,
) {
    companion object {
        fun fromInitialize(result: JSONObject?): LspServerCapabilities {
            val caps = result?.optJSONObject("capabilities") ?: return LspServerCapabilities()
            return LspServerCapabilities(
                completion = caps.has("completionProvider"),
                diagnostics = caps.has("diagnosticProvider") || caps.has("textDocumentSync"),
                hover = caps.has("hoverProvider"),
                definition = caps.has("definitionProvider"),
                references = caps.has("referencesProvider"),
                rename = caps.has("renameProvider"),
                codeAction = caps.has("codeActionProvider"),
                formatting = caps.has("documentFormattingProvider"),
                semanticTokens = caps.has("semanticTokensProvider"),
            )
        }
    }
}

data class LspDiagnostic(
    val message: String,
    val severity: Int = 1,
    val line: Int = 1,
    val column: Int = 1,
    val endLine: Int = line,
    val endColumn: Int = column,
    val source: String = "LSP",
)

data class LspCodeAction(
    val title: String,
    val kind: String = "quickfix",
    val edit: JSONObject? = null,
    val command: JSONObject? = null,
)

data class LspSemanticToken(
    val line: Int,
    val startCharacter: Int,
    val length: Int,
    val tokenType: Int,
    val tokenModifiers: Int = 0,
)

internal fun parseCompletionResult(result: JSONObject?): List<LspCompletionItem> {
    val array = when {
        result == null -> null
        result.has("items") -> result.optJSONArray("items")
        result.has("result") -> result.optJSONObject("result")?.optJSONArray("items")
        else -> null
    } ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val label = item.optString("label").ifBlank { return@mapNotNull null }
        val edit = item.optJSONObject("textEdit")
        val insert = item.optString("insertText").ifBlank {
            edit?.optString("newText").orEmpty().ifBlank { label }
        }
        LspCompletionItem(label, item.optString("detail"), insert, item.optInt("kind", 0))
    }
}

internal fun parseDiagnostics(params: JSONObject?): Pair<String?, List<LspDiagnostic>> {
    if (params == null) return null to emptyList()
    val uri = params.optString("uri").ifBlank { null }
    val diagnostics = params.optJSONArray("diagnostics") ?: JSONArray()
    val parsed = (0 until diagnostics.length()).mapNotNull { i ->
        val item = diagnostics.optJSONObject(i) ?: return@mapNotNull null
        val range = item.optJSONObject("range") ?: return@mapNotNull null
        val start = range.optJSONObject("start") ?: return@mapNotNull null
        val end = range.optJSONObject("end") ?: start
        LspDiagnostic(
            message = item.optString("message"),
            severity = item.optInt("severity", 1),
            line = start.optInt("line", 0) + 1,
            column = start.optInt("character", 0) + 1,
            endLine = end.optInt("line", start.optInt("line", 0)) + 1,
            endColumn = end.optInt("character", start.optInt("character", 0)) + 1,
            source = item.optString("source").ifBlank { "LSP" },
        )
    }
    return uri to parsed
}

internal fun parseCodeActions(result: JSONObject?): List<LspCodeAction> {
    val array = result?.optJSONArray("result") ?: result?.optJSONArray("items") ?: return emptyList()
    return (0 until array.length()).mapNotNull { i ->
        val item = array.optJSONObject(i) ?: return@mapNotNull null
        val action = item.optJSONObject("codeAction") ?: item
        val title = action.optString("title").ifBlank { return@mapNotNull null }
        LspCodeAction(title, action.optString("kind", "quickfix"), action.optJSONObject("edit"), action.optJSONObject("command"))
    }
}

/** Decode LSP semantic token delta encoding into absolute token positions. */
internal fun parseSemanticTokens(result: JSONObject?): List<LspSemanticToken> {
    val data = result?.optJSONArray("data") ?: result?.optJSONObject("result")?.optJSONArray("data") ?: return emptyList()
    var line = 0
    var character = 0
    return buildList {
        var i = 0
        while (i + 4 < data.length()) {
            val deltaLine = data.optInt(i++)
            val deltaStart = data.optInt(i++)
            val length = data.optInt(i++)
            val tokenType = data.optInt(i++)
            val modifiers = data.optInt(i++)
            line += deltaLine
            character = if (deltaLine == 0) character + deltaStart else deltaStart
            add(LspSemanticToken(line, character, length, tokenType, modifiers))
        }
    }
}

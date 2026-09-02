package io.github.norbertweb.bluebird.editor.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

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
) {
    companion object {
        fun fromInitialize(result: JSONObject?): LspServerCapabilities {
            val caps = result?.optJSONObject("capabilities") ?: return LspServerCapabilities()
            return LspServerCapabilities(
                completion = caps.has("completionProvider"),
                diagnostics = caps.has("diagnosticProvider") || caps.optBoolean("textDocumentSync", false),
                hover = caps.has("hoverProvider"),
                definition = caps.has("definitionProvider"),
                references = caps.has("referencesProvider"),
                rename = caps.has("renameProvider"),
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

/**
 * Thin asynchronous facade over an initialized LSP client. Requests are dispatched off the
 * Compose/UI thread and stale completion responses can be cancelled by request id.
 */
class AsyncLspLanguageService(
    private val client: StdioLanguageServerClient,
) {
    private val timeoutScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1) { runnable ->
        Thread(runnable, "Bluebird-LSP-Timeout").apply { isDaemon = true }
    }

    fun requestCompletion(
        filePath: String,
        text: String,
        offset: Int,
        callback: (List<LspCompletionItem>) -> Unit,
    ): Int? = client.requestCompletionAsync(filePath, text, offset, callback)

    fun requestDiagnostics(
        filePath: String,
        text: String,
        callback: (List<LspDiagnostic>) -> Unit,
    ): Int? = client.requestDiagnosticsAsync(filePath, text, callback)

    fun cancel(requestId: Int) = client.cancelRequest(requestId)

    fun shutdown() {
        timeoutScheduler.shutdownNow()
        executor.shutdownNow()
    }
}

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

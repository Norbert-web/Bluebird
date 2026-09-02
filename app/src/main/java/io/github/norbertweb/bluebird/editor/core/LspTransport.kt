package io.github.norbertweb.bluebird.editor.core

import android.net.Uri
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** JSON-RPC 2.0 transport compatible with stdio-based Language Server Protocol servers. */
class JsonRpcStdioTransport private constructor(
    private val process: Process,
    private val input: BufferedInputStream,
    private val writer: BufferedWriter,
) {
    private val nextId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, PendingRequest>()
    @Volatile private var running = true
    @Volatile var notificationHandler: ((String, JSONObject) -> Unit)? = null

    data class PendingRequest(val latch: CountDownLatch = CountDownLatch(1), @Volatile var result: JSONObject? = null, @Volatile var error: JSONObject? = null)

    init {
        Thread({ readLoop() }, "Bluebird-LSP-Reader").apply { isDaemon = true }.start()
    }

    fun notify(method: String, params: JSONObject = JSONObject()) {
        if (!running) return
        send(JSONObject().put("jsonrpc", "2.0").put("method", method).put("params", params))
    }

    fun request(method: String, params: JSONObject = JSONObject(), timeoutMs: Long = 3500L): JSONObject? {
        if (!running) return null
        val id = nextId.getAndIncrement()
        val pendingRequest = PendingRequest()
        pending[id] = pendingRequest
        send(JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method).put("params", params))
        pendingRequest.latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        pending.remove(id)
        return pendingRequest.result
    }

    fun requestAsync(method: String, params: JSONObject = JSONObject(), callback: (JSONObject?) -> Unit): Int? {
        if (!running) return null
        val id = nextId.getAndIncrement()
        val pendingRequest = PendingRequest()
        pending[id] = pendingRequest
        send(JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method).put("params", params))
        Thread({
            // Never allow a broken language server to leave an editor request pending forever.
            pendingRequest.latch.await(8000L, TimeUnit.MILLISECONDS)
            pending.remove(id)
            callback(pendingRequest.result)
        }, "Bluebird-LSP-Request-$id").apply { isDaemon = true }.start()
        return id
    }

    fun cancelRequest(id: Int) {
        pending.remove(id)?.latch?.countDown()
        if (running) notify("$/cancelRequest", JSONObject().put("id", id))
    }

    fun close() {
        running = false
        runCatching { writer.close() }
        runCatching { input.close() }
        runCatching { process.destroy() }
        pending.values.forEach { it.latch.countDown() }
        pending.clear()
    }

    private fun send(payload: JSONObject) {
        if (!running) return
        synchronized(writer) {
            runCatching {
                val body = payload.toString()
                val bytes = body.toByteArray(StandardCharsets.UTF_8)
                writer.write("Content-Length: ${bytes.size}\r\n")
                writer.write("Content-Type: application/vscode-jsonrpc; charset=utf-8\r\n\r\n")
                writer.write(body)
                writer.flush()
            }.onFailure { running = false }
        }
    }

    private fun readLoop() {
        while (running) {
            try {
                var contentLength = -1
                while (true) {
                    val line = readHeaderLine() ?: return
                    if (line.isEmpty()) break
                    val separator = line.indexOf(':')
                    if (separator > 0 && line.substring(0, separator).equals("Content-Length", true)) {
                        contentLength = line.substring(separator + 1).trim().toIntOrNull() ?: -1
                    }
                }
                if (contentLength < 0) continue
                val bytes = ByteArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val count = input.read(bytes, read, contentLength - read)
                    if (count < 0) return
                    read += count
                }
                handle(JSONObject(String(bytes, StandardCharsets.UTF_8)))
            } catch (_: Exception) {
                running = false
            }
        }
    }

    private fun readHeaderLine(): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (true) {
            val value = input.read()
            if (value < 0) return if (bytes.size() == 0) null else bytes.toString(StandardCharsets.US_ASCII.name())
            if (value == '\n'.code) {
                val raw = bytes.toString(StandardCharsets.US_ASCII.name())
                return raw.removeSuffix("\r")
            }
            bytes.write(value)
        }
    }

    private fun handle(message: JSONObject) {
        if (message.has("method") && !message.has("id")) {
            notificationHandler?.invoke(message.optString("method"), message.optJSONObject("params") ?: JSONObject())
            return
        }
        if (!message.has("id") || !message.has("result") && !message.has("error")) return
        val id = message.optInt("id", -1)
        if (id < 0) return
        pending[id]?.let { waiter ->
            waiter.result = message.optJSONObject("result")
            waiter.error = message.optJSONObject("error")
            waiter.latch.countDown()
        }
    }

    companion object {
        fun start(command: List<String>, workingDirectory: String? = null): JsonRpcStdioTransport? {
            if (command.isEmpty()) return null
            return runCatching {
                val process = ProcessBuilder(command).apply {
                    if (!workingDirectory.isNullOrBlank()) directory(java.io.File(workingDirectory))
                    redirectErrorStream(false)
                }.start()
                JsonRpcStdioTransport(process, BufferedInputStream(process.inputStream), BufferedWriter(OutputStreamWriter(process.outputStream, StandardCharsets.UTF_8)))
            }.getOrNull()
        }
    }
}

/** Minimal LSP client for servers that expose HTML/CSS/JavaScript/TypeScript over stdio. */
class StdioLanguageServerClient(
    private val command: List<String>,
    private val workingDirectory: String? = null,
) : LanguageServerClient {
    private var transport: JsonRpcStdioTransport? = null
    private var rootPath: String = ""
    private var capabilitiesValue: LspServerCapabilities = LspServerCapabilities()
    private val versions = ConcurrentHashMap<String, Int>()

    override fun initialize(rootPath: String): Boolean {
        shutdown()
        this.rootPath = rootPath
        val t = JsonRpcStdioTransport.start(command, workingDirectory ?: rootPath) ?: return false
        transport = t
        val rootUri = Uri.fromFile(java.io.File(rootPath)).toString()
        val result = t.request("initialize", JSONObject()
            .put("processId", android.os.Process.myPid())
            .put("rootUri", rootUri)
            .put("capabilities", JSONObject()
                .put("textDocument", JSONObject()
                    .put("definition", JSONObject())
                    .put("references", JSONObject())
                    .put("rename", JSONObject())
                    .put("hover", JSONObject())
                    .put("publishDiagnostics", JSONObject())))
        )
        if (result == null) { shutdown(); return false }
        capabilitiesValue = LspServerCapabilities.fromInitialize(result)
        t.notificationHandler = { method, params ->
            if (method == "textDocument/publishDiagnostics") diagnosticsHandler?.invoke(parseDiagnostics(params))
        }
        t.notify("initialized", JSONObject())
        return true
    }

    override fun shutdown() {
        transport?.let { runCatching { it.request("shutdown", JSONObject(), 1000) }; runCatching { it.notify("exit") }; it.close() }
        transport = null
    }

    @Volatile var diagnosticsHandler: ((Pair<String?, List<LspDiagnostic>>) -> Unit)? = null

    override fun didOpen(filePath: String, text: String, languageId: String) {
        val uri = fileToUri(filePath)
        versions[uri] = 1
        transport?.notify("textDocument/didOpen", JSONObject().put("textDocument", JSONObject().put("uri", uri).put("languageId", languageId).put("version", 1).put("text", text)))
    }

    override fun didChange(filePath: String, text: String) {
        val uri = fileToUri(filePath)
        val version = (versions[uri] ?: 0) + 1
        versions[uri] = version
        transport?.notify("textDocument/didChange", JSONObject().put("textDocument", JSONObject().put("uri", uri).put("version", version)).put("contentChanges", org.json.JSONArray().put(JSONObject().put("text", text))))
    }

    override fun didClose(filePath: String) {
        val uri = fileToUri(filePath)
        versions.remove(uri)
        transport?.notify("textDocument/didClose", JSONObject().put("textDocument", JSONObject().put("uri", uri)))
    }

    override fun definition(filePath: String, offset: Int): DefinitionLocation? = null
    override fun references(filePath: String, offset: Int): List<ReferenceLocation> = emptyList()
    override fun rename(filePath: String, offset: Int, newName: String): Map<String, String> = emptyMap()
    override fun hover(filePath: String, offset: Int): HoverInfo? = null

    override fun completion(filePath: String, offset: Int): List<LspCompletionItem> =
        parseCompletionResult(rawRequest("textDocument/completion", textPositionParams(filePath, offset)))

    override fun codeActions(filePath: String, startOffset: Int, endOffset: Int): List<LspCodeAction> {
        if (!capabilitiesValue.codeAction) return emptyList()
        val text = runCatching { java.io.File(filePath).readText() }.getOrDefault("")
        return parseCodeActions(rawRequest("textDocument/codeAction", rangeParams(filePath, text, startOffset, endOffset)))
    }

    override fun formatDocument(filePath: String): List<JSONObject> {
        if (!capabilitiesValue.formatting) return emptyList()
        return rawRequest("textDocument/formatting", JSONObject()
            .put("textDocument", JSONObject().put("uri", fileToUri(filePath)))
            .put("options", JSONObject().put("tabSize", 2).put("insertSpaces", true)))
            ?.optJSONArray("result")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
    }

    override fun semanticTokens(filePath: String): List<LspSemanticToken> {
        if (!capabilitiesValue.semanticTokens) return emptyList()
        return parseSemanticTokens(rawRequest("textDocument/semanticTokens/full", JSONObject()
            .put("textDocument", JSONObject().put("uri", fileToUri(filePath)))))
    }

    override fun capabilities(): LspServerCapabilities = capabilitiesValue

    fun requestCompletionAsync(filePath: String, text: String, offset: Int, callback: (List<LspCompletionItem>) -> Unit): Int? {
        val params = textPositionParamsFromText(filePath, text, offset)
        return transport?.requestAsync("textDocument/completion", params) { callback(parseCompletionResult(it)) }
    }

    fun requestSemanticTokensAsync(filePath: String, callback: (List<LspSemanticToken>) -> Unit): Int? {
        if (!capabilitiesValue.semanticTokens) return null
        val params = JSONObject().put("textDocument", JSONObject().put("uri", fileToUri(filePath)))
        return transport?.requestAsync("textDocument/semanticTokens/full", params) { callback(parseSemanticTokens(it)) }
    }

    fun requestDiagnosticsAsync(filePath: String, text: String, callback: (List<LspDiagnostic>) -> Unit): Int? {
        if (!capabilitiesValue.diagnostics) return null
        // Diagnostics are normally pushed by publishDiagnostics after didOpen/didChange.
        val uri = fileToUri(filePath)
        diagnosticsHandler = { (reportedUri, diagnostics) ->
            if (reportedUri == null || reportedUri == uri) callback(diagnostics)
        }
        didOpen(filePath, text, languageIdFor(filePath))
        return null
    }

    fun cancelRequest(id: Int) { transport?.cancelRequest(id) }

    fun rawRequest(method: String, params: JSONObject = JSONObject(), timeoutMs: Long = 3500L): JSONObject? = transport?.request(method, params, timeoutMs)

    fun isRunning(): Boolean = transport != null
    fun workspaceRoot(): String = rootPath

    private fun rangeParams(filePath: String, text: String, startOffset: Int, endOffset: Int): JSONObject {
        fun position(offset: Int): JSONObject {
            val safe = offset.coerceIn(0, text.length)
            val line = text.take(safe).count { it == '\n' }
            val lastNewline = text.lastIndexOf('\n', safe - 1)
            return JSONObject().put("line", line).put("character", safe - (lastNewline + 1))
        }
        return JSONObject().put("textDocument", JSONObject().put("uri", fileToUri(filePath)))
            .put("range", JSONObject().put("start", position(startOffset)).put("end", position(endOffset)))
            .put("context", JSONObject().put("diagnostics", JSONArray()))
    }

    private fun textPositionParamsFromText(filePath: String, text: String, offset: Int): JSONObject {
        val safe = offset.coerceIn(0, text.length)
        val line = text.take(safe).count { it == '\n' }
        val lastNewline = text.lastIndexOf('\n', safe - 1)
        val character = safe - (lastNewline + 1)
        return JSONObject().put("textDocument", JSONObject().put("uri", fileToUri(filePath))).put("position", JSONObject().put("line", line).put("character", character))
    }

    private fun textPositionParams(filePath: String, offset: Int): JSONObject {
        val text = runCatching { java.io.File(filePath).readText() }.getOrDefault("")
        return textPositionParamsFromText(filePath, text, offset)
    }

    private fun fileToUri(path: String): String = Uri.fromFile(java.io.File(path)).toString()
    private fun languageIdFor(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "html"
        "css", "scss", "sass", "less" -> "css"
        "ts", "tsx" -> "typescript"
        "jsx" -> "javascriptreact"
        else -> "javascript"
    }
}

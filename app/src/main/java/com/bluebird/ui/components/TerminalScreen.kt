package com.bluebird.ui.components

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluebird.core.filesystem.BluebirdFileSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────
// Terminal colour palette — base colours are now mutable (see `color`
// command); these are just the defaults on boot. Kept close to
// monochrome like real cmd.exe; semantic accents (error/info/etc.)
// stay fixed regardless of `color` for readability.
// ─────────────────────────────────────────────────────────────────
private val DefaultBg   = Color(0xFF0C0C0C)
private val DefaultText = Color(0xFFF2F2F2)
private val TermError   = Color(0xFFF2F2F2)   // cmd doesn't colour its own errors — kept plain
private val TermSuccess = Color(0xFF4EC9B0)
private val TermWarning = Color(0xFFDCDCAA)
private val TermInfo    = Color(0xFF9CDCFE)

// ─────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────
enum class TermLineType { INPUT, OUTPUT, ERROR, INFO, SUCCESS, WARNING, SYSTEM }

data class TermLine(
    val text: String,
    val type: TermLineType = TermLineType.OUTPUT,
    val segments: List<TermSegment>? = null
)

data class TermSegment(val text: String, val color: Color)

// ─────────────────────────────────────────────────────────────────
// Canonical CMD commands, plus Unix verbs kept as aliases.
// ─────────────────────────────────────────────────────────────────
private val CANONICAL_COMMANDS = listOf(
    "help", "cls", "dir", "cd", "mkdir", "rmdir", "copy", "move", "del",
    "ren", "type", "echo", "tree", "find", "where", "ver", "vol", "history", "exit",
    "attrib", "xcopy", "sort", "findstr", "fc", "more",
    "path", "set", "title", "color", "prompt",
    "systeminfo", "date", "time", "whoami",
    "chkdsk", "format",
    "tasklist", "taskkill",
    "apps", "open", "openapp", "close", "gui", "tty", "music",
    "settings", "recent", "theme", "wallpaper", "battery",
    "alias", "run", "rem", "pause",
    "admin", "clip", "share", "js", "bbfetch"
)

private val ALIASES = mapOf(
    "ls" to "dir", "ll" to "dir", "la" to "dir",
    "clear" to "cls",
    "md" to "mkdir",
    "rd" to "rmdir",
    "cp" to "copy",
    "mv" to "move",
    "cat" to "type",
    "rename" to "ren",
    "erase" to "del",
    "chdir" to "cd",
    "quit" to "exit",
    "launch" to "open", "start" to "open",
    "terminal" to "cls"
)

private val BUILT_IN_COMMANDS = CANONICAL_COMMANDS + ALIASES.keys + "rm" + "pwd"

// ─────────────────────────────────────────────────────────────────
// Small file-backed key=value store, used for persisted settings and
// aliases. Lives under C:\System\Config so it's inside the normal
// Bluebird sandbox and shows up in `dir` like anything else.
// ─────────────────────────────────────────────────────────────────
private fun configFile() = File(BluebirdFileSystem.root, "System/Config/bluebird.cfg")
private fun aliasFile() = File(BluebirdFileSystem.root, "System/Config/aliases.cfg")

private fun loadKeyValueFile(f: File): Map<String, String> {
    if (!f.exists()) return emptyMap()
    val map = mutableMapOf<String, String>()
    f.readLines().forEach { line ->
        val idx = line.indexOf('=')
        if (idx > 0) map[line.substring(0, idx)] = line.substring(idx + 1)
    }
    return map
}

private fun saveKeyValueFile(f: File, map: Map<String, String>) {
    try {
        f.parentFile?.mkdirs()
        f.writeText(map.entries.joinToString("\n") { "${it.key}=${it.value}" })
    } catch (_: Exception) { /* best-effort persistence */ }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
    return "%.1f %s".format(value, units[unit])
}

// ─────────────────────────────────────────────────────────────────
// TerminalScreen
//
// onOpenApp / onEnterTty / onEnterGui / onExit are the seams for
// Phase 3/4 wiring (App Registry + the GUI↔TTY state machine live at
// the launcher level, not inside this composable). Until that lands,
// the corresponding commands print an honest "not wired up yet"
// message instead of pretending to switch modes.
// ─────────────────────────────────────────────────────────────────
@Composable
fun TerminalScreen(
    isDark: Boolean,
    onOpenApp: ((String) -> Unit)? = null,
    onEnterTty: (() -> Unit)? = null,
    onEnterGui: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusReq  = remember { FocusRequester() }

    // ── Core state ───────────────────────────────────────────────
    var currentDir by remember { mutableStateOf(BluebirdFileSystem.homeDir()) }
    var input      by remember { mutableStateOf("") }
    val lines      = remember { mutableStateListOf<TermLine>() }
    val history    = remember { mutableStateListOf<String>() }
    var historyIdx by remember { mutableIntStateOf(-1) }
    var isRunning  by remember { mutableStateOf(false) }

    // Autocomplete
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    var showSuggest by remember { mutableStateOf(false) }
    var tabCycleIdx by remember { mutableIntStateOf(0) }

    // `more` pagination continuation
    var moreBuffer by remember { mutableStateOf<List<String>?>(null) }

    // Explorer / admin mode — mounts D:\ (real device storage) when true
    var adminMode by remember { mutableStateOf(false) }

    // Shell customization state
    var windowTitle by remember { mutableStateOf("") }
    var bgColor by remember { mutableStateOf(DefaultBg) }
    var fgColor by remember { mutableStateOf(DefaultText) }
    var promptFormat by remember { mutableStateOf("\$P\$G") }

    // Session env vars (SET) — not persisted, matching real cmd.exe
    val envVars = remember { mutableStateMapOf<String, String>() }

    // Persisted settings + aliases
    val settingsMap = remember { mutableStateMapOf<String, String>().apply { putAll(loadKeyValueFile(configFile())) } }
    val aliasMap = remember { mutableStateMapOf<String, String>().apply { putAll(loadKeyValueFile(aliasFile())) } }

    // PATH-like search locations for `where`
    val searchPath = remember { mutableStateListOf("C:\\Program Files") }

    // Simulated "running" Bluebird apps until a real App Registry exists
    val runningApps = remember { mutableStateListOf<String>() }

    // ── JS console (sandboxed WebView, no Kotlin bridge exposed) ───
    val jsWebView = remember { WebView(context).apply { settings.javaScriptEnabled = true } }
    var jsReady by remember { mutableStateOf(false) }

    fun evalJs(code: String, onResult: (String) -> Unit) {
        jsWebView.evaluateJavascript(code) { result -> onResult(result ?: "") }
    }

    // ── Helpers ──────────────────────────────────────────────────
    fun scrollBottom() {
        scope.launch { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1) }
    }

    fun emit(text: String, type: TermLineType = TermLineType.OUTPUT) {
        lines.add(TermLine(text, type))
        scrollBottom()
    }

    fun emitRich(segments: List<TermSegment>) {
        lines.add(TermLine(segments.joinToString("") { it.text }, segments = segments))
        scrollBottom()
    }

    fun emitBlank() = emit("")
    fun accessDenied() = emit("Access is denied.", TermLineType.ERROR)

    fun toReal(path: String) = BluebirdFileSystem.toReal(path, currentDir, adminMode)
    fun allowed(f: File) = BluebirdFileSystem.isAccessAllowed(f, adminMode)
    fun virtual(f: File) = BluebirdFileSystem.toVirtual(f, adminMode)
    fun driveLetter(f: File) = if (virtual(f).startsWith("D:")) "D" else "C"

    fun prompt(): String {
        var s = promptFormat
        s = s.replace("\$P", virtual(currentDir))
        s = s.replace("\$G", ">")
        s = s.replace("\$D", SimpleDateFormat("EEE MM/dd/yyyy", Locale.US).format(Date()))
        s = s.replace("\$T", SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()))
        return s
    }

    // ── Autocomplete logic ────────────────────────────────────────
    fun buildSuggestions(partial: String) {
        val parts = partial.trimStart().split("\\s+".toRegex())
        suggestions = when {
            parts.size <= 1 -> {
                val p = parts.firstOrNull() ?: ""
                BUILT_IN_COMMANDS.filter { it.startsWith(p, ignoreCase = true) }.sorted()
            }
            else -> {
                val last = parts.last()
                val dir = if (last.contains("\\") || last.contains("/")) {
                    val parent = last.substringBeforeLast("\\").substringBeforeLast("/")
                    toReal(parent)
                } else currentDir
                val prefix = last.substringAfterLast("\\").substringAfterLast("/")
                dir.listFiles()
                    ?.filter { it.name.startsWith(prefix, ignoreCase = true) }
                    ?.map { it.name + if (it.isDirectory) "\\" else "" }
                    ?.sorted() ?: emptyList()
            }
        }
        showSuggest = suggestions.isNotEmpty()
        tabCycleIdx = 0
    }

    fun applyTab() {
        if (suggestions.isEmpty()) { buildSuggestions(input); return }
        val parts  = input.split("\\s+".toRegex()).toMutableList()
        val suffix = suggestions[tabCycleIdx % suggestions.size]
        if (parts.size <= 1) input = suffix
        else { parts[parts.lastIndex] = suffix; input = parts.joinToString(" ") }
        tabCycleIdx = (tabCycleIdx + 1) % suggestions.size
    }

    // ── `more` pagination ────────────────────────────────────────
    fun showMorePage(pending: List<String>) {
        val pageSize = 20
        pending.take(pageSize).forEach { emit(it) }
        val remaining = pending.drop(pageSize)
        if (remaining.isNotEmpty()) {
            emit("-- More  (press Enter) --", TermLineType.INFO)
            moreBuffer = remaining
        } else {
            moreBuffer = null
        }
    }

    // ── DIR — authentic cmd.exe directory listing ───────────────────
    fun dirListing(target: File, bare: Boolean, recursive: Boolean, showHidden: Boolean) {
        if (!allowed(target)) { accessDenied(); return }
        if (!target.exists()) { emit("The system cannot find the path specified.", TermLineType.ERROR); return }

        if (recursive) {
            var fileCount = 0; var dirCount = 0; var totalBytes = 0L
            target.walkTopDown().forEach { f ->
                if (f == target) return@forEach
                if (!showHidden && f.name.startsWith(".")) return@forEach
                if (bare) emit(virtual(f))
                if (f.isDirectory) dirCount++ else { fileCount++; totalBytes += f.length() }
            }
            if (!bare) {
                emit(" Directory of ${virtual(target)} and subdirectories")
                emit("${fileCount.toString().padStart(15)} File(s) ${"%,d".format(totalBytes).padStart(14)} bytes")
                emit("${dirCount.toString().padStart(15)} Dir(s)")
            }
            return
        }

        val files = target.listFiles()
            ?.filter { showHidden || !it.name.startsWith(".") }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: run { emit("The system cannot find the path specified.", TermLineType.ERROR); return }

        if (bare) { files.forEach { emit(it.name) }; return }

        val sdf = SimpleDateFormat("MM/dd/yyyy  hh:mm a", Locale.US)
        emit(" Volume in drive ${driveLetter(target)} is BLUEBIRD")
        emit(" Volume Serial Number is 1337-0001")
        emitBlank()
        emit(" Directory of ${virtual(target)}")
        emitBlank()

        var fileCount = 0; var dirCount = 0; var totalBytes = 0L
        files.forEach { f ->
            val date = sdf.format(Date(f.lastModified()))
            if (f.isDirectory) { dirCount++; emit("$date    <DIR>          ${f.name}") }
            else { fileCount++; totalBytes += f.length(); emit("$date    ${"%,d".format(f.length()).padStart(14)} ${f.name}") }
        }
        if (files.isEmpty()) emit("File Not Found")
        emitBlank()
        emit("${fileCount.toString().padStart(15)} File(s) ${"%,d".format(totalBytes).padStart(14)} bytes")
        emit("${dirCount.toString().padStart(15)} Dir(s)  ${"%,d".format(BluebirdFileSystem.freeBytes()).padStart(14)} bytes free")
    }

    // ── Command executor ──────────────────────────────────────────
    fun execute(rawIn: String) {
        val trimmedRaw = rawIn.trim()

        if (trimmedRaw.isBlank()) {
            moreBuffer?.let { showMorePage(it) }
            return
        }

        // normalize a couple of no-space cmd.exe shortcuts
        var trimmed = trimmedRaw
        if (trimmed.equals("cd..", ignoreCase = true)) trimmed = "cd .."
        if (trimmed.equals("cd\\", ignoreCase = true)) trimmed = "cd \\"

        // %VAR% expansion
        trimmed = trimmed.replace(Regex("%([A-Za-z_][A-Za-z0-9_]*)%")) { m -> envVars[m.groupValues[1]] ?: "" }

        if (history.isEmpty() || history.last() != trimmedRaw) history.add(trimmedRaw)
        historyIdx = -1

        emitRich(listOf(TermSegment("${prompt()} ", fgColor), TermSegment(trimmedRaw, fgColor)))

        val firstPass = trimmed.split("\\s+".toRegex())
        var typed = firstPass[0].lowercase()
        var args = firstPass.drop(1)

        // one-level user alias expansion
        aliasMap[typed]?.let { expansion ->
            val expParts = expansion.trim().split("\\s+".toRegex())
            typed = expParts[0].lowercase()
            args = expParts.drop(1) + args
        }

        val cmd  = ALIASES[typed] ?: typed
        val rest = args.joinToString(" ")

        when (cmd) {
            // ── Navigation ──────────────────────────────────────────
            "cd" -> {
                if (typed == "pwd" || args.isEmpty()) {
                    emit(virtual(currentDir))
                } else {
                    val target = toReal(rest)
                    when {
                        !allowed(target) -> accessDenied()
                        !target.exists() -> emit("The system cannot find the path specified.", TermLineType.ERROR)
                        !target.isDirectory -> emit("The directory name is invalid.", TermLineType.ERROR)
                        else -> currentDir = target
                    }
                }
            }

            "dir" -> {
                val flags = args.filter { it.startsWith("/") }.map { it.lowercase() }
                val pathArg = args.firstOrNull { !it.startsWith("/") }
                val target = if (pathArg != null) toReal(pathArg) else currentDir
                dirListing(target, bare = "/b" in flags, recursive = "/s" in flags, showHidden = "/a" in flags)
            }

            "tree" -> {
                val root = if (args.isNotEmpty()) toReal(rest) else currentDir
                if (!allowed(root)) { accessDenied(); return }
                if (!root.exists()) { emit("The system cannot find the path specified.", TermLineType.ERROR); return }
                emit(virtual(root))
                fun walk(dir: File, prefix: String) {
                    val kids = dir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() } ?: return
                    kids.forEachIndexed { i, kid ->
                        val last = i == kids.lastIndex
                        emit(prefix + (if (last) "└── " else "├── ") + kid.name)
                        walk(kid, prefix + (if (last) "    " else "│   "))
                    }
                }
                walk(root, "")
            }

            // ── File / directory ops ─────────────────────────────────
            "mkdir" -> {
                if (args.isEmpty()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val f = toReal(rest)
                when {
                    !allowed(f) -> accessDenied()
                    f.exists() -> emit("A subdirectory or file $rest already exists.", TermLineType.ERROR)
                    !f.mkdirs() -> emit("The system cannot create the directory.", TermLineType.ERROR)
                }
            }

            "rmdir" -> {
                if (args.isEmpty()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val recursive = "/s" in args.map { it.lowercase() }
                val name = args.firstOrNull { !it.startsWith("/") } ?: ""
                val f = toReal(name)
                when {
                    !allowed(f) -> accessDenied()
                    !f.exists() -> emit("The system cannot find the file specified.", TermLineType.ERROR)
                    !f.isDirectory -> emit("The directory name is invalid.", TermLineType.ERROR)
                    else -> {
                        val ok = if (recursive) f.deleteRecursively() else f.delete()
                        if (!ok) emit("The directory is not empty.", TermLineType.ERROR)
                    }
                }
            }

            "rm" -> {
                if (args.isEmpty()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val recursive = args.any { it == "-r" || it == "-rf" || it == "-fr" }
                args.filter { !it.startsWith("-") }.forEach { name ->
                    val f = toReal(name)
                    when {
                        !allowed(f) -> accessDenied()
                        !f.exists() -> emit("rm: cannot remove '$name': No such file or directory", TermLineType.ERROR)
                        f.isDirectory && !recursive -> emit("rm: cannot remove '$name': Is a directory (use -r)", TermLineType.ERROR)
                        else -> { val ok = if (f.isDirectory) f.deleteRecursively() else f.delete(); if (!ok) accessDenied() }
                    }
                }
            }

            "del" -> {
                if (args.isEmpty()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val recursive = "/s" in args.map { it.lowercase() }
                args.filter { !it.startsWith("/") }.forEach { name ->
                    if (recursive) {
                        var any = false
                        currentDir.walkTopDown().filter { it.isFile && it.name.equals(name, true) }.forEach { f ->
                            if (allowed(f)) { f.delete(); any = true }
                        }
                        if (!any) emit("Could not find $name", TermLineType.ERROR)
                    } else {
                        val f = toReal(name)
                        when {
                            !allowed(f) -> accessDenied()
                            !f.exists() -> emit("Could not find $name", TermLineType.ERROR)
                            f.isDirectory -> accessDenied()
                            !f.delete() -> emit("The process cannot access the file because it is being used by another process.", TermLineType.ERROR)
                        }
                    }
                }
            }

            "copy", "move" -> {
                if (args.size < 2) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val src  = toReal(args[0])
                val dest = toReal(args[1])
                when {
                    !allowed(src) || !allowed(dest) -> accessDenied()
                    !src.exists() -> emit("The system cannot find the file specified.", TermLineType.ERROR)
                    else -> {
                        val finalDest = if (dest.isDirectory) File(dest, src.name) else dest
                        try {
                            if (src.isDirectory) src.copyRecursively(finalDest, overwrite = true)
                            else src.copyTo(finalDest, overwrite = true)
                            if (cmd == "move") src.deleteRecursively()
                            emit("        1 file(s) copied.")
                        } catch (_: Exception) { accessDenied() }
                    }
                }
            }

            "xcopy" -> {
                if (args.size < 2) { emit("Invalid number of parameters", TermLineType.ERROR); return }
                val src = toReal(args[0]); val dest = toReal(args[1])
                when {
                    !allowed(src) || !allowed(dest) -> accessDenied()
                    !src.exists() -> emit("File not found - ${args[0]}", TermLineType.ERROR)
                    else -> {
                        var count = 0
                        try {
                            if (src.isDirectory) {
                                src.walkTopDown().filter { it.isFile }.forEach { f ->
                                    val target = File(dest, f.relativeTo(src).path)
                                    target.parentFile?.mkdirs()
                                    f.copyTo(target, overwrite = true)
                                    count++
                                }
                            } else {
                                val finalDest = if (dest.isDirectory) File(dest, src.name) else dest
                                src.copyTo(finalDest, overwrite = true)
                                count = 1
                            }
                            emit("$count File(s) copied")
                        } catch (_: Exception) { accessDenied() }
                    }
                }
            }

            "ren" -> {
                if (args.size < 2) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val src = toReal(args[0])
                when {
                    !allowed(src) -> accessDenied()
                    !src.exists() -> emit("The system cannot find the file specified.", TermLineType.ERROR)
                    else -> if (!src.renameTo(File(src.parentFile, args[1]))) accessDenied()
                }
            }

            "type" -> {
                if (args.isEmpty()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                args.forEach { name ->
                    val f = toReal(name)
                    when {
                        !allowed(f) -> accessDenied()
                        !f.exists() -> emit("The system cannot find the file specified.", TermLineType.ERROR)
                        f.isDirectory -> accessDenied()
                        f.length() > 500_000 -> emit("File too large to display (>500KB)", TermLineType.WARNING)
                        else -> f.readLines().forEach { emit(it) }
                    }
                }
            }

            "more" -> {
                if (rest.isBlank()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val f = toReal(rest)
                when {
                    !allowed(f) -> accessDenied()
                    !f.exists() || f.isDirectory -> emit("The system cannot find the file specified.", TermLineType.ERROR)
                    else -> showMorePage(f.readLines())
                }
            }

            "sort" -> {
                if (rest.isBlank()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val f = toReal(rest)
                when {
                    !allowed(f) -> accessDenied()
                    !f.exists() || f.isDirectory -> emit("The system cannot find the file specified.", TermLineType.ERROR)
                    else -> f.readLines().sorted().forEach { emit(it) }
                }
            }

            "attrib" -> {
                if (args.isEmpty()) {
                    currentDir.listFiles()?.sortedBy { it.name.lowercase() }?.forEach { f ->
                        val attrs = buildString {
                            append(if (f.isDirectory) "D" else "A")
                            if (f.name.startsWith(".")) append("H")
                            if (!f.canWrite()) append("R")
                        }
                        emit("${attrs.padEnd(6)} ${virtual(f)}")
                    }
                    return
                }
                val flag = args[0]; val nameArg = args.getOrNull(1)
                if (nameArg == null) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val f = toReal(nameArg)
                when {
                    !allowed(f) -> accessDenied()
                    !f.exists() -> emit("File not found - $nameArg", TermLineType.ERROR)
                    else -> when (flag.lowercase()) {
                        "+h" -> if (!f.name.startsWith(".")) f.renameTo(File(f.parentFile, ".${f.name}"))
                        "-h" -> if (f.name.startsWith(".")) f.renameTo(File(f.parentFile, f.name.removePrefix(".")))
                        "+r" -> f.setWritable(false)
                        "-r" -> f.setWritable(true)
                        else -> emit("Invalid attribute.", TermLineType.ERROR)
                    }
                }
            }

            // ── find (text search, cmd's FIND — not a file search) ────
            "find" -> {
                val quoted = Regex("\"([^\"]*)\"").find(rest)
                if (quoted == null || args.size < 2) { emit("FIND: Parameter format not correct", TermLineType.ERROR); return }
                val pattern = quoted.groupValues[1]
                val fileArg = rest.substring(quoted.range.last + 1).trim()
                val f = toReal(fileArg)
                if (!allowed(f)) { accessDenied(); return }
                if (!f.exists() || f.isDirectory) { emit("File not found - $fileArg", TermLineType.ERROR); return }
                emit("---------- ${f.name}")
                var any = false
                f.readLines().forEach { line -> if (line.contains(pattern, ignoreCase = "/i" in args)) { emit(line); any = true } }
                if (!any) emit("")
            }

            "findstr" -> {
                val clean = args.filterNot { it.startsWith("/") }
                if (clean.size < 2) { emit("FINDSTR: Search string not specified.", TermLineType.ERROR); return }
                val ignoreCase = "/i" in args.map { it.lowercase() }
                val regex = try {
                    if (ignoreCase) Regex(clean[0], RegexOption.IGNORE_CASE) else Regex(clean[0])
                } catch (_: Exception) { null }
                val f = toReal(clean[1])
                when {
                    regex == null -> emit("FINDSTR: Invalid pattern", TermLineType.ERROR)
                    !allowed(f) -> accessDenied()
                    !f.exists() -> emit("FINDSTR: ${clean[1]}: No such file", TermLineType.ERROR)
                    else -> {
                        var any = false
                        f.readLines().forEach { line -> if (regex.containsMatchIn(line)) { emit(line); any = true } }
                        if (!any) emit("")
                    }
                }
            }

            "fc" -> {
                if (args.size < 2) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val f1 = toReal(args[0]); val f2 = toReal(args[1])
                if (!f1.exists() || !f2.exists()) { emit("The system cannot find the file specified.", TermLineType.ERROR); return }
                val l1 = f1.readLines(); val l2 = f2.readLines()
                emit("Comparing files ${virtual(f1)} and ${virtual(f2)}")
                var diffs = 0
                for (i in 0 until maxOf(l1.size, l2.size)) {
                    val a = l1.getOrNull(i); val b = l2.getOrNull(i)
                    if (a != b) {
                        diffs++
                        emit("***** ${args[0]}"); a?.let { emit(it) }
                        emit("***** ${args[1]}"); b?.let { emit(it) }
                        emit("*****")
                    }
                }
                if (diffs == 0) emit("FC: no differences encountered")
            }

            "where" -> {
                if (args.isEmpty()) { emit("ERROR: Please enter a filename.", TermLineType.ERROR); return }
                val name = args[0]
                val hits = mutableListOf<String>()
                currentDir.listFiles()?.filter { it.name.equals(name, true) }?.forEach { hits += virtual(it) }
                File(BluebirdFileSystem.root, "Program Files").walkTopDown().take(2000)
                    .filter { it.name.equals(name, true) }.forEach { hits += virtual(it) }
                if (hits.isEmpty()) emit("INFO: Could not find files for the given pattern(s).", TermLineType.ERROR)
                else hits.distinct().forEach { emit(it) }
            }

            "path" -> {
                if (rest.isBlank()) emit("PATH=" + searchPath.joinToString(";"))
                else { searchPath.clear(); searchPath.addAll(rest.split(";")) }
            }

            "set" -> {
                if (rest.isBlank()) {
                    if (envVars.isEmpty()) emit("Environment variable not defined", TermLineType.ERROR)
                    else envVars.entries.sortedBy { it.key }.forEach { (k, v) -> emit("$k=$v") }
                } else {
                    val eq = rest.indexOf('=')
                    if (eq <= 0) emit("Environment variable $rest not defined", TermLineType.ERROR)
                    else envVars[rest.substring(0, eq)] = rest.substring(eq + 1)
                }
            }

            "title" -> windowTitle = rest

            "color" -> when (rest.lowercase()) {
                "", "reset", "07" -> { bgColor = DefaultBg; fgColor = DefaultText }
                "green", "0a" -> { bgColor = Color.Black; fgColor = Color(0xFF00FF00) }
                "amber", "06" -> { bgColor = Color.Black; fgColor = Color(0xFFFFB000) }
                "blue", "01" -> { bgColor = Color(0xFF000080); fgColor = Color.White }
                else -> emit("Invalid color code.", TermLineType.ERROR)
            }

            "prompt" -> promptFormat = if (rest.isBlank()) "\$P\$G" else rest

            "systeminfo" -> {
                emitBlank()
                emit("Host Name:            ${Build.MODEL}")
                emit("OS Version:           Android ${Build.VERSION.RELEASE} (Bluebird 1.0)")
                emit("System Manufacturer:  ${Build.MANUFACTURER}")
                val stat = StatFs(BluebirdFileSystem.root.absolutePath)
                emit("Total Storage:        ${formatFileSize(stat.totalBytes)}")
                emit("Available Storage:    ${formatFileSize(stat.freeBytes)}")
                try {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val mi = ActivityManager.MemoryInfo()
                    am.getMemoryInfo(mi)
                    emit("Total Memory:         ${formatFileSize(mi.totalMem)}")
                    emit("Available Memory:     ${formatFileSize(mi.availMem)}")
                } catch (_: Exception) { }
            }

            "date" -> emit(SimpleDateFormat("EEE MM/dd/yyyy", Locale.US).format(Date()))
            "time" -> emit(SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()))
            "whoami" -> emit(BluebirdFileSystem.currentUser)

            "chkdsk" -> {
                emit("Bluebird is verifying files and folders...")
                BluebirdFileSystem.ensureStructure()
                emit("Verification complete.")
                emit("No problems found — any missing standard folders were recreated.", TermLineType.SUCCESS)
            }

            "format" -> {
                val target = args.getOrNull(0)?.lowercase()
                val confirmed = args.any { it.equals("/y", true) }
                when {
                    target != "temp" -> emit("Access is denied. Only the Temp volume can be formatted.", TermLineType.ERROR)
                    !confirmed -> emit("WARNING: This will erase all files in C:\\Temp. Type FORMAT TEMP /Y to confirm.", TermLineType.WARNING)
                    else -> {
                        File(BluebirdFileSystem.root, "Temp").listFiles()?.forEach { it.deleteRecursively() }
                        emit("Format complete.", TermLineType.SUCCESS)
                    }
                }
            }

            "tasklist" -> {
                if (runningApps.isEmpty()) emit("No Bluebird apps are currently open.", TermLineType.INFO)
                else {
                    emit("Image Name                   Status")
                    emit("========================     ========")
                    runningApps.forEach { emit("${it.padEnd(28)} Running") }
                }
            }

            "taskkill" -> {
                val name = args.firstOrNull { !it.startsWith("/") }
                val match = name?.let { n -> runningApps.firstOrNull { it.equals(n, true) || it.removePrefix("Bluebird ").equals(n, true) } }
                when {
                    name == null -> emit("ERROR: Invalid syntax.", TermLineType.ERROR)
                    match != null -> { runningApps.remove(match); emit("SUCCESS: The process \"$match\" has been terminated.", TermLineType.SUCCESS) }
                    else -> emit("ERROR: The process \"$name\" not found.", TermLineType.ERROR)
                }
            }

            // ── Bluebird ecosystem ─────────────────────────────────────
            "apps" -> {
                emit("Installed applications")
                emit("────────────────────────")
                BluebirdFileSystem.bluebirdApps.forEachIndexed { i, name -> emit("${i + 1}  $name") }
                if ("/all" in args.map { it.lowercase() }) {
                    try {
                        val pm = context.packageManager
                        val launchable = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                        emitBlank()
                        emit("Device applications (use OPENAPP <name>)")
                        emit("────────────────────────────────────────")
                        launchable.map { it.activityInfo.applicationInfo.loadLabel(pm).toString() }
                            .distinct().sorted().forEach { emit("   $it") }
                    } catch (_: Exception) {
                        emit("Could not list device applications.", TermLineType.ERROR)
                    }
                }
            }

            "open" -> {
                if (rest.isBlank()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val match = BluebirdFileSystem.bluebirdApps.firstOrNull {
                    it.equals(rest, true) || it.removePrefix("Bluebird ").equals(rest, true)
                }
                when {
                    match != null -> {
                        emit("Launching $match...", TermLineType.SUCCESS)
                        if (match !in runningApps) runningApps.add(match)
                        onOpenApp?.invoke(match) ?: emit("(App Registry not wired up yet — placeholder until Phase 3.)", TermLineType.WARNING)
                    }
                    else -> {
                        val f = toReal(rest)
                        if (f.exists() && allowed(f)) {
                            try {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "*/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                                emit("Opening: ${f.name}", TermLineType.SUCCESS)
                            } catch (_: Exception) {
                                emit("'$rest' is not recognized as an internal or external command,", TermLineType.ERROR)
                                emit("operable program or batch file.", TermLineType.ERROR)
                            }
                        } else {
                            emit("'$rest' is not recognized as an internal or external command,", TermLineType.ERROR)
                            emit("operable program or batch file.", TermLineType.ERROR)
                        }
                    }
                }
            }

            "openapp" -> {
                if (rest.isBlank()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                try {
                    val pm = context.packageManager
                    val intent = pm.getLaunchIntentForPackage(rest)
                        ?: pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                            .find { it.activityInfo.applicationInfo.loadLabel(pm).toString().equals(rest, ignoreCase = true) }
                            ?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
                    if (intent != null) {
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        emit("Launching $rest...", TermLineType.SUCCESS)
                    } else {
                        emit("'$rest' is not recognized as an internal or external command,", TermLineType.ERROR)
                        emit("operable program or batch file.", TermLineType.ERROR)
                    }
                } catch (_: Exception) {
                    emit("Unable to launch '$rest'.", TermLineType.ERROR)
                }
            }

            "close" -> emit("Nothing to close.", TermLineType.INFO)

            "gui" -> onEnterGui?.invoke() ?: emit("TTY↔GUI switching lives at the launcher level and isn't wired up yet (Phase 4).", TermLineType.WARNING)
            "tty" -> onEnterTty?.invoke() ?: emit("TTY mode (launcher takeover) isn't wired up yet — see Phase 4 in the design doc.", TermLineType.WARNING)
            "music" -> {
                val sub = args.getOrNull(0)?.lowercase() ?: "status"
                emit("Music: '$sub' isn't wired up yet — needs the Music Service first (Phase 5).", TermLineType.WARNING)
            }

            "settings" -> when {
                args.isEmpty() -> {
                    emit("Launching Bluebird Settings...", TermLineType.SUCCESS)
                    onOpenApp?.invoke("Bluebird Settings") ?: emit("(App Registry not wired up yet.)", TermLineType.WARNING)
                }
                args[0].equals("get", true) -> {
                    val key = args.getOrNull(1)
                    if (key == null) settingsMap.entries.sortedBy { it.key }.forEach { (k, v) -> emit("$k=$v") }
                    else emit(settingsMap[key] ?: "(not set)")
                }
                args[0].equals("set", true) -> {
                    val kv = args.drop(1).joinToString(" "); val eq = kv.indexOf('=')
                    if (eq <= 0) emit("Usage: settings set KEY=VALUE", TermLineType.ERROR)
                    else { settingsMap[kv.substring(0, eq)] = kv.substring(eq + 1); saveKeyValueFile(configFile(), settingsMap); emit("OK") }
                }
                else -> emit("Usage: settings [get|set] ...", TermLineType.ERROR)
            }

            "recent" -> {
                val docs = File(BluebirdFileSystem.homeDir(), "Documents")
                val files = docs.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }?.take(10)
                if (files.isNullOrEmpty()) emit("No recent documents.", TermLineType.INFO)
                else { emit("Recently modified in Documents:"); files.forEach { emit("  ${it.name}") } }
            }

            "theme" -> when (rest.lowercase()) {
                "dark", "" -> { bgColor = DefaultBg; fgColor = DefaultText; emit("Theme set to dark.") }
                "light" -> { bgColor = Color(0xFFF5F5F5); fgColor = Color(0xFF111111); emit("Theme set to light.") }
                else -> emit("Usage: theme [dark|light]", TermLineType.ERROR)
            }

            "wallpaper" -> {
                if (rest.isBlank()) emit(settingsMap["wallpaper"] ?: "(default)")
                else {
                    settingsMap["wallpaper"] = rest; saveKeyValueFile(configFile(), settingsMap)
                    emit("Wallpaper preference saved. Applying it to the GUI launcher isn't wired up yet (Phase 4).", TermLineType.WARNING)
                }
            }

            "battery" -> {
                try {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    emit("Battery: ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%")
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
                    val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    emit("Network: ${if (online) "Connected" else "Offline"}")
                } catch (_: Exception) { emit("Unable to read battery/network status.", TermLineType.ERROR) }
            }

            // ── Scripting ────────────────────────────────────────────
            "alias" -> {
                if (rest.isBlank()) {
                    if (aliasMap.isEmpty()) emit("No aliases defined.", TermLineType.INFO)
                    else aliasMap.entries.sortedBy { it.key }.forEach { (k, v) -> emit("$k=$v") }
                } else {
                    val eq = rest.indexOf('=')
                    if (eq <= 0) { emit("Usage: alias name=command", TermLineType.ERROR); return }
                    val name = rest.substring(0, eq).trim().lowercase()
                    if (name in CANONICAL_COMMANDS) { emit("Cannot alias a built-in command name.", TermLineType.ERROR); return }
                    aliasMap[name] = rest.substring(eq + 1).trim()
                    saveKeyValueFile(aliasFile(), aliasMap)
                    emit("Alias '$name' saved.")
                }
            }

            "run" -> {
                if (rest.isBlank()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val f = toReal(rest)
                when {
                    !allowed(f) -> accessDenied()
                    !f.exists() || f.isDirectory -> emit("The system cannot find the file specified.", TermLineType.ERROR)
                    f.extension.equals("js", true) -> {
                        if (!jsReady) { emit("JavaScript engine still starting up, try again in a moment.", TermLineType.WARNING); return }
                        isRunning = true
                        evalJs(f.readText()) { result -> if (result.isNotBlank() && result != "null") emit(result.trim('"')); isRunning = false }
                    }
                    else -> f.readLines().forEach { line ->
                        if (line.isNotBlank() && !line.trimStart().startsWith("rem", true)) execute(line)
                    }
                }
            }

            "rem" -> { /* comment line — no-op */ }
            "pause" -> emit("Press any key to continue . . .", TermLineType.INFO)

            // ── Advanced ─────────────────────────────────────────────
            "admin" -> when (rest.lowercase()) {
                "on" -> {
                    adminMode = true
                    emit("Explorer mode ON — D:\\ now mounted, mapped to real device storage.", TermLineType.WARNING)
                    emit("This is outside Bluebird's managed sandbox — be careful. Type 'admin off' to unmount.", TermLineType.WARNING)
                }
                "off" -> {
                    adminMode = false
                    if (virtual(currentDir).startsWith("D:")) currentDir = BluebirdFileSystem.homeDir()
                    emit("Explorer mode OFF — D:\\ unmounted.", TermLineType.INFO)
                }
                else -> emit("Usage: admin on | admin off", TermLineType.ERROR)
            }

            "clip" -> {
                if (rest.isBlank()) { emit("Usage: clip <text> or clip <file>", TermLineType.ERROR); return }
                val f = toReal(rest)
                val text = if (f.exists() && f.isFile) f.readText() else rest
                try {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Bluebird", text))
                    emit("Copied to clipboard.", TermLineType.SUCCESS)
                } catch (_: Exception) { emit("Unable to access clipboard.", TermLineType.ERROR) }
            }

            "share" -> {
                if (rest.isBlank()) { emit("The syntax of the command is incorrect.", TermLineType.ERROR); return }
                val f = toReal(rest)
                if (!f.exists() || f.isDirectory) { emit("The system cannot find the file specified.", TermLineType.ERROR); return }
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, "Share ${f.name}"
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) { emit("Unable to share file.", TermLineType.ERROR) }
            }

            "js" -> {
                if (rest.isBlank()) { emit("Usage: js <code>", TermLineType.ERROR); return }
                if (!jsReady) { emit("JavaScript engine still starting up, try again in a moment.", TermLineType.WARNING); return }
                isRunning = true
                evalJs(rest) { result -> if (result.isNotBlank() && result != "null") emit(result.trim('"')); isRunning = false }
            }

            "bbfetch" -> {
                emitRich(listOf(TermSegment("  ▄▄▄▄▄▄▄  ", TermInfo)))
                emitRich(listOf(TermSegment(" ▐ Bluebird ▌ ", TermInfo)))
                emitRich(listOf(TermSegment("  ▀▀▀▀▀▀▀  ", TermInfo)))
                emitBlank()
                emit("User:      ${BluebirdFileSystem.currentUser}")
                emit("OS:        Bluebird 1.0 on Android ${Build.VERSION.RELEASE}")
                emit("Device:    ${Build.MANUFACTURER} ${Build.MODEL}")
                val stat = StatFs(BluebirdFileSystem.root.absolutePath)
                emit("Storage:   ${formatFileSize(stat.totalBytes - stat.freeBytes)} / ${formatFileSize(stat.totalBytes)}")
                emit("Shell:     Bluebird Command Prompt 1.0")
            }

            // ── Misc ─────────────────────────────────────────────────
            "echo" -> if (rest.isEmpty()) emit("ECHO is on.") else emit(rest)
            "ver" -> emit("Bluebird [Version 1.0]")
            "vol" -> { emit(" Volume in drive C is BLUEBIRD"); emit(" Volume Serial Number is 1337-0001") }
            "history" -> {
                val n = args.getOrNull(0)?.toIntOrNull() ?: history.size
                history.takeLast(n).forEachIndexed { i, h -> emit("  ${(history.size - n + i + 1).toString().padStart(4)}  $h") }
            }
            "cls" -> lines.clear()
            "exit" -> onExit?.invoke() ?: emit("Type 'close' in the title bar to close the terminal.", TermLineType.INFO)

            "help" -> {
                emitRich(listOf(TermSegment("Bluebird Command Prompt — Commands", TermInfo)))
                emitBlank()
                listOf(
                    "Navigation" to listOf("CD", "DIR", "TREE", "PATH"),
                    "Files"      to listOf("MKDIR", "RMDIR", "COPY", "MOVE", "XCOPY", "DEL", "REN", "TYPE", "MORE", "SORT", "FIND", "FINDSTR", "FC", "WHERE", "ATTRIB"),
                    "System"     to listOf("SYSTEMINFO", "DATE", "TIME", "WHOAMI", "CHKDSK", "FORMAT", "TASKLIST", "TASKKILL", "BATTERY"),
                    "Shell"      to listOf("SET", "TITLE", "COLOR", "PROMPT", "ECHO", "VER", "VOL", "HISTORY", "CLS", "HELP", "EXIT"),
                    "Scripting"  to listOf("ALIAS", "RUN", "REM", "PAUSE", "JS"),
                    "Bluebird"   to listOf("APPS", "OPEN", "OPENAPP", "SETTINGS", "RECENT", "THEME", "WALLPAPER", "MUSIC", "GUI", "TTY"),
                    "Advanced"   to listOf("ADMIN", "CLIP", "SHARE", "BBFETCH")
                ).forEach { (section, cmds) ->
                    emitRich(listOf(TermSegment("  $section:", TermInfo)))
                    cmds.chunked(4).forEach { row -> emit("    " + row.joinToString("  ") { it.padEnd(10) }) }
                    emitBlank()
                }
                emit("Unix-style aliases also work: ls, cp, mv, rm, cat, md, rd, pwd, clear, chdir, erase")
                emit("ADMIN mounts D:\\ — real device storage outside the Bluebird sandbox. Use carefully.")
            }

            else -> {
                emit("'$typed' is not recognized as an internal or external command,", TermLineType.ERROR)
                emit("operable program or batch file.", TermLineType.ERROR)
            }
        }
    }

    // ── Boot sequence ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        BluebirdFileSystem.ensureStructure()
        emit("Bluebird Command Prompt [Version 1.0]")
        emit("(c) Bluebird OS. All rights reserved.")
        emitBlank()
        delay(80)
        try { focusReq.requestFocus() } catch (_: Exception) {}
    }

    // JS engine boot (isolated blank page — no Kotlin bridge is exposed to it)
    LaunchedEffect(Unit) {
        jsWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { jsReady = true }
        }
        jsWebView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                emit("console: ${cm.message()}", TermLineType.INFO)
                return true
            }
        }
        jsWebView.loadDataWithBaseURL(null, "<html><body></body></html>", "text/html", "UTF-8", null)
    }

    LaunchedEffect(lines.size) { scrollBottom() }

    // ─────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) { detectTapGestures { try { focusReq.requestFocus() } catch (_: Exception) {} } }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            if (windowTitle.isNotBlank()) {
                Text(
                    text = windowTitle,
                    color = fgColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(lines) { line -> TerminalLine(line, fgColor) }
                if (isRunning) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            CircularProgressIndicator(color = TermInfo, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            Text("running…", color = fgColor.copy(0.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            if (showSuggest && suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.take(12).forEach { sug ->
                        Text(
                            text = sug,
                            color = TermInfo,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(Color(0xFF262626), RoundedCornerShape(3.dp))
                                .clickable {
                                    val parts = input.split("\\s+".toRegex()).toMutableList()
                                    if (parts.size <= 1) input = sug
                                    else { parts[parts.lastIndex] = sug; input = parts.joinToString(" ") }
                                    showSuggest = false
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${prompt()} ",
                    color = fgColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                BasicTextField(
                    value = input,
                    onValueChange = { new ->
                        input = new
                        if (new.isNotBlank()) buildSuggestions(new) else showSuggest = false
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = fgColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(fgColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (!isRunning) { val cmd = input; input = ""; showSuggest = false; execute(cmd) }
                    }),
                    modifier = Modifier.weight(1f).focusRequester(focusReq)
                )

                IconButton(
                    onClick = { applyTab() },
                    modifier = Modifier.size(28.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                ) { Text("⇥", color = TermInfo, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }

                IconButton(
                    onClick = {
                        if (history.isEmpty()) return@IconButton
                        historyIdx = if (historyIdx < 0) history.size - 1 else (historyIdx - 1).coerceAtLeast(0)
                        input = history[historyIdx]
                    },
                    modifier = Modifier.size(28.dp)
                ) { Icon(Icons.Default.KeyboardArrowUp, "History up", tint = fgColor.copy(0.6f), modifier = Modifier.size(16.dp)) }

                IconButton(
                    onClick = {
                        if (historyIdx < 0) return@IconButton
                        historyIdx++
                        input = if (historyIdx >= history.size) { historyIdx = -1; "" } else history[historyIdx]
                    },
                    modifier = Modifier.size(28.dp)
                ) { Icon(Icons.Default.KeyboardArrowDown, "History down", tint = fgColor.copy(0.6f), modifier = Modifier.size(16.dp)) }

                IconButton(
                    onClick = { if (!isRunning && input.isNotBlank()) { val cmd = input; input = ""; showSuggest = false; execute(cmd) } },
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (input.isNotBlank() && !isRunning) Color(0xFF3A3A3A) else Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                ) { Icon(Icons.Default.Send, "Run", tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
@Composable
private fun TerminalLine(line: TermLine, fgColor: Color) {
    val baseColor = when (line.type) {
        TermLineType.INPUT, TermLineType.OUTPUT -> fgColor
        TermLineType.ERROR -> TermError
        TermLineType.INFO -> TermInfo
        TermLineType.SUCCESS -> TermSuccess
        TermLineType.WARNING -> TermWarning
        TermLineType.SYSTEM -> fgColor.copy(0.5f)
    }

    if (line.segments != null) {
        Row(modifier = Modifier.padding(vertical = 1.dp)) {
            line.segments.forEach { seg ->
                Text(seg.text, color = seg.color, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, softWrap = false)
            }
        }
    } else {
        Text(
            text = line.text, color = baseColor, fontSize = 12.5.sp,
            fontFamily = FontFamily.Monospace, lineHeight = 16.sp,
            modifier = Modifier.padding(vertical = 1.dp)
        )
    }
}

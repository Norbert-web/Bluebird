package com.bluebird.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────
// Terminal colour palette (VS Code Dark+ inspired)
// ─────────────────────────────────────────────────────────────────
private val TermBg        = Color(0xFF0C0C0C)
private val TermText      = Color(0xFFCCCCCC)
private val TermPrompt    = Color(0xFF4EC9B0)   // teal — current dir
private val TermUser      = Color(0xFF569CD6)   // blue — user@host
private val TermAt        = Color(0xFFCCCCCC)
private val TermError     = Color(0xFFF44747)
private val TermSuccess   = Color(0xFF4EC9B0)
private val TermWarning   = Color(0xFFDCDCAA)
private val TermInfo      = Color(0xFF9CDCFE)
private val TermDir       = Color(0xFF569CD6)
private val TermExe       = Color(0xFF4EC9B0)
private val TermSymlink   = Color(0xFF9CDCFE)
private val TermSelection = Color(0xFF264F78)

// ─────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────
enum class TermLineType { INPUT, OUTPUT, ERROR, INFO, SUCCESS, WARNING, SYSTEM }

data class TermLine(
    val text: String,
    val type: TermLineType = TermLineType.OUTPUT,
    val segments: List<TermSegment>? = null   // rich coloured output
)

data class TermSegment(val text: String, val color: Color)

// ─────────────────────────────────────────────────────────────────
// Built-in command registry
// ─────────────────────────────────────────────────────────────────
private val BUILT_IN_COMMANDS = listOf(
    "help", "clear", "cls", "pwd", "ls", "ll", "la", "dir",
    "cd", "mkdir", "rmdir", "rm", "cp", "mv", "touch", "cat",
    "echo", "date", "whoami", "hostname", "uname", "env", "export",
    "history", "which", "find", "grep", "head", "tail", "wc",
    "open", "launch", "start", "apps", "kill", "ps",
    "df", "du", "free", "uptime", "ifconfig", "ping",
    "exit", "quit"
)

// ─────────────────────────────────────────────────────────────────
// TerminalScreen
// ─────────────────────────────────────────────────────────────────
@Composable
fun TerminalScreen(isDark: Boolean) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val listState  = rememberLazyListState()
    val focusReq   = remember { FocusRequester() }

    // State
    var currentDir  by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var input       by remember { mutableStateOf("") }
    val lines       = remember { mutableStateListOf<TermLine>() }
    val history     = remember { mutableStateListOf<String>() }
    var historyIdx  by remember { mutableIntStateOf(-1) }
    var isRunning   by remember { mutableStateOf(false) }
    val envVars     = remember { mutableStateMapOf<String, String>() }

    // Autocomplete
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    var showSuggest by remember { mutableStateOf(false) }
    var tabCycleIdx by remember { mutableIntStateOf(0) }

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

    fun prompt(): String = currentDir.absolutePath
        .replace(Environment.getExternalStorageDirectory().absolutePath, "~")

    // ── Autocomplete logic ────────────────────────────────────────
    fun buildSuggestions(partial: String) {
        val parts = partial.trimStart().split("\\s+".toRegex())
        suggestions = when {
            parts.size <= 1 -> {
                // Complete command name
                val p = parts.firstOrNull() ?: ""
                (BUILT_IN_COMMANDS + (currentDir.listFiles()
                    ?.filter { it.isFile && it.canExecute() }
                    ?.map { it.name } ?: emptyList()))
                    .filter { it.startsWith(p) }.sorted()
            }
            else -> {
                // Complete file/dir path
                val last = parts.last()
                val dir  = if (last.contains("/")) {
                    val parent = last.substringBeforeLast("/")
                    if (parent.startsWith("/")) File(parent)
                    else File(currentDir, parent)
                } else currentDir
                val prefix = last.substringAfterLast("/")
                dir.listFiles()
                    ?.filter { it.name.startsWith(prefix) }
                    ?.map { it.name + if (it.isDirectory) "/" else "" }
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
        if (parts.size <= 1) {
            input = suffix
        } else {
            parts[parts.lastIndex] = suffix
            input = parts.joinToString(" ")
        }
        tabCycleIdx = (tabCycleIdx + 1) % suggestions.size
    }

    // ── File listing helper ───────────────────────────────────────
    fun lsDir(dir: File, showHidden: Boolean, longFormat: Boolean) {
        val files = dir.listFiles()
            ?.filter { showHidden || !it.name.startsWith(".") }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: run { emit("ls: cannot access '${dir.name}': No such file or directory", TermLineType.ERROR); return }

        if (files.isEmpty()) { emit("(empty directory)", TermLineType.INFO); return }

        if (longFormat) {
            val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
            files.forEach { f ->
                val perms = buildString {
                    append(if (f.isDirectory) "d" else "-")
                    append(if (f.canRead()) "r" else "-")
                    append(if (f.canWrite()) "w" else "-")
                    append(if (f.canExecute()) "x" else "-")
                }
                val size  = if (f.isDirectory) "-" else formatFileSize(f.length())
                val date  = sdf.format(Date(f.lastModified()))
                val color = when {
                    f.isDirectory -> TermDir
                    f.canExecute() -> TermExe
                    else -> TermText
                }
                emitRich(listOf(
                    TermSegment("$perms  ", TermText.copy(0.5f)),
                    TermSegment("${size.padStart(8)}  ", TermInfo),
                    TermSegment("$date  ", TermText.copy(0.6f)),
                    TermSegment(f.name + if (f.isDirectory) "/" else "", color)
                ))
            }
            emit("${files.size} items", TermLineType.INFO)
        } else {
            // Columnar output — group into rows of 4
            val names = files.map { it.name + if (it.isDirectory) "/" else "" }
            val maxLen = names.maxOf { it.length } + 2
            val cols  = maxOf(1, 60 / maxLen)
            names.chunked(cols).forEach { row ->
                emitRich(row.mapIndexed { i, name ->
                    val f = files[names.indexOf(name.trimEnd('/'))]
                    val color = when {
                        f.isDirectory -> TermDir
                        f.canExecute() -> TermExe
                        else -> TermText
                    }
                    TermSegment(name.padEnd(maxLen), color)
                })
            }
        }
    }

    // ── Command executor ──────────────────────────────────────────
    fun execute(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return

        // Record history (deduplicate consecutive)
        if (history.isEmpty() || history.last() != trimmed) history.add(trimmed)
        historyIdx = -1

        // Echo the prompt + command
        emitRich(listOf(
            TermSegment("bluebird", TermUser),
            TermSegment("@", TermAt),
            TermSegment("android", TermPrompt),
            TermSegment(":${prompt()}$ ", TermText.copy(0.7f)),
            TermSegment(trimmed, TermText)
        ))

        // Expand env vars
        val expanded = trimmed.replace(Regex("\\$([A-Za-z_][A-Za-z0-9_]*)")) {
            envVars[it.groupValues[1]] ?: System.getenv(it.groupValues[1]) ?: ""
        }

        val parts   = expanded.split("\\s+".toRegex())
        val cmd     = parts[0].lowercase()
        val args    = parts.drop(1)

        when (cmd) {
            // ── Navigation ──────────────────────────────────────────
            "pwd" -> emit(currentDir.absolutePath)

            "cd"  -> {
                val target = when {
                    args.isEmpty() || args[0] == "~" -> Environment.getExternalStorageDirectory()
                    args[0] == ".."                  -> currentDir.parentFile ?: currentDir
                    args[0] == "-"                   -> currentDir   // simplified
                    args[0].startsWith("/")          -> File(args[0])
                    else                             -> File(currentDir, args[0])
                }
                when {
                    !target.exists()     -> emit("cd: ${args.getOrElse(0){"~"}}: No such file or directory", TermLineType.ERROR)
                    !target.isDirectory  -> emit("cd: ${args[0]}: Not a directory", TermLineType.ERROR)
                    !target.canRead()    -> emit("cd: ${args[0]}: Permission denied", TermLineType.ERROR)
                    else                 -> currentDir = target
                }
            }

            // ── Listing ─────────────────────────────────────────────
            "ls"  -> lsDir(
                dir = if (args.isNotEmpty() && !args[0].startsWith("-")) File(currentDir, args[0]) else currentDir,
                showHidden = args.any { it.contains("a") && it.startsWith("-") },
                longFormat = args.any { it.contains("l") && it.startsWith("-") }
            )
            "ll"  -> lsDir(currentDir, showHidden = false, longFormat = true)
            "la"  -> lsDir(currentDir, showHidden = true,  longFormat = true)
            "dir" -> lsDir(currentDir, showHidden = false, longFormat = false)

            // ── File ops ────────────────────────────────────────────
            "mkdir" -> {
                if (args.isEmpty()) { emit("mkdir: missing operand", TermLineType.ERROR); return@execute }
                val createParents = args.contains("-p")
                args.filter { !it.startsWith("-") }.forEach { name ->
                    val f = File(currentDir, name)
                    val ok = if (createParents) f.mkdirs() else f.mkdir()
                    if (!ok && !f.exists()) emit("mkdir: cannot create directory '$name'", TermLineType.ERROR)
                }
            }

            "rmdir" -> {
                if (args.isEmpty()) { emit("rmdir: missing operand", TermLineType.ERROR); return@execute }
                args.forEach { name ->
                    val f = File(currentDir, name)
                    when {
                        !f.exists()     -> emit("rmdir: '$name': No such file or directory", TermLineType.ERROR)
                        !f.isDirectory  -> emit("rmdir: '$name': Not a directory", TermLineType.ERROR)
                        !f.delete()     -> emit("rmdir: '$name': Directory not empty", TermLineType.ERROR)
                    }
                }
            }

            "rm" -> {
                if (args.isEmpty()) { emit("rm: missing operand", TermLineType.ERROR); return@execute }
                val recursive = args.any { it == "-r" || it == "-rf" || it == "-fr" }
                val force     = args.any { it.contains("f") && it.startsWith("-") }
                args.filter { !it.startsWith("-") }.forEach { name ->
                    val f = File(currentDir, name)
                    when {
                        !f.exists() && !force -> emit("rm: cannot remove '$name': No such file or directory", TermLineType.ERROR)
                        f.isDirectory && !recursive -> emit("rm: cannot remove '$name': Is a directory (use -r)", TermLineType.ERROR)
                        else -> {
                            val ok = if (f.isDirectory) f.deleteRecursively() else f.delete()
                            if (!ok) emit("rm: cannot remove '$name': Permission denied", TermLineType.ERROR)
                        }
                    }
                }
            }

            "cp" -> {
                if (args.size < 2) { emit("cp: missing destination operand", TermLineType.ERROR); return@execute }
                val src  = File(if (args[0].startsWith("/")) args[0] else "${currentDir}/${args[0]}")
                val dest = File(if (args[1].startsWith("/")) args[1] else "${currentDir}/${args[1]}")
                when {
                    !src.exists()  -> emit("cp: '${args[0]}': No such file or directory", TermLineType.ERROR)
                    src.isDirectory -> src.copyRecursively(dest, overwrite = true)
                    else -> src.copyTo(dest, overwrite = true)
                }
            }

            "mv" -> {
                if (args.size < 2) { emit("mv: missing destination operand", TermLineType.ERROR); return@execute }
                val src  = File(if (args[0].startsWith("/")) args[0] else "${currentDir}/${args[0]}")
                val dest = File(if (args[1].startsWith("/")) args[1] else "${currentDir}/${args[1]}")
                when {
                    !src.exists()  -> emit("mv: '${args[0]}': No such file or directory", TermLineType.ERROR)
                    else -> src.renameTo(dest).also { ok -> if (!ok) emit("mv: failed to move '${args[0]}'", TermLineType.ERROR) }
                }
            }

            "touch" -> {
                if (args.isEmpty()) { emit("touch: missing file operand", TermLineType.ERROR); return@execute }
                args.forEach { name ->
                    val f = File(currentDir, name)
                    if (!f.exists()) f.createNewFile() else f.setLastModified(System.currentTimeMillis())
                }
            }

            "cat" -> {
                if (args.isEmpty()) { emit("cat: missing operand", TermLineType.ERROR); return@execute }
                args.filter { !it.startsWith("-") }.forEach { name ->
                    val f = File(if (name.startsWith("/")) name else "${currentDir}/$name")
                    when {
                        !f.exists()    -> emit("cat: $name: No such file or directory", TermLineType.ERROR)
                        f.isDirectory  -> emit("cat: $name: Is a directory", TermLineType.ERROR)
                        f.length() > 500_000 -> emit("cat: $name: File too large to display (>500KB)", TermLineType.WARNING)
                        else -> f.readLines().forEach { emit(it) }
                    }
                }
            }

            "head" -> {
                val n = args.find { it.startsWith("-n") }?.drop(2)?.toIntOrNull()
                    ?: args.find { it.startsWith("-") && it.drop(1).all { c -> c.isDigit() } }?.drop(1)?.toIntOrNull()
                    ?: 10
                val file = args.find { !it.startsWith("-") } ?: run { emit("head: missing operand", TermLineType.ERROR); return@execute }
                val f = File(if (file.startsWith("/")) file else "${currentDir}/$file")
                if (!f.exists()) { emit("head: $file: No such file or directory", TermLineType.ERROR); return@execute }
                f.readLines().take(n).forEach { emit(it) }
            }

            "tail" -> {
                val n = args.find { it.startsWith("-n") }?.drop(2)?.toIntOrNull() ?: 10
                val file = args.find { !it.startsWith("-") } ?: run { emit("tail: missing operand", TermLineType.ERROR); return@execute }
                val f = File(if (file.startsWith("/")) file else "${currentDir}/$file")
                if (!f.exists()) { emit("tail: $file: No such file or directory", TermLineType.ERROR); return@execute }
                f.readLines().takeLast(n).forEach { emit(it) }
            }

            "wc" -> {
                val file = args.find { !it.startsWith("-") } ?: run { emit("wc: missing operand", TermLineType.ERROR); return@execute }
                val f = File(if (file.startsWith("/")) file else "${currentDir}/$file")
                if (!f.exists()) { emit("wc: $file: No such file or directory", TermLineType.ERROR); return@execute }
                val content = f.readText()
                val lines2  = content.lines().size
                val words   = content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                val bytes   = f.length()
                emit("  $lines2  $words  $bytes $file")
            }

            "grep" -> {
                if (args.size < 2) { emit("grep: usage: grep [options] PATTERN FILE", TermLineType.ERROR); return@execute }
                val ignoreCase = args.contains("-i")
                val showLine   = args.contains("-n")
                val invert     = args.contains("-v")
                val pattern    = args.find { !it.startsWith("-") } ?: run { emit("grep: missing pattern", TermLineType.ERROR); return@execute }
                val file       = args.drop(args.indexOf(pattern) + 1).find { !it.startsWith("-") }
                    ?: run { emit("grep: missing file", TermLineType.ERROR); return@execute }
                val f = File(if (file.startsWith("/")) file else "${currentDir}/$file")
                if (!f.exists()) { emit("grep: $file: No such file or directory", TermLineType.ERROR); return@execute }
                val regex = if (ignoreCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
                var matchCount = 0
                f.readLines().forEachIndexed { i, line ->
                    val match = regex.containsMatchIn(line)
                    if (match xor invert) {
                        matchCount++
                        emit(if (showLine) "${i + 1}:$line" else line)
                    }
                }
                if (matchCount == 0) emit("(no matches)", TermLineType.INFO)
            }

            "find" -> {
                val path    = args.find { !it.startsWith("-") } ?: "."
                val nameArg = args.getOrNull(args.indexOf("-name") + 1)
                val typeArg = args.getOrNull(args.indexOf("-type") + 1)
                val dir2    = if (path.startsWith("/")) File(path) else File(currentDir, path)
                if (!dir2.exists()) { emit("find: '$path': No such file or directory", TermLineType.ERROR); return@execute }
                var count = 0
                dir2.walkTopDown().take(500).forEach { f ->
                    val nameOk = nameArg == null || f.name.matches(nameArg.replace("*", ".*").toRegex())
                    val typeOk = typeArg == null || (typeArg == "f" && f.isFile) || (typeArg == "d" && f.isDirectory)
                    if (nameOk && typeOk) { emit(f.absolutePath); count++ }
                }
                emit("$count result(s)", TermLineType.INFO)
            }

            // ── Disk / system info ──────────────────────────────────
            "df" -> {
                val stat = android.os.StatFs(Environment.getExternalStorageDirectory().absolutePath)
                val total = stat.totalBytes
                val free  = stat.freeBytes
                val used  = total - free
                emit("Filesystem          Size    Used   Avail  Use%")
                emit("/sdcard    ${formatFileSize(total).padStart(8)}  ${formatFileSize(used).padStart(8)}  ${formatFileSize(free).padStart(8)}  ${"${"%.0f".format(used * 100.0 / total)}%".padStart(4)}")
            }

            "du" -> {
                val target = if (args.isNotEmpty() && !args[0].startsWith("-")) File(currentDir, args[0]) else currentDir
                val size   = target.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                emit("${formatFileSize(size)}\t${target.absolutePath}")
            }

            "free" -> {
                val info = android.app.ActivityManager.MemoryInfo()
                (context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                    .getMemoryInfo(info)
                val total = info.totalMem
                val avail = info.availMem
                val used  = total - avail
                emit("              total        used        free")
                emit("Mem:   ${formatFileSize(total).padStart(12)}  ${formatFileSize(used).padStart(10)}  ${formatFileSize(avail).padStart(10)}")
            }

            "uptime" -> {
                val upMs  = android.os.SystemClock.elapsedRealtime()
                val hours = upMs / 3_600_000
                val mins  = (upMs % 3_600_000) / 60_000
                emit("up ${hours}h ${mins}m")
            }

            "uname" -> {
                val all = args.contains("-a") || args.isEmpty()
                val parts2 = buildString {
                    append("Linux")
                    if (all || args.contains("-n")) append("  android")
                    if (all || args.contains("-r")) append("  ${android.os.Build.VERSION.RELEASE}")
                    if (all || args.contains("-m")) append("  ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "aarch64"}")
                }
                emit(parts2)
            }

            "ps" -> {
                emit("PID   NAME")
                val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                @Suppress("DEPRECATION")
                am.runningAppProcesses?.take(20)?.forEach { proc ->
                    emit("${proc.pid.toString().padStart(5)}  ${proc.processName}")
                } ?: emit("(permission required for full process list)", TermLineType.WARNING)
            }

            "ifconfig" -> {
                try {
                    val ifaces = java.net.NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
                    if (ifaces.isEmpty()) { emit("(no interfaces found)", TermLineType.INFO) }
                    ifaces.filter { it.isUp }.forEach { iface ->
                        emitRich(listOf(TermSegment(iface.name, TermPrompt), TermSegment(":", TermText)))
                        iface.inetAddresses.toList().forEach { addr ->
                            emit("  inet ${addr.hostAddress}")
                        }
                    }
                } catch (_: Exception) {
                    emit("ifconfig: permission denied", TermLineType.ERROR)
                }
            }

            "ping" -> {
                val host = args.find { !it.startsWith("-") } ?: run { emit("ping: missing host", TermLineType.ERROR); return@execute }
                val count = args.getOrNull(args.indexOf("-c") + 1)?.toIntOrNull() ?: 4
                isRunning = true
                scope.launch(Dispatchers.IO) {
                    repeat(count) { i ->
                        val start = System.currentTimeMillis()
                        val reach = try { java.net.InetAddress.getByName(host).isReachable(2000) } catch (_: Exception) { false }
                        val ms    = System.currentTimeMillis() - start
                        withContext(Dispatchers.Main) {
                            if (reach) emit("64 bytes from $host: icmp_seq=$i time=${ms}ms")
                            else       emit("Request timeout for icmp_seq $i", TermLineType.WARNING)
                        }
                        delay(1000)
                    }
                    withContext(Dispatchers.Main) { isRunning = false }
                }
            }

            // ── App launching ───────────────────────────────────────
            "open", "launch", "start" -> {
                val target = args.joinToString(" ")
                if (target.isBlank()) { emit("$cmd: missing argument", TermLineType.ERROR); return@execute }
                // Try as package name first
                val intent = context.packageManager.getLaunchIntentForPackage(target)
                    ?: context.packageManager.queryIntentActivities(
                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
                    ).find { it.activityInfo.applicationInfo.loadLabel(context.packageManager)
                        .toString().equals(target, ignoreCase = true)
                    }?.let { context.packageManager.getLaunchIntentForPackage(it.activityInfo.packageName) }
                if (intent != null) {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    emit("Launched: $target", TermLineType.SUCCESS)
                } else {
                    // Try opening as file
                    val f = File(if (target.startsWith("/")) target else "${currentDir}/$target")
                    if (f.exists()) {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                            emit("Opening: ${f.name}", TermLineType.SUCCESS)
                        } catch (_: Exception) {
                            emit("$cmd: cannot open '${f.name}': No suitable app found", TermLineType.ERROR)
                        }
                    } else {
                        emit("$cmd: '$target': not found", TermLineType.ERROR)
                    }
                }
            }

            "apps" -> {
                emit("Installed apps:", TermLineType.INFO)
                val pm   = context.packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                    .sortedBy { it.loadLabel(pm).toString().lowercase() }
                apps.forEach { app ->
                    emitRich(listOf(
                        TermSegment("  ${app.loadLabel(pm).toString().padEnd(24)}", TermText),
                        TermSegment("  ${app.packageName}", TermText.copy(0.5f))
                    ))
                }
                emit("${apps.size} launchable apps", TermLineType.INFO)
            }

            // ── Shell execution (real commands via /system/bin/sh) ──
            "sh", "bash" -> {
                val script = args.joinToString(" ")
                if (script.isBlank()) { emit("Interactive shell not supported; use sh -c 'command'", TermLineType.WARNING); return@execute }
                isRunning = true
                scope.launch(Dispatchers.IO) {
                    runShellCommand(script.removePrefix("-c").trim(), currentDir) { line, isErr ->
                        scope.launch(Dispatchers.Main) {
                            emit(line, if (isErr) TermLineType.ERROR else TermLineType.OUTPUT)
                        }
                    }
                    withContext(Dispatchers.Main) { isRunning = false }
                }
            }

            // ── Env ──────────────────────────────────────────────────
            "env" -> {
                (System.getenv().entries + envVars.entries).sortedBy { it.key }.forEach { (k, v) ->
                    emitRich(listOf(TermSegment("$k=", TermPrompt), TermSegment(v, TermText)))
                }
            }

            "export" -> {
                args.forEach { arg ->
                    val eqIdx = arg.indexOf('=')
                    if (eqIdx > 0) envVars[arg.substring(0, eqIdx)] = arg.substring(eqIdx + 1)
                    else emit("export: invalid syntax: '$arg'", TermLineType.ERROR)
                }
            }

            // ── Misc ─────────────────────────────────────────────────
            "echo" -> emit(args.joinToString(" "))

            "date" -> emit(SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault()).format(Date()))

            "whoami" -> emit("bluebird")

            "hostname" -> emit(android.os.Build.MODEL.lowercase().replace(" ", "-"))

            "which" -> {
                args.forEach { cmd2 ->
                    if (cmd2 in BUILT_IN_COMMANDS) emit("${cmd2}: shell built-in")
                    else emit("${cmd2}: not found", TermLineType.ERROR)
                }
            }

            "history" -> {
                val n = args.getOrNull(0)?.toIntOrNull() ?: history.size
                history.takeLast(n).forEachIndexed { i, h ->
                    emit("  ${(history.size - n + i + 1).toString().padStart(4)}  $h")
                }
            }

            "clear", "cls" -> lines.clear()

            "exit", "quit" -> emit("Type 'close' in the title bar to close the terminal.", TermLineType.INFO)

            "help" -> {
                emitRich(listOf(TermSegment("Bluebird Terminal — Built-in Commands", TermPrompt)))
                emit("")
                listOf(
                    "Navigation"  to listOf("pwd", "cd <dir>", "ls [-la] [dir]", "ll", "la"),
                    "Files"       to listOf("cat", "head", "tail", "wc", "grep", "find", "touch", "mkdir", "rm [-r]", "cp", "mv"),
                    "Apps"        to listOf("open/launch/start <pkg|name|file>", "apps", "ps"),
                    "System"      to listOf("uname", "uptime", "df", "du", "free", "ifconfig", "ping"),
                    "Shell"       to listOf("echo", "export", "env", "date", "whoami", "history", "clear"),
                ).forEach { (section, cmds) ->
                    emit("")
                    emitRich(listOf(TermSegment("  $section:", TermInfo)))
                    cmds.chunked(4).forEach { row ->
                        emit("    " + row.joinToString("  ") { it.padEnd(22) })
                    }
                }
                emit("")
                emit("Tab: autocomplete   ↑/↓: history", TermLineType.INFO)
            }

            // ── Passthrough to Android shell ─────────────────────────
            else -> {
                isRunning = true
                scope.launch(Dispatchers.IO) {
                    var hadOutput = false
                    runShellCommand("$expanded", currentDir) { line, isErr ->
                        hadOutput = true
                        scope.launch(Dispatchers.Main) {
                            emit(line, if (isErr) TermLineType.ERROR else TermLineType.OUTPUT)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (!hadOutput) emit("$cmd: command not found", TermLineType.ERROR)
                        isRunning = false
                    }
                }
            }
        }
    }

    // ── Boot sequence ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        emit("Bluebird Terminal  v1.0", TermLineType.SYSTEM)
        emit("Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MODEL}", TermLineType.SYSTEM)
        emit("Type 'help' for available commands.", TermLineType.INFO)
        emit("")
        delay(80)
        try { focusReq.requestFocus() } catch (_: Exception) {}
    }

    // ── Auto-scroll on new output ──────────────────────────────────
    LaunchedEffect(lines.size) { scrollBottom() }

    // ─────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBg)
            .pointerInput(Unit) { detectTapGestures { try { focusReq.requestFocus() } catch (_: Exception) {} } }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Output pane ──────────────────────────────────────────
            LazyColumn(
                state    = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(lines) { line ->
                    TerminalLine(line)
                }
                // Running spinner
                if (isRunning) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            CircularProgressIndicator(
                                color    = TermPrompt,
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp
                            )
                            Text("running…", color = TermText.copy(0.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // ── Autocomplete suggestions ─────────────────────────────
            if (showSuggest && suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A2E))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.take(12).forEach { sug ->
                        Text(
                            text     = sug,
                            color    = TermInfo,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(Color(0xFF1E1E3A), RoundedCornerShape(3.dp))
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

            // ── Input row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Prompt
                Text(
                    text       = "${prompt()}$",
                    color      = TermPrompt,
                    fontSize   = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                // Input field
                BasicTextField(
                    value         = input,
                    onValueChange = { new ->
                        input = new
                        if (new.isNotBlank()) buildSuggestions(new)
                        else showSuggest = false
                    },
                    singleLine    = true,
                    textStyle     = TextStyle(
                        color      = TermText,
                        fontSize   = 13.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(TermPrompt),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (!isRunning) {
                            val cmd = input
                            input = ""
                            showSuggest = false
                            execute(cmd)
                        }
                    }),
                    modifier      = Modifier
                        .weight(1f)
                        .focusRequester(focusReq)
                )

                // Action buttons
                if (isRunning) {
                    IconButton(onClick = { isRunning = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Stop, "Stop", tint = TermError, modifier = Modifier.size(16.dp))
                    }
                }

                // Tab (autocomplete)
                IconButton(
                    onClick  = { applyTab() },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                ) {
                    Text("⇥", color = TermInfo, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                // History up
                IconButton(
                    onClick = {
                        if (history.isEmpty()) return@IconButton
                        historyIdx = if (historyIdx < 0) history.size - 1 else (historyIdx - 1).coerceAtLeast(0)
                        input = history[historyIdx]
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "History up", tint = TermText.copy(0.6f), modifier = Modifier.size(16.dp))
                }

                // History down
                IconButton(
                    onClick = {
                        if (historyIdx < 0) return@IconButton
                        historyIdx++
                        input = if (historyIdx >= history.size) { historyIdx = -1; "" }
                        else history[historyIdx]
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "History down", tint = TermText.copy(0.6f), modifier = Modifier.size(16.dp))
                }

                // Send
                IconButton(
                    onClick = {
                        if (!isRunning && input.isNotBlank()) {
                            val cmd = input
                            input = ""
                            showSuggest = false
                            execute(cmd)
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (input.isNotBlank() && !isRunning) Color(0xFF0078D4) else Color(0xFF2A2A2A),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Icon(Icons.Default.Send, "Run", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Render a single terminal line (plain or rich-coloured)
// ─────────────────────────────────────────────────────────────────
@Composable
private fun TerminalLine(line: TermLine) {
    val baseColor = when (line.type) {
        TermLineType.INPUT   -> TermText
        TermLineType.OUTPUT  -> TermText
        TermLineType.ERROR   -> TermError
        TermLineType.INFO    -> TermInfo
        TermLineType.SUCCESS -> TermSuccess
        TermLineType.WARNING -> TermWarning
        TermLineType.SYSTEM  -> TermText.copy(0.4f)
    }

    if (line.segments != null) {
        Row(modifier = Modifier.padding(vertical = 1.dp)) {
            line.segments.forEach { seg ->
                Text(
                    text       = seg.text,
                    color      = seg.color,
                    fontSize   = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    softWrap   = false
                )
            }
        }
    } else {
        Text(
            text       = line.text,
            color      = baseColor,
            fontSize   = 12.5.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            modifier   = Modifier.padding(vertical = 1.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Format a byte count into a human-readable string (B / KB / MB / GB)
// ─────────────────────────────────────────────────────────────────
private fun formatFileSize(bytes: Long): String = when {
    bytes < 0            -> "?"
    bytes < 1_024L       -> "${bytes}B"
    bytes < 1_048_576L   -> "${"%.1f".format(bytes / 1_024.0)}K"
    bytes < 1_073_741_824L -> "${"%.1f".format(bytes / 1_048_576.0)}M"
    else                 -> "${"%.2f".format(bytes / 1_073_741_824.0)}G"
}

// ─────────────────────────────────────────────────────────────────
// Shell command runner — executes via /system/bin/sh
// ─────────────────────────────────────────────────────────────────
private suspend fun runShellCommand(
    command: String,
    workDir: File,
    onLine: (String, Boolean) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val proc = ProcessBuilder("/system/bin/sh", "-c", command)
            .directory(workDir)
            .redirectErrorStream(false)
            .start()

        // Use coroutineScope so stdout/stderr jobs are tied to this scope,
        // not leaked via GlobalScope
        kotlinx.coroutines.coroutineScope {
            val stdoutJob = launch(Dispatchers.IO) {
                BufferedReader(InputStreamReader(proc.inputStream)).use { br ->
                    br.lineSequence().forEach { onLine(it, false) }
                }
            }
            val stderrJob = launch(Dispatchers.IO) {
                BufferedReader(InputStreamReader(proc.errorStream)).use { br ->
                    br.lineSequence().forEach { onLine(it, true) }
                }
            }

            proc.waitFor()
            stdoutJob.join()
            stderrJob.join()
        }
    } catch (e: Exception) {
        onLine("sh: ${e.message}", true)
    }
}



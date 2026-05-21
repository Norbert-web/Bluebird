package com.bluebird.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────
// Design Tokens
// ─────────────────────────────────────────────────────────────────

private object Ed {
    // Dark
    val DBg        = Color(0xFF1E1E1E)
    val DSurface   = Color(0xFF252526)
    val DSurfaceH  = Color(0xFF2D2D2D)
    val DLineNum   = Color(0xFF252526)
    val DBorder    = Color(0xFF3C3C3C)
    val DText      = Color(0xFFD4D4D4)
    val DTextSec   = Color(0xFF888888)
    val DTextMuted = Color(0xFF555555)
    val DTab       = Color(0xFF2D2D2D)
    val DTabActive = Color(0xFF1E1E1E)
    val DStatusBar = Color(0xFF007ACC)

    // Light
    val LBg        = Color(0xFFFFFFFF)
    val LSurface   = Color(0xFFF3F3F3)
    val LSurfaceH  = Color(0xFFEBEBEB)
    val LLineNum   = Color(0xFFF0F0F0)
    val LBorder    = Color(0xFFE0E0E0)
    val LText      = Color(0xFF1A1A1A)
    val LTextSec   = Color(0xFF666666)
    val LTextMuted = Color(0xFFAAAAAA)
    val LTab       = Color(0xFFECECEC)
    val LTabActive = Color(0xFFFFFFFF)
    val LStatusBar = Color(0xFF0078D4)

    val Accent       = Color(0xFF0078D4)
    val AccentGlow   = Color(0xFF429CE3)
    val Gold         = Color(0xFFFFB900)
    val DangerRed    = Color(0xFFD83B01)
    val SuccessGreen = Color(0xFF107C10)

    // Syntax colours (VS Code Dark+)
    val StringColor  = Color(0xFFCE9178)
    val KeywordColor = Color(0xFF569CD6)
    val CommentColor = Color(0xFF6A9955)
    val NumberColor  = Color(0xFFB5CEA8)
    val TypeColor    = Color(0xFF4EC9B0)
    val FuncColor    = Color(0xFFDCDCAA)
    val OpColor      = Color(0xFFD4D4D4)
    val AnnotColor   = Color(0xFF9CDCFE)
}

// ─────────────────────────────────────────────────────────────────
// Supported Encodings
// ─────────────────────────────────────────────────────────────────

private enum class FileEncoding(val label: String, val charset: Charset) {
    UTF8("UTF-8", Charsets.UTF_8),
    UTF16("UTF-16", Charsets.UTF_16),
    ISO8859("ISO-8859-1", Charsets.ISO_8859_1),
    WIN1252("Windows-1252", charset("windows-1252"))
}

private fun charset(name: String): Charset = try {
    Charset.forName(name)
} catch (_: Exception) { Charsets.UTF_8 }

// ─────────────────────────────────────────────────────────────────
// Tab Data — one per open file
// ─────────────────────────────────────────────────────────────────

private data class TabData(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String = "",
    val fileName: String = "Untitled.txt",
    val content: TextFieldValue = TextFieldValue(""),
    val isSaved: Boolean = false,
    val isModified: Boolean = false,
    val encoding: FileEncoding = FileEncoding.UTF8,
    val isReadOnly: Boolean = false,
    val undoStack: List<TextFieldValue> = emptyList(),
    val redoStack: List<TextFieldValue> = emptyList()
)

// ─────────────────────────────────────────────────────────────────
// Editor State (shared across all tabs)
// ─────────────────────────────────────────────────────────────────

private class EditorState(initialPath: String = "", initialContent: String = "") {

    // ── Tabs ──────────────────────────────────────────────────────
    var tabs by mutableStateOf(
        listOf(
            if (initialPath.isNotEmpty())
                TabData(
                    filePath = initialPath,
                    fileName = File(initialPath).name,
                    content = TextFieldValue(initialContent),
                    isSaved = true,
                    isModified = false
                )
            else TabData()
        )
    )
    var activeTabIndex by mutableStateOf(0)

    val activeTab get() = tabs.getOrElse(activeTabIndex) { TabData() }

    // ── Per-tab helpers ───────────────────────────────────────────
    fun updateTab(block: TabData.() -> TabData) {
        tabs = tabs.toMutableList().also { it[activeTabIndex] = it[activeTabIndex].block() }
    }

    fun updateContent(new: TextFieldValue) {
        updateTab {
            val prev = content
            val newUndo = (undoStack + prev).takeLast(200)
            copy(content = new, isModified = true, isSaved = false, undoStack = newUndo, redoStack = emptyList())
        }
    }

    fun undo() {
        val tab = activeTab
        if (tab.undoStack.isEmpty()) return
        val prev = tab.undoStack.last()
        updateTab {
            copy(
                content = prev,
                undoStack = undoStack.dropLast(1),
                redoStack = redoStack + content,
                isModified = true, isSaved = false
            )
        }
    }

    fun redo() {
        val tab = activeTab
        if (tab.redoStack.isEmpty()) return
        val next = tab.redoStack.last()
        updateTab {
            copy(
                content = next,
                redoStack = redoStack.dropLast(1),
                undoStack = undoStack + content,
                isModified = true, isSaved = false
            )
        }
    }

    fun newTab(path: String = "", content: String = "") {
        val tab = if (path.isNotEmpty())
            TabData(filePath = path, fileName = File(path).name, content = TextFieldValue(content), isSaved = true)
        else TabData()
        tabs = tabs + tab
        activeTabIndex = tabs.lastIndex
    }

    fun closeTab(index: Int) {
        if (tabs.size == 1) { tabs = listOf(TabData()); activeTabIndex = 0; return }
        tabs = tabs.toMutableList().also { it.removeAt(index) }
        activeTabIndex = (activeTabIndex).coerceAtMost(tabs.lastIndex)
    }

    // ── View settings (shared) ────────────────────────────────────
    var wordWrap      by mutableStateOf(true)
    var showLineNums  by mutableStateOf(true)
    var fontSize      by mutableStateOf(14.sp)
    var fontFamily: FontFamily by mutableStateOf(FontFamily.Monospace)
    var zoom          by mutableStateOf(1f)
    var syntaxHighlight by mutableStateOf(true)
    var isReadOnly    get() = activeTab.isReadOnly
        set(v) = updateTab { copy(isReadOnly = v) }.let {}

    // ── Find / Replace ────────────────────────────────────────────
    var showFindBar   by mutableStateOf(false)
    var findQuery     by mutableStateOf("")
    var replaceQuery  by mutableStateOf("")
    var showReplace   by mutableStateOf(false)
    var useRegex      by mutableStateOf(false)
    var matchCase     by mutableStateOf(false)
    var currentMatchIndex by mutableStateOf(0)

    // ── Dialogs ───────────────────────────────────────────────────
    var showSaveAsDialog    by mutableStateOf(false)
    var showUnsavedDialog   by mutableStateOf(false)
    var showGoToLineDialog  by mutableStateOf(false)
    var showEncodingPicker  by mutableStateOf(false)
    var toastMsg            by mutableStateOf<String?>(null)
    var toastIsError        by mutableStateOf(false)

    // ── Computed from active tab ──────────────────────────────────
    val filePath   get() = activeTab.filePath
    val fileName   get() = activeTab.fileName
    val content    get() = activeTab.content
    val isSaved    get() = activeTab.isSaved
    val isModified get() = activeTab.isModified
    val encoding   get() = activeTab.encoding
    val canUndo    get() = activeTab.undoStack.isNotEmpty()
    val canRedo    get() = activeTab.redoStack.isNotEmpty()

    val lineCount  get() = content.text.count { it == '\n' } + 1
    val wordCount  get() = content.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val charCount  get() = content.text.length
    val cursorLine get() = content.text.substring(0, content.selection.start.coerceAtMost(content.text.length)).count { it == '\n' } + 1
    val cursorCol  get() = content.text.substring(0, content.selection.start.coerceAtMost(content.text.length)).substringAfterLast('\n').length + 1
    val fileExt    get() = fileName.substringAfterLast('.', "txt").lowercase()

    // ── Find helpers ──────────────────────────────────────────────
    fun findMatches(): List<IntRange> {
        if (findQuery.isEmpty()) return emptyList()
        return try {
            val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val pattern = if (useRegex) Regex(findQuery, flags) else Regex(Regex.escape(findQuery), flags)
            pattern.findAll(content.text).map { it.range }.toList()
        } catch (_: Exception) { emptyList() }
    }

    fun jumpToMatch(delta: Int) {
        val matches = findMatches()
        if (matches.isEmpty()) return
        currentMatchIndex = ((currentMatchIndex + delta).mod(matches.size))
        val range = matches[currentMatchIndex]
        updateTab { copy(content = content.copy(selection = TextRange(range.first, range.last + 1))) }
    }

    fun replaceOne() {
        val matches = findMatches()
        if (matches.isEmpty()) return
        val idx = currentMatchIndex.coerceIn(0, matches.lastIndex)
        val range = matches[idx]
        val new = content.text.substring(0, range.first) + replaceQuery + content.text.substring(range.last + 1)
        updateContent(content.copy(text = new, selection = TextRange(range.first + replaceQuery.length)))
    }

    fun replaceAll() {
        if (findQuery.isEmpty()) return
        try {
            val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val pattern = if (useRegex) Regex(findQuery, flags) else Regex(Regex.escape(findQuery), flags)
            val new = pattern.replace(content.text, replaceQuery)
            updateContent(content.copy(text = new))
        } catch (_: Exception) {}
    }

    // ── Encoding ──────────────────────────────────────────────────
    fun setEncoding(enc: FileEncoding) = updateTab { copy(encoding = enc) }

    // ── Go to line ────────────────────────────────────────────────
    fun goToLine(line: Int) {
        val text = content.text
        val lines = text.split('\n')
        val targetLine = line.coerceIn(1, lines.size)
        var offset = 0
        for (i in 0 until targetLine - 1) offset += lines[i].length + 1
        updateTab { copy(content = content.copy(selection = TextRange(offset))) }
    }

    fun toast(msg: String, error: Boolean = false) { toastMsg = msg; toastIsError = error }
}

// ─────────────────────────────────────────────────────────────────
// Syntax Highlighting
// ─────────────────────────────────────────────────────────────────

private val KOTLIN_KEYWORDS = setOf(
    "fun","val","var","class","object","interface","if","else","when","for","while","do",
    "return","import","package","is","as","in","!in","null","true","false","this","super",
    "override","open","sealed","data","enum","companion","by","constructor","init","get","set",
    "private","public","protected","internal","abstract","suspend","inline","reified","typealias",
    "try","catch","finally","throw","break","continue","it","let","run","apply","also","with","to"
)
private val KOTLIN_TYPES = setOf(
    "Int","Long","Short","Byte","Double","Float","Boolean","String","Char","Unit","Any","Nothing",
    "List","MutableList","Map","MutableMap","Set","MutableSet","Array","Pair","Triple","Result",
    "Modifier","Color","Dp","Sp","TextStyle","FontFamily","FontWeight"
)

private fun buildSyntaxAnnotatedString(text: String, ext: String, findQuery: String, matchCase: Boolean, useRegex: Boolean): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        if (ext in setOf("kt", "kts", "java", "js", "ts", "py", "dart", "cpp", "c", "cs", "go", "rs", "swift")) {
            highlightSyntax(text, ext)
        }
        // Search highlights on top
        if (findQuery.isNotEmpty()) {
            try {
                val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                val pat = if (useRegex) Regex(findQuery, flags) else Regex(Regex.escape(findQuery), flags)
                pat.findAll(text).forEach { m ->
                    addStyle(SpanStyle(background = Ed.Accent.copy(0.35f), color = Color.White), m.range.first, m.range.last + 1)
                }
            } catch (_: Exception) {}
        }
    }
}

private fun AnnotatedString.Builder.highlightSyntax(text: String, ext: String) {
    // Line comments
    val commentPat = when (ext) {
        "py" -> Regex("#[^\n]*")
        else -> Regex("//[^\n]*|/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL))
    }
    commentPat.findAll(text).forEach {
        addStyle(SpanStyle(color = Ed.CommentColor), it.range.first, it.range.last + 1)
    }
    // Strings (double and single quoted, skipping escaped)
    Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'""").findAll(text).forEach {
        addStyle(SpanStyle(color = Ed.StringColor), it.range.first, it.range.last + 1)
    }
    // Numbers
    Regex("""\b\d+\.?\d*[fFLl]?\b""").findAll(text).forEach {
        addStyle(SpanStyle(color = Ed.NumberColor), it.range.first, it.range.last + 1)
    }
    // Keywords (Kotlin/Java/general)
    Regex("""\b(${KOTLIN_KEYWORDS.joinToString("|")})\b""").findAll(text).forEach {
        addStyle(SpanStyle(color = Ed.KeywordColor, fontWeight = FontWeight.Medium), it.range.first, it.range.last + 1)
    }
    // Types
    Regex("""\b(${KOTLIN_TYPES.joinToString("|")})\b""").findAll(text).forEach {
        addStyle(SpanStyle(color = Ed.TypeColor), it.range.first, it.range.last + 1)
    }
    // Annotations (@Something)
    Regex("""@\w+""").findAll(text).forEach {
        addStyle(SpanStyle(color = Ed.AnnotColor), it.range.first, it.range.last + 1)
    }
    // Function calls
    Regex("""\b([a-zA-Z_]\w*)\s*(?=\()""").findAll(text).forEach {
        addStyle(SpanStyle(color = Ed.FuncColor), it.range.first, it.range.last + 1)
    }
}

// ─────────────────────────────────────────────────────────────────
// Auto-save helper (writes to cache)
// ─────────────────────────────────────────────────────────────────

private fun autosavePath(cacheDir: File, fileName: String) =
    File(cacheDir, "autosave_${fileName.replace("/", "_")}.tmp")

// ─────────────────────────────────────────────────────────────────
// State factory
// ─────────────────────────────────────────────────────────────────

@Composable
private fun rememberEditorState(initialPath: String): EditorState {
    return remember(initialPath) {
        val content = if (initialPath.isNotEmpty()) {
            try { File(initialPath).readText() } catch (_: Exception) { "" }
        } else ""
        EditorState(initialPath, content)
    }
}

// ─────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────

@Composable
fun TextEditorScreen(
    isDark: Boolean,
    initialPath: String = ""
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val s = rememberEditorState(initialPath)

    val bg       = if (isDark) Ed.DBg       else Ed.LBg
    val surface  = if (isDark) Ed.DSurface  else Ed.LSurface
    val surfH    = if (isDark) Ed.DSurfaceH else Ed.LSurfaceH
    val lineNum  = if (isDark) Ed.DLineNum  else Ed.LLineNum
    val border   = if (isDark) Ed.DBorder   else Ed.LBorder
    val tc       = if (isDark) Ed.DText     else Ed.LText
    val tcs      = if (isDark) Ed.DTextSec  else Ed.LTextSec
    val tcm      = if (isDark) Ed.DTextMuted else Ed.LTextMuted
    val tabBg    = if (isDark) Ed.DTab      else Ed.LTab
    val tabActive = if (isDark) Ed.DTabActive else Ed.LTabActive

    // ── Shared scroll state (syncs gutter + editor) ──────────────
    val sharedScroll = rememberScrollState()

    // ── Toast dismiss ────────────────────────────────────────────
    LaunchedEffect(s.toastMsg) {
        if (s.toastMsg != null) { delay(2500); s.toastMsg = null }
    }

    // ── Auto-save every 30s when modified ────────────────────────
    LaunchedEffect(s.isModified, s.content.text) {
        if (s.isModified) {
            delay(30_000)
            try {
                val f = autosavePath(context.cacheDir, s.fileName)
                f.writeText(s.content.text)
            } catch (_: Exception) {}
        }
    }

    // ── Offer autosave restore on open ───────────────────────────
    LaunchedEffect(Unit) {
        if (initialPath.isNotEmpty()) {
            val draft = autosavePath(context.cacheDir, File(initialPath).name)
            if (draft.exists() && draft.lastModified() > File(initialPath).lastModified()) {
                s.toast("⚠ Autosave draft found — restore via File > Restore Draft")
            }
        }
    }

    fun save() {
        val path = s.filePath
        if (path.isEmpty()) { s.showSaveAsDialog = true; return }
        try {
            File(path).writeText(s.content.text, s.encoding.charset)
            s.updateTab { copy(isSaved = true, isModified = false) }
            autosavePath(context.cacheDir, s.fileName).delete()
            s.toast("Saved — ${s.fileName}")
        } catch (e: Exception) { s.toast("Save failed: ${e.message}", error = true) }
    }

    fun saveAs(newPath: String) {
        try {
            val f = File(newPath)
            f.parentFile?.mkdirs()
            f.writeText(s.content.text, s.encoding.charset)
            s.updateTab { copy(filePath = newPath, fileName = f.name, isSaved = true, isModified = false) }
            s.showSaveAsDialog = false
            s.toast("Saved as ${File(newPath).name}")
        } catch (e: Exception) { s.toast("Save failed: ${e.message}", error = true) }
    }

    fun shareText() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, s.content.text)
                putExtra(Intent.EXTRA_SUBJECT, s.fileName)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${s.fileName}"))
        } catch (e: Exception) { s.toast("Share failed: ${e.message}", error = true) }
    }

    fun restoreDraft() {
        val draft = autosavePath(context.cacheDir, s.fileName)
        if (draft.exists()) {
            try {
                val recovered = draft.readText()
                s.updateContent(s.content.copy(text = recovered))
                s.toast("Draft restored")
            } catch (_: Exception) { s.toast("Restore failed", error = true) }
        } else { s.toast("No draft found") }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {

            // ── Multi-tab title bar ──────────────────────────────
            TabBar(s, isDark, surface, tabBg, tabActive, border, tc, tcs, tcm,
                onSave = ::save, onSaveAs = { s.showSaveAsDialog = true })

            // ── Menu bar ────────────────────────────────────────
            MenuBar(
                s, isDark, surfH, border, tc, tcs, tcm,
                onSave = ::save,
                onSaveAs = { s.showSaveAsDialog = true },
                onShare = ::shareText,
                onRestoreDraft = ::restoreDraft,
                context = context
            )

            // ── Find / Replace bar ───────────────────────────────
            AnimatedVisibility(
                s.showFindBar,
                enter = slideInVertically { -it } + fadeIn(),
                exit  = slideOutVertically { -it } + fadeOut()
            ) {
                FindReplaceBar(s, isDark, surface, border, tc, tcs, tcm)
            }

            // ── Editor body ──────────────────────────────────────
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (s.showLineNums) {
                    // FIX #1: gutter shares sharedScroll with editor
                    LineNumberGutter(s, lineNum, tcm, border, sharedScroll)
                }
                // FIX #1 continued: editor uses same sharedScroll
                EditorArea(s, isDark, bg, tc, tcs, tcm, sharedScroll)
            }

            // ── Status bar ───────────────────────────────────────
            StatusBar(s, isDark, border, tc, tcs)
        }

        // ── Toast ─────────────────────────────────────────────────
        s.toastMsg?.let { msg ->
            Box(Modifier.fillMaxSize().padding(bottom = 36.dp), contentAlignment = Alignment.BottomCenter) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF323232), shadowElevation = 10.dp) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (s.toastIsError) Icons.Default.Error else Icons.Default.CheckCircle,
                            null, tint = if (s.toastIsError) Ed.DangerRed else Ed.SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(msg, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Save As Dialog ────────────────────────────────────────
        if (s.showSaveAsDialog) {
            SaveAsDialog(
                currentName = s.fileName,
                isDark = isDark, surface = surface, tc = tc, tcs = tcs,
                onConfirm = { name ->
                    val dir = if (s.filePath.isNotEmpty()) File(s.filePath).parentFile
                    else File(Environment.getExternalStorageDirectory(), "Desktop")
                    dir?.mkdirs()
                    val newPath = File(dir, if (name.contains('.')) name else "$name.txt").absolutePath
                    saveAs(newPath)
                },
                onDismiss = { s.showSaveAsDialog = false }
            )
        }

        // ── Go-to-line Dialog ─────────────────────────────────────
        if (s.showGoToLineDialog) {
            GoToLineDialog(
                maxLine = s.lineCount, isDark = isDark, surface = surface, tc = tc, tcs = tcs,
                onConfirm = { line -> s.goToLine(line); s.showGoToLineDialog = false },
                onDismiss = { s.showGoToLineDialog = false }
            )
        }

        // ── Encoding Picker ───────────────────────────────────────
        if (s.showEncodingPicker) {
            EncodingPickerDialog(
                current = s.encoding, isDark = isDark, surface = surface, tc = tc, tcs = tcs,
                onConfirm = { enc -> s.setEncoding(enc); s.showEncodingPicker = false; s.toast("Encoding set to ${enc.label}") },
                onDismiss = { s.showEncodingPicker = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Tab Bar (multi-tab, FIX #7)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TabBar(
    s: EditorState, isDark: Boolean,
    surface: Color, tabBg: Color, tabActive: Color, border: Color,
    tc: Color, tcs: Color, tcm: Color,
    onSave: () -> Unit, onSaveAs: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(38.dp).background(surface).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Box(
                Modifier.size(22.dp).background(Ed.Accent, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))

            // Tabs
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                s.tabs.forEachIndexed { index, tab ->
                    val isActive = index == s.activeTabIndex
                    Row(
                        Modifier
                            .height(34.dp)
                            .widthIn(min = 100.dp, max = 200.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isActive) tabActive else tabBg)
                            .clickable { s.activeTabIndex = index }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (tab.isModified) Box(Modifier.size(6.dp).background(Ed.Gold, RoundedCornerShape(3.dp)))
                        if (tab.isReadOnly) Icon(Icons.Default.Lock, null, tint = tcm, modifier = Modifier.size(10.dp))
                        Text(
                            tab.fileName, color = if (isActive) tc else tcs,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            Icons.Default.Close, null, tint = tcm,
                            modifier = Modifier.size(13.dp).clip(RoundedCornerShape(2.dp)).clickable { s.closeTab(index) }
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                }
                // New tab
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).clickable { s.newTab() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, "New tab", tint = tcm, modifier = Modifier.size(16.dp)) }
            }

            Spacer(Modifier.width(6.dp))
            EdToolBtn(Icons.Default.Save, "Save", tc, enabled = s.isModified) { onSave() }
        }
        Divider(color = border)
    }
}

// ─────────────────────────────────────────────────────────────────
// Menu Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MenuBar(
    s: EditorState, isDark: Boolean,
    surfH: Color, border: Color, tc: Color, tcs: Color, tcm: Color,
    onSave: () -> Unit, onSaveAs: () -> Unit,
    onShare: () -> Unit, onRestoreDraft: () -> Unit,
    context: Context
) {
    var showFileMenu   by remember { mutableStateOf(false) }
    var showEditMenu   by remember { mutableStateOf(false) }
    var showViewMenu   by remember { mutableStateOf(false) }
    var showFormatMenu by remember { mutableStateOf(false) }

    val menuSurface = if (isDark) Ed.DSurface else Ed.LBg
    val menuBorder  = if (isDark) Ed.DBorder  else Ed.LBorder

    fun closeAll() { showFileMenu = false; showEditMenu = false; showViewMenu = false; showFormatMenu = false }

    Row(
        Modifier.fillMaxWidth().height(32.dp).background(surfH).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── File ──
        Box {
            MenuBtn("File", tc, showFileMenu) { showFileMenu = !showFileMenu; showEditMenu = false; showViewMenu = false; showFormatMenu = false }
            DropdownMenu(expanded = showFileMenu, onDismissRequest = { showFileMenu = false }, containerColor = menuSurface) {
                DdItem(Icons.Default.Add,        "New",              "Ctrl+N", tc) { s.newTab(); closeAll() }
                DdItem(Icons.Default.FolderOpen, "Open",             "Ctrl+O", tc) { closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Save,       "Save",             "Ctrl+S", tc, enabled = s.isModified) { onSave(); closeAll() }
                DdItem(Icons.Default.SaveAs,     "Save As…",         "Ctrl+Shift+S", tc) { onSaveAs(); closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Share,      "Share",            "", tc) { onShare(); closeAll() }
                DdItem(Icons.Default.ContentCopy,"Copy All",         "", tc) {
                    // Copy full content to clipboard
                    val clipboard = androidx.compose.ui.platform.LocalClipboardManager
                    closeAll()
                }
                DdItem(Icons.Default.Restore,    "Restore Draft",    "", tc) { onRestoreDraft(); closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Close,      "Close Tab",        "", tc) { s.closeTab(s.activeTabIndex); closeAll() }
            }
        }
        // ── Edit ──
        Box {
            MenuBtn("Edit", tc, showEditMenu) { showEditMenu = !showEditMenu; showFileMenu = false; showViewMenu = false; showFormatMenu = false }
            DropdownMenu(expanded = showEditMenu, onDismissRequest = { showEditMenu = false }, containerColor = menuSurface) {
                // FIX #2: Undo/Redo wired up
                DdItem(Icons.Default.Undo,         "Undo",              "Ctrl+Z", tc, enabled = s.canUndo) { s.undo(); closeAll() }
                DdItem(Icons.Default.Redo,         "Redo",              "Ctrl+Y", tc, enabled = s.canRedo) { s.redo(); closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.ContentCut,   "Cut",               "Ctrl+X", tc) { closeAll() }
                DdItem(Icons.Default.ContentCopy,  "Copy",              "Ctrl+C", tc) { closeAll() }
                DdItem(Icons.Default.ContentPaste, "Paste",             "Ctrl+V", tc) { closeAll() }
                DdItem(Icons.Default.SelectAll,    "Select All",        "Ctrl+A", tc) {
                    s.updateTab { copy(content = content.copy(selection = TextRange(0, content.text.length))) }
                    closeAll()
                }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Search,       "Find",              "Ctrl+F", tc) { s.showFindBar = true; s.showReplace = false; closeAll() }
                DdItem(Icons.Default.FindReplace,  "Replace",           "Ctrl+H", tc) { s.showFindBar = true; s.showReplace = true; closeAll() }
                DdItem(Icons.Default.VerticalAlignCenter, "Go to Line…","Ctrl+G", tc) { s.showGoToLineDialog = true; closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Schedule,     "Insert Date/Time",  "F5", tc) {
                    val dt = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(Date())
                    val cur = s.content; val pos = cur.selection.start
                    s.updateContent(cur.copy(text = cur.text.substring(0, pos) + dt + cur.text.substring(pos)))
                    closeAll()
                }
            }
        }
        // ── Format ──
        Box {
            MenuBtn("Format", tc, showFormatMenu) { showFormatMenu = !showFormatMenu; showFileMenu = false; showEditMenu = false; showViewMenu = false }
            DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }, containerColor = menuSurface) {
                DdCheckItem("Word Wrap",           s.wordWrap,          tc) { s.wordWrap = !s.wordWrap; closeAll() }
                DdCheckItem("Syntax Highlighting", s.syntaxHighlight,   tc) { s.syntaxHighlight = !s.syntaxHighlight; closeAll() }
                DdCheckItem("Read-only Mode",      s.isReadOnly,        tc) { s.isReadOnly = !s.isReadOnly; closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.TextDecrease, "Decrease Font",     "Ctrl+-", tc) { if (s.fontSize.value > 8f) s.fontSize = (s.fontSize.value - 1).sp; closeAll() }
                DdItem(Icons.Default.TextIncrease, "Increase Font",     "Ctrl++", tc) { if (s.fontSize.value < 36f) s.fontSize = (s.fontSize.value + 1).sp; closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Code,         "Monospace Font",    "", tc) { s.fontFamily = FontFamily.Monospace; closeAll() }
                DdItem(Icons.Default.Subject,      "Sans-Serif Font",   "", tc) { s.fontFamily = FontFamily.SansSerif; closeAll() }
                DdItem(Icons.Default.Notes,        "Serif Font",        "", tc) { s.fontFamily = FontFamily.Serif; closeAll() }
            }
        }
        // ── View ──
        Box {
            MenuBtn("View", tc, showViewMenu) { showViewMenu = !showViewMenu; showFileMenu = false; showEditMenu = false; showFormatMenu = false }
            DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }, containerColor = menuSurface) {
                DdCheckItem("Line Numbers",    s.showLineNums, tc) { s.showLineNums = !s.showLineNums; closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.ZoomIn,     "Zoom In",    "Ctrl++", tc) { s.zoom = (s.zoom + 0.1f).coerceAtMost(3f); closeAll() }
                DdItem(Icons.Default.ZoomOut,    "Zoom Out",   "Ctrl+-", tc) { s.zoom = (s.zoom - 0.1f).coerceAtLeast(0.5f); closeAll() }
                DdItem(Icons.Default.ZoomOutMap, "Reset Zoom", "Ctrl+0", tc) { s.zoom = 1f; closeAll() }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Code,       "Encoding…",  "", tc) { s.showEncodingPicker = true; closeAll() }
            }
        }

        Spacer(Modifier.weight(1f))

        // Quick toolbar
        EdToolBtn(Icons.Default.Search,             "Find",         tc) { s.showFindBar = !s.showFindBar }
        EdToolBtn(Icons.Default.WrapText,           "Word Wrap",    if (s.wordWrap) Ed.Accent else tc) { s.wordWrap = !s.wordWrap }
        EdToolBtn(Icons.Default.FormatListNumbered, "Line Numbers", if (s.showLineNums) Ed.Accent else tc) { s.showLineNums = !s.showLineNums }
        EdToolBtn(if (s.isReadOnly) Icons.Default.Lock else Icons.Default.LockOpen, "Read-only", if (s.isReadOnly) Ed.Gold else tc) { s.isReadOnly = !s.isReadOnly }
    }
    Divider(color = border)
}

@Composable
private fun MenuBtn(label: String, tc: Color, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(if (active) Ed.Accent.copy(0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (active) Ed.Accent else tc, fontSize = 12.sp)
    }
}

@Composable
private fun DdItem(icon: ImageVector, label: String, shortcut: String, tc: Color, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (enabled) tc else tc.copy(0.4f), fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = if (enabled) tc.copy(0.7f) else tc.copy(0.3f), modifier = Modifier.size(15.dp)) },
        trailingIcon = { if (shortcut.isNotEmpty()) Text(shortcut, color = tc.copy(0.4f), fontSize = 10.sp) },
        onClick = onClick, enabled = enabled
    )
}

@Composable
private fun DdCheckItem(label: String, checked: Boolean, tc: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = tc, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                null, tint = if (checked) Ed.Accent else tc.copy(0.5f),
                modifier = Modifier.size(15.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun DdDivider(color: Color) = Divider(Modifier.padding(vertical = 3.dp), color = color)

// ─────────────────────────────────────────────────────────────────
// Find / Replace Bar  (FIX #3: prev/next actually work; FIX regex)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FindReplaceBar(
    s: EditorState, isDark: Boolean,
    surface: Color, border: Color, tc: Color, tcs: Color, tcm: Color
) {
    val inputBg   = if (isDark) Ed.DSurfaceH else Ed.LSurfaceH
    val matches   = remember(s.findQuery, s.content.text, s.matchCase, s.useRegex) { s.findMatches() }
    val matchCount = matches.size
    val isInvalidRegex = s.useRegex && s.findQuery.isNotEmpty() && try { Regex(s.findQuery); false } catch (_: Exception) { true }

    Column(Modifier.fillMaxWidth().background(surface).padding(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FindField("Find", s.findQuery, inputBg, if (isInvalidRegex) Ed.DangerRed else border, tc, tcm) { s.findQuery = it; s.currentMatchIndex = 0 }

            // Match count
            Text(
                when {
                    isInvalidRegex               -> "Invalid regex"
                    s.findQuery.isEmpty()        -> ""
                    matchCount == 0              -> "No matches"
                    else                         -> "${s.currentMatchIndex + 1}/$matchCount"
                },
                color = if (matchCount == 0 && s.findQuery.isNotEmpty()) Ed.DangerRed else tcm,
                fontSize = 11.sp
            )

            // FIX #3: prev/next navigate between matches
            EdToolBtn(Icons.Default.KeyboardArrowUp,   "Previous", tc, enabled = matchCount > 0) { s.jumpToMatch(-1) }
            EdToolBtn(Icons.Default.KeyboardArrowDown, "Next",     tc, enabled = matchCount > 0) { s.jumpToMatch(+1) }

            // Regex toggle (FIX #11)
            Box(
                Modifier.height(26.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (s.useRegex) Ed.Accent.copy(0.2f) else Color.Transparent)
                    .border(1.dp, if (s.useRegex) Ed.Accent else border, RoundedCornerShape(4.dp))
                    .clickable { s.useRegex = !s.useRegex }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) { Text(".*", color = if (s.useRegex) Ed.Accent else tcm, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }

            // Case toggle
            Box(
                Modifier.height(26.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (s.matchCase) Ed.Accent.copy(0.2f) else Color.Transparent)
                    .border(1.dp, if (s.matchCase) Ed.Accent else border, RoundedCornerShape(4.dp))
                    .clickable { s.matchCase = !s.matchCase }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) { Text("Aa", color = if (s.matchCase) Ed.Accent else tcm, fontSize = 11.sp) }

            EdToolBtn(Icons.Default.FindReplace, "Replace", tc) { s.showReplace = !s.showReplace }
            Spacer(Modifier.weight(1f))
            EdToolBtn(Icons.Default.Close, "Close", tcm) { s.showFindBar = false; s.findQuery = ""; s.replaceQuery = "" }
        }

        AnimatedVisibility(s.showReplace) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FindField("Replace with", s.replaceQuery, inputBg, border, tc, tcm) { s.replaceQuery = it }
                OutlinedButton(
                    onClick = { s.replaceOne() },
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    enabled = matchCount > 0 && !s.activeTab.isReadOnly
                ) { Text("Replace", fontSize = 11.sp, color = tc) }
                OutlinedButton(
                    onClick = { s.replaceAll() },
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    enabled = matchCount > 0 && !s.activeTab.isReadOnly
                ) { Text("Replace All", fontSize = 11.sp, color = tc) }
            }
        }
    }
    Divider(color = border)
}

@Composable
private fun FindField(
    placeholder: String, value: String,
    bg: Color, border: Color, tc: Color, tcm: Color,
    onChange: (String) -> Unit
) {
    Row(
        Modifier.width(200.dp).height(30.dp)
            .clip(RoundedCornerShape(5.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = tcm, fontSize = 12.sp)
            BasicTextField(
                value, onChange,
                textStyle = TextStyle(color = tc, fontSize = 12.sp),
                cursorBrush = SolidColor(Ed.Accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(Icons.Default.Close, null, tint = tcm,
                modifier = Modifier.size(14.dp).clickable { onChange("") })
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Line Number Gutter  (FIX #1: shared scroll)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LineNumberGutter(
    s: EditorState, lineNumBg: Color, tcm: Color, border: Color,
    scrollState: ScrollState   // ← shared with EditorArea
) {
    Column(
        Modifier
            .width(52.dp).fillMaxHeight().background(lineNumBg)
            .verticalScroll(scrollState)   // same ScrollState
            .padding(top = 12.dp, bottom = 12.dp, end = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        repeat(s.lineCount) { i ->
            val lineNo = i + 1
            val isCurrent = lineNo == s.cursorLine
            Text(
                "$lineNo",
                color = if (isCurrent) Ed.Accent else tcm,
                fontSize = (s.fontSize.value * s.zoom * 0.85f).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .padding(vertical = 0.5.dp)
                    .clickable { s.goToLine(lineNo) }   // FIX #9 bonus: tap line num → jump
            )
        }
    }
    Box(Modifier.fillMaxHeight().width(1.dp).background(border))
}

// ─────────────────────────────────────────────────────────────────
// Editor Area  (FIX #1: shared scroll; #4: scrollbar; #6: syntax)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EditorArea(
    s: EditorState, isDark: Boolean,
    bg: Color, tc: Color, tcs: Color, tcm: Color,
    scrollState: ScrollState   // ← shared with LineNumberGutter
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    // FIX #6: Syntax highlighting wired into visualTransformation
    val highlightedText = remember(s.content.text, s.fileExt, s.findQuery, s.matchCase, s.useRegex, s.syntaxHighlight) {
        if (s.syntaxHighlight) {
            buildSyntaxAnnotatedString(s.content.text, s.fileExt, s.findQuery, s.matchCase, s.useRegex)
        } else {
            buildAnnotatedString {
                append(s.content.text)
                if (s.findQuery.isNotEmpty()) {
                    try {
                        val flags = if (s.matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                        val pat = if (s.useRegex) Regex(s.findQuery, flags) else Regex(Regex.escape(s.findQuery), flags)
                        pat.findAll(s.content.text).forEach { m ->
                            addStyle(SpanStyle(background = Ed.Accent.copy(0.35f), color = Color.White), m.range.first, m.range.last + 1)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(bg)
            .clickable { try { focusRequester.requestFocus() } catch (_: Exception) {} }
    ) {
        BasicTextField(
            value = s.content,
            onValueChange = { if (!s.activeTab.isReadOnly) s.updateContent(it) },  // FIX #10: respect read-only
            textStyle = TextStyle(
                color = tc,
                fontSize = (s.fontSize.value * s.zoom).sp,
                fontFamily = s.fontFamily,
                lineHeight = (s.fontSize.value * s.zoom * 1.6f).sp
            ),
            cursorBrush = SolidColor(if (s.activeTab.isReadOnly) Color.Transparent else Ed.Accent),
            readOnly = s.activeTab.isReadOnly,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .verticalScroll(scrollState)   // FIX #1: shared scroll
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .then(if (!s.wordWrap) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
        ) { innerTextField ->
            // Overlay annotated string (syntax + search highlights) on top
            Box {
                // Render highlighted text as background layer
                Text(
                    text = highlightedText,
                    style = TextStyle(
                        fontSize = (s.fontSize.value * s.zoom).sp,
                        fontFamily = s.fontFamily,
                        lineHeight = (s.fontSize.value * s.zoom * 1.6f).sp
                    )
                )
                // Transparent actual input field on top (carries cursor + selection)
                innerTextField()
            }
        }

        // FIX #4: Corrected scrollbar math
        if (scrollState.maxValue > 0) {
            val thumbHeightFrac = 0.08f
            val scrollFrac = scrollState.value.toFloat() / scrollState.maxValue
            Box(Modifier.align(Alignment.CenterEnd).width(4.dp).fillMaxHeight().padding(vertical = 4.dp)) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val trackH = maxHeight
                    val thumbH = trackH * thumbHeightFrac
                    val maxOffset = trackH - thumbH
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .width(4.dp)
                            .height(thumbH)
                            .offset(y = maxOffset * scrollFrac)
                            .background(tcm.copy(0.5f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Status Bar  (FIX #13: encoding clickable)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(s: EditorState, isDark: Boolean, border: Color, tc: Color, tcs: Color) {
    val bg = if (isDark) Ed.DStatusBar else Ed.LStatusBar
    Column {
        Divider(color = border)
        Row(
            Modifier.fillMaxWidth().height(24.dp).background(bg).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FIX #13: encoding chip is clickable → opens picker
            StatusChip(
                s.encoding.label, Color.White,
                clickable = true, onClick = { s.showEncodingPicker = true }
            )
            StatusChip("${s.fileExt.uppercase()}", Color.White.copy(0.8f))
            if (s.activeTab.isReadOnly) StatusChip("🔒 Read-only", Ed.Gold)

            Spacer(Modifier.weight(1f))

            StatusChip("Ln ${s.cursorLine}, Col ${s.cursorCol}", Color.White)
            StatusChip("${s.wordCount}w", Color.White.copy(0.8f))
            StatusChip("${s.charCount}c", Color.White.copy(0.8f))
            StatusChip("${(s.zoom * 100).toInt()}%", Color.White.copy(0.8f))
            if (s.isModified) StatusChip("● Unsaved", Ed.Gold)
            else StatusChip("✓ Saved", Color(0xFF90EE90))
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color, clickable: Boolean = false, onClick: () -> Unit = {}) {
    Text(
        label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium,
        modifier = if (clickable) Modifier.clickable(onClick = onClick) else Modifier
    )
}

// ─────────────────────────────────────────────────────────────────
// Save As Dialog
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SaveAsDialog(
    currentName: String,
    isDark: Boolean, surface: Color, tc: Color, tcs: Color,
    onConfirm: (String) -> Unit, onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SaveAs, null, tint = Ed.Accent, modifier = Modifier.size(18.dp))
                Text("Save As", color = tc, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("File will be saved to Desktop folder.", color = tcs, fontSize = 12.sp)
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("File name") }, singleLine = true,
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
                )
                Text("Tip: include extension, e.g. notes.txt", color = tcs.copy(0.6f), fontSize = 10.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Ed.Accent),
                shape = RoundedCornerShape(6.dp)
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
                Text("Cancel", color = tc)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Go-to-Line Dialog  (Feature #9)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun GoToLineDialog(
    maxLine: Int, isDark: Boolean, surface: Color, tc: Color, tcs: Color,
    onConfirm: (Int) -> Unit, onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val lineNum = input.toIntOrNull()
    val isValid = lineNum != null && lineNum in 1..maxLine

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VerticalAlignCenter, null, tint = Ed.Accent, modifier = Modifier.size(18.dp))
                Text("Go to Line", color = tc, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Enter a line number (1–$maxLine):", color = tcs, fontSize = 12.sp)
                OutlinedTextField(
                    value = input, onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("Line number") }, singleLine = true,
                    isError = input.isNotEmpty() && !isValid,
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirm(lineNum!!) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Ed.Accent),
                shape = RoundedCornerShape(6.dp)
            ) { Text("Go") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
                Text("Cancel", color = tc)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Encoding Picker Dialog  (Feature #13)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EncodingPickerDialog(
    current: FileEncoding, isDark: Boolean, surface: Color, tc: Color, tcs: Color,
    onConfirm: (FileEncoding) -> Unit, onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Code, null, tint = Ed.Accent, modifier = Modifier.size(18.dp))
                Text("File Encoding", color = tc, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Choose encoding for read & write:", color = tcs, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                FileEncoding.values().forEach { enc ->
                    Row(
                        Modifier
                            .fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (selected == enc) Ed.Accent.copy(0.12f) else Color.Transparent)
                            .clickable { selected = enc }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = selected == enc, onClick = { selected = enc },
                            colors = RadioButtonDefaults.colors(selectedColor = Ed.Accent))
                        Column {
                            Text(enc.label, color = tc, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = Ed.Accent),
                shape = RoundedCornerShape(6.dp)
            ) { Text("Apply") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
                Text("Cancel", color = tc)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Shared icon button
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EdToolBtn(
    icon: ImageVector, desc: String, tint: Color,
    enabled: Boolean = true, onClick: () -> Unit
) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(16.dp))
    }
}

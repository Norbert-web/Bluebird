package com.win11launcher.ui.screens

import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.win11launcher.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────
// Design Tokens
// ─────────────────────────────────────────────────────────────────

private object Ed {
    // Dark
    val DBg        = Color(0xFF1E1E1E)   // VS Code dark
    val DSurface   = Color(0xFF252526)
    val DSurfaceH  = Color(0xFF2D2D2D)
    val DLineNum   = Color(0xFF3C3C3C)
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
    val LLineNum   = Color(0xFFEEEEEE)
    val LBorder    = Color(0xFFE0E0E0)
    val LText      = Color(0xFF1A1A1A)
    val LTextSec   = Color(0xFF666666)
    val LTextMuted = Color(0xFFAAAAAA)
    val LTab       = Color(0xFFECECEC)
    val LTabActive = Color(0xFFFFFFFF)
    val LStatusBar = Color(0xFF0078D4)

    val Accent     = Color(0xFF0078D4)
    val AccentGlow = Color(0xFF429CE3)
    val Gold       = Color(0xFFFFB900)
    val DangerRed  = Color(0xFFD83B01)
    val SuccessGreen = Color(0xFF107C10)
    val StringColor  = Color(0xFFCE9178)
    val KeywordColor = Color(0xFF569CD6)
    val CommentColor = Color(0xFF6A9955)
    val NumberColor  = Color(0xFFB5CEA8)
    val TypeColor    = Color(0xFF4EC9B0)
}

// ─────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────

private class EditorState(initialPath: String = "", initialContent: String = "") {
    var filePath   by mutableStateOf(initialPath)
    var fileName   by mutableStateOf(if (initialPath.isNotEmpty()) File(initialPath).name else "Untitled.txt")
    var content    by mutableStateOf(TextFieldValue(initialContent))
    var isSaved    by mutableStateOf(initialPath.isNotEmpty())
    var isModified by mutableStateOf(false)
    var wordWrap   by mutableStateOf(true)
    var showLineNums by mutableStateOf(true)
    var fontSize   by mutableStateOf(14.sp)
    var fontFamily: FontFamily by mutableStateOf(FontFamily.Monospace)
    var showFindBar by mutableStateOf(false)
    var findQuery  by mutableStateOf("")
    var replaceQuery by mutableStateOf("")
    var showReplace by mutableStateOf(false)
    var toastMsg   by mutableStateOf<String?>(null)
    var showSaveAsDialog by mutableStateOf(false)
    var showUnsavedDialog by mutableStateOf(false)
    var zoom       by mutableStateOf(1f)

    val lineCount get() = content.text.count { it == '\n' } + 1
    val wordCount get() = content.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val charCount get() = content.text.length
    val cursorLine get() = content.text.substring(0, content.selection.start.coerceAtMost(content.text.length)).count { it == '\n' } + 1
    val cursorCol  get() = content.text.substring(0, content.selection.start.coerceAtMost(content.text.length)).substringAfterLast('\n').length + 1
    val fileExt    get() = fileName.substringAfterLast('.', "txt").lowercase()

    fun updateContent(new: TextFieldValue) {
        content = new; isModified = true; isSaved = false
    }
    fun toast(msg: String) { toastMsg = msg }
}

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
    initialPath: String = "",
    viewModel: LauncherViewModel? = null
) {
    val scope = rememberCoroutineScope()
    val s = rememberEditorState(initialPath)

    val bg      = if (isDark) Ed.DBg       else Ed.LBg
    val surface = if (isDark) Ed.DSurface  else Ed.LSurface
    val surfH   = if (isDark) Ed.DSurfaceH else Ed.LSurfaceH
    val lineNum = if (isDark) Ed.DLineNum  else Ed.LLineNum
    val border  = if (isDark) Ed.DBorder   else Ed.LBorder
    val tc      = if (isDark) Ed.DText     else Ed.LText
    val tcs     = if (isDark) Ed.DTextSec  else Ed.LTextSec
    val tcm     = if (isDark) Ed.DTextMuted else Ed.LTextMuted
    val tabBg   = if (isDark) Ed.DTab      else Ed.LTab
    val tabActive= if (isDark) Ed.DTabActive else Ed.LTabActive

    // Toast dismiss
    LaunchedEffect(s.toastMsg) {
        if (s.toastMsg != null) { delay(2200); s.toastMsg = null }
    }

    fun save() {
        val path = s.filePath
        if (path.isEmpty()) { s.showSaveAsDialog = true; return }
        try {
            File(path).writeText(s.content.text)
            s.isSaved = true; s.isModified = false
            s.toast("Saved — ${s.fileName}")
        } catch (e: Exception) { s.toast("Save failed: ${e.message}") }
    }

    fun saveAs(newPath: String) {
        try {
            val f = File(newPath)
            f.parentFile?.mkdirs(); f.writeText(s.content.text)
            s.filePath = newPath; s.fileName = f.name
            s.isSaved = true; s.isModified = false
            s.showSaveAsDialog = false
            s.toast("Saved as ${f.name}")
        } catch (e: Exception) { s.toast("Save failed: ${e.message}") }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {

            // ── Title / Tab bar ──
            TitleBar(s, isDark, surface, tabBg, tabActive, border, tc, tcs, tcm,
                onSave = ::save, onSaveAs = { s.showSaveAsDialog = true })

            // ── Menu bar ──
            MenuBar(s, isDark, surfH, border, tc, tcs, tcm, onSave = ::save, onSaveAs = { s.showSaveAsDialog = true })

            // ── Find / Replace bar ──
            AnimatedVisibility(s.showFindBar, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
                FindReplaceBar(s, isDark, surface, border, tc, tcs, tcm)
            }

            // ── Editor ──
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // Line numbers gutter
                if (s.showLineNums) {
                    LineNumberGutter(s, lineNum, tcm, border)
                }
                // Text area
                EditorArea(s, isDark, bg, tc, tcs, tcm)
            }

            // ── Status bar ──
            StatusBar(s, isDark, border, tc, tcs)
        }

        // ── Toast ──
        s.toastMsg?.let { msg ->
            Box(Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF323232), shadowElevation = 8.dp) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Ed.SuccessGreen, modifier = Modifier.size(16.dp))
                        Text(msg, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Save As Dialog ──
        if (s.showSaveAsDialog) {
            SaveAsDialog(
                currentName = s.fileName,
                isDark = isDark,
                surface = surface, tc = tc, tcs = tcs,
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
    }
}

// ─────────────────────────────────────────────────────────────────
// Title Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TitleBar(
    s: EditorState, isDark: Boolean,
    surface: Color, tabBg: Color, tabActive: Color, border: Color,
    tc: Color, tcs: Color, tcm: Color,
    onSave: () -> Unit, onSaveAs: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(38.dp).background(surface).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App icon
            Box(Modifier.size(22.dp).background(Ed.Accent, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            // Tab (single file tab, Windows 11 Notepad style)
            Row(
                Modifier.height(34.dp).widthIn(min = 120.dp, max = 240.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(tabActive)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (s.isModified) Box(Modifier.size(6.dp).background(Ed.Gold, RoundedCornerShape(3.dp)))
                Text(s.fileName, color = tc, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (s.isModified) {
                    Icon(Icons.Default.Circle, null, tint = tc.copy(0.5f), modifier = Modifier.size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape).clickable { onSave() })
                }
            }

            Spacer(Modifier.weight(1f))

            // Quick save
            EdToolBtn(Icons.Default.Save, "Save (Ctrl+S)", tc, enabled = s.isModified) { onSave() }
            Text("${s.wordCount} words", color = tcm, fontSize = 10.sp)
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
    onSave: () -> Unit, onSaveAs: () -> Unit
) {
    var showFileMenu by remember { mutableStateOf(false) }
    var showEditMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showFormatMenu by remember { mutableStateOf(false) }

    val menuSurface = if (isDark) Ed.DSurface else Ed.LBg
    val menuBorder = if (isDark) Ed.DBorder else Ed.LBorder

    Row(
        Modifier.fillMaxWidth().height(32.dp).background(surfH).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File menu
        Box {
            MenuBtn("File", tc, showFileMenu) { showFileMenu = !showFileMenu; showEditMenu = false; showViewMenu = false; showFormatMenu = false }
            DropdownMenu(expanded = showFileMenu, onDismissRequest = { showFileMenu = false }, containerColor = menuSurface) {
                DdItem(Icons.Default.Add, "New",         "Ctrl+N", tc) { showFileMenu = false }
                DdItem(Icons.Default.FolderOpen, "Open", "Ctrl+O", tc) { showFileMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Save, "Save",       "Ctrl+S", tc, enabled = s.isModified) { onSave(); showFileMenu = false }
                DdItem(Icons.Default.SaveAs, "Save as…", "Ctrl+Shift+S", tc) { onSaveAs(); showFileMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Print, "Print",     "Ctrl+P", tc) { showFileMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Close, "Close",     "", tc) { showFileMenu = false }
            }
        }
        // Edit menu
        Box {
            MenuBtn("Edit", tc, showEditMenu) { showEditMenu = !showEditMenu; showFileMenu = false; showViewMenu = false; showFormatMenu = false }
            DropdownMenu(expanded = showEditMenu, onDismissRequest = { showEditMenu = false }, containerColor = menuSurface) {
                DdItem(Icons.Default.Undo, "Undo", "Ctrl+Z", tc) { showEditMenu = false }
                DdItem(Icons.Default.Redo, "Redo", "Ctrl+Y", tc) { showEditMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.ContentCut, "Cut",     "Ctrl+X", tc) { showEditMenu = false }
                DdItem(Icons.Default.ContentCopy, "Copy",   "Ctrl+C", tc) { showEditMenu = false }
                DdItem(Icons.Default.ContentPaste, "Paste", "Ctrl+V", tc) { showEditMenu = false }
                DdItem(Icons.Default.SelectAll, "Select all","Ctrl+A", tc) { showEditMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Search, "Find",      "Ctrl+F", tc) { s.showFindBar = true; s.showReplace = false; showEditMenu = false }
                DdItem(Icons.Default.FindReplace, "Replace","Ctrl+H", tc) { s.showFindBar = true; s.showReplace = true; showEditMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Schedule, "Insert date/time", "F5", tc) {
                    val dt = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(Date())
                    val cur = s.content; val pos = cur.selection.start
                    s.updateContent(cur.copy(text = cur.text.substring(0, pos) + dt + cur.text.substring(pos)))
                    showEditMenu = false
                }
            }
        }
        // Format menu
        Box {
            MenuBtn("Format", tc, showFormatMenu) { showFormatMenu = !showFormatMenu; showFileMenu = false; showEditMenu = false; showViewMenu = false }
            DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }, containerColor = menuSurface) {
                DdCheckItem("Word wrap", s.wordWrap, tc) { s.wordWrap = !s.wordWrap; showFormatMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.TextDecrease, "Decrease font", "Ctrl+-", tc) { if (s.fontSize.value > 8f) s.fontSize = (s.fontSize.value - 1).sp; showFormatMenu = false }
                DdItem(Icons.Default.TextIncrease, "Increase font", "Ctrl++", tc) { if (s.fontSize.value < 36f) s.fontSize = (s.fontSize.value + 1).sp; showFormatMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Code, "Monospace font", "", tc) { s.fontFamily = FontFamily.Monospace; showFormatMenu = false }
                DdItem(Icons.Default.Subject, "Default font",  "", tc) { s.fontFamily = FontFamily.SansSerif; showFormatMenu = false }
            }
        }
        // View menu
        Box {
            MenuBtn("View", tc, showViewMenu) { showViewMenu = !showViewMenu; showFileMenu = false; showEditMenu = false; showFormatMenu = false }
            DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }, containerColor = menuSurface) {
                DdCheckItem("Line numbers", s.showLineNums, tc) { s.showLineNums = !s.showLineNums; showViewMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.ZoomIn,  "Zoom in",  "Ctrl++", tc) { s.zoom = (s.zoom + 0.1f).coerceAtMost(3f); showViewMenu = false }
                DdItem(Icons.Default.ZoomOut, "Zoom out", "Ctrl+-", tc) { s.zoom = (s.zoom - 0.1f).coerceAtLeast(0.5f); showViewMenu = false }
                DdItem(Icons.Default.ZoomOutMap, "Reset zoom", "Ctrl+0", tc) { s.zoom = 1f; showViewMenu = false }
                DdDivider(menuBorder)
                DdItem(Icons.Default.Fullscreen, "Focus mode", "", tc) { showViewMenu = false }
            }
        }

        Spacer(Modifier.weight(1f))

        // Quick toolbar icons
        EdToolBtn(Icons.Default.Search, "Find", tc) { s.showFindBar = !s.showFindBar }
        EdToolBtn(Icons.Default.WrapText, "Word wrap", if (s.wordWrap) Ed.Accent else tc) { s.wordWrap = !s.wordWrap }
        EdToolBtn(Icons.Default.FormatListNumbered, "Line numbers", if (s.showLineNums) Ed.Accent else tc) { s.showLineNums = !s.showLineNums }
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
        leadingIcon = { Icon(if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null, tint = if (checked) Ed.Accent else tc.copy(0.5f), modifier = Modifier.size(15.dp)) },
        onClick = onClick
    )
}

@Composable
private fun DdDivider(color: Color) = Divider(Modifier.padding(vertical = 3.dp), color = color)

// ─────────────────────────────────────────────────────────────────
// Find / Replace Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FindReplaceBar(s: EditorState, isDark: Boolean, surface: Color, border: Color, tc: Color, tcs: Color, tcm: Color) {
    val inputBg = if (isDark) Ed.DSurfaceH else Ed.LSurfaceH
    val occurrences = remember(s.findQuery, s.content.text) {
        if (s.findQuery.isEmpty()) 0 else Regex(Regex.escape(s.findQuery), RegexOption.IGNORE_CASE).findAll(s.content.text).count()
    }

    Column(Modifier.fillMaxWidth().background(surface).padding(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Find field
            FindField("Find", s.findQuery, inputBg, border, tc, tcm) { s.findQuery = it }
            Text(if (s.findQuery.isEmpty()) "" else "$occurrences match${if (occurrences != 1) "es" else ""}",
                color = if (occurrences == 0 && s.findQuery.isNotEmpty()) Ed.DangerRed else tcm, fontSize = 11.sp)
            EdToolBtn(Icons.Default.KeyboardArrowUp, "Previous", tc) {}
            EdToolBtn(Icons.Default.KeyboardArrowDown, "Next", tc) {}
            EdToolBtn(Icons.Default.FindReplace, "Replace", tc) { s.showReplace = !s.showReplace }
            Spacer(Modifier.weight(1f))
            EdToolBtn(Icons.Default.Close, "Close", tcm) { s.showFindBar = false; s.findQuery = ""; s.replaceQuery = "" }
        }
        AnimatedVisibility(s.showReplace) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FindField("Replace", s.replaceQuery, inputBg, border, tc, tcm) { s.replaceQuery = it }
                OutlinedButton(onClick = {
                    if (s.findQuery.isNotEmpty()) {
                        val new = s.content.text.replaceFirst(s.findQuery, s.replaceQuery, ignoreCase = true)
                        s.updateContent(s.content.copy(text = new))
                    }
                }, shape = RoundedCornerShape(5.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Replace", fontSize = 11.sp, color = tc)
                }
                OutlinedButton(onClick = {
                    if (s.findQuery.isNotEmpty()) {
                        val new = s.content.text.replace(s.findQuery, s.replaceQuery, ignoreCase = true)
                        s.updateContent(s.content.copy(text = new))
                    }
                }, shape = RoundedCornerShape(5.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Replace All", fontSize = 11.sp, color = tc)
                }
            }
        }
    }
    Divider(color = border)
}

@Composable
private fun FindField(placeholder: String, value: String, bg: Color, border: Color, tc: Color, tcm: Color, onChange: (String) -> Unit) {
    Row(
        Modifier.width(220.dp).height(30.dp).clip(RoundedCornerShape(5.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = tcm, fontSize = 12.sp)
            BasicTextField(value, onChange, textStyle = TextStyle(color = tc, fontSize = 12.sp), cursorBrush = SolidColor(Ed.Accent), singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Icon(Icons.Default.Close, null, tint = tcm, modifier = Modifier.size(14.dp).clickable { onChange("") })
    }
}

// ─────────────────────────────────────────────────────────────────
// Line Number Gutter
// ─────────────────────────────────────────────────────────────────

@Composable
private fun LineNumberGutter(s: EditorState, lineNumBg: Color, tcm: Color, border: Color) {
    val scrollState = rememberScrollState()
    Column(
        Modifier.width(50.dp).fillMaxHeight().background(lineNumBg)
            .verticalScroll(scrollState).padding(top = 12.dp, bottom = 12.dp, end = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        repeat(s.lineCount) { i ->
            val lineNum = i + 1
            val isCurrent = lineNum == s.cursorLine
            Text(
                "$lineNum",
                color = if (isCurrent) Ed.Accent else tcm,
                fontSize = (s.fontSize.value * 0.85f).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(vertical = 0.5.dp)
            )
        }
    }
    Divider(Modifier.fillMaxHeight().width(1.dp), color = border)
}

// ─────────────────────────────────────────────────────────────────
// Editor Area
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EditorArea(s: EditorState, isDark: Boolean, bg: Color, tc: Color, tcs: Color, tcm: Color) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val highlightedText = remember(s.content.text, s.fileExt, s.findQuery) {
        buildAnnotatedString {
            val text = s.content.text
            append(text)
            // Highlight search matches
            if (s.findQuery.isNotEmpty()) {
                Regex(Regex.escape(s.findQuery), RegexOption.IGNORE_CASE).findAll(text).forEach { match ->
                    addStyle(SpanStyle(background = Ed.Accent.copy(0.3f), color = Color.White), match.range.first, match.range.last + 1)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(bg).clickable { try { focusRequester.requestFocus() } catch (_: Exception) {} }) {
        BasicTextField(
            value = s.content,
            onValueChange = { s.updateContent(it) },
            textStyle = TextStyle(
                color = tc,
                fontSize = (s.fontSize.value * s.zoom).sp,
                fontFamily = s.fontFamily,
                lineHeight = (s.fontSize.value * s.zoom * 1.6f).sp
            ),
            cursorBrush = SolidColor(Ed.Accent),
            modifier = Modifier.fillMaxSize()
                .focusRequester(focusRequester)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .then(if (!s.wordWrap) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
        )

        // Scrollbar indicator
        Box(Modifier.align(Alignment.CenterEnd).width(4.dp).fillMaxHeight().padding(vertical = 4.dp)) {
            val scrollFrac = if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue else 0f
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.1f).offset(y = (scrollFrac * 90).dp).background(tcm.copy(0.4f), RoundedCornerShape(2.dp)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Status Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(s: EditorState, isDark: Boolean, border: Color, tc: Color, tcs: Color) {
    val bg = Ed.DStatusBar
    Column {
        Divider(color = border)
        Row(
            Modifier.fillMaxWidth().height(24.dp).background(bg).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encoding
            StatusChip("UTF-8", Color.White)
            StatusChip("${s.fileExt.uppercase()} file", Color.White.copy(0.8f))
            Spacer(Modifier.weight(1f))
            StatusChip("Ln ${s.cursorLine}, Col ${s.cursorCol}", Color.White)
            StatusChip("${s.wordCount} words", Color.White.copy(0.8f))
            StatusChip("${s.charCount} chars", Color.White.copy(0.8f))
            StatusChip("${(s.zoom * 100).toInt()}%", Color.White.copy(0.8f))
            if (s.isModified) StatusChip("● Unsaved", Ed.Gold)
            else StatusChip("✓ Saved", Color(0xFF90EE90))
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) =
    Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)

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
        title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.SaveAs, null, tint = Ed.Accent, modifier = Modifier.size(18.dp))
            Text("Save As", color = tc, fontWeight = FontWeight.SemiBold)
        }},
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
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
            colors = ButtonDefaults.buttonColors(containerColor = Ed.Accent), shape = RoundedCornerShape(6.dp)) { Text("Save") } },
        dismissButton = { OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) { Text("Cancel", color = tc) } }
    )
}

// ─────────────────────────────────────────────────────────────────
// Tiny helpers
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EdToolBtn(icon: ImageVector, desc: String, tint: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(16.dp)) }
}

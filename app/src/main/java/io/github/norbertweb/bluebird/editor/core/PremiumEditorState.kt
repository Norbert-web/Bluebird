package io.github.norbertweb.bluebird.editor.editor.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.norbertweb.bluebird.editor.core.Bookmark
import io.github.norbertweb.bluebird.editor.core.EditorSettings
import io.github.norbertweb.bluebird.editor.core.FileEncoding
import io.github.norbertweb.bluebird.editor.core.FindResult
import io.github.norbertweb.bluebird.editor.core.HistoryEntry
import io.github.norbertweb.bluebird.editor.core.LineEnding
import io.github.norbertweb.bluebird.editor.core.TabData
import io.github.norbertweb.bluebird.editor.editor.actions.ActionResult
import io.github.norbertweb.bluebird.editor.editor.actions.computeStats
import io.github.norbertweb.bluebird.editor.editor.actions.convertLineEndings
import io.github.norbertweb.bluebird.editor.editor.actions.cutText
import io.github.norbertweb.bluebird.editor.editor.actions.deleteLine
import io.github.norbertweb.bluebird.editor.editor.actions.detectLineEnding
import io.github.norbertweb.bluebird.editor.editor.actions.duplicateLine
import io.github.norbertweb.bluebird.editor.editor.actions.handleCharInput
import io.github.norbertweb.bluebird.editor.editor.actions.handleEnter
import io.github.norbertweb.bluebird.editor.editor.actions.handleTab
import io.github.norbertweb.bluebird.editor.editor.actions.insertText
import io.github.norbertweb.bluebird.editor.editor.actions.moveLineDown
import io.github.norbertweb.bluebird.editor.editor.actions.moveLineUp
import io.github.norbertweb.bluebird.editor.editor.actions.removeDuplicateLines
import io.github.norbertweb.bluebird.editor.editor.actions.selectLine
import io.github.norbertweb.bluebird.editor.editor.actions.selectWord
import io.github.norbertweb.bluebird.editor.editor.actions.sortLines
import io.github.norbertweb.bluebird.editor.editor.actions.toCamelCase
import io.github.norbertweb.bluebird.editor.editor.actions.toLowerCase
import io.github.norbertweb.bluebird.editor.editor.actions.toSnakeCase
import io.github.norbertweb.bluebird.editor.editor.actions.toTitleCase
import io.github.norbertweb.bluebird.editor.editor.actions.toUpperCase
import io.github.norbertweb.bluebird.editor.editor.actions.toggleLineComment
import io.github.norbertweb.bluebird.editor.editor.actions.trimTrailingWhitespace
import io.github.norbertweb.bluebird.editor.editor.highlighting.extractWords
import io.github.norbertweb.bluebird.editor.editor.highlighting.getSuggestions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// Autosave helper
// ─────────────────────────────────────────────────────────────────

fun autosavePath(cacheDir: File, fileName: String): File =
    File(cacheDir, "autosave_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}")

// ─────────────────────────────────────────────────────────────────
// PremiumEditorState
// ─────────────────────────────────────────────────────────────────

class PremiumEditorState(
    initialPath: String = "",
    initialContent: String = "",
    savedSettings: EditorSettings = EditorSettings(),
) {
    // ── Tabs ──────────────────────────────────────────────────────
    var tabs by mutableStateOf(
        listOf(
            if (initialPath.isNotEmpty())
                TabData(
                    filePath = initialPath,
                    fileName = File(initialPath).name,
                    content = TextFieldValue(initialContent),
                    isSaved = true,
                    isModified = false,
                    lastSavedContent = initialContent,
                    lineEnding = detectLineEnding(initialContent),
                )
            else TabData()
        )
    )
    var activeTabIndex by mutableStateOf(0)
    val activeTab get() = tabs.getOrElse(activeTabIndex) { TabData() }

    fun updateTab(block: TabData.() -> TabData) {
        tabs = tabs.toMutableList().also { list ->
            if (activeTabIndex in list.indices) list[activeTabIndex] = list[activeTabIndex].block()
        }
    }

    // ── Content Updates (with debounced undo) ─────────────────────
    private var lastHistoryPushTime = 0L
    private val HISTORY_DEBOUNCE_MS = 500L

    fun updateContent(new: TextFieldValue) {
        val now = System.currentTimeMillis()
        val prev = activeTab.content
        val shouldPushHistory = (now - lastHistoryPushTime) > HISTORY_DEBOUNCE_MS ||
                Math.abs(new.text.length - prev.text.length) > 20 ||
                (prev.text.isNotEmpty() && new.text.isNotEmpty() && prev.text.last() == '\n')

        updateTab {
            val newUndo = if (shouldPushHistory)
                (undoStack + HistoryEntry(prev)).takeLast(500)
            else undoStack

            copy(
                content = new,
                isModified = new.text != lastSavedContent,
                isSaved = new.text == lastSavedContent,
                undoStack = newUndo,
                redoStack = if (shouldPushHistory) emptyList() else redoStack,
            )
        }
        if (shouldPushHistory) lastHistoryPushTime = now

        // Autocomplete
        if (settings.snippetsEnabled) {
            val allWords = extractWords(new.text)
            val suggestions = getSuggestions(new.text, new.selection.start, allWords)
            autocompleteSuggestions = suggestions
        }
    }

    // ── Undo / Redo ───────────────────────────────────────────────
    fun undo() {
        val tab = activeTab
        if (tab.undoStack.isEmpty()) return
        val prev = tab.undoStack.last()
        updateTab {
            copy(
                content = prev.value,
                undoStack = undoStack.dropLast(1),
                redoStack = (redoStack + HistoryEntry(content)).takeLast(500),
                isModified = prev.value.text != lastSavedContent,
                isSaved = prev.value.text == lastSavedContent,
            )
        }
    }

    fun redo() {
        val tab = activeTab
        if (tab.redoStack.isEmpty()) return
        val next = tab.redoStack.last()
        updateTab {
            copy(
                content = next.value,
                redoStack = redoStack.dropLast(1),
                undoStack = (undoStack + HistoryEntry(content)).takeLast(500),
                isModified = next.value.text != lastSavedContent,
                isSaved = next.value.text == lastSavedContent,
            )
        }
    }

    // ── Tabs ──────────────────────────────────────────────────────
    fun newTab(path: String = "", content: String = "") {
        val tab = if (path.isNotEmpty())
            TabData(
                filePath = path,
                fileName = File(path).name,
                content = TextFieldValue(content),
                isSaved = true,
                lastSavedContent = content,
                lineEnding = detectLineEnding(content),
            )
        else TabData()
        tabs = tabs + tab
        activeTabIndex = tabs.lastIndex
    }

    fun closeTab(index: Int): Boolean {
        // Returns true if OK to close, false if tab has unsaved changes (caller should confirm)
        if (tabs[index].isModified) return false
        forceCloseTab(index)
        return true
    }

    fun forceCloseTab(index: Int) {
        if (tabs.size == 1) { tabs = listOf(TabData()); activeTabIndex = 0; return }
        tabs = tabs.toMutableList().also { it.removeAt(index) }
        activeTabIndex = (activeTabIndex).coerceAtMost(tabs.lastIndex)
    }

    fun pinTab(index: Int) {
        tabs = tabs.toMutableList().also {
            it[index] = it[index].copy(isPinned = !it[index].isPinned)
        }
    }

    fun reorderTabs(from: Int, to: Int) {
        if (from == to || from !in tabs.indices || to !in tabs.indices) return
        val mutable = tabs.toMutableList()
        val tab = mutable.removeAt(from)
        mutable.add(to, tab)
        tabs = mutable
        activeTabIndex = to
    }

    // ── Settings ──────────────────────────────────────────────────
    var settings by mutableStateOf(savedSettings)

    // Convenience accessors
    val isDark get() = io.github.norbertweb.bluebird.editor.ui.theme.EdThemes.get(settings.theme).isDark
    val colors get() = io.github.norbertweb.bluebird.editor.ui.theme.EdThemes.get(settings.theme)
    val wordWrap get() = settings.wordWrap
    val showLineNums get() = settings.showLineNumbers
    val fontSize get() = settings.fontSize
    val zoom get() = settings.zoom
    val syntaxHighlight get() = settings.syntaxHighlight
    val isReadOnly get() = activeTab.isReadOnly
    val indentStyle get() = settings.indentStyle

    fun updateSettings(block: EditorSettings.() -> EditorSettings) {
        settings = settings.block()
    }

    // ── Find / Replace ────────────────────────────────────────────
    var showFindBar by mutableStateOf(false)
    var findQuery by mutableStateOf("")
    var replaceQuery by mutableStateOf("")
    var showReplace by mutableStateOf(false)
    var useRegex by mutableStateOf(false)
    var matchCase by mutableStateOf(false)
    var wholeWord by mutableStateOf(false)
    var currentMatchIndex by mutableStateOf(0)

    fun findMatches(): List<FindResult> {
        if (findQuery.isEmpty()) return emptyList()
        return try {
            val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val rawPat = if (useRegex) findQuery else Regex.escape(findQuery)
            val pat = if (wholeWord) Regex("\\b$rawPat\\b", flags) else Regex(rawPat, flags)
            val text = content.text
            val lines = text.split('\n')
            var lineOffset = 0
            val results = mutableListOf<FindResult>()
            lines.forEachIndexed { lineIdx, line ->
                pat.findAll(line).forEach { m ->
                    results.add(FindResult(
                        range = (lineOffset + m.range.first)..(lineOffset + m.range.last),
                        lineNumber = lineIdx + 1,
                        lineText = line.trim().take(80),
                        matchText = m.value,
                    ))
                }
                lineOffset += line.length + 1
            }
            results
        } catch (_: Exception) { emptyList() }
    }

    fun jumpToMatch(delta: Int) {
        val matches = findMatches()
        if (matches.isEmpty()) return
        currentMatchIndex = ((currentMatchIndex + delta).mod(matches.size))
        val range = matches[currentMatchIndex].range
        updateTab { copy(content = content.copy(selection = TextRange(range.first, range.last + 1))) }
    }

    fun replaceOne() {
        val matches = findMatches()
        if (matches.isEmpty()) return
        val idx = currentMatchIndex.coerceIn(0, matches.lastIndex)
        val range = matches[idx].range
        val new = content.text.substring(0, range.first) + replaceQuery + content.text.substring(range.last + 1)
        updateContent(content.copy(text = new, selection = TextRange(range.first + replaceQuery.length)))
    }

    fun replaceAll() {
        if (findQuery.isEmpty()) return
        try {
            val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val rawPat = if (useRegex) findQuery else Regex.escape(findQuery)
            val pat = if (wholeWord) Regex("\\b$rawPat\\b", flags) else Regex(rawPat, flags)
            val new = pat.replace(content.text, replaceQuery)
            updateContent(content.copy(text = new))
            toast("Replaced all occurrences")
        } catch (_: Exception) { toast("Invalid regex pattern", error = true) }
    }

    // ── Go To Line ────────────────────────────────────────────────
    fun goToLine(line: Int) {
        val text = content.text
        val lines = text.split('\n')
        val targetLine = line.coerceIn(1, lines.size)
        var offset = 0
        for (i in 0 until targetLine - 1) offset += lines[i].length + 1
        updateTab { copy(content = content.copy(selection = TextRange(offset))) }
    }

    fun goToColumn(col: Int) {
        val line = cursorLine
        val text = content.text
        val lines = text.split('\n')
        var offset = 0
        for (i in 0 until line - 1) offset += lines[i].length + 1
        offset += (col - 1).coerceIn(0, lines.getOrElse(line - 1) { "" }.length)
        updateTab { copy(content = content.copy(selection = TextRange(offset))) }
    }

    // ── Bookmarks ─────────────────────────────────────────────────
    fun toggleBookmark(line: Int, label: String = "") {
        val bookmarks = activeTab.bookmarks
        val existing = bookmarks.find { it.line == line }
        updateTab {
            copy(bookmarks = if (existing != null)
                bookmarks.filter { it.line != line }
            else
                bookmarks + Bookmark(line = line, label = label)
            )
        }
    }

    fun nextBookmark() {
        val bookmarks = activeTab.bookmarks.sortedBy { it.line }
        if (bookmarks.isEmpty()) return
        val next = bookmarks.firstOrNull { it.line > cursorLine } ?: bookmarks.first()
        goToLine(next.line)
    }

    fun prevBookmark() {
        val bookmarks = activeTab.bookmarks.sortedBy { it.line }
        if (bookmarks.isEmpty()) return
        val prev = bookmarks.lastOrNull { it.line < cursorLine } ?: bookmarks.last()
        goToLine(prev.line)
    }

    // ── Folding ───────────────────────────────────────────────────
    fun toggleFold(line: Int) {
        updateTab {
            copy(foldedLines = if (line in foldedLines) foldedLines - line else foldedLines + line)
        }
    }

    // ── Text Actions ──────────────────────────────────────────────
    private fun applyAction(result: ActionResult) {
        if (result.shouldRecord) updateContent(result.newValue) else updateTab { copy(content = result.newValue) }
    }

    fun handleEnterKey() = applyAction(handleEnter(content, indentStyle, settings.autoCloseBrackets))
    fun handleTabKey(shift: Boolean = false) = applyAction(handleTab(content, indentStyle, shift))
    fun handleChar(char: Char) {
        val result = handleCharInput(char, content, settings.autoCloseBrackets)
        if (result != null) applyAction(result)
    }
    fun duplicateCurrentLine() = applyAction(duplicateLine(content))
    fun moveCurrentLineUp() = applyAction(moveLineUp(content))
    fun moveCurrentLineDown() = applyAction(moveLineDown(content))
    fun deleteCurrentLine() = applyAction(deleteLine(content))
    fun selectCurrentWord() = applyAction(selectWord(content))
    fun selectCurrentLine() = applyAction(selectLine(content))
    fun toggleComment() = applyAction(toggleLineComment(content, fileExt))
    fun toUpper() = applyAction(toUpperCase(content))
    fun toLower() = applyAction(toLowerCase(content))
    fun toTitle() = applyAction(toTitleCase(content))
    fun toSnake() = applyAction(toSnakeCase(content))
    fun toCamel() = applyAction(toCamelCase(content))
    fun sortLinesAsc() = applyAction(sortLines(content, descending = false))
    fun sortLinesDesc() = applyAction(sortLines(content, descending = true))
    fun removeDuplicates() = applyAction(removeDuplicateLines(content))
    fun trimWhitespace() = applyAction(trimTrailingWhitespace(content))
    fun insertDateTime() {
        val dt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        applyAction(insertText(content, dt))
    }
    fun insertIsoDate() {
        val dt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        applyAction(insertText(content, dt))
    }
    fun insertSnippet(body: String) = applyAction(insertText(content, body))

    fun cutSelection(): String {
        val (result, cut) = cutText(content)
        applyAction(result)
        return cut
    }

    // ── Encoding / Line Endings ───────────────────────────────────
    fun setEncoding(enc: FileEncoding) = updateTab { copy(encoding = enc) }
    fun setLineEnding(le: LineEnding) {
        val converted = convertLineEndings(content.text, le)
        updateContent(content.copy(text = converted))
        updateTab { copy(lineEnding = le) }
    }

    // ── Computed Properties ───────────────────────────────────────
    val filePath get() = activeTab.filePath
    val fileName get() = activeTab.fileName
    val content get() = activeTab.content
    val isSaved get() = activeTab.isSaved
    val isModified get() = activeTab.isModified
    val encoding get() = activeTab.encoding
    val lineEnding get() = activeTab.lineEnding
    val canUndo get() = activeTab.undoStack.isNotEmpty()
    val canRedo get() = activeTab.redoStack.isNotEmpty()
    val bookmarks get() = activeTab.bookmarks
    val foldedLines get() = activeTab.foldedLines

    val lineCount get() = content.text.count { it == '\n' } + 1
    val wordCount get() = content.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val charCount get() = content.text.length
    val selCount get() = if (content.selection.length > 0) content.selection.length else 0
    val cursorLine get() = content.text.substring(0, content.selection.start.coerceAtMost(content.text.length)).count { it == '\n' } + 1
    val cursorCol get() = content.text.substring(0, content.selection.start.coerceAtMost(content.text.length)).substringAfterLast('\n').length + 1
    val fileExt get() = fileName.substringAfterLast('.', "txt").lowercase()

    // ── Autocomplete ──────────────────────────────────────────────
    var autocompleteSuggestions by mutableStateOf<List<String>>(emptyList())
    var showAutocomplete by mutableStateOf(false)

    fun acceptSuggestion(word: String) {
        val text = content.text
        val pos = content.selection.start
        val wordStart = text.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '(', ')', '[', ']', '{', '}', '.', ',', ';', ':'), pos - 1) + 1
        val newText = text.substring(0, wordStart) + word + text.substring(pos)
        updateContent(content.copy(text = newText, selection = TextRange(wordStart + word.length)))
        showAutocomplete = false
    }

    // ── Dialogs / UI State ────────────────────────────────────────
    var showSaveAsDialog by mutableStateOf(false)
    var showUnsavedDialog by mutableStateOf(false)
    var pendingCloseTabIndex by mutableStateOf(-1)
    var showGoToLineDialog by mutableStateOf(false)
    var showEncodingPicker by mutableStateOf(false)
    var showLineEndingPicker by mutableStateOf(false)
    var showSettingsPanel by mutableStateOf(false)
    var showStatsPanel by mutableStateOf(false)
    var showSnippetManager by mutableStateOf(false)
    var showBookmarksPanel by mutableStateOf(false)
    var showThemePicker by mutableStateOf(false)
    var showCommandPalette by mutableStateOf(false)
    var showFindResultsPanel by mutableStateOf(false)
    var showMinimap by mutableStateOf(settings.showMinimap)

    // ── Toast / Notification ──────────────────────────────────────
    var toastMsg by mutableStateOf<String?>(null)
    var toastIsError by mutableStateOf(false)
    fun toast(msg: String, error: Boolean = false) { toastMsg = msg; toastIsError = error }

    // ── File Save/Load ────────────────────────────────────────────
    fun saveToFile(context: Context) {
        val path = filePath
        if (path.isEmpty()) { showSaveAsDialog = true; return }
        try {
            val finalContent = if (settings.trimTrailingWhitespace)
                trimTrailingWhitespace(content).newValue.text else content.text
            val withNewline = if (settings.insertFinalNewline && !finalContent.endsWith('\n'))
                "$finalContent\n" else finalContent
            File(path).writeText(withNewline, encoding.charset)
            autosavePath(context.cacheDir, fileName).delete()
            updateTab { copy(isSaved = true, isModified = false, lastSavedContent = withNewline) }
            toast("Saved — $fileName")
            // Add to recent files
            val recent = (listOf(path) + settings.recentFiles.filter { it != path }).take(20)
            updateSettings { copy(recentFiles = recent) }
        } catch (e: Exception) { toast("Save failed: ${e.message}", error = true) }
    }

    fun saveAs(context: Context, newPath: String) {
        try {
            val f = File(newPath)
            f.parentFile?.mkdirs()
            f.writeText(content.text, encoding.charset)
            updateTab { copy(filePath = newPath, fileName = f.name, isSaved = true, isModified = false, lastSavedContent = content.text) }
            showSaveAsDialog = false
            toast("Saved as ${f.name}")
            val recent = (listOf(newPath) + settings.recentFiles.filter { it != newPath }).take(20)
            updateSettings { copy(recentFiles = recent) }
        } catch (e: Exception) { toast("Save failed: ${e.message}", error = true) }
    }

    fun autosave(context: Context) {
        if (!settings.autosaveEnabled || !isModified) return
        try {
            autosavePath(context.cacheDir, fileName).writeText(content.text)
        } catch (_: Exception) {}
    }

    fun restoreDraft(context: Context) {
        val draft = autosavePath(context.cacheDir, fileName)
        if (draft.exists()) {
            try {
                val recovered = draft.readText()
                updateContent(content.copy(text = recovered))
                toast("Draft restored")
            } catch (_: Exception) { toast("Restore failed", error = true) }
        } else toast("No draft found")
    }

    fun loadFile(context: Context, path: String) {
        try {
            val file = File(path)
            val text = file.readText()
            // Check if tab is already open
            val existing = tabs.indexOfFirst { it.filePath == path }
            if (existing != -1) { activeTabIndex = existing; return }
            newTab(path, text)
            toast("Opened ${file.name}")
        } catch (e: Exception) { toast("Open failed: ${e.message}", error = true) }
    }

    // ── Statistics ────────────────────────────────────────────────
    val stats get() = computeStats(content)

    // ── Command Palette Commands ──────────────────────────────────
    val allCommands: List<EditorCommand> get() = listOf(
        EditorCommand("Save", "Ctrl+S", "file") { /* handled by caller */ },
        EditorCommand("Save As…", "Ctrl+Shift+S", "file") { showSaveAsDialog = true },
        EditorCommand("New Tab", "Ctrl+N", "file") { newTab() },
        EditorCommand("Close Tab", "", "file") { closeTab(activeTabIndex) },
        EditorCommand("Undo", "Ctrl+Z", "edit") { undo() },
        EditorCommand("Redo", "Ctrl+Y", "edit") { redo() },
        EditorCommand("Find", "Ctrl+F", "edit") { showFindBar = true; showReplace = false },
        EditorCommand("Find & Replace", "Ctrl+H", "edit") { showFindBar = true; showReplace = true },
        EditorCommand("Go to Line…", "Ctrl+G", "edit") { showGoToLineDialog = true },
        EditorCommand("Toggle Comment", "Ctrl+/", "edit") { toggleComment() },
        EditorCommand("Duplicate Line", "Ctrl+D", "edit") { duplicateCurrentLine() },
        EditorCommand("Delete Line", "Ctrl+Shift+K", "edit") { deleteCurrentLine() },
        EditorCommand("Move Line Up", "Alt+Up", "edit") { moveCurrentLineUp() },
        EditorCommand("Move Line Down", "Alt+Down", "edit") { moveCurrentLineDown() },
        EditorCommand("Select Word", "Ctrl+W", "edit") { selectCurrentWord() },
        EditorCommand("Select Line", "", "edit") { selectCurrentLine() },
        EditorCommand("Select All", "Ctrl+A", "edit") { updateTab { copy(content = content.copy(selection = TextRange(0, content.text.length))) } },
        EditorCommand("UPPERCASE", "", "transform") { toUpper() },
        EditorCommand("lowercase", "", "transform") { toLower() },
        EditorCommand("Title Case", "", "transform") { toTitle() },
        EditorCommand("snake_case", "", "transform") { toSnake() },
        EditorCommand("camelCase", "", "transform") { toCamel() },
        EditorCommand("Sort Lines Ascending", "", "transform") { sortLinesAsc() },
        EditorCommand("Sort Lines Descending", "", "transform") { sortLinesDesc() },
        EditorCommand("Remove Duplicate Lines", "", "transform") { removeDuplicates() },
        EditorCommand("Trim Trailing Whitespace", "", "transform") { trimWhitespace() },
        EditorCommand("Insert Date/Time", "F5", "insert") { insertDateTime() },
        EditorCommand("Toggle Word Wrap", "", "view") { updateSettings { copy(wordWrap = !wordWrap) } },
        EditorCommand("Toggle Line Numbers", "", "view") { updateSettings { copy(showLineNumbers = !showLineNumbers) } },
        EditorCommand("Toggle Minimap", "", "view") { showMinimap = !showMinimap },
        EditorCommand("Toggle Syntax Highlighting", "", "view") { updateSettings { copy(syntaxHighlight = !syntaxHighlight) } },
        EditorCommand("Toggle Read-only", "", "view") { updateTab { copy(isReadOnly = !isReadOnly) } },
        EditorCommand("Zoom In", "Ctrl++", "view") { updateSettings { copy(zoom = (zoom + 0.1f).coerceAtMost(4f)) } },
        EditorCommand("Zoom Out", "Ctrl+-", "view") { updateSettings { copy(zoom = (zoom - 0.1f).coerceAtLeast(0.25f)) } },
        EditorCommand("Reset Zoom", "Ctrl+0", "view") { updateSettings { copy(zoom = 1f) } },
        EditorCommand("Next Bookmark", "F2", "nav") { nextBookmark() },
        EditorCommand("Previous Bookmark", "Shift+F2", "nav") { prevBookmark() },
        EditorCommand("Show Bookmarks", "", "nav") { showBookmarksPanel = true },
        EditorCommand("Show Statistics", "", "view") { showStatsPanel = true },
        EditorCommand("Encoding…", "", "file") { showEncodingPicker = true },
        EditorCommand("Line Ending…", "", "file") { showLineEndingPicker = true },
        EditorCommand("Change Theme…", "", "view") { showThemePicker = true },
        EditorCommand("Settings", "", "view") { showSettingsPanel = true },
        EditorCommand("Snippet Manager", "", "view") { showSnippetManager = true },
        EditorCommand("Restore Draft", "", "file") { /* handled by caller */ },
    )
}

data class EditorCommand(
    val label: String,
    val shortcut: String,
    val category: String,
    val action: () -> Unit,
)

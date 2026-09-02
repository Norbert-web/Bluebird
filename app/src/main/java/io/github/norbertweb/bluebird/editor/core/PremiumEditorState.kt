package io.github.norbertweb.bluebird.editor.editor.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.norbertweb.bluebird.editor.core.Bookmark
import io.github.norbertweb.bluebird.editor.core.Diagnostic
import io.github.norbertweb.bluebird.editor.core.DiagnosticSeverity
import io.github.norbertweb.bluebird.editor.core.LanguageServerManager
import io.github.norbertweb.bluebird.editor.core.LspCodeAction
import io.github.norbertweb.bluebird.editor.core.LspDiagnostic
import io.github.norbertweb.bluebird.editor.core.LspSemanticToken
import io.github.norbertweb.bluebird.editor.core.WebLanguageTools
import io.github.norbertweb.bluebird.editor.core.CompletionEngine
import io.github.norbertweb.bluebird.editor.core.EditorSettings
import io.github.norbertweb.bluebird.editor.core.EditorSelection
import io.github.norbertweb.bluebird.editor.core.LineIndex
import io.github.norbertweb.bluebird.editor.core.FileEncoding
import io.github.norbertweb.bluebird.editor.core.FindResult
import io.github.norbertweb.bluebird.editor.core.HistoryEntry
import io.github.norbertweb.bluebird.editor.core.WorkspaceSymbolIndex
import io.github.norbertweb.bluebird.editor.core.WorkspaceSearchResult
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
import io.github.norbertweb.bluebird.editor.editor.actions.handleWebCharInput
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// Autosave helper
// ─────────────────────────────────────────────────────────────────

fun autosavePath(cacheDir: File, fileName: String): File =
    File(cacheDir, "autosave_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}")

/** Minimal text diff used to keep secondary carets aligned with primary TextField edits. */
data class TextEditDelta(val start: Int, val removedLength: Int, val replacement: String) {
    companion object {
        fun from(old: String, new: String): TextEditDelta {
            var start = 0
            val common = minOf(old.length, new.length)
            while (start < common && old[start] == new[start]) start++
            var oldEnd = old.length
            var newEnd = new.length
            while (oldEnd > start && newEnd > start && old[oldEnd - 1] == new[newEnd - 1]) { oldEnd--; newEnd-- }
            return TextEditDelta(start, oldEnd - start, new.substring(start, newEnd))
        }
    }
    fun mapSelection(selection: EditorSelection): EditorSelection {
        val editEnd = start + removedLength
        fun map(pos: Int): Int = when {
            pos <= start -> pos
            pos >= editEnd -> pos + replacement.length - removedLength
            else -> start + replacement.length
        }
        return EditorSelection(map(selection.start), map(selection.end))
    }
}

// ─────────────────────────────────────────────────────────────────
// PremiumEditorState
// ─────────────────────────────────────────────────────────────────

class PremiumEditorState(
    initialPath: String = "",
    initialContent: String = "",
    savedSettings: EditorSettings = EditorSettings(),
    initialIsDark: Boolean = false,
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
    /** Active editor group. 0 = primary, 1 = secondary. */
    var activeEditorGroup by mutableIntStateOf(0)

    /**
     * Active tab is resolved per editor group.  The old activeTabIndex API is
     * retained for compatibility with menus/actions that operate on the
     * currently focused group.
     */
    var activeTabIndex: Int
        get() {
            val id = if (activeEditorGroup == 0) workspaceLayout.primaryTabId
            else workspaceLayout.secondaryTabId
            return id?.let { wanted -> tabs.indexOfFirst { it.id == wanted } }
                ?.takeIf { it >= 0 }
                ?: 0
        }
        set(value) {
            val index = value.coerceIn(0, (tabs.lastIndex).coerceAtLeast(0))
            val id = tabs.getOrNull(index)?.id
            selectWorkspaceTab(activeEditorGroup, id)
        }

    val activeTab get() = tabs.getOrElse(activeTabIndex) { TabData() }

    fun activateEditorGroup(group: Int) {
        activeEditorGroup = group.coerceIn(0, if (workspaceLayout.secondGroupVisible) 1 else 0)
    }

    fun tabIdsForGroup(group: Int): List<String> =
        if (group == 0) workspaceLayout.primaryTabIds else workspaceLayout.secondaryTabIds

    fun tabsForGroup(group: Int): List<TabData> =
        tabIdsForGroup(group).mapNotNull { id -> tabs.firstOrNull { it.id == id } }

    fun tabIdForGroup(group: Int): String? =
        if (group == 0) workspaceLayout.primaryTabId else workspaceLayout.secondaryTabId

    fun tabIndexForGroup(group: Int): Int =
        tabIdForGroup(group)?.let { id -> tabs.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 } ?: 0

    fun activeTabForGroup(group: Int): TabData =
        tabs.getOrElse(tabIndexForGroup(group)) { TabData() }

    fun selectTabInGroup(group: Int, index: Int) {
        val tab = tabsForGroup(group).getOrNull(index) ?: return
        selectWorkspaceTab(group, tab.id)
    }

    fun reorderTabsInGroup(group: Int, from: Int, to: Int) {
        val ids = tabIdsForGroup(group).toMutableList()
        if (from !in ids.indices || to !in ids.indices || from == to) return
        val id = ids.removeAt(from)
        ids.add(to, id)
        val active = if (group == 0) workspaceLayout.primaryTabId else workspaceLayout.secondaryTabId
        workspaceLayout = if (group == 0) workspaceLayout.copy(primaryTabIds = ids, primaryTabId = active)
        else workspaceLayout.copy(secondaryTabIds = ids, secondaryTabId = active)
    }

    fun selectTabIdInGroup(group: Int, tabId: String) {
        if (tabs.any { it.id == tabId }) selectWorkspaceTab(group, tabId)
    }

    fun withEditorGroup(group: Int, action: () -> Unit) {
        val previous = activeEditorGroup
        activeEditorGroup = group.coerceIn(0, if (workspaceLayout.secondGroupVisible) 1 else 0)
        try { action() } finally { activeEditorGroup = previous }
    }

    fun updateTab(block: TabData.() -> TabData) {
        tabs = tabs.toMutableList().also { list ->
            if (activeTabIndex in list.indices) list[activeTabIndex] = list[activeTabIndex].block()
        }
    }

    fun updateTabById(tabId: String, block: TabData.() -> TabData) {
        tabs = tabs.toMutableList().also { list ->
            val index = list.indexOfFirst { it.id == tabId }
            if (index >= 0) list[index] = list[index].block()
        }
    }

    fun updateContentForTab(tabId: String, new: TextFieldValue) {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return
        val shouldPushHistory = true
        val delta = TextEditDelta.from(tab.content.text, new.text)
        val mappedSecondary = tab.secondarySelections.map { delta.mapSelection(it) }
        val (editedSecondaryText, finalSecondary) = applySecondaryEdits(new.text, mappedSecondary, delta.replacement)
        updateTabById(tabId) {
            val newUndo = if (shouldPushHistory) (undoStack + HistoryEntry(content)).takeLast(500) else undoStack
            copy(
                content = new.copy(text = editedSecondaryText),
                isModified = editedSecondaryText != lastSavedContent,
                isSaved = editedSecondaryText == lastSavedContent,
                undoStack = newUndo,
                redoStack = if (shouldPushHistory) emptyList() else redoStack,
                secondarySelections = finalSecondary,
            )
        }
        if (languageServerManager.isConnected() && tab.filePath.isNotBlank()) {
            val current = tabs.firstOrNull { it.id == tabId }
            if (current != null) {
                languageServerManager.didChange(current)
                languageServerManager.requestSemanticTokensAsync(current.filePath) { tokens ->
                    lspSemanticTokensByPath = lspSemanticTokensByPath.toMutableMap().apply { this[current.filePath] = tokens }
                }
            }
        }
        if (settings.snippetsEnabled) {
            val allWords = extractWords(new.text)
            autocompleteSuggestions = CompletionEngine.suggest(new.text, new.selection.start, tab.fileName, allWords).map { it.label }
            showAutocomplete = autocompleteSuggestions.isNotEmpty()
        }
    }

    private fun applySecondaryEdits(
        text: String,
        selections: List<EditorSelection>,
        replacement: String,
    ): Pair<String, List<EditorSelection>> {
        if (selections.isEmpty()) return text to emptyList()
        var result = text
        val ordered = selections.sortedByDescending { it.min }
        ordered.forEach { sel ->
            val start = sel.min.coerceIn(0, result.length)
            val end = sel.max.coerceIn(start, result.length)
            result = result.substring(0, start) + replacement + result.substring(end)
        }
        val finalSelections = selections.map { sel ->
            val shiftFromLeft = selections
                .filter { it.min < sel.min }
                .sumOf { replacement.length - (it.max - it.min) }
            val caret = (sel.min + replacement.length + shiftFromLeft).coerceIn(0, result.length)
            EditorSelection(caret, caret)
        }.sortedBy { it.start }
        return result to finalSelections
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

        if (tab.filePath.isNotBlank()) workspaceIndex.updateDocument(tab.filePath, new.text)

        // Autocomplete
        if (settings.snippetsEnabled) {
            val allWords = extractWords(new.text)
            val suggestions = CompletionEngine.suggest(new.text, new.selection.start, fileName, allWords).map { it.label }
            autocompleteSuggestions = suggestions
            showAutocomplete = suggestions.isNotEmpty()
        }
        if (tab.filePath.isNotBlank()) workspaceIndex.updateDocument(tab.filePath, new.text)
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
        val newIndex = tabs.lastIndex
        val group = activeEditorGroup
        workspaceLayout = if (group == 0) {
            workspaceLayout.copy(
                primaryTabId = tab.id,
                primaryTabIds = (workspaceLayout.primaryTabIds + tab.id).distinct(),
            )
        } else {
            workspaceLayout.copy(
                secondaryTabId = tab.id,
                secondaryTabIds = (workspaceLayout.secondaryTabIds + tab.id).distinct(),
            )
        }
        activeTabIndex = newIndex
    }

    fun closeTab(index: Int): Boolean {
        // Returns true if OK to close, false if tab has unsaved changes (caller should confirm)
        if (tabs[index].isModified) return false
        forceCloseTab(index)
        return true
    }

    fun forceCloseTab(index: Int) {
        if (index !in tabs.indices) return
        val removedId = tabs[index].id
        if (tabs.size == 1) {
            val replacement = TabData()
            tabs = listOf(replacement)
            workspaceLayout = workspaceLayout.copy(
                primaryTabId = replacement.id,
                secondaryTabId = if (workspaceLayout.secondGroupVisible) replacement.id else null,
            )
            return
        }

        val replacementId = tabs.getOrNull(if (index < tabs.lastIndex) index + 1 else index - 1)?.id
        tabs = tabs.toMutableList().also { it.removeAt(index) }
        workspaceLayout = workspaceLayout.copy(
            primaryTabId = if (workspaceLayout.primaryTabId == removedId) replacementId else workspaceLayout.primaryTabId,
            secondaryTabId = if (workspaceLayout.secondaryTabId == removedId) replacementId else workspaceLayout.secondaryTabId,
            primaryTabIds = workspaceLayout.primaryTabIds.filterNot { it == removedId }.let { ids -> if (ids.isEmpty() && workspaceLayout.secondGroupVisible) listOfNotNull(replacementId) else ids },
            secondaryTabIds = workspaceLayout.secondaryTabIds.filterNot { it == removedId }.let { ids -> if (ids.isEmpty() && workspaceLayout.secondGroupVisible) listOfNotNull(replacementId) else ids },
        )
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
        val movedId = tab.id
        tabs = mutable
        if (activeEditorGroup == 0) selectWorkspaceTab(0, movedId) else selectWorkspaceTab(1, movedId)
    }

    // ── Workspace layout ─────────────────────────────────────────
    var workspaceLayout by mutableStateOf(WorkspaceLayout())
        private set

    init {
        val firstId = tabs.firstOrNull()?.id
        workspaceLayout = workspaceLayout.copy(
            primaryTabId = firstId,
            primaryTabIds = tabs.map { it.id },
        )
    }

    fun splitEditor(orientation: SplitOrientation) {
        val current = activeTab.id
        val primary = workspaceLayout.primaryTabId ?: current
        val existingPrimary = workspaceLayout.primaryTabIds.ifEmpty { tabs.map { it.id } }
        val existingSecondary = workspaceLayout.secondaryTabIds.ifEmpty {
            listOf(tabs.firstOrNull { it.id != primary }?.id ?: current)
        }
        val secondary = workspaceLayout.secondaryTabId
            ?: existingSecondary.firstOrNull()
            ?: current
        workspaceLayout = workspaceLayout.copy(
            orientation = orientation,
            secondGroupVisible = true,
            primaryTabId = primary,
            secondaryTabId = secondary,
            primaryTabIds = existingPrimary.filterNot { it in existingSecondary },
            secondaryTabIds = existingSecondary,
        )
    }

    fun closeEditorGroup() {
        workspaceLayout = workspaceLayout.copy(secondGroupVisible = false, orientation = SplitOrientation.NONE, secondaryTabId = null)
    }

    fun setWorkspaceSplitRatio(ratio: Float) {
        workspaceLayout = workspaceLayout.copy(secondGroupRatio = ratio.coerceIn(0.25f, 0.75f))
    }

    fun restoreWorkspaceLayout(saved: WorkspaceLayout) {
        val allIds = tabs.map { it.id }
        var primaryIds = saved.primaryTabIds.filter { it in allIds }
        var secondaryIds = saved.secondaryTabIds.filter { it in allIds }
        val savedSecondary = saved.secondaryTabId?.takeIf { it in allIds }
        if (primaryIds.isEmpty()) primaryIds = allIds.toMutableList()
        if (secondaryIds.isEmpty() && savedSecondary != null) secondaryIds = listOf(savedSecondary)
        if (saved.secondGroupVisible && secondaryIds.isNotEmpty()) {
            primaryIds = primaryIds.filterNot { it in secondaryIds }
        }
        val primary = saved.primaryTabId?.takeIf { it in primaryIds } ?: primaryIds.firstOrNull()
        val secondary = savedSecondary?.takeIf { it in secondaryIds } ?: secondaryIds.firstOrNull()
        workspaceLayout = saved.copy(
            primaryTabId = primary,
            secondaryTabId = secondary,
            primaryTabIds = primaryIds,
            secondaryTabIds = secondaryIds,
        )
    }

    fun selectWorkspaceTab(group: Int, tabId: String?) {
        workspaceLayout = if (group == 0) workspaceLayout.copy(primaryTabId = tabId)
        else workspaceLayout.copy(secondaryTabId = tabId)
    }

    fun moveTabToGroup(tabId: String, fromGroup: Int, toGroup: Int) {
        if (fromGroup == toGroup || tabs.none { it.id == tabId }) return
        val from = if (fromGroup == 0) workspaceLayout.primaryTabIds else workspaceLayout.secondaryTabIds
        val to = if (toGroup == 0) workspaceLayout.primaryTabIds else workspaceLayout.secondaryTabIds
        val newFrom = from.filterNot { it == tabId }
        val newTo = (to + tabId).distinct()
        workspaceLayout = if (fromGroup == 0) workspaceLayout.copy(
            primaryTabIds = newFrom,
            secondaryTabIds = newTo,
            primaryTabId = workspaceLayout.primaryTabId.takeUnless { it == tabId } ?: newFrom.firstOrNull(),
            secondaryTabId = if (toGroup == 1) tabId else workspaceLayout.secondaryTabId,
        ) else workspaceLayout.copy(
            secondaryTabIds = newFrom,
            primaryTabIds = newTo,
            secondaryTabId = workspaceLayout.secondaryTabId.takeUnless { it == tabId } ?: newFrom.firstOrNull(),
            primaryTabId = if (toGroup == 0) tabId else workspaceLayout.primaryTabId,
        )
    }

    // ── Settings ──────────────────────────────────────────────────
    var settings by mutableStateOf(savedSettings.copy(theme = io.github.norbertweb.bluebird.editor.core.EditorTheme.SYSTEM))

    // Appearance is a property of the host system, never a persisted editor theme.
    var systemIsDark by mutableStateOf(initialIsDark)

    fun setSystemTheme(isDark: Boolean) {
        systemIsDark = isDark
    }

    // Convenience accessors
    val isDark get() = systemIsDark
    val colors get() = io.github.norbertweb.bluebird.editor.ui.theme.EdThemes.system(systemIsDark)
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

    fun findMatchesForTab(tabId: String): List<FindResult> {
        if (findQuery.isEmpty()) return emptyList()
        val tab = tabs.firstOrNull { it.id == tabId } ?: return emptyList()
        return try {
            val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val rawPat = if (useRegex) findQuery else Regex.escape(findQuery)
            val pat = if (wholeWord) Regex("\\b$rawPat\\b", flags) else Regex(rawPat, flags)
            val lines = tab.content.text.split('\n')
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

    // ── Multi-cursor editing ──────────────────────────────────────
    val secondarySelections get() = activeTab.secondarySelections

    fun clearSecondarySelections() {
        updateTab { copy(secondarySelections = emptyList()) }
    }

    /** Add a caret at the same column on the next/previous line. */
    fun addCaretOnAdjacentLine(direction: Int) {
        val text = content.text
        val index = LineIndex(text)
        val primaryLine = index.lineForOffset(content.selection.start)
        val primaryStart = index.lineStart(primaryLine)
        val column = content.selection.start - primaryStart
        val targetLine = primaryLine + direction
        if (targetLine !in 1..index.lineCount) return
        val offset = index.offsetAt(targetLine, column, text)
        if (offset == content.selection.start || secondarySelections.any { it.start == offset && it.end == offset }) return
        updateTab { copy(secondarySelections = (secondarySelections + EditorSelection(offset)).distinctBy { it.start to it.end }.sortedBy { it.start }) }
    }

    /** Ctrl/Cmd+D-style next-occurrence caret. */
    fun addNextOccurrence() {
        val selection = content.selection
        if (!selection.collapsed) return
        val tokenStart = content.text.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '(', ')', '[', ']', '{', '}', '.', ',', ';', ':'), selection.start - 1) + 1
        val tokenEnd = content.text.indexOfAny(charArrayOf(' ', '\n', '\t', '(', ')', '[', ']', '{', '}', '.', ',', ';', ':'), selection.start).let { if (it < 0) content.text.length else it }
        if (tokenEnd <= tokenStart) return
        val token = content.text.substring(tokenStart, tokenEnd)
        val occupied = (secondarySelections + EditorSelection(selection.start)).map { it.start }.toSet()
        val next = Regex.escape(token).toRegex().findAll(content.text, tokenEnd).firstOrNull { it.range.first !in occupied } ?: return
        updateTab { copy(secondarySelections = (secondarySelections + EditorSelection(next.range.first, next.range.last + 1)).distinctBy { it.start to it.end }.sortedBy { it.start }) }
    }

    /** Applies an insertion/replacement to all secondary carets after the primary edit. */
    fun applyMultiCursorEdit(delta: TextEditDelta) {
        if (secondarySelections.isEmpty() || delta.replacement.isEmpty() && delta.removedLength == 0) return
        val current = content.text
        var result = current
        var shift = 0
        val ordered = secondarySelections.sortedByDescending { it.min }
        for (sel in ordered) {
            val start = (sel.min + shift).coerceIn(0, result.length)
            val end = (sel.max + shift).coerceIn(start, result.length)
            result = result.substring(0, start) + delta.replacement + result.substring(end)
            shift += delta.replacement.length - (end - start)
        }
        val newSelections = ordered.map {
            val p = (it.min + shift).coerceIn(0, result.length)
            EditorSelection(p + delta.replacement.length, p + delta.replacement.length)
        }.sortedBy { it.start }
        updateContent(content.copy(text = result))
        updateTab { copy(secondarySelections = newSelections) }
    }

    // ── Folding ───────────────────────────────────────────────────
    fun foldableRegions(tabId: String = activeTab.id): List<FoldRegion> {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return emptyList()
        return FoldingModel.regions(tab.content.text)
    }

    fun isFoldableLine(tabId: String = activeTab.id, line: Int): Boolean =
        foldableRegions(tabId).any { it.startLine == line }

    fun toggleFold(line: Int) {
        if (!isFoldableLine(activeTab.id, line)) return
        updateTab {
            copy(foldedLines = if (line in foldedLines) foldedLines - line else foldedLines + line)
        }
    }

    fun foldAll() {
        val starts = foldableRegions().map { it.startLine }.toSet()
        updateTab { copy(foldedLines = starts) }
    }

    fun unfoldAll() {
        updateTab { copy(foldedLines = emptySet()) }
    }

    fun toggleFoldAtCursor() = toggleFold(cursorLine)

    fun setCursorOffset(offset: Int, extendSelection: Boolean = false) {
        val tab = activeTab
        val safe = offset.coerceIn(0, tab.content.text.length)
        val selection = if (extendSelection) {
            TextRange(tab.content.selection.start, safe)
        } else TextRange(safe, safe)
        updateTab { copy(content = content.copy(selection = selection)) }
    }

    fun setSelectionRange(start: Int, end: Int) {
        val tab = activeTab
        val a = start.coerceIn(0, tab.content.text.length)
        val b = end.coerceIn(0, tab.content.text.length)
        updateTab { copy(content = content.copy(selection = TextRange(a, b))) }
    }

    fun selectWordAtOffset(offset: Int) {
        val tab = activeTab
        val text = tab.content.text
        if (text.isEmpty()) return setSelectionRange(0, 0)
        val pos = offset.coerceIn(0, text.length)
        var start = pos
        var end = pos
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_')) start--
        while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_')) end++
        if (start == end && pos < text.length) {
            start = pos
            end = pos + 1
        }
        setSelectionRange(start, end)
    }

    fun selectLineAtOffset(offset: Int) {
        val tab = activeTab
        val index = LineIndex(tab.content.text)
        val line = index.lineForOffset(offset.coerceIn(0, tab.content.text.length))
        setSelectionRange(index.lineStart(line), index.lineEnd(tab.content.text, line))
    }

    // ── Text Actions ──────────────────────────────────────────────
    private fun applyAction(result: ActionResult) {
        if (result.shouldRecord) updateContent(result.newValue) else updateTab { copy(content = result.newValue) }
    }

    fun handleEnterKey() = applyAction(handleEnter(content, indentStyle, settings.autoCloseBrackets))
    fun handleTabKey(shift: Boolean = false) = applyAction(handleTab(content, indentStyle, shift))
    fun moveLeft(extend: Boolean = false, byWord: Boolean = false) = applyAction(io.github.norbertweb.bluebird.editor.editor.actions.moveLeft(content, extend, byWord))
    fun moveRight(extend: Boolean = false, byWord: Boolean = false) = applyAction(io.github.norbertweb.bluebird.editor.editor.actions.moveRight(content, extend, byWord))
    fun moveHome(extend: Boolean = false, document: Boolean = false) = applyAction(io.github.norbertweb.bluebird.editor.editor.actions.moveHome(content, extend, document))
    fun moveEnd(extend: Boolean = false, document: Boolean = false) = applyAction(io.github.norbertweb.bluebird.editor.editor.actions.moveEnd(content, extend, document))
    fun moveByPage(direction: Int, extend: Boolean = false) {
        val lineHeight = 1
        val currentLine = LineIndex(content.text).lineForOffset(content.selection.start)
        val targetLine = (currentLine + direction * 30).coerceIn(1, LineIndex(content.text).lineCount)
        val index = LineIndex(content.text)
        val column = content.selection.start - index.lineStart(currentLine)
        val target = index.offsetAt(targetLine, column, content.text)
        applyAction(ActionResult(content.copy(selection = if (extend) TextRange(content.selection.start, target) else TextRange(target)), shouldRecord = false))
    }
    fun deleteBackward(byWord: Boolean = false) = applyAction(io.github.norbertweb.bluebird.editor.editor.actions.deleteBackward(content, byWord))
    fun deleteForward(byWord: Boolean = false) = applyAction(io.github.norbertweb.bluebird.editor.editor.actions.deleteForward(content, byWord))
    fun handleChar(char: Char) {
        val result = if (fileExt in setOf("html", "htm", "jsx", "tsx")) {
            handleWebCharInput(char, content, fileExt, settings.autoCloseBrackets)
        } else {
            handleCharInput(char, content, settings.autoCloseBrackets)
        }
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

    /** Clipboard-aware copy semantics for primary + secondary selections.
     * Multiple selections are exported in document order, separated by newlines.
     * With no selection, the current line is copied (without its line ending).
     */
    fun copySelectionText(): String {
        val selections = (listOf(content.selectionAsEditorSelection()) + secondarySelections)
            .filter { !it.isCaret }
            .sortedBy { it.min }
        if (selections.isEmpty()) {
            val line = content.currentLine()
            val start = content.lineStartOffset(line)
            val end = content.lineEndOffset(line)
            return content.text.substring(start, end)
        }
        return selections.joinToString("\n") {
            content.text.substring(it.min.coerceAtLeast(0), it.max.coerceAtMost(content.text.length))
        }
    }

    /** Cut all active selections atomically. If nothing is selected, cuts the current line. */
    fun cutSelectionText(): String {
        val selections = (listOf(content.selectionAsEditorSelection()) + secondarySelections)
            .filter { !it.isCaret }
            .distinctBy { it.min to it.max }
            .sortedByDescending { it.min }
        if (selections.isEmpty()) {
            val (result, cut) = cutText(content)
            applyAction(result)
            clearSecondarySelections()
            return cut
        }
        var result = content.text
        val copied = selections.sortedBy { it.min }.joinToString("\n") {
            content.text.substring(it.min.coerceAtLeast(0), it.max.coerceAtMost(content.text.length))
        }
        selections.forEach { sel ->
            val start = sel.min.coerceIn(0, result.length)
            val end = sel.max.coerceIn(start, result.length)
            result = result.removeRange(start, end)
        }
        val anchor = selections.minOf { it.min }.coerceIn(0, result.length)
        updateContent(content.copy(text = result, selection = TextRange(anchor)))
        clearSecondarySelections()
        return copied
    }

    /** Paste into all carets/selections. If clipboard has multiple lines and there are
     * multiple targets, matching lines are distributed one-per-target; otherwise the
     * complete clipboard text is inserted into every target. */
    fun pasteText(paste: String) {
        val targets = (listOf(content.selectionAsEditorSelection()) + secondarySelections)
            .distinctBy { it.min to it.max }
            .sortedBy { it.min }
        if (targets.size <= 1) {
            val target = targets.firstOrNull() ?: EditorSelection(content.selection.start)
            val start = target.min.coerceIn(0, content.text.length)
            val end = target.max.coerceIn(start, content.text.length)
            val newText = content.text.substring(0, start) + paste + content.text.substring(end)
            updateContent(content.copy(text = newText, selection = TextRange(start + paste.length)))
            clearSecondarySelections()
            return
        }
        val chunks = paste.split("\n")
        var result = content.text
        val inserted = mutableListOf<EditorSelection>()
        targets.sortedByDescending { it.min }.forEachIndexed { reverseIndex, target ->
            val start = target.min.coerceIn(0, result.length)
            val end = target.max.coerceIn(start, result.length)
            val chunkIndex = targets.lastIndex - reverseIndex
            val value = if (chunks.size == targets.size) chunks[chunkIndex] else paste
            result = result.substring(0, start) + value + result.substring(end)
            inserted += EditorSelection(start + value.length)
        }
        val primaryOffset = inserted.lastOrNull()?.start?.coerceIn(0, result.length) ?: content.selection.start
        updateContent(content.copy(text = result, selection = TextRange(primaryOffset)))
        clearSecondarySelections()
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

    private val currentLineIndex get() = LineIndex(content.text)
    val lineCount get() = currentLineIndex.lineCount
    val wordCount get() = content.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val charCount get() = content.text.length
    val selCount get() = if (content.selection.length > 0) content.selection.length else 0
    val cursorLine get() = currentLineIndex.lineForOffset(content.selection.start)
    fun cursorLineForTab(tabId: String): Int {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return 1
        return LineIndex(tab.content.text).lineForOffset(tab.content.selection.start)
    }
    val cursorCol get() = content.selection.start.coerceAtLeast(0) - currentLineIndex.lineStart(cursorLine) + 1
    val fileExt get() = fileName.substringAfterLast('.', "txt").lowercase()

    // ── Autocomplete ──────────────────────────────────────────────
    var autocompleteSuggestions by mutableStateOf<List<String>>(emptyList())
    var showAutocomplete by mutableStateOf(false)
    var showLivePreview by mutableStateOf(false)
    /** Desktop-style workspace overview; keeps the editor shell available underneath. */
    var showWorkspaceHome by mutableStateOf(false)

    fun acceptSuggestion(word: String) {
        val text = content.text
        val pos = content.selection.start
        val wordStart = CompletionEngine.findPrefixStart(text, pos)
        val newText = text.substring(0, wordStart) + word + text.substring(pos)
        updateContent(content.copy(text = newText, selection = TextRange(wordStart + word.length)))
        showAutocomplete = false
    }

    // ── Diagnostics ───────────────────────────────────────────────
    /** Lightweight structural diagnostics; replaced by language services in Phase 5. */
    fun diagnosticsForTab(tabId: String): List<Diagnostic> =
        tabs.firstOrNull { it.id == tabId }?.let { WebLanguageTools.diagnostics(it.content.text, it.fileName) } ?: emptyList()

    val diagnostics: List<Diagnostic>
        get() = diagnosticsForTab(activeTab.id)

    // ── Language intelligence ────────────────────────────────────
    var languageHover by mutableStateOf<HoverInfo?>(null)
    var referenceLocations by mutableStateOf<List<ReferenceLocation>>(emptyList())
    var showReferencesPanel by mutableStateOf(false)
    var pendingWorkspaceOpen by mutableStateOf<DefinitionLocation?>(null)
    var workspaceSearchResults by mutableStateOf<List<WorkspaceSearchResult>>(emptyList())
    var showWorkspaceSearch by mutableStateOf(false)
    var showWorkspaceSymbols by mutableStateOf(false)
    var showWorkspaceOutline by mutableStateOf(false)
    var showRenameSymbol by mutableStateOf(false)
    var renameTarget by mutableStateOf("")
    var workspaceSymbolQuery by mutableStateOf("")
    var workspaceIndexStatus by mutableStateOf("Workspace index not built")
    val workspaceIndex = WorkspaceSymbolIndex()

    // Phase 10 — live language-service UI state. The manager is optional: local
    // Bluebird intelligence remains the fallback when no LSP server is attached.
    val languageServerManager = LanguageServerManager()
    var lspStatus by mutableStateOf("Local intelligence")
    var lspDiagnosticsByPath by mutableStateOf<Map<String, List<LspDiagnostic>>>(emptyMap())
    var lspSemanticTokensByPath by mutableStateOf<Map<String, List<LspSemanticToken>>>(emptyMap())
    var pendingCodeActions by mutableStateOf<List<LspCodeAction>>(emptyList())
    var showCodeActions by mutableStateOf(false)
    private val lspOpenedPaths = mutableSetOf<String>()

    fun lspDiagnosticsForTab(tabId: String): List<LspDiagnostic> {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return emptyList()
        return lspDiagnosticsByPath[tab.filePath].orEmpty()
    }

    fun lspDiagnosticsForActiveTab(): List<LspDiagnostic> = lspDiagnosticsForTab(activeTab.id)

    fun lspSemanticTokensForTab(tabId: String): List<LspSemanticToken> {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return emptyList()
        return lspSemanticTokensByPath[tab.filePath].orEmpty()
    }

    fun refreshLspState() {
        lspStatus = if (languageServerManager.isConnected()) "LSP: Connected" else "Local intelligence"
    }

    fun syncLspEditorState(tab: TabData) {
        refreshLspState()
        if (!languageServerManager.isConnected() || tab.filePath.isBlank()) return
        languageServerManager.setDiagnosticsHandler { uri, diagnostics ->
            val path = uri?.let { runCatching { java.net.URI(it).path }.getOrNull() } ?: tab.filePath
            if (path.isBlank()) return@setDiagnosticsHandler
            lspDiagnosticsByPath = lspDiagnosticsByPath.toMutableMap().apply { this[path] = diagnostics }
        }
        if (lspOpenedPaths.add(tab.filePath)) {
            languageServerManager.didOpen(tab)
        }
        languageServerManager.requestSemanticTokensAsync(tab.filePath) { tokens ->
            lspSemanticTokensByPath = lspSemanticTokensByPath.toMutableMap().apply { this[tab.filePath] = tokens }
        }
    }

    fun requestCodeActions() {
        val tab = activeTab
        if (tab.filePath.isBlank() || !languageServerManager.isConnected()) {
            pendingCodeActions = emptyList()
            showCodeActions = true
            return
        }
        val start = tab.content.selection.min
        val end = tab.content.selection.max
        pendingCodeActions = languageServerManager.codeActions(tab.filePath, start, end)
        showCodeActions = true
    }

    fun applyFormatting() {
        val tab = activeTab
        if (tab.filePath.isNotBlank() && languageServerManager.isConnected() &&
            languageServerManager.capabilities().formatting) {
            val edits = languageServerManager.formatDocument(tab.filePath)
            if (edits.isNotEmpty()) {
                // The LSP edit application layer can be extended without changing the UI.
                toast("Formatting edits received from LSP")
                return
            }
        }
        val formatted = tab.content.text.lineSequence().joinToString("\n") { it.trimEnd() }
        if (formatted != tab.content.text) {
            updateContentForTab(tab.id, TextFieldValue(formatted, TextRange(formatted.length)))
            toast("Formatted document")
        } else toast("Document is already formatted")
    }

    fun diagnosticCount(): Int = diagnostics.size + lspDiagnosticsForActiveTab().size
    fun lspErrorCount(): Int = lspDiagnosticsForActiveTab().count { it.severity == 1 }
    fun lspWarningCount(): Int = lspDiagnosticsForActiveTab().count { it.severity == 2 }

    val workspaceRoot: File?
        get() = activeTab.filePath.takeIf { it.isNotBlank() }?.let { locateWorkspaceRoot(File(it)) }

    private fun locateWorkspaceRoot(file: File): File? {
        var dir: File? = if (file.isDirectory) file else file.parentFile
        var candidate: File? = dir
        while (dir != null) {
            if (File(dir, ".git").exists() || File(dir, "package.json").exists() ||
                File(dir, "settings.gradle").exists() || File(dir, "settings.gradle.kts").exists()) {
                candidate = dir
            }
            dir = dir.parentFile
        }
        return candidate
    }

    fun rebuildWorkspaceIndex() {
        val openBuffers = tabs.filter { it.filePath.isNotBlank() }
            .associate { it.filePath to it.content.text }
        workspaceIndex.rebuild(workspaceRoot, openBuffers)
        workspaceIndexStatus = "${workspaceIndex.indexedFilePaths.size} files · ${workspaceIndex.indexedSymbolCount} symbols"
    }

    fun searchWorkspace(query: String, caseSensitive: Boolean = false, regexMode: Boolean = false) {
        val openBuffers = tabs.filter { it.filePath.isNotBlank() }.associate { it.filePath to it.content.text }
        workspaceIndex.ensureFresh(workspaceRoot, openBuffers)
        workspaceIndexStatus = "${workspaceIndex.indexedFilePaths.size} files · ${workspaceIndex.indexedSymbolCount} symbols"
        workspaceSearchResults = workspaceIndex.search(query, caseSensitive, regexMode)
        showWorkspaceSearch = true
        toast(if (workspaceSearchResults.isEmpty()) "No workspace results" else "${workspaceSearchResults.size} workspace result(s)")
    }

    fun showWorkspaceSymbolTree() {
        workspaceIndex.ensureFresh(workspaceRoot, tabs.filter { it.filePath.isNotBlank() }.associate { it.filePath to it.content.text })
        workspaceIndexStatus = "${workspaceIndex.indexedFilePaths.size} files · ${workspaceIndex.indexedSymbolCount} symbols"
        workspaceSymbolQuery = ""
        showWorkspaceSymbols = true
    }

    fun showWorkspaceOutline() {
        workspaceIndex.ensureFresh(workspaceRoot, tabs.filter { it.filePath.isNotBlank() }.associate { it.filePath to it.content.text })
        workspaceIndexStatus = "${workspaceIndex.indexedFilePaths.size} files · ${workspaceIndex.indexedSymbolCount} symbols"
        showWorkspaceOutline = true
    }

    fun prepareRenameSymbol() {
        val tab = activeTab
        val word = LanguageIntelligence.wordAt(tab.content.text, tab.content.selection.start)
        if (word.isBlank()) { toast("Place the cursor on a symbol first"); return }
        renameTarget = word
        showRenameSymbol = true
    }

    fun renameWorkspaceSymbol(newName: String) {
        val oldName = renameTarget
        if (newName.isBlank() || oldName.isBlank() || oldName == newName) {
            showRenameSymbol = false
            return
        }
        rebuildWorkspaceIndex()
        val changes = workspaceIndex.renameSymbol(oldName, newName)
        if (changes.isEmpty()) { toast("No references found for $oldName"); showRenameSymbol = false; return }
        changes.forEach { (path, text) ->
            val open = tabs.firstOrNull { runCatching { File(it.filePath).canonicalPath == File(path).canonicalPath }.getOrDefault(it.filePath == path) }
            if (open != null) {
                updateContentForTab(open.id, TextFieldValue(text, TextRange(text.length)))
            } else {
                runCatching { ProductionHardening.atomicWriteText(File(path), text) }
            }
        }
        rebuildWorkspaceIndex()
        showRenameSymbol = false
        toast("Renamed $oldName → $newName in ${changes.size} file(s)")
    }

    fun documentSymbolsForTab(tabId: String): List<DocumentSymbol> =
        tabs.firstOrNull { it.id == tabId }?.let { LanguageIntelligence.symbols(it.content.text, it.fileName) }.orEmpty()

    fun goToDefinition() {
        val tab = activeTab
        val word = LanguageIntelligence.wordAt(tab.content.text, tab.content.selection.start)
        rebuildWorkspaceIndex()
        val location = workspaceIndex.definitionForImport(tab.filePath, word)
            ?: workspaceIndex.definition(word, tab.filePath)
            ?: LanguageIntelligence.definition(tab.content.text, tab.fileName, tab.content.selection.start)
        if (location == null) { toast("No definition found") ; return }
        val open = tabs.indexOfFirst { it.filePath == location.fileName }
        if (open >= 0) {
            selectTabIdInGroup(activeEditorGroup, tabs[open].id)
            updateTabById(tabs[open].id) { copy(content = content.copy(selection = TextRange(location.offset))) }
        } else {
            pendingWorkspaceOpen = location
        }
        toast("Definition: ${location.symbol} · line ${location.line}")
    }

    fun findReferences() {
        val tab = activeTab
        val word = LanguageIntelligence.wordAt(tab.content.text, tab.content.selection.start)
        rebuildWorkspaceIndex()
        referenceLocations = workspaceIndex.referencesWithImports(tab.filePath, word)
        showReferencesPanel = referenceLocations.isNotEmpty()
        toast(if (referenceLocations.isEmpty()) "No references found" else "${referenceLocations.size} reference(s) found")
    }

    fun showHoverInfo() {
        val tab = activeTab
        languageHover = LanguageIntelligence.hover(tab.content.text, tab.fileName, tab.content.selection.start)
        if (languageHover == null) toast("No language information available")
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
    var showCommandPalette by mutableStateOf(false)
    var showQuickOpen by mutableStateOf(false)
    var showSymbolPicker by mutableStateOf(false)
    var showFindResultsPanel by mutableStateOf(false)
    // ── Phase 1 IDE shell state ───────────────────────────────────
    var shellActivity by mutableStateOf(ShellActivity.EXPLORER)
    var explorerRefreshKey by mutableIntStateOf(0)
    var showBottomPanel by mutableStateOf(false)
    var bottomPanel by mutableStateOf(BottomPanel.OUTPUT)

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
            ProductionHardening.atomicWriteText(File(path), withNewline, encoding.charset)
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
            ProductionHardening.atomicWriteText(f, content.text, encoding.charset)
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
            val draft = autosavePath(context.cacheDir, fileName)
            ProductionHardening.atomicWriteText(draft, content.text, encoding.charset, ProductionHardening.MAX_AUTOSAVE_BYTES)
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
        EditorCommand("Quick Open", "Ctrl+P", "navigate") { showQuickOpen = true },
        EditorCommand("Command Palette", "Ctrl+Shift+P", "navigate") { showCommandPalette = true },
        EditorCommand("Find", "Ctrl+F", "edit") { showFindBar = true; showReplace = false },
        EditorCommand("Find & Replace", "Ctrl+H", "edit") { showFindBar = true; showReplace = true },
        EditorCommand("Search in Workspace", "Ctrl+Shift+F", "navigate") { showWorkspaceSearch = true },
        EditorCommand("Workspace Symbols", "Ctrl+T", "navigate") { showWorkspaceSymbolTree() },
        EditorCommand("Workspace Outline", "Ctrl+Shift+T", "navigate") { showWorkspaceOutline() },
        EditorCommand("Rename Symbol", "F2", "navigate") { prepareRenameSymbol() },
        EditorCommand("Go to Line…", "Ctrl+G", "edit") { showGoToLineDialog = true },
        EditorCommand("Go to Definition", "F12", "navigate") { goToDefinition() },
        EditorCommand("Find References", "Shift+F12", "navigate") { findReferences() },
        EditorCommand("Show Hover Information", "Ctrl+K Ctrl+I", "navigate") { showHoverInfo() },
        EditorCommand("Quick Fix / Code Actions", "Ctrl+.", "navigate") { requestCodeActions() },
        EditorCommand("Format Document", "Shift+Alt+F", "format") { applyFormatting() },
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
        EditorCommand("Next Bookmark", "Ctrl+F2", "nav") { nextBookmark() },
        EditorCommand("Previous Bookmark", "Ctrl+Shift+F2", "nav") { prevBookmark() },
        EditorCommand("Show Bookmarks", "", "nav") { showBookmarksPanel = true },
        EditorCommand("Go to Symbol…", "Ctrl+Shift+O", "navigate") { showSymbolPicker = true },
        EditorCommand("Show Statistics", "", "view") { showStatsPanel = true },
        EditorCommand("Encoding…", "", "file") { showEncodingPicker = true },
        EditorCommand("Line Ending…", "", "file") { showLineEndingPicker = true },
        EditorCommand("Settings", "", "view") { showSettingsPanel = true },
        EditorCommand("Snippet Manager", "", "view") { showSnippetManager = true },
        EditorCommand("Restore Draft", "", "file") { /* handled by caller */ },
    )
}

enum class ShellActivity {
    EXPLORER, SEARCH, SOURCE_CONTROL, RUN_DEBUG, EXTENSIONS
}

enum class BottomPanel {
    PROBLEMS, OUTPUT, TERMINAL
}

data class EditorCommand(
    val label: String,
    val shortcut: String,
    val category: String,
    val action: () -> Unit,
)

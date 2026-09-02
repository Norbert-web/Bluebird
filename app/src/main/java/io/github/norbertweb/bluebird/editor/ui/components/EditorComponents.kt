package io.github.norbertweb.bluebird.editor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.editor.core.DEFAULT_SNIPPETS
import io.github.norbertweb.bluebird.editor.core.FileEncoding
import io.github.norbertweb.bluebird.editor.core.IndentStyle
import io.github.norbertweb.bluebird.editor.core.LineEnding
import io.github.norbertweb.bluebird.editor.core.TabData
import io.github.norbertweb.bluebird.editor.editor.core.PremiumEditorState
import io.github.norbertweb.bluebird.editor.ui.theme.EditorColors
import io.github.norbertweb.bluebird.ui.components.FluentIcon

// ─────────────────────────────────────────────────────────────────
// Tab Bar
// ─────────────────────────────────────────────────────────────────

@Composable
fun PremiumTabBar(
    s: PremiumEditorState,
    onSave: () -> Unit,
    onNew: () -> Unit,
    group: Int = 0,
) {
    val c = s.colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(c.surface).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Box(
                Modifier.size(24.dp).background(c.accent, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(FluentIcon.Code, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(8.dp))

            // Tabs
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                s.tabsForGroup(group).forEachIndexed { index, tab ->
                    val isActive = index == s.tabIndexForGroup(group)
                    TabItem(
                        tab = tab, isActive = isActive, colors = c,
                        onClick = { s.selectTabInGroup(group, index); s.activateEditorGroup(group) },
                        onMove = { from, to ->
                            s.activateEditorGroup(group)
                            s.reorderTabsInGroup(group, from, to)
                        },
                        index = index,
                        onClose = {
                            if (!s.closeTab(index)) {
                                s.pendingCloseTabIndex = index
                                s.showUnsavedDialog = true
                            }
                        },
                        onPin = { s.pinTab(index) }
                    )
                    Spacer(Modifier.width(2.dp))
                }
                // New tab button
                EdIconBtn(FluentIcon.Add, "New Tab", c.textMuted) { onNew() }
            }

            Spacer(Modifier.width(8.dp))
            // Quick action buttons
            EdIconBtn(FluentIcon.Save, "Save", if (s.activeTabForGroup(group).isModified) c.accent else c.textMuted, enabled = s.activeTabForGroup(group).isModified) { s.activateEditorGroup(group); onSave() }
            EdIconBtn(FluentIcon.Search, "Command Palette", c.textMuted) { s.activateEditorGroup(group); s.showCommandPalette = true }
        }
        Divider(color = c.border, thickness = 1.dp)
    }
}

@Composable
private fun TabItem(
    tab: TabData, isActive: Boolean, colors: EditorColors,
    onClick: () -> Unit, onMove: (Int, Int) -> Unit, index: Int,
    onClose: () -> Unit, onPin: () -> Unit
) {
    var dragDistance by remember(tab.id) { mutableStateOf(0f) }
    Row(
        Modifier
            .height(36.dp).widthIn(min = 90.dp, max = 220.dp)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(if (isActive) colors.tabActive else colors.tabBg)
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) colors.accent else Color.Transparent,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            )
            .clickable(onClick = onClick)
            .pointerInput(tab.id) {
                detectDragGestures(
                    onDragEnd = { dragDistance = 0f },
                    onDragCancel = { dragDistance = 0f },
                    onDrag = { _, drag ->
                        dragDistance += drag.x
                        when {
                            dragDistance > 70f && index < Int.MAX_VALUE -> {
                                onMove(index, index + 1)
                                dragDistance = 0f
                            }
                            dragDistance < -70f && index > 0 -> {
                                onMove(index, index - 1)
                                dragDistance = 0f
                            }
                        }
                    }
                )
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // File type icon dot
        val dotColor = fileTypeColor(tab.fileExt, colors)
        Box(Modifier.size(7.dp).background(dotColor, CircleShape))

        // Pin indicator
        if (tab.isPinned) Icon(FluentIcon.PushPin, null, tint = colors.accent, modifier = Modifier.size(10.dp))

        // Modified dot
        if (tab.isModified) Box(Modifier.size(6.dp).background(colors.gold, CircleShape))

        Text(
            tab.fileName,
            color = if (isActive) colors.text else colors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        // Close button
        if (!tab.isPinned) {
            Icon(
                FluentIcon.Close, null, tint = colors.textMuted,
                modifier = Modifier.size(13.dp).clip(RoundedCornerShape(2.dp))
                    .clickable(onClick = onClose)
            )
        }
    }
}

private val TabData.fileExt get() = fileName.substringAfterLast('.', "txt").lowercase()

private fun fileTypeColor(ext: String, c: EditorColors): Color = when (ext) {
    "kt", "kts" -> Color(0xFF7F52FF)
    "java" -> Color(0xFFED8B00)
    "py", "pyw" -> Color(0xFF3776AB)
    "js", "jsx" -> Color(0xFFF7DF1E)
    "ts", "tsx" -> Color(0xFF3178C6)
    "rs" -> Color(0xFFDEA584)
    "go" -> Color(0xFF00ADD8)
    "html", "htm" -> Color(0xFFE34F26)
    "css", "scss" -> Color(0xFF1572B6)
    "json" -> Color(0xFF000000).copy(alpha = 0.6f)
    "md" -> Color(0xFF083fa1)
    "sql" -> Color(0xFF336791)
    "xml", "svg" -> Color(0xFFFF6600)
    "sh", "bash" -> Color(0xFF89E051)
    else -> c.textMuted
}

// ─────────────────────────────────────────────────────────────────
// Breadcrumb Bar
// ─────────────────────────────────────────────────────────────────

@Composable
fun BreadcrumbBar(s: PremiumEditorState, group: Int = 0) {
    val tab = s.activeTabForGroup(group)
    val c = s.colors
    if (!s.settings.showBreadcrumb || tab.filePath.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().height(22.dp).background(c.surface).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val parts = tab.filePath.split('/').filter { it.isNotEmpty() }
        parts.forEachIndexed { i, part ->
            if (i > 0) Text("/", color = c.textMuted, fontSize = 11.sp)
            Text(
                part,
                color = if (i == parts.lastIndex) c.text else c.textMuted,
                fontSize = 11.sp,
                fontWeight = if (i == parts.lastIndex) FontWeight.Medium else FontWeight.Normal
            )
        }
        Spacer(Modifier.weight(1f))
        val cursorBefore = tab.content.text.substring(0, tab.content.selection.start.coerceAtMost(tab.content.text.length))
        val cursorLine = cursorBefore.count { it == '\n' } + 1
        val cursorCol = cursorBefore.substringAfterLast('\n').length + 1
        Text("Ln $cursorLine, Col $cursorCol", color = c.textMuted, fontSize = 10.sp)
    }
    Divider(color = c.border)
}

// ─────────────────────────────────────────────────────────────────
// Menu Bar
// ─────────────────────────────────────────────────────────────────

@Composable
fun PremiumMenuBar(
    s: PremiumEditorState,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRestoreDraft: () -> Unit,
    onOpenFile: () -> Unit,
    group: Int = 0,
) {
    val c = s.colors
    var openMenu by remember { mutableStateOf<String?>(null) }
    fun closeAll() { s.activateEditorGroup(group); s.activateEditorGroup(group); openMenu = null }
    fun toggle(name: String) { s.activateEditorGroup(group); s.activateEditorGroup(group); openMenu = if (openMenu == name) null else name }

    Row(
        Modifier.fillMaxWidth().height(30.dp).background(c.surfaceHover).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── File ──
        MenuEntry("File", c, openMenu == "file", { toggle("file") }) {
            DdItem(FluentIcon.Add, "New Tab", "Ctrl+N", c) { s.activateEditorGroup(group); s.newTab(); closeAll() }
            DdItem(FluentIcon.FolderOpen, "Open File…", "Ctrl+O", c) { onOpenFile(); closeAll() }
            DdItem(FluentIcon.History, "Recent Files", "", c, enabled = s.settings.recentFiles.isNotEmpty()) { closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Save, "Save", "Ctrl+S", c, enabled = s.isModified) { onSave(); closeAll() }
            DdItem(FluentIcon.SaveAs, "Save As…", "Ctrl+Shift+S", c) { s.activateEditorGroup(group); s.showSaveAsDialog = true; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Share, "Share", "", c) { onShare(); closeAll() }
            DdItem(FluentIcon.ContentCopy, "Copy All", "", c) { closeAll() }
            DdItem(FluentIcon.Restore, "Restore Draft", "", c) { onRestoreDraft(); closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Analytics, "Statistics", "", c) { s.activateEditorGroup(group); s.showStatsPanel = true; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Close, "Close Tab", "Ctrl+W", c) {
                if (!s.closeTab(s.activeTabIndex)) { s.activateEditorGroup(group); s.pendingCloseTabIndex = s.activeTabIndex; s.showUnsavedDialog = true }
                closeAll()
            }
        }

        // ── Edit ──
        MenuEntry("Edit", c, openMenu == "edit", { toggle("edit") }) {
            DdItem(FluentIcon.Undo, "Undo", "Ctrl+Z", c, enabled = s.canUndo) { s.activateEditorGroup(group); s.undo(); closeAll() }
            DdItem(FluentIcon.Redo, "Redo", "Ctrl+Y", c, enabled = s.canRedo) { s.activateEditorGroup(group); s.redo(); closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.ContentCut, "Cut", "Ctrl+X", c) { closeAll() }
            DdItem(FluentIcon.ContentCopy, "Copy", "Ctrl+C", c) { closeAll() }
            DdItem(FluentIcon.ContentPaste, "Paste", "Ctrl+V", c) { closeAll() }
            DdItem(FluentIcon.SelectAll, "Select All", "Ctrl+A", c) {
                s.updateTab { copy(content = content.copy(selection = TextRange(0, content.text.length))) }
                closeAll()
            }
            DdDivider(c.border)
            DdItem(FluentIcon.Search, "Find", "Ctrl+F", c) { s.activateEditorGroup(group); s.showFindBar = true; s.showReplace = false; closeAll() }
            DdItem(FluentIcon.FindReplace, "Replace", "Ctrl+H", c) { s.activateEditorGroup(group); s.showFindBar = true; s.showReplace = true; closeAll() }
            DdItem(FluentIcon.VerticalAlignCenter, "Go to Line…", "Ctrl+G", c) { s.activateEditorGroup(group); s.showGoToLineDialog = true; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Comment, "Toggle Comment", "Ctrl+/", c) { s.activateEditorGroup(group); s.toggleComment(); closeAll() }
            DdItem(FluentIcon.ContentCopy, "Duplicate Line", "Ctrl+D", c) { s.activateEditorGroup(group); s.duplicateCurrentLine(); closeAll() }
            DdItem(FluentIcon.DeleteForever, "Delete Line", "Ctrl+Shift+K", c) { s.activateEditorGroup(group); s.deleteCurrentLine(); closeAll() }
            DdItem(FluentIcon.ArrowUpward, "Move Line Up", "Alt+↑", c) { s.activateEditorGroup(group); s.moveCurrentLineUp(); closeAll() }
            DdItem(FluentIcon.ArrowDownward, "Move Line Down", "Alt+↓", c) { s.activateEditorGroup(group); s.moveCurrentLineDown(); closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Schedule, "Insert Date/Time", "F5", c) { s.activateEditorGroup(group); s.insertDateTime(); closeAll() }
        }

        // ── Selection ──
        MenuEntry("Selection", c, openMenu == "sel", { toggle("sel") }) {
            DdItem(FluentIcon.TextFields, "UPPERCASE", "", c) { s.activateEditorGroup(group); s.toUpper(); closeAll() }
            DdItem(FluentIcon.TextFields, "lowercase", "", c) { s.activateEditorGroup(group); s.toLower(); closeAll() }
            DdItem(FluentIcon.TextFields, "Title Case", "", c) { s.activateEditorGroup(group); s.toTitle(); closeAll() }
            DdItem(FluentIcon.TextFields, "snake_case", "", c) { s.activateEditorGroup(group); s.toSnake(); closeAll() }
            DdItem(FluentIcon.TextFields, "camelCase", "", c) { s.activateEditorGroup(group); s.toCamel(); closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Sort, "Sort Lines Ascending", "", c) { s.activateEditorGroup(group); s.sortLinesAsc(); closeAll() }
            DdItem(FluentIcon.Sort, "Sort Lines Descending", "", c) { s.activateEditorGroup(group); s.sortLinesDesc(); closeAll() }
            DdItem(FluentIcon.FilterList, "Remove Duplicate Lines", "", c) { s.activateEditorGroup(group); s.removeDuplicates(); closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.SpaceBar, "Trim Trailing Whitespace", "", c) { s.activateEditorGroup(group); s.trimWhitespace(); closeAll() }
        }

        // ── View ──
        MenuEntry("View", c, openMenu == "view", { toggle("view") }) {
            DdCheckItem("Word Wrap", s.settings.wordWrap, c) { s.activateEditorGroup(group); s.updateSettings { copy(wordWrap = !wordWrap) }; closeAll() }
            DdCheckItem("Line Numbers", s.settings.showLineNumbers, c) { s.activateEditorGroup(group); s.updateSettings { copy(showLineNumbers = !showLineNumbers) }; closeAll() }
            DdCheckItem("Minimap", s.showMinimap, c) { s.activateEditorGroup(group); s.showMinimap = !s.showMinimap; closeAll() }
            DdCheckItem("Syntax Highlighting", s.settings.syntaxHighlight, c) { s.activateEditorGroup(group); s.updateSettings { copy(syntaxHighlight = !syntaxHighlight) }; closeAll() }
            DdCheckItem("Breadcrumb", s.settings.showBreadcrumb, c) { s.activateEditorGroup(group); s.updateSettings { copy(showBreadcrumb = !showBreadcrumb) }; closeAll() }
            DdCheckItem("Show Whitespace", s.settings.showWhitespace, c) { s.activateEditorGroup(group); s.updateSettings { copy(showWhitespace = !showWhitespace) }; closeAll() }
            DdCheckItem("Column Guide", s.settings.showColumnGuide, c) { s.activateEditorGroup(group); s.updateSettings { copy(showColumnGuide = !showColumnGuide) }; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.ZoomIn, "Zoom In", "Ctrl++", c) { s.activateEditorGroup(group); s.updateSettings { copy(zoom = (zoom + 0.1f).coerceAtMost(4f)) }; closeAll() }
            DdItem(FluentIcon.ZoomOut, "Zoom Out", "Ctrl+-", c) { s.activateEditorGroup(group); s.updateSettings { copy(zoom = (zoom - 0.1f).coerceAtLeast(0.25f)) }; closeAll() }
            DdItem(FluentIcon.ZoomOutMap, "Reset Zoom", "Ctrl+0", c) { s.activateEditorGroup(group); s.updateSettings { copy(zoom = 1f) }; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Settings, "Settings…", "", c) { s.activateEditorGroup(group); s.showSettingsPanel = true; closeAll() }
        }

        // ── Format ──
        MenuEntry("Format", c, openMenu == "fmt", { toggle("fmt") }) {
            DdItem(FluentIcon.Code, "Monospace", "", c) { s.activateEditorGroup(group); s.updateSettings { copy(fontFamily = "Monospace") }; closeAll() }
            DdItem(FluentIcon.Subject, "Sans-Serif", "", c) { s.activateEditorGroup(group); s.updateSettings { copy(fontFamily = "SansSerif") }; closeAll() }
            DdItem(FluentIcon.Notes, "Serif", "", c) { s.activateEditorGroup(group); s.updateSettings { copy(fontFamily = "Serif") }; closeAll() }
            DdItem(FluentIcon.SpaceBar, "Courier", "", c) { s.activateEditorGroup(group); s.updateSettings { copy(fontFamily = "Courier") }; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.TextDecrease, "Decrease Font", "Ctrl+-", c) { s.activateEditorGroup(group); s.updateSettings { copy(fontSize = (fontSize - 1).coerceAtLeast(8f)) }; closeAll() }
            DdItem(FluentIcon.TextIncrease, "Increase Font", "Ctrl++", c) { s.activateEditorGroup(group); s.updateSettings { copy(fontSize = (fontSize + 1).coerceAtMost(48f)) }; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.FormatIndentIncrease, "2 Spaces", "", c) { s.activateEditorGroup(group); s.updateSettings { copy(indentStyle = IndentStyle.SPACES_2) }; closeAll() }
            DdItem(FluentIcon.FormatIndentIncrease, "4 Spaces", "", c) { s.activateEditorGroup(group); s.updateSettings { copy(indentStyle = IndentStyle.SPACES_4) }; closeAll() }
            DdItem(FluentIcon.FormatIndentIncrease, "Tabs", "", c) { s.activateEditorGroup(group); s.updateSettings { copy(indentStyle = IndentStyle.TAB) }; closeAll() }
        }

        // ── Nav ──
        MenuEntry("Navigate", c, openMenu == "nav", { toggle("nav") }) {
            DdItem(FluentIcon.Bookmark, "Toggle Bookmark", "Ctrl+B", c) { s.activateEditorGroup(group); s.toggleBookmark(s.cursorLine); closeAll() }
            DdItem(FluentIcon.NavigateNext, "Next Bookmark", "F2", c) { s.activateEditorGroup(group); s.nextBookmark(); closeAll() }
            DdItem(FluentIcon.NavigateBefore, "Prev Bookmark", "Shift+F2", c) { s.activateEditorGroup(group); s.prevBookmark(); closeAll() }
            DdItem(FluentIcon.BookmarkBorder, "Show Bookmarks", "", c) { s.activateEditorGroup(group); s.showBookmarksPanel = true; closeAll() }
            DdDivider(c.border)
            DdItem(FluentIcon.Extension, "Snippet Manager", "", c) { s.activateEditorGroup(group); s.showSnippetManager = true; closeAll() }
        }

        Spacer(Modifier.weight(1f))

        // Quick icon strip
        EdIconBtn(FluentIcon.FindReplace, "Find", c.textMuted) { s.activateEditorGroup(group); s.showFindBar = !s.showFindBar }
        EdIconBtn(FluentIcon.WrapText, "Word Wrap", if (s.settings.wordWrap) c.accent else c.textMuted) { s.activateEditorGroup(group); s.updateSettings { copy(wordWrap = !wordWrap) } }
        EdIconBtn(FluentIcon.FormatListNumbered, "Line Nums", if (s.settings.showLineNumbers) c.accent else c.textMuted) { s.activateEditorGroup(group); s.updateSettings { copy(showLineNumbers = !showLineNumbers) } }
        EdIconBtn(if (s.isReadOnly) FluentIcon.Lock else FluentIcon.LockOpen, "Read-only", if (s.isReadOnly) c.gold else c.textMuted) { s.activateEditorGroup(group); s.updateTab { copy(isReadOnly = !isReadOnly) } }
    }
    Divider(color = c.border, thickness = 1.dp)
}

@Composable
private fun MenuEntry(
    label: String,
    c: EditorColors,
    isOpen: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box {
        MenuBtn(label, c, isOpen, onToggle)
        DropdownMenu(expanded = isOpen, onDismissRequest = onToggle) { content() }
    }
}

@Composable
fun MenuBtn(label: String, c: EditorColors, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(if (active) c.accent.copy(0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (active) c.accent else c.text, fontSize = 12.sp)
    }
}

@Composable
fun DdItem(
    icon: ImageVector, label: String, shortcut: String, c: EditorColors,
    enabled: Boolean = true, onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, color = if (enabled) c.text else c.text.copy(0.35f), fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = if (enabled) c.textSecondary else c.textMuted, modifier = Modifier.size(15.dp)) },
        trailingIcon = { if (shortcut.isNotEmpty()) Text(shortcut, color = c.textMuted, fontSize = 10.sp) },
        onClick = onClick, enabled = enabled
    )
}

@Composable
fun DdCheckItem(label: String, checked: Boolean, c: EditorColors, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = c.text, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                if (checked) FluentIcon.CheckBox else FluentIcon.CheckBoxOutlineBlank,
                null, tint = if (checked) c.accent else c.textMuted,
                modifier = Modifier.size(15.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
fun DdDivider(color: Color) = Divider(Modifier.padding(vertical = 3.dp), color = color)

// ─────────────────────────────────────────────────────────────────
// Find / Replace Bar
// ─────────────────────────────────────────────────────────────────

@Composable
fun PremiumFindBar(s: PremiumEditorState) {
    val c = s.colors
    val inputBg = c.surfaceHover
    val matches = remember(s.findQuery, s.content.text, s.matchCase, s.useRegex, s.wholeWord) { s.activateEditorGroup(group); s.findMatches() }
    val matchCount = matches.size
    val isInvalidRegex = s.useRegex && s.findQuery.isNotEmpty() && try { Regex(s.findQuery); false } catch (_: Exception) { true }

    Column(Modifier.fillMaxWidth().background(c.surface).padding(8.dp)) {
        // Find row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FindField("Find…", s.findQuery, inputBg, c.border, c.text, c.textMuted,
                isError = isInvalidRegex, onChange = { s.activateEditorGroup(group); s.findQuery = it; s.currentMatchIndex = 0 })

            // Match info
            Text(
                if (s.findQuery.isEmpty()) "" else if (matchCount == 0) "No results" else "${(s.currentMatchIndex + 1)} / $matchCount",
                color = if (matchCount == 0 && s.findQuery.isNotEmpty()) c.danger else c.textMuted,
                fontSize = 11.sp, modifier = Modifier.width(60.dp)
            )

            ToggleChip("Aa", s.matchCase, c) { s.activateEditorGroup(group); s.matchCase = !s.matchCase }
            ToggleChip(".*", s.useRegex, c) { s.activateEditorGroup(group); s.useRegex = !s.useRegex }
            ToggleChip("W", s.wholeWord, c) { s.activateEditorGroup(group); s.wholeWord = !s.wholeWord }

            Spacer(Modifier.weight(1f))

            EdIconBtn(FluentIcon.KeyboardArrowUp, "Previous", c.text, enabled = matchCount > 0) { s.activateEditorGroup(group); s.jumpToMatch(-1) }
            EdIconBtn(FluentIcon.KeyboardArrowDown, "Next", c.text, enabled = matchCount > 0) { s.activateEditorGroup(group); s.jumpToMatch(1) }
            EdIconBtn(FluentIcon.FormatListBulleted, "All Results", c.text, enabled = matchCount > 0) { s.activateEditorGroup(group); s.showFindResultsPanel = true }
            EdIconBtn(FluentIcon.Close, "Close Find", c.textMuted) { s.activateEditorGroup(group); s.showFindBar = false }
        }

        // Replace row
        AnimatedVisibility(s.showReplace) {
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FindField("Replace…", s.replaceQuery, inputBg, c.border, c.text, c.textMuted,
                    onChange = { s.activateEditorGroup(group); s.replaceQuery = it })
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { s.activateEditorGroup(group); s.replaceOne() },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    enabled = matchCount > 0 && !s.activeTab.isReadOnly
                ) { Text("Replace", fontSize = 11.sp, color = c.text) }
                OutlinedButton(
                    onClick = { s.activateEditorGroup(group); s.replaceAll() },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    enabled = matchCount > 0 && !s.activeTab.isReadOnly
                ) { Text("Replace All", fontSize = 11.sp, color = c.text) }
            }
        }
    }
    Divider(color = c.border)
}

@Composable
private fun ToggleChip(label: String, active: Boolean, c: EditorColors, onClick: () -> Unit) {
    Box(
        Modifier.size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) c.accent.copy(0.2f) else Color.Transparent)
            .border(1.dp, if (active) c.accent else c.border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) c.accent else c.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun FindField(
    placeholder: String, value: String, bg: Color, border: Color, tc: Color, tcm: Color,
    isError: Boolean = false, onChange: (String) -> Unit
) {
    Row(
        Modifier.width(220.dp).height(28.dp)
            .clip(RoundedCornerShape(5.dp)).background(bg)
            .border(1.dp, if (isError) Color.Red else border, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = tcm, fontSize = 12.sp)
            BasicTextField(value, onChange,
                textStyle = TextStyle(color = tc, fontSize = 12.sp),
                cursorBrush = SolidColor(tc), singleLine = true,
                modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) {
            Icon(FluentIcon.Close, null, tint = tcm,
                modifier = Modifier.size(13.dp).clickable { onChange("") })
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Line Number Gutter
// ─────────────────────────────────────────────────────────────────

@Composable
fun PremiumGutter(s: PremiumEditorState, scrollState: ScrollState, tab: TabData = s.activeTab) {
    val c = s.colors
    val effectiveFontSize = (s.fontSize * s.zoom).sp

    Column(
        Modifier.width(56.dp).fillMaxHeight()
            .background(c.lineNumBg)
            .verticalScroll(scrollState)
            .padding(top = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        val lineCount = tab.content.text.count { it == '\n' } + 1
        val cursorBefore = tab.content.text.substring(0, tab.content.selection.start.coerceAtMost(tab.content.text.length))
        val cursorLine = cursorBefore.count { it == '\n' } + 1
        repeat(lineCount) { i ->
            val lineNo = i + 1
            val isCurrent = lineNo == cursorLine
            val hasBookmark = tab.bookmarks.any { it.line == lineNo }
            val isModified = false // would track line-level changes with git diff

            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    .clickable { s.activateEditorGroup(group); s.goToLineForTab(tab.id, lineNo) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Git gutter indicator (left 3px)
                if (s.settings.showGitGutter) {
                    Box(Modifier.width(3.dp).fillMaxHeight()
                        .background(if (isModified) c.gutterModified else Color.Transparent))
                }

                // Bookmark indicator
                if (hasBookmark) {
                    Box(Modifier.size(8.dp).background(c.gold, CircleShape))
                } else {
                    Spacer(Modifier.width(8.dp))
                }

                // Line number
                Text(
                    "$lineNo",
                    color = if (isCurrent) c.accent else c.textMuted,
                    fontSize = (effectiveFontSize.value * 0.85f).sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(end = 8.dp, top = 0.5.dp, bottom = 0.5.dp)
                )
            }
        }
    }
    Box(Modifier.fillMaxHeight().width(1.dp).background(c.border))
}

// ─────────────────────────────────────────────────────────────────
// Minimap
// ─────────────────────────────────────────────────────────────────

@Composable
fun MinimapPanel(s: PremiumEditorState, scrollState: ScrollState, totalContentHeight: Int, tab: TabData = s.activeTab) {
    val c = s.colors
    val text = tab.content.text
    val width = 80.dp

    Canvas(
        Modifier.width(width).fillMaxHeight()
            .background(c.minimapBg)
            .border(BorderStroke(1.dp, c.border))
    ) {
        val totalLines = text.count { it == '\n' } + 1
        val lineHeight = size.height / totalLines.coerceAtLeast(1)
        val charWidth = size.width / 80f

        text.split('\n').forEachIndexed { lineIdx, line ->
            val y = lineIdx * lineHeight
            line.take(80).forEachIndexed { charIdx, ch ->
                if (!ch.isWhitespace()) {
                    drawRect(
                        color = c.text.copy(alpha = 0.4f),
                        topLeft = Offset(charIdx * charWidth, y),
                        size = androidx.compose.ui.geometry.Size(charWidth * 0.8f, lineHeight * 0.7f)
                    )
                }
            }
        }

        // Viewport indicator
        if (totalContentHeight > 0) {
            val viewportFrac = size.height.coerceAtLeast(1f) / totalContentHeight.toFloat()
            val scrollFrac = scrollState.value.toFloat() / scrollState.maxValue.coerceAtLeast(1)
            val thumbHeight = (size.height * viewportFrac).coerceAtLeast(20f)
            val thumbTop = (size.height - thumbHeight) * scrollFrac
            drawRect(
                color = c.accent.copy(alpha = 0.15f),
                topLeft = Offset(0f, thumbTop),
                size = androidx.compose.ui.geometry.Size(size.width, thumbHeight)
            )
            drawRect(
                color = c.accent.copy(alpha = 0.3f),
                topLeft = Offset(0f, thumbTop),
                size = androidx.compose.ui.geometry.Size(size.width, 1f)
            )
            drawRect(
                color = c.accent.copy(alpha = 0.3f),
                topLeft = Offset(0f, thumbTop + thumbHeight - 1f),
                size = androidx.compose.ui.geometry.Size(size.width, 1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Status Bar
// ─────────────────────────────────────────────────────────────────

@Composable
fun PremiumStatusBar(s: PremiumEditorState, onEncodingClick: () -> Unit, onLineEndingClick: () -> Unit, group: Int = 0) {
    val c = s.colors
    val bg = c.statusBar
    Column {
        Divider(color = c.border)
        Row(
            Modifier.fillMaxWidth().height(24.dp).background(bg).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left side
            StatusChip(s.encoding.label, Color.White, clickable = true, onClick = onEncodingClick)
            StatusChip(s.lineEnding.label, Color.White.copy(0.8f), clickable = true, onClick = onLineEndingClick)
            StatusChip(s.fileExt.uppercase(), Color.White.copy(0.7f))
            if (s.isReadOnly) StatusChip("🔒 Read-only", c.gold)

            Spacer(Modifier.weight(1f))

            // Right side
            if (s.selCount > 0) StatusChip("${s.selCount} selected", Color.White.copy(0.9f))
            StatusChip("Ln ${s.cursorLine}, Col ${s.cursorCol}", Color.White)
            StatusChip("${s.wordCount}w ${s.charCount}c", Color.White.copy(0.8f))
            StatusChip("${(s.zoom * 100).toInt()}%", Color.White.copy(0.7f))
            StatusChip("PROBLEMS", Color.White.copy(0.78f), clickable = true, onClick = {
                s.bottomPanel = io.github.norbertweb.bluebird.editor.core.BottomPanel.PROBLEMS
                s.showBottomPanel = true
            })
            if (s.isModified) StatusChip("● Unsaved", c.gold)
            else StatusChip("✓ Saved", Color(0xFF90EE90))
        }
    }
}

private val PremiumEditorState.fileExt get() = fileName.substringAfterLast('.', "txt").lowercase()

@Composable
fun StatusChip(label: String, color: Color, clickable: Boolean = false, onClick: () -> Unit = {}) {
    Text(
        label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium,
        modifier = if (clickable) Modifier.clickable(onClick = onClick) else Modifier
    )
}

// ─────────────────────────────────────────────────────────────────
// Command Palette
// ─────────────────────────────────────────────────────────────────

@Composable
fun CommandPalette(s: PremiumEditorState) {
    val c = s.colors
    var query by remember { mutableStateOf("") }
    val focusReq = remember { FocusRequester() }

    val filtered = remember(query, s.allCommands) {
        if (query.isBlank()) s.allCommands
        else s.allCommands.filter { it.label.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { runCatching { focusReq.requestFocus() } }

    AlertDialog(
        onDismissRequest = { s.showCommandPalette = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = null,
        text = {
            Column {
                // Search field
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(c.surfaceHover)
                        .border(1.dp, c.accent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(FluentIcon.Search, null, tint = c.accent, modifier = Modifier.size(16.dp))
                    BasicTextField(
                        query, { query = it },
                        textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                        cursorBrush = SolidColor(c.accent),
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusReq),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) Text("Search commands…", color = c.textMuted, fontSize = 14.sp)
                                inner()
                            }
                        }
                    )
                    Text("${filtered.size} commands", color = c.textMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))

                // Results
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    var lastCat = ""
                    filtered.forEach { cmd ->
                        if (cmd.category != lastCat) {
                            lastCat = cmd.category
                            item {
                                Text(
                                    cmd.category.uppercase(), color = c.accent,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { cmd.action(); s.showCommandPalette = false }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cmd.label, color = c.text, fontSize = 13.sp)
                                if (cmd.shortcut.isNotEmpty()) {
                                    Text(cmd.shortcut, color = c.textMuted, fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.background(c.surfaceHover, RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// ─────────────────────────────────────────────────────────────────
// Autocomplete Popup
// ─────────────────────────────────────────────────────────────────

@Composable
fun AutocompletePopup(s: PremiumEditorState) {
    val c = s.colors
    if (!s.showAutocomplete || s.autocompleteSuggestions.isEmpty()) return
    Column(
        Modifier.background(c.surface, RoundedCornerShape(6.dp))
            .border(1.dp, c.border, RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        s.autocompleteSuggestions.take(6).forEach { word ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    .clickable { s.acceptSuggestion(word) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(FluentIcon.TextFields, null, tint = c.textMuted, modifier = Modifier.size(12.dp))
                Text(word, color = c.text, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Bookmarks Panel
// ─────────────────────────────────────────────────────────────────

@Composable
fun BookmarksPanel(s: PremiumEditorState) {
    val c = s.colors
    AlertDialog(
        onDismissRequest = { s.showBookmarksPanel = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.Bookmark, null, tint = c.gold, modifier = Modifier.size(18.dp))
                Text("Bookmarks", color = c.text, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            if (s.bookmarks.isEmpty()) {
                Text("No bookmarks. Press Ctrl+B on any line.", color = c.textSecondary, fontSize = 13.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(s.bookmarks.sortedBy { it.line }) { _, bm ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .clickable { s.goToLine(bm.line); s.showBookmarksPanel = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.size(8.dp).background(Color(bm.color), CircleShape))
                            Text("Line ${bm.line}", color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(60.dp))
                            Text(bm.label.ifEmpty { "—" }, color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Icon(FluentIcon.Close, null, tint = c.textMuted,
                                modifier = Modifier.size(14.dp).clickable { s.toggleBookmark(bm.line) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { s.showBookmarksPanel = false }) { Text("Done", color = c.accent) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Statistics Panel
// ─────────────────────────────────────────────────────────────────

@Composable
fun StatisticsPanel(s: PremiumEditorState) {
    val c = s.colors
    val stats = s.stats

    AlertDialog(
        onDismissRequest = { s.showStatsPanel = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.Analytics, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("Document Statistics", color = c.text, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatRow("Lines", "${stats.lines}", c)
                StatRow("Words", "${stats.words}", c)
                StatRow("Characters", "${stats.chars}", c)
                StatRow("Chars (no spaces)", "${stats.charsNoSpaces}", c)
                StatRow("Paragraphs", "${stats.paragraphs}", c)
                StatRow("Longest line", "${stats.longestLine} chars", c)
                StatRow("Avg line length", String.format("%.1f chars", stats.avgLineLength), c)
                StatRow("Reading time", String.format("%.1f min", stats.readingTimeMin), c)
                if (stats.selectedChars > 0) {
                    Divider(color = c.border)
                    Text("Selection", color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    StatRow("Selected chars", "${stats.selectedChars}", c)
                    StatRow("Selected words", "${stats.selectedWords}", c)
                    StatRow("Selected lines", "${stats.selectedLines}", c)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { s.showStatsPanel = false }) { Text("Close", color = c.accent) }
        }
    )
}

@Composable
private fun StatRow(label: String, value: String, c: EditorColors) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = c.textSecondary, fontSize = 13.sp)
        Text(value, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

// ─────────────────────────────────────────────────────────────────
// Settings Panel
// ─────────────────────────────────────────────────────────────────

@Composable
fun SettingsPanel(s: PremiumEditorState) {
    val c = s.colors
    AlertDialog(
        onDismissRequest = { s.showSettingsPanel = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.Settings, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("Editor Settings", color = c.text, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item { SettingSection("Editing", c) }
                item { SettingToggle("Auto Indent", s.settings.autoIndent, c) { s.updateSettings { copy(autoIndent = !autoIndent) } } }
                item { SettingToggle("Auto Close Brackets", s.settings.autoCloseBrackets, c) { s.updateSettings { copy(autoCloseBrackets = !autoCloseBrackets) } } }
                item { SettingToggle("Bracket Matching", s.settings.bracketMatching, c) { s.updateSettings { copy(bracketMatching = !bracketMatching) } } }
                item { SettingToggle("Snippets", s.settings.snippetsEnabled, c) { s.updateSettings { copy(snippetsEnabled = !snippetsEnabled) } } }
                item { SettingToggle("Trim Trailing Whitespace on Save", s.settings.trimTrailingWhitespace, c) { s.updateSettings { copy(trimTrailingWhitespace = !trimTrailingWhitespace) } } }
                item { SettingToggle("Insert Final Newline on Save", s.settings.insertFinalNewline, c) { s.updateSettings { copy(insertFinalNewline = !insertFinalNewline) } } }

                item { SettingSection("Display", c) }
                item { SettingToggle("Syntax Highlighting", s.settings.syntaxHighlight, c) { s.updateSettings { copy(syntaxHighlight = !syntaxHighlight) } } }
                item { SettingToggle("Word Wrap", s.settings.wordWrap, c) { s.updateSettings { copy(wordWrap = !wordWrap) } } }
                item { SettingToggle("Line Numbers", s.settings.showLineNumbers, c) { s.updateSettings { copy(showLineNumbers = !showLineNumbers) } } }
                item { SettingToggle("Minimap", s.settings.showMinimap, c) { s.updateSettings { copy(showMinimap = !showMinimap) } } }
                item { SettingToggle("Breadcrumb", s.settings.showBreadcrumb, c) { s.updateSettings { copy(showBreadcrumb = !showBreadcrumb) } } }
                item { SettingToggle("Highlight Current Line", s.settings.highlightCurrentLine, c) { s.updateSettings { copy(highlightCurrentLine = !highlightCurrentLine) } } }
                item { SettingToggle("Show Whitespace", s.settings.showWhitespace, c) { s.updateSettings { copy(showWhitespace = !showWhitespace) } } }
                item { SettingToggle("Column Guide (80)", s.settings.showColumnGuide, c) { s.updateSettings { copy(showColumnGuide = !showColumnGuide) } } }

                item { SettingSection("Autosave", c) }
                item { SettingToggle("Enable Autosave", s.settings.autosaveEnabled, c) { s.updateSettings { copy(autosaveEnabled = !autosaveEnabled) } } }
                item { SettingToggle("Git Gutter", s.settings.showGitGutter, c) { s.updateSettings { copy(showGitGutter = !showGitGutter) } } }
            }
        },
        confirmButton = {
            TextButton(onClick = { s.showSettingsPanel = false }) { Text("Done", color = c.accent) }
        }
    )
}

@Composable
private fun SettingSection(label: String, c: EditorColors) {
    Text(label.uppercase(), color = c.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun SettingToggle(label: String, value: Boolean, c: EditorColors, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = c.text, fontSize = 13.sp)
        Switch(checked = value, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = c.accent))
    }
}

// ─────────────────────────────────────────────────────────────────
// Find Results Panel
// ─────────────────────────────────────────────────────────────────

@Composable
fun FindResultsPanel(s: PremiumEditorState) {
    val c = s.colors
    val matches = remember(s.findQuery, s.content.text, s.matchCase, s.useRegex, s.wholeWord) { s.findMatches() }

    AlertDialog(
        onDismissRequest = { s.showFindResultsPanel = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.Search, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("${matches.size} Results for \"${s.findQuery}\"", color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 500.dp)) {
                itemsIndexed(matches) { idx, result ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (idx == s.currentMatchIndex) c.accent.copy(0.12f) else Color.Transparent)
                            .clickable {
                                s.currentMatchIndex = idx
                                s.updateTab { copy(content = content.copy(selection = TextRange(result.range.first, result.range.last + 1))) }
                                s.showFindResultsPanel = false
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${result.lineNumber}", color = c.accent, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp))
                        Text(result.lineText, color = c.textSecondary, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { s.showFindResultsPanel = false }) { Text("Close", color = c.accent) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Snippet Manager
// ─────────────────────────────────────────────────────────────────

@Composable
fun SnippetManager(s: PremiumEditorState) {
    val c = s.colors
    val allSnippets = DEFAULT_SNIPPETS + s.settings.customSnippets

    AlertDialog(
        onDismissRequest = { s.showSnippetManager = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.Extension, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("Snippets", color = c.text, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 500.dp)) {
                itemsIndexed(allSnippets) { _, snip ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .clickable { s.insertSnippet(snip.body); s.showSnippetManager = false }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(snip.trigger, color = c.accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.background(c.surfaceHover, RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
                            Text(snip.description, color = c.text, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            Text(if (snip.language == "*") "all" else snip.language, color = c.textMuted, fontSize = 10.sp)
                        }
                        Text(snip.body.take(60) + if (snip.body.length > 60) "…" else "",
                            color = c.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { s.showSnippetManager = false }) { Text("Close", color = c.accent) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Dialogs: Save As, Go To Line, Encoding, Line Ending, Unsaved
// ─────────────────────────────────────────────────────────────────

@Composable
fun SaveAsDialog(s: PremiumEditorState, onConfirm: (String) -> Unit) {
    val c = s.colors
    var name by remember { mutableStateOf(s.fileName) }
    AlertDialog(
        onDismissRequest = { s.showSaveAsDialog = false },
        containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = { DialogTitle(FluentIcon.SaveAs, "Save As", c) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("File will be saved to the current directory.", color = c.textSecondary, fontSize = 12.sp)
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("File name") }, singleLine = true,
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                Text("Tip: include extension, e.g. notes.kt", color = c.textMuted, fontSize = 10.sp)
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                shape = RoundedCornerShape(6.dp)) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = { s.showSaveAsDialog = false }, shape = RoundedCornerShape(6.dp)) { Text("Cancel", color = c.text) }
        }
    )
}

@Composable
fun UnsavedChangesDialog(s: PremiumEditorState, onSave: () -> Unit) {
    val c = s.colors
    AlertDialog(
        onDismissRequest = { s.showUnsavedDialog = false; s.pendingCloseTabIndex = -1 },
        containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = { DialogTitle(FluentIcon.Warning, "Unsaved Changes", c) },
        text = { Text("\"${s.tabs.getOrNull(s.pendingCloseTabIndex)?.fileName ?: ""}\" has unsaved changes. Save before closing?", color = c.text) },
        confirmButton = {
            Button(onClick = { onSave(); s.showUnsavedDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                shape = RoundedCornerShape(6.dp)) { Text("Save & Close") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    s.forceCloseTab(s.pendingCloseTabIndex)
                    s.pendingCloseTabIndex = -1; s.showUnsavedDialog = false
                }, shape = RoundedCornerShape(6.dp)) { Text("Discard", color = c.danger) }
                OutlinedButton(onClick = { s.showUnsavedDialog = false; s.pendingCloseTabIndex = -1 },
                    shape = RoundedCornerShape(6.dp)) { Text("Cancel", color = c.text) }
            }
        }
    )
}

@Composable
fun GoToLineDialog(s: PremiumEditorState) {
    val c = s.colors
    var input by remember { mutableStateOf("") }
    val lineNum = input.toIntOrNull()
    val isValid = lineNum != null && lineNum in 1..s.lineCount
    AlertDialog(
        onDismissRequest = { s.showGoToLineDialog = false },
        containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = { DialogTitle(FluentIcon.VerticalAlignCenter, "Go to Line", c) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Enter a line number (1–${s.lineCount}):", color = c.textSecondary, fontSize = 12.sp)
                OutlinedTextField(value = input, onValueChange = { input = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Line") }, singleLine = true, isError = input.isNotEmpty() && !isValid,
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (isValid) { s.goToLine(lineNum!!); s.showGoToLineDialog = false } },
                enabled = isValid, colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                shape = RoundedCornerShape(6.dp)) { Text("Go") }
        },
        dismissButton = {
            OutlinedButton(onClick = { s.showGoToLineDialog = false }, shape = RoundedCornerShape(6.dp)) { Text("Cancel", color = c.text) }
        }
    )
}

@Composable
fun EncodingPickerDialog(s: PremiumEditorState) {
    val c = s.colors
    var selected by remember { mutableStateOf(s.encoding) }
    AlertDialog(
        onDismissRequest = { s.showEncodingPicker = false },
        containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = { DialogTitle(FluentIcon.Code, "File Encoding", c) },
        text = {
            LazyColumn(Modifier.heightIn(max = 350.dp)) {
                itemsIndexed(FileEncoding.values().toList()) { _, enc ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (selected == enc) c.accent.copy(0.12f) else Color.Transparent)
                            .clickable { selected = enc }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = selected == enc, onClick = { selected = enc },
                            colors = RadioButtonDefaults.colors(selectedColor = c.accent))
                        Text(enc.label, color = c.text, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { s.setEncoding(selected); s.showEncodingPicker = false; s.toast("Encoding: ${selected.label}") },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent), shape = RoundedCornerShape(6.dp)) { Text("Apply") }
        },
        dismissButton = {
            OutlinedButton(onClick = { s.showEncodingPicker = false }, shape = RoundedCornerShape(6.dp)) { Text("Cancel", color = c.text) }
        }
    )
}

@Composable
fun LineEndingDialog(s: PremiumEditorState) {
    val c = s.colors
    var selected by remember { mutableStateOf(s.lineEnding) }
    AlertDialog(
        onDismissRequest = { s.showLineEndingPicker = false },
        containerColor = c.surface, shape = RoundedCornerShape(12.dp),
        title = { DialogTitle(FluentIcon.Code, "Line Endings", c) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LineEnding.values().forEach { le ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(if (selected == le) c.accent.copy(0.12f) else Color.Transparent)
                            .clickable { selected = le }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = selected == le, onClick = { selected = le },
                            colors = RadioButtonDefaults.colors(selectedColor = c.accent))
                        Column {
                            Text(le.label, color = c.text, fontSize = 13.sp)
                            Text("\"${le.chars.replace("\r", "\\r").replace("\n", "\\n")}\"", color = c.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { s.setLineEnding(selected); s.showLineEndingPicker = false; s.toast("Line ending: ${selected.label}") },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent), shape = RoundedCornerShape(6.dp)) { Text("Apply") }
        },
        dismissButton = {
            OutlinedButton(onClick = { s.showLineEndingPicker = false }, shape = RoundedCornerShape(6.dp)) { Text("Cancel", color = c.text) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// Toast / Notification
// ─────────────────────────────────────────────────────────────────

@Composable
fun EditorToast(s: PremiumEditorState) {
    val c = s.colors
    val msg = s.toastMsg ?: return
    Box(Modifier.fillMaxSize().padding(bottom = 36.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF323232), shadowElevation = 12.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (s.toastIsError) FluentIcon.Error else FluentIcon.CheckCircle,
                    null, tint = if (s.toastIsError) c.danger else c.success,
                    modifier = Modifier.size(16.dp)
                )
                Text(msg, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Shared Utilities
// ─────────────────────────────────────────────────────────────────

@Composable
fun EdIconBtn(
    icon: ImageVector, desc: String, tint: Color,
    enabled: Boolean = true, onClick: () -> Unit
) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun DialogTitle(icon: ImageVector, label: String, c: EditorColors) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(18.dp))
        Text(label, color = c.text, fontWeight = FontWeight.SemiBold)
    }
}

private val Modifier.alpha: (Float) -> Modifier get() = { a -> this.then(Modifier.graphicsLayer { alpha = a }) }

// ─────────────────────────────────────────────────────────────────
// Quick Open / Symbol navigation — Phase 2
// ─────────────────────────────────────────────────────────────────

@Composable
fun QuickOpenDialog(s: PremiumEditorState) {
    val c = s.colors
    var query by remember { mutableStateOf("") }
    val focusReq = remember { FocusRequester() }
    val group = s.activeEditorGroup
    val candidates = remember(s.tabs, s.settings.recentFiles, query, group) {
        val open = s.tabsForGroup(group).map { it.filePath.ifEmpty { it.fileName } to it.fileName }
        val recent = s.settings.recentFiles.map { it to java.io.File(it).name }
        (open + recent).distinctBy { it.first }
            .filter { query.isBlank() || it.second.contains(query, true) || it.first.contains(query, true) }
            .take(60)
    }
    LaunchedEffect(Unit) { runCatching { focusReq.requestFocus() } }
    AlertDialog(
        onDismissRequest = { s.showQuickOpen = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.Search, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("Quick Open", color = c.text, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth().background(c.surfaceHover, RoundedCornerShape(7.dp))
                        .border(1.dp, c.border, RoundedCornerShape(7.dp)).padding(10.dp).focusRequester(focusReq),
                    decorationBox = { inner -> Box { if (query.isEmpty()) Text("Type a file name or path…", color = c.textMuted, fontSize = 14.sp); inner() } }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    itemsIndexed(candidates) { _, (path, name) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                                val open = s.tabs.indexOfFirst { it.filePath == path }
                                if (open >= 0) s.selectTabIdInGroup(group, s.tabs[open].id)
                                else if (path.isNotEmpty()) { s.toast("Open this file from Explorer: $path") }
                                s.showQuickOpen = false
                            }.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(FluentIcon.Code, null, tint = c.textMuted, modifier = Modifier.size(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, color = c.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (path.isNotEmpty()) Text(path, color = c.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun SymbolPickerDialog(s: PremiumEditorState) {
    val c = s.colors
    val tab = s.activeTab
    var query by remember { mutableStateOf("") }
    val symbols = remember(tab.id, tab.content.text, tab.fileName) { extractSymbolsForPicker(tab.content.text, tab.fileName) }
    val filtered = remember(query, symbols) { symbols.filter { query.isBlank() || it.name.contains(query, true) || it.kind.contains(query, true) } }
    AlertDialog(
        onDismissRequest = { s.showSymbolPicker = false },
        containerColor = c.surface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FluentIcon.Code, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("Go to Symbol", color = c.text, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth().background(c.surfaceHover, RoundedCornerShape(7.dp))
                        .border(1.dp, c.border, RoundedCornerShape(7.dp)).padding(10.dp),
                    decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search symbols…", color = c.textMuted, fontSize = 14.sp); inner() } }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    itemsIndexed(filtered) { _, symbol ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                                s.goToLine(symbol.line)
                                s.showSymbolPicker = false
                            }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(FluentIcon.Code, null, tint = c.accent, modifier = Modifier.size(14.dp))
                            Text(symbol.name, color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${symbol.kind}  ${symbol.line}", color = c.textMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private data class PickerSymbol(val name: String, val kind: String, val line: Int)

private fun extractSymbolsForPicker(text: String, fileName: String): List<PickerSymbol> {
    val result = mutableListOf<PickerSymbol>()
    val patterns = listOf(
        Regex("\\b(?:fun|function)\\s+([A-Za-z_][A-Za-z0-9_]*)"),
        Regex("\\b(?:class|interface|object|struct|enum)\\s+([A-Za-z_][A-Za-z0-9_]*)"),
        Regex("\\b(?:val|var|const|let)\\s+([A-Za-z_][A-Za-z0-9_]*)"),
        Regex("(?:def|async\\s+def)\\s+([A-Za-z_][A-Za-z0-9_]*)")
    )
    text.lineSequence().forEachIndexed { index, line ->
        patterns.forEachIndexed { patternIndex, regex ->
            regex.find(line)?.let { match ->
                val kind = when (patternIndex) { 0, 3 -> "function"; 1 -> "type"; else -> "symbol" }
                result += PickerSymbol(match.groupValues[1], kind, index + 1)
            }
        }
    }
    return result.distinctBy { "${it.line}:${it.name}" }.take(500)
}

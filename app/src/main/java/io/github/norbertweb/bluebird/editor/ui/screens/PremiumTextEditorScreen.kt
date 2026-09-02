package io.github.norbertweb.bluebird.editor.ui.screens

import android.content.Intent
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.norbertweb.bluebird.editor.core.EditorSettings
import io.github.norbertweb.bluebird.editor.editor.core.PremiumEditorState
import io.github.norbertweb.bluebird.editor.editor.highlighting.findMatchingBracket
import io.github.norbertweb.bluebird.editor.ui.components.ReferencesDialog
import io.github.norbertweb.bluebird.editor.ui.components.WorkspaceSearchDialog
import io.github.norbertweb.bluebird.editor.ui.components.WorkspaceSymbolsDialog
import io.github.norbertweb.bluebird.editor.ui.components.RenameSymbolDialog
import io.github.norbertweb.bluebird.editor.ui.components.AutocompletePopup
import io.github.norbertweb.bluebird.editor.ui.components.BookmarksPanel
import io.github.norbertweb.bluebird.editor.ui.components.BreadcrumbBar
import io.github.norbertweb.bluebird.editor.ui.components.CommandPalette
import io.github.norbertweb.bluebird.editor.ui.components.LanguageHoverDialog
import io.github.norbertweb.bluebird.editor.ui.components.EditorToast
import io.github.norbertweb.bluebird.editor.ui.components.EncodingPickerDialog
import io.github.norbertweb.bluebird.editor.ui.components.FindResultsPanel
import io.github.norbertweb.bluebird.editor.ui.components.IdeShell
import io.github.norbertweb.bluebird.editor.ui.components.GoToLineDialog
import io.github.norbertweb.bluebird.editor.ui.components.LineEndingDialog
import io.github.norbertweb.bluebird.editor.ui.components.MinimapPanel
import io.github.norbertweb.bluebird.editor.ui.components.LivePreviewPane
import io.github.norbertweb.bluebird.editor.ui.components.PremiumFindBar
import io.github.norbertweb.bluebird.editor.ui.components.PremiumGutter
import io.github.norbertweb.bluebird.editor.ui.components.VirtualizedCodeSurface
import io.github.norbertweb.bluebird.editor.ui.components.PremiumMenuBar
import io.github.norbertweb.bluebird.editor.ui.components.PremiumStatusBar
import io.github.norbertweb.bluebird.editor.ui.components.QuickOpenDialog
import io.github.norbertweb.bluebird.editor.ui.components.SymbolPickerDialog
import io.github.norbertweb.bluebird.editor.ui.components.PremiumTabBar
import io.github.norbertweb.bluebird.editor.ui.components.SaveAsDialog
import io.github.norbertweb.bluebird.editor.ui.components.SettingsPanel
import io.github.norbertweb.bluebird.editor.ui.components.SnippetManager
import io.github.norbertweb.bluebird.editor.ui.components.StatisticsPanel
import io.github.norbertweb.bluebird.editor.ui.components.UnsavedChangesDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ─────────────────────────────────────────────────────────────────
// PremiumTextEditorScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun PremiumTextEditorScreen(
    @Suppress("UNUSED_PARAMETER") isDark: Boolean = false,

    filePath: String = "",
    initialContent: String = "",
    savedSettings: EditorSettings = EditorSettings(),
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val systemIsDark = isSystemInDarkTheme()

    // State is held in a ViewModel so it survives configuration changes and
    // activity restarts. remember{} alone is wiped on rotation / back-stack return.
    val vm = androidx.lifecycle.viewmodel.compose.viewModel<EditorViewModel>(
        key = filePath.ifEmpty { "untitled" },
        factory = EditorViewModelFactory(filePath, initialContent, savedSettings, systemIsDark),
    )
    val s = vm.state

    LaunchedEffect(systemIsDark) {
        s.setSystemTheme(systemIsDark)
    }

    val c = s.colors

    // ── File picker launcher ──────────────────────────────────────
    val openFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        val text = context.contentResolver.openInputStream(selectedUri)?.use {
                            it.bufferedReader().readText()
                        } ?: throw IllegalStateException("Empty file")
                        val path = selectedUri.path ?: selectedUri.toString()
                        val name = selectedUri.lastPathSegment ?: "opened_file.txt"
                        Triple(name, path, text)
                    }.getOrElse { error -> error }
                }
                when (result) {
                    is Triple<*, *, *> -> {
                        val name = result.first as String
                        val path = result.second as String
                        val text = result.third as String
                        s.newTab(path, text)
                        s.toast("Opened $name")
                    }
                    is Throwable -> s.toast("Open failed: ${result.message}", error = true)
                }
            }
        }
    }

    // ── Autosave timer ────────────────────────────────────────────
    LaunchedEffect(s.isModified, s.settings.autosaveEnabled, s.settings.autosaveIntervalMs) {
        while (isActive) {
            delay(s.settings.autosaveIntervalMs.coerceAtLeast(1000L))
            if (s.isModified && s.settings.autosaveEnabled) {
                withContext(kotlinx.coroutines.Dispatchers.IO) { s.autosave(context) }
            }
        }
    }

    // ── Workspace definition navigation ─────────────────────────
    LaunchedEffect(s.pendingWorkspaceOpen) {
        val location = s.pendingWorkspaceOpen ?: return@LaunchedEffect
        s.pendingWorkspaceOpen = null
        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { File(location.fileName).readText() }
        }
        result.onSuccess { text ->
            s.newTab(location.fileName, text)
            val opened = s.activeTab
            s.updateTabById(opened.id) { copy(content = content.copy(selection = TextRange(location.offset))) }
        }.onFailure { error -> s.toast("Could not open definition: ${error.message}", error = true) }
    }

    // ── Toast auto-dismiss ────────────────────────────────────────
    LaunchedEffect(s.toastMsg) {
        if (s.toastMsg != null) {
            delay(2500)
            s.toastMsg = null
        }
    }

    // ── Back handler ──────────────────────────────────────────────
    BackHandler {
        if (s.showCommandPalette) { s.showCommandPalette = false; return@BackHandler }
        if (s.showFindBar) { s.showFindBar = false; return@BackHandler }
        if (s.isModified) { s.showUnsavedDialog = true; s.pendingCloseTabIndex = -1 }
        else onBack()
    }

    // ─────────────────────────────────────────────────────────────
    // Phase 1 — Professional Fluent IDE shell
    Box(Modifier.fillMaxSize().background(c.bg)) {
        IdeShell(
            s = s,
            onOpenFile = { openFileLauncher.launch(arrayOf("*/*")) },
            onOpenWorkspacePath = { path ->
                scope.launch {
                    val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { File(path).readText() }
                    }
                    result.onSuccess { text -> s.newTab(path, text); s.toast("Opened ${File(path).name}") }
                        .onFailure { error -> s.toast("Open failed: ${error.message}", error = true) }
                }
            },
            onSave = { s.saveToFile(context) },
            onNewTab = { s.newTab() },
        ) { group ->
            EditorGroupContent(
                s = s,
                group = group,
                context = context,
                clipboard = clipboard,
                openFile = { openFileLauncher.launch(arrayOf("*/*")) },
                share = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, s.content.text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                },
                restoreDraft = { s.restoreDraft(context) },
                save = { s.saveToFile(context) },
                newTab = { s.newTab() },
            )
        }

        if (s.showAutocomplete && s.autocompleteSuggestions.isNotEmpty()) {
            Box(Modifier.align(Alignment.BottomStart).padding(start = if (s.showLineNums) 60.dp else 8.dp, bottom = 32.dp)) {
                AutocompletePopup(s)
            }
        }

        EditorToast(s)
    }

        // ── Dialogs ───────────────────────────────────────────────
        if (s.showCommandPalette) CommandPalette(s)
        if (s.showQuickOpen) QuickOpenDialog(s)
        if (s.showWorkspaceSearch) WorkspaceSearchDialog(s)
        if (s.showWorkspaceSymbols) WorkspaceSymbolsDialog(s)
        if (s.showRenameSymbol) RenameSymbolDialog(s)
        if (s.showReferencesPanel) ReferencesDialog(s)
        if (s.languageHover != null) LanguageHoverDialog(s)
        if (s.showSymbolPicker) SymbolPickerDialog(s)
        if (s.showSaveAsDialog) SaveAsDialog(s) { name ->
            val dir = if (s.filePath.isNotEmpty()) File(s.filePath).parent ?: context.filesDir.path
                      else context.filesDir.path
            s.saveAs(context, "$dir/$name")
        }
        if (s.showUnsavedDialog) UnsavedChangesDialog(s, onSave = { s.saveToFile(context) })
        if (s.showGoToLineDialog) GoToLineDialog(s)
        if (s.showEncodingPicker) EncodingPickerDialog(s)
        if (s.showLineEndingPicker) LineEndingDialog(s)
        if (s.showSettingsPanel) SettingsPanel(s)
        if (s.showStatsPanel) StatisticsPanel(s)
        if (s.showBookmarksPanel) BookmarksPanel(s)
        if (s.showSnippetManager) SnippetManager(s)
        if (s.showFindResultsPanel) FindResultsPanel(s)
}

@Composable
private fun EditorGroupContent(
    s: PremiumEditorState,
    group: Int,
    context: Context,
    clipboard: ClipboardManager,
    openFile: () -> Unit,
    share: () -> Unit,
    restoreDraft: () -> Unit,
    save: () -> Unit,
    newTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tab = s.activeTabForGroup(group)
    val c = s.colors
    val fontFamily = remember(s.settings.fontFamily) {
        when (s.settings.fontFamily) {
            "SansSerif" -> FontFamily.SansSerif
            "Serif" -> FontFamily.Serif
            "Courier" -> FontFamily.Cursive
            else -> FontFamily.Monospace
        }
    }
    val effectiveFontSize = (s.fontSize * s.zoom).sp
    val scrollState = rememberScrollState()
    LaunchedEffect(tab.id) {
        scrollState.scrollTo(tab.scrollOffset.coerceAtLeast(0))
    }
    LaunchedEffect(tab.id) {
        snapshotFlow { scrollState.value }.collect { offset ->
            if (s.activeEditorGroup == group && s.tabIdForGroup(group) == tab.id && offset != tab.scrollOffset) {
                s.updateTabById(tab.id) { copy(scrollOffset = offset) }
            }
        }
    }
    var contentHeightPx by remember(s.tabIdForGroup(group)) { mutableStateOf(0) }

    val bracketMatch = remember(tab.content.selection.start, tab.content.text, s.settings.bracketMatching) {
        if (s.settings.bracketMatching) findMatchingBracket(tab.content.text, tab.content.selection.start) else null
    }
    val matches = remember(tab.id, s.findQuery, tab.content.text, s.matchCase, s.useRegex, s.wholeWord) {
        if (s.showFindBar && s.activeEditorGroup == group && s.findQuery.isNotEmpty()) {
            s.findMatchesForTab(tab.id)
        } else emptyList()
    }
    val currentMatchRange = remember(s.currentMatchIndex, matches) { matches.getOrNull(s.currentMatchIndex)?.range }


    Column(modifier.fillMaxSize().background(c.bg)) {
        PremiumTabBar(s, onSave = { s.activateEditorGroup(group); save() }, onNew = { s.activateEditorGroup(group); newTab() }, group = group)
        PremiumMenuBar(
            s,
            group = group,
            onSave = { s.activateEditorGroup(group); save() },
            onShare = { s.activateEditorGroup(group); share() },
            onRestoreDraft = { s.activateEditorGroup(group); restoreDraft() },
            onOpenFile = { s.activateEditorGroup(group); openFile() },
        )
        BreadcrumbBar(s, group = group)
        AnimatedVisibility(s.showFindBar && s.activeEditorGroup == group, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
            PremiumFindBar(s)
        }
        Row(Modifier.weight(1f)) {
            if (s.showLineNums) PremiumGutter(s, scrollState, tab, group)
            if (s.showLivePreview && isWebPreviewSupported(tab.fileName)) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    EditorTextField(
                        s = s, group = group, tab = tab, fontFamily = fontFamily,
                        effectiveFontSize = effectiveFontSize, scrollState = scrollState,
                        bracketMatch = bracketMatch, clipboard = clipboard,
                        onContentHeightMeasured = { contentHeightPx = it },
                    )
                    if (s.settings.showColumnGuide) {
                        val guideOffset = effectiveFontSize.value * s.settings.columnLimit * 0.6f
                        Box(Modifier.fillMaxHeight().width(1.dp).offset(x = guideOffset.dp).background(c.border.copy(alpha = 0.5f)))
                    }
                }
                LivePreviewPane(tab, c, Modifier.weight(0.9f).fillMaxHeight())
            } else {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    EditorTextField(
                        s = s, group = group, tab = tab, fontFamily = fontFamily,
                        effectiveFontSize = effectiveFontSize, scrollState = scrollState,
                        bracketMatch = bracketMatch, clipboard = clipboard,
                        onContentHeightMeasured = { contentHeightPx = it },
                    )
                    if (s.settings.showColumnGuide) {
                        val guideOffset = effectiveFontSize.value * s.settings.columnLimit * 0.6f
                        Box(Modifier.fillMaxHeight().width(1.dp).offset(x = guideOffset.dp).background(c.border.copy(alpha = 0.5f)))
                    }
                }
            }
            if (s.showMinimap && s.settings.showMinimap) MinimapPanel(s, scrollState, contentHeightPx, tab)
        }
        PremiumStatusBar(
            s,
            group = group,
            onEncodingClick = { s.activateEditorGroup(group); s.showEncodingPicker = true },
            onLineEndingClick = { s.activateEditorGroup(group); s.showLineEndingPicker = true },
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Core Editor TextField
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EditorTextField(
    s: PremiumEditorState,
    group: Int,
    tab: io.github.norbertweb.bluebird.editor.core.TabData,
    fontFamily: FontFamily,
    effectiveFontSize: TextUnit,
    scrollState: ScrollState,
    bracketMatch: io.github.norbertweb.bluebird.editor.editor.highlighting.BracketMatch?,
    clipboard: ClipboardManager,
    onContentHeightMeasured: (Int) -> Unit,
) {
    VirtualizedCodeSurface(
        s = s,
        group = group,
        tab = tab,
        fontFamily = fontFamily,
        fontSize = effectiveFontSize,
        scrollState = scrollState,
        bracketMatch = bracketMatch,
        onContentHeightMeasured = onContentHeightMeasured,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SecondaryCursorOverlay(
    selections: List<io.github.norbertweb.bluebird.editor.core.EditorSelection>,
    text: String,
    fontSize: TextUnit,
    color: Color,
) {
    val lineHeightPx = with(LocalDensity.current) { (fontSize * 1.6f).toPx() }
    val charWidthPx = with(LocalDensity.current) { (fontSize * 0.6f).toPx() }
    val paddingPx = with(LocalDensity.current) { 12.dp.toPx() }
    Canvas(Modifier.fillMaxSize()) {
        selections.forEach { selection ->
            val pos = selection.start.coerceIn(0, text.length)
            val before = text.substring(0, pos)
            val line = before.count { it == '\n' }
            val col = before.substringAfterLast('\n').length
            val x = paddingPx + col * charWidthPx
            val y = paddingPx + line * lineHeightPx
            if (!selection.isCaret) {
                drawRect(color.copy(alpha = 0.14f), Offset(x, y), androidx.compose.ui.geometry.Size(
                    (selection.end - selection.start).coerceAtLeast(1) * charWidthPx, lineHeightPx))
            }
            drawRect(color, Offset(x, y), androidx.compose.ui.geometry.Size(1.5f, lineHeightPx))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Current Line Highlight
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CurrentLineHighlight(
    lineNumber: Int,
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily,
    text: String,
) {
    val lineHeightPx = with(LocalDensity.current) { (fontSize * 1.6f).toPx() }
    val paddingTopPx = with(LocalDensity.current) { 12.dp.toPx() }
    val topOffset = paddingTopPx + (lineNumber - 1) * lineHeightPx

    Box(
        Modifier.fillMaxWidth().offset(y = with(LocalDensity.current) { topOffset.toDp() })
            .height(with(LocalDensity.current) { lineHeightPx.toDp() })
            .background(color)
    )
}

// ─────────────────────────────────────────────────────────────────
// Bracket Highlights
// ─────────────────────────────────────────────────────────────────

@Composable
private fun BracketHighlights(
    match: io.github.norbertweb.bluebird.editor.editor.highlighting.BracketMatch,
    text: String,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    color: Color,
) {
    // Simplified: just highlight using Canvas at bracket positions
    val lineHeightPx = with(LocalDensity.current) { (fontSize * 1.6f).toPx() }
    val charWidthPx = with(LocalDensity.current) { (fontSize * 0.6f).toPx() }
    val paddingPx = with(LocalDensity.current) { 12.dp.toPx() }

    fun posToXY(pos: Int): Offset {
        val before = text.substring(0, pos.coerceAtMost(text.length))
        val line = before.count { it == '\n' }
        val col = before.substringAfterLast('\n').length
        return Offset(paddingPx + col * charWidthPx, paddingPx + line * lineHeightPx)
    }

    Canvas(Modifier.fillMaxSize()) {
        if (match.openPos >= 0) {
            val (x, y) = posToXY(match.openPos)
            drawRect(
                color = color.copy(alpha = if (match.isValid) 0.25f else 0.15f),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(charWidthPx, lineHeightPx)
            )
        }
        if (match.closePos >= 0) {
            val (x, y) = posToXY(match.closePos)
            drawRect(
                color = color.copy(alpha = if (match.isValid) 0.25f else 0.15f),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(charWidthPx, lineHeightPx)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Whitespace Visual Transformation
// ─────────────────────────────────────────────────────────────────

class WhitespaceTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder()
        text.text.forEach { ch ->
            when (ch) {
                ' ' -> builder.append('·')
                '\t' -> builder.append('→')
                '\n' -> builder.append('¶')
                else -> builder.append(ch)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

// ─────────────────────────────────────────────────────────────────
// Keyboard Shortcut Handler (called from onKeyEvent)
// ─────────────────────────────────────────────────────────────────

// Note: In Android Compose, hardware keyboard shortcuts are handled
// via Modifier.onKeyEvent or by intercepting KeyEvents in the Activity.
// This function maps key events to editor actions.

fun handleKeyEvent(
    keyCode: Int,
    isCtrl: Boolean, isShift: Boolean, isAlt: Boolean,
    s: PremiumEditorState,
    clipboard: ClipboardManager,
    onSave: () -> Unit,
): Boolean {

    return when {
        isCtrl && !isShift && keyCode == KeyEvent.KEYCODE_S -> { onSave(); true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_S -> { s.showSaveAsDialog = true; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_Z && !isShift -> { s.undo(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_Y -> { s.redo(); true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_Z -> { s.redo(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_F -> { s.showFindBar = true; s.showReplace = false; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_H -> { s.showFindBar = true; s.showReplace = true; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_G -> { s.showGoToLineDialog = true; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_N -> { s.newTab(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_D -> { s.addNextOccurrence(); true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_D -> { s.duplicateCurrentLine(); true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_K -> { s.deleteCurrentLine(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_SLASH -> { s.toggleComment(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_B -> { s.toggleBookmark(s.cursorLine); true }
        keyCode == KeyEvent.KEYCODE_F2 && !isShift && !isCtrl -> { s.prepareRenameSymbol(); true }
        keyCode == KeyEvent.KEYCODE_F2 && isCtrl && !isShift -> { s.nextBookmark(); true }
        keyCode == KeyEvent.KEYCODE_F2 && isCtrl && isShift -> { s.prevBookmark(); true }
        keyCode == KeyEvent.KEYCODE_F5 -> { s.insertDateTime(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_A -> {
            s.updateTab { copy(content = content.copy(selection = TextRange(0, content.text.length))) }; true
        }
        isCtrl && keyCode == KeyEvent.KEYCODE_X -> {
            val cut = s.cutSelectionText()
            clipboard.setText(AnnotatedString(cut))
            true
        }
        isCtrl && keyCode == KeyEvent.KEYCODE_C -> {
            clipboard.setText(AnnotatedString(s.copySelectionText()))
            true
        }
        isCtrl && keyCode == KeyEvent.KEYCODE_V -> {
            val paste = clipboard.getText()?.text ?: return false
            s.pasteText(paste)
            true
        }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_P -> { s.showCommandPalette = true; true }
        isCtrl && !isShift && keyCode == KeyEvent.KEYCODE_P -> { s.showQuickOpen = true; true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_O -> { s.showSymbolPicker = true; true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_LEFT_BRACKET -> { s.toggleFoldAtCursor(); true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_RIGHT_BRACKET -> { s.unfoldAll(); true }
        isCtrl && isAlt && keyCode == KeyEvent.KEYCODE_LEFT_BRACKET -> { s.foldAll(); true }
        isAlt && isShift && keyCode == KeyEvent.KEYCODE_DPAD_UP -> { s.addCaretOnAdjacentLine(-1); true }
        isAlt && isShift && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> { s.addCaretOnAdjacentLine(1); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_EQUALS -> { s.updateSettings { copy(zoom = (zoom + 0.1f).coerceAtMost(4f)) }; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_MINUS -> { s.updateSettings { copy(zoom = (zoom - 0.1f).coerceAtLeast(0.25f)) }; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_0 -> { s.updateSettings { copy(zoom = 1f) }; true }
        isAlt && keyCode == KeyEvent.KEYCODE_DPAD_UP -> { s.moveCurrentLineUp(); true }
        isAlt && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> { s.moveCurrentLineDown(); true }
        keyCode == KeyEvent.KEYCODE_TAB -> { s.handleTabKey(isShift); true }
        keyCode == KeyEvent.KEYCODE_ENTER -> { s.handleEnterKey(); true }
        keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> { s.moveLeft(isShift, isCtrl || isAlt); true }
        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> { s.moveRight(isShift, isCtrl || isAlt); true }
        keyCode == KeyEvent.KEYCODE_MOVE_HOME -> { s.moveHome(isShift, isCtrl); true }
        keyCode == KeyEvent.KEYCODE_MOVE_END -> { s.moveEnd(isShift, isCtrl); true }
        keyCode == KeyEvent.KEYCODE_PAGE_UP -> { s.moveByPage(-1, isShift); true }
        keyCode == KeyEvent.KEYCODE_PAGE_DOWN -> { s.moveByPage(1, isShift); true }
        keyCode == KeyEvent.KEYCODE_DEL -> { s.deleteBackward(isCtrl || isAlt); true }
        keyCode == KeyEvent.KEYCODE_FORWARD_DEL -> { s.deleteForward(isCtrl || isAlt); true }
        keyCode == KeyEvent.KEYCODE_ESCAPE -> {
            when {
                s.showCommandPalette -> s.showCommandPalette = false
                s.showQuickOpen -> s.showQuickOpen = false
                s.showSymbolPicker -> s.showSymbolPicker = false
                s.showFindBar -> s.showFindBar = false
                else -> return false
            }
            true
        }
        else -> false
    }
}

// ─────────────────────────────────────────────────────────────────
// Extension helpers used in screen
// ─────────────────────────────────────────────────────────────────

private fun TextFieldValue.hasSelection() = selection.length > 0
private fun TextFieldValue.selectedText() = text.substring(selection.start, selection.end)
private val PremiumEditorState.fileExt get() = fileName.substringAfterLast('.', "txt").lowercase()

// ─────────────────────────────────────────────────────────────────
// EditorViewModel — survives configuration changes & activity restarts
// ─────────────────────────────────────────────────────────────────

class EditorViewModel(
    filePath: String,
    initialContent: String,
    savedSettings: EditorSettings,
    initialIsDark: Boolean,
) : androidx.lifecycle.ViewModel() {
    val state = PremiumEditorState(
        initialPath = filePath,
        initialContent = initialContent,
        savedSettings = savedSettings,
        initialIsDark = initialIsDark,
    )
}

class EditorViewModelFactory(
    private val filePath: String,
    private val initialContent: String,
    private val savedSettings: EditorSettings,
    private val initialIsDark: Boolean,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return EditorViewModel(filePath, initialContent, savedSettings, initialIsDark) as T
    }
}

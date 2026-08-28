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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.github.norbertweb.bluebird.editor.editor.highlighting.buildSyntaxHighlight
import io.github.norbertweb.bluebird.editor.editor.highlighting.findMatchingBracket
import io.github.norbertweb.bluebird.editor.ui.components.AutocompletePopup
import io.github.norbertweb.bluebird.editor.ui.components.BookmarksPanel
import io.github.norbertweb.bluebird.editor.ui.components.BreadcrumbBar
import io.github.norbertweb.bluebird.editor.ui.components.CommandPalette
import io.github.norbertweb.bluebird.editor.ui.components.EditorToast
import io.github.norbertweb.bluebird.editor.ui.components.EncodingPickerDialog
import io.github.norbertweb.bluebird.editor.ui.components.FindResultsPanel
import io.github.norbertweb.bluebird.editor.ui.components.GoToLineDialog
import io.github.norbertweb.bluebird.editor.ui.components.LineEndingDialog
import io.github.norbertweb.bluebird.editor.ui.components.MinimapPanel
import io.github.norbertweb.bluebird.editor.ui.components.PremiumFindBar
import io.github.norbertweb.bluebird.editor.ui.components.PremiumGutter
import io.github.norbertweb.bluebird.editor.ui.components.PremiumMenuBar
import io.github.norbertweb.bluebird.editor.ui.components.PremiumStatusBar
import io.github.norbertweb.bluebird.editor.ui.components.PremiumTabBar
import io.github.norbertweb.bluebird.editor.ui.components.SaveAsDialog
import io.github.norbertweb.bluebird.editor.ui.components.SettingsPanel
import io.github.norbertweb.bluebird.editor.ui.components.SnippetManager
import io.github.norbertweb.bluebird.editor.ui.components.StatisticsPanel
import io.github.norbertweb.bluebird.editor.ui.components.ThemePickerDialog
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
    isDark: Boolean,

    filePath: String = "",
    initialContent: String = "",
    savedSettings: EditorSettings = EditorSettings(),
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // State is held in a ViewModel so it survives configuration changes and
    // activity restarts. remember{} alone is wiped on rotation / back-stack return.
    val vm = androidx.lifecycle.viewmodel.compose.viewModel<EditorViewModel>(
        key = filePath.ifEmpty { "untitled" },
        factory = EditorViewModelFactory(filePath, initialContent, savedSettings),
    )
    val s = vm.state

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

    // ── Toast auto-dismiss ────────────────────────────────────────
    LaunchedEffect(s.toastMsg) {
        if (s.toastMsg != null) {
            delay(2500)
            s.toastMsg = null
        }
    }

    // ── Bracket match ─────────────────────────────────────────────
    val bracketMatch = remember(s.content.selection.start, s.content.text, s.settings.bracketMatching) {
        if (s.settings.bracketMatching) findMatchingBracket(s.content.text, s.content.selection.start)
        else null
    }

    // ── Syntax highlighted text ───────────────────────────────────
    val matches = remember(s.findQuery, s.content.text, s.matchCase, s.useRegex, s.wholeWord) {
        if (s.showFindBar && s.findQuery.isNotEmpty()) s.findMatches() else emptyList()
    }
    val currentMatchRange = remember(s.currentMatchIndex, matches) {
        matches.getOrNull(s.currentMatchIndex)?.range
    }

    val highlightedText = remember(
        s.content.text, s.fileExt, s.settings.theme, s.settings.syntaxHighlight,
        s.findQuery, s.matchCase, s.useRegex, s.wholeWord, s.currentMatchIndex
    ) {
        if (s.settings.syntaxHighlight) {
            buildSyntaxHighlight(
                text = s.content.text,
                ext = s.fileExt,
                colors = c,
                findQuery = if (s.showFindBar) s.findQuery else "",
                matchCase = s.matchCase,
                useRegex = s.useRegex,
                currentMatchRange = currentMatchRange,
                allMatchRanges = matches.map { it.range },
                showWhitespace = s.settings.showWhitespace,
            )
        } else buildAnnotatedString { append(s.content.text) }
    }

    // ── Back handler ──────────────────────────────────────────────
    BackHandler {
        if (s.showCommandPalette) { s.showCommandPalette = false; return@BackHandler }
        if (s.showFindBar) { s.showFindBar = false; return@BackHandler }
        if (s.isModified) { s.showUnsavedDialog = true; s.pendingCloseTabIndex = -1 }
        else onBack()
    }

    // ── Content height for minimap ────────────────────────────────
    var contentHeightPx by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    // ─────────────────────────────────────────────────────────────
    // Root Layout
    // ─────────────────────────────────────────────────────────────
    MaterialTheme(
        colorScheme = if (c.isDark) darkColorScheme(primary = c.accent) else lightColorScheme(primary = c.accent)
    ) {
        Box(Modifier.fillMaxSize().background(c.bg)) {
            Column(Modifier.fillMaxSize()) {

                // ── Tab bar ───────────────────────────────────────
                PremiumTabBar(s, onSave = { s.saveToFile(context) }, onNew = { s.newTab() })

                // ── Menu bar ──────────────────────────────────────
                PremiumMenuBar(
                    s,
                    onSave = { s.saveToFile(context) },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, s.content.text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                    },
                    onRestoreDraft = { s.restoreDraft(context) },
                    onOpenFile = { openFileLauncher.launch(arrayOf("*/*")) },
                )

                // ── Breadcrumb ────────────────────────────────────
                BreadcrumbBar(s)

                // ── Find bar ──────────────────────────────────────
                AnimatedVisibility(s.showFindBar, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                    PremiumFindBar(s)
                }

                // ── Main editor area ──────────────────────────────
                Row(Modifier.weight(1f)) {

                    // Line gutter
                    if (s.showLineNums) {
                        PremiumGutter(s, scrollState)
                    }

                    // Text area
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        EditorTextField(
                            s = s,
                            highlightedText = highlightedText,
                            fontFamily = fontFamily,
                            effectiveFontSize = effectiveFontSize,
                            scrollState = scrollState,
                            bracketMatch = bracketMatch,
                            clipboard = clipboard,
                            onContentHeightMeasured = { contentHeightPx = it },
                        )

                        // Column guide line
                        if (s.settings.showColumnGuide) {
                            val guideOffset = remember(effectiveFontSize) {
                                effectiveFontSize.value * s.settings.columnLimit * 0.6f
                            }
                            Box(
                                Modifier.fillMaxHeight()
                                    .width(1.dp)
                                    .offset(x = guideOffset.dp)
                                    .background(c.border.copy(0.5f))
                            )
                        }
                    }

                    // Minimap
                    if (s.showMinimap && s.settings.showMinimap) {
                        MinimapPanel(s, scrollState, contentHeightPx)
                    }
                }

                // ── Status bar ────────────────────────────────────
                PremiumStatusBar(
                    s,
                    onEncodingClick = { s.showEncodingPicker = true },
                    onLineEndingClick = { s.showLineEndingPicker = true },
                )
            }

            // ── Autocomplete popup ────────────────────────────────
            if (s.showAutocomplete && s.autocompleteSuggestions.isNotEmpty()) {
                Box(Modifier.align(Alignment.BottomStart).padding(start = if (s.showLineNums) 60.dp else 8.dp, bottom = 32.dp)) {
                    AutocompletePopup(s)
                }
            }

            // ── Toast ─────────────────────────────────────────────
            EditorToast(s)
        }

        // ── Dialogs ───────────────────────────────────────────────
        if (s.showCommandPalette) CommandPalette(s)
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
        if (s.showThemePicker) ThemePickerDialog(s)
        if (s.showBookmarksPanel) BookmarksPanel(s)
        if (s.showSnippetManager) SnippetManager(s)
        if (s.showFindResultsPanel) FindResultsPanel(s)
    }
}

// ─────────────────────────────────────────────────────────────────
// Core Editor TextField
// ─────────────────────────────────────────────────────────────────

@Composable
private fun EditorTextField(
    s: PremiumEditorState,
    highlightedText: AnnotatedString,
    fontFamily: FontFamily,
    effectiveFontSize: TextUnit,
    scrollState: ScrollState,
    bracketMatch: io.github.norbertweb.bluebird.editor.editor.highlighting.BracketMatch?,
    clipboard: ClipboardManager,
    onContentHeightMeasured: (Int) -> Unit,
) {
    val c = s.colors
    val cursorLine = s.cursorLine

    Box(
        Modifier.fillMaxSize()
            .background(c.bg)
            .verticalScroll(scrollState)
            .onGloballyPositioned { onContentHeightMeasured(it.size.height) }
    ) {
        // Current line highlight
        if (s.settings.highlightCurrentLine) {
            CurrentLineHighlight(
                lineNumber = cursorLine,
                fontSize = effectiveFontSize,
                color = c.currentLineBg,
                fontFamily = fontFamily,
                text = s.content.text,
            )
        }

        // Bracket match highlights
        if (bracketMatch != null) {
            BracketHighlights(bracketMatch, s.content.text, effectiveFontSize, fontFamily, c.accent)
        }

        BasicTextField(
            value = s.content,
            onValueChange = { newVal ->
                if (!s.isReadOnly) s.updateContent(newVal)
            },
            enabled = !s.isReadOnly,
            visualTransformation = if (s.settings.showWhitespace) WhitespaceTransformation() else VisualTransformation.None,
            textStyle = TextStyle(
                // Use real text color — syntax SpanStyles override per-token.
                // The old Color.Transparent + overlay approach ate all touch events.
                color = Color.Transparent,
                fontSize = effectiveFontSize,
                fontFamily = fontFamily,
                lineHeight = effectiveFontSize * 1.6f,
                letterSpacing = 0.3.sp,
            ),
            cursorBrush = SolidColor(c.accent),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Default,
            ),
            // Syntax highlighting via decorationBox — correct Compose pattern.
            // The highlighted AnnotatedString renders underneath; BasicTextField
            // draws only the cursor and selection highlight on top.
            decorationBox = { innerTextField ->
                Box {
                    Text(
                        text = highlightedText,
                        style = TextStyle(
                            fontSize = effectiveFontSize,
                            fontFamily = fontFamily,
                            lineHeight = effectiveFontSize * 1.6f,
                            letterSpacing = 0.3.sp,
                        ),
                        softWrap = s.settings.wordWrap,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    innerTextField()
                }
            },
        )

        // Read-only watermark
        if (s.isReadOnly) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopEnd) {
                Surface(shape = RoundedCornerShape(4.dp), color = c.gold.copy(0.15f), modifier = Modifier) {
                    Text("  READ ONLY  ", color = c.gold, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
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
        isCtrl && keyCode == KeyEvent.KEYCODE_D -> { s.duplicateCurrentLine(); true }
        isCtrl && isShift && keyCode == KeyEvent.KEYCODE_K -> { s.deleteCurrentLine(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_SLASH -> { s.toggleComment(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_B -> { s.toggleBookmark(s.cursorLine); true }
        keyCode == KeyEvent.KEYCODE_F2 && !isShift -> { s.nextBookmark(); true }
        keyCode == KeyEvent.KEYCODE_F2 && isShift -> { s.prevBookmark(); true }
        keyCode == KeyEvent.KEYCODE_F5 -> { s.insertDateTime(); true }
        isCtrl && keyCode == KeyEvent.KEYCODE_A -> {
            s.updateTab { copy(content = content.copy(selection = TextRange(0, content.text.length))) }; true
        }
        isCtrl && keyCode == KeyEvent.KEYCODE_X -> {
            val cut = s.cutSelection()
            clipboard.setText(AnnotatedString(cut))
            true
        }
        isCtrl && keyCode == KeyEvent.KEYCODE_C -> {
            val selected = if (s.content.selection.length > 0) s.content.selectedText()
                           else s.content.text.split('\n').getOrElse(s.cursorLine - 1) { "" }
            clipboard.setText(AnnotatedString(selected))
            true
        }
        isCtrl && keyCode == KeyEvent.KEYCODE_V -> {
            val paste = clipboard.getText()?.text ?: return false
            val pos = s.content.selection.start
            val (start, end) = if (s.content.hasSelection()) s.content.selection.start to s.content.selection.end else pos to pos
            val newText = s.content.text.substring(0, start) + paste + s.content.text.substring(end)
            s.updateContent(s.content.copy(text = newText, selection = TextRange(start + paste.length)))
            true
        }
        isCtrl && keyCode == KeyEvent.KEYCODE_P -> { s.showCommandPalette = true; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_EQUALS -> { s.updateSettings { copy(zoom = (zoom + 0.1f).coerceAtMost(4f)) }; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_MINUS -> { s.updateSettings { copy(zoom = (zoom - 0.1f).coerceAtLeast(0.25f)) }; true }
        isCtrl && keyCode == KeyEvent.KEYCODE_0 -> { s.updateSettings { copy(zoom = 1f) }; true }
        isAlt && keyCode == KeyEvent.KEYCODE_DPAD_UP -> { s.moveCurrentLineUp(); true }
        isAlt && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> { s.moveCurrentLineDown(); true }
        keyCode == KeyEvent.KEYCODE_TAB -> { s.handleTabKey(isShift); true }
        keyCode == KeyEvent.KEYCODE_ENTER -> { s.handleEnterKey(); true }
        keyCode == KeyEvent.KEYCODE_ESCAPE -> {
            when {
                s.showCommandPalette -> s.showCommandPalette = false
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
) : androidx.lifecycle.ViewModel() {
    val state = PremiumEditorState(
        initialPath = filePath,
        initialContent = initialContent,
        savedSettings = savedSettings,
    )
}

class EditorViewModelFactory(
    private val filePath: String,
    private val initialContent: String,
    private val savedSettings: EditorSettings,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return EditorViewModel(filePath, initialContent, savedSettings) as T
    }
}

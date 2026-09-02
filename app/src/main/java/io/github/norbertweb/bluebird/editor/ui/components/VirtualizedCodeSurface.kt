package io.github.norbertweb.bluebird.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.norbertweb.bluebird.editor.core.TabData
import io.github.norbertweb.bluebird.editor.core.DocumentLineModel
import io.github.norbertweb.bluebird.editor.core.LineIndex
import io.github.norbertweb.bluebird.editor.editor.core.PremiumEditorState
import io.github.norbertweb.bluebird.editor.editor.highlighting.BracketMatch
import io.github.norbertweb.bluebird.editor.highlighting.buildSyntaxHighlight
import io.github.norbertweb.bluebird.editor.ui.theme.EditorColors

/**
 * Line-oriented visible editor surface.
 *
 * A single transparent BasicTextField remains the Android IME/editing bridge,
 * while the visible code is rendered only for the current viewport. This
 * avoids asking Compose to layout a giant AnnotatedString for the whole file.
 */
@Composable
fun VirtualizedCodeSurface(
    s: PremiumEditorState,
    group: Int,
    tab: TabData,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    scrollState: ScrollState,
    bracketMatch: BracketMatch?,
    onContentHeightMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = s.colors
    val density = LocalDensity.current
    val lineHeightPx = with(density) { (fontSize * 1.6f).toPx() }
    val charWidthPx = with(density) { (fontSize * 0.60f).toPx() }
    val horizontalScroll = rememberScrollState()
    val model = remember(tab.content.text) { DocumentLineModel(tab.content.text) }
    val index = remember(tab.content.text) { LineIndex(tab.content.text) }
    val totalLines = model.lineCount.coerceAtLeast(1)
    val totalHeightPx = (lineHeightPx * totalLines + with(density) { 24.dp.toPx() }).toInt()
    val maxLineLength = remember(tab.content.text) {
        tab.content.text.lineSequence().maxOfOrNull { it.length } ?: 0
    }
    val minWidthPx = with(density) { 320.dp.toPx() }
    val contentWidthPx = maxOf(minWidthPx, 24.dp.toPx() + maxLineLength * charWidthPx)
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val viewport = remember(scrollState.value, viewportHeightPx, totalLines, lineHeightPx) {
        model.viewport(scrollState.value, viewportHeightPx, lineHeightPx, overscan = 10)
    }

    LaunchedEffect(totalHeightPx) { onContentHeightMeasured(totalHeightPx) }

    val visibleLines = remember(
        tab.content.text,
        viewport.firstLine,
        viewport.lastLine,
        tab.foldedLines,
        s.findQuery,
        s.showFindBar,
        s.currentMatchIndex,
        tab.id,
        c,
    ) {
        val currentMatch = if (s.showFindBar) s.findMatchesForTab(tab.id).getOrNull(s.currentMatchIndex) else null
        model.visibleLines(viewport.firstLine, viewport.lastLine, tab.foldedLines).map { line ->
            val currentLocal = currentMatch?.let {
                val start = it.range.first - line.startOffset
                val end = it.range.last + 1 - line.startOffset
                if (start in 0..line.text.length && end > start && start < line.text.length) start until end else null
            }
            val annotated = if (s.settings.syntaxHighlight) {
                buildSyntaxHighlight(
                    text = line.text,
                    ext = tab.fileName.substringAfterLast('.', "txt").lowercase(),
                    colors = c,
                    findQuery = if (s.showFindBar) s.findQuery else "",
                    matchCase = s.matchCase,
                    useRegex = s.useRegex,
                    currentMatchRange = currentLocal,
                    showWhitespace = s.settings.showWhitespace,
                )
            } else {
                AnnotatedString(line.text)
            }
            line to annotated
        }
    }

    var dragAnchor by remember(tab.id) { mutableIntStateOf(tab.content.selection.start) }
    var lastTapMillis by remember(tab.id) { mutableIntStateOf(0) }
    var lastTapOffset by remember(tab.id) { mutableIntStateOf(-1) }

    fun offsetAtPosition(position: Offset): Int {
        val pad = with(density) { 12.dp.toPx() }
        val y = position.y.coerceAtLeast(pad)
        val contentVisualIndex = ((scrollState.value + y - pad) / lineHeightPx).toInt()
        val visualIndex = contentVisualIndex - viewport.firstLine
        val line = model.visibleLines(viewport.firstLine, viewport.lastLine, tab.foldedLines)
            .getOrNull(visualIndex.coerceAtLeast(0))?.lineNumber
            ?: index.lineForOffset(tab.content.selection.start)
        val col = ((position.x - pad + horizontalScroll.value) / charWidthPx)
            .toInt().coerceAtLeast(0)
        return index.offsetAt(line, col, tab.content.text)
    }

    androidx.compose.foundation.layout.Box(
        modifier
            .background(c.bg)
            .onGloballyPositioned { viewportHeightPx = it.size.height }
            .verticalScroll(scrollState)
            .horizontalScroll(horizontalScroll)
            .pointerInput(tab.id, viewport.firstLine, viewport.lastLine, tab.foldedLines, horizontalScroll.value) {
                detectTapGestures(
                    onDoubleTap = { position ->
                        val offset = offsetAtPosition(position)
                        s.activateEditorGroup(group)
                        s.selectWordAtOffset(offset)
                        lastTapMillis = 0
                    },
                    onLongPress = { position ->
                        val offset = offsetAtPosition(position)
                        s.activateEditorGroup(group)
                        s.selectLineAtOffset(offset)
                        dragAnchor = offset
                    },
                    onTap = { position ->
                        val now = System.currentTimeMillis().toInt()
                        val offset = offsetAtPosition(position)
                        val isThirdTap = now - lastTapMillis in 1..650 && kotlin.math.abs(offset - lastTapOffset) < 2
                        s.activateEditorGroup(group)
                        if (isThirdTap) {
                            s.selectLineAtOffset(offset)
                            lastTapMillis = 0
                        } else {
                            s.setCursorOffset(offset)
                            lastTapMillis = now
                            lastTapOffset = offset
                        }
                    }
                )
            }
            .pointerInput(tab.id, viewport.firstLine, viewport.lastLine, tab.foldedLines) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        dragAnchor = offsetAtPosition(position)
                        s.activateEditorGroup(group)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val end = offsetAtPosition(change.position)
                        s.setSelectionRange(dragAnchor, end)
                    }
                )
            }
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .then(Modifier.width(with(density) { contentWidthPx.toDp() }))
                .height(with(density) { totalHeightPx.toDp() })
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawBackgroundAndSelections(
                    c = c,
                    tab = tab,
                    index = index,
                    text = tab.content.text,
                    lineHeightPx = lineHeightPx,
                    charWidthPx = charWidthPx,
                    paddingPx = with(density) { 12.dp.toPx() },
                    bracketMatch = bracketMatch,
                )
            }

            androidx.compose.foundation.layout.Column(
                Modifier
                    .matchParentSize()
                    .alpha(0.01f)
            ) {
                BasicTextField(
                    value = tab.content,
                    onValueChange = { newVal ->
                        s.activateEditorGroup(group)
                        if (!tab.isReadOnly) s.updateContentForTab(tab.id, newVal)
                    },
                    enabled = !tab.isReadOnly,
                    textStyle = TextStyle(
                        color = Color.Transparent,
                        fontSize = fontSize,
                        fontFamily = fontFamily,
                        lineHeight = fontSize * 1.6f,
                    ),
                    cursorBrush = SolidColor(Color.Transparent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Default,
                    ),
                    modifier = Modifier
                        .matchParentSize()
                        .onFocusChanged { if (it.isFocused) s.activateEditorGroup(group) }
                    decorationBox = { inner -> inner() },
                )
            }

            val textMeasurer = rememberTextMeasurer()
            val style = TextStyle(
                color = c.text,
                fontSize = fontSize,
                fontFamily = fontFamily,
                lineHeight = fontSize * 1.6f,
            )
            val layouts = remember(visibleLines, style, textMeasurer) {
                visibleLines.map { (line, annotated) ->
                    line to textMeasurer.measure(annotated, style)
                }
            }

            Canvas(Modifier.matchParentSize()) {
                val pad = with(density) { 12.dp.toPx() }
                layouts.forEach { (line, layout) ->
                    val y = pad + (line.lineNumber - 1) * lineHeightPx
                    drawText(layout, topLeft = Offset(pad, y))
                    if (line.isFolded) {
                        drawLine(
                            color = c.accent,
                            start = Offset(pad + layout.size.width + 5f, y + lineHeightPx * 0.55f),
                            end = Offset(pad + layout.size.width + 15f, y + lineHeightPx * 0.55f),
                            strokeWidth = 2f,
                        )
                    }
                }
                drawPrimaryCursorAndSelections(
                    c = c,
                    tab = tab,
                    index = index,
                    text = tab.content.text,
                    lineHeightPx = lineHeightPx,
                    charWidthPx = charWidthPx,
                    paddingPx = pad,
                )
            }
        }
    }
}

private fun DrawScope.drawBackgroundAndSelections(
    c: EditorColors,
    tab: TabData,
    index: LineIndex,
    text: String,
    lineHeightPx: Float,
    charWidthPx: Float,
    paddingPx: Float,
    bracketMatch: BracketMatch?,
) {
    val cursorLine = index.lineForOffset(tab.content.selection.start)
    drawRect(
        color = c.currentLineBg,
        topLeft = Offset(0f, paddingPx + (cursorLine - 1) * lineHeightPx),
        size = androidx.compose.ui.geometry.Size(size.width, lineHeightPx),
    )

    val primary = tab.content.selection
    if (!primary.collapsed) {
        drawSelection(primary.start, primary.end, index, text, lineHeightPx, charWidthPx, paddingPx, c.selectionBg)
    }
    tab.secondarySelections.filterNot { it.isCaret }.forEach {
        drawSelection(it.start, it.end, index, text, lineHeightPx, charWidthPx, paddingPx, c.selectionBg.copy(alpha = 0.12f))
    }

    bracketMatch?.let { match ->
        drawBracket(match.openPos, index, text, lineHeightPx, charWidthPx, paddingPx, c.accentGlow)
        drawBracket(match.closePos, index, text, lineHeightPx, charWidthPx, paddingPx, c.accentGlow)
    }
}

private fun DrawScope.drawPrimaryCursorAndSelections(
    c: EditorColors,
    tab: TabData,
    index: LineIndex,
    text: String,
    lineHeightPx: Float,
    charWidthPx: Float,
    paddingPx: Float,
) {
    fun caret(offset: Int, color: Color, width: Float = 1.8f) {
        val safe = offset.coerceIn(0, text.length)
        val line = index.lineForOffset(safe)
        val col = safe - index.lineStart(line)
        val x = paddingPx + col * charWidthPx
        val y = paddingPx + (line - 1) * lineHeightPx
        drawLine(color, Offset(x, y + 2f), Offset(x, y + lineHeightPx - 2f), width)
    }

    if (tab.content.selection.collapsed) {
        caret(tab.content.selection.start, c.accent, 2f)
    }
    tab.secondarySelections.filter { it.isCaret }.forEach { caret(it.start, c.accent.copy(alpha = 0.9f), 1.5f) }
}

private fun DrawScope.drawSelection(
    start: Int,
    end: Int,
    index: LineIndex,
    text: String,
    lineHeightPx: Float,
    charWidthPx: Float,
    paddingPx: Float,
    color: Color,
) {
    val a = minOf(start, end).coerceIn(0, text.length)
    val b = maxOf(start, end).coerceIn(0, text.length)
    var pos = a
    while (pos < b) {
        val line = index.lineForOffset(pos)
        val lineEnd = minOf(b, index.lineEnd(text, line))
        val colStart = pos - index.lineStart(line)
        val colEnd = if (lineEnd == index.lineStart(line) && pos < b) colStart + 1 else lineEnd - index.lineStart(line)
        drawRect(
            color = color,
            topLeft = Offset(paddingPx + colStart * charWidthPx, paddingPx + (line - 1) * lineHeightPx),
            size = androidx.compose.ui.geometry.Size(
                ((colEnd - colStart).coerceAtLeast(1)) * charWidthPx,
                lineHeightPx,
            )
        )
        pos = if (lineEnd < b) lineEnd + 1 else b
    }
}

private fun DrawScope.drawBracket(
    offset: Int,
    index: LineIndex,
    text: String,
    lineHeightPx: Float,
    charWidthPx: Float,
    paddingPx: Float,
    color: Color,
) {
    val safe = offset.coerceIn(0, text.length)
    val line = index.lineForOffset(safe)
    val col = safe - index.lineStart(line)
    drawRect(
        color = color.copy(alpha = 0.20f),
        topLeft = Offset(paddingPx + col * charWidthPx, paddingPx + (line - 1) * lineHeightPx),
        size = androidx.compose.ui.geometry.Size(charWidthPx, lineHeightPx),
    )
    drawRect(
        color = color,
        topLeft = Offset(paddingPx + col * charWidthPx, paddingPx + (line - 1) * lineHeightPx),
        size = androidx.compose.ui.geometry.Size(charWidthPx, 1.5f),
    )
}

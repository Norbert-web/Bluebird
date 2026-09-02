package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// BlockViews.kt — renders the block tree: individual block composables, the paginated document
// canvas that lays pages out one under another, and the headings navigation panel.
// ============================================================================================

import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import io.github.norbertweb.bluebird.ui.theme.bluebirdColors

// ---- search ------------------------------------------------------------------------------

data class SearchMatch(val blockIndex: Int, val start: Int, val end: Int)

fun findAllMatches(doc: WordDocument, query: String): List<SearchMatch> {
    if (query.isBlank()) return emptyList()
    val matches = mutableListOf<SearchMatch>()
    doc.blocks.forEachIndexed { idx, b ->
        if (b is ParagraphBlock) {
            val text = b.field.text
            var start = text.indexOf(query, 0, ignoreCase = true)
            while (start >= 0) {
                matches.add(SearchMatch(idx, start, start + query.length))
                start = text.indexOf(query, start + 1, ignoreCase = true)
            }
        }
    }
    return matches
}

// ---- paragraph -----------------------------------------------------------------------------

@Composable
fun ParagraphView(
    para: ParagraphBlock, zoom: Float, textColor: Color,
    listNumber: Int? = null, showPlaceholder: Boolean = false, readOnly: Boolean = false,
    highlightRange: IntRange? = null,
    jumpRequester: BringIntoViewRequester? = null,
    requestFocus: Boolean = false, onFocusConsumed: () -> Unit = {},
    onFocus: () -> Unit = {}, onValueChange: (TextFieldValue) -> Unit = {},
    onCopy: () -> Unit = {}, onCut: () -> Unit = {}, onPaste: () -> Unit = {},
    onSelectAll: () -> Unit = {}, onLink: () -> Unit = {}, onComment: () -> Unit = {},
    onBoundaryKey: (Boolean) -> Boolean = { false },
    onMoveAcrossParagraph: (Boolean, Boolean, Boolean) -> Boolean = { _, _, _ -> false },
    onDocumentSelectionDelete: (Boolean) -> Boolean = { false },
    onSelectionChange: (TextRange) -> Unit = {}
) {
    val focusRequester = remember(para.id) { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusConsumed()
        }
    }
    val style = BuiltInStyles.byId(para.styleId)
    val align = para.alignmentOverride ?: style.alignment
    val spacingBefore = para.spacingBeforeOverride ?: style.spacingBefore
    val spacingAfter = para.spacingAfterOverride ?: style.spacingAfter
    val listIndent = (para.listLevel * 20).toFloat()
    Row(
        modifier = Modifier.fillMaxWidth()
            .let { if (jumpRequester != null) it.bringIntoViewRequester(jumpRequester) else it }
            .padding(
                start = ((listIndent + para.leftIndentPt / 0.75f) * zoom).dp,
                end = ((para.rightIndentPt / 0.75f) * zoom).dp,
                top = (spacingBefore * zoom).dp,
                bottom = (spacingAfter * zoom).dp
            )
    ) {
        if (para.listType != null) {
            val marker = if (para.listType == ListType.BULLET) "•" else "${listNumber ?: 1}."
            Text(marker, color = textColor, fontSize = (style.fontSize * zoom).sp, modifier = Modifier.padding(end = 6.dp))
        }
        Box(Modifier.weight(1f)) {
            val selectionColors = TextSelectionColors(
                handleColor = bluebirdColors.AccentBlue,
                backgroundColor = bluebirdColors.AccentBlue.copy(alpha = 0.22f)
            )
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                BasicTextField(
                    value = para.field,
                onValueChange = onValueChange,
                enabled = !readOnly,
                visualTransformation = RichTextTransformation(
                    if (para.field.text.isEmpty()) emptyList()
                    else normalizeAndMerge(para.spans, para.field.text.length, style.baseAttrs()),
                    highlightRange
                ),
                textStyle = TextStyle(
                    color = textColor, fontSize = (style.fontSize * zoom).sp, textAlign = align,
                    lineHeight = (style.fontSize * para.lineSpacing * zoom).sp,
                    textIndent = TextIndent(firstLine = (para.firstLineIndentPt * zoom / 0.75f).sp)
                ),
                cursorBrush = SolidColor(bluebirdColors.AccentBlue),
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (it.isFocused && !readOnly) onFocus() }
                        .onPreviewKeyEvent { event ->
                            if (readOnly || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val sel = para.field.selection
                            val cursor = sel.end.coerceIn(0, para.field.text.length)
                            val collapsed = sel.collapsed
                            if (!collapsed && (event.key == Key.Backspace || event.key == Key.Delete)) {
                                if (onDocumentSelectionDelete(event.key == Key.Delete)) return@onPreviewKeyEvent true
                            }
                            if (event.isCtrlPressed && !event.isShiftPressed && event.key == Key.A) {
                                onSelectAll(); return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.MoveHome && event.isCtrlPressed) {
                                val anchor = if (event.isShiftPressed) sel.start else 0
                                val target = 0
                                para.field = para.field.copy(selection = if (event.isShiftPressed) TextRange(anchor, target) else TextRange(target))
                                onSelectionChange(para.field.selection)
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.MoveEnd && event.isCtrlPressed) {
                                val anchor = if (event.isShiftPressed) sel.start else para.field.text.length
                                val target = para.field.text.length
                                para.field = para.field.copy(selection = if (event.isShiftPressed) TextRange(anchor, target) else TextRange(target))
                                onSelectionChange(para.field.selection)
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.MoveHome) {
                                val target = 0
                                para.field = para.field.copy(selection = if (event.isShiftPressed) TextRange(sel.start, target) else TextRange(target))
                                onSelectionChange(para.field.selection)
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.MoveEnd) {
                                val target = para.field.text.length
                                para.field = para.field.copy(selection = if (event.isShiftPressed) TextRange(sel.start, target) else TextRange(target))
                                onSelectionChange(para.field.selection)
                                return@onPreviewKeyEvent true
                            }
                            if (event.isShiftPressed && !event.isCtrlPressed && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) {
                                val atBoundary = if (event.key == Key.DirectionLeft) cursor == 0 else cursor == para.field.text.length
                                if (atBoundary) return@onPreviewKeyEvent onMoveAcrossParagraph(event.key == Key.DirectionRight, true, false)
                            }
                            if (event.isCtrlPressed && event.isShiftPressed && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) {
                                val text = para.field.text
                                var p = if (event.key == Key.DirectionLeft) sel.min else sel.max
                                if (event.key == Key.DirectionLeft) {
                                    while (p > 0 && text[p - 1].isWhitespace()) p--
                                    while (p > 0 && !text[p - 1].isWhitespace()) p--
                                } else {
                                    while (p < text.length && text[p].isWhitespace()) p++
                                    while (p < text.length && !text[p].isWhitespace()) p++
                                }
                                if ((event.key == Key.DirectionLeft && sel.min == 0) || (event.key == Key.DirectionRight && sel.max == text.length)) {
                                    return@onPreviewKeyEvent onMoveAcrossParagraph(event.key == Key.DirectionRight, true, true)
                                }
                                para.field = para.field.copy(selection = TextRange(sel.start, p))
                                onSelectionChange(para.field.selection)
                                return@onPreviewKeyEvent true
                            }
                            if (event.isCtrlPressed && !event.isShiftPressed && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) {
                                val text = para.field.text
                                var p = if (event.key == Key.DirectionLeft) sel.min else sel.max
                                if (event.key == Key.DirectionLeft) {
                                    while (p > 0 && text[p - 1].isWhitespace()) p--
                                    while (p > 0 && !text[p - 1].isWhitespace()) p--
                                } else {
                                    while (p < text.length && text[p].isWhitespace()) p++
                                    while (p < text.length && !text[p].isWhitespace()) p++
                                }
                                para.field = para.field.copy(selection = TextRange(p))
                                onSelectionChange(para.field.selection)
                                return@onPreviewKeyEvent true
                            }
                            if (collapsed && cursor == 0 && event.key == Key.Backspace) {
                                return@onPreviewKeyEvent onBoundaryKey(false)
                            }
                            if (collapsed && cursor == para.field.text.length && event.key == Key.Delete) {
                                return@onPreviewKeyEvent onBoundaryKey(true)
                            }
                            if (collapsed && event.isCtrlPressed && event.key == Key.Backspace) {
                                val text = para.field.text
                                var p = cursor
                                while (p > 0 && text[p - 1].isWhitespace()) p--
                                while (p > 0 && !text[p - 1].isWhitespace()) p--
                                if (p < cursor) {
                                    onValueChange(TextFieldValue(text.removeRange(p, cursor), TextRange(p)))
                                    return@onPreviewKeyEvent true
                                }
                            }
                            false
                        }
                )
            if (showPlaceholder && para.field.text.isEmpty()) {
                Text("Start typing…", color = textColor.copy(alpha = 0.35f), fontSize = (style.fontSize * zoom).sp)
            }
        }
    }
}
}

// ---- image ---------------------------------------------------------------------------------

@Composable
private fun BoxScope.ImageResizeHandle(
    alignment: Alignment,
    id: String,
    onDrag: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .align(alignment)
            .background(Color.White, CircleShape)
            .border(1.dp, bluebirdColors.AccentBlue, CircleShape)
            .pointerInput(id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val delta = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart) {
                            -dragAmount.x
                        } else {
                            dragAmount.x
                        }
                        onDrag(delta)
                    }
                )
            }
    )
}

@Composable
fun ImageView(img: ImageBlock, zoom: Float, readOnly: Boolean, selected: Boolean = false, onSelect: () -> Unit, onDelete: () -> Unit) {
    var showContextMenu by remember(img.id) { mutableStateOf(false) }
    val horizontalAlignment = when (img.alignment) {
        TextAlign.Center -> Alignment.CenterHorizontally
        TextAlign.End -> Alignment.End
        else -> Alignment.Start
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).combinedClickable(
            enabled = !readOnly,
            onClick = { onSelect() },
            onLongClick = { onSelect(); showContextMenu = true }
        ),
        horizontalAlignment = horizontalAlignment
    ) {
        val bmp = img.bitmap
        if (bmp != null) {
            Box(
                modifier = Modifier
                    .width((img.widthDp * zoom).dp)
                    .wrapContentHeight()
                    .then(if (selected) Modifier.border(1.dp, bluebirdColors.AccentBlue) else Modifier)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(), contentDescription = "Inserted image",
                    modifier = Modifier.fillMaxWidth().rotate(img.rotationDeg.toFloat())
                )
                if (selected && !readOnly) {
                    ImageResizeHandle(Alignment.TopStart, img.id) { dx ->
                        img.widthDp = (img.widthDp + dx / zoom).toInt().coerceIn(80, 900)
                    }
                    ImageResizeHandle(Alignment.TopEnd, img.id) { dx ->
                        img.widthDp = (img.widthDp + dx / zoom).toInt().coerceIn(80, 900)
                    }
                    ImageResizeHandle(Alignment.BottomStart, img.id) { dx ->
                        img.widthDp = (img.widthDp + dx / zoom).toInt().coerceIn(80, 900)
                    }
                    ImageResizeHandle(Alignment.BottomEnd, img.id) { dx ->
                        img.widthDp = (img.widthDp + dx / zoom).toInt().coerceIn(80, 900)
                    }
                    Box(
                        Modifier.align(Alignment.TopCenter).offset(y = (-18).dp).size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FluentIcon("arrow_rotate_clockwise", "Rotate", modifier = Modifier.size(14.dp), tint = bluebirdColors.AccentBlue)
                    }
                }
            }
        }
        if (showContextMenu && !readOnly) {
            WordDropdownMenu(expanded = true, onDismissRequest = { showContextMenu = false }) {
                WordMenuItem("copy", "Select picture") { onSelect(); showContextMenu = false }
                WordMenuDivider()
                WordMenuItem("text_align_left", "Align left") { img.alignment = TextAlign.Start; showContextMenu = false }
                WordMenuItem("text_align_center", "Center") { img.alignment = TextAlign.Center; showContextMenu = false }
                WordMenuItem("text_align_right", "Align right") { img.alignment = TextAlign.End; showContextMenu = false }
                WordMenuDivider()
                WordMenuItem("arrow_rotate_clockwise", "Rotate 90°") { img.rotationDeg = (img.rotationDeg + 90) % 360; showContextMenu = false }
                WordMenuItem("delete", "Delete picture") { showContextMenu = false; onDelete() }
            }
        }
    }
}

// ---- table -----------------------------------------------------------------------------------

@Composable
fun TableView(
    table: TableBlock, zoom: Float, textColor: Color, readOnly: Boolean, selected: Boolean = false,
    onParagraphFocus: (ParagraphBlock) -> Unit, onSelect: () -> Unit, onDelete: () -> Unit,
    onCopy: () -> Unit = {}, onCut: () -> Unit = {}, onPaste: () -> Unit = {},
    onSelectAll: () -> Unit = {}, onLink: () -> Unit = {}, onComment: () -> Unit = {},
    onBoundaryKey: (Boolean) -> Boolean = { false },
    onMoveAcrossParagraph: (Boolean, Boolean, Boolean) -> Boolean = { _, _, _ -> false }
) {
    var shadeTargetCell by remember { mutableStateOf<TableCell?>(null) }
    var selectedCell by remember { mutableStateOf<TableCell?>(null) }
    var showContextMenu by remember(table.id) { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .then(if (selected) Modifier.border(1.dp, bluebirdColors.AccentBlue, RoundedCornerShape(2.dp)) else Modifier)
            .combinedClickable(
                enabled = !readOnly,
                onClick = { onSelect() },
                onLongClick = { onSelect(); showContextMenu = true }
            )
    ) {
        if (!readOnly) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = {
                    val cols = table.rows.firstOrNull()?.cells?.size ?: 1
                    table.rows.add(TableRow().apply { repeat(cols) { cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) } })
                }, modifier = Modifier.size(24.dp)) { FluentIcon("table_insert_row", "Add row", modifier = Modifier.size(14.dp)) }
                IconButton(onClick = { if (table.rows.size > 1) table.rows.removeAt(table.rows.lastIndex) }, modifier = Modifier.size(24.dp)) {
                    FluentIcon("table_delete_row", "Remove row", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { table.rows.forEach { it.cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) } }, modifier = Modifier.size(24.dp)) {
                    FluentIcon("table_insert_column", "Add column", modifier = Modifier.size(14.dp)) }
                IconButton(onClick = { table.rows.forEach { if (it.cells.size > 1) it.cells.removeAt(it.cells.lastIndex) } }, modifier = Modifier.size(24.dp)) {
                    FluentIcon("table_delete_column", "Remove column", modifier = Modifier.size(14.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    FluentIcon("delete", "Delete table", modifier = Modifier.size(14.dp)) }
            }
        }
        table.rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.cells.forEach { cell ->
                    val cellNumbers = computeListNumbers(cell.blocks)
                    Column(
                        Modifier.weight(1f)
                            .border(if (selectedCell === cell) 1.dp else 0.5.dp,
                                if (selectedCell === cell) bluebirdColors.AccentBlue else Color.Gray.copy(alpha = 0.5f))
                            .background(if (selectedCell === cell && cell.backgroundColor == null) bluebirdColors.AccentBlue.copy(alpha = 0.07f)
                                else cell.backgroundColor ?: Color.Transparent)
                            .combinedClickable(
                                enabled = !readOnly,
                                onClick = { onSelect(); selectedCell = cell; shadeTargetCell = cell },
                                onLongClick = { onSelect(); selectedCell = cell; shadeTargetCell = cell; showContextMenu = true }
                            )
                            .padding(4.dp)
                    ) {
                        cell.blocks.forEachIndexed { i, para ->
                            ParagraphView(
                                para = para, zoom = zoom * 0.9f, textColor = textColor,
                                listNumber = cellNumbers[para.id], readOnly = readOnly,
                                onFocus = { onParagraphFocus(para) },
                                onCopy = onCopy, onCut = onCut, onPaste = onPaste,
                                onSelectAll = onSelectAll, onLink = onLink, onComment = onComment,
                                onValueChange = { newVal ->
                                    if (newVal.text.contains("\n")) {
                                        val extra = splitParagraphFromEditedValue(para, newVal)
                                        cell.blocks.addAll(i + 1, extra)
                                    } else {
                                        val old = para.field.text
                                        val base = BuiltInStyles.byId(para.styleId).baseAttrs()
                                        val (field, spans) = editParagraphText(old, newVal.text, newVal.selection.end, para.spans, para.typingStyle, base)
                                        para.spans = spans
                                        para.field = field
                                        para.typingStyle = typingStyleAtCursor(para, field.selection.end)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        if (!readOnly && shadeTargetCell != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Shade cell:", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                Spacer(Modifier.width(6.dp))
                listOf(null, Color(0xFFFFF2CC), Color(0xFFD9EAD3), Color(0xFFCFE2F3), Color(0xFFF4CCCC)).forEach { c ->
                    Box(Modifier.size(18.dp).padding(2.dp).clip(CircleShape)
                        .background(c ?: Color.LightGray.copy(alpha = 0.3f))
                        .clickable { shadeTargetCell?.backgroundColor = c })
                }
            }
        }
        if (showContextMenu && !readOnly) {
            WordDropdownMenu(expanded = true, onDismissRequest = { showContextMenu = false }) {
                WordMenuItem("table_insert_row", "Insert row") {
                    val cols = table.rows.firstOrNull()?.cells?.size ?: 1
                    table.rows.add(TableRow().apply { repeat(cols) { cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) } })
                    showContextMenu = false
                }
                WordMenuItem("table_insert_column", "Insert column") {
                    table.rows.forEach { it.cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) }
                    showContextMenu = false
                }
                WordMenuItem("color_background", "Shade selected cell", enabled = shadeTargetCell != null) { shadeTargetCell?.backgroundColor = Color(0xFFFFF2CC); showContextMenu = false }
                WordMenuDivider()
                WordMenuItem("table_delete_row", "Delete last row", enabled = table.rows.size > 1) { if (table.rows.size > 1) table.rows.removeAt(table.rows.lastIndex); showContextMenu = false }
                WordMenuItem("table_delete_column", "Delete last column", enabled = table.rows.any { it.cells.size > 1 }) { table.rows.forEach { if (it.cells.size > 1) it.cells.removeAt(it.cells.lastIndex) }; showContextMenu = false }
                WordMenuItem("delete", "Delete table") { showContextMenu = false; onDelete() }
            }
        }
    }
}

// ---- page break / TOC ------------------------------------------------------------------------

@Composable
fun PageBreakView(readOnly: Boolean, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.5f)))
        Text(" Page Break ", fontSize = 10.sp, color = Color.Gray)
        Box(Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.5f)))
        if (!readOnly) {
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                FluentIcon("dismiss", null, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun TocView(toc: TocBlock, textColor: Color, readOnly: Boolean, onRegenerate: () -> Unit, onDelete: () -> Unit, onJump: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).border(0.5.dp, Color.Gray.copy(alpha = 0.4f)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Table of Contents", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (!readOnly) {
                IconButton(onClick = onRegenerate, modifier = Modifier.size(22.dp)) { FluentIcon("arrow_clockwise", "Update", modifier = Modifier.size(14.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) { FluentIcon("delete", "Remove", modifier = Modifier.size(14.dp)) }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (toc.entries.isEmpty()) {
            Text("No headings yet — use Heading 1-3 styles, then Update.", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
        }
        toc.entries.forEach { entry ->
            Text(
                entry.text, fontSize = 12.sp, color = bluebirdColors.AccentBlue,
                modifier = Modifier.padding(start = (entry.level * 14).dp, top = 2.dp).clickable { onJump(entry.targetBlockId) }
            )
        }
    }
}

// ---- the paginated document canvas -------------------------------------------------------------

@Composable
fun PagedDocumentView(
    doc: WordDocument, zoom: Float, pageColor: Color, textColor: Color,
    viewMode: DocumentViewMode = DocumentViewMode.MULTIPLE_PAGES, readOnly: Boolean, showRuler: Boolean = true,
    focusedParagraph: ParagraphBlock? = null,
    focusTargetParagraphId: String? = null,
    onFocusTargetChange: (String?) -> Unit = {},
    onFocusTargetConsumed: () -> Unit = {},
    matches: List<SearchMatch>, activeMatch: SearchMatch?,
    jumpRequesters: MutableMap<String, BringIntoViewRequester>, jumpTargetIds: Set<String>,
    onParagraphFocus: (ParagraphBlock) -> Unit,
    onTopIndexFocus: (Int) -> Unit,
    onImageSelect: () -> Unit = {},
    onTableSelect: () -> Unit = {},
    selectedBlockId: String? = null,
    onTextEdit: () -> Unit = {},
    onCopy: () -> Unit = {}, onCut: () -> Unit = {}, onPaste: () -> Unit = {},
    onSelectAll: () -> Unit = {}, onLink: () -> Unit = {}, onComment: () -> Unit = {},
    onSelectionChange: (ParagraphBlock, TextRange) -> Unit = { _, _ -> },
    onMoveAcrossParagraph: (ParagraphBlock, Int, Boolean, Boolean, Boolean) -> Boolean = { _, _, _, _, _ -> false },
    onDocumentSelectionDelete: (Boolean) -> Boolean = { false },
    onRegenerateToc: (TocBlock) -> Unit,
    onJumpToBlock: (String) -> Unit
) {
    val pages = paginate(doc)
    val topNumbers = computeListNumbers(doc.blocks)
    val (pageWidthPt, pageHeightPt) = doc.pageSettings.dimensionsPt()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFFE7E9EC))) {
        val pageWidthZoom = if (viewMode == DocumentViewMode.PAGE_WIDTH) {
            ((maxWidth.value - 24f) / pageWidthPt).coerceIn(0.6f, 2f)
        } else zoom
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            if (showRuler) {
                HorizontalRuler(
                    widthPt = pageWidthPt, zoom = pageWidthZoom,
                    pageSettings = doc.pageSettings,
                    focusedParagraph = focusedParagraph,
                    enabled = !readOnly,
                onLeftIndentChange = { focusedParagraph?.let { it.leftIndentPt = it.leftIndentPt.coerceIn(0f, 288f); doc.isDirty = true } },
                onFirstLineIndentChange = { focusedParagraph?.let { it.firstLineIndentPt = it.firstLineIndentPt.coerceIn(-144f, 144f); doc.isDirty = true } },
                    onRightIndentChange = { focusedParagraph?.let { it.rightIndentPt = it.rightIndentPt.coerceIn(0f, 288f); doc.isDirty = true } }
                )
                Spacer(Modifier.height(3.dp))
            }
            pages.forEachIndexed { pageIdx, page ->
                Box(
                    modifier = Modifier
                        .width((pageWidthPt * pageWidthZoom).dp)
                        .heightIn(min = (pageHeightPt * pageWidthZoom).dp)
                        .shadow(6.dp, RoundedCornerShape(1.dp), clip = false)
                        .border(0.5.dp, Color(0xFFD5D7DA), RoundedCornerShape(1.dp))
                        .background(pageColor)
                        .padding(bottom = if (viewMode == DocumentViewMode.MULTIPLE_PAGES) 18.dp else 4.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        if (doc.showHeader) {
                            HeaderFooterEditor(
                                paragraph = doc.headerParagraph, pageNumber = pageIdx + 1,
                                zoom = pageWidthZoom, textColor = textColor, readOnly = readOnly,
                                label = "Header",
                                topPadding = (doc.pageSettings.marginTopPt * zoom * 0.25f).dp
                            )
                        }
                        Column(
                            Modifier.padding(
                                start = (doc.pageSettings.marginLeftPt * zoom).dp,
                                end = (doc.pageSettings.marginRightPt * zoom).dp,
                                top = (doc.pageSettings.marginTopPt * zoom * (if (doc.showHeader) 0.6f else 1f)).dp,
                                bottom = (doc.pageSettings.marginBottomPt * zoom * (if (doc.showFooter) 0.6f else 1f)).dp
                            )
                        ) {
                            if (page.entries.isEmpty() && pages.size == 1) {
                                Text("Start typing…", color = textColor.copy(alpha = 0.35f), fontSize = (16 * zoom).sp)
                            }
                            page.entries.forEach { (index, block) ->
                                when (block) {
                                    is ParagraphBlock -> {
                                        val match = matches.firstOrNull { it.blockIndex == index }
                                        val isActive = activeMatch?.blockIndex == index
                                        val requester = if (block.id in jumpTargetIds) {
                                            jumpRequesters.getOrPut(block.id) { BringIntoViewRequester() }
                                        } else null
                                        ParagraphView(
                                            para = block, zoom = pageWidthZoom, textColor = textColor,
                                            listNumber = topNumbers[block.id],
                                            showPlaceholder = doc.blocks.size == 1 && block.field.text.isEmpty(),
                                            readOnly = readOnly,
                                            requestFocus = focusTargetParagraphId == block.id,
                                            onFocusConsumed = { if (focusTargetParagraphId == block.id) onFocusTargetConsumed() },
                                            highlightRange = if (isActive && activeMatch != null) activeMatch.start until activeMatch.end
                                                else match?.let { it.start until it.end },
                                            jumpRequester = requester,
                                            onFocus = { onParagraphFocus(block); onTopIndexFocus(index) },
                                            onCopy = onCopy, onCut = onCut, onPaste = onPaste,
                                            onSelectAll = onSelectAll, onLink = onLink, onComment = onComment,
                                            onSelectionChange = { range -> onSelectionChange(block, range) },
                                            onMoveAcrossParagraph = { forward, extend, byWord -> onMoveAcrossParagraph(block, index, forward, extend, byWord) },
                                            onDocumentSelectionDelete = onDocumentSelectionDelete,
                                            onBoundaryKey = { forward ->
                                                val targetIndex = if (forward) index + 1 else index - 1
                                                if (targetIndex !in doc.blocks.indices) false
                                                else {
                                                    val target = doc.blocks[targetIndex] as? ParagraphBlock
                                                    if (target == null) false
                                                    else {
                                                        doc.pushUndoSnapshot()
                                                        if (!forward) {
                                                            val mergedText = target.field.text + block.field.text
                                                            val joinPos = target.field.text.length
                                                            target.field = TextFieldValue(mergedText, TextRange(joinPos))
                                                            target.spans = normalizeAndMerge(
                                                                target.spans + block.spans.map { it.copy(start = it.start + joinPos, end = it.end + joinPos) },
                                                                mergedText.length, BuiltInStyles.byId(target.styleId).baseAttrs()
                                                            )
                                                            doc.blocks.removeAt(index)
                                                            onFocusTargetChange(target.id)
                                                        } else {
                                                            val next = target
                                                            val joinPos = block.field.text.length
                                                            val mergedText = block.field.text + next.field.text
                                                            block.field = TextFieldValue(mergedText, TextRange(joinPos))
                                                            block.spans = normalizeAndMerge(
                                                                block.spans + next.spans.map { it.copy(start = it.start + joinPos, end = it.end + joinPos) },
                                                                mergedText.length, BuiltInStyles.byId(block.styleId).baseAttrs()
                                                            )
                                                            doc.blocks.removeAt(targetIndex)
                                                            onFocusTargetChange(block.id)
                                                        }
                                                        doc.isDirty = true
                                                        doc.lastModified = System.currentTimeMillis()
                                                        true
                                                    }
                                                }
                                            },
                                            onValueChange = { newVal ->
                                                onTextEdit()
                                                if (newVal.text.contains("\n")) {
                                                    val extra = splitParagraphFromEditedValue(block, newVal)
                                                    doc.blocks.addAll(index + 1, extra)
                                                    onFocusTargetChange(extra.lastOrNull()?.id)
                                                } else {
                                                    val old = block.field.text
                                                    val base = BuiltInStyles.byId(block.styleId).baseAttrs()
                                                    val (field, spans) = editParagraphText(old, newVal.text, newVal.selection.end, block.spans, block.typingStyle, base)
                                                    block.spans = spans
                                                    block.field = field
                                                    block.typingStyle = typingStyleAtCursor(block, field.selection.end)
                                                }
                                                doc.isDirty = true
                                                doc.lastModified = System.currentTimeMillis()
                                            }
                                        )
                                    }
                                    is ImageBlock -> ImageView(
                                        img = block, zoom = pageWidthZoom, readOnly = readOnly, selected = selectedBlockId == block.id,
                                        onSelect = { onTopIndexFocus(index); onImageSelect() },
                                        onDelete = { doc.blocks.removeAt(index) }
                                    )
                                    is TableBlock -> TableView(
                                        table = block, zoom = pageWidthZoom, textColor = textColor, readOnly = readOnly, selected = selectedBlockId == block.id,
                                        onParagraphFocus = onParagraphFocus,
                                        onSelect = { onTopIndexFocus(index); onTableSelect() },
                                        onCopy = onCopy, onCut = onCut, onPaste = onPaste,
                                        onSelectAll = onSelectAll, onLink = onLink, onComment = onComment,
                                        onDelete = { doc.blocks.removeAt(index) }
                                    )
                                    is PageBreakBlock -> PageBreakView(readOnly = readOnly, onDelete = { doc.blocks.removeAt(index) })
                                    is TocBlock -> TocView(
                                        toc = block, textColor = textColor, readOnly = readOnly,
                                        onRegenerate = { onRegenerateToc(block) },
                                        onDelete = { doc.blocks.removeAt(index) },
                                        onJump = onJumpToBlock
                                    )
                                }
                            }
                        }
                        if (doc.showFooter) {
                            HeaderFooterEditor(
                                paragraph = doc.footerParagraph, pageNumber = pageIdx + 1,
                                zoom = pageWidthZoom, textColor = textColor, readOnly = readOnly,
                                label = "Footer",
                                bottomPadding = (doc.pageSettings.marginBottomPt * zoom * 0.25f).dp
                            )
                        }
                    }
                }
                Text("Page ${pageIdx + 1} of ${pages.size}", fontSize = 9.sp, color = textColor.copy(alpha = 0.4f), modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}


@Composable
private fun HeaderFooterEditor(
    paragraph: ParagraphBlock, pageNumber: Int, zoom: Float, textColor: Color,
    readOnly: Boolean, label: String, topPadding: Dp = 0.dp, bottomPadding: Dp = 0.dp
) {
    val displayText = paragraph.field.text.replace("{page}", pageNumber.toString())
    BasicTextField(
        value = paragraph.field,
        onValueChange = { paragraph.field = it },
        enabled = !readOnly,
        textStyle = TextStyle(color = textColor.copy(alpha = 0.65f), fontSize = (10 * zoom).sp, textAlign = TextAlign.Center),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = bottomPadding)
    )
    if (paragraph.field.text.isEmpty() && !readOnly) {
        Text(label, fontSize = (9 * zoom).sp, color = textColor.copy(alpha = 0.25f), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun HorizontalRuler(
    widthPt: Float, zoom: Float, pageSettings: PageSettings,
    focusedParagraph: ParagraphBlock?, enabled: Boolean,
    onLeftIndentChange: (Float) -> Unit,
    onFirstLineIndentChange: (Float) -> Unit,
    onRightIndentChange: (Float) -> Unit
) {
    val rulerHeight = 28.dp
    val start = pageSettings.marginLeftPt
    val end = widthPt - pageSettings.marginRightPt
    Box(
        Modifier.width((widthPt * zoom).dp).height(rulerHeight)
            .background(Color(0xFFF3F3F3), RoundedCornerShape(1.dp))
            .border(0.5.dp, Color(0xFFD3D5D8), RoundedCornerShape(1.dp))
            .drawBehind {
                val unit = zoom
                val inch = 72f * unit
                drawRect(Color(0xFFF3F3F3))
                drawRect(Color(0xFFE8E8E8), androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
                var x = start * unit
                while (x <= end * unit) {
                    drawLine(Color(0xFF8A8A8A).copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(x, size.height), androidx.compose.ui.geometry.Offset(x, size.height - 10.dp.toPx()))
                    for (minor in 1..7) {
                        val mx = x + minor * inch / 8f
                        if (mx < end * unit) drawLine(Color(0xFF9A9A9A).copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(mx, size.height), androidx.compose.ui.geometry.Offset(mx, size.height - 5.dp.toPx()))
                    }
                    x += inch
                }
            }
    ) {
        Text("0", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.offset(x = (start * zoom + 2).dp, y = 1.dp))
        Text("1", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.offset(x = (start * zoom + 72 * zoom - 3).dp, y = 1.dp))
        Text("2", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.offset(x = (start * zoom + 144 * zoom - 3).dp, y = 1.dp))

        if (focusedParagraph != null) {
            val leftX = (start + focusedParagraph.leftIndentPt) * zoom
            val firstX = (start + focusedParagraph.leftIndentPt + focusedParagraph.firstLineIndentPt) * zoom
            val rightX = (widthPt - pageSettings.marginRightPt - focusedParagraph.rightIndentPt) * zoom

            RulerMarker(leftX, MarkerType.LEFT, enabled) { dx ->
                onLeftIndentChange(focusedParagraph.leftIndentPt + dx / zoom)
            }
            RulerMarker(firstX, MarkerType.FIRST_LINE, enabled) { dx ->
                onFirstLineIndentChange(focusedParagraph.firstLineIndentPt + dx / zoom)
            }
            RulerMarker(rightX, MarkerType.RIGHT, enabled) { dx ->
                onRightIndentChange(focusedParagraph.rightIndentPt - dx / zoom)
            }
        }
    }
}

private enum class MarkerType { LEFT, FIRST_LINE, RIGHT }

@Composable
private fun RulerMarker(x: Float, type: MarkerType, enabled: Boolean, onDrag: (Float) -> Unit) {
    Box(
        Modifier.offset(x = (x - 5).dp, y = if (type == MarkerType.RIGHT) 2.dp else 15.dp)
            .size(width = 12.dp, height = 12.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val path = androidx.compose.ui.graphics.Path()
            if (type == MarkerType.RIGHT) {
                path.moveTo(size.width / 2f, 0f); path.lineTo(size.width, size.height); path.lineTo(0f, size.height)
            } else {
                path.moveTo(0f, 0f); path.lineTo(size.width, 0f); path.lineTo(size.width / 2f, size.height)
            }
            drawPath(path, bluebirdColors.AccentBlue)
        }
    }
}

// ---- navigation panel (headings outline) --------------------------------------------------

@Composable
fun NavigationPanel(doc: WordDocument, textColor: Color, onJump: (String) -> Unit) {
    val headings = doc.blocks.filterIsInstance<ParagraphBlock>().filter { it.styleId in BuiltInStyles.HEADING_IDS || it.styleId == "title" }
    Column(Modifier.width(168.dp).fillMaxHeight().padding(6.dp)) {
        Text("Navigation", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        if (headings.isEmpty()) {
            Text("No headings yet", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
        }
        LazyColumn(Modifier.weight(1f)) {
            items(headings.size) { i ->
                val h = headings[i]
                val level = when (h.styleId) { "title" -> 0; "heading1" -> 1; "heading2" -> 2; else -> 3 }
                Text(
                    h.field.text.ifBlank { "(untitled)" }, fontSize = 12.sp, color = textColor,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(start = (level * 12).dp, top = 4.dp, bottom = 4.dp)
                        .clickable { onJump(h.id) })
            }
        }
    }
}


@Composable
fun PageThumbnailPanel(doc: WordDocument, textColor: Color, onJump: (String) -> Unit) {
    val pages = remember(doc.id, doc.blocks.size) { paginate(doc) }
    Column(
        Modifier.width(86.dp).fillMaxHeight()
            .background(textColor.copy(alpha = 0.035f))
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FluentIcon("document_page_number", "Pages", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pages", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(pages) { pageIndex, page ->
                val first = page.entries.firstOrNull()
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.25f))
                        .background(Color.White)
                        .clickable { first?.let { onJump(it.block.id) } }
                        .padding(4.dp)
                ) {
                    Text(
                        "Page ${pageIndex + 1}",
                        color = Color.DarkGray, fontSize = 9.sp,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Box(
                        Modifier.fillMaxWidth().height(84.dp)
                            .background(Color.White)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(5.dp)) {
                            repeat(minOf(10, page.entries.size)) {
                                Box(
                                    Modifier.fillMaxWidth(if (it == 0) 0.72f else 0.9f)
                                        .height(2.dp)
                                        .background(Color.LightGray)
                                        .padding(bottom = 5.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

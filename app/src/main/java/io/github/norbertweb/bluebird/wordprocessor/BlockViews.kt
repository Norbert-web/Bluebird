package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// BlockViews.kt — renders the block tree: individual block composables, the paginated document
// canvas that lays pages out one under another, and the headings navigation panel.
// ============================================================================================

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onFocus: () -> Unit = {}, onValueChange: (TextFieldValue) -> Unit = {}
) {
    val style = BuiltInStyles.byId(para.styleId)
    val align = para.alignmentOverride ?: style.alignment
    Row(
        modifier = Modifier.fillMaxWidth()
            .let { if (jumpRequester != null) it.bringIntoViewRequester(jumpRequester) else it }
            .padding(
                start = (para.listLevel * 20 * zoom).dp,
                top = (style.spacingBefore * zoom).dp,
                bottom = (style.spacingAfter * zoom).dp
            )
    ) {
        if (para.listType != null) {
            val marker = if (para.listType == ListType.BULLET) "•" else "${listNumber ?: 1}."
            Text(marker, color = textColor, fontSize = (style.fontSize * zoom).sp, modifier = Modifier.padding(end = 6.dp))
        }
        Box(Modifier.weight(1f)) {
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
                    lineHeight = (style.fontSize * 1.4f * zoom).sp
                ),
                cursorBrush = SolidColor(bluebirdColors.AccentBlue),
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused && !readOnly) onFocus() }
            )
            if (showPlaceholder && para.field.text.isEmpty()) {
                Text("Start typing…", color = textColor.copy(alpha = 0.35f), fontSize = (style.fontSize * zoom).sp)
            }
        }
    }
}

// ---- image ---------------------------------------------------------------------------------

@Composable
fun ImageView(img: ImageBlock, zoom: Float, readOnly: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val horizontalAlignment = when (img.alignment) {
        TextAlign.Center -> Alignment.CenterHorizontally
        TextAlign.End -> Alignment.End
        else -> Alignment.Start
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(enabled = !readOnly) { onSelect() },
        horizontalAlignment = horizontalAlignment
    ) {
        val bmp = img.bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(), contentDescription = "Inserted image",
                modifier = Modifier.width((img.widthDp * zoom).dp).rotate(img.rotationDeg.toFloat())
            )
        } else {
            Box(
                Modifier.width((img.widthDp * zoom).dp).height(120.dp).background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { Text("Image unavailable", fontSize = 11.sp) }
        }
        if (!readOnly) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { img.alignment = TextAlign.Start }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.FormatAlignLeft, null, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { img.alignment = TextAlign.Center }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.FormatAlignCenter, null, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { img.alignment = TextAlign.End }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.FormatAlignRight, null, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { img.widthDp = (img.widthDp - 40).coerceAtLeast(80) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { img.widthDp = (img.widthDp + 40).coerceAtMost(900) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { img.rotationDeg = (img.rotationDeg + 90) % 360 }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.RotateRight, null, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ---- table -----------------------------------------------------------------------------------

@Composable
fun TableView(
    table: TableBlock, zoom: Float, textColor: Color, readOnly: Boolean,
    onParagraphFocus: (ParagraphBlock) -> Unit, onSelect: () -> Unit, onDelete: () -> Unit
) {
    var shadeTargetCell by remember { mutableStateOf<TableCell?>(null) }

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (!readOnly) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = {
                        val cols = table.rows.firstOrNull()?.cells?.size ?: 1
                        table.rows.add(TableRow().apply { repeat(cols) { cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) } })
                    },
                    modifier = Modifier.size(24.dp)
                ) { Icon(Icons.Default.PlaylistAdd, "Add row", modifier = Modifier.size(14.dp)) }
                IconButton(onClick = { if (table.rows.size > 1) table.rows.removeAt(table.rows.lastIndex) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.PlaylistRemove, "Remove row", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { table.rows.forEach { it.cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) } }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ViewColumn, "Add column", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { table.rows.forEach { if (it.cells.size > 1) it.cells.removeAt(it.cells.lastIndex) } }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ViewCompact, "Remove column", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Delete table", modifier = Modifier.size(14.dp))
                }
            }
        }
        table.rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.cells.forEach { cell ->
                    val cellNumbers = computeListNumbers(cell.blocks)
                    Column(
                        Modifier.weight(1f)
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.5f))
                            .background(cell.backgroundColor ?: Color.Transparent)
                            .clickable(enabled = !readOnly) { onSelect(); shadeTargetCell = cell }
                            .padding(4.dp)
                    ) {
                        cell.blocks.forEachIndexed { i, para ->
                            ParagraphView(
                                para = para, zoom = zoom * 0.9f, textColor = textColor,
                                listNumber = cellNumbers[para.id], readOnly = readOnly,
                                onFocus = { onParagraphFocus(para) },
                                onValueChange = { newVal ->
                                    if (newVal.text.contains("\n")) {
                                        val extra = applyEnterSplit(para, newVal)
                                        cell.blocks.addAll(i + 1, extra)
                                    } else {
                                        val old = para.field.text
                                        val base = BuiltInStyles.byId(para.styleId).baseAttrs()
                                        val (field, spans) = editParagraphText(old, newVal.text, newVal.selection.end, para.spans, para.typingStyle, base)
                                        para.spans = spans
                                        para.field = field
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
                    Box(
                        Modifier.size(18.dp).padding(2.dp).clip(CircleShape)
                            .background(c ?: Color.LightGray.copy(alpha = 0.3f))
                            .clickable { shadeTargetCell?.backgroundColor = c }
                    )
                }
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
                Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
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
                IconButton(onClick = onRegenerate, modifier = Modifier.size(22.dp)) { Icon(Icons.Default.Refresh, "Update", modifier = Modifier.size(14.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) { Icon(Icons.Default.Delete, "Remove", modifier = Modifier.size(14.dp)) }
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
    doc: WordDocument, zoom: Float, pageColor: Color, textColor: Color, readOnly: Boolean,
    matches: List<SearchMatch>, activeMatch: SearchMatch?,
    jumpRequesters: MutableMap<String, BringIntoViewRequester>, jumpTargetIds: Set<String>,
    onParagraphFocus: (ParagraphBlock) -> Unit,
    onTopIndexFocus: (Int) -> Unit,
    onRegenerateToc: (TocBlock) -> Unit,
    onJumpToBlock: (String) -> Unit
) {
    val pages = paginate(doc)
    val topNumbers = computeListNumbers(doc.blocks)
    val (pageWidthPt, pageHeightPt) = doc.pageSettings.dimensionsPt()

    Box(modifier = Modifier.fillMaxSize().background(Color(0x11000000))) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            pages.forEachIndexed { pageIdx, page ->
                Box(
                    modifier = Modifier
                        .width((pageWidthPt * zoom).dp)
                        .heightIn(min = (pageHeightPt * zoom).dp)
                        .shadow(6.dp, RoundedCornerShape(2.dp))
                        .background(pageColor)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        if (doc.showHeader) {
                            Text(
                                doc.headerParagraph.field.text.replace("{page}", "${pageIdx + 1}"),
                                fontSize = (10 * zoom).sp, color = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(
                                    top = (doc.pageSettings.marginTopPt * zoom * 0.4f).dp,
                                    start = (doc.pageSettings.marginLeftPt * zoom).dp
                                )
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
                                            para = block, zoom = zoom, textColor = textColor,
                                            listNumber = topNumbers[block.id],
                                            showPlaceholder = doc.blocks.size == 1 && block.field.text.isEmpty(),
                                            readOnly = readOnly,
                                            highlightRange = if (isActive && activeMatch != null) activeMatch.start until activeMatch.end
                                                else match?.let { it.start until it.end },
                                            jumpRequester = requester,
                                            onFocus = { onParagraphFocus(block); onTopIndexFocus(index) },
                                            onValueChange = { newVal ->
                                                if (newVal.text.contains("\n")) {
                                                    val extra = applyEnterSplit(block, newVal)
                                                    doc.blocks.addAll(index + 1, extra)
                                                } else {
                                                    val old = block.field.text
                                                    val base = BuiltInStyles.byId(block.styleId).baseAttrs()
                                                    val (field, spans) = editParagraphText(old, newVal.text, newVal.selection.end, block.spans, block.typingStyle, base)
                                                    block.spans = spans
                                                    block.field = field
                                                }
                                                doc.isDirty = true
                                                doc.lastModified = System.currentTimeMillis()
                                            }
                                        )
                                    }
                                    is ImageBlock -> ImageView(
                                        img = block, zoom = zoom, readOnly = readOnly,
                                        onSelect = { onTopIndexFocus(index) },
                                        onDelete = { doc.blocks.removeAt(index) }
                                    )
                                    is TableBlock -> TableView(
                                        table = block, zoom = zoom, textColor = textColor, readOnly = readOnly,
                                        onParagraphFocus = onParagraphFocus,
                                        onSelect = { onTopIndexFocus(index) },
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
                            Text(
                                doc.footerParagraph.field.text.replace("{page}", "${pageIdx + 1}"),
                                fontSize = (10 * zoom).sp, color = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(
                                    bottom = (doc.pageSettings.marginBottomPt * zoom * 0.4f).dp,
                                    start = (doc.pageSettings.marginLeftPt * zoom).dp
                                )
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

// ---- navigation panel (headings outline) --------------------------------------------------

@Composable
fun NavigationPanel(doc: WordDocument, textColor: Color, onJump: (String) -> Unit) {
    val headings = doc.blocks.filterIsInstance<ParagraphBlock>().filter { it.styleId in BuiltInStyles.HEADING_IDS || it.styleId == "title" }
    Column(Modifier.width(220.dp).fillMaxHeight().padding(8.dp)) {
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

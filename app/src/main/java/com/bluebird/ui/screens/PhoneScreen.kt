package com.bluebird.ui.screens

// ============================================================================================
// NOTE: The composable is kept named `PhoneScreen` on purpose so existing navigation/routing
// that points at this screen keeps working. Functionally this is no longer a dialer — it is a
// desktop-Word-inspired document editor ("Word Impress"): ribbon toolbar, rich text formatting,
// New / Open / Save / Save As, PDF export, and native PDF page reading.
//
// ARCHITECTURE (v2 — block-based document model):
// A document is no longer "one string + a list of format spans." It is a list of BLOCKS
// (ParagraphBlock, ImageBlock, TableBlock, PageBreakBlock), matching the .wdoc specification's
// document.json shape (sections -> blocks -> paragraph/runs, image, table, pageBreak). Each
// ParagraphBlock still uses the original flat-span rich-text engine internally (text + a
// gap-free list of FormatRange), but spans now layer ON TOP OF a named paragraph STYLE
// (Normal, Title, Heading 1-3, Quote, Caption, ...) instead of a hardcoded blank default —
// exactly like direct character formatting overriding a style in real Word.
//
// KNOWN SIMPLIFICATIONS (intentional, called out so future passes know where to look):
//  - No real pagination/reflow yet — PageBreakBlock renders as a visual divider on screen and
//    forces a new page on PDF export, but on-screen "pages" don't reflow content between them.
//  - Table cells hold ParagraphBlocks only (no nested images/tables) — matches the spec's
//    intent ("cell contains blocks") but only paragraph blocks are wired up for now.
//  - Pressing Enter mid-paragraph splits it into two ParagraphBlocks (real block-per-paragraph
//    behavior), but focus does not auto-follow into the new block yet — tap it to keep typing.
//  - Images are persisted as base64 inside the single JSON file for now (no zip package / media/
//    folder yet) — that's next once .wdoc packaging work starts, per the block/styles-first plan.
//  - PDF export renders paragraphs, images, and a basic bordered table grid; it does not yet
//    replicate every style's exact typography (bold/italic/color are honored, spacing is close).
// ============================================================================================

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluebird.ui.theme.Win11Colors
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================================
// PALETTE — MS Word inspired (ribbon blue / page white), respects the existing isDark flag
// ============================================================================================

private object WordTheme {
    val ribbonBlue = Color(0xFF2B579A)
    val ribbonBlueDark = Color(0xFF1C3B6B)
    val ribbonAccent = Color(0xFF41A5EE)
    val pageWhite = Color(0xFFFFFFFF)
    val pageShadow = Color(0x40000000)
    val darkCanvas = Color(0xFF121212)
    val darkSurface = Color(0xFF1E1E1E)
    val darkPage = Color(0xFF262626)
}

private val PRESET_COLORS = listOf(
    Color(0xFF1A1A1A), Color(0xFFC00000), Color(0xFF2B579A), Color(0xFF1E7A34),
    Color(0xFFB8860B), Color(0xFF6A1B9A), Color(0xFFE07B00), Color(0xFF616161)
)

// ============================================================================================
// DATA MODEL — character formatting
// ============================================================================================

enum class FontChoice(val label: String, val family: FontFamily) {
    CALIBRI("Calibri", FontFamily.SansSerif),
    CAMBRIA("Cambria", FontFamily.Serif),
    CONSOLAS("Consolas", FontFamily.Monospace),
    ARIAL("Arial", FontFamily.SansSerif)
}

data class StyleAttrs(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontSize: Int = 16,
    val color: Color = Color(0xFF1A1A1A),
    val font: FontChoice = FontChoice.CALIBRI
)

data class FormatRange(val start: Int, val end: Int, val style: StyleAttrs)

enum class DocKind { RICH_TEXT, PDF }
enum class ListType { BULLET, NUMBER }

// ============================================================================================
// DATA MODEL — named paragraph styles (Normal / Title / Heading 1-3 / Quote / Caption)
// ============================================================================================

data class DocStyle(
    val id: String,
    val name: String,
    val font: FontChoice = FontChoice.CALIBRI,
    val fontSize: Int = 16,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val color: Color = Color(0xFF1A1A1A),
    val alignment: TextAlign = TextAlign.Start,
    val spacingBefore: Int = 0,
    val spacingAfter: Int = 8
) {
    /** The character formatting a paragraph in this style has where no direct formatting overrides it. */
    fun baseAttrs(): StyleAttrs = StyleAttrs(
        bold = bold, italic = italic, underline = false, fontSize = fontSize, color = color, font = font
    )
}

object BuiltInStyles {
    val NORMAL = DocStyle("normal", "Normal", fontSize = 16, spacingAfter = 8)
    val TITLE = DocStyle("title", "Title", fontSize = 30, bold = true, alignment = TextAlign.Center, spacingAfter = 12)
    val SUBTITLE = DocStyle(
        "subtitle", "Subtitle", fontSize = 18, italic = true, alignment = TextAlign.Center,
        color = Color(0xFF595959), spacingAfter = 12
    )
    val HEADING1 = DocStyle("heading1", "Heading 1", fontSize = 24, bold = true, color = WordTheme.ribbonBlue, spacingBefore = 14, spacingAfter = 6)
    val HEADING2 = DocStyle("heading2", "Heading 2", fontSize = 20, bold = true, color = WordTheme.ribbonBlue, spacingBefore = 10, spacingAfter = 6)
    val HEADING3 = DocStyle("heading3", "Heading 3", fontSize = 17, bold = true, spacingBefore = 8, spacingAfter = 4)
    val QUOTE = DocStyle("quote", "Quote", fontSize = 16, italic = true, color = Color(0xFF595959), spacingBefore = 6, spacingAfter = 6)
    val CAPTION = DocStyle("caption", "Caption", fontSize = 12, italic = true, color = Color(0xFF595959), spacingAfter = 6)

    val ALL = listOf(NORMAL, TITLE, SUBTITLE, HEADING1, HEADING2, HEADING3, QUOTE, CAPTION)

    fun byId(id: String): DocStyle = ALL.firstOrNull { it.id == id } ?: NORMAL
}

// ============================================================================================
// DATA MODEL — the block tree (paragraph / image / table / page break)
//
// These are plain observable classes (var ... by mutableStateOf) rather than immutable data
// classes, because the tree can now nest (table -> row -> cell -> paragraphs) and copy-on-write
// through that nesting gets unwieldy fast. Structural changes (add/remove/reorder blocks, rows,
// cells) go through SnapshotStateList so Compose still recomposes correctly.
// ============================================================================================

sealed class Block {
    abstract val id: String
}

class ParagraphBlock(override val id: String = UUID.randomUUID().toString()) : Block() {
    var styleId by mutableStateOf("normal")
    var alignmentOverride by mutableStateOf<TextAlign?>(null)
    var listType by mutableStateOf<ListType?>(null)
    var listLevel by mutableStateOf(0)
    var field by mutableStateOf(TextFieldValue(""))
    var spans by mutableStateOf<List<FormatRange>>(emptyList())
    var typingStyle by mutableStateOf(StyleAttrs())
}

class ImageBlock(override val id: String = UUID.randomUUID().toString()) : Block() {
    var bitmap by mutableStateOf<Bitmap?>(null)
    var base64 by mutableStateOf<String?>(null)
    var widthDp by mutableStateOf(300)
    var alignment by mutableStateOf(TextAlign.Center)
}

class TableCell {
    val blocks = mutableStateListOf<ParagraphBlock>()
}

class TableRow {
    val cells = mutableStateListOf<TableCell>()
}

class TableBlock(override val id: String = UUID.randomUUID().toString()) : Block() {
    val rows = mutableStateListOf<TableRow>()
}

class PageBreakBlock(override val id: String = UUID.randomUUID().toString()) : Block()

private fun newTable(rowCount: Int, colCount: Int): TableBlock {
    val t = TableBlock()
    repeat(rowCount) {
        val row = TableRow()
        repeat(colCount) { row.cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) }
        t.rows.add(row)
    }
    return t
}

// ============================================================================================
// DATA MODEL — the document itself
// ============================================================================================

class WordDocument(title: String) {
    val id: String = UUID.randomUUID().toString()
    var title by mutableStateOf(title)
    val blocks = mutableStateListOf<Block>(ParagraphBlock())
    var kind by mutableStateOf(DocKind.RICH_TEXT)
    var pdfUri by mutableStateOf<Uri?>(null)
    var savedUri by mutableStateOf<Uri?>(null)
    var lastModified by mutableStateOf(System.currentTimeMillis())
    var isDirty by mutableStateOf(false)
}

// ============================================================================================
// RICH TEXT ENGINE — plain text + a flat, gap-free list of format spans over it.
// Gaps are now filled with a supplied `base` (the paragraph style's look) instead of a
// hardcoded blank StyleAttrs(), so unformatted text still renders in its paragraph's style.
// ============================================================================================

private fun normalizeAndMerge(rawSpans: List<FormatRange>, length: Int, base: StyleAttrs = StyleAttrs()): List<FormatRange> {
    if (length <= 0) return emptyList()
    val sorted = rawSpans.filter { it.start < it.end }.sortedBy { it.start }
    val filled = mutableListOf<FormatRange>()
    var cursor = 0
    for (span in sorted) {
        val s = span.start.coerceIn(0, length)
        val e = span.end.coerceIn(0, length)
        if (s > cursor) filled.add(FormatRange(cursor, s, base))
        if (e > s) filled.add(FormatRange(s, e, span.style))
        cursor = maxOf(cursor, e)
    }
    if (cursor < length) filled.add(FormatRange(cursor, length, base))
    val merged = mutableListOf<FormatRange>()
    for (span in filled) {
        val last = merged.lastOrNull()
        if (last != null && last.end == span.start && last.style == span.style) {
            merged[merged.lastIndex] = last.copy(end = span.end)
        } else merged.add(span)
    }
    return merged
}

private fun splitSpansAt(spans: List<FormatRange>, pos: Int): List<FormatRange> {
    if (pos <= 0) return spans
    val result = mutableListOf<FormatRange>()
    for (span in spans) {
        if (pos > span.start && pos < span.end) {
            result.add(span.copy(end = pos))
            result.add(span.copy(start = pos))
        } else result.add(span)
    }
    return result
}

/** Applies [transform] to every span fully covered by [start,end), then re-normalizes against [base]. */
private fun applyStyle(
    spans: List<FormatRange>, start: Int, end: Int, length: Int, base: StyleAttrs,
    transform: (StyleAttrs) -> StyleAttrs
): List<FormatRange> {
    if (start >= end) return spans
    var list = splitSpansAt(spans, start)
    list = splitSpansAt(list, end)
    list = list.map { span ->
        if (span.start >= start && span.end <= end) span.copy(style = transform(span.style)) else span
    }
    return normalizeAndMerge(list, length, base)
}

private fun styleAt(spans: List<FormatRange>, pos: Int, base: StyleAttrs): StyleAttrs =
    spans.firstOrNull { pos >= it.start && pos < it.end }?.style ?: base

private fun rangeHas(spans: List<FormatRange>, start: Int, end: Int, pick: (StyleAttrs) -> Boolean): Boolean {
    val relevant = spans.filter { it.end > start && it.start < end }
    if (relevant.isEmpty()) return false
    return relevant.all { pick(it.style) }
}

/** Finds the common-prefix/suffix edit region between an old and new string. */
private fun diffRegion(old: String, new: String): Triple<Int, Int, Int> {
    val minLen = minOf(old.length, new.length)
    var prefix = 0
    while (prefix < minLen && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (suffix < (minLen - prefix) && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    return Triple(prefix, old.length - suffix, new.length - suffix)
}

private fun adjustSpansForEdit(
    spans: List<FormatRange>, newLength: Int,
    changeStart: Int, changeOldEnd: Int, changeNewEnd: Int, typingStyle: StyleAttrs, base: StyleAttrs
): List<FormatRange> {
    val lengthDelta = (changeNewEnd - changeStart) - (changeOldEnd - changeStart)
    val result = mutableListOf<FormatRange>()
    for (span in spans) {
        when {
            span.end <= changeStart -> result.add(span)
            span.start >= changeOldEnd -> result.add(span.copy(start = span.start + lengthDelta, end = span.end + lengthDelta))
            else -> {
                if (span.start < changeStart) result.add(span.copy(end = changeStart))
                if (span.end > changeOldEnd) result.add(span.copy(start = changeOldEnd + lengthDelta, end = span.end + lengthDelta))
            }
        }
    }
    if (changeNewEnd > changeStart) result.add(FormatRange(changeStart, changeNewEnd, typingStyle))
    return normalizeAndMerge(result, newLength, base)
}

private fun buildStyledText(text: String, spans: List<FormatRange>): AnnotatedString = buildAnnotatedString {
    append(text)
    for (span in spans) {
        addStyle(
            SpanStyle(
                fontWeight = if (span.style.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (span.style.italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (span.style.underline) TextDecoration.Underline else TextDecoration.None,
                fontSize = span.style.fontSize.sp,
                color = span.style.color,
                fontFamily = span.style.font.family
            ),
            span.start, span.end
        )
    }
}

private class RichTextTransformation(private val spans: List<FormatRange>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(buildStyledText(text.text, spans), OffsetMapping.Identity)
}

/** Splits `para` at every newline in [newValue]: `para` keeps the first line, the rest come back as new blocks. */
private fun applyEnterSplit(para: ParagraphBlock, newValue: TextFieldValue): List<ParagraphBlock> {
    val parts = newValue.text.split("\n")
    val base = BuiltInStyles.byId(para.styleId).baseAttrs()
    para.field = TextFieldValue(parts[0])
    para.spans = normalizeAndMerge(emptyList(), parts[0].length, base)
    // A new paragraph after a heading/title/subtitle drops back to Normal, matching desktop word processors.
    val nextStyleId = if (para.styleId in setOf("title", "subtitle", "heading1", "heading2", "heading3")) "normal" else para.styleId
    return parts.drop(1).map { t ->
        ParagraphBlock().apply {
            styleId = nextStyleId
            listType = para.listType
            listLevel = para.listLevel
            val b2 = BuiltInStyles.byId(nextStyleId).baseAttrs()
            field = TextFieldValue(t, TextRange(t.length))
            spans = normalizeAndMerge(emptyList(), t.length, b2)
            typingStyle = b2
        }
    }
}

/** Computes the display number for every NUMBER-list paragraph in [blocks], restarting per indent level. */
private fun computeListNumbers(blocks: List<Block>): Map<String, Int> {
    val counters = mutableMapOf<Int, Int>()
    val result = mutableMapOf<String, Int>()
    for (b in blocks) {
        if (b is ParagraphBlock && b.listType == ListType.NUMBER) {
            val lvl = b.listLevel
            val n = (counters[lvl] ?: 0) + 1
            counters[lvl] = n
            counters.keys.filter { it > lvl }.toList().forEach { counters.remove(it) }
            result[b.id] = n
        } else if (b is ParagraphBlock) {
            counters.clear()
        }
    }
    return result
}

// ============================================================================================
// SERIALIZATION — JSON shaped like the .wdoc spec's document.json (format/version/metadata,
// one section with a flat block list). Kept as a single JSON blob for now; splitting this into
// metadata.json / document.json / styles.json inside a real zip package is the next step once
// packaging work starts.
// ============================================================================================

private fun styleAttrsToJson(s: StyleAttrs) = JSONObject().apply {
    put("bold", s.bold); put("italic", s.italic); put("underline", s.underline)
    put("size", s.fontSize); put("color", s.color.toArgb()); put("font", s.font.name)
}

private fun styleAttrsFromJson(o: JSONObject, fallback: StyleAttrs) = StyleAttrs(
    bold = o.optBoolean("bold", fallback.bold),
    italic = o.optBoolean("italic", fallback.italic),
    underline = o.optBoolean("underline", fallback.underline),
    fontSize = o.optInt("size", fallback.fontSize),
    color = Color(o.optInt("color", fallback.color.toArgb())),
    font = FontChoice.entries.firstOrNull { it.name == o.optString("font", fallback.font.name) } ?: fallback.font
)

private fun textAlignToString(a: TextAlign): String = when (a) {
    TextAlign.Center -> "Center"; TextAlign.End -> "End"; TextAlign.Justify -> "Justify"; else -> "Start"
}

private fun textAlignFromString(s: String?): TextAlign? = when (s) {
    "Center" -> TextAlign.Center; "End" -> TextAlign.End; "Justify" -> TextAlign.Justify; "Start" -> TextAlign.Start
    else -> null
}

private fun paragraphToJson(p: ParagraphBlock): JSONObject = JSONObject().apply {
    put("type", "paragraph")
    put("style", p.styleId)
    p.alignmentOverride?.let { put("alignment", textAlignToString(it)) }
    p.listType?.let { put("list", JSONObject().apply { put("type", it.name); put("level", p.listLevel) }) }
    val base = BuiltInStyles.byId(p.styleId).baseAttrs()
    val text = p.field.text
    val spans = normalizeAndMerge(p.spans, text.length, base)
    val runs = JSONArray()
    for (sp in spans) {
        runs.put(JSONObject().apply {
            put("text", text.substring(sp.start, sp.end))
            put("attrs", styleAttrsToJson(sp.style))
        })
    }
    put("runs", runs)
}

private fun paragraphFromJson(o: JSONObject): ParagraphBlock {
    val p = ParagraphBlock()
    p.styleId = o.optString("style", "normal")
    val base = BuiltInStyles.byId(p.styleId).baseAttrs()
    if (o.has("alignment")) p.alignmentOverride = textAlignFromString(o.optString("alignment"))
    o.optJSONObject("list")?.let { l ->
        p.listType = ListType.entries.firstOrNull { it.name == l.optString("type") }
        p.listLevel = l.optInt("level", 0)
    }
    val runsArr = o.optJSONArray("runs") ?: JSONArray()
    val sb = StringBuilder()
    val spans = mutableListOf<FormatRange>()
    for (i in 0 until runsArr.length()) {
        val r = runsArr.getJSONObject(i)
        val t = r.optString("text", "")
        val start = sb.length
        sb.append(t)
        val end = sb.length
        val attrs = r.optJSONObject("attrs")?.let { styleAttrsFromJson(it, base) } ?: base
        if (end > start) spans.add(FormatRange(start, end, attrs))
    }
    p.field = TextFieldValue(sb.toString())
    p.spans = normalizeAndMerge(spans, sb.length, base)
    p.typingStyle = base
    return p
}

private fun blockToJson(b: Block): JSONObject = when (b) {
    is ParagraphBlock -> paragraphToJson(b)
    is ImageBlock -> JSONObject().apply {
        put("type", "image")
        put("data", b.base64 ?: "")
        put("widthDp", b.widthDp)
        put("alignment", textAlignToString(b.alignment))
    }
    is TableBlock -> JSONObject().apply {
        put("type", "table")
        val rowsArr = JSONArray()
        for (row in b.rows) {
            val cellsArr = JSONArray()
            for (cell in row.cells) {
                val blocksArr = JSONArray()
                for (cb in cell.blocks) blocksArr.put(paragraphToJson(cb))
                cellsArr.put(JSONObject().apply { put("blocks", blocksArr) })
            }
            rowsArr.put(JSONObject().apply { put("cells", cellsArr) })
        }
        put("rows", rowsArr)
    }
    is PageBreakBlock -> JSONObject().apply { put("type", "pageBreak") }
}

private fun blockFromJson(o: JSONObject): Block = when (o.optString("type")) {
    "image" -> ImageBlock().apply {
        val b64 = o.optString("data", "")
        base64 = b64.ifEmpty { null }
        widthDp = o.optInt("widthDp", 300)
        alignment = textAlignFromString(o.optString("alignment")) ?: TextAlign.Center
        base64?.let { data ->
            try {
                val bytes = Base64.decode(data, Base64.DEFAULT)
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { /* corrupt/missing image data — leave bitmap null, UI shows a placeholder */ }
        }
    }
    "table" -> {
        val t = TableBlock()
        val rowsArr = o.optJSONArray("rows") ?: JSONArray()
        for (i in 0 until rowsArr.length()) {
            val rowObj = rowsArr.getJSONObject(i)
            val row = TableRow()
            val cellsArr = rowObj.optJSONArray("cells") ?: JSONArray()
            for (j in 0 until cellsArr.length()) {
                val cellObj = cellsArr.getJSONObject(j)
                val cell = TableCell()
                val blocksArr = cellObj.optJSONArray("blocks") ?: JSONArray()
                for (k in 0 until blocksArr.length()) cell.blocks.add(paragraphFromJson(blocksArr.getJSONObject(k)))
                if (cell.blocks.isEmpty()) cell.blocks.add(ParagraphBlock())
                row.cells.add(cell)
            }
            t.rows.add(row)
        }
        t
    }
    "pageBreak" -> PageBreakBlock()
    else -> paragraphFromJson(o)
}

private fun serializeDocument(doc: WordDocument): String {
    val root = JSONObject()
    root.put("format", "wdoc")
    root.put("version", 1)
    root.put("metadata", JSONObject().apply {
        put("title", doc.title)
        put("modified", doc.lastModified)
    })
    val blocksArr = JSONArray()
    for (b in doc.blocks) blocksArr.put(blockToJson(b))
    root.put("document", JSONObject().apply {
        put("sections", JSONArray().put(JSONObject().apply { put("blocks", blocksArr) }))
    })
    return root.toString()
}

private fun parseDocument(raw: String, fallbackTitle: String): WordDocument {
    val doc = WordDocument(fallbackTitle)
    try {
        val root = JSONObject(raw)
        if (root.optString("format") == "wdoc") {
            doc.title = root.optJSONObject("metadata")?.optString("title", fallbackTitle) ?: fallbackTitle
            val sections = root.optJSONObject("document")?.optJSONArray("sections")
            val blocksArr = sections?.optJSONObject(0)?.optJSONArray("blocks")
            if (blocksArr != null && blocksArr.length() > 0) {
                doc.blocks.clear()
                for (i in 0 until blocksArr.length()) doc.blocks.add(blockFromJson(blocksArr.getJSONObject(i)))
                return doc
            }
        }
    } catch (_: Exception) {
        // Not our JSON format (e.g. an older WORDIMPRESS1 save, or an imported .txt) — fall through.
    }
    doc.blocks.clear()
    doc.blocks.add(ParagraphBlock().apply { field = TextFieldValue(raw) })
    return doc
}

private fun plainTextOf(doc: WordDocument): String = doc.blocks.joinToString("\n\n") { b ->
    when (b) {
        is ParagraphBlock -> b.field.text
        is ImageBlock -> "[image]"
        is TableBlock -> b.rows.joinToString("\n") { row -> row.cells.joinToString(" | ") { c -> c.blocks.joinToString(" ") { it.field.text } } }
        is PageBreakBlock -> "---- page break ----"
    }
}

// ============================================================================================
// PDF EXPORT — walks the block tree onto A4-ish pages using android.graphics.pdf.PdfDocument
// ============================================================================================

private fun exportDocToPdf(doc: WordDocument): PdfDocument {
    val pdf = PdfDocument()
    val pageWidth = 595; val pageHeight = 842
    val margin = 48f
    val maxWidth = pageWidth - margin * 2

    var pageNum = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
    var canvas = page.canvas
    var y = margin

    fun newPage() {
        pdf.finishPage(page)
        pageNum++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        canvas = page.canvas
        y = margin
    }

    for (block in doc.blocks) {
        when (block) {
            is PageBreakBlock -> newPage()

            is ParagraphBlock -> {
                val style = BuiltInStyles.byId(block.styleId)
                val paint = Paint().apply {
                    textSize = style.fontSize.toFloat() * 1.1f
                    isAntiAlias = true
                    isFakeBoldText = style.bold
                    textSkewX = if (style.italic) -0.2f else 0f
                    color = style.color.toArgb()
                }
                val lineHeight = paint.fontSpacing
                y += style.spacingBefore.toFloat() * 0.5f
                val text = block.field.text
                val rawLines = if (text.isEmpty()) listOf("") else text.split("\n")
                for (raw in rawLines) {
                    var remaining = raw
                    if (remaining.isEmpty()) {
                        if (y + lineHeight > pageHeight - margin) newPage()
                        y += lineHeight
                        continue
                    }
                    while (remaining.isNotEmpty()) {
                        if (y + lineHeight > pageHeight - margin) newPage()
                        val count = paint.breakText(remaining, true, maxWidth, null)
                        var breakAt = count
                        if (breakAt < remaining.length) {
                            val lastSpace = remaining.lastIndexOf(' ', breakAt - 1)
                            if (lastSpace > 0) breakAt = lastSpace
                        }
                        val line = remaining.substring(0, breakAt).trimEnd()
                        val align = block.alignmentOverride ?: style.alignment
                        val x = when (align) {
                            TextAlign.Center -> margin + (maxWidth - paint.measureText(line)) / 2
                            TextAlign.End -> margin + maxWidth - paint.measureText(line)
                            else -> margin
                        }
                        canvas.drawText(line, x, y + lineHeight * 0.8f, paint)
                        y += lineHeight
                        remaining = remaining.substring(breakAt).trimStart()
                    }
                }
                y += style.spacingAfter.toFloat() * 0.5f
            }

            is ImageBlock -> {
                block.bitmap?.let { bmp ->
                    val w = minOf(maxWidth, block.widthDp.toFloat())
                    val h = w * bmp.height / bmp.width.toFloat()
                    if (y + h > pageHeight - margin) newPage()
                    val scaled = Bitmap.createScaledBitmap(bmp, w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1), true)
                    val x = when (block.alignment) {
                        TextAlign.Center -> margin + (maxWidth - w) / 2
                        TextAlign.End -> margin + maxWidth - w
                        else -> margin
                    }
                    canvas.drawBitmap(scaled, x, y, null)
                    y += h + 10f
                }
            }

            is TableBlock -> {
                val textPaint = Paint().apply { textSize = 11f; isAntiAlias = true; color = android.graphics.Color.BLACK }
                val borderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = android.graphics.Color.GRAY }
                val cols = block.rows.firstOrNull()?.cells?.size ?: 0
                if (cols > 0) {
                    val colWidth = maxWidth / cols
                    val rowHeight = 24f
                    for (row in block.rows) {
                        if (y + rowHeight > pageHeight - margin) newPage()
                        row.cells.forEachIndexed { c, cell ->
                            val cx = margin + c * colWidth
                            canvas.drawRect(cx, y, cx + colWidth, y + rowHeight, borderPaint)
                            val cellText = cell.blocks.joinToString(" ") { it.field.text }
                            canvas.drawText(cellText.take(40), cx + 4, y + rowHeight * 0.65f, textPaint)
                        }
                        y += rowHeight
                    }
                    y += 10f
                }
            }
        }
    }
    pdf.finishPage(page)
    return pdf
}

// ============================================================================================
// MAIN COMPOSABLE
// ============================================================================================

private enum class RibbonTab { FILE, HOME, INSERT, VIEW }

@Composable
fun PhoneScreen(isDark: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val textColor = if (isDark) Win11Colors.TextPrimary else Win11Colors.TextPrimaryLight
    val appBg = if (isDark) WordTheme.darkCanvas else Color(0xFFE8EAED)
    val pageColor = if (isDark) WordTheme.darkPage else WordTheme.pageWhite
    val ribbonBg = if (isDark) WordTheme.ribbonBlueDark else WordTheme.ribbonBlue
    val ribbonStripBg = if (isDark) WordTheme.darkSurface else Color(0xFFF3F2F1)

    val documents = remember { mutableStateListOf(WordDocument("Document1")) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    val currentDoc = documents[currentIndex.coerceIn(0, documents.lastIndex)]

    // Which paragraph currently owns the formatting toolbar, and where top-level inserts land.
    // Both reset whenever the active document changes.
    var focusedParagraph by remember(currentDoc.id) {
        mutableStateOf(currentDoc.blocks.filterIsInstance<ParagraphBlock>().firstOrNull())
    }
    var focusedTopIndex by remember(currentDoc.id) { mutableStateOf(0) }

    var ribbonTab by remember { mutableStateOf(RibbonTab.HOME) }
    var sidebarOpen by remember { mutableStateOf(true) }
    var zoom by remember { mutableStateOf(1f) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    fun notify(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun loadPdfPages(uri: Uri) {
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) { notify("Couldn't open PDF"); return }
            val renderer = PdfRenderer(pfd)
            val pages = mutableListOf<Bitmap>()
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages.add(bmp)
                }
            }
            renderer.close()
            pfd.close()
            pdfPages = pages
        } catch (e: Exception) {
            e.printStackTrace()
            notify("Failed to read PDF: ${e.message}")
        }
    }

    // ---- File pickers -------------------------------------------------------------------
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val mime = context.contentResolver.getType(uri) ?: ""
            if (mime.contains("pdf") || uri.toString().endsWith(".pdf", true)) {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
                val doc = WordDocument(name).apply { kind = DocKind.PDF; pdfUri = uri }
                documents.add(doc)
                currentIndex = documents.lastIndex
                ribbonTab = RibbonTab.HOME
                loadPdfPages(uri)
            } else {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported document"
                val doc = parseDocument(text, name).apply { savedUri = uri }
                documents.add(doc)
                currentIndex = documents.lastIndex
                ribbonTab = RibbonTab.HOME
            }
            notify("Opened")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Couldn't open file")
        }
    }

    val openPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
        val doc = WordDocument(name).apply { kind = DocKind.PDF; pdfUri = uri }
        documents.add(doc)
        currentIndex = documents.lastIndex
        loadPdfPages(uri)
    }

    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(serializeDocument(currentDoc).toByteArray()) }
            currentDoc.savedUri = uri
            currentDoc.isDirty = false
            notify("Saved")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Save failed")
        }
    }

    val createTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(plainTextOf(currentDoc).toByteArray()) }
            notify("Saved")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Save failed")
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val pdf = exportDocToPdf(currentDoc)
            context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            pdf.close()
            notify("Exported to PDF")
        } catch (e: Exception) {
            e.printStackTrace(); notify("PDF export failed")
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val img = ImageBlock().apply {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                }
                val insertAt = (focusedTopIndex + 1).coerceIn(0, currentDoc.blocks.size)
                currentDoc.blocks.add(insertAt, img)
                focusedTopIndex = insertAt
                currentDoc.isDirty = true
            } else notify("Couldn't read image")
        } catch (e: Exception) {
            e.printStackTrace(); notify("Couldn't insert image")
        }
    }

    fun saveCurrent() {
        val uri = currentDoc.savedUri
        if (uri == null) {
            createDocLauncher.launch("${currentDoc.title}.wdoc")
        } else {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(serializeDocument(currentDoc).toByteArray()) }
                currentDoc.isDirty = false
                notify("Saved")
            } catch (e: Exception) {
                e.printStackTrace(); notify("Save failed")
            }
        }
    }

    fun newDocument() {
        documents.add(WordDocument("Document${documents.size + 1}"))
        currentIndex = documents.lastIndex
        ribbonTab = RibbonTab.HOME
    }

    fun insertBlockAfterFocus(block: Block) {
        val insertAt = (focusedTopIndex + 1).coerceIn(0, currentDoc.blocks.size)
        currentDoc.blocks.add(insertAt, block)
        focusedTopIndex = insertAt
        currentDoc.isDirty = true
    }

    // ---- Formatting helpers (operate on the currently focused paragraph) -----------------
    fun toggleAttribute(pick: (StyleAttrs) -> Boolean, set: (StyleAttrs, Boolean) -> StyleAttrs) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        val len = p.field.text.length
        if (sel.collapsed) {
            p.typingStyle = set(p.typingStyle, !pick(p.typingStyle))
        } else {
            val newVal = !rangeHas(p.spans, sel.min, sel.max, pick)
            p.spans = applyStyle(p.spans, sel.min, sel.max, len, base) { s -> set(s, newVal) }
        }
        currentDoc.isDirty = true
    }

    fun applyFontSize(size: Int) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = p.typingStyle.copy(fontSize = size)
        } else {
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> s.copy(fontSize = size) }
        }
        currentDoc.isDirty = true
    }

    fun applyColor(color: Color) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = p.typingStyle.copy(color = color)
        } else {
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> s.copy(color = color) }
        }
        currentDoc.isDirty = true
    }

    fun applyFont(font: FontChoice) {
        val p = focusedParagraph ?: return
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val sel = p.field.selection
        if (sel.collapsed) {
            p.typingStyle = p.typingStyle.copy(font = font)
        } else {
            p.spans = applyStyle(p.spans, sel.min, sel.max, p.field.text.length, base) { s -> s.copy(font = font) }
        }
        currentDoc.isDirty = true
    }

    fun setAlign(a: TextAlign) {
        val p = focusedParagraph ?: return
        p.alignmentOverride = a
        currentDoc.isDirty = true
    }

    fun applyParagraphStyle(styleId: String) {
        val p = focusedParagraph ?: return
        p.styleId = styleId
        val base = BuiltInStyles.byId(styleId).baseAttrs()
        p.spans = normalizeAndMerge(emptyList(), p.field.text.length, base)
        p.typingStyle = base
        currentDoc.isDirty = true
    }

    fun toggleList(type: ListType) {
        val p = focusedParagraph ?: return
        p.listType = if (p.listType == type) null else type
        currentDoc.isDirty = true
    }

    fun changeIndent(delta: Int) {
        val p = focusedParagraph ?: return
        p.listLevel = (p.listLevel + delta).coerceIn(0, 4)
        currentDoc.isDirty = true
    }

    fun insertTextAtCursor(text: String) {
        val p = focusedParagraph ?: return
        val sel = p.field.selection
        val old = p.field.text
        val newText = old.substring(0, sel.start) + text + old.substring(sel.end)
        val base = BuiltInStyles.byId(p.styleId).baseAttrs()
        val (s, oe, ne) = diffRegion(old, newText)
        p.spans = adjustSpansForEdit(p.spans, newText.length, s, oe, ne, p.typingStyle, base)
        p.field = TextFieldValue(newText, TextRange(sel.start + text.length))
        currentDoc.isDirty = true
    }

    val fp = focusedParagraph
    val toolbarStyle: StyleAttrs = if (fp == null) StyleAttrs() else {
        val base = BuiltInStyles.byId(fp.styleId).baseAttrs()
        val sel = fp.field.selection
        if (sel.collapsed) fp.typingStyle
        else StyleAttrs(
            bold = rangeHas(fp.spans, sel.min, sel.max) { it.bold },
            italic = rangeHas(fp.spans, sel.min, sel.max) { it.italic },
            underline = rangeHas(fp.spans, sel.min, sel.max) { it.underline },
            fontSize = styleAt(fp.spans, sel.min, base).fontSize,
            color = styleAt(fp.spans, sel.min, base).color,
            font = styleAt(fp.spans, sel.min, base).font
        )
    }
    val toolbarAlign = fp?.let { it.alignmentOverride ?: BuiltInStyles.byId(it.styleId).alignment } ?: TextAlign.Start
    val toolbarStyleId = fp?.styleId ?: "normal"
    val toolbarListType = fp?.listType
    val homeEnabled = currentDoc.kind == DocKind.RICH_TEXT && fp != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = appBg
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(appBg)) {

            // ==================== TITLE BAR ====================
            Row(
                modifier = Modifier.fillMaxWidth().background(ribbonBg).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier
                    .size(22.dp)
                    .clickable { sidebarOpen = !sidebarOpen })
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = currentDoc.title,
                    onValueChange = { t -> currentDoc.title = t; currentDoc.isDirty = true },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.weight(1f)
                )
                if (currentDoc.isDirty) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(WordTheme.ribbonAccent))
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier
                    .size(20.dp)
                    .clickable { if (currentDoc.kind == DocKind.RICH_TEXT) saveCurrent() })
            }

            // ==================== RIBBON TABS ====================
            Row(modifier = Modifier.fillMaxWidth().background(ribbonBg)) {
                listOf(RibbonTab.FILE to "File", RibbonTab.HOME to "Home", RibbonTab.INSERT to "Insert", RibbonTab.VIEW to "View")
                    .forEach { (tab, label) ->
                        val selected = ribbonTab == tab
                        Text(
                            label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { ribbonTab = tab }
                                .background(if (selected) ribbonStripBg.copy(alpha = 0.25f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
            }

            // ==================== BACKSTAGE (File tab) or RIBBON STRIP ====================
            if (ribbonTab == RibbonTab.FILE) {
                FileBackstage(
                    isDark = isDark, textColor = textColor, bg = ribbonStripBg, documents = documents,
                    currentIndex = currentIndex,
                    onSelectDoc = { i -> currentIndex = i; ribbonTab = RibbonTab.HOME },
                    onNew = { newDocument() },
                    onOpen = { openDocLauncher.launch(arrayOf("text/*", "application/pdf", "*/*")) },
                    onOpenPdf = { openPdfLauncher.launch(arrayOf("application/pdf")) },
                    onSave = { saveCurrent() },
                    onSaveAs = { showSaveAsDialog = true },
                    onExportPdf = { exportPdfLauncher.launch("${currentDoc.title}.pdf") },
                    onClose = { ribbonTab = RibbonTab.HOME }
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth().background(ribbonStripBg)) {
                    when (ribbonTab) {
                        RibbonTab.HOME -> HomeRibbon(
                            enabled = homeEnabled,
                            typingOrSelectionStyle = toolbarStyle,
                            alignment = toolbarAlign,
                            currentStyleId = toolbarStyleId,
                            listType = toolbarListType,
                            onBold = { toggleAttribute({ s -> s.bold }, { s, v -> s.copy(bold = v) }) },
                            onItalic = { toggleAttribute({ s -> s.italic }, { s, v -> s.copy(italic = v) }) },
                            onUnderline = { toggleAttribute({ s -> s.underline }, { s, v -> s.copy(underline = v) }) },
                            onFontSizeChange = { applyFontSize(it) },
                            onColorChange = { applyColor(it) },
                            onFontChange = { applyFont(it) },
                            onAlignChange = { setAlign(it) },
                            onBullet = { toggleList(ListType.BULLET) },
                            onNumbered = { toggleList(ListType.NUMBER) },
                            onIndentIncrease = { changeIndent(1) },
                            onIndentDecrease = { changeIndent(-1) },
                            onStyleChange = { applyParagraphStyle(it) }
                        )
                        RibbonTab.INSERT -> InsertRibbon(
                            onInsertDate = { insertTextAtCursor(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())) },
                            onInsertDivider = { insertTextAtCursor("\n" + "─".repeat(32) + "\n") },
                            onInsertTable = { showTableDialog = true },
                            onInsertImage = { imagePickerLauncher.launch("image/*") },
                            onInsertPageBreak = { insertBlockAfterFocus(PageBreakBlock()) }
                        )
                        RibbonTab.VIEW -> ViewRibbon(
                            sidebarOpen = sidebarOpen,
                            zoom = zoom,
                            onToggleSidebar = { sidebarOpen = !sidebarOpen },
                            onZoomChange = { zoom = it }
                        )
                        else -> {}
                    }
                }
            }

            // ==================== MAIN CONTENT: sidebar + canvas/pdf ====================
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedVisibility(visible = sidebarOpen) {
                    DocumentSidebar(
                        documents = documents, currentIndex = currentIndex, isDark = isDark, textColor = textColor,
                        onSelect = { currentIndex = it },
                        onNew = { newDocument() },
                        onDelete = { i ->
                            if (documents.size > 1) {
                                documents.removeAt(i)
                                if (currentIndex >= documents.size) currentIndex = documents.lastIndex
                            } else notify("Can't delete the only open document")
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (currentDoc.kind == DocKind.PDF) {
                        PdfViewerPane(pages = pdfPages, zoom = zoom, isDark = isDark)
                    } else {
                        BlockListEditor(
                            doc = currentDoc, zoom = zoom, pageColor = pageColor, textColor = textColor,
                            onParagraphFocus = { p -> focusedParagraph = p },
                            onTopIndexFocus = { i -> focusedTopIndex = i }
                        )
                    }
                }
            }

            // ==================== STATUS BAR ====================
            StatusBar(doc = currentDoc, zoom = zoom, textColor = textColor, ribbonStripBg = ribbonStripBg, onZoomChange = { zoom = it })
        }
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            currentTitle = currentDoc.title,
            onDismiss = { showSaveAsDialog = false },
            onConfirm = { newTitle, format ->
                currentDoc.title = newTitle
                showSaveAsDialog = false
                when (format) {
                    SaveFormat.WDOC -> createDocLauncher.launch("$newTitle.wdoc")
                    SaveFormat.TXT -> createTxtLauncher.launch("$newTitle.txt")
                    SaveFormat.PDF -> exportPdfLauncher.launch("$newTitle.pdf")
                }
            }
        )
    }

    if (showTableDialog) {
        InsertTableDialog(
            onDismiss = { showTableDialog = false },
            onConfirm = { rows, cols ->
                insertBlockAfterFocus(newTable(rows, cols))
                showTableDialog = false
            }
        )
    }
}

// ============================================================================================
// BLOCK LIST EDITOR — renders the "page" and every top-level block on it
// ============================================================================================

@Composable
private fun BlockListEditor(
    doc: WordDocument, zoom: Float, pageColor: Color, textColor: Color,
    onParagraphFocus: (ParagraphBlock) -> Unit,
    onTopIndexFocus: (Int) -> Unit
) {
    val topNumbers = computeListNumbers(doc.blocks)
    Box(modifier = Modifier.fillMaxSize().background(Color(0x11000000))) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .width((520 * zoom).dp)
                    .heightIn(min = (700 * zoom).dp)
                    .shadow(6.dp, RoundedCornerShape(2.dp))
                    .background(pageColor)
                    .padding((48 * zoom).dp)
            ) {
                Column {
                    if (doc.blocks.isEmpty()) {
                        Text("Start typing…", color = textColor.copy(alpha = 0.35f), fontSize = (16 * zoom).sp)
                    }
                    doc.blocks.forEachIndexed { index, block ->
                        when (block) {
                            is ParagraphBlock -> ParagraphView(
                                para = block, zoom = zoom, textColor = textColor,
                                listNumber = topNumbers[block.id],
                                showPlaceholder = doc.blocks.size == 1 && block.field.text.isEmpty(),
                                onFocus = { onParagraphFocus(block); onTopIndexFocus(index) },
                                onValueChange = { newVal ->
                                    if (newVal.text.contains("\n")) {
                                        val extra = applyEnterSplit(block, newVal)
                                        doc.blocks.addAll(index + 1, extra)
                                    } else {
                                        val old = block.field.text
                                        val (s, oe, ne) = diffRegion(old, newVal.text)
                                        val base = BuiltInStyles.byId(block.styleId).baseAttrs()
                                        block.spans = adjustSpansForEdit(block.spans, newVal.text.length, s, oe, ne, block.typingStyle, base)
                                        block.field = newVal
                                    }
                                    doc.isDirty = true
                                    doc.lastModified = System.currentTimeMillis()
                                }
                            )
                            is ImageBlock -> ImageView(
                                img = block, zoom = zoom,
                                onSelect = { onTopIndexFocus(index) },
                                onDelete = { doc.blocks.removeAt(index) }
                            )
                            is TableBlock -> TableView(
                                table = block, zoom = zoom, textColor = textColor,
                                onParagraphFocus = onParagraphFocus,
                                onSelect = { onTopIndexFocus(index) },
                                onDelete = { doc.blocks.removeAt(index) }
                            )
                            is PageBreakBlock -> PageBreakView(onDelete = { doc.blocks.removeAt(index) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ParagraphView(
    para: ParagraphBlock, zoom: Float, textColor: Color,
    listNumber: Int? = null, showPlaceholder: Boolean = false,
    onFocus: () -> Unit, onValueChange: (TextFieldValue) -> Unit
) {
    val style = BuiltInStyles.byId(para.styleId)
    val align = para.alignmentOverride ?: style.alignment
    Row(
        modifier = Modifier.fillMaxWidth()
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
                visualTransformation = RichTextTransformation(
                    if (para.field.text.isEmpty()) emptyList()
                    else normalizeAndMerge(para.spans, para.field.text.length, style.baseAttrs())
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = textColor, fontSize = (style.fontSize * zoom).sp, textAlign = align,
                    lineHeight = (style.fontSize * 1.4f * zoom).sp
                ),
                cursorBrush = SolidColor(Win11Colors.AccentBlue),
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFocus() }
            )
            if (showPlaceholder && para.field.text.isEmpty()) {
                Text("Start typing…", color = textColor.copy(alpha = 0.35f), fontSize = (style.fontSize * zoom).sp)
            }
        }
    }
}

@Composable
private fun ImageView(img: ImageBlock, zoom: Float, onSelect: () -> Unit, onDelete: () -> Unit) {
    val horizontalAlignment = when (img.alignment) {
        TextAlign.Center -> Alignment.CenterHorizontally
        TextAlign.End -> Alignment.End
        else -> Alignment.Start
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSelect() },
        horizontalAlignment = horizontalAlignment
    ) {
        val bmp = img.bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(), contentDescription = "Inserted image",
                modifier = Modifier.width((img.widthDp * zoom).dp)
            )
        } else {
            Box(
                Modifier.width((img.widthDp * zoom).dp).height(120.dp).background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { Text("Image unavailable", fontSize = 11.sp) }
        }
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
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun TableView(
    table: TableBlock, zoom: Float, textColor: Color,
    onParagraphFocus: (ParagraphBlock) -> Unit, onSelect: () -> Unit, onDelete: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onSelect() }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(12.dp))
            }
        }
        table.rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.cells.forEach { cell ->
                    val cellNumbers = computeListNumbers(cell.blocks)
                    Column(
                        Modifier.weight(1f).border(0.5.dp, Color.Gray.copy(alpha = 0.5f)).padding(4.dp)
                    ) {
                        cell.blocks.forEachIndexed { i, para ->
                            ParagraphView(
                                para = para, zoom = zoom * 0.9f, textColor = textColor,
                                listNumber = cellNumbers[para.id],
                                onFocus = { onParagraphFocus(para) },
                                onValueChange = { newVal ->
                                    if (newVal.text.contains("\n")) {
                                        val extra = applyEnterSplit(para, newVal)
                                        cell.blocks.addAll(i + 1, extra)
                                    } else {
                                        val old = para.field.text
                                        val (s, oe, ne) = diffRegion(old, newVal.text)
                                        val base = BuiltInStyles.byId(para.styleId).baseAttrs()
                                        para.spans = adjustSpansForEdit(para.spans, newVal.text.length, s, oe, ne, para.typingStyle, base)
                                        para.field = newVal
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageBreakView(onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.5f)))
        Text(" Page Break ", fontSize = 10.sp, color = Color.Gray)
        Box(Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.5f)))
        IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
        }
    }
}

// ============================================================================================
// PDF VIEWER
// ============================================================================================

@Composable
private fun PdfViewerPane(pages: List<Bitmap>, zoom: Float, isDark: Boolean) {
    if (pages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Loading PDF…", fontSize = 12.sp)
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(if (isDark) WordTheme.darkCanvas else Color(0xFFE8EAED)),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        itemsIndexed(pages) { index, bmp ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = bmp.asImageBitmap(), contentDescription = "Page ${index + 1}",
                    modifier = Modifier
                        .width((360 * zoom).dp)
                        .shadow(4.dp)
                        .background(Color.White)
                )
                Text("Page ${index + 1} of ${pages.size}", fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

// ============================================================================================
// STATUS BAR
// ============================================================================================

@Composable
private fun StatusBar(doc: WordDocument, zoom: Float, textColor: Color, ribbonStripBg: Color, onZoomChange: (Float) -> Unit) {
    val paragraphs = doc.blocks.filterIsInstance<ParagraphBlock>()
    val wordCount = paragraphs.sumOf { p -> p.field.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
    val charCount = paragraphs.sumOf { it.field.text.length }
    Row(
        modifier = Modifier.fillMaxWidth().background(ribbonStripBg).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (doc.kind == DocKind.PDF) "PDF document · read-only"
            else "$wordCount words · $charCount characters · ${paragraphs.size} paragraphs",
            color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ZoomOut, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier
            .size(14.dp)
            .clickable { onZoomChange((zoom - 0.1f).coerceAtLeast(0.6f)) })
        Slider(
            value = zoom, onValueChange = onZoomChange, valueRange = 0.6f..2f,
            modifier = Modifier.width(90.dp).padding(horizontal = 4.dp)
        )
        Icon(Icons.Default.ZoomIn, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier
            .size(14.dp)
            .clickable { onZoomChange((zoom + 0.1f).coerceAtMost(2f)) })
        Spacer(Modifier.width(6.dp))
        Text("${(zoom * 100).toInt()}%", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

// ============================================================================================
// SAVE AS DIALOG
// ============================================================================================

private enum class SaveFormat { WDOC, TXT, PDF }

@Composable
private fun SaveAsDialog(currentTitle: String, onDismiss: () -> Unit, onConfirm: (String, SaveFormat) -> Unit) {
    var name by remember { mutableStateOf(currentTitle) }
    var format by remember { mutableStateOf(SaveFormat.WDOC) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save As") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("File name") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Text("Format", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Column {
                    listOf(
                        SaveFormat.WDOC to "Word Impress Document (.wdoc)",
                        SaveFormat.TXT to "Plain Text (.txt)",
                        SaveFormat.PDF to "PDF Document (.pdf)"
                    ).forEach { (f, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { format = f }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = format == f, onClick = { format = f })
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, format) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun InsertTableDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var rows by remember { mutableStateOf("3") }
    var cols by remember { mutableStateOf("3") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Table") },
        text = {
            Column {
                OutlinedTextField(
                    value = rows, onValueChange = { rows = it.filter(Char::isDigit) },
                    label = { Text("Rows") }, singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cols, onValueChange = { cols = it.filter(Char::isDigit) },
                    label = { Text("Columns") }, singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm((rows.toIntOrNull() ?: 1).coerceIn(1, 20), (cols.toIntOrNull() ?: 1).coerceIn(1, 10))
            }) { Text("Insert") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================================================================
// BACKSTAGE (File menu)
// ============================================================================================

@Composable
private fun FileBackstage(
    isDark: Boolean, textColor: Color, bg: Color, documents: List<WordDocument>, currentIndex: Int,
    onSelectDoc: (Int) -> Unit, onNew: () -> Unit, onOpen: () -> Unit, onOpenPdf: () -> Unit,
    onSave: () -> Unit, onSaveAs: () -> Unit, onExportPdf: () -> Unit, onClose: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp).background(bg)) {
        Column(
            modifier = Modifier.width(200.dp).fillMaxHeight()
                .background(if (isDark) WordTheme.ribbonBlueDark else WordTheme.ribbonBlue)
                .padding(vertical = 8.dp)
        ) {
            BackstageAction(Icons.Default.NoteAdd, "New", onNew)
            BackstageAction(Icons.Default.FolderOpen, "Open…", onOpen)
            BackstageAction(Icons.Default.PictureAsPdf, "Open PDF…", onOpenPdf)
            BackstageAction(Icons.Default.Save, "Save", onSave)
            BackstageAction(Icons.Default.SaveAs, "Save As…", onSaveAs)
            BackstageAction(Icons.Default.PictureAsPdf, "Export as PDF…", onExportPdf)
            Spacer(Modifier.weight(1f))
            BackstageAction(Icons.Default.Close, "Close", onClose)
        }
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Text("Recent", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            documents.forEachIndexed { i, doc ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectDoc(i) }
                        .background(if (i == currentIndex) Win11Colors.AccentBlue.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (doc.kind == DocKind.PDF) Icons.Default.PictureAsPdf else Icons.Default.Description,
                        null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(doc.title, color = textColor, fontSize = 13.sp)
                        Text(
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(doc.lastModified)),
                            color = textColor.copy(alpha = 0.5f), fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackstageAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}

// ============================================================================================
// HOME RIBBON — style, font, bold/italic/underline, color, alignment, lists, indent
// ============================================================================================

@Composable
private fun HomeRibbon(
    enabled: Boolean, typingOrSelectionStyle: StyleAttrs, alignment: TextAlign,
    currentStyleId: String, listType: ListType?,
    onBold: () -> Unit, onItalic: () -> Unit, onUnderline: () -> Unit,
    onFontSizeChange: (Int) -> Unit, onColorChange: (Color) -> Unit, onFontChange: (FontChoice) -> Unit,
    onAlignChange: (TextAlign) -> Unit, onBullet: () -> Unit, onNumbered: () -> Unit,
    onIndentIncrease: () -> Unit, onIndentDecrease: () -> Unit, onStyleChange: (String) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Paragraph style
        Box {
            RibbonChip(BuiltInStyles.byId(currentStyleId).name, enabled) { showStyleMenu = true }
            DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                BuiltInStyles.ALL.forEach { st ->
                    DropdownMenuItem(
                        text = { Text(st.name, fontWeight = if (st.bold) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onStyleChange(st.id); showStyleMenu = false }
                    )
                }
            }
        }

        RibbonDivider()

        // Font family
        Box {
            RibbonChip(typingOrSelectionStyle.font.label, enabled) { showFontMenu = true }
            DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                FontChoice.entries.forEach { f ->
                    DropdownMenuItem(text = { Text(f.label, fontFamily = f.family) }, onClick = { onFontChange(f); showFontMenu = false })
                }
            }
        }

        // Font size stepper
        RibbonIconButton(Icons.Default.Remove, enabled) { onFontSizeChange((typingOrSelectionStyle.fontSize - 1).coerceAtLeast(8)) }
        RibbonChip("${typingOrSelectionStyle.fontSize}", enabled) {}
        RibbonIconButton(Icons.Default.Add, enabled) { onFontSizeChange((typingOrSelectionStyle.fontSize + 1).coerceAtMost(96)) }

        RibbonDivider()

        RibbonToggleIcon(Icons.Default.FormatBold, typingOrSelectionStyle.bold, enabled, onBold)
        RibbonToggleIcon(Icons.Default.FormatItalic, typingOrSelectionStyle.italic, enabled, onItalic)
        RibbonToggleIcon(Icons.Default.FormatUnderlined, typingOrSelectionStyle.underline, enabled, onUnderline)

        Box {
            Box(
                modifier = Modifier.size(32.dp).padding(4.dp).clip(RoundedCornerShape(4.dp))
                    .background(typingOrSelectionStyle.color)
                    .clickable(enabled = enabled) { showColorPicker = true }
            )
            DropdownMenu(expanded = showColorPicker, onDismissRequest = { showColorPicker = false }) {
                Row(Modifier.padding(8.dp)) {
                    PRESET_COLORS.forEach { c ->
                        Box(
                            modifier = Modifier.size(24.dp).padding(2.dp).clip(CircleShape).background(c)
                                .clickable { onColorChange(c); showColorPicker = false }
                        )
                    }
                }
            }
        }

        RibbonDivider()

        RibbonToggleIcon(Icons.Default.FormatAlignLeft, alignment == TextAlign.Start, enabled) { onAlignChange(TextAlign.Start) }
        RibbonToggleIcon(Icons.Default.FormatAlignCenter, alignment == TextAlign.Center, enabled) { onAlignChange(TextAlign.Center) }
        RibbonToggleIcon(Icons.Default.FormatAlignRight, alignment == TextAlign.End, enabled) { onAlignChange(TextAlign.End) }
        RibbonToggleIcon(Icons.Default.FormatAlignJustify, alignment == TextAlign.Justify, enabled) { onAlignChange(TextAlign.Justify) }

        RibbonDivider()

        RibbonToggleIcon(Icons.Default.FormatListBulleted, listType == ListType.BULLET, enabled, onBullet)
        RibbonToggleIcon(Icons.Default.FormatListNumbered, listType == ListType.NUMBER, enabled, onNumbered)
        RibbonIconButton(Icons.Default.FormatIndentDecrease, enabled) { onIndentDecrease() }
        RibbonIconButton(Icons.Default.FormatIndentIncrease, enabled) { onIndentIncrease() }
    }
}

@Composable
private fun InsertRibbon(
    onInsertDate: () -> Unit, onInsertDivider: () -> Unit, onInsertTable: () -> Unit,
    onInsertImage: () -> Unit, onInsertPageBreak: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RibbonLabeledAction(Icons.Default.Image, "Picture", onInsertImage)
        RibbonLabeledAction(Icons.Default.TableChart, "Table", onInsertTable)
        // If InsertPageBreak isn't available in your Material Icons version, swap for another icon.
        RibbonLabeledAction(Icons.Default.InsertPageBreak, "Page Break", onInsertPageBreak)
        RibbonLabeledAction(Icons.Default.CalendarToday, "Date", onInsertDate)
        RibbonLabeledAction(Icons.Default.HorizontalRule, "Divider", onInsertDivider)
    }
}

@Composable
private fun ViewRibbon(sidebarOpen: Boolean, zoom: Float, onToggleSidebar: () -> Unit, onZoomChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RibbonToggleIcon(Icons.Default.ViewSidebar, sidebarOpen, true) { onToggleSidebar() }
        Text("Zoom", fontSize = 12.sp)
        Slider(
            value = zoom, onValueChange = onZoomChange, valueRange = 0.6f..2f,
            modifier = Modifier.width(160.dp)
        )
        Text("${(zoom * 100).toInt()}%", fontSize = 12.sp)
    }
}

@Composable private fun RibbonDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Color.Gray.copy(alpha = 0.3f)))
}

@Composable
private fun RibbonChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.12f))
            .clickable(enabled = enabled) { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun RibbonIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RibbonToggleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
            .background(if (selected) Win11Colors.AccentBlue.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (selected) Win11Colors.AccentBlue else LocalContentColor.current, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RibbonLabeledAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp)
    }
}

// ============================================================================================
// SIDEBAR
// ============================================================================================

@Composable
private fun DocumentSidebar(
    documents: List<WordDocument>, currentIndex: Int, isDark: Boolean, textColor: Color,
    onSelect: (Int) -> Unit, onNew: () -> Unit, onDelete: (Int) -> Unit
) {
    Column(
        modifier = Modifier.width(220.dp).fillMaxHeight()
            .background(if (isDark) WordTheme.darkSurface else Color(0xFFF3F2F1))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Documents", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onNew, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(documents.size) { i ->
                val doc = documents[i]
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(if (i == currentIndex) Win11Colors.AccentBlue.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (doc.kind == DocKind.PDF) Icons.Default.PictureAsPdf else Icons.Default.Description,
                        null, tint = Win11Colors.AccentBlue, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        doc.title, color = textColor, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDelete(i) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

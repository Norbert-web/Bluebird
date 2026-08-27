package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// WdocIO.kt — everything that turns a WordDocument into bytes and back.
//
// .wdoc v2 is a real ZIP package (matches the original spec discussion):
//   metadata.json   — format/version/title/author/created/modified
//   settings.json   — page setup, header/footer visibility
//   styles.json     — the named style registry (built-ins today; forward-compatible with
//                      custom styles later)
//   document.json   — sections -> blocks -> paragraph/runs, image, table, pageBreak, toc
//   media/image_NNN.png — image bytes, referenced by filename from document.json
//
// Old single-JSON .wdoc files (from before packaging) and plain-text files both still open
// correctly via [openAnyDocument] — it sniffs the "PK" zip signature first, then falls back.
// ============================================================================================

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ---- small JSON helpers ----------------------------------------------------------------------

private fun styleAttrsToJson(s: StyleAttrs) = JSONObject().apply {
    put("bold", s.bold); put("italic", s.italic); put("underline", s.underline)
    put("strike", s.strikethrough); put("super", s.superscript); put("sub", s.subscript)
    put("size", s.fontSize); put("color", s.color.toArgb()); put("font", s.font.name)
    s.highlight?.let { put("highlight", it.toArgb()) }
    s.link?.let { put("link", it) }
}

private fun styleAttrsFromJson(o: JSONObject, fallback: StyleAttrs) = StyleAttrs(
    bold = o.optBoolean("bold", fallback.bold),
    italic = o.optBoolean("italic", fallback.italic),
    underline = o.optBoolean("underline", fallback.underline),
    strikethrough = o.optBoolean("strike", fallback.strikethrough),
    superscript = o.optBoolean("super", fallback.superscript),
    subscript = o.optBoolean("sub", fallback.subscript),
    fontSize = o.optInt("size", fallback.fontSize),
    color = Color(o.optInt("color", fallback.color.toArgb())),
    font = FontChoice.entries.firstOrNull { it.name == o.optString("font", fallback.font.name) } ?: fallback.font,
    highlight = if (o.has("highlight")) Color(o.optInt("highlight")) else fallback.highlight,
    link = if (o.has("link")) o.optString("link") else fallback.link
)

fun textAlignToString(a: TextAlign): String = when (a) {
    TextAlign.Center -> "Center"; TextAlign.End -> "End"; TextAlign.Justify -> "Justify"; else -> "Start"
}

fun textAlignFromString(s: String?): TextAlign? = when (s) {
    "Center" -> TextAlign.Center; "End" -> TextAlign.End; "Justify" -> TextAlign.Justify; "Start" -> TextAlign.Start
    else -> null
}

private fun pageSettingsToJson(p: PageSettings) = JSONObject().apply {
    put("sizeId", p.sizeId); put("orientation", p.orientation)
    put("customWidthPt", p.customWidthPt.toDouble()); put("customHeightPt", p.customHeightPt.toDouble())
    put("marginTopPt", p.marginTopPt.toDouble()); put("marginBottomPt", p.marginBottomPt.toDouble())
    put("marginLeftPt", p.marginLeftPt.toDouble()); put("marginRightPt", p.marginRightPt.toDouble())
}

private fun pageSettingsFromJson(o: JSONObject) = PageSettings(
    sizeId = o.optString("sizeId", "A4"),
    orientation = o.optString("orientation", "portrait"),
    customWidthPt = o.optDouble("customWidthPt", 595.0).toFloat(),
    customHeightPt = o.optDouble("customHeightPt", 842.0).toFloat(),
    marginTopPt = o.optDouble("marginTopPt", 72.0).toFloat(),
    marginBottomPt = o.optDouble("marginBottomPt", 72.0).toFloat(),
    marginLeftPt = o.optDouble("marginLeftPt", 72.0).toFloat(),
    marginRightPt = o.optDouble("marginRightPt", 72.0).toFloat()
)

// ---- paragraph <-> json ------------------------------------------------------------------------

fun paragraphToJson(p: ParagraphBlock): JSONObject = JSONObject().apply {
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

fun paragraphFromJson(o: JSONObject): ParagraphBlock {
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

// ---- block <-> json (mediaNames maps ImageBlock.id -> "image_NNN.png" inside media/) ----------

fun blockToJson(b: Block, mediaNames: Map<String, String> = emptyMap()): JSONObject = when (b) {
    is ParagraphBlock -> paragraphToJson(b)
    is ImageBlock -> JSONObject().apply {
        put("type", "image")
        put("media", mediaNames[b.id] ?: "")
        // Legacy fallback so a plain (non-zip) export/import still carries the image inline.
        if (mediaNames[b.id] == null) put("data", b.base64 ?: "")
        put("widthDp", b.widthDp)
        put("alignment", textAlignToString(b.alignment))
        put("rotation", b.rotationDeg)
    }
    is TableBlock -> JSONObject().apply {
        put("type", "table")
        val rowsArr = JSONArray()
        for (row in b.rows) {
            val cellsArr = JSONArray()
            for (cell in row.cells) {
                val blocksArr = JSONArray()
                for (cb in cell.blocks) blocksArr.put(paragraphToJson(cb))
                cellsArr.put(JSONObject().apply {
                    put("blocks", blocksArr)
                    cell.backgroundColor?.let { put("bg", it.toArgb()) }
                })
            }
            rowsArr.put(JSONObject().apply { put("cells", cellsArr) })
        }
        put("rows", rowsArr)
    }
    is PageBreakBlock -> JSONObject().apply { put("type", "pageBreak") }
    is TocBlock -> JSONObject().apply {
        put("type", "toc")
        val arr = JSONArray()
        b.entries.forEach { e -> arr.put(JSONObject().apply { put("text", e.text); put("level", e.level); put("targetBlockId", e.targetBlockId) }) }
        put("entries", arr)
    }
}

fun blockFromJson(o: JSONObject, mediaEntries: Map<String, ByteArray> = emptyMap()): Block = when (o.optString("type")) {
    "image" -> ImageBlock().apply {
        widthDp = o.optInt("widthDp", 300)
        alignment = textAlignFromString(o.optString("alignment")) ?: TextAlign.Center
        rotationDeg = o.optInt("rotation", 0)
        val mediaName = o.optString("media", "")
        val mediaBytes = mediaEntries["media/$mediaName"]
        val bytes = mediaBytes ?: o.optString("data", "").ifEmpty { null }?.let {
            try { Base64.decode(it, Base64.DEFAULT) } catch (_: Exception) { null }
        }
        if (bytes != null) {
            base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
            try { bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } catch (_: Exception) { /* corrupt image data */ }
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
                if (cellObj.has("bg")) cell.backgroundColor = Color(cellObj.optInt("bg"))
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
    "toc" -> TocBlock().apply {
        val arr = o.optJSONArray("entries") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            entries.add(TocEntry(e.optString("text"), e.optInt("level"), e.optString("targetBlockId")))
        }
    }
    else -> paragraphFromJson(o)
}

// ---- zip packaging --------------------------------------------------------------------------

private fun collectImageBlocks(blocks: List<Block>, out: MutableList<ImageBlock>) {
    for (b in blocks) when (b) {
        is ImageBlock -> out.add(b)
        is TableBlock -> b.rows.forEach { row -> row.cells.forEach { collectImageBlocks(it.blocks, out) } }
        else -> {}
    }
}

fun serializeDocumentZip(doc: WordDocument): ByteArray {
    val bos = ByteArrayOutputStream()
    ZipOutputStream(bos).use { zos ->
        val metadata = JSONObject().apply {
            put("format", "wdoc"); put("version", 2)
            put("title", doc.title); put("author", doc.author)
            put("created", doc.created); put("modified", doc.lastModified)
        }
        zos.putNextEntry(ZipEntry("metadata.json")); zos.write(metadata.toString().toByteArray()); zos.closeEntry()

        val settings = JSONObject().apply {
            put("page", pageSettingsToJson(doc.pageSettings))
            put("showHeader", doc.showHeader); put("showFooter", doc.showFooter)
        }
        zos.putNextEntry(ZipEntry("settings.json")); zos.write(settings.toString().toByteArray()); zos.closeEntry()

        val stylesArr = JSONArray()
        BuiltInStyles.ALL.forEach { s ->
            stylesArr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("font", s.font.name); put("size", s.fontSize)
                put("bold", s.bold); put("italic", s.italic); put("color", s.color.toArgb())
                put("alignment", textAlignToString(s.alignment))
                put("spacingBefore", s.spacingBefore); put("spacingAfter", s.spacingAfter)
            })
        }
        zos.putNextEntry(ZipEntry("styles.json"))
        zos.write(JSONObject().put("styles", stylesArr).toString().toByteArray())
        zos.closeEntry()

        val images = mutableListOf<ImageBlock>()
        collectImageBlocks(doc.blocks, images)
        val mediaNames = mutableMapOf<String, String>()
        images.forEachIndexed { i, img ->
            val b64 = img.base64 ?: return@forEachIndexed
            val name = "image_%03d.png".format(i + 1)
            mediaNames[img.id] = name
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                zos.putNextEntry(ZipEntry("media/$name"))
                zos.write(bytes)
                zos.closeEntry()
            } catch (_: Exception) { /* skip a corrupt image rather than fail the whole save */ }
        }

        val blocksArr = JSONArray()
        for (b in doc.blocks) blocksArr.put(blockToJson(b, mediaNames))
        val document = JSONObject().apply {
            put("header", paragraphToJson(doc.headerParagraph))
            put("footer", paragraphToJson(doc.footerParagraph))
            put("sections", JSONArray().put(JSONObject().apply { put("blocks", blocksArr) }))
        }
        zos.putNextEntry(ZipEntry("document.json")); zos.write(document.toString().toByteArray()); zos.closeEntry()
    }
    return bos.toByteArray()
}

private fun readZipEntries(bytes: ByteArray): Map<String, ByteArray> {
    val map = mutableMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) map[entry.name] = zis.readBytes()
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    return map
}

private fun parseDocumentZip(bytes: ByteArray, fallbackTitle: String): WordDocument {
    val entries = readZipEntries(bytes)
    val doc = WordDocument(fallbackTitle)
    entries["metadata.json"]?.let {
        val m = JSONObject(String(it))
        doc.title = m.optString("title", fallbackTitle)
        doc.author = m.optString("author", "")
        doc.created = m.optLong("created", System.currentTimeMillis())
    }
    entries["settings.json"]?.let {
        val s = JSONObject(String(it))
        s.optJSONObject("page")?.let { pj -> doc.pageSettings = pageSettingsFromJson(pj) }
        doc.showHeader = s.optBoolean("showHeader", false)
        doc.showFooter = s.optBoolean("showFooter", false)
    }
    entries["document.json"]?.let {
        val d = JSONObject(String(it))
        d.optJSONObject("header")?.let { h -> doc.headerParagraph.copyFrom(paragraphFromJson(h)) }
        d.optJSONObject("footer")?.let { f -> doc.footerParagraph.copyFrom(paragraphFromJson(f)) }
        val blocksArr = d.optJSONArray("sections")?.optJSONObject(0)?.optJSONArray("blocks")
        if (blocksArr != null && blocksArr.length() > 0) {
            doc.blocks.clear()
            for (i in 0 until blocksArr.length()) doc.blocks.add(blockFromJson(blocksArr.getJSONObject(i), entries))
        }
    }
    return doc
}

/** Opens a .wdoc (zip or legacy single-JSON) or a plain-text file, detected from its bytes. */
fun openAnyDocument(bytes: ByteArray, fallbackTitle: String): WordDocument {
    if (bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
        return try { parseDocumentZip(bytes, fallbackTitle) } catch (e: Exception) { WordDocument(fallbackTitle) }
    }
    val text = String(bytes, Charsets.UTF_8)
    try {
        val root = JSONObject(text)
        if (root.optString("format") == "wdoc") {
            val doc = WordDocument(root.optJSONObject("metadata")?.optString("title", fallbackTitle) ?: fallbackTitle)
            val blocksArr = root.optJSONObject("document")?.optJSONArray("sections")?.optJSONObject(0)?.optJSONArray("blocks")
            if (blocksArr != null && blocksArr.length() > 0) {
                doc.blocks.clear()
                for (i in 0 until blocksArr.length()) doc.blocks.add(blockFromJson(blocksArr.getJSONObject(i)))
            }
            return doc
        }
    } catch (_: Exception) { /* not JSON — plain text import */ }
    val doc = WordDocument(fallbackTitle)
    doc.blocks.clear()
    doc.blocks.add(ParagraphBlock().apply { field = TextFieldValue(text) })
    return doc
}

fun plainTextOf(doc: WordDocument): String = doc.blocks.joinToString("\n\n") { b ->
    when (b) {
        is ParagraphBlock -> b.field.text
        is ImageBlock -> "[image]"
        is TableBlock -> b.rows.joinToString("\n") { row -> row.cells.joinToString(" | ") { c -> c.blocks.joinToString(" ") { it.field.text } } }
        is PageBreakBlock -> "---- page break ----"
        is TocBlock -> "Table of Contents:\n" + b.entries.joinToString("\n") { "  ".repeat(it.level) + it.text }
    }
}

// ---- duplicate / deep copy --------------------------------------------------------------------

private fun deepCopyParagraph(p: ParagraphBlock): ParagraphBlock = ParagraphBlock().apply {
    styleId = p.styleId; alignmentOverride = p.alignmentOverride
    listType = p.listType; listLevel = p.listLevel
    field = TextFieldValue(p.field.text, TextRange(p.field.text.length))
    spans = p.spans
    typingStyle = p.typingStyle
}

private fun deepCopyBlock(b: Block): Block = when (b) {
    is ParagraphBlock -> deepCopyParagraph(b)
    is ImageBlock -> ImageBlock().apply {
        bitmap = b.bitmap; base64 = b.base64; widthDp = b.widthDp; alignment = b.alignment; rotationDeg = b.rotationDeg
    }
    is TableBlock -> TableBlock().apply {
        b.rows.forEach { row ->
            val newRow = TableRow()
            row.cells.forEach { cell ->
                val newCell = TableCell().apply { backgroundColor = cell.backgroundColor }
                cell.blocks.forEach { newCell.blocks.add(deepCopyParagraph(it)) }
                newRow.cells.add(newCell)
            }
            rows.add(newRow)
        }
    }
    is PageBreakBlock -> PageBreakBlock()
    is TocBlock -> TocBlock().apply { entries.addAll(b.entries) }
}

/** "Save a Copy": an independent clone of [source], unsaved (no file uri yet). */
fun duplicateDocument(source: WordDocument): WordDocument {
    val clone = WordDocument("${source.title} Copy", source.pageSettings)
    clone.author = source.author
    clone.blocks.clear()
    source.blocks.forEach { clone.blocks.add(deepCopyBlock(it)) }
    clone.headerParagraph.copyFrom(deepCopyParagraph(source.headerParagraph))
    clone.footerParagraph.copyFrom(deepCopyParagraph(source.footerParagraph))
    clone.showHeader = source.showHeader
    clone.showFooter = source.showFooter
    return clone
}

// ---- undo / redo (bounded snapshot stack over the block list) ---------------------------------

private const val MAX_UNDO_DEPTH = 40

private fun snapshotBlocksJson(doc: WordDocument): String {
    val arr = JSONArray()
    for (b in doc.blocks) arr.put(blockToJson(b))
    return arr.toString()
}

private fun restoreBlocksFromJson(doc: WordDocument, json: String) {
    try {
        val arr = JSONArray(json)
        doc.blocks.clear()
        for (i in 0 until arr.length()) doc.blocks.add(blockFromJson(arr.getJSONObject(i)))
    } catch (_: Exception) { /* corrupt snapshot — leave the document as-is */ }
}

/** Call before any meaningfully distinct edit (a formatting toggle, insert, delete, ...). */
fun WordDocument.pushUndoSnapshot() {
    undoStack.add(snapshotBlocksJson(this))
    if (undoStack.size > MAX_UNDO_DEPTH) undoStack.removeAt(0)
    redoStack.clear()
}

/** Returns true if an undo was performed. Caller should clear any paragraph-identity-based focus state after. */
fun WordDocument.undo(): Boolean {
    if (undoStack.isEmpty()) return false
    redoStack.add(snapshotBlocksJson(this))
    val prev = undoStack.removeAt(undoStack.lastIndex)
    restoreBlocksFromJson(this, prev)
    isDirty = true
    return true
}

fun WordDocument.redo(): Boolean {
    if (redoStack.isEmpty()) return false
    undoStack.add(snapshotBlocksJson(this))
    val next = redoStack.removeAt(redoStack.lastIndex)
    restoreBlocksFromJson(this, next)
    isDirty = true
    return true
}

// ---- crash-recovery drafts ---------------------------------------------------------------------
// A lightweight safety net: every autosave tick also writes the live in-memory document to local
// app storage (independent of whether the user has picked a save location yet). On a clean
// Save/Close we delete the draft; anything left behind at the next cold start means the app didn't
// shut down cleanly, so we offer to restore it.

private fun draftsDir(context: Context): File = File(context.filesDir, "wdoc_drafts").apply { mkdirs() }

fun draftFile(context: Context, docId: String): File = File(draftsDir(context), "$docId.wdoc")

fun saveDraft(context: Context, doc: WordDocument) {
    try { draftFile(context, doc.id).writeBytes(serializeDocumentZip(doc)) } catch (_: Exception) { }
}

fun deleteDraft(context: Context, docId: String) {
    try { draftFile(context, docId).delete() } catch (_: Exception) { }
}

fun listDraftFiles(context: Context): List<File> = draftsDir(context).listFiles()?.toList() ?: emptyList()

fun loadDraftAsDocument(file: File): WordDocument? =
    try { openAnyDocument(file.readBytes(), file.nameWithoutExtension) } catch (_: Exception) { null }

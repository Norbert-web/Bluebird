package io.github.norbertweb.bluebird.wordprocessor

// ============================================================================================
// WdocModel.kt — the document's data model.
//
// A document is a flat list of BLOCKS (ParagraphBlock, ImageBlock, TableBlock, PageBreakBlock,
// TocBlock). Blocks are plain observable classes (var ... by mutableStateOf) rather than
// immutable data classes, because the tree can nest (table -> row -> cell -> paragraphs) and
// copy-on-write through that nesting gets unwieldy fast. Structural changes (add/remove/reorder
// blocks, rows, cells) go through SnapshotStateList so Compose still recomposes correctly.
// ============================================================================================

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import java.util.UUID

// ---- Character formatting -------------------------------------------------------------------

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
    val strikethrough: Boolean = false,
    val superscript: Boolean = false,
    val subscript: Boolean = false,
    val fontSize: Int = 16,
    val color: Color = Color(0xFF1A1A1A),
    val font: FontChoice = FontChoice.CALIBRI,
    val highlight: Color? = null,
    val link: String? = null
)

data class FormatRange(val start: Int, val end: Int, val style: StyleAttrs)

enum class DocKind { RICH_TEXT, PDF }
enum class ListType { BULLET, NUMBER }

// ---- Named paragraph styles -------------------------------------------------------------------

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
    val HEADING1 = DocStyle("heading1", "Heading 1", fontSize = 24, bold = true, color = Color(0xFF2B579A), spacingBefore = 14, spacingAfter = 6)
    val HEADING2 = DocStyle("heading2", "Heading 2", fontSize = 20, bold = true, color = Color(0xFF2B579A), spacingBefore = 10, spacingAfter = 6)
    val HEADING3 = DocStyle("heading3", "Heading 3", fontSize = 17, bold = true, spacingBefore = 8, spacingAfter = 4)
    val QUOTE = DocStyle("quote", "Quote", fontSize = 16, italic = true, color = Color(0xFF595959), spacingBefore = 6, spacingAfter = 6)
    val CAPTION = DocStyle("caption", "Caption", fontSize = 12, italic = true, color = Color(0xFF595959), spacingAfter = 6)

    val ALL = listOf(NORMAL, TITLE, SUBTITLE, HEADING1, HEADING2, HEADING3, QUOTE, CAPTION)
    val HEADING_IDS = setOf("title", "subtitle", "heading1", "heading2", "heading3")

    fun byId(id: String): DocStyle = ALL.firstOrNull { it.id == id } ?: NORMAL
}

// ---- The block tree -----------------------------------------------------------------------

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

/** Copies another paragraph's content into this (already-constructed) paragraph, in place. */
fun ParagraphBlock.copyFrom(other: ParagraphBlock) {
    styleId = other.styleId
    alignmentOverride = other.alignmentOverride
    listType = other.listType
    listLevel = other.listLevel
    field = other.field
    spans = other.spans
    typingStyle = other.typingStyle
}

class ImageBlock(override val id: String = UUID.randomUUID().toString()) : Block() {
    var bitmap by mutableStateOf<Bitmap?>(null)
    var base64 by mutableStateOf<String?>(null)
    var widthDp by mutableStateOf(300)
    var alignment by mutableStateOf(TextAlign.Center)
    var rotationDeg by mutableStateOf(0)
}

class TableCell {
    val blocks = mutableStateListOf<ParagraphBlock>()
    var backgroundColor by mutableStateOf<Color?>(null)
}

class TableRow {
    val cells = mutableStateListOf<TableCell>()
}

class TableBlock(override val id: String = UUID.randomUUID().toString()) : Block() {
    val rows = mutableStateListOf<TableRow>()
}

class PageBreakBlock(override val id: String = UUID.randomUUID().toString()) : Block()

data class TocEntry(val text: String, val level: Int, val targetBlockId: String)

/** A generated (static, "Update Field"-style) table of contents snapshot. */
class TocBlock(override val id: String = UUID.randomUUID().toString()) : Block() {
    val entries = mutableStateListOf<TocEntry>()
}

fun newTable(rowCount: Int, colCount: Int): TableBlock {
    val t = TableBlock()
    repeat(rowCount) {
        val row = TableRow()
        repeat(colCount) { row.cells.add(TableCell().apply { blocks.add(ParagraphBlock()) }) }
        t.rows.add(row)
    }
    return t
}

/** Regenerates a TOC from the document's current heading paragraphs (Heading 1-3, Title). */
fun buildTocEntries(blocks: List<Block>): List<TocEntry> =
    blocks.filterIsInstance<ParagraphBlock>()
        .filter { it.styleId in setOf("title", "heading1", "heading2", "heading3") }
        .map { p ->
            val level = when (p.styleId) { "title" -> 0; "heading1" -> 1; "heading2" -> 2; else -> 3 }
            TocEntry(text = p.field.text.ifBlank { "(untitled)" }, level = level, targetBlockId = p.id)
        }

// ---- Page setup -----------------------------------------------------------------------------

/** All page-geometry values are in points (1/72 inch) — matches PDF export units directly. */
data class PageSettings(
    val sizeId: String = "A4", // A4, LETTER, LEGAL, CUSTOM
    val orientation: String = "portrait", // portrait, landscape
    val customWidthPt: Float = 595f,
    val customHeightPt: Float = 842f,
    val marginTopPt: Float = 72f,
    val marginBottomPt: Float = 72f,
    val marginLeftPt: Float = 72f,
    val marginRightPt: Float = 72f
) {
    fun dimensionsPt(): Pair<Float, Float> {
        val (w, h) = when (sizeId) {
            "LETTER" -> 612f to 792f
            "LEGAL" -> 612f to 1008f
            "CUSTOM" -> customWidthPt to customHeightPt
            else -> 595f to 842f // A4
        }
        return if (orientation == "landscape") h to w else w to h
    }
}

data class Bookmark(val id: String = UUID.randomUUID().toString(), var name: String, var blockId: String)

/** App-wide preferences (defaults for new documents, autosave behavior). Lives for the process lifetime. */
class AppSettings {
    var autosaveEnabled by mutableStateOf(true)
    var autosaveIntervalSec by mutableStateOf(30)
    var defaultFont by mutableStateOf(FontChoice.CALIBRI)
    var defaultFontSize by mutableStateOf(16)
    var defaultPageSettings by mutableStateOf(PageSettings())
}

// ---- The document itself --------------------------------------------------------------------

class WordDocument(title: String, pageDefaults: PageSettings = PageSettings()) {
    val id: String = UUID.randomUUID().toString()
    var title by mutableStateOf(title)
    val blocks = mutableStateListOf<Block>(ParagraphBlock())
    var kind by mutableStateOf(DocKind.RICH_TEXT)
    var pdfUri by mutableStateOf<Uri?>(null)
    var savedUri by mutableStateOf<Uri?>(null)
    var lastModified by mutableStateOf(System.currentTimeMillis())
    var created by mutableStateOf(System.currentTimeMillis())
    var author by mutableStateOf("")
    var isDirty by mutableStateOf(false)
    var readOnly by mutableStateOf(false)
    var pageSettings by mutableStateOf(pageDefaults)

    var showHeader by mutableStateOf(false)
    var showFooter by mutableStateOf(false)
    val headerParagraph = ParagraphBlock()
    val footerParagraph = ParagraphBlock()

    val bookmarks = mutableStateListOf<Bookmark>()

    // Bounded undo/redo history — each entry is a serialized snapshot of `blocks` (see WdocIO.kt).
    val undoStack = mutableStateListOf<String>()
    val redoStack = mutableStateListOf<String>()
    var lastEditSnapshotAt by mutableStateOf(0L)
}

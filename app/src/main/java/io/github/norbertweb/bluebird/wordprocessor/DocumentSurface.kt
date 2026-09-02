package io.github.norbertweb.bluebird.wordprocessor

import androidx.compose.ui.text.TextRange

/**
 * Logical document positions are independent of the individual Compose text fields used to
 * render paragraphs. This is the bridge that lets the editor behave like one continuous Word
 * document while keeping the existing rich block renderer.
 */
data class DocumentSelection(
    val startBlockId: String,
    val startOffset: Int,
    val endBlockId: String,
    val endOffset: Int
) {
    val collapsed: Boolean get() = startBlockId == endBlockId && startOffset == endOffset
}

data class DocumentPosition(val blockId: String, val offset: Int)

class DocumentTextSurface(private val doc: WordDocument) {
    fun paragraphIndex(id: String): Int = doc.blocks.indexOfFirst { it.id == id }

    fun normalize(selection: DocumentSelection): DocumentSelection? {
        val a = paragraphIndex(selection.startBlockId)
        val b = paragraphIndex(selection.endBlockId)
        if (a < 0 || b < 0) return null
        return if (a < b || (a == b && selection.startOffset <= selection.endOffset)) selection
        else DocumentSelection(selection.endBlockId, selection.endOffset, selection.startBlockId, selection.startOffset)
    }

    fun paragraphRange(selection: DocumentSelection): Pair<Int, Int>? {
        val normalized = normalize(selection) ?: return null
        val a = paragraphIndex(normalized.startBlockId)
        val b = paragraphIndex(normalized.endBlockId)
        return if (a >= 0 && b >= 0) a to b else null
    }

    /** A linear text projection used for keyboard/clipboard semantics. Paragraph breaks are \n. */
    fun linearText(): String = doc.blocks.filterIsInstance<ParagraphBlock>().joinToString("\n") { it.field.text }

    fun selectedText(selection: DocumentSelection): String {
        val normalized = normalize(selection) ?: return ""
        val (a, b) = paragraphRange(normalized) ?: return ""
        val paragraphs = doc.blocks.subList(a, b + 1).filterIsInstance<ParagraphBlock>()
        if (paragraphs.isEmpty()) return ""
        return paragraphs.mapIndexed { i, p ->
            val start = if (i == 0) normalized.startOffset else 0
            val end = if (i == paragraphs.lastIndex) normalized.endOffset else p.field.text.length
            p.field.text.substring(start.coerceIn(0, p.field.text.length), end.coerceIn(0, p.field.text.length))
        }.joinToString("\n")
    }

    fun clamp(position: DocumentPosition): DocumentPosition {
        val p = doc.blocks.firstOrNull { it.id == position.blockId } as? ParagraphBlock
            ?: return position
        return DocumentPosition(p.id, position.offset.coerceIn(0, p.field.text.length))
    }

    /** Maps a document position into the linear text projection. */
    fun toLinearOffset(position: DocumentPosition): Int? {
        val p = doc.blocks.indexOfFirst { it.id == position.blockId }
        if (p < 0) return null
        var offset = 0
        doc.blocks.take(p).filterIsInstance<ParagraphBlock>().forEach {
            offset += it.field.text.length + 1
        }
        val paragraph = doc.blocks[p] as? ParagraphBlock ?: return null
        return offset + position.offset.coerceIn(0, paragraph.field.text.length)
    }
}

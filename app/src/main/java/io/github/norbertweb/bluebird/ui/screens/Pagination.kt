package io.github.norbertweb.bluebird.ui.screens

// ============================================================================================
// Pagination.kt — turns the flat `doc.blocks` list into physical pages.
//
// NOTE ON ACCURACY: block heights are ESTIMATED (average character width for text wrapping,
// fixed row heights for tables) rather than measured from actual Compose layout. This is a
// deliberate simplification — true text-measurement-based reflow needs a two-pass
// SubcomposeLayout, which is a good next step but a much bigger/riskier piece of work. What you
// get here is real, honest multi-page behavior (correct page size/orientation/margins, content
// flows across pages, manual page breaks force a new page) — just not pixel-exact line breaks.
// ============================================================================================

import kotlin.math.ceil
import kotlin.math.max

data class IndexedBlock(val index: Int, val block: Block)
data class DocPage(val entries: List<IndexedBlock>)

private fun estimateParagraphHeightPt(p: ParagraphBlock, contentWidthPt: Float): Float {
    val style = BuiltInStyles.byId(p.styleId)
    val fontSize = style.fontSize.toFloat()
    val avgCharWidth = fontSize * 0.52f
    val charsPerLine = max(1, (contentWidthPt / avgCharWidth).toInt())
    val text = p.field.text
    val textLines = if (text.isEmpty()) 1 else text.split("\n").sumOf { line ->
        max(1, ceil(line.length / charsPerLine.toFloat()).toInt())
    }
    val lineHeight = fontSize * 1.4f
    return style.spacingBefore + textLines * lineHeight + style.spacingAfter
}

private fun estimateImageHeightPt(img: ImageBlock): Float {
    val bmp = img.bitmap
    val h = if (bmp != null && bmp.width > 0) img.widthDp * bmp.height / bmp.width.toFloat() else 120f
    return h + 40f // + the small align/resize/delete toolbar row under it
}

private fun estimateTableHeightPt(t: TableBlock): Float = t.rows.size * 30f + 30f

private fun estimateTocHeightPt(t: TocBlock): Float = 30f + t.entries.size * 22f

private fun estimateBlockHeightPt(b: Block, contentWidthPt: Float): Float = when (b) {
    is ParagraphBlock -> estimateParagraphHeightPt(b, contentWidthPt)
    is ImageBlock -> estimateImageHeightPt(b)
    is TableBlock -> estimateTableHeightPt(b)
    is TocBlock -> estimateTocHeightPt(b)
    is PageBreakBlock -> 0f
}

/** Splits [doc]'s blocks into pages sized per [WordDocument.pageSettings], honoring manual page breaks. */
fun paginate(doc: WordDocument): List<DocPage> {
    val settings = doc.pageSettings
    val (pageWidthPt, pageHeightPt) = settings.dimensionsPt()
    val contentWidthPt = pageWidthPt - settings.marginLeftPt - settings.marginRightPt
    var contentHeightPt = pageHeightPt - settings.marginTopPt - settings.marginBottomPt
    if (doc.showHeader) contentHeightPt -= 28f
    if (doc.showFooter) contentHeightPt -= 28f

    val pages = mutableListOf<MutableList<IndexedBlock>>(mutableListOf())
    var used = 0f
    doc.blocks.forEachIndexed { idx, block ->
        if (block is PageBreakBlock) {
            pages.last().add(IndexedBlock(idx, block))
            pages.add(mutableListOf())
            used = 0f
            return@forEachIndexed
        }
        val h = estimateBlockHeightPt(block, contentWidthPt)
        if (used > 0f && used + h > contentHeightPt) {
            pages.add(mutableListOf())
            used = 0f
        }
        pages.last().add(IndexedBlock(idx, block))
        used += h
    }
    if (pages.size > 1 && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
    return pages.map { DocPage(it) }
}

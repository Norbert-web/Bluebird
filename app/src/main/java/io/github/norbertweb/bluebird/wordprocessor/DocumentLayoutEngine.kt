package io.github.norbertweb.bluebird.wordprocessor

import kotlin.math.ceil
import kotlin.math.max

/**
 * Shared document layout engine used by the editor, status UI, navigation and print metadata.
 *
 * The renderer is intentionally still Compose-based, but all pagination decisions now come from
 * one deterministic engine. This removes the old situation where different callers made their
 * own approximations about page geometry, spacing, headings and object sizes.
 */
data class LayoutBlockMetrics(
    val blockId: String,
    val heightPt: Float,
    val keepWithNext: Boolean = false,
    val overflowPage: Boolean = false
)

data class DocumentLayoutDiagnostics(
    val pageCount: Int,
    val overflowingBlockIds: List<String>,
    val headingSplitsAvoided: Int,
    val usedContentPt: Float,
    val contentHeightPt: Float
)

data class DocumentLayoutResult(
    val pages: List<DocPage>,
    val diagnostics: DocumentLayoutDiagnostics
)

object DocumentLayoutEngine {
    private const val LIST_INDENT_PT = 20f
    private const val OBJECT_PADDING_PT = 20f
    private const val HEADER_FOOTER_RESERVE_PT = 28f

    private fun baseStyle(p: ParagraphBlock): DocStyle = BuiltInStyles.byId(p.styleId)

    private fun paragraphMaxFontSize(p: ParagraphBlock): Float {
        val style = baseStyle(p)
        val direct = if (p.field.text.isEmpty()) emptyList()
        else normalizeAndMerge(p.spans, p.field.text.length, style.baseAttrs())
        return max(style.fontSize.toFloat(), direct.maxOfOrNull { it.style.fontSize.toFloat() } ?: style.fontSize.toFloat())
    }

    private fun paragraphCharWidth(fontSize: Float, text: String): Float {
        // More realistic than one global character average: whitespace, narrow letters and
        // digits consume less width while capitals/punctuation are somewhat wider.
        if (text.isEmpty()) return fontSize * 0.48f
        var units = 0f
        text.forEach { c ->
            units += when {
                c == ' ' || c == '\t' -> 0.30f
                c in "ilI.,:;'!|`" -> 0.27f
                c in "mwMW@#%&" -> 0.82f
                c.isDigit() -> 0.52f
                c.isUpperCase() -> 0.63f
                else -> 0.52f
            }
        }
        return (units / text.length) * fontSize
    }

    private fun lineCountForText(text: String, firstWidth: Float, subsequentWidth: Float, avgCharWidth: Float): Int {
        val safeFirst = firstWidth.coerceAtLeast(24f)
        val safeSubsequent = subsequentWidth.coerceAtLeast(24f)
        var total = 0
        text.split('\n').forEachIndexed { lineIndex, rawLine ->
            val width = if (lineIndex == 0) safeFirst else safeSubsequent
            if (rawLine.isEmpty()) {
                total += 1
                return@forEachIndexed
            }
            var currentWidth = 0f
            var lines = 1
            rawLine.split(Regex("(\\s+)")).filter { it.isNotEmpty() }.forEach { token ->
                val tokenWidth = if (token.all { it.isWhitespace() }) {
                    avgCharWidth * token.length
                } else {
                    avgCharWidth * token.length * if (token.length > 24) 1.03f else 1f
                }
                val space = if (currentWidth > 0f) avgCharWidth * 0.30f else 0f
                if (currentWidth > 0f && currentWidth + space + tokenWidth > width) {
                    lines++
                    currentWidth = tokenWidth.coerceAtMost(width)
                } else {
                    currentWidth += space + tokenWidth
                }
                if (tokenWidth > width && token.length > 1) {
                    val chars = max(1, (width / avgCharWidth).toInt())
                    val extra = ceil(token.length / chars.toFloat()).toInt() - 1
                    lines += extra.coerceAtLeast(0)
                    currentWidth = (token.length % chars) * avgCharWidth
                }
            }
            total += lines
        }
        return total.coerceAtLeast(1)
    }

    private fun paragraphHeight(p: ParagraphBlock, contentWidthPt: Float): Float {
        val style = baseStyle(p)
        val fontSize = paragraphMaxFontSize(p)
        val listIndent = if (p.listType != null) (p.listLevel + 1) * LIST_INDENT_PT else 0f
        val firstLineIndent = p.firstLineIndentPt
        val available = (contentWidthPt - p.leftIndentPt - p.rightIndentPt - listIndent).coerceAtLeast(36f)
        val firstWidth = (available - firstLineIndent).coerceAtLeast(24f)
        val avgCharWidth = paragraphCharWidth(fontSize, p.field.text)
        val lines = lineCountForText(p.field.text, firstWidth, available, avgCharWidth)
        val before = (p.spacingBeforeOverride ?: style.spacingBefore).toFloat()
        val after = (p.spacingAfterOverride ?: style.spacingAfter).toFloat()
        return before + lines * fontSize * p.lineSpacing.coerceAtLeast(0.8f) + after
    }

    private fun imageHeight(img: ImageBlock, contentWidthPt: Float): Float {
        val bmp = img.bitmap
        val natural = if (bmp != null && bmp.width > 0) {
            img.widthDp * bmp.height / bmp.width.toFloat()
        } else 120f
        val clampedWidth = img.widthDp.toFloat().coerceIn(80f, contentWidthPt)
        val height = if (img.widthDp > 0) natural * clampedWidth / img.widthDp else natural
        val quarterTurn = ((img.rotationDeg % 180) + 180) % 180 == 90
        val rotated = if (quarterTurn && bmp != null && bmp.width > 0) clampedWidth * bmp.width / bmp.height.toFloat() else height
        return rotated + OBJECT_PADDING_PT
    }

    private fun tableHeight(table: TableBlock, contentWidthPt: Float): Float {
        if (table.rows.isEmpty()) return 32f
        val columnCount = table.rows.maxOfOrNull { it.cells.size }?.coerceAtLeast(1) ?: 1
        val cellWidth = (contentWidthPt / columnCount).coerceAtLeast(36f)
        val rowHeights = table.rows.map { row ->
            row.cells.maxOfOrNull { cell ->
                cell.blocks.sumOf { paragraphHeight(it, cellWidth).toDouble() }.toFloat().coerceAtLeast(24f)
            }?.coerceAtLeast(24f) ?: 24f
        }
        return rowHeights.sum() + 1f * rowHeights.size + OBJECT_PADDING_PT
    }

    private fun height(block: Block, contentWidthPt: Float): Float = when (block) {
        is ParagraphBlock -> paragraphHeight(block, contentWidthPt)
        is ImageBlock -> imageHeight(block, contentWidthPt)
        is TableBlock -> tableHeight(block, contentWidthPt)
        is TocBlock -> 30f + block.entries.size * 22f
        is PageBreakBlock -> 0f
    }

    private fun keepWithNext(block: Block): Boolean = block is ParagraphBlock &&
        block.styleId in setOf("title", "heading1", "heading2", "heading3")

    fun layout(doc: WordDocument): DocumentLayoutResult {
        val settings = doc.pageSettings
        val (pageWidthPt, pageHeightPt) = settings.dimensionsPt()
        val contentWidthPt = (pageWidthPt - settings.marginLeftPt - settings.marginRightPt).coerceAtLeast(36f)
        val contentHeightPt = (
            pageHeightPt - settings.marginTopPt - settings.marginBottomPt -
                (if (doc.showHeader) HEADER_FOOTER_RESERVE_PT else 0f) -
                (if (doc.showFooter) HEADER_FOOTER_RESERVE_PT else 0f)
            ).coerceAtLeast(48f)

        val metrics = doc.blocks.map { block ->
            LayoutBlockMetrics(
                blockId = block.id,
                heightPt = height(block, contentWidthPt),
                keepWithNext = keepWithNext(block)
            )
        }

        val pages = mutableListOf<MutableList<IndexedBlock>>()
        pages.add(mutableListOf())
        var used = 0f
        var headingMoves = 0
        val overflow = mutableListOf<String>()

        fun newPage() {
            if (pages.last().isNotEmpty()) pages.add(mutableListOf())
            used = 0f
        }

        doc.blocks.forEachIndexed { index, block ->
            if (block is PageBreakBlock) {
                pages.last().add(IndexedBlock(index, block))
                newPage()
                return@forEachIndexed
            }
            val metric = metrics[index]
            val nextHeight = metrics.getOrNull(index + 1)?.heightPt ?: 0f
            val shouldMoveHeading = metric.keepWithNext && pages.last().isNotEmpty() &&
                used + metric.heightPt + nextHeight > contentHeightPt
            if (shouldMoveHeading) {
                newPage()
                headingMoves++
            } else if (used > 0f && used + metric.heightPt > contentHeightPt) {
                newPage()
            }
            val actualOverflow = metric.heightPt > contentHeightPt
            if (actualOverflow) overflow += block.id
            pages.last().add(IndexedBlock(index, block))
            used += metric.heightPt
        }

        while (pages.size > 1 && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
        val finalPages = pages.map { DocPage(it.toList()) }
        return DocumentLayoutResult(
            pages = finalPages,
            diagnostics = DocumentLayoutDiagnostics(
                pageCount = finalPages.size.coerceAtLeast(1),
                overflowingBlockIds = overflow.distinct(),
                headingSplitsAvoided = headingMoves,
                usedContentPt = used,
                contentHeightPt = contentHeightPt
            )
        )
    }
}

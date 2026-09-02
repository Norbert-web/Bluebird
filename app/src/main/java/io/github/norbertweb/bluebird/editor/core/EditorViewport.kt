package io.github.norbertweb.bluebird.editor.core

/**
 * Immutable, viewport-aware representation of a document's lines.
 * The editor text input still owns the authoritative text; this layer is used
 * by visual surfaces (gutter, minimap, diagnostics and folding) so they do not
 * need to materialize every line on every recomposition.
 */
data class VisibleLine(
    val lineNumber: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val isFolded: Boolean,
)

data class EditorViewport(
    val firstLine: Int,
    val lastLine: Int,
    val lineHeightPx: Float,
    val topSpacerPx: Float,
    val bottomSpacerPx: Float,
)

class DocumentLineModel(private val text: String) {
    private val index = LineIndex(text)
    val lineCount: Int get() = index.lineCount

    fun line(lineNumber: Int): String {
        if (lineNumber !in 1..lineCount) return ""
        val start = index.lineStart(lineNumber)
        val end = index.lineEnd(text, lineNumber)
        return text.substring(start, end)
    }

    fun visibleLines(
        firstLine: Int,
        lastLine: Int,
        foldedLines: Set<Int> = emptySet(),
    ): List<VisibleLine> {
        val first = firstLine.coerceIn(1, lineCount)
        val last = lastLine.coerceIn(first, lineCount)
        val hidden = FoldingModel.hiddenLines(text, foldedLines, index)
        return (first..last).filterNot { it in hidden }.map { lineNumber ->
            val start = index.lineStart(lineNumber)
            val end = index.lineEnd(text, lineNumber)
            VisibleLine(lineNumber, start, end, text.substring(start, end), lineNumber in foldedLines)
        }
    }

    fun viewport(
        scrollPx: Int,
        viewportHeightPx: Int,
        lineHeightPx: Float,
        overscan: Int = 8,
    ): EditorViewport {
        val safeHeight = viewportHeightPx.coerceAtLeast(1)
        val safeLineHeight = lineHeightPx.coerceAtLeast(1f)
        val firstVisible = (scrollPx / safeLineHeight).toInt() + 1
        val visibleCount = kotlin.math.ceil(safeHeight / safeLineHeight).toInt() + 1
        val first = (firstVisible - overscan).coerceAtLeast(1)
        val last = (firstVisible + visibleCount + overscan).coerceAtMost(lineCount)
        return EditorViewport(
            firstLine = first,
            lastLine = last,
            lineHeightPx = safeLineHeight,
            topSpacerPx = (first - 1) * safeLineHeight,
            bottomSpacerPx = (lineCount - last) * safeLineHeight,
        )
    }
}

/** A cheap folded-line projection used by non-editing surfaces. */
fun foldedVisibleLineNumbers(lineCount: Int, foldedLines: Set<Int>): List<Int> {
    if (foldedLines.isEmpty()) return (1..lineCount).toList()
    val hidden = mutableSetOf<Int>()
    foldedLines.sorted().forEach { start ->
        if (start !in 1..lineCount) return@forEach
        var line = start + 1
        while (line <= lineCount) {
            hidden += line
            line++
            if (line in foldedLines) break
        }
    }
    return (1..lineCount).filterNot(hidden::contains)
}


/** Lightweight structural folding model. Regions are derived from braces and indentation. */
data class FoldRegion(val startLine: Int, val endLine: Int)

object FoldingModel {
    fun regions(text: String): List<FoldRegion> {
        val index = LineIndex(text)
        val regions = mutableListOf<FoldRegion>()
        val stack = ArrayDeque<Int>()
        var inSingle = false
        var inDouble = false
        var escaped = false
        text.forEachIndexed { offset, ch ->
            if (ch == '\n') return@forEachIndexed
            if (escaped) { escaped = false; return@forEachIndexed }
            if ((inSingle || inDouble) && ch == '\\') { escaped = true; return@forEachIndexed }
            if (!inDouble && ch == '\'') { inSingle = !inSingle; return@forEachIndexed }
            if (!inSingle && ch == '"') { inDouble = !inDouble; return@forEachIndexed }
            if (inSingle || inDouble) return@forEachIndexed
            when (ch) {
                '{' -> stack.addLast(index.lineForOffset(offset))
                '}' -> if (stack.isNotEmpty()) {
                    val start = stack.removeLast()
                    val end = index.lineForOffset(offset)
                    if (end > start) regions += FoldRegion(start, end)
                }
            }
        }
        return regions.distinctBy { it.startLine to it.endLine }.sortedBy { it.startLine }
    }

    fun hiddenLines(text: String, foldedStarts: Set<Int>, index: LineIndex = LineIndex(text)): Set<Int> {
        if (foldedStarts.isEmpty()) return emptySet()
        val ranges = regions(text).filter { it.startLine in foldedStarts }
        val hidden = mutableSetOf<Int>()
        ranges.forEach { region ->
            for (line in (region.startLine + 1) until region.endLine) hidden += line
            // Keep the closing brace visible; it gives users a stable fold boundary.
        }
        return hidden
    }
}

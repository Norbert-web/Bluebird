package io.github.norbertweb.bluebird.ui.screens

// ============================================================================================
// RichTextEngine.kt — plain text + a flat, gap-free list of format spans over it.
// Gaps are filled with a supplied `base` (the paragraph style's look) instead of a hardcoded
// blank StyleAttrs(), so unformatted text still renders in its paragraph's style; direct
// character formatting (bold, color, a search highlight, ...) layers on top per range.
// ============================================================================================

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

fun normalizeAndMerge(rawSpans: List<FormatRange>, length: Int, base: StyleAttrs = StyleAttrs()): List<FormatRange> {
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

fun splitSpansAt(spans: List<FormatRange>, pos: Int): List<FormatRange> {
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
fun applyStyle(
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

fun styleAt(spans: List<FormatRange>, pos: Int, base: StyleAttrs): StyleAttrs =
    spans.firstOrNull { pos >= it.start && pos < it.end }?.style ?: base

fun rangeHas(spans: List<FormatRange>, start: Int, end: Int, pick: (StyleAttrs) -> Boolean): Boolean {
    val relevant = spans.filter { it.end > start && it.start < end }
    if (relevant.isEmpty()) return false
    return relevant.all { pick(it.style) }
}

/** Finds the common-prefix/suffix edit region between an old and new string. */
fun diffRegion(old: String, new: String): Triple<Int, Int, Int> {
    val minLen = minOf(old.length, new.length)
    var prefix = 0
    while (prefix < minLen && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (suffix < (minLen - prefix) && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    return Triple(prefix, old.length - suffix, new.length - suffix)
}

fun adjustSpansForEdit(
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

/** Applies [text] edited at cursor [cursorAfter], returning the updated field + spans as a pair. */
fun editParagraphText(
    old: String, newText: String, cursorAfter: Int, oldSpans: List<FormatRange>, typingStyle: StyleAttrs, base: StyleAttrs
): Pair<TextFieldValue, List<FormatRange>> {
    val (s, oe, ne) = diffRegion(old, newText)
    val spans = adjustSpansForEdit(oldSpans, newText.length, s, oe, ne, typingStyle, base)
    return TextFieldValue(newText, TextRange(cursorAfter)) to spans
}

fun buildStyledText(text: String, spans: List<FormatRange>, highlightRange: IntRange? = null): AnnotatedString = buildAnnotatedString {
    append(text)
    for (span in spans) {
        addStyle(
            SpanStyle(
                fontWeight = if (span.style.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (span.style.italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = when {
                    span.style.underline && span.style.strikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    span.style.underline -> TextDecoration.Underline
                    span.style.strikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                },
                fontSize = (if (span.style.superscript || span.style.subscript) span.style.fontSize * 0.7f else span.style.fontSize.toFloat()).sp,
                baselineShift = when {
                    span.style.superscript -> BaselineShift.Superscript
                    span.style.subscript -> BaselineShift.Subscript
                    else -> null
                },
                color = span.style.color,
                background = span.style.highlight ?: Color.Unspecified,
                fontFamily = span.style.font.family
            ),
            span.start, span.end
        )
    }
    if (highlightRange != null && highlightRange.first < highlightRange.last + 1) {
        val s = highlightRange.first.coerceIn(0, text.length)
        val e = (highlightRange.last + 1).coerceIn(0, text.length)
        if (e > s) addStyle(SpanStyle(background = Color(0xFFFFEB3B).copy(alpha = 0.6f)), s, e)
    }
}

class RichTextTransformation(
    private val spans: List<FormatRange>,
    private val highlightRange: IntRange? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(buildStyledText(text.text, spans, highlightRange), OffsetMapping.Identity)
}

/** Splits `para` at every newline in [newValue]: `para` keeps the first line, the rest come back as new blocks. */
fun applyEnterSplit(para: ParagraphBlock, newValue: TextFieldValue): List<ParagraphBlock> {
    val parts = newValue.text.split("\n")
    val base = BuiltInStyles.byId(para.styleId).baseAttrs()
    para.field = TextFieldValue(parts[0])
    para.spans = normalizeAndMerge(emptyList(), parts[0].length, base)
    // A new paragraph after a heading/title/subtitle drops back to Normal, matching desktop word processors.
    val nextStyleId = if (para.styleId in BuiltInStyles.HEADING_IDS) "normal" else para.styleId
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
fun computeListNumbers(blocks: List<Block>): Map<String, Int> {
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

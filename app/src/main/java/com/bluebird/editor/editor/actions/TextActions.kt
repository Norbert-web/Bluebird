package com.bluebird.editor.editor.actions

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.bluebird.editor.core.IndentStyle
import com.bluebird.editor.core.LineEnding

// ─────────────────────────────────────────────────────────────────
// Text Action Results
// ─────────────────────────────────────────────────────────────────

data class ActionResult(
    val newValue: TextFieldValue,
    val shouldRecord: Boolean = true,
)

// ─────────────────────────────────────────────────────────────────
// Selection Helpers
// ─────────────────────────────────────────────────────────────────

fun TextFieldValue.hasSelection(): Boolean = selection.length > 0
fun TextFieldValue.selectedText(): String = text.substring(selection.start, selection.end)
fun TextFieldValue.lineAt(offset: Int): Int = text.substring(0, offset.coerceIn(0, text.length)).count { it == '\n' }
fun TextFieldValue.lineStartOffset(line: Int): Int {
    var pos = 0; var l = 0
    while (l < line && pos < text.length) { if (text[pos] == '\n') l++; pos++ }
    return pos
}
fun TextFieldValue.lineEndOffset(line: Int): Int {
    val start = lineStartOffset(line)
    val next = text.indexOf('\n', start)
    return if (next == -1) text.length else next
}
fun TextFieldValue.currentLine(): Int = lineAt(selection.start)
fun TextFieldValue.selectionLines(): IntRange {
    val startLine = lineAt(selection.start)
    val endLine = lineAt(if (selection.end > selection.start) selection.end - 1 else selection.end)
    return startLine..endLine
}

// ─────────────────────────────────────────────────────────────────
// Auto-Indent on Enter
// ─────────────────────────────────────────────────────────────────

fun handleEnter(tfv: TextFieldValue, indentStyle: IndentStyle, autoCloseBrackets: Boolean): ActionResult {
    val text = tfv.text
    val pos = tfv.selection.start
    val indent = indentStyle.chars

    // Find current line indentation
    val lineStart = text.lastIndexOf('\n', pos - 1) + 1
    val lineText = text.substring(lineStart, pos)
    val leadingWhitespace = lineText.takeWhile { it == ' ' || it == '\t' }

    // Check if we're after an opener
    val charBefore = text.getOrNull(pos - 1)
    val charAfter = text.getOrNull(pos)

    val extraIndent = if (charBefore in listOf('{', '(', '[', ':')) indent else ""
    val dedent = if (charBefore in listOf('{', '(', '[') && charAfter in listOf('}', ')', ']') && autoCloseBrackets) {
        // Insert closing bracket on its own line
        val insert = "\n$leadingWhitespace$extraIndent\n$leadingWhitespace"
        val newText = text.substring(0, pos) + insert + text.substring(pos)
        val newPos = pos + 1 + leadingWhitespace.length + extraIndent.length
        return ActionResult(tfv.copy(text = newText, selection = TextRange(newPos)))
    } else false

    val insert = "\n$leadingWhitespace$extraIndent"
    val newText = text.substring(0, pos) + insert + text.substring(pos)
    val newPos = pos + insert.length
    return ActionResult(tfv.copy(text = newText, selection = TextRange(newPos)))
}

// ─────────────────────────────────────────────────────────────────
// Auto-Close Brackets
// ─────────────────────────────────────────────────────────────────

private val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'', '`' to '`')
private val CLOSERS = setOf(')', ']', '}')

fun handleCharInput(char: Char, tfv: TextFieldValue, autoCloseBrackets: Boolean): ActionResult? {
    if (!autoCloseBrackets) return null
    val text = tfv.text
    val pos = tfv.selection.start

    // Skip-over close bracket if next char is the same
    if (char in CLOSERS && text.getOrNull(pos) == char) {
        return ActionResult(tfv.copy(selection = TextRange(pos + 1)), shouldRecord = false)
    }

    // Auto-close
    val close = PAIRS[char] ?: return null
    val hasSelection = tfv.hasSelection()

    return if (hasSelection) {
        // Wrap selection
        val selected = tfv.selectedText()
        val newText = text.substring(0, tfv.selection.start) + char + selected + close + text.substring(tfv.selection.end)
        val newSel = TextRange(tfv.selection.start + 1, tfv.selection.end + 1)
        ActionResult(tfv.copy(text = newText, selection = newSel))
    } else {
        val newText = text.substring(0, pos) + char + close + text.substring(pos)
        ActionResult(tfv.copy(text = newText, selection = TextRange(pos + 1)), shouldRecord = false)
    }
}

// ─────────────────────────────────────────────────────────────────
// Tab Handling (indent/dedent)
// ─────────────────────────────────────────────────────────────────

val IndentStyle.chars: String get() = when (this) {
    IndentStyle.SPACES_2 -> "  "
    IndentStyle.SPACES_4 -> "    "
    IndentStyle.TAB -> "\t"
}

fun handleTab(tfv: TextFieldValue, indentStyle: IndentStyle, shift: Boolean): ActionResult {
    val indent = indentStyle.chars
    val text = tfv.text
    val pos = tfv.selection.start

    return if (tfv.hasSelection()) {
        // Multi-line indent/dedent
        val lines = tfv.selectionLines()
        val lineStart = tfv.lineStartOffset(lines.first)
        val lineEnd = tfv.lineEndOffset(lines.last)
        val selectedPart = text.substring(lineStart, lineEnd)

        val modified = if (shift) {
            selectedPart.split('\n').joinToString("\n") { line ->
                when {
                    line.startsWith(indent) -> line.removePrefix(indent)
                    line.startsWith("\t") -> line.removePrefix("\t")
                    line.startsWith(" ") -> line.trimStart(' ')
                    else -> line
                }
            }
        } else {
            selectedPart.split('\n').joinToString("\n") { "$indent$it" }
        }

        val newText = text.substring(0, lineStart) + modified + text.substring(lineEnd)
        val delta = modified.length - selectedPart.length
        ActionResult(tfv.copy(text = newText, selection = TextRange(pos, (tfv.selection.end + delta).coerceAtLeast(pos))))
    } else {
        if (shift) {
            // Dedent current line
            val lineStart = tfv.lineStartOffset(tfv.currentLine())
            val line = text.substring(lineStart)
            val stripped = when {
                line.startsWith(indent) -> line.removePrefix(indent)
                line.startsWith("\t") -> line.removePrefix("\t")
                line.startsWith(" ") -> line.trimStart(' ')
                else -> return ActionResult(tfv)
            }
            val removed = line.length - stripped.length
            val newText = text.substring(0, lineStart) + stripped
            ActionResult(tfv.copy(text = newText, selection = TextRange((pos - removed).coerceAtLeast(lineStart))))
        } else {
            // Insert indent
            val newText = text.substring(0, pos) + indent + text.substring(pos)
            ActionResult(tfv.copy(text = newText, selection = TextRange(pos + indent.length)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Comment Toggle
// ─────────────────────────────────────────────────────────────────

private fun commentCharFor(ext: String): String? = when (ext) {
    "kt", "kts", "java", "js", "jsx", "ts", "tsx", "cs", "go", "rs", "dart", "swift", "cpp", "c", "h", "cc" -> "//"
    "py", "pyw", "rb", "sh", "bash", "zsh", "yaml", "yml", "toml" -> "#"
    "sql" -> "--"
    "html", "htm", "xml" -> null // block comment only
    "css", "scss", "sass", "less" -> null // /* */
    "lua" -> "--"
    "r" -> "#"
    else -> "//"
}

fun toggleLineComment(tfv: TextFieldValue, ext: String): ActionResult {
    val commentChar = commentCharFor(ext) ?: return ActionResult(tfv)
    val text = tfv.text
    val lines = tfv.selectionLines()
    val lineStart = tfv.lineStartOffset(lines.first)
    val lineEnd = tfv.lineEndOffset(lines.last)
    val block = text.substring(lineStart, lineEnd)
    val blockLines = block.split('\n')

    // Determine if ALL lines are commented
    val allCommented = blockLines.all { it.trimStart().startsWith(commentChar) }

    val modified = blockLines.joinToString("\n") { line ->
        if (allCommented) {
            val stripped = line.trimStart()
            val prefix = line.substring(0, line.length - stripped.length)
            if (stripped.startsWith("$commentChar ")) prefix + stripped.removePrefix("$commentChar ")
            else prefix + stripped.removePrefix(commentChar)
        } else {
            val stripped = line.trimStart()
            val prefix = line.substring(0, line.length - stripped.length)
            "$prefix$commentChar $stripped"
        }
    }

    val newText = text.substring(0, lineStart) + modified + text.substring(lineEnd)
    val cursorDelta = if (allCommented) -(commentChar.length + 1) else (commentChar.length + 1)
    val newPos = (tfv.selection.start + cursorDelta).coerceIn(lineStart, lineStart + modified.length)
    return ActionResult(tfv.copy(text = newText, selection = TextRange(newPos)))
}

// ─────────────────────────────────────────────────────────────────
// Duplicate Line
// ─────────────────────────────────────────────────────────────────

fun duplicateLine(tfv: TextFieldValue): ActionResult {
    val text = tfv.text
    val line = tfv.currentLine()
    val lineStart = tfv.lineStartOffset(line)
    val lineEnd = tfv.lineEndOffset(line)
    val lineText = text.substring(lineStart, lineEnd)
    val insert = "\n$lineText"
    val newText = text.substring(0, lineEnd) + insert + text.substring(lineEnd)
    val newPos = tfv.selection.start + insert.length
    return ActionResult(tfv.copy(text = newText, selection = TextRange(newPos)))
}

// ─────────────────────────────────────────────────────────────────
// Move Line Up/Down
// ─────────────────────────────────────────────────────────────────

fun moveLineUp(tfv: TextFieldValue): ActionResult {
    val text = tfv.text
    val line = tfv.currentLine()
    if (line == 0) return ActionResult(tfv)
    val prevStart = tfv.lineStartOffset(line - 1)
    val prevEnd = tfv.lineEndOffset(line - 1)
    val currStart = prevEnd + 1
    val currEnd = tfv.lineEndOffset(line)
    val prevLine = text.substring(prevStart, prevEnd)
    val currLine = text.substring(currStart, currEnd)
    val newText = text.substring(0, prevStart) + currLine + "\n" + prevLine + text.substring(currEnd)
    val newPos = prevStart + (tfv.selection.start - currStart)
    return ActionResult(tfv.copy(text = newText, selection = TextRange(newPos.coerceAtLeast(prevStart))))
}

fun moveLineDown(tfv: TextFieldValue): ActionResult {
    val text = tfv.text
    val line = tfv.currentLine()
    val lineStart = tfv.lineStartOffset(line)
    val lineEnd = tfv.lineEndOffset(line)
    if (lineEnd >= text.length) return ActionResult(tfv)
    val nextStart = lineEnd + 1
    val nextEnd = text.indexOf('\n', nextStart).let { if (it == -1) text.length else it }
    val currLine = text.substring(lineStart, lineEnd)
    val nextLine = text.substring(nextStart, nextEnd)
    val newText = text.substring(0, lineStart) + nextLine + "\n" + currLine + text.substring(nextEnd)
    val delta = nextLine.length + 1
    val newPos = tfv.selection.start + delta
    return ActionResult(tfv.copy(text = newText, selection = TextRange(newPos.coerceAtMost(newText.length))))
}

// ─────────────────────────────────────────────────────────────────
// Delete Line
// ─────────────────────────────────────────────────────────────────

fun deleteLine(tfv: TextFieldValue): ActionResult {
    val text = tfv.text
    val line = tfv.currentLine()
    val lineStart = tfv.lineStartOffset(line)
    val lineEnd = tfv.lineEndOffset(line)
    val newText = if (lineEnd < text.length) {
        text.substring(0, lineStart) + text.substring(lineEnd + 1)
    } else if (lineStart > 0) {
        text.substring(0, lineStart - 1)
    } else {
        ""
    }
    return ActionResult(tfv.copy(text = newText, selection = TextRange(lineStart.coerceAtMost(newText.length))))
}

// ─────────────────────────────────────────────────────────────────
// Select Word / Line
// ─────────────────────────────────────────────────────────────────

fun selectWord(tfv: TextFieldValue): ActionResult {
    val text = tfv.text
    val pos = tfv.selection.start
    var start = pos
    var end = pos
    while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_')) start--
    while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_')) end++
    return ActionResult(tfv.copy(selection = TextRange(start, end)), shouldRecord = false)
}

fun selectLine(tfv: TextFieldValue): ActionResult {
    val line = tfv.currentLine()
    val start = tfv.lineStartOffset(line)
    val end = tfv.lineEndOffset(line)
    return ActionResult(tfv.copy(selection = TextRange(start, end)), shouldRecord = false)
}

// ─────────────────────────────────────────────────────────────────
// Case Transformations
// ─────────────────────────────────────────────────────────────────

fun toUpperCase(tfv: TextFieldValue): ActionResult {
    if (!tfv.hasSelection()) return ActionResult(tfv)
    val text = tfv.text
    val upper = tfv.selectedText().uppercase()
    val new = text.substring(0, tfv.selection.start) + upper + text.substring(tfv.selection.end)
    return ActionResult(tfv.copy(text = new))
}

fun toLowerCase(tfv: TextFieldValue): ActionResult {
    if (!tfv.hasSelection()) return ActionResult(tfv)
    val text = tfv.text
    val lower = tfv.selectedText().lowercase()
    val new = text.substring(0, tfv.selection.start) + lower + text.substring(tfv.selection.end)
    return ActionResult(tfv.copy(text = new))
}

fun toTitleCase(tfv: TextFieldValue): ActionResult {
    if (!tfv.hasSelection()) return ActionResult(tfv)
    val text = tfv.text
    val title = tfv.selectedText().split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    val new = text.substring(0, tfv.selection.start) + title + text.substring(tfv.selection.end)
    return ActionResult(tfv.copy(text = new))
}

fun toSnakeCase(tfv: TextFieldValue): ActionResult {
    if (!tfv.hasSelection()) return ActionResult(tfv)
    val text = tfv.text
    val snake = tfv.selectedText()
        .replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .replace(Regex("[\\s-]+"), "_")
        .lowercase()
    val new = text.substring(0, tfv.selection.start) + snake + text.substring(tfv.selection.end)
    return ActionResult(tfv.copy(text = new))
}

fun toCamelCase(tfv: TextFieldValue): ActionResult {
    if (!tfv.hasSelection()) return ActionResult(tfv)
    val text = tfv.text
    val camel = tfv.selectedText()
        .split(Regex("[_\\s-]+"))
        .mapIndexed { i, s -> if (i == 0) s.lowercase() else s.replaceFirstChar { it.uppercase() } }
        .joinToString("")
    val new = text.substring(0, tfv.selection.start) + camel + text.substring(tfv.selection.end)
    return ActionResult(tfv.copy(text = new))
}

// ─────────────────────────────────────────────────────────────────
// Sort Lines
// ─────────────────────────────────────────────────────────────────

fun sortLines(tfv: TextFieldValue, descending: Boolean = false): ActionResult {
    val text = tfv.text
    val lines = if (tfv.hasSelection()) {
        val range = tfv.selectionLines()
        text.split('\n').subList(range.first, range.last + 1)
    } else text.split('\n')

    val sorted = if (descending) lines.sortedDescending() else lines.sorted()
    val newText = if (tfv.hasSelection()) {
        val range = tfv.selectionLines()
        val allLines = text.split('\n').toMutableList()
        for (i in range) allLines[i] = sorted[i - range.first]
        allLines.joinToString("\n")
    } else sorted.joinToString("\n")

    return ActionResult(tfv.copy(text = newText))
}

// ─────────────────────────────────────────────────────────────────
// Remove Duplicate Lines
// ─────────────────────────────────────────────────────────────────

fun removeDuplicateLines(tfv: TextFieldValue): ActionResult {
    val seen = LinkedHashSet<String>()
    val newText = tfv.text.split('\n').filter { seen.add(it) }.joinToString("\n")
    return ActionResult(tfv.copy(text = newText))
}

// ─────────────────────────────────────────────────────────────────
// Trim Trailing Whitespace
// ─────────────────────────────────────────────────────────────────

fun trimTrailingWhitespace(tfv: TextFieldValue): ActionResult {
    val newText = tfv.text.split('\n').joinToString("\n") { it.trimEnd() }
    return ActionResult(tfv.copy(text = newText))
}

// ─────────────────────────────────────────────────────────────────
// Line Ending Conversion
// ─────────────────────────────────────────────────────────────────

fun convertLineEndings(text: String, to: LineEnding): String {
    val unified = text.replace("\r\n", "\n").replace("\r", "\n")
    return when (to) {
        LineEnding.LF -> unified
        LineEnding.CRLF -> unified.replace("\n", "\r\n")
        LineEnding.CR -> unified.replace("\n", "\r")
    }
}

fun detectLineEnding(text: String): LineEnding {
    val crlfCount = Regex("\r\n").findAll(text).count()
    val crCount = Regex("\r(?!\n)").findAll(text).count()
    val lfCount = Regex("(?<!\r)\n").findAll(text).count()
    return when {
        crlfCount >= lfCount && crlfCount >= crCount -> LineEnding.CRLF
        crCount > lfCount -> LineEnding.CR
        else -> LineEnding.LF
    }
}

// ─────────────────────────────────────────────────────────────────
// Insert Date/Time
// ─────────────────────────────────────────────────────────────────

fun insertText(tfv: TextFieldValue, insert: String): ActionResult {
    val text = tfv.text
    val pos = tfv.selection.start
    val (start, end) = if (tfv.hasSelection()) tfv.selection.start to tfv.selection.end else pos to pos
    val newText = text.substring(0, start) + insert + text.substring(end)
    return ActionResult(tfv.copy(text = newText, selection = TextRange(start + insert.length)))
}

// ─────────────────────────────────────────────────────────────────
// Cut/Copy/Paste (returns text to put in clipboard, or null)
// ─────────────────────────────────────────────────────────────────

fun cutText(tfv: TextFieldValue): Pair<ActionResult, String> {
    val selected = if (tfv.hasSelection()) tfv.selectedText()
    else {
        // Cut entire line if no selection
        val line = tfv.currentLine()
        val s = tfv.lineStartOffset(line)
        val e = tfv.lineEndOffset(line)
        tfv.text.substring(s, e)
    }
    val text = tfv.text
    val (start, end) = if (tfv.hasSelection()) tfv.selection.start to tfv.selection.end
    else {
        val line = tfv.currentLine()
        val s = tfv.lineStartOffset(line)
        val e = if (tfv.lineEndOffset(line) < text.length) tfv.lineEndOffset(line) + 1 else tfv.lineEndOffset(line)
        s to e
    }
    val newText = text.substring(0, start) + text.substring(end)
    return ActionResult(tfv.copy(text = newText, selection = TextRange(start))) to selected
}

// ─────────────────────────────────────────────────────────────────
// Statistics
// ─────────────────────────────────────────────────────────────────

data class TextStats(
    val lines: Int,
    val words: Int,
    val chars: Int,
    val charsNoSpaces: Int,
    val selectedChars: Int,
    val selectedWords: Int,
    val selectedLines: Int,
    val paragraphs: Int,
    val avgLineLength: Double,
    val longestLine: Int,
    val readingTimeMin: Double,
)

fun computeStats(tfv: TextFieldValue): TextStats {
    val text = tfv.text
    val lines = text.split('\n')
    val words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val selected = if (tfv.hasSelection()) tfv.selectedText() else ""
    return TextStats(
        lines = lines.size,
        words = words,
        chars = text.length,
        charsNoSpaces = text.count { !it.isWhitespace() },
        selectedChars = selected.length,
        selectedWords = if (selected.isBlank()) 0 else selected.trim().split(Regex("\\s+")).count { it.isNotEmpty() },
        selectedLines = if (selected.isEmpty()) 0 else selected.count { it == '\n' } + 1,
        paragraphs = text.split(Regex("\n\n+")).count { it.isNotBlank() },
        avgLineLength = if (lines.isEmpty()) 0.0 else lines.map { it.length }.average(),
        longestLine = lines.maxOfOrNull { it.length } ?: 0,
        readingTimeMin = words / 200.0,
    )
}

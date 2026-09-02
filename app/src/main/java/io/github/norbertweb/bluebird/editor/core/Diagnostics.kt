package io.github.norbertweb.bluebird.editor.core

/** Lightweight editor diagnostics used until a full language-service/LSP layer is added. */
enum class DiagnosticSeverity { ERROR, WARNING, INFO }

data class Diagnostic(
    val line: Int,
    val column: Int,
    val severity: DiagnosticSeverity,
    val message: String,
    val source: String = "Bluebird"
)

/** Fast, allocation-light diagnostics for common structural problems. */
fun analyzeDiagnostics(text: String, fileName: String): List<Diagnostic> {
    if (text.isEmpty()) return emptyList()
    val result = mutableListOf<Diagnostic>()
    val stack = ArrayDeque<Pair<Char, Pair<Int, Int>>>()
    val pairs = mapOf('(' to ')', '[' to ']', '{' to '}')
    val closing = pairs.values.toSet()
    var inSingle = false
    var inDouble = false
    var escaped = false

    text.lineSequence().forEachIndexed { lineIndex, line ->
        var col = 0
        while (col < line.length) {
            val ch = line[col]
            if (escaped) { escaped = false; col++; continue }
            if ((inSingle || inDouble) && ch == '\\') { escaped = true; col++; continue }
            if (!inDouble && ch == '\'') { inSingle = !inSingle; col++; continue }
            if (!inSingle && ch == '"') { inDouble = !inDouble; col++; continue }
            if (!inSingle && !inDouble) {
                if (ch in pairs) stack.add(ch to (lineIndex + 1 to col + 1))
                else if (ch in closing) {
                    val expected = stack.lastOrNull()?.first?.let { pairs[it] }
                    if (expected != ch) {
                        result += Diagnostic(lineIndex + 1, col + 1, DiagnosticSeverity.ERROR,
                            "Unexpected '$ch'.${if (expected != null) " Expected '$expected'." else " No matching opening bracket."}")
                    } else stack.removeLast()
                }
            }
            col++
        }
        val todo = Regex("\\b(TODO|FIXME)\\b").find(line)
        if (todo != null) {
            result += Diagnostic(lineIndex + 1, todo.range.first + 1, DiagnosticSeverity.INFO,
                "${todo.value} marker")
        }
    }
    stack.asReversed().forEach { (open, pos) ->
        result += Diagnostic(pos.first, pos.second, DiagnosticSeverity.ERROR,
            "Unclosed '$open'. Expected '${pairs[open]}'.")
    }
    if (!inSingle && !inDouble && text.endsWith("\n\n")) {
        // Keep this informational only; it is intentionally non-invasive.
        val lines = text.count { it == '\n' }
        result += Diagnostic(lines.coerceAtLeast(1), 1, DiagnosticSeverity.INFO, "File ends with an extra blank line")
    }
    return result.sortedWith(compareBy<Diagnostic> { it.line }.thenBy { it.column }).take(1000)
}

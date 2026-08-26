package io.github.norbertweb.bluebird.editor.core

import androidx.compose.ui.text.input.TextFieldValue
import java.nio.charset.Charset
import java.util.UUID

// ─────────────────────────────────────────────────────────────────
// Enums & Constants
// ─────────────────────────────────────────────────────────────────

enum class FileEncoding(val label: String, val charset: Charset) {
    UTF8("UTF-8", Charsets.UTF_8),
    UTF8_BOM("UTF-8 BOM", Charsets.UTF_8),
    UTF16_LE("UTF-16 LE", Charsets.UTF_16LE),
    UTF16_BE("UTF-16 BE", Charsets.UTF_16BE),
    ISO8859("ISO-8859-1", Charsets.ISO_8859_1),
    WIN1252("Windows-1252", safeCharset("windows-1252")),
    ASCII("ASCII", Charsets.US_ASCII),
    SHIFT_JIS("Shift-JIS", safeCharset("Shift_JIS")),
    GB2312("GB2312", safeCharset("GB2312")),
}

enum class LineEnding(val label: String, val chars: String) {
    LF("LF (Unix)", "\n"),
    CRLF("CRLF (Windows)", "\r\n"),
    CR("CR (Classic Mac)", "\r"),
}

enum class EditorTheme(val label: String) {
    VSCODE_DARK("VS Code Dark"),
    VSCODE_LIGHT("VS Code Light"),
    MONOKAI("Monokai"),
    SOLARIZED_DARK("Solarized Dark"),
    SOLARIZED_LIGHT("Solarized Light"),
    GITHUB_DARK("GitHub Dark"),
    GITHUB_LIGHT("GitHub Light"),
    DRACULA("Dracula"),
    ONE_DARK("One Dark"),
    NORD("Nord"),
}

enum class IndentStyle(val label: String) {
    SPACES_2("2 Spaces"),
    SPACES_4("4 Spaces"),
    TAB("Tab"),
}

enum class BracketPair(val open: Char, val close: Char) {
    PAREN('(', ')'),
    BRACKET('[', ']'),
    BRACE('{', '}'),
    ANGLE('<', '>'),
    SINGLE_QUOTE('\'', '\''),
    DOUBLE_QUOTE('"', '"'),
    BACKTICK('`', '`'),
}

private fun safeCharset(name: String): Charset = try {
    Charset.forName(name)
} catch (_: Exception) { Charsets.UTF_8 }

// ─────────────────────────────────────────────────────────────────
// Undo / Redo History Entry (grouped, debounced)
// ─────────────────────────────────────────────────────────────────

data class HistoryEntry(
    val value: TextFieldValue,
    val timestamp: Long = System.currentTimeMillis(),
)

// ─────────────────────────────────────────────────────────────────
// Bookmark
// ─────────────────────────────────────────────────────────────────

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val line: Int,
    val label: String = "",
    val color: Long = 0xFFFFB900,
)

// ─────────────────────────────────────────────────────────────────
// Code Snippet
// ─────────────────────────────────────────────────────────────────

data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val trigger: String,
    val description: String,
    val body: String,
    val language: String = "*", // "*" = all languages
)

val DEFAULT_SNIPPETS = listOf(
    Snippet("fn-main", "Kotlin main function", "fun main() {\n    \n}", "kt"),
    Snippet("data-class", "Kotlin data class", "data class \${1:Name}(\n    val \${2:field}: \${3:Type}\n)", "kt"),
    Snippet("comp", "Composable function", "@Composable\nfun \${1:Name}() {\n    \n}", "kt"),
    Snippet("if-else", "if/else block", "if (\${1:condition}) {\n    \${2}\n} else {\n    \${3}\n}", "*"),
    Snippet("for-in", "for-in loop", "for (\${1:item} in \${2:collection}) {\n    \${3}\n}", "*"),
    Snippet("try-catch", "try/catch block", "try {\n    \${1}\n} catch (e: \${2:Exception}) {\n    \${3}\n}", "*"),
    Snippet("log", "Log statement", "Log.d(\"\${1:TAG}\", \"\${2:message}\")", "kt"),
    Snippet("todo", "TODO comment", "// TODO: \${1:description}", "*"),
    Snippet("fixme", "FIXME comment", "// FIXME: \${1:description}", "*"),
    Snippet("date", "ISO date", "2025-01-01", "*"),
)

// ─────────────────────────────────────────────────────────────────
// Cursor Position
// ─────────────────────────────────────────────────────────────────

data class CursorPosition(val line: Int, val col: Int, val offset: Int)

// ─────────────────────────────────────────────────────────────────
// Tab Data (per-file state)
// ─────────────────────────────────────────────────────────────────

data class TabData(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String = "",
    val fileName: String = "Untitled.txt",
    val content: TextFieldValue = TextFieldValue(""),
    val isSaved: Boolean = false,
    val isModified: Boolean = false,
    val encoding: FileEncoding = FileEncoding.UTF8,
    val lineEnding: LineEnding = LineEnding.LF,
    val isReadOnly: Boolean = false,
    val isPinned: Boolean = false,
    val undoStack: List<HistoryEntry> = emptyList(),
    val redoStack: List<HistoryEntry> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val foldedLines: Set<Int> = emptySet(),
    val lastSavedContent: String = "",
    val scrollOffset: Int = 0,
    val cursorPosition: CursorPosition = CursorPosition(1, 1, 0),
    val lastAutosaveTime: Long = 0L,
)

// ─────────────────────────────────────────────────────────────────
// Find Result
// ─────────────────────────────────────────────────────────────────

data class FindResult(
    val range: IntRange,
    val lineNumber: Int,
    val lineText: String,
    val matchText: String,
)

// ─────────────────────────────────────────────────────────────────
// Editor Settings (persisted globally)
// ─────────────────────────────────────────────────────────────────

data class EditorSettings(
    val theme: EditorTheme = EditorTheme.VSCODE_DARK,
    val fontSize: Float = 14f,
    val fontFamily: String = "Monospace",
    val wordWrap: Boolean = true,
    val showLineNumbers: Boolean = true,
    val showMinimap: Boolean = true,
    val syntaxHighlight: Boolean = true,
    val autoIndent: Boolean = true,
    val bracketMatching: Boolean = true,
    val autoCloseBrackets: Boolean = true,
    val indentStyle: IndentStyle = IndentStyle.SPACES_4,
    val tabSize: Int = 4,
    val showWhitespace: Boolean = false,
    val highlightCurrentLine: Boolean = true,
    val smoothScrolling: Boolean = true,
    val zoom: Float = 1f,
    val snippetsEnabled: Boolean = true,
    val autosaveEnabled: Boolean = true,
    val autosaveIntervalMs: Long = 30_000L,
    val showBreadcrumb: Boolean = true,
    val showGitGutter: Boolean = false,
    val trimTrailingWhitespace: Boolean = false,
    val insertFinalNewline: Boolean = true,
    val columnLimit: Int = 80,
    val showColumnGuide: Boolean = false,
    val recentFiles: List<String> = emptyList(),
    val customSnippets: List<Snippet> = emptyList(),
)

// ─────────────────────────────────────────────────────────────────
// Diff / Change Tracking
// ─────────────────────────────────────────────────────────────────

enum class ChangeType { ADDED, MODIFIED, DELETED }

data class LineChange(val lineNumber: Int, val type: ChangeType)

package io.github.norbertweb.bluebird.editor.utils

import android.content.Context
import android.content.SharedPreferences
import io.github.norbertweb.bluebird.editor.core.*
import org.json.JSONArray
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────
// EditorPreferences — persists settings across app restarts
// ─────────────────────────────────────────────────────────────────

object EditorPreferences {

    private const val PREFS_NAME = "bluebird_editor_prefs"
    private const val KEY_THEME = "theme"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_FONT_FAMILY = "font_family"
    private const val KEY_WORD_WRAP = "word_wrap"
    private const val KEY_SHOW_LINE_NUMS = "show_line_numbers"
    private const val KEY_SHOW_MINIMAP = "show_minimap"
    private const val KEY_SYNTAX_HIGHLIGHT = "syntax_highlight"
    private const val KEY_AUTO_INDENT = "auto_indent"
    private const val KEY_BRACKET_MATCHING = "bracket_matching"
    private const val KEY_AUTO_CLOSE_BRACKETS = "auto_close_brackets"
    private const val KEY_INDENT_STYLE = "indent_style"
    private const val KEY_TAB_SIZE = "tab_size"
    private const val KEY_SHOW_WHITESPACE = "show_whitespace"
    private const val KEY_HIGHLIGHT_CURRENT_LINE = "highlight_current_line"
    private const val KEY_SMOOTH_SCROLLING = "smooth_scrolling"
    private const val KEY_ZOOM = "zoom"
    private const val KEY_SNIPPETS_ENABLED = "snippets_enabled"
    private const val KEY_AUTOSAVE_ENABLED = "autosave_enabled"
    private const val KEY_AUTOSAVE_INTERVAL = "autosave_interval"
    private const val KEY_SHOW_BREADCRUMB = "show_breadcrumb"
    private const val KEY_SHOW_GIT_GUTTER = "show_git_gutter"
    private const val KEY_TRIM_TRAILING = "trim_trailing_whitespace"
    private const val KEY_INSERT_FINAL_NEWLINE = "insert_final_newline"
    private const val KEY_COLUMN_LIMIT = "column_limit"
    private const val KEY_SHOW_COLUMN_GUIDE = "show_column_guide"
    private const val KEY_RECENT_FILES = "recent_files"
    private const val KEY_CUSTOM_SNIPPETS = "custom_snippets"
    private const val KEY_LAYOUT = "workspace_layout"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Save ──────────────────────────────────────────────────────

    fun save(context: Context, settings: EditorSettings) {
        prefs(context).edit().apply {
            // Kept only to migrate old preference files safely.
            putString(KEY_THEME, EditorTheme.SYSTEM.name)
            putFloat(KEY_FONT_SIZE, settings.fontSize)
            putString(KEY_FONT_FAMILY, settings.fontFamily)
            putBoolean(KEY_WORD_WRAP, settings.wordWrap)
            putBoolean(KEY_SHOW_LINE_NUMS, settings.showLineNumbers)
            putBoolean(KEY_SHOW_MINIMAP, settings.showMinimap)
            putBoolean(KEY_SYNTAX_HIGHLIGHT, settings.syntaxHighlight)
            putBoolean(KEY_AUTO_INDENT, settings.autoIndent)
            putBoolean(KEY_BRACKET_MATCHING, settings.bracketMatching)
            putBoolean(KEY_AUTO_CLOSE_BRACKETS, settings.autoCloseBrackets)
            putString(KEY_INDENT_STYLE, settings.indentStyle.name)
            putInt(KEY_TAB_SIZE, settings.tabSize)
            putBoolean(KEY_SHOW_WHITESPACE, settings.showWhitespace)
            putBoolean(KEY_HIGHLIGHT_CURRENT_LINE, settings.highlightCurrentLine)
            putBoolean(KEY_SMOOTH_SCROLLING, settings.smoothScrolling)
            putFloat(KEY_ZOOM, settings.zoom)
            putBoolean(KEY_SNIPPETS_ENABLED, settings.snippetsEnabled)
            putBoolean(KEY_AUTOSAVE_ENABLED, settings.autosaveEnabled)
            putLong(KEY_AUTOSAVE_INTERVAL, settings.autosaveIntervalMs)
            putBoolean(KEY_SHOW_BREADCRUMB, settings.showBreadcrumb)
            putBoolean(KEY_SHOW_GIT_GUTTER, settings.showGitGutter)
            putBoolean(KEY_TRIM_TRAILING, settings.trimTrailingWhitespace)
            putBoolean(KEY_INSERT_FINAL_NEWLINE, settings.insertFinalNewline)
            putInt(KEY_COLUMN_LIMIT, settings.columnLimit)
            putBoolean(KEY_SHOW_COLUMN_GUIDE, settings.showColumnGuide)

            // Recent files as JSON array
            val recentArr = JSONArray().also { arr -> settings.recentFiles.forEach { arr.put(it) } }
            putString(KEY_RECENT_FILES, recentArr.toString())

            // Custom snippets as JSON array
            val snippetsArr = JSONArray().also { arr ->
                settings.customSnippets.forEach { snip ->
                    arr.put(JSONObject().also { obj ->
                        obj.put("id", snip.id)
                        obj.put("trigger", snip.trigger)
                        obj.put("description", snip.description)
                        obj.put("body", snip.body)
                        obj.put("language", snip.language)
                    })
                }
            }
            putString(KEY_CUSTOM_SNIPPETS, snippetsArr.toString())
        }.apply()
    }

    // ── Load ──────────────────────────────────────────────────────

    fun load(context: Context): EditorSettings {
        val p = prefs(context)
        if (!p.contains(KEY_THEME)) return EditorSettings() // first launch

        // Appearance is controlled exclusively by the Android system. Older
        // saved theme values are intentionally ignored during migration.
        val theme = EditorTheme.SYSTEM

        val indentStr = p.getString(KEY_INDENT_STYLE, IndentStyle.SPACES_4.name) ?: IndentStyle.SPACES_4.name
        val indent = try { IndentStyle.valueOf(indentStr) } catch (_: Exception) { IndentStyle.SPACES_4 }

        // Parse recent files
        val recentFiles = try {
            val arr = JSONArray(p.getString(KEY_RECENT_FILES, "[]") ?: "[]")
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }

        // Parse custom snippets
        val customSnippets = try {
            val arr = JSONArray(p.getString(KEY_CUSTOM_SNIPPETS, "[]") ?: "[]")
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val obj = arr.getJSONObject(i)
                    Snippet(
                        id = obj.optString("id"),
                        trigger = obj.getString("trigger"),
                        description = obj.getString("description"),
                        body = obj.getString("body"),
                        language = obj.optString("language", "*"),
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }

        return EditorSettings(
            theme = theme,
            fontSize = p.getFloat(KEY_FONT_SIZE, 14f),
            fontFamily = p.getString(KEY_FONT_FAMILY, "Monospace") ?: "Monospace",
            wordWrap = p.getBoolean(KEY_WORD_WRAP, true),
            showLineNumbers = p.getBoolean(KEY_SHOW_LINE_NUMS, true),
            showMinimap = p.getBoolean(KEY_SHOW_MINIMAP, true),
            syntaxHighlight = p.getBoolean(KEY_SYNTAX_HIGHLIGHT, true),
            autoIndent = p.getBoolean(KEY_AUTO_INDENT, true),
            bracketMatching = p.getBoolean(KEY_BRACKET_MATCHING, true),
            autoCloseBrackets = p.getBoolean(KEY_AUTO_CLOSE_BRACKETS, true),
            indentStyle = indent,
            tabSize = p.getInt(KEY_TAB_SIZE, 4),
            showWhitespace = p.getBoolean(KEY_SHOW_WHITESPACE, false),
            highlightCurrentLine = p.getBoolean(KEY_HIGHLIGHT_CURRENT_LINE, true),
            smoothScrolling = p.getBoolean(KEY_SMOOTH_SCROLLING, true),
            zoom = p.getFloat(KEY_ZOOM, 1f),
            snippetsEnabled = p.getBoolean(KEY_SNIPPETS_ENABLED, true),
            autosaveEnabled = p.getBoolean(KEY_AUTOSAVE_ENABLED, true),
            autosaveIntervalMs = p.getLong(KEY_AUTOSAVE_INTERVAL, 30_000L),
            showBreadcrumb = p.getBoolean(KEY_SHOW_BREADCRUMB, true),
            showGitGutter = p.getBoolean(KEY_SHOW_GIT_GUTTER, false),
            trimTrailingWhitespace = p.getBoolean(KEY_TRIM_TRAILING, false),
            insertFinalNewline = p.getBoolean(KEY_INSERT_FINAL_NEWLINE, true),
            columnLimit = p.getInt(KEY_COLUMN_LIMIT, 80),
            showColumnGuide = p.getBoolean(KEY_SHOW_COLUMN_GUIDE, false),
            recentFiles = recentFiles,
            customSnippets = customSnippets,
        )
    }

    // ── Recent files helpers ──────────────────────────────────────

    fun addRecentFile(context: Context, path: String) {
        val settings = load(context)
        val updated = (listOf(path) + settings.recentFiles.filter { it != path }).take(20)
        save(context, settings.copy(recentFiles = updated))
    }

    fun clearRecentFiles(context: Context) {
        val settings = load(context)
        save(context, settings.copy(recentFiles = emptyList()))
    }

    // ── Session files (last open tabs) ───────────────────────────

    private const val KEY_SESSION = "session_files"

    fun saveSession(context: Context, openFiles: List<String>) {
        val arr = JSONArray().also { it -> openFiles.forEach { p -> it.put(p) } }
        prefs(context).edit().putString(KEY_SESSION, arr.toString()).apply()
    }

    fun saveWorkspaceLayout(context: Context, layout: WorkspaceLayout) {
        val obj = JSONObject().apply {
            put("orientation", layout.orientation.name)
            put("secondGroupVisible", layout.secondGroupVisible)
            put("secondGroupRatio", layout.secondGroupRatio.toDouble())
            put("primaryTabId", layout.primaryTabId)
            put("secondaryTabId", layout.secondaryTabId)
            put("primaryTabIds", JSONArray(layout.primaryTabIds))
            put("secondaryTabIds", JSONArray(layout.secondaryTabIds))
        }
        prefs(context).edit().putString(KEY_LAYOUT, obj.toString()).apply()
    }

    fun loadWorkspaceLayout(context: Context): WorkspaceLayout {
        return try {
            val obj = JSONObject(prefs(context).getString(KEY_LAYOUT, "{}") ?: "{}")
            val orientation = runCatching { SplitOrientation.valueOf(obj.optString("orientation", SplitOrientation.NONE.name)) }
                .getOrDefault(SplitOrientation.NONE)
            WorkspaceLayout(
                orientation = orientation,
                secondGroupVisible = obj.optBoolean("secondGroupVisible", false),
                secondGroupRatio = obj.optDouble("secondGroupRatio", 0.5).toFloat().coerceIn(0.25f, 0.75f),
                primaryTabId = obj.optString("primaryTabId", null),
                secondaryTabId = obj.optString("secondaryTabId", null),
                primaryTabIds = obj.optJSONArray("primaryTabIds")?.let { arr -> (0 until arr.length()).map { i -> arr.optString(i) }.filter { it.isNotEmpty() } } ?: emptyList(),
                secondaryTabIds = obj.optJSONArray("secondaryTabIds")?.let { arr -> (0 until arr.length()).map { i -> arr.optString(i) }.filter { it.isNotEmpty() } } ?: emptyList(),
            )
        } catch (_: Exception) { WorkspaceLayout() }
    }

    fun loadSession(context: Context): List<String> = try {
        val arr = JSONArray(prefs(context).getString(KEY_SESSION, "[]") ?: "[]")
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }
}

// ─────────────────────────────────────────────────────────────────
// File Utilities
// ─────────────────────────────────────────────────────────────────

object FileUtils {

    /** Detect encoding from BOM bytes */
    fun detectEncoding(bytes: ByteArray): FileEncoding = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> FileEncoding.UTF8_BOM
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> FileEncoding.UTF16_LE
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> FileEncoding.UTF16_BE
        else -> FileEncoding.UTF8
    }

    /** Read file bytes and strip BOM if needed */
    fun readFileContent(path: String): Pair<String, FileEncoding> {
        val bytes = java.io.File(path).readBytes()
        val encoding = detectEncoding(bytes)
        val stripped = when (encoding) {
            FileEncoding.UTF8_BOM -> bytes.drop(3).toByteArray()
            FileEncoding.UTF16_LE, FileEncoding.UTF16_BE -> bytes.drop(2).toByteArray()
            else -> bytes
        }
        return stripped.toString(encoding.charset) to encoding
    }

    /** Friendly file size string */
    fun humanSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.1f".format(bytes / 1024.0 / 1024.0)} MB"
    }

    /** Get file language name from extension */
    fun languageName(ext: String): String = when (ext) {
        "kt", "kts" -> "Kotlin"
        "java" -> "Java"
        "py", "pyw" -> "Python"
        "js", "jsx" -> "JavaScript"
        "ts", "tsx" -> "TypeScript"
        "rs" -> "Rust"
        "go" -> "Go"
        "html", "htm" -> "HTML"
        "css" -> "CSS"
        "scss" -> "SCSS"
        "json" -> "JSON"
        "md", "mdx" -> "Markdown"
        "sql" -> "SQL"
        "sh", "bash", "zsh" -> "Shell"
        "xml" -> "XML"
        "svg" -> "SVG"
        "yaml", "yml" -> "YAML"
        "toml" -> "TOML"
        "c" -> "C"
        "cpp", "cc", "cxx" -> "C++"
        "cs" -> "C#"
        "swift" -> "Swift"
        "rb" -> "Ruby"
        "php" -> "PHP"
        "dart" -> "Dart"
        "r" -> "R"
        "lua" -> "Lua"
        "txt" -> "Plain Text"
        else -> ext.uppercase()
    }

    /** Suggested new file templates */
    fun templateFor(ext: String): String = when (ext) {
        "kt" -> "package com.example\n\nfun main() {\n    println(\"Hello, World!\")\n}\n"
        "java" -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}\n"
        "py" -> "#!/usr/bin/env python3\n\ndef main():\n    print(\"Hello, World!\")\n\nif __name__ == \"__main__\":\n    main()\n"
        "html" -> "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Document</title>\n</head>\n<body>\n    \n</body>\n</html>\n"
        "css" -> "/* Styles */\n\n* {\n    box-sizing: border-box;\n    margin: 0;\n    padding: 0;\n}\n\nbody {\n    font-family: sans-serif;\n}\n"
        "md" -> "# Title\n\n## Introduction\n\n> Write something great here.\n\n## Contents\n\n- Item 1\n- Item 2\n\n## Conclusion\n"
        "json" -> "{\n    \n}\n"
        "sh" -> "#!/bin/bash\n\nset -euo pipefail\n\necho \"Hello, World!\"\n"
        "sql" -> "-- SQL Script\n\nSELECT *\nFROM table_name\nWHERE condition = 'value'\nLIMIT 100;\n"
        "yaml", "yml" -> "---\nname: example\nversion: 1.0.0\nconfig:\n  key: value\n"
        "toml" -> "[package]\nname = \"example\"\nversion = \"0.1.0\"\n\n[dependencies]\n"
        else -> ""
    }
}

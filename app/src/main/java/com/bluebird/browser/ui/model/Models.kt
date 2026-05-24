package com.win11launcher.browser.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════════════════════════════════

const val NEWTAB_URL = "bluebird://newtab"
const val BLANK_URL  = "about:blank"
const val MAX_HISTORY_ENTRIES = 1000
const val MAX_TABS = 30

// ═══════════════════════════════════════════════════════════════════════
// TAB MODEL
// ═══════════════════════════════════════════════════════════════════════

data class BrowserTab(
    val id: String              = UUID.randomUUID().toString(),
    var title: String           = "New Tab",
    var url: String             = NEWTAB_URL,
    var displayUrl: String      = "",           // shown in address bar (may differ from loaded url)
    val backStack: MutableList<String>    = mutableListOf(),
    val forwardStack: MutableList<String> = mutableListOf(),
    var faviconUrl: String?     = null,         // real favicon URL fetched from page
    var faviconColor: Long      = 0xFF1A73E8,   // fallback letter-avatar color
    var isPrivate: Boolean      = false,
    var isMuted: Boolean        = false,
    var isPinned: Boolean       = false,
    var isLoading: Boolean      = false,
    var loadProgress: Float     = 0f,
    var lastVisited: Long       = System.currentTimeMillis(),
    var scrollY: Int            = 0
)

// ═══════════════════════════════════════════════════════════════════════
// BOOKMARK MODEL
// ═══════════════════════════════════════════════════════════════════════

data class Bookmark(
    val id: String          = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val faviconColor: Long  = 0xFF1A73E8,
    val folder: String      = "Bookmarks Bar",
    val addedAt: Long       = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════════════
// HISTORY MODEL
// ═══════════════════════════════════════════════════════════════════════

data class HistoryEntry(
    val id: String          = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val faviconColor: Long  = 0xFF1A73E8,
    val visitedAt: Long     = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════════════
// DOWNLOAD MODEL
// ═══════════════════════════════════════════════════════════════════════

enum class DownloadStatus { DOWNLOADING, COMPLETED, FAILED, PAUSED }

data class DownloadItem(
    val id: String              = UUID.randomUUID().toString(),
    val downloadManagerId: Long = -1L,          // DownloadManager's own ID for querying
    val fileName: String,
    val url: String,
    val mimeType: String,
    val fileSize: Long          = 0L,
    var status: DownloadStatus  = DownloadStatus.DOWNLOADING,
    val startedAt: Long         = System.currentTimeMillis(),
    var progress: Float         = 0f,
    var bytesDownloaded: Long   = 0L
)

// ═══════════════════════════════════════════════════════════════════════
// SETTINGS MODEL
// ═══════════════════════════════════════════════════════════════════════

enum class SearchEngine(val label: String, val queryUrl: String, val homeUrl: String) {
    GOOGLE(    "Google",    "https://www.google.com/search?q=",          "https://www.google.com"),
    BING(      "Bing",      "https://www.bing.com/search?q=",            "https://www.bing.com"),
    DUCKDUCKGO("DuckDuckGo","https://duckduckgo.com/?q=",               "https://duckduckgo.com"),
    ECOSIA(    "Ecosia",    "https://www.ecosia.org/search?q=",          "https://www.ecosia.org"),
    BRAVE(     "Brave",     "https://search.brave.com/search?q=",        "https://search.brave.com")
}

enum class StartPage { NEW_TAB, BLANK, CONTINUE_WHERE_LEFT_OFF }

enum class BrowserPanel { NONE, BOOKMARKS, HISTORY, DOWNLOADS, SETTINGS, EXTENSIONS, COLLECTIONS }

data class BrowserSettings(
    val useBuiltInKeyboard: Boolean  = false,
    val searchEngine: SearchEngine   = SearchEngine.GOOGLE,
    val darkMode: Boolean            = false,
    val adBlockEnabled: Boolean      = true,
    val trackingProtection: Boolean  = true,
    val javaScriptEnabled: Boolean   = true,
    val saveCookies: Boolean         = true,
    val showBookmarksBar: Boolean    = true,
    val startPage: StartPage         = StartPage.CONTINUE_WHERE_LEFT_OFF,
    val fontSize: Int                = 100,      // percent
    val defaultZoom: Int             = 100,      // percent
    val showImages: Boolean          = true,
    val popupBlocker: Boolean        = true,
    val locationAccess: Boolean      = false,
    val cameraAccess: Boolean        = false,
    val microphoneAccess: Boolean    = false,
    val mixedContentAllowed: Boolean = false,
    val saveFormData: Boolean        = true,
    val desktopMode: Boolean         = false
)

// ═══════════════════════════════════════════════════════════════════════
// PERMISSION REQUEST MODEL
// ═══════════════════════════════════════════════════════════════════════

data class PermissionRequest(
    val origin: String,
    val resources: Array<String>,        // e.g. PermissionRequest.RESOURCE_VIDEO_CAPTURE
    val grant: () -> Unit,
    val deny: () -> Unit
) {
    override fun equals(other: Any?) = other is PermissionRequest && origin == other.origin
    override fun hashCode() = origin.hashCode()
}

// ═══════════════════════════════════════════════════════════════════════
// JS DIALOG MODEL
// ═══════════════════════════════════════════════════════════════════════

enum class JsDialogType { ALERT, CONFIRM, PROMPT }

data class JsDialogState(
    val type: JsDialogType,
    val message: String,
    val defaultValue: String = "",      // for PROMPT
    val onConfirm: (String) -> Unit,    // for CONFIRM: "true"/"false", PROMPT: user text
    val onDismiss: () -> Unit
)

// ═══════════════════════════════════════════════════════════════════════
// SSL DIALOG MODEL
// ═══════════════════════════════════════════════════════════════════════

data class SslDialogState(
    val host: String,
    val errorDescription: String,
    val onProceed: () -> Unit,
    val onCancel: () -> Unit
)

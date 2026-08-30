package com.io.github.norbertweb.bluebird.browser.model

import java.util.UUID

const val NEWTAB_URL = "io.github.norbertweb.bluebird://newtab"
const val BLANK_URL  = "about:blank"
const val MAX_HISTORY_ENTRIES = 1000
const val MAX_TABS = 30
const val MAX_DOWNLOAD_ENTRIES = 200

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(), var title: String = "New Tab", var url: String = NEWTAB_URL,
    var displayUrl: String = "", val backStack: MutableList<String> = mutableListOf(), val forwardStack: MutableList<String> = mutableListOf(),
    var faviconUrl: String? = null, var faviconColor: Long = 0xFF1A73E8, var isPrivate: Boolean = false, var isMuted: Boolean = false,
    var isPinned: Boolean = false, var isLoading: Boolean = false, var loadProgress: Float = 0f,
    var lastVisited: Long = System.currentTimeMillis(), var scrollY: Int = 0,
    var rendererDiscarded: Boolean = false, var checkpointAt: Long = 0L,
    var groupId: String? = null
)



data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Tab group",
    val color: Long = 0xFF1A73E8,
    val createdAt: Long = System.currentTimeMillis()
)

data class Bookmark(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val faviconUrl: String? = null,
    val faviconColor: Long = 0xFF1A73E8, val folder: String = "Bookmarks Bar", val addedAt: Long = System.currentTimeMillis())

data class BookmarkFolder(val id: String = UUID.randomUUID().toString(), val name: String, val parent: String = "Bookmarks Bar", val createdAt: Long = System.currentTimeMillis())

data class HistoryEntry(val id: String = UUID.randomUUID().toString(), val title: String, val url: String, val faviconUrl: String? = null,
    val faviconColor: Long = 0xFF1A73E8, val visitedAt: Long = System.currentTimeMillis())

enum class DownloadStatus { DOWNLOADING, COMPLETED, FAILED, PAUSED, CANCELLED }

data class DownloadItem(val id: String = UUID.randomUUID().toString(), val downloadManagerId: Long = -1L, val fileName: String,
    val url: String, val mimeType: String, val fileSize: Long = 0L, var status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val startedAt: Long = System.currentTimeMillis(), var progress: Float = 0f, var bytesDownloaded: Long = 0L)

enum class SearchEngine(val label: String, val queryUrl: String, val homeUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q=", "https://www.google.com"),
    BING("Bing", "https://www.bing.com/search?q=", "https://www.bing.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "https://duckduckgo.com"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=", "https://www.ecosia.org"),
    BRAVE("Brave", "https://search.brave.com/search?q=", "https://search.brave.com")
}

enum class StartPage { NEW_TAB, BLANK, CONTINUE_WHERE_LEFT_OFF }
enum class BrowserPanel { NONE, BOOKMARKS, HISTORY, DOWNLOADS, SETTINGS, EXTENSIONS, COLLECTIONS, CHATGPT, SITE_SETTINGS, PASSWORDS }

data class BrowserSettings(val searchEngine: SearchEngine = SearchEngine.GOOGLE, val darkMode: Boolean = false,
    val adBlockEnabled: Boolean = true, val trackingProtection: Boolean = true, val javaScriptEnabled: Boolean = true,
    val saveCookies: Boolean = true, val showBookmarksBar: Boolean = true,
    val startPage: StartPage = StartPage.CONTINUE_WHERE_LEFT_OFF, val fontSize: Int = 100, val defaultZoom: Int = 100,
    val showImages: Boolean = true, val popupBlocker: Boolean = true, val locationAccess: Boolean = false,
    val cameraAccess: Boolean = false, val microphoneAccess: Boolean = false, val mixedContentAllowed: Boolean = false,
    val saveFormData: Boolean = true, val desktopMode: Boolean = false,
    val offerToSavePasswords: Boolean = true, val autofillPasswords: Boolean = true,
    val requireDeviceAuthForPasswords: Boolean = true,
    val homeShowQuickLinks: Boolean = true,
    val homeShowRecentSites: Boolean = true,
    val homeShowBookmarks: Boolean = true,
    val homeShowNews: Boolean = true,
    val homeShowWallpaper: Boolean = true)

enum class StoredPermissionDecision { ALLOW, DENY }

data class NewsSubscription(
    val id: String,
    val name: String,
    val feedUrl: String,
    val enabled: Boolean = true
)

data class NewsArticle(
    val id: String,
    val source: String,
    val title: String,
    val url: String,
    val publishedAt: Long = 0L,
    val summary: String = ""
)

data class SitePermission(val origin: String, val resource: String, val decision: StoredPermissionDecision,
    val updatedAt: Long = System.currentTimeMillis())

data class PermissionRequest(val origin: String, val resources: Array<String>, val grant: () -> Unit, val deny: () -> Unit,
    val remember: ((StoredPermissionDecision) -> Unit)? = null) {
    override fun equals(other: Any?) = other is PermissionRequest && origin == other.origin
    override fun hashCode() = origin.hashCode()
}

enum class JsDialogType { ALERT, CONFIRM, PROMPT }
data class JsDialogState(val type: JsDialogType, val message: String, val defaultValue: String = "",
    val onConfirm: (String) -> Unit, val onDismiss: () -> Unit)

data class SslDialogState(val host: String, val errorDescription: String, val onProceed: () -> Unit, val onCancel: () -> Unit)

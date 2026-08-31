package io.github.norbertweb.bluebird.browser.data

import android.content.Context
import android.content.SharedPreferences
import io.github.norbertweb.bluebird.browser.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════
// BrowserRepository — single source of truth for all persisted state
// Uses SharedPreferences (JSON) — no Room dependency needed for
// a single-file bundle that fits the launcher project structure.
// ═══════════════════════════════════════════════════════════════════════

class BrowserRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bluebird_browser", Context.MODE_PRIVATE)

    // ─── SETTINGS ───────────────────────────────────────────────────

    fun saveSettings(s: BrowserSettings) {
        prefs.edit().apply {
            putString ("s_searchEngine",    s.searchEngine.name)
            putBoolean("s_darkMode",        s.darkMode)
            putBoolean("s_adBlock",         s.adBlockEnabled)
            putBoolean("s_tracking",        s.trackingProtection)
            putBoolean("s_js",              s.javaScriptEnabled)
            putBoolean("s_cookies",         s.saveCookies)
            putBoolean("s_bookmarksBar",    s.showBookmarksBar)
            putString ("s_startPage",       s.startPage.name)
            putInt    ("s_fontSize",        s.fontSize)
            putInt    ("s_zoom",            s.defaultZoom)
            putBoolean("s_images",          s.showImages)
            putBoolean("s_popups",          s.popupBlocker)
            putBoolean("s_location",        s.locationAccess)
            putBoolean("s_camera",          s.cameraAccess)
            putBoolean("s_mic",             s.microphoneAccess)
            putBoolean("s_mixedContent",    s.mixedContentAllowed)
            putBoolean("s_formData",        s.saveFormData)
            putBoolean("s_desktopMode",     s.desktopMode)
            putBoolean("s_offerSavePasswords", s.offerToSavePasswords)
            putBoolean("s_autofillPasswords", s.autofillPasswords)
            putBoolean("s_requireDeviceAuthPasswords", s.requireDeviceAuthForPasswords)
            putBoolean("s_homeQuickLinks", s.homeShowQuickLinks)
            putBoolean("s_homeRecentSites", s.homeShowRecentSites)
            putBoolean("s_homeBookmarks", s.homeShowBookmarks)
            putBoolean("s_homeNews", s.homeShowNews)
            putBoolean("s_homeWallpaper", s.homeShowWallpaper)
        }.apply()
    }

    fun loadSettings(): BrowserSettings = BrowserSettings(
        searchEngine        = runCatching {
            SearchEngine.valueOf(prefs.getString("s_searchEngine", "GOOGLE")!!)
        }.getOrDefault(SearchEngine.GOOGLE),
        darkMode            = prefs.getBoolean("s_darkMode",     false),
        adBlockEnabled      = prefs.getBoolean("s_adBlock",      true),
        trackingProtection  = prefs.getBoolean("s_tracking",     true),
        javaScriptEnabled   = prefs.getBoolean("s_js",           true),
        saveCookies         = prefs.getBoolean("s_cookies",      true),
        showBookmarksBar    = prefs.getBoolean("s_bookmarksBar", true),
        startPage           = runCatching {
            StartPage.valueOf(prefs.getString("s_startPage", "CONTINUE_WHERE_LEFT_OFF")!!)
        }.getOrDefault(StartPage.CONTINUE_WHERE_LEFT_OFF),
        fontSize            = prefs.getInt("s_fontSize",  100),
        defaultZoom         = prefs.getInt("s_zoom",      100),
        showImages          = prefs.getBoolean("s_images",       true),
        popupBlocker        = prefs.getBoolean("s_popups",       true),
        locationAccess      = prefs.getBoolean("s_location",     false),
        cameraAccess        = prefs.getBoolean("s_camera",       false),
        microphoneAccess    = prefs.getBoolean("s_mic",          false),
        mixedContentAllowed = prefs.getBoolean("s_mixedContent", false),
        saveFormData        = prefs.getBoolean("s_formData",     true),
        desktopMode         = prefs.getBoolean("s_desktopMode",  false),
        offerToSavePasswords = prefs.getBoolean("s_offerSavePasswords", true),
        autofillPasswords   = prefs.getBoolean("s_autofillPasswords", true),
        requireDeviceAuthForPasswords = prefs.getBoolean("s_requireDeviceAuthPasswords", true),
        homeShowQuickLinks  = prefs.getBoolean("s_homeQuickLinks", true),
        homeShowRecentSites = prefs.getBoolean("s_homeRecentSites", true),
        homeShowBookmarks   = prefs.getBoolean("s_homeBookmarks", true),
        homeShowNews        = prefs.getBoolean("s_homeNews", true),
        homeShowWallpaper   = prefs.getBoolean("s_homeWallpaper", true)
    )

    // ─── TABS ────────────────────────────────────────────────────────

    fun saveTabs(tabs: List<BrowserTab>, activeTabId: String) {
        val arr = JSONArray()
        tabs.forEach { tab ->
            // Don't persist private tabs
            if (tab.isPrivate) return@forEach
            val obj = JSONObject().apply {
                put("id",           tab.id)
                put("title",        tab.title)
                put("url",          tab.url)
                put("displayUrl",   tab.displayUrl)
                put("faviconColor", tab.faviconColor)
                put("isPinned",     tab.isPinned)
                put("lastVisited",  tab.lastVisited)
                put("scrollY",      tab.scrollY)
                put("rendererDiscarded", tab.rendererDiscarded)
                put("checkpointAt", tab.checkpointAt)
                put("groupId", tab.groupId)
                val back = JSONArray(); tab.backStack.forEach { back.put(it) }; put("backStack", back)
                val fwd  = JSONArray(); tab.forwardStack.forEach { fwd.put(it) }; put("forwardStack", fwd)
            }
            arr.put(obj)
        }
        prefs.edit()
            .putString("tabs_json",    arr.toString())
            .putString("active_tab",   activeTabId)
            .apply()
    }

    /** Crash-oriented checkpoint: commits tab/session state synchronously.
     * Kept separate from the normal debounced save so lifecycle transitions
     * can force the latest browser state to disk before the process is killed.
     */
    fun checkpointTabs(tabs: List<BrowserTab>, activeTabId: String) {
        val arr = JSONArray()
        tabs.forEach { tab ->
            if (tab.isPrivate) return@forEach
            arr.put(JSONObject().apply {
                put("id", tab.id); put("title", tab.title); put("url", tab.url)
                put("displayUrl", tab.displayUrl); put("faviconColor", tab.faviconColor)
                put("isPinned", tab.isPinned); put("lastVisited", tab.lastVisited)
                put("scrollY", tab.scrollY); put("rendererDiscarded", tab.rendererDiscarded)
                put("checkpointAt", tab.checkpointAt)
                put("groupId", tab.groupId)
                val back = JSONArray(); tab.backStack.forEach { back.put(it) }; put("backStack", back)
                val fwd = JSONArray(); tab.forwardStack.forEach { fwd.put(it) }; put("forwardStack", fwd)
            })
        }
        prefs.edit().putString("tabs_json", arr.toString()).putString("active_tab", activeTabId).commit()
    }

    fun loadTabs(): Pair<List<BrowserTab>, String> {
        val raw = prefs.getString("tabs_json", null) ?: return Pair(emptyList(), "")
        val activeId = prefs.getString("active_tab", "") ?: ""
        return try {
            val arr = JSONArray(raw)
            val tabs = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val backArr  = obj.optJSONArray("backStack")  ?: JSONArray()
                val fwdArr   = obj.optJSONArray("forwardStack") ?: JSONArray()
                BrowserTab(
                    id           = obj.getString("id"),
                    title        = obj.optString("title", "New Tab"),
                    url          = obj.optString("url", NEWTAB_URL),
                    displayUrl   = obj.optString("displayUrl", ""),
                    faviconColor = obj.optLong("faviconColor", 0xFF1A73E8),
                    isPinned     = obj.optBoolean("isPinned", false),
                    lastVisited  = obj.optLong("lastVisited", System.currentTimeMillis()),
                    scrollY      = obj.optInt("scrollY", 0),
                    rendererDiscarded = obj.optBoolean("rendererDiscarded", false),
                    checkpointAt = obj.optLong("checkpointAt", 0L),
                    groupId      = obj.optString("groupId", "").takeIf { it.isNotBlank() },
                    backStack    = (0 until backArr.length()).map { backArr.getString(it) }.toMutableList(),
                    forwardStack = (0 until fwdArr.length()).map { fwdArr.getString(it) }.toMutableList()
                )
            }
            Pair(tabs, activeId)
        } catch (e: Exception) {
            Pair(emptyList(), "")
        }
    }

    // ─── TAB GROUPS ──────────────────────────────────────────────────

    fun saveTabGroups(groups: List<TabGroup>) {
        val arr = JSONArray()
        groups.forEach { group ->
            arr.put(JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("color", group.color)
                put("createdAt", group.createdAt)
            })
        }
        prefs.edit().putString("tab_groups_json", arr.toString()).apply()
    }

    fun loadTabGroups(): List<TabGroup> {
        val raw = prefs.getString("tab_groups_json", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TabGroup(
                    id = obj.optString("id"),
                    name = obj.optString("name", "Tab group"),
                    color = obj.optLong("color", 0xFF1A73E8),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }.getOrElse { emptyList() }
    }

    // ─── BOOKMARKS ───────────────────────────────────────────────────

    fun saveBookmarks(bookmarks: List<Bookmark>) {
        val arr = JSONArray()
        bookmarks.forEach { bm ->
            arr.put(JSONObject().apply {
                put("id",           bm.id)
                put("title",        bm.title)
                put("url",          bm.url)
                put("faviconColor", bm.faviconColor)
                put("folder",       bm.folder)
                put("addedAt",      bm.addedAt)
            })
        }
        prefs.edit().putString("bookmarks_json", arr.toString()).apply()
    }

    fun loadBookmarks(): List<Bookmark> {
        val raw = prefs.getString("bookmarks_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Bookmark(
                    id           = obj.getString("id"),
                    title        = obj.optString("title", ""),
                    url          = obj.optString("url", ""),
                    faviconColor = obj.optLong("faviconColor", 0xFF1A73E8),
                    folder       = obj.optString("folder", "Bookmarks Bar"),
                    addedAt      = obj.optLong("addedAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    // ─── BOOKMARK FOLDERS ─────────────────────────────────────────────

    fun saveBookmarkFolders(folders: List<BookmarkFolder>) {
        val arr = JSONArray()
        folders.filter { it.name.isNotBlank() && it.name != "Bookmarks Bar" }.take(100).forEach { folder ->
            arr.put(JSONObject().apply {
                put("id", folder.id); put("name", folder.name.trim()); put("parent", folder.parent); put("createdAt", folder.createdAt)
            })
        }
        prefs.edit().putString("bookmark_folders_json", arr.toString()).apply()
    }

    fun loadBookmarkFolders(): List<BookmarkFolder> {
        val raw = prefs.getString("bookmark_folders_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BookmarkFolder(o.optString("id", UUID.randomUUID().toString()), o.optString("name", ""), o.optString("parent", "Bookmarks Bar"), o.optLong("createdAt", System.currentTimeMillis()))
            }.filter { it.name.isNotBlank() && it.name != "Bookmarks Bar" }
        } catch (_: Exception) { emptyList() }
    }

    // ─── HISTORY ─────────────────────────────────────────────────────

    fun saveHistory(history: List<HistoryEntry>) {
        // Only persist last 500 entries to cap file size
        val arr = JSONArray()
        history.take(500).forEach { entry ->
            arr.put(JSONObject().apply {
                put("id",           entry.id)
                put("title",        entry.title)
                put("url",          entry.url)
                put("faviconColor", entry.faviconColor)
                put("visitedAt",    entry.visitedAt)
            })
        }
        prefs.edit().putString("history_json", arr.toString()).apply()
    }

    fun loadHistory(): List<HistoryEntry> {
        val raw = prefs.getString("history_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryEntry(
                    id           = obj.getString("id"),
                    title        = obj.optString("title", ""),
                    url          = obj.optString("url", ""),
                    faviconColor = obj.optLong("faviconColor", 0xFF1A73E8),
                    visitedAt    = obj.optLong("visitedAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) { emptyList() }
    }


    // ─── DOWNLOADS ───────────────────────────────────────────────────

    fun saveDownloads(downloads: List<DownloadItem>) {
        val arr = JSONArray()
        downloads.take(MAX_DOWNLOAD_ENTRIES).forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("downloadManagerId", item.downloadManagerId)
                put("fileName", item.fileName)
                put("url", item.url)
                put("mimeType", item.mimeType)
                put("fileSize", item.fileSize)
                put("status", item.status.name)
                put("startedAt", item.startedAt)
                put("progress", item.progress.toDouble())
                put("bytesDownloaded", item.bytesDownloaded)
            })
        }
        prefs.edit().putString("downloads_json", arr.toString()).apply()
    }

    fun loadDownloads(): List<DownloadItem> {
        val raw = prefs.getString("downloads_json", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                DownloadItem(
                    id = obj.optString("id"),
                    downloadManagerId = obj.optLong("downloadManagerId", -1L),
                    fileName = obj.optString("fileName", "Download"),
                    url = obj.optString("url", ""),
                    mimeType = obj.optString("mimeType", "*/*"),
                    fileSize = obj.optLong("fileSize", 0L),
                    status = runCatching { DownloadStatus.valueOf(obj.optString("status", DownloadStatus.FAILED.name)) }.getOrDefault(DownloadStatus.FAILED),
                    startedAt = obj.optLong("startedAt", System.currentTimeMillis()),
                    progress = obj.optDouble("progress", 0.0).toFloat(),
                    bytesDownloaded = obj.optLong("bytesDownloaded", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun clearDownloads() = prefs.edit().remove("downloads_json").apply()

    // ─── SITE PERMISSIONS ───────────────────────────────────────────

    fun saveSitePermission(permission: SitePermission) {
        val all = loadSitePermissions().filterNot { it.origin == permission.origin && it.resource == permission.resource }.toMutableList()
        all.add(0, permission)
        val arr = JSONArray()
        all.take(200).forEach { p ->
            arr.put(JSONObject().apply {
                put("origin", p.origin)
                put("resource", p.resource)
                put("decision", p.decision.name)
                put("updatedAt", p.updatedAt)
            })
        }
        prefs.edit().putString("site_permissions_json", arr.toString()).apply()
    }

    fun getSitePermission(origin: String, resource: String): StoredPermissionDecision? =
        loadSitePermissions().firstOrNull { it.origin == origin && it.resource == resource }?.decision

    fun loadSitePermissions(): List<SitePermission> {
        val raw = prefs.getString("site_permissions_json", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SitePermission(
                    origin = obj.optString("origin", ""),
                    resource = obj.optString("resource", ""),
                    decision = runCatching { StoredPermissionDecision.valueOf(obj.optString("decision", StoredPermissionDecision.DENY.name)) }.getOrDefault(StoredPermissionDecision.DENY),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
    }

    fun removeSitePermission(origin: String, resource: String) {
        val remaining = loadSitePermissions().filterNot { it.origin == origin && it.resource == resource }
        val arr = JSONArray()
        remaining.forEach { p -> arr.put(JSONObject().apply { put("origin", p.origin); put("resource", p.resource); put("decision", p.decision.name); put("updatedAt", p.updatedAt) }) }
        prefs.edit().putString("site_permissions_json", arr.toString()).apply()
    }

    fun clearSitePermissions() = prefs.edit().remove("site_permissions_json").apply()

    // ─── CLEAR ───────────────────────────────────────────────────────

    fun clearHistory() = prefs.edit().remove("history_json").apply()

    fun clearCookies(context: Context) {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
    }

    fun clearCache(context: Context) {
        val wv = android.webkit.WebView(context)
        wv.clearCache(true)
        wv.destroy()
    }

    fun clearSiteStorage() {
        android.webkit.WebStorage.getInstance().deleteAllData()
    }

    fun clearFormData(context: Context) {
        val wv = android.webkit.WebView(context)
        wv.clearFormData()
        wv.destroy()
    }

    fun clearCookiesAndCache(context: Context) {
        clearCookies(context)
        clearCache(context)
    }

    fun clearAll() = prefs.edit().clear().apply()

    companion object {
        @Volatile private var INSTANCE: BrowserRepository? = null
        fun get(context: Context): BrowserRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BrowserRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}

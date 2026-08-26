package com.win11launcher.browser.data

import android.content.Context
import android.content.SharedPreferences
import com.win11launcher.browser.model.*
import org.json.JSONArray
import org.json.JSONObject

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
            putBoolean("s_builtInKb",       s.useBuiltInKeyboard)
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
        }.apply()
    }

    fun loadSettings(): BrowserSettings = BrowserSettings(
        useBuiltInKeyboard  = prefs.getBoolean("s_builtInKb",    false),
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
        desktopMode         = prefs.getBoolean("s_desktopMode",  false)
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
                    backStack    = (0 until backArr.length()).map { backArr.getString(it) }.toMutableList(),
                    forwardStack = (0 until fwdArr.length()).map { fwdArr.getString(it) }.toMutableList()
                )
            }
            Pair(tabs, activeId)
        } catch (e: Exception) {
            Pair(emptyList(), "")
        }
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

    // ─── CLEAR ───────────────────────────────────────────────────────

    fun clearHistory() = prefs.edit().remove("history_json").apply()

    fun clearCookiesAndCache(context: Context) {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        val wv = android.webkit.WebView(context)
        wv.clearCache(true)
        wv.destroy()
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

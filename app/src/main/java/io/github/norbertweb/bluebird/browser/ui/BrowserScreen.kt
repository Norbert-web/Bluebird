package com.io.github.norbertweb.bluebird.browser.ui

// Use model alias to avoid clashing with android.webkit.PermissionRequest
import android.content.Intent
import android.print.PrintManager
import android.webkit.GeolocationPermissions
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.io.github.norbertweb.bluebird.browser.data.BrowserRepository
import com.io.github.norbertweb.bluebird.browser.model.Bookmark
import com.io.github.norbertweb.bluebird.browser.model.BrowserPanel
import com.io.github.norbertweb.bluebird.browser.model.BrowserSettings
import com.io.github.norbertweb.bluebird.browser.model.BrowserTab
import com.io.github.norbertweb.bluebird.browser.model.DownloadItem
import com.io.github.norbertweb.bluebird.browser.model.HistoryEntry
import com.io.github.norbertweb.bluebird.browser.model.JsDialogState
import com.io.github.norbertweb.bluebird.browser.model.MAX_HISTORY_ENTRIES
import com.io.github.norbertweb.bluebird.browser.model.MAX_TABS
import com.io.github.norbertweb.bluebird.browser.model.NEWTAB_URL
import com.io.github.norbertweb.bluebird.browser.model.SslDialogState
import com.io.github.norbertweb.bluebird.browser.ui.components.BookmarksBar
import com.io.github.norbertweb.bluebird.browser.ui.components.EdgeNavigationBar
import com.io.github.norbertweb.bluebird.browser.ui.components.EdgeTabBar
import com.io.github.norbertweb.bluebird.browser.ui.components.FindInPageBar
import com.io.github.norbertweb.bluebird.browser.ui.components.GeolocationDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.JsDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.PermissionRequestDialog
import com.io.github.norbertweb.bluebird.browser.ui.components.SslWarningDialog
import com.io.github.norbertweb.bluebird.browser.ui.keyboard.FloatingKeyboard
import com.io.github.norbertweb.bluebird.browser.ui.newtab.NewTabPage
import com.io.github.norbertweb.bluebird.browser.ui.panels.AddressSuggestionsDropdown
import com.io.github.norbertweb.bluebird.browser.ui.panels.EdgeContextMenu
import com.io.github.norbertweb.bluebird.browser.ui.panels.SidePanel
import com.io.github.norbertweb.bluebird.browser.ui.panels.TabOverviewGrid
import com.io.github.norbertweb.bluebird.browser.ui.webview.BrowserWebView
import com.io.github.norbertweb.bluebird.browser.utils.DownloadHelper
import com.io.github.norbertweb.bluebird.browser.utils.UrlUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.io.github.norbertweb.bluebird.browser.model.PermissionRequest as BrowserPermissionRequest

// ═══════════════════════════════════════════════════════════════════════
// BrowserScreen — root composable
// All state is initialized from the repository (persisted) and saved
// back on every meaningful change via a debounced coroutine.
// ═══════════════════════════════════════════════════════════════════════

private data class BrowserPersistedState(
    val tabs: Pair<List<BrowserTab>, String>,
    val settings: BrowserSettings,
    val bookmarks: List<Bookmark>,
    val history: List<HistoryEntry>,
)

@Composable
fun BrowserScreen() {
    val context    = LocalContext.current
    val repo       = remember { BrowserRepository.get(context) }
    val scope      = rememberCoroutineScope()
    val dlHelper   = remember { DownloadHelper(context) }

    // ── Persistence: lazy, off-main initialization ────────────────────
    // JSON parsing of history/tabs is deferred until after the first frame.
    var browserInitialized by remember { mutableStateOf(false) }
    val tabs = remember {
        androidx.compose.runtime.snapshots.SnapshotStateList<BrowserTab>().also { it.add(BrowserTab()) }
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var settings by remember { mutableStateOf(BrowserSettings()) }
    val bookmarks = remember { androidx.compose.runtime.snapshots.SnapshotStateList<Bookmark>() }
    val history = remember { androidx.compose.runtime.snapshots.SnapshotStateList<HistoryEntry>() }

    LaunchedEffect(repo) {
        val loaded = withContext(kotlinx.coroutines.Dispatchers.IO) {
            BrowserPersistedState(
                tabs = repo.loadTabs(),
                settings = repo.loadSettings(),
                bookmarks = repo.loadBookmarks(),
                history = repo.loadHistory()
            )
        }
        if (!isActive) return@LaunchedEffect
        val (savedTabs, savedActiveId) = loaded.tabs
        tabs.clear()
        tabs.addAll(if (savedTabs.isEmpty()) listOf(BrowserTab()) else savedTabs)
        activeTabId = if (savedActiveId.isNotEmpty() && tabs.any { it.id == savedActiveId }) savedActiveId else tabs.first().id
        settings = loaded.settings
        bookmarks.clear(); bookmarks.addAll(loaded.bookmarks)
        history.clear(); history.addAll(loaded.history)
        browserInitialized = true
    }

    val downloads = remember {
        androidx.compose.runtime.snapshots.SnapshotStateList<DownloadItem>()
    }

    // ── WebView instance map (tab id → WebView) ───────────────────────
    val webViews = remember { mutableMapOf<String, WebView>() }

    // ── UI state ──────────────────────────────────────────────────────
    var addressText       by remember { mutableStateOf("") }
    var addressBarFocused by remember { mutableStateOf(false) }
    var showMenu          by remember { mutableStateOf(false) }
    var showTabOverview   by remember { mutableStateOf(false) }
    var activePanel       by remember { mutableStateOf(BrowserPanel.NONE) }
    var findQuery         by remember { mutableStateOf("") }
    var isFindActive      by remember { mutableStateOf(false) }
    var findActiveMatch   by remember { mutableStateOf(0) }
    var findTotalMatches  by remember { mutableStateOf(0) }
    var currentZoom       by remember { mutableStateOf(settings.defaultZoom) }
    var jsDialog          by remember { mutableStateOf<JsDialogState?>(null) }
    var sslDialog         by remember { mutableStateOf<SslDialogState?>(null) }
    var permDialog        by remember { mutableStateOf<BrowserPermissionRequest?>(null) }
    var geoDialog         by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }
    var showKb            by remember { mutableStateOf(false) }

    // ── Derived ───────────────────────────────────────────────────────
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    val isDark    = settings.darkMode

    // ── Colors ────────────────────────────────────────────────────────
    val surfaceColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF3F3F3)
    val navBarBg     = if (isDark) Color(0xFF252525) else Color(0xFFF3F3F3)
    val borderColor  = if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD)
    val textColor    = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)
    val mainBg       = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)

    // ═════════════════════════════════════════════════════════════════
    // Debounced persistence save
    // ═════════════════════════════════════════════════════════════════

    var saveJob by remember { mutableStateOf<Job?>(null) }
    fun scheduleSave() {
        if (!browserInitialized) return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(800)
            repo.saveTabs(tabs, activeTabId)
            repo.saveBookmarks(bookmarks)
            repo.saveHistory(history)
            repo.saveSettings(settings)
        }
    }

    LaunchedEffect(activeTabId)    { scheduleSave() }
    LaunchedEffect(tabs.size)      { scheduleSave() }
    LaunchedEffect(settings)       { scheduleSave() }
    LaunchedEffect(bookmarks.size) { scheduleSave() }
    LaunchedEffect(history.size)   { scheduleSave() }

    DisposableEffect(Unit) {
        onDispose {
            if (!browserInitialized) { dlHelper.destroy(); return@onDispose }
            repo.saveTabs(tabs, activeTabId)
            repo.saveBookmarks(bookmarks)
            repo.saveHistory(history)
            repo.saveSettings(settings)
            dlHelper.destroy()
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Helper functions
    // ═════════════════════════════════════════════════════════════════

    fun activeWebView() = webViews[activeTabId]

    fun updateTab(id: String, block: BrowserTab.() -> Unit) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx >= 0) {
            tabs[idx] = tabs[idx].copy().also(block)
        }
    }

    fun navigate(rawUrl: String, tabId: String = activeTabId) {
        val url    = UrlUtils.resolveUrl(rawUrl, settings.searchEngine)
        val tabIdx = tabs.indexOfFirst { it.id == tabId }
        if (tabIdx < 0) return
        val tab = tabs[tabIdx]
        if (tab.url.isNotEmpty() && tab.url != NEWTAB_URL) {
            tab.backStack.add(tab.url)
            tab.forwardStack.clear()
        }
        tabs[tabIdx] = tab.copy(url = url, displayUrl = UrlUtils.displayUrl(url))
        addressText       = UrlUtils.displayUrl(url)
        addressBarFocused = false
        showKb            = false
        if (url != NEWTAB_URL) webViews[tabId]?.loadUrl(url)
        scheduleSave()
    }

    fun addTab(url: String = NEWTAB_URL, isPrivate: Boolean = false): String {
        if (tabs.size >= MAX_TABS) return activeTabId
        val tab = BrowserTab(url = url, isPrivate = isPrivate,
            faviconColor = UrlUtils.colorForUrl(url))
        tabs.add(tab)
        activeTabId = tab.id
        addressText = UrlUtils.displayUrl(url)
        scheduleSave()
        return tab.id
    }

    fun closeTab(tab: BrowserTab) {
        webViews.remove(tab.id)?.destroy()
        val idx = tabs.indexOf(tab)
        tabs.remove(tab)
        if (tabs.isEmpty()) {
            addTab()
        } else if (tab.id == activeTabId) {
            activeTabId = tabs.getOrNull(idx)?.id ?: tabs.last().id
        }
        scheduleSave()
    }

    fun goBack() {
        val tab = activeTab
        val wv  = activeWebView()
        if (wv?.canGoBack() == true) {
            wv.goBack()
        } else if (tab.backStack.isNotEmpty()) {
            val prev = tab.backStack.removeLast()
            tab.forwardStack.add(tab.url)
            updateTab(tab.id) { this.url = prev; this.displayUrl = UrlUtils.displayUrl(prev) }
            wv?.loadUrl(prev)
        }
    }

    fun goForward() {
        val tab = activeTab
        val wv  = activeWebView()
        if (wv?.canGoForward() == true) {
            wv.goForward()
        } else if (tab.forwardStack.isNotEmpty()) {
            val next = tab.forwardStack.removeLast()
            tab.backStack.add(tab.url)
            updateTab(tab.id) { this.url = next; this.displayUrl = UrlUtils.displayUrl(next) }
            wv?.loadUrl(next)
        }
    }

    fun toggleBookmark() {
        val url = activeTab.url
        if (url == NEWTAB_URL || url.isBlank()) return
        val existing = bookmarks.indexOfFirst { it.url == url }
        if (existing >= 0) {
            bookmarks.removeAt(existing)
        } else {
            bookmarks.add(0, Bookmark(
                title        = activeTab.title.ifBlank { url },
                url          = url,
                faviconColor = activeTab.faviconColor,
                folder       = "Bookmarks Bar"
            ))
        }
        scheduleSave()
    }

    fun addHistoryEntry(url: String, title: String, color: Long) {
        if (url == NEWTAB_URL || url.isBlank() || activeTab.isPrivate) return
        history.removeIf { it.url == url }
        history.add(0, HistoryEntry(
            title        = title.ifBlank { url },
            url          = url,
            faviconColor = color
        ))
        while (history.size > MAX_HISTORY_ENTRIES) history.removeLast()
        scheduleSave()
    }

    fun setZoom(zoom: Int) {
        currentZoom = zoom.coerceIn(25, 500)
        activeWebView()?.setInitialScale(currentZoom)
        settings = settings.copy(defaultZoom = currentZoom)
        scheduleSave()
    }

    fun sharePage() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, activeTab.url)
            putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    fun printPage() {
        val wv = activeWebView() ?: return
        val pm = context.getSystemService(PrintManager::class.java)
        val jobName = activeTab.title.ifBlank { "Bluebird Page" }
        val adapter = wv.createPrintDocumentAdapter(jobName)
        pm.print(jobName, adapter, android.print.PrintAttributes.Builder().build())
    }

    // ── Back handler ──────────────────────────────────────────────────
    BackHandler {
        when {
            showMenu          -> showMenu = false
            showTabOverview   -> showTabOverview = false
            activePanel != BrowserPanel.NONE -> activePanel = BrowserPanel.NONE
            isFindActive      -> {
                isFindActive = false; findQuery = ""
                activeWebView()?.clearMatches()
            }
            addressBarFocused -> addressBarFocused = false
            else              -> goBack()
        }
    }

    // ── Sync address bar when tab changes ─────────────────────────────
    LaunchedEffect(activeTabId) {
        val tab = tabs.firstOrNull { it.id == activeTabId }
        if (tab != null) {
            addressText = UrlUtils.displayUrl(tab.url)
            currentZoom = settings.defaultZoom
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Layout
    // ═════════════════════════════════════════════════════════════════

    val isBookmarked = bookmarks.any { it.url == activeTab.url }
    val canGoBack    = (activeWebView()?.canGoBack() == true) || activeTab.backStack.isNotEmpty()
    val canGoForward = (activeWebView()?.canGoForward() == true) || activeTab.forwardStack.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainBg)
    ) {
        // ── Tab Bar ────────────────────────────────────────────────────
        EdgeTabBar(
            tabs            = tabs,
            activeTabId     = activeTabId,
            isDark          = isDark,
            tabBarBg        = navBarBg,
            borderColor     = borderColor,
            onTabSelected   = { tab ->
                activeTabId     = tab.id
                addressText     = UrlUtils.displayUrl(tab.url)
                currentZoom     = settings.defaultZoom
                showTabOverview = false
            },
            onTabClosed     = { closeTab(it) },
            onNewTab        = { addTab(); showTabOverview = false },
            onNewPrivateTab = { addTab(isPrivate = true); showTabOverview = false },
            onTabOverview   = { showTabOverview = !showTabOverview }
        )

        // ── Navigation Bar ─────────────────────────────────────────────
        EdgeNavigationBar(
            isDark              = isDark,
            navBarBg            = navBarBg,
            borderColor         = borderColor,
            isLoading           = activeTab.isLoading,
            loadingProgress     = activeTab.loadProgress,
            canGoBack           = canGoBack,
            canGoForward        = canGoForward,
            addressText         = addressText,
            addressBarFocused   = addressBarFocused,
            isBookmarked        = isBookmarked,
            useBuiltInKb        = settings.useBuiltInKeyboard,
            isPrivateTab        = activeTab.isPrivate,
            activeUrl           = activeTab.url,
            zoomLevel           = currentZoom,
            onBack              = { goBack() },
            onForward           = { goForward() },
            onRefresh           = {
                if (activeTab.isLoading) activeWebView()?.stopLoading()
                else activeWebView()?.reload()
            },
            onHome              = { navigate(settings.searchEngine.homeUrl) },
            onAddressChange     = { text ->
                addressText = text
                if (!addressBarFocused) addressBarFocused = true
            },
            onAddressFocus      = {
                addressBarFocused = true
                if (settings.useBuiltInKeyboard) showKb = true
            },
            onAddressGo         = {
                navigate(addressText)
                if (settings.useBuiltInKeyboard) showKb = false
            },
            onBookmarkToggle    = { toggleBookmark() },
            onMenuOpen          = { showMenu = true },
            onBookmarksPanel    = {
                activePanel = if (activePanel == BrowserPanel.BOOKMARKS) BrowserPanel.NONE
                else BrowserPanel.BOOKMARKS
            },
            onHistoryPanel      = {
                activePanel = if (activePanel == BrowserPanel.HISTORY) BrowserPanel.NONE
                else BrowserPanel.HISTORY
            },
            onDownloadsPanel    = {
                activePanel = if (activePanel == BrowserPanel.DOWNLOADS) BrowserPanel.NONE
                else BrowserPanel.DOWNLOADS
            },
            onSettingsPanel     = {
                activePanel = if (activePanel == BrowserPanel.SETTINGS) BrowserPanel.NONE
                else BrowserPanel.SETTINGS
            },
            onFindInPage        = {
                isFindActive = true
                if (settings.useBuiltInKeyboard) showKb = true
            },
            onDesktopModeToggle = {
                settings = settings.copy(desktopMode = !settings.desktopMode)
                activeWebView()?.reload()
            }
        )

        // ── Bookmarks Bar ──────────────────────────────────────────────
        AnimatedVisibility(settings.showBookmarksBar && !activeTab.isPrivate) {
            BookmarksBar(
                bookmarks       = bookmarks,
                isDark          = isDark,
                navBarBg        = navBarBg,
                borderColor     = borderColor,
                onBookmarkClick = { bm -> navigate(bm.url) }
            )
        }

        // ── Address suggestions ────────────────────────────────────────
        AnimatedVisibility(
            visible = addressBarFocused && addressText.isNotEmpty() &&
                    (history.isNotEmpty() || bookmarks.isNotEmpty())
        ) {
            AddressSuggestionsDropdown(
                query             = addressText,
                history           = history,
                bookmarks         = bookmarks,
                isDark            = isDark,
                onSuggestionClick = { url ->
                    navigate(url)
                    addressBarFocused = false
                    showKb = false
                },
                onDismiss         = { addressBarFocused = false }
            )
        }

        // ── Find in Page bar ───────────────────────────────────────────
        AnimatedVisibility(isFindActive) {
            FindInPageBar(
                query         = findQuery,
                activeMatch   = findActiveMatch,
                totalMatches  = findTotalMatches,
                isDark        = isDark,
                useBuiltInKb  = settings.useBuiltInKeyboard,
                onQueryChange = { q ->
                    findQuery = q
                    if (q.isEmpty()) {
                        activeWebView()?.clearMatches()
                        findActiveMatch  = 0
                        findTotalMatches = 0
                    } else {
                        activeWebView()?.findAllAsync(q)
                    }
                },
                onFocused     = { if (settings.useBuiltInKeyboard) showKb = true },
                onFindNext    = { activeWebView()?.findNext(true) },
                onFindPrev    = { activeWebView()?.findNext(false) },
                onClose       = {
                    isFindActive     = false
                    findQuery        = ""
                    findActiveMatch  = 0
                    findTotalMatches = 0
                    activeWebView()?.clearMatches()
                    showKb = false
                }
            )
        }

        // ── Main content area ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ── One WebView per tab; only active one fills the screen ──
            tabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val isNewTab = tab.url == NEWTAB_URL

                // Hide inactive tabs by collapsing them to zero size
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isActive) 1f else 0f)
                        .then(if (!isActive) Modifier.size(0.dp) else Modifier)
                ) {
                    if (isNewTab) {
                        NewTabPage(
                            isDark     = isDark,
                            isPrivate  = tab.isPrivate,
                            settings   = settings,
                            bookmarks  = bookmarks,
                            history    = history,
                            onNavigate = { url -> navigate(url, tab.id) }
                        )
                    } else {
                        BrowserWebView(
                            url                  = tab.url,
                            tab                  = tab,
                            settings             = settings,
                            findQuery            = findQuery,
                            isFindActive         = isFindActive,
                            modifier             = Modifier.fillMaxSize(),
                            onWebViewReady       = { wv -> webViews[tab.id] = wv },
                            onPageStarted        = { url ->
                                updateTab(tab.id) {
                                    this.url          = url
                                    this.displayUrl   = UrlUtils.displayUrl(url)
                                    this.isLoading    = true
                                    this.loadProgress = 0f
                                }
                                if (isActive) addressText = UrlUtils.displayUrl(url)
                            },
                            onProgressChanged    = { p ->
                                updateTab(tab.id) { loadProgress = p / 100f }
                            },
                            onPageFinished       = { url, pageTitle ->
                                val resolvedTitle = pageTitle?.takeIf { it.isNotBlank() } ?: url
                                val color         = UrlUtils.colorForUrl(url)
                                updateTab(tab.id) {
                                    this.url          = url
                                    this.displayUrl   = UrlUtils.displayUrl(url)
                                    this.isLoading    = false
                                    this.loadProgress = 1f
                                    this.lastVisited  = System.currentTimeMillis()
                                    this.faviconColor = color
                                }
                                addHistoryEntry(url, resolvedTitle, color)
                                if (isActive) addressText = UrlUtils.displayUrl(url)
                                scheduleSave()
                            },
                            onTitleChanged       = { title ->
                                updateTab(tab.id) { this.title = title }
                                scheduleSave()
                            },
                            onUrlChanged         = { url ->
                                if (isActive) addressText = UrlUtils.displayUrl(url)
                            },
                            onFaviconChanged     = { /* bitmap available; cache if needed */ },
                            onFindResultsChanged = { active, total ->
                                findActiveMatch  = active
                                findTotalMatches = total
                            },
                            onDownloadStart      = { dlUrl, ua, cd, mime, len ->
                                val item = dlHelper.enqueue(
                                    url                = dlUrl,
                                    userAgent          = ua,
                                    contentDisposition = cd,
                                    mimeType           = mime,
                                    contentLength      = len,
                                    onProgress         = { progress, bytes ->
                                        val idx = downloads.indexOfFirst { it.url == dlUrl }
                                        if (idx >= 0) {
                                            downloads[idx] = downloads[idx].copy(
                                                progress        = progress,
                                                bytesDownloaded = bytes
                                            )
                                        }
                                    },
                                    onComplete = { status ->
                                        val idx = downloads.indexOfFirst { it.url == dlUrl }
                                        if (idx >= 0) {
                                            downloads[idx] = downloads[idx].copy(status = status)
                                        }
                                    }
                                )
                                downloads.add(0, item)
                                activePanel = BrowserPanel.DOWNLOADS
                            },
                            onJsDialog           = { d -> jsDialog = d },
                            onSslError           = { d -> sslDialog = d },
                            onPermissionRequest  = { r -> permDialog = r },
                            onGeolocationRequest = { origin, cb -> geoDialog = Pair(origin, cb) },
                            onNewTabRequested    = { url -> addTab(url) },
                            isDark               = isDark
                        )
                    }
                }
            }

            // ── Side Panel — anchored TopEnd inside Box ────────────────
            // AnimatedVisibility is NOT used here because its ColumnScope
            // extension overload cannot be resolved unambiguously inside Box.
            // Visibility is controlled by the outer if-guard instead.
            if (activePanel != BrowserPanel.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .zIndex(10f)
                ) {
                    SidePanel(
                        panel            = activePanel,
                        isDark           = isDark,
                        surfaceColor     = surfaceColor,
                        textColor        = textColor,
                        borderColor      = borderColor,
                        bookmarks        = bookmarks,
                        history          = history,
                        downloads        = downloads,
                        settings         = settings,
                        onSettingsChange = { s ->
                            settings = s
                            val wv = activeWebView()
                            wv?.settings?.javaScriptEnabled        = s.javaScriptEnabled
                            wv?.settings?.loadsImagesAutomatically = s.showImages
                            scheduleSave()
                        },
                        onNavigate       = { url -> navigate(url); activePanel = BrowserPanel.NONE },
                        onClose          = { activePanel = BrowserPanel.NONE }
                    )
                }
            }

            // ── Tab Overview ───────────────────────────────────────────
            if (showTabOverview) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                ) {
                    TabOverviewGrid(
                        tabs            = tabs,
                        activeTabId     = activeTabId,
                        isDark          = isDark,
                        onSelectTab     = { tab ->
                            activeTabId     = tab.id
                            addressText     = UrlUtils.displayUrl(tab.url)
                            showTabOverview = false
                        },
                        onCloseTab      = { closeTab(it) },
                        onNewTab        = { addTab(); showTabOverview = false },
                        onNewPrivateTab = { addTab(isPrivate = true); showTabOverview = false }
                    )
                }
            }

            // ── Context Menu ───────────────────────────────────────────
            if (showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(30f)
                ) {
                    EdgeContextMenu(
                        isDark          = isDark,
                        surfaceColor    = surfaceColor,
                        textColor       = textColor,
                        isBookmarked    = isBookmarked,
                        isPrivateTab    = activeTab.isPrivate,
                        currentZoom     = currentZoom,
                        onNewTab        = { addTab() },
                        onNewPrivateTab = { addTab(isPrivate = true) },
                        onBookmarks     = { activePanel = BrowserPanel.BOOKMARKS },
                        onHistory       = { activePanel = BrowserPanel.HISTORY },
                        onDownloads     = { activePanel = BrowserPanel.DOWNLOADS },
                        onSettings      = { activePanel = BrowserPanel.SETTINGS },
                        onFindInPage    = { isFindActive = true },
                        onPrint         = { printPage() },
                        onZoomIn        = { setZoom(currentZoom + 10) },
                        onZoomOut       = { setZoom(currentZoom - 10) },
                        onZoomReset     = { setZoom(100) },
                        onAddBookmark   = { toggleBookmark() },
                        onShare         = { sharePage() },
                        onClearData     = {
                            history.clear()
                            repo.clearHistory()
                            repo.clearCookiesAndCache(context)
                        },
                        onDismiss       = { showMenu = false }
                    )
                }
            }
        }

        // ── Floating Keyboard (bottom of Column, outside Box) ──────────
        AnimatedVisibility(
            visible = showKb && settings.useBuiltInKeyboard,
            enter   = slideInVertically { it },
            exit    = slideOutVertically { it }
        ) {
            FloatingKeyboard(
                currentText  = if (isFindActive) findQuery else addressText,
                onTextChange = { text ->
                    if (isFindActive) {
                        findQuery = text
                        if (text.isEmpty()) {
                            activeWebView()?.clearMatches()
                            findActiveMatch  = 0
                            findTotalMatches = 0
                        } else {
                            activeWebView()?.findAllAsync(text)
                        }
                    } else {
                        addressText       = text
                        addressBarFocused = true
                    }
                },
                onDismiss    = { showKb = false; addressBarFocused = false },
                onSubmit     = {
                    if (isFindActive) activeWebView()?.findNext(true)
                    else navigate(addressText)
                },
                isDark       = isDark
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Dialogs (float above everything)
    // ═════════════════════════════════════════════════════════════════

    jsDialog?.let { dialog ->
        JsDialog(state = dialog, isDark = isDark)
    }

    sslDialog?.let { dialog ->
        SslWarningDialog(
            state  = dialog.copy(
                onProceed = { dialog.onProceed(); sslDialog = null },
                onCancel  = { dialog.onCancel();  sslDialog = null }
            ),
            isDark = isDark
        )
    }

    permDialog?.let { req ->
        PermissionRequestDialog(
            request = BrowserPermissionRequest(
                origin    = req.origin,
                resources = req.resources,
                grant     = { req.grant(); permDialog = null },
                deny      = { req.deny();  permDialog = null }
            ),
            isDark = isDark
        )
    }

    geoDialog?.let { (origin, cb) ->
        GeolocationDialog(
            origin  = origin,
            onAllow = { cb.invoke(origin, true, false);  geoDialog = null },
            onDeny  = { cb.invoke(origin, false, false); geoDialog = null },
            isDark  = isDark
        )
    }
}
